package com.nikhilm.hourglass.dashboard.services;

import com.nikhilm.hourglass.dashboard.exceptions.DashboardException;
import com.nikhilm.hourglass.dashboard.models.*;
import com.nikhilm.hourglass.dashboard.repositories.DashboardMetricRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.data.mongodb.core.mapping.TextScore;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Slf4j
class DashboardServiceTest {

    @Mock
    DashboardMetricRepository dashboardMetricRepository;

    @Mock
    DashboardMetricToMetricResponseMapper mapper;


    @InjectMocks
    DashboardService dashboardService;

    DashboardMetric dashboardMetric;

    @BeforeEach
    public void setup() {
        dashboardMetric = null;
        LocalDate currentDate = LocalDate.now();
        LocalDate currentMonth = LocalDate.of(currentDate.getYear(), currentDate.getMonth(), 2);
        dashboardMetric = new DashboardMetric();
        dashboardMetric.setUserId("abc");
        MetricSummary metricSummary = new MetricSummary();
        metricSummary.setTasksCompleted(3L);
        metricSummary.setTasksPlanned(5L);
        metricSummary.setGoalsPlanned(10);
        metricSummary.setMonth(currentMonth);
        MetricSummary metricSummary1 = new MetricSummary();
        metricSummary1.setTasksCompleted(3L);
        metricSummary1.setTasksPlanned(5L);
        metricSummary1.setGoalsPlanned(10);
        metricSummary1.setMonth(currentMonth.minusMonths(1L));
        MetricSummary metricSummary2 = new MetricSummary();
        metricSummary2.setTasksCompleted(3L);
        metricSummary2.setTasksPlanned(5L);
        metricSummary2.setGoalsPlanned(10);
        metricSummary2.setMonth(currentMonth.minusMonths(2L));
        dashboardMetric.getMetricSummaryList().addAll(Arrays.asList(metricSummary, metricSummary1, metricSummary2));

    }

    @Test
    public void testGetMetrics() {
        MetricResponse response = new MetricResponse();
        dashboardMetric.setAverageScore(45.5);
        response.setAverageScore(45.5);
        Mockito.when(dashboardMetricRepository.findByUserId("abc")).thenReturn(Mono.just(dashboardMetric));
        Mockito.when(mapper.dashboardMetricToMetricResponse(any(DashboardMetric.class))).thenReturn(response);
        StepVerifier.create(dashboardService.getMetrics("abc"))
                .expectSubscription()
                .expectNextMatches(metricResponse -> metricResponse.getAverageScore() == dashboardMetric.getAverageScore())
                .verifyComplete();
    }
    @Test
    public void testGetMetricsNotFound() {

        Mockito.when(dashboardMetricRepository.findByUserId("abc")).thenReturn(Mono.empty());
        StepVerifier.create(dashboardService.getMetrics("abc"))
                .expectSubscription()
                .expectErrorMessage("User records not found")
                .verify();
    }
    @Test
    public void testInitMetrics()   {
        Mockito.when(dashboardMetricRepository.save(any(DashboardMetric.class))).thenReturn(Mono.just(dashboardMetric));
        StepVerifier.create(dashboardService.initMetrics("abc"))
                .expectSubscription()
                .expectNextMatches(dashboardMetric -> dashboardMetric.getUserId().equals("abc"))
                .verifyComplete();

    }
    @Test
    public void testInitializeMetrics()   {
        Mockito.when(dashboardMetricRepository.save(any(DashboardMetric.class))).thenReturn(Mono.just(dashboardMetric));
        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setName("A new goal");
        goal.setLevel(GoalLevel.EASY);
        goal.setDueDate(LocalDate.now().plusDays(10L));


        Event event = new Event(Event.Type.USER_ADDED, "abc", goal);
        dashboardService.initializeUser(event);
        verify(dashboardMetricRepository, times(1)).save(any(DashboardMetric.class));

    }
    @Test
    public void testInitMetricsError()   {
        Mockito.when(dashboardMetricRepository.save(any(DashboardMetric.class)))
                .thenReturn(Mono.error(new RuntimeException()));
        StepVerifier.create(dashboardService.initMetrics("abc"))
                .expectSubscription()
                .expectErrorMessage("Internal server error!")
                .verify();

    }

    @Test
    public void testComputeScoreOnGoalAdd(){

        ArgumentCaptor<DashboardMetric> captor = ArgumentCaptor.forClass(DashboardMetric.class);
        dashboardMetric.setTotalGoalsInProgress(4L);

        Mockito.when(dashboardMetricRepository.findByUserId("abc")).thenReturn(Mono.just(dashboardMetric));

        Mockito.when(dashboardMetricRepository.save(captor.capture())).thenReturn(Mono.just(dashboardMetric));
        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setName("A new goal");
        goal.setLevel(GoalLevel.EASY);
        goal.setDueDate(LocalDate.now().plusDays(10L));

        StepVerifier.create(dashboardService.computeScoreOnGoalAdd(goal))
                .expectSubscription()
                .expectNext(dashboardMetric)
                .verifyComplete();

        DashboardMetric metric = captor.getValue();
        assertEquals(metric.getTotalGoalsInProgress(), dashboardMetric.getTotalGoalsInProgress() + 1);
    }
    @Test
    public void testComputeScoreOnGoalAddInvalid(){


        Mockito.when(dashboardMetricRepository.findByUserId("abc")).thenReturn(Mono.empty());

        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setName("A new goal");
        goal.setLevel(GoalLevel.EASY);
        goal.setDueDate(LocalDate.now().plusDays(10L));

        StepVerifier.create(dashboardService.computeScoreOnGoalAdd(goal))
                .expectSubscription()
                .expectErrorMessage("User records not found!")
                .verify();


    }
    @Test
    public void testComputeScoreOnGoalComplete(){

        ArgumentCaptor<DashboardMetric> captor = ArgumentCaptor.forClass(DashboardMetric.class);

        Mockito.when(dashboardMetricRepository.findByUserId("abc")).thenReturn(Mono.just(dashboardMetric));

        Mockito.when(dashboardMetricRepository.save(captor.capture())).thenReturn(Mono.just(dashboardMetric));
        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setName("A new goal");
        goal.setLevel(GoalLevel.EASY);
        goal.setDueDate(LocalDate.now().plusDays(10L));

        StepVerifier.create(dashboardService.computeScoreOnGoalComplete(goal))
                .expectSubscription()
                .expectNext(dashboardMetric)
                .verifyComplete();

        DashboardMetric metric = captor.getValue();
        assertEquals(metric.getEasyGoalsCompleted(), dashboardMetric.getEasyGoalsCompleted() + 1);
    }
    @Test
    public void testComputeScoreOnGoalCompleteModerate(){

        dashboardService.setModerateGoalScore(25);
        ArgumentCaptor<DashboardMetric> captor = ArgumentCaptor.forClass(DashboardMetric.class);

        Mockito.when(dashboardMetricRepository.findByUserId("abc")).thenReturn(Mono.just(dashboardMetric));

        Mockito.when(dashboardMetricRepository.save(captor.capture())).thenReturn(Mono.just(dashboardMetric));
        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setName("A new goal");
        goal.setLevel(GoalLevel.MODERATE);
        goal.setDueDate(LocalDate.now().minusDays(5L));
        long initialGoalScore = 100;
        dashboardMetric.setGoalScore(initialGoalScore);


        StepVerifier.create(dashboardService.computeScoreOnGoalComplete(goal))
                .expectSubscription()
                .expectNext(dashboardMetric)
                .verifyComplete();

        DashboardMetric metric = captor.getValue();
        assertEquals(metric.getModerateGoalsCompleted(), dashboardMetric.getModerateGoalsCompleted() + 1);
        assertEquals(120, metric.getGoalScore());
    }
    @Test
    public void testComputeScoreOnGoalCompleteExtreme(){

        ArgumentCaptor<DashboardMetric> captor = ArgumentCaptor.forClass(DashboardMetric.class);

        Mockito.when(dashboardMetricRepository.findByUserId("abc")).thenReturn(Mono.just(dashboardMetric));

        Mockito.when(dashboardMetricRepository.save(captor.capture())).thenReturn(Mono.just(dashboardMetric));
        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setName("A new goal");
        goal.setLevel(GoalLevel.EXTREME);
        goal.setDueDate(LocalDate.now().plusDays(10L));

        StepVerifier.create(dashboardService.computeScoreOnGoalComplete(goal))
                .expectSubscription()
                .expectNext(dashboardMetric)
                .verifyComplete();

        DashboardMetric metric = captor.getValue();
        assertEquals(metric.getExtremeGoalsCompleted(), dashboardMetric.getExtremeGoalsCompleted() + 1);
    }
    @Test
    public void testComputeScoreOnGoalCompleteInvalid(){

        Mockito.when(dashboardMetricRepository.findByUserId("abc")).thenReturn(Mono.empty());
        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setName("A new goal");
        goal.setLevel(GoalLevel.EASY);
        goal.setDueDate(LocalDate.now().plusDays(10L));

        StepVerifier.create(dashboardService.computeScoreOnGoalComplete(goal))
                .expectSubscription()
                .expectErrorMessage("User records not found!")
                .verify();


    }
    @Test
    public void testComputeScoreOnGoalDeferred(){

        dashboardService.setDeferPenalty(6);
        dashboardService.setExtremeGoalScore(50);

        log.info("total deferred " + dashboardMetric.getTotalGoalsDeferred());

        ArgumentCaptor<DashboardMetric> captor = ArgumentCaptor.forClass(DashboardMetric.class);

        Mockito.when(dashboardMetricRepository.findByUserId("abc")).thenReturn(Mono.just(dashboardMetric));

        Mockito.when(dashboardMetricRepository.save(captor.capture())).thenReturn(Mono.just(dashboardMetric));
        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setName("A new goal");
        goal.setLevel(GoalLevel.EXTREME);
        goal.setStatus(GoalStatus.DEFERRED);
        goal.setDueDate(LocalDate.now().minusDays(4L));
        long initialGoalScore = 100;
        dashboardMetric.setGoalScore(initialGoalScore);

        StepVerifier.create(dashboardService.computeScoreOnGoalDeferred(goal))
                .expectSubscription()
                .expectNext(dashboardMetric)
                .verifyComplete();

        DashboardMetric metric = captor.getValue();
        assertEquals(metric.getTotalGoalsDeferred(), dashboardMetric.getTotalGoalsDeferred() + 1);
        assertEquals(90, metric.getGoalScore());
    }
    @Test
    public void testComputeScoreOnGoalResumed(){

        dashboardService.setResumeBonus(1);
        dashboardService.setEasyGoalScore(10);
        log.info("total resumed " + dashboardMetric.getTotalGoalsInProgress());

        ArgumentCaptor<DashboardMetric> captor = ArgumentCaptor.forClass(DashboardMetric.class);

        Mockito.when(dashboardMetricRepository.findByUserId("abc")).thenReturn(Mono.just(dashboardMetric));

        Mockito.when(dashboardMetricRepository.save(captor.capture())).thenReturn(Mono.just(dashboardMetric));
        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setName("A new goal");
        goal.setLevel(GoalLevel.EASY);
        goal.setStatus(GoalStatus.DEFERRED);
        goal.setDueDate(LocalDate.now().minusDays(2L));
        long initialGoalScore = 100;
        dashboardMetric.setGoalScore(initialGoalScore);
        StepVerifier.create(dashboardService.computeScoreOnGoalResumed(goal))
                .expectSubscription()
                .expectNext(dashboardMetric)
                .verifyComplete();

        DashboardMetric metric = captor.getValue();
        assertEquals(metric.getTotalGoalsInProgress(), dashboardMetric.getTotalGoalsInProgress() + 1);
        assertEquals(99, metric.getGoalScore());
    }
    @Test
    public void testComputeScoreOnTaskAdd(){
        LocalDate currentMonth = LocalDate.now();

        DashboardMetric metric = new DashboardMetric();
        MetricSummary metricSummary = new MetricSummary();
        metricSummary.setTasksCompleted(3L);
        metricSummary.setTasksPlanned(5L);
        metricSummary.setGoalsPlanned(10);
        metricSummary.setMonth(LocalDate.of(currentMonth.getYear(), currentMonth.getMonth(), 2));
        metric.getMetricSummaryList().add(metricSummary);

        log.info("initial summary " + metricSummary);
        long initialTasksPlanned = metricSummary.getTasksPlanned();
        ArgumentCaptor<DashboardMetric> captor = ArgumentCaptor.forClass(DashboardMetric.class);


        Mockito.when(dashboardMetricRepository.findByUserId("abc")).thenReturn(Mono.just(metric));

        Mockito.when(dashboardMetricRepository.save(captor.capture())).thenReturn(Mono.just(dashboardMetric));
        Task task = new Task();
        task.setUserId("abc");
        task.setName("A new task");
        task.setDueDate(LocalDate.now());

        StepVerifier.create(dashboardService.computeScoreOnTaskAdd(task))
                .expectSubscription()
                .expectNext(dashboardMetric)
                .verifyComplete();

        DashboardMetric captorMetric = captor.getValue();

        MetricSummary computedSummary = captorMetric.getMetricSummaryList().stream()
                .filter(summary -> summary.getMonth()
                        .equals(LocalDate.of(currentMonth.getYear(), currentMonth.getMonth(), 2)))
                .findAny().get();
        assertEquals(initialTasksPlanned + 1, computedSummary.getTasksPlanned());
    }
    @Test
    public void testComputeScoreOnTaskComplete(){
        LocalDate currentMonth = LocalDate.now();

        DashboardMetric metric = new DashboardMetric();
        MetricSummary metricSummary = new MetricSummary();
        metricSummary.setTasksCompleted(3L);
        metricSummary.setTasksPlanned(5L);
        metricSummary.setGoalsPlanned(10);
        metricSummary.setMonth(LocalDate.of(currentMonth.getYear(), currentMonth.getMonth(), 2));
        metric.getMetricSummaryList().add(metricSummary);

        log.info("initial summary " + metricSummary);
        long initialTasksCompleted = metricSummary.getTasksCompleted();
        ArgumentCaptor<DashboardMetric> captor = ArgumentCaptor.forClass(DashboardMetric.class);


        Mockito.when(dashboardMetricRepository.findByUserId("abc")).thenReturn(Mono.just(metric));

        Mockito.when(dashboardMetricRepository.save(captor.capture())).thenReturn(Mono.just(dashboardMetric));
        Task task = new Task();
        task.setUserId("abc");
        task.setName("A new task");
        task.setDueDate(LocalDate.now());

        StepVerifier.create(dashboardService.computeScoreOnTaskComplete(task))
                .expectSubscription()
                .expectNext(dashboardMetric)
                .verifyComplete();

        DashboardMetric captorMetric = captor.getValue();

        MetricSummary computedSummary = captorMetric.getMetricSummaryList().stream()
                .filter(summary -> summary.getMonth()
                        .equals(LocalDate.of(currentMonth.getYear(), currentMonth.getMonth(), 2)))
                .findAny().get();
        assertEquals(initialTasksCompleted + 1, computedSummary.getTasksCompleted());
    }

    @Test
    public void testComputeScoreOnTaskCompleteInvalid(){

        Mockito.when(dashboardMetricRepository.findByUserId("abc")).thenReturn(Mono.empty());
        Task task = new Task();
        task.setUserId("abc");
        task.setName("A new task");
        task.setDueDate(LocalDate.now());
        StepVerifier.create(dashboardService.computeScoreOnTaskComplete(task))
                .expectSubscription()
                .expectErrorMessage("User records not found")
                .verify();


    }
    @Test
    public void testComputeScoreOnTaskAddInvalid(){


        Mockito.when(dashboardMetricRepository.findByUserId("abc")).thenReturn(Mono.empty());

        Task task = new Task();
        task.setUserId("abc");
        task.setName("A new task");
        task.setDueDate(LocalDate.now());

        StepVerifier.create(dashboardService.computeScoreOnTaskAdd(task))
                .expectSubscription()
                .expectErrorMessage("User records not found")
                .verify();


    }


}