package com.sustainai.greenops.dto.ai;

public record ClusterTelemetryDto(
        String clusterId,
        String region,
        double cpuUtilizationPercent,
        double powerDrawWatts,
        double gridCarbonIntensityGramsPerKwh
) {}