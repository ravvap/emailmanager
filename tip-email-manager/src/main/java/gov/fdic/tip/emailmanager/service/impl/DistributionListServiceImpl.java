package gov.fdic.tip.emailmanager.service.impl;

import gov.fdic.tip.emailmanager.dto.CreateDistributionListRequest;
import gov.fdic.tip.emailmanager.dto.DistributionListSummaryDto;
import gov.fdic.tip.emailmanager.entity.Contact;
import gov.fdic.tip.emailmanager.entity.DistributionList;
import gov.fdic.tip.emailmanager.entity.DistributionListMember;
import gov.fdic.tip.emailmanager.repository.ContactRepository;
import gov.fdic.tip.emailmanager.repository.DistributionListRepository;
import gov.fdic.tip.emailmanager.service.AuditLogService;
import gov.fdic.tip.emailmanager.service.DistributionListService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DistributionListServiceImpl implements DistributionListService {

    private final DistributionListRepository distributionListRepository;
    private final ContactRepository contactRepository;
    private final AuditLogService auditLogService;

    public DistributionListServiceImpl(DistributionListRepository distributionListRepository,
                                       ContactRepository contactRepository,
                                       AuditLogService auditLogService) {
        this.distributionListRepository = distributionListRepository;
        this.contactRepository = contactRepository;
        this.auditLogService = auditLogService;
    }

 // Details for ALL Distribution Lists (Grid view)
    @Override
    @Transactional(readOnly = true)
    public List<DistributionListSummaryDto> getAllDistributionListsSummary() {
        return distributionListRepository.findByDeletedAtIsNullOrderByUpdatedAtDescCreatedAtDesc().stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());
    }

    // Details per SPECIFIC Distribution List (Modal view)
    @Override
    @Transactional(readOnly = true)
    public DistributionListViewDto getDistributionListDetails(Long id, String filter, Pageable pageable) {
        DistributionList list = distributionListRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Distribution List not found with id: " + id));

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
        // Validation: Unique active name constraint
        if ("Active".equalsIgnoreCase(request.getStatus()) && distributionListRepository.existsActiveName(request.getName())) {
            throw new IllegalArgumentException("A distribution list with the name '" + request.getName() + "' already exists.");
        }

        DistributionList list = new DistributionList();
        list.setName(request.getName().trim());
        list.setStatus(request.getStatus());
        list.setCreatedBy(username);

        // Associate selected contact members during list creation
        for (Long contactId : request.getContactIds()) {
            Contact contact = contactRepository.findByIdAndDeletedAtIsNull(contactId)
                    .orElseThrow(() -> new IllegalArgumentException("Contact not found with ID: " + contactId));

            // Rule: Only active contacts can be added to distribution lists
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
                "Created distribution list '" + savedList.getName() + "' with " + savedList.getMembers().size() + " contact member(s).");

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