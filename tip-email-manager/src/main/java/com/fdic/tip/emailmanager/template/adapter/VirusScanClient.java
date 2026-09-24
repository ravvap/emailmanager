package com.fdic.tip.emailmanager.template.adapter;

/**
 * Boundary to the platform's virus-scanning service. Kept separate from
 * AttachmentStoragePortImpl (single responsibility: scanning vs.
 * storage) so either can be swapped — e.g. ICAP/ClamAV today, a
 * different scanner later — without touching the other.
 */
public interface VirusScanClient {

    VirusScanResult scan(byte[] content, String fileName);
}
