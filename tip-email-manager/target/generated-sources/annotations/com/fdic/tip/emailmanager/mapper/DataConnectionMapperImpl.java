package com.fdic.tip.emailmanager.mapper;

import com.fdic.tip.emailmanager.dto.DataConnectionDto;
import com.fdic.tip.emailmanager.entity.DataConnection;
import com.fdic.tip.emailmanager.entity.DatabaseLocation;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-24T16:44:11-0400",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Eclipse Adoptium)"
)
@Component
public class DataConnectionMapperImpl implements DataConnectionMapper {

    @Override
    public DataConnectionDto toDto(DataConnection entity) {
        if ( entity == null ) {
            return null;
        }

        DataConnectionDto dataConnectionDto = new DataConnectionDto();

        dataConnectionDto.setDatabaseLocationId( entityDatabaseLocationId( entity ) );
        dataConnectionDto.setId( entity.getId() );
        dataConnectionDto.setName( entity.getName() );
        dataConnectionDto.setDescription( entity.getDescription() );
        dataConnectionDto.setStatus( entity.getStatus() );
        dataConnectionDto.setUsedInTemplate( entity.isUsedInTemplate() );
        Set<Long> set = entity.getAuthorUserIds();
        if ( set != null ) {
            dataConnectionDto.setAuthorUserIds( new LinkedHashSet<Long>( set ) );
        }
        dataConnectionDto.setCreatedBy( entity.getCreatedBy() );
        dataConnectionDto.setCreatedAt( entity.getCreatedAt() );
        dataConnectionDto.setUpdatedBy( entity.getUpdatedBy() );
        dataConnectionDto.setUpdatedAt( entity.getUpdatedAt() );

        return dataConnectionDto;
    }

    @Override
    public DataConnection toEntity(DataConnectionDto dto) {
        if ( dto == null ) {
            return null;
        }

        DataConnection.DataConnectionBuilder dataConnection = DataConnection.builder();

        dataConnection.id( dto.getId() );
        dataConnection.name( dto.getName() );
        dataConnection.description( dto.getDescription() );
        dataConnection.status( dto.getStatus() );
        Set<Long> set = dto.getAuthorUserIds();
        if ( set != null ) {
            dataConnection.authorUserIds( new LinkedHashSet<Long>( set ) );
        }
        dataConnection.createdBy( dto.getCreatedBy() );
        dataConnection.createdAt( dto.getCreatedAt() );
        dataConnection.updatedBy( dto.getUpdatedBy() );
        dataConnection.updatedAt( dto.getUpdatedAt() );

        return dataConnection.build();
    }

    @Override
    public void updateEntityFromDto(DataConnectionDto dto, DataConnection entity) {
        if ( dto == null ) {
            return;
        }

        entity.setName( dto.getName() );
        entity.setDescription( dto.getDescription() );
        entity.setStatus( dto.getStatus() );
        entity.setUsedInTemplate( dto.isUsedInTemplate() );
        if ( entity.getAuthorUserIds() != null ) {
            Set<Long> set = dto.getAuthorUserIds();
            if ( set != null ) {
                entity.getAuthorUserIds().clear();
                entity.getAuthorUserIds().addAll( set );
            }
            else {
                entity.setAuthorUserIds( null );
            }
        }
        else {
            Set<Long> set = dto.getAuthorUserIds();
            if ( set != null ) {
                entity.setAuthorUserIds( new LinkedHashSet<Long>( set ) );
            }
        }
    }

    private Long entityDatabaseLocationId(DataConnection dataConnection) {
        if ( dataConnection == null ) {
            return null;
        }
        DatabaseLocation databaseLocation = dataConnection.getDatabaseLocation();
        if ( databaseLocation == null ) {
            return null;
        }
        Long id = databaseLocation.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
