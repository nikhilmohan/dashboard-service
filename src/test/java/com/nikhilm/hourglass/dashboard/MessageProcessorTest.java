package com.nikhilm.hourglass.dashboard;

import static org.junit.jupiter.api.Assertions.*;

import com.nikhilm.hourglass.dashboard.exceptions.DashboardException;
import com.nikhilm.hourglass.dashboard.models.*;
import com.nikhilm.hourglass.dashboard.services.DashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageProcessorTest {

    @Mock
    DashboardService dashboardService;

    @InjectMocks
    MessageProcessor messageProcessor;

    @Test
    public void testProcessTaskEvent()  {

        DashboardMetric metric = new DashboardMetric();

        Task task = new Task();
        task.setUserId("abc");
        task.setName("A new task");
        task.setDueDate(LocalDate.now());

        Event event = new Event<String, Object>(Event.Type.TASK_ADDED, task.getId(),
                task);

        Mockito.when(dashboardService.computeScoreOnTaskAdd(any(Task.class))).thenReturn(Mono.just(metric));
        messageProcessor.processTaskEvents(event);
        verify(dashboardService).computeScoreOnTaskAdd(any(Task.class));

    }
    @Test
    public void testProcessTaskCompleteEvent()  {

        DashboardMetric metric = new DashboardMetric();

        Task task = new Task();
        task.setUserId("abc");
        task.setName("A new task");
        task.setDueDate(LocalDate.now());

        Event event = new Event<String, Object>(Event.Type.TASK_COMPLETED, task.getId(),
                task);

        Mockito.when(dashboardService.computeScoreOnTaskComplete(any(Task.class))).thenReturn(Mono.just(metric));
        messageProcessor.processTaskEvents(event);
        verify(dashboardService).computeScoreOnTaskComplete(any(Task.class));

    }
    @Test
    public void testProcessInvalidEventType()  {

        Task task = new Task();
        task.setUserId("abc");
        task.setName("A new task");
        task.setDueDate(LocalDate.now());

        Event event = new Event<String, Object>(Event.Type.USER_ADDED, task.getId(),
                task);

        assertThrows(RuntimeException.class, ()->messageProcessor.processTaskEvents(event));
    }


    @Test
    public void testProcessInvalidEventFormat() {
        Event event = new Event<String, Object>(Event.Type.TASK_ADDED, "key",
                new Object());
        assertThrows(DashboardException.class, ()->messageProcessor.processTaskEvents(event));

    }
    @Test
    public void testProcessGoalInvalidEventFormat() {
        Event event = new Event<String, Object>(Event.Type.GOAL_ADDED, "key",
                new Object());
        assertThrows(DashboardException.class, ()->messageProcessor.processGoalEvents(event));

    }
    @Test
    public void testProcessGoalInvalidTaskEventMissing() {
        Event event = new Event<String, Object>(Event.Type.TASK_COMPLETED, "key",
                null);
        assertThrows(DashboardException.class, ()->messageProcessor.processTaskEvents(event));

    }
    @Test
    public void testProcessEmptyEventPayload() {
        Event event = new Event<String, Object>(Event.Type.TASK_ADDED, "key",
                Optional.empty());
        assertThrows(DashboardException.class, ()->messageProcessor.processTaskEvents(event));

    }
    @Test
    public void testProcessGoalEvent()  {

        DashboardMetric metric = new DashboardMetric();

        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setName("A new goal");
        goal.setLevel(GoalLevel.EASY);
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setDueDate(LocalDate.now().minusDays(2L));

        Event event = new Event<String, Object>(Event.Type.GOAL_ADDED, goal.getId(),
                goal);

        Mockito.when(dashboardService.computeScoreOnGoalAdd(any(Goal.class))).thenReturn(Mono.just(metric));
        messageProcessor.processGoalEvents(event);
        verify(dashboardService).computeScoreOnGoalAdd(any(Goal.class));

    }
    @Test
    public void testProcessGoalCompleteEvent()  {

        DashboardMetric metric = new DashboardMetric();

        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setName("A new goal");
        goal.setLevel(GoalLevel.EASY);
        goal.setStatus(GoalStatus.COMPLETED);
        goal.setDueDate(LocalDate.now().minusDays(2L));

        Event event = new Event<String, Object>(Event.Type.GOAL_COMPLETED, goal.getId(),
                goal);

        Mockito.when(dashboardService.computeScoreOnGoalComplete(any(Goal.class))).thenReturn(Mono.just(metric));
        messageProcessor.processGoalEvents(event);
        verify(dashboardService).computeScoreOnGoalComplete(any(Goal.class));

    }
    @Test
    public void testProcessGoalInvalidEventType()  {

        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setName("A new goal");
        goal.setLevel(GoalLevel.EASY);
        goal.setStatus(GoalStatus.COMPLETED);
        goal.setDueDate(LocalDate.now().minusDays(2L));


        Event event = new Event<String, Object>(Event.Type.USER_ADDED, goal.getId(),
                goal);

        assertThrows(RuntimeException.class, ()->messageProcessor.processGoalEvents(event));
    }
    @Test
    public void testProcessGoalDeferredEvent()  {

        DashboardMetric metric = new DashboardMetric();

        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setName("A new goal");
        goal.setLevel(GoalLevel.EASY);
        goal.setStatus(GoalStatus.DEFERRED);
        goal.setDueDate(LocalDate.now().minusDays(2L));

        Event event = new Event<String, Object>(Event.Type.GOAL_DEFERRED, goal.getId(), goal);

        Mockito.when(dashboardService.computeScoreOnGoalDeferred(any(Goal.class))).thenReturn(Mono.just(metric));
        messageProcessor.processGoalEvents(event);
        verify(dashboardService).computeScoreOnGoalDeferred(any(Goal.class));

    }
    @Test
    public void testProcessGoalResumedEvent()  {

        DashboardMetric metric = new DashboardMetric();

        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setName("A new goal");
        goal.setLevel(GoalLevel.EASY);
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setDueDate(LocalDate.now().minusDays(2L));

        Event event = new Event<String, Object>(Event.Type.GOAL_RESUMED, goal.getId(), goal);

        Mockito.when(dashboardService.computeScoreOnGoalResumed(any(Goal.class))).thenReturn(Mono.just(metric));
        messageProcessor.processGoalEvents(event);
        verify(dashboardService).computeScoreOnGoalResumed(any(Goal.class));

    }
    @Test
    public void testProcessUserEvent()  {

        DashboardMetric metric = new DashboardMetric();
        Event event = new Event<String, Object>(Event.Type.USER_ADDED, "abc",null);

        doNothing().when(dashboardService).initializeUser(event);
        messageProcessor.processUserEvents(event);
        verify(dashboardService).initializeUser(event);

    }
    @Test
    public void testProcessUserEventInvalid()  {

        DashboardMetric metric = new DashboardMetric();
        Event event = new Event<String, Object>(Event.Type.GOAL_RESUMED, "abc",null);

        assertThrows(RuntimeException.class, ()->messageProcessor.processUserEvents(event));


    }
    @Test
    public void testProcessGoalInvalidGoalEventMissing() {
        Event event = new Event<String, Object>(Event.Type.GOAL_RESUMED, "key",
                null);
        assertThrows(DashboardException.class, ()->messageProcessor.processGoalEvents(event));

    }
}
