package com.nikhilm.hourglass.dashboard.resource;

import com.nikhilm.hourglass.dashboard.exceptions.DashboardException;
import com.nikhilm.hourglass.dashboard.models.DashboardMetric;
import com.nikhilm.hourglass.dashboard.models.MetricResponse;
import com.nikhilm.hourglass.dashboard.services.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class DashboardResource {

    @Autowired
    DashboardService dashboardService;

    @Autowired
    ReactiveCircuitBreakerFactory factory;

    ReactiveCircuitBreaker rcb;

    public DashboardResource(ReactiveCircuitBreakerFactory factory)  {
        this.factory = factory;
        rcb = factory.create("dashboard");
    }

    @GetMapping("/metrics")
    public Mono<MetricResponse> getMetrics(@RequestHeader("user") String user)  {
        log.info("Request received " + user);
        return rcb.run(dashboardService.getMetrics(user),
                throwable -> Mono.error(new DashboardException(500, "Internal server error!")));
    }

    @PostMapping("/metrics/{userId}")
    public Mono<ResponseEntity<Object>> initMetrics(@PathVariable("userId") String userId,
                                                    @RequestHeader("user") String user)   {
        log.info("init metrics for " + userId);
        if (!user.equalsIgnoreCase(userId)) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
        }
        return dashboardService.initMetrics(userId)
                .map(dashboardMetric -> ResponseEntity.ok().build())
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
}
