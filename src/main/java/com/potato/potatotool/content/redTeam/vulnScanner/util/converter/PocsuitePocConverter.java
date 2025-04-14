package com.potato.potatotool.content.redTeam.vulnScanner.util.converter;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.Matcher;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.MatcherType;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.MatchersCondition;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.OperationType;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.Severity;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocsuiteJsonObj;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Potato
 * @date 2025/3/19 16:30
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
        if (pocInfo.getAppVersion() != null) {
            poc.setVersion(pocInfo.getAppVersion());
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
        
        // 设置路径
        pocStep.setPath(safeGetString(step.getVulPath(), "/"));
        
        // 设置请求头
        if (step.getHeaders() != null && !step.getHeaders().isEmpty()) {
            pocStep.setHeaders(step.getHeaders());
        }
        
        // 设置请求体
        pocStep.setBody(step.getParams());
        
        // 处理匹配规则
        convertMatchers(step, pocStep);
        
        // 设置结果输出
        pocStep.setOutput(step.getResult());
        
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
            
            // // 处理时间匹配（如果有）
            // if (match.getTime() != null && !match.getTime().isEmpty()) {
            //     Matcher matcher = new Matcher();
            //     matcher.setType(MatcherType.TIME);
            //     List<String> values = new ArrayList<>();
            //     values.add(match.getTime());
            //     matcher.setValues(values);
            //     matcher.setCondition("OR");
            //     matcher.setOperation(OperationType.GREATER_EQUAL); // 时间匹配通常是大于等于
            //     matcher.setPart("response_time"); // 匹配响应时间
            //     matchers.add(matcher);
            // }
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