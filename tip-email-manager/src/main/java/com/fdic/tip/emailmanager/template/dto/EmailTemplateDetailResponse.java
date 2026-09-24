package com.fdic.tip.emailmanager.template.dto;

import com.fdic.tip.emailmanager.template.enums.RecipientMode;
import com.fdic.tip.emailmanager.template.enums.TemplateStatus;
import com.fdic.tip.emailmanager.template.enums.VersionStatus;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/** Full Preview-and-Submit shape. */
@Value
@Builder
public class EmailTemplateDetailResponse {

    Long templateId;
    String templateName;
    TemplateStatus templateStatus;

    Long templateVersionId;
    Integer versionNumber;
    VersionStatus versionStatus;

    String fromDisplayName;
    Long dataSourceQueryId;
    Integer dataSourceQueryVersion;
    Boolean hasMergeFieldConflict;
    Integer newerQueryVersionAvailable;

    String subject;
    String bodyHtml;

    RecipientMode recipientMode;
    String recipientEmailColumn;
    String recipientNameColumn;

    List<String> attachmentFileNames;
    List<String> selectedRecipientNames;
}
