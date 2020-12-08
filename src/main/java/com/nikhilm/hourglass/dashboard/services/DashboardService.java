package com.nikhilm.hourglass.dashboard.services;

import ch.qos.logback.classic.spi.IThrowableProxy;
import com.nikhilm.hourglass.dashboard.exceptions.DashboardException;
import com.nikhilm.hourglass.dashboard.models.*;
import com.nikhilm.hourglass.dashboard.repositories.DashboardMetricRepository;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.text.DecimalFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.nikhilm.hourglass.dashboard.models.Operation.GOAL_ADDED;
import static java.util.Comparator.comparing;

@Service
@Slf4j
public class DashboardService {

    @Autowired
    DashboardMetricRepository dashboardMetricRepository;


    private int easyGoalScore;

    private int moderateGoalScore;

    private int extremeGoalScore;

    private int resumeBonus;

    private int deferPenalty;

    @Value("${deferPenalty}")
    public void setDeferPenalty(int deferPenalty)   {
        this.deferPenalty = deferPenalty;
    }
    @Value("${resumeBonus}")
    public void setResumeBonus(int resumeBonus)   {
        this.resumeBonus = resumeBonus;
    }

    @Value("${goal.score.easy}")
    public void setEasyGoalScore(int easyGoalScore) {
        this.easyGoalScore = easyGoalScore;
    }
    @Value("${goal.score.moderate}")
    public void setModerateGoalScore(int moderateGoalScore) {
        this.moderateGoalScore = moderateGoalScore;
    }
    @Value("${goal.score.extreme}")
    public void setExtremeGoalScore(int extremeGoalScore)   {
        this.extremeGoalScore = extremeGoalScore;
    }

    @Autowired
    DashboardMetricToMetricResponseMapper mapper;



    public Mono<DashboardMetric> computeScoreOnGoalAdd(Goal addedGoal) {
         return dashboardMetricRepository.findByUserId(addedGoal.getUserId())
                .map(dashboardMetric -> {
                    long inProgressGoals = dashboardMetric.getTotalGoalsInProgress();
                    DashboardMetric updatedMetric = DashboardMetric.from(dashboardMetric);
                    updatedMetric.setTotalGoalsInProgress(inProgressGoals + 1);
                    log.info("Set total goals in progress " + updatedMetric.getTotalGoalsInProgress());
                    return computeGoalSummary(updatedMetric, addedGoal, GOAL_ADDED, null);

                })
                 .switchIfEmpty(Mono.defer(()->Mono.error(new DashboardException(404, "User records not found!"))))
                 .flatMap(dashboardMetricRepository::save);

    }

    private LocalDate findMetricSummaryKey(LocalDate date) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-yyyy");
        LocalDate summaryDate = LocalDate.of(date.getYear(), date.getMonth(), 2);
        log.info("Summary date " + summaryDate);
        return summaryDate;
    }

    private double computeAverageScore(List<MetricSummary> metricSummaryList) {
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        long totalScore = metricSummaryList.stream()
                .map(metricSummary -> metricSummary.getScore())
                .reduce((aLong, aLong2) -> aLong + aLong2).orElse(0L);
        log.info("Metric summary " + metricSummaryList + " Size " + metricSummaryList.size());
        log.info("total score " + totalScore);
        return Double.parseDouble(decimalFormat.format(totalScore / metricSummaryList.size()));
    }

    private DashboardMetric computeGoalSummary(DashboardMetric dashboardMetric, Goal goal, Operation operation, Optional<Long> score) {
        DashboardMetric metric = DashboardMetric.from(dashboardMetric);
        MetricSummary metricSummary = null;
        List<MetricSummary> metricSummaries = null;

        switch (operation) {
            case GOAL_ADDED:
                metricSummary = metric.getMetricSummaryList().stream()
                        .filter(summary -> summary.getMonth().isEqual(findMetricSummaryKey(goal.getDueDate())))
                        .findAny().orElse(new MetricSummary(findMetricSummaryKey(goal.getDueDate())));
                metricSummary.setGoalsPlanned(metricSummary.getGoalsPlanned() + 1);
                metricSummaries = metric.getMetricSummaryList()
                        .stream()
                        .filter(summary ->
                                !summary.getMonth().isEqual(findMetricSummaryKey(goal.getDueDate()))
                        ).collect(Collectors.toList());
                metricSummaries.add(metricSummary);
                log.info("Metric summary " + metricSummary);
                metric.setMetricSummaryList(metricSummaries);

                break;
            case GOAL_COMPLETED:
                metricSummary = metric.getMetricSummaryList().stream()
                        .filter(summary -> summary.getMonth().isEqual(findMetricSummaryKey(
                                LocalDate.now())))
                        .findAny().orElse(new MetricSummary(findMetricSummaryKey(LocalDate.now())));
                metricSummary.setGoalsAccomplished(metricSummary.getGoalsAccomplished() + 1);
                metricSummary.setScore(metricSummary.getScore() + score.orElse(0L));
                metricSummaries = metric.getMetricSummaryList()
                        .stream()
                        .filter(summary ->
                                !summary.getMonth().isEqual(findMetricSummaryKey(goal.getDueDate()))
                        ).collect(Collectors.toList());
                metricSummaries.add(metricSummary);
                metric.setMetricSummaryList(metricSummaries);
                metric.setAverageScore(computeAverageScore(metric.getMetricSummaryList()));
                break;
            case GOAL_DEFERRED:
                metricSummary = metric.getMetricSummaryList().stream()
                        .filter(summary -> summary.getMonth().isEqual(findMetricSummaryKey(goal.getDueDate())))
                        .findAny().orElse(new MetricSummary(goal.getDueDate()));
                metricSummary.setGoalsPlanned(metricSummary.getGoalsPlanned() - 1);
                metricSummary.setScore(metricSummary.getScore() - score.orElse(0L));
                metricSummaries = metric.getMetricSummaryList()
                        .stream()
                        .filter(summary ->
                                !summary.getMonth().isEqual(findMetricSummaryKey(goal.getDueDate()))
                        ).collect(Collectors.toList());
                metricSummaries.add(metricSummary);
                metric.setMetricSummaryList(metricSummaries);
                metric.setAverageScore(computeAverageScore(metric.getMetricSummaryList()));
                break;
            case GOAL_RESUMED:
                metricSummary = metric.getMetricSummaryList().stream()
                        .filter(summary -> summary.getMonth().isEqual(findMetricSummaryKey(goal.getDueDate())))
                        .findAny().orElse(new MetricSummary(findMetricSummaryKey(goal.getDueDate())));
                metricSummary.setGoalsPlanned(metricSummary.getGoalsPlanned() + 1);
                metricSummary.setScore(metricSummary.getScore() + score.orElse(0L));
                metricSummaries = metric.getMetricSummaryList()
                        .stream()
                        .filter(summary ->
                                !summary.getMonth().isEqual(findMetricSummaryKey(goal.getDueDate()))
                        ).collect(Collectors.toList());
                metricSummaries.add(metricSummary);
                metric.setMetricSummaryList(metricSummaries);
                metric.setAverageScore(computeAverageScore(metric.getMetricSummaryList()));
                break;
        }
        return metric;
    }

    public Mono<DashboardMetric> computeScoreOnGoalComplete(Goal completedGoal) {
        return dashboardMetricRepository.findByUserId(completedGoal.getUserId())
                .map(dashboardMetric -> {
                    DashboardMetric updatedMetric = DashboardMetric.from(dashboardMetric);
                    long completedGoals = dashboardMetric.getTotalGoalsCompleted();
                    long scoreToAdd = 0;
                    switch (completedGoal.getLevel()) {
                        case EASY:
                            scoreToAdd = easyGoalScore;
                            updatedMetric.setEasyGoalsCompleted(updatedMetric.getEasyGoalsCompleted() + 1);
                            break;
                        case MODERATE:
                            log.info("Completing a moderate goal " + moderateGoalScore);
                            scoreToAdd = moderateGoalScore;
                            updatedMetric.setModerateGoalsCompleted(updatedMetric.getModerateGoalsCompleted() + 1);
                            break;
                        case EXTREME:
                            scoreToAdd = extremeGoalScore;
                            updatedMetric.setExtremeGoalsCompleted(updatedMetric.getExtremeGoalsCompleted() + 1);
                            break;
                    }
                    //to update
                    if (completedGoal.getDueDate().isBefore(LocalDate.now())) {
                        long elapsed = ChronoUnit.DAYS.between(completedGoal.getDueDate(), LocalDate.now());
                        scoreToAdd = scoreToAdd - elapsed;
                        log.info("scoreToAdd " + scoreToAdd + " elapsed " + elapsed);
                        updatedMetric.setGoalsCompletedAfterTime(updatedMetric.getGoalsCompletedAfterTime() + 1);
                    } else {
                        updatedMetric.setGoalsCompletedOnTime(updatedMetric.getGoalsCompletedOnTime() + 1);
                    }
                    updatedMetric.setGoalScore(dashboardMetric.getGoalScore() + scoreToAdd);
                    updatedMetric.setTotalGoalsCompleted(completedGoals + 1);
                    updatedMetric.setTotalGoalsInProgress(updatedMetric.getTotalGoalsInProgress() - 1);
                    return computeGoalSummary(updatedMetric, completedGoal, Operation.GOAL_COMPLETED, Optional.of(scoreToAdd));
                })
                .switchIfEmpty(Mono.defer(()->Mono.error(new DashboardException(404, "User records not found!"))))
                .flatMap(dashboardMetricRepository::save);

    }

    public Mono<DashboardMetric> computeScoreOnGoalDeferred(Goal deferredGoal) {
        return dashboardMetricRepository.findByUserId(deferredGoal.getUserId())
                .map(dashboardMetric -> {
                    DashboardMetric updatedMetric = DashboardMetric.from(dashboardMetric);
                    long deferredGoals = dashboardMetric.getTotalGoalsDeferred();
                    long scoreToDecrement = deferPenalty;

                    if (deferredGoal.getDueDate().isBefore(LocalDate.now())) {
                        long elapsed = ChronoUnit.DAYS.between(deferredGoal.getDueDate(), LocalDate.now());
                        scoreToDecrement = scoreToDecrement + elapsed;
                    }
                    updatedMetric.setGoalScore(dashboardMetric.getGoalScore() - scoreToDecrement);
                    updatedMetric.setTotalGoalsDeferred(deferredGoals + 1);
                    updatedMetric.setTotalGoalsInProgress(updatedMetric.getTotalGoalsInProgress() - 1);
                    return computeGoalSummary(updatedMetric, deferredGoal, Operation.GOAL_DEFERRED, Optional.of(scoreToDecrement));
                })
                .switchIfEmpty(Mono.defer(()->Mono.error(new DashboardException(404, "User records not found"))))
                .flatMap(dashboardMetricRepository::save);

    }

    public Mono<DashboardMetric> computeScoreOnGoalResumed(Goal resumedGoal) {
        return dashboardMetricRepository.findByUserId(resumedGoal.getUserId())
                .map(dashboardMetric -> {
                    DashboardMetric updatedMetric = DashboardMetric.from(dashboardMetric);
                    long inProgressGoals = dashboardMetric.getTotalGoalsInProgress();
                    long scoreToAdd = resumeBonus;

                    if (resumedGoal.getDueDate().isBefore(LocalDate.now())) {
                        long elapsed = ChronoUnit.DAYS.between(resumedGoal.getDueDate(), LocalDate.now());
                        scoreToAdd = scoreToAdd - elapsed;
                    }
                    updatedMetric.setGoalScore(dashboardMetric.getGoalScore() + scoreToAdd);
                    updatedMetric.setTotalGoalsInProgress(inProgressGoals + 1);
                    updatedMetric.setTotalGoalsDeferred(updatedMetric.getTotalGoalsDeferred() - 1);

                    return computeGoalSummary(updatedMetric, resumedGoal, Operation.GOAL_RESUMED, Optional.of(scoreToAdd));
                })
                .switchIfEmpty(Mono.defer(()->Mono.error(new DashboardException(404, "User records not found"))))
                .flatMap(dashboardMetricRepository::save);

    }

    public Mono<DashboardMetric> computeScoreOnTaskAdd(Task addedTask) {
        return dashboardMetricRepository.findByUserId(addedTask.getUserId())
                .map(dashboardMetric -> {
                    DashboardMetric updatedMetric = DashboardMetric.from(dashboardMetric);
                    MetricSummary metricSummary = updatedMetric.getMetricSummaryList().stream()
                            .filter(summary -> summary.getMonth().isEqual(findMetricSummaryKey(addedTask.getDueDate())))
                            .findAny().orElse(new MetricSummary(findMetricSummaryKey(addedTask.getDueDate())));
                    metricSummary.setTasksPlanned(metricSummary.getTasksPlanned() + 1);
                    log.info("Metricsummary " + metricSummary);
                    List<MetricSummary> metricSummaries = updatedMetric.getMetricSummaryList()
                                                            .stream()
                                                            .filter(summary ->
                                                                !summary.getMonth().isEqual(findMetricSummaryKey(addedTask.getDueDate()))
                                                            ).collect(Collectors.toList());
                    metricSummaries.add(metricSummary);
                    updatedMetric.setMetricSummaryList(metricSummaries);
                    log.info("Dashboardmetric " + dashboardMetric);
                    return updatedMetric;
                })
                .switchIfEmpty(Mono.defer(()->Mono.error(new DashboardException(404, "User records not found"))))
                .flatMap(dashboardMetricRepository::save);
    }

    public Mono<DashboardMetric> computeScoreOnTaskComplete(Task completedTask) {
        return dashboardMetricRepository.findByUserId(completedTask.getUserId())
                .map(dashboardMetric -> {
                    DashboardMetric updatedMetric = DashboardMetric.from(dashboardMetric);
                    MetricSummary metricSummary = updatedMetric.getMetricSummaryList().stream()
                            .filter(summary -> summary.getMonth().isEqual(findMetricSummaryKey(LocalDate.now())))
                            .findAny().orElse(new MetricSummary(findMetricSummaryKey(LocalDate.now())));
                    metricSummary.setTasksCompleted(metricSummary.getTasksCompleted() + 1);
                    List<MetricSummary> metricSummaries = updatedMetric.getMetricSummaryList()
                            .stream()
                            .filter(summary ->
                                    !summary.getMonth().isEqual(findMetricSummaryKey(LocalDate.now()))
                            ).collect(Collectors.toList());
                    metricSummaries.add(metricSummary);
                    updatedMetric.setMetricSummaryList(metricSummaries);
                    return updatedMetric;
                })
                .switchIfEmpty(Mono.defer(()->Mono.error(new DashboardException(404, "User records not found"))))
                .flatMap(dashboardMetricRepository::save);

    }

    public Mono<MetricResponse> getMetrics(String userId) {

        LocalDate currentMonth = LocalDate.now();

        return dashboardMetricRepository.findByUserId(userId)
                .map(dashboardMetric -> {
                    List<GoalTrend> trends = new ArrayList<>();
                    trends.addAll(
                    dashboardMetric.getMetricSummaryList()
                            .stream()
                            .filter(this::includeMetric)
                            .sorted(comparing(summary -> summary.getMonth().getMonth().getValue()))
                            .map(summary -> {
                                return new GoalTrend
                                        (summary.getMonth().getMonth()
                                                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                                                summary.getScore());

                            })
                            .collect(Collectors.toList()));
                    MetricResponse metricResponse = mapper.dashboardMetricToMetricResponse(dashboardMetric);
                    metricResponse.setTrends(trends);
                    MetricSummary currentSummary = dashboardMetric.getMetricSummaryList().stream()
                            .filter(summary -> {
                                LocalDate monthStarting = LocalDate.of(currentMonth.getYear(), currentMonth.getMonth(), 2);
                                return summary.getMonth().isEqual(monthStarting);
                    }).findAny().orElse(new MetricSummary());
                    metricResponse.setTasksPlanned(currentSummary.getTasksPlanned());
                    metricResponse.setTasksCompleted(currentSummary.getTasksCompleted());
                    metricResponse.setGoalsPlanned(currentSummary.getGoalsPlanned());
                    metricResponse.setGoalsAccomplished(currentSummary.getGoalsAccomplished());
                    metricResponse.setFallback(getFallbackValue(dashboardMetric));
                    metricResponse.setActiveMonth(LocalDate.of(currentMonth.getYear(), currentMonth.getMonthValue(), 2));
                    return metricResponse;
                })
                .switchIfEmpty(Mono.defer(()->Mono.error(new DashboardException(404, "User records not found"))));

    }

    private String getFallbackValue(DashboardMetric dashboardMetric) {

        return (dashboardMetric.getMetricSummaryList().stream()
                .anyMatch(summary -> summary.getGoalsPlanned() > 0 || summary.getTasksPlanned() > 0))
                ? "" : "Please start adding goals and tasks!";
    }

    public Mono<DashboardMetric> initMetrics(String userId)  {
        return initMetricsForUser(userId);

    }

    private boolean includeMetric(MetricSummary metricSummary) {
        return LocalDate.now().minusMonths(3).isBefore(metricSummary.getMonth());


    }


    private Mono<DashboardMetric> initMetricsForUser(String userId)    {
        DashboardMetric dashboardMetric = new DashboardMetric();
        dashboardMetric.setUserId(userId);
        log.info("saving metric " + dashboardMetric.getUserId());
        return dashboardMetricRepository.save(dashboardMetric)
                .doOnError((throwable) -> {
                    log.error("exception occured!");
                    throw  new DashboardException(500, "Internal server error!");
                });

    }

    public void initializeUser(Event event) {
        initMetricsForUser(event.getKey().toString()).block();

    }

}

