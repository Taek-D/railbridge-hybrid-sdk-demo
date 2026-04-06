package com.demo.railbridge.sdk;

public class SdkStatusSnapshot {

    private final boolean initialized;
    private final String version;

    public SdkStatusSnapshot(boolean initialized, String version) {
        this.initialized = initialized;
        this.version = version;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public String getVersion() {
        return version;
    }
}
