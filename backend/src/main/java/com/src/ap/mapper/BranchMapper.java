package com.src.ap.mapper;

import com.src.ap.dto.branch.BranchRequest;
import com.src.ap.dto.branch.BranchResponse;
import com.src.ap.entity.Branch;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface BranchMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employees", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Branch toEntity(BranchRequest request);

    @Mapping(target = "employeeCount", expression = "java(branch.getEmployees() != null ? branch.getEmployees().size() : 0)")
    BranchResponse toResponse(Branch branch);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employees", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(BranchRequest request, @MappingTarget Branch branch);
}