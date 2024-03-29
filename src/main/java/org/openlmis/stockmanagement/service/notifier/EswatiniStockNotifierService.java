package org.openlmis.stockmanagement.service.notifier;

import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.referencedata.RightDto;
import org.openlmis.stockmanagement.dto.referencedata.SupervisoryNodeDto;
import org.openlmis.stockmanagement.dto.referencedata.UserDto;
import org.openlmis.stockmanagement.service.notification.NotificationService;
import org.openlmis.stockmanagement.service.referencedata.RightReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.SupervisingUsersReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.SupervisoryNodeReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.FacilityReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.ProgramReferenceDataService;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.UUID;

import static org.openlmis.stockmanagement.service.PermissionService.STOCK_INVENTORIES_EDIT;

@Service
public class EswatiniStockNotifierService {

    private static final XLogger XLOGGER = XLoggerFactory.getXLogger(
            EswatiniStockAdjustmentNotifier.class);

    @Autowired
    private SupervisoryNodeReferenceDataService supervisoryNodeReferenceDataService;

    @Autowired
    private SupervisingUsersReferenceDataService supervisingUsersReferenceDataService;
    
    @Autowired
    private FacilityReferenceDataService facilityReferenceDataService;

    @Autowired
    private ProgramReferenceDataService programReferenceDataService;

    @Autowired
    private RightReferenceDataService rightReferenceDataService;

    @Autowired
    private NotificationService notificationService;

    public void sendMessage(StockEventDto stockEventDto, String message) {
        RightDto rightDto = rightReferenceDataService.findRight(STOCK_INVENTORIES_EDIT);
        UUID programId = stockEventDto.getProgramId();
        UUID facilityId = stockEventDto.getFacilityId();
        String programName = programReferenceDataService.findOne(programId).getName();
        String facilityName = facilityReferenceDataService.findOne(facilityId).getName();

        Collection<UserDto> recipients = getEditors(programId, facilityId, rightDto.getId());

        for (UserDto recipient : recipients) {
            XLOGGER.debug("Recipient username = {}", recipient.getUsername());
            String subject = String.format("Stock for %s - %s  has been adjusted", facilityName, programName);
            notificationService.notify(recipient,
                    subject, message);
        }
    }

    public Collection<UserDto> getEditors(UUID programId, UUID facilityId, UUID rightId) {
        SupervisoryNodeDto supervisoryNode = supervisoryNodeReferenceDataService
                .findSupervisoryNode(programId, facilityId);

        if (supervisoryNode == null) {
            throw new IllegalArgumentException(
                    String.format("There is no supervisory node for program %s and facility %s",
                            programId, facilityId));
        }

        XLOGGER.debug("Supervisory node ID = {}", supervisoryNode.getId());
        XLOGGER.debug("Searching for supervising users with node ID = {} rightid = {} programId = {}",
                supervisoryNode.getId(),
                rightId,
                programId);

        return supervisingUsersReferenceDataService
                .findAll(supervisoryNode.getId(), rightId, programId);
    }
}
