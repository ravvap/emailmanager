package gov.fdic.tip.emailmanager.repository;

import gov.fdic.tip.emailmanager.entity.DistributionList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistributionListRepository extends JpaRepository<DistributionList, Long> {

    // Retrieve non-deleted lists sorted by updated_at descending (latest first per mockup requirement)
    List<DistributionList> findByDeletedAtIsNullOrderByUpdatedAtDescCreatedAtDesc();

    Optional<DistributionList> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT COUNT(d) > 0 FROM DistributionList d WHERE LOWER(d.name) = LOWER(:name) AND d.status = 'Active' AND d.deletedAt IS NULL")
    boolean existsActiveName(@Param("name") String name);

    @Query("SELECT COUNT(d) > 0 FROM DistributionList d WHERE LOWER(d.name) = LOWER(:name) AND d.status = 'Active' AND d.id <> :id AND d.deletedAt IS NULL")
    boolean existsActiveNameExcludingId(@Param("name") String name, @Param("id") Long id);

    // Checks if the distribution list is currently associated with an active/in-flight email send batch
    @Query(value = "SELECT COUNT(*) > 0 FROM batch_job bj WHERE bj.distribution_list_id = :listId AND bj.status IN ('PENDING', 'PROCESSING', 'IN_FLIGHT')", nativeQuery = true)
    boolean isInFlightSend(@Param("listId") Long listId);

    // Checks if the distribution list has ever been used in past send histories
    @Query(value = "SELECT COUNT(*) > 0 FROM send_log sl WHERE sl.distribution_list_id = :listId", nativeQuery = true)
    boolean hasSendHistory(@Param("listId") Long listId);
    
    
}