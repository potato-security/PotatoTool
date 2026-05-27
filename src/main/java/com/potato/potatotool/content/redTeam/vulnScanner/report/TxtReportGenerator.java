package com.potato.potatotool.content.redTeam.vulnScanner.report;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.model.StepExecutionRecord;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TXT 报告生成器
 */
public class TxtReportGenerator {

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public File generate(List<ScanResult> results, String outputPath) throws IOException {
        return generate(results, outputPath, "未知");
    }

    public File generate(List<ScanResult> results, String outputPath, String scanDuration) throws IOException {
        results = ReportDataSanitizer.sanitizeResults(results);
        File outputFile = new File(outputPath);
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            writer.write("PotatoTool 漏洞扫描报告");
            writer.newLine();
            writer.write(repeat("=", 64));
            writer.newLine();
            writer.write("生成时间: " + sdf.format(new Date()));
            writer.newLine();
            writer.write("扫描耗时: " + safeText(scanDuration));
            writer.newLine();
            writer.write("扫描目标数: " + countDistinctTargets(results));
            writer.newLine();
            writer.write("POC 数量: " + countDistinctPocs(results));
            writer.newLine();
            writer.write("漏洞结果数: " + (results == null ? 0 : results.size()));
            writer.newLine();
            writer.write("严重度统计: " + formatSeveritySummary(results));
            writer.newLine();
            writer.newLine();

            if (results != null) {
                for (int i = 0; i < results.size(); i++) {
                    appendResult(writer, results.get(i), i + 1);
                }
            }
        }
        return outputFile;
    }

    private void appendResult(BufferedWriter writer, ScanResult result, int index) throws IOException {
        PocObj.Poc poc = result.getPoc();
        writer.write("[" + index + "] " + safeText(poc != null ? poc.getName() : null));
        writer.newLine();
        writer.write("----------------------------------------------------------------");
        writer.newLine();
        writeField(writer, "目标地址", result.getTarget());
        writeField(writer, "POC ID", poc != null ? poc.getId() : null);
        writeField(writer, "严重程度", poc != null && poc.getSeverity() != null ? poc.getSeverity().name() : null);
        writeField(writer, "协议", poc != null ? poc.getProtocol() : null);
        writeField(writer, "漏洞类型", poc != null ? poc.getVulType() : null);
        writeField(writer, "POC 格式", poc != null ? poc.getOriginalFormat() : null);
        writeField(writer, "CVE ID", result.getCveId());
        writeField(writer, "CWE ID", result.getCweId());
        writeField(writer, "CVSS", result.getCvssScore());
        writeField(writer, "检测路径", result.getMatchedPath());
        writeField(writer, "检测 Payload", result.getMatchedPayload());
        writeField(writer, "参数键名", result.getFormattedParamKeys());
        writeField(writer, "变量值", result.getFormattedVariableValues());
        writeField(writer, "提取数据", result.getFormattedOutputData());
        writeField(writer, "修复建议", result.getRecommendation());

        appendStepRecords(writer, result.getStepRecords());
        appendBlock(writer, "HTTP 请求", trim(result.getRawRequest(), 5000));
        appendBlock(writer, "HTTP 响应片段", trim(result.getRawResponseSnippet(), 5000));
        writer.newLine();
    }

    private void appendStepRecords(BufferedWriter writer, List<StepExecutionRecord> stepRecords) throws IOException {
        if (stepRecords == null || stepRecords.isEmpty()) {
            return;
        }

        writer.write("执行链路:");
        writer.newLine();
        for (int i = 0; i < stepRecords.size(); i++) {
            StepExecutionRecord record = stepRecords.get(i);
            writer.write("  - 步骤 " + (i + 1)
                    + " | 方法=" + safeText(record.getRequestMethod())
                    + " | URL=" + safeText(record.getRequestUrl())
                    + " | 状态码=" + record.getResponseCode()
                    + " | 匹配=" + (record.isMatched() ? "是" : "否"));
            writer.newLine();
        }
    }

    private void appendBlock(BufferedWriter writer, String title, String content) throws IOException {
        if (content == null || content.trim().isEmpty()) {
            return;
        }

        writer.write(title + ":");
        writer.newLine();
        writer.write(content);
        writer.newLine();
    }

    private void writeField(BufferedWriter writer, String label, String value) throws IOException {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        writer.write(label + ": " + value);
        writer.newLine();
    }

    private int countDistinctTargets(List<ScanResult> results) {
        java.util.Set<String> targets = new java.util.LinkedHashSet<String>();
        if (results != null) {
            for (ScanResult result : results) {
                if (result != null && result.getTarget() != null) {
                    targets.add(result.getTarget());
                }
            }
        }
        return targets.size();
    }

    private int countDistinctPocs(List<ScanResult> results) {
        java.util.Set<String> pocIds = new java.util.LinkedHashSet<String>();
        if (results != null) {
            for (ScanResult result : results) {
                if (result != null && result.getPoc() != null && result.getPoc().getId() != null) {
                    pocIds.add(result.getPoc().getId());
                }
            }
        }
        return pocIds.size();
    }

    private String formatSeveritySummary(List<ScanResult> results) {
        Map<PocObj.Severity, Integer> counts = new HashMap<PocObj.Severity, Integer>();
        counts.put(PocObj.Severity.CRITICAL, 0);
        counts.put(PocObj.Severity.HIGH, 0);
        counts.put(PocObj.Severity.MEDIUM, 0);
        counts.put(PocObj.Severity.LOW, 0);
        counts.put(PocObj.Severity.INFO, 0);

        if (results != null) {
            for (ScanResult result : results) {
                if (result == null || result.getPoc() == null || result.getPoc().getSeverity() == null) {
                    continue;
                }
                PocObj.Severity severity = result.getPoc().getSeverity();
                counts.put(severity, counts.get(severity) + 1);
            }
        }

        return "CRITICAL=" + counts.get(PocObj.Severity.CRITICAL)
                + ", HIGH=" + counts.get(PocObj.Severity.HIGH)
                + ", MEDIUM=" + counts.get(PocObj.Severity.MEDIUM)
                + ", LOW=" + counts.get(PocObj.Severity.LOW)
                + ", INFO=" + counts.get(PocObj.Severity.INFO);
    }

    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "\n...(截断)";
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}
