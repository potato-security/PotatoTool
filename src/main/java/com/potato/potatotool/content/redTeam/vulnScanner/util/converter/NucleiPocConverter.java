package com.potato.potatotool.content.redTeam.vulnScanner.util.converter;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.Matcher;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.MatcherType;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.MatchersCondition;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.Severity;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.potato.potatotool.content.redTeam.vulnScanner.event.ScanErrorEvent.logUnrecognizedExpression;

/**
 * @author Potato
 * @date 2025/2/19 16:30
 * Nuclei YAML POC转换器，用于将Nuclei格式的POC转换为通用PocObj
 */
public class NucleiPocConverter extends AbstractPocConverter<NucleiYamlObj.Poc> {

    private static final Set<String> SUPPORTED_MATCHER_TYPES = new HashSet<>(Arrays.asList(
            "status", "size", "word", "regex", "binary", "dsl", "time", "xpath", "json", "kval"
    ));
    private static final String PROTOCOL_INFO_PREFIX = "协议检测结果: ";

    private List<Map<String, Object>> conversionWarnings = new ArrayList<>();
    private List<Map<String, Object>> unsupportedCapabilities = new ArrayList<>();

    /**
     * 将Nuclei YAML POC转换为通用PocObj
     * @param nucleiPoc Nuclei YAML POC对象
     * @return 通用PocObj对象
     */
    @Override
    public PocObj.Poc convert(NucleiYamlObj.Poc nucleiPoc) {
        // 检查是否为有效的 Nuclei POC（至少包含一种协议）
        if (nucleiPoc == null) {
            System.err.println("❌ POC 对象为 null，无法转换");
            return null;
        }

        PocObj.Poc poc = new PocObj.Poc();
        this.conversionWarnings = new ArrayList<>();
        this.unsupportedCapabilities = new ArrayList<>();

        // 详细检查每个协议
        boolean hasProtocol = false;
        StringBuilder protocolInfo = new StringBuilder(PROTOCOL_INFO_PREFIX);

        if (nucleiPoc.getHttp() != null && !nucleiPoc.getHttp().isEmpty()) {
            hasProtocol = true;
            protocolInfo.append("HTTP(").append(nucleiPoc.getHttp().size()).append(") ");
        }
        if (nucleiPoc.getRequests() != null && !nucleiPoc.getRequests().isEmpty()) {
            hasProtocol = true;
            protocolInfo.append("Requests(").append(nucleiPoc.getRequests().size()).append(") ");
        }
        if (nucleiPoc.getTcp() != null && !nucleiPoc.getTcp().isEmpty()) {
            hasProtocol = true;
            protocolInfo.append("TCP(").append(nucleiPoc.getTcp().size()).append(") ");
        }
        if (nucleiPoc.getNetwork() != null && !nucleiPoc.getNetwork().isEmpty()) {
            hasProtocol = true;
            protocolInfo.append("Network(").append(nucleiPoc.getNetwork().size()).append(") ");
        }
        if (nucleiPoc.getDns() != null && !nucleiPoc.getDns().isEmpty()) {
            hasProtocol = true;
            protocolInfo.append("DNS(").append(nucleiPoc.getDns().size()).append(") ");
        }
        if (nucleiPoc.getWebsocket() != null && !nucleiPoc.getWebsocket().isEmpty()) {
            hasProtocol = true;
            protocolInfo.append("WebSocket(").append(nucleiPoc.getWebsocket().size()).append(") ");
        }
        if (nucleiPoc.getSsl() != null && !nucleiPoc.getSsl().isEmpty()) {
            hasProtocol = true;
            protocolInfo.append("SSL(").append(nucleiPoc.getSsl().size()).append(") ");
        }
        if (nucleiPoc.getFile() != null && !nucleiPoc.getFile().isEmpty()) {
            hasProtocol = true;
            protocolInfo.append("File(").append(nucleiPoc.getFile().size()).append(") ");
        }
        if (nucleiPoc.getHeadless() != null && !nucleiPoc.getHeadless().isEmpty()) {
            hasProtocol = true;
            protocolInfo.append("Headless(").append(nucleiPoc.getHeadless().size()).append(") ");
        }
        if (nucleiPoc.getCode() != null && !nucleiPoc.getCode().isEmpty()) {
            hasProtocol = true;
            protocolInfo.append("Code(").append(nucleiPoc.getCode().size()).append(") ");
        }
        if (nucleiPoc.getJavascript() != null && !nucleiPoc.getJavascript().isEmpty()) {
            hasProtocol = true;
            protocolInfo.append("JavaScript(").append(nucleiPoc.getJavascript().size()).append(") ");
        }

        if (!hasProtocol) {
            System.err.println("❌ POC ID: " + nucleiPoc.getId() + " - 未检测到任何协议");
            System.err.println("   " + nucleiPoc);
            System.err.println("   " + protocolInfo);
            System.err.println("   可能原因: YAML 解析失败，导致协议字段为 null 或空");

            convertBasicInfo(nucleiPoc, poc);
            convertSearchQueries(nucleiPoc, poc);
            convertVariablesAndFlow(nucleiPoc, poc);

            String protocolSnapshot = protocolInfo.length() > "协议检测结果: ".length()
                    ? protocolInfo.toString()
                    : "协议检测结果: none";

            addUnsupportedCapability(
                    "NO_PROTOCOL_BLOCK",
                    "P0",
                    "nuclei",
                    "global",
                    "protocol",
                    protocolSnapshot,
                    "drop",
                    "模板未检测到任何协议块，当前不执行扫描"
            );

            finalizeConvertedPoc(poc, nucleiPoc);
            return poc;
        }

        System.out.println("✅ POC 转换开始 - ID: " + nucleiPoc.getId() + ", " + protocolInfo);

        // 转换基本信息
        convertBasicInfo(nucleiPoc, poc);

        // 转换搜索查询
        convertSearchQueries(nucleiPoc, poc);

        // 处理变量和流程
        convertVariablesAndFlow(nucleiPoc, poc);

        // 设置协议并处理请求
        setProtocolAndProcessRequests(nucleiPoc, poc);

        // 设置原始POC
        finalizeConvertedPoc(poc, nucleiPoc);

        // 注意：Nuclei 默认攻击模式为 batteringram（已在 PocObj 中设置）
        // 如果 Nuclei POC 在 YAML 中指定了 attack 字段，会在 processPayloads 方法中覆盖默认值

        return poc;
    }

    private void finalizeConvertedPoc(PocObj.Poc poc, NucleiYamlObj.Poc nucleiPoc) {
        poc.setOriginalPoc(nucleiPoc);
        poc.setOriginalFormat("nuclei");
        poc.setConversionWarnings(new ArrayList<>(conversionWarnings));
        poc.setUnsupportedCapabilities(new ArrayList<>(unsupportedCapabilities));
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
        
        // 使用基类的 parseSeverity 方法
        poc.setSeverity(parseSeverity(info.getSeverity().name()));
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
            // 转换为 Map<String, Object> 以匹配父类方法签名
            Map<String, Object> variablesObjMap = new HashMap<>(nucleiPoc.getVariables());
            transformPayloadMap(variablesObjMap, payloads);
            if (!payloads.isEmpty()) {
                poc.setVariables(payloads);
            }
        }
        
        // 设置自包含和流程
        poc.setSelfContained(nucleiPoc.isSelf_contained());
        if (nucleiPoc.getFlow() != null && !nucleiPoc.getFlow().isEmpty()) {
            poc.setFlow(nucleiPoc.getFlow());
            if (!isSimpleFlowExpression(nucleiPoc.getFlow())) {
                addConversionWarning("UNSUPPORTED_FLOW_EXPRESSION", "P1", "nuclei", "global", "flow",
                        nucleiPoc.getFlow(), "fallback",
                        "flow 表达式超出当前简化执行子集，后续阶段将按兼容路径执行");
            }
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
        } else if (nucleiPoc.getNetwork() != null && !nucleiPoc.getNetwork().isEmpty()) {
            poc.setProtocol("tcp");
            processTcpRequests(nucleiPoc.getNetwork(), poc);
        } else if (nucleiPoc.getDns() != null && !nucleiPoc.getDns().isEmpty()) {
            poc.setProtocol("dns");
            processDnsRequests(nucleiPoc.getDns(), poc);
        } else if (nucleiPoc.getWebsocket() != null && !nucleiPoc.getWebsocket().isEmpty()) {
            poc.setProtocol("websocket");
            processWebSocketRequests(nucleiPoc.getWebsocket(), poc);
        } else if (nucleiPoc.getSsl() != null && !nucleiPoc.getSsl().isEmpty()) {
            poc.setProtocol("ssl");
            processSslRequests(nucleiPoc.getSsl(), poc);
        } else if (nucleiPoc.getFile() != null && !nucleiPoc.getFile().isEmpty()) {
            poc.setProtocol("file");
            processFileRequests(nucleiPoc.getFile(), poc);
        } else if (nucleiPoc.getHeadless() != null && !nucleiPoc.getHeadless().isEmpty()) {
            poc.setProtocol("headless");
            processHeadlessRequests(nucleiPoc.getHeadless(), poc);
        } else if (nucleiPoc.getCode() != null && !nucleiPoc.getCode().isEmpty()) {
            poc.setProtocol("code");
            processCodeRequests(nucleiPoc.getCode(), poc);
        } else if (nucleiPoc.getJavascript() != null && !nucleiPoc.getJavascript().isEmpty()) {
            poc.setProtocol("javascript");
            processCodeRequests(nucleiPoc.getJavascript(), poc);  // 复用 code 处理逻辑
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
                    
                    boolean hasRaw = httpRequest.getRaw() != null && !httpRequest.getRaw().isEmpty();

                    if (hasRaw) {
                        // 如果有raw字段，通常只处理一次（raw中包含完整的HTTP请求）
                        PocObj.PocStep step = createHttpStep(httpRequest, i, null, "");
                        if (step != null) {
                            verifySteps.add(step);
                        }
                    } else if (httpRequest.getPath() != null && !httpRequest.getPath().isEmpty()) {
                        // 如果有多个path，为每个path创建一个独立的step
                        for (int pIdx = 0; pIdx < httpRequest.getPath().size(); pIdx++) {
                            String path = httpRequest.getPath().get(pIdx);
                            // 如果有多个path，添加后缀区分stepId
                            String suffix = httpRequest.getPath().size() > 1 ? "_" + pIdx : "";
                            
                            PocObj.PocStep step = createHttpStep(httpRequest, i, path, suffix);
                            if (step != null) {
                                verifySteps.add(step);
                            }
                        }
                    } else {
                        // 既没有raw也没有path的情况（可能是异常或空请求）
                        PocObj.PocStep step = createHttpStep(httpRequest, i, null, "");
                        if (step != null) {
                            verifySteps.add(step);
                        }
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
     * @param specificPath 指定的路径（如果为null则尝试从httpRequest获取）
     * @param idSuffix ID后缀（用于区分多路径裂变）
     * @return PocStep对象
     */
    private PocObj.PocStep createHttpStep(NucleiYamlObj.Http httpRequest, int index, String specificPath, String idSuffix) {
        PocObj.PocStep step = new PocObj.PocStep();
        
        // 设置步骤ID
        step.setStepId("http_" + index + idSuffix);
        
        // 检查是否使用raw格式
        boolean hasRaw = httpRequest.getRaw() != null && !httpRequest.getRaw().isEmpty();
        
        if (hasRaw) {
            // 如果有raw字段，优先使用raw（raw中包含完整的HTTP请求）
            step.setRaw(httpRequest.getRaw());
            // 注意：raw格式中已经包含了method、path、headers等信息，这里不需要单独设置
            // 如果同时设置了headers字段，会与raw冲突，应该忽略
            // 清空headers字段，防止与raw冲突
            step.setHeaders(null);
        } else {
            // 没有raw字段时，使用普通字段
            // 设置请求方法
            step.setMethod(httpRequest.getMethod());
            
            // 设置路径
            if (specificPath != null) {
                step.setPath(specificPath);
            } else if (httpRequest.getPath() != null && !httpRequest.getPath().isEmpty()) {
                step.setPath(httpRequest.getPath().get(0)); // Fallback
            }
            
            // 设置请求头
            if (httpRequest.getHeaders() != null && !httpRequest.getHeaders().isEmpty()) {
                step.setHeaders(httpRequest.getHeaders());
            }
        }
        
        // 设置是否跟随重定向
        step.setFollowRedirect(httpRequest.isRedirects());
        
        // 设置是否不安全请求
        step.setUnsafe(httpRequest.isUnsafe());
        
        // 设置是否禁用Cookie
        step.setDisableCookie(httpRequest.isDisable_cookie());
        
        // 设置是否禁用路径自动合并
        step.setDisablePathAutomerge(httpRequest.isDisable_path_automerge());
        
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
                String matcherType = templateMatcher != null ? templateMatcher.getType() : "unknown";
                addConversionWarning("MATCHER_CONVERSION_ERROR", "P1", "nuclei", inferProtocolFromStep(step),
                        "matcher", matcherType, "fallback",
                        "转换匹配器时出错: " + e.getMessage());
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
                    
                    if (tcpRequest.getHost() != null && !tcpRequest.getHost().isEmpty()) {
                        // 如果有多个host，为每个host创建一个独立的step
                        for (int hIdx = 0; hIdx < tcpRequest.getHost().size(); hIdx++) {
                            String host = tcpRequest.getHost().get(hIdx);
                            String suffix = tcpRequest.getHost().size() > 1 ? "_" + hIdx : "";
                            
                            PocObj.TcpStep step = createTcpStep(tcpRequest, i, host, suffix);
                            if (step != null) {
                                verifySteps.add(step);
                            }
                        }
                    } else {
                        // 没有host的情况（可能是异常）
                        PocObj.TcpStep step = createTcpStep(tcpRequest, i, null, "");
                        if (step != null) {
                            verifySteps.add(step);
                        }
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
     * @param specificHost 指定的主机
     * @param idSuffix ID后缀
     * @return TcpStep对象
     */
    private PocObj.TcpStep createTcpStep(NucleiYamlObj.Tcp tcpRequest, int index, String specificHost, String idSuffix) {
        PocObj.TcpStep step = new PocObj.TcpStep();
        
        // 设置步骤ID
        step.setStepId("tcp_" + index + idSuffix);
        
        // 设置主机和端口
        if (specificHost != null) {
            step.setHost(specificHost);
        } else if (tcpRequest.getHost() != null && !tcpRequest.getHost().isEmpty()) {
            step.setHost(tcpRequest.getHost().get(0)); // Fallback
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
            
            // 调试: 输出实际的matcher类型
            System.out.println("[DEBUG] Matcher类型: " + templateMatcher.getType() + ", 实际类: " + templateMatcher.getClass().getSimpleName());
            
            String matcherType = templateMatcher.getType();
            if (!SUPPORTED_MATCHER_TYPES.contains(matcherType)) {
                addUnsupportedCapability("UNSUPPORTED_MATCHER_TYPE", "P0", "nuclei", "unknown",
                        "matcher.type", matcherType, "fallback",
                        "当前版本不支持该 matcher 类型，已降级为 UNKNOWN");
            }

            // 根据类型设置匹配器类型
            switch (matcherType) {
                case "status":
                    matcher.setType(MatcherType.STATUS);
                    if (templateMatcher instanceof NucleiYamlObj.Status) {
                        NucleiYamlObj.Status status = (NucleiYamlObj.Status) templateMatcher;
                        List<String> values = new ArrayList<>();
                        // 调试: 输出 status 字段的值
                        System.out.println("[DEBUG] Status.getStatus() = " + status.getStatus());
                        if (status.getStatus() != null) {
                            for (Integer statusCode : status.getStatus()) {
                                values.add(statusCode.toString());
                            }
                        }
                        System.out.println("[DEBUG] 转换后 values = " + values);
                        matcher.setValues(values);
                        // 设置名称，优先使用matcher自带的name
                        if (status.getName() != null && !status.getName().isEmpty()) {
                            matcher.setName(status.getName());
                        } else {
                            matcher.setName("status_matcher");
                        }
                    }
                    break;
                case "size":
                    matcher.setType(MatcherType.SIZE);
                    if (templateMatcher instanceof NucleiYamlObj.Size) {
                        NucleiYamlObj.Size size = (NucleiYamlObj.Size) templateMatcher;
                        List<String> values = new ArrayList<>();
                        if (size.getSize() != null) {
                            for (Integer expectedSize : size.getSize()) {
                                values.add(String.valueOf(expectedSize));
                            }
                        }
                        matcher.setValues(values);
                        if (size.getName() != null && !size.getName().isEmpty()) {
                            matcher.setName(size.getName());
                        } else {
                            matcher.setName("size_matcher");
                        }
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
                        // 设置名称，优先使用matcher自带的name
                        if (word.getName() != null && !word.getName().isEmpty()) {
                            matcher.setName(word.getName());
                        } else {
                            matcher.setName("word_matcher");
                        }
                    }
                    break;
                case "regex":
                    matcher.setType(MatcherType.REGEX);
                    if (templateMatcher instanceof NucleiYamlObj.Regex) {
                        NucleiYamlObj.Regex regex = (NucleiYamlObj.Regex) templateMatcher;
                        matcher.setValues(regex.getRegex());
                        matcher.setPart(regex.getPart());
                        matcher.setGroup(regex.getGroup());
                        matcher.setInternal(String.valueOf(regex.isInternal()));
                        // 设置名称，优先使用matcher自带的name
                        if (regex.getName() != null && !regex.getName().isEmpty()) {
                            matcher.setName(regex.getName());
                        } else {
                            matcher.setName("regex_matcher");
                        }
                    }
                    break;
                case "binary":
                    matcher.setType(MatcherType.BINARY);
                    if (templateMatcher instanceof NucleiYamlObj.Binary) {
                        NucleiYamlObj.Binary binary = (NucleiYamlObj.Binary) templateMatcher;
                        matcher.setValues(binary.getBinary());
                        matcher.setPart(binary.getPart());
                        // 设置名称，优先使用matcher自带的name
                        if (binary.getName() != null && !binary.getName().isEmpty()) {
                            matcher.setName(binary.getName());
                        } else {
                            matcher.setName("binary_matcher");
                        }
                    }
                    break;
                case "dsl":
                    matcher.setType(MatcherType.DSL);
                    if (templateMatcher instanceof NucleiYamlObj.Dsl) {
                        NucleiYamlObj.Dsl dsl = (NucleiYamlObj.Dsl) templateMatcher;
                        matcher.setValues(dsl.getDsl());
                        
                        // 验证DSL表达式
                        if (dsl.getDsl() != null) {
                            for (String dslExpr : dsl.getDsl()) {
                                try {
                                    DslEvaluatorRefactored.validateDslSyntax(dslExpr);
                                } catch (IllegalArgumentException e) {
                                    System.err.println("警告: Nuclei DSL表达式验证失败 - " + e.getMessage());
                                    logUnrecognizedExpression(e.getMessage());
                                    // 继续执行，但记录警告
                                }
                            }
                        }
                        
                        // 设置名称，优先使用matcher自带的name
                        if (dsl.getName() != null && !dsl.getName().isEmpty()) {
                            matcher.setName(dsl.getName());
                        } else {
                            matcher.setName("dsl_matcher");
                        }
                    }
                    break;
                case "time":
                    matcher.setType(MatcherType.TIME);
                    if (templateMatcher instanceof NucleiYamlObj.Time) {
                        NucleiYamlObj.Time time = (NucleiYamlObj.Time) templateMatcher;
                        matcher.setValues(time.getTime());
                        matcher.setOperation(PocObj.OperationType.GREATER_EQUAL);
                        matcher.setPart("response_time");
                        matcher.setTimeUnit("s");
                        if (time.getName() != null && !time.getName().isEmpty()) {
                            matcher.setName(time.getName());
                        } else {
                            matcher.setName("time_matcher");
                        }
                    }
                    break;
                case "xpath":
                    matcher.setType(MatcherType.XPATH);
                    if (templateMatcher instanceof NucleiYamlObj.Xpath) {
                        NucleiYamlObj.Xpath xpath = (NucleiYamlObj.Xpath) templateMatcher;
                        matcher.setValues(xpath.getXpath());
                        matcher.setPart(xpath.getPart());
                        matcher.setAttribute(xpath.getAttribute());
                        matcher.setNegative(xpath.isNegative());
                        // 设置名称，优先使用matcher自带的name
                        if (xpath.getName() != null && !xpath.getName().isEmpty()) {
                            matcher.setName(xpath.getName());
                        } else {
                            matcher.setName("xpath_matcher");
                        }
                    }
                    break;
                case "json":
                    matcher.setType(MatcherType.JSON);
                    if (templateMatcher instanceof NucleiYamlObj.Json) {
                        NucleiYamlObj.Json json = (NucleiYamlObj.Json) templateMatcher;
                        matcher.setValues(json.getJson());
                        matcher.setPart(json.getPart());
                        matcher.setNegative(json.isNegative());
                        // 设置名称，优先使用matcher自带的name
                        if (json.getName() != null && !json.getName().isEmpty()) {
                            matcher.setName(json.getName());
                        } else {
                            matcher.setName("json_matcher");
                        }
                    }
                    break;
                case "kval":
                    matcher.setType(MatcherType.KVAL);
                    if (templateMatcher instanceof NucleiYamlObj.Kval) {
                        NucleiYamlObj.Kval kval = (NucleiYamlObj.Kval) templateMatcher;
                        matcher.setValues(kval.getKval());
                        matcher.setPart(kval.getPart());
                        matcher.setNegative(kval.isNegative());
                        // 设置名称，优先使用matcher自带的name
                        if (kval.getName() != null && !kval.getName().isEmpty()) {
                            matcher.setName(kval.getName());
                        } else {
                            matcher.setName("kval_matcher");
                        }
                    }
                    break;
                default:
                    matcher.setType(MatcherType.UNKNOWN);
                    matcher.setName("unknown_matcher");
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
     * 处理提取器
     * @param extractors 提取器列表
     * @param step POC步骤
     */
    private void processExtractors(List<NucleiYamlObj.TemplateMatcher> extractors, PocObj.PocStep step) {
        if (extractors == null || extractors.isEmpty()) {
            return;
        }
        
        List<PocObj.Matcher> extractorList = new ArrayList<>();
        for (NucleiYamlObj.TemplateMatcher extractor : extractors) {
            try {
                PocObj.Matcher matcher = new PocObj.Matcher();
                
                // 处理不同类型的提取器
                if (extractor instanceof NucleiYamlObj.Regex) {
                    NucleiYamlObj.Regex regex = (NucleiYamlObj.Regex) extractor;
                    if (regex.getRegex() != null && !regex.getRegex().isEmpty()) {
                        matcher.setType(PocObj.MatcherType.REGEX);
                        matcher.setPart(regex.getPart());
                        matcher.setValues(regex.getRegex());
                        matcher.setOperation(PocObj.OperationType.REGEX_MATCH);
                        matcher.setGroup(regex.getGroup());
                        matcher.setInternal(String.valueOf(regex.isInternal()));
                        
                        // 设置名称，优先使用extractor自带的name
                        if (regex.getName() != null && !regex.getName().isEmpty()) {
                            matcher.setName(regex.getName());
                        } else {
                            matcher.setName("regex_" + extractorList.size());
                        }
                        
                        extractorList.add(matcher);
                    }
                } else if (extractor instanceof NucleiYamlObj.Json) {
                    NucleiYamlObj.Json json = (NucleiYamlObj.Json) extractor;
                    if (json.getJson() != null && !json.getJson().isEmpty()) {
                        matcher.setType(PocObj.MatcherType.JSON);
                        matcher.setPart(json.getPart());
                        matcher.setValues(json.getJson());
                        matcher.setNegative(json.isNegative());
                        matcher.setOperation(PocObj.OperationType.DEFAULT);

                        // 设置名称，优先使用extractor自带的name
                        if (json.getName() != null && !json.getName().isEmpty()) {
                            matcher.setName(json.getName());
                        } else {
                            matcher.setName("json_" + extractorList.size());
                        }

                        extractorList.add(matcher);
                    }
                } else if (extractor instanceof NucleiYamlObj.Xpath) {
                    NucleiYamlObj.Xpath xpath = (NucleiYamlObj.Xpath) extractor;
                    if (xpath.getXpath() != null && !xpath.getXpath().isEmpty()) {
                        matcher.setType(PocObj.MatcherType.XPATH);
                        matcher.setPart(xpath.getPart());
                        matcher.setValues(xpath.getXpath());
                        matcher.setAttribute(xpath.getAttribute());
                        matcher.setNegative(xpath.isNegative());
                        matcher.setOperation(PocObj.OperationType.DEFAULT);

                        // 设置名称，优先使用extractor自带的name
                        if (xpath.getName() != null && !xpath.getName().isEmpty()) {
                            matcher.setName(xpath.getName());
                        } else {
                            matcher.setName("xpath_" + extractorList.size());
                        }

                        extractorList.add(matcher);
                    }
                } else if (extractor instanceof NucleiYamlObj.Dsl) {
                    NucleiYamlObj.Dsl dsl = (NucleiYamlObj.Dsl) extractor;
                    if (dsl.getDsl() != null && !dsl.getDsl().isEmpty()) {
                        matcher.setType(PocObj.MatcherType.DSL);
                        matcher.setValues(dsl.getDsl());
                        
                        // 验证DSL表达式
                        for (String dslExpr : dsl.getDsl()) {
                            try {
                                DslEvaluatorRefactored.validateDslSyntax(dslExpr);
                            } catch (IllegalArgumentException e) {
                                System.err.println("警告: Nuclei DSL提取器表达式验证失败 - " + e.getMessage());
                                logUnrecognizedExpression(e.getMessage());
                                // 继续执行，但记录警告
                            }
                        }
                        
                        matcher.setOperation(PocObj.OperationType.DEFAULT);
                        
                        // 设置名称，优先使用extractor自带的name
                        if (dsl.getName() != null && !dsl.getName().isEmpty()) {
                            matcher.setName(dsl.getName());
                        } else {
                            matcher.setName("dsl_" + extractorList.size());
                        }
                        
                        extractorList.add(matcher);
                    }
                } else if (extractor instanceof NucleiYamlObj.Kval) {
                    NucleiYamlObj.Kval kval = (NucleiYamlObj.Kval) extractor;
                    if (kval.getKval() != null && !kval.getKval().isEmpty()) {
                        matcher.setType(PocObj.MatcherType.KVAL);
                        matcher.setPart(kval.getPart());
                        matcher.setValues(kval.getKval());
                        matcher.setNegative(kval.isNegative());
                        matcher.setOperation(PocObj.OperationType.DEFAULT);

                        // 设置名称，优先使用extractor自带的name
                        if (kval.getName() != null && !kval.getName().isEmpty()) {
                            matcher.setName(kval.getName());
                        } else {
                            matcher.setName("kval_" + extractorList.size());
                        }

                        extractorList.add(matcher);
                    }
                } else {
                    addUnsupportedCapability("UNSUPPORTED_EXTRACTOR_TYPE", "P1", "nuclei", inferProtocolFromStep(step),
                            "extractor.type", extractor.getType(), "fallback",
                            "当前版本未实现该 extractor 类型，已忽略");
                }
            } catch (Exception e) {
                String extractorType = extractor != null ? extractor.getType() : "unknown";
                addConversionWarning("EXTRACTOR_CONVERSION_ERROR", "P1", "nuclei", inferProtocolFromStep(step),
                        "extractor", extractorType, "fallback",
                        "处理提取器时出错: " + e.getMessage());
                System.out.println("处理提取器时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        if (!extractorList.isEmpty()) {
            step.setExtractors(extractorList);
        }
    }
        
    /**
     * 处理Nuclei requests请求（旧版本格式或通用请求格式）
     * @param requests 请求列表
     * @param poc 通用POC对象
     */
    private void processRequestsField(List<NucleiYamlObj.Request> requests, PocObj.Poc poc) {
        try {
            List<PocObj.PocStep> verifySteps = new ArrayList<>();
            
            for (int i = 0; i < requests.size(); i++) {
                try {
                    NucleiYamlObj.Request request = requests.get(i);
                    
                    boolean hasRaw = request.getRaw() != null && !request.getRaw().isEmpty();
                    
                    if (hasRaw) {
                        PocObj.PocStep step = createRequestStep(request, i, null, "");
                        if (step != null) {
                            verifySteps.add(step);
                        }
                    } else if (request.getPath() != null && !request.getPath().isEmpty()) {
                        // 多路径裂变
                        for (int pIdx = 0; pIdx < request.getPath().size(); pIdx++) {
                            String path = request.getPath().get(pIdx);
                            String suffix = request.getPath().size() > 1 ? "_" + pIdx : "";
                            
                            PocObj.PocStep step = createRequestStep(request, i, path, suffix);
                            if (step != null) {
                                verifySteps.add(step);
                            }
                        }
                    } else {
                        PocObj.PocStep step = createRequestStep(request, i, null, "");
                        if (step != null) {
                            verifySteps.add(step);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("处理Request请求时出错: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // request字段通常也作为verify步骤
            if (!verifySteps.isEmpty()) {
                // 如果已经有verifySteps（比如从http字段解析的），则追加
                List<PocObj.PocStep> existingSteps = poc.getVerifySteps();
                if (existingSteps == null) {
                    poc.setVerifySteps(verifySteps);
                } else {
                    existingSteps.addAll(verifySteps);
                }
            }
        } catch (Exception e) {
            System.out.println("处理Request请求列表时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建Request步骤
     * @param request Request对象
     * @param index 索引
     * @param specificPath 指定路径
     * @param idSuffix ID后缀
     * @return PocStep对象
     */
    private PocObj.PocStep createRequestStep(NucleiYamlObj.Request request, int index, String specificPath, String idSuffix) {
        PocObj.PocStep step = new PocObj.PocStep();
        
        // 设置步骤ID
        step.setStepId("request_" + index + idSuffix);
        
        // 检查是否使用raw格式
        boolean hasRaw = request.getRaw() != null && !request.getRaw().isEmpty();
        
        if (hasRaw) {
            step.setRaw(request.getRaw());
            step.setHeaders(null);
        } else {
            step.setMethod(request.getMethod());
            
            if (specificPath != null) {
                step.setPath(specificPath);
            } else if (request.getPath() != null && !request.getPath().isEmpty()) {
                step.setPath(request.getPath().get(0));
            }
            
            if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
                step.setHeaders(request.getHeaders());
            }
        }
        
        step.setFollowRedirect(request.isRedirects());
        step.setUnsafe(request.isUnsafe());
        step.setDisableCookie(request.isDisable_cookie());
        step.setDisablePathAutomerge(request.isDisable_path_automerge());
        
        processMatchers(request.getMatchers(), request.getMatchers_condition(), step);
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
                    transformPayloadMap(http.getPayloads(), payloads);
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
                    transformPayloadMap(request.getPayloads(), payloads);
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
                    transformPayloadMap(tcp.getPayloads(), payloads);
                }
                if (tcp.getAttack() != null) {
                    poc.setVariablesType(tcp.getAttack());
                }
            }
        }

        // 处理Network请求中的payloads（与TCP结构兼容）
        if (nucleiPoc.getNetwork() != null && !nucleiPoc.getNetwork().isEmpty()) {
            for (NucleiYamlObj.Tcp network : nucleiPoc.getNetwork()) {
                if (network.getPayloads() != null && !network.getPayloads().isEmpty()) {
                    transformPayloadMap(network.getPayloads(), payloads);
                }
                if (network.getAttack() != null) {
                    poc.setVariablesType(network.getAttack());
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
     * 处理 Nuclei DNS 请求
     * @param dnsRequests DNS 请求列表
     * @param poc 通用 POC 对象
     */
    private void processDnsRequests(List<NucleiYamlObj.Dns> dnsRequests, PocObj.Poc poc) {
        try {
            List<PocObj.PocStep> verifySteps = new ArrayList<>();
            
            for (int i = 0; i < dnsRequests.size(); i++) {
                try {
                    NucleiYamlObj.Dns dnsRequest = dnsRequests.get(i);
                    PocObj.DnsStep step = createDnsStep(dnsRequest, i);
                    if (step != null) {
                        verifySteps.add(step);
                    }
                } catch (Exception e) {
                    System.out.println("处理 DNS 请求时出错: " + e.getMessage());
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
            System.out.println("处理 DNS 请求列表时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建 DNS 步骤
     * @param dnsRequest DNS 请求对象
     * @param index 索引
     * @return DnsStep 对象
     */
    private PocObj.DnsStep createDnsStep(NucleiYamlObj.Dns dnsRequest, int index) {
        PocObj.DnsStep step = new PocObj.DnsStep();

        // 设置步骤 ID
        step.setStepId("dns_" + index);

        // 设置域名（name现在是单个字符串，不是列表）
        if (dnsRequest.getName() != null && !dnsRequest.getName().isEmpty()) {
            step.setDomain(dnsRequest.getName());
        }

        // 设置 DNS 记录类型
        step.setType(dnsRequest.getType());

        // 设置自定义解析器
        if (dnsRequest.getResolvers() != null && !dnsRequest.getResolvers().isEmpty()) {
            step.setResolver(dnsRequest.getResolvers());
        }

        // 处理匹配器
        processMatchers(dnsRequest.getMatchers(), dnsRequest.getMatchers_condition(), step);

        // 处理提取器
        processExtractors(dnsRequest.getExtractors(), step);

        return step;
    }
    
    /**
     * 处理 Nuclei WebSocket 请求
     * @param websocketRequests WebSocket 请求列表
     * @param poc 通用 POC 对象
     */
    private void processWebSocketRequests(List<NucleiYamlObj.WebSocket> websocketRequests, PocObj.Poc poc) {
        try {
            List<PocObj.PocStep> verifySteps = new ArrayList<>();
            
            for (int i = 0; i < websocketRequests.size(); i++) {
                try {
                    NucleiYamlObj.WebSocket wsRequest = websocketRequests.get(i);
                    PocObj.WebSocketStep step = createWebSocketStep(wsRequest, i);
                    if (step != null) {
                        verifySteps.add(step);
                    }
                } catch (Exception e) {
                    System.out.println("处理 WebSocket 请求时出错: " + e.getMessage());
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
            System.out.println("处理 WebSocket 请求列表时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建 WebSocket 步骤
     * @param wsRequest WebSocket 请求对象
     * @param index 索引
     * @return WebSocketStep 对象
     */
    private PocObj.WebSocketStep createWebSocketStep(NucleiYamlObj.WebSocket wsRequest, int index) {
        PocObj.WebSocketStep step = new PocObj.WebSocketStep();
        
        // 设置步骤 ID
        step.setStepId("websocket_" + index);
        
        // 设置 WebSocket 地址
        step.setAddress(wsRequest.getAddress());
        
        // 设置请求头
        if (wsRequest.getHeaders() != null) {
            step.setHeaders(wsRequest.getHeaders());
        }
        
        // 处理输入消息
        if (wsRequest.getInputs() != null && !wsRequest.getInputs().isEmpty()) {
            List<String> messages = new ArrayList<>();
            for (NucleiYamlObj.WebSocketInput input : wsRequest.getInputs()) {
                if (input.getData() != null) {
                    messages.add(input.getData());
                }
            }
            step.setMessages(messages);
        }
        
        // 处理匹配器
        processMatchers(wsRequest.getMatchers(), wsRequest.getMatchers_condition(), step);
        
        // 处理提取器
        processExtractors(wsRequest.getExtractors(), step);
        
        return step;
    }
    
    /**
     * 处理 Nuclei SSL/TLS 请求
     * @param sslRequests SSL 请求列表
     * @param poc 通用 POC 对象
     */
    private void processSslRequests(List<NucleiYamlObj.Ssl> sslRequests, PocObj.Poc poc) {
        try {
            List<PocObj.PocStep> verifySteps = new ArrayList<>();
            
            for (int i = 0; i < sslRequests.size(); i++) {
                try {
                    NucleiYamlObj.Ssl sslRequest = sslRequests.get(i);
                    PocObj.SslStep step = createSslStep(sslRequest, i);
                    if (step != null) {
                        verifySteps.add(step);
                    }
                } catch (Exception e) {
                    System.out.println("处理 SSL 请求时出错: " + e.getMessage());
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
            System.out.println("处理 SSL 请求列表时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建 SSL 步骤
     * @param sslRequest SSL 请求对象
     * @param index 索引
     * @return SslStep 对象
     */
    private PocObj.SslStep createSslStep(NucleiYamlObj.Ssl sslRequest, int index) {
        PocObj.SslStep step = new PocObj.SslStep();

        // 设置步骤 ID
        step.setStepId("ssl_" + index);

        // 设置目标地址
        step.setAddress(sslRequest.getAddress());

        // 设置默认超时时间（10秒），避免 0 超时
        step.setTimeout(15000);

        // 处理匹配器
        processMatchers(sslRequest.getMatchers(), sslRequest.getMatchers_condition(), step);

        // 处理提取器
        processExtractors(sslRequest.getExtractors(), step);

        return step;
    }
    
    /**
     * 处理 Nuclei File 请求
     * @param fileRequests File 请求列表
     * @param poc 通用 POC 对象
     */
    private void processFileRequests(List<NucleiYamlObj.FileProtocol> fileRequests, PocObj.Poc poc) {
        try {
            List<PocObj.PocStep> verifySteps = new ArrayList<>();
            
            for (int i = 0; i < fileRequests.size(); i++) {
                try {
                    NucleiYamlObj.FileProtocol fileRequest = fileRequests.get(i);
                    PocObj.FileStep step = createFileStep(fileRequest, i);
                    if (step != null) {
                        verifySteps.add(step);
                    }
                } catch (Exception e) {
                    System.out.println("处理 File 请求时出错: " + e.getMessage());
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
            System.out.println("处理 File 请求列表时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建 File 步骤
     * @param fileRequest File 请求对象
     * @param index 索引
     * @return FileStep 对象
     */
    private PocObj.FileStep createFileStep(NucleiYamlObj.FileProtocol fileRequest, int index) {
        PocObj.FileStep step = new PocObj.FileStep();
        
        // 设置步骤 ID
        step.setStepId("file_" + index);
        
        // 设置路径
        if (fileRequest.getPaths() != null) {
            step.setPaths(fileRequest.getPaths());
        }
        
        // 设置扩展名
        if (fileRequest.getExtensions() != null) {
            step.setExtensions(fileRequest.getExtensions());
        }
        
        // 设置递归和大小限制
        step.setRecursive(fileRequest.isRecursive());
        step.setMaxSize(fileRequest.getMax_size());
        
        // 处理匹配器
        processMatchers(fileRequest.getMatchers(), fileRequest.getMatchers_condition(), step);
        
        // 处理提取器
        processExtractors(fileRequest.getExtractors(), step);
        
        return step;
    }
    
    /**
     * 处理 Nuclei Headless 请求
     * @param headlessRequests Headless 请求列表
     * @param poc 通用 POC 对象
     */
    private void processHeadlessRequests(List<NucleiYamlObj.Headless> headlessRequests, PocObj.Poc poc) {
        try {
            List<PocObj.PocStep> verifySteps = new ArrayList<>();
            
            for (int i = 0; i < headlessRequests.size(); i++) {
                try {
                    NucleiYamlObj.Headless headlessRequest = headlessRequests.get(i);
                    PocObj.HeadlessStep step = createHeadlessStep(headlessRequest, i);
                    if (step != null) {
                        verifySteps.add(step);
                    }
                } catch (Exception e) {
                    System.out.println("处理 Headless 请求时出错: " + e.getMessage());
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
            System.out.println("处理 Headless 请求列表时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建 Headless 步骤
     * @param headlessRequest Headless 请求对象
     * @param index 索引
     * @return HeadlessStep 对象
     */
    private PocObj.HeadlessStep createHeadlessStep(NucleiYamlObj.Headless headlessRequest, int index) {
        PocObj.HeadlessStep step = new PocObj.HeadlessStep();
        
        // 设置步骤 ID
        step.setStepId("headless_" + index);
        
        // 转换操作步骤
        if (headlessRequest.getSteps() != null && !headlessRequest.getSteps().isEmpty()) {
            List<PocObj.BrowserAction> actions = new ArrayList<>();
            
            // 提取 URL（从第一个 navigate 操作）
            for (NucleiYamlObj.HeadlessStep hlStep : headlessRequest.getSteps()) {
                if ("navigate".equals(hlStep.getAction()) && hlStep.getArgs() != null) {
                    String url = hlStep.getArgs().get("url");
                    if (url != null) {
                        step.setUrl(url);
                        break;
                    }
                }
            }
            
            // 转换所有操作
            for (NucleiYamlObj.HeadlessStep hlStep : headlessRequest.getSteps()) {
                PocObj.BrowserAction action = new PocObj.BrowserAction();
                action.setAction(hlStep.getAction());
                action.setArgs(hlStep.getArgs());
                actions.add(action);
            }
            
            step.setActions(actions);
        }
        
        // 处理匹配器
        processMatchers(headlessRequest.getMatchers(), headlessRequest.getMatchers_condition(), step);
        
        // 处理提取器
        processExtractors(headlessRequest.getExtractors(), step);
        
        return step;
    }
    
    /**
     * 处理 Nuclei Code 请求
     * @param codeRequests Code 请求列表
     * @param poc 通用 POC 对象
     */
    private void processCodeRequests(List<NucleiYamlObj.Code> codeRequests, PocObj.Poc poc) {
        try {
            List<PocObj.PocStep> verifySteps = new ArrayList<>();
            
            for (int i = 0; i < codeRequests.size(); i++) {
                try {
                    NucleiYamlObj.Code codeRequest = codeRequests.get(i);
                    PocObj.CodeStep step = createCodeStep(codeRequest, i);
                    if (step != null) {
                        verifySteps.add(step);
                    }
                } catch (Exception e) {
                    System.out.println("处理 Code 请求时出错: " + e.getMessage());
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
            System.out.println("处理 Code 请求列表时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建 Code 步骤
     * @param codeRequest Code 请求对象
     * @param index 索引
     * @return CodeStep 对象
     */
    private PocObj.CodeStep createCodeStep(NucleiYamlObj.Code codeRequest, int index) {
        PocObj.CodeStep step = new PocObj.CodeStep();

        // 设置步骤 ID
        step.setStepId("code_" + index);

        // 设置代码引擎（默认 JavaScript）
        if (codeRequest.getEngine() != null && !codeRequest.getEngine().isEmpty()) {
            step.setEngine(codeRequest.getEngine().get(0));
        } else {
            step.setEngine("javascript");
        }

        step.setSource(codeRequest.getSource());

        // 处理匹配器
        processMatchers(codeRequest.getMatchers(), codeRequest.getMatchers_condition(), step);

        // 处理提取器
        processExtractors(codeRequest.getExtractors(), step);

        return step;
    }

    private void addConversionWarning(String code, String level, String format, String protocol,
                                      String field, Object value, String action, String message) {
        Map<String, Object> warning = new HashMap<>();
        warning.put("code", code);
        warning.put("level", level);
        warning.put("format", format);
        warning.put("protocol", protocol);
        warning.put("field", field);
        warning.put("value", value == null ? "" : String.valueOf(value));
        warning.put("action", action);
        warning.put("message", message);
        conversionWarnings.add(warning);
    }

    private void addUnsupportedCapability(String code, String level, String format, String protocol,
                                          String field, Object value, String action, String message) {
        Map<String, Object> capability = new HashMap<>();
        capability.put("code", code);
        capability.put("level", level);
        capability.put("format", format);
        capability.put("protocol", protocol);
        capability.put("field", field);
        capability.put("value", value == null ? "" : String.valueOf(value));
        capability.put("action", action);
        capability.put("message", message);
        unsupportedCapabilities.add(capability);
    }

    private String inferProtocolFromStep(PocObj.PocStep step) {
        if (step == null || step.getStepId() == null) {
            return "unknown";
        }
        String stepId = step.getStepId().toLowerCase();
        int idx = stepId.indexOf('_');
        return idx > 0 ? stepId.substring(0, idx) : stepId;
    }

    private boolean isSimpleFlowExpression(String flow) {
        if (flow == null || flow.trim().isEmpty()) {
            return true;
        }
        // 与 PocExecutor 保持一致：支持 http_1/http(1) 且仅包含 &&/|| 组合的简单表达式
        String normalized = flow.trim();
        return normalized.matches("(?i)\\s*[a-z]+_?\\d+\\s*([&]{2}|[|]{2})\\s*[a-z]+_?\\d+(\\s*([&]{2}|[|]{2})\\s*[a-z]+_?\\d+)*\\s*")
                || normalized.matches("(?i)\\s*[a-z]+\\(\\d+\\)\\s*([&]{2}|[|]{2})\\s*[a-z]+\\(\\d+\\)(\\s*([&]{2}|[|]{2})\\s*[a-z]+\\(\\d+\\))*\\s*");
    }
}
