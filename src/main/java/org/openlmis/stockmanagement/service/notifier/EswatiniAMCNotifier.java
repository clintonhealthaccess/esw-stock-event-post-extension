package org.openlmis.stockmanagement.service.notifier;

import org.apache.commons.lang.text.StrSubstitutor;
import org.openlmis.stockmanagement.dto.referencedata.RightDto;
import org.openlmis.stockmanagement.dto.referencedata.UserDto;
import org.openlmis.stockmanagement.service.EswMessageService;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class EswatiniAMCNotifier {

    private static final XLogger XLOGGER = XLoggerFactory.getXLogger(EswatiniAMCNotifier.class);

    private static final String AMC_EMAIL_ALERT_SUBJECT = "amc.email.alert.subject";
    private static final String AMC_EMAIL_ALERT_BODY = "amc.email.alert.body";
    private static final String AMC_EMAIL_ALERT_LINEITEM_BODY = "amc.email.alert.lineItem.body";

    public static final String REQUISITION_VIEW = "REQUISITION_VIEW";


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

    @Autowired
    private EswMessageService eswMessageService;

    @Value("${time.zoneId}")
    private String timeZoneId;

    @Value("${amc.alert.cron}")
    private String amcAlertCron;

    @Value("${amc.alert.should.override.date}")
    private boolean amcAlertShouldOverrideDate;

    @Value("${amc.alert.override.date}")
    private String amcAlertOverrideDate;

    @PostConstruct
    private void postConstruct() {
        XLOGGER.debug("amc.alert.cron is {}", amcAlertCron);
    }

    @Scheduled(cron = "${amc.alert.cron}", zone = "${time.zoneId}")
    public void cronJob() {
        LocalDate executionDate = LocalDate.now(ZoneId.of(timeZoneId));
        if (amcAlertShouldOverrideDate)
           executionDate = LocalDate.parse(amcAlertOverrideDate);
        XLOGGER.debug("INIT amcAlertCron {}", executionDate);
        sendAMCAlert(executionDate);
    }

    private void sendAMCAlert(LocalDate currentDate) {
        try {
            XLOGGER.debug("INIT sendAMCAlert");
            EswatiniProcessingPeriodDto p0 = getProcessingPeriod(currentDate.minusMonths(1));
            XLOGGER.debug("p0 id: {}", p0.getId());
            EswatiniProcessingPeriodDto p1 = getProcessingPeriod(currentDate.minusMonths(2));
            XLOGGER.debug("p1 id: {}", p1.getId());
            EswatiniProcessingPeriodDto p2 = getProcessingPeriod(currentDate.minusMonths(3));
            XLOGGER.debug("p2 id: {}", p2.getId());
            List<EswatiniProcessingPeriodDto> processingPeriods = Arrays.asList(p0, p1, p2);
            List<EswatiniRequisitionDto> requisitions = requisitionService.searchAndFilter(getSearchParams(p0));
            XLOGGER.debug("req size: {}", requisitions.size());
            for (EswatiniRequisitionDto r : requisitions) {
                XLOGGER.debug("r id: {}", r.getId());
                EswatiniRequisitionDto pastReqMinusOne = getPastRequisition(r, p1);
                EswatiniRequisitionDto pastReqMinusTwo = getPastRequisition(r, p2);
                XLOGGER.debug("Req Id: {}, Req -1 Id: {}, Req -2 Id: {}", r.getId(), pastReqMinusOne.getId(), pastReqMinusTwo.getId());
                compareAndSendAlert(r, pastReqMinusOne, pastReqMinusTwo, processingPeriods);
            }
        } catch(RuntimeException runtimeException) {
            XLOGGER.error("Error sending amc alert", runtimeException);
        }
    }

    private void compareAndSendAlert(EswatiniRequisitionDto r,
                                     EswatiniRequisitionDto pastReqMinusOne,
                                     EswatiniRequisitionDto pastReqMinusTwo, List<EswatiniProcessingPeriodDto> processingPeriods) {
        List<EswatiniRequisitionLineItemDto> lineItemsWithAvgConsumption = r.getRequisitionLineItems().stream()
                .filter(l -> l.getAverageConsumption() != null)
                .collect(Collectors.toList());
        List<List<EswatiniRequisitionLineItemDto>>
                lineItemsWithLowerAMC = new ArrayList<>();
        for(EswatiniRequisitionLineItemDto lineItem : lineItemsWithAvgConsumption) {
            EswatiniRequisitionLineItemDto minusOneLineItem = matchingLineItem(lineItem, pastReqMinusOne);
            EswatiniRequisitionLineItemDto minusTwoLineItem = matchingLineItem(lineItem, pastReqMinusTwo);
            XLOGGER.debug("lineitem {} lineitem-1 {} lineitem-2 {}", lineItem, minusOneLineItem, minusTwoLineItem);
            if (minusOneLineItem != null && minusTwoLineItem != null
                    && lineItem.getAverageConsumption() < minusOneLineItem.getAverageConsumption()
                    && lineItem.getAverageConsumption() < minusTwoLineItem.getAverageConsumption()
            ) {
                lineItemsWithLowerAMC.add(Arrays.asList(lineItem, minusOneLineItem, minusTwoLineItem));
            }
        }

        if(lineItemsWithLowerAMC.size() > 0) {
            RightDto right = rightReferenceDataService.findRight(REQUISITION_VIEW);
            Collection<UserDto> editors = stockNotifierService.getEditors(r.getProgram().getId(),
                    r.getFacility().getId(),
                    right.getId());
            for (UserDto editor : editors) {
                Map<String, String> substitutionMap = constructSubstitutionMap(r, editor, lineItemsWithLowerAMC, processingPeriods);
                StrSubstitutor strSubstitutor = new StrSubstitutor(substitutionMap);
                String subject = strSubstitutor.replace(eswMessageService.getMessage(AMC_EMAIL_ALERT_SUBJECT));
                String body = strSubstitutor.replace(eswMessageService.getMessage(AMC_EMAIL_ALERT_BODY));
                XLOGGER.debug("Sending mail, user: {} subject: {} body: {}",
                        editor.getUsername(),
                        subject,
                        body);
                notificationService.notify(editor, subject, body);
            }
        }

    }

    private Map<String, String> constructSubstitutionMap(EswatiniRequisitionDto requisition,
                                                         UserDto user,
                                                         List<List<EswatiniRequisitionLineItemDto>> lineItemsWithLowerAMC,
                                                         List<EswatiniProcessingPeriodDto> processingPeriods) {
        Map<String, String> valueMap = new HashMap<>();
        valueMap.put("username", user.getUsername());
        valueMap.put("currentProcessingPeriodName", processingPeriods.get(0).getName());
        valueMap.put("programName", stockCardNotifier.getProgramName(requisition.getProgram().getId()));
        valueMap.put("facilityName", stockCardNotifier.getFacilityName(requisition.getFacility().getId()));
        valueMap.put("lineItemsBody", constructLineItemsBody(lineItemsWithLowerAMC, processingPeriods));

        XLOGGER.debug("Values for subject body {}", valueMap);

        return valueMap;
    }

    private String constructLineItemsBody(List<List<EswatiniRequisitionLineItemDto>> lineItemsWithLowerAMC,
                                          List<EswatiniProcessingPeriodDto> processingPeriods) {
        StringBuilder messageBuilder = new StringBuilder();

        for(List<EswatiniRequisitionLineItemDto> items: lineItemsWithLowerAMC) {
            EswatiniRequisitionLineItemDto l0 = items.get(0);
            EswatiniRequisitionLineItemDto l1 = items.get(1);
            EswatiniRequisitionLineItemDto l2 = items.get(2);
            EswatiniProcessingPeriodDto p0 = processingPeriods.get(0);
            EswatiniProcessingPeriodDto p1 = processingPeriods.get(1);
            EswatiniProcessingPeriodDto p2 = processingPeriods.get(2);
            Map<String, Object> valueMap = new HashMap<>();
            valueMap.put("productName", stockCardNotifier.getOrderableName(l0.getOrderable().getId()));
            valueMap.put("currentProcessingPeriodName", p0.getName());
            valueMap.put("minusOneProcessingPeriodName", p1.getName());
            valueMap.put("minusTwoProcessingPeriodName", p2.getName());
            valueMap.put("currentProcessingPeriodAMC", l0.getAverageConsumption());
            valueMap.put("minusOneProcessingPeriodAMC", l1.getAverageConsumption());
            valueMap.put("minusTwoProcessingPeriodAMC", l2.getAverageConsumption());

            XLOGGER.debug("Values for line item body {}", valueMap);

            StrSubstitutor strSubstitutor = new StrSubstitutor(valueMap);
            String lineItemBody = strSubstitutor.replace(eswMessageService.getMessage(AMC_EMAIL_ALERT_LINEITEM_BODY));
            messageBuilder.append(lineItemBody);
        }
        String lineItemsBody = messageBuilder.toString();
        XLOGGER.debug("lineItemsBody is {}", lineItemsBody);
        return lineItemsBody;
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
        List<EswatiniProcessingPeriodDto> processingPeriodDtos = processingPeriodService
                .getPage(RequestParameters.init())
                .toList();
        Optional<EswatiniProcessingPeriodDto> first = processingPeriodDtos.stream()
                .filter(dto -> dto.getDurationInMonths() == 1)
                .filter(dto -> isWithinRange(currentDate, dto.getStartDate(), dto.getEndDate())).findFirst();
        return first.orElseThrow(() -> new RuntimeException("Processing Period not found"));
    }

    boolean isWithinRange(LocalDate testDate, LocalDate startDate, LocalDate endDate) {
        return !(testDate.isBefore(startDate) || testDate.isAfter(endDate));
    }
}

