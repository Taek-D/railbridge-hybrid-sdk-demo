package com.demo.railbridge.sdk;

import java.util.Locale;

public enum ScenarioPreset {
    NORMAL("normal"),
    TIMEOUT("timeout"),
    INTERNAL_ERROR("internal_error"),
    CALLBACK_LOSS("callback_loss"),
    DUPLICATE_CALLBACK("duplicate_callback"),
    RETRY_EXHAUSTED("retry_exhausted");

    private final String value;

    ScenarioPreset(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ScenarioPreset fromValue(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return NORMAL;
        }

        String normalized = rawValue.trim()
                .toLowerCase(Locale.US)
                .replace('-', '_')
                .replace(' ', '_');

        for (ScenarioPreset preset : values()) {
            if (preset.value.equals(normalized) || preset.name().toLowerCase(Locale.US).equals(normalized)) {
                return preset;
            }
        }

        return NORMAL;
    }
}
