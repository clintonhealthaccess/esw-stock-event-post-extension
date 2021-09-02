package org.openlmis.stockmanagement.service.notifier;

import org.openlmis.stockmanagement.dto.referencedata.RoleAssignmentDto;
import org.openlmis.stockmanagement.dto.referencedata.UserDto;
import org.openlmis.stockmanagement.service.EswatiniProcessingPeriodService;
import org.openlmis.stockmanagement.service.EswatiniRoleAssignmentService;
import org.openlmis.stockmanagement.service.EswatiniUserService;
import org.openlmis.stockmanagement.service.dtos.ProcessingPeriodDto;
import org.openlmis.stockmanagement.service.notification.NotificationService;
import org.openlmis.stockmanagement.util.RequestParameters;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

@Component
public class EswatiniScheduledNotifier {

    private static final XLogger XLOGGER = XLoggerFactory.getXLogger(EswatiniScheduledNotifier.class);
    private static final String REQUIRED_ROLE_NAME = "Stock Manager";

    @Value("${time.zoneId}")
    private String timeZoneId;

    @Autowired
    private EswatiniUserService userService;

    @Autowired
    private EswatiniRoleAssignmentService roleAssignmentService;

    int counter = 0;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EswatiniProcessingPeriodService processingPeriodService;

    @Scheduled(cron = "*/10 * * * * *", zone = "${time.zoneId}")
    public void remindToDoPhysicalCounting() {
        try {
            if (counter <= 5) {
                XLOGGER.debug("remindToDoPhysicalCounting, Counter: {}", counter);

                Page<UserDto> userDtos = userService.getPage(RequestParameters.init());
                for (UserDto user : userDtos) {
                    Collection<RoleAssignmentDto> roleAssignments = roleAssignmentService.getRoleAssignments(user.getId());
                    boolean roleMatches = roleAssignments.stream()
                            .anyMatch(roleAssignmentDto -> REQUIRED_ROLE_NAME.equals(roleAssignmentDto.getRole().getName()));
                    if (roleMatches) {
                        ProcessingPeriodDto processingPeriod = getProcessingPeriod(LocalDate.now());
                        XLOGGER.debug("Sending the reminder mail to {}, ProcessingPeriod {}", user.getUsername(), processingPeriod);
                        notificationService.notify(user,
                                "Reminder to perform physical count and report for " + processingPeriod.getDescription(),
                                "Please perform physical count and report");
                    } else {
                        XLOGGER.debug("Not sending the reminder mail to {} because they does not have the role {}", user.getUsername(), REQUIRED_ROLE_NAME);
                    }
                }

            }
        } finally {
            counter++;
        }

    }

    public ProcessingPeriodDto getProcessingPeriod(LocalDate currentDate) {
        Page<ProcessingPeriodDto> page = processingPeriodService.getPage(RequestParameters.init());
        Optional<ProcessingPeriodDto> first = page.stream().filter(dto -> isWithinRange(currentDate, dto.getStartDate(), dto.getEndDate())).findFirst();
        ProcessingPeriodDto periodDto = first.orElseThrow(() -> new RuntimeException("Processing Period not found"));
        return periodDto;
    }

    boolean isWithinRange(LocalDate testDate, LocalDate startDate, LocalDate endDate) {
        return !(testDate.isBefore(startDate) || testDate.isAfter(endDate));
    }
}
