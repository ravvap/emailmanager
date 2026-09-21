package gov.fdic.tip.emailmanager.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "business_events_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessEventAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "actor_id", length = 255)
    private String actorId;

    @Column(name = "actor_label", length = 255)
    private String actorLabel;

    @Column(name = "actor_type", length = 50)
    private String actorType;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @Column(name = "module", length = 100)
    private String module;

    @Column(name = "outcome", length = 20)
    private String outcome;

    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    @Column(name = "target_entity_id", length = 255)
    private String targetEntityId;

    @Column(name = "target_entity_label", length = 255)
    private String targetEntityLabel;

    @Column(name = "target_entity_type", length = 100)
    private String targetEntityType;

    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @Column(name = "user_agent", length = 255)
    private String userAgent;
}