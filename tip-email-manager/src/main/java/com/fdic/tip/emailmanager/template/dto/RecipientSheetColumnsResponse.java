package com.fdic.tip.emailmanager.template.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/** Response after selecting a sheet — lets the UI populate the Recipient Name / Recipient Email mapping dropdowns. */
@Value
@Builder
public class RecipientSheetColumnsResponse {

    String sheetName;
    List<String> columns;
}
