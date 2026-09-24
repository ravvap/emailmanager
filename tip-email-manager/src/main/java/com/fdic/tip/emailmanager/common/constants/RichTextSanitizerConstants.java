package com.fdic.tip.emailmanager.common.constants;

/** Formatting-boundary constants for the WYSIWYG body sanitizer (EM-8 AC: "fixed, curated set of formatting controls"). */
public final class RichTextSanitizerConstants {

    private RichTextSanitizerConstants() {
    }

    // Protocols allowed in <a href> and <img src> after sanitization.
    public static final String[] ALLOWED_LINK_PROTOCOLS = {"http", "https", "mailto"};

    // Plain-text conversion conventions (EM-8 AC)
    public static final String PLAIN_TEXT_LIST_BULLET = "- ";
    public static final String PLAIN_TEXT_IMAGE_PLACEHOLDER_PREFIX = "[image: ";
    public static final String PLAIN_TEXT_IMAGE_PLACEHOLDER_SUFFIX = "]";
    public static final String PLAIN_TEXT_UNNAMED_IMAGE = "attachment";
}
