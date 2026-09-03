package gov.fdic.tip.emailmanager.repository;

import gov.fdic.tip.emailmanager.entity.ContactAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactAttributeRepository extends JpaRepository<ContactAttribute, Long> {

    List<ContactAttribute> findByDeletedAtIsNull();

    List<ContactAttribute> findByStatusAndDeletedAtIsNull(String status);

    Optional<ContactAttribute> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndDeletedAtIsNull(String name, Long id);

    // Mock query: Checks if an attribute option value is currently used by any contact metadata record
    @Query(value = "SELECT COUNT(*) > 0 FROM contact_attribute_value WHERE attribute_id = :attributeId AND value = :value", nativeQuery = true)
    boolean isOptionInUseByContacts(@Param("attributeId") Long attributeId, @Param("value") String value);
}