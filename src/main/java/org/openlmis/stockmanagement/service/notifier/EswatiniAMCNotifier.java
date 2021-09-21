package org.openlmis.stockmanagement.service.notifier;

import org.openlmis.stockmanagement.service.EswatiniProcessingPeriodService;
import org.openlmis.stockmanagement.service.EswatiniRequisitionService;
import org.openlmis.stockmanagement.service.dtos.EswatiniProcessingPeriodDto;
import org.openlmis.stockmanagement.service.dtos.EswatiniRequisitionDto;
import org.openlmis.stockmanagement.util.RequestParameters;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Component
public class EswatiniAMCNotifier {

    private static final XLogger XLOGGER = XLoggerFactory.getXLogger(EswatiniAMCNotifier.class);

    @Autowired
    private EswatiniProcessingPeriodService processingPeriodService;

    @Autowired
    private EswatiniRequisitionService requisitionService;

    @Value("${time.zoneId}")
    private String timeZoneId;

    @Value("${amc.alert.cron}")
    private String amcAlertCron;

    @PostConstruct
    private void postConstruct() {
        XLOGGER.debug("amc.alert.cron is {}", amcAlertCron);
    }

    @Scheduled(cron = "${amc.alert.cron}", zone = "${time.zoneId}")
    public void cronJob() {
        XLOGGER.debug("INIT amcAlertCron");
        LocalDate currentDate = LocalDate.now(ZoneId.of(timeZoneId));
        LocalDate d = LocalDate.of(2017, 5, 1);
        sendAMCAlert(d);
    }

    private void sendAMCAlert(LocalDate currentDate) {
        try {
            XLOGGER.debug("INIT sendAMCAlert");
            EswatiniProcessingPeriodDto processingPeriodLastMonth = getProcessingPeriod(currentDate.minusMonths(1));
            XLOGGER.debug("p1 id: {}", processingPeriodLastMonth.getId());
            EswatiniProcessingPeriodDto processingPeriodLastMinusOneMonth = getProcessingPeriod(currentDate.minusMonths(2));
            XLOGGER.debug("p2 id: {}", processingPeriodLastMinusOneMonth.getId());
            EswatiniProcessingPeriodDto processingPeriodLastMinusTwoMonths = getProcessingPeriod(currentDate.minusMonths(3));
            XLOGGER.debug("p3 id: {}", processingPeriodLastMinusTwoMonths.getId());
            List<EswatiniRequisitionDto> requisitions = requisitionService.searchAndFilter(getSearchParams(processingPeriodLastMonth));
            XLOGGER.debug("req size: {}", requisitions.size());
            for (EswatiniRequisitionDto r : requisitions) {
                XLOGGER.debug("r id: {}", r.getId());
                EswatiniRequisitionDto pastReqMinusOne = getPastRequisition(r, processingPeriodLastMinusOneMonth);
                EswatiniRequisitionDto pastReqMinusTwo = getPastRequisition(r, processingPeriodLastMinusTwoMonths);
                XLOGGER.debug("Req Id: {}, Req -1 Id: {}, Req -2 Id: {}", r.getId(), pastReqMinusOne.getId(), pastReqMinusTwo.getId());
            }
        } catch(RuntimeException runtimeException) {
            XLOGGER.debug("Error sending amc alert", runtimeException.getCause());
        }
    }

    private RequestParameters getSearchParams(EswatiniProcessingPeriodDto processingPeriod) {
        Map<String, Object> params = new HashMap<>();
        params.put("processingPeriod", processingPeriod.getId());
        return RequestParameters.of(params);
    }

    private RequestParameters getSearchParams(EswatiniProcessingPeriodDto processingPeriod, UUID programId, UUID facilityId) {
        Map<String, Object> params = new HashMap<>();
        params.put("processingPeriod", processingPeriod.getId());
        params.put("program", programId);
        params.put("facility", facilityId);
        return RequestParameters.of(params);
    }

    EswatiniRequisitionDto getPastRequisition(EswatiniRequisitionDto requisition, EswatiniProcessingPeriodDto pastProcessingPeriod) {
        RequestParameters searchParams = getSearchParams(pastProcessingPeriod,
                requisition.getProgram().getId(),
                requisition.getFacility().getId());
        return requisitionService
                .searchAndFilter(searchParams)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("past requisition not found"));
    }

    EswatiniProcessingPeriodDto getProcessingPeriod(LocalDate currentDate) {
        Page<EswatiniProcessingPeriodDto> page = processingPeriodService.getPage(RequestParameters.init());
        Optional<EswatiniProcessingPeriodDto> first = page.stream().filter(dto -> isWithinRange(currentDate, dto.getStartDate(), dto.getEndDate())).findFirst();
        return first.orElseThrow(() -> new RuntimeException("Processing Period not found"));
    }

    boolean isWithinRange(LocalDate testDate, LocalDate startDate, LocalDate endDate) {
        return !(testDate.isBefore(startDate) || testDate.isAfter(endDate));
    }
}

