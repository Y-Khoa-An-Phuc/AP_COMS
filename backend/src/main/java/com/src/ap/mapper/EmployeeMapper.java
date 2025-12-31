package com.src.ap.mapper;

import com.src.ap.dto.employee.EmployeeRequest;
import com.src.ap.dto.employee.EmployeeResponse;
import com.src.ap.entity.Employee;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "occupation", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Employee toEntity(EmployeeRequest request);

    @Mapping(source = "occupation.id", target = "occupationId")
    @Mapping(source = "occupation.name", target = "occupationName")
    EmployeeResponse toResponse(Employee employee);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "occupation", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(EmployeeRequest request, @MappingTarget Employee employee);
}