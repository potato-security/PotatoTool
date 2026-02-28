package com.potato.potatotool.content.redTeam.vulnScanner.util.converter;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.Matcher;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.MatcherType;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.MatchersCondition;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.OperationType;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.Severity;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocsuiteJsonObj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Potato
 * @date 2025/2/19 16:30
 * Pocsuite JSON POC转换器，用于将Pocsuite格式的POC转换为通用PocObj
 */
public class PocsuitePocConverter implements IPocConverter<PocsuiteJsonObj.PocJson> {

    /**
     * 将Pocsuite JSON POC转换为通用PocObj
     * @param pocsuitePoc Pocsuite JSON POC对象
     * @return 通用PocObj对象
     */
    @Override
    public PocObj.Poc convert(PocsuiteJsonObj.PocJson pocsuitePoc) {
        if (pocsuitePoc == null || pocsuitePoc.getPocExecute() == null) {
            System.out.println("输入的非Pocsuite_Json_POC对象");
            return null;
        }
        
        PocObj.Poc poc = new PocObj.Poc();
        
        // 转换基本信息
        convertBasicInfo(pocsuitePoc, poc);
        
        // 转换验证步骤
        convertVerifySteps(pocsuitePoc, poc);
        
        // 转换攻击步骤
        convertExploitSteps(pocsuitePoc, poc);
        
        // 设置原始POC
        // poc.setOriginalPoc(pocsuitePoc);
        poc.setOriginalFormat("pocsuite");

        // 设置 variablesType（Pocsuite 默认为 clusterbomb）
        poc.setVariablesType(PocObj.VariablesType.clusterbomb);

        return poc;
    }
    
    /**
     * 转换基本信息
     * @param pocsuitePoc Pocsuite JSON POC对象
     * @param poc 通用POC对象
     */
    private void convertBasicInfo(PocsuiteJsonObj.PocJson pocsuitePoc, PocObj.Poc poc) {
        if (pocsuitePoc.getPocInfo() == null) {
            return;
        }
        
        PocsuiteJsonObj.PocInfo pocInfo = pocsuitePoc.getPocInfo();
        
        // 设置基本字段，使用安全的获取方法
        poc.setId(safeGetString(pocInfo.getVulID()));
        poc.setName(safeGetString(pocInfo.getName()));
        poc.setAuthor(safeGetString(pocInfo.getAuthor()));
        poc.setDescription(safeGetString(pocInfo.getDesc()));
        poc.setProtocol(safeGetString(pocInfo.getProtocol(), "http"));
        poc.setVulType(safeGetString(pocInfo.getVulType()));
        poc.setVersion(safeGetString(pocInfo.getVersion()));
        poc.setCreateTime(safeGetString(pocInfo.getCreateDate()));
        poc.setUpdateTime(safeGetString(pocInfo.getUpdateDate()));
        poc.setAppPowerLink(safeGetString(pocInfo.getAppPowerLink()));
        poc.setPocDesc(safeGetString(pocInfo.getDesc())); // 使用desc作为pocDesc
        
        // 设置严重程度
        convertSeverity(pocInfo, poc);
        
        // 设置参考链接
        if (pocInfo.getReferences() != null) {
            poc.setReferences(pocInfo.getReferences());
        }
        
        // 设置应用名称和版本
        poc.setProduct(pocInfo.getAppName());
        // 如果version为空，则使用appVersion
        if (poc.getVersion() == null || poc.getVersion().isEmpty()) {
            if (pocInfo.getAppVersion() != null) {
                poc.setVersion(pocInfo.getAppVersion());
            }
        }
        
        // 设置漏洞日期
        appendVulnDate(pocInfo, poc);
    }
    
    /**
     * 转换严重程度
     * @param pocInfo Pocsuite POC信息对象
     * @param poc 通用POC对象
     */
    private void convertSeverity(PocsuiteJsonObj.PocInfo pocInfo, PocObj.Poc poc) {
        if (pocInfo.getVulType() == null) {
            poc.setSeverity(Severity.UNKNOWN);
            return;
        }
        
        String vulType = pocInfo.getVulType().toLowerCase();
        
        // 关键漏洞类型 - 严重
        if (containsAny(vulType, "rce", "code execution", "command", "remote", "backdoor", "arbitrary code")) {
            poc.setSeverity(Severity.CRITICAL);
        }
        // 高危漏洞类型
        else if (containsAny(vulType, "sql", "injection", "xss", "csrf", "ssrf", "traversal", "upload", "xxe")) {
            poc.setSeverity(Severity.HIGH);
        }
        // 中危漏洞类型
        else if (containsAny(vulType, "bypass", "overflow", "dos", "brute force", "weak password")) {
            poc.setSeverity(Severity.MEDIUM);
        }
        // 低危漏洞类型
        else if (containsAny(vulType, "disclosure", "information", "leak", "exposure")) {
            poc.setSeverity(Severity.LOW);
        }
        // 默认未知
        else {
            poc.setSeverity(Severity.UNKNOWN);
        }
    }
    
    /**
     * 添加漏洞日期到描述中
     * @param pocInfo Pocsuite POC信息对象
     * @param poc 通用POC对象
     */
    private void appendVulnDate(PocsuiteJsonObj.PocInfo pocInfo, PocObj.Poc poc) {
        if (pocInfo.getVulDate() == null) {
            return;
        }
        
        String description = poc.getDescription();
        if (description == null) {
            description = "";
        }
        if (!description.isEmpty()) {
            description += "\n";
        }
        description += "漏洞发现日期: " + pocInfo.getVulDate();
        poc.setDescription(description);
    }
    
    /**
     * 转换验证步骤
     * @param pocsuitePoc Pocsuite JSON POC对象
     * @param poc 通用POC对象
     */
    private void convertVerifySteps(PocsuiteJsonObj.PocJson pocsuitePoc, PocObj.Poc poc) {
        if (pocsuitePoc.getPocExecute() == null || pocsuitePoc.getPocExecute().getVerify() == null) {
            return;
        }
        
        List<PocObj.PocStep> verifySteps = new ArrayList<>();
        
        for (PocsuiteJsonObj.Step step : pocsuitePoc.getPocExecute().getVerify()) {
            try {
                PocObj.PocStep pocStep = convertStep(step, "verify_", verifySteps.size());
                if (pocStep != null) {
                    verifySteps.add(pocStep);
                }
            } catch (Exception e) {
                System.out.println("转换验证步骤时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        if (!verifySteps.isEmpty()) {
            poc.setVerifySteps(verifySteps);
        }
    }
    
    /**
     * 转换攻击步骤
     * @param pocsuitePoc Pocsuite JSON POC对象
     * @param poc 通用POC对象
     */
    private void convertExploitSteps(PocsuiteJsonObj.PocJson pocsuitePoc, PocObj.Poc poc) {
        if (pocsuitePoc.getPocExecute() == null || pocsuitePoc.getPocExecute().getAttack() == null) {
            return;
        }
        
        List<PocObj.PocStep> exploitSteps = new ArrayList<>();
        
        for (PocsuiteJsonObj.Step step : pocsuitePoc.getPocExecute().getAttack()) {
            try {
                PocObj.PocStep pocStep = convertStep(step, "exploit_", exploitSteps.size());
                if (pocStep != null) {
                    exploitSteps.add(pocStep);
                }
            } catch (Exception e) {
                System.out.println("转换攻击步骤时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        if (!exploitSteps.isEmpty()) {
            poc.setExploitSteps(exploitSteps);
        }
    }
    
    /**
     * 转换单个步骤
     * @param step Pocsuite步骤对象
     * @param prefix 步骤ID前缀
     * @param index 步骤索引
     * @return 通用POC步骤对象
     */
    private PocObj.PocStep convertStep(PocsuiteJsonObj.Step step, String prefix, int index) {
        if (step == null) {
            return null;
        }
        
        PocObj.PocStep pocStep = new PocObj.PocStep();
        
        // 设置步骤ID
        pocStep.setStepId(step.getStep() != null ? step.getStep() : prefix + index);
        
        // 设置请求方法
        pocStep.setMethod(safeGetString(step.getMethod(), "GET"));
        
        // 设置路径（GET 请求需要将 params 拼接到路径上作为查询字符串）
        String path = safeGetString(step.getVulPath(), "/");
        String method = safeGetString(step.getMethod(), "GET").toUpperCase();
        String params = step.getParams();
        
        if ("GET".equals(method) && params != null && !params.isEmpty()) {
            // GET 请求：将 params 作为查询字符串拼接到路径
            if (path.contains("?")) {
                path = path + "&" + params;
            } else {
                path = path + "?" + params;
            }
            pocStep.setPath(path);
            pocStep.setBody(null);
        } else {
            // POST/其他请求：params 作为请求体
            pocStep.setPath(path);
            pocStep.setBody(params);
        }
        
        // 设置请求头
        if (step.getHeaders() != null && !step.getHeaders().isEmpty()) {
            pocStep.setHeaders(step.getHeaders());
        }
        
        // 处理 Pocsuite 特有的 necessary 字段
        // necessary 字段描述执行该步骤的前置条件（如：需要登录、需要特定权限等）
        if (step.getNecessary() != null && !step.getNecessary().isEmpty()) {
            String prerequisite = step.getNecessary();
            
            // 输出日志提醒
            System.out.println("[Pocsuite] 步骤 " + pocStep.getStepId() + 
                             " 前置条件: " + prerequisite);
            
            // 将前置条件存储到 output 中，供后续分析或展示
            if (pocStep.getOutput() == null) {
                pocStep.setOutput(new HashMap<>());
            }
            pocStep.getOutput().put("_prerequisite", prerequisite);
            pocStep.getOutput().put("_prerequisite_step", pocStep.getStepId());
        }
        
        // 处理匹配规则
        convertMatchers(step, pocStep);
        
        // 设置结果输出
        convertOutput(step.getResult(), pocStep);
        
        return pocStep;
    }
    
    /**
     * 转换匹配规则
     * @param step Pocsuite步骤对象
     * @param pocStep 通用POC步骤对象
     */
    private void convertMatchers(PocsuiteJsonObj.Step step, PocObj.PocStep pocStep) {
        List<Matcher> matchers = new ArrayList<>();
        
        // 处理正则表达式匹配
        if (step.getMatch() != null) {
            PocsuiteJsonObj.Match match = step.getMatch();
            
            if (match.getRegex() != null && !match.getRegex().isEmpty()) {
                Matcher matcher = new Matcher();
                matcher.setType(MatcherType.REGEX);
                matcher.setValues(match.getRegex());
                matcher.setCondition("OR");
                matcher.setPart("body"); // 默认匹配响应体
                matcher.setOperation(OperationType.REGEX_MATCH); // 设置操作类型为正则匹配
                matchers.add(matcher);
            }
            
            // 处理时间匹配（如果有）
            if (match.getTime() != null && !match.getTime().isEmpty()) {
                Matcher matcher = new Matcher();
                matcher.setType(MatcherType.TIME);
                List<String> values = new ArrayList<>();
                values.add(match.getTime());
                matcher.setValues(values);
                matcher.setCondition("OR");
                matcher.setOperation(OperationType.GREATER_EQUAL); // 时间匹配通常是大于等于
                matcher.setPart("response_time"); // 匹配响应时间
                matcher.setTimeUnit("s"); // Pocsuite 时间单位为秒
                matchers.add(matcher);
            }
        }
        
        // 设置状态码匹配
        if (step.getStatus() != null && !step.getStatus().isEmpty()) {
            Matcher matcher = new Matcher();
            matcher.setType(MatcherType.STATUS);
            List<String> values = new ArrayList<>();
            values.add(step.getStatus());
            matcher.setValues(values);
            matcher.setCondition("OR");
            matcher.setOperation(OperationType.EQUAL); // 状态码通常是精确匹配
            matchers.add(matcher);
        }
        
        if (!matchers.isEmpty()) {
            pocStep.setMatchers(matchers);
            pocStep.setMatchersCondition(MatchersCondition.AND); // Pocsuite默认使用AND条件
        }
    }
    
    /**
     * 英文字段到中文的映射
     */
    private static final Map<String, String> FIELD_MAPPING = new HashMap<>();
    static {
        // 顶级字段映射
        FIELD_MAPPING.put("DBInfo", "数据库内容");
        FIELD_MAPPING.put("ShellInfo", "Webshell信息");
        FIELD_MAPPING.put("FileInfo", "文件信息");
        FIELD_MAPPING.put("XSSInfo", "跨站脚本信息");
        FIELD_MAPPING.put("AdminInfo", "管理员信息");
        FIELD_MAPPING.put("Database", "数据库信息");
        FIELD_MAPPING.put("VerifyInfo", "验证信息");
        FIELD_MAPPING.put("SiteAttr", "网站服务器信息");
        FIELD_MAPPING.put("Process", "服务器进程");
        
        // 子字段映射
        FIELD_MAPPING.put("Username", "管理员用户名");
        FIELD_MAPPING.put("Password", "管理员密码");
        FIELD_MAPPING.put("Salt", "加密盐值");
        FIELD_MAPPING.put("Uid", "用户ID");
        FIELD_MAPPING.put("Groupid", "用户组ID");
        FIELD_MAPPING.put("URL", "验证URL");
        FIELD_MAPPING.put("Content", "文件内容");
        FIELD_MAPPING.put("Filename", "文件名称");
        FIELD_MAPPING.put("Payload", "验证Payload");
        FIELD_MAPPING.put("Hostname", "数据库主机名");
        FIELD_MAPPING.put("DBname", "数据库名");
        FIELD_MAPPING.put("Postdata", "验证POST数据");
        FIELD_MAPPING.put("Path", "网站绝对路径");
    }
    
    /**
     * 转换输出结果，将英文字段转为中文，并提取正则表达式
     * 支持任意深度的嵌套结构
     * @param result 原始结果Map
     * @param pocStep POC步骤对象
     */
    @SuppressWarnings("unchecked")
    private void convertOutput(Map<String, Object> result, PocObj.PocStep pocStep) {
        if (result == null || result.isEmpty()) {
            return;
        }
        
        Map<String, Object> chineseOutput = new HashMap<>();
        List<Matcher> extractors = new ArrayList<>();
        int[] extractorIndex = {1}; // 使用数组以便在递归中共享
        
        // 递归处理嵌套结构
        for (Map.Entry<String, Object> entry : result.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // 转换字段名称为中文
            String chineseKey = FIELD_MAPPING.getOrDefault(key, key);
            Object convertedValue = convertValue(value, extractors, extractorIndex);
            chineseOutput.put(chineseKey, convertedValue);
        }
        
        // 设置转换后的输出和提取器
        pocStep.setOutput(chineseOutput);
        if (!extractors.isEmpty()) {
            pocStep.setExtractors(extractors);
        }
    }
    
    /**
     * 递归转换值，支持任意深度的嵌套
     * @param value 原始值
     * @param extractors 提取器列表
     * @param extractorIndex 提取器索引（数组形式以便在递归中共享）
     * @return 转换后的值
     */
    @SuppressWarnings("unchecked")
    private Object convertValue(Object value, List<Matcher> extractors, int[] extractorIndex) {
        if (value == null) {
            return null;
        }
        
        // 处理字符串类型的正则表达式
        if (value instanceof String) {
            String strValue = (String) value;
            if (strValue.startsWith("<regex>")) {
                String regex = strValue.substring(7); // 去除<regex>前缀
                String outputVar = "output_" + extractorIndex[0];
                
                // 创建提取器
                Matcher extractor = new Matcher();
                extractor.setType(MatcherType.REGEX);
                extractor.setPart("body"); // 默认从响应体提取
                extractor.setName(outputVar);
                List<String> values = new ArrayList<>();
                values.add(regex);
                extractor.setValues(values);
                extractor.setOperation(OperationType.REGEX_MATCH);
                extractors.add(extractor);
                
                extractorIndex[0]++;
                
                // 返回模板变量
                return "{{" + outputVar + "}}";
            } else if (strValue.isEmpty()) {
                // 空字符串保持为空
                return "";
            } else {
                // 普通字符串直接返回
                return strValue;
            }
        }
        
        // 处理Map类型的嵌套结构
        if (value instanceof Map) {
            Map<String, Object> mapValue = (Map<String, Object>) value;
            Map<String, Object> convertedMap = new HashMap<>();
            
            for (Map.Entry<String, Object> entry : mapValue.entrySet()) {
                String key = entry.getKey();
                Object subValue = entry.getValue();
                
                // 转换字段名称为中文
                String chineseKey = FIELD_MAPPING.getOrDefault(key, key);
                
                // 递归处理嵌套值
                Object convertedSubValue = convertValue(subValue, extractors, extractorIndex);
                convertedMap.put(chineseKey, convertedSubValue);
            }
            
            return convertedMap;
        }
        
        // 处理List类型（虽然Pocsuite标准中不常见，但为了健壮性也支持）
        if (value instanceof List) {
            List<Object> listValue = (List<Object>) value;
            List<Object> convertedList = new ArrayList<>();
            
            for (Object item : listValue) {
                Object convertedItem = convertValue(item, extractors, extractorIndex);
                convertedList.add(convertedItem);
            }
            
            return convertedList;
        }
        
        // 其他类型直接返回
        return value;
    }
    
    /**
     * 安全获取字符串值，避免空指针异常
     * @param value 原始值
     * @return 非空字符串值，如果原始值为null则返回空字符串
     */
    private String safeGetString(String value) {
        return value != null ? value : "";
    }
    
    /**
     * 安全获取字符串值，避免空指针异常，并提供默认值
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 非空字符串值，如果原始值为null则返回默认值
     */
    private String safeGetString(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    /**
     * 检查字符串是否包含任意一个给定的子字符串
     * @param str 要检查的字符串
     * @param substrings 要查找的子字符串数组
     * @return 如果包含任意一个子字符串则返回true，否则返回false
     */
    private boolean containsAny(String str, String... substrings) {
        if (str == null || substrings == null) {
            return false;
        }
        
        for (String substring : substrings) {
            if (str.contains(substring)) {
                return true;
            }
        }
        
        return false;
    }
}