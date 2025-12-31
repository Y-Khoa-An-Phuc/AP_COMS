package com.src.ap.mapper;

import com.src.ap.dto.occupation.OccupationRequest;
import com.src.ap.dto.occupation.OccupationResponse;
import com.src.ap.entity.Occupation;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface OccupationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employees", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Occupation toEntity(OccupationRequest request);

    @Mapping(target = "employeeCount", expression = "java(occupation.getEmployees() != null ? occupation.getEmployees().size() : 0)")
    OccupationResponse toResponse(Occupation occupation);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employees", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(OccupationRequest request, @MappingTarget Occupation occupation);
}