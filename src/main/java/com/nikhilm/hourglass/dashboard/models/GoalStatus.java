package com.nikhilm.hourglass.dashboard.models;

import com.fasterxml.jackson.annotation.JsonValue;

public enum GoalStatus {
    ACTIVE("A"), DEFERRED("D"), COMPLETED("C");
    private String value;

    GoalStatus(String value)  {
        this.value = value;
    }

    @JsonValue
    public String getValue()  {
        return this.value;
    }
}
