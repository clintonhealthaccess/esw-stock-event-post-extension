package org.openlmis.stockmanagement.service;

import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.extension.point.StockEventPostProcessor;
import org.openlmis.stockmanagement.service.notifier.EswatiniPhysicalInventoryNotifier;
import org.openlmis.stockmanagement.service.notifier.EswatiniStockAdjustmentNotifier;
import org.openlmis.stockmanagement.service.notifier.EswatiniStockoutNotifier;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;

@Component(value = "EswatiniStockEventPostProcessor")
public class EswatiniStockEventPostProcessor implements StockEventPostProcessor {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(
          StockEventPostProcessor.class);

  @Autowired
  private EswatiniPhysicalInventoryNotifier eswatiniPhysicalInventoryNotifier;

  @Autowired
  private EswatiniStockAdjustmentNotifier eswatiniStockAdjustmentNotifier;

  @Autowired
  private EswatiniStockoutNotifier eswatiniStockoutNotifier;

  @Override
  public void process(StockEventDto stockEventDto) {
    XLOGGER.debug("EswatiniStockEventPostProcessor init");
    OAuth2Authentication authentication = (OAuth2Authentication) SecurityContextHolder
            .getContext()
            .getAuthentication();
    if (!authentication.isClientOnly()) {
      if (stockEventDto.isPhysicalInventory()) {
        XLOGGER.debug("Trying to notify all users about the physical inventory update");
        eswatiniPhysicalInventoryNotifier.notify(stockEventDto);
      } else {
        XLOGGER.debug("This event is not a physical inventory");
        //eswatiniStockAdjustmentNotifier.notify(stockEventDto);
      }
    } else {
      XLOGGER.debug("Skip notifying for client only api call");
    }
    eswatiniStockoutNotifier.notify(stockEventDto);
  }
}
