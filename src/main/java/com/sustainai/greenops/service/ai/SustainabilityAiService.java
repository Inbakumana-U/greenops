package com.sustainai.greenops.service.ai;

import com.sustainai.greenops.dto.ai.ClusterTelemetryDto;
import com.sustainai.greenops.dto.ai.OptimizationAdviceResponse;

public interface SustainabilityAiService {
    OptimizationAdviceResponse generateAdvice(ClusterTelemetryDto telemetry);
}