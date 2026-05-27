package com.potato.potatotool.content.redTeam.portScanner.model;

public class PortResult {
    private String host;
    private int port;
    private PortState state;
    private String service;
    private String banner;
    private boolean tls;
    private long rttMs;
    private long timestamp;

    public PortResult() {
        this.timestamp = System.currentTimeMillis();
    }

    public PortResult(String host, int port, PortState state) {
        this.host = host;
        this.port = port;
        this.state = state;
        this.timestamp = System.currentTimeMillis();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public PortState getState() {
        return state;
    }

    public void setState(PortState state) {
        this.state = state;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getBanner() {
        return banner;
    }

    public void setBanner(String banner) {
        this.banner = banner;
    }

    public boolean isTls() {
        return tls;
    }

    public void setTls(boolean tls) {
        this.tls = tls;
    }

    public long getRttMs() {
        return rttMs;
    }

    public void setRttMs(long rttMs) {
        this.rttMs = rttMs;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String toTargetUrl() {
        String svc = service == null ? "" : service.toLowerCase();
        boolean httpService = svc.equals("http") || svc.startsWith("http-") || svc.equals("http-proxy");
        boolean httpsService = svc.equals("https") || svc.startsWith("https-");
        boolean isHttps = httpsService || (tls && httpService) || (svc.isEmpty() && (port == 443 || port == 8443 || port == 9443));
        boolean isHttp = !isHttps && (
                httpService
                        || (svc.isEmpty() && (port == 80 || port == 81 || port == 3000 || port == 5000
                                || port == 8000 || port == 8008 || port == 8080 || port == 8081 || port == 8888 || port == 9000))
        );
        if (isHttps) {
            return "https://" + host + ":" + port;
        }
        if (isHttp) {
            return "http://" + host + ":" + port;
        }
        return host + ":" + port;
    }
}
