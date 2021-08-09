package org.openlmis.stockmanagement.service;

import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.extension.point.StockEventPostProcessor;
import org.openlmis.stockmanagement.service.notifier.StockAdjustmentNotifier;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(value = "EswatiniStockEventPostProcessor")
public class EswatiniStockEventPostProcessor implements StockEventPostProcessor {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(
          StockEventPostProcessor.class);

  @Autowired
  private StockAdjustmentNotifier stockAdjustmentNotifier;

  @Override
  public void process(StockEventDto stockEventDto) {
    XLOGGER.debug("EswatiniStockEventPostProcessor init");
    stockAdjustmentNotifier.notify(stockEventDto);
  }
}
