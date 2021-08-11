package org.openlmis.stockmanagement.service.notifier;

import antlr.ASTNULLType;
import org.apache.logging.log4j.util.Strings;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventory;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventoryLineItem;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.referencedata.RightDto;
import org.openlmis.stockmanagement.dto.referencedata.SupervisoryNodeDto;
import org.openlmis.stockmanagement.dto.referencedata.UserDto;
import org.openlmis.stockmanagement.repository.PhysicalInventoriesRepository;
import org.openlmis.stockmanagement.service.notification.NotificationService;
import org.openlmis.stockmanagement.service.referencedata.RightReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.SupervisingUsersReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.SupervisoryNodeReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.UserReferenceDataService;
import org.openlmis.stockmanagement.util.AuthenticationHelper;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import static org.openlmis.stockmanagement.service.PermissionService.STOCK_INVENTORIES_EDIT;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class StockAdjustmentNotifier {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(
          StockAdjustmentNotifier.class);

  @Autowired
  private StockCardNotifier stockCardNotifier;

  @Autowired
  private PhysicalInventoriesRepository physicalInventoriesRepository;

  @Autowired
  private RightReferenceDataService rightReferenceDataService;

  @Autowired
  private SupervisoryNodeReferenceDataService supervisoryNodeReferenceDataService;

  @Autowired
  private SupervisingUsersReferenceDataService supervisingUsersReferenceDataService;

  @Autowired
  private UserReferenceDataService userReferenceDataService;

  @Autowired
  private NotificationService notificationService;

  @Autowired
  private AuthenticationHelper authenticationHelper;

  public void notify(StockEventDto stockEventDto) {

    String currentUserName = authenticationHelper.getCurrentUser().getUsername();
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
    RightDto rightDto = rightReferenceDataService.findRight(STOCK_INVENTORIES_EDIT);
    UUID programId = stockEventDto.getProgramId();
    UUID facilityId = stockEventDto.getFacilityId();

    Collection<UserDto> recipients = getEditors(programId, facilityId, rightDto.getId());

    UserDto user = userReferenceDataService.findUser("dhc_store");
    recipients = Arrays.asList(user);

    for (UserDto recipient : recipients) {
      if (facilityId.equals(recipient.getHomeFacilityId())) {
        XLOGGER.debug("Recipient username = {}", recipient.getUsername());
        String programName = stockCardNotifier.getProgramName(programId);
        String facilityName = stockCardNotifier.getFacilityName(facilityId);
        String subject = String.format("Stock for %s - %s  has been adjusted", facilityName, programName);
        notificationService.notify(recipient,
                subject, messageBuilder.toString());
      }
    }

  }

  private Collection<UserDto> getEditors(UUID programId, UUID facilityId, UUID rightId) {
    SupervisoryNodeDto supervisoryNode = supervisoryNodeReferenceDataService
            .findSupervisoryNode(programId, facilityId);

    if (supervisoryNode == null) {
      throw new IllegalArgumentException(
              String.format("There is no supervisory node for program %s and facility %s",
                      programId, facilityId));
    }

    XLOGGER.debug("Supervisory node ID = {}", supervisoryNode.getId());

    return supervisingUsersReferenceDataService
            .findAll(supervisoryNode.getId(), rightId, programId);
  }
}