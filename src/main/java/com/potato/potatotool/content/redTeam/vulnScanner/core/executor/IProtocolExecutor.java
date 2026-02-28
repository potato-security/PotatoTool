package com.potato.potatotool.content.redTeam.vulnScanner.core.executor;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import java.util.Map;

/**
 * 协议执行器接口
 * 定义了不同协议（HTTP、DNS、WebSocket、SSL等）的执行契约
 * 
 * 设计目的：
 * 1. 解耦 PocExecutor 中的协议特定逻辑
 * 2. 便于扩展新协议支持
 * 3. 提高代码可测试性
 * 
 * @author Potato
 * @date 2025-11-26
 */
public interface IProtocolExecutor<T extends PocObj.PocStep> {
    
    /**
     * 获取执行器支持的协议类型
     * @return 协议类型标识（如 "http", "dns", "websocket" 等）
     */
    String getProtocol();
    
    /**
     * 检查是否支持执行指定的步骤类型
     * @param step POC步骤
     * @return 是否支持
     */
    boolean supports(PocObj.PocStep step);
    
    /**
     * 执行协议步骤
     * 
     * @param step 协议特定的步骤对象
     * @param variables 变量映射（包含已提取的变量和payload）
     * @param context 执行上下文
     * @return 执行结果
     */
    ExecutionResult execute(T step, Map<String, String> variables, ExecutionContext context);
    
    /**
     * 执行结果
     */
    class ExecutionResult {
        private final boolean success;
        private final String message;
        private final Map<String, String> extractedVariables;
        private final Object response;
        
        private ExecutionResult(boolean success, String message, 
                                Map<String, String> extractedVariables, Object response) {
            this.success = success;
            this.message = message;
            this.extractedVariables = extractedVariables;
            this.response = response;
        }
        
        public static ExecutionResult success() {
            return new ExecutionResult(true, null, null, null);
        }
        
        public static ExecutionResult success(String message) {
            return new ExecutionResult(true, message, null, null);
        }
        
        public static ExecutionResult success(Map<String, String> extractedVariables) {
            return new ExecutionResult(true, null, extractedVariables, null);
        }
        
        public static ExecutionResult success(Object response, Map<String, String> extractedVariables) {
            return new ExecutionResult(true, null, extractedVariables, response);
        }
        
        public static ExecutionResult failure(String message) {
            return new ExecutionResult(false, message, null, null);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Map<String, String> getExtractedVariables() { return extractedVariables; }
        public Object getResponse() { return response; }
    }
    
    /**
     * 执行上下文
     * 包含执行所需的配置和缓存
     */
    class ExecutionContext {
        private final String target;
        private final PocObj.GlobalConfig globalConfig;
        private final Object responseCache;
        private final int timeout;
        private final String proxy;
        
        public ExecutionContext(String target, PocObj.GlobalConfig globalConfig,
                               Object responseCache, int timeout, String proxy) {
            this.target = target;
            this.globalConfig = globalConfig;
            this.responseCache = responseCache;
            this.timeout = timeout;
            this.proxy = proxy;
        }
        
        public String getTarget() { return target; }
        public PocObj.GlobalConfig getGlobalConfig() { return globalConfig; }
        public Object getResponseCache() { return responseCache; }
        public int getTimeout() { return timeout; }
        public String getProxy() { return proxy; }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private String target;
            private PocObj.GlobalConfig globalConfig;
            private Object responseCache;
            private int timeout = 30;
            private String proxy;
            
            public Builder target(String target) {
                this.target = target;
                return this;
            }
            
            public Builder globalConfig(PocObj.GlobalConfig config) {
                this.globalConfig = config;
                return this;
            }
            
            public Builder responseCache(Object cache) {
                this.responseCache = cache;
                return this;
            }
            
            public Builder timeout(int timeout) {
                this.timeout = timeout;
                return this;
            }
            
            public Builder proxy(String proxy) {
                this.proxy = proxy;
                return this;
            }
            
            public ExecutionContext build() {
                return new ExecutionContext(target, globalConfig, responseCache, timeout, proxy);
            }
        }
    }
}
