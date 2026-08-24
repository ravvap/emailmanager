package com.fdic.tip.emailmanager.mapper;

 import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.fdic.tip.emailmanager.dto.DataConnectionDto;
import com.fdic.tip.emailmanager.entity.DataConnection;

@Mapper(componentModel = "spring")
public interface DataConnectionMapper {

    // Maps entity.databaseLocation.id -> dto.databaseLocationId
    @Mapping(source = "databaseLocation.id", target = "databaseLocationId")
    DataConnectionDto toDto(DataConnection entity);

    // Entity mapping ignores databaseLocation reference (handled manually in service via repository)
    @Mapping(target = "databaseLocation", ignore = true)
    DataConnection toEntity(DataConnectionDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "databaseLocation", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromDto(DataConnectionDto dto, @MappingTarget DataConnection entity);
}