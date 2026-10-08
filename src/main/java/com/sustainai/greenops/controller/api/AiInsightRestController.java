package com.sustainai.greenops.controller.api;

import com.sustainai.greenops.dto.ai.ClusterTelemetryDto;
import com.sustainai.greenops.dto.ai.OptimizationAdviceResponse;
import com.sustainai.greenops.service.ai.SustainabilityAiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiInsightRestController {

    private final SustainabilityAiService aiService;

    public AiInsightRestController(SustainabilityAiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<OptimizationAdviceResponse> analyze(@RequestBody ClusterTelemetryDto telemetry) {
        OptimizationAdviceResponse advice = aiService.generateAdvice(telemetry);
        return ResponseEntity.ok(advice);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "GreenOps AI Recommendation Engine"
        ));
    }
}