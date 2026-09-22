package gov.fdic.tip.emailmanager.repository;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import gov.fdic.tip.emailmanager.entity.DataSourceQuery;

/**
 * Repository interface for Data Source Query operations.
 *
 * @author prasad ravva
 */
@Repository
public interface DataSourceQueryRepository extends JpaRepository<DataSourceQuery, UUID> {

    List<DataSourceQuery> findByAssetIdOrderByVersionDesc(UUID assetId);

    List<DataSourceQuery> findAllByOrderByCreatedAtDesc();

    Optional<DataSourceQuery> findTopByAssetIdOrderByVersionDesc(UUID assetId);
}