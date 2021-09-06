package org.openlmis.stockmanagement.service.notifier;

import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.dto.referencedata.LotDto;
import org.openlmis.stockmanagement.dto.referencedata.RightDto;
import org.openlmis.stockmanagement.i18n.MessageService;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.service.referencedata.LotReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.RightReferenceDataService;
import org.openlmis.stockmanagement.util.Message;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.openlmis.stockmanagement.i18n.MessageKeys.NOTIFICATION_NEAR_EXPIRY_CONTENT;
import static org.openlmis.stockmanagement.i18n.MessageKeys.NOTIFICATION_NEAR_EXPIRY_SUBJECT;
import static org.openlmis.stockmanagement.service.PermissionService.STOCK_INVENTORIES_EDIT;

@Component
public class EswatiniNearExpiryNotifier {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(EswatiniNearExpiryNotifier.class);

  @Autowired
  LotReferenceDataService lotReferenceDataService;

  @Autowired
  RightReferenceDataService rightReferenceDataService;

  @Autowired
  StockCardRepository stockCardRepository;

  @Autowired
  EswatiniStockCardNotifier stockCardNotifier;

  @Autowired
  private MessageService messageService;

  @Value("${time.zoneId}")
  private String timeZoneId;

  private Map<UUID, LotDto> expiringLotMap;

  private LocalDate expirationDate;

  /**
   * Check stock cards with lots that have a certain expiration date. If any are found, notify stock
   * card owners.
   */
  @Scheduled(cron = "${stockmanagement.nearExpiry.cron}", zone = "${time.zoneId}")
  public void checkNearExpiryAndNotify() {
    expirationDate = LocalDate.now(ZoneId.of(timeZoneId)).plusMonths(6);
    XLOGGER.debug("Expiration date = {}", expirationDate);
    expiringLotMap = lotReferenceDataService.getAllLotsExpiringOn(expirationDate)
            .stream()
            .collect(Collectors.toMap(LotDto::getId, Function.identity()));
    Collection<UUID> expiringLotIds = expiringLotMap.keySet();
    XLOGGER.debug("Expiring Lot IDs = {}", expiringLotIds);

    List<StockCard> expiringStockCards = stockCardRepository.findByLotIdIn(expiringLotIds);
    XLOGGER.debug("Expiring Stock Card IDs = {}", expiringStockCards.stream()
            .map(StockCard::getId)
            .collect(Collectors.toList()));

    RightDto right = rightReferenceDataService.findRight(STOCK_INVENTORIES_EDIT);
    UUID rightId = right.getId();
    expiringStockCards.forEach(card -> {
      NotificationMessageParams params = new NotificationMessageParams(
              getMessage(NOTIFICATION_NEAR_EXPIRY_SUBJECT),
              getMessage(NOTIFICATION_NEAR_EXPIRY_CONTENT),
              constructSubstitutionMap(card));
      stockCardNotifier.notifyStockEditors(card, rightId, params);
    });
  }

  Map<String, String> constructSubstitutionMap(StockCard stockCard) {
    Map<String, String> valuesMap = new HashMap<>();
    valuesMap.put("facilityName", stockCardNotifier.getFacilityName(stockCard.getFacilityId()));
    valuesMap.put("programName", stockCardNotifier.getProgramName(stockCard.getProgramId()));
    valuesMap.put("orderableName", stockCardNotifier.getOrderableName(stockCard.getOrderableId()));
    LotDto lot = expiringLotMap.get(stockCard.getLotId());
    valuesMap.put("lotCode", null != lot ? lot.getLotCode() : "");
    valuesMap.put("expirationDate", stockCardNotifier.getDateFormatter().format(expirationDate));
    valuesMap.put("urlToViewBinCard", stockCardNotifier.getUrlToViewBinCard(stockCard.getId()));
    return valuesMap;
  }

  private String getMessage(String key) {
    return messageService
            .localize(new Message(key))
            .getMessage();
  }
}