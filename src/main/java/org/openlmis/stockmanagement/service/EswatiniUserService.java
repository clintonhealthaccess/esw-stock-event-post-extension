package org.openlmis.stockmanagement.service;

import org.openlmis.stockmanagement.dto.referencedata.UserDto;
import org.openlmis.stockmanagement.service.referencedata.BaseReferenceDataService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
public class EswatiniUserService extends BaseReferenceDataService<UserDto> {

    @Override
    protected String getUrl() {
        return "/api/users/";
    }

    @Override
    protected Class<UserDto> getResultClass() {
        return UserDto.class;
    }

    @Override
    protected Class<UserDto[]> getArrayResultClass() {
        return UserDto[].class;
    }

    public Collection<UserDto> findAll() {
        return super.findAll("", new HashMap<>());
    }
}
