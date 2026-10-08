package com.sustainai.greenops.service.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sustainai.greenops.dto.ai.ClusterTelemetryDto;
import com.sustainai.greenops.dto.ai.OptimizationAdviceResponse;
import com.sustainai.greenops.service.ai.SustainabilityAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class SustainabilityAiServiceImpl implements SustainabilityAiService {

    private static final Logger log = LoggerFactory.getLogger(SustainabilityAiServiceImpl.class);

    private final RestClient aiRestClient;
    private final ObjectMapper objectMapper;

    @Value("${greenops.ai.model:gpt-3.5-turbo}")
    private String modelName;

    @Value("${greenops.ai.api-key:}")
    private String apiKey;

    public SustainabilityAiServiceImpl(RestClient aiRestClient, ObjectMapper objectMapper) {
        this.aiRestClient = aiRestClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public OptimizationAdviceResponse generateAdvice(ClusterTelemetryDto telemetry) {
        // If API key is not configured, immediately use deterministic GreenOps heuristics
        if (apiKey == null || apiKey.isBlank()) {
            return generateHeuristicAdvice(telemetry, "No AI API key supplied. Running local deterministic GreenOps rules.");
        }

        try {
            String systemPrompt = """
                You are an enterprise GreenOps Cloud Sustainability Engineer.
                Analyze cluster telemetry and output ONLY a JSON object with keys:
                "recommendedAction" (string),
                "estimatedKwhSaved" (number),
                "estimatedCarbonAvoidedKg" (number),
                "urgencyLevel" ("LOW"|"MEDIUM"|"CRITICAL"),
                "justification" (string).
                Do not include markdown blocks or extra text.
                """;

            String userContent = String.format(
                    "Cluster: %s, Region: %s, CPU Load: %.2f%%, Power Draw: %.2f W, Grid Carbon Intensity: %.2f gCO2/kWh",
                    telemetry.clusterId(), telemetry.region(), telemetry.cpuUtilizationPercent(),
                    telemetry.powerDrawWatts(), telemetry.gridCarbonIntensityGramsPerKwh()
            );

            Map<String, Object> requestBody = Map.of(
                    "model", modelName,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userContent)
                    ),
                    "temperature", 0.2
            );

            String rawResponse = aiRestClient.post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(rawResponse);
            String aiMessage = root.path("choices").get(0).path("message").path("content").asText();

            JsonNode parsed = objectMapper.readTree(aiMessage);

            return new OptimizationAdviceResponse(
                    telemetry.clusterId(),
                    parsed.path("recommendedAction").asText("Scale down idle instances"),
                    parsed.path("estimatedKwhSaved").asDouble(12.5),
                    parsed.path("estimatedCarbonAvoidedKg").asDouble(4.2),
                    parsed.path("urgencyLevel").asText("MEDIUM"),
                    parsed.path("justification").asText(aiMessage),
                    false
            );

        } catch (Exception ex) {
            log.warn("External AI call failed or timed out. Falling back to local heuristics: {}", ex.getMessage());
            return generateHeuristicAdvice(telemetry, "Fallback active: " + ex.getMessage());
        }
    }

    private OptimizationAdviceResponse generateHeuristicAdvice(ClusterTelemetryDto t, String note) {
        String action;
        String urgency;
        double kwhSaved;

        if (t.cpuUtilizationPercent() < 20.0) {
            action = "Consolidate workloads and place node " + t.clusterId() + " into deep sleep / eco-standby.";
            urgency = "HIGH";
            kwhSaved = (t.powerDrawWatts() * 0.70 * 24.0) / 1000.0; // 70% reduction over 24h
        } else if (t.cpuUtilizationPercent() > 85.0) {
            action = "Thermal throttle warning: Shift peak batch jobs to off-peak carbon-light regions.";
            urgency = "CRITICAL";
            kwhSaved = (t.powerDrawWatts() * 0.15 * 6.0) / 1000.0;
        } else {
            action = "Maintain dynamic voltage and frequency scaling (DVFS); metrics within optimal green envelope.";
            urgency = "LOW";
            kwhSaved = 2.4;
        }

        double carbonAvoided = (kwhSaved * t.gridCarbonIntensityGramsPerKwh()) / 1000.0;

        return new OptimizationAdviceResponse(
                t.clusterId(),
                action,
                Math.round(kwhSaved * 100.0) / 100.0,
                Math.round(carbonAvoided * 100.0) / 100.0,
                urgency,
                note,
                true
        );
    }
}