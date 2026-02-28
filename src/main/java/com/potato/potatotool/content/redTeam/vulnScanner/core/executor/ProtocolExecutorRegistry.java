package com.potato.potatotool.content.redTeam.vulnScanner.core.executor;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协议执行器注册表
 * 管理所有协议执行器，提供统一的执行入口
 * 
 * 使用方式：
 * 1. 通过 getInstance() 获取单例
 * 2. 使用 register() 注册新的协议执行器
 * 3. 使用 execute() 执行协议步骤
 * 
 * @author Potato
 * @date 2025-11-26
 */
public class ProtocolExecutorRegistry {
    
    private static volatile ProtocolExecutorRegistry instance;
    
    /**
     * 按协议名称索引的执行器
     */
    private final Map<String, IProtocolExecutor<?>> executorsByProtocol;
    
    /**
     * 所有已注册的执行器列表
     */
    private final List<IProtocolExecutor<?>> allExecutors;
    
    private ProtocolExecutorRegistry() {
        this.executorsByProtocol = new ConcurrentHashMap<>();
        this.allExecutors = new ArrayList<>();
        
        // 注册默认执行器
        registerDefaultExecutors();
    }
    
    /**
     * 获取单例实例
     */
    public static ProtocolExecutorRegistry getInstance() {
        if (instance == null) {
            synchronized (ProtocolExecutorRegistry.class) {
                if (instance == null) {
                    instance = new ProtocolExecutorRegistry();
                }
            }
        }
        return instance;
    }
    
    /**
     * 注册默认的协议执行器
     */
    private void registerDefaultExecutors() {
        // 注册 DNS 执行器
        register(new DnsProtocolExecutor());
        
        // 其他协议执行器可以在这里添加
        // register(new WebSocketProtocolExecutor());
        // register(new SslProtocolExecutor());
        // register(new FileProtocolExecutor());
        // register(new HeadlessProtocolExecutor());
        // register(new CodeProtocolExecutor());
    }
    
    /**
     * 注册协议执行器
     * 
     * @param executor 协议执行器
     */
    public void register(IProtocolExecutor<?> executor) {
        if (executor == null) {
            return;
        }
        
        executorsByProtocol.put(executor.getProtocol().toLowerCase(), executor);
        allExecutors.add(executor);
        
        System.out.println("已注册协议执行器: " + executor.getProtocol());
    }
    
    /**
     * 获取指定协议的执行器
     * 
     * @param protocol 协议名称
     * @return 执行器（可能为空）
     */
    public Optional<IProtocolExecutor<?>> getExecutor(String protocol) {
        if (protocol == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(executorsByProtocol.get(protocol.toLowerCase()));
    }
    
    /**
     * 根据步骤类型查找支持的执行器
     * 
     * @param step POC步骤
     * @return 支持该步骤的执行器（可能为空）
     */
    public Optional<IProtocolExecutor<?>> findExecutorForStep(PocObj.PocStep step) {
        for (IProtocolExecutor<?> executor : allExecutors) {
            if (executor.supports(step)) {
                return Optional.of(executor);
            }
        }
        return Optional.empty();
    }
    
    /**
     * 执行协议步骤
     * 
     * @param step POC步骤
     * @param variables 变量映射
     * @param context 执行上下文
     * @return 执行结果
     */
    @SuppressWarnings("unchecked")
    public IProtocolExecutor.ExecutionResult execute(
            PocObj.PocStep step, 
            Map<String, String> variables,
            IProtocolExecutor.ExecutionContext context) {
        
        Optional<IProtocolExecutor<?>> executorOpt = findExecutorForStep(step);
        
        if (!executorOpt.isPresent()) {
            return IProtocolExecutor.ExecutionResult.failure(
                "未找到支持该步骤类型的协议执行器: " + step.getClass().getSimpleName());
        }
        
        try {
            IProtocolExecutor executor = executorOpt.get();
            return executor.execute(step, variables, context);
        } catch (Exception e) {
            return IProtocolExecutor.ExecutionResult.failure(
                "协议执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查是否支持指定协议
     */
    public boolean supportsProtocol(String protocol) {
        return protocol != null && executorsByProtocol.containsKey(protocol.toLowerCase());
    }
    
    /**
     * 检查是否支持指定步骤类型
     */
    public boolean supportsStep(PocObj.PocStep step) {
        return findExecutorForStep(step).isPresent();
    }
    
    /**
     * 获取所有已注册的协议列表
     */
    public List<String> getRegisteredProtocols() {
        return new ArrayList<>(executorsByProtocol.keySet());
    }
    
    /**
     * 获取注册统计信息
     */
    public String getStats() {
        return String.format("协议执行器注册表: %d 个协议 (%s)", 
            executorsByProtocol.size(), 
            String.join(", ", getRegisteredProtocols()));
    }
}
