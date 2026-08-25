package com.fdic.tip.emailmanager.service;

import java.util.List;

import com.fdic.tip.emailmanager.dto.ConnectionTestResultDto;
import com.fdic.tip.emailmanager.dto.DataConnectionDto;
import com.fdic.tip.emailmanager.dto.TestConnectionRequest;

public interface DataConnectionService {
    DataConnectionDto createConnection(DataConnectionDto dto, String username);
    DataConnectionDto updateConnection(Long id, DataConnectionDto dto, String username);
    List<DataConnectionDto> getAllConnections();
    List<DataConnectionDto> getActiveConnectionsForAuthor(String username);
    void deleteConnection(Long id, String username);
  //  boolean testConnection(TestConnectionRequest request);
	/**
	 * {@inheritDoc}
	 */
	List<DataConnectionDto> getActiveConnectionsForAuthor(Long userId);
	
    void deleteConnection(Long id, String username);
	ConnectionTestResultDto testConnection(DataConnectionDto dto);
	
	List<AuthorDropdownDto> getEligibleAuthors();
}