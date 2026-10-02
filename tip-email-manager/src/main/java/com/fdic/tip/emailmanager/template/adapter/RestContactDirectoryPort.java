package com.fdic.tip.emailmanager.template.adapter;

import com.fdic.tip.emailmanager.common.constants.RecipientDirectoryConstants;
import com.fdic.tip.emailmanager.template.service.ContactDirectoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates selected contact ids against the existing contacts
 * directory endpoint — the same one the Recipients Mode screen's
 * "Contacts" grid already lists from (EM-1 schema: contact, status
 * 'ACTIVE'/'INACTIVE').
 */
@Component
public class RestContactDirectoryPort implements ContactDirectoryPort {

    private final RestTemplate restTemplate;
    private final String contactsUrl;

    public RestContactDirectoryPort(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${" + RecipientDirectoryConstants.PROP_CONTACTS_URL + "}") String contactsUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.contactsUrl = contactsUrl;
    }

    @Override
    public List<Long> findInvalidIds(Collection<Long> contactIds) {
        if (contactIds == null || contactIds.isEmpty()) {
            return List.of();
        }
        Set<Long> activeIds = fetchActiveIds();
        return contactIds.stream()
                .filter(id -> !activeIds.contains(id))
                .collect(Collectors.toList());
    }

    private Set<Long> fetchActiveIds() {
        try {
            DirectoryItem[] items = restTemplate.getForObject(contactsUrl, DirectoryItem[].class);
            if (items == null) {
                return Set.of();
            }
            return Arrays.stream(items)
                    .filter(DirectoryItem::isActive)
                    .map(DirectoryItem::id)
                    .collect(Collectors.toSet());
        } catch (RestClientException ex) {
            throw new IllegalStateException(RecipientDirectoryConstants.MSG_DIRECTORY_FETCH_FAILED, ex);
        }
    }
}
