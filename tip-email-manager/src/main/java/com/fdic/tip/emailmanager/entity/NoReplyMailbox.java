package com.fdic.tip.emailmanager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "no_reply_mailbox")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoReplyMailbox extends BaseAuditableEntity {

    @Id
    private Long id = 1L;

    @Column(name = "email_address", nullable = false, unique = true)
    private String emailAddress;
}