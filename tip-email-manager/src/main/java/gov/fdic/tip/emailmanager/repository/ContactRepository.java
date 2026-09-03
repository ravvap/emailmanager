package gov.fdic.tip.emailmanager.repository;

import gov.fdic.tip.emailmanager.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    List<Contact> findByDeletedAtIsNull();

    Optional<Contact> findByIdAndDeletedAtIsNull(Long id);

    // Guard rule: Duplicate active contacts with same email are rejected
    @Query("SELECT COUNT(c) > 0 FROM Contact c WHERE LOWER(c.email) = LOWER(:email) AND c.status = 'Active' AND c.deletedAt IS NULL")
    boolean existsActiveEmail(@Param("email") String email);

    @Query("SELECT COUNT(c) > 0 FROM Contact c WHERE LOWER(c.email) = LOWER(:email) AND c.status = 'Active' AND c.id <> :id AND c.deletedAt IS NULL")
    boolean existsActiveEmailExcludingId(@Param("email") String email, @Param("id") Long id);

    // Queries to check list memberships and send history for deletion rules
    @Query(value = "SELECT COUNT(*) FROM distribution_list_member WHERE contact_id = :contactId", nativeQuery = true)
    long countListMemberships(@Param("contactId") Long contactId);

    @Query(value = "SELECT dl.name FROM distribution_list dl JOIN distribution_list_member dlm ON dl.id = dlm.list_id WHERE dlm.contact_id = :contactId", nativeQuery = true)
    List<String> findAssociatedDistributionListNames(@Param("contactId") Long contactId);

    @Query(value = "SELECT COUNT(*) FROM send_log_recipient WHERE contact_id = :contactId", nativeQuery = true)
    long countSendHistory(@Param("contactId") Long contactId);

    // Attribute search filtering (Date / Fixed List / Text)
    @Query("SELECT DISTINCT c FROM Contact c JOIN c.attributeValues av WHERE av.attribute.id = :attributeId AND LOWER(av.attributeValue) = LOWER(:value) AND c.deletedAt IS NULL")
    List<Contact> findByAttributeAndValue(@Param("attributeId") Long attributeId, @Param("value") String value);
}