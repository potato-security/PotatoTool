package com.potato.potatotool.content.redTeam.vulnScanner.model;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.FingerprintInfo;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.InputType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 漏洞扫描结果类
 */
public class ScanResult {
    // ========== 基础字段 ==========
    private String target; // 目标URL
    private PocObj.Poc poc; // 匹配的POC
    private boolean vulnerable; // 是否存在漏洞
    private long timestamp; // 扫描时间戳
    private Map<String, Object> details = new HashMap<>(); // 详细信息

    // ========== 新增：指纹与报告相关 ==========
    /**
     * 目标的指纹信息
     */
    private FingerprintInfo fingerprint;

    /**
     * 输入类型
     */
    private InputType inputType;

    /**
     * 匹配的请求路径
     */
    private String matchedPath;

    /**
     * 触发漏洞的 Payload
     */
    private String matchedPayload;

    /**
     * 提取的数据列表
     */
    private List<String> extractedData;

    /**
     * 原始请求（调试用）
     */
    private String rawRequest;

    /**
     * 响应片段（限制长度）
     */
    private String rawResponseSnippet;

    /**
     * POC 来源（nuclei/goby/xray/pocsuite）
     */
    private String pocSource;

    /**
     * 是否为重复结果（被去重）
     */
    private boolean duplicate = false;

    /**
     * 重复原因
     */
    private String duplicateReason;

    // ========== 增强报告相关字段 ==========

    /**
     * 步骤执行记录列表
     * 记录POC执行过程中每个步骤的请求/响应详情
     * 支持多步骤POC的完整记录（如 Request_1, Response_1, Request_2, Response_2...）
     */
    private List<StepExecutionRecord> stepRecords = new ArrayList<>();

    /**
     * 修复建议（冗余自POC，便于报告生成）
     */
    private String recommendation;

    /**
     * 参数键名列表
     * 记录触发漏洞使用的参数名（如 cmd, id, payload 等）
     */
    private List<String> paramKeys = new ArrayList<>();

    /**
     * 变量-值映射
     * 记录POC执行过程中使用的所有变量及其值
     */
    private Map<String, String> variableValues = new HashMap<>();

    /**
     * Output 提取的结果数据
     * 记录POC中 output/extractors 提取的数据
     */
    private Map<String, Object> outputData = new HashMap<>();

    /**
     * CVE编号（冗余自POC）
     */
    private String cveId;

    /**
     * CWE编号（冗余自POC）
     */
    private String cweId;

    /**
     * CVSS评分（冗余自POC）
     */
    private String cvssScore;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public PocObj.Poc getPoc() {
        return poc;
    }

    public void setPoc(PocObj.Poc poc) {
        this.poc = poc;
    }

    public boolean isVulnerable() {
        return vulnerable;
    }

    public void setVulnerable(boolean vulnerable) {
        this.vulnerable = vulnerable;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public void addDetail(String key, Object value) {
        this.details.put(key, value);
    }
    
    // ========== 新增字段的 Getter/Setter ==========
    
    public FingerprintInfo getFingerprint() {
        return fingerprint;
    }
    
    public void setFingerprint(FingerprintInfo fingerprint) {
        this.fingerprint = fingerprint;
    }
    
    public InputType getInputType() {
        return inputType;
    }
    
    public void setInputType(InputType inputType) {
        this.inputType = inputType;
    }
    
    public String getMatchedPath() {
        return matchedPath;
    }
    
    public void setMatchedPath(String matchedPath) {
        this.matchedPath = matchedPath;
    }
    
    public String getMatchedPayload() {
        return matchedPayload;
    }
    
    public void setMatchedPayload(String matchedPayload) {
        this.matchedPayload = matchedPayload;
    }
    
    public List<String> getExtractedData() {
        return extractedData;
    }
    
    public void setExtractedData(List<String> extractedData) {
        this.extractedData = extractedData;
    }
    
    public String getRawRequest() {
        // 如果本地字段为空但有 stepRecords，从最后一个成功匹配的步骤获取
        if ((rawRequest == null || rawRequest.isEmpty()) && stepRecords != null && !stepRecords.isEmpty()) {
            // 查找最后一个匹配成功的步骤
            for (int i = stepRecords.size() - 1; i >= 0; i--) {
                StepExecutionRecord record = stepRecords.get(i);
                if (record.isMatched() && record.getRawRequest() != null) {
                    return record.getRawRequest();
                }
            }
            // 如果没有匹配的，返回最后一个步骤的请求
            StepExecutionRecord lastRecord = stepRecords.get(stepRecords.size() - 1);
            if (lastRecord.getRawRequest() != null) {
                return lastRecord.getRawRequest();
            }
        }
        return rawRequest;
    }
    
    public void setRawRequest(String rawRequest) {
        this.rawRequest = rawRequest;
    }
    
    public String getRawResponseSnippet() {
        // 如果本地字段为空但有 stepRecords，从最后一个成功匹配的步骤获取
        if ((rawResponseSnippet == null || rawResponseSnippet.isEmpty()) && stepRecords != null && !stepRecords.isEmpty()) {
            // 查找最后一个匹配成功的步骤
            for (int i = stepRecords.size() - 1; i >= 0; i--) {
                StepExecutionRecord record = stepRecords.get(i);
                if (record.isMatched() && record.getRawResponse() != null) {
                    return record.getRawResponse();
                }
            }
            // 如果没有匹配的，返回最后一个步骤的响应
            StepExecutionRecord lastRecord = stepRecords.get(stepRecords.size() - 1);
            if (lastRecord.getRawResponse() != null) {
                return lastRecord.getRawResponse();
            }
        }
        return rawResponseSnippet;
    }
    
    public void setRawResponseSnippet(String rawResponseSnippet) {
        // 限制响应片段长度
        if (rawResponseSnippet != null && rawResponseSnippet.length() > 2000) {
            this.rawResponseSnippet = rawResponseSnippet.substring(0, 2000) + "...";
        } else {
            this.rawResponseSnippet = rawResponseSnippet;
        }
    }
    
    public String getPocSource() {
        return pocSource;
    }
    
    public void setPocSource(String pocSource) {
        this.pocSource = pocSource;
    }
    
    public boolean isDuplicate() {
        return duplicate;
    }
    
    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }
    
    public String getDuplicateReason() {
        return duplicateReason;
    }
    
    public void setDuplicateReason(String duplicateReason) {
        this.duplicateReason = duplicateReason;
    }

    // ========== 增强报告字段的 Getter/Setter ==========

    public List<StepExecutionRecord> getStepRecords() {
        return stepRecords;
    }

    public void setStepRecords(List<StepExecutionRecord> stepRecords) {
        this.stepRecords = stepRecords;
    }

    /**
     * 添加步骤执行记录
     */
    public void addStepRecord(StepExecutionRecord record) {
        if (record != null) {
            this.stepRecords.add(record);
        }
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public List<String> getParamKeys() {
        return paramKeys;
    }

    public void setParamKeys(List<String> paramKeys) {
        this.paramKeys = paramKeys;
    }

    /**
     * 添加参数键名
     */
    public void addParamKey(String key) {
        if (key != null && !key.isEmpty() && !this.paramKeys.contains(key)) {
            this.paramKeys.add(key);
        }
    }

    public Map<String, String> getVariableValues() {
        return variableValues;
    }

    public void setVariableValues(Map<String, String> variableValues) {
        this.variableValues = variableValues;
    }

    /**
     * 添加变量值
     */
    public void addVariableValue(String name, String value) {
        if (name != null && value != null) {
            this.variableValues.put(name, value);
        }
    }

    public Map<String, Object> getOutputData() {
        return outputData;
    }

    public void setOutputData(Map<String, Object> outputData) {
        this.outputData = outputData;
    }

    /**
     * 添加输出数据
     */
    public void addOutputData(String name, Object value) {
        if (name != null && value != null) {
            this.outputData.put(name, value);
        }
    }

    public String getCveId() {
        return cveId;
    }

    public void setCveId(String cveId) {
        this.cveId = cveId;
    }

    public String getCweId() {
        return cweId;
    }

    public void setCweId(String cweId) {
        this.cweId = cweId;
    }

    public String getCvssScore() {
        return cvssScore;
    }

    public void setCvssScore(String cvssScore) {
        this.cvssScore = cvssScore;
    }

    /**
     * 从POC中填充冗余字段
     * 便于报告生成时直接访问
     */
    public void populateFromPoc() {
        if (poc != null) {
            this.recommendation = poc.getRecommendation();
            this.cveId = poc.getCveId();
            this.cweId = poc.getCweId();
            this.cvssScore = poc.getCvssScore();
            this.pocSource = poc.getOriginalFormat();
        }
    }

    /**
     * 获取格式化的参数键名字符串
     * 用于报告展示
     */
    public String getFormattedParamKeys() {
        if (paramKeys == null || paramKeys.isEmpty()) {
            return "";
        }
        return String.join(", ", paramKeys);
    }

    /**
     * 获取格式化的变量值字符串
     * 用于报告展示
     */
    public String getFormattedVariableValues() {
        if (variableValues == null || variableValues.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : variableValues.entrySet()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(entry.getKey()).append(" = ").append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * 获取格式化的输出数据字符串
     * 用于报告展示
     */
    public String getFormattedOutputData() {
        if (outputData == null || outputData.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : outputData.entrySet()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(entry.getKey()).append(": ").append(entry.getValue());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "ScanResult{" +
                "target='" + target + '\'' +
                ", poc=" + (poc != null ? poc.getName() : "null") +
                ", vulnerable=" + vulnerable +
                ", timestamp=" + timestamp +
                ", pocSource='" + pocSource + '\'' +
                ", stepCount=" + (stepRecords != null ? stepRecords.size() : 0) +
                '}';
    }
} 