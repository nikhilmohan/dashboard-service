package com.nikhilm.hourglass.dashboard.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

//goalTarget: 50,
//        goalAccomplished: 28,
//        taskTarget: 4,
//        taskCompleted: 1,
//        totalGoalsCompleted: 54,
//        totalGoalsInProgress: 25,
//        totalGoalsDeferred: 18,
//        goalsCompletedOnTime: 173,
//        goalsCompletedAfterTime: 29,
//        easyGoalsCompleted: 56,
//        moderateGoalsCompleted: 117,
//        extremeGoalsCompleted: 29,
//        GoalTrendJan: 28,
//        GoalTrendFeb: 24,
//        GoalTrendMarch: 33

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Document(collection = "metrics")
public class DashboardMetric {
    @Id
    private String id;
    private String userId;
    private long goalScore = 0;
    private double averageScore = 0.0;
    private long totalGoalsCompleted = 0;
    private long totalGoalsInProgress = 0;
    private long totalGoalsDeferred = 0;
    private long goalsCompletedOnTime = 0;
    private long goalsCompletedAfterTime = 0;
    private long easyGoalsCompleted = 0;
    private long moderateGoalsCompleted = 0;
    private long extremeGoalsCompleted = 0;
    private List<MetricSummary> metricSummaryList = new ArrayList<>();

    public static DashboardMetric from(DashboardMetric source) {
        return new DashboardMetric(source.id,source.userId, source.goalScore, source.averageScore,
                source.totalGoalsCompleted, source.totalGoalsInProgress, source.totalGoalsDeferred,
                source.goalsCompletedOnTime, source.goalsCompletedAfterTime, source.easyGoalsCompleted,
                source.moderateGoalsCompleted, source.extremeGoalsCompleted, source.metricSummaryList);
    }

}
