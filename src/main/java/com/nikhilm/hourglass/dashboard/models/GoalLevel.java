package com.nikhilm.hourglass.dashboard.models;

import com.fasterxml.jackson.annotation.JsonValue;

public enum GoalLevel {

   EASY("Easy"), MODERATE("Moderate"), EXTREME("Extreme");

   private String level;

   GoalLevel(String level)   {
      this.level = level;
   }

   @JsonValue
   public String getLevel() {
      return this.level;
   }

}
