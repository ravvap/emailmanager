package com.fdic.tip.emailmanager.template.adapter;

import com.fdic.tip.emailmanager.common.constants.RecipientDirectoryConstants;
import com.fdic.tip.emailmanager.template.service.DistributionListDirectoryPort;
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
 * Validates selected distribution list ids against the existing
 * distribution-list directory endpoint — the same one the Recipients
 * Mode screen's "Distribution Lists" grid already lists from (EM-1
 * schema: distribution_list, status 'Active'/'Inactive').
 */
@Component
public class RestDistributionListDirectoryPort implements DistributionListDirectoryPort {

    private final RestTemplate restTemplate;
    private final String distributionListsUrl;

    public RestDistributionListDirectoryPort(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${" + RecipientDirectoryConstants.PROP_DISTRIBUTION_LISTS_URL + "}") String distributionListsUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.distributionListsUrl = distributionListsUrl;
    }

    @Override
    public List<Long> findInvalidIds(Collection<Long> distributionListIds) {
        if (distributionListIds == null || distributionListIds.isEmpty()) {
            return List.of();
        }
        Set<Long> activeIds = fetchActiveIds();
        return distributionListIds.stream()
                .filter(id -> !activeIds.contains(id))
                .collect(Collectors.toList());
    }

    private Set<Long> fetchActiveIds() {
        try {
            DirectoryItem[] items = restTemplate.getForObject(distributionListsUrl, DirectoryItem[].class);
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
