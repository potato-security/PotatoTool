package com.potato.potatotool.utils.browser;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * High-level CDP page helpers shared by browser automation callers.
 */
public final class CdpPage {
    private final CdpClient client;

    CdpPage(CdpClient client) {
        this.client = client;
    }

    public interface WaitCondition {
        boolean test(CdpPage page) throws CdpException;
    }

    public void navigate(String url, int timeoutMillis) throws CdpException {
        JsonObject params = new JsonObject();
        params.addProperty("url", url);
        client.request("Page.navigate", params, timeoutMillis);
    }

    public JsonObject evaluate(String expression, boolean returnByValue, int timeoutMillis) throws CdpException {
        JsonObject params = new JsonObject();
        params.addProperty("expression", expression);
        params.addProperty("returnByValue", returnByValue);
        params.addProperty("awaitPromise", true);

        JsonObject response = client.request("Runtime.evaluate", params, timeoutMillis);
        JsonObject result = response != null && response.has("result") && response.get("result").isJsonObject()
                ? response.getAsJsonObject("result")
                : null;
        if (result != null && result.has("exceptionDetails") && result.get("exceptionDetails").isJsonObject()) {
            throw new CdpException(CdpErrorType.PROTOCOL_ERROR,
                    "JavaScript evaluation failed: " + extractExceptionMessage(result.getAsJsonObject("exceptionDetails")));
        }
        return response;
    }

    public void addScriptToEvaluateOnNewDocument(String script, int timeoutMillis) throws CdpException {
        JsonObject params = new JsonObject();
        params.addProperty("source", script == null ? "" : script);
        client.request("Page.addScriptToEvaluateOnNewDocument", params, timeoutMillis);
    }

    public JsonObject waitForEvent(String eventMethod, int timeoutMillis) throws CdpException {
        return client.waitForEvent(eventMethod, timeoutMillis);
    }

    public void handleJavaScriptDialog(boolean accept, String promptText, int timeoutMillis) throws CdpException {
        JsonObject params = new JsonObject();
        params.addProperty("accept", accept);
        params.addProperty("promptText", promptText == null ? "" : promptText);
        client.request("Page.handleJavaScriptDialog", params, timeoutMillis);
    }

    public JsonElement evaluateValue(String expression, int timeoutMillis) throws CdpException {
        JsonObject response = evaluate(expression, true, timeoutMillis);
        JsonObject envelope = extractEvaluationResult(response);
        if (envelope == null) {
            return JsonNull.INSTANCE;
        }
        if (envelope.has("value")) {
            return envelope.get("value");
        }
        if (envelope.has("unserializableValue")) {
            return envelope.get("unserializableValue");
        }
        return JsonNull.INSTANCE;
    }

    public CdpPageSnapshot snapshot(int timeoutMillis) throws CdpException {
        JsonElement value = evaluateValue(
                "({url: location.href, html: document.documentElement ? document.documentElement.outerHTML : ''})",
                timeoutMillis);
        if (value == null || !value.isJsonObject()) {
            return new CdpPageSnapshot("", "");
        }

        JsonObject snapshot = value.getAsJsonObject();
        return new CdpPageSnapshot(readJsonString(snapshot, "url"), readJsonString(snapshot, "html"));
    }

    public String readTitle(int timeoutMillis) throws CdpException {
        JsonElement value = evaluateValue("document.title", timeoutMillis);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
    }

    public String captureScreenshotBase64(int timeoutMillis) throws CdpException {
        return captureScreenshotBase64(false, timeoutMillis);
    }

    public String captureScreenshotBase64(boolean captureBeyondViewport, int timeoutMillis) throws CdpException {
        JsonObject params = new JsonObject();
        params.addProperty("captureBeyondViewport", captureBeyondViewport);
        JsonObject response = client.request("Page.captureScreenshot", params, timeoutMillis);
        JsonObject result = response != null && response.has("result") && response.get("result").isJsonObject()
                ? response.getAsJsonObject("result")
                : null;
        return readJsonString(result, "data");
    }

    public void enableNetwork(int timeoutMillis) throws CdpException {
        client.request("Network.enable", null, timeoutMillis);
    }

    public void setExtraHttpHeaders(Map<String, String> headers, int timeoutMillis) throws CdpException {
        JsonObject params = new JsonObject();
        JsonObject jsonHeaders = new JsonObject();
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                jsonHeaders.addProperty(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
            }
        }
        params.add("headers", jsonHeaders);
        client.request("Network.setExtraHTTPHeaders", params, timeoutMillis);
    }

    public void setUserAgentOverride(String userAgent, int timeoutMillis) throws CdpException {
        JsonObject params = new JsonObject();
        params.addProperty("userAgent", userAgent == null ? "" : userAgent);
        client.request("Network.setUserAgentOverride", params, timeoutMillis);
    }

    public Map<String, String> readAllCookies(int timeoutMillis) throws CdpException {
        client.request("Network.enable", null, timeoutMillis);
        JsonObject response = client.request("Network.getAllCookies", null, timeoutMillis);
        JsonObject result = response != null && response.has("result") && response.get("result").isJsonObject()
                ? response.getAsJsonObject("result")
                : null;
        if (result == null || !result.has("cookies") || !result.get("cookies").isJsonArray()) {
            return new LinkedHashMap<String, String>();
        }

        Map<String, String> cookieMap = new LinkedHashMap<String, String>();
        for (JsonElement element : result.getAsJsonArray("cookies")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject cookie = element.getAsJsonObject();
            String name = readJsonString(cookie, "name");
            if (!name.isEmpty()) {
                cookieMap.put(name, readJsonString(cookie, "value"));
            }
        }
        return cookieMap;
    }

    public boolean waitUntil(WaitCondition condition, long timeoutMillis, long pollIntervalMillis) throws CdpException {
        if (condition == null) {
            return false;
        }

        long deadline = System.currentTimeMillis() + timeoutMillis;
        long interval = Math.max(50L, pollIntervalMillis);
        while (System.currentTimeMillis() < deadline) {
            if (condition.test(this)) {
                return true;
            }
            sleep(interval);
        }
        return condition.test(this);
    }

    public void waitForDocumentReadyState(final String readyState, long timeoutMillis, long pollIntervalMillis)
            throws CdpException {
        boolean matched = waitUntil(new WaitCondition() {
            @Override
            public boolean test(CdpPage page) throws CdpException {
                JsonElement state = page.evaluateValue("document.readyState", 5000);
                return state != null && state.isJsonPrimitive() && readyState.equals(state.getAsString());
            }
        }, timeoutMillis, pollIntervalMillis);
        if (!matched) {
            throw CdpException.timeout("Timed out waiting for document.readyState=" + readyState);
        }
    }

    private void sleep(long intervalMillis) throws CdpException {
        try {
            Thread.sleep(intervalMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CdpException(CdpErrorType.INTERRUPTED, "Interrupted while waiting for CDP condition", e);
        }
    }

    private JsonObject extractEvaluationResult(JsonObject response) {
        JsonObject result = response != null && response.has("result") && response.get("result").isJsonObject()
                ? response.getAsJsonObject("result")
                : null;
        if (result == null || !result.has("result") || !result.get("result").isJsonObject()) {
            return null;
        }
        return result.getAsJsonObject("result");
    }

    private String extractExceptionMessage(JsonObject exceptionDetails) {
        String text = readJsonString(exceptionDetails, "text");
        if (!text.isEmpty()) {
            return text;
        }
        JsonObject exception = exceptionDetails.has("exception") && exceptionDetails.get("exception").isJsonObject()
                ? exceptionDetails.getAsJsonObject("exception")
                : null;
        return readJsonString(exception, "description");
    }

    private String readJsonString(JsonObject jsonObject, String key) {
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
