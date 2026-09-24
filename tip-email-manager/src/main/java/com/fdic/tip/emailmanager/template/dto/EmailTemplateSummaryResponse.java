package com.fdic.tip.emailmanager.template.dto;

import com.fdic.tip.emailmanager.template.enums.TemplateStatus;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

/** Row shape for the Templates list screen. */
@Value
@Builder
public class EmailTemplateSummaryResponse {

    Long templateId;
    String templateName;
    String fromDisplayName;
    String subject;
    Integer version;
    TemplateStatus status;
    String ownerUserId;
    OffsetDateTime updatedAt;
}
