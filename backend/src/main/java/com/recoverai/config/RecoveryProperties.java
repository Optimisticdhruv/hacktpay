package com.recoverai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "recoverai")
public record RecoveryProperties(String storageMode, boolean publicWebhookOnly, Firebase firebase, Razorpay razorpay, Recovery recovery, Thresholds thresholds, Ollama ollama, String frontendUrl) {
    public record Firebase(String projectId, String serviceAccountPath) {}
    public record Razorpay(String keyId, String keySecret, String webhookSecret) {
        public boolean isConfigured() { return keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank(); }
    }
    public record Recovery(int maxAttempts, int paymentLinkExpiryHours, boolean aiEnabled) {}
    public record Thresholds(int minimumSuccessfulPaymentsForPaymentLink, double minimumPaymentFailureScore,
                             double minimumAbandonmentScore, double minimumOverdueScore,
                             long maximumRecoveryAmountPaise) {}
    public record Ollama(boolean enabled, String baseUrl, String model, int timeoutSeconds) {}
}
