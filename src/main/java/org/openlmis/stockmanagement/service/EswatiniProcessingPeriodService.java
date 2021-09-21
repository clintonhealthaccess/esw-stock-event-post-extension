package org.openlmis.stockmanagement.service;

import org.openlmis.stockmanagement.service.dtos.EswatiniProcessingPeriodDto;
import org.openlmis.stockmanagement.service.referencedata.BaseReferenceDataService;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EswatiniProcessingPeriodService extends BaseReferenceDataService<EswatiniProcessingPeriodDto> {

    private static final XLogger XLOGGER = XLoggerFactory.getXLogger(
            EswatiniProcessingPeriodService.class);

    @Override
    protected String getUrl() {
        return "/api/processingPeriods/";
    }

    @Override
    protected Class<EswatiniProcessingPeriodDto> getResultClass() {
        return EswatiniProcessingPeriodDto.class;
    }

    @Override
    protected Class<EswatiniProcessingPeriodDto[]> getArrayResultClass() {
        return EswatiniProcessingPeriodDto[].class;
    }
}
