package com.recoverai.controller;

import com.recoverai.evaluation.EvaluationService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evaluation")
@CrossOrigin(origins = "${recoverai.frontend-url}")
public class EvaluationController {
    private final EvaluationService evaluations;

    public EvaluationController(EvaluationService evaluations) { this.evaluations = evaluations; }

    @PostMapping("/run")
    public EvaluationService.EvaluationResult run(@RequestBody(required = false) EvaluationRequest request) {
        int size = request == null || request.datasetSize() == null ? 240 : request.datasetSize();
        long seed = request == null || request.seed() == null ? 42L : request.seed();
        return evaluations.run(size, seed);
    }

    public record EvaluationRequest(Integer datasetSize, Long seed) {}
}
