package gov.fdic.tip.emailmanager.repository;

import gov.fdic.tip.emailmanager.entity.DistributionListMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DistributionListMemberRepository extends JpaRepository<DistributionListMember, Long> {

    // Filter list members by name/email search term with pagination (per view/edit modal requirement)
    @Query("SELECT dlm FROM DistributionListMember dlm JOIN dlm.contact c WHERE dlm.distributionList.id = :listId AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<DistributionListMember> filterMembersByQuery(@Param("listId") Long listId, @Param("query") String query, Pageable pageable);
    
 // Filter list members by Name or Email search query with pagination
    @Query("SELECT dlm FROM DistributionListMember dlm " +
           "JOIN dlm.contact c " +
           "WHERE dlm.distributionList.id = :listId " +
           "AND c.deletedAt IS NULL " +
           "AND (:query IS NULL OR :query = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<DistributionListMember> filterMembersByListIdAndQuery(
            @Param("listId") Long listId,
            @Param("query") String query,
            Pageable pageable);
}