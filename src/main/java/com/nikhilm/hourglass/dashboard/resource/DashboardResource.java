package com.nikhilm.hourglass.dashboard.resource;

import com.nikhilm.hourglass.dashboard.exceptions.ApiError;
import com.nikhilm.hourglass.dashboard.exceptions.DashboardException;
import com.nikhilm.hourglass.dashboard.models.MetricResponse;
import com.nikhilm.hourglass.dashboard.services.DashboardService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@OpenAPIDefinition(
        info = @Info(
                title = "Dashboard service API",
                version = "1.0",
                description = "API for managing user statistics regarding goals and tasks",
                contact = @Contact(name = "Nikhil Mohan", email = "nikmohan81@gmail.com")
        )
)
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

    @Operation(summary = "List all goal/ task metrics for the user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metrics data received for user",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MetricResponse.class)) }),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class)) })})
    @GetMapping("/metrics")
    public Mono<MetricResponse> getMetrics(@RequestHeader("user") String user)  {
        log.info("Request received " + user);
        return rcb.run(dashboardService.getMetrics(user),
                throwable -> Mono.error(new DashboardException(500, "Internal server error!")));
    }

    @Operation(summary = "Initialize metrics resource for user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metrics resource initialized",
                    content = { @Content }),
            @ApiResponse(responseCode = "403", description = "Forbidden access",
                    content = { @Content})})
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
