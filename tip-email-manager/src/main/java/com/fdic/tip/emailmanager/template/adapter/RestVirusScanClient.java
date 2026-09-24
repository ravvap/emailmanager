package com.fdic.tip.emailmanager.template.adapter;

import com.fdic.tip.emailmanager.common.constants.AttachmentStorageConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Calls the platform's shared virus-scanning REST endpoint. Assumes a
 * synchronous scan-and-verdict contract: POST the file bytes, read the
 * verdict back off a response header. Reconcile the URL/header/verdict
 * values below against the platform's actual scanning service before
 * wiring this up — they're this adapter's best assumption, not a
 * confirmed contract.
 */
@Component
public class RestVirusScanClient implements VirusScanClient {

    private final RestTemplate restTemplate;
    private final String scanUrl;

    public RestVirusScanClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${" + AttachmentStorageConstants.PROP_VIRUS_SCAN_URL + "}") String scanUrl,
            @Value("${" + AttachmentStorageConstants.PROP_VIRUS_SCAN_TIMEOUT_MS + ":"
                    + AttachmentStorageConstants.DEFAULT_VIRUS_SCAN_TIMEOUT_MS + "}") int timeoutMs) {
        this.scanUrl = scanUrl;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Override
    public VirusScanResult scan(byte[] content, String fileName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("file", fileName);

            ResponseEntity<String> response = restTemplate.exchange(
                    scanUrl, HttpMethod.POST, new HttpEntity<>(content, headers), String.class);

            String verdict = response.getHeaders().getFirst(AttachmentStorageConstants.VIRUS_SCAN_VERDICT_HEADER);
            if (response.getStatusCode().is2xxSuccessful() && "CLEAN".equalsIgnoreCase(verdict)) {
                return new VirusScanResult(VirusScanResult.Verdict.CLEAN, null);
            }
            if ("INFECTED".equalsIgnoreCase(verdict)) {
                return new VirusScanResult(VirusScanResult.Verdict.INFECTED, response.getBody());
            }
            return new VirusScanResult(VirusScanResult.Verdict.FAILED, "Unexpected scan response: " + verdict);
        } catch (RestClientException ex) {
            return new VirusScanResult(VirusScanResult.Verdict.FAILED, ex.getMessage());
        }
    }
}
