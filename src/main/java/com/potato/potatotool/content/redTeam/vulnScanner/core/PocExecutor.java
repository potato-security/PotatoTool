package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.google.gson.Gson;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.exception.NetworkException;
import com.potato.potatotool.content.redTeam.vulnScanner.exception.ScanExecutionException;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.VariableExtractor;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslContextBuilder;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslLogContext;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.ReverseObject;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor.XrayCelEvaluator;
import com.potato.potatotool.content.redTeam.vulnScanner.http.*;
import com.potato.potatotool.content.redTeam.vulnScanner.matchers.ResponseMatcher;
import com.potato.potatotool.content.redTeam.vulnScanner.matchers.SimpleMatcher;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.model.StepExecutionRecord;
import com.potato.potatotool.content.redTeam.vulnScanner.config.VulnScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.util.ConnectionPoolManager;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PayloadCombiner;
import com.potato.potatotool.content.redTeam.vulnScanner.util.RegexCompat;
import com.potato.potatotool.content.redTeam.vulnScanner.util.RuntimeExpressionEvaluator;
import com.potato.potatotool.content.redTeam.vulnScanner.util.ScanLogger;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.BufferedReader;
import java.net.ConnectException;
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
import java.util.concurrent.atomic.AtomicLong;
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

    private static final Gson GSON = new Gson();
    private static final String DNSLOG_UNAVAILABLE_SKIPPED = "DNSLOG_UNAVAILABLE_SKIPPED";
    private static final String OUTBOUND_RESTRICTED_SKIPPED = "OUTBOUND_RESTRICTED_SKIPPED";
    private static final String GOBY_LAST_BODY_KEY = "__goby_last_body";
    private static final String GOBY_LAST_HEADER_KEY = "__goby_last_header";
    private static final String GOBY_LAST_STATUS_KEY = "__goby_last_status";
    private static final List<PocObj.MatcherType> NON_HTTP_ALLOWED_MATCHERS = Arrays.asList(
            PocObj.MatcherType.WORD,
            PocObj.MatcherType.REGEX,
            PocObj.MatcherType.BINARY,
            PocObj.MatcherType.DSL,
            PocObj.MatcherType.GROUP
    );
    private static final List<PocObj.MatcherType> HEADLESS_ALLOWED_MATCHERS = Arrays.asList(
            PocObj.MatcherType.STATUS,
            PocObj.MatcherType.WORD,
            PocObj.MatcherType.REGEX,
            PocObj.MatcherType.BINARY,
            PocObj.MatcherType.DSL,
            PocObj.MatcherType.XPATH,
            PocObj.MatcherType.JSON,
            PocObj.MatcherType.KVAL,
            PocObj.MatcherType.GROUP
    );

    private final ScanConfig scanConfig;
    private final ConnectionPoolManager connectionPool;
    private final ResponseCache responseCache;
    private final ResponseCacheService responseCacheService;
    private final RequestSignatureService requestSignatureService;
    private final AtomicLong nextRequestPermitTimeMs = new AtomicLong(0L);

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
    private ThreadLocal<List<Map<String, Object>>> semanticWarnings = ThreadLocal.withInitial(ArrayList::new);
    private ThreadLocal<Map<String, String>> cookieJar = ThreadLocal.withInitial(LinkedHashMap::new);
    private ThreadLocal<List<String>> dnsLogManagementMockDomains = ThreadLocal.withInitial(ArrayList::new);
    private ThreadLocal<String> flowExecutionMode = ThreadLocal.withInitial(() -> "stepsCondition");
    private ThreadLocal<Map<String, Object>> lastStepTemplateAliases = ThreadLocal.withInitial(LinkedHashMap::new);
    private ThreadLocal<String> currentTarget = new ThreadLocal<>();

    public PocExecutor(ScanConfig scanConfig) {
        this.scanConfig = scanConfig != null ? scanConfig : new ScanConfig();
        this.connectionPool = ConnectionPoolManager.getInstance();
        this.responseCache = new ResponseCache();
        this.responseCacheService = new ResponseCacheService(1000, this.scanConfig.getResponseCacheTtlMs());
        this.requestSignatureService = new RequestSignatureService();
        HttpLogService.setInteractionWaitSeconds(resolveOobWaitSeconds(this.scanConfig));
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
        currentTarget.set(target);
        responseCache.clear();
        if (scanConfig != null) {
            responseCacheService.setCacheTtlMs(scanConfig.getResponseCacheTtlMs());
            if (!scanConfig.isEnableResponseCache()) {
                responseCacheService.clear();
            }
            HttpLogService.setInteractionWaitSeconds(resolveOobWaitSeconds(scanConfig));
        }

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
        cookieJar.get().clear();
        dnsLogManagementMockDomains.get().clear();
        flowExecutionMode.set("stepsCondition");

        if (poc.getFlow() != null && !poc.getFlow().trim().isEmpty() && !isSimpleFlowExpression(poc.getFlow())) {
            if (!"nuclei".equalsIgnoreCase(poc.getOriginalFormat())) {
                addSemanticWarning(
                        "FLOW_EXECUTION_FALLBACK",
                        "P1",
                        poc.getProtocol(),
                        "flow",
                        poc.getFlow(),
                        "fallback",
                        "当前版本仍以兼容执行路径处理 flow，后续将切换到专用 flow 执行器"
                );
            }
        }

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
                allVariables.putAll(withNucleiDictionaryAliases(dictionaryVariables));
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
                Map<String, List<String>> mergedVariables = new HashMap<>(withNucleiDictionaryAliases(dictionaryVariables));

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

        // 将已使用变量扩展为依赖闭包，避免 md1 -> @@md5(r1) 这类上游变量被裁剪
        usedVariables = expandUsedVariableDependencies(pocVariables, usedVariables);

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
            if (poc.getFlow() != null && !poc.getFlow().trim().isEmpty()) {
                return executeStepsWithFlow(target, steps, globalConfig, extractedValues, poc.getFlow(), poc.getStepsCondition());
            }
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
                logDebug("PAYLOAD", "变量组合模式: batteringram (同步模式) - 共 " + combinations.size() + " 个组合");
                break;

            case pitchfork:
                // 配对模式：多个变量列表索引严格配对
                combinations = PayloadCombiner.generatePitchforkCombinations(pocVariables);
                logDebug("PAYLOAD", "变量组合模式: pitchfork (配对模式) - 共 " + combinations.size() + " 个组合");
                break;

            case clusterbomb:
                // 笛卡尔积模式：生成所有可能组合
                // 从配置读取最大组合数限制
                final int MAX_COMBINATIONS = VulnScanConfig.getInstance().getMaxPayloadCombinations();
                long totalCombinations = PayloadCombiner.countCombinations(pocVariables);

                if (totalCombinations > MAX_COMBINATIONS) {
                    logWarn("PAYLOAD", "Payload 组合数过多 (" + totalCombinations +
                            ")，已限制为 " + MAX_COMBINATIONS + " 个（可在配置中调整 maxPayloadCombinations）");
                }

                combinations = PayloadCombiner.generateCombinationsWithLimit(pocVariables, MAX_COMBINATIONS);
                logDebug("PAYLOAD", "变量组合模式: clusterbomb (笛卡尔积) - 共 " + combinations.size() + " 个组合");

                // 如果组合数超过 100，输出详细信息供排查
                if (scanConfig.isDebug() && combinations.size() > 100) {
                    logWarn("PAYLOAD", "POC [" + poc.getId() + "] 产生了 " + combinations.size() + " 个 payload 组合，以下是前 100 个组合");

                    int displayCount = Math.min(100, combinations.size());
                    for (int i = 0; i < displayCount; i++) {
                        Map<String, String> combination = combinations.get(i);
                        logDebug("PAYLOAD", "组合 #" + (i + 1) + ":");
                        for (Map.Entry<String, String> entry : combination.entrySet()) {
                            logDebug("PAYLOAD", "  " + entry.getKey() + ": " + entry.getValue());
                        }
                    }
                }
                break;

            default:
                // 默认使用 batteringram
                combinations = PayloadCombiner.generateBatteringRamCombinations(pocVariables);
                logDebug("PAYLOAD", "变量组合模式: batteringram (默认) - 共 " + combinations.size() + " 个组合");
                break;
        }

        // 逐个测试组合
        boolean anySuccess = false; // 记录是否有任何组合成功
        boolean continueOnMatch = poc.isContinueOnMatch(); // Xray continue_ 字段

        for (int i = 0; i < combinations.size(); i++) {
            Map<String, String> combination = combinations.get(i);

            // 转换组合变量为 Object Map，仅处理 @@expression 运行时表达式，保留 Goby 函数延迟到请求替换阶段执行
            Map<String, List<String>> combinationVariables = new HashMap<>();
            for (Map.Entry<String, String> entry : combination.entrySet()) {
                combinationVariables.put(entry.getKey(), Collections.singletonList(entry.getValue()));
            }
            Map<String, Object> objectCombination = RuntimeExpressionEvaluator.evaluateRuntimeExpressions(combinationVariables);

            try {
                // 使用当前组合执行所有步骤
                boolean success;
                if (poc.getFlow() != null && !poc.getFlow().trim().isEmpty()) {
                    success = executeStepsWithFlow(target, steps, globalConfig, objectCombination, poc.getFlow(), poc.getStepsCondition());
                } else {
                    success = executeStepsWithVariables(target, steps, globalConfig, objectCombination, poc.getStepsCondition());
                }

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

    private Map<String, List<String>> withNucleiDictionaryAliases(Map<String, List<String>> source) {
        Map<String, List<String>> result = new HashMap<>();
        if (source != null) {
            result.putAll(source);
        }
        addSingleValueAlias(result, "username", "user");
        addSingleValueAlias(result, "password", "pass");
        return result;
    }

    private void addSingleValueAlias(Map<String, List<String>> variables, String alias, String sourceKey) {
        if (variables == null || variables.containsKey(alias)) {
            return;
        }
        List<String> values = variables.get(sourceKey);
        if (values != null && !values.isEmpty()) {
            variables.put(alias, Collections.singletonList(values.get(0)));
        }
    }

    private boolean executeStepsWithFlow(
        String target,
        List<PocObj.PocStep> steps,
        PocObj.GlobalConfig globalConfig,
        Map<String, Object> initialVariables,
        String flowExpression,
        PocObj.MatchersCondition fallbackCondition
    ) throws Exception {
        if (flowExpression == null || flowExpression.trim().isEmpty()) {
            return executeStepsWithVariables(target, steps, globalConfig, initialVariables, fallbackCondition);
        }

        String normalizedFlow = flowExpression.trim();
        if (isNucleiScriptFlow(normalizedFlow)) {
            try {
                boolean executed = executeNucleiScriptFlow(target, steps, globalConfig, initialVariables, normalizedFlow);
                flowExecutionMode.set("flow");
                return executed;
            } catch (UnsupportedOperationException e) {
                addSemanticWarning(
                        "FLOW_SCRIPT_PARTIAL_FALLBACK",
                        "P1",
                        "nuclei",
                        "flow",
                        normalizedFlow,
                        "fallback",
                        "flow 脚本包含当前未实现语义，已回退到兼容执行路径: " + e.getMessage()
                );
            }
        }

        if (!isSimpleFlowExpression(normalizedFlow)) {
            addSemanticWarning(
                    "FLOW_COMPLEX_FALLBACK",
                    "P1",
                    "mixed",
                    "flow",
                    normalizedFlow,
                    "fallback",
                    "复杂 flow 表达式暂按 stepsCondition 兼容执行"
            );
            flowExecutionMode.set("flow-fallback");
            return executeStepsWithVariables(target, steps, globalConfig, initialVariables, fallbackCondition);
        }

        Map<String, PocObj.PocStep> stepMap = new HashMap<>();
        for (PocObj.PocStep step : steps) {
            if (step != null && step.getStepId() != null) {
                stepMap.put(step.getStepId().toLowerCase(), step);
            }
        }

        if (normalizedFlow.contains("||")) {
            String[] orGroups = normalizedFlow.split("\\|\\|");
            for (String group : orGroups) {
                Map<String, Object> groupVariables = new HashMap<>(initialVariables);
                String[] andParts = group.split("&&");
                boolean groupMatched = true;
                boolean groupProved = false;

                for (String part : andParts) {
                    String token = normalizeFlowToken(part);
                    PocObj.PocStep step = stepMap.get(token);
                    if (step == null) {
                        addSemanticWarning("FLOW_STEP_NOT_FOUND", "P1", "mixed", "flow",
                                token, "skip", "flow 中引用的步骤不存在");
                        groupMatched = false;
                        break;
                    }
                    NucleiStepResult stepResult = executeStepDetailed(target, step, globalConfig, groupVariables,
                            resolveStepIndex(steps, step));
                    if (!stepResult.canContinueInAndChain()) {
                        groupMatched = false;
                        break;
                    }
                    if (stepResult.matched) {
                        groupProved = true;
                    }
                }

                if (groupMatched && groupProved) {
                    flowExecutionMode.set("flow");
                    return true;
                }
            }
            flowExecutionMode.set("flow");
            return false;
        }

        Map<String, Object> extractedValues = new HashMap<>(initialVariables);
        String[] andParts = normalizedFlow.split("&&");
        boolean proved = false;
        for (String part : andParts) {
            String token = normalizeFlowToken(part);
            PocObj.PocStep step = stepMap.get(token);
            if (step == null) {
                addSemanticWarning("FLOW_STEP_NOT_FOUND", "P1", "mixed", "flow",
                        token, "skip", "flow 中引用的步骤不存在");
                return false;
            }
            NucleiStepResult stepResult = executeStepDetailed(target, step, globalConfig, extractedValues,
                    resolveStepIndex(steps, step));
            if (!stepResult.canContinueInAndChain()) {
                return false;
            }
            if (stepResult.matched) {
                proved = true;
            }
        }
        flowExecutionMode.set("flow");
        return proved;
    }

    private boolean isNucleiScriptFlow(String flowExpression) {
        if (flowExpression == null || flowExpression.trim().isEmpty()) {
            return false;
        }
        String normalized = flowExpression.trim();
        return normalized.contains("\n")
                || normalized.contains(";")
                || normalized.contains("if")
                || normalized.contains("for")
                || normalized.contains("set(")
                || normalized.contains("iterate(")
                || normalized.contains("template.")
                || normalized.contains("template[")
                || normalized.contains("hasOwnProperty");
    }

    private boolean executeNucleiScriptFlow(
            String target,
            List<PocObj.PocStep> steps,
            PocObj.GlobalConfig globalConfig,
            Map<String, Object> initialVariables,
            String flowExpression
    ) throws Exception {
        if (steps == null || steps.isEmpty()) {
            return false;
        }

        final Map<String, Object> runtimeVariables = new LinkedHashMap<>();
        if (initialVariables != null) {
            runtimeVariables.putAll(initialVariables);
        }

        Context jsContext = null;
        try {
            jsContext = Context.newBuilder("js")
                    .allowAllAccess(false)
                    .allowIO(false)
                    .allowHostAccess(HostAccess.NONE)
                    .allowHostClassLookup(className -> false)
                    .allowCreateThread(false)
                    .allowCreateProcess(false)
                    .allowNativeAccess(false)
                    .allowPolyglotAccess(PolyglotAccess.NONE)
                    .option("engine.WarnInterpreterOnly", "false")
                    .build();

            Value bindings = jsContext.getBindings("js");
            final Context flowContext = jsContext;

            bindings.putMember("set", (ProxyExecutable) arguments -> {
                if (arguments == null || arguments.length < 2) {
                    return null;
                }
                Object keyObj = toJavaObject(arguments[0]);
                if (keyObj == null) {
                    return null;
                }
                String key = String.valueOf(keyObj);
                Object value = toJavaObject(arguments[1]);
                runtimeVariables.put(key, value);
                refreshNucleiTemplateObject(flowContext, runtimeVariables);
                return null;
            });

            bindings.putMember("iterate", (ProxyExecutable) arguments -> {
                List<Object> values = new ArrayList<>();
                if (arguments != null) {
                    for (Value argument : arguments) {
                        appendIterateValues(values, toJavaObject(argument));
                    }
                }
                return toJsonJsValue(flowContext, values);
            });

            bindings.putMember("log", (ProxyExecutable) arguments -> {
                if (arguments != null && arguments.length > 0) {
                    System.out.println("[Nuclei Flow] " + String.valueOf(toJavaObject(arguments[0])));
                    if (arguments.length == 1) {
                        return arguments[0];
                    }
                }
                return null;
            });

            registerNucleiProtocolFunctions(flowContext, target, steps, globalConfig, runtimeVariables);
            refreshNucleiTemplateObject(flowContext, runtimeVariables);

            Value result = flowContext.eval("js", flowExpression);
            Object exported = toJavaObject(result);
            if (exported instanceof Boolean) {
                return (Boolean) exported;
            }
            return exported != null;
        } catch (PolyglotException e) {
            throw new UnsupportedOperationException(e.getMessage(), e);
        } finally {
            if (jsContext != null) {
                jsContext.close();
            }
        }
    }

    private boolean isSimpleFlowExpression(String flowExpression) {
        if (flowExpression == null || flowExpression.trim().isEmpty()) {
            return true;
        }
        String flow = flowExpression.trim();
        return flow.matches("(?i)\\s*[a-z]+_?\\d+\\s*([&]{2}|[|]{2})\\s*[a-z]+_?\\d+(\\s*([&]{2}|[|]{2})\\s*[a-z]+_?\\d+)*\\s*")
                || flow.matches("(?i)\\s*[a-z]+\\(\\d+\\)\\s*([&]{2}|[|]{2})\\s*[a-z]+\\(\\d+\\)(\\s*([&]{2}|[|]{2})\\s*[a-z]+\\(\\d+\\))*\\s*");
    }

    private String normalizeFlowToken(String token) {
        if (token == null) {
            return "";
        }
        String normalized = token.trim().toLowerCase();
        if (normalized.contains("(") && normalized.endsWith(")")) {
            int left = normalized.indexOf('(');
            int right = normalized.lastIndexOf(')');
            if (left > 0 && right > left + 1) {
                String prefix = normalized.substring(0, left);
                String idx = normalized.substring(left + 1, right);
                return prefix + "_" + idx;
            }
        }
        return normalized;
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
        Map<String, Object> extractedValues = materializeGobyRuntimeVariables(initialVariables);

        // OR 条件：任一步骤匹配即可
        boolean isOrCondition = (stepsCondition == PocObj.MatchersCondition.OR);

        // 对于 OR 条件，单独处理每个步骤
        if (isOrCondition) {
            for (int i = 0; i < steps.size(); i++) {
                PocObj.PocStep step = steps.get(i);
                if (step == null) {
                    continue;
                }
                Map<String, Object> stepVariables = new HashMap<>(extractedValues);
                try {
                    NucleiStepResult stepResult = executeStepDetailed(target, step, globalConfig, stepVariables, i + 1);
                    if (stepResult.matched) {
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
        boolean proved = false;
        for (int i = 0; i < steps.size(); i++) {
            PocObj.PocStep step = steps.get(i);
            if (step == null) {
                continue; // 跳过空步骤
            }

            int stepIndex = i + 1;
            try {
                NucleiStepResult result = executeStepDetailed(target, step, globalConfig, extractedValues, stepIndex);
                if (!result.canContinueInAndChain()) {
                    return false;
                }
                if (result.matched) {
                    proved = true;
                }
            } catch (SocketTimeoutException e) {
                throw new NetworkException(NetworkException.NetworkErrorType.READ_TIMEOUT,
                    "请求超时: " + target, e);
            } catch (ConnectException e) {
                throw new NetworkException(NetworkException.NetworkErrorType.CONNECTION_REFUSED,
                    "连接被拒绝: " + target, e);
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

        return proved;
    }

    private void registerNucleiProtocolFunctions(Context jsContext,
                                                 String target,
                                                 List<PocObj.PocStep> steps,
                                                 PocObj.GlobalConfig globalConfig,
                                                 Map<String, Object> runtimeVariables) {
        if (jsContext == null || steps == null) {
            return;
        }

        Map<String, List<PocObj.PocStep>> stepsByProtocol = new LinkedHashMap<>();
        Map<String, Map<String, PocObj.PocStep>> stepsByProtocolAndId = new LinkedHashMap<>();
        Map<String, Map<Integer, PocObj.PocStep>> stepsByProtocolAndIndex = new LinkedHashMap<>();

        for (int i = 0; i < steps.size(); i++) {
            PocObj.PocStep step = steps.get(i);
            if (step == null) {
                continue;
            }
            String protocol = inferProtocolName(step);
            List<PocObj.PocStep> protocolSteps = stepsByProtocol.get(protocol);
            if (protocolSteps == null) {
                protocolSteps = new ArrayList<>();
                stepsByProtocol.put(protocol, protocolSteps);
            }
            protocolSteps.add(step);

            Map<Integer, PocObj.PocStep> indexMap = stepsByProtocolAndIndex.get(protocol);
            if (indexMap == null) {
                indexMap = new LinkedHashMap<>();
                stepsByProtocolAndIndex.put(protocol, indexMap);
            }
            indexMap.put(indexMap.size() + 1, step);

            Map<String, PocObj.PocStep> idMap = stepsByProtocolAndId.get(protocol);
            if (idMap == null) {
                idMap = new LinkedHashMap<>();
                stepsByProtocolAndId.put(protocol, idMap);
            }
            if (step.getStepId() != null) {
                idMap.put(step.getStepId().toLowerCase(Locale.ROOT), step);
            }
            Object nucleiId = step.getOutput() != null ? step.getOutput().get("_nuclei_id") : null;
            if (nucleiId != null) {
                idMap.put(String.valueOf(nucleiId).trim().toLowerCase(Locale.ROOT), step);
            }
        }

        Set<String> protocolNames = new LinkedHashSet<>(stepsByProtocol.keySet());
        if (protocolNames.contains("request")) {
            protocolNames.add("http");
            protocolNames.add("requests");
        }

        final Context finalContext = jsContext;
        for (String protocolName : protocolNames) {
            final String normalizedProtocol = protocolName;
            jsContext.getBindings("js").putMember(protocolName, (ProxyExecutable) arguments -> {
                String stepLookupProtocol = normalizeFlowProtocolName(normalizedProtocol);
                List<PocObj.PocStep> protocolSteps = resolveProtocolSteps(stepLookupProtocol, stepsByProtocol);
                if (protocolSteps == null || protocolSteps.isEmpty()) {
                    return false;
                }

                List<PocObj.PocStep> stepsToExecute = resolveFlowTargetSteps(
                        stepLookupProtocol,
                        arguments,
                        protocolSteps,
                        stepsByProtocolAndIndex,
                        stepsByProtocolAndId
                );

                boolean anyExecuted = false;
                boolean anyMatched = false;
                for (PocObj.PocStep step : stepsToExecute) {
                    if (step == null) {
                        continue;
                    }
                    anyExecuted = true;
                    NucleiStepResult result;
                    try {
                        result = executeNucleiFlowStep(target, step, globalConfig, runtimeVariables,
                                resolveStepIndex(steps, step));
                    } catch (Exception e) {
                        throw new IllegalStateException(e.getMessage(), e);
                    }
                    refreshNucleiTemplateObject(finalContext, runtimeVariables);
                    if (result.matched) {
                        anyMatched = true;
                    }
                }
                return anyExecuted && anyMatched;
            });
        }
    }

    private List<PocObj.PocStep> resolveProtocolSteps(String protocol,
                                                      Map<String, List<PocObj.PocStep>> stepsByProtocol) {
        List<PocObj.PocStep> steps = stepsByProtocol.get(protocol);
        if ((steps == null || steps.isEmpty()) && "http".equals(protocol)) {
            steps = stepsByProtocol.get("request");
        }
        return steps;
    }

    private List<PocObj.PocStep> resolveFlowTargetSteps(String protocol,
                                                        Value[] arguments,
                                                        List<PocObj.PocStep> protocolSteps,
                                                        Map<String, Map<Integer, PocObj.PocStep>> stepsByProtocolAndIndex,
                                                        Map<String, Map<String, PocObj.PocStep>> stepsByProtocolAndId) {
        List<PocObj.PocStep> result = new ArrayList<>();
        if (arguments == null || arguments.length == 0) {
            result.addAll(protocolSteps);
            return result;
        }

        Map<Integer, PocObj.PocStep> indexMap = stepsByProtocolAndIndex.get(protocol);
        if (indexMap == null && "http".equals(protocol)) {
            indexMap = stepsByProtocolAndIndex.get("request");
        }

        Map<String, PocObj.PocStep> idMap = stepsByProtocolAndId.get(protocol);
        if (idMap == null && "http".equals(protocol)) {
            idMap = stepsByProtocolAndId.get("request");
        }

        for (Value argument : arguments) {
            Object value = toJavaObject(argument);
            PocObj.PocStep step = null;
            if (value instanceof Number) {
                int index = ((Number) value).intValue();
                if (indexMap != null) {
                    step = indexMap.get(index);
                }
            } else if (value != null) {
                String key = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
                if (key.matches("\\d+") && indexMap != null) {
                    step = indexMap.get(Integer.parseInt(key));
                }
                if (step == null && idMap != null) {
                    step = idMap.get(key);
                    if (step == null && "http".equals(protocol) && key.startsWith("http_")) {
                        step = idMap.get("request_" + key.substring("http_".length()));
                    }
                }
            }
            if (step != null) {
                result.add(step);
            }
        }

        return result;
    }

    private String normalizeFlowProtocolName(String protocolName) {
        if (protocolName == null) {
            return "";
        }
        String normalized = protocolName.trim().toLowerCase(Locale.ROOT);
        if ("requests".equals(normalized)) {
            return "request";
        }
        return normalized;
    }

    private String inferProtocolName(PocObj.PocStep step) {
        if (step instanceof PocObj.DnsStep) {
            return "dns";
        }
        if (step instanceof PocObj.WebSocketStep) {
            return "websocket";
        }
        if (step instanceof PocObj.SslStep) {
            return "ssl";
        }
        if (step instanceof PocObj.FileStep) {
            return "file";
        }
        if (step instanceof PocObj.HeadlessStep) {
            return "headless";
        }
        if (step instanceof PocObj.TcpStep) {
            return "tcp";
        }
        if (step instanceof PocObj.CodeStep) {
            String protocolName = ((PocObj.CodeStep) step).getProtocolName();
            return protocolName == null || protocolName.trim().isEmpty()
                    ? "code"
                    : protocolName.trim().toLowerCase(Locale.ROOT);
        }
        if (step.getStepId() != null) {
            String stepId = step.getStepId().toLowerCase(Locale.ROOT);
            int idx = stepId.indexOf('_');
            if (idx > 0) {
                return stepId.substring(0, idx);
            }
        }
        return "http";
    }

    private void refreshNucleiTemplateObject(Context jsContext, Map<String, Object> runtimeVariables) {
        if (jsContext == null) {
            return;
        }
        Map<String, Object> templateContext = buildNucleiTemplateContext(runtimeVariables);
        String json = GSON.toJson(templateContext);
        jsContext.getBindings("js").putMember("template", jsContext.eval("js", "(" + json + ")"));
    }

    private Map<String, Object> buildNucleiTemplateContext(Map<String, Object> runtimeVariables) {
        Map<String, Object> templateContext = new LinkedHashMap<>();
        if (runtimeVariables != null) {
            templateContext.putAll(runtimeVariables);
        }
        Map<String, Object> aliases = lastStepTemplateAliases.get();
        if (aliases != null && !aliases.isEmpty()) {
            templateContext.putAll(aliases);
        }
        return templateContext;
    }

    private void appendIterateValues(List<Object> values, Object candidate) {
        if (candidate == null) {
            return;
        }
        if (candidate instanceof List) {
            for (Object item : (List<?>) candidate) {
                values.add(item);
            }
            return;
        }
        if (candidate.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(candidate);
            for (int i = 0; i < length; i++) {
                values.add(java.lang.reflect.Array.get(candidate, i));
            }
            return;
        }
        values.add(candidate);
    }

    private Object toJsonJsValue(Context jsContext, Object object) {
        return jsContext.eval("js", "(" + GSON.toJson(object) + ")");
    }

    private Object toJavaObject(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isString()) {
            return value.asString();
        }
        if (value.isNumber()) {
            if (value.fitsInInt()) {
                return value.asInt();
            }
            if (value.fitsInLong()) {
                return value.asLong();
            }
            if (value.fitsInDouble()) {
                return value.asDouble();
            }
        }
        if (value.hasArrayElements()) {
            List<Object> list = new ArrayList<>();
            long size = value.getArraySize();
            for (long i = 0; i < size; i++) {
                list.add(toJavaObject(value.getArrayElement(i)));
            }
            return list;
        }
        if (value.hasMembers()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (String key : value.getMemberKeys()) {
                map.put(key, toJavaObject(value.getMember(key)));
            }
            return map;
        }
        return value.toString();
    }

    /**
     * 执行单个步骤并返回结果
     * 用于 OR 条件的步骤执行，支持所有协议类型
     */
    private boolean executeAndStep(String target, PocObj.PocStep step,
                                   PocObj.GlobalConfig globalConfig,
                                   Map<String, Object> extractedValues,
                                   int stepIndex) throws Exception {
        return executeStepDetailed(target, step, globalConfig, extractedValues, stepIndex).matched;
    }

    private NucleiStepResult executeNucleiFlowStep(String target, PocObj.PocStep step,
                                                   PocObj.GlobalConfig globalConfig,
                                                   Map<String, Object> extractedValues,
                                                   int stepIndex) throws Exception {
        NucleiStepResult result = executeStepDetailed(target, step, globalConfig, extractedValues, stepIndex);
        boolean flowMatched = result.matched || result.hasExtractorOutput || result.executedWithoutOperators();
        return new NucleiStepResult(flowMatched, result.hasExtractorOutput, result.hasOperators);
    }

    private NucleiStepResult executeStepDetailed(String target, PocObj.PocStep step,
                                                 PocObj.GlobalConfig globalConfig,
                                                 Map<String, Object> extractedValues,
                                                 int stepIndex) throws Exception {
        if (shouldSkipReverseWaitStep(step, extractedValues)) {
            markDnsStepSkipped(stepIndex, step, DNSLOG_UNAVAILABLE_SKIPPED);
            return new NucleiStepResult(false, false, hasStepOperators(step), false);
        }

        // 根据步骤类型选择执行方式
        if (step instanceof PocObj.DnsStep) {
            boolean executed = executeDnsStep((PocObj.DnsStep) step, extractedValues, stepIndex);
            boolean matched = hasStepMatchers(step) && executed;
            return new NucleiStepResult(matched, hasExtractorValues(step, extractedValues), hasStepOperators(step), executed);
        } else if (step instanceof PocObj.WebSocketStep) {
            boolean executed = executeWebSocketStep((PocObj.WebSocketStep) step, extractedValues);
            boolean matched = hasStepMatchers(step) && executed;
            return new NucleiStepResult(matched, hasExtractorValues(step, extractedValues), hasStepOperators(step), executed);
        } else if (step instanceof PocObj.SslStep) {
            boolean executed = executeSslStep(target, (PocObj.SslStep) step, extractedValues);
            boolean matched = hasStepMatchers(step) && executed;
            return new NucleiStepResult(matched, hasExtractorValues(step, extractedValues), hasStepOperators(step), executed);
        } else if (step instanceof PocObj.FileStep) {
            boolean executed = executeFileStep(target, (PocObj.FileStep) step, extractedValues);
            boolean matched = hasStepMatchers(step) && executed;
            return new NucleiStepResult(matched, hasExtractorValues(step, extractedValues), hasStepOperators(step), executed);
        } else if (step instanceof PocObj.HeadlessStep) {
            boolean executed = executeHeadlessStep((PocObj.HeadlessStep) step, extractedValues, globalConfig);
            boolean matched = hasStepMatchers(step) && executed;
            updateTemplateAliasesForStep(step, null, null, extractedValues);
            return new NucleiStepResult(matched, hasExtractorValues(step, extractedValues), hasStepOperators(step), executed);
        } else if (step instanceof PocObj.TcpStep) {
            boolean executed = executeTcpStep(target, (PocObj.TcpStep) step, extractedValues);
            boolean matched = hasStepMatchers(step) && executed;
            return new NucleiStepResult(matched, hasExtractorValues(step, extractedValues), hasStepOperators(step), executed);
        } else if (step instanceof PocObj.CodeStep) {
            boolean executed = executeCodeStep((PocObj.CodeStep) step, extractedValues);
            boolean matched = hasStepMatchers(step) && executed;
            updateTemplateAliasesForCodeStep((PocObj.CodeStep) step, extractedValues);
            return new NucleiStepResult(matched, hasExtractorValues(step, extractedValues), hasStepOperators(step), executed);
        }

        String baseCacheKey = step.getStepId() != null ? step.getStepId() : "step_" + (stepIndex - 1);

        // 检查是否为多块raw请求（Nuclei多请求场景）
        if (step.getRaw() != null && step.getRaw().size() > 1) {
            if (step.isIterateAll()) {
                return executeIterateAllRawStep(target, step, globalConfig, extractedValues, stepIndex, baseCacheKey);
            }

            // 多块raw请求 - 依次执行所有块并收集所有响应
            List<CustomHttpResponse> allResponses = new ArrayList<>();
            boolean hasExtractorOutput = false;
            boolean hasNamedExtractors = hasNamedExtractors(step);
            boolean matched = false;

            for (int rawIndex = 0; rawIndex < step.getRaw().size(); rawIndex++) {
                // 创建单块请求
                PocObj.PocStep singleRawStep = createSingleRawStep(step, rawIndex);
                RequestObj requestObj = createRequest(target, singleRawStep, globalConfig, extractedValues);
                applyCookieReuse(requestObj, globalConfig, singleRawStep);

                long requestTime = System.currentTimeMillis();
                CustomHttpResponse response;
                long responseTime;
                try {
                    // 发送请求
                    response = executeHttpRequest(requestObj);
                    responseTime = System.currentTimeMillis();
                } catch (Exception e) {
                    logWarn("HTTP", "raw块 " + (rawIndex + 1) + " 请求异常: " + e.getMessage());
                    continue;
                }

                if (response == null) {
                    logWarn("HTTP", "raw块 " + (rawIndex + 1) + " 请求失败");
                    continue;
                }

                // 设置响应时间
                response.setResponseTime(responseTime - requestTime);
                captureCookies(response, globalConfig, singleRawStep);
                updateGobyResponseContext(extractedValues, response);

                // 缓存响应（使用索引后缀）
                String cacheKey = baseCacheKey + "_" + (rawIndex + 1);
                responseCache.put(cacheKey, response, requestTime, responseTime);

                // 收集响应
                allResponses.add(response);

                // 提取变量（每个raw块都可能提取变量）
                if (step.getExtractors() != null && !step.getExtractors().isEmpty()) {
                    VariableExtractor.extractVariablesObj(
                            response,
                            selectExtractorsForRawIndex(step.getExtractors(), rawIndex + 1),
                            extractedValues,
                            true
                    );
                    hasExtractorOutput = hasExtractorValues(step, extractedValues);
                }
            }

            // 使用最后一个响应进行匹配（matcher会从responseCache中访问所有索引响应）
            if (!allResponses.isEmpty()) {
                CustomHttpResponse lastResponse = allResponses.get(allResponses.size() - 1);

                // 将所有响应添加到缓存，供DSL matcher访问索引变量（body_1, body_2等）
                responseCache.putMultipleResponses(baseCacheKey, allResponses);

                // Nuclei HTTP operators 语义：extractor 先于 matcher，且当前 step 提取值可被 matcher 读取。
                boolean matchersPassed = evaluateHttpMatchers(lastResponse, step, baseCacheKey, extractedValues);
                matched = hasStepMatchers(step) && matchersPassed;
                updateTemplateAliasesForStep(step, null, lastResponse, extractedValues);

                // 如果匹配成功，保存最后一个请求/响应数据用于报告生成
                if (matchersPassed) {
                    RequestObj lastRequestObj = createRequest(target, createSingleRawStep(step, allResponses.size() - 1),
                            globalConfig, extractedValues);
                    lastMatchedRequest.set(lastRequestObj);
                    lastMatchedResponse.set(lastResponse);
                    lastMatchedPath.set(lastRequestObj.getUrl());
                    String payload = extractPayloadFromVariables(extractedValues);
                    lastMatchedPayload.set(payload);
                }

                if (!matchersPassed && hasStepMatchers(step)) {
                    return new NucleiStepResult(false, hasExtractorOutput, hasStepOperators(step), false);
                }

                // 处理 output 提取（Xray POC）
                if (step.getOutput() != null && !step.getOutput().isEmpty()) {
                    RequestObj lastRequestObj = createRequest(target, createSingleRawStep(step, allResponses.size() - 1),
                            globalConfig, extractedValues);
                    extractOutputVariables(step, lastResponse, lastRequestObj, extractedValues);
                    extractedOutputData.get().putAll(extractedValues);
                }
            }

            // 所有 raw 块都执行失败时应判定当前步骤失败
            return new NucleiStepResult(matched, hasExtractorOutput, hasStepOperators(step),
                    !allResponses.isEmpty() && (!hasNamedExtractors || hasExtractorOutput || matched));
        }

        if (step.getPathCandidates() != null && step.getPathCandidates().size() > 1) {
            return executePathCandidateStep(target, step, globalConfig, extractedValues, stepIndex, baseCacheKey);
        }

        // 单块raw或普通请求
        RequestObj requestObj = createRequest(target, step, globalConfig, extractedValues);
        applyCookieReuse(requestObj, globalConfig, step);

        // 记录请求时间
        long requestTime = System.currentTimeMillis();

        // 发送请求
        try (CustomHttpResponse response = executeHttpRequest(requestObj)) {
            // 记录响应时间
            long responseTime = System.currentTimeMillis();

            // 检查响应是否为空
            if (response == null) {
                return new NucleiStepResult(false, false, hasStepOperators(step), false);
            }

            // 设置响应时间到 response 对象（用于时间盲注检测）
            response.setResponseTime(responseTime - requestTime);
            captureCookies(response, globalConfig, step);
            updateGobyResponseContext(extractedValues, response);

            // 缓存响应（用于diff操作）
            responseCache.put(baseCacheKey, response, requestTime, responseTime);

            boolean hasExtractorOutput = false;
            if (step.getExtractors() != null && !step.getExtractors().isEmpty()) {
                if (step.isIterateAll()) {
                    VariableExtractor.extractVariablesObjAll(response, step.getExtractors(), extractedValues, true);
                } else {
                    VariableExtractor.extractVariablesObj(response, step.getExtractors(), extractedValues, true);
                }
                hasExtractorOutput = hasExtractorValues(step, extractedValues);
            }

            // 匹配结果（传入responseCache以支持diff操作，传递stepId以支持索引变量）
            boolean matchersPassed = evaluateHttpMatchers(response, step, baseCacheKey, extractedValues);
            boolean matched = hasStepMatchers(step) && matchersPassed;
            updateTemplateAliasesForStep(step, requestObj, response, extractedValues);

            // ========== 记录步骤执行详情（用于增强报告） ==========
            StepExecutionRecord record = new StepExecutionRecord(stepIndex, baseCacheKey);
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
            if (matchersPassed) {
                lastMatchedRequest.set(requestObj);
                lastMatchedResponse.set(response);
                lastMatchedPath.set(requestObj.getUrl());
                String payload = extractPayloadFromVariables(extractedValues);
                lastMatchedPayload.set(payload);
            }

            // 如果匹配失败且步骤是必要的，则返回失败
            if (!matchersPassed && hasStepMatchers(step)) {
                return new NucleiStepResult(false, hasExtractorOutput, hasStepOperators(step), false);
            }

            // 处理 output 提取（Xray POC）
            if (step.getOutput() != null && !step.getOutput().isEmpty()) {
                extractOutputVariables(step, response, requestObj, extractedValues);
                extractedOutputData.get().putAll(extractedValues);
            }
            if (step.getExtractors() != null && !step.getExtractors().isEmpty()) {
                for (PocObj.Matcher extractor : step.getExtractors()) {
                    if (extractor.getName() != null && extractedValues.containsKey(extractor.getName())) {
                        record.addExtractedVariable(extractor.getName(), extractedValues.get(extractor.getName()));
                    }
                }
            }
            boolean hasNamedExtractors = hasNamedExtractors(step);
            return new NucleiStepResult(matched, hasExtractorOutput, hasStepOperators(step),
                    !hasNamedExtractors || hasExtractorOutput || matched);
        }
    }

    private NucleiStepResult executeIterateAllRawStep(String target, PocObj.PocStep step,
                                                      PocObj.GlobalConfig globalConfig,
                                                      Map<String, Object> extractedValues,
                                                      int stepIndex,
                                                      String baseCacheKey) throws Exception {
        List<RawIterationState> states = new ArrayList<>();
        states.add(new RawIterationState(new HashMap<>(extractedValues), new ArrayList<CustomHttpResponse>(), null));

        boolean anyResponse = false;
        boolean hasExtractorOutput = false;

        for (int rawIndex = 0; rawIndex < step.getRaw().size(); rawIndex++) {
            List<RawIterationState> nextStates = new ArrayList<>();

            for (RawIterationState state : states) {
                List<Map<String, Object>> variableContexts = expandIterableVariables(state.variables);
                for (Map<String, Object> requestVariables : variableContexts) {
                    PocObj.PocStep singleRawStep = createSingleRawStep(step, rawIndex);
                    RequestObj requestObj = createRequest(target, singleRawStep, globalConfig, requestVariables);
                    applyCookieReuse(requestObj, globalConfig, singleRawStep);

                    long requestTime = System.currentTimeMillis();
                    CustomHttpResponse response;
                    long responseTime;
                    try {
                        response = executeHttpRequest(requestObj);
                        responseTime = System.currentTimeMillis();
                    } catch (Exception e) {
                        logWarn("HTTP", "iterate-all raw块 " + (rawIndex + 1) + " 请求异常: " + e.getMessage());
                        continue;
                    }

                    if (response == null) {
                        continue;
                    }

                    anyResponse = true;
                    response.setResponseTime(responseTime - requestTime);
                    captureCookies(response, globalConfig, singleRawStep);

                    Map<String, Object> nextVariables = new HashMap<>(requestVariables);
                    List<CustomHttpResponse> nextResponses = new ArrayList<>(state.responses);
                    nextResponses.add(response);
                    updateGobyResponseContext(nextVariables, response);

                    StepExecutionRecord record = buildHttpStepRecord(
                            stepIndex,
                            baseCacheKey + "_" + (rawIndex + 1),
                            requestObj,
                            response,
                            responseTime - requestTime,
                            false
                    );
                    stepExecutionRecords.get().add(record);

                    if (step.getExtractors() != null && !step.getExtractors().isEmpty()) {
                        VariableExtractor.extractVariablesObjAll(
                                response,
                                selectExtractorsForRawIndex(step.getExtractors(), rawIndex + 1),
                                nextVariables,
                                true
                        );
                        hasExtractorOutput = hasExtractorValues(step, nextVariables);
                        for (PocObj.Matcher extractor : step.getExtractors()) {
                            if (extractor.getName() != null && nextVariables.containsKey(extractor.getName())) {
                                record.addExtractedVariable(extractor.getName(), nextVariables.get(extractor.getName()));
                            }
                        }
                    }

                    nextStates.add(new RawIterationState(nextVariables, nextResponses, requestObj));
                }
            }

            if (nextStates.isEmpty()) {
                return new NucleiStepResult(false, hasExtractorOutput, hasStepOperators(step), false);
            }
            states = nextStates;
        }

        boolean anyMatched = false;
        for (RawIterationState state : states) {
            if (state.responses.isEmpty()) {
                continue;
            }

            responseCache.putMultipleResponses(baseCacheKey, state.responses);
            CustomHttpResponse lastResponse = state.responses.get(state.responses.size() - 1);

            boolean matchersPassed = evaluateHttpMatchers(lastResponse, step, baseCacheKey, state.variables);
            boolean matched = hasStepMatchers(step) && matchersPassed;

            if (matchersPassed) {
                if (matched) {
                    anyMatched = true;
                }
                RequestObj lastRequestObj = state.lastRequest != null
                        ? state.lastRequest
                        : createRequest(target, createSingleRawStep(step, state.responses.size() - 1),
                                globalConfig, state.variables);
                lastMatchedRequest.set(lastRequestObj);
                lastMatchedResponse.set(lastResponse);
                lastMatchedPath.set(lastRequestObj.getUrl());
                lastMatchedPayload.set(extractPayloadFromVariables(state.variables));
                extractedValues.putAll(state.variables);
                updateTemplateAliasesForStep(step, lastRequestObj, lastResponse, state.variables);

                if (step.getOutput() != null && !step.getOutput().isEmpty()) {
                    extractOutputVariables(step, lastResponse, lastRequestObj, state.variables);
                    extractedOutputData.get().putAll(state.variables);
                }

                if (matched && shouldStopAtFirstMatch(globalConfig, step)) {
                    return new NucleiStepResult(true, hasExtractorOutput, hasStepOperators(step), true);
                }
            }
        }

        return new NucleiStepResult(anyMatched, hasExtractorOutput, hasStepOperators(step), anyResponse);
    }

    private NucleiStepResult executePathCandidateStep(String target, PocObj.PocStep step,
                                                      PocObj.GlobalConfig globalConfig,
                                                      Map<String, Object> extractedValues,
                                                      int stepIndex,
                                                      String baseCacheKey) throws Exception {
        boolean anyResponse = false;
        boolean anyMatched = false;
        boolean hasExtractorOutput = false;
        List<String> paths = step.getPathCandidates();

        for (int pathIndex = 0; pathIndex < paths.size(); pathIndex++) {
            PocObj.PocStep candidateStep = createPathCandidateStep(step, pathIndex);
            List<Map<String, Object>> variableContexts = step.isIterateAll()
                    ? expandIterableVariables(extractedValues)
                    : Collections.singletonList(extractedValues);

            for (Map<String, Object> requestVariables : variableContexts) {
                RequestObj requestObj = createRequest(target, candidateStep, globalConfig, requestVariables);
                applyCookieReuse(requestObj, globalConfig, candidateStep);

                long requestTime = System.currentTimeMillis();
                CustomHttpResponse response;
                long responseTime;
                try {
                    response = executeHttpRequest(requestObj);
                    responseTime = System.currentTimeMillis();
                } catch (Exception e) {
                    logWarn("HTTP", "path候选 " + (pathIndex + 1) + " 请求异常: " + e.getMessage());
                    continue;
                }

                if (response == null) {
                    continue;
                }

                anyResponse = true;
                response.setResponseTime(responseTime - requestTime);
                captureCookies(response, globalConfig, candidateStep);
                updateGobyResponseContext(requestVariables, response);

                String cacheKey = baseCacheKey + "_path_" + (pathIndex + 1);
                responseCache.put(cacheKey, response, requestTime, responseTime);
                responseCache.put(baseCacheKey, response, requestTime, responseTime);

                if (step.getExtractors() != null && !step.getExtractors().isEmpty()) {
                    if (step.isIterateAll()) {
                        VariableExtractor.extractVariablesObjAll(response, step.getExtractors(), extractedValues, true);
                    } else {
                        VariableExtractor.extractVariablesObj(response, step.getExtractors(), extractedValues, true);
                    }
                    hasExtractorOutput = hasExtractorValues(step, extractedValues);
                }

                boolean matchersPassed = evaluateHttpMatchers(response, step, baseCacheKey, requestVariables);
                boolean matched = hasStepMatchers(step) && matchersPassed;
                updateTemplateAliasesForStep(step, requestObj, response, requestVariables);

                StepExecutionRecord record = buildHttpStepRecord(stepIndex, cacheKey, requestObj, response,
                        responseTime - requestTime, matched);
                stepExecutionRecords.get().add(record);
                if (step.getExtractors() != null && !step.getExtractors().isEmpty()) {
                    for (PocObj.Matcher extractor : step.getExtractors()) {
                        if (extractor.getName() != null && extractedValues.containsKey(extractor.getName())) {
                            record.addExtractedVariable(extractor.getName(), extractedValues.get(extractor.getName()));
                        }
                    }
                }

                if (step.getOutput() != null && !step.getOutput().isEmpty()) {
                    extractOutputVariables(step, response, requestObj, requestVariables);
                    extractedOutputData.get().putAll(requestVariables);
                }

                if (matchersPassed) {
                    if (matched) {
                        anyMatched = true;
                    }
                    lastMatchedRequest.set(requestObj);
                    lastMatchedResponse.set(response);
                    lastMatchedPath.set(requestObj.getUrl());
                    lastMatchedPayload.set(extractPayloadFromVariables(requestVariables));

                    extractedValues.putAll(requestVariables);
                    if (matched && shouldStopAtFirstMatch(globalConfig, step)) {
                        return new NucleiStepResult(true, hasExtractorOutput, hasStepOperators(step), true);
                    }
                }
            }
        }

        return new NucleiStepResult(anyMatched, hasExtractorOutput, hasStepOperators(step), anyResponse);
    }

    private Map<String, Object> materializeGobyRuntimeVariables(Map<String, Object> initialVariables) {
        Map<String, Object> resolved = new LinkedHashMap<>();
        if (initialVariables == null || initialVariables.isEmpty()) {
            return resolved;
        }
        resolved.putAll(initialVariables);

        int maxIterations = 8;
        for (int i = 0; i < maxIterations; i++) {
            boolean changed = false;
            Map<String, String> stringVariables = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : resolved.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    stringVariables.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }

            for (Map.Entry<String, Object> entry : resolved.entrySet()) {
                Object currentValue = entry.getValue();
                if (!(currentValue instanceof String)) {
                    continue;
                }

                String value = (String) currentValue;
                if (hasUnresolvedVariableDependencies(entry.getKey(), value, stringVariables)) {
                    continue;
                }
                String processed = replaceResolvedVariablePlaceholders(value, stringVariables);
                processed = GobyFunctionProcessor.processGobyFunctions(processed, stringVariables);
                if (isLazyInteractshPlaceholder(processed)) {
                    processed = GobyFunctionProcessor.processGobyFunctions("@@httplog()", stringVariables);
                }
                if (!processed.equals(value)) {
                    entry.setValue(processed);
                    stringVariables.put(entry.getKey(), processed);
                    changed = true;
                }
            }

            if (!changed) {
                break;
            }
        }

        return resolved;
    }

    private boolean hasUnresolvedVariableDependencies(String variableName,
                                                      String value,
                                                      Map<String, String> variables) {
        if (value == null || value.isEmpty() || variables == null || variables.isEmpty()) {
            return false;
        }

        Set<String> dependencies = new HashSet<>();
        extractVariableNames(value, dependencies);
        if (dependencies.isEmpty()) {
            return false;
        }

        for (String dependency : dependencies) {
            if (dependency == null || dependency.isEmpty() || dependency.equals(variableName)) {
                continue;
            }
            String dependencyValue = variables.get(dependency);
            if (dependencyValue != null && looksLikeDeferredVariableValue(dependencyValue)) {
                return true;
            }
        }
        return false;
    }

    private boolean looksLikeDeferredVariableValue(String value) {
        return value != null
                && (value.contains("@@")
                || value.contains("{{")
                || value.contains("{{{"));
    }

    private String replaceResolvedVariablePlaceholders(String value, Map<String, String> variables) {
        if (value == null || variables == null || variables.isEmpty()) {
            return value;
        }

        String result = value;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String varName = entry.getKey();
            String varValue = entry.getValue();
            if (varName == null || varValue == null) {
                continue;
            }
            result = result.replace("{{{" + varName + "}}}", varValue);
            result = result.replace("{{" + varName + "}}", varValue);
        }
        return result;
    }

    private boolean isLazyInteractshPlaceholder(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim();
        return normalized.toUpperCase(Locale.ROOT).contains("LAZY_INTERACTSH")
                || "@@httplog()".equals(normalized.toLowerCase(Locale.ROOT))
                || "@@httplog()".equals(normalized);
    }

    private StepExecutionRecord buildHttpStepRecord(int stepIndex, String stepId, RequestObj requestObj,
                                                    CustomHttpResponse response, long responseTime,
                                                    boolean matched) {
        StepExecutionRecord record = new StepExecutionRecord(stepIndex, stepId);
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
        record.setResponseTime(responseTime);
        record.setMatched(matched);
        record.buildRawRequest();
        record.buildRawResponse();
        return record;
    }

    private boolean evaluateHttpMatchers(CustomHttpResponse response, PocObj.PocStep step,
                                         String stepId, Map<String, Object> extractedValues) {
        if (step == null || step.getMatchers() == null || step.getMatchers().isEmpty()) {
            return true;
        }
        DslLogContext.setDebugEnabled(scanConfig.isDebug());
        try {
            return ResponseMatcher.matchResponse(
                    response, step.getMatchers(), step.getMatchersCondition(), responseCache,
                    stepId, extractedValues
            );
        } finally {
            DslLogContext.clear();
        }
    }

    private boolean hasStepOperators(PocObj.PocStep step) {
        if (step == null) {
            return false;
        }
        return hasStepMatchers(step) || hasStepExtractors(step);
    }

    private boolean hasStepMatchers(PocObj.PocStep step) {
        return step != null && step.getMatchers() != null && !step.getMatchers().isEmpty();
    }

    private boolean hasStepExtractors(PocObj.PocStep step) {
        return step != null && step.getExtractors() != null && !step.getExtractors().isEmpty();
    }

    private boolean hasNamedExtractors(PocObj.PocStep step) {
        if (step == null || step.getExtractors() == null || step.getExtractors().isEmpty()) {
            return false;
        }
        for (PocObj.Matcher extractor : step.getExtractors()) {
            if (extractor != null && extractor.getName() != null && !extractor.getName().trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasExtractorValues(PocObj.PocStep step, Map<String, Object> variables) {
        if (step == null || step.getExtractors() == null || step.getExtractors().isEmpty()
                || variables == null || variables.isEmpty()) {
            return false;
        }
        for (PocObj.Matcher extractor : step.getExtractors()) {
            if (extractor == null || extractor.getName() == null || extractor.getName().trim().isEmpty()) {
                continue;
            }
            if (variables.containsKey(extractor.getName())) {
                return true;
            }
        }
        return false;
    }

    private void updateTemplateAliasesForCodeStep(PocObj.CodeStep step, Map<String, Object> variables) {
        if (step == null || variables == null) {
            return;
        }
        Map<String, Object> aliases = lastStepTemplateAliases.get();
        aliases.clear();

        String protocolName = step.getProtocolName();
        if (protocolName == null || protocolName.trim().isEmpty()) {
            protocolName = "code";
        }
        protocolName = protocolName.trim().toLowerCase(Locale.ROOT);

        Object responseValue = variables.get(protocolName + "_response");
        if (responseValue != null) {
            aliases.put(protocolName + "_response", responseValue);
            if (step.getStepId() != null) {
                aliases.put(step.getStepId().toLowerCase(Locale.ROOT) + "_response", responseValue);
            }
        }
    }

    private void updateTemplateAliasesForStep(PocObj.PocStep step, RequestObj requestObj,
                                              CustomHttpResponse response, Map<String, Object> extractedValues) {
        Map<String, Object> aliases = lastStepTemplateAliases.get();
        aliases.clear();
        if (step == null) {
            return;
        }

        String stepId = step.getStepId();
        String protocolPrefix = inferProtocolName(step);
        String numericSuffix = extractNumericStepSuffix(stepId);
        String nucleiId = extractNucleiId(step);

        Map<String, Object> responseContext = new LinkedHashMap<>();
        if (response != null) {
            responseContext.putAll(DslContextBuilder.createDslContext(response, requestObj, responseCache, stepId));
        }
        if (extractedValues != null) {
            responseContext.putAll(extractedValues);
        }

        for (Map.Entry<String, Object> entry : responseContext.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key == null || value == null) {
                continue;
            }
            aliases.put(key, value);

            if (stepId != null && !stepId.trim().isEmpty()) {
                aliases.put(stepId.toLowerCase(Locale.ROOT) + "_" + key, value);
            }
            if (nucleiId != null && !nucleiId.isEmpty()) {
                aliases.put(nucleiId + "_" + key, value);
            }

            if (numericSuffix != null && protocolPrefix != null) {
                aliases.put(protocolPrefix + "_" + numericSuffix + "_" + key, value);
            }

            if ("request".equals(protocolPrefix) && numericSuffix != null) {
                aliases.put("http_" + numericSuffix + "_" + key, value);
            }
            if ("request".equals(protocolPrefix)) {
                aliases.put("http_" + key, value);
            } else if ("http".equals(protocolPrefix)) {
                aliases.put("http_" + key, value);
            }
        }
    }

    private String extractNumericStepSuffix(String stepId) {
        if (stepId == null) {
            return null;
        }
        java.util.regex.Matcher matcher = Pattern.compile(".*_(\\d+)(?:_.*)?$").matcher(stepId.toLowerCase(Locale.ROOT));
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractNucleiId(PocObj.PocStep step) {
        if (step == null || step.getOutput() == null) {
            return null;
        }
        Object nucleiId = step.getOutput().get("_nuclei_id");
        if (nucleiId == null) {
            return null;
        }
        String normalized = String.valueOf(nucleiId).trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private static class NucleiStepResult {
        private final boolean matched;
        private final boolean hasExtractorOutput;
        private final boolean hasOperators;
        private final boolean executed;

        private NucleiStepResult(boolean matched, boolean hasExtractorOutput, boolean hasOperators) {
            this(matched, hasExtractorOutput, hasOperators, matched);
        }

        private NucleiStepResult(boolean matched, boolean hasExtractorOutput, boolean hasOperators, boolean executed) {
            this.matched = matched;
            this.hasExtractorOutput = hasExtractorOutput;
            this.hasOperators = hasOperators;
            this.executed = executed;
        }

        private boolean executedWithoutOperators() {
            return executed && !hasOperators;
        }

        private boolean canContinueInAndChain() {
            return matched || hasExtractorOutput || executedWithoutOperators();
        }
    }

    private boolean shouldStopAtFirstMatch(PocObj.GlobalConfig globalConfig, PocObj.PocStep step) {
        return (globalConfig != null && globalConfig.isStopAtFirstMatch())
                || (step != null && step.isStopAtFirstMatch());
    }

    private List<Map<String, Object>> expandIterableVariables(Map<String, Object> variables) {
        List<Map<String, Object>> expanded = new ArrayList<>();
        expanded.add(new HashMap<>(variables));

        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            Object value = entry.getValue();
            if (!(value instanceof List)) {
                continue;
            }

            List<?> values = (List<?>) value;
            if (values.isEmpty()) {
                continue;
            }

            List<Map<String, Object>> next = new ArrayList<>();
            for (Map<String, Object> existing : expanded) {
                for (Object item : values) {
                    Map<String, Object> copy = new HashMap<>(existing);
                    copy.put(entry.getKey(), item == null ? "" : item);
                    next.add(copy);
                }
            }
            expanded = next;
        }

        return expanded;
    }

    private static class RawIterationState {
        private final Map<String, Object> variables;
        private final List<CustomHttpResponse> responses;
        private final RequestObj lastRequest;

        private RawIterationState(Map<String, Object> variables,
                                  List<CustomHttpResponse> responses,
                                  RequestObj lastRequest) {
            this.variables = variables;
            this.responses = responses;
            this.lastRequest = lastRequest;
        }
    }


    private int resolveStepIndex(List<PocObj.PocStep> steps, PocObj.PocStep targetStep) {
        if (steps == null || steps.isEmpty() || targetStep == null) {
            return 1;
        }
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i) == targetStep) {
                return i + 1;
            }
        }
        return 1;
    }

    private List<PocObj.Matcher> selectExtractorsForRawIndex(List<PocObj.Matcher> extractors, int rawIndex) {
        if (extractors == null || extractors.isEmpty()) {
            return extractors;
        }

        List<PocObj.Matcher> selected = new ArrayList<>();
        for (PocObj.Matcher extractor : extractors) {
            if (extractor == null) {
                continue;
            }
            String originalPart = extractor.getPart();
            if (!VariableExtractor.matchesIndexedPart(originalPart, rawIndex)) {
                continue;
            }

            Integer indexedPart = VariableExtractor.getIndexedPartNumber(originalPart);
            if (indexedPart == null) {
                selected.add(extractor);
            } else {
                PocObj.Matcher copy = copyMatcher(extractor);
                copy.setPart(VariableExtractor.stripIndexedPart(originalPart));
                selected.add(copy);
            }
        }
        return selected;
    }

    private PocObj.Matcher copyMatcher(PocObj.Matcher source) {
        PocObj.Matcher copy = new PocObj.Matcher();
        copy.setType(source.getType());
        copy.setPart(source.getPart());
        copy.setValues(source.getValues());
        copy.setNegative(source.isNegative());
        copy.setCondition(source.getCondition());
        copy.setName(source.getName());
        copy.setIndex(source.getIndex());
        copy.setCaseInsensitive(source.isCaseInsensitive());
        copy.setGreedy(source.isGreedy());
        copy.setInternal(source.getInternal());
        copy.setGroup(source.getGroup());
        copy.setAttribute(source.getAttribute());
        copy.setEncoding(source.getEncoding());
        copy.setSubMatchers(source.getSubMatchers());
        copy.setOperation(source.getOperation());
        copy.setTimeUnit(source.getTimeUnit());
        return copy;
    }

    private boolean shouldSkipReverseWaitStep(PocObj.PocStep step, Map<String, Object> variables) {
        if (step == null || variables == null || variables.isEmpty()) {
            return false;
        }

        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            ReverseObject reverseObject = unwrapReverseObject(entry.getValue());
            if (reverseObject == null || reverseObject.isAvailable()) {
                continue;
            }
            if (containsReverseWaitReference(step, entry.getKey())) {
                return true;
            }
        }
        return false;
    }

    private ReverseObject unwrapReverseObject(Object value) {
        if (value instanceof ReverseObject) {
            return (ReverseObject) value;
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (!list.isEmpty() && list.get(0) instanceof ReverseObject) {
                return (ReverseObject) list.get(0);
            }
        }
        return null;
    }

    private boolean containsReverseWaitReference(PocObj.PocStep step, String variableName) {
        if (step == null) {
            return false;
        }
        String marker = variableName == null ? "reverse.wait(" : variableName + ".wait(";

        if (containsIgnoreCase(step.getPath(), marker)
                || containsIgnoreCase(step.getBody(), marker)
                || containsIgnoreCase(step.getPath(), "reverse.wait(")
                || containsIgnoreCase(step.getBody(), "reverse.wait(")) {
            return true;
        }

        if (step.getRaw() != null) {
            for (String raw : step.getRaw()) {
                if (containsIgnoreCase(raw, marker) || containsIgnoreCase(raw, "reverse.wait(")) {
                    return true;
                }
            }
        }

        if (step.getMatchers() != null) {
            for (PocObj.Matcher matcher : step.getMatchers()) {
                if (matcher == null || matcher.getValues() == null) {
                    continue;
                }
                for (String value : matcher.getValues()) {
                    if (containsIgnoreCase(value, marker) || containsIgnoreCase(value, "reverse.wait(")) {
                        return true;
                    }
                }
            }
        }

        if (step.getOutput() != null) {
            for (Object outputExpr : step.getOutput().values()) {
                if (outputExpr != null) {
                    String expr = String.valueOf(outputExpr);
                    if (containsIgnoreCase(expr, marker) || containsIgnoreCase(expr, "reverse.wait(")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean containsIgnoreCase(String source, String token) {
        if (source == null || token == null) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(token.toLowerCase(Locale.ROOT));
    }

    private void markDnsStepSkipped(int stepIndex, PocObj.PocStep step, String value) {
        String stepId = step != null && step.getStepId() != null ? step.getStepId() : "step_" + (stepIndex - 1);
        StepExecutionRecord record = new StepExecutionRecord(stepIndex, stepId);
        record.setStepType("dns");
        record.setMatched(false);
        record.setErrorMessage(DNSLOG_UNAVAILABLE_SKIPPED);
        stepExecutionRecords.get().add(record);

        addSemanticWarning(
                DNSLOG_UNAVAILABLE_SKIPPED,
                "P1",
                "dns",
                "domain",
                value,
                "skip",
                "DNS OOB 不可用，步骤已跳过"
        );
    }

    private CustomHttpResponse executeHttpRequest(RequestObj requestObj) throws Exception {
        CustomHttpResponse dnsLogMockResponse = createDnsLogManagementMockResponse(requestObj);
        if (dnsLogMockResponse != null) {
            return dnsLogMockResponse;
        }
        CustomHttpResponse restrictedResponse = createRestrictedOutboundMockResponse(requestObj);
        if (restrictedResponse != null) {
            return restrictedResponse;
        }
        String cacheKey = buildRequestCacheKey(requestObj);
        if (cacheKey != null && scanConfig != null && scanConfig.isEnableResponseCache()) {
            ResponseCacheService.CachedResponse cached = responseCacheService.get(cacheKey);
            if (cached != null) {
                return buildSyntheticHttpResponse(
                        requestObj.getUrl(),
                        cached.getBody(),
                        cached.getHeaders(),
                        cached.getStatusCode(),
                        cached.getResponseTimeMs());
            }
        }
        try {
            applyGlobalRateLimit();
            CustomHttpResponse response = requests(requestObj);
            if (cacheKey != null && response != null && scanConfig != null && scanConfig.isEnableResponseCache()) {
                responseCacheService.put(
                        cacheKey,
                        response.getResponseCode(),
                        flattenHeaders(response.getHeaderFields()),
                        response.getTextStr(),
                        response.getResponseTime());
            }
            return response;
        } catch (Exception e) {
            throw unwrapRequestException(e);
        }
    }

    private void applyGlobalRateLimit() throws InterruptedException {
        if (scanConfig == null) {
            return;
        }
        int requestsPerSecond = scanConfig.getRequestsPerSecond();
        if (requestsPerSecond <= 0) {
            return;
        }

        long intervalMs = Math.max(1L, 1000L / requestsPerSecond);
        while (true) {
            long now = System.currentTimeMillis();
            long permitAt = nextRequestPermitTimeMs.get();
            long candidate = Math.max(now, permitAt);
            long next = candidate + intervalMs;
            if (nextRequestPermitTimeMs.compareAndSet(permitAt, next)) {
                long waitMs = candidate - now;
                if (waitMs > 0) {
                    Thread.sleep(waitMs);
                }
                return;
            }
        }
    }

    private String buildRequestCacheKey(RequestObj requestObj) {
        return requestSignatureService.buildForRequest(requestObj);
    }

    private Map<String, String> getRequestSignatureHeaders(RequestObj requestObj) {
        if (requestObj == null) {
            return Collections.emptyMap();
        }
        Map<String, String> headers = requestObj.getRequestSignatureHeaders();
        return headers != null ? headers : requestObj.getHeaders();
    }

    private Map<String, String> flattenHeaders(Map<String, List<String>> headers) {
        Map<String, String> flattened = new LinkedHashMap<>();
        if (headers == null || headers.isEmpty()) {
            return flattened;
        }
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            flattened.put(entry.getKey(), String.join(", ", entry.getValue()));
        }
        return flattened;
    }

    private Exception unwrapRequestException(Exception exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return (SocketTimeoutException) current;
            }
            if (current instanceof ConnectException) {
                return (ConnectException) current;
            }
            if (current instanceof UnknownHostException) {
                return (UnknownHostException) current;
            }
            if (current instanceof IOException) {
                return (IOException) current;
            }
            current = current.getCause();
        }
        return exception;
    }

    private CustomHttpResponse createDnsLogManagementMockResponse(RequestObj requestObj) {
        if (!DnsLogService.isMockMode() || requestObj == null || requestObj.getUrl() == null) {
            return null;
        }

        try {
            URL url = new URL(requestObj.getUrl());
            String host = url.getHost();
            String path = url.getPath();
            if (host == null || path == null || !isDnsLogCnHost(host)) {
                return null;
            }

            if ("/getdomain.php".equalsIgnoreCase(path)) {
                String domain = DnsLogService.generateDnsLogDomain();
                if (domain != null && !domain.trim().isEmpty()) {
                    dnsLogManagementMockDomains.get().add(domain.trim());
                }

                Map<String, String> headers = new LinkedHashMap<>();
                headers.put("Content-Type", "text/plain; charset=utf-8");
                headers.put("Set-Cookie", "PHPSESSID=potatotool-mock; path=/");
                return buildSyntheticHttpResponse(requestObj.getUrl(), domain, headers);
            }

            if ("/getrecords.php".equalsIgnoreCase(path)) {
                Map<String, String> headers = new LinkedHashMap<>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                return buildSyntheticHttpResponse(requestObj.getUrl(), buildDnsLogMockRecordsForTriggeredDomains(), headers);
            }
        } catch (Exception ignored) {
            return null;
        }

        return null;
    }

    private boolean isDnsLogCnHost(String host) {
        String normalizedHost = host == null ? "" : host.trim().toLowerCase(Locale.ROOT);
        return "dnslog.cn".equals(normalizedHost) || "www.dnslog.cn".equals(normalizedHost);
    }

    private CustomHttpResponse createRestrictedOutboundMockResponse(RequestObj requestObj) {
        if (scanConfig == null || !scanConfig.isRestrictOutboundRequestsToTargetHost()
                || requestObj == null || requestObj.getUrl() == null) {
            return null;
        }

        try {
            String requestUrl = requestObj.getUrl().trim();
            URL outboundUrl = new URL(requestUrl);
            String outboundHost = normalizeHost(outboundUrl.getHost());
            if (outboundHost == null || outboundHost.isEmpty()) {
                return null;
            }

            String baseTarget = currentTarget.get();
            if (baseTarget == null || baseTarget.trim().isEmpty()) {
                return null;
            }

            String targetHost = extractHostSafely(baseTarget);
            if (targetHost == null || targetHost.isEmpty() || targetHost.equals(outboundHost)) {
                return null;
            }

            logWarn("HTTP", "已阻断越界外联请求: " + requestUrl + " (targetHost=" + targetHost + ")");
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Content-Type", "text/plain; charset=utf-8");
            headers.put("X-PotatoTool-Restricted", "true");
            return buildSyntheticHttpResponse(
                    requestUrl,
                    OUTBOUND_RESTRICTED_SKIPPED,
                    headers,
                    599,
                    0L);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractHostSafely(String urlOrTarget) {
        try {
            URL url = new URL(urlOrTarget);
            return normalizeHost(url.getHost());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalizeHost(String host) {
        return host == null ? null : host.trim().toLowerCase(Locale.ROOT);
    }

    private String buildDnsLogMockRecords() {
        List<String> domains = dnsLogManagementMockDomains.get();
        if (domains == null || domains.isEmpty()) {
            return "[]";
        }

        StringBuilder records = new StringBuilder("[");
        long timestamp = System.currentTimeMillis() / 1000;
        for (int i = 0; i < domains.size(); i++) {
            if (i > 0) {
                records.append(',');
            }
            records.append("{\"domain\":\"")
                    .append(escapeJson(domains.get(i)))
                    .append("\",\"ip\":\"127.0.0.1\",\"time\":\"")
                    .append(timestamp)
                    .append("\"}");
        }
        records.append(']');
        return records.toString();
    }

    private String buildDnsLogMockRecordsForTriggeredDomains() {
        List<String> domains = dnsLogManagementMockDomains.get();
        if (domains == null || domains.isEmpty()) {
            return "[]";
        }

        List<String> triggeredDomains = new ArrayList<>();
        for (String domain : domains) {
            if (DnsLogService.hasDnsResolution(domain)) {
                triggeredDomains.add(domain);
            }
        }
        if (triggeredDomains.isEmpty()) {
            return "[]";
        }

        StringBuilder records = new StringBuilder("[");
        long timestamp = System.currentTimeMillis() / 1000;
        for (int i = 0; i < triggeredDomains.size(); i++) {
            if (i > 0) {
                records.append(',');
            }
            records.append("{\"domain\":\"")
                    .append(escapeJson(triggeredDomains.get(i)))
                    .append("\",\"ip\":\"127.0.0.1\",\"time\":\"")
                    .append(timestamp)
                    .append("\"}");
        }
        records.append(']');
        return records.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private CustomHttpResponse buildSyntheticHttpResponse(String url, String body, Map<String, String> headers) {
        return buildSyntheticHttpResponse(url, body, headers, 200, 0L);
    }

    private CustomHttpResponse buildSyntheticHttpResponse(String url,
                                                          String body,
                                                          Map<String, String> headers,
                                                          int statusCode,
                                                          long responseTimeMs) {
        String responseBody = body == null ? "" : body;
        String contentType = headers != null && headers.get("Content-Type") != null
                ? headers.get("Content-Type")
                : "text/plain; charset=utf-8";

        Response.Builder responseBuilder = new Response.Builder()
                .request(new Request.Builder().url(url).build())
                .protocol(Protocol.HTTP_1_1)
                .code(statusCode)
                .message(statusCode == 200 ? "OK" : String.valueOf(statusCode))
                .body(ResponseBody.create(MediaType.parse(contentType), responseBody));

        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                if (header.getKey() != null && header.getValue() != null) {
                    responseBuilder.addHeader(header.getKey(), header.getValue());
                }
            }
        }

        CustomHttpResponse synthetic = new CustomHttpResponse(responseBuilder.build());
        synthetic.setResponseTime(responseTimeMs);
        return synthetic;
    }

    /**
     * 创建HTTP请求对象
     */
    private RequestObj createRequest(String target, PocObj.PocStep step,
                                     PocObj.GlobalConfig globalConfig,
                                     Map<String, Object> extractedValues) {
        applyGobyRequestVariables(step, extractedValues);
        RequestObj requestObj = new RequestObj();
        // 漏扫链路在下方统一按 default < global < step 合并 headers，避免 RequestObj 构造时的默认 headers
        // 被误判为 POC 显式 headers，进而影响缓存/聚合签名。
        requestObj.setHeaders(null);
        requestObj.setRequestSignatureHeaders(null);
        // RequestObj 会默认继承总代理；漏扫链路统一改为显式代理，避免绕过 Proxy.services.VulnScan。
        HttpHandler.applyProxySettings(requestObj);

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
            if (pocStepHeaders != null && !pocStepHeaders.isEmpty()) {
                requestObj.setHeaders(pocStepHeaders);
            }
            RequestSignatureService.HeaderResolution headerResolution = requestSignatureService.resolveHeaders(globalConfig, step);
            requestObj.setHeaders(headerResolution.getResolvedHeaders());
            requestObj.setRequestSignatureHeaders(headerResolution.getSignatureHeaders());
        }

        if (step.getCookie() != null && !step.getCookie().trim().isEmpty()) {
            Map<String, String> headers = requestObj.getHeaders();
            Map<String, String> signatureHeaders = requestObj.getRequestSignatureHeaders();
            if (headers == null) {
                headers = new LinkedHashMap<>();
            }
            if (signatureHeaders == null) {
                signatureHeaders = new LinkedHashMap<>();
            }
            String cookieValue = HttpHandler.replaceVariablesObj(step.getCookie(), extractedValues);
            putHeader(headers, "Cookie", cookieValue);
            putHeader(signatureHeaders, "Cookie", cookieValue);
            requestObj.setHeaders(headers);
            requestObj.setRequestSignatureHeaders(signatureHeaders);
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
            requestObj.setReadTimeout(timeout);
            requestObj.setWriteTimeout(timeout);
            requestObj.setCallTimeout(Math.max(timeout, timeout + 5));
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

    private void applyGobyRequestVariables(PocObj.PocStep step, Map<String, Object> extractedValues) {
        if (step == null || step.getRequestVariables() == null || step.getRequestVariables().isEmpty()
                || extractedValues == null) {
            return;
        }

        Map<String, String> stringVariables = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : extractedValues.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                stringVariables.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        for (String requestVariable : step.getRequestVariables()) {
            if (requestVariable == null || requestVariable.trim().isEmpty()) {
                continue;
            }

            String[] parts = requestVariable.split("\\|", -1);
            if (parts.length < 2) {
                continue;
            }

            String varName = parts[0].trim();
            String dataSource = parts[1].trim().toLowerCase(Locale.ROOT);
            String operation = parts.length > 2 ? parts[2].trim().toLowerCase(Locale.ROOT) : "";
            String value = parts.length > 3 ? parts[3] : "";
            if (varName.isEmpty()) {
                continue;
            }

            String resolvedValue = resolveGobyRequestVariable(varName, dataSource, operation, value, stringVariables);
            if (resolvedValue == null) {
                continue;
            }

            extractedValues.put(varName, resolvedValue);
            stringVariables.put(varName, resolvedValue);
        }
    }

    private String resolveGobyRequestVariable(String varName,
                                              String dataSource,
                                              String operation,
                                              String value,
                                              Map<String, String> variables) {
        if ("rand".equals(dataSource)) {
            String functionArg = value == null ? "" : value.trim();
            if ("int".equals(operation) || "str".equals(operation)) {
                return GobyFunctionProcessor.processGobyFunctions("@@random(" + functionArg + ")", variables);
            }
            return null;
        }

        if ("dnslog".equals(dataSource)) {
            return GobyFunctionProcessor.processGobyFunctions("@@dnslog()", variables);
        }

        if ("httplog".equals(dataSource)) {
            return GobyFunctionProcessor.processGobyFunctions("@@httplog()", variables);
        }

        if ("define".equals(dataSource) || dataSource.isEmpty()) {
            if ("str".equals(operation) || "text".equals(operation)) {
                return replaceResolvedVariablePlaceholders(value, variables);
            }
            if (isSupportedGobyFunctionOperation(operation)) {
                return GobyFunctionProcessor.processGobyFunctions("@@" + operation + "(" + value + ")", variables);
            }
        }

        if (isResponseDataSource(dataSource)) {
            String content = resolveGobyResponseDataSource(dataSource, variables);
            if (content == null) {
                return null;
            }

            String normalizedOperation = (operation == null || operation.isEmpty()
                    || "undefined".equals(operation)) ? "regex" : operation;
            if ("text".equals(normalizedOperation)) {
                normalizedOperation = "regex";
            }

            String pattern = value == null ? "" : value.trim();
            if ("regex".equals(normalizedOperation) && pattern.isEmpty()) {
                pattern = "(?s).*";
            }

            PocObj.Matcher extractor = new PocObj.Matcher();
            extractor.setName(varName);
            extractor.setPart("body");
            extractor.setType(PocObj.MatcherType.REGEX);
            extractor.setGroup(-1);
            extractor.setValues(Collections.singletonList(pattern));

            Map<String, Object> extracted = new HashMap<>();
            VariableExtractor.extractVariablesFromTextObj(content,
                    Collections.singletonList(extractor),
                    extracted,
                    true);
            Object extractedValue = extracted.get(varName);
            return extractedValue != null ? String.valueOf(extractedValue) : null;
        }

        return null;
    }

    private boolean isSupportedGobyFunctionOperation(String operation) {
        if (operation == null || operation.isEmpty()) {
            return false;
        }
        return Arrays.asList("base64", "md5", "sha1", "sha256", "sha512",
                "urlencode", "urldecode", "timestamp", "date", "uuid", "file").contains(operation);
    }

    private boolean isResponseDataSource(String dataSource) {
        return "body".equals(dataSource)
                || "lastbody".equals(dataSource)
                || "header".equals(dataSource)
                || "lastheader".equals(dataSource)
                || "status".equals(dataSource)
                || "code".equals(dataSource)
                || "lastraw".equals(dataSource);
    }

    private String resolveGobyResponseDataSource(String dataSource, Map<String, String> variables) {
        if (variables == null || dataSource == null) {
            return null;
        }

        if ("body".equals(dataSource) || "lastbody".equals(dataSource) || "lastraw".equals(dataSource)) {
            return variables.get(GOBY_LAST_BODY_KEY);
        }
        if ("header".equals(dataSource) || "lastheader".equals(dataSource)) {
            return variables.get(GOBY_LAST_HEADER_KEY);
        }
        if ("status".equals(dataSource) || "code".equals(dataSource)) {
            return variables.get(GOBY_LAST_STATUS_KEY);
        }
        return null;
    }

    private void updateGobyResponseContext(Map<String, Object> extractedValues, CustomHttpResponse response) {
        if (extractedValues == null || response == null) {
            return;
        }
        extractedValues.put(GOBY_LAST_BODY_KEY, VariableExtractor.getResponsePart(response, "body"));
        extractedValues.put(GOBY_LAST_HEADER_KEY, VariableExtractor.getResponsePart(response, "header"));
        extractedValues.put(GOBY_LAST_STATUS_KEY, VariableExtractor.getResponsePart(response, "status"));
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

    private void applyCookieReuse(RequestObj requestObj, PocObj.GlobalConfig globalConfig, PocObj.PocStep step) {
        if (requestObj == null || globalConfig == null || !globalConfig.isCookieReuse()) {
            return;
        }
        if (step != null && step.isDisableCookie()) {
            return;
        }

        Map<String, String> jar = cookieJar.get();
        if (jar == null || jar.isEmpty()) {
            return;
        }

        Map<String, String> headers = requestObj.getHeaders();
        if (headers == null) {
            headers = new LinkedHashMap<>();
        }
        Map<String, String> signatureHeaders = requestObj.getRequestSignatureHeaders();
        if (signatureHeaders == null) {
            signatureHeaders = new LinkedHashMap<String, String>(headers);
        }

        String jarHeader = buildCookieHeader(jar);
        if (jarHeader.isEmpty()) {
            return;
        }

        String existing = findHeader(headers, "Cookie");
        if (existing == null || existing.trim().isEmpty()) {
            putHeader(headers, "Cookie", jarHeader);
            putHeader(signatureHeaders, "Cookie", jarHeader);
        } else {
            String mergedCookie = mergeCookieHeader(existing, jar);
            putHeader(headers, "Cookie", mergedCookie);
            putHeader(signatureHeaders, "Cookie", mergedCookie);
        }
        requestObj.setHeaders(headers);
        requestObj.setRequestSignatureHeaders(signatureHeaders);
    }

    private void captureCookies(CustomHttpResponse response, PocObj.GlobalConfig globalConfig, PocObj.PocStep step) {
        if (response == null || globalConfig == null || !globalConfig.isCookieReuse()) {
            return;
        }
        if (step != null && step.isDisableCookie()) {
            return;
        }

        Map<String, List<String>> headers = response.getHeaderFields();
        if (headers == null || headers.isEmpty()) {
            return;
        }

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() == null || !"Set-Cookie".equalsIgnoreCase(entry.getKey())) {
                continue;
            }
            if (entry.getValue() == null) {
                continue;
            }
            for (String cookieLine : entry.getValue()) {
                storeCookie(cookieLine);
            }
        }
    }

    private void storeCookie(String cookieLine) {
        if (cookieLine == null || cookieLine.trim().isEmpty()) {
            return;
        }
        String pair = cookieLine.split(";", 2)[0].trim();
        int eq = pair.indexOf('=');
        if (eq <= 0) {
            return;
        }
        String name = pair.substring(0, eq).trim();
        String value = pair.substring(eq + 1).trim();
        if (!name.isEmpty()) {
            cookieJar.get().put(name, value);
        }
    }

    private String findHeader(Map<String, String> headers, String headerName) {
        if (headers == null || headerName == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(headerName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void putHeader(Map<String, String> headers, String headerName, String headerValue) {
        if (headers == null || headerName == null) {
            return;
        }
        String existingKey = null;
        for (String key : headers.keySet()) {
            if (key != null && key.equalsIgnoreCase(headerName)) {
                existingKey = key;
                break;
            }
        }
        headers.put(existingKey != null ? existingKey : headerName, headerValue);
    }

    private String buildCookieHeader(Map<String, String> cookies) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue() == null ? "" : entry.getValue());
        }
        return sb.toString();
    }

    private String mergeCookieHeader(String existing, Map<String, String> jar) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (existing != null && !existing.trim().isEmpty()) {
            String[] pairs = existing.split(";");
            for (String pair : pairs) {
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    merged.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
                }
            }
        }
        if (jar != null) {
            for (Map.Entry<String, String> entry : jar.entrySet()) {
                if (entry.getKey() != null && !merged.containsKey(entry.getKey())) {
                    merged.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return buildCookieHeader(merged);
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
                if (result.getMatchedPath() == null || result.getMatchedPath().isEmpty()) {
                    String fallbackMatchedPath = findMatchedRequestUrl(records);
                    if (fallbackMatchedPath != null && !fallbackMatchedPath.isEmpty()) {
                        result.setMatchedPath(fallbackMatchedPath);
                    }
                }
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

            attachInternalWarningDetails(result, poc);
            attachResultExplainability(result, poc);
        } catch (Exception e) {
            if (debugMode) {
                System.err.println("填充增强报告字段失败: " + e.getMessage());
            }
        } finally {
            // 清理 ThreadLocal 防止内存泄漏
            clearThreadLocals();
        }

        return result;
    }

    private String findMatchedRequestUrl(List<StepExecutionRecord> records) {
        if (records == null || records.isEmpty()) {
            return null;
        }
        for (int i = records.size() - 1; i >= 0; i--) {
            StepExecutionRecord record = records.get(i);
            if (record != null && record.isMatched()
                    && record.getRequestUrl() != null
                    && !record.getRequestUrl().trim().isEmpty()) {
                return record.getRequestUrl();
            }
        }
        return null;
    }

    private void attachResultExplainability(ScanResult result, PocObj.Poc poc) {
        if (result == null || poc == null) {
            return;
        }
        if ("http-missing-security-headers".equalsIgnoreCase(poc.getId())) {
            List<String> missingHeaders = detectMissingSecurityHeaders(result.getRawResponseSnippet());
            if (!missingHeaders.isEmpty()) {
                result.addDetail("missing_security_headers", missingHeaders);
                if (result.getMatchedPath() == null || result.getMatchedPath().isEmpty()) {
                    result.setMatchedPath("missing headers: " + String.join(", ", missingHeaders));
                } else {
                    result.addDetail("matched_reason", "missing headers: " + String.join(", ", missingHeaders));
                }
            }
        }
    }

    private List<String> detectMissingSecurityHeaders(String rawResponse) {
        List<String> requiredHeaders = Arrays.asList(
                "strict-transport-security",
                "content-security-policy",
                "permissions-policy",
                "x-frame-options",
                "x-content-type-options",
                "x-permitted-cross-domain-policies",
                "referrer-policy",
                "clear-site-data",
                "cross-origin-embedder-policy",
                "cross-origin-opener-policy",
                "cross-origin-resource-policy"
        );
        if (rawResponse == null) {
            return Collections.emptyList();
        }
        String lower = rawResponse.toLowerCase(Locale.ROOT);
        List<String> missing = new ArrayList<String>();
        for (String header : requiredHeaders) {
            if (!lower.contains(header + ":")) {
                missing.add(header);
            }
        }
        return missing;
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
        attachInternalWarningDetails(result, poc);

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

        clearThreadLocals();
        return result;
    }

    private void clearThreadLocals() {
        lastMatchedRequest.remove();
        lastMatchedResponse.remove();
        lastMatchedPath.remove();
        lastMatchedPayload.remove();
        stepExecutionRecords.remove();
        extractedOutputData.remove();
        usedVariableValues.remove();
        usedParamKeys.remove();
        semanticWarnings.remove();
        cookieJar.remove();
        flowExecutionMode.remove();
        lastStepTemplateAliases.remove();
        currentTarget.remove();
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

        logDebug("EXTRACTOR", "开始提取 output 变量，共 " + step.getOutput().size() + " 个");

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
                    logDebug("EXTRACTOR", "提取变量成功: " + varName + " = " + value);
                } else {
                    logWarn("EXTRACTOR", "Output 表达式求值返回 null: " + varName + " = " + expression);
                }
            } catch (Exception e) {
                logWarn("EXTRACTOR", "Output 提取失败: " + varName + " = " + expression + ", 错误: " + e.getMessage());
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
        }
        Map<String, String> signatureHeaders = requestObj.getRequestSignatureHeaders();
        if (signatureHeaders == null) {
            signatureHeaders = new LinkedHashMap<String, String>(headers);
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
            requestObj.setHeaders(headers);
            requestObj.setRequestSignatureHeaders(new LinkedHashMap<String, String>(headers));
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
    private boolean executeDnsStep(PocObj.DnsStep dnsStep, Map<String, Object> variables, int stepIndex) {
        try {
            // 替换变量（支持嵌套变量）
            String domain = HttpHandler.replaceVariablesObj(dnsStep.getDomain(), variables);
            if (!DnsLogService.isRealDnsLogDomain(domain)) {
                markDnsStepSkipped(stepIndex, dnsStep, domain);
                return false;
            }

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

            if (dnsStep.getExtractors() != null && !dnsStep.getExtractors().isEmpty()) {
                VariableExtractor.extractVariablesFromTextObj(responseBody, dnsStep.getExtractors(), variables, true);
            }

            // 执行匹配（简化版，使用字符串匹配）
            if (dnsStep.getMatchers() != null && !dnsStep.getMatchers().isEmpty()) {
                validateNonHttpMatcherTypes(dnsStep, "dns");
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

            String responseBody = String.join("\n", wsResponse.getReceivedMessages());
            if (wsStep.getExtractors() != null && !wsStep.getExtractors().isEmpty()) {
                VariableExtractor.extractVariablesFromTextObj(responseBody, wsStep.getExtractors(), variables, true);
            }

            // 执行匹配
            if (wsStep.getMatchers() != null && !wsStep.getMatchers().isEmpty()) {
                validateNonHttpMatcherTypes(wsStep, "websocket");
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
    private boolean executeSslStep(String target, PocObj.SslStep sslStep, Map<String, Object> variables) {
        try {
            if (shouldSkipSslStepForTarget(target)) {
                System.out.println("跳过 SSL/TLS 步骤: 非 TLS 目标 " + target);
                return false;
            }

            // 替换变量（支持嵌套变量）
            String address = HttpHandler.replaceVariablesObj(sslStep.getAddress(), variables);

            System.out.println("→ 执行 SSL/TLS 检测: " + address);

            // 执行 SSL 检测
            SslHandler.SslResponse sslResponse = SslHandler.check(address, sslStep.getTimeout());

            if (!sslResponse.isSuccess()) {
                System.err.println("SSL/TLS 检测失败: " + sslResponse.getError());
                return false;
            }

            String raw = sslResponse.getRaw();
            if (sslStep.getExtractors() != null && !sslStep.getExtractors().isEmpty()) {
                VariableExtractor.extractVariablesFromTextObj(raw, sslStep.getExtractors(), variables, true);
            }

            // 执行匹配
            if (sslStep.getMatchers() != null && !sslStep.getMatchers().isEmpty()) {
                validateNonHttpMatcherTypes(sslStep, "ssl");
                boolean matched = SimpleMatcher.match(raw, sslStep);
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

    private boolean shouldSkipSslStepForTarget(String target) {
        if (target == null) {
            return false;
        }
        String normalized = target.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://");
    }

    private int resolveOobWaitSeconds(ScanConfig config) {
        if (config == null) {
            return 8;
        }
        int explicitWaitSeconds = config.getOobInteractionWaitSeconds();
        if (explicitWaitSeconds >= 0) {
            return explicitWaitSeconds;
        }
        int timeout = config.getTimeout();
        if (timeout <= 0) {
            return 8;
        }
        return Math.max(1, Math.min(8, timeout));
    }

    /**
     * 执行 File 步骤
     */
    private boolean executeFileStep(String target, PocObj.FileStep fileStep, Map<String, Object> variables) {
        try {
            List<String> sourcePaths = fileStep.getPaths();
            if ((sourcePaths == null || sourcePaths.isEmpty()) && target != null && !target.trim().isEmpty()) {
                sourcePaths = Collections.singletonList(target);
            }
            if (sourcePaths == null || sourcePaths.isEmpty()) {
                System.out.println("File 步骤缺少扫描路径");
                return false;
            }

            // 替换变量（支持嵌套变量）
            List<String> paths = sourcePaths.stream()
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
            boolean hasMatchers = fileStep.getMatchers() != null && !fileStep.getMatchers().isEmpty();
            if (hasMatchers) {
                validateNonHttpMatcherTypes(fileStep, "file");
            }
            for (FileHandler.FileResponse fileResponse : fileResponses) {
                if (fileResponse.isSuccess() && fileResponse.getContent() != null) {
                    String content = fileResponse.getContent();
                    if (fileStep.getExtractors() != null && !fileStep.getExtractors().isEmpty()) {
                        VariableExtractor.extractVariablesFromTextObj(content, fileStep.getExtractors(), variables, true);
                    }
                    if (hasMatchers) {
                        boolean matched = SimpleMatcher.match(content, fileStep);
                        if (matched) {
                            System.out.println("✓ 文件匹配成功: " + fileResponse.getPath());
                            return true;
                        }
                    }
                }
            }
            if (hasMatchers) {
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
    private boolean executeHeadlessStep(PocObj.HeadlessStep headlessStep,
                                        Map<String, Object> variables,
                                        PocObj.GlobalConfig globalConfig) {
        try {
            if (scanConfig != null && !scanConfig.isEnableHeadless()) {
                System.err.println("[Headless 跳过] Headless 开关已关闭，当前扫描不会执行 Headless 步骤");
                return false;
            }

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
                browserSteps.add(new HeadlessHandler.BrowserStep(action.getAction(), action.getName(), args));
            }

            String startUrl = resolveHeadlessStartUrl(headlessStep, variables);
            int timeoutSeconds = resolveHeadlessTimeout(headlessStep, globalConfig);

            // 执行 Headless 操作
            HeadlessHandler.HeadlessResponse headlessResponse =
                HeadlessHandler.execute(startUrl, browserSteps, timeoutSeconds);

            if (headlessResponse.getLogs() != null) {
                for (String log : headlessResponse.getLogs()) {
                    if (log != null && log.startsWith("[Headless")) {
                        System.out.println(log);
                    }
                }
            }

            if (!headlessResponse.isSuccess()) {
                System.err.println("Headless 操作失败: " + headlessResponse.getError());
                return false;
            }

            Map<String, Object> headlessRuntime = buildHeadlessRuntimeValues(headlessResponse);
            variables.putAll(headlessRuntime);

            String pageSource = headlessResponse.getPageSource();
            if (headlessStep.getExtractors() != null && !headlessStep.getExtractors().isEmpty()) {
                extractHeadlessVariables(headlessStep.getExtractors(), headlessRuntime, variables);
            }

            // 执行匹配
            if (headlessStep.getMatchers() != null && !headlessStep.getMatchers().isEmpty()) {
                validateNonHttpMatcherTypes(headlessStep, "headless");
                boolean matched = matchHeadlessStep(headlessStep, headlessRuntime, variables);
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

    String resolveHeadlessStartUrl(PocObj.HeadlessStep headlessStep, Map<String, Object> variables) {
        if (headlessStep == null || headlessStep.getUrl() == null) {
            return null;
        }
        return HttpHandler.replaceVariablesObj(headlessStep.getUrl(), variables);
    }

    int resolveHeadlessTimeout(PocObj.HeadlessStep headlessStep, PocObj.GlobalConfig globalConfig) {
        int timeoutSeconds = headlessStep == null ? 0 : determineTimeout(headlessStep, globalConfig);
        return timeoutSeconds > 0 ? timeoutSeconds : 30;
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
                    System.err.println("  → 请在 设置 -> 基础配置 中配置 Python 路径");
                    System.err.println("  → 或在 config.json 的 EnvPath.python 中指定");
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
            applyCodeArgsToContext(codeStep.getArgs(), context);
            context.put("template", new HashMap<>(context));

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

            publishCodeResponseVariables(codeStep, resultString, variables);

            // 执行匹配
            if (codeStep.getExtractors() != null && !codeStep.getExtractors().isEmpty()) {
                VariableExtractor.extractVariablesFromTextObj(resultString, codeStep.getExtractors(), variables, true);
            }
            if (codeStep.getMatchers() != null && !codeStep.getMatchers().isEmpty()) {
                validateNonHttpMatcherTypes(codeStep, "code");
                boolean matched = SimpleMatcher.match(resultString, codeStep, variables);
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

    private void applyCodeArgsToContext(Object args, Map<String, Object> context) {
        if (args == null || context == null) {
            return;
        }
        if (args instanceof Map) {
            Map<?, ?> argMap = (Map<?, ?>) args;
            for (Map.Entry<?, ?> entry : argMap.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                String key = String.valueOf(entry.getKey());
                Object value = entry.getValue();
                context.put(key, value instanceof String ? HttpHandler.replaceVariablesObj((String) value, context) : value);
            }
        }
    }

    private void publishCodeResponseVariables(PocObj.CodeStep codeStep, String resultString, Map<String, Object> variables) {
        if (codeStep == null || variables == null) {
            return;
        }
        String output = resultString == null ? "" : resultString.trim();
        String protocolName = codeStep.getProtocolName();
        if (protocolName == null || protocolName.trim().isEmpty()) {
            protocolName = "code";
        }
        protocolName = protocolName.trim().toLowerCase(Locale.ROOT);

        variables.put(protocolName + "_response", output);
        if ("javascript".equals(protocolName)) {
            variables.put("javascript_response", output);
        } else if ("code".equals(protocolName)) {
            variables.put("code_response", output);
        }

        String stepId = codeStep.getStepId();
        if (stepId != null && !stepId.trim().isEmpty()) {
            variables.put(stepId.trim().toLowerCase(Locale.ROOT) + "_response", output);
        }
    }

    /**
     * 执行 TCP 步骤
     */
    private boolean executeTcpStep(String target, PocObj.TcpStep tcpStep, Map<String, Object> variables) {
        try {
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

            List<String> candidateHosts = resolveTcpHosts(target, tcpStep, variables);
            List<Integer> candidatePorts = resolveTcpPorts(target, tcpStep, variables);
            if (candidateHosts.isEmpty()) {
                System.err.println("TCP 步骤缺少有效主机");
                return false;
            }
            if (candidatePorts.isEmpty()) {
                System.err.println("TCP 步骤缺少有效端口");
                return false;
            }

            validateNonHttpMatcherTypes(tcpStep, "tcp");

            int timeout = tcpStep.getTimeout() > 0 ? tcpStep.getTimeout() : 10;
            String lastError = null;
            for (String host : candidateHosts) {
                for (Integer port : candidatePorts) {
                    if (port == null) {
                        continue;
                    }
                    System.out.println("→ 执行 TCP 通信: " + host + ":" + port);
                    SocketHandler.SocketResponse response = SocketHandler.execute(
                            host,
                            port,
                            processedInputs,
                            timeout
                    );

                    if (!response.isSuccess()) {
                        lastError = response.getError();
                        System.err.println("TCP 通信失败: " + host + ":" + port + " - " + response.getError());
                        continue;
                    }

                    String raw = response.getRawString();
                    if (tcpStep.getExtractors() != null && !tcpStep.getExtractors().isEmpty()) {
                        VariableExtractor.extractVariablesFromTextObj(raw, tcpStep.getExtractors(), variables, true);
                    }
                    if (tcpStep.getMatchers() != null && !tcpStep.getMatchers().isEmpty()) {
                        boolean matched = SimpleMatcher.match(raw, tcpStep);
                        if (!matched) {
                            System.out.println("TCP 响应不匹配: " + host + ":" + port);
                            continue;
                        }
                    }

                    System.out.println("✓ TCP 步骤执行成功");
                    return true;
                }
            }
            if (lastError != null) {
                System.err.println("TCP 候选全部失败，最后错误: " + lastError);
            }
            return false;

        } catch (Exception e) {
            System.err.println("TCP 步骤执行失败: " + e.getMessage());
            return false;
        }
    }

    private List<String> resolveTcpHosts(String target, PocObj.TcpStep tcpStep, Map<String, Object> variables) {
        LinkedHashSet<String> resolvedHosts = new LinkedHashSet<>();
        String targetHost = firstNonBlank(resolveTcpTargetHost(target, variables));
        String singleHost = firstNonBlank(resolveTcpHostValue(tcpStep.getHost(), variables));
        List<String> hosts = tcpStep.getHosts();

        if (targetHost != null) {
            resolvedHosts.add(targetHost);
        }
        if (hosts != null) {
            for (String host : hosts) {
                String resolved = firstNonBlank(resolveTcpHostValue(host, variables));
                if (resolved != null) {
                    resolvedHosts.add(resolved);
                }
            }
        }
        if (singleHost != null) {
            resolvedHosts.add(singleHost);
        }
        return new ArrayList<String>(resolvedHosts);
    }

    private List<Integer> resolveTcpPorts(String target, PocObj.TcpStep tcpStep, Map<String, Object> variables) {
        LinkedHashSet<Integer> resolvedPorts = new LinkedHashSet<>();
        Integer targetPort = resolveTcpTargetPort(target, variables);
        if (targetPort != null) {
            resolvedPorts.add(targetPort);
        }

        String portStr = HttpHandler.replaceVariablesObj(tcpStep.getPort(), variables);
        if (portStr != null) {
            String[] candidates = portStr.split(",");
            for (String candidate : candidates) {
                String trimmed = candidate == null ? null : candidate.trim();
                if (trimmed == null || trimmed.isEmpty()) {
                    continue;
                }
                try {
                    resolvedPorts.add(Integer.parseInt(trimmed));
                } catch (NumberFormatException e) {
                    System.err.println("无效的端口: " + trimmed);
                }
            }
        }
        return new ArrayList<Integer>(resolvedPorts);
    }

    private String resolveTcpTargetHost(String target, Map<String, Object> variables) {
        TargetEndpoint endpoint = parseTargetEndpoint(target);
        if (endpoint != null && endpoint.host != null) {
            return endpoint.host;
        }
        Object hostname = variables.get("Hostname");
        String value = objectToScalarString(hostname);
        if (value != null) {
            return stripTcpSchemeAndPort(value);
        }
        Object host = variables.get("Host");
        return stripTcpSchemeAndPort(objectToScalarString(host));
    }

    private Integer resolveTcpTargetPort(String target, Map<String, Object> variables) {
        TargetEndpoint endpoint = parseTargetEndpoint(target);
        if (endpoint != null && endpoint.port != null) {
            return endpoint.port;
        }
        Object portValue = variables.get("Port");
        String value = objectToScalarString(portValue);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String resolveTcpHostValue(String rawHost, Map<String, Object> variables) {
        return stripTcpSchemeAndPort(firstNonBlank(HttpHandler.replaceVariablesObj(rawHost, variables)));
    }

    private String stripTcpSchemeAndPort(String hostValue) {
        String value = firstNonBlank(hostValue);
        if (value == null) {
            return null;
        }
        int schemeIndex = value.indexOf("://");
        if (schemeIndex >= 0) {
            value = value.substring(schemeIndex + 3);
        }
        int colonIndex = value.lastIndexOf(':');
        if (colonIndex > 0 && value.indexOf(']') == -1) {
            String suffix = value.substring(colonIndex + 1);
            if (suffix.matches("\\d+")) {
                value = value.substring(0, colonIndex);
            }
        }
        return firstNonBlank(value);
    }

    private String firstNonBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String objectToScalarString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List) {
            List list = (List) value;
            if (list.isEmpty()) {
                return null;
            }
            Object first = list.get(0);
            return first == null ? null : String.valueOf(first);
        }
        return String.valueOf(value);
    }

    private TargetEndpoint parseTargetEndpoint(String target) {
        String value = firstNonBlank(target);
        if (value == null) {
            return null;
        }
        try {
            String normalized = value.contains("://") ? value : "tcp://" + value;
            URL url = new URL(normalized.replaceFirst("^tcp://", "http://"));
            String host = firstNonBlank(url.getHost());
            int port = url.getPort();
            if (host == null) {
                return null;
            }
            return new TargetEndpoint(host, port > 0 ? port : null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static class TargetEndpoint {
        private final String host;
        private final Integer port;

        private TargetEndpoint(String host, Integer port) {
            this.host = host;
            this.port = port;
        }
    }

    private void addSemanticWarning(String code, String level, String protocol,
                                    String field, Object value, String action, String message) {
        Map<String, Object> warning = new HashMap<>();
        warning.put("code", code);
        warning.put("level", level);
        warning.put("protocol", protocol);
        warning.put("field", field);
        warning.put("value", value == null ? "" : String.valueOf(value));
        warning.put("action", action);
        warning.put("message", message);
        semanticWarnings.get().add(warning);
    }

    private void attachInternalWarningDetails(ScanResult result, PocObj.Poc poc) {
        if (result == null) {
            return;
        }

        List<Map<String, Object>> semantic = semanticWarnings.get();
        if (semantic != null && !semantic.isEmpty()) {
            result.addDetail("semanticWarnings", new ArrayList<>(semantic));
        }

        if (poc != null) {
            if (poc.getConversionWarnings() != null && !poc.getConversionWarnings().isEmpty()) {
                result.addDetail("conversionWarnings", new ArrayList<>(poc.getConversionWarnings()));
            }
            if (poc.getUnsupportedCapabilities() != null && !poc.getUnsupportedCapabilities().isEmpty()) {
                result.addDetail("unsupportedCapabilities", new ArrayList<>(poc.getUnsupportedCapabilities()));
            }
        }

        String flowMode = flowExecutionMode.get();
        if (flowMode == null || flowMode.trim().isEmpty()) {
            flowMode = "stepsCondition";
        }
        result.addDetail("flowExecutionMode", flowMode);
        result.addDetail("matcherPolicy", "legacy_guarded");

        int p0 = countWarningByLevel(semantic, poc, "P0");
        int p1 = countWarningByLevel(semantic, poc, "P1");
        int p2 = countWarningByLevel(semantic, poc, "P2");
        Map<String, Integer> summary = new HashMap<>();
        summary.put("P0", p0);
        summary.put("P1", p1);
        summary.put("P2", p2);
        result.addDetail("warningSummary", summary);
    }

    private int countWarningByLevel(List<Map<String, Object>> semantic, PocObj.Poc poc, String level) {
        int count = 0;
        if (semantic != null) {
            for (Map<String, Object> warning : semantic) {
                if (warning != null && level.equals(String.valueOf(warning.get("level")))) {
                    count++;
                }
            }
        }
        if (poc != null) {
            if (poc.getConversionWarnings() != null) {
                for (Map<String, Object> warning : poc.getConversionWarnings()) {
                    if (warning != null && level.equals(String.valueOf(warning.get("level")))) {
                        count++;
                    }
                }
            }
            if (poc.getUnsupportedCapabilities() != null) {
                for (Map<String, Object> warning : poc.getUnsupportedCapabilities()) {
                    if (warning != null && level.equals(String.valueOf(warning.get("level")))) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void validateNonHttpMatcherTypes(PocObj.PocStep step, String protocol) {
        if (step == null || step.getMatchers() == null) {
            return;
        }
        List<PocObj.MatcherType> allowedMatchers = "headless".equalsIgnoreCase(protocol)
                ? HEADLESS_ALLOWED_MATCHERS
                : NON_HTTP_ALLOWED_MATCHERS;
        for (PocObj.Matcher matcher : step.getMatchers()) {
            if (matcher == null || matcher.getType() == null) {
                continue;
            }
            if (!allowedMatchers.contains(matcher.getType())) {
                addSemanticWarning(
                        "NON_HTTP_MATCHER_UNSUPPORTED",
                        "P1",
                        protocol,
                        "matcher.type",
                        matcher.getType().name(),
                        "fallback",
                        "非HTTP协议收到不支持的 matcher 类型，当前将按不匹配处理"
                );
                throw new IllegalArgumentException("Unsupported matcher type for protocol " + protocol + ": " + matcher.getType());
            }
        }
    }

    private Map<String, Object> buildHeadlessRuntimeValues(HeadlessHandler.HeadlessResponse response) {
        Map<String, Object> runtime = new LinkedHashMap<String, Object>();
        if (response == null) {
            return runtime;
        }

        String body = response.getPageSource() == null ? "" : response.getPageSource();
        Object headerValue = response.getScriptResults() == null ? null : response.getScriptResults().get("header");
        Object statusValue = response.getScriptResults() == null ? null : response.getScriptResults().get("status_code");

        runtime.put("data", body);
        runtime.put("body", body);
        runtime.put("resp", body);
        runtime.put("response", body);
        runtime.put("header", toHeadlessString(headerValue));
        runtime.put("status_code", toHeadlessString(statusValue == null ? "200" : statusValue));
        runtime.put("history", "");

        if (response.getScriptResults() != null) {
            for (Map.Entry<String, Object> entry : response.getScriptResults().entrySet()) {
                if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                    continue;
                }
                runtime.put(entry.getKey(), entry.getValue());
            }
        }
        return runtime;
    }

    private void extractHeadlessVariables(List<PocObj.Matcher> extractors,
                                          Map<String, Object> headlessRuntime,
                                          Map<String, Object> variables) {
        if (extractors == null || extractors.isEmpty() || headlessRuntime == null || variables == null) {
            return;
        }
        for (PocObj.Matcher extractor : extractors) {
            if (extractor == null || extractor.getName() == null || extractor.getName().trim().isEmpty()) {
                continue;
            }
            if (!shouldIncludeHeadlessInternalExtractor(extractor)) {
                continue;
            }

            Object sourceValue = resolveHeadlessPartValue(headlessRuntime, extractor.getPart());
            String sourceText = normalizeHeadlessContent(sourceValue);
            if (sourceText == null || sourceText.isEmpty()) {
                continue;
            }

            String extractedValue = extractHeadlessValue(extractor, sourceText, headlessRuntime);
            if (extractedValue != null) {
                variables.put(extractor.getName(), extractedValue);
            }
        }
    }

    private boolean shouldIncludeHeadlessInternalExtractor(PocObj.Matcher extractor) {
        if (extractor == null) {
            return false;
        }
        String internal = extractor.getInternal();
        return internal == null || "true".equalsIgnoreCase(internal.trim()) || "false".equalsIgnoreCase(internal.trim());
    }

    private boolean matchHeadlessStep(PocObj.HeadlessStep headlessStep,
                                      Map<String, Object> runtime,
                                      Map<String, Object> variables) {
        if (headlessStep == null || headlessStep.getMatchers() == null || headlessStep.getMatchers().isEmpty()) {
            return true;
        }
        PocObj.MatchersCondition condition = headlessStep.getMatchersCondition() == null
                ? PocObj.MatchersCondition.AND
                : headlessStep.getMatchersCondition();

        if (condition == PocObj.MatchersCondition.AND) {
            for (PocObj.Matcher matcher : headlessStep.getMatchers()) {
                if (!matchSingleHeadlessMatcher(matcher, runtime, variables)) {
                    return false;
                }
            }
            return true;
        }

        for (PocObj.Matcher matcher : headlessStep.getMatchers()) {
            if (matchSingleHeadlessMatcher(matcher, runtime, variables)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchSingleHeadlessMatcher(PocObj.Matcher matcher,
                                               Map<String, Object> runtime,
                                               Map<String, Object> variables) {
        if (matcher == null) {
            return false;
        }

        boolean result;
        if (matcher.getType() == PocObj.MatcherType.GROUP) {
            PocObj.HeadlessStep nestedStep = new PocObj.HeadlessStep();
            nestedStep.setMatchers(matcher.getSubMatchers());
            nestedStep.setMatchersCondition("OR".equalsIgnoreCase(matcher.getCondition())
                    ? PocObj.MatchersCondition.OR : PocObj.MatchersCondition.AND);
            result = matchHeadlessStep(nestedStep, runtime, variables);
        } else if (matcher.getType() == PocObj.MatcherType.DSL) {
            String body = normalizeHeadlessContent(resolveHeadlessPartValue(runtime, matcher.getPart()));
            result = SimpleMatcher.matchSingle(body, matcher, buildHeadlessDslVariables(runtime, variables));
        } else if (matcher.getType() == PocObj.MatcherType.STATUS) {
            result = matchHeadlessStatusMatcher(matcher, runtime);
        } else {
            Object partValue = resolveHeadlessPartValue(runtime, matcher.getPart());
            String content = normalizeHeadlessContent(partValue);
            if (content == null) {
                content = "";
            }
            switch (matcher.getType()) {
                case WORD:
                case REGEX:
                case BINARY:
                    result = SimpleMatcher.matchSingle(content, matcher, variables);
                    break;
                case JSON:
                    result = matchHeadlessJson(matcher, content);
                    break;
                case XPATH:
                    result = matchHeadlessXpath(matcher, content);
                    break;
                case KVAL:
                    result = matchHeadlessKval(matcher, partValue, content);
                    break;
                default:
                    result = false;
                    break;
            }
        }

        return matcher.isNegative() ? !result : result;
    }

    private Map<String, Object> buildHeadlessDslVariables(Map<String, Object> runtime, Map<String, Object> variables) {
        Map<String, Object> context = new LinkedHashMap<String, Object>();
        if (variables != null) {
            context.putAll(variables);
        }
        if (runtime != null) {
            context.putAll(runtime);
        }
        Object body = resolveHeadlessPartValue(runtime, "body");
        String bodyText = normalizeHeadlessContent(body);
        context.put("response", bodyText);
        context.put("body", bodyText);
        context.put("data", bodyText);
        return context;
    }

    private boolean matchHeadlessStatusMatcher(PocObj.Matcher matcher, Map<String, Object> runtime) {
        if (matcher == null || matcher.getValues() == null || matcher.getValues().isEmpty()) {
            return false;
        }
        Object rawStatus = resolveHeadlessPartValue(runtime, "status_code");
        if (rawStatus == null) {
            return false;
        }
        try {
            int actual = Integer.parseInt(String.valueOf(rawStatus).trim());
            boolean requireAll = "AND".equalsIgnoreCase(matcher.getCondition());
            for (String value : matcher.getValues()) {
                boolean matched = false;
                try {
                    matched = actual == Integer.parseInt(value.trim());
                } catch (Exception ignored) {
                }
                if (requireAll && !matched) {
                    return false;
                }
                if (!requireAll && matched) {
                    return true;
                }
            }
            return requireAll;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean matchHeadlessJson(PocObj.Matcher matcher, String content) {
        if (matcher == null || content == null || matcher.getValues() == null || matcher.getValues().isEmpty()) {
            return false;
        }
        boolean requireAll = "AND".equalsIgnoreCase(matcher.getCondition());
        for (String value : matcher.getValues()) {
            boolean matched = com.potato.potatotool.content.redTeam.vulnScanner.extractors.JsonExtractor
                    .matchJson(content, Collections.singletonList(value));
            if (requireAll && !matched) {
                return false;
            }
            if (!requireAll && matched) {
                return true;
            }
        }
        return requireAll;
    }

    private boolean matchHeadlessXpath(PocObj.Matcher matcher, String content) {
        if (matcher == null || content == null || matcher.getValues() == null || matcher.getValues().isEmpty()) {
            return false;
        }
        boolean requireAll = "AND".equalsIgnoreCase(matcher.getCondition());
        for (String xpathExpr : matcher.getValues()) {
            String extracted = extractHeadlessXpath(content, Collections.singletonList(xpathExpr), matcher.getAttribute());
            boolean matched = extracted != null && !extracted.isEmpty();
            if (requireAll && !matched) {
                return false;
            }
            if (!requireAll && matched) {
                return true;
            }
        }
        return requireAll;
    }

    private boolean matchHeadlessKval(PocObj.Matcher matcher, Object partValue, String content) {
        if (matcher == null || matcher.getValues() == null || matcher.getValues().isEmpty()) {
            return false;
        }
        boolean requireAll = "AND".equalsIgnoreCase(matcher.getCondition());
        for (String key : matcher.getValues()) {
            String extracted = extractHeadlessKval(partValue, content, Collections.singletonList(key));
            boolean matched = extracted != null;
            if (requireAll && !matched) {
                return false;
            }
            if (!requireAll && matched) {
                return true;
            }
        }
        return requireAll;
    }

    private Object resolveHeadlessPartValue(Map<String, Object> runtime, String part) {
        if (runtime == null) {
            return null;
        }
        String normalizedPart = part == null ? "" : part.trim();
        if (normalizedPart.isEmpty() || "body".equalsIgnoreCase(normalizedPart) || "resp".equalsIgnoreCase(normalizedPart)) {
            return runtime.get("data");
        }
        if ("response".equalsIgnoreCase(normalizedPart)) {
            Object response = runtime.get("response");
            return response != null ? response : runtime.get("data");
        }
        if ("header".equalsIgnoreCase(normalizedPart)) {
            return runtime.get("header");
        }
        if ("status".equalsIgnoreCase(normalizedPart) || "status_code".equalsIgnoreCase(normalizedPart)) {
            return runtime.get("status_code");
        }
        if (runtime.containsKey(normalizedPart)) {
            return runtime.get(normalizedPart);
        }
        return runtime.get(normalizedPart.toLowerCase(Locale.ROOT));
    }

    private String normalizeHeadlessContent(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return GSON.toJson(value);
    }

    private String toHeadlessString(Object value) {
        if (value == null) {
            return "";
        }
        return value instanceof String ? (String) value : GSON.toJson(value);
    }

    private String extractHeadlessValue(PocObj.Matcher extractor, String sourceText, Map<String, Object> runtime) {
        if (extractor == null) {
            return null;
        }
        switch (extractor.getType()) {
            case REGEX:
                return extractHeadlessRegex(sourceText, extractor.getValues(), extractor.getGroup());
            case JSON:
                return extractHeadlessJson(sourceText, extractor.getValues());
            case XPATH:
                return extractHeadlessXpath(sourceText, extractor.getValues(), extractor.getAttribute());
            case KVAL:
                return extractHeadlessKval(resolveHeadlessPartValue(runtime, extractor.getPart()), sourceText, extractor.getValues());
            case DSL:
                return extractHeadlessDsl(sourceText, extractor.getValues(), runtime, extractor.getPart());
            default:
                return null;
        }
    }

    private String extractHeadlessRegex(String content, List<String> patterns, int group) {
        if (content == null || patterns == null || patterns.isEmpty()) {
            return null;
        }
        for (String regex : patterns) {
            try {
                Pattern pattern = RegexCompat.compile(regex);
                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    int effectiveGroup = group <= matcher.groupCount() && group >= 0
                            ? group
                            : (matcher.groupCount() > 0 ? 1 : 0);
                    return matcher.group(effectiveGroup);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String extractHeadlessJson(String content, List<String> expressions) {
        if (content == null || expressions == null || expressions.isEmpty()) {
            return null;
        }
        String expr = expressions.get(0);
        if (expr == null) {
            return null;
        }
        if (expr.trim().startsWith("$")) {
            return com.potato.potatotool.content.redTeam.vulnScanner.extractors.JsonExtractor
                    .extractByJsonPath(content, expr.trim());
        }
        return com.potato.potatotool.content.redTeam.vulnScanner.extractors.JsonExtractor.extractJson(content, expressions);
    }

    private String extractHeadlessXpath(String content, List<String> xpaths, String attribute) {
        if (content == null || xpaths == null || xpaths.isEmpty()) {
            return null;
        }
        try {
            org.jsoup.nodes.Document jsoupDocument = org.jsoup.Jsoup.parse(content);
            org.w3c.dom.Document document = new org.jsoup.helper.W3CDom().namespaceAware(false).fromJsoup(jsoupDocument);
            javax.xml.xpath.XPath xpath = new net.sf.saxon.xpath.XPathFactoryImpl().newXPath();
            for (String xpathExpr : xpaths) {
                if (xpathExpr == null || xpathExpr.trim().isEmpty()) {
                    continue;
                }
                String expression = xpathExpr;
                if (attribute != null && !attribute.trim().isEmpty()) {
                    expression = xpathExpr + "/@" + attribute.trim();
                }
                String result = xpath.evaluate(expression, document);
                if (result != null && !result.trim().isEmpty()) {
                    return result;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String extractHeadlessDsl(String content, List<String> expressions,
                                      Map<String, Object> runtime, String part) {
        if (expressions == null || expressions.isEmpty()) {
            return null;
        }
        Map<String, Object> context = new LinkedHashMap<String, Object>();
        if (runtime != null) {
            context.putAll(runtime);
        }
        if (content != null) {
            context.put("body", content);
            context.put("data", content);
            context.put("response", content);
        }
        if (part != null && content != null) {
            context.put(part, content);
        }
        for (String expression : expressions) {
            if (expression == null || expression.trim().isEmpty()) {
                continue;
            }
            String evaluated = DslEvaluatorRefactored.resolveValueOrFunction(expression, context);
            if (evaluated != null) {
                return evaluated;
            }
        }
        return null;
    }

    private String extractHeadlessKval(Object sourceValue, String content, List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return null;
        }
        if (sourceValue instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> sourceMap = (Map<Object, Object>) sourceValue;
            for (String key : keys) {
                if (key == null) {
                    continue;
                }
                Object value = sourceMap.get(key);
                if (value == null) {
                    value = sourceMap.get(key.trim());
                }
                if (value != null) {
                    return String.valueOf(value);
                }
            }
        }

        if (content == null) {
            return null;
        }

        for (String key : keys) {
            if (key == null) {
                continue;
            }
            try {
                com.google.gson.JsonElement element = new com.google.gson.JsonParser().parse(content);
                if (element.isJsonObject()) {
                    com.google.gson.JsonObject object = element.getAsJsonObject();
                    if (object.has(key) && !object.get(key).isJsonNull()) {
                        com.google.gson.JsonElement value = object.get(key);
                        return value.isJsonPrimitive() ? value.getAsString() : value.toString();
                    }
                }
            } catch (Exception ignored) {
            }
        }

        for (String key : keys) {
            if (key == null) {
                continue;
            }
            Pattern objectKeyPattern = Pattern.compile(Pattern.quote(key) + "\\s*[:=]\\s*['\\\"]?([^,'\\\"}\\]\\r\\n]+)");
            Matcher matcher = objectKeyPattern.matcher(content);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }

        if (keys.size() == 1 && content != null && !content.trim().isEmpty()) {
            return content;
        }
        return null;
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
            applyContextToEnvironment(pb.environment(), context);
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

    private void applyContextToEnvironment(Map<String, String> environment, Map<String, Object> context) {
        if (environment == null || context == null || context.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String key = entry.getKey();
            if (!key.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                continue;
            }
            String value = String.valueOf(entry.getValue());
            environment.put(key, value);
            environment.put(key.toUpperCase(Locale.ROOT), value);
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
            int explicitPort = url.getPort();
            int actualPort = explicitPort != -1 ? explicitPort : url.getDefaultPort();

            // {{BaseURL}} 和 {{RootURL}} - 基础URL（protocol + host + port）
            String baseURL = url.getProtocol() + "://" + hostname;
            if (explicitPort != -1 && explicitPort != url.getDefaultPort()) {
                baseURL += ":" + actualPort;
            }
            variables.put("BaseURL", Collections.singletonList(baseURL));
            variables.put("RootURL", Collections.singletonList(baseURL));

            // {{Hostname}} - 主机名
            variables.put("Hostname", Collections.singletonList(hostname));

            // {{Host}} - 主机名，避免与模板中的 {{Host}}:{{Port}} 组合时重复拼接端口
            variables.put("Host", Collections.singletonList(hostname));

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

            String randstr = generateNucleiRandstr();
            variables.put("randstr", Collections.singletonList(randstr));
            variables.put("randstr_1", Collections.singletonList(generateNucleiRandstr()));
            variables.put("randstr_2", Collections.singletonList(generateNucleiRandstr()));
            String uuid = UUID.randomUUID().toString();
            variables.put("uuid", Collections.singletonList(uuid));
            variables.put("UUID", Collections.singletonList(uuid));
            variables.put("random_uuid", Collections.singletonList(uuid));
            variables.put("random_int", Collections.singletonList(String.valueOf(new Random().nextInt(1000000))));

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
            // 变量值保留为裸域名，兼容模板中显式拼接 http:// / https:// 的写法
            variables.put("interactsh-url", Collections.singletonList("{{LAZY_INTERACTSH}}"));
            variables.put("interactsh_url", Collections.singletonList("{{LAZY_INTERACTSH}}"));

        } catch (MalformedURLException e) {
            System.err.println("[警告] 目标URL格式错误，无法提取内置变量: " + target);
            // 返回最基本的变量
            variables.put("BaseURL", Collections.singletonList(target));
            variables.put("Input", Collections.singletonList(target));
        }

        return variables;
    }

    private String generateNucleiRandstr() {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
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

            String trimmed = expression.trim();
            if (trimmed.startsWith("{{") && trimmed.endsWith("}}")) {
                String innerExpr = trimmed.substring(2, trimmed.length() - 2).trim();
                String compoundResult = evaluateCompoundExpression(innerExpr, context);
                if (compoundResult != null) {
                    return compoundResult;
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

    private String evaluateCompoundExpression(String expression, Map<String, Object> context) {
        String arithmeticResult = evaluateArithmeticExpression(expression, context);
        if (arithmeticResult != null) {
            return arithmeticResult;
        }

        return evaluateCompoundPlusExpression(expression, context);
    }

    private String evaluateArithmeticExpression(String expression, Map<String, Object> context) {
        List<String> terms = splitTopLevelArithmeticTerms(expression);
        if (terms.size() <= 1) {
            return null;
        }

        List<String> operators = extractTopLevelArithmeticOperators(expression);
        if (operators.size() != terms.size() - 1) {
            return null;
        }

        List<Double> values = new ArrayList<>();
        for (String term : terms) {
            String resolved = DslEvaluatorRefactored.resolveValueOrFunction(term, context);
            if (resolved == null) {
                return null;
            }
            String value = resolved.trim();
            if (!value.matches("^-?\\d+(\\.\\d+)?$")) {
                return null;
            }
            values.add(Double.parseDouble(value));
        }

        boolean hasMulDiv = false;
        for (String op : operators) {
            if ("*".equals(op) || "/".equals(op)) {
                hasMulDiv = true;
                break;
            }
        }

        if (hasMulDiv) {
            List<Double> reducedValues = new ArrayList<>();
            List<String> reducedOps = new ArrayList<>();
            double current = values.get(0);
            for (int i = 0; i < operators.size(); i++) {
                String op = operators.get(i);
                double next = values.get(i + 1);
                if ("*".equals(op)) {
                    current *= next;
                } else if ("/".equals(op)) {
                    current /= next;
                } else {
                    reducedValues.add(current);
                    reducedOps.add(op);
                    current = next;
                }
            }
            reducedValues.add(current);

            double result = reducedValues.get(0);
            for (int i = 0; i < reducedOps.size(); i++) {
                String op = reducedOps.get(i);
                double next = reducedValues.get(i + 1);
                if ("+".equals(op)) {
                    result += next;
                } else {
                    result -= next;
                }
            }
            return stringifyNumberResult(result, values);
        }

        double result = values.get(0);
        for (int i = 0; i < operators.size(); i++) {
            String op = operators.get(i);
            double next = values.get(i + 1);
            if ("+".equals(op)) {
                result += next;
            } else {
                result -= next;
            }
        }
        return stringifyNumberResult(result, values);
    }

    private String stringifyNumberResult(double result, List<Double> sourceValues) {
        boolean allInteger = Math.abs(result - Math.rint(result)) < 1e-9;
        if (allInteger) {
            for (Double value : sourceValues) {
                if (Math.abs(value - Math.rint(value)) >= 1e-9) {
                    allInteger = false;
                    break;
                }
            }
        }
        if (allInteger) {
            return String.valueOf((long) Math.rint(result));
        }
        return String.valueOf(result);
    }

    private List<String> splitTopLevelArithmeticTerms(String expression) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenDepth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                current.append(c);
                continue;
            }
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                current.append(c);
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote) {
                if (c == '(') {
                    parenDepth++;
                } else if (c == ')' && parenDepth > 0) {
                    parenDepth--;
                } else if ((c == '+' || c == '-' || c == '*' || c == '/') && parenDepth == 0) {
                    if ((c == '+' || c == '-') && isUnaryOperator(expression, i)) {
                        current.append(c);
                        continue;
                    }
                    parts.add(current.toString().trim());
                    current.setLength(0);
                    continue;
                }
            }

            current.append(c);
        }

        if (current.length() > 0) {
            parts.add(current.toString().trim());
        }
        return parts;
    }

    private List<String> extractTopLevelArithmeticOperators(String expression) {
        List<String> operators = new ArrayList<>();
        int parenDepth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote) {
                if (c == '(') {
                    parenDepth++;
                } else if (c == ')' && parenDepth > 0) {
                    parenDepth--;
                } else if ((c == '+' || c == '-' || c == '*' || c == '/') && parenDepth == 0) {
                    if ((c == '+' || c == '-') && isUnaryOperator(expression, i)) {
                        continue;
                    }
                    operators.add(String.valueOf(c));
                }
            }
        }

        return operators;
    }

    private boolean isUnaryOperator(String expression, int index) {
        for (int i = index - 1; i >= 0; i--) {
            char prev = expression.charAt(i);
            if (Character.isWhitespace(prev)) {
                continue;
            }
            return prev == '(' || prev == '+' || prev == '-' || prev == '*' || prev == '/';
        }
        return true;
    }

    private String evaluateCompoundPlusExpression(String expression, Map<String, Object> context) {
        List<String> parts = splitByTopLevelPlus(expression);
        if (parts.size() <= 1) {
            return null;
        }

        List<String> values = new ArrayList<>();
        boolean allNumeric = true;

        for (String part : parts) {
            String value = DslEvaluatorRefactored.resolveValueOrFunction(part, context);
            if (value == null) {
                value = "";
            }
            value = value.trim();
            values.add(value);
            if (!value.matches("^-?\\d+(\\.\\d+)?$")) {
                allNumeric = false;
            }
        }

        if (allNumeric) {
            double sum = 0;
            boolean allInteger = true;
            for (String value : values) {
                if (value.contains(".")) {
                    allInteger = false;
                }
                sum += Double.parseDouble(value);
            }
            if (allInteger) {
                return String.valueOf((long) sum);
            }
            return String.valueOf(sum);
        }

        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            sb.append(value);
        }
        return sb.toString();
    }

    private List<String> splitByTopLevelPlus(String expression) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenDepth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                current.append(c);
                continue;
            }
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                current.append(c);
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote) {
                if (c == '(') {
                    parenDepth++;
                } else if (c == ')' && parenDepth > 0) {
                    parenDepth--;
                } else if (c == '+' && parenDepth == 0) {
                    parts.add(current.toString().trim());
                    current.setLength(0);
                    continue;
                }
            }

            current.append(c);
        }

        if (current.length() > 0) {
            parts.add(current.toString().trim());
        }

        return parts;
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
        singleRawStep.setIterateAll(originalStep.isIterateAll());
        singleRawStep.setReadAll(originalStep.isReadAll());
        singleRawStep.setTimeout(originalStep.getTimeout());
        singleRawStep.setRetries(originalStep.getRetries());
        singleRawStep.setProxy(originalStep.getProxy());
        singleRawStep.setRequestVariables(originalStep.getRequestVariables());

        // 只设置当前这一个raw块
        List<String> singleRaw = new ArrayList<>();
        singleRaw.add(originalStep.getRaw().get(rawIndex));
        singleRawStep.setRaw(singleRaw);

        // 注意：不复制matchers和extractors，这些会在所有块执行完后统一处理

        return singleRawStep;
    }

    private PocObj.PocStep createPathCandidateStep(PocObj.PocStep originalStep, int pathIndex) {
        PocObj.PocStep candidateStep = new PocObj.PocStep();

        candidateStep.setStepId(originalStep.getStepId() + "_path_" + (pathIndex + 1));
        candidateStep.setMethod(originalStep.getMethod());
        candidateStep.setPath(originalStep.getPathCandidates().get(pathIndex));
        candidateStep.setHeaders(originalStep.getHeaders());
        candidateStep.setBody(originalStep.getBody());
        candidateStep.setDataType(originalStep.getDataType());
        candidateStep.setFollowRedirect(originalStep.isFollowRedirect());
        candidateStep.setCookie(originalStep.getCookie());
        candidateStep.setProxy(originalStep.getProxy());
        candidateStep.setTimeout(originalStep.getTimeout());
        candidateStep.setDelay(originalStep.getDelay());
        candidateStep.setUnsafe(originalStep.isUnsafe());
        candidateStep.setDisableCookie(originalStep.isDisableCookie());
        candidateStep.setDisablePathAutomerge(originalStep.isDisablePathAutomerge());
        candidateStep.setCache(originalStep.isCache());
        candidateStep.setIterateAll(originalStep.isIterateAll());
        candidateStep.setReadAll(originalStep.isReadAll());
        candidateStep.setEncoding(originalStep.getEncoding());
        candidateStep.setCompressed(originalStep.isCompressed());
        candidateStep.setCompressionType(originalStep.getCompressionType());
        candidateStep.setChunked(originalStep.isChunked());
        candidateStep.setAuthType(originalStep.getAuthType());
        candidateStep.setUsername(originalStep.getUsername());
        candidateStep.setPassword(originalStep.getPassword());
        candidateStep.setToken(originalStep.getToken());
        candidateStep.setRequestVariables(originalStep.getRequestVariables());
        candidateStep.setRetries(originalStep.getRetries());
        candidateStep.setRetryInterval(originalStep.getRetryInterval());
        candidateStep.setStopAtFirstMatch(originalStep.isStopAtFirstMatch());

        return candidateStep;
    }

    private static final Pattern DOUBLE_BRACE_VARIABLE_PATTERN = Pattern.compile("\\{\\{([\\w-]+)\\}\\}");
    private static final Pattern TRIPLE_BRACE_VARIABLE_PATTERN = Pattern.compile("\\{\\{\\{([\\w-]+)\\}\\}\\}");
    private static final Pattern HELPER_ARGUMENT_PATTERN = Pattern.compile("\\{\\{\\s*[A-Za-z_][\\w]*\\s*\\((.*?)\\)\\s*\\}\\}");
    private static final Pattern GOBY_FUNCTION_ARGUMENT_PATTERN = Pattern.compile("@@[A-Za-z_][\\w]*\\((.*?)\\)");

    /**
     * 检测 POC 实际使用的变量
     * 遍历所有步骤，提取 {{varName}} / {{{varName}}} 格式的变量引用
     *
     * @param steps POC步骤列表
     * @return 实际使用的变量名集合
     */
    private Set<String> detectUsedVariables(List<PocObj.PocStep> steps) {
        Set<String> usedVariables = new HashSet<>();
        if (steps == null || steps.isEmpty()) {
            return usedVariables;
        }

        for (PocObj.PocStep step : steps) {
            if (step == null) {
                continue;
            }

            // 检测不同协议类型的变量使用
            if (step instanceof PocObj.DnsStep) {
                // DNS 协议
                PocObj.DnsStep dnsStep = (PocObj.DnsStep) step;
                extractVariableNames(dnsStep.getDomain(), usedVariables);
                extractVariableNames(dnsStep.getResolver(), usedVariables);

            } else if (step instanceof PocObj.WebSocketStep) {
                // WebSocket 协议
                PocObj.WebSocketStep wsStep = (PocObj.WebSocketStep) step;
                extractVariableNames(wsStep.getAddress(), usedVariables);
                if (wsStep.getMessages() != null) {
                    for (String message : wsStep.getMessages()) {
                        extractVariableNames(message, usedVariables);
                    }
                }
                if (wsStep.getHeaders() != null) {
                    for (String headerValue : wsStep.getHeaders().values()) {
                        extractVariableNames(headerValue, usedVariables);
                    }
                }

            } else if (step instanceof PocObj.SslStep) {
                // SSL/TLS 协议
                PocObj.SslStep sslStep = (PocObj.SslStep) step;
                extractVariableNames(sslStep.getAddress(), usedVariables);

            } else if (step instanceof PocObj.FileStep) {
                // File 协议
                PocObj.FileStep fileStep = (PocObj.FileStep) step;
                if (fileStep.getPaths() != null) {
                    for (String path : fileStep.getPaths()) {
                        extractVariableNames(path, usedVariables);
                    }
                }

            } else if (step instanceof PocObj.HeadlessStep) {
                // Headless 协议
                PocObj.HeadlessStep headlessStep = (PocObj.HeadlessStep) step;
                extractVariableNames(headlessStep.getUrl(), usedVariables);
                if (headlessStep.getActions() != null) {
                    for (PocObj.BrowserAction action : headlessStep.getActions()) {
                        if (action.getArgs() != null) {
                            for (String argValue : action.getArgs().values()) {
                                extractVariableNames(argValue, usedVariables);
                            }
                        }
                    }
                }

            } else if (step instanceof PocObj.CodeStep) {
                // Code 协议
                PocObj.CodeStep codeStep = (PocObj.CodeStep) step;
                extractVariableNames(codeStep.getSource(), usedVariables);

            } else if (step instanceof PocObj.TcpStep) {
                // TCP 协议
                PocObj.TcpStep tcpStep = (PocObj.TcpStep) step;
                extractVariableNames(tcpStep.getHost(), usedVariables);
                extractVariableNames(tcpStep.getPort(), usedVariables);
                if (tcpStep.getInputs() != null) {
                    for (PocObj.Input input : tcpStep.getInputs()) {
                        extractVariableNames(input.getData(), usedVariables);
                    }
                }

            } else {
                // HTTP 协议（包括普通 PocStep）
                // 检测 path
                extractVariableNames(step.getPath(), usedVariables);

                // 检测 body
                extractVariableNames(step.getBody(), usedVariables);

                // 检测 headers
                if (step.getHeaders() != null) {
                    for (Map.Entry<String, String> header : step.getHeaders().entrySet()) {
                        extractVariableNames(header.getKey(), usedVariables);
                        extractVariableNames(header.getValue(), usedVariables);
                    }
                }

                // 检测 raw 请求
                if (step.getRaw() != null) {
                    for (String rawBlock : step.getRaw()) {
                        extractVariableNames(rawBlock, usedVariables);
                    }
                }
            }

            extractVariableNames(step.getCookie(), usedVariables);
            extractVariableNames(step.getProxy(), usedVariables);
            extractVariableNames(step.getDelay(), usedVariables);
            extractVariableNames(step.getUsername(), usedVariables);
            extractVariableNames(step.getPassword(), usedVariables);
            extractVariableNames(step.getToken(), usedVariables);
            collectMatcherVariableNames(step.getMatchers(), usedVariables);
            collectMatcherVariableNames(step.getExtractors(), usedVariables);

            // 检测 output（Xray POC 的输出变量定义）
            if (step.getOutput() != null) {
                for (Object outputValue : step.getOutput().values()) {
                    if (outputValue != null) {
                        extractVariableNames(outputValue.toString(), usedVariables);
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
     * @param usedVariables 用于收集变量名的集合
     */
    private void extractVariableNames(String input, Set<String> usedVariables) {
        if (input == null || input.isEmpty()) {
            return;
        }

        extractVariableNamesByPattern(input, DOUBLE_BRACE_VARIABLE_PATTERN, usedVariables);
        extractVariableNamesByPattern(input, TRIPLE_BRACE_VARIABLE_PATTERN, usedVariables);

        extractBareHelperArgumentNames(input, usedVariables);
        extractGobyFunctionArgumentNames(input, usedVariables);
    }

    private void extractVariableNamesByPattern(String input, Pattern pattern, Set<String> usedVariables) {
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            String varName = matcher.group(1);
            if (varName != null && !varName.isEmpty()) {
                usedVariables.add(varName);
            }
        }
    }

    private void collectMatcherVariableNames(List<PocObj.Matcher> matchers, Set<String> usedVariables) {
        if (matchers == null || matchers.isEmpty()) {
            return;
        }
        for (PocObj.Matcher matcher : matchers) {
            collectMatcherVariableNames(matcher, usedVariables);
        }
    }

    private void collectMatcherVariableNames(PocObj.Matcher matcher, Set<String> usedVariables) {
        if (matcher == null) {
            return;
        }

        extractVariableNames(matcher.getPart(), usedVariables);
        extractVariableNames(matcher.getAttribute(), usedVariables);
        extractVariableNames(matcher.getInternal(), usedVariables);

        if (matcher.getValues() != null) {
            for (String value : matcher.getValues()) {
                extractVariableNames(value, usedVariables);
            }
        }

        if (matcher.getSubMatchers() != null) {
            for (PocObj.Matcher subMatcher : matcher.getSubMatchers()) {
                collectMatcherVariableNames(subMatcher, usedVariables);
            }
        }
    }

    private void extractBareHelperArgumentNames(String input, Set<String> usedVariables) {
        Matcher helperMatcher = HELPER_ARGUMENT_PATTERN.matcher(input);
        while (helperMatcher.find()) {
            String args = helperMatcher.group(1);
            if (args == null || args.isEmpty()) {
                continue;
            }
            List<String> parts = splitHelperArguments(args);
            for (String part : parts) {
                String token = stripQuotes(part.trim());
                if (token.matches("[A-Za-z_][A-Za-z0-9_-]*")) {
                    usedVariables.add(token);
                }
            }
        }
    }

    private void extractGobyFunctionArgumentNames(String input, Set<String> usedVariables) {
        Matcher gobyFunctionMatcher = GOBY_FUNCTION_ARGUMENT_PATTERN.matcher(input);
        while (gobyFunctionMatcher.find()) {
            String args = gobyFunctionMatcher.group(1);
            if (args == null || args.isEmpty()) {
                continue;
            }
            List<String> parts = splitHelperArguments(args);
            for (String part : parts) {
                String token = stripQuotes(part.trim());
                if (token.matches("[A-Za-z_][A-Za-z0-9_-]*")) {
                    usedVariables.add(token);
                }
            }
        }
    }

    private List<String> splitHelperArguments(String args) {
        List<String> parts = new ArrayList<>();
        if (args == null || args.isEmpty()) {
            return parts;
        }

        StringBuilder current = new StringBuilder();
        int parenDepth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                current.append(c);
                continue;
            }
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                current.append(c);
                continue;
            }
            if (!inSingleQuote && !inDoubleQuote) {
                if (c == '(') {
                    parenDepth++;
                } else if (c == ')' && parenDepth > 0) {
                    parenDepth--;
                } else if (c == ',' && parenDepth == 0) {
                    parts.add(current.toString());
                    current.setLength(0);
                    continue;
                }
            }
            current.append(c);
        }
        parts.add(current.toString());
        return parts;
    }

    private String stripQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '\'' && last == '\'') || (first == '"' && last == '"')) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private Set<String> expandUsedVariableDependencies(Map<String, List<String>> allVariables,
                                                       Set<String> usedVariables) {
        Set<String> expanded = new HashSet<>();
        if (usedVariables != null) {
            expanded.addAll(usedVariables);
        }
        if (allVariables == null || allVariables.isEmpty() || expanded.isEmpty()) {
            return expanded;
        }

        ArrayDeque<String> queue = new ArrayDeque<>(expanded);
        while (!queue.isEmpty()) {
            String variableName = queue.poll();
            List<String> values = allVariables.get(variableName);
            if (values == null || values.isEmpty()) {
                continue;
            }
            for (String value : values) {
                Set<String> dependencies = new HashSet<>();
                extractVariableNames(value, dependencies);
                for (String dependency : dependencies) {
                    if (!expanded.contains(dependency) && allVariables.containsKey(dependency)) {
                        expanded.add(dependency);
                        queue.add(dependency);
                    }
                }
            }
        }

        return expanded;
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

    private void logDebug(String category, String message) {
        if (scanConfig.isDebug()) {
            ScanLogger.getInstance().debug(category, message);
        }
    }

    private void logWarn(String category, String message) {
        ScanLogger.getInstance().warn(category, message);
    }

    /**
     * 关闭执行器
     */
    public void shutdown() {
        clearThreadLocals();
        DslLogContext.clear();
        responseCache.clear();
        responseCacheService.clear();
        HttpLogService.closeClient();
    }
}
