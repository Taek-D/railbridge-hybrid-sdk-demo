package com.demo.railbridge.sdk;

import java.util.concurrent.atomic.AtomicReference;

public class ScenarioController {

    private final AtomicReference<ScenarioPreset> activePreset;

    public ScenarioController() {
        this(ScenarioPreset.NORMAL);
    }

    public ScenarioController(ScenarioPreset initialPreset) {
        activePreset = new AtomicReference<>(sanitize(initialPreset));
    }

    public ScenarioPreset getActivePreset() {
        return activePreset.get();
    }

    public void setActivePreset(ScenarioPreset preset) {
        activePreset.set(sanitize(preset));
    }

    public void setActivePreset(String presetValue) {
        setActivePreset(ScenarioPreset.fromValue(presetValue));
    }

    private ScenarioPreset sanitize(ScenarioPreset preset) {
        return preset == null ? ScenarioPreset.NORMAL : preset;
    }
}
