package com.nikhilm.hourglass.dashboard.services;

import com.nikhilm.hourglass.dashboard.models.DashboardMetric;
import com.nikhilm.hourglass.dashboard.models.MetricResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface DashboardMetricToMetricResponseMapper {
    @Mappings({
            @Mapping(target="goalsPlanned", ignore = true),
            @Mapping(target="goalsAccomplished", ignore = true),
            @Mapping(target="tasksPlanned", ignore = true),
            @Mapping(target="tasksCompleted", ignore = true),
            @Mapping(target="trends", ignore = true)
    })
    MetricResponse dashboardMetricToMetricResponse(DashboardMetric metric);
}
