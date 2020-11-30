package com.nikhilm.hourglass.dashboard.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class MetricSummary {
    private LocalDate month;
    private long score = 0;
    private long goalsPlanned = 0;
    private long goalsAccomplished = 0;
    private long tasksPlanned = 0;
    private long tasksCompleted = 0;
    public MetricSummary(LocalDate month)   {
        this.month = month;
    }
}
