package com.fdic.tip.emailmanager.mapper;

import com.fdic.tip.emailmanager.dto.NoReplyMailboxDto;
import com.fdic.tip.emailmanager.entity.NoReplyMailbox;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-24T16:44:11-0400",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Eclipse Adoptium)"
)
@Component
public class NoReplyMailboxMapperImpl implements NoReplyMailboxMapper {

    @Override
    public NoReplyMailboxDto toDto(NoReplyMailbox entity) {
        if ( entity == null ) {
            return null;
        }

        NoReplyMailboxDto noReplyMailboxDto = new NoReplyMailboxDto();

        noReplyMailboxDto.setId( entity.getId() );
        noReplyMailboxDto.setEmailAddress( entity.getEmailAddress() );
        noReplyMailboxDto.setCreatedBy( entity.getCreatedBy() );
        noReplyMailboxDto.setCreatedAt( entity.getCreatedAt() );
        noReplyMailboxDto.setUpdatedBy( entity.getUpdatedBy() );
        noReplyMailboxDto.setUpdatedAt( entity.getUpdatedAt() );

        return noReplyMailboxDto;
    }

    @Override
    public NoReplyMailbox toEntity(NoReplyMailboxDto dto) {
        if ( dto == null ) {
            return null;
        }

        NoReplyMailbox.NoReplyMailboxBuilder noReplyMailbox = NoReplyMailbox.builder();

        noReplyMailbox.id( dto.getId() );
        noReplyMailbox.emailAddress( dto.getEmailAddress() );

        return noReplyMailbox.build();
    }

    @Override
    public void updateEntityFromDto(NoReplyMailboxDto dto, NoReplyMailbox entity) {
        if ( dto == null ) {
            return;
        }

        entity.setEmailAddress( dto.getEmailAddress() );
    }
}
