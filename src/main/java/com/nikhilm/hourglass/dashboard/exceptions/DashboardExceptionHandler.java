package com.nikhilm.hourglass.dashboard.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class DashboardExceptionHandler {

    @ExceptionHandler(DashboardException.class)
    public ResponseEntity<ApiError> handleGoalException(DashboardException e) {
        log.error("Exception " + e.getMessage());
        return ResponseEntity.status(e.getStatus()).body(new ApiError(String.valueOf(e.getStatus()), e.getMessage()));
    }




}
