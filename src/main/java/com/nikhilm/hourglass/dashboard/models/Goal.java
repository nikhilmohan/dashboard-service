package com.nikhilm.hourglass.dashboard.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Goal {

    private String id;
    private String userId;
    private String name;
    private String description;
    private List<String> notes;
    private GoalLevel level;
    private LocalDate completedOn;
    private GoalStatus status = GoalStatus.ACTIVE;
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate dueDate;
    private int votes;
}

