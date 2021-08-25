package org.openlmis.stockmanagement.service.notifier;

import org.openlmis.stockmanagement.dto.referencedata.RoleAssignmentDto;
import org.openlmis.stockmanagement.dto.referencedata.UserDto;
import org.openlmis.stockmanagement.service.EswatiniRoleAssignmentService;
import org.openlmis.stockmanagement.service.EswatiniUserService;
import org.openlmis.stockmanagement.service.notification.NotificationService;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;

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

    @Scheduled(cron = "*/10 * * * * *", zone = "${time.zoneId}")
    public void remindToDoPhysicalCounting() {
        XLOGGER.debug("INIT remindToDoPhysicalCounting, Counter: {}", counter);
        try {
            if (counter == 0) {
                XLOGGER.debug("Counter = 0 so sending mail");

                Collection<UserDto> userDtos = userService.findAll();
                for (UserDto user : userDtos) {
                    Collection<RoleAssignmentDto> roleAssignments = roleAssignmentService.getRoleAssignments(user.getId());
                    boolean sendNotification = roleAssignments.stream()
                            .anyMatch(roleAssignmentDto -> REQUIRED_ROLE_NAME.equals(roleAssignmentDto.getRole().getName()));
                    if (sendNotification) {
                        XLOGGER.debug("Sending the reminder mail to {}", user.getUsername());
                        notificationService.notify(user,
                                "Reminder to perform physical count and report",
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
}
