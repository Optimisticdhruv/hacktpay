package com.recoverai.domain;
import java.util.List;
public record PolicyResult(boolean approved, String blockedBy, List<PolicyCheck> checks) {}
