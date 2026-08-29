package com.recoverai.domain;

import com.recoverai.evaluation.EvaluationService;
import java.time.Instant;

/** Persisted synthetic evaluation; never represents a Razorpay transaction. */
public record EvaluationRun(String id, EvaluationService.EvaluationResult result, Instant createdAt) {}
