package org.openlmis.stockmanagement.service.notifier;

import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.identity.OrderableLotIdentity;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.openlmis.stockmanagement.dto.referencedata.LotDto;
import org.openlmis.stockmanagement.dto.referencedata.RightDto;
import org.openlmis.stockmanagement.i18n.MessageService;
import org.openlmis.stockmanagement.service.referencedata.LotReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.RightReferenceDataService;
import org.openlmis.stockmanagement.util.Message;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.openlmis.stockmanagement.i18n.MessageKeys.NOTIFICATION_STOCKOUT_CONTENT;
import static org.openlmis.stockmanagement.i18n.MessageKeys.NOTIFICATION_STOCKOUT_SUBJECT;
import static org.openlmis.stockmanagement.service.PermissionService.STOCK_INVENTORIES_EDIT;

@Component
public class EswatiniStockoutNotifier {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(
          EswatiniStockoutNotifier.class);

  @Autowired
  private EswatiniStockCardNotifier stockCardNotifier;

  @Autowired
  private RightReferenceDataService rightReferenceDataService;

  @Autowired
  private LotReferenceDataService lotReferenceDataService;

  @Autowired
  private MessageService messageService;

  @Value("${email.urlToInitiateRequisition}")
  private String urlToInitiateRequisition;


  @Async
  public void notify(StockEventDto stockEventDto) {
    RightDto right = rightReferenceDataService.findRight(STOCK_INVENTORIES_EDIT);
    stockEventDto
            .getLineItems()
            .forEach(line -> callNotifications(stockEventDto, line, right.getId()));

  }

  private void callNotifications(StockEventDto event, StockEventLineItemDto eventLine,
                                 UUID rightId) {
    OrderableLotIdentity identity = OrderableLotIdentity.identityOf(eventLine);
    StockCard stockCard = event.getContext().findCard(identity);

    if (stockCard.getStockOnHand() == 0) {
      NotificationMessageParams params = new NotificationMessageParams(
              getMessage(NOTIFICATION_STOCKOUT_SUBJECT),
              getMessage(NOTIFICATION_STOCKOUT_CONTENT),
              constructSubstitutionMap(stockCard));
      stockCardNotifier.notifyStockEditors(stockCard, rightId, params);
    }
  }

  private Map<String, String> constructSubstitutionMap(StockCard stockCard) {
    Map<String, String> valuesMap = new HashMap<>();
    valuesMap.put("facilityName", stockCardNotifier.getFacilityName(stockCard.getFacilityId()));
    valuesMap.put("orderableName", stockCardNotifier.getOrderableName(stockCard.getOrderableId()));
    valuesMap.put("orderableNameLotInformation",
            getOrderableNameLotInformation(valuesMap.get("orderableName"), stockCard.getLotId()));
    valuesMap.put("programName", stockCardNotifier.getProgramName(stockCard.getProgramId()));

    List<StockCardLineItem> lineItems = stockCard.getLineItems();
    LocalDate stockoutDate = lineItems.get(lineItems.size() - 1).getOccurredDate();
    valuesMap.put("stockoutDate", stockCardNotifier.getDateFormatter().format(stockoutDate));
    long numberOfDaysOfStockout = getNumberOfDaysOfStockout(stockoutDate);
    valuesMap.put("numberOfDaysOfStockout", numberOfDaysOfStockout
            + (numberOfDaysOfStockout == 1 ? " day" : " days"));

    valuesMap.put("urlToViewBinCard", stockCardNotifier.getUrlToViewBinCard(stockCard.getId()));
    valuesMap.put("urlToInitiateRequisition", getUrlToInitiateRequisition(stockCard));
    return valuesMap;
  }

  private String getOrderableNameLotInformation(String orderableName, UUID lotId) {
    if (lotId != null) {
      LotDto lot = lotReferenceDataService.findOne(lotId);
      return orderableName + " " + lot.getLotCode();
    }
    return orderableName;
  }

  private long getNumberOfDaysOfStockout(LocalDate stockoutDate) {
    return ChronoUnit.DAYS.between(stockoutDate, LocalDate.now());
  }

  private String getUrlToInitiateRequisition(StockCard stockCard) {
    return MessageFormat.format(urlToInitiateRequisition,
            stockCard.getFacilityId(), stockCard.getProgramId(), "true", "false");
  }

  private String getMessage(String key) {
    return messageService
            .localize(new Message(key))
            .getMessage();
  }
}