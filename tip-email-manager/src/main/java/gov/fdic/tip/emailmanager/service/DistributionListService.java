package gov.fdic.tip.emailmanager.service;

import gov.fdic.tip.emailmanager.dto.DistributionListDto;
import gov.fdic.tip.emailmanager.dto.DistributionListMemberDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DistributionListService {

    List<DistributionListDto> getAllDistributionLists();

    DistributionListDto getDistributionListById(Long id);

    Page<DistributionListMemberDto> filterMembers(Long listId, String query, Pageable pageable);

    DistributionListDto createDistributionList(DistributionListDto dto, String username);

    DistributionListDto updateDistributionList(Long id, DistributionListDto dto, String username);

    void deleteDistributionList(Long id, String username);
    
    
 // 1. Fetch summary list of all distribution lists for main table grid
    List<DistributionListSummaryDto> getAllDistributionListsSummary();

    // 2. Fetch specific distribution list details + paginated/filtered members for View Modal
    DistributionListViewDto getDistributionListDetails(Long id, String filter, Pageable pageable);

    // 3. Standalone query for member filtering and pagination
 
    DistributionListSummaryDto createDistributionList(CreateDistributionListRequest request, String username);

 }