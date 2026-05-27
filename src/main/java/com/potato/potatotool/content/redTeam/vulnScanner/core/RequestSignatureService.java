package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.Poc;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.PocStep;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.utils.network.HeaderManager;
import com.potato.potatotool.utils.network.RequestObj;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * 请求签名服务。
 * 统一聚合签名与响应缓存签名的判定逻辑，避免不同链路出现语义漂移。
 */
public class RequestSignatureService {

    public static final String SIGNATURE_RANDOM_AGENT = "{{RUNTIME_RANDOM_AGENT}}";
    public static final String SIGNATURE_UUID = "{{RUNTIME_UUID}}";

    public RequestSignature buildForPoc(Poc poc) {
        if (poc == null || poc.getVerifySteps() == null || poc.getVerifySteps().size() != 1) {
            return RequestSignature.uncoalescible("verify-steps");
        }
        String protocol = poc.getProtocol();
        if (protocol != null && !protocol.equalsIgnoreCase("http") && !protocol.equalsIgnoreCase("https")) {
            return RequestSignature.uncoalescible("protocol");
        }

        PocStep step = poc.getVerifySteps().get(0);
        if (hasRuntimeSensitiveFeatures(poc, step)) {
            return RequestSignature.uncoalescible("runtime-sensitive");
        }

        HeaderResolution headerResolution = resolveHeaders(poc, step);
        if (step.getRaw() != null && !step.getRaw().isEmpty()) {
            if (step.getRaw().size() != 1) {
                return RequestSignature.uncoalescible("raw-multi");
            }
            String raw = step.getRaw().get(0);
            if (containsRuntimePlaceholder(raw)) {
                return RequestSignature.uncoalescible("raw-placeholder");
            }
            String key = "raw|" + hashText(raw)
                    + "|headers:" + normalizeHeaders(headerResolution.getSignatureHeaders())
                    + "|follow:" + step.isFollowRedirect();
            return RequestSignature.coalescible(key, headerResolution.getSignatureHeaders());
        }

        String key = new StringBuilder()
                .append("http|")
                .append(normalizeMethod(step.getMethod()))
                .append('|')
                .append(normalizeText(step.getPath(), "/"))
                .append("|body:")
                .append(hashText(step.getBody()))
                .append("|headers:")
                .append(normalizeHeaders(headerResolution.getSignatureHeaders()))
                .append("|cookie:")
                .append(normalizeText(step.getCookie(), ""))
                .append("|follow:")
                .append(step.isFollowRedirect())
                .toString();
        return RequestSignature.coalescible(key, headerResolution.getSignatureHeaders());
    }

    public String buildForRequest(RequestObj requestObj) {
        if (requestObj == null || requestObj.getUrl() == null || requestObj.getUrl().trim().isEmpty()) {
            return null;
        }
        String method = requestObj.getMethod() == null ? "GET" : requestObj.getMethod();
        String url = requestObj.getUrl();
        String path = "/";
        String body = requestObj.getPostData() == null ? null : new String(requestObj.getPostData(), StandardCharsets.UTF_8);
        Map<String, String> headers = requestObj.getRequestSignatureHeaders();
        if (headers == null) {
            headers = requestObj.getHeaders();
        }
        try {
            URL parsed = new URL(url);
            path = parsed.getPath();
            if (parsed.getQuery() != null && !parsed.getQuery().isEmpty()) {
                path = path + "?" + parsed.getQuery();
            }
            String baseUrl = parsed.getProtocol() + "://" + parsed.getAuthority();
            return new StringBuilder()
                    .append(normalizeUrl(baseUrl))
                    .append("|")
                    .append(normalizeMethod(method))
                    .append("|")
                    .append(path)
                    .append("|body:")
                    .append(hashText(body))
                    .append("|headers:")
                    .append(normalizeHeaders(headers))
                    .toString();
        } catch (MalformedURLException ignored) {
            return new StringBuilder()
                    .append(normalizeUrl(url))
                    .append("|")
                    .append(normalizeMethod(method))
                    .append("|")
                    .append(path)
                    .append("|body:")
                    .append(hashText(body))
                    .append("|headers:")
                    .append(normalizeHeaders(headers))
                    .toString();
        }
    }

    public HeaderResolution resolveHeaders(Poc poc, PocStep step) {
        PocObj.GlobalConfig globalConfig = poc != null ? poc.getGlobalConfig() : null;
        return resolveHeaders(globalConfig, step);
    }

    public HeaderResolution resolveHeaders(PocObj.GlobalConfig globalConfig, PocStep step) {
        Map<String, String> pocGlobalHeaders = null;
        if (globalConfig != null
                && globalConfig.getGlobalHeaders() != null
                && !globalConfig.getGlobalHeaders().isEmpty()) {
            pocGlobalHeaders = new LinkedHashMap<String, String>(globalConfig.getGlobalHeaders());
        }

        Map<String, String> builtinDefaults = HeaderManager.getInstance().getCustomHeaders();
        Map<String, String> merged = HeaderManager.getInstance().mergeHeaders(null, pocGlobalHeaders);

        Set<String> explicitHeaderNames = new HashSet<String>();
        if (pocGlobalHeaders != null) {
            explicitHeaderNames.addAll(normalizeHeaderNames(pocGlobalHeaders));
        }
        if (step != null && step.getHeaders() != null && !step.getHeaders().isEmpty()) {
            merged.putAll(step.getHeaders());
            explicitHeaderNames.addAll(normalizeHeaderNames(step.getHeaders()));
        }

        Map<String, String> resolvedHeaders = new LinkedHashMap<String, String>();
        Map<String, String> signatureHeaders = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : merged.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            if (name == null || value == null) {
                continue;
            }
            boolean normalizeDynamicValue = isBuiltinDynamicHeader(name, value, builtinDefaults, explicitHeaderNames);
            HeaderRender render = renderHeaderValue(value, normalizeDynamicValue);
            resolvedHeaders.put(name, render.resolvedValue);
            signatureHeaders.put(name, render.signatureValue);
        }
        if (step != null && step.getCookie() != null && !step.getCookie().trim().isEmpty()) {
            resolvedHeaders.put("Cookie", step.getCookie());
            signatureHeaders.put("Cookie", step.getCookie());
        }
        return new HeaderResolution(resolvedHeaders, signatureHeaders);
    }

    private boolean hasRuntimeSensitiveFeatures(Poc poc, PocStep step) {
        if (poc == null || step == null) {
            return true;
        }
        return (poc.getVariables() != null && !poc.getVariables().isEmpty())
                || (step.getPathCandidates() != null && !step.getPathCandidates().isEmpty())
                || (step.getRequestVariables() != null && !step.getRequestVariables().isEmpty())
                || (step.getExtractors() != null && !step.getExtractors().isEmpty())
                || (step.getOutput() != null && !step.getOutput().isEmpty())
                || step.isIterateAll()
                || step.isReadAll()
                || step.isUnsafe()
                || step.isChunked()
                || step.isCompressed()
                || containsRuntimePlaceholder(step.getPath())
                || containsRuntimePlaceholder(step.getBody())
                || containsRuntimePlaceholder(step.getCookie())
                || containsRuntimePlaceholder(step.getHeaders());
    }

    private Set<String> normalizeHeaderNames(Map<String, String> headers) {
        Set<String> names = new HashSet<String>();
        if (headers == null || headers.isEmpty()) {
            return names;
        }
        for (String name : headers.keySet()) {
            if (name != null) {
                names.add(name.trim().toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }

    private boolean isBuiltinDynamicHeader(String name, String value,
                                           Map<String, String> builtinDefaults,
                                           Set<String> explicitHeaderNames) {
        if (name == null || value == null || builtinDefaults == null || builtinDefaults.isEmpty()) {
            return false;
        }
        String normalizedName = name.trim().toLowerCase(Locale.ROOT);
        if (explicitHeaderNames != null && explicitHeaderNames.contains(normalizedName)) {
            return false;
        }
        for (Map.Entry<String, String> entry : builtinDefaults.entrySet()) {
            if (entry.getKey() != null
                    && entry.getKey().trim().equalsIgnoreCase(name)
                    && value.equals(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    private HeaderRender renderHeaderValue(String value, boolean normalizeDynamicValue) {
        String resolved = value;
        String signature = value;
        if (resolved.contains(HeaderManager.RANDOM_AGENT_PLACEHOLDER)) {
            resolved = resolved.replace(HeaderManager.RANDOM_AGENT_PLACEHOLDER,
                    com.potato.potatotool.utils.data.StrUtils.RandomUserAgent());
            signature = signature.replace(HeaderManager.RANDOM_AGENT_PLACEHOLDER,
                    normalizeDynamicValue ? SIGNATURE_RANDOM_AGENT : resolved);
        }
        if (resolved.contains(HeaderManager.UUID_PLACEHOLDER)) {
            String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
            resolved = resolved.replace(HeaderManager.UUID_PLACEHOLDER, uuid);
            signature = signature.replace(HeaderManager.UUID_PLACEHOLDER,
                    normalizeDynamicValue ? SIGNATURE_UUID : uuid);
        }
        return new HeaderRender(resolved, signature);
    }

    private String normalizeHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return "";
        }
        List<String> pairs = new ArrayList<String>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            pairs.add(entry.getKey().trim().toLowerCase(Locale.ROOT) + ":" + entry.getValue());
        }
        Collections.sort(pairs);
        return pairs.toString();
    }

    private String normalizeMethod(String method) {
        return method == null || method.trim().isEmpty() ? "GET" : method.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value, String defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    private String normalizeUrl(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String hashText(String value) {
        if (value == null || value.isEmpty()) {
            return "empty";
        }
        return String.valueOf(value.hashCode());
    }

    private boolean containsRuntimePlaceholder(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        for (String value : values.values()) {
            if (containsRuntimePlaceholder(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsRuntimePlaceholder(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return value.contains("{{") || value.contains("}}") || value.contains("@@");
    }

    public static final class HeaderResolution {
        private final Map<String, String> resolvedHeaders;
        private final Map<String, String> signatureHeaders;

        public HeaderResolution(Map<String, String> resolvedHeaders, Map<String, String> signatureHeaders) {
            this.resolvedHeaders = resolvedHeaders == null
                    ? Collections.<String, String>emptyMap()
                    : new LinkedHashMap<String, String>(resolvedHeaders);
            this.signatureHeaders = signatureHeaders == null
                    ? Collections.<String, String>emptyMap()
                    : new LinkedHashMap<String, String>(signatureHeaders);
        }

        public Map<String, String> getResolvedHeaders() {
            return resolvedHeaders;
        }

        public Map<String, String> getSignatureHeaders() {
            return signatureHeaders;
        }
    }

    public static final class RequestSignature {
        private final boolean coalescible;
        private final String blockReason;
        private final String signatureKey;
        private final Map<String, String> signatureHeaders;

        private RequestSignature(boolean coalescible, String blockReason, String signatureKey,
                                 Map<String, String> signatureHeaders) {
            this.coalescible = coalescible;
            this.blockReason = blockReason;
            this.signatureKey = signatureKey;
            this.signatureHeaders = signatureHeaders == null
                    ? Collections.<String, String>emptyMap()
                    : new LinkedHashMap<String, String>(signatureHeaders);
        }

        public static RequestSignature coalescible(String signatureKey, Map<String, String> signatureHeaders) {
            return new RequestSignature(true, null, signatureKey, signatureHeaders);
        }

        public static RequestSignature uncoalescible(String blockReason) {
            return new RequestSignature(false, blockReason, null, Collections.<String, String>emptyMap());
        }

        public boolean isCoalescible() {
            return coalescible;
        }

        public String getBlockReason() {
            return blockReason;
        }

        public String getSignatureKey() {
            return signatureKey;
        }

        public Map<String, String> getSignatureHeaders() {
            return signatureHeaders;
        }
    }

    private static final class HeaderRender {
        private final String resolvedValue;
        private final String signatureValue;

        private HeaderRender(String resolvedValue, String signatureValue) {
            this.resolvedValue = resolvedValue;
            this.signatureValue = signatureValue;
        }
    }
}
