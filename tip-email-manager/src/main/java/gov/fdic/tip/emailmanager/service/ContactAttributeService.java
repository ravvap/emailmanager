package gov.fdic.tip.emailmanager.service;

import gov.fdic.tip.emailmanager.dto.ContactAttributeDto;

import java.util.List;

public interface ContactAttributeService {

    List<ContactAttributeDto> getAllAttributes();

    List<ContactAttributeDto> getActiveAttributes();

    ContactAttributeDto createAttribute(ContactAttributeDto dto, String username);

    ContactAttributeDto updateAttribute(Long id, ContactAttributeDto dto, String username);

    void deleteAttribute(Long id, String username);
}