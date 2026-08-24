package com.fdic.tip.emailmanager.mapper;

import com.fdic.tip.emailmanager.dto.DomainAllowlistDto;
import com.fdic.tip.emailmanager.entity.InternalDomainAllowlist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct Mapper interface for InternalDomainAllowlist conversions.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InternalDomainAllowlistMapper {

    /**
     * Converts an InternalDomainAllowlist entity to a DomainAllowlistDto.
     *
     * @param entity Source InternalDomainAllowlist entity.
     * @return Transformed DomainAllowlistDto.
     */
    DomainAllowlistDto toDto(InternalDomainAllowlist entity);

    /**
     * Converts a DomainAllowlistDto to an InternalDomainAllowlist entity.
     *
     * @param dto Source DomainAllowlistDto.
     * @return Transformed InternalDomainAllowlist entity.
     */
    InternalDomainAllowlist toEntity(DomainAllowlistDto dto);

    /**
     * Updates an existing InternalDomainAllowlist entity in place using values from DomainAllowlistDto.
     * Audit attributes are excluded from the update.
     *
     * @param dto Source DomainAllowlistDto.
     * @param entity Target InternalDomainAllowlist entity to update.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromDto(DomainAllowlistDto dto, @MappingTarget InternalDomainAllowlist entity);
}