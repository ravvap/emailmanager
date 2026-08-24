package com.fdic.tip.emailmanager.mapper;

import com.fdic.tip.emailmanager.dto.NoReplyMailboxDto;
import com.fdic.tip.emailmanager.entity.NoReplyMailbox;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NoReplyMailboxMapper {

    NoReplyMailboxDto toDto(NoReplyMailbox entity);

    NoReplyMailbox toEntity(NoReplyMailboxDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromDto(NoReplyMailboxDto dto, @MappingTarget NoReplyMailbox entity);
}