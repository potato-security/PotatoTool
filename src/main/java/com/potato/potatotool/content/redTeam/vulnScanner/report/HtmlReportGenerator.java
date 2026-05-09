package com.potato.potatotool.content.redTeam.vulnScanner.report;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.model.StepExecutionRecord;
import com.potato.potatotool.utils.report.PotatoReportBranding;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTML 报告生成器
 */
public class HtmlReportGenerator {

    private static final String TEMPLATE_PATH = "/templates/report-template-enhanced.html";
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final String ICON_TARGET = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2zm0 18c-4.4 0-8-3.6-8-8s3.6-8 8-8 8 3.6 8 8-3.6 8-8 8zm0-14c-3.3 0-6 2.7-6 6s2.7 6 6 6 6-2.7 6-6-2.7-6-6-6zm0 10c-2.2 0-4-1.8-4-4s1.8-4 4-4 4 1.8 4 4-1.8 4-4 4zm0-6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z\"/></svg></span>";
    private static final String ICON_TAG = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M21.4 11.6l-9-9C12.1 2.2 11.6 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .6.2 1.1.6 1.4l9 9c.4.4.9.6 1.4.6s1-.2 1.4-.6l7-7c.4-.4.6-.9.6-1.4s-.2-1-.6-1.4zM5.5 7C4.7 7 4 6.3 4 5.5S4.7 4 5.5 4 7 4.7 7 5.5 6.3 7 5.5 7z\"/></svg></span>";
    private static final String ICON_CLIPBOARD = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M19 3h-4.2C14.4 1.8 13.3 1 12 1s-2.4.8-2.8 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.6 0 1 .4 1 1s-.4 1-1 1-1-.4-1-1 .4-1 1-1zm7 16H5V5h2v3h10V5h2v14z\"/></svg></span>";
    private static final String ICON_PACKAGE = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M20 2H4c-1.1 0-2 .9-2 2v3h20V4c0-1.1-.9-2-2-2zm0 19H4c-1.1 0-2-.9-2-2V9h20v10c0 1.1-.9 2-2 2zM9 12H7v2h2v-2zm4 0h-2v2h2v-2zm4 0h-2v2h2v-2z\"/></svg></span>";
    private static final String ICON_CVE = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><circle cx=\"12\" cy=\"12\" r=\"10\" fill=\"#"
            + PotatoReportBranding.COLOR_CRITICAL + "\"/></svg></span>";
    private static final String ICON_CWE = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M12 2L2 12l10 10 10-10L12 2z\" fill=\"#"
            + PotatoReportBranding.COLOR_HIGH + "\"/></svg></span>";
    private static final String ICON_CHART = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M5 9.2h3V19H5zM10.6 5h2.8v14h-2.8zm5.6 8H19v6h-2.8z\"/></svg></span>";
    private static final String ICON_SEARCH = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M15.5 14h-.8l-.3-.3c1-1.1 1.6-2.6 1.6-4.2C16 5.9 13.1 3 9.5 3S3 5.9 3 9.5 5.9 16 9.5 16c1.6 0 3.1-.6 4.2-1.6l.3.3v.8l5 5 1.5-1.5-5-5zm-6 0C7 14 5 12 5 9.5S7 5 9.5 5 14 7 14 9.5 12 14 9.5 14z\"/></svg></span>";
    private static final String ICON_SYRINGE = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M11 2v4.5l2 2V2h-2zm-2 7.5L4.5 14 2 14v2l2.5 0 1.3-1.3L8 17l1.5-1.5-2.2-2.2L11 9.5 9 7.5zm8.5-1L15 11l2.2 2.2-1.5 1.5L13.5 13l-4.5 4.5L7.5 19l1.5 1.5 1.5-1.5L15 14.5l2.2 2.2 1.5-1.5L16.5 13l2.5-2.5-1.5-1.5z\"/></svg></span>";
    private static final String ICON_KEY = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M12.65 10a6 6 0 1 0 0 4H17v4h4v-4h2v-4H12.65zM7 14a2 2 0 1 1 0-4 2 2 0 0 1 0 4z\"/></svg></span>";
    private static final String ICON_EDIT = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M3 17.25V21h3.75L17.8 9.94l-3.75-3.75L3 17.25zM20.7 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z\"/></svg></span>";
    private static final String ICON_UPLOAD = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M9 16h6v-6h4l-7-7-7 7h4v6zm-4 2h14v2H5v-2z\"/></svg></span>";
    private static final String ICON_DOWNLOAD = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z\"/></svg></span>";
    private static final String ICON_BULB = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M9 21c0 .5.4 1 1 1h4c.6 0 1-.5 1-1v-1H9v1zm3-19C8.1 2 5 5.1 5 9c0 2.4 1.2 4.5 3 5.7V17c0 .5.4 1 1 1h6c.6 0 1-.5 1-1v-2.3c1.8-1.3 3-3.4 3-5.7 0-3.9-3.1-7-7-7z\"/></svg></span>";

    public File generate(List<ScanResult> results, String outputPath, String scanDuration) throws IOException {
        String template = loadTemplate();
        Map<String, Object> variables = buildVariables(results, scanDuration);
        String html = replaceTemplateVariables(template, variables);
        html = html.replace("{{VULNERABILITY_LIST}}", generateVulnListHtml(results));

        File outputFile = new File(outputPath);
        if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            writer.write(html);
        }
        return outputFile;
    }

    public File generate(List<ScanResult> results, String outputPath) throws IOException {
        return generate(results, outputPath, "未知");
    }

    private String loadTemplate() throws IOException {
        InputStream is = getClass().getResourceAsStream(TEMPLATE_PATH);
        if (is == null) {
            throw new IOException("报告模板不存在: " + TEMPLATE_PATH);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        }
    }

    private Map<String, Object> buildVariables(List<ScanResult> results, String scanDuration) {
        Map<String, Object> vars = new HashMap<String, Object>();

        int targetCount = distinctTargetCount(results);
        int pocCount = distinctPocCount(results);
        int vulnCount = results == null ? 0 : results.size();

        Map<PocObj.Severity, Long> severityCount = new HashMap<PocObj.Severity, Long>();
        if (results != null) {
            for (ScanResult result : results) {
                PocObj.Severity severity = result.getPoc().getSeverity();
                Long current = severityCount.get(severity);
                severityCount.put(severity, current == null ? 1L : current + 1L);
            }
        }

        long critical = getSeverityCount(severityCount, PocObj.Severity.CRITICAL);
        long high = getSeverityCount(severityCount, PocObj.Severity.HIGH);
        long medium = getSeverityCount(severityCount, PocObj.Severity.MEDIUM);
        long low = getSeverityCount(severityCount, PocObj.Severity.LOW);
        long info = getSeverityCount(severityCount, PocObj.Severity.INFO);
        long urgentCount = critical + high;
        long total = critical + high + medium + low + info;

        vars.put("REPORT_DATE", sdf.format(new Date()));
        vars.put("SCAN_DURATION", scanDuration == null || scanDuration.trim().isEmpty() ? "未知" : scanDuration);
        vars.put("TARGET_COUNT", targetCount);
        vars.put("POC_COUNT", pocCount);
        vars.put("VULN_COUNT", vulnCount);
        vars.put("CRITICAL_AND_HIGH_COUNT", urgentCount);

        vars.put("SEVERITY_CRITICAL", critical);
        vars.put("SEVERITY_HIGH", high);
        vars.put("SEVERITY_MEDIUM", medium);
        vars.put("SEVERITY_LOW", low);
        vars.put("SEVERITY_INFO", info);

        vars.put("CRITICAL_PERCENT", formatPercent(critical, total));
        vars.put("HIGH_PERCENT", formatPercent(high, total));
        vars.put("MEDIUM_PERCENT", formatPercent(medium, total));
        vars.put("LOW_PERCENT", formatPercent(low, total));
        vars.put("INFO_PERCENT", formatPercent(info, total));

        double criticalDeg = total > 0 ? (critical * 360.0) / total : 0;
        double highDeg = total > 0 ? criticalDeg + (high * 360.0) / total : 0;
        double mediumDeg = total > 0 ? highDeg + (medium * 360.0) / total : 0;
        double lowDeg = total > 0 ? mediumDeg + (low * 360.0) / total : 0;
        vars.put("CRITICAL_DEG", formatDegree(criticalDeg));
        vars.put("HIGH_DEG", formatDegree(highDeg));
        vars.put("MEDIUM_DEG", formatDegree(mediumDeg));
        vars.put("LOW_DEG", formatDegree(lowDeg));

        long maxCount = Math.max(critical, Math.max(high, Math.max(medium, Math.max(low, info))));
        long chartCeiling = resolveChartCeiling(maxCount);
        vars.put("BAR_CHART_CEILING", chartCeiling);
        vars.put("BAR_CHART_TICK_HIGH", formatAxisTick(chartCeiling));
        vars.put("BAR_CHART_TICK_MID_HIGH", formatAxisTick(chartCeiling * 0.75d));
        vars.put("BAR_CHART_TICK_MID", formatAxisTick(chartCeiling * 0.5d));
        vars.put("BAR_CHART_TICK_LOW", formatAxisTick(chartCeiling * 0.25d));

        vars.put("SEVERITY_CRITICAL_PCT", formatBarPercent(critical, chartCeiling));
        vars.put("SEVERITY_HIGH_PCT", formatBarPercent(high, chartCeiling));
        vars.put("SEVERITY_MEDIUM_PCT", formatBarPercent(medium, chartCeiling));
        vars.put("SEVERITY_LOW_PCT", formatBarPercent(low, chartCeiling));
        vars.put("SEVERITY_INFO_PCT", formatBarPercent(info, chartCeiling));

        vars.put("RISK_LEVEL", resolveRiskLevel(critical, high, medium, low, info));
        vars.put("RISK_CLASS", resolveRiskClass(critical, high, medium, low, info));
        vars.put("RISK_SUMMARY", buildRiskSummary(vulnCount, targetCount, urgentCount));
        vars.put("ACTION_GUIDE", buildActionGuide(vulnCount, urgentCount));
        vars.put("REPORT_STATUS", vulnCount > 0 ? "存在待处置风险" : "未发现导出结果");

        vars.put("ICON_DATA_URI", PotatoReportBranding.getIconDataUri());
        vars.put("POTATO_WORDMARK_DATA_URI", PotatoReportBranding.getPotatoWordmarkDataUri());
        vars.put("TOOL_WORDMARK_DATA_URI", PotatoReportBranding.getToolWordmarkDataUri());

        vars.put("BRAND_PRIMARY", "#" + PotatoReportBranding.COLOR_PRIMARY);
        vars.put("BRAND_PRIMARY_DARK", "#" + PotatoReportBranding.COLOR_PRIMARY_DARK);
        vars.put("BRAND_PRIMARY_SOFT", "#" + PotatoReportBranding.COLOR_PRIMARY_SOFT);
        vars.put("BRAND_ACCENT", "#" + PotatoReportBranding.COLOR_ACCENT);
        vars.put("BRAND_ACCENT_SOFT", "#" + PotatoReportBranding.COLOR_ACCENT_SOFT);
        vars.put("BRAND_BG", "#" + PotatoReportBranding.COLOR_BACKGROUND);
        vars.put("BRAND_SURFACE", "#" + PotatoReportBranding.COLOR_SURFACE);
        vars.put("BRAND_SURFACE_SOFT", "#" + PotatoReportBranding.COLOR_SURFACE_SOFT);
        vars.put("BRAND_BORDER", "#" + PotatoReportBranding.COLOR_BORDER);
        vars.put("BRAND_TEXT", "#" + PotatoReportBranding.COLOR_TEXT_DARK);
        vars.put("BRAND_MUTED", "#" + PotatoReportBranding.COLOR_TEXT_MUTED);
        vars.put("BRAND_CODE_BG", "#" + PotatoReportBranding.COLOR_CODE_BG);
        vars.put("BRAND_SUCCESS", "#" + PotatoReportBranding.COLOR_SUCCESS);
        vars.put("BRAND_SUCCESS_SOFT", "#" + PotatoReportBranding.COLOR_SUCCESS_SOFT);
        vars.put("BRAND_CRITICAL", "#" + PotatoReportBranding.COLOR_CRITICAL);
        vars.put("BRAND_CRITICAL_SOFT", "#" + PotatoReportBranding.COLOR_CRITICAL_SOFT);
        vars.put("BRAND_HIGH", "#" + PotatoReportBranding.COLOR_HIGH);
        vars.put("BRAND_HIGH_SOFT", "#" + PotatoReportBranding.COLOR_HIGH_SOFT);
        vars.put("BRAND_MEDIUM", "#" + PotatoReportBranding.COLOR_MEDIUM);
        vars.put("BRAND_MEDIUM_SOFT", "#" + PotatoReportBranding.COLOR_MEDIUM_SOFT);
        vars.put("BRAND_LOW", "#" + PotatoReportBranding.COLOR_LOW);
        vars.put("BRAND_LOW_SOFT", "#" + PotatoReportBranding.COLOR_LOW_SOFT);
        vars.put("BRAND_INFO", "#" + PotatoReportBranding.COLOR_INFO);
        vars.put("BRAND_INFO_SOFT", "#" + PotatoReportBranding.COLOR_INFO_SOFT);

        return vars;
    }

    private String replaceTemplateVariables(String template, Map<String, Object> variables) {
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return result;
    }

    private String generateVulnListHtml(List<ScanResult> results) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            ScanResult result = results.get(i);
            PocObj.Poc poc = result.getPoc();
            String severity = poc.getSeverity().name().toLowerCase();

            sb.append("<article class=\"vuln-item ").append(severity).append("\" id=\"vuln-").append(i)
                    .append("\" data-severity=\"").append(severity)
                    .append("\" onclick=\"toggleDetails(").append(i).append(")\" tabindex=\"0\" role=\"button\" aria-expanded=\"false\">\n");

            sb.append("  <div class=\"vuln-kicker\">\n");
            sb.append("    <span class=\"kicker-index\">#").append(i + 1).append("</span>\n");
            if (hasText(poc.getProtocol())) {
                sb.append("    <span class=\"kicker-chip\">").append(escapeHtml(poc.getProtocol())).append("</span>\n");
            }
            if (hasText(poc.getOriginalFormat())) {
                sb.append("    <span class=\"kicker-chip\">").append(escapeHtml(poc.getOriginalFormat())).append("</span>\n");
            }
            sb.append("  </div>\n");

            sb.append("  <div class=\"vuln-header\">\n");
            sb.append("    <div class=\"vuln-title\">").append(escapeHtml(poc.getName())).append("</div>\n");
            sb.append("    <div class=\"vuln-badges\">\n");
            sb.append("      <span class=\"severity-badge ").append(severity).append("\">")
                    .append(escapeHtml(poc.getSeverity().name())).append("</span>\n");
            sb.append("      <span class=\"expand-badge\">点击展开证据</span>\n");
            sb.append("    </div>\n");
            sb.append("  </div>\n");

            if (hasText(poc.getDescription())) {
                sb.append("  <div class=\"vuln-description\"><strong>漏洞说明：</strong>")
                        .append(escapeHtml(poc.getDescription())).append("</div>\n");
            }

            sb.append("  <div class=\"vuln-meta\">\n");
            appendMetaItem(sb, ICON_TARGET, "目标", result.getTarget());
            appendMetaItem(sb, ICON_TAG, "POC ID", poc.getId());
            appendMetaItem(sb, ICON_CLIPBOARD, "类型", poc.getVulType());
            appendMetaItem(sb, ICON_PACKAGE, "格式", poc.getOriginalFormat());
            appendMetaItem(sb, ICON_CVE, "CVE", result.getCveId());
            appendMetaItem(sb, ICON_CWE, "CWE", result.getCweId());
            appendMetaItem(sb, ICON_CHART, "CVSS", result.getCvssScore());
            sb.append("  </div>\n");

            sb.append("  <div class=\"vuln-details\" id=\"details-").append(i).append("\">\n");

            appendDetailBlock(sb, ICON_SEARCH, "检测路径", result.getMatchedPath(), false);
            appendDetailBlock(sb, ICON_SYRINGE, "检测 Payload", result.getMatchedPayload(), true);
            appendDetailBlock(sb, ICON_KEY, "参数键名", result.getFormattedParamKeys(), false);
            appendDetailBlock(sb, ICON_EDIT, "变量值", result.getFormattedVariableValues(), true);
            appendDetailBlock(sb, ICON_UPLOAD, "提取数据", result.getFormattedOutputData(), true);
            appendDetailBlock(sb, ICON_BULB, "修复建议", result.getRecommendation(), false);

            List<StepExecutionRecord> stepRecords = result.getStepRecords();
            if (stepRecords != null && !stepRecords.isEmpty()) {
                sb.append("    <section class=\"detail-section\">\n");
                sb.append("      <h4 class=\"detail-section-title\">").append(ICON_CLIPBOARD).append(" 执行链路</h4>\n");
                for (int j = 0; j < stepRecords.size(); j++) {
                    StepExecutionRecord record = stepRecords.get(j);
                    sb.append("      <div class=\"step-record\">\n");
                    sb.append("        <div class=\"step-header\">步骤 ").append(j + 1);
                    if (record.isMatched()) {
                        sb.append(" <span class=\"step-matched\">已命中</span>");
                    }
                    sb.append("</div>\n");
                    sb.append("        <div class=\"step-info\">\n");
                    sb.append("          <div><strong>URL：</strong>").append(escapeHtml(record.getRequestUrl())).append("</div>\n");
                    sb.append("          <div><strong>方法：</strong>").append(escapeHtml(record.getRequestMethod())).append("</div>\n");
                    sb.append("          <div><strong>状态码：</strong>").append(record.getResponseCode()).append("</div>\n");
                    sb.append("          <div><strong>响应时间：</strong>").append(record.getResponseTime()).append(" ms</div>\n");
                    Map<String, Object> extractedVars = record.getExtractedVariables();
                    if (extractedVars != null && !extractedVars.isEmpty()) {
                        sb.append("          <div><strong>提取变量：</strong>").append(escapeHtml(mapToText(extractedVars))).append("</div>\n");
                    }
                    sb.append("        </div>\n");

                    appendDetailsBlock(sb, "请求头", mapToText(record.getRequestHeaders()), false);
                    appendDetailsBlock(sb, "请求体", trim(record.getRequestBody(), 1500), true);
                    appendDetailsBlock(sb, "响应头", mapToText(record.getResponseHeaders()), false);
                    appendDetailsBlock(sb, "响应体", trim(record.getResponseBody(), 1500), true);
                    sb.append("      </div>\n");
                }
                sb.append("    </section>\n");
            }

            appendDetailBlock(sb, ICON_UPLOAD, "HTTP 请求", result.getRawRequest(), true);
            appendDetailBlock(sb, ICON_DOWNLOAD, "HTTP 响应片段", result.getRawResponseSnippet(), true);

            sb.append("  </div>\n");
            sb.append("</article>\n");
        }
        return sb.toString();
    }

    private void appendMetaItem(StringBuilder sb, String icon, String label, String value) {
        if (!hasText(value)) {
            return;
        }
        sb.append("    <div class=\"vuln-meta-item\">\n");
        sb.append("      <span class=\"vuln-meta-label\">").append(icon).append(" ").append(escapeHtml(label)).append("</span>\n");
        sb.append("      <span class=\"vuln-meta-value\">").append(escapeHtml(value)).append("</span>\n");
        sb.append("    </div>\n");
    }

    private void appendDetailBlock(StringBuilder sb, String icon, String title, String value, boolean codeBlock) {
        if (!hasText(value)) {
            return;
        }
        sb.append("    <section class=\"detail-section\">\n");
        sb.append("      <h4 class=\"detail-section-title\">").append(icon).append(" ").append(escapeHtml(title)).append("</h4>\n");
        if (codeBlock) {
            sb.append("      <pre class=\"code-block\">").append(escapeHtml(trim(value, 5000))).append("</pre>\n");
        } else {
            sb.append("      <div class=\"detail-content\">").append(escapeHtml(value)).append("</div>\n");
        }
        sb.append("    </section>\n");
    }

    private void appendDetailsBlock(StringBuilder sb, String summary, String content, boolean code) {
        if (!hasText(content)) {
            return;
        }
        sb.append("        <details class=\"step-details\">\n");
        sb.append("          <summary>").append(escapeHtml(summary)).append("</summary>\n");
        if (code) {
            sb.append("          <pre class=\"code-block\">").append(escapeHtml(content)).append("</pre>\n");
        } else {
            sb.append("          <div class=\"detail-content\">").append(escapeHtml(content)).append("</div>\n");
        }
        sb.append("        </details>\n");
    }

    private int distinctTargetCount(List<ScanResult> results) {
        List<String> targets = new ArrayList<String>();
        if (results != null) {
            for (ScanResult result : results) {
                String target = safeText(result.getTarget());
                if (!target.isEmpty() && !targets.contains(target)) {
                    targets.add(target);
                }
            }
        }
        return targets.size();
    }

    private int distinctPocCount(List<ScanResult> results) {
        List<String> ids = new ArrayList<String>();
        if (results != null) {
            for (ScanResult result : results) {
                String id = safeText(result.getPoc().getId());
                if (!id.isEmpty() && !ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        return ids.size();
    }

    private long getSeverityCount(Map<PocObj.Severity, Long> severityCount, PocObj.Severity severity) {
        Long count = severityCount.get(severity);
        return count == null ? 0L : count.longValue();
    }

    private String formatPercent(long count, long total) {
        if (total <= 0) {
            return "0.0";
        }
        return String.format("%.1f", count * 100.0 / total);
    }

    private String formatDegree(double degree) {
        return String.format("%.1fdeg", degree);
    }

    private String formatBarPercent(long count, long chartCeiling) {
        if (chartCeiling <= 0 || count <= 0) {
            return "0";
        }
        double percent = count * 100.0 / chartCeiling;
        return String.format("%.1f", percent);
    }

    private String formatAxisTick(double value) {
        return String.valueOf(Math.max(0L, Math.round(value)));
    }

    private long resolveChartCeiling(long maxCount) {
        if (maxCount <= 0) {
            return 4L;
        }

        long padded = maxCount <= 4 ? 4L : (long) Math.ceil(maxCount * 1.15d);
        return roundUp(padded, 4L);
    }

    private long roundUp(long value, long step) {
        if (step <= 1) {
            return value;
        }
        return ((value + step - 1) / step) * step;
    }

    private String resolveRiskLevel(long critical, long high, long medium, long low, long info) {
        if (critical > 0) {
            return "极高";
        }
        if (high > 0) {
            return "高";
        }
        if (medium > 0) {
            return "中";
        }
        if (low > 0) {
            return "低";
        }
        return info > 0 ? "信息" : "无结果";
    }

    private String resolveRiskClass(long critical, long high, long medium, long low, long info) {
        if (critical > 0) {
            return "critical";
        }
        if (high > 0) {
            return "high";
        }
        if (medium > 0) {
            return "medium";
        }
        if (low > 0) {
            return "low";
        }
        return info > 0 ? "info" : "neutral";
    }

    private String buildRiskSummary(int vulnCount, int targetCount, long urgentCount) {
        if (urgentCount > 0) {
            return "本次共发现 " + vulnCount + " 条漏洞结果，覆盖 " + targetCount
                    + " 个目标，其中 " + urgentCount + " 条为高优先级风险，建议立即完成复核与修复。";
        }
        if (vulnCount > 0) {
            return "本次共发现 " + vulnCount + " 条漏洞结果，当前未出现高优先级风险，但仍需结合资产重要性持续验证与处置。";
        }
        return "当前未发现可导出的漏洞结果，可保留本次配置作为后续复测基线。";
    }

    private String buildActionGuide(int vulnCount, long urgentCount) {
        if (urgentCount > 0) {
            return "建议优先按 CRITICAL / HIGH 分级建单，先处理外网暴露资产、核心业务系统及存在公开编号的漏洞。";
        }
        if (vulnCount > 0) {
            return "建议继续结合业务影响、资产暴露面和历史漏洞趋势完成分级处置，并留存证据用于复测。";
        }
        return "建议后续在指纹、POC 或资产版本变化后重新执行扫描，并对关键资产保持周期性核验。";
    }

    private String mapToText(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(String.valueOf(entry.getKey())).append(": ").append(String.valueOf(entry.getValue()));
        }
        return builder.toString();
    }

    private String trim(String text, int maxLength) {
        String value = text == null ? "" : text;
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "\n...(内容截断，共 " + value.length() + " 字符)";
    }

    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }

    private String escapeHtml(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
