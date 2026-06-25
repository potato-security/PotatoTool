package com.potato.potatotool.content.redTeam.payload.detector;

public class DetectorPayloadRequest {
    private final DetectorType detectorType;
    private final String className;
    private final String dnsDomain;
    private final String httpBaseUrl;
    private final int sleepSeconds;
    private final String serverType;

    public DetectorPayloadRequest(DetectorType detectorType,
                                  String className,
                                  String dnsDomain,
                                  String httpBaseUrl,
                                  int sleepSeconds,
                                  String serverType) {
        this.detectorType = detectorType;
        this.className = className;
        this.dnsDomain = dnsDomain;
        this.httpBaseUrl = httpBaseUrl;
        this.sleepSeconds = sleepSeconds;
        this.serverType = serverType;
    }

    public DetectorType getDetectorType() {
        return detectorType;
    }

    public String getClassName() {
        return className;
    }

    public String getDnsDomain() {
        return dnsDomain;
    }

    public String getHttpBaseUrl() {
        return httpBaseUrl;
    }

    public int getSleepSeconds() {
        return sleepSeconds;
    }

    public String getServerType() {
        return serverType;
    }
}
