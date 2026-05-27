package com.potato.potatotool.content.redTeam.vulnScanner.correctness;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.loader.PocLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("内置 POC 完备正确性 manifest 生成")
public class BuiltinPocCorrectnessManifestTest {

    private static final Path POC_ROOT = Paths.get("src/main/resources/poc");
    private static final Path OUTPUT_ROOT = Paths.get("target/vulnscan-poc-correctness");
    private static final Path MANIFEST = OUTPUT_ROOT.resolve("manifest.tsv");
    private static final Path SUMMARY = OUTPUT_ROOT.resolve("summary.tsv");
    private static final Path FAILURES = OUTPUT_ROOT.resolve("parse-failures.tsv");

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_BLOCKED = "blocked-by-parse";
    private static final String UNKNOWN = "UNKNOWN";

    private static final String[] DANGEROUS_TOKENS = {
            "curl ",
            "wget ",
            "bash -c",
            "cmd.exe",
            "powershell",
            "processbuilder",
            "runtime.getruntime",
            "os.system(",
            "subprocess.",
            "interactsh",
            "dnslog",
            "ceye",
            "reverse shell",
            "rm -rf",
            "delete from",
            "drop table",
            "/bin/sh",
            "nc -e",
            "bash -i"
    };

    @Test
    @DisplayName("应为内置 POC 全量生成正确性校验 manifest")
    public void shouldGenerateBuiltinPocCorrectnessManifest() throws Exception {
        assertTrue(Files.isDirectory(POC_ROOT), "内置 POC 目录不存在: " + POC_ROOT);
        Files.createDirectories(OUTPUT_ROOT);

        List<Path> pocFiles = listPocFiles(POC_ROOT);
        assertFalse(pocFiles.isEmpty(), "内置 POC 清单为空");

        PocLoader loader = new PocLoader();
        loader.setVerbose(false);
        loader.setSkipInvalidPocs(true);

        List<ManifestRecord> records = new ArrayList<ManifestRecord>();
        List<String> failureRows = new ArrayList<String>();
        failureRows.add("path\tformat\tparse_status\tsuspected_layer\tmessage");

        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        PrintStream quiet = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                // discard verbose converter diagnostics while building the manifest
            }
        }, true, "UTF-8");
        try {
            System.setOut(quiet);
            System.setErr(quiet);
            for (Path file : pocFiles) {
                ManifestRecord record = inspectFile(loader, file);
                records.add(record);
                if (!"parsed".equals(record.parseStatus)) {
                    failureRows.add(record.relativePath + "\t"
                            + record.sourceFormat + "\t"
                            + record.parseStatus + "\t"
                            + record.suspectedLayer + "\t"
                            + clean(record.parseMessage));
                }
            }
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
            quiet.close();
        }

        writeManifest(records);
        writeSummary(records);
        Files.write(FAILURES, failureRows, StandardCharsets.UTF_8);

        assertTrue(Files.isRegularFile(MANIFEST), "manifest 未生成");
        assertTrue(Files.isRegularFile(SUMMARY), "summary 未生成");
        assertTrue(records.size() == pocFiles.size(), "manifest 记录数应等于内置 POC 文件数");
    }

    private static List<Path> listPocFiles(Path root) throws IOException {
        final List<Path> files = new ArrayList<Path>();
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
                if (name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".json")) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        Collections.sort(files, new Comparator<Path>() {
            @Override
            public int compare(Path left, Path right) {
                return normalize(left).compareTo(normalize(right));
            }
        });
        return files;
    }

    private static ManifestRecord inspectFile(PocLoader loader, Path file) throws IOException {
        ManifestRecord record = new ManifestRecord();
        record.relativePath = normalize(POC_ROOT.relativize(file));
        record.sourceFormat = detectSourceFormat(record.relativePath);
        record.extension = extension(file);
        record.sizeBytes = Files.size(file);

        String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        record.dangerousIndicators = joinSet(findDangerousIndicators(content));

        try {
            PocObj.Poc poc = loader.loadFromFile(file.toString());
            if (poc == null) {
                record.parseStatus = "null-poc";
                record.parseMessage = "loader returned null";
                record.suspectedLayer = "converter";
                applyPendingStatuses(record, STATUS_BLOCKED);
                applyPathFallbacks(record);
                return record;
            }

            record.parseStatus = "parsed";
            record.parseMessage = "";
            record.suspectedLayer = "";
            fillFromPoc(record, poc);
            applyPendingStatuses(record, STATUS_PENDING);
        } catch (Exception e) {
            record.parseStatus = "parse-failed";
            record.parseMessage = e.getMessage();
            record.suspectedLayer = "converter";
            applyPendingStatuses(record, STATUS_BLOCKED);
            applyPathFallbacks(record);
        }

        return record;
    }

    private static void fillFromPoc(ManifestRecord record, PocObj.Poc poc) {
        record.pocId = value(poc.getId());
        record.pocName = value(poc.getName());
        record.originalFormat = value(poc.getOriginalFormat());
        record.protocol = value(poc.getProtocol());
        record.category = poc.getCategory() == null ? UNKNOWN : poc.getCategory().name();
        record.severity = poc.getSeverity() == null ? UNKNOWN : poc.getSeverity().name();
        record.inputType = poc.getInputType() == null ? UNKNOWN : poc.getInputType().name();
        record.tags = joinList(poc.getTags());
        record.cveId = value(poc.getCveId());
        record.cweId = value(poc.getCweId());
        record.vulType = value(poc.getVulType());
        record.product = value(poc.getProduct());
        record.requiresHeadless = poc.isRequiresHeadless() || hasStepType(poc, PocObj.HeadlessStep.class);
        record.requiresCode = poc.isRequiresCode() || hasStepType(poc, PocObj.CodeStep.class);
        record.requiresFuzz = poc.isRequiresFuzz() || hasVariables(poc);
        record.requiresOob = containsIgnoreCase(record.tags, "oast")
                || containsIgnoreCase(record.tags, "oob")
                || containsIgnoreCase(record.tags, "dnslog")
                || containsIgnoreCase(record.dangerousIndicators, "interactsh")
                || containsIgnoreCase(record.dangerousIndicators, "dnslog");
        record.requiresAuth = requiresAuth(poc);
        record.stepCount = countSteps(poc);
        record.matcherTypes = joinSet(collectMatcherTypes(poc, false));
        record.extractorTypes = joinSet(collectMatcherTypes(poc, true));
        record.stepTypes = joinSet(collectStepTypes(poc));
        record.hasFlow = hasText(poc.getFlow());
        record.hasVariables = hasVariables(poc);
        record.hasConversionWarnings = poc.getConversionWarnings() != null && !poc.getConversionWarnings().isEmpty();
        record.hasSemanticWarnings = poc.getSemanticWarnings() != null && !poc.getSemanticWarnings().isEmpty();
        record.hasUnsupportedCapabilities = poc.getUnsupportedCapabilities() != null && !poc.getUnsupportedCapabilities().isEmpty();
        record.validationBucket = determineValidationBucket(record);
    }

    private static void applyPathFallbacks(ManifestRecord record) {
        String path = record.relativePath.toLowerCase(Locale.ROOT);
        if (path.contains("/headless/")) {
            record.requiresHeadless = true;
            record.category = "HEADLESS";
        }
        if (path.contains("/code/") || path.contains("/javascript/")) {
            record.requiresCode = true;
            record.category = path.contains("/javascript/") ? "JAVASCRIPT" : "CODE";
        }
        if (path.contains("/dns/")) {
            record.protocol = "dns";
        } else if (path.contains("/ssl/")) {
            record.protocol = "ssl";
        } else if (path.contains("/file/")) {
            record.protocol = "file";
        } else if (path.contains("/http/")) {
            record.protocol = "http";
        }
        if (path.contains("/osint/")) {
            record.category = "OSINT";
        }
        record.validationBucket = determineValidationBucket(record);
    }

    private static void applyPendingStatuses(ManifestRecord record, String status) {
        record.mockVerified = status;
        record.dockerVerified = status;
        record.onlineAuthorizedVerified = status;
        record.manualReviewed = status;
        record.blockedByEnv = status;
        record.destructiveSkipped = status;
    }

    private static String determineValidationBucket(ManifestRecord record) {
        if (!"parsed".equals(record.parseStatus)) {
            return "parser-fix-first";
        }
        if (record.requiresCode || hasText(record.dangerousIndicators)) {
            return "isolated-review";
        }
        if (record.requiresHeadless) {
            return "headless";
        }
        if (record.requiresOob) {
            return "oob";
        }
        if (record.requiresAuth) {
            return "auth";
        }
        if ("file".equalsIgnoreCase(record.protocol)) {
            return "file";
        }
        if ("dns".equalsIgnoreCase(record.protocol) || "ssl".equalsIgnoreCase(record.protocol)
                || "tcp".equalsIgnoreCase(record.protocol) || "udp".equalsIgnoreCase(record.protocol)
                || "javascript".equalsIgnoreCase(record.protocol)) {
            return "protocol-specific";
        }
        return "mock-first";
    }

    private static boolean requiresAuth(PocObj.Poc poc) {
        if (poc.getGlobalConfig() != null && poc.getGlobalConfig().getAuthConfig() != null
                && !poc.getGlobalConfig().getAuthConfig().isEmpty()) {
            return true;
        }
        for (PocObj.PocStep step : allSteps(poc)) {
            if (hasText(step.getAuthType()) || hasText(step.getUsername()) || hasText(step.getPassword())
                    || hasText(step.getToken()) || hasText(step.getCookie())
                    || containsAuthHeader(step.getHeaders())) {
                return true;
            }
        }
        String tags = joinList(poc.getTags()).toLowerCase(Locale.ROOT);
        return tags.contains("default-login") || tags.contains("credential") || tags.contains("token");
    }

    private static boolean containsAuthHeader(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return false;
        }
        for (String key : headers.keySet()) {
            if (key != null) {
                String lower = key.toLowerCase(Locale.ROOT);
                if ("authorization".equals(lower) || "cookie".equals(lower) || lower.contains("token")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasVariables(PocObj.Poc poc) {
        return poc.getVariables() != null && !poc.getVariables().isEmpty();
    }

    private static boolean hasStepType(PocObj.Poc poc, Class<?> type) {
        for (PocObj.PocStep step : allSteps(poc)) {
            if (type.isInstance(step)) {
                return true;
            }
        }
        return false;
    }

    private static int countSteps(PocObj.Poc poc) {
        return allSteps(poc).size();
    }

    private static List<PocObj.PocStep> allSteps(PocObj.Poc poc) {
        List<PocObj.PocStep> steps = new ArrayList<PocObj.PocStep>();
        if (poc.getVerifySteps() != null) {
            steps.addAll(poc.getVerifySteps());
        }
        if (poc.getExploitSteps() != null) {
            steps.addAll(poc.getExploitSteps());
        }
        return steps;
    }

    private static Set<String> collectMatcherTypes(PocObj.Poc poc, boolean extractors) {
        Set<String> types = new HashSet<String>();
        for (PocObj.PocStep step : allSteps(poc)) {
            List<PocObj.Matcher> matchers = extractors ? step.getExtractors() : step.getMatchers();
            if (matchers == null) {
                continue;
            }
            for (PocObj.Matcher matcher : matchers) {
                collectMatcherType(types, matcher);
            }
        }
        return types;
    }

    private static void collectMatcherType(Set<String> types, PocObj.Matcher matcher) {
        if (matcher == null) {
            return;
        }
        types.add(matcher.getType() == null ? UNKNOWN : matcher.getType().name());
        if (matcher.getSubMatchers() != null) {
            for (PocObj.Matcher subMatcher : matcher.getSubMatchers()) {
                collectMatcherType(types, subMatcher);
            }
        }
    }

    private static Set<String> collectStepTypes(PocObj.Poc poc) {
        Set<String> types = new HashSet<String>();
        for (PocObj.PocStep step : allSteps(poc)) {
            if (step instanceof PocObj.DnsStep) {
                types.add("dns");
            } else if (step instanceof PocObj.TcpStep) {
                types.add("tcp");
            } else if (step instanceof PocObj.WebSocketStep) {
                types.add("websocket");
            } else if (step instanceof PocObj.SslStep) {
                types.add("ssl");
            } else if (step instanceof PocObj.FileStep) {
                types.add("file");
            } else if (step instanceof PocObj.HeadlessStep) {
                types.add("headless");
            } else if (step instanceof PocObj.CodeStep) {
                PocObj.CodeStep codeStep = (PocObj.CodeStep) step;
                types.add(hasText(codeStep.getProtocolName()) ? codeStep.getProtocolName() : "code");
            } else {
                types.add("http");
            }
        }
        return types;
    }

    private static Set<String> findDangerousIndicators(String content) {
        Set<String> found = new HashSet<String>();
        if (content == null) {
            return found;
        }
        String lower = content.toLowerCase(Locale.ROOT);
        for (String token : DANGEROUS_TOKENS) {
            if (lower.contains(token)) {
                found.add(token.trim());
            }
        }
        return found;
    }

    private static String detectSourceFormat(String relativePath) {
        String path = relativePath.toLowerCase(Locale.ROOT);
        if (path.startsWith("nucleipoc/")) {
            return "nuclei";
        }
        if (path.startsWith("gobypoc/")) {
            return "goby";
        }
        if (path.startsWith("xraypoc/")) {
            return "xray";
        }
        if (path.startsWith("pocsuitepoc/")) {
            return "pocsuite";
        }
        return "unknown";
    }

    private static String extension(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private static void writeManifest(List<ManifestRecord> records) throws IOException {
        List<String> rows = new ArrayList<String>();
        rows.add("path\tsource_format\textension\tparse_status\tsuspected_layer\tparse_message"
                + "\tpoc_id\tname\toriginal_format\tprotocol\tcategory\tseverity\tinput_type"
                + "\ttags\tcve_id\tcwe_id\tvul_type\tproduct\tstep_count\tstep_types"
                + "\tmatcher_types\textractor_types\trequires_auth\trequires_oob\trequires_headless"
                + "\trequires_code\trequires_fuzz\tdangerous_indicators\thas_flow\thas_variables"
                + "\thas_conversion_warnings\thas_semantic_warnings\thas_unsupported_capabilities"
                + "\tvalidation_bucket\tmock_verified\tdocker_verified\tonline_authorized_verified"
                + "\tmanual_reviewed\tblocked_by_env\tdestructive_skipped\tsize_bytes");

        for (ManifestRecord record : records) {
            rows.add(record.toTsv());
        }
        Files.write(MANIFEST, rows, StandardCharsets.UTF_8);
    }

    private static void writeSummary(List<ManifestRecord> records) throws IOException {
        List<String> rows = new ArrayList<String>();
        rows.add("metric\tkey\tvalue");
        rows.add("total\tall\t" + records.size());
        appendCounts(rows, "source_format", countBy(records, new KeyExtractor() {
            @Override
            public String key(ManifestRecord record) {
                return record.sourceFormat;
            }
        }));
        appendCounts(rows, "parse_status", countBy(records, new KeyExtractor() {
            @Override
            public String key(ManifestRecord record) {
                return record.parseStatus;
            }
        }));
        appendCounts(rows, "protocol", countBy(records, new KeyExtractor() {
            @Override
            public String key(ManifestRecord record) {
                return record.protocol;
            }
        }));
        appendCounts(rows, "category", countBy(records, new KeyExtractor() {
            @Override
            public String key(ManifestRecord record) {
                return record.category;
            }
        }));
        appendCounts(rows, "severity", countBy(records, new KeyExtractor() {
            @Override
            public String key(ManifestRecord record) {
                return record.severity;
            }
        }));
        appendCounts(rows, "validation_bucket", countBy(records, new KeyExtractor() {
            @Override
            public String key(ManifestRecord record) {
                return record.validationBucket;
            }
        }));
        appendBoolean(rows, records, "requires_auth", new BooleanExtractor() {
            @Override
            public boolean value(ManifestRecord record) {
                return record.requiresAuth;
            }
        });
        appendBoolean(rows, records, "requires_oob", new BooleanExtractor() {
            @Override
            public boolean value(ManifestRecord record) {
                return record.requiresOob;
            }
        });
        appendBoolean(rows, records, "requires_headless", new BooleanExtractor() {
            @Override
            public boolean value(ManifestRecord record) {
                return record.requiresHeadless;
            }
        });
        appendBoolean(rows, records, "requires_code", new BooleanExtractor() {
            @Override
            public boolean value(ManifestRecord record) {
                return record.requiresCode;
            }
        });
        appendBoolean(rows, records, "requires_fuzz", new BooleanExtractor() {
            @Override
            public boolean value(ManifestRecord record) {
                return record.requiresFuzz;
            }
        });
        Files.write(SUMMARY, rows, StandardCharsets.UTF_8);
    }

    private static Map<String, Integer> countBy(List<ManifestRecord> records, KeyExtractor extractor) {
        Map<String, Integer> counts = new TreeMap<String, Integer>();
        for (ManifestRecord record : records) {
            String key = value(extractor.key(record));
            Integer current = counts.get(key);
            counts.put(key, current == null ? 1 : current + 1);
        }
        return counts;
    }

    private static void appendCounts(List<String> rows, String metric, Map<String, Integer> counts) {
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            rows.add(metric + "\t" + entry.getKey() + "\t" + entry.getValue());
        }
    }

    private static void appendBoolean(List<String> rows, List<ManifestRecord> records,
                                      String metric, BooleanExtractor extractor) {
        int trueCount = 0;
        int falseCount = 0;
        for (ManifestRecord record : records) {
            if (extractor.value(record)) {
                trueCount++;
            } else {
                falseCount++;
            }
        }
        rows.add(metric + "\ttrue\t" + trueCount);
        rows.add(metric + "\tfalse\t" + falseCount);
    }

    private static boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String value(String value) {
        return value == null || value.trim().isEmpty() ? "" : value.trim();
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\t', ' ')
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim();
    }

    private static String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        List<String> cleaned = new ArrayList<String>();
        for (String value : values) {
            if (hasText(value)) {
                cleaned.add(value.trim());
            }
        }
        Collections.sort(cleaned);
        return join(cleaned, ",");
    }

    private static String joinSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        List<String> sorted = new ArrayList<String>(values);
        Collections.sort(sorted);
        return join(sorted, ",");
    }

    private static String join(List<String> values, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(delimiter);
            }
            builder.append(clean(value));
        }
        return builder.toString();
    }

    private static String normalize(Path path) {
        return path.toString().replace(File.separatorChar, '/');
    }

    private interface KeyExtractor {
        String key(ManifestRecord record);
    }

    private interface BooleanExtractor {
        boolean value(ManifestRecord record);
    }

    private static class ManifestRecord {
        String relativePath = "";
        String sourceFormat = "";
        String extension = "";
        String parseStatus = "";
        String suspectedLayer = "";
        String parseMessage = "";
        String pocId = "";
        String pocName = "";
        String originalFormat = "";
        String protocol = "";
        String category = "";
        String severity = "";
        String inputType = "";
        String tags = "";
        String cveId = "";
        String cweId = "";
        String vulType = "";
        String product = "";
        int stepCount;
        String stepTypes = "";
        String matcherTypes = "";
        String extractorTypes = "";
        boolean requiresAuth;
        boolean requiresOob;
        boolean requiresHeadless;
        boolean requiresCode;
        boolean requiresFuzz;
        String dangerousIndicators = "";
        boolean hasFlow;
        boolean hasVariables;
        boolean hasConversionWarnings;
        boolean hasSemanticWarnings;
        boolean hasUnsupportedCapabilities;
        String validationBucket = "";
        String mockVerified = "";
        String dockerVerified = "";
        String onlineAuthorizedVerified = "";
        String manualReviewed = "";
        String blockedByEnv = "";
        String destructiveSkipped = "";
        long sizeBytes;

        String toTsv() {
            List<String> values = new ArrayList<String>();
            values.add(relativePath);
            values.add(sourceFormat);
            values.add(extension);
            values.add(parseStatus);
            values.add(suspectedLayer);
            values.add(parseMessage);
            values.add(pocId);
            values.add(pocName);
            values.add(originalFormat);
            values.add(protocol);
            values.add(category);
            values.add(severity);
            values.add(inputType);
            values.add(tags);
            values.add(cveId);
            values.add(cweId);
            values.add(vulType);
            values.add(product);
            values.add(String.valueOf(stepCount));
            values.add(stepTypes);
            values.add(matcherTypes);
            values.add(extractorTypes);
            values.add(String.valueOf(requiresAuth));
            values.add(String.valueOf(requiresOob));
            values.add(String.valueOf(requiresHeadless));
            values.add(String.valueOf(requiresCode));
            values.add(String.valueOf(requiresFuzz));
            values.add(dangerousIndicators);
            values.add(String.valueOf(hasFlow));
            values.add(String.valueOf(hasVariables));
            values.add(String.valueOf(hasConversionWarnings));
            values.add(String.valueOf(hasSemanticWarnings));
            values.add(String.valueOf(hasUnsupportedCapabilities));
            values.add(validationBucket);
            values.add(mockVerified);
            values.add(dockerVerified);
            values.add(onlineAuthorizedVerified);
            values.add(manualReviewed);
            values.add(blockedByEnv);
            values.add(destructiveSkipped);
            values.add(String.valueOf(sizeBytes));

            List<String> cleaned = new ArrayList<String>();
            for (String value : values) {
                cleaned.add(clean(value));
            }
            return join(cleaned, "\t");
        }
    }
}
