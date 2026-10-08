package com.sustainai.greenops.dto.ai;

public record OptimizationAdviceResponse(
        String clusterId,
        String recommendedAction,
        double estimatedKwhSaved,
        double estimatedCarbonAvoidedKg,
        String urgencyLevel,
        String justification,
        boolean isFallbackHeuristic
) {}