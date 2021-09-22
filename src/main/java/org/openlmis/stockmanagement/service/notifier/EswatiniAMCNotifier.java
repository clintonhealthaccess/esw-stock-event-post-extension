package org.openlmis.stockmanagement.service.notifier;

import org.openlmis.stockmanagement.dto.referencedata.RightDto;
import org.openlmis.stockmanagement.dto.referencedata.UserDto;
import org.openlmis.stockmanagement.service.EswatiniProcessingPeriodService;
import org.openlmis.stockmanagement.service.EswatiniRequisitionService;
import org.openlmis.stockmanagement.service.dtos.EswatiniProcessingPeriodDto;
import org.openlmis.stockmanagement.service.dtos.EswatiniRequisitionDto;
import org.openlmis.stockmanagement.service.dtos.EswatiniRequisitionLineItemDto;
import org.openlmis.stockmanagement.service.notification.NotificationService;
import org.openlmis.stockmanagement.service.referencedata.RightReferenceDataService;
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
import java.util.stream.Collectors;

import static org.openlmis.stockmanagement.service.PermissionService.STOCK_INVENTORIES_EDIT;

@Component
public class EswatiniAMCNotifier {

    private static final XLogger XLOGGER = XLoggerFactory.getXLogger(EswatiniAMCNotifier.class);

    @Autowired
    private EswatiniProcessingPeriodService processingPeriodService;

    @Autowired
    private EswatiniRequisitionService requisitionService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EswatiniStockNotifierService stockNotifierService;

    @Autowired
    private RightReferenceDataService rightReferenceDataService;

    @Autowired
    private EswatiniStockCardNotifier stockCardNotifier;

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
                compareAndSendAlert(r, pastReqMinusOne, pastReqMinusTwo);
            }
        } catch(RuntimeException runtimeException) {
            XLOGGER.error("Error sending amc alert", runtimeException);
        }
    }

    private void compareAndSendAlert(EswatiniRequisitionDto r,
                                     EswatiniRequisitionDto pastReqMinusOne,
                                     EswatiniRequisitionDto pastReqMinusTwo) {
        List<EswatiniRequisitionLineItemDto> lineItemsWithAvgConsumption = r.getRequisitionLineItems().stream()
                .filter(l -> l.getAverageConsumption() != null)
                .collect(Collectors.toList());
        for(EswatiniRequisitionLineItemDto lineItem : lineItemsWithAvgConsumption) {
            EswatiniRequisitionLineItemDto pastMinusOneLineItem = matchingLineItem(lineItem, pastReqMinusOne);
            EswatiniRequisitionLineItemDto pastMinusTwoLineItem = matchingLineItem(lineItem, pastReqMinusTwo);
            XLOGGER.debug("lineitem {} lineitem-1 {} lineitem-2 {}", lineItem, pastMinusOneLineItem, pastMinusTwoLineItem);
            if (pastMinusOneLineItem != null && pastMinusTwoLineItem != null) {
                if (lineItem.getAverageConsumption() < pastMinusOneLineItem.getAverageConsumption()
                        && lineItem.getAverageConsumption() < pastMinusTwoLineItem.getAverageConsumption()) {
                    RightDto right = rightReferenceDataService.findRight(STOCK_INVENTORIES_EDIT);
                    Collection<UserDto> editors = stockNotifierService.getEditors(r.getFacility().getId(),
                            r.getFacility().getId(),
                            right.getId());
                    for (UserDto editor : editors) {
                        String subject = String.format("AMC is lower");
                        String body = String.format("AMC is lower for %s", stockCardNotifier.getOrderableName(lineItem.getOrderable().getId()));
                        XLOGGER.debug("Sending mail, user: {} subject: {} body: {}",
                                editor.getUsername(),
                                subject,
                                body);
                        notificationService.notify(editor, subject, body);
                    }
                }
            }
        }

    }

    private EswatiniRequisitionLineItemDto matchingLineItem(EswatiniRequisitionLineItemDto lineItem,
                                                            EswatiniRequisitionDto requisition) {
        return requisition.getRequisitionLineItems().stream()
                .filter(l -> l.getAverageConsumption() != null)
                .filter(l -> l.getOrderable().getId().equals(lineItem.getOrderable().getId()))
                .findFirst()
                .orElse(null);
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
        List<EswatiniProcessingPeriodDto> processingPeriodDtos = page.toList();
        Optional<EswatiniProcessingPeriodDto> first = processingPeriodDtos.stream()
                .filter(dto -> dto.getDurationInMonths() == 1)
                .filter(dto -> isWithinRange(currentDate, dto.getStartDate(), dto.getEndDate())).findFirst();
        return first.orElseThrow(() -> new RuntimeException("Processing Period not found"));
    }

    boolean isWithinRange(LocalDate testDate, LocalDate startDate, LocalDate endDate) {
        return !(testDate.isBefore(startDate) || testDate.isAfter(endDate));
    }
}

