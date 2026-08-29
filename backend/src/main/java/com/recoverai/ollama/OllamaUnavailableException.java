package com.recoverai.ollama;

public class OllamaUnavailableException extends RuntimeException {
    private final String reason;

    public OllamaUnavailableException(String reason, String message) {
        super(message);
        this.reason = reason;
    }

    public OllamaUnavailableException(String reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
