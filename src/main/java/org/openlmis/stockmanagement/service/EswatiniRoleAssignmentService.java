package org.openlmis.stockmanagement.service;

import org.openlmis.stockmanagement.dto.referencedata.RoleAssignmentDto;
import org.openlmis.stockmanagement.service.referencedata.BaseReferenceDataService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

@Service
public class EswatiniRoleAssignmentService extends BaseReferenceDataService<RoleAssignmentDto> {

    @Override
    protected String getUrl() {
        return "/api/users/";
    }

    @Override
    protected Class<RoleAssignmentDto> getResultClass() {
        return RoleAssignmentDto.class;
    }

    @Override
    protected Class<RoleAssignmentDto[]> getArrayResultClass() {
        return RoleAssignmentDto[].class;
    }

    public Collection<RoleAssignmentDto> getRoleAssignments (UUID userId) {
        return super.findAll(String.format("%s/roleAssignments", userId.toString()), new HashMap<>());
    }
}
