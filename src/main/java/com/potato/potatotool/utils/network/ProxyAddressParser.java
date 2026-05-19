package com.potato.potatotool.utils.network;

import java.net.Proxy;
import java.net.URI;

/**
 * 统一解析代理地址，支持协议、凭据与 host:port 简写。
 */
public final class ProxyAddressParser {

    private ProxyAddressParser() {
    }

    public static final class ParsedProxyAddress {
        private final String scheme;
        private final String host;
        private final int port;
        private final String username;
        private final String password;

        ParsedProxyAddress(String scheme, String host, int port, String username, String password) {
            this.scheme = scheme;
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
        }

        public String getScheme() {
            return scheme;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public boolean hasCredentials() {
            return username != null && !username.isEmpty();
        }

        public Proxy.Type toJavaProxyType(String proxyTypeHint) {
            if ("SOCKS".equalsIgnoreCase(proxyTypeHint) || "socks".equalsIgnoreCase(scheme)) {
                return Proxy.Type.SOCKS;
            }
            // Java/OkHttp 仅区分 HTTP 与 SOCKS；HTTPS 代理地址在执行链中走 HTTP 代理路由。
            return Proxy.Type.HTTP;
        }
    }

    public static ParsedProxyAddress parse(String address, String proxyTypeHint) {
        if (address == null) {
            return null;
        }

        String trimmed = address.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String normalized = trimmed;
        if (!trimmed.contains("://")) {
            normalized = buildDefaultScheme(proxyTypeHint) + "://" + trimmed;
        }

        try {
            URI uri = new URI(normalized);
            String scheme = uri.getScheme();
            if (!isSupportedScheme(scheme)) {
                return null;
            }

            String host = uri.getHost();
            int port = uri.getPort();
            if (host == null || host.trim().isEmpty() || port <= 0 || port > 65535) {
                return null;
            }

            String username = null;
            String password = null;
            String userInfo = uri.getUserInfo();
            if (userInfo != null && !userInfo.isEmpty()) {
                int separator = userInfo.indexOf(':');
                if (separator >= 0) {
                    username = userInfo.substring(0, separator);
                    password = userInfo.substring(separator + 1);
                } else {
                    username = userInfo;
                    password = "";
                }
            }

            return new ParsedProxyAddress(scheme.toLowerCase(), host, port, username, password);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isSupportedScheme(String scheme) {
        if (scheme == null) {
            return false;
        }
        return "http".equalsIgnoreCase(scheme)
                || "https".equalsIgnoreCase(scheme)
                || "socks".equalsIgnoreCase(scheme);
    }

    private static String buildDefaultScheme(String proxyTypeHint) {
        return "SOCKS".equalsIgnoreCase(proxyTypeHint) ? "socks" : "http";
    }
}
