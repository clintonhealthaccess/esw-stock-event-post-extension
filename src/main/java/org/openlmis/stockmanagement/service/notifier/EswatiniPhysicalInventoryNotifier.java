package org.openlmis.stockmanagement.service.notifier;

import org.apache.logging.log4j.util.Strings;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventory;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventoryLineItem;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.repository.PhysicalInventoriesRepository;
import org.openlmis.stockmanagement.service.EswatiniUserService;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class EswatiniPhysicalInventoryNotifier {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(
          EswatiniPhysicalInventoryNotifier.class);

  @Autowired
  private StockCardNotifier stockCardNotifier;

  @Autowired
  private PhysicalInventoriesRepository physicalInventoriesRepository;

  @Autowired
  private EswatiniNotifierService eswatiniNotifierService;

  @Autowired
  private EswatiniUserService userService;

  public void notify(StockEventDto stockEventDto) {
    String message = buildMessage(stockEventDto);
    eswatiniNotifierService.sendMessage(stockEventDto, message);
  }

  private String buildMessage(StockEventDto stockEventDto) {
    String currentUserName = userService.getCurrentUserName(stockEventDto);

    String initialBody = String.format("User %s has made following stock adjustments: \n", currentUserName);
    StringBuilder messageBuilder = new StringBuilder(initialBody);
    UUID resourceId = stockEventDto.getResourceId();
    Optional<PhysicalInventory> inventoryOptional = physicalInventoriesRepository.findById(resourceId);
    inventoryOptional.ifPresent(physicalInventory -> {
      List<PhysicalInventoryLineItem> lineItems = physicalInventory.getLineItems();
      lineItems.forEach(item -> {
        String reasonsText = item.getStockAdjustments().stream()
                .map(a -> String.format("%d %s", a.getQuantity(), a.getReason().getName()))
                .collect(Collectors.joining(", "));
        String orderableName = stockCardNotifier.getOrderableName(item.getOrderableId());
        Integer previousStockOnHandWhenSubmitted = item.getPreviousStockOnHandWhenSubmitted();
        Integer quantity = item.getQuantity();
        int diff = quantity - previousStockOnHandWhenSubmitted;
        if (diff != 0) {
          messageBuilder.append(String.format("%s, Stock on Hand: %d, Current Stock: %d%s\n",
                  orderableName,
                  previousStockOnHandWhenSubmitted,
                  quantity,
                  !Strings.isEmpty(reasonsText) ?
                          String.format(" , Reasons: %s", reasonsText)
                          : ""
                  )
          );
        }
      });
    });
    return messageBuilder.toString();
  }
}