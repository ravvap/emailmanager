package com.fdic.tip.emailmanager.service;

import com.fdic.tip.emailmanager.dto.NoReplyMailboxDto;

public interface NoReplyMailboxService {

	NoReplyMailboxDto getMailbox();

    NoReplyMailboxDto saveOrUpdateMailbox(NoReplyMailboxDto dto, String username);

    void deleteMailbox(String username);
}