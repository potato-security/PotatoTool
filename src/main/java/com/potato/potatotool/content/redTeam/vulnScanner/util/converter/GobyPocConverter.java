package com.potato.potatotool.content.redTeam.vulnScanner.util.converter;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.GobyJsonObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.Matcher;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.MatcherType;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.MatchersCondition;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.OperationType;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.Severity;

import java.util.*;

/**
 * @author Potato
 * @date 2025/3/19 16:30
 * Goby JSON POC转换器，用于将Goby格式的POC转换为通用PocObj
 */
public class GobyPocConverter implements IPocConverter<GobyJsonObj.PocJson> {

    /**
     * 将Goby JSON POC转换为通用PocObj
     * @param gobyPoc Goby JSON POC对象
     * @return 通用PocObj对象
     */
    @Override
    public PocObj.Poc convert(GobyJsonObj.PocJson gobyPoc) {
        if (gobyPoc == null || gobyPoc.getScanSteps() == null) {
            System.out.println("输入的非Goby_Json_POC对象");
            return null;
        }

        PocObj.Poc poc = new PocObj.Poc();
        
        // 转换基本信息
        convertBasicInfo(gobyPoc, poc);
        
        // 转换扫描步骤
        convertScanSteps(gobyPoc, poc);
        
        // 转换利用步骤
        convertExploitSteps(gobyPoc, poc);
        
        // 处理参数
        convertExpParams(gobyPoc, poc);
        
        // 设置原始POC
        // poc.setOriginalPoc(gobyPoc);
        poc.setOriginalFormat("goby");

        return poc;
    }
    
    /**
     * 转换基本信息
     */
    private void convertBasicInfo(GobyJsonObj.PocJson gobyPoc, PocObj.Poc poc) {
        // 基本信息
        poc.setId(gobyPoc.getName() != null ? gobyPoc.getName() : "unknown_goby_poc");
        poc.setName(gobyPoc.getName() != null ? gobyPoc.getName() : "");
        poc.setAuthor(gobyPoc.getAuthor() != null ? gobyPoc.getAuthor() : "");
        poc.setDescription(gobyPoc.getDescription() != null ? gobyPoc.getDescription() : "");
        poc.setProduct(gobyPoc.getProduct() != null ? gobyPoc.getProduct() : "");
        poc.setHomepage(gobyPoc.getHomepage() != null ? gobyPoc.getHomepage() : "");
        poc.setImpact(gobyPoc.getImpact() != null ? gobyPoc.getImpact() : "");
        
        // 处理Recommendation字段（注意Goby中有两种拼写）
        poc.setRecommendation(gobyPoc.getRecommendation() != null ? gobyPoc.getRecommendation() :
                gobyPoc.getRecommandation() != null ? gobyPoc.getRecommandation() : "");
        
        poc.setCreateTime(gobyPoc.getPostTime() != null ? gobyPoc.getPostTime() : "");
        poc.setVersion(gobyPoc.getGobyVersion() != null ? gobyPoc.getGobyVersion() : "");
        poc.setAppPowerLink(gobyPoc.getHomepage() != null ? gobyPoc.getHomepage() : ""); // 使用Homepage作为AppPowerLink
        
        // 设置参考链接
        if (gobyPoc.getReferences() != null) {
            poc.setReferences(gobyPoc.getReferences());
        }
        
        // 设置标签
        if (gobyPoc.getTags() != null) {
            poc.setTags(gobyPoc.getTags());
        }
        
        // 设置严重程度
        convertSeverity(gobyPoc, poc);
        
        // 设置搜索查询
        if (gobyPoc.getGobyQuery() != null && !gobyPoc.getGobyQuery().isEmpty()) {
            Map<String, String> searchQueries = new HashMap<>();
            searchQueries.put("goby", gobyPoc.getGobyQuery());
            poc.setSearchQueries(searchQueries);
        }
        
        // 设置协议
        poc.setProtocol("http"); // Goby默认使用HTTP协议
    }
    
    /**
     * 转换严重程度
     */
    private void convertSeverity(GobyJsonObj.PocJson gobyPoc, PocObj.Poc poc) {
        if (gobyPoc.getLevel() == null) {
            poc.setSeverity(Severity.UNKNOWN);
            return;
        }
        
        switch (gobyPoc.getLevel().toLowerCase()) {
            // 文本表示
            case "info":
            case "5":
                poc.setSeverity(Severity.INFO);
                break;
            case "low":
            case "4":
                poc.setSeverity(Severity.LOW);
                break;
            case "medium":
            case "3":
                poc.setSeverity(Severity.MEDIUM);
                break;
            case "high":
            case "2":
                poc.setSeverity(Severity.HIGH);
                break;
            case "critical":
            case "1":
                poc.setSeverity(Severity.CRITICAL);
                break;
            default:
                poc.setSeverity(Severity.UNKNOWN);
        }
    }
    
    /**
     * 转换扫描步骤，使用GobyJsonObj中定义的类型
     */
    private void convertScanSteps(GobyJsonObj.PocJson gobyPoc, PocObj.Poc poc) {
        if (gobyPoc.getScanSteps() == null || gobyPoc.getScanSteps().isEmpty()) {
            return;
        }
        
        List<PocObj.PocStep> verifySteps = new ArrayList<>();
        
        // 确定匹配条件
        MatchersCondition globalCondition = MatchersCondition.AND; // 默认为AND
        int startIndex = 0;
        
        // 检查第一个元素是否为条件字符串
        if (!gobyPoc.getScanSteps().isEmpty() && gobyPoc.getScanSteps().get(0) instanceof String) {
            String condition = (String) gobyPoc.getScanSteps().get(0);
            if ("OR".equalsIgnoreCase(condition)) {
                globalCondition = MatchersCondition.OR;
            }
            startIndex = 1;
        }
        
        // 从条件后的元素开始处理步骤
        for (int i = startIndex; i < gobyPoc.getScanSteps().size(); i++) {
            Object stepObj = gobyPoc.getScanSteps().get(i);
            try {
                PocObj.PocStep step = new PocObj.PocStep();
                step.setStepId("scan_" + i);
                
                // 修改这里：不直接强制类型转换，而是从Map构建ScanStep对象
                if (stepObj instanceof Map) {
                    GobyJsonObj.ScanStep scanStep = convertMapToScanStep((Map<String, Object>) stepObj);
                    convertScanStep(scanStep, step, globalCondition, poc);
                    verifySteps.add(step);
                } else {
                    System.out.println("扫描步骤格式不正确: " + stepObj.getClass().getName());
                }
            } catch (Exception e) {
                System.out.println("转换扫描步骤时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        if (!verifySteps.isEmpty()) {
            poc.setVerifySteps(verifySteps);
        }
    }
    
    /**
     * 将Map转换为ScanStep对象
     */
    private GobyJsonObj.ScanStep convertMapToScanStep(Map<String, Object> map) {
        GobyJsonObj.ScanStep scanStep = new GobyJsonObj.ScanStep();
        
        // 处理Request
        if (map.containsKey("Request")) {
            Object requestObj = map.get("Request");
            if (requestObj instanceof Map) {
                GobyJsonObj.Request request = new GobyJsonObj.Request();
                Map<String, Object> requestMap = (Map<String, Object>) requestObj;
                
                request.setMethod(safeGetString(requestMap, "method"));
                request.setUri(safeGetString(requestMap, "uri"));
                request.setFollow_redirect(safeGetBoolean(requestMap, "follow_redirect"));
                request.setData_type(safeGetString(requestMap, "data_type"));
                request.setData(safeGetString(requestMap, "data"));
                
                // 处理header
                if (requestMap.containsKey("header") && requestMap.get("header") instanceof Map) {
                    Map<String, Object> headerMap = (Map<String, Object>) requestMap.get("header");
                    LinkedHashMap<String, String> headers = new LinkedHashMap<>();
                    for (Map.Entry<String, Object> entry : headerMap.entrySet()) {
                        headers.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : "");
                    }
                    request.setHeader(headers);
                }
                
                // 处理set_variable
                if (requestMap.containsKey("set_variable") && requestMap.get("set_variable") instanceof List) {
                    List<Object> varList = (List<Object>) requestMap.get("set_variable");
                    List<String> variables = new ArrayList<>();
                    for (Object var : varList) {
                        if (var != null) {
                            variables.add(var.toString());
                        }
                    }
                    request.setSet_variable(variables);
                }
                
                scanStep.setRequest(request);
            }
        }
        
        // 处理ResponseTest
        if (map.containsKey("ResponseTest")) {
            Object responseTestObj = map.get("ResponseTest");
            if (responseTestObj instanceof Map) {
                GobyJsonObj.ResponseTest responseTest = new GobyJsonObj.ResponseTest();
                Map<String, Object> responseTestMap = (Map<String, Object>) responseTestObj;
                
                responseTest.setType(safeGetString(responseTestMap, "type"));
                responseTest.setOperation(safeGetString(responseTestMap, "operation"));
                
                // 处理checks
                if (responseTestMap.containsKey("checks") && responseTestMap.get("checks") instanceof List) {
                    List<Object> checksList = (List<Object>) responseTestMap.get("checks");
                    List<GobyJsonObj.Check> checks = new ArrayList<>();
                    
                    for (Object checkObj : checksList) {
                        if (checkObj instanceof Map) {
                            GobyJsonObj.Check check = convertMapToCheck((Map<String, Object>) checkObj);
                            checks.add(check);
                        }
                    }
                    
                    responseTest.setChecks(checks);
                }
                
                scanStep.setResponseTest(responseTest);
            }
        }
        
        // 处理SetVariable
        if (map.containsKey("SetVariable") && map.get("SetVariable") instanceof List) {
            List<Object> varList = (List<Object>) map.get("SetVariable");
            List<String> variables = new ArrayList<>();
            for (Object var : varList) {
                if (var != null) {
                    variables.add(var.toString());
                }
            }
            scanStep.setSetVariable(variables);
        }
        
        return scanStep;
    }
    
    /**
     * 将Map转换为Check对象
     */
    private GobyJsonObj.Check convertMapToCheck(Map<String, Object> map) {
        GobyJsonObj.Check check = new GobyJsonObj.Check();
        
        check.setType(safeGetString(map, "type"));
        check.setVariable(safeGetString(map, "variable"));
        check.setOperation(safeGetString(map, "operation"));
        check.setValue(safeGetString(map, "value"));
        check.setBz(safeGetString(map, "bz"));
        
        // 处理子检查项（递归）
        if (map.containsKey("checks") && map.get("checks") instanceof List) {
            List<Object> checksList = (List<Object>) map.get("checks");
            List<GobyJsonObj.Check> checks = new ArrayList<>();
            
            for (Object checkObj : checksList) {
                if (checkObj instanceof Map) {
                    GobyJsonObj.Check subCheck = convertMapToCheck((Map<String, Object>) checkObj);
                    checks.add(subCheck);
                }
            }
            
            check.setChecks(checks);
        }
        
        return check;
    }
    
    /**
     * 转换ScanStep对象到PocStep
     */
    private void convertScanStep(GobyJsonObj.ScanStep scanStep, PocObj.PocStep step, MatchersCondition defaultCondition, PocObj.Poc poc) {
        // 处理Request
        if (scanStep.getRequest() != null) {
            GobyJsonObj.Request request = scanStep.getRequest();
            step.setMethod(request.getMethod());
            step.setPath(request.getUri());
            step.setFollowRedirect(request.isFollow_redirect());
            
            if (request.getHeader() != null && !request.getHeader().isEmpty()) {
                step.setHeaders(new HashMap<>(request.getHeader()));
            }
            
            step.setBody(request.getData());
            step.setDataType(request.getData_type());
            
            // 处理Request中的Set_variable提取器
            if (request.getSet_variable() != null && !request.getSet_variable().isEmpty()) {
                processExtractors(request.getSet_variable(), step, poc);
            }
        }
        
        // 处理ResponseTest
        if (scanStep.getResponseTest() != null) {
            GobyJsonObj.ResponseTest responseTest = scanStep.getResponseTest();
            
            // 设置匹配条件
            if (responseTest.getOperation() != null) {
                step.setMatchersCondition("and".equalsIgnoreCase(responseTest.getOperation()) ? 
                    MatchersCondition.AND : MatchersCondition.OR);
            } else {
                step.setMatchersCondition(defaultCondition);
            }
            
            // 处理检查项
            if (responseTest.getChecks() != null && !responseTest.getChecks().isEmpty()) {
                List<Matcher> matchers = new ArrayList<>();
                
                for (GobyJsonObj.Check check : responseTest.getChecks()) {
                    Matcher matcher = convertCheck(check);
                    if (matcher != null) {
                        matchers.add(matcher);
                    }
                }
                
                if (!matchers.isEmpty()) {
                    step.setMatchers(matchers);
                }
            }
        }
        
        // 处理ScanStep中的SetVariable提取器
        if (scanStep.getSetVariable() != null && !scanStep.getSetVariable().isEmpty()) {
            processExtractors(scanStep.getSetVariable(), step, poc);
        }
    }
    
    /**
     * 处理提取器列表并添加到步骤中
     * @param extractorDefs 提取器定义列表
     * @param step 要添加提取器的步骤
     */
    private void processExtractors(List<String> extractorDefs, PocObj.PocStep step, PocObj.Poc poc) {
        if (extractorDefs == null || extractorDefs.isEmpty()) {
            return;
        }
        
        // 获取已有的提取器列表，如果为空则创建新列表
        List<Matcher> extractors = step.getExtractors() != null ? 
            new ArrayList<>(step.getExtractors()) : new ArrayList<>();
        
        for (String extractorDef : extractorDefs) {
            Matcher extractor = processExtractorDefinition(extractorDef, extractors, poc);
            if (extractor != null) {
                extractors.add(extractor);
            }
        }
        
        if (!extractors.isEmpty()) {
            step.setExtractors(extractors);
        }
    }
    
    /**
     * 转换Check对象到Matcher
     */
    private Matcher convertCheck(GobyJsonObj.Check check) {
        if (check == null) {
            return null;
        }
        
        // 如果是group类型，需要递归处理子检查项
        if ("group".equals(check.getType()) && check.getChecks() != null && !check.getChecks().isEmpty()) {
            // 创建一个组合匹配器
            Matcher groupMatcher = new Matcher();
            groupMatcher.setType(MatcherType.GROUP);
            groupMatcher.setName("Group Matcher");
            
            // 设置组合条件
            if (check.getOperation() != null) {
                groupMatcher.setCondition("and".equalsIgnoreCase(check.getOperation()) ? "AND" : "OR");
            } else {
                groupMatcher.setCondition("AND"); // 默认使用AND条件
            }
            
            // 递归处理子检查项
            List<Matcher> subMatchers = new ArrayList<>();
            for (GobyJsonObj.Check subCheck : check.getChecks()) {
                Matcher subMatcher = convertCheck(subCheck);
                if (subMatcher != null) {
                    subMatchers.add(subMatcher);
                }
            }
            
            // 设置子匹配器
            groupMatcher.setSubMatchers(subMatchers);
            return groupMatcher;
        }
        
        // 处理item类型
        Matcher matcher = new Matcher();
        
        // 设置匹配类型和操作类型
        String checkType = check.getType();
        if ("item".equals(checkType)) {
            String variable = check.getVariable();
            String operation = check.getOperation();
            
            // 根据变量设置匹配类型
            if (variable != null) {
                if ("$code".equals(variable)) {
                    matcher.setType(MatcherType.STATUS);
                } else if ("$body".equals(variable)) {
                    matcher.setType(MatcherType.WORD);
                } else if ("$header".equals(variable)) {
                    matcher.setType(MatcherType.WORD);
                } else {
                    matcher.setType(MatcherType.WORD); // 默认为WORD类型
                }
            } else {
                matcher.setType(MatcherType.WORD); // 默认为WORD类型
            }
            
            // 根据operation设置操作类型
            if (operation != null) {
                switch (operation.toLowerCase()) {
                    case "contains":
                        matcher.setOperation(OperationType.CONTAINS);
                        break;
                    case "not contains":
                        matcher.setOperation(OperationType.NOT_CONTAINS);
                        break;
                    case "regex":
                        matcher.setOperation(OperationType.REGEX_MATCH);
                        // 如果是正则操作，匹配类型也应该是REGEX
                        matcher.setType(MatcherType.REGEX);
                        break;
                    case "start with":
                        matcher.setOperation(OperationType.PREFIX);
                        break;
                    case "end with":
                        matcher.setOperation(OperationType.SUFFIX);
                        break;
                    case "==":
                        matcher.setOperation(OperationType.EQUAL);
                        break;
                    case "!=":
                        matcher.setOperation(OperationType.NOT_EQUAL);
                        break;
                    case ">":
                        matcher.setOperation(OperationType.GREATER);
                        break;
                    case "<":
                        matcher.setOperation(OperationType.LESS);
                        break;
                    default:
                        matcher.setOperation(OperationType.DEFAULT);
                }
            } else {
                matcher.setOperation(OperationType.DEFAULT); // 默认操作
            }
        } else {
            matcher.setType(MatcherType.UNKNOWN);
            matcher.setOperation(OperationType.DEFAULT);
        }
        
        // 设置匹配值
        List<String> values = new ArrayList<>();
        if (check.getValue() != null) {
            values.add(check.getValue());
        }
        matcher.setValues(values);
        
        // 设置匹配部分
        if (check.getVariable() != null) {
            // 处理特殊变量
            String variable = check.getVariable();
            if (variable.startsWith("$")) {
                switch (variable) {
                    case "$body":
                        matcher.setPart("body");
                        break;
                    case "$header":
                        matcher.setPart("header");
                        break;
                    case "$code":
                        matcher.setPart("status");
                        break;
                    default:
                        matcher.setPart(variable);
                }
            } else {
                matcher.setPart(variable);
            }
        }
        
        // 设置匹配条件（这里指的是多个值之间的条件，与operation不同）
        matcher.setCondition("AND"); // 默认使用AND条件
        
        // 设置备注
        matcher.setName(check.getBz() != null && !check.getBz().isEmpty()? check.getBz() : "");
        
        return matcher;
    }
    
    /**
     * 处理提取器定义字符串并创建提取器对象或添加到payloads
     * @param extractorDef 提取器定义字符串，格式为：变量名|数据源|操作类型|匹配值
     * @param existingExtractors 已存在的提取器列表，用于检查变量名是否重复
     * @return 创建的提取器对象，如果是特殊处理类型则返回null
     */
    private Matcher processExtractorDefinition(String extractorDef, List<Matcher> existingExtractors, PocObj.Poc poc) {
        if (extractorDef == null || extractorDef.isEmpty()) {
            return null;
        }
        
        // 分割提取器定义字符串
        String[] parts = extractorDef.split("\\|");
        if (parts.length < 3) {
            System.out.println("跳过不正确的提取器格式: " + extractorDef);
            return null;
        }
        
        String varName = parts[0].trim();
        String dataSource = parts[1].trim().toLowerCase();
        String operation = parts[2].trim().toLowerCase();
        String value = parts.length > 3 && parts[3] != null ? parts[3].trim() : "";
        
        // 特殊情况处理：随机字符串生成
        if ("rand".equals(dataSource) && ("str".equals(operation) || "int".equals(operation))) {
            // 将随机字符串定义添加到payloads而不是extractors
            List<String> values = new ArrayList<>();
            values.add("@@random(" + value + ")"); // 例如：@@random(4) 表示生成4位随机字符串
            poc.getVariables().put(varName, values);
            return null; // 返回null表示不添加到extractors
        }
        
        // 特殊情况处理：Base64编码
        if ("define".equals(dataSource) && "base64".equals(operation)) {
            // 将Base64编码定义添加到payloads
            List<String> values = new ArrayList<>();
            values.add("@@base64(" + value + ")"); // 例如：@@base64(str2) 表示对str2进行Base64编码
            poc.getVariables().put(varName, values);
            return null;
        }
        
        // 特殊情况处理：DNSLog
        if ("dnslog".equals(dataSource)) {
            // 将DNSLog定义添加到payloads
            List<String> values = new ArrayList<>();
            values.add("@@dnslog()"); // 生成dnslog域名并检查
            poc.getVariables().put(varName, values);
            return null;
        }
        
        // 特殊情况处理：MD5/SHA1等哈希
        if ("md5".equals(operation) || "sha1".equals(operation) ||
             "sha256".equals(operation) || "sha512".equals(operation)) {
            // 将哈希编码定义添加到payloads
            if (poc != null && poc.getVariables() != null) {
                List<String> values = new ArrayList<>();
                values.add("@@"+operation.toLowerCase() + "(" + value + ")"); // 例如：md5(password)
                poc.getVariables().put(varName, values);
            }
            return null;
        }
        
        // 特殊情况处理：URL编码
        if (("urlencode".equals(operation) || "urldecode".equals(operation))) {
            // 将URL编码/解码定义添加到payloads
            List<String> values = new ArrayList<>();
            String opType = "urlencode".equals(operation) ? "URLENCODE" : "URLDECODE";
            values.add("@@" + opType + "(" + value + ")");
            poc.getVariables().put(varName, values);
            return null;
        }
        
        // 特殊情况处理：固定值
        if (("define".equals(dataSource) || "".equals(dataSource)) && ("str".equals(operation) || "text".equals(operation))) {
            // 将固定字符串定义添加到payloads
            List<String> values = new ArrayList<>();
            values.add(value); // 直接使用值
            poc.getVariables().put(varName, values);
            return null;
        }
        
        // 创建提取器
        Matcher extractor = new Matcher();
        
        // 处理变量名唯一，确保不重复
        String uniqueVarName = ensureUniqueVariableName(varName, existingExtractors);
        extractor.setName(uniqueVarName);
        
        // 处理数据源
        setExtractorDataSource(extractor, dataSource);
        
        // 处理操作类型
        setExtractorOperationType(extractor, operation);
        
        if(value.equals("") && operation.equals("regex")) {
            value = ".*";
        }
        // 处理匹配值
        if (!value.isEmpty()) {
            List<String> values = new ArrayList<>();
            values.add(value);
            extractor.setValues(values);
        }
        
        return extractor;
    }
    
    /**
     * 确保变量名在提取器列表中唯一
     * @param varName 原始变量名
     * @param existingExtractors 已存在的提取器列表
     * @return 唯一的变量名
     */
    private String ensureUniqueVariableName(String varName, List<Matcher> existingExtractors) {
        if (existingExtractors == null || existingExtractors.isEmpty()) {
            return varName;
        }
        
        String uniqueVarName = varName;
        int suffix = 1;
        
        while (isVariableNameExists(uniqueVarName, existingExtractors)) {
            uniqueVarName = varName + "_" + suffix++;
        }
        
        return uniqueVarName;
    }
    
    /**
     * 设置提取器的数据源
     * @param extractor 提取器对象
     * @param dataSource 数据源字符串
     */
    private void setExtractorDataSource(Matcher extractor, String dataSource) {
        switch (dataSource) {
            case "body":
            case "lastbody":
            case "lastraw":
                extractor.setPart("body");
                break;
            case "header":
            case "lastheader":
                extractor.setPart("header");
                break;
            case "status":
            case "code":
                extractor.setPart("status");
                extractor.setType(MatcherType.STATUS);
                break;
            case "reverse":
                extractor.setPart("reverse");
                break;
            case "dnslog":
                extractor.setPart("dnslog");
                break;
            default:
                extractor.setPart("body"); // 默认使用body
        }
    }
    
    /**
     * 设置提取器的操作类型
     * @param extractor 提取器对象
     * @param operation 操作类型字符串
     */
    private void setExtractorOperationType(Matcher extractor, String operation) {
        switch (operation) {
            case "regex":
                extractor.setType(MatcherType.REGEX);
                extractor.setOperation(OperationType.REGEX_MATCH);
                break;
            case "contains":
                extractor.setType(MatcherType.WORD);
                extractor.setOperation(OperationType.CONTAINS);
                break;
            case "sub":
                extractor.setType(MatcherType.WORD);
                extractor.setOperation(OperationType.CONTAINS);
                // 子字符串提取，可能需要特殊处理
                break;
            case "add":
                extractor.setType(MatcherType.WORD);
                extractor.setOperation(OperationType.CONTAINS);
                // 字符串拼接，可能需要特殊处理
                break;
            case "json":
                extractor.setType(MatcherType.JSON);
                extractor.setOperation(OperationType.DEFAULT);
                break;
            case "xpath":
                extractor.setType(MatcherType.XPATH);
                extractor.setOperation(OperationType.DEFAULT);
                break;
            default:
                extractor.setType(MatcherType.WORD);
                extractor.setOperation(OperationType.CONTAINS);
        }
    }
    
    /**
     * 检查变量名是否已存在于提取器列表中
     * @param varName 变量名
     * @param extractors 提取器列表
     * @return 是否存在
     */
    private boolean isVariableNameExists(String varName, List<Matcher> extractors) {
        if (varName == null || extractors == null || extractors.isEmpty()) {
            return false;
        }
        
        for (Matcher extractor : extractors) {
            if (varName.equals(extractor.getName())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 转换利用步骤
     */
    private void convertExploitSteps(GobyJsonObj.PocJson gobyPoc, PocObj.Poc poc) {
        if (!gobyPoc.isHasExp() || gobyPoc.getExploitSteps() == null || gobyPoc.getExploitSteps().isEmpty()) {
            return;
        }
        
        List<PocObj.PocStep> exploitSteps = new ArrayList<>();
        
        // 确定匹配条件
        MatchersCondition globalCondition = MatchersCondition.AND; // 默认为AND
        int startIndex = 0;
        
        // 检查第一个元素是否为条件字符串
        if (!gobyPoc.getExploitSteps().isEmpty() && gobyPoc.getExploitSteps().get(0) instanceof String) {
            String condition = (String) gobyPoc.getExploitSteps().get(0);
            if ("OR".equalsIgnoreCase(condition)) {
                globalCondition = MatchersCondition.OR;
            }
            startIndex = 1;
        }
        
        // 处理步骤
        for (int i = startIndex; i < gobyPoc.getExploitSteps().size(); i++) {
            Object stepObj = gobyPoc.getExploitSteps().get(i);
            try {
                PocObj.PocStep step = new PocObj.PocStep();
                step.setStepId("exploit_" + i);
                
                // 修改这里：不直接强制类型转换，而是从Map构建ScanStep对象
                if (stepObj instanceof Map) {
                    GobyJsonObj.ScanStep exploitStep = convertMapToScanStep((Map<String, Object>) stepObj);
                    convertScanStep(exploitStep, step, globalCondition, poc);
                    exploitSteps.add(step);
                } else {
                    System.out.println("利用步骤格式不正确: " + stepObj.getClass().getName());
                }
            } catch (Exception e) {
                System.out.println("转换利用步骤时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        if (!exploitSteps.isEmpty()) {
            poc.setExploitSteps(exploitSteps);
        }
    }
    
    /**
     * 转换ExpParam参数
     */
    private void convertExpParams(GobyJsonObj.PocJson gobyPoc, PocObj.Poc poc) {
        if (gobyPoc.getExpParams() == null || gobyPoc.getExpParams().isEmpty()) {
            return;
        }

        // 获取已存在的payloads，防止覆盖通过提取器添加的payload
        Map<String, List<String>> payloads = poc.getVariables() != null ?
            new HashMap<>(poc.getVariables()) : new HashMap<>();

        for (GobyJsonObj.ExpParam param : gobyPoc.getExpParams()) {
            if (param.getName() != null) {
                List<String> valueList = new ArrayList<>();
                String data = param.getValue();
                if (param.getType() != null && param.getType().toLowerCase().contains("select")){
                    valueList = new ArrayList<>(Arrays.asList(data.split(",")));
                }else {
                    valueList.add(data);
                }
                payloads.put(param.getName(), valueList);
            }
        }

        poc.setVariables(payloads);
    }
    
    /**
     * 从Map中安全获取字符串值
     * @param map Map对象
     * @param key 键名
     * @return 字符串值，如果不存在则返回null
     */
    private String safeGetString(Map<String, Object> map, String key) {
        if (map == null || key == null || !map.containsKey(key)) {
            return null;
        }
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * 从Map中安全获取布尔值
     * @param map Map对象
     * @param key 键名
     * @return 布尔值，如果不存在则返回false
     */
    private boolean safeGetBoolean(Map<String, Object> map, String key) {
        if (map == null || key == null || !map.containsKey(key)) {
            return false;
        }
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value != null && "true".equalsIgnoreCase(value.toString());
    }
}
