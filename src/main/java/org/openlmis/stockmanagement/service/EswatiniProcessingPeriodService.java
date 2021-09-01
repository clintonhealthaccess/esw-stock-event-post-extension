package org.openlmis.stockmanagement.service;

import org.openlmis.stockmanagement.service.dtos.ProcessingPeriodDto;
import org.openlmis.stockmanagement.service.referencedata.BaseReferenceDataService;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EswatiniProcessingPeriodService extends BaseReferenceDataService<ProcessingPeriodDto> {

    private static final XLogger XLOGGER = XLoggerFactory.getXLogger(
            EswatiniProcessingPeriodService.class);

    @Override
    protected String getUrl() {
        return "/api/processingPeriods/";
    }

    @Override
    protected Class<ProcessingPeriodDto> getResultClass() {
        return ProcessingPeriodDto.class;
    }

    @Override
    protected Class<ProcessingPeriodDto[]> getArrayResultClass() {
        return ProcessingPeriodDto[].class;
    }
}
