package com.potato.potatotool.utils.browser;

/**
 * Lightweight page snapshot used by callers that need current URL and DOM HTML.
 */
public final class CdpPageSnapshot {
    private final String url;
    private final String html;

    public CdpPageSnapshot(String url, String html) {
        this.url = url == null ? "" : url;
        this.html = html == null ? "" : html;
    }

    public String getUrl() {
        return url;
    }

    public String getHtml() {
        return html;
    }
}
