package gov.fdic.tip.emailmanager.sendemail.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "em_email_send_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailSendAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "send_id", nullable = false)
    private Long sendId;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "performed_by", nullable = false)
    private String performedBy;

    @Column(name = "detail", length = 1000)
    private String detail;

    @CreationTimestamp
    @Column(name = "performed_at", nullable = false, updatable = false)
    private OffsetDateTime performedAt;
}