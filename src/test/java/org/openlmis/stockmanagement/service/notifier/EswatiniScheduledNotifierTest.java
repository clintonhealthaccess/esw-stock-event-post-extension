package org.openlmis.stockmanagement.service.notifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.openlmis.stockmanagement.dto.referencedata.RoleAssignmentDto;
import org.openlmis.stockmanagement.dto.referencedata.RoleDto;
import org.openlmis.stockmanagement.dto.referencedata.UserDto;
import org.openlmis.stockmanagement.service.EswatiniProcessingPeriodService;
import org.openlmis.stockmanagement.service.EswatiniRoleAssignmentService;
import org.openlmis.stockmanagement.service.EswatiniUserService;
import org.openlmis.stockmanagement.service.dtos.ProcessingPeriodDto;
import org.openlmis.stockmanagement.service.notification.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EswatiniScheduledNotifierTest {

    @InjectMocks
    @Spy
    private EswatiniScheduledNotifier notifier;

    @Mock
    private EswatiniUserService userService;

    @Mock
    private EswatiniRoleAssignmentService roleAssignmentService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EswatiniProcessingPeriodService processingPeriodService;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void remindToDoPhysicalCounting() {
        List<UserDto> users = new ArrayList<>();
        users.add(new UserDto());
        RoleAssignmentDto roleAssignmentDto = new RoleAssignmentDto();
        RoleDto roleDto = new RoleDto();
        roleDto.setName("Stock Manager");
        roleAssignmentDto.setRole(roleDto);
        Collection<RoleAssignmentDto> roleAssignmentDtos = new ArrayList<>();
        roleAssignmentDtos.add(roleAssignmentDto);
        Page<UserDto> userDtos = new PageImpl<>(users);
        ProcessingPeriodDto processingPeriodDto = new ProcessingPeriodDto();
        Mockito.when(userService.getPage(any())).thenReturn(userDtos);
        Mockito.when(roleAssignmentService.getRoleAssignments(any())).thenReturn(roleAssignmentDtos);
        doReturn(processingPeriodDto).when(notifier).getProcessingPeriod(any());
        notifier.remindToDoPhysicalCounting(LocalDate.of(2021, 9, 2), 28);
        notifier.remindToDoPhysicalCounting(LocalDate.of(2021, 9, 2), 28);
        verify(notificationService, times(2)).notify(any(), any(), any());
    }
}