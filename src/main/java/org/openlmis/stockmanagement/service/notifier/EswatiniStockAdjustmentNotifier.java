package org.openlmis.stockmanagement.service.notifier;

import com.google.common.collect.Iterables;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.openlmis.stockmanagement.service.CalculatedStockOnHandService;
import org.openlmis.stockmanagement.util.AuthenticationHelper;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class EswatiniStockAdjustmentNotifier {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(
          EswatiniStockAdjustmentNotifier.class);

  @Autowired
  private StockCardNotifier stockCardNotifier;

  @Autowired
  private AuthenticationHelper authenticationHelper;

  @Autowired
  private CalculatedStockOnHandService calculatedStockOnHandService;

  @Autowired
  private EswatiniNotifierService eswatiniNotifierService;

  public void notify(StockEventDto stockEventDto) {
    String message = buildMessage(stockEventDto);
    eswatiniNotifierService.sendMessage(stockEventDto, message);
  }

  private String buildMessage(StockEventDto stockEventDto) {
    String currentUserName = authenticationHelper.getCurrentUser().getUsername();
    String initialBody = String.format("User %s has made following stock adjustments: \n", currentUserName);
    StringBuilder messageBuilder = new StringBuilder(initialBody);
    for (StockEventLineItemDto item : stockEventDto.getLineItems()) {
      UUID orderableId = item.getOrderableId();
      String orderableName = stockCardNotifier.getOrderableName(orderableId);

      List<StockCard> cards = calculatedStockOnHandService.getStockCardsWithStockOnHandByOrderableIds(stockEventDto.getProgramId(),
              stockEventDto.getFacilityId(),
              Arrays.asList(orderableId));
      for (StockCard stockCard : cards) {
        StockCardLineItem latestLineItem = Iterables.getLast(stockCard.getLineItems());
        Integer quantityWithSign = latestLineItem.getQuantityWithSign();
        Integer stockOnHand = latestLineItem.getStockOnHand();

        messageBuilder.append(String.format("%s, Stock on Hand: %d, Current Stock: %d, Reason: %s\n",
                        orderableName,
                        stockOnHand + (-quantityWithSign),
                        stockOnHand,
                        latestLineItem.getReason().getName()
                )
        );

      }
    }

    return messageBuilder.toString();
  }
}