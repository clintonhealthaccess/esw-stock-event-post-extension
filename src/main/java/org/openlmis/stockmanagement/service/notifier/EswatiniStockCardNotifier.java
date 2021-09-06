package org.openlmis.stockmanagement.service.notifier;

import org.apache.commons.lang.text.StrSubstitutor;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.dto.referencedata.UserDto;
import org.openlmis.stockmanagement.service.notification.NotificationService;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Component
public class EswatiniStockCardNotifier extends BaseNotifier {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(EswatiniStockCardNotifier.class);

  @Autowired
  private EswatiniStockNotifierService eswatiniNotifierService;

  @Autowired
  private NotificationService notificationService;

  @Async
  public void notifyStockEditors(StockCard stockCard, UUID rightId,
                                 NotificationMessageParams params) {
    Collection<UserDto> recipients = eswatiniNotifierService.getEditors(stockCard.getProgramId(),
            stockCard.getFacilityId(),
            rightId);

    Map<String, String> valuesMap = params.getSubstitutionMap();
    StrSubstitutor sub = new StrSubstitutor(valuesMap);

    for (UserDto recipient : recipients) {
      if (stockCard.getFacilityId().equals(recipient.getHomeFacilityId())) {
        valuesMap.put("username", recipient.getUsername());
        XLOGGER.debug("Recipient username = {}", recipient.getUsername());
        notificationService.notify(recipient,
                sub.replace(params.getMessageSubject()), sub.replace(params.getMessageContent()));
      }
    }
  }
}
