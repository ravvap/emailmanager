package com.fdic.tip.emailmanager.mapper;

import com.fdic.tip.emailmanager.dto.DomainAllowlistDto;
import com.fdic.tip.emailmanager.entity.InternalDomainAllowlist;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-24T16:44:11-0400",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Eclipse Adoptium)"
)
@Component
public class InternalDomainAllowlistMapperImpl implements InternalDomainAllowlistMapper {

    @Override
    public DomainAllowlistDto toDto(InternalDomainAllowlist entity) {
        if ( entity == null ) {
            return null;
        }

        DomainAllowlistDto domainAllowlistDto = new DomainAllowlistDto();

        domainAllowlistDto.setId( entity.getId() );
        domainAllowlistDto.setDomain( entity.getDomain() );
        domainAllowlistDto.setStatus( entity.getStatus() );
        domainAllowlistDto.setCreatedBy( entity.getCreatedBy() );
        domainAllowlistDto.setCreatedAt( entity.getCreatedAt() );
        domainAllowlistDto.setUpdatedBy( entity.getUpdatedBy() );
        domainAllowlistDto.setUpdatedAt( entity.getUpdatedAt() );

        return domainAllowlistDto;
    }

    @Override
    public InternalDomainAllowlist toEntity(DomainAllowlistDto dto) {
        if ( dto == null ) {
            return null;
        }

        InternalDomainAllowlist.InternalDomainAllowlistBuilder internalDomainAllowlist = InternalDomainAllowlist.builder();

        internalDomainAllowlist.id( dto.getId() );
        internalDomainAllowlist.domain( dto.getDomain() );
        internalDomainAllowlist.status( dto.getStatus() );

        return internalDomainAllowlist.build();
    }

    @Override
    public void updateEntityFromDto(DomainAllowlistDto dto, InternalDomainAllowlist entity) {
        if ( dto == null ) {
            return;
        }

        entity.setDomain( dto.getDomain() );
        entity.setStatus( dto.getStatus() );
    }
}
