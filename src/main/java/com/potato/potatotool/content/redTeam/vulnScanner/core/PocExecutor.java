package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.exception.NetworkException;
import com.potato.potatotool.content.redTeam.vulnScanner.exception.ScanExecutionException;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.VariableExtractor;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.XrayCelEvaluator;
import com.potato.potatotool.content.redTeam.vulnScanner.http.*;
import com.potato.potatotool.content.redTeam.vulnScanner.matchers.ResponseMatcher;
import com.potato.potatotool.content.redTeam.vulnScanner.matchers.SimpleMatcher;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.model.StepExecutionRecord;
import com.potato.potatotool.content.redTeam.vulnScanner.config.VulnScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.config.HeaderManager;
import com.potato.potatotool.content.redTeam.vulnScanner.util.ConnectionPoolManager;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PayloadCombiner;
import com.potato.potatotool.content.redTeam.vulnScanner.util.RuntimeExpressionEvaluator;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.network.RequestUtils.requests;

/**
 * POC执行器
 * 负责执行单个POC对目标的扫描
 * 
 * @author Potato
 * @date 2024-10-28
 */
public class PocExecutor {

    private final ScanConfig scanConfig;
    private final ConnectionPoolManager connectionPool;
    private final ResponseCache responseCache;

    // 用于存储最后匹配的请求/响应数据（报告生成用）
    private ThreadLocal<RequestObj> lastMatchedRequest = new ThreadLocal<>();
    private ThreadLocal<CustomHttpResponse> lastMatchedResponse = new ThreadLocal<>();
    private ThreadLocal<String> lastMatchedPath = new ThreadLocal<>();
    private ThreadLocal<String> lastMatchedPayload = new ThreadLocal<>();

    // 用于存储步骤执行记录和提取的数据（报告生成用）
    private ThreadLocal<List<StepExecutionRecord>> stepExecutionRecords = ThreadLocal.withInitial(ArrayList::new);
    private ThreadLocal<Map<String, Object>> extractedOutputData = ThreadLocal.withInitial(HashMap::new);
    private ThreadLocal<Map<String, String>> usedVariableValues = ThreadLocal.withInitial(HashMap::new);
    private ThreadLocal<List<String>> usedParamKeys = ThreadLocal.withInitial(ArrayList::new);

    public PocExecutor(ScanConfig scanConfig) {
        this.scanConfig = scanConfig != null ? scanConfig : new ScanConfig();
        this.connectionPool = ConnectionPoolManager.getInstance();
        this.responseCache = new ResponseCache();
    }
    
    /**
     * 执行POC扫描
     * 
     * @param target 目标URL
     * @param poc POC对象
     * @return 扫描结果
     * @throws ScanExecutionException 当扫描执行失败时
     * @throws NetworkException 当网络请求失败时
     */
    public ScanResult execute(String target, PocObj.Poc poc) 
            throws ScanExecutionException, NetworkException {
        
        long startTime = System.currentTimeMillis();
        
        // 尝试获取连接
        if (!connectionPool.acquireConnection(target)) {
            throw new NetworkException("连接池已满，无法获取连接");
        }
        
        try {
            // 执行验证步骤
            if (poc.getVerifySteps() != null && !poc.getVerifySteps().isEmpty()) {
                boolean isVulnerable = executeSteps(target, poc);
                
                if (isVulnerable) {
                    // 创建扫描结果
                    return createVulnerableResult(target, poc, startTime);
                } else {
                    // 创建未发现漏洞的结果
                    return createNegativeResult(target, poc, startTime);
                }
            }
            
            return createNegativeResult(target, poc, startTime);
            
        } catch (NetworkException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScanExecutionException(target, poc.getId(), "扫描被中断", e);
        } catch (Exception e) {
            throw new ScanExecutionException(target, poc.getId(), "扫描执行异常: " + e.getMessage(), e);
        } finally {
            // 释放连接
            connectionPool.releaseConnection(target);
        }
    }
    
    /**
     * 执行POC检测步骤
     */
    private boolean executeSteps(String target, PocObj.Poc poc) throws Exception {
        if (target == null || poc == null || poc.getVerifySteps() == null || poc.getVerifySteps().isEmpty()) {
            return false;
        }

        List<PocObj.PocStep> steps = poc.getVerifySteps();
        PocObj.GlobalConfig globalConfig = poc.getGlobalConfig();

        // 从POC对象中获取变量
        Map<String, List<String>> pocVariables = poc.getVariables();

        // 加载配置文件中定义的字典变量（{{user}}, {{pass}} 等）
        Map<String, List<String>> dictionaryVariables = VulnScanConfig.getInstance().loadAllDictionaries();

        // 如果是 Nuclei POC，注入内置变量（Helper Variables）
        if ("nuclei".equals(poc.getOriginalFormat())) {
            Map<String, List<String>> allVariables = new HashMap<>();

            // 首先添加内置变量
            Map<String, List<String>> helperVariables = extractHelperVariables(target);
            if (helperVariables != null) {
                allVariables.putAll(helperVariables);
            }

            // 然后合并字典变量（{{user}}, {{pass}} 等）
            if (dictionaryVariables != null && !dictionaryVariables.isEmpty()) {
                allVariables.putAll(dictionaryVariables);
            }

            // 然后合并 POC 中定义的 payloads/variables（POC 定义的变量会覆盖内置变量和字典变量）
            if (pocVariables != null) {
                allVariables.putAll(pocVariables);
            }

            // 使用合并后的变量
            pocVariables = allVariables;

            // 预计算 variables 中的 DSL 表达式
            pocVariables = evaluateDslVariables(pocVariables, target);
        } else {
            // 非 Nuclei POC 也支持字典变量
            if (dictionaryVariables != null && !dictionaryVariables.isEmpty()) {
                Map<String, List<String>> mergedVariables = new HashMap<>(dictionaryVariables);

                // POC 定义的变量会覆盖字典变量
                if (pocVariables != null) {
                    mergedVariables.putAll(pocVariables);
                }

                pocVariables = mergedVariables;
            }
        }


        // 检测 POC 实际使用的变量（完整变量检测，包括字典变量和 POC 自定义变量）
        Set<String> usedVariables = detectUsedVariables(steps);
        
        // 确保运行时表达式变量不被过滤（它们可能被其他变量依赖）
        if (pocVariables != null) {
            for (Map.Entry<String, List<String>> entry : pocVariables.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    String val = entry.getValue().get(0);
                    if (RuntimeExpressionEvaluator.shouldPreserveVariable(val)) {
                        usedVariables.add(entry.getKey());
                    }
                }
            }
        }

        // 过滤未使用的变量（减少不必要的 payload 组合）
        if (pocVariables != null && !pocVariables.isEmpty()) {
            pocVariables = filterUnusedVariables(pocVariables, usedVariables);
        }
        // ���查是否有多值变量（需要变量组合）
        boolean hasMultipleValues = false;
        if (pocVariables != null && !pocVariables.isEmpty()) {
            for (List<String> values : pocVariables.values()) {
                if (values != null && values.size() > 1) {
                    hasMultipleValues = true;
                    break;
                }
            }
        }

        // 如果没有多值变量，直接取第一个值执行
        if (!hasMultipleValues) {
            // 使用工具类统一处理运行时表达式评估
            Map<String, Object> extractedValues = RuntimeExpressionEvaluator.evaluateVariables(pocVariables);
            // 传递步骤条件（OR/AND）
            return executeStepsWithVariables(target, steps, globalConfig, extractedValues, poc.getStepsCondition());
        }

        // 根据 variablesType 生成变量组合
        PocObj.VariablesType variablesType = poc.getVariablesType();
        List<Map<String, String>> combinations;

        switch (variablesType) {
            case batteringram:
                // 同步模式：所有变量使用同一个索引的值（循环）
                combinations = PayloadCombiner.generateBatteringRamCombinations(pocVariables);
                System.out.println("变量组合模式: batteringram (同步模式) - 共 " + combinations.size() + " 个组合");
                break;

            case pitchfork:
                // 配对模式：多个变量列表索引严格配对
                combinations = PayloadCombiner.generatePitchforkCombinations(pocVariables);
                System.out.println("变量组合模式: pitchfork (配对模式) - 共 " + combinations.size() + " 个组合");
                break;

            case clusterbomb:
                // 笛卡尔积模式：生成所有可能组合
                // 从配置读取最大组合数限制
                final int MAX_COMBINATIONS = VulnScanConfig.getInstance().getMaxPayloadCombinations();
                long totalCombinations = PayloadCombiner.countCombinations(pocVariables);

                if (totalCombinations > MAX_COMBINATIONS) {
                    System.err.println("警告: Payload 组合数过多 (" + totalCombinations +
                            ")，已��制为 " + MAX_COMBINATIONS + " 个（可在配置中调整 maxPayloadCombinations）");
                }

                combinations = PayloadCombiner.generateCombinationsWithLimit(pocVariables, MAX_COMBINATIONS);
                System.out.println("变量组合模式: clusterbomb (笛卡尔积) - 共 " + combinations.size() + " 个组合");

                // 如果组合数超过 100，输出详细信息供排查
                if (combinations.size() > 100) {
                    System.err.println("========================================");
                    System.err.println("警告: POC [" + poc.getId() + "] 产生了 " + combinations.size() + " 个 payload 组合");
                    System.err.println("以下是前 100 个组合的详细内容：");
                    System.err.println("========================================");

                    int displayCount = Math.min(100, combinations.size());
                    for (int i = 0; i < displayCount; i++) {
                        Map<String, String> combination = combinations.get(i);
                        System.err.println("组合 #" + (i + 1) + ":");
                        for (Map.Entry<String, String> entry : combination.entrySet()) {
                            System.err.println("  " + entry.getKey() + ": " + entry.getValue());
                        }
                    }
                    System.err.println("========================================");
                }
                break;

            default:
                // 默认使用 batteringram
                combinations = PayloadCombiner.generateBatteringRamCombinations(pocVariables);
                System.out.println("变量组合模式: batteringram (默认) - 共 " + combinations.size() + " 个组合");
                break;
        }

        // 逐个测试组合
        boolean anySuccess = false; // 记录是否有任何组合成功
        boolean continueOnMatch = poc.isContinueOnMatch(); // Xray continue_ 字段

        for (int i = 0; i < combinations.size(); i++) {
            Map<String, String> combination = combinations.get(i);
            
            // 转换组合变量为 Object Map 并处理表达式
            Map<String, Object> objectCombination = new HashMap<>();
            for (Map.Entry<String, String> entry : combination.entrySet()) {
                String val = entry.getValue();
                if (val != null && val.startsWith("@@expression:")) {
                    String expr = val.substring("@@expression:".length());
                    Object evalResult = XrayCelEvaluator.evaluateForValue(expr, null, null);
                    objectCombination.put(entry.getKey(), evalResult != null ? evalResult : val);
                } else {
                    objectCombination.put(entry.getKey(), val);
                }
            }

            try {
                // 使用当前组合执行所有步骤
                boolean success = executeStepsWithVariables(target, steps, globalConfig, objectCombination);

                if (success) {
                    anySuccess = true;

                    // 如果 continueOnMatch=false（默认），则第一次成功后立即返回
                    if (!continueOnMatch) {
                        return true;
                    }
                    // 否则继续执行其他组合
                }
            } catch (Exception e) {
                // 记录错误但继续尝试其他组合（静默处理）
            }
        }

        return anySuccess;
    }
    
    /**
     * 使用指定变量执行步骤（AND 条件）
     */
    private boolean executeStepsWithVariables(
        String target,
        List<PocObj.PocStep> steps,
        PocObj.GlobalConfig globalConfig,
        Map<String, Object> initialVariables
    ) throws Exception {
        return executeStepsWithVariables(target, steps, globalConfig, initialVariables, PocObj.MatchersCondition.AND);
    }
    
    /**
     * 使用指定变量执行步骤
     * 
     * @param target 目标URL
     * @param steps 步骤列表
     * @param globalConfig 全局配置
     * @param initialVariables 初始变量映射
     * @param stepsCondition 步骤间的条件（AND: 所有步骤都要通过，OR: 任一步骤通过即可）
     * @return 是否成功
     * @throws Exception 执行异常
     */
    private boolean executeStepsWithVariables(
        String target,
        List<PocObj.PocStep> steps,
        PocObj.GlobalConfig globalConfig,
        Map<String, Object> initialVariables,
        PocObj.MatchersCondition stepsCondition
    ) throws Exception {
        // 使用提供的初始变量
        Map<String, Object> extractedValues = new HashMap<>(initialVariables);
        
        // OR 条件：任一步骤匹配即可
        boolean isOrCondition = (stepsCondition == PocObj.MatchersCondition.OR);
        
        // 对于 OR 条件，单独处理每个步骤
        if (isOrCondition) {
            for (PocObj.PocStep step : steps) {
                if (step == null) continue;
                try {
                    boolean stepResult = executeSingleStep(target, step, globalConfig, extractedValues, steps);
                    if (stepResult) {
                        return true; // OR 条件：任一步骤成功就返回 true
                    }
                } catch (Exception e) {
                    // OR 条件下，单步失败继续尝试下一步
                    System.err.println("步骤执行异常（OR 条件继续）: " + e.getMessage());
                }
            }
            return false; // OR 条件：所有步骤都失败返回 false
        }
        
        // AND 条件：原有逻辑（所有步骤都必须成功）
        for (PocObj.PocStep step : steps) {
            if (step == null) {
                continue; // 跳过空步骤
            }
            
            try {
                // 根据步骤类型选择执行方式
                if (step instanceof PocObj.DnsStep) {
                    // DNS 协议执行
                    if (!executeDnsStep((PocObj.DnsStep) step, extractedValues)) {
                        return false;
                    }
                    
                } else if (step instanceof PocObj.WebSocketStep) {
                    // WebSocket 协议执行
                    if (!executeWebSocketStep((PocObj.WebSocketStep) step, extractedValues)) {
                        return false;
                    }
                    
                } else if (step instanceof PocObj.SslStep) {
                    // SSL/TLS 协议执行
                    if (!executeSslStep((PocObj.SslStep) step, extractedValues)) {
                        return false;
                    }
                    
                } else if (step instanceof PocObj.FileStep) {
                    // File 协议执行
                    if (!executeFileStep((PocObj.FileStep) step, extractedValues)) {
                        return false;
                    }
                    
                } else if (step instanceof PocObj.HeadlessStep) {
                    // Headless 协议执行
                    if (!executeHeadlessStep((PocObj.HeadlessStep) step, extractedValues)) {
                        return false;
                    }
                    
                } else if (step instanceof PocObj.TcpStep) {
                    // TCP/Socket 协议执行
                    if (!executeTcpStep((PocObj.TcpStep) step, extractedValues)) {
                        return false;
                    }
                    
                } else if (step instanceof PocObj.CodeStep) {
                    // Code 协议执行
                    if (!executeCodeStep((PocObj.CodeStep) step, extractedValues)) {
                        return false;
                    }
                    
                } else {
                    // HTTP/TCP 现有逻辑
                    // 检查是否为多块raw请求（Nuclei多请求场景）
                    if (step.getRaw() != null && step.getRaw().size() > 1) {
                        // 多块raw请求 - 依次执行所有块并收集所有响应
                        List<CustomHttpResponse> allResponses = new ArrayList<>();

                        for (int rawIndex = 0; rawIndex < step.getRaw().size(); rawIndex++) {
                            // 创建单块请求
                            PocObj.PocStep singleRawStep = createSingleRawStep(step, rawIndex);
                            RequestObj requestObj = createRequest(target, singleRawStep, globalConfig, extractedValues);

                            // 发送请求
                            long requestTime = System.currentTimeMillis();
                            CustomHttpResponse response = requests(requestObj);
                            long responseTime = System.currentTimeMillis();

                            if (response == null) {
                                System.err.println("警告: raw块 " + (rawIndex + 1) + " 请求失败");
                                continue;
                            }

                            // 设置响应时间
                            response.setResponseTime(responseTime - requestTime);

                            // 缓存响应（使用索引后缀）
                            String cacheKey = (step.getStepId() != null ? step.getStepId() : "step_" + steps.indexOf(step))
                                            + "_" + (rawIndex + 1);
                            responseCache.put(cacheKey, response, requestTime, responseTime);

                            // 收集响应
                            allResponses.add(response);

                            // 提取变量（每个raw块都可能提取变量）
                            if (step.getExtractors() != null && !step.getExtractors().isEmpty()) {
                                VariableExtractor.extractVariablesObj(response, step.getExtractors(), extractedValues);
                            }
                        }

                        // 使用最后一个响应进行匹配（matcher会从responseCache中访问所有索引响应）
                        if (!allResponses.isEmpty()) {
                            CustomHttpResponse lastResponse = allResponses.get(allResponses.size() - 1);

                            // 将所有响应添加到缓存，供DSL matcher访问索引变量（body_1, body_2等）
                            responseCache.putMultipleResponses(
                                step.getStepId() != null ? step.getStepId() : "step_" + steps.indexOf(step),
                                allResponses
                            );

                            // 匹配结果（传递stepId以支持索引变量：body_1, body_2, status_code_2等）
                            // 同时传递extractedValues以支持Xray CEL表达式中的变量求值（如 s1, s2）
                            boolean matched = ResponseMatcher.matchResponse(
                                lastResponse, step.getMatchers(), step.getMatchersCondition(), responseCache,
                                step.getStepId() != null ? step.getStepId() : "step_" + steps.indexOf(step),
                                extractedValues
                            );

                            // 如果匹配成功，保存最后一个请求/响应数据用于报告生成
                            if (matched) {
                                RequestObj lastRequestObj = createRequest(target, createSingleRawStep(step, allResponses.size() - 1),
                                                                         globalConfig, extractedValues);
                                lastMatchedRequest.set(lastRequestObj);
                                lastMatchedResponse.set(lastResponse);
                                lastMatchedPath.set(lastRequestObj.getUrl());
                                String payload = extractPayloadFromVariables(extractedValues);
                                lastMatchedPayload.set(payload);
                            }

                            if (!matched) {
                                return false;
                            }

                            // 处理 output 提取（Xray POC）
                            if (step.getOutput() != null && !step.getOutput().isEmpty()) {
                                RequestObj lastRequestObj = createRequest(target, createSingleRawStep(step, allResponses.size() - 1),
                                                                         globalConfig, extractedValues);
                                extractOutputVariables(step, lastResponse, lastRequestObj, extractedValues);
                            }
                        }

                    } else {
                        // 单块raw或普通请求
                        RequestObj requestObj = createRequest(target, step, globalConfig, extractedValues);

                        // 记录请求时间
                        long requestTime = System.currentTimeMillis();

                        // 发送请求
                        try (CustomHttpResponse response = requests(requestObj)) {
                        // 记录响应时间
                        long responseTime = System.currentTimeMillis();

                        // 检查响应是否为空
                        if (response == null) {
                            continue; // 跳过当前步骤，继续下一步
                        }

                        // 设置响应时间到 response 对象（用于时间盲注检测）
                        response.setResponseTime(responseTime - requestTime);

                        // 缓存响应（用于diff操作）
                        String cacheKey = step.getStepId() != null ? step.getStepId() : "step_" + steps.indexOf(step);
                        responseCache.put(cacheKey, response, requestTime, responseTime);

                        // 匹配结果（传入responseCache以支持diff操作，传递stepId以支持索引变量）
                        // 同时传递extractedValues以支持Xray CEL表达式中的变量求值（如 s1, s2）
                        boolean matched = ResponseMatcher.matchResponse(
                            response, step.getMatchers(), step.getMatchersCondition(), responseCache,
                            cacheKey, extractedValues
                        );

                        // ========== 记录步骤执行详情（用于增强报告） ==========
                        StepExecutionRecord record = new StepExecutionRecord(steps.indexOf(step) + 1, cacheKey);
                        record.setStepType("http");
                        record.setRequestUrl(requestObj.getUrl());
                        record.setRequestMethod(requestObj.getMethod());
                        if (requestObj.getHeaders() != null) {
                            record.setRequestHeaders(new HashMap<>(requestObj.getHeaders()));
                        }
                        if (requestObj.getPostData() != null) {
                            record.setRequestBody(new String(requestObj.getPostData(), StandardCharsets.UTF_8));
                        }
                        record.setResponseCode(response.getResponseCode());
                        // 转换响应头格式
                        if (response.getHeaderFields() != null) {
                            Map<String, String> flatHeaders = new HashMap<>();
                            for (Map.Entry<String, List<String>> entry : response.getHeaderFields().entrySet()) {
                                if (entry.getKey() != null && entry.getValue() != null) {
                                    flatHeaders.put(entry.getKey(), String.join(", ", entry.getValue()));
                                }
                            }
                            record.setResponseHeaders(flatHeaders);
                        }
                        record.setResponseBodyWithLimit(response.getTextStr());
                        record.setResponseTime(responseTime - requestTime);
                        record.setMatched(matched);
                        record.buildRawRequest();
                        record.buildRawResponse();
                        stepExecutionRecords.get().add(record);

                        // 如果匹配成功，保存请求/响应数据用于报告生成
                        if (matched) {
                            lastMatchedRequest.set(requestObj);
                            lastMatchedResponse.set(response);
                            lastMatchedPath.set(requestObj.getUrl());
                            // 提取payload（从extractedValues中获取）
                            String payload = extractPayloadFromVariables(extractedValues);
                            lastMatchedPayload.set(payload);
                        }

                        // 如果匹配失败且步骤是必要的，则返回失败
                        if (!matched) {
                            return false;
                        }

                        // 处理 output 提取（Xray POC）
                        if (step.getOutput() != null && !step.getOutput().isEmpty()) {
                            extractOutputVariables(step, response, requestObj, extractedValues);
                            // 记录提取的output数据
                            extractedOutputData.get().putAll(extractedValues);
                        }

                        // 提取变量（Nuclei POC）
                        if (step.getExtractors() != null && !step.getExtractors().isEmpty()) {
                            VariableExtractor.extractVariablesObj(response, step.getExtractors(), extractedValues);
                            // 记录提取的变量到步骤记录
                            for (PocObj.Matcher extractor : step.getExtractors()) {
                                if (extractor.getName() != null && extractedValues.containsKey(extractor.getName())) {
                                    record.addExtractedVariable(extractor.getName(), extractedValues.get(extractor.getName()));
                                }
                            }
                        }
                    }
                    }
                }
            } catch (SocketTimeoutException e) {
                throw new NetworkException(NetworkException.NetworkErrorType.READ_TIMEOUT, 
                    "请求超时: " + target, e);
            } catch (UnknownHostException e) {
                throw new NetworkException(NetworkException.NetworkErrorType.UNKNOWN_HOST, 
                    "未知主机: " + target, e);
            } catch (IOException e) {
                // 判断是否是网络相关异常
                if (isNetworkError(e)) {
                    throw new NetworkException("网络请求失败: " + e.getMessage(), e);
                }
                throw e;
            }
        }
        
        // 所有步骤执行完毕，返回成功
        return true;
    }
    
    /**
     * 执行单个步骤并返回结果
     * 用于 OR 条件的步骤执行，支持所有协议类型
     */
    private boolean executeSingleStep(String target, PocObj.PocStep step,
                                       PocObj.GlobalConfig globalConfig,
                                       Map<String, Object> extractedValues,
                                       List<PocObj.PocStep> allSteps) throws Exception {
        // 根据步骤类型选择执行方式
        if (step instanceof PocObj.DnsStep) {
            return executeDnsStep((PocObj.DnsStep) step, extractedValues);
        } else if (step instanceof PocObj.WebSocketStep) {
            return executeWebSocketStep((PocObj.WebSocketStep) step, extractedValues);
        } else if (step instanceof PocObj.SslStep) {
            return executeSslStep((PocObj.SslStep) step, extractedValues);
        } else if (step instanceof PocObj.FileStep) {
            return executeFileStep((PocObj.FileStep) step, extractedValues);
        } else if (step instanceof PocObj.HeadlessStep) {
            return executeHeadlessStep((PocObj.HeadlessStep) step, extractedValues);
        } else if (step instanceof PocObj.TcpStep) {
            return executeTcpStep((PocObj.TcpStep) step, extractedValues);
        } else if (step instanceof PocObj.CodeStep) {
            return executeCodeStep((PocObj.CodeStep) step, extractedValues);
        }

        // HTTP 请求
        RequestObj requestObj = createRequest(target, step, globalConfig, extractedValues);
        
        // 发送请求
        long requestTime = System.currentTimeMillis();
        try (CustomHttpResponse response = requests(requestObj)) {
            long responseTime = System.currentTimeMillis();
            
            if (response == null) {
                return false;
            }
            
            // 设置响应时间
            response.setResponseTime(responseTime - requestTime);
            
            // 缓存响应
            String cacheKey = step.getStepId() != null ? step.getStepId() : "step_" + allSteps.indexOf(step);
            responseCache.put(cacheKey, response, requestTime, responseTime);
            
            // 匹配结果
            boolean matched = ResponseMatcher.matchResponse(
                response, step.getMatchers(), step.getMatchersCondition(), responseCache,
                cacheKey, extractedValues
            );
            
            // 提取变量（即使不匹配也可能需要提取变量供后续步骤使用）
            if (step.getExtractors() != null && !step.getExtractors().isEmpty()) {
                VariableExtractor.extractVariablesObj(response, step.getExtractors(), extractedValues);
            }
            
            return matched;
        }
    }
    
    /**
     * 创建HTTP请求对象
     */
    private RequestObj createRequest(String target, PocObj.PocStep step, 
                                     PocObj.GlobalConfig globalConfig, 
                                     Map<String, Object> extractedValues) {
        RequestObj requestObj = new RequestObj();
        
        // POC扫描时保留原始URL编码，不进行归一化
        // 用于正确检测路径遍历等漏洞（如 %2e%2e、%u002e 等编码绕过）
        requestObj.setPreserveRawUrl(true);
        
        // 检查是否使用raw请求格式
        if (step.getRaw() != null && !step.getRaw().isEmpty()) {
            // 处理raw格式的HTTP请求
            HttpHandler.processRawRequestObj(requestObj, step.getRaw(), target, extractedValues);
        } else {
            // 替换变量
            String path = HttpHandler.replaceVariablesObj(step.getPath(), extractedValues);
            String body = HttpHandler.replaceVariablesObj(step.getBody(), extractedValues);
            
            // 构建完整URL
            String url = buildUrl(target, path);
            requestObj.setUrl(url);
            
            // 设置请求方法
            if (step.getMethod() != null && !step.getMethod().isEmpty()) {
                requestObj.setMethod(step.getMethod());
            }
            
            // 设置请求头 - POC step级别headers（后续会与全局headers合并）
            if (step.getHeaders() != null && !step.getHeaders().isEmpty()) {
                Map<String, String> headers = new HashMap<>();
                for (Map.Entry<String, String> entry : step.getHeaders().entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        headers.put(entry.getKey(),
                            HttpHandler.replaceVariablesObj(entry.getValue(), extractedValues));
                    }
                }
                requestObj.setHeaders(headers);
            }

            // 设置请求体
            if (body != null && !body.isEmpty()) {
                requestObj.setPostData(body);
            }
        }

        // 合并headers：全局defaultHeaders < POC globalHeaders < POC stepHeaders
        {
            Map<String, String> pocStepHeaders = requestObj.getHeaders();
            Map<String, String> pocGlobalHeaders = null;
            if (globalConfig != null && globalConfig.getGlobalHeaders() != null &&
                !globalConfig.getGlobalHeaders().isEmpty()) {
                pocGlobalHeaders = new HashMap<>();
                for (Map.Entry<String, String> entry : globalConfig.getGlobalHeaders().entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        pocGlobalHeaders.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            // 使用HeaderManager合并：defaultHeaders < pocGlobalHeaders < pocStepHeaders
            Map<String, String> merged = HeaderManager.getInstance().mergeHeaders(null, pocGlobalHeaders);
            if (pocStepHeaders != null && !pocStepHeaders.isEmpty()) {
                merged.putAll(pocStepHeaders);
            }
            requestObj.setHeaders(merged);
        }
        
        // 应用认证配置（Goby POC Authentication支持）
        if (globalConfig != null && globalConfig.getAuthConfig() != null && 
            !globalConfig.getAuthConfig().isEmpty()) {
            applyAuthenticationConfig(requestObj, globalConfig.getAuthConfig(), extractedValues);
        }
        
        // 设置超时时间
        int timeout = determineTimeout(step, globalConfig);
        if (timeout > 0) {
            requestObj.setTimeOut(timeout);
        }
        
        // 设置重试次数
        int retries = determineRetries(step, globalConfig);
        if (retries > 0) {
            requestObj.setRetries(retries);
        }
        
        // 设置是否跟随重定向
        requestObj.setFollowRedirects(step.isFollowRedirect());
        
        // 设置代理
        String proxy = determineProxy(step, globalConfig);
        if (proxy != null && !proxy.isEmpty()) {
            requestObj.setProxies(proxy);
        }
        
        // 设置最大响应大小
        if (scanConfig != null && scanConfig.getMaxResponseSize() > 0) {
            requestObj.setMaxResponseSize(scanConfig.getMaxResponseSize());
        }
        
        return requestObj;
    }
    
    /**
     * 构建完整URL
     */
    private String buildUrl(String target, String path) {
        // 确保 target 不为空
        if (target == null || target.trim().isEmpty()) {
            System.err.println("[错误] 目标 URL 为空");
            return null;
        }
        
        if (path == null || path.isEmpty()) {
            return target;
        }
        
        // 如果 path 是完整URL，直接返回
        if (path.toLowerCase().startsWith("http://") || path.toLowerCase().startsWith("https://")) {
            return path;
        }

        // 检查 path 是否包含未替换的变量，使用根路径作为后备
        if (path.matches("^\\{\\{.*\\}\\}$")) {
            path = "/";
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (target.endsWith("/")) {
            target = target.substring(0, target.length() - 1);
        }
        return target + path;
    }
    
    /**
     * 确定超时时间
     */
    private int determineTimeout(PocObj.PocStep step, PocObj.GlobalConfig globalConfig) {
        if (step.getTimeout() > 0) {
            return step.getTimeout();
        } else if (globalConfig != null && globalConfig.getRetryInterval() > 0) {
            return globalConfig.getRetryInterval() / 1000;
        } else if (scanConfig != null && scanConfig.getTimeout() > 0) {
            return scanConfig.getTimeout();
        }
        return 0;
    }
    
    /**
     * 确定重试次数
     */
    private int determineRetries(PocObj.PocStep step, PocObj.GlobalConfig globalConfig) {
        if (step.getRetries() > 0) {
            return step.getRetries();
        } else if (globalConfig != null && globalConfig.getMaxRetries() > 0) {
            return globalConfig.getMaxRetries();
        }
        return 0;
    }
    
    /**
     * 确定代理设置
     */
    private String determineProxy(PocObj.PocStep step, PocObj.GlobalConfig globalConfig) {
        if (step.getProxy() != null && !step.getProxy().isEmpty()) {
            return step.getProxy();
        } else if (globalConfig != null && globalConfig.getProxy() != null && !globalConfig.getProxy().isEmpty()) {
            return globalConfig.getProxy();
        } else if (scanConfig != null && scanConfig.getProxy() != null && !scanConfig.getProxy().isEmpty()) {
            return scanConfig.getProxy();
        }
        return null;
    }
    
    /**
     * 判断是否是网络错误
     */
    private boolean isNetworkError(Exception e) {
        if (e == null || e.getMessage() == null) {
            return false;
        }
        
        String message = e.getMessage().toLowerCase();
        return message.contains("请求失败，超出重试次数") ||
               message.contains("connection timed out") ||
               message.contains("read timed out") ||
               message.contains("sockettimeoutexception") ||
               message.contains("connectexception") ||
               message.contains("unknownhostexception");
    }
    
    /**
     * 创建漏洞结果
     */
    private ScanResult createVulnerableResult(String target, PocObj.Poc poc, long startTime) {
        ScanResult result = new ScanResult();
        result.setTarget(target);
        result.setPoc(poc);
        result.setVulnerable(true);
        result.setTimestamp(System.currentTimeMillis());

        // 添加详细信息
        result.addDetail("protocol", poc.getProtocol());
        result.addDetail("severity", poc.getSeverity());
        result.addDetail("scan_duration", System.currentTimeMillis() - startTime);
        result.addDetail("description", poc.getDescription());
        result.addDetail("references", poc.getReferences());

        // 填充请求/响应数据（用于报告生成和UI展示）
        try {
            RequestObj matchedRequest = lastMatchedRequest.get();
            CustomHttpResponse matchedResponse = lastMatchedResponse.get();
            String matchedPath = lastMatchedPath.get();
            String matchedPayload = lastMatchedPayload.get();

            if (matchedRequest != null) {
                result.setRawRequest(formatHttpRequest(matchedRequest));
            }

            if (matchedResponse != null) {
                result.setRawResponseSnippet(formatHttpResponse(matchedResponse));
            }

            if (matchedPath != null) {
                result.setMatchedPath(matchedPath);
            }

            if (matchedPayload != null) {
                result.setMatchedPayload(matchedPayload);
            }
        } catch (Exception e) {
            if (debugMode) {
                System.err.println("填充请求/响应数据失败: " + e.getMessage());
            }
        }

        // 填充增强报告字段（独立 try-catch，避免互相影响）
        try {
            result.populateFromPoc();
        } catch (Exception ignored) {}

        try {
            List<StepExecutionRecord> records = stepExecutionRecords.get();
            if (records != null && !records.isEmpty()) {
                result.setStepRecords(new ArrayList<>(records));
            }

            Map<String, Object> outputData = extractedOutputData.get();
            if (outputData != null && !outputData.isEmpty()) {
                result.setOutputData(new HashMap<>(outputData));
            }

            Map<String, String> varValues = usedVariableValues.get();
            if (varValues != null && !varValues.isEmpty()) {
                result.setVariableValues(new HashMap<>(varValues));
            }

            List<String> paramKeys = usedParamKeys.get();
            if (paramKeys != null && !paramKeys.isEmpty()) {
                result.setParamKeys(new ArrayList<>(paramKeys));
            }
        } catch (Exception e) {
            if (debugMode) {
                System.err.println("填充增强报告字段失败: " + e.getMessage());
            }
        } finally {
            // 清理 ThreadLocal 防止内存泄漏
            lastMatchedRequest.remove();
            lastMatchedResponse.remove();
            lastMatchedPath.remove();
            lastMatchedPayload.remove();
            stepExecutionRecords.remove();
            extractedOutputData.remove();
            usedVariableValues.remove();
            usedParamKeys.remove();
        }

        return result;
    }

    /**
     * 从变量映射中提取 payload 信息
     * 用于报告生成和漏洞复现
     *
     * @param extractedValues 提取的变量映射
     * @return payload 字符串
     */
    private String extractPayloadFromVariables(Map<String, Object> extractedValues) {
        if (extractedValues == null || extractedValues.isEmpty()) {
            return "";
        }

        StringBuilder payload = new StringBuilder();

        // 常见的 payload 变量名
        String[] payloadKeys = {"payload", "cmd", "command", "exploit", "shell", "sql", "xss", "injection"};

        // 首先查找明确的 payload 变量
        for (String key : payloadKeys) {
            if (extractedValues.containsKey(key)) {
                Object value = extractedValues.get(key);
                if (value != null) {
                    payload.append(key).append("=").append(value.toString()).append("; ");
                }
            }
        }

        // 如果没有找到明确的 payload，提取所有非内置变量
        if (payload.length() == 0) {
            // 排除内置变量
            String[] builtinKeys = {"BaseURL", "RootURL", "Hostname", "Host", "Port", "Path",
                                   "File", "Scheme", "Input", "ip", "FQDN", "RDN", "DN", "SD"};
            java.util.Set<String> builtinSet = new java.util.HashSet<>(java.util.Arrays.asList(builtinKeys));

            for (Map.Entry<String, Object> entry : extractedValues.entrySet()) {
                String key = entry.getKey();
                if (!builtinSet.contains(key) && entry.getValue() != null) {
                    payload.append(key).append("=").append(entry.getValue().toString()).append("; ");
                }
            }
        }

        return payload.toString().trim();
    }
    
    /**
     * 创建负面结果（未发现漏洞）
     */
    private ScanResult createNegativeResult(String target, PocObj.Poc poc, long startTime) {
        ScanResult result = new ScanResult();
        result.setTarget(target);
        result.setPoc(poc);
        result.setVulnerable(false);
        result.setTimestamp(System.currentTimeMillis());
        result.addDetail("scan_duration", System.currentTimeMillis() - startTime);
        return result;
    }
    
    /**
     * 提取 output 变量（Xray POC）
     * 使用 CEL 表达式从响应中提取数据
     * 
     * @param step POC步骤
     * @param response HTTP响应
     * @param request HTTP请求
     * @param extractedValues 已提取的变量映射
     */
    private void extractOutputVariables(
        PocObj.PocStep step,
        CustomHttpResponse response,
        RequestObj request,
        Map<String, Object> extractedValues
    ) {
        if (step.getOutput() == null || step.getOutput().isEmpty()) {
            return;
        }
        
        System.out.println("开始提取 output 变量，共 " + step.getOutput().size() + " 个");
        
        for (Map.Entry<String, Object> entry : step.getOutput().entrySet()) {
            String varName = entry.getKey();
            Object expressionObj = entry.getValue();
            
            if (expressionObj == null) {
                continue;
            }
            
            String expression = expressionObj.toString();
            
            try {
                // 使用 Xray CEL 评估器提取值
                // 注：Pocsuite 的正则表达式（<regex>前缀）在转换器中已被处理为 extractors
                Object value = XrayCelEvaluator.evaluateForValue(
                    expression,
                    response,
                    request
                );
                
                if (value != null) {
                    extractedValues.put(varName, value);
                    System.out.println("提取变量成功: " + varName + " = " + value);
                } else {
                    System.err.println("Output 表达式求值返回 null: " + varName + " = " + expression);
                }
            } catch (Exception e) {
                System.err.println("Output 提取失败: " + varName + " = " + expression);
                System.err.println("   错误: " + e.getMessage());
            }
        }
    }
    
    /**
     * 应用认证配置到请求
     * 
     * @param requestObj 请求对象
     * @param authConfig 认证配置
     * @param variables 变量映射
     */
    private void applyAuthenticationConfig(RequestObj requestObj, 
                                          Map<String, String> authConfig,
                                          Map<String, Object> variables) {
        if (authConfig == null || authConfig.isEmpty()) {
            return;
        }
        
        String authType = authConfig.get("type");
        if (authType == null || authType.isEmpty()) {
            return;
        }
        
        // 确保请求头存在
        Map<String, String> headers = requestObj.getHeaders();
        if (headers == null) {
            headers = new HashMap<>();
            requestObj.setHeaders(headers);
        }
        
        try {
            switch (authType.toLowerCase()) {
                case "basic":
                    applyBasicAuth(headers, authConfig, variables);
                    break;
                    
                case "bearer":
                    applyBearerAuth(headers, authConfig, variables);
                    break;
                    
                case "cookie":
                    applyCookieAuth(headers, authConfig, variables);
                    break;
                    
                default:
                    System.err.println("不支持的认证类型: " + authType);
            }
        } catch (Exception e) {
            System.err.println("应用认证配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 应用Basic认证
     */
    private void applyBasicAuth(Map<String, String> headers, 
                               Map<String, String> authConfig,
                               Map<String, Object> variables) {
        String username = resolveAuthValue(authConfig.get("username"), variables);
        String password = resolveAuthValue(authConfig.get("password"), variables);
        
        if (username == null || password == null) {
            return;
        }
        
        String credentials = username + ":" + password;
        String encoded = Base64.getEncoder().encodeToString(
            credentials.getBytes(StandardCharsets.UTF_8)
        );
        
        headers.put("Authorization", "Basic " + encoded);
    }
    
    /**
     * 应用Bearer认证
     */
    private void applyBearerAuth(Map<String, String> headers,
                                Map<String, String> authConfig,
                                Map<String, Object> variables) {
        String token = resolveAuthValue(authConfig.get("token"), variables);
        
        if (token != null && !token.isEmpty()) {
            headers.put("Authorization", "Bearer " + token);
        }
    }
    
    /**
     * 应用Cookie认证
     */
    private void applyCookieAuth(Map<String, String> headers,
                                Map<String, String> authConfig,
                                Map<String, Object> variables) {
        String cookiesJson = authConfig.get("cookies");
        if (cookiesJson == null || cookiesJson.isEmpty()) {
            return;
        }
        
        try {
            // 简单解析cookies JSON
            cookiesJson = cookiesJson.trim();
            if (cookiesJson.startsWith("{") && cookiesJson.endsWith("}")) {
                cookiesJson = cookiesJson.substring(1, cookiesJson.length() - 1);
                
                StringBuilder cookieHeader = new StringBuilder();
                if (headers.containsKey("Cookie")) {
                    cookieHeader.append(headers.get("Cookie"));
                }
                
                String[] pairs = cookiesJson.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split(":", 2);
                    if (kv.length == 2) {
                        String key = kv[0].trim().replace("\"", "");
                        String value = kv[1].trim().replace("\"", "");
                        value = resolveAuthValue(value, variables);
                        
                        if (cookieHeader.length() > 0) {
                            cookieHeader.append("; ");
                        }
                        cookieHeader.append(key).append("=").append(value);
                    }
                }
                
                headers.put("Cookie", cookieHeader.toString());
            }
        } catch (Exception e) {
            System.err.println("解析Cookie配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析认证值（支持变量引用）
     */
    private String resolveAuthValue(String value, Map<String, Object> variables) {
        if (value == null) {
            return null;
        }
        
        // 如果是变量引用 {{{varName}}}
        if (value.contains("{{{") && value.contains("}}}") && variables != null) {
            String result = value;
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String val = entry.getValue() != null ? entry.getValue().toString() : "";
                result = result.replace("{{{" + entry.getKey() + "}}}", val);
            }
            return result;
        }
        
        return value;
    }
    

    /**
     * 执行 DNS 步骤
     */
    private boolean executeDnsStep(PocObj.DnsStep dnsStep, Map<String, Object> variables) {
        try {
            // 替换变量（支持嵌套变量）
            String domain = HttpHandler.replaceVariablesObj(dnsStep.getDomain(), variables);
            
            System.out.println("→ 执行 DNS 查询: " + domain);
            
            // 执行 DNS 查询
            DnsHandler.DnsResponse dnsResponse = DnsHandler.query(
                domain,
                dnsStep.getType() != null ? dnsStep.getType() : "A",
                dnsStep.getResolver(),
                true,
                2
            );
            
            if (!dnsResponse.isSuccess()) {
                System.err.println("DNS 查询失败: " + dnsResponse.getError());
                return false;
            }
            
            // 构造可匹配的响应体
            String responseBody = String.join("\n", dnsResponse.getAnswers());
            
            // 执行匹配（简化版，使用字符串匹配）
            if (dnsStep.getMatchers() != null && !dnsStep.getMatchers().isEmpty()) {
                boolean matched = SimpleMatcher.match(responseBody, dnsStep);
                if (!matched) {
                    System.out.println("DNS 响应不匹配");
                    return false;
                }
            }
            
            System.out.println("✓ DNS 步骤执行成功");
            return true;
            
        } catch (Exception e) {
            System.err.println("DNS 步骤执行失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 执行 WebSocket 步骤
     */
    private boolean executeWebSocketStep(PocObj.WebSocketStep wsStep, Map<String, Object> variables) {
        try {
            System.out.println("→ 执行 WebSocket 通信: " + wsStep.getAddress());

            // 替换变量（支持嵌套变量）
            String address = HttpHandler.replaceVariablesObj(wsStep.getAddress(), variables);
            List<String> messages = wsStep.getMessages().stream()
                .map(msg -> HttpHandler.replaceVariablesObj(msg, variables))
                .collect(Collectors.toList());
            
            // 执行 WebSocket 通信
            WebSocketHandler.WebSocketResponse wsResponse = 
                WebSocketHandler.communicate(address, messages, wsStep.getHeaders(), 10);
            
            if (!wsResponse.isSuccess()) {
                System.err.println("WebSocket 通信失败: " + wsResponse.getError());
                return false;
            }
            
            // 执行匹配
            if (wsStep.getMatchers() != null && !wsStep.getMatchers().isEmpty()) {
                String responseBody = String.join("\n", wsResponse.getReceivedMessages());
                boolean matched = SimpleMatcher.match(responseBody, wsStep);
                if (!matched) {
                    System.out.println("WebSocket 响应不匹配");
                    return false;
                }
            }
            
            System.out.println("✓ WebSocket 步骤执行成功");
            return true;
            
        } catch (Exception e) {
            System.err.println("WebSocket 步骤执行失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 执行 SSL/TLS 步骤
     */
    private boolean executeSslStep(PocObj.SslStep sslStep, Map<String, Object> variables) {
        try {
            // 替换变量（支持嵌套变量）
            String address = HttpHandler.replaceVariablesObj(sslStep.getAddress(), variables);
            
            System.out.println("→ 执行 SSL/TLS 检测: " + address);
            
            // 执行 SSL 检测
            SslHandler.SslResponse sslResponse = SslHandler.check(address, sslStep.getTimeout());
            
            if (!sslResponse.isSuccess()) {
                System.err.println("SSL/TLS 检测失败: " + sslResponse.getError());
                return false;
            }
            
            // 执行匹配
            if (sslStep.getMatchers() != null && !sslStep.getMatchers().isEmpty()) {
                boolean matched = SimpleMatcher.match(sslResponse.getRaw(), sslStep);
                if (!matched) {
                    System.out.println("SSL/TLS 响应不匹配");
                    return false;
                }
            }
            
            System.out.println("✓ SSL/TLS 步骤执行成功");
            return true;
            
        } catch (Exception e) {
            System.err.println("SSL/TLS 步骤执行失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 执行 File 步骤
     */
    private boolean executeFileStep(PocObj.FileStep fileStep, Map<String, Object> variables) {
        try {
            // 替换变量（支持嵌套变量）
            List<String> paths = fileStep.getPaths().stream()
                .map(path -> HttpHandler.replaceVariablesObj(path, variables))
                .collect(Collectors.toList());
            
            System.out.println("→ 执行文件扫描: " + paths);
            
            // 执行文件扫描
            List<FileHandler.FileResponse> fileResponses = FileHandler.scanPaths(
                paths,
                fileStep.getExtensions(),
                fileStep.isRecursive(),
                fileStep.getMaxSize()
            );
            
            if (fileResponses.isEmpty()) {
                System.out.println("未找到匹配的文件");
                return false;
            }
            
            // 执行匹配（检查任意文件匹配即可）
            if (fileStep.getMatchers() != null && !fileStep.getMatchers().isEmpty()) {
                for (FileHandler.FileResponse fileResponse : fileResponses) {
                    if (fileResponse.isSuccess() && fileResponse.getContent() != null) {
                        boolean matched = SimpleMatcher.match(fileResponse.getContent(), fileStep);
                        if (matched) {
                            System.out.println("✓ 文件匹配成功: " + fileResponse.getPath());
                            return true;
                        }
                    }
                }
                System.out.println("所有文件都不匹配");
                return false;
            }
            
            System.out.println("✓ File 步骤执行成功");
            return true;
            
        } catch (Exception e) {
            System.err.println("File 步骤执行失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 执行 Headless 步骤
     */
    private boolean executeHeadlessStep(PocObj.HeadlessStep headlessStep, Map<String, Object> variables) {
        try {
            System.out.println("→ 执行 Headless 浏览器操作");

            // 转换操作步骤（支持嵌套变量）
            List<HeadlessHandler.BrowserStep> browserSteps = new ArrayList<>();
            for (PocObj.BrowserAction action : headlessStep.getActions()) {
                Map<String, String> args = new HashMap<>();
                if (action.getArgs() != null) {
                    for (Map.Entry<String, String> entry : action.getArgs().entrySet()) {
                        args.put(entry.getKey(), HttpHandler.replaceVariablesObj(entry.getValue(), variables));
                    }
                }
                browserSteps.add(new HeadlessHandler.BrowserStep(action.getAction(), args));
            }
            
            // 执行 Headless 操作
            HeadlessHandler.HeadlessResponse headlessResponse = 
                HeadlessHandler.execute(headlessStep.getUrl(), browserSteps, 30);
            
            if (!headlessResponse.isSuccess()) {
                System.err.println("Headless 操作失败: " + headlessResponse.getError());
                return false;
            }
            
            // 执行匹配
            if (headlessStep.getMatchers() != null && !headlessStep.getMatchers().isEmpty()) {
                boolean matched = SimpleMatcher.match(headlessResponse.getPageSource(), headlessStep);
                if (!matched) {
                    System.out.println("Headless 响应不匹配");
                    return false;
                }
            }
            
            System.out.println("✓ Headless 步骤执行成功");
            return true;
            
        } catch (Exception e) {
            System.err.println("Headless 步骤执行失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 执行 Code 步骤
     * 支持 JavaScript 和 Python 引擎
     */
    private boolean executeCodeStep(PocObj.CodeStep codeStep, Map<String, Object> variables) {
        try {
            // 检查引擎类型
            String engine = codeStep.getEngine();
            if (engine == null || engine.isEmpty()) {
                engine = "javascript"; // 默认为 JavaScript
            }

            String engineLower = engine.toLowerCase();

            // 检查是否为支持的引擎
            if (!isSupportedEngine(engineLower)) {
                // 特殊处理 Python：提示用户配置 Python 路径
                if (isPythonEngine(engineLower)) {
                    System.err.println("✗ Code 步骤失败: Python 引擎不可用");
                    System.err.println("  → 请配置 Python 路径: PythonHandler.setPythonPath(\"/path/to/python\")");
                    System.err.println("  → 或设置环境变量 PYTHON_PATH");
                } else {
                    System.err.println("✗ Code 步骤失败: 不支持的引擎类型 '" + engine + "'");
                    System.err.println("  → 当前支持: JavaScript/JS, Python/Py, Shell(sh/bash/cmd), PowerShell, Ruby, Go, Perl, PHP");
                }
                return false;
            }

            System.out.println("→ 执行 Code 代码 (引擎: " + engine + ")");

            // 准备上下文
            Map<String, Object> context = new HashMap<>(variables);

            String resultString;
            boolean success;

            // 根据引擎类型执行代码
            if (isPythonEngine(engineLower)) {
                // Python 引擎
                PythonHandler.PythonResponse pythonResponse = 
                    PythonHandler.executePython(codeStep.getSource(), context);
                
                success = pythonResponse.isSuccess();
                resultString = pythonResponse.getResultString();
                
                if (!success) {
                    System.err.println("Python 执行失败: " + pythonResponse.getError());
                    if (!pythonResponse.getOutput().isEmpty()) {
                        System.out.println("Python 输出: " + pythonResponse.getOutput());
                    }
                    return false;
                }
                
                // 输出 Python 执行信息
                if (!pythonResponse.getOutput().isEmpty()) {
                    System.out.println("Python stdout: " + pythonResponse.getOutput());
                }
                System.out.println("✓ Python 执行成功 (" + pythonResponse.getExecutionTime() + "ms)");
                
            } else if (isShellEngine(engineLower)) {
                // Shell 类引擎（sh, bash, cmd, powershell, ruby, go, perl, php）
                ShellExecutionResult shellResult = executeShellCode(engineLower, codeStep.getSource(), context);
                
                success = shellResult.success;
                resultString = shellResult.output;
                
                if (!success) {
                    System.err.println("Shell 执行失败: " + shellResult.error);
                    return false;
                }
                
                System.out.println("✓ " + engine + " 执行成功 (" + shellResult.executionTime + "ms)");
                if (!resultString.isEmpty()) {
                    System.out.println("输出: " + resultString);
                }
                
            } else {
                // JavaScript 引擎
                CodeHandler.CodeResponse codeResponse =
                    CodeHandler.executeJavaScript(codeStep.getSource(), context);

                success = codeResponse.isSuccess();
                resultString = codeResponse.getResultString();
                
                if (!success) {
                    System.err.println("JavaScript 执行失败: " + codeResponse.getError());
                    return false;
                }
                
                System.out.println("✓ JavaScript 执行成功，结果: " + codeResponse.getResult());
            }

            // 执行匹配
            if (codeStep.getMatchers() != null && !codeStep.getMatchers().isEmpty()) {
                boolean matched = SimpleMatcher.match(resultString, codeStep);
                if (!matched) {
                    System.out.println("Code 结果不匹配");
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            System.err.println("Code 步骤执行失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 执行 TCP 步骤
     */
    private boolean executeTcpStep(PocObj.TcpStep tcpStep, Map<String, Object> variables) {
        try {
            // 替换变量
            String host = HttpHandler.replaceVariablesObj(tcpStep.getHost(), variables);
            String portStr = HttpHandler.replaceVariablesObj(tcpStep.getPort(), variables);
            
            System.out.println("→ 执行 TCP 通信: " + host + ":" + portStr);
            
            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                System.err.println("无效的端口: " + portStr);
                return false;
            }

            // 处理输入列表中的变量
            List<PocObj.Input> processedInputs = new ArrayList<>();
            if (tcpStep.getInputs() != null) {
                for (PocObj.Input input : tcpStep.getInputs()) {
                    PocObj.Input processed = new PocObj.Input();
                    processed.setType(input.getType());
                    processed.setName(input.getName());
                    processed.setRead(input.getRead());
                    processed.setEncoding(input.getEncoding());
                    // 替换数据中的变量
                    if (input.getData() != null) {
                        processed.setData(HttpHandler.replaceVariablesObj(input.getData(), variables));
                    }
                    processedInputs.add(processed);
                }
            }

            // 执行 Socket 通信
            SocketHandler.SocketResponse response = SocketHandler.execute(
                host,
                port,
                processedInputs,
                tcpStep.getTimeout() > 0 ? tcpStep.getTimeout() : 10 // 默认10秒
            );

            if (!response.isSuccess()) {
                System.err.println("TCP 通信失败: " + response.getError());
                return false;
            }

            // 执行匹配
            if (tcpStep.getMatchers() != null && !tcpStep.getMatchers().isEmpty()) {
                boolean matched = SimpleMatcher.match(response.getRawString(), tcpStep);
                if (!matched) {
                    System.out.println("TCP 响应不匹配");
                    return false;
                }
            }

            System.out.println("✓ TCP 步骤执行成功");
            return true;

        } catch (Exception e) {
            System.err.println("TCP 步骤执行失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 检查是否为支持的 Code 引擎类型
     * 支持: JavaScript, Python, Shell(sh/bash/cmd), PowerShell, Ruby, Go, Perl, PHP
     */
    private boolean isSupportedEngine(String engine) {
        if ("javascript".equals(engine) || "js".equals(engine)) {
            return true;
        }
        if (isPythonEngine(engine)) {
            // Python 需要检查是否可用
            return PythonHandler.isPythonAvailable();
        }
        // Shell 类引擎 - 通过 ProcessBuilder 执行
        if (isShellEngine(engine)) {
            return true;
        }
        return false;
    }
    
    /**
     * 检查是否为 Python 引擎
     */
    private boolean isPythonEngine(String engine) {
        return "python".equals(engine) || "python3".equals(engine) || "py".equals(engine);
    }
    
    /**
     * 检查是否为 Shell 类引擎（sh, bash, cmd, powershell, ruby, go 等）
     */
    private boolean isShellEngine(String engine) {
        switch (engine) {
            case "sh":
            case "bash":
            case "cmd":
            case "powershell":
            case "powershell.exe":
            case "ps":
            case "ruby":
            case "rb":
            case "go":
            case "perl":
            case "php":
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Shell 执行结果
     */
    private static class ShellExecutionResult {
        boolean success;
        String output;
        String error;
        long executionTime;
        
        ShellExecutionResult(boolean success, String output, String error, long executionTime) {
            this.success = success;
            this.output = output;
            this.error = error;
            this.executionTime = executionTime;
        }
    }
    
    /**
     * 执行 Shell 类代码
     * 支持: sh, bash, cmd, powershell, ruby, go, perl, php
     */
    private ShellExecutionResult executeShellCode(String engine, String code, Map<String, Object> context) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 替换代码中的变量
            String processedCode = HttpHandler.replaceVariablesObj(code, context);
            
            // 根据引擎类型构建命令
            List<String> command = new ArrayList<>();
            
            // 判断操作系统
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            
            switch (engine) {
                case "sh":
                case "bash":
                    if (isWindows) {
                        // Windows 上尝试使用 Git Bash 或 WSL
                        command.add("bash");
                        command.add("-c");
                        command.add(processedCode);
                    } else {
                        command.add(engine.equals("sh") ? "/bin/sh" : "/bin/bash");
                        command.add("-c");
                        command.add(processedCode);
                    }
                    break;
                    
                case "cmd":
                    command.add("cmd");
                    command.add("/c");
                    command.add(processedCode);
                    break;
                    
                case "powershell":
                case "powershell.exe":
                case "ps":
                    if (isWindows) {
                        command.add("powershell");
                        command.add("-Command");
                        command.add(processedCode);
                    } else {
                        // Linux/Mac 上使用 pwsh（PowerShell Core）
                        command.add("pwsh");
                        command.add("-Command");
                        command.add(processedCode);
                    }
                    break;
                    
                case "ruby":
                case "rb":
                    command.add("ruby");
                    command.add("-e");
                    command.add(processedCode);
                    break;
                    
                case "go":
                    // Go 需要先编译运行，这里使用 go run
                    // 注意：这需要代码是完整的 Go 程序
                    command.add("go");
                    command.add("run");
                    command.add("-"); // 从 stdin 读取
                    break;
                    
                case "perl":
                    command.add("perl");
                    command.add("-e");
                    command.add(processedCode);
                    break;
                    
                case "php":
                    command.add("php");
                    command.add("-r");
                    command.add(processedCode);
                    break;
                    
                default:
                    return new ShellExecutionResult(false, "", "不支持的引擎类型: " + engine, 0);
            }
            
            // 执行命令
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);
            
            Process process = pb.start();
            
            // 如果是 go run，需要向 stdin 写入代码
            if ("go".equals(engine)) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(processedCode.getBytes("UTF-8"));
                    os.flush();
                }
            }
            
            // 读取输出
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdout.append(line).append("\n");
                }
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderr.append(line).append("\n");
                }
            }
            
            // 等待进程完成（最多 30 秒）
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new ShellExecutionResult(false, stdout.toString().trim(), "执行超时（30秒）", 
                        System.currentTimeMillis() - startTime);
            }
            
            int exitCode = process.exitValue();
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (exitCode == 0) {
                return new ShellExecutionResult(true, stdout.toString().trim(), "", executionTime);
            } else {
                return new ShellExecutionResult(false, stdout.toString().trim(), 
                        stderr.toString().trim() + " (exit code: " + exitCode + ")", executionTime);
            }
            
        } catch (Exception e) {
            return new ShellExecutionResult(false, "", "执行异常: " + e.getMessage(), 
                    System.currentTimeMillis() - startTime);
        }
    }
    
    /**
     * 从目标 URL 提取 Nuclei Helper Variables（内置变量）
     *
     * 支持的变量：
     * - {{BaseURL}} - 完整的基础URL（如 https://example.com）
     * - {{RootURL}} - 根URL（与BaseURL相同）
     * - {{Hostname}} - 主机名（如 www.example.com）
     * - {{Host}} - 主机:端口（如 example.com:443）
     * - {{Port}} - 端口号（如 443）
     * - {{Path}} - URL路径（如 /api/v1/users）
     * - {{File}} - 路径文件名部分（如 users）
     * - {{Scheme}} - 协议方案（如 https）
     * - {{Input}} - 原始输入目标
     * - {{ip}} - 目标IP地址（通过DNS解析）
     * - {{FQDN}} - 完全限定域名（如 www.example.com）
     * - {{RDN}} - 根域名（如 example.com）
     * - {{DN}} - 域名主体不含TLD（如 example）
     * - {{SD}} - 子域名部分（如 www 或 www.blog）
     *
     * @param target 目标URL
     * @return 内置变量映射（变量名 -> 单值列表）
     */
    private Map<String, List<String>> extractHelperVariables(String target) {
        Map<String, List<String>> variables = new HashMap<>();

        try {
            // 确保 target 是完整 URL
            String fullUrl = target;
            if (!target.startsWith("http://") && !target.startsWith("https://")) {
                fullUrl = "http://" + target;
            }

            URL url = new URL(fullUrl);
            String hostname = url.getHost();

            // {{BaseURL}} 和 {{RootURL}} - 基础URL（protocol + host + port）
            String baseURL = url.getProtocol() + "://" + hostname;
            int port = url.getPort();
            if (port != -1 && port != url.getDefaultPort()) {
                baseURL += ":" + port;
            }
            variables.put("BaseURL", Collections.singletonList(baseURL));
            variables.put("RootURL", Collections.singletonList(baseURL));

            // {{Hostname}} - 主机名
            variables.put("Hostname", Collections.singletonList(hostname));

            // {{Host}} - 主机:端口
            String host = hostname;
            int actualPort = port != -1 ? port : url.getDefaultPort();
            if (actualPort != 80 && actualPort != 443) {
                host += ":" + actualPort;
            }
            variables.put("Host", Collections.singletonList(host));

            // {{Port}} - 端口号
            String portStr = String.valueOf(actualPort);
            variables.put("Port", Collections.singletonList(portStr));

            // {{Path}} - URL路径
            String path = url.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }
            variables.put("Path", Collections.singletonList(path));

            // {{File}} - 路径文件名部分
            String file = "";
            if (path != null && !path.isEmpty() && !path.equals("/")) {
                String[] pathParts = path.split("/");
                if (pathParts.length > 0) {
                    String lastPart = pathParts[pathParts.length - 1];
                    // 移除文件扩展名
                    int dotIndex = lastPart.lastIndexOf('.');
                    file = dotIndex > 0 ? lastPart.substring(0, dotIndex) : lastPart;
                }
            }
            variables.put("File", Collections.singletonList(file));

            // {{Scheme}} - 协议
            variables.put("Scheme", Collections.singletonList(url.getProtocol()));

            // {{Input}} - 原始输入
            variables.put("Input", Collections.singletonList(target));

            // {{ip}} - IP地址（懒加载，使用时才 DNS 解析）
            variables.put("ip", Collections.singletonList("{{LAZY_IP:" + hostname + "}}"));

            // ========== 新增域名拆分变量（Nuclei 2.7+） ==========
            // {{FQDN}} - Fully Qualified Domain Name（完整域名）
            variables.put("FQDN", Collections.singletonList(hostname));

            // 拆分域名各部分
            String[] domainParts = hostname.split("\\.");
            if (domainParts.length >= 2) {
                // {{RDN}} - Root Domain Name（根域名 = 倒数第2部分 + TLD）
                // 例：www.blog.example.com → example.com
                String rdn = domainParts[domainParts.length - 2] + "." + domainParts[domainParts.length - 1];
                variables.put("RDN", Collections.singletonList(rdn));

                // {{DN}} - Domain Name without TLD（域名主体，不含顶级域名）
                // 例：www.blog.example.com → example
                String dn = domainParts[domainParts.length - 2];
                variables.put("DN", Collections.singletonList(dn));

                // {{SD}} - Subdomain（子域名部分）
                // 例：www.blog.example.com → www.blog
                if (domainParts.length > 2) {
                    StringBuilder sd = new StringBuilder();
                    for (int i = 0; i < domainParts.length - 2; i++) {
                        if (i > 0) {
                            sd.append(".");
                        }
                        sd.append(domainParts[i]);
                    }
                    variables.put("SD", Collections.singletonList(sd.toString()));
                } else {
                    // 没有子域名，SD 为空字符串
                    variables.put("SD", Collections.singletonList(""));
                }
            } else {
                // 域名格式异常（如 localhost），使用默认值
                variables.put("RDN", Collections.singletonList(hostname));
                variables.put("DN", Collections.singletonList(hostname));
                variables.put("SD", Collections.singletonList(""));
            }

            // ========== Interactsh URL 按需初始化（OOB 检测）==========
            // 注意：interactsh-url 变量将在 replaceVariables 时按需生成
            // 这里只添加占位符，避免每个 POC 都触发 Interactsh 注册
            variables.put("interactsh-url", Collections.singletonList("{{LAZY_INTERACTSH}}"));

        } catch (MalformedURLException e) {
            System.err.println("[警告] 目标URL格式错误，无法提取内置变量: " + target);
            // 返回最基本的变量
            variables.put("BaseURL", Collections.singletonList(target));
            variables.put("Input", Collections.singletonList(target));
        }

        return variables;
    }

    /**
     * 预计算 variables 中的 DSL 表达式
     *
     * 处理如下情况：
     * variables:
     *   rand: "{{rand_base(5)}}"           → 计算随机值
     *   valid_username: "{{replace(user, '.', '')}}"  → 需要依赖其他变量
     *
     * @param variables 原始变量映射
     * @param target 目标URL（用于某些需要目标信息的DSL函数）
     * @return 计算后的变量映射
     */
    private Map<String, List<String>> evaluateDslVariables(Map<String, List<String>> variables, String target) {
        if (variables == null || variables.isEmpty()) {
            return variables;
        }

        Map<String, List<String>> evaluatedVariables = new HashMap<>();
        
        // 用于存储已计算的变量值（供后续变量引用），使用 Object 类型以支持 evaluateDslExpression
        Map<String, Object> flatVariables = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : variables.entrySet()) {
            String varName = entry.getKey();
            List<String> values = entry.getValue();

            if (values == null || values.isEmpty()) {
                evaluatedVariables.put(varName, values);
                continue;
            }

            List<String> newValues = new ArrayList<>();

            for (String value : values) {
                if (value == null) {
                    newValues.add(null);
                    continue;
                }

                // 检查是否包含 DSL 表达式 {{...}}
                if (value.contains("{{") && value.contains("}}")) {
                    try {
                        // 首先替换已知变量
                        String processedValue = value;
                        for (Map.Entry<String, Object> varEntry : flatVariables.entrySet()) {
                            String placeholder = "{{" + varEntry.getKey() + "}}";
                            if (processedValue.contains(placeholder) && varEntry.getValue() != null) {
                                processedValue = processedValue.replace(placeholder, varEntry.getValue().toString());
                            }
                        }

                        // 只有当整个值仍然被 {{}} 包围时，才计算 DSL 函数
                        // 这样可以避免错误地评估 path 中的裸露函数，如 "print(md5({{num}}))" → "print(md5(999))"
                        if (processedValue.trim().startsWith("{{") && processedValue.trim().endsWith("}}")) {
                            // 然后计算 DSL 函数
                            String evaluatedValue = evaluateDslExpression(processedValue, target, flatVariables);
                            newValues.add(evaluatedValue);

                            // 更新 flatVariables，供后续变量使用
                            flatVariables.put(varName, evaluatedValue);
                        } else {
                            // 替换后不再是完整的 {{}} 格式，保留替换后的值（不评估 DSL）
                            newValues.add(processedValue);
                            flatVariables.put(varName, processedValue);
                        }
                    } catch (Exception e) {
                        System.err.println("[警告] DSL 表达式计算失败: " + value + " - " + e.getMessage());
                        // 计算失败，保留原值
                        newValues.add(value);
                    }
                } else {
                    // 不包含 DSL 表达式，直接使用
                    newValues.add(value);
                    flatVariables.put(varName, value);
                }
            }

            evaluatedVariables.put(varName, newValues);
        }

        return evaluatedVariables;
    }

    /**
     * 计算单个 DSL 表达式
     *
     * @param expression DSL 表达式（如 "{{rand_base(5)}}" 或 "{{replace(user, '.', '')}}"）
     * @param target 目标URL
     * @param variables 当前变量映射
     * @return 计算结果
     */
    private String evaluateDslExpression(String expression, String target, Map<String, Object> variables) {
        if (expression == null || expression.isEmpty()) {
            return expression;
        }

        // 使用 DslEvaluatorRefactored 计算表达式
        try {
            // 构建 context，将 Map<String, Object> 直接使用
            Map<String, Object> context = new HashMap<>();
            if (variables != null) {
                context.putAll(variables);
            }

            // 添加目标URL相关的内置变量到 context（如果还没有的话）
            if (!context.containsKey("BaseURL") && target != null) {
                try {
                    URL url = new URL(target.startsWith("http") ? target : "http://" + target);
                    context.put("BaseURL", url.getProtocol() + "://" + url.getHost());
                    context.put("Hostname", url.getHost());
                } catch (Exception ignored) {
                }
            }

            // 调用 DslEvaluatorRefactored.evaluateFunctionForValue
            String result = DslEvaluatorRefactored.evaluateFunctionForValue(expression, context);

            return result != null ? result : expression;
        } catch (Exception e) {
            // 如果 DslEvaluatorRefactored 失败，直接返回原值
            System.err.println("[警告] DSL 函数计算失败: " + expression + " - " + e.getMessage());
            return expression;
        }
    }

    /**
     * 创建只包含单个raw块的临时PocStep
     * 用于多块raw请求的逐块执行
     *
     * @param originalStep 原始步骤（包含多个raw块）
     * @param rawIndex 要提取的raw块索引
     * @return 只包含指定raw块的新PocStep
     */
    private PocObj.PocStep createSingleRawStep(PocObj.PocStep originalStep, int rawIndex) {
        PocObj.PocStep singleRawStep = new PocObj.PocStep();

        // 复制基本属性
        singleRawStep.setStepId(originalStep.getStepId() + "_" + (rawIndex + 1));
        singleRawStep.setFollowRedirect(originalStep.isFollowRedirect());
        singleRawStep.setUnsafe(originalStep.isUnsafe());
        singleRawStep.setDisableCookie(originalStep.isDisableCookie());
        singleRawStep.setDisablePathAutomerge(originalStep.isDisablePathAutomerge());
        singleRawStep.setTimeout(originalStep.getTimeout());
        singleRawStep.setRetries(originalStep.getRetries());
        singleRawStep.setProxy(originalStep.getProxy());

        // 只设置当前这一个raw块
        List<String> singleRaw = new ArrayList<>();
        singleRaw.add(originalStep.getRaw().get(rawIndex));
        singleRawStep.setRaw(singleRaw);

        // 注意：不复制matchers和extractors，这些会在所有块执行完后统一处理

        return singleRawStep;
    }

    /**
     * 检测 POC 实际使用的变量
     * 遍历所有步骤，提取 {{varName}} 格式的变量引用
     *
     * @param steps POC步骤列表
     * @return 实际使用的变量名集合
     */
    private Set<String> detectUsedVariables(List<PocObj.PocStep> steps) {
        Set<String> usedVariables = new HashSet<>();
        if (steps == null || steps.isEmpty()) {
            return usedVariables;
        }

        Pattern varPattern = Pattern.compile("\\{\\{([\\w-]+)\\}\\}");

        for (PocObj.PocStep step : steps) {
            if (step == null) {
                continue;
            }

            // 检测不同协议类型的变量使用
            if (step instanceof PocObj.DnsStep) {
                // DNS 协议
                PocObj.DnsStep dnsStep = (PocObj.DnsStep) step;
                extractVariableNames(dnsStep.getDomain(), varPattern, usedVariables);
                extractVariableNames(dnsStep.getResolver(), varPattern, usedVariables);

            } else if (step instanceof PocObj.WebSocketStep) {
                // WebSocket 协议
                PocObj.WebSocketStep wsStep = (PocObj.WebSocketStep) step;
                extractVariableNames(wsStep.getAddress(), varPattern, usedVariables);
                if (wsStep.getMessages() != null) {
                    for (String message : wsStep.getMessages()) {
                        extractVariableNames(message, varPattern, usedVariables);
                    }
                }
                if (wsStep.getHeaders() != null) {
                    for (String headerValue : wsStep.getHeaders().values()) {
                        extractVariableNames(headerValue, varPattern, usedVariables);
                    }
                }

            } else if (step instanceof PocObj.SslStep) {
                // SSL/TLS 协议
                PocObj.SslStep sslStep = (PocObj.SslStep) step;
                extractVariableNames(sslStep.getAddress(), varPattern, usedVariables);

            } else if (step instanceof PocObj.FileStep) {
                // File 协议
                PocObj.FileStep fileStep = (PocObj.FileStep) step;
                if (fileStep.getPaths() != null) {
                    for (String path : fileStep.getPaths()) {
                        extractVariableNames(path, varPattern, usedVariables);
                    }
                }

            } else if (step instanceof PocObj.HeadlessStep) {
                // Headless 协议
                PocObj.HeadlessStep headlessStep = (PocObj.HeadlessStep) step;
                extractVariableNames(headlessStep.getUrl(), varPattern, usedVariables);
                if (headlessStep.getActions() != null) {
                    for (PocObj.BrowserAction action : headlessStep.getActions()) {
                        if (action.getArgs() != null) {
                            for (String argValue : action.getArgs().values()) {
                                extractVariableNames(argValue, varPattern, usedVariables);
                            }
                        }
                    }
                }

            } else if (step instanceof PocObj.CodeStep) {
                // Code 协议
                PocObj.CodeStep codeStep = (PocObj.CodeStep) step;
                extractVariableNames(codeStep.getSource(), varPattern, usedVariables);

            } else if (step instanceof PocObj.TcpStep) {
                // TCP 协议
                PocObj.TcpStep tcpStep = (PocObj.TcpStep) step;
                extractVariableNames(tcpStep.getHost(), varPattern, usedVariables);
                extractVariableNames(tcpStep.getPort(), varPattern, usedVariables);
                if (tcpStep.getInputs() != null) {
                    for (PocObj.Input input : tcpStep.getInputs()) {
                        extractVariableNames(input.getData(), varPattern, usedVariables);
                    }
                }

            } else {
                // HTTP 协议（包括普通 PocStep）
                // 检测 path
                extractVariableNames(step.getPath(), varPattern, usedVariables);

                // 检测 body
                extractVariableNames(step.getBody(), varPattern, usedVariables);

                // 检测 headers
                if (step.getHeaders() != null) {
                    for (Map.Entry<String, String> header : step.getHeaders().entrySet()) {
                        extractVariableNames(header.getKey(), varPattern, usedVariables);
                        extractVariableNames(header.getValue(), varPattern, usedVariables);
                    }
                }

                // 检测 raw 请求
                if (step.getRaw() != null) {
                    for (String rawBlock : step.getRaw()) {
                        extractVariableNames(rawBlock, varPattern, usedVariables);
                    }
                }

                // 检测 output（Xray POC 的输出变量定义）
                if (step.getOutput() != null) {
                    for (Object outputValue : step.getOutput().values()) {
                        if (outputValue != null) {
                            extractVariableNames(outputValue.toString(), varPattern, usedVariables);
                        }
                    }
                }
            }
        }

        return usedVariables;
    }

    /**
     * 从字符串中提取变量名
     *
     * @param input 输入字符串
     * @param pattern 正则表达式模式
     * @param usedVariables 用于收集变量名的集合
     */
    private void extractVariableNames(String input, Pattern pattern, Set<String> usedVariables) {
        if (input == null || input.isEmpty()) {
            return;
        }

        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            String varName = matcher.group(1); // 提取 {{varName}} 中的 varName
            if (varName != null && !varName.isEmpty()) {
                usedVariables.add(varName);
            }
        }
    }

    /**
     * 过滤未使用的变量
     * 只保留 POC 实际引用的变量
     *
     * @param allVariables 所有可用变量
     * @param usedVariables 实际使用的变量名集合
     * @return 过滤后的变量映射
     */
    private Map<String, List<String>> filterUnusedVariables(
        Map<String, List<String>> allVariables,
        Set<String> usedVariables
    ) {
        if (allVariables == null || allVariables.isEmpty()) {
            return new HashMap<>();
        }

        if (usedVariables == null || usedVariables.isEmpty()) {
            // 如果没有使用任何变量，返回空映射
            return new HashMap<>();
        }

        Map<String, List<String>> filteredVariables = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : allVariables.entrySet()) {
            String varName = entry.getKey();
            if (usedVariables.contains(varName)) {
                filteredVariables.put(varName, entry.getValue());
            }
        }

        return filteredVariables;
    }

    // ==================== HTTP 格式化方法 ====================

    /**
     * 格式化 HTTP 请求为标准格式
     *
     * 标准格式示例:
     * GET /path?query=value HTTP/1.1
     * Host: www.example.com
     * User-Agent: Mozilla/5.0
     * Content-Type: application/json
     *
     * {"key": "value"}
     */
    private String formatHttpRequest(RequestObj request) {
        StringBuilder sb = new StringBuilder();

        String url = request.getUrl();
        String method = request.getMethod() != null ? request.getMethod() : "GET";

        // 解析 URL 提取路径和 Host
        String path = "/";
        String host = "";
        String protocol = "HTTP/1.1";

        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            host = parsedUrl.getHost();
            if (parsedUrl.getPort() > 0 && parsedUrl.getPort() != 80 && parsedUrl.getPort() != 443) {
                host += ":" + parsedUrl.getPort();
            }
            path = parsedUrl.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }
            String query = parsedUrl.getQuery();
            if (query != null && !query.isEmpty()) {
                path += "?" + query;
            }
        } catch (Exception e) {
            // URL 解析失败，使用原始 URL
            path = url;
        }

        // 请求行
        sb.append(method).append(" ").append(path).append(" ").append(protocol).append("\r\n");

        // Host 头（如果请求头中没有则添加）
        Map<String, String> headers = request.getHeaders();
        boolean hasHost = false;
        if (headers != null) {
            for (String key : headers.keySet()) {
                if ("host".equalsIgnoreCase(key)) {
                    hasHost = true;
                    break;
                }
            }
        }
        if (!hasHost && !host.isEmpty()) {
            sb.append("Host: ").append(host).append("\r\n");
        }

        // 其他请求头
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                sb.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
            }
        }

        // 空行（分隔请求头和请求体）
        sb.append("\r\n");

        // 请求体
        byte[] postData = request.getPostData();
        if (postData != null && postData.length > 0) {
            sb.append(new String(postData, StandardCharsets.UTF_8));
        }

        return sb.toString();
    }

    /**
     * 格式化 HTTP 响应为标准格式
     *
     * 标准格式示例:
     * HTTP/1.1 200 OK
     * Content-Type: text/html; charset=utf-8
     * Server: nginx
     * Content-Length: 1234
     *
     * <!DOCTYPE html>...
     */
    private String formatHttpResponse(CustomHttpResponse response) {
        StringBuilder sb = new StringBuilder();

        // 状态行
        int statusCode = response.getResponseCode();
        String statusText = getHttpStatusText(statusCode);
        sb.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusText).append("\r\n");

        // 响应头（关键头）
        Map<String, List<String>> headers = response.getHeaderFields();
        if (headers != null) {
            // 优先显示的头
            String[] priorityHeaders = {"Content-Type", "Server", "Content-Length", "Set-Cookie", "Location", "X-Powered-By"};
            for (String headerName : priorityHeaders) {
                for (Map.Entry<String, List<String>> header : headers.entrySet()) {
                    String key = header.getKey();
                    if (key != null && key.equalsIgnoreCase(headerName)) {
                        sb.append(key).append(": ").append(String.join(", ", header.getValue())).append("\r\n");
                        break;
                    }
                }
            }
        }

        // 空行（分隔响应头和响应体）
        sb.append("\r\n");

        // 响应体（智能截取）
        String responseBody = response.getTextStr();
        if (responseBody != null && !responseBody.isEmpty()) {
            int maxLength = 4000; // 增加到 4000 字符
            if (responseBody.length() <= maxLength) {
                sb.append(responseBody);
            } else {
                // 智能截取：保留头部和尾部
                int headLength = maxLength * 3 / 4; // 75% 头部
                int tailLength = maxLength / 4;     // 25% 尾部
                sb.append(responseBody.substring(0, headLength));
                sb.append("\r\n\r\n... [truncated ").append(responseBody.length() - maxLength).append(" chars] ...\r\n\r\n");
                sb.append(responseBody.substring(responseBody.length() - tailLength));
            }
        }

        return sb.toString();
    }

    /**
     * 获取 HTTP 状态码对应的文本
     */
    private String getHttpStatusText(int statusCode) {
        switch (statusCode) {
            case 200: return "OK";
            case 201: return "Created";
            case 204: return "No Content";
            case 301: return "Moved Permanently";
            case 302: return "Found";
            case 304: return "Not Modified";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 500: return "Internal Server Error";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            default: return "Unknown";
        }
    }

    /**
     * 关闭执行器
     */
    public void shutdown() {
        // 清理资源
    }
}

