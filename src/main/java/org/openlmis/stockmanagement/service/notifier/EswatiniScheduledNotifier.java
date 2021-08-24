package org.openlmis.stockmanagement.service.notifier;

import org.apache.commons.collections4.map.HashedMap;
import org.openlmis.stockmanagement.dto.referencedata.RightDto;
import org.openlmis.stockmanagement.dto.referencedata.UserDto;
import org.openlmis.stockmanagement.service.EswatiniUserService;
import org.openlmis.stockmanagement.service.notification.NotificationService;
import org.openlmis.stockmanagement.service.referencedata.RightReferenceDataService;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.UUID;

import static org.openlmis.stockmanagement.service.PermissionService.STOCK_INVENTORIES_EDIT;

@Component
public class EswatiniScheduledNotifier {

    private static final XLogger XLOGGER = XLoggerFactory.getXLogger(EswatiniScheduledNotifier.class);

    @Value("${time.zoneId}")
    private String timeZoneId;

    @Autowired
    private EswatiniUserService eswatiniUserService;

    int counter = 0;

    @Autowired
    private RightReferenceDataService rightReferenceDataService;

    @Autowired
    private NotificationService notificationService;

    @Scheduled(cron = "*/10 * * * * *", zone = "${time.zoneId}")
    public void remindToDoPhysicalCounting() {
        XLOGGER.debug("INIT remindToDoPhysicalCounting, Counter: {}", counter);

        if(counter == 0) {
            XLOGGER.debug("Counter = 0 so sending mail");
            RightDto rightDto = rightReferenceDataService.findRight(STOCK_INVENTORIES_EDIT);
            UUID rightId = rightDto.getId();
            XLOGGER.debug("Right ID: {}", rightId);
            HashedMap<String, Object> parameters = new HashedMap<>();
            parameters.put("rightId", rightId);
            Collection<UserDto> userDtos = eswatiniUserService.rightSearch(parameters);
            for (UserDto user : userDtos) {
                XLOGGER.debug("Sending reminder mail to {}", user.getEmail());
                notificationService.notify(user, "Reminder to perform physical count and report", "Please perform physical count and report");
            }
        }
        counter++;
    }
}
