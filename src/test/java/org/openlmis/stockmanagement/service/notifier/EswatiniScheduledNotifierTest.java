package org.openlmis.stockmanagement.service.notifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.openlmis.stockmanagement.dto.referencedata.RoleAssignmentDto;
import org.openlmis.stockmanagement.dto.referencedata.RoleDto;
import org.openlmis.stockmanagement.dto.referencedata.UserDto;
import org.openlmis.stockmanagement.service.EswatiniRoleAssignmentService;
import org.openlmis.stockmanagement.service.EswatiniUserService;
import org.openlmis.stockmanagement.service.dtos.ProcessingPeriodDto;
import org.openlmis.stockmanagement.service.notification.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.scheduling.support.CronSequenceGenerator;

import java.time.LocalDate;
import java.util.*;

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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCron() {
        CronSequenceGenerator generator = new CronSequenceGenerator("0 30 21 * * ?", TimeZone.getTimeZone("UTC"));
        Date now = GregorianCalendar.getInstance().getTime();
        Date next = generator.next(now);
        Date nextToNext = generator.next(next);
        System.out.println(next);
        System.out.println(nextToNext);
    }
    @Test
    void remindToDoPhysicalCounting() {
        setupForReminderToDoPhysicalCounting();
        notifier.remindToDoPhysicalCounting(LocalDate.of(2021, 9, 2), 28);
        notifier.remindToDoPhysicalCounting(LocalDate.of(2021, 9, 2), 28);
        verify(notificationService, times(2)).notify(any(), any(), any());
    }

    private void setupForReminderToDoPhysicalCounting() {
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
    }
}