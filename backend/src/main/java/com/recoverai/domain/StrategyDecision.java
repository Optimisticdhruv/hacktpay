package com.recoverai.domain;
import java.util.List;
public record StrategyDecision(String diagnosis, double recoverabilityScore, RecoveryAction recommendedAction,
                               int delayMinutes, List<String> reasons, boolean fallbackUsed) {}
