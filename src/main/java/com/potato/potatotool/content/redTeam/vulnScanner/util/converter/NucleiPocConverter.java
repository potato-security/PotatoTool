package com.potato.potatotool.content.redTeam.vulnScanner.util.converter;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.Matcher;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.MatcherType;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.MatchersCondition;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.Severity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Potato
 * @date 2025/3/19 16:30
 * Nuclei YAML POC转换器，用于将Nuclei格式的POC转换为通用PocObj
 */
public class NucleiPocConverter implements IPocConverter<NucleiYamlObj.Poc> {

    /**
     * 将Nuclei YAML POC转换为通用PocObj
     * @param nucleiPoc Nuclei YAML POC对象
     * @return 通用PocObj对象
     */
    @Override
    public PocObj.Poc convert(NucleiYamlObj.Poc nucleiPoc) {
        if (nucleiPoc == null || (nucleiPoc.getHttp() == null && nucleiPoc.getRequests() == null && nucleiPoc.getTcp() == null) ) {
            System.out.println("输入的非Nuclei_Yaml_POC对象");
            return null;
        }
        
        PocObj.Poc poc = new PocObj.Poc();
        
        // 转换基本信息
        convertBasicInfo(nucleiPoc, poc);
        
        // 转换搜索查询
        convertSearchQueries(nucleiPoc, poc);
        
        // 处理变量和流程
        convertVariablesAndFlow(nucleiPoc, poc);
        
        // 设置协议并处理请求
        setProtocolAndProcessRequests(nucleiPoc, poc);
        
        // 设置原始POC
        poc.setOriginalPoc(nucleiPoc);
        poc.setOriginalFormat("nuclei");
        
        return poc;
    }
    
    /**
     * 转换基本信息
     * @param nucleiPoc Nuclei POC对象
     * @param poc 通用POC对象
     */
    private void convertBasicInfo(NucleiYamlObj.Poc nucleiPoc, PocObj.Poc poc) {
        // 设置ID
        poc.setId(nucleiPoc.getId());
        
        // 设置基本信息字段
        if (nucleiPoc.getInfo() != null) {
            NucleiYamlObj.Info info = nucleiPoc.getInfo();
            poc.setName(safeGetString(info.getName()));
            poc.setAuthor(safeGetString(info.getAuthor()));
            poc.setDescription(safeGetString(info.getDescription()));
            
            // 设置严重程度
            convertSeverity(info, poc);
            
            // 设置参考链接
            convertReferences(info, poc);
            
            // 设置分类信息
            convertClassification(info, poc);
            
            // 设置标签
            convertTags(info, poc);
        }
    }
    
    /**
     * 转换标签信息
     * @param info Nuclei POC信息对象
     * @param poc 通用POC对象
     */
    private void convertTags(NucleiYamlObj.Info info, PocObj.Poc poc) {
        if (info.getTags() == null || info.getTags().isEmpty()) {
            return;
        }
        
        List<String> tags = new ArrayList<>();
        // 处理可能的逗号分隔的标签
        String[] tagArray = info.getTags().split("\\s*,\\s*");
        for (String tag : tagArray) {
            if (!tag.trim().isEmpty()) {
                tags.add(tag.trim());
            }
        }
        
        if (!tags.isEmpty()) {
            poc.setTags(tags);
        }
    }
    
    /**
     * 转换严重程度
     * @param info Nuclei POC信息对象
     * @param poc 通用POC对象
     */
    private void convertSeverity(NucleiYamlObj.Info info, PocObj.Poc poc) {
        if (info.getSeverity() == null) {
            poc.setSeverity(Severity.UNKNOWN);
            return;
        }
        
        switch (info.getSeverity()) {
            case info:
                poc.setSeverity(Severity.INFO);
                break;
            case low:
                poc.setSeverity(Severity.LOW);
                break;
            case medium:
                poc.setSeverity(Severity.MEDIUM);
                break;
            case high:
                poc.setSeverity(Severity.HIGH);
                break;
            case critical:
                poc.setSeverity(Severity.CRITICAL);
                break;
            default:
                poc.setSeverity(Severity.UNKNOWN);
        }
    }
    
    /**
     * 转换参考链接
     * @param info Nuclei POC信息对象
     * @param poc 通用POC对象
     */
    private void convertReferences(NucleiYamlObj.Info info, PocObj.Poc poc) {
        if (info.getReference() == null) {
            return;
        }
        
        List<String> references = new ArrayList<>();
        if (info.getReference() instanceof List) {
            references.addAll((List<String>) info.getReference());
        } else if (info.getReference() instanceof String) {
            references.add((String) info.getReference());
        }
        
        if (!references.isEmpty()) {
            poc.setReferences(references);
        }
    }
    
    /**
     * 转换分类信息
     * @param info Nuclei POC信息对象
     * @param poc 通用POC对象
     */
    private void convertClassification(NucleiYamlObj.Info info, PocObj.Poc poc) {
        if (info.getClassification() == null) {
            return;
        }
        
        NucleiYamlObj.Classification classification = info.getClassification();
        poc.setCveId(classification.getCve_id());
        poc.setCweId(classification.getCwe_id());
        poc.setCvssScore(classification.getCvss_score());
        poc.setCvssMetrics(classification.getCvss_metrics());
    }
    
    /**
     * 转换搜索查询
     * @param nucleiPoc Nuclei POC对象
     * @param poc 通用POC对象
     */
    private void convertSearchQueries(NucleiYamlObj.Poc nucleiPoc, PocObj.Poc poc) {
        if (nucleiPoc.getInfo() == null || nucleiPoc.getInfo().getMetadata() == null) {
            return;
        }
        
        NucleiYamlObj.Metadata metadata = nucleiPoc.getInfo().getMetadata();
        Map<String, String> searchQueries = new HashMap<>();
        
        // 处理Shodan查询
        processSearchQuery(metadata.getShodan_query(), "shodan", searchQueries);
        
        // 处理Fofa查询
        processSearchQuery(metadata.getFofa_query(), "fofa", searchQueries);
        
        // 处理Google查询
        processSearchQuery(metadata.getGoogle_query(), "google", searchQueries);
        
        // 处理PublicWWW查询
        processSearchQuery(metadata.getPublicwww_query(), "publicwww", searchQueries);
        
        // 处理ZoomEye查询
        processSearchQuery(metadata.getZoomeye_query(), "zoomeye", searchQueries);
        
        if (!searchQueries.isEmpty()) {
            poc.setSearchQueries(searchQueries);
        }
    }
    
    /**
     * 处理搜索查询
     * @param queryObj 查询对象
     * @param engineName 搜索引擎名称
     * @param searchQueries 搜索查询Map
     */
    private void processSearchQuery(Object queryObj, String engineName, Map<String, String> searchQueries) {
        if (queryObj == null) {
            return;
        }
        
        if (queryObj instanceof List) {
            searchQueries.put(engineName, String.join(" ", (List<String>) queryObj));
        } else {
            searchQueries.put(engineName, queryObj.toString());
        }
    }
    
    /**
     * 转换变量和流程
     * @param nucleiPoc Nuclei POC对象
     * @param poc 通用POC对象
     */
    private void convertVariablesAndFlow(NucleiYamlObj.Poc nucleiPoc, PocObj.Poc poc) {
        // 设置变量
        if (nucleiPoc.getVariables() != null && !nucleiPoc.getVariables().isEmpty()) {
            Map<String, List<String>> payloads = new HashMap<>();
            transformPayloadMap(nucleiPoc.getVariables(), payloads);
            if (!payloads.isEmpty()) {
                poc.setVariables(payloads);
            }
        }
        
        // 设置自包含和流程
        poc.setSelfContained(nucleiPoc.isSelf_contained());
        if (nucleiPoc.getFlow() != null && !nucleiPoc.getFlow().isEmpty()) {
            poc.setFlow(nucleiPoc.getFlow());
        }
        
        // 处理payloads字段
        processPayloads(nucleiPoc, poc);
    }
    
    /**
     * 设置协议并处理请求
     * @param nucleiPoc Nuclei POC对象
     * @param poc 通用POC对象
     */
    private void setProtocolAndProcessRequests(NucleiYamlObj.Poc nucleiPoc, PocObj.Poc poc) {
        // 根据请求类型动态设置协议
        if (nucleiPoc.getHttp() != null && !nucleiPoc.getHttp().isEmpty()) {
            poc.setProtocol("http");
            processHttpRequests(nucleiPoc.getHttp(), poc);
        } else if (nucleiPoc.getRequests() != null && !nucleiPoc.getRequests().isEmpty()) {
            poc.setProtocol("http");
            // 处理requests请求
            processRequestsField(nucleiPoc.getRequests(), poc);
        } else if (nucleiPoc.getTcp() != null && !nucleiPoc.getTcp().isEmpty()) {
            poc.setProtocol("tcp");
            processTcpRequests(nucleiPoc.getTcp(), poc);
        } else {
            // 默认使用HTTP协议
            poc.setProtocol("http");
        }
        
        // 处理全局配置
        processGlobalConfig(nucleiPoc, poc);
    }
    
    /**
     * 处理Nuclei HTTP请求
     * @param httpRequests HTTP请求列表
     * @param poc 通用POC对象
     */
    private void processHttpRequests(List<NucleiYamlObj.Http> httpRequests, PocObj.Poc poc) {
        try {
            List<PocObj.PocStep> verifySteps = new ArrayList<>();
            
            for (int i = 0; i < httpRequests.size(); i++) {
                try {
                    NucleiYamlObj.Http httpRequest = httpRequests.get(i);
                    PocObj.PocStep step = createHttpStep(httpRequest, i);
                    if (step != null) {
                        verifySteps.add(step);
                    }
                } catch (Exception e) {
                    System.out.println("处理HTTP请求时出错: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            if (!verifySteps.isEmpty()) {
                poc.setVerifySteps(verifySteps);
            }
        } catch (Exception e) {
            System.out.println("处理HTTP请求列表时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建HTTP步骤
     * @param httpRequest HTTP请求对象
     * @param index 索引
     * @return PocStep对象
     */
    private PocObj.PocStep createHttpStep(NucleiYamlObj.Http httpRequest, int index) {
        PocObj.PocStep step = new PocObj.PocStep();
        
        // 设置步骤ID
        step.setStepId("http_" + index);
        
        // 设置请求方法
        step.setMethod(httpRequest.getMethod());
        
        // 设置路径
        if (httpRequest.getPath() != null && !httpRequest.getPath().isEmpty()) {
            step.setPath(httpRequest.getPath().get(0)); // 取第一个路径，实际可能需要更复杂的处理
        }
        
        // 设置请求头
        if (httpRequest.getHeaders() != null) {
            step.setHeaders(httpRequest.getHeaders());
        }
        
        // 设置是否跟随重定向
        step.setFollowRedirect(httpRequest.isRedirects());
        
        // 设置是否不安全请求
        step.setUnsafe(httpRequest.isUnsafe());
        
        // 设置是否禁用Cookie
        step.setDisableCookie(httpRequest.isDisable_cookie());
        
        // 设置是否禁用路径自动合并
        step.setDisablePathAutomerge(httpRequest.isDisable_path_automerge());
        
        // 设置原始请求
        if (httpRequest.getRaw() != null) {
            step.setRaw(httpRequest.getRaw());
        }
        
        // 处理匹配器
        processMatchers(httpRequest.getMatchers(), httpRequest.getMatchers_condition(), step);
        
        // 处理提取器
        processExtractors(httpRequest.getExtractors(), step);
        
        return step;
    }
    
    /**
     * 处理匹配器
     * @param templateMatchers 模板匹配器列表
     * @param matchersCondition 匹配器条件
     * @param step POC步骤
     */
    private void processMatchers(List<NucleiYamlObj.TemplateMatcher> templateMatchers, NucleiYamlObj.MatchersCondition matchersCondition, PocObj.PocStep step) {
        if (templateMatchers == null || templateMatchers.isEmpty()) {
            return;
        }
        
        List<Matcher> matchers = new ArrayList<>();
        for (NucleiYamlObj.TemplateMatcher templateMatcher : templateMatchers) {
            try {
                Matcher matcher = convertMatcher(templateMatcher);
                if (matcher != null) {
                    matchers.add(matcher);
                }
            } catch (Exception e) {
                System.out.println("转换匹配器时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        if (!matchers.isEmpty()) {
            step.setMatchers(matchers);
            
            // 设置匹配条件
            if (matchersCondition == NucleiYamlObj.MatchersCondition.and) {
                step.setMatchersCondition(MatchersCondition.AND);
            } else {
                step.setMatchersCondition(MatchersCondition.OR);
            }
        }
    }
    
    /**
     * 处理Nuclei TCP请求
     * @param tcpRequests TCP请求列表
     * @param poc 通用POC对象
     */
    private void processTcpRequests(List<NucleiYamlObj.Tcp> tcpRequests, PocObj.Poc poc) {
        try {
            List<PocObj.PocStep> verifySteps = new ArrayList<>();
            
            for (int i = 0; i < tcpRequests.size(); i++) {
                try {
                    NucleiYamlObj.Tcp tcpRequest = tcpRequests.get(i);
                    PocObj.TcpStep step = createTcpStep(tcpRequest, i);
                    if (step != null) {
                        verifySteps.add(step);
                    }
                } catch (Exception e) {
                    System.out.println("处理TCP请求时出错: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // 合并到现有的验证步骤中
            List<PocObj.PocStep> existingSteps = poc.getVerifySteps();
            if (existingSteps == null) {
                existingSteps = new ArrayList<>();
            }
            existingSteps.addAll(verifySteps);
            poc.setVerifySteps(existingSteps);
        } catch (Exception e) {
            System.out.println("处理TCP请求列表时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建TCP步骤
     * @param tcpRequest TCP请求对象
     * @param index 索引
     * @return TcpStep对象
     */
    private PocObj.TcpStep createTcpStep(NucleiYamlObj.Tcp tcpRequest, int index) {
        PocObj.TcpStep step = new PocObj.TcpStep();
        
        // 设置步骤ID
        step.setStepId("tcp_" + index);
        
        // 设置主机和端口
        if (tcpRequest.getHost() != null && !tcpRequest.getHost().isEmpty()) {
            step.setHost(tcpRequest.getHost().get(0)); // 取第一个主机，实际可能需要更复杂的处理
        }
        step.setPort(tcpRequest.getPort());
        
        // 设置读取大小
        step.setReadSize(tcpRequest.getRead_size());
        
        // 处理输入
        processInputs(tcpRequest.getInputs(), step);
        
        // 处理匹配器
        processMatchers(tcpRequest.getMatchers(), tcpRequest.getMatchers_condition(), step);
        
        // 处理提取器
        processExtractors(tcpRequest.getExtractors(), step);
        
        return step;
    }
    
    /**
     * 处理TCP输入
     * @param inputs 输入列表
     * @param step TCP步骤
     */
    private void processInputs(List<NucleiYamlObj.Input> inputs, PocObj.TcpStep step) {
        if (inputs == null || inputs.isEmpty()) {
            return;
        }
        
        List<PocObj.Input> newInputs = new ArrayList<>();
        for (NucleiYamlObj.Input input : inputs) {
            try {
                PocObj.Input newInput = new PocObj.Input();
                newInput.setData(input.getData());
                newInput.setType(input.getType() != null ? input.getType().toString() : null);
                newInput.setName(input.getName());
                newInput.setRead(String.valueOf(input.getRead()));
                newInputs.add(newInput);
            } catch (Exception e) {
                System.out.println("处理TCP输入时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        if (!newInputs.isEmpty()) {
            step.setInputs(newInputs);
        }
    }
    
    /**
     * 转换Nuclei匹配器
     * @param templateMatcher 模板匹配器
     * @return 匹配器对象
     */
    private Matcher convertMatcher(NucleiYamlObj.TemplateMatcher templateMatcher) {
        if (templateMatcher == null) {
            return null;
        }
        
        try {
            Matcher matcher = new Matcher();
            
            // 根据类型设置匹配器类型
            switch (templateMatcher.getType()) {
                case "status":
                    matcher.setType(MatcherType.STATUS);
                    if (templateMatcher instanceof NucleiYamlObj.Status) {
                        NucleiYamlObj.Status status = (NucleiYamlObj.Status) templateMatcher;
                        List<String> values = new ArrayList<>();
                        for (Integer statusCode : status.getStatus()) {
                            values.add(statusCode.toString());
                        }
                        matcher.setValues(values);
                    }
                    break;
                case "word":
                    matcher.setType(MatcherType.WORD);
                    if (templateMatcher instanceof NucleiYamlObj.Word) {
                        NucleiYamlObj.Word word = (NucleiYamlObj.Word) templateMatcher;
                        matcher.setValues(word.getWords());
                        matcher.setPart(word.getPart());
                        matcher.setNegative(word.isNegative());
                        matcher.setCaseInsensitive(word.isCase_insensitive());
                    }
                    break;
                case "regex":
                    matcher.setType(MatcherType.REGEX);
                    if (templateMatcher instanceof NucleiYamlObj.Regex) {
                        NucleiYamlObj.Regex regex = (NucleiYamlObj.Regex) templateMatcher;
                        matcher.setValues(regex.getRegex());
                        matcher.setPart(regex.getPart());
                        matcher.setGroup(regex.getGroup());
                        matcher.setName(regex.getName());
                        matcher.setInternal(String.valueOf(regex.isInternal()));
                    }
                    break;
                case "binary":
                    matcher.setType(MatcherType.BINARY);
                    if (templateMatcher instanceof NucleiYamlObj.Binary) {
                        NucleiYamlObj.Binary binary = (NucleiYamlObj.Binary) templateMatcher;
                        matcher.setValues(binary.getBinary());
                        matcher.setPart(binary.getPart());
                    }
                    break;
                case "dsl":
                    matcher.setType(MatcherType.DSL);
                    if (templateMatcher instanceof NucleiYamlObj.Dsl) {
                        NucleiYamlObj.Dsl dsl = (NucleiYamlObj.Dsl) templateMatcher;
                        matcher.setValues(dsl.getDsl());
                        matcher.setName(dsl.getName());
                    }
                    break;
                case "xpath":
                    matcher.setType(MatcherType.XPATH);
                    if (templateMatcher instanceof NucleiYamlObj.Xpath) {
                        NucleiYamlObj.Xpath xpath = (NucleiYamlObj.Xpath) templateMatcher;
                        matcher.setValues(xpath.getXpath());
                        matcher.setAttribute(xpath.getAttribute());
                    }
                    break;
                case "json":
                    matcher.setType(MatcherType.JSON);
                    if (templateMatcher instanceof NucleiYamlObj.Json) {
                        NucleiYamlObj.Json json = (NucleiYamlObj.Json) templateMatcher;
                        matcher.setValues(json.getJson());
                    }
                    break;
                case "kval":
                    matcher.setType(MatcherType.KVAL);
                    if (templateMatcher instanceof NucleiYamlObj.Kval) {
                        NucleiYamlObj.Kval kval = (NucleiYamlObj.Kval) templateMatcher;
                        matcher.setValues(kval.getKval());
                    }
                    break;
                default:
                    matcher.setType(MatcherType.UNKNOWN);
            }
            
            // 设置条件
            if (templateMatcher.getCondition() == NucleiYamlObj.Condition.and) {
                matcher.setCondition("AND");
            } else {
                matcher.setCondition("OR");
            }
            
            return matcher;
        } catch (Exception e) {
            System.out.println("转换匹配器时出错: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 安全获取字符串值
     * @param value 原始值
     * @return 字符串值，如果为null则返回空字符串
     */
    private String safeGetString(String value) {
        return value != null ? value : "";
    }
    
    /**
     * 安全获取字符串值，带默认值
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 字符串值，如果为null则返回默认值
     */
    private String safeGetString(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    /**
     * 处理提取器
     * @param extractors 提取器列表
     * @param step POC步骤
     */
    private void processExtractors(List<NucleiYamlObj.TemplateMatcher> extractors, PocObj.PocStep step) {
        if (extractors == null || extractors.isEmpty()) {
            return;
        }
        
        Map<String, String> extractorMap = new HashMap<>();
        
        for (NucleiYamlObj.TemplateMatcher extractor : extractors) {
            try {
                // 处理不同类型的提取器
                if (extractor instanceof NucleiYamlObj.Regex) {
                    NucleiYamlObj.Regex regex = (NucleiYamlObj.Regex) extractor;
                    if (regex.getName() != null && !regex.getName().isEmpty() && regex.getRegex() != null && !regex.getRegex().isEmpty()) {
                        extractorMap.put(regex.getName(), "regex:" + String.join(",", regex.getRegex()));
                    }
                } else if (extractor instanceof NucleiYamlObj.Json) {
                    NucleiYamlObj.Json json = (NucleiYamlObj.Json) extractor;
                    if (json.getJson() != null && !json.getJson().isEmpty()) {
                        extractorMap.put("json_" + extractorMap.size(), "json:" + String.join(",", json.getJson()));
                    }
                } else if (extractor instanceof NucleiYamlObj.Xpath) {
                    NucleiYamlObj.Xpath xpath = (NucleiYamlObj.Xpath) extractor;
                    if (xpath.getXpath() != null && !xpath.getXpath().isEmpty()) {
                        extractorMap.put("xpath_" + extractorMap.size(), "xpath:" + String.join(",", xpath.getXpath()));
                    }
                } else if (extractor instanceof NucleiYamlObj.Dsl) {
                    NucleiYamlObj.Dsl dsl = (NucleiYamlObj.Dsl) extractor;
                    if (dsl.getName() != null && !dsl.getName().isEmpty() && dsl.getDsl() != null && !dsl.getDsl().isEmpty()) {
                        extractorMap.put(dsl.getName(), "dsl:" + String.join(",", dsl.getDsl()));
                    }
                }
            } catch (Exception e) {
                System.out.println("处理提取器时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        if (!extractorMap.isEmpty()) {
//            step.setExtractors(extractorMap);
        }
    }
    
    /**
     * 处理requests字段
     * @param requests 请求列表
     * @param poc 通用POC对象
     */
    private void processRequestsField(List<NucleiYamlObj.Request> requests, PocObj.Poc poc) {
        try {
            List<PocObj.PocStep> verifySteps = new ArrayList<>();
            
            for (int i = 0; i < requests.size(); i++) {
                try {
                    NucleiYamlObj.Request request = requests.get(i);
                    PocObj.PocStep step = createRequestStep(request, i);
                    if (step != null) {
                        verifySteps.add(step);
                    }
                } catch (Exception e) {
                    System.out.println("处理Request请求时出错: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            if (!verifySteps.isEmpty()) {
                poc.setVerifySteps(verifySteps);
            }
        } catch (Exception e) {
            System.out.println("处理Request请求列表时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建Request步骤
     * @param request Request请求对象
     * @param index 索引
     * @return PocStep对象
     */
    private PocObj.PocStep createRequestStep(NucleiYamlObj.Request request, int index) {
        PocObj.PocStep step = new PocObj.PocStep();
        
        // 设置步骤ID
        step.setStepId("request_" + index);
        
        // 设置请求方法
        step.setMethod(request.getMethod());
        
        // 设置路径
        if (request.getPath() != null && !request.getPath().isEmpty()) {
            step.setPath(request.getPath().get(0)); // 取第一个路径
        }
        
        // 设置请求头
        if (request.getHeaders() != null) {
            step.setHeaders(request.getHeaders());
        }
        
        // 设置是否跟随重定向
        step.setFollowRedirect(request.isRedirects());
        
        // 设置是否不安全请求
        step.setUnsafe(request.isUnsafe());
        
        // 设置是否禁用Cookie
        step.setDisableCookie(request.isDisable_cookie());
        
        // 设置是否禁用路径自动合并
        step.setDisablePathAutomerge(request.isDisable_path_automerge());
        
        // 设置原始请求
        if (request.getRaw() != null) {
            step.setRaw(request.getRaw());
        }
        
        // 处理匹配器
        processMatchers(request.getMatchers(), request.getMatchers_condition(), step);
        
        // 处理提取器
        processExtractors(request.getExtractors(), step);
        
        return step;
    }
    
    /**
     * 处理全局配置
     * @param nucleiPoc Nuclei POC对象
     * @param poc 通用POC对象
     */
    private void processGlobalConfig(NucleiYamlObj.Poc nucleiPoc, PocObj.Poc poc) {
        PocObj.GlobalConfig globalConfig = poc.getGlobalConfig();
        
        // 处理HTTP请求的全局配置
        if (nucleiPoc.getHttp() != null && !nucleiPoc.getHttp().isEmpty()) {
            NucleiYamlObj.Http firstHttp = nucleiPoc.getHttp().get(0);
            
            // 设置Cookie复用
            globalConfig.setCookieReuse(firstHttp.isCookie_reuse());
            
            // 设置线程数
            if (firstHttp.getThreads() > 0) {
                globalConfig.setThreads(firstHttp.getThreads());
            }
            
            // 设置首次匹配后停止
            globalConfig.setStopAtFirstMatch(firstHttp.isStop_at_first_match());
        }
        // 处理Request请求的全局配置
        else if (nucleiPoc.getRequests() != null && !nucleiPoc.getRequests().isEmpty()) {
            NucleiYamlObj.Request firstRequest = nucleiPoc.getRequests().get(0);
            
            // 设置Cookie复用
            globalConfig.setCookieReuse(firstRequest.isCookie_reuse());
            
            // 设置线程数
            if (firstRequest.getThreads() > 0) {
                globalConfig.setThreads(firstRequest.getThreads());
            }
            
            // 设置首次匹配后停止
            globalConfig.setStopAtFirstMatch(firstRequest.isStop_at_first_match());
        }
    }
    
    /**
     * 处理Payloads字段
     * @param nucleiPoc Nuclei POC对象
     * @param poc 通用POC对象
     */
    private void processPayloads(NucleiYamlObj.Poc nucleiPoc, PocObj.Poc poc) {
        Map<String, List<String>> payloads = new HashMap<>();
        
        // 处理HTTP请求中的payloads
        if (nucleiPoc.getHttp() != null && !nucleiPoc.getHttp().isEmpty()) {
            for (NucleiYamlObj.Http http : nucleiPoc.getHttp()) {
                if (http.getPayloads() != null && !http.getPayloads().isEmpty()) {
                    processPayloadMap(http.getPayloads(), payloads);
                }
                if (http.getAttack() != null) {
                    poc.setVariablesType(http.getAttack());
                }
            }
        }
        
        // 处理Request请求中的payloads
        if (nucleiPoc.getRequests() != null && !nucleiPoc.getRequests().isEmpty()) {
            for (NucleiYamlObj.Request request : nucleiPoc.getRequests()) {
                if (request.getPayloads() != null && !request.getPayloads().isEmpty()) {
                    processPayloadMap(request.getPayloads(), payloads);
                }
                if (request.getAttack() != null) {
                    poc.setVariablesType(request.getAttack());
                }
            }
        }
        
        // 处理TCP请求中的payloads
        if (nucleiPoc.getTcp() != null && !nucleiPoc.getTcp().isEmpty()) {
            for (NucleiYamlObj.Tcp tcp : nucleiPoc.getTcp()) {
                if (tcp.getPayloads() != null && !tcp.getPayloads().isEmpty()) {
                    processPayloadMap(tcp.getPayloads(), payloads);
                }
                if (tcp.getAttack() != null) {
                    poc.setVariablesType(tcp.getAttack());
                }
            }
        }
        
        if (!payloads.isEmpty()) {
            // 获取现有的变量并合并新的payloads
            Map<String, List<String>> existingVariables = poc.getVariables();
            if (existingVariables != null) {
                existingVariables.putAll(payloads);
            } else {
                poc.setVariables(payloads);
            }
        }
    }
    
    /**
     * 处理Payload映射
     * @param sourcePayloads 源Payload映射
     * @param targetPayloads 目标Payload映射
     */
    private void processPayloadMap(Map<String, Object> sourcePayloads, Map<String, List<String>> targetPayloads) {
        for (Map.Entry<String, Object> entry : sourcePayloads.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof List) {
                // 如果已经是List<String>，直接添加
                @SuppressWarnings("unchecked")
                List<String> valueList = (List<String>) value;
                targetPayloads.put(key, valueList);
            } else if (value instanceof String) {
                // 如果是单个字符串，转换为List
                List<String> valueList = new ArrayList<>();
                valueList.add((String) value);
                targetPayloads.put(key, valueList);
            }
        }
    }
    private void transformPayloadMap(Map<String, String> sourcePayloads, Map<String, List<String>> targetPayloads) {
        for (Map.Entry<String, String> entry : sourcePayloads.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            List<String> valueList = new ArrayList<>();
            valueList.add(value);
            targetPayloads.put(key, valueList);
        }
    }

}