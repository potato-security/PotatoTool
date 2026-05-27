package com.potato.potatotool.content.redTeam.vulnScanner.report;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.model.StepExecutionRecord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ReportDataSanitizer {

    static final String REDACTED = "[REDACTED]";

    private static final Pattern COLON_VALUE_PATTERN =
            Pattern.compile("(?m)^([ \\t]*)([A-Za-z0-9_-]+)(\\s*:\\s*)([^\\r\\n]*)(\\r?)$");
    private static final Pattern JSON_FIELD_PATTERN =
            Pattern.compile("(?i)(\"([^\"]+)\"\\s*:\\s*\")([^\"]*)(\")");
    private static final Pattern PAIR_FIELD_PATTERN =
            Pattern.compile("(?i)(^|[?&;\\s])([A-Za-z0-9_-]+)(=)([^&;\\s]*)");
    private static final Pattern AUTH_SCHEME_PATTERN =
            Pattern.compile("(?i)\\b(Bearer|Basic)\\s+([^\\s,;]+)");
    private static final Pattern UNRESOLVED_TEMPLATE_PLACEHOLDER_PATTERN =
            Pattern.compile("\\{\\{[^{}\\r\\n]{1,160}\\}\\}");

    private ReportDataSanitizer() {
    }

    static List<ScanResult> sanitizeResults(List<ScanResult> results) {
        List<ScanResult> sanitized = new ArrayList<ScanResult>();
        if (results == null) {
            return sanitized;
        }

        for (ScanResult result : results) {
            sanitized.add(sanitizeResult(result));
        }
        return sanitized;
    }

    private static ScanResult sanitizeResult(ScanResult source) {
        ScanResult target = new ScanResult();
        if (source == null) {
            return target;
        }

        target.setTarget(sanitizeText(source.getTarget()));
        target.setPoc(copyPoc(source.getPoc()));
        target.setVulnerable(source.isVulnerable());
        target.setTimestamp(source.getTimestamp());
        target.setDetails(sanitizeObjectMap(source.getDetails()));
        target.setFingerprint(sanitizeFingerprint(source.getFingerprint()));
        target.setInputType(source.getInputType());
        target.setMatchedPath(sanitizeText(source.getMatchedPath()));
        target.setMatchedPayload(sanitizeText(source.getMatchedPayload()));
        target.setExtractedData(sanitizeStringList(source.getExtractedData()));
        target.setRawRequest(sanitizeText(source.getRawRequest()));
        target.setRawResponseSnippet(sanitizeText(source.getRawResponseSnippet()));
        target.setPocSource(sanitizeText(source.getPocSource()));
        target.setDuplicate(source.isDuplicate());
        target.setDuplicateReason(sanitizeText(source.getDuplicateReason()));
        target.setStepRecords(sanitizeStepRecords(source.getStepRecords()));
        target.setRecommendation(sanitizeText(source.getRecommendation()));
        target.setParamKeys(sanitizeStringList(source.getParamKeys()));
        target.setVariableValues(sanitizeStringMap(source.getVariableValues()));
        target.setOutputData(sanitizeObjectMap(source.getOutputData()));
        target.setCveId(sanitizeText(source.getCveId()));
        target.setCweId(sanitizeText(source.getCweId()));
        target.setCvssScore(sanitizeText(source.getCvssScore()));
        return target;
    }

    private static PocObj.Poc copyPoc(PocObj.Poc source) {
        PocObj.Poc target = new PocObj.Poc();
        if (source == null) {
            return target;
        }

        target.setId(sanitizeText(source.getId()));
        target.setName(sanitizeText(source.getName()));
        target.setAuthor(sanitizeText(source.getAuthor()));
        target.setSeverity(source.getSeverity());
        target.setDescription(sanitizeText(source.getDescription()));
        target.setReferences(sanitizeStringList(source.getReferences()));
        target.setProduct(sanitizeText(source.getProduct()));
        target.setVersion(sanitizeText(source.getVersion()));
        target.setTags(sanitizeStringList(source.getTags()));
        target.setCreateTime(sanitizeText(source.getCreateTime()));
        target.setUpdateTime(sanitizeText(source.getUpdateTime()));
        target.setProtocol(sanitizeText(source.getProtocol()));
        target.setAppPowerLink(sanitizeText(source.getAppPowerLink()));
        target.setPocDesc(sanitizeText(source.getPocDesc()));
        target.setSelfContained(source.isSelfContained());
        target.setFlow(sanitizeText(source.getFlow()));
        target.setImpact(sanitizeText(source.getImpact()));
        target.setRecommendation(sanitizeText(source.getRecommendation()));
        target.setHomepage(sanitizeText(source.getHomepage()));
        target.setSearchQueries(sanitizeStringMap(source.getSearchQueries()));
        target.setVariables(sanitizeVariables(source.getVariables()));
        target.setVariablesType(source.getVariablesType());
        target.setContinueOnMatch(source.isContinueOnMatch());
        target.setCveId(sanitizeText(source.getCveId()));
        target.setCweId(sanitizeText(source.getCweId()));
        target.setVulType(sanitizeText(source.getVulType()));
        target.setCvssScore(sanitizeText(source.getCvssScore()));
        target.setCvssMetrics(sanitizeText(source.getCvssMetrics()));
        target.setOriginalFormat(sanitizeText(source.getOriginalFormat()));
        target.setInputType(source.getInputType());
        target.setCategory(source.getCategory());
        target.setNormalizedTags(sanitizeStringSet(source.getNormalizedTags()));
        target.setFingerprint(sanitizeFingerprint(source.getFingerprint()));
        target.setRequiresHeadless(source.isRequiresHeadless());
        target.setRequiresCode(source.isRequiresCode());
        target.setRequiresFuzz(source.isRequiresFuzz());
        target.setFileTargetType(source.getFileTargetType());
        target.setSpecificFilePath(sanitizeText(source.getSpecificFilePath()));
        target.setFileExtensions(sanitizeStringList(source.getFileExtensions()));
        return target;
    }

    private static List<StepExecutionRecord> sanitizeStepRecords(List<StepExecutionRecord> source) {
        List<StepExecutionRecord> target = new ArrayList<StepExecutionRecord>();
        if (source == null) {
            return target;
        }

        for (StepExecutionRecord record : source) {
            StepExecutionRecord sanitized = new StepExecutionRecord();
            if (record != null) {
                sanitized.setStepIndex(record.getStepIndex());
                sanitized.setStepId(sanitizeText(record.getStepId()));
                sanitized.setStepType(sanitizeText(record.getStepType()));
                sanitized.setRequestUrl(sanitizeText(record.getRequestUrl()));
                sanitized.setRequestMethod(sanitizeText(record.getRequestMethod()));
                sanitized.setRequestHeaders(sanitizeStringMap(record.getRequestHeaders()));
                sanitized.setRequestBody(sanitizeText(record.getRequestBody()));
                sanitized.setRawRequest(sanitizeText(record.getRawRequest()));
                sanitized.setResponseCode(record.getResponseCode());
                sanitized.setResponseHeaders(sanitizeStringMap(record.getResponseHeaders()));
                sanitized.setResponseBody(sanitizeText(record.getResponseBody()));
                sanitized.setResponseBodyLength(record.getResponseBodyLength());
                sanitized.setRawResponse(sanitizeText(record.getRawResponse()));
                sanitized.setResponseTime(record.getResponseTime());
                sanitized.setMatched(record.isMatched());
                sanitized.setMatchedValues(sanitizeStringList(record.getMatchedValues()));
                sanitized.setExtractedVariables(sanitizeObjectMap(record.getExtractedVariables()));
                sanitized.setErrorMessage(sanitizeText(record.getErrorMessage()));
                sanitized.setTimestamp(record.getTimestamp());
            }
            target.add(sanitized);
        }
        return target;
    }

    private static Map<String, String> sanitizeStringMap(Map<String, String> source) {
        Map<String, String> target = new LinkedHashMap<String, String>();
        if (source == null) {
            return target;
        }

        for (Map.Entry<String, String> entry : source.entrySet()) {
            if (entry == null || entry.getKey() == null) {
                continue;
            }
            target.put(entry.getKey(), sanitizeValue(entry.getKey(), entry.getValue()));
        }
        return target;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> sanitizeObjectMap(Map<String, ?> source) {
        Map<String, Object> target = new LinkedHashMap<String, Object>();
        if (source == null) {
            return target;
        }

        for (Map.Entry<String, ?> entry : source.entrySet()) {
            if (entry == null || entry.getKey() == null) {
                continue;
            }
            target.put(entry.getKey(), sanitizeObject(entry.getKey(), entry.getValue()));
        }
        return target;
    }

    private static Map<String, List<String>> copyVariables(Map<String, List<String>> source) {
        return sanitizeVariables(source);
    }

    private static Map<String, String> copyStringMap(Map<String, String> source) {
        Map<String, String> target = new LinkedHashMap<String, String>();
        if (source == null) {
            return target;
        }

        for (Map.Entry<String, String> entry : source.entrySet()) {
            if (entry == null || entry.getKey() == null) {
                continue;
            }
            target.put(entry.getKey(), sanitizeText(entry.getValue()));
        }
        return target;
    }

    private static List<String> copyStringList(List<String> source) {
        List<String> target = new ArrayList<String>();
        if (source == null) {
            return target;
        }

        for (String item : source) {
            target.add(sanitizeText(item));
        }
        return target;
    }

    private static List<String> sanitizeStringList(List<String> source) {
        List<String> target = new ArrayList<String>();
        if (source == null) {
            return target;
        }

        for (String item : source) {
            target.add(sanitizeText(item));
        }
        return target;
    }

    private static Set<String> sanitizeStringSet(Set<String> source) {
        Set<String> target = new LinkedHashSet<String>();
        if (source == null) {
            return target;
        }

        for (String item : source) {
            target.add(sanitizeText(item));
        }
        return target;
    }

    private static Map<String, List<String>> sanitizeVariables(Map<String, List<String>> source) {
        Map<String, List<String>> target = new LinkedHashMap<String, List<String>>();
        if (source == null) {
            return target;
        }

        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            if (entry == null || entry.getKey() == null) {
                continue;
            }
            target.put(entry.getKey(), sanitizeStringList(entry.getValue()));
        }
        return target;
    }

    private static PocObj.FingerprintInfo sanitizeFingerprint(PocObj.FingerprintInfo source) {
        if (source == null) {
            return null;
        }

        PocObj.FingerprintInfo target = new PocObj.FingerprintInfo();
        target.setProductName(sanitizeText(source.getProductName()));
        target.setVersion(sanitizeText(source.getVersion()));
        target.setTechnology(sanitizeText(source.getTechnology()));
        target.setServer(sanitizeText(source.getServer()));
        target.setOs(sanitizeText(source.getOs()));
        target.setDetectedTags(sanitizeStringList(source.getDetectedTags()));
        target.setNormalizedTags(sanitizeStringSet(source.getNormalizedTags()));
        target.setBanner(sanitizeText(source.getBanner()));
        target.setDetectedAt(source.getDetectedAt());
        target.setSource(sanitizeText(source.getSource()));
        target.setConfidence(source.getConfidence());
        return target;
    }

    private static Object sanitizeObject(String key, Object value) {
        if (value == null) {
            return null;
        }
        if (isSensitiveKey(key)) {
            return REDACTED;
        }
        return sanitizeObject(value);
    }

    @SuppressWarnings("unchecked")
    private static Object sanitizeObject(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return sanitizeText((String) value);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Map) {
            return sanitizeObjectMap((Map<String, ?>) value);
        }
        if (value instanceof List) {
            List<Object> sanitized = new ArrayList<Object>();
            for (Object item : (List<?>) value) {
                sanitized.add(sanitizeObject(item));
            }
            return sanitized;
        }
        return sanitizeText(String.valueOf(value));
    }

    private static String sanitizeValue(String key, String value) {
        if (value == null) {
            return null;
        }
        if (isSensitiveKey(key)) {
            return REDACTED;
        }
        return sanitizeText(value);
    }

    static String sanitizeText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String sanitized = maskColonLines(text);
        sanitized = maskJsonFields(sanitized);
        sanitized = maskPairs(sanitized);
        sanitized = maskAuthSchemes(sanitized);
        return removeUnresolvedTemplatePlaceholders(sanitized);
    }

    static String removeUnresolvedTemplatePlaceholders(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String sanitized = text;
        for (int i = 0; i < 5; i++) {
            Matcher matcher = UNRESOLVED_TEMPLATE_PLACEHOLDER_PATTERN.matcher(sanitized);
            String replaced = matcher.replaceAll("[UNRESOLVED_TEMPLATE]");
            if (replaced.equals(sanitized)) {
                break;
            }
            sanitized = replaced;
        }
        return sanitized;
    }

    private static String maskColonLines(String text) {
        Matcher matcher = COLON_VALUE_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(2);
            String replacement = matcher.group(0);
            if (isSensitiveKey(key)) {
                replacement = matcher.group(1) + key + matcher.group(3) + REDACTED + matcher.group(5);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String maskJsonFields(String text) {
        Matcher matcher = JSON_FIELD_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(2);
            String replacement = matcher.group(0);
            if (isSensitiveKey(key)) {
                replacement = matcher.group(1) + REDACTED + matcher.group(4);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String maskPairs(String text) {
        Matcher matcher = PAIR_FIELD_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(2);
            String replacement = matcher.group(0);
            if (isSensitiveKey(key)) {
                replacement = matcher.group(1) + key + matcher.group(3) + REDACTED;
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String maskAuthSchemes(String text) {
        Matcher matcher = AUTH_SCHEME_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String replacement = matcher.group(1) + " " + REDACTED;
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }

        String normalized = key.trim();
        if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() > 1) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        normalized = normalized.toLowerCase(Locale.ROOT);

        return normalized.equals("authorization")
                || normalized.equals("proxy-authorization")
                || normalized.equals("proxy_authorization")
                || normalized.contains("authorization")
                || normalized.equals("cookie")
                || normalized.equals("set-cookie")
                || normalized.contains("cookie")
                || normalized.equals("x-security-audit-token")
                || normalized.startsWith("x-ai-")
                || normalized.startsWith("x-internal-")
                || normalized.contains("token");
    }
}
