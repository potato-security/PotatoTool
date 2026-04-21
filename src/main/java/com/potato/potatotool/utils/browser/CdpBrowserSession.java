package com.potato.potatotool.utils.browser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Attached DevTools browser session bound to one selected page target.
 */
public final class CdpBrowserSession implements AutoCloseable {
    private static final int LIST_TARGET_TIMEOUT_MILLIS = 3000;
    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 3000;
    private static final int DEFAULT_SOCKET_READ_TIMEOUT_MILLIS = 10000;
    private static final int DEFAULT_ENABLE_TIMEOUT_MILLIS = 5000;

    private final int devToolsPort;
    private final CdpTargetInfo targetInfo;
    private final CdpClient client;
    private final CdpPage page;

    private CdpBrowserSession(int devToolsPort, CdpTargetInfo targetInfo, CdpClient client) {
        this.devToolsPort = devToolsPort;
        this.targetInfo = targetInfo;
        this.client = client;
        this.page = new CdpPage(client);
    }

    public static CdpBrowserSession attach(int devToolsPort) throws CdpException {
        return attach(devToolsPort, CdpTargetSelector.firstPage());
    }

    public static CdpBrowserSession attach(int devToolsPort, CdpTargetSelector selector) throws CdpException {
        List<CdpTargetInfo> targets = listTargets(devToolsPort);
        CdpTargetSelector effectiveSelector = selector == null ? CdpTargetSelector.firstPage() : selector;
        CdpTargetInfo target = effectiveSelector.select(targets);
        if (target == null) {
            throw new CdpException(CdpErrorType.NO_PAGE_TARGET,
                    "No attachable DevTools page target found on port " + devToolsPort);
        }

        CdpClient client = CdpClient.connect(
                target.getWebSocketDebuggerUrl(),
                DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DEFAULT_SOCKET_READ_TIMEOUT_MILLIS);
        try {
            client.request("Page.enable", null, DEFAULT_ENABLE_TIMEOUT_MILLIS);
            client.request("Runtime.enable", null, DEFAULT_ENABLE_TIMEOUT_MILLIS);
            return new CdpBrowserSession(devToolsPort, target, client);
        } catch (CdpException e) {
            client.close();
            throw e;
        }
    }

    public static List<CdpTargetInfo> listTargets(int devToolsPort) throws CdpException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL("http://127.0.0.1:" + devToolsPort + "/json/list");
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(LIST_TARGET_TIMEOUT_MILLIS);
            connection.setReadTimeout(LIST_TARGET_TIMEOUT_MILLIS);
            connection.setRequestMethod("GET");

            int statusCode = connection.getResponseCode();
            if (statusCode != 200) {
                throw new CdpException(CdpErrorType.INVALID_RESPONSE,
                        "Unexpected DevTools target discovery status on port " + devToolsPort + ": " + statusCode);
            }

            try (InputStream inputStream = connection.getInputStream()) {
                String body = readAll(inputStream);
                JsonElement element = new JsonParser().parse(body);
                if (element == null || !element.isJsonArray()) {
                    throw new CdpException(CdpErrorType.INVALID_RESPONSE,
                            "Unexpected DevTools target discovery payload on port " + devToolsPort);
                }

                JsonArray array = element.getAsJsonArray();
                List<CdpTargetInfo> result = new ArrayList<CdpTargetInfo>();
                for (JsonElement item : array) {
                    if (!item.isJsonObject()) {
                        continue;
                    }
                    JsonObject target = item.getAsJsonObject();
                    result.add(new CdpTargetInfo(
                            readJsonString(target, "id"),
                            readJsonString(target, "type"),
                            readJsonString(target, "title"),
                            readJsonString(target, "url"),
                            readJsonString(target, "webSocketDebuggerUrl")
                    ));
                }
                return result;
            }
        } catch (ConnectException e) {
            throw new CdpException(CdpErrorType.PORT_UNAVAILABLE,
                    "Chrome DevTools port is not reachable: " + devToolsPort, e);
        } catch (SocketTimeoutException e) {
            throw new CdpException(CdpErrorType.PORT_UNAVAILABLE,
                    "Chrome DevTools port did not respond in time: " + devToolsPort, e);
        } catch (CdpException e) {
            throw e;
        } catch (Exception e) {
            throw new CdpException(CdpErrorType.INVALID_RESPONSE,
                    "Failed to query DevTools targets from port " + devToolsPort, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static Map<String, String> readAllCookies(int devToolsPort, CdpTargetSelector selector) throws CdpException {
        try (CdpBrowserSession session = attach(devToolsPort, selector)) {
            return session.getPage().readAllCookies(DEFAULT_ENABLE_TIMEOUT_MILLIS);
        }
    }

    public int getDevToolsPort() {
        return devToolsPort;
    }

    public CdpTargetInfo getTargetInfo() {
        return targetInfo;
    }

    public CdpPage getPage() {
        return page;
    }

    @Override
    public void close() {
        client.close();
    }

    private static String readAll(InputStream inputStream) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = inputStream.read(buffer)) >= 0) {
            baos.write(buffer, 0, len);
        }
        return new String(baos.toByteArray(), "UTF-8");
    }

    private static String readJsonString(JsonObject jsonObject, String key) {
        if (jsonObject == null || key == null || !jsonObject.has(key) || jsonObject.get(key).isJsonNull()) {
            return "";
        }
        try {
            return jsonObject.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }
}
