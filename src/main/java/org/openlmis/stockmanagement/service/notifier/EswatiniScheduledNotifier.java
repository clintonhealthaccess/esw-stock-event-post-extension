package org.openlmis.stockmanagement.service.notifier;

import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class EswatiniScheduledNotifier {

    private static final XLogger XLOGGER = XLoggerFactory.getXLogger(EswatiniScheduledNotifier.class);

    @Value("${time.zoneId}")
    private String timeZoneId;

    @Scheduled(cron = "*/10 * * * * *", zone = "${time.zoneId}")
    public void remindToDoPhysicalCounting() {
        XLOGGER.debug("INIT remindToDoPhysicalCounting");
        XLOGGER.debug("Time now: {} Timezone: {}", LocalDate.now(ZoneId.of(timeZoneId)), timeZoneId);
    }
}
