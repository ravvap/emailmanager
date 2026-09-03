package gov.fdic.tip.emailmanager.service;

import gov.fdic.tip.emailmanager.dto.ApprovedSenderDto;

import java.util.List;

public interface ApprovedSenderService {

    List<ApprovedSenderDto> getAllSenders();

    List<ApprovedSenderDto> getActiveSenders();

    ApprovedSenderDto createSender(ApprovedSenderDto dto, String username);

    ApprovedSenderDto updateSender(Long id, ApprovedSenderDto dto, String username);

    void deleteSender(Long id, String username);
}