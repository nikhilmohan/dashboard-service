package com.nikhilm.hourglass.dashboard.resource;

import com.nikhilm.hourglass.dashboard.models.DashboardMetric;
import com.nikhilm.hourglass.dashboard.models.MetricResponse;
import com.nikhilm.hourglass.dashboard.services.DashboardService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;

@WebFluxTest
class DashboardResourceTest {

    @MockBean
    DashboardService dashboardService;


    @Autowired
    WebTestClient webTestClient;

    @Test
    public void testGetMetrics() {
        MetricResponse metricResponse = new MetricResponse();
        metricResponse.setGoalsPlanned(10);
        metricResponse.setGoalsAccomplished(5);

        Mockito.when(dashboardService.getMetrics("abc")).thenReturn(Mono.just(metricResponse));
        MetricResponse response = webTestClient.get().uri("http://localhost:9060/metrics")
                .header("user", "abc")
                .exchange()
                .expectBody(MetricResponse.class)
                .returnResult()
                .getResponseBody();

        assertTrue(response.getGoalsAccomplished() == 5 && response.getGoalsPlanned() == 10);

    }
    @Test
    public void testGetMetricsError() {
        MetricResponse metricResponse = new MetricResponse();
        metricResponse.setGoalsPlanned(10);
        metricResponse.setGoalsAccomplished(5);

        Mockito.when(dashboardService.getMetrics("abc")).thenReturn(Mono.error(new RuntimeException()));
        webTestClient.get().uri("http://localhost:9060/metrics")
                .header("user", "abc")
                .exchange()
                .expectStatus()
                .is5xxServerError();

    }
    @Test
    public void testinitMetrics() {
        DashboardMetric dashboardMetric = new DashboardMetric();
        dashboardMetric.setUserId("abc");

        Mockito.when(dashboardService.initMetrics("abc")).thenReturn(Mono.just(dashboardMetric));
        webTestClient.post().uri("http://localhost:9060/metrics/abc")
                .header("user", "abc")
                .exchange()
                .expectStatus().isOk();
    }
    @Test
    public void testinitMetricsForbidden() {
        webTestClient.post().uri("http://localhost:9060/metrics/abc")
                .header("user", "zyx")
                .exchange()
                .expectStatus().isForbidden();
    }
    @Test
    public void testinitMetricsError() {
        Mockito.when(dashboardService.initMetrics("abc")).thenReturn(Mono.error(new RuntimeException()));

        webTestClient.post().uri("http://localhost:9060/metrics/abc")
                .header("user", "zyx")
                .exchange()
                .expectStatus().isForbidden();
    }




}