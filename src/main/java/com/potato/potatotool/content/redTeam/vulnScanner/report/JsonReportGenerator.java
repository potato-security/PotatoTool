package com.potato.potatotool.content.redTeam.vulnScanner.report;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.model.StepExecutionRecord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * JSON报告生成器
 * 使用Gson生成JSON格式报告
 * 
 * @author Potato
 * @date 2025/11/03
 */
public class JsonReportGenerator {
    
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public File generate(List<ScanResult> results, String outputPath) throws IOException {
        JsonObject report = new JsonObject();
        
        // 报告元信息
        JsonObject metadata = new JsonObject();
        metadata.addProperty("tool", "PotatoTool");
        metadata.addProperty("version", "2.5.1");
        metadata.addProperty("generateTime", sdf.format(new Date()));
        report.add("metadata", metadata);
        
        // 统计信息
        JsonObject statistics = new JsonObject();
        statistics.addProperty("targetCount", 
            (int) results.stream().map(ScanResult::getTarget).distinct().count());
        statistics.addProperty("pocCount", 
            (int) results.stream().map(r -> r.getPoc().getId()).distinct().count());
        statistics.addProperty("vulnCount", results.size());
        
        // 按严重度统计
        JsonObject severityStats = new JsonObject();
        Map<PocObj.Severity, Long> severityCount = new HashMap<>();
        for (ScanResult result : results) {
            PocObj.Severity severity = result.getPoc().getSeverity();
            severityCount.put(severity, severityCount.getOrDefault(severity, 0L) + 1);
        }
        for (Map.Entry<PocObj.Severity, Long> entry : severityCount.entrySet()) {
            severityStats.addProperty(entry.getKey().name(), entry.getValue());
        }
        statistics.add("severityStats", severityStats);
        
        // 按格式统计
        JsonObject formatStats = new JsonObject();
        Map<String, Long> formatCount = new HashMap<>();
        for (ScanResult result : results) {
            String format = result.getPoc().getOriginalFormat();
            formatCount.put(format, formatCount.getOrDefault(format, 0L) + 1);
        }
        for (Map.Entry<String, Long> entry : formatCount.entrySet()) {
            formatStats.addProperty(entry.getKey(), entry.getValue());
        }
        statistics.add("formatStats", formatStats);
        
        report.add("statistics", statistics);
        
        // 漏洞详情
        JsonArray vulnerabilities = new JsonArray();
        for (ScanResult result : results) {
            JsonObject vuln = new JsonObject();
            vuln.addProperty("target", result.getTarget());
            vuln.addProperty("pocId", result.getPoc().getId());
            vuln.addProperty("pocName", result.getPoc().getName());
            vuln.addProperty("severity", result.getPoc().getSeverity().name());
            vuln.addProperty("vulnType", result.getPoc().getVulType());
            vuln.addProperty("pocFormat", result.getPoc().getOriginalFormat());
            vuln.addProperty("protocol", result.getPoc().getProtocol());
            vuln.addProperty("description", result.getPoc().getDescription());
            vuln.addProperty("foundTime", sdf.format(new Date()));

            // 检测详情
            if (result.getMatchedPath() != null && !result.getMatchedPath().isEmpty()) {
                vuln.addProperty("matchedPath", result.getMatchedPath());
            }
            if (result.getMatchedPayload() != null && !result.getMatchedPayload().isEmpty()) {
                vuln.addProperty("matchedPayload", result.getMatchedPayload());
            }

            // CVE/CWE/CVSS 信息
            if (result.getCveId() != null && !result.getCveId().isEmpty()) {
                vuln.addProperty("cveId", result.getCveId());
            }
            if (result.getCweId() != null && !result.getCweId().isEmpty()) {
                vuln.addProperty("cweId", result.getCweId());
            }
            if (result.getCvssScore() != null && !result.getCvssScore().isEmpty()) {
                vuln.addProperty("cvssScore", result.getCvssScore());
            }

            // 参数键名
            List<String> paramKeys = result.getParamKeys();
            if (paramKeys != null && !paramKeys.isEmpty()) {
                JsonArray paramKeysArray = new JsonArray();
                for (String key : paramKeys) {
                    paramKeysArray.add(key);
                }
                vuln.add("paramKeys", paramKeysArray);
            }

            // 变量值
            Map<String, String> varValues = result.getVariableValues();
            if (varValues != null && !varValues.isEmpty()) {
                JsonObject varValuesObj = new JsonObject();
                for (Map.Entry<String, String> entry : varValues.entrySet()) {
                    varValuesObj.addProperty(entry.getKey(), entry.getValue());
                }
                vuln.add("variableValues", varValuesObj);
            }

            // 提取的输出数据
            Map<String, Object> outputData = result.getOutputData();
            if (outputData != null && !outputData.isEmpty()) {
                JsonObject outputDataObj = new JsonObject();
                for (Map.Entry<String, Object> entry : outputData.entrySet()) {
                    outputDataObj.addProperty(entry.getKey(), String.valueOf(entry.getValue()));
                }
                vuln.add("outputData", outputDataObj);
            }

            // 修复建议
            if (result.getRecommendation() != null && !result.getRecommendation().isEmpty()) {
                vuln.addProperty("recommendation", result.getRecommendation());
            }

            // 步骤执行记录
            List<StepExecutionRecord> stepRecords = result.getStepRecords();
            if (stepRecords != null && !stepRecords.isEmpty()) {
                JsonArray stepsArray = new JsonArray();
                for (StepExecutionRecord record : stepRecords) {
                    JsonObject stepObj = new JsonObject();
                    stepObj.addProperty("stepIndex", record.getStepIndex());
                    if (record.getStepId() != null) {
                        stepObj.addProperty("stepId", record.getStepId());
                    }
                    if (record.getStepType() != null) {
                        stepObj.addProperty("stepType", record.getStepType());
                    }
                    stepObj.addProperty("requestUrl", record.getRequestUrl());
                    stepObj.addProperty("requestMethod", record.getRequestMethod());
                    stepObj.addProperty("responseCode", record.getResponseCode());
                    stepObj.addProperty("responseTime", record.getResponseTime());
                    stepObj.addProperty("matched", record.isMatched());

                    // 请求头
                    if (record.getRequestHeaders() != null && !record.getRequestHeaders().isEmpty()) {
                        JsonObject headersObj = new JsonObject();
                        for (Map.Entry<String, String> header : record.getRequestHeaders().entrySet()) {
                            headersObj.addProperty(header.getKey(), header.getValue());
                        }
                        stepObj.add("requestHeaders", headersObj);
                    }

                    // 请求体（限制长度）
                    if (record.getRequestBody() != null && !record.getRequestBody().isEmpty()) {
                        String body = record.getRequestBody();
                        if (body.length() > 2000) {
                            body = body.substring(0, 2000) + "...(截断)";
                        }
                        stepObj.addProperty("requestBody", body);
                    }

                    // 响应体（限制长度）
                    if (record.getResponseBody() != null && !record.getResponseBody().isEmpty()) {
                        String body = record.getResponseBody();
                        if (body.length() > 2000) {
                            body = body.substring(0, 2000) + "...(截断)";
                        }
                        stepObj.addProperty("responseBody", body);
                    }

                    // 提取的变量
                    if (record.getExtractedVariables() != null && !record.getExtractedVariables().isEmpty()) {
                        JsonObject extractedObj = new JsonObject();
                        for (Map.Entry<String, Object> entry : record.getExtractedVariables().entrySet()) {
                            extractedObj.addProperty(entry.getKey(), String.valueOf(entry.getValue()));
                        }
                        stepObj.add("extractedVariables", extractedObj);
                    }

                    // 匹配值
                    if (record.getMatchedValues() != null && !record.getMatchedValues().isEmpty()) {
                        JsonArray matchedArray = new JsonArray();
                        for (String val : record.getMatchedValues()) {
                            matchedArray.add(val);
                        }
                        stepObj.add("matchedValues", matchedArray);
                    }

                    stepsArray.add(stepObj);
                }
                vuln.add("stepRecords", stepsArray);
            }

            // HTTP请求/响应（最终匹配的）
            if (result.getRawRequest() != null && !result.getRawRequest().isEmpty()) {
                vuln.addProperty("rawRequest", result.getRawRequest());
            }
            if (result.getRawResponseSnippet() != null && !result.getRawResponseSnippet().isEmpty()) {
                vuln.addProperty("rawResponseSnippet", result.getRawResponseSnippet());
            }

            // 添加标签
            if (result.getPoc().getTags() != null && !result.getPoc().getTags().isEmpty()) {
                JsonArray tags = new JsonArray();
                for (String tag : result.getPoc().getTags()) {
                    tags.add(tag);
                }
                vuln.add("tags", tags);
            }

            vulnerabilities.add(vuln);
        }
        report.add("vulnerabilities", vulnerabilities);
        
        // 写入文件
        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();
        
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            writer.write(gson.toJson(report));
        }
        
        return outputFile;
    }
}


