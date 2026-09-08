package gov.fdic.tip.emailmanager.service.impl;

import gov.fdic.tip.emailmanager.constant.ActorType;
import gov.fdic.tip.emailmanager.dto.*;
import gov.fdic.tip.emailmanager.entity.Contact;
import gov.fdic.tip.emailmanager.entity.DistributionList;
import gov.fdic.tip.emailmanager.entity.DistributionListMember;
import gov.fdic.tip.emailmanager.repository.ContactRepository;
import gov.fdic.tip.emailmanager.repository.DistributionListMemberRepository;
import gov.fdic.tip.emailmanager.repository.DistributionListRepository;
import gov.fdic.tip.emailmanager.service.AuditLogService;
import gov.fdic.tip.emailmanager.service.DistributionListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class DistributionListServiceImpl implements DistributionListService {

    private final DistributionListRepository distributionListRepository;
    private final DistributionListMemberRepository memberRepository;
    private final ContactRepository contactRepository;
    private final AuditLogService auditLogService;

    public DistributionListServiceImpl(DistributionListRepository distributionListRepository,
                                       DistributionListMemberRepository memberRepository,
                                       ContactRepository contactRepository,
                                       AuditLogService auditLogService) {
        this.distributionListRepository = distributionListRepository;
        this.memberRepository = memberRepository;
        this.contactRepository = contactRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public DistributionListSummaryDto createDistributionList(CreateDistributionListRequest request, String actorEmail) {
        log.debug("Executing createDistributionList for name: {}", request.getName());

        if ("Active".equalsIgnoreCase(request.getStatus()) && distributionListRepository.existsActiveName(request.getName().trim())) {
            log.error("Failed to create distribution list. Duplicate active name: {}", request.getName());

            auditLogService.emitAuditEvent(
                    "DISTRIBUTION_LIST_CREATE_FAILED",
                    "N/A",
                    request.getName(),
                    ActorType.USER,
                    actorEmail,
                    actorEmail,
                    Map.of("reason", "Duplicate active distribution list name"),
                    "FAILURE"
            );
            throw new IllegalArgumentException("A distribution list with the name '" + request.getName() + "' already exists.");
        }

        DistributionList list = new DistributionList();
        list.setName(request.getName().trim());
        list.setStatus(request.getStatus());
        list.setCreatedBy(actorEmail);

        for (Long contactId : request.getContactIds()) {
            Contact contact = contactRepository.findByIdAndDeletedAtIsNull(contactId)
                    .orElseThrow(() -> new IllegalArgumentException("Contact not found with ID: " + contactId));

            if (!"Active".equalsIgnoreCase(contact.getStatus())) {
                throw new IllegalArgumentException("Contact (" + contact.getEmail() + ") is inactive and cannot be added.");
            }

            DistributionListMember member = new DistributionListMember();
            member.setDistributionList(list);
            member.setContact(contact);
            member.setAddedBy(actorEmail);
            list.getMembers().add(member);
        }

        DistributionList savedList = distributionListRepository.save(list);

        auditLogService.emitAuditEvent(
                "DISTRIBUTION_LIST_SERVICE_CREATE",
                String.valueOf(savedList.getId()),
                savedList.getName(),
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("memberCount", savedList.getMembers().size()),
                "SUCCESS"
        );

        return convertToSummaryDto(savedList);
    }

    @Override
    public DistributionListSummaryDto updateDistributionList(Long id, CreateDistributionListRequest request, String actorEmail) {
        log.debug("Executing updateDistributionList for ID: {}", id);

        DistributionList list = distributionListRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Distribution List not found with id: " + id));

        if (distributionListRepository.isInFlightSend(id)) {
            log.warn("Blocked update attempt on list ID: {} due to in-flight send process.", id);

            auditLogService.emitAuditEvent(
                    "DISTRIBUTION_LIST_UPDATE_BLOCKED",
                    String.valueOf(id),
                    list.getName(),
                    ActorType.USER,
                    actorEmail,
                    actorEmail,
                    Map.of("reason", "In-flight send process active"),
                    "FAILURE"
            );
            throw new IllegalStateException("Edit not allowed on a distribution list with an in-flight send.");
        }

        if ("Active".equalsIgnoreCase(request.getStatus()) &&
            distributionListRepository.existsActiveNameExcludingId(request.getName().trim(), id)) {
            throw new IllegalArgumentException("A distribution list with the name '" + request.getName() + "' already exists.");
        }

        list.setName(request.getName().trim());
        list.setStatus(request.getStatus());
        list.setUpdatedBy(actorEmail);

        list.getMembers().clear();
        if (request.getContactIds() != null && !request.getContactIds().isEmpty()) {
            for (Long contactId : request.getContactIds()) {
                Contact contact = contactRepository.findByIdAndDeletedAtIsNull(contactId)
                        .orElseThrow(() -> new IllegalArgumentException("Contact not found with ID: " + contactId));

                if (!"Active".equalsIgnoreCase(contact.getStatus())) {
                    throw new IllegalArgumentException("Contact (" + contact.getEmail() + ") is inactive and cannot be added.");
                }

                DistributionListMember member = new DistributionListMember();
                member.setDistributionList(list);
                member.setContact(contact);
                member.setAddedBy(actorEmail);
                list.getMembers().add(member);
            }
        }

        DistributionList updatedList = distributionListRepository.save(list);

        auditLogService.emitAuditEvent(
                "DISTRIBUTION_LIST_SERVICE_UPDATE",
                String.valueOf(updatedList.getId()),
                updatedList.getName(),
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("memberCount", updatedList.getMembers().size()),
                "SUCCESS"
        );

        return convertToSummaryDto(updatedList);
    }

    @Override
    public void deleteDistributionList(Long id, String actorEmail) {
        log.debug("Executing deleteDistributionList for ID: {}", id);

        DistributionList list = distributionListRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Distribution List not found with id: " + id));

        if (distributionListRepository.isInFlightSend(id)) {
            log.warn("Blocked delete attempt on list ID: {} due to in-flight send process.", id);

            auditLogService.emitAuditEvent(
                    "DISTRIBUTION_LIST_DELETE_BLOCKED",
                    String.valueOf(id),
                    list.getName(),
                    ActorType.USER,
                    actorEmail,
                    actorEmail,
                    Map.of("reason", "In-flight send process active"),
                    "FAILURE"
            );
            throw new IllegalStateException("Deletion not allowed on a distribution list with an in-flight send.");
        }

        boolean hasHistory = distributionListRepository.hasSendHistory(id);

        if (!hasHistory) {
            distributionListRepository.delete(list);
            log.info("[SPLUNK_TRACKING] Permanently deleted distribution list ID: {}", id);

            auditLogService.emitAuditEvent(
                    "DISTRIBUTION_LIST_HARD_DELETE",
                    String.valueOf(id),
                    list.getName(),
                    ActorType.USER,
                    actorEmail,
                    actorEmail,
                    Map.of("deleteType", "HARD_DELETE"),
                    "SUCCESS"
            );
        } else {
            list.setStatus("Inactive");
            list.setDeletedBy(actorEmail);
            list.setDeletedAt(OffsetDateTime.now());
            distributionListRepository.save(list);
            log.info("[SPLUNK_TRACKING] Soft deleted (deactivated) distribution list ID: {}", id);

            auditLogService.emitAuditEvent(
                    "DISTRIBUTION_LIST_SOFT_DELETE",
                    String.valueOf(id),
                    list.getName(),
                    ActorType.USER,
                    actorEmail,
                    actorEmail,
                    Map.of("deleteType", "SOFT_DELETE"),
                    "SUCCESS"
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DistributionListSummaryDto> getAllDistributionListsSummary() {
        return distributionListRepository.findByDeletedAtIsNullOrderByUpdatedAtDescCreatedAtDesc().stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DistributionListViewDto getDistributionListDetails(Long id, String filter, Pageable pageable) {
        DistributionList list = distributionListRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Distribution List not found with id: " + id));

        Page<DistributionListMemberDto> membersPage = filterMembers(id, filter, pageable);

        DistributionListViewDto dto = new DistributionListViewDto();
        dto.setId(list.getId());
        dto.setName(list.getName());
        dto.setStatus(list.getStatus());
        dto.setCreatedBy(list.getCreatedBy());
        dto.setCreatedAt(list.getCreatedAt());
        dto.setUpdatedBy(list.getUpdatedBy());
        dto.setUpdatedAt(list.getUpdatedAt());
        dto.setMembers(membersPage);

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DistributionListMemberDto> filterMembers(Long listId, String query, Pageable pageable) {
        return memberRepository.filterMembersByListIdAndQuery(listId, query != null ? query.trim() : "", pageable)
                .map(dlm -> {
                    DistributionListMemberDto dto = new DistributionListMemberDto();
                    dto.setContactId(dlm.getContact().getId());
                    dto.setName(dlm.getContact().getName());
                    dto.setEmail(dlm.getContact().getEmail());
                    dto.setStatus(dlm.getContact().getStatus());
                    return dto;
                });
    }

    private DistributionListSummaryDto convertToSummaryDto(DistributionList list) {
        long count = distributionListRepository.countActiveMembersByListId(list.getId());
        boolean isInFlight = distributionListRepository.isInFlightSend(list.getId());
        boolean hasHistory = distributionListRepository.hasSendHistory(list.getId());

        DistributionListSummaryDto dto = new DistributionListSummaryDto();
        dto.setId(list.getId());
        dto.setName(list.getName());
        dto.setMemberCount(count);
        dto.setMemberDisplayLabel(count + " Contact" + (count == 1 ? "" : "s"));
        dto.setStatus(list.getStatus());
        dto.setCreatedBy(list.getCreatedBy());
        dto.setCreatedAt(list.getCreatedAt());
        dto.setUpdatedBy(list.getUpdatedBy());
        dto.setUpdatedAt(list.getUpdatedAt());
        dto.setCanEdit(!isInFlight);
        dto.setCanDelete("Inactive".equalsIgnoreCase(list.getStatus()) && !hasHistory && !isInFlight);

        return dto;
    }
}