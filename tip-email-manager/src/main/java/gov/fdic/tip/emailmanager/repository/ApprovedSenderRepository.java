package gov.fdic.tip.emailmanager.repository;

import gov.fdic.tip.emailmanager.entity.ApprovedSender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovedSenderRepository extends JpaRepository<ApprovedSender, Long> {

    List<ApprovedSender> findByDeletedAtIsNull();

    List<ApprovedSender> findByStatusAndDeletedAtIsNull(String status);

    Optional<ApprovedSender> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByMailboxAddressAndDeletedAtIsNull(String mailboxAddress);
}