package com.fdic.tip.emailmanager.template.service;

/** One parsed data row from the mapped sheet — raw values for the name/email columns, before persistence or validation. */
public record RecipientFileRow(int rowNumber, String recipientName, String recipientEmail) {
}
