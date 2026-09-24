package com.fdic.tip.emailmanager.template.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * EM-11 side-by-side comparison. Both full snapshots are returned so the
 * UI can render a rendered (not raw-code) diff; changedFields is a
 * lightweight hint of which top-level attributes differ.
 */
@Value
@Builder
public class VersionCompareResponse {

    EmailTemplateDetailResponse versionA;
    EmailTemplateDetailResponse versionB;
    List<String> changedFields;
}
