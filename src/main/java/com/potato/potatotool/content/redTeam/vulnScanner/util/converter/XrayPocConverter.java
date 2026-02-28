package com.potato.potatotool.content.redTeam.vulnScanner.util.converter;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.Matcher;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.MatcherType;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.MatchersCondition;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.XrayYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.SetVariableEvaluator;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.XrayCelParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.potato.potatotool.content.redTeam.vulnScanner.event.ScanErrorEvent.logUnrecognizedExpression;

/**
 * @author Potato
 * @date 2025/2/19 16:30
 * Xray YAML POC转换器，用于将Xray格式的POC转换为通用PocObj
 */
public class XrayPocConverter implements IPocConverter<XrayYamlObj.Poc> {

    /**
     * 将Xray YAML POC转换为通用PocObj
     * @param xrayPoc Xray YAML POC对象
     * @return 通用PocObj对象
     */
    @Override
    public PocObj.Poc convert(XrayYamlObj.Poc xrayPoc) {
        if (xrayPoc == null || xrayPoc.getRules() == null) {
            System.out.println("输入的非Xray_Yaml_POC对象");
            return null;
        }
        
        PocObj.Poc poc = new PocObj.Poc();
        
        // 转换基本信息
        convertBasicInfo(xrayPoc, poc);
        
        // 转换规则和验证步骤
        convertRules(xrayPoc, poc);
        
        // 处理搜索查询
        convertSearchQueries(xrayPoc, poc);
        
        // 处理变量
        convertVariables(xrayPoc, poc);
        
        // 处理全局expression
        convertGlobalExpression(xrayPoc, poc);
        
        // 设置原始POC
        // poc.setOriginalPoc(xrayPoc);
        poc.setOriginalFormat("xray");
        
        return poc;
    }
    
    /**
     * 转换基本信息
     * @param xrayPoc Xray POC对象
     * @param poc 通用POC对象
     */
    private void convertBasicInfo(XrayYamlObj.Poc xrayPoc, PocObj.Poc poc) {
        // 基本信息
        poc.setId(xrayPoc.getName() != null ? xrayPoc.getName() : "unknown_xray_poc");
        poc.setName(xrayPoc.getName() != null ? xrayPoc.getName() : "");
        poc.setProtocol(xrayPoc.getTransport() != null ? xrayPoc.getTransport() : "http");
        
        // 设置详细信息
        if (xrayPoc.getDetail() != null) {
            XrayYamlObj.Detail detail = xrayPoc.getDetail();
            poc.setAuthor(detail.getAuthor() != null ? detail.getAuthor() : "");
            poc.setDescription(detail.getDescription() != null ? detail.getDescription() : "");
            
            // 设置参考链接
            if (detail.getLinks() != null) {
                poc.setReferences(detail.getLinks());
            }
            
            // 设置标签
            if (detail.getTags() != null) {
                List<String> tags = new ArrayList<>();
                tags.add(detail.getTags());
                poc.setTags(tags);
            }
            
            // 设置版本
            poc.setVersion(detail.getVersion() != null ? detail.getVersion() : "");
            
            // 设置漏洞信息
            if (detail.getVulnerability() != null) {
                poc.setCveId(detail.getVulnerability().getId());
            }
        }
    }
    
    /**
     * 转换规则和验证步骤
     * @param xrayPoc Xray POC对象
     * @param poc 通用POC对象
     */
    private void convertRules(XrayYamlObj.Poc xrayPoc, PocObj.Poc poc) {
        if (xrayPoc.getRules() == null || xrayPoc.getRules().isEmpty()) {
            return;
        }
        
        List<PocObj.PocStep> verifySteps = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : xrayPoc.getRules().entrySet()) {
            try {
                // 处理LinkedHashMap到Rule对象的转换
                Object ruleObj = entry.getValue();
                if (!(ruleObj instanceof LinkedHashMap)) {
                    System.out.println("规则格式不正确: " + ruleObj.getClass().getName());
                    continue;
                }
                
                LinkedHashMap<String, Object> ruleMap = (LinkedHashMap<String, Object>) ruleObj;
                XrayYamlObj.Rule rule = convertMapToRule(ruleMap);
                
                // 创建并设置步骤
                PocObj.PocStep step = createPocStep(entry.getKey(), rule);
                verifySteps.add(step);
            } catch (Exception e) {
                System.out.println("转换规则时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        if (!verifySteps.isEmpty()) {
            poc.setVerifySteps(verifySteps);
        }
    }
    
    /**
     * 将Map转换为Rule对象
     * @param ruleMap 规则Map
     * @return Rule对象
     */
    private XrayYamlObj.Rule convertMapToRule(LinkedHashMap<String, Object> ruleMap) {
        XrayYamlObj.Rule rule = new XrayYamlObj.Rule();
        
        // 设置Request
        if (ruleMap.containsKey("request")) {
            Object requestObj = ruleMap.get("request");
            if (requestObj instanceof LinkedHashMap) {
                LinkedHashMap<String, Object> requestMap = (LinkedHashMap<String, Object>) requestObj;
                XrayYamlObj.RuleRequest request = convertMapToRequest(requestMap);
                rule.setRequest(request);
            }
        }
        
        // 设置Expression
        if (ruleMap.containsKey("expression")) {
            rule.setExpression(String.valueOf(ruleMap.get("expression")));
        }
        
        // 设置Output
        if (ruleMap.containsKey("output") && ruleMap.get("output") instanceof LinkedHashMap) {
            LinkedHashMap<String, String> output = new LinkedHashMap<>();
            LinkedHashMap<String, Object> outputMap = (LinkedHashMap<String, Object>) ruleMap.get("output");
            for (Map.Entry<String, Object> outputEntry : outputMap.entrySet()) {
                output.put(outputEntry.getKey(), String.valueOf(outputEntry.getValue()));
            }
            rule.setOutput(output);
        }
        
        return rule;
    }
    
    /**
     * 将Map转换为RuleRequest对象
     * @param requestMap 请求Map
     * @return RuleRequest对象
     */
    private XrayYamlObj.RuleRequest convertMapToRequest(LinkedHashMap<String, Object> requestMap) {
        XrayYamlObj.RuleRequest request = new XrayYamlObj.RuleRequest();
        
        // 设置Request的各个字段
        if (requestMap.containsKey("method")) {
            request.setMethod(String.valueOf(requestMap.get("method")));
        }
        if (requestMap.containsKey("path")) {
            request.setPath(String.valueOf(requestMap.get("path")));
        }
        if (requestMap.containsKey("headers") && requestMap.get("headers") instanceof LinkedHashMap) {
            LinkedHashMap<String, String> headers = new LinkedHashMap<>();
            LinkedHashMap<String, Object> headersMap = (LinkedHashMap<String, Object>) requestMap.get("headers");
            for (Map.Entry<String, Object> headerEntry : headersMap.entrySet()) {
                headers.put(headerEntry.getKey(), String.valueOf(headerEntry.getValue()));
            }
            request.setHeaders(headers);
        }
        if (requestMap.containsKey("body")) {
            request.setBody(String.valueOf(requestMap.get("body")));
        }
        if (requestMap.containsKey("follow_redirects")) {
            request.setFollow_redirects(Boolean.parseBoolean(String.valueOf(requestMap.get("follow_redirects"))));
        }
        if (requestMap.containsKey("cache")) {
            request.setCache(Boolean.parseBoolean(String.valueOf(requestMap.get("cache"))));
        }
        if (requestMap.containsKey("connectionID")) {
            request.setConnectionID(String.valueOf(requestMap.get("connectionID")));
        }
        if (requestMap.containsKey("content")) {
            request.setContent(String.valueOf(requestMap.get("content")));
        }
        if (requestMap.containsKey("readTimeout")) {
            request.setReadTimeout(String.valueOf(requestMap.get("readTimeout")));
        }
        
        return request;
    }
    
    /**
     * 创建PocStep对象
     * @param stepId 步骤ID
     * @param rule 规则对象
     * @return PocStep对象
     */
    private PocObj.PocStep createPocStep(String stepId, XrayYamlObj.Rule rule) {
        PocObj.PocStep step = new PocObj.PocStep();
        
        // 设置步骤ID
        step.setStepId(stepId);
        
        // 设置请求信息
        if (rule.getRequest() != null) {
            XrayYamlObj.RuleRequest request = rule.getRequest();
            step.setMethod(request.getMethod());
            step.setPath(request.getPath());
            step.setHeaders(request.getHeaders());
            // 优先使用content字段，如果content为空则使用body字段
            if (request.getContent() != null && !request.getContent().isEmpty()) {
                step.setBody(request.getContent());
            } else {
                step.setBody(request.getBody());
            }
            step.setFollowRedirect(request.isFollow_redirects());
            step.setCache(request.isCache());
            step.setConnectionId(request.getConnectionID());
            
            // 处理超时设置
            if (request.getReadTimeout() != null && !request.getReadTimeout().isEmpty()) {
                try {
                    // 尝试将超时时间转换为整数
                    step.setTimeout(Integer.parseInt(request.getReadTimeout()));
                } catch (NumberFormatException e) {
                    // 如果转换失败，记录错误但不中断处理
                    System.out.println("无法解析超时时间: " + request.getReadTimeout());
                }
            }
        }
        
        // 设置表达式作为匹配规则
        if (rule.getExpression() != null) {
            Matcher matcher = createExpressionMatcher(rule.getExpression());
            List<Matcher> matchers = new ArrayList<>();
            matchers.add(matcher);
            step.setMatchers(matchers);
            step.setMatchersCondition(MatchersCondition.AND); // Xray默认使用AND条件
        }
        
        // 设置输出
        if (rule.getOutput() != null && !rule.getOutput().isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.putAll(rule.getOutput());
            step.setOutput(result);
        }
        
        return step;
    }
    
    /**
     * 创建表达式匹配器
     * @param expression 表达式
     * @return 匹配器对象
     */
    private Matcher createExpressionMatcher(String expression) {
        // 验证CEL表达式语法
        try {
            XrayCelParser.validateCelSyntax(expression);
        } catch (IllegalArgumentException e) {
            System.err.println("警告: Xray CEL表达式验证失败 - " + e.getMessage());
            logUnrecognizedExpression(e.getMessage());
            // 继续执行，但记录警告
        }
        
        Matcher matcher = new Matcher();
        matcher.setType(MatcherType.CEL);
        List<String> values = new ArrayList<>();
        values.add(expression);
        matcher.setValues(values);
        matcher.setCondition("AND"); // Xray默认使用AND条件
        return matcher;
    }
    
    /**
     * 转换搜索查询
     * @param xrayPoc Xray POC对象
     * @param poc 通用POC对象
     */
    private void convertSearchQueries(XrayYamlObj.Poc xrayPoc, PocObj.Poc poc) {
        if (xrayPoc.getQuery() != null && !xrayPoc.getQuery().isEmpty()) {
            Map<String, String> searchQueries = new HashMap<>();
            searchQueries.put("xray", xrayPoc.getQuery());
            poc.setSearchQueries(searchQueries);
        }
    }
    
    /**
     * 转换变量
     * @param xrayPoc Xray POC对象
     * @param poc 通用POC对象
     */
    private void convertVariables(XrayYamlObj.Poc xrayPoc, PocObj.Poc poc) {
        // 创建合并的变量 Map（先 set，后 payloads，允许覆盖）
        Map<String, List<String>> combinedVariables = new HashMap<>();

        // 1. 处理 set 变量（支持变量引用和属性访问）
        if (xrayPoc.getSet() != null && !xrayPoc.getSet().isEmpty()) {
            Map<String, List<String>> setMap = new HashMap<>();
            Map<String, String> evaluatedSimpleVars = new HashMap<>(); // 存储已求值的字符串变量
            Map<String, Object> objectContext = new HashMap<>(); // 存储对象（如 ReverseObject）

            // 求值所有 set 变量（支持引用已求值的变量和属性访问）
            for (Map.Entry<String, Object> entry : xrayPoc.getSet().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof List) {
                    // 如果值是列表，对每个元素求值
                    List<String> valueList = new ArrayList<>();
                    for (Object item : (List<?>) value) {
                        if (item != null) {
                            // 执行函数求值，并支持变量替换和属性访问
                            String strValue = item.toString();
                            String evaluated = evaluateWithVariableSubstitution(strValue, evaluatedSimpleVars, objectContext);
                            valueList.add(evaluated);
                        }
                    }
                    setMap.put(key, valueList);
                } else if (value != null) {
                    // 如果值不是列表但不为空，创建只有一个元素的列表
                    String strValue = value.toString();
                    
                    // 如果是 newReverse() 等返回对象的函数，标记为需要在运行时求值的表达式
                    // 注意：不立即执行，避免在转换阶段创建对象，而是让 Executor 在运行时创建
                    if (strValue.contains("newReverse()")) {
                        List<String> valueList = new ArrayList<>();
                        valueList.add("@@expression:" + strValue);
                        setMap.put(key, valueList);
                        // 记录到 evaluatedSimpleVars 以便后续引用也能识别（尽管后续引用可能也需要运行时处理）
                        evaluatedSimpleVars.put(key, "@@expression:" + strValue);
                        continue;
                    }
                    
                    // 如果引用了 reverse 对象的属性（如 reverse.url, reverse.domain），也标记为运行时表达式
                    if (strValue.startsWith("reverse.") || strValue.contains("reverse.")) {
                        List<String> valueList = new ArrayList<>();
                        valueList.add("@@expression:" + strValue);
                        setMap.put(key, valueList);
                        evaluatedSimpleVars.put(key, "@@expression:" + strValue);
                        continue;
                    }
                    
                    String evaluated = evaluateWithVariableSubstitution(strValue, evaluatedSimpleVars, objectContext);
                    List<String> valueList = new ArrayList<>();
                    valueList.add(evaluated);
                    setMap.put(key, valueList);

                    // 保存单值变量供后续引用
                    evaluatedSimpleVars.put(key, evaluated);
                }
            }

            // 将 set 变量添加到合并 Map
            combinedVariables.putAll(setMap);
        }

        // 2. 处理 payloads 变量（可覆盖 set 中的同名变量）
        if (xrayPoc.getPayloads() != null && xrayPoc.getPayloads().getPayloads() != null && !xrayPoc.getPayloads().getPayloads().isEmpty()) {
            Map<String, List<String>> payloadsMap = new HashMap<>();

            for (Map.Entry<String, Object> entry : xrayPoc.getPayloads().getPayloads().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof List) {
                    // 如果值是列表，对每个��素求值
                    List<String> valueList = new ArrayList<>();
                    for (Object item : (List<?>) value) {
                        if (item != null) {
                            // 执行函数求值
                            String evaluated = SetVariableEvaluator.evaluateVariable(item.toString());
                            valueList.add(evaluated);
                        }
                    }
                    payloadsMap.put(key, valueList);
                } else if (value != null) {
                    // 如果值不是列表但不为空，创建只有一个元素的列表
                    // 执行函数求值
                    String evaluated = SetVariableEvaluator.evaluateVariable(value.toString());
                    List<String> valueList = new ArrayList<>();
                    valueList.add(evaluated);
                    payloadsMap.put(key, valueList);
                }
            }

            // 将 payloads 变量合并到 combinedVariables（允许覆盖 set）
            combinedVariables.putAll(payloadsMap);
        }

        // 3. 一次性设置所有变量
        if (!combinedVariables.isEmpty()) {
            poc.setVariables(combinedVariables);
        }

        // 4. 解析 continue_ 字段（命中一个 payload 后是否继续）
        if (xrayPoc.getPayloads() != null) {
            poc.setContinueOnMatch(xrayPoc.getPayloads().isContinue_());
        }

        // 5. 设置 variablesType（Xray 默认为 clusterbomb）
        poc.setVariablesType(PocObj.VariablesType.clusterbomb);
    }
    
    /**
     * 转换全局表达式
     * @param xrayPoc Xray POC对象
     * @param poc 通用POC对象
     */
    private void convertGlobalExpression(XrayYamlObj.Poc xrayPoc, PocObj.Poc poc) {
        if (xrayPoc.getExpression() != null && !xrayPoc.getExpression().isEmpty()) {
            String expression = xrayPoc.getExpression();
            // 将全局expression设置为flow字段
            poc.setFlow(expression);
            
            // 根据全局表达式设置步骤间的条件
            // 如果表达式包含 || (OR)，设置为 OR 条件
            // 如果表达式包含 && (AND) 或只有单个规则调用，设置为 AND 条件
            if (expression.contains("||")) {
                poc.setStepsCondition(MatchersCondition.OR);
            } else {
                poc.setStepsCondition(MatchersCondition.AND);
            }
        }
    }
    
    /**
     * 安全获取字符串值
     * @param map Map对象
     * @param key 键
     * @return 字符串值，如果不存在则返回空字符串
     */
    private String safeGetString(Map<String, Object> map, String key) {
        if (map.containsKey(key) && map.get(key) != null) {
            return String.valueOf(map.get(key));
        }
        return "";
    }
    
    /**
     * 安全获取布尔值
     * @param map Map对象
     * @param key 键
     * @return 布尔值，如果不存在则返回false
     */
    private boolean safeGetBoolean(Map<String, Object> map, String key) {
        if (map.containsKey(key) && map.get(key) != null) {
            return Boolean.parseBoolean(String.valueOf(map.get(key)));
        }
        return false;
    }
    
    /**
     * 求值并替换变量（带对象上下文）
     * 
     * @param value 待求值的字符串（可能包含变量引用或属性访问）
     * @param variables 已求值的字符串变量映射
     * @param objectContext 对象上下文（如 ReverseObject）
     * @return 求值后的结果
     */
    private String evaluateWithVariableSubstitution(String value, Map<String, String> variables, Map<String, Object> objectContext) {
        if (value == null) {
            return "";
        }
        
        // 先替换已知变量（支持 {{varName}} 格式）
        String substituted = value;
        if (variables != null && !variables.isEmpty()) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String varName = entry.getKey();
                String varValue = entry.getValue();
                if (varName != null && varValue != null) {
                    // 替换 {{varName}} 格式
                    substituted = substituted.replace("{{" + varName + "}}", varValue);
                }
            }
        }
        
        // 然后执行函数求值（传入对象上下文以支持属性访问）
        return com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.SetVariableEvaluator.evaluateVariable(substituted, objectContext);
    }
}