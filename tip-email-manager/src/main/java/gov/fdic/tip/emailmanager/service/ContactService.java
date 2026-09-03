package gov.fdic.tip.emailmanager.service;

import gov.fdic.tip.emailmanager.dto.ContactDto;

import java.util.List;

public interface ContactService {

    List<ContactDto> getAllContacts();

    List<ContactDto> searchContactsByAttribute(Long attributeId, String value);

    ContactDto createContact(ContactDto dto, String username);

    ContactDto updateContact(Long id, ContactDto dto, String username);

    void deleteContact(Long id, String username);
}