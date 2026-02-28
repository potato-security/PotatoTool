package com.potato.potatotool.content.redTeam.vulnScanner.report;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.model.StepExecutionRecord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HTML报告生成器 - 使用增强版模板
 * 生成专业的暗色主题HTML漏洞扫描报告
 *
 * @author Potato
 * @date 2025/01/06
 */
public class HtmlReportGenerator {

    private static final String TEMPLATE_PATH = "/templates/report-template-enhanced.html";
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // SVG 图标常量（替代 emoji）
    private static final String ICON_TARGET = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2zm0 18c-4.4 0-8-3.6-8-8s3.6-8 8-8 8 3.6 8 8-3.6 8-8 8zm0-14c-3.3 0-6 2.7-6 6s2.7 6 6 6 6-2.7 6-6-2.7-6-6-6zm0 10c-2.2 0-4-1.8-4-4s1.8-4 4-4 4 1.8 4 4-1.8 4-4 4zm0-6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z\"/></svg></span>";
    private static final String ICON_TAG = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M21.4 11.6l-9-9C12.1 2.2 11.6 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .6.2 1.1.6 1.4l9 9c.4.4.9.6 1.4.6s1-.2 1.4-.6l7-7c.4-.4.6-.9.6-1.4s-.2-1-.6-1.4zM5.5 7C4.7 7 4 6.3 4 5.5S4.7 4 5.5 4 7 4.7 7 5.5 6.3 7 5.5 7z\"/></svg></span>";
    private static final String ICON_CLIPBOARD = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M19 3h-4.2C14.4 1.8 13.3 1 12 1s-2.4.8-2.8 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.6 0 1 .4 1 1s-.4 1-1 1-1-.4-1-1 .4-1 1-1zm7 16H5V5h2v3h10V5h2v14z\"/></svg></span>";
    private static final String ICON_PACKAGE = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M20 2H4c-1.1 0-2 .9-2 2v3h20V4c0-1.1-.9-2-2-2zm0 19H4c-1.1 0-2-.9-2-2V9h20v10c0 1.1-.9 2-2 2zM9 12H7v2h2v-2zm4 0h-2v2h2v-2zm4 0h-2v2h2v-2z\"/></svg></span>";
    private static final String ICON_CVE = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><circle cx=\"12\" cy=\"12\" r=\"10\" fill=\"%23DC2626\"/></svg></span>";
    private static final String ICON_CWE = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M12 2L2 12l10 10 10-10L12 2z\" fill=\"%23EA580C\"/></svg></span>";
    private static final String ICON_CHART = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M5 9.2h3V19H5zM10.6 5h2.8v14h-2.8zm5.6 8H19v6h-2.8z\"/></svg></span>";
    private static final String ICON_SEARCH = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M15.5 14h-.8l-.3-.3c1-1.1 1.6-2.6 1.6-4.2C16 5.9 13.1 3 9.5 3S3 5.9 3 9.5 5.9 16 9.5 16c1.6 0 3.1-.6 4.2-1.6l.3.3v.8l5 5 1.5-1.5-5-5zm-6 0C7 14 5 12 5 9.5S7 5 9.5 5 14 7 14 9.5 12 14 9.5 14z\"/></svg></span>";
    private static final String ICON_SYRINGE = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M11 2v4.5l2 2V2h-2zm-2 7.5L4.5 14 2 14v2l2.5 0 1.3-1.3L8 17l1.5-1.5-2.2-2.2L11 9.5 9 7.5zm8.5-1L15 11l2.2 2.2-1.5 1.5L13.5 13l-4.5 4.5L7.5 19l1.5 1.5 1.5-1.5L15 14.5l2.2 2.2 1.5-1.5L16.5 13l2.5-2.5-1.5-1.5z\"/></svg></span>";
    private static final String ICON_KEY = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M12.65 10a6 6 0 1 0 0 4H17v4h4v-4h2v-4H12.65zM7 14a2 2 0 1 1 0-4 2 2 0 0 1 0 4z\"/></svg></span>";
    private static final String ICON_EDIT = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M3 17.25V21h3.75L17.8 9.94l-3.75-3.75L3 17.25zM20.7 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z\"/></svg></span>";
    private static final String ICON_UPLOAD = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M9 16h6v-6h4l-7-7-7 7h4v6zm-4 2h14v2H5v-2z\"/></svg></span>";
    private static final String ICON_DOWNLOAD = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z\"/></svg></span>";
    private static final String ICON_BULB = "<span class=\"icon\"><svg viewBox=\"0 0 24 24\"><path d=\"M9 21c0 .5.4 1 1 1h4c.6 0 1-.5 1-1v-1H9v1zm3-19C8.1 2 5 5.1 5 9c0 2.4 1.2 4.5 3 5.7V17c0 .5.4 1 1 1h6c.6 0 1-.5 1-1v-2.3c1.8-1.3 3-3.4 3-5.7 0-3.9-3.1-7-7-7z\"/></svg></span>";

    /**
     * 生成HTML报告
     *
     * @param results 扫描结果列表
     * @param outputPath 输出文件路径
     * @param scanDuration 扫描耗时(格式: "05分32秒")
     * @return 生成的报告文件
     * @throws IOException IO异常
     */
    public File generate(List<ScanResult> results, String outputPath, String scanDuration) throws IOException {
        // 加载模板
        String template = loadTemplate();

        // 计算统计数据
        Map<String, Object> variables = buildVariables(results, scanDuration);

        // 替换模板变量
        String html = replaceTemplateVariables(template, variables);

        // 生成漏洞列表HTML
        String vulnListHtml = generateVulnListHtml(results);
        html = html.replace("{{VULNERABILITY_LIST}}", vulnListHtml);

        // 写入文件
        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            writer.write(html);
        }

        return outputFile;
    }

    /**
     * 兼容方法 - 自动计算扫描耗时
     */
    public File generate(List<ScanResult> results, String outputPath) throws IOException {
        return generate(results, outputPath, "未知");
    }

    /**
     * 从resources加载HTML模板
     */
    private String loadTemplate() throws IOException {
        try (InputStream is = getClass().getResourceAsStream(TEMPLATE_PATH);
             BufferedReader reader = new BufferedReader(
                 new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * 构建模板变量映射
     */
    private Map<String, Object> buildVariables(List<ScanResult> results, String scanDuration) {
        Map<String, Object> vars = new HashMap<>();

        // 基础统计
        int targetCount = (int) results.stream()
            .map(ScanResult::getTarget)
            .distinct()
            .count();
        int pocCount = (int) results.stream()
            .map(r -> r.getPoc().getId())
            .distinct()
            .count();
        int vulnCount = results.size();

        // 按严重度统计
        Map<PocObj.Severity, Long> severityCount = new HashMap<>();
        for (ScanResult result : results) {
            PocObj.Severity severity = result.getPoc().getSeverity();
            severityCount.put(severity, severityCount.getOrDefault(severity, 0L) + 1);
        }

        // 高危漏洞数 (CRITICAL + HIGH)
        long criticalAndHighCount = severityCount.getOrDefault(PocObj.Severity.CRITICAL, 0L)
                                  + severityCount.getOrDefault(PocObj.Severity.HIGH, 0L);

        // 填充基础变量
        vars.put("REPORT_DATE", sdf.format(new Date()));
        vars.put("SCAN_DURATION", scanDuration);

        // 第一组：基础统计
        vars.put("TARGET_COUNT", targetCount);
        vars.put("POC_COUNT", pocCount);
        vars.put("VULN_COUNT", vulnCount);
        vars.put("CRITICAL_AND_HIGH_COUNT", criticalAndHighCount); // 高危漏洞数

        // 第二组：严重度分布
        long critical = severityCount.getOrDefault(PocObj.Severity.CRITICAL, 0L);
        long high = severityCount.getOrDefault(PocObj.Severity.HIGH, 0L);
        long medium = severityCount.getOrDefault(PocObj.Severity.MEDIUM, 0L);
        long low = severityCount.getOrDefault(PocObj.Severity.LOW, 0L);
        long info = severityCount.getOrDefault(PocObj.Severity.INFO, 0L);

        vars.put("SEVERITY_CRITICAL", critical);
        vars.put("SEVERITY_HIGH", high);
        vars.put("SEVERITY_MEDIUM", medium);
        vars.put("SEVERITY_LOW", low);
        vars.put("SEVERITY_INFO", info);

        // 计算图表数据
        long total = critical + high + medium + low + info;

        // 计算百分比占比（用于饼图图例）
        if (total > 0) {
            vars.put("CRITICAL_PERCENT", String.format("%.1f", (critical * 100.0) / total));
            vars.put("HIGH_PERCENT", String.format("%.1f", (high * 100.0) / total));
            vars.put("MEDIUM_PERCENT", String.format("%.1f", (medium * 100.0) / total));
            vars.put("LOW_PERCENT", String.format("%.1f", (low * 100.0) / total));
            vars.put("INFO_PERCENT", String.format("%.1f", (info * 100.0) / total));
        } else {
            vars.put("CRITICAL_PERCENT", "0.0");
            vars.put("HIGH_PERCENT", "0.0");
            vars.put("MEDIUM_PERCENT", "0.0");
            vars.put("LOW_PERCENT", "0.0");
            vars.put("INFO_PERCENT", "0.0");
        }

        if (total > 0) {
            // 饼图角度计算（360度分布）
            double criticalDeg = (critical * 360.0) / total;
            double highDeg = criticalDeg + (high * 360.0) / total;
            double mediumDeg = highDeg + (medium * 360.0) / total;
            double lowDeg = mediumDeg + (low * 360.0) / total;

            // CSS变量用于饼图
            vars.put("CRITICAL_DEG", String.format("%.1fdeg", criticalDeg));
            vars.put("HIGH_DEG", String.format("%.1fdeg", highDeg));
            vars.put("MEDIUM_DEG", String.format("%.1fdeg", mediumDeg));
            vars.put("LOW_DEG", String.format("%.1fdeg", lowDeg));

            // 柱状图百分比计算（基于最大值，保留小数精度）
            long maxCount = Math.max(critical, Math.max(high, Math.max(medium, Math.max(low, info))));
            if (maxCount > 0) {
                // 计算百分比，保留小数，确保最小值至少有5%的高度（便于可见）
                double criticalPct = (critical * 100.0) / maxCount;
                double highPct = (high * 100.0) / maxCount;
                double mediumPct = (medium * 100.0) / maxCount;
                double lowPct = (low * 100.0) / maxCount;
                double infoPct = (info * 100.0) / maxCount;

                // 如果值大于0但计算出的百分比小于5%，设置为5%以确保可见
                if (critical > 0 && criticalPct < 5) criticalPct = 5;
                if (high > 0 && highPct < 5) highPct = 5;
                if (medium > 0 && mediumPct < 5) mediumPct = 5;
                if (low > 0 && lowPct < 5) lowPct = 5;
                if (info > 0 && infoPct < 5) infoPct = 5;

                vars.put("SEVERITY_CRITICAL_PCT", String.format("%.1f", criticalPct));
                vars.put("SEVERITY_HIGH_PCT", String.format("%.1f", highPct));
                vars.put("SEVERITY_MEDIUM_PCT", String.format("%.1f", mediumPct));
                vars.put("SEVERITY_LOW_PCT", String.format("%.1f", lowPct));
                vars.put("SEVERITY_INFO_PCT", String.format("%.1f", infoPct));
            } else {
                vars.put("SEVERITY_CRITICAL_PCT", "0");
                vars.put("SEVERITY_HIGH_PCT", "0");
                vars.put("SEVERITY_MEDIUM_PCT", "0");
                vars.put("SEVERITY_LOW_PCT", "0");
                vars.put("SEVERITY_INFO_PCT", "0");
            }
        } else {
            // 无数据时的默认值
            vars.put("CRITICAL_DEG", "0deg");
            vars.put("HIGH_DEG", "0deg");
            vars.put("MEDIUM_DEG", "0deg");
            vars.put("LOW_DEG", "0deg");
            vars.put("SEVERITY_CRITICAL_PCT", 0);
            vars.put("SEVERITY_HIGH_PCT", 0);
            vars.put("SEVERITY_MEDIUM_PCT", 0);
            vars.put("SEVERITY_LOW_PCT", 0);
            vars.put("SEVERITY_INFO_PCT", 0);
        }

        return vars;
    }

    /**
     * 替换模板中的变量占位符
     */
    private String replaceTemplateVariables(String template, Map<String, Object> variables) {
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = String.valueOf(entry.getValue());
            result = result.replace(placeholder, value);
        }
        return result;
    }

    /**
     * 生成漏洞列表HTML内容
     */
    private String generateVulnListHtml(List<ScanResult> results) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < results.size(); i++) {
            ScanResult result = results.get(i);
            PocObj.Poc poc = result.getPoc();
            String severity = poc.getSeverity().name().toLowerCase();

            // 漏洞卡片容器
            sb.append("<div class=\"vuln-item ").append(severity).append("\" id=\"vuln-").append(i)
              .append("\" onclick=\"toggleDetails(").append(i).append(")\" tabindex=\"0\" role=\"button\" aria-expanded=\"false\">\n");

            // 卡片头部
            sb.append("    <div class=\"vuln-header\">\n");
            sb.append("        <div class=\"vuln-title\">\n");
            sb.append("            ").append(escapeHtml(poc.getName())).append("\n");
            sb.append("            <span class=\"severity-badge ").append(severity).append("\">")
              .append(poc.getSeverity().name()).append("</span>\n");
            sb.append("        </div>\n");
            sb.append("    </div>\n");

            // 元数据行
            sb.append("    <div class=\"vuln-meta\">\n");
            sb.append("        <div class=\"vuln-meta-item\">\n");
            sb.append("            <span class=\"vuln-meta-label\">").append(ICON_TARGET).append(" 目标:</span>\n");
            sb.append("            <span class=\"vuln-meta-value\">").append(escapeHtml(result.getTarget())).append("</span>\n");
            sb.append("        </div>\n");
            sb.append("        <div class=\"vuln-meta-item\">\n");
            sb.append("            <span class=\"vuln-meta-label\">").append(ICON_TAG).append(" POC ID:</span>\n");
            sb.append("            <span class=\"vuln-meta-value\">").append(escapeHtml(poc.getId())).append("</span>\n");
            sb.append("        </div>\n");

            if (poc.getVulType() != null && !poc.getVulType().isEmpty()) {
                sb.append("        <div class=\"vuln-meta-item\">\n");
                sb.append("            <span class=\"vuln-meta-label\">").append(ICON_CLIPBOARD).append(" 类型:</span>\n");
                sb.append("            <span class=\"vuln-meta-value\">").append(escapeHtml(poc.getVulType())).append("</span>\n");
                sb.append("        </div>\n");
            }

            if (poc.getOriginalFormat() != null) {
                sb.append("        <div class=\"vuln-meta-item\">\n");
                sb.append("            <span class=\"vuln-meta-label\">").append(ICON_PACKAGE).append(" 格式:</span>\n");
                sb.append("            <span class=\"vuln-meta-value\">").append(escapeHtml(poc.getOriginalFormat())).append("</span>\n");
                sb.append("        </div>\n");
            }

            // CVE/CWE/CVSS 信息
            if (result.getCveId() != null && !result.getCveId().isEmpty()) {
                sb.append("        <div class=\"vuln-meta-item\">\n");
                sb.append("            <span class=\"vuln-meta-label\">").append(ICON_CVE).append(" CVE:</span>\n");
                sb.append("            <span class=\"vuln-meta-value\">").append(escapeHtml(result.getCveId())).append("</span>\n");
                sb.append("        </div>\n");
            }

            if (result.getCweId() != null && !result.getCweId().isEmpty()) {
                sb.append("        <div class=\"vuln-meta-item\">\n");
                sb.append("            <span class=\"vuln-meta-label\">").append(ICON_CWE).append(" CWE:</span>\n");
                sb.append("            <span class=\"vuln-meta-value\">").append(escapeHtml(result.getCweId())).append("</span>\n");
                sb.append("        </div>\n");
            }

            if (result.getCvssScore() != null && !result.getCvssScore().isEmpty()) {
                sb.append("        <div class=\"vuln-meta-item\">\n");
                sb.append("            <span class=\"vuln-meta-label\">").append(ICON_CHART).append(" CVSS:</span>\n");
                sb.append("            <span class=\"vuln-meta-value\">").append(escapeHtml(result.getCvssScore())).append("</span>\n");
                sb.append("        </div>\n");
            }

            sb.append("    </div>\n");

            // 漏洞描述
            if (poc.getDescription() != null && !poc.getDescription().isEmpty()) {
                sb.append("    <div class=\"vuln-description\">\n");
                sb.append("        <strong>描述:</strong> ").append(escapeHtml(poc.getDescription())).append("\n");
                sb.append("    </div>\n");
            }

            // 可展开详情区域
            sb.append("    <div class=\"vuln-details\" id=\"details-").append(i).append("\">\n");

            // 检测路径
            if (result.getMatchedPath() != null && !result.getMatchedPath().isEmpty()) {
                sb.append("        <div class=\"detail-section\">\n");
                sb.append("            <h4 class=\"detail-section-title\">").append(ICON_SEARCH).append(" 检测路径</h4>\n");
                sb.append("            <div class=\"detail-content\">")
                  .append(escapeHtml(result.getMatchedPath())).append("</div>\n");
                sb.append("        </div>\n");
            }

            // Payload
            if (result.getMatchedPayload() != null && !result.getMatchedPayload().isEmpty()) {
                sb.append("        <div class=\"detail-section\">\n");
                sb.append("            <h4 class=\"detail-section-title\">").append(ICON_SYRINGE).append(" 检测Payload</h4>\n");
                sb.append("            <div class=\"detail-content\">")
                  .append(escapeHtml(result.getMatchedPayload())).append("</div>\n");
                sb.append("        </div>\n");
            }

            // 参数键名
            String paramKeys = result.getFormattedParamKeys();
            if (paramKeys != null && !paramKeys.isEmpty()) {
                sb.append("        <div class=\"detail-section\">\n");
                sb.append("            <h4 class=\"detail-section-title\">").append(ICON_KEY).append(" 参数键名</h4>\n");
                sb.append("            <div class=\"detail-content\">")
                  .append(escapeHtml(paramKeys)).append("</div>\n");
                sb.append("        </div>\n");
            }

            // 变量值
            String varValues = result.getFormattedVariableValues();
            if (varValues != null && !varValues.isEmpty()) {
                sb.append("        <div class=\"detail-section\">\n");
                sb.append("            <h4 class=\"detail-section-title\">").append(ICON_EDIT).append(" 变量值</h4>\n");
                sb.append("            <pre class=\"code-block\">")
                  .append(escapeHtml(varValues)).append("</pre>\n");
                sb.append("        </div>\n");
            }

            // 提取的输出数据
            String outputData = result.getFormattedOutputData();
            if (outputData != null && !outputData.isEmpty()) {
                sb.append("        <div class=\"detail-section\">\n");
                sb.append("            <h4 class=\"detail-section-title\">").append(ICON_UPLOAD).append(" 提取数据</h4>\n");
                sb.append("            <pre class=\"code-block\">")
                  .append(escapeHtml(outputData)).append("</pre>\n");
                sb.append("        </div>\n");
            }

            // 修复建议
            if (result.getRecommendation() != null && !result.getRecommendation().isEmpty()) {
                sb.append("        <div class=\"detail-section\">\n");
                sb.append("            <h4 class=\"detail-section-title\">").append(ICON_BULB).append(" 修复建议</h4>\n");
                sb.append("            <div class=\"detail-content\">")
                  .append(escapeHtml(result.getRecommendation())).append("</div>\n");
                sb.append("        </div>\n");
            }

            // 步骤详情（多步骤请求/响应）
            List<StepExecutionRecord> stepRecords = result.getStepRecords();
            if (stepRecords != null && !stepRecords.isEmpty()) {
                sb.append("        <div class=\"detail-section\">\n");
                sb.append("            <h4 class=\"detail-section-title\">").append(ICON_CLIPBOARD).append(" 步骤执行详情 (共 ")
                  .append(stepRecords.size()).append(" 步)</h4>\n");

                for (int j = 0; j < stepRecords.size(); j++) {
                    StepExecutionRecord record = stepRecords.get(j);
                    sb.append("            <div class=\"step-record\">\n");
                    sb.append("                <div class=\"step-header\">步骤 ").append(j + 1);
                    if (record.isMatched()) {
                        sb.append(" <span class=\"step-matched\">✓ 匹配</span>");
                    }
                    sb.append("</div>\n");
                    sb.append("                <div class=\"step-info\">\n");
                    sb.append("                    <div><strong>URL:</strong> ").append(escapeHtml(record.getRequestUrl())).append("</div>\n");
                    sb.append("                    <div><strong>方法:</strong> ").append(escapeHtml(record.getRequestMethod())).append("</div>\n");
                    sb.append("                    <div><strong>状态码:</strong> ").append(record.getResponseCode()).append("</div>\n");
                    sb.append("                    <div><strong>响应时间:</strong> ").append(record.getResponseTime()).append("ms</div>\n");

                    // 显示提取的变量
                    Map<String, Object> extractedVars = record.getExtractedVariables();
                    if (extractedVars != null && !extractedVars.isEmpty()) {
                        sb.append("                    <div><strong>提取变量:</strong> ");
                        for (Map.Entry<String, Object> entry : extractedVars.entrySet()) {
                            sb.append(escapeHtml(entry.getKey())).append("=").append(escapeHtml(String.valueOf(entry.getValue()))).append("; ");
                        }
                        sb.append("</div>\n");
                    }

                    sb.append("                </div>\n");

                    // 请求头（折叠）
                    Map<String, String> reqHeaders = record.getRequestHeaders();
                    if (reqHeaders != null && !reqHeaders.isEmpty()) {
                        sb.append("                <details class=\"step-details\">\n");
                        sb.append("                    <summary>请求头</summary>\n");
                        sb.append("                    <pre class=\"code-block\">");
                        for (Map.Entry<String, String> h : reqHeaders.entrySet()) {
                            sb.append(escapeHtml(h.getKey())).append(": ").append(escapeHtml(h.getValue())).append("\n");
                        }
                        sb.append("</pre>\n");
                        sb.append("                </details>\n");
                    }

                    // 请求体（折叠）
                    if (record.getRequestBody() != null && !record.getRequestBody().isEmpty()) {
                        String reqBody = record.getRequestBody();
                        if (reqBody.length() > 500) {
                            reqBody = reqBody.substring(0, 500) + "...";
                        }
                        sb.append("                <details class=\"step-details\">\n");
                        sb.append("                    <summary>请求体</summary>\n");
                        sb.append("                    <pre class=\"code-block\">").append(escapeHtml(reqBody)).append("</pre>\n");
                        sb.append("                </details>\n");
                    }

                    // 响应头（折叠）
                    Map<String, String> respHeaders = record.getResponseHeaders();
                    if (respHeaders != null && !respHeaders.isEmpty()) {
                        sb.append("                <details class=\"step-details\">\n");
                        sb.append("                    <summary>响应头</summary>\n");
                        sb.append("                    <pre class=\"code-block\">");
                        for (Map.Entry<String, String> h : respHeaders.entrySet()) {
                            sb.append(escapeHtml(h.getKey())).append(": ").append(escapeHtml(h.getValue())).append("\n");
                        }
                        sb.append("</pre>\n");
                        sb.append("                </details>\n");
                    }

                    // 响应体（折叠）
                    if (record.getResponseBody() != null && !record.getResponseBody().isEmpty()) {
                        String respBody = record.getResponseBody();
                        if (respBody.length() > 500) {
                            respBody = respBody.substring(0, 500) + "...";
                        }
                        sb.append("                <details class=\"step-details\">\n");
                        sb.append("                    <summary>响应体</summary>\n");
                        sb.append("                    <pre class=\"code-block\">").append(escapeHtml(respBody)).append("</pre>\n");
                        sb.append("                </details>\n");
                    }

                    sb.append("            </div>\n");
                }

                sb.append("        </div>\n");
            }

            // HTTP请求（最后匹配的）
            if (result.getRawRequest() != null && !result.getRawRequest().isEmpty()) {
                sb.append("        <div class=\"detail-section\">\n");
                sb.append("            <h4 class=\"detail-section-title\">").append(ICON_UPLOAD).append(" HTTP请求</h4>\n");
                sb.append("            <pre class=\"code-block\">")
                  .append(escapeHtml(result.getRawRequest())).append("</pre>\n");
                sb.append("        </div>\n");
            }

            // HTTP响应（最后匹配的）
            if (result.getRawResponseSnippet() != null && !result.getRawResponseSnippet().isEmpty()) {
                sb.append("        <div class=\"detail-section\">\n");
                sb.append("            <h4 class=\"detail-section-title\">").append(ICON_DOWNLOAD).append(" HTTP响应片段</h4>\n");
                sb.append("            <pre class=\"code-block\">")
                  .append(escapeHtml(result.getRawResponseSnippet())).append("</pre>\n");
                sb.append("        </div>\n");
            }

            sb.append("    </div>\n");
            sb.append("</div>\n\n");
        }

        return sb.toString();
    }

    /**
     * HTML转义,防止XSS攻击
     */
    private String escapeHtml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                 .replace("<", "&lt;")
                 .replace(">", "&gt;")
                 .replace("\"", "&quot;")
                 .replace("'", "&#x27;");
    }
}