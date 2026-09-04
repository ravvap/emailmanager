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
}