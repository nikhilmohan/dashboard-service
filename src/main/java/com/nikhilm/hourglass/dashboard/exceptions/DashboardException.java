package com.nikhilm.hourglass.dashboard.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardException extends RuntimeException{
    private int status;
    private String message;
}
