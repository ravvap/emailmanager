package com.fdic.tip.emailmanager.template.service;

/** Boundary for cleaning pasted rich text to the supported formatting set and deriving plain-text alternatives. */
public interface RichTextSanitizerPort {

    String sanitize(String rawHtml);

    String toPlainText(String sanitizedHtml);
}
