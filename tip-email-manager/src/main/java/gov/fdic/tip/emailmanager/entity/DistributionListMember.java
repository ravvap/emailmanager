package gov.fdic.tip.emailmanager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "distribution_list_member")
@Getter
@Setter
public class DistributionListMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    private DistributionList distributionList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @Column(name = "added_by", nullable = false)
    private String addedBy;

    @Column(name = "added_at", nullable = false, updatable = false)
    private OffsetDateTime addedAt;

    @PrePersist
    protected void onCreate() {
        this.addedAt = OffsetDateTime.now();
    }
}