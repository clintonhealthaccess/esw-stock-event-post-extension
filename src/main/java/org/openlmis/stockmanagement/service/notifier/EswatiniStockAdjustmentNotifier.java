package org.openlmis.stockmanagement.service.notifier;

import com.google.common.collect.Iterables;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.dto.StockCardDto;
import org.openlmis.stockmanagement.dto.StockCardLineItemDto;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.service.EswatiniUserService;
import org.openlmis.stockmanagement.service.StockCardService;
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
  private EswatiniStockCardNotifier stockCardNotifier;

  @Autowired
  private EswatiniUserService eswatiniUserService;

  @Autowired
  private StockCardService stockCardService;

  @Autowired
  private StockCardRepository stockCardRepository;

  @Autowired
  private EswatiniStockNotifierService eswatiniNotifierService;

  public void notify(StockEventDto stockEventDto) {
    String message = buildMessage(stockEventDto);
    eswatiniNotifierService.sendMessage(stockEventDto, message);
  }

  private String buildMessage(StockEventDto stockEventDto) {
    String currentUserName = eswatiniUserService.getCurrentUserName(stockEventDto);
    String initialBody = String.format("User %s has made following stock adjustments: \n", currentUserName);
    StringBuilder messageBuilder = new StringBuilder(initialBody);
    for (StockEventLineItemDto item : stockEventDto.getLineItems()) {
      UUID orderableId = item.getOrderableId();
      String orderableName = stockCardNotifier.getOrderableName(orderableId);

      List<StockCard> cards = stockCardRepository.findByOrderableIdInAndProgramIdAndFacilityId(Arrays.asList(orderableId),
              stockEventDto.getProgramId(),
              stockEventDto.getFacilityId());
      for (StockCard stockCard : cards) {
        StockCardDto stockCardDto = stockCardService.findStockCardById(stockCard.getId());
        StockCardLineItemDto stockCardLineItemDto = Iterables.getLast(stockCardDto.getLineItems());
        StockCardLineItem lineItem = stockCardLineItemDto.getLineItem();
        Integer quantityWithSign = lineItem.getQuantityWithSign();
        Integer stockOnHand = lineItem.getStockOnHand();
        StockCardLineItemReason reason = lineItem.getReason();
        XLOGGER.debug("QuantityWithSign: {} StockOnHand: {} Orderable Name: {} Reason Name: {}",
                quantityWithSign,
                stockOnHand,
                orderableName,
                reason != null  ? reason.getName() : "");
        messageBuilder.append(String.format("%s, Stock on Hand: %d, Current Stock: %d, Reason: %s\n",
                        orderableName,
                        stockOnHand + (-quantityWithSign),
                        stockOnHand,
                        reason != null  ? reason.getName() : ""
                )
        );

      }
    }

    return messageBuilder.toString();
  }
}