package com.potato.potatotool.utils.browser;

/**
 * Minimal metadata about a DevTools target tab/page.
 */
public final class CdpTargetInfo {
    private final String id;
    private final String type;
    private final String title;
    private final String url;
    private final String webSocketDebuggerUrl;

    public CdpTargetInfo(String id, String type, String title, String url, String webSocketDebuggerUrl) {
        this.id = normalize(id);
        this.type = normalize(type);
        this.title = normalize(title);
        this.url = normalize(url);
        this.webSocketDebuggerUrl = normalize(webSocketDebuggerUrl);
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getWebSocketDebuggerUrl() {
        return webSocketDebuggerUrl;
    }

    public boolean isPage() {
        return "page".equalsIgnoreCase(type);
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }
}
