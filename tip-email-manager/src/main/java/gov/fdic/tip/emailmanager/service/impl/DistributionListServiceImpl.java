package gov.fdic.tip.emailmanager.service.impl;

import gov.fdic.tip.emailmanager.dto.CreateDistributionListRequest;
import gov.fdic.tip.emailmanager.dto.DistributionListSummaryDto;
import gov.fdic.tip.emailmanager.dto.DistributionListViewDto;
import gov.fdic.tip.emailmanager.dto.DistributionListMemberDto;
import gov.fdic.tip.emailmanager.entity.Contact;
import gov.fdic.tip.emailmanager.entity.DistributionList;
import gov.fdic.tip.emailmanager.entity.DistributionListMember;
import gov.fdic.tip.emailmanager.repository.ContactRepository;
import gov.fdic.tip.emailmanager.repository.DistributionListMemberRepository;
import gov.fdic.tip.emailmanager.repository.DistributionListRepository;
import gov.fdic.tip.emailmanager.service.AuditLogService;
import gov.fdic.tip.emailmanager.service.DistributionListService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
    public DistributionListSummaryDto updateDistributionList(Long id, CreateDistributionListRequest request, String username) {
        // Fetch existing list
        DistributionList list = distributionListRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Distribution List not found with id: " + id));

        // Rule: Edit not allowed on a list with an in-flight send
        if (distributionListRepository.isInFlightSend(id)) {
            throw new IllegalStateException("Edit not allowed on a distribution list with an in-flight send.");
        }

        // Validation: Name uniqueness check for Active lists (excluding current list ID)
        if ("Active".equalsIgnoreCase(request.getStatus()) &&
            distributionListRepository.existsActiveNameExcludingId(request.getName().trim(), id)) {
            throw new IllegalArgumentException("A distribution list with the name '" + request.getName() + "' already exists.");
        }

        // Update list properties
        list.setName(request.getName().trim());
        list.setStatus(request.getStatus());
        list.setUpdatedBy(username);

        // Replace contact members roster
        list.getMembers().clear();
        if (request.getContactIds() != null && !request.getContactIds().isEmpty()) {
            for (Long contactId : request.getContactIds()) {
                Contact contact = contactRepository.findByIdAndDeletedAtIsNull(contactId)
                        .orElseThrow(() -> new IllegalArgumentException("Contact not found with ID: " + contactId));

                // Rule: Only active contacts can be associated
                if (!"Active".equalsIgnoreCase(contact.getStatus())) {
                    throw new IllegalArgumentException("Contact (" + contact.getEmail() + ") is inactive and cannot be added.");
                }

                DistributionListMember member = new DistributionListMember();
                member.setDistributionList(list);
                member.setContact(contact);
                member.setAddedBy(username);
                list.getMembers().add(member);
            }
        }

        DistributionList updatedList = distributionListRepository.save(list);

        auditLogService.logAction("DISTRIBUTION_LIST", updatedList.getId(), "UPDATE", username,
                "Updated distribution list '" + updatedList.getName() + "' with " + updatedList.getMembers().size() + " member(s).");

        return convertToSummaryDto(updatedList);
    }

    @Override
    public void deleteDistributionList(Long id, String username) {
        DistributionList list = distributionListRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Distribution List not found with id: " + id));

        // Rule: Deletion not allowed on a list with an in-flight send
        if (distributionListRepository.isInFlightSend(id)) {
            throw new IllegalStateException("Deletion not allowed on a distribution list with an in-flight send.");
        }

        boolean hasHistory = distributionListRepository.hasSendHistory(id);

        // Rule: Delete allowed on inactive distribution lists that do not have send history
        if (!hasHistory) {
            // Hard delete from database if never used in email sends
            distributionListRepository.delete(list);
            auditLogService.logAction("DISTRIBUTION_LIST", id, "HARD_DELETE", username,
                    "Permanently deleted distribution list: " + list.getName());
        } else {
            // Soft delete (deactivate) to preserve historic send logs and audit trail
            list.setStatus("Inactive");
            list.setDeletedBy(username);
            list.setDeletedAt(OffsetDateTime.now());
            distributionListRepository.save(list);

            auditLogService.logAction("DISTRIBUTION_LIST", id, "SOFT_DELETE", username,
                    "Deactivated distribution list due to existing send history: " + list.getName());
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

    @Override
    public DistributionListSummaryDto createDistributionList(CreateDistributionListRequest request, String username) {
        if ("Active".equalsIgnoreCase(request.getStatus()) && distributionListRepository.existsActiveName(request.getName().trim())) {
            throw new IllegalArgumentException("A distribution list with the name '" + request.getName() + "' already exists.");
        }

        DistributionList list = new DistributionList();
        list.setName(request.getName().trim());
        list.setStatus(request.getStatus());
        list.setCreatedBy(username);

        for (Long contactId : request.getContactIds()) {
            Contact contact = contactRepository.findByIdAndDeletedAtIsNull(contactId)
                    .orElseThrow(() -> new IllegalArgumentException("Contact not found with ID: " + contactId));

            if (!"Active".equalsIgnoreCase(contact.getStatus())) {
                throw new IllegalArgumentException("Contact (" + contact.getEmail() + ") is inactive and cannot be added.");
            }

            DistributionListMember member = new DistributionListMember();
            member.setDistributionList(list);
            member.setContact(contact);
            member.setAddedBy(username);
            list.getMembers().add(member);
        }

        DistributionList savedList = distributionListRepository.save(list);
        auditLogService.logAction("DISTRIBUTION_LIST", savedList.getId(), "CREATE", username,
                "Created distribution list '" + savedList.getName() + "' with " + savedList.getMembers().size() + " member(s).");

        return convertToSummaryDto(savedList);
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