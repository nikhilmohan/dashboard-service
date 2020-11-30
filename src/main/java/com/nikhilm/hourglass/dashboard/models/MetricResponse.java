package com.nikhilm.hourglass.dashboard.models;

import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class MetricResponse {
    private long goalScore;
    private double averageScore;
    private long totalGoalsCompleted;
    private long totalGoalsInProgress;
    private long totalGoalsDeferred;
    private long goalsCompletedOnTime;
    private long goalsCompletedAfterTime;
    private long easyGoalsCompleted;
    private long moderateGoalsCompleted;
    private long extremeGoalsCompleted;
    private long goalsPlanned;
    private long goalsAccomplished;
    private long tasksPlanned;
    private long tasksCompleted;
    private List <GoalTrend> trends = new ArrayList<>();
    private LocalDate activeMonth;
    private String fallback;
}
