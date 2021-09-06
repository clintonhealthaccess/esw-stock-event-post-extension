package org.openlmis.stockmanagement.service;

import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.referencedata.UserDto;
import org.openlmis.stockmanagement.service.referencedata.BaseReferenceDataService;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EswatiniUserService extends BaseReferenceDataService<UserDto> {

    private static final XLogger XLOGGER = XLoggerFactory.getXLogger(
            EswatiniUserService.class);

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

    public String getCurrentUserName(StockEventDto stockEventDto) {
        UUID currentUserId = stockEventDto.getContext().getCurrentUserId();
        UserDto currentUser = super.findOne(currentUserId);
        String currentUserName = currentUser.getUsername();
        XLOGGER.debug("Current User Name: {}", currentUserName);
        return currentUserName;
    }
}
