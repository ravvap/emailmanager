package com.fdic.tip.emailmanager.template.enums;

/** How recipients are determined for a template version (EM-8 AC). */
public enum RecipientMode {
    CONTACT_DISTRIBUTION_LIST,
    DATA_SOURCE_QUERY,
    FILE_UPLOAD,
    DEFINE_AT_SEND
}
