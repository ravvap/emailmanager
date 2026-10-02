package com.fdic.tip.emailmanager.template.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/** Response after uploading a recipient file — lets the UI populate the Sheet dropdown next. */
@Value
@Builder
public class RecipientFileUploadResponse {

    String fileName;
    List<String> sheetNames;
}
