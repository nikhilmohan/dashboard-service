package com.nikhilm.hourglass.dashboard.repositories;

import com.nikhilm.hourglass.dashboard.models.DashboardMetric;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface DashboardMetricRepository extends ReactiveMongoRepository<DashboardMetric, String> {

    public Mono<DashboardMetric> findByUserId(String userId);
}
