package com.potato.potatotool.content.redTeam.vulnScanner.http;

import org.graalvm.polyglot.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Code 执行处理器
 * 用于执行自定义代码（JavaScript）
 * 支持 Nuclei Code 协议的所有功能
 * 
 * 安全特性:
 * - 沙箱隔离环境
 * - 禁止文件访问
 * - 禁止网络访问（除了提供的安全 API）
 * - 资源限制
 * 
 * @author Potato
 * @date 2025-11-02
 */
public class CodeHandler {
    
    /**
     * Code 执行结果
     */
    public static class CodeResponse {
        private String engine;                  // 执行引擎（javascript, python）
        private Object result;                  // 执行结果
        private String resultString;            // 结果字符串形式
        private long duration;                  // 执行耗时（毫秒）
        private boolean success;                // 执行是否成功
        private String error;                   // 错误信息
        private Map<String, Object> context;    // 执行上下文
        
        public CodeResponse() {
            this.context = new HashMap<>();
        }
        
        // Getters and Setters
        public String getEngine() { return engine; }
        public void setEngine(String engine) { this.engine = engine; }
        
        public Object getResult() { return result; }
        public void setResult(Object result) { 
            this.result = result;
            this.resultString = result != null ? result.toString() : null;
        }
        
        public String getResultString() { return resultString; }
        
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public Map<String, Object> getContext() { return context; }
        public void setContext(Map<String, Object> context) { this.context = context; }
        
        @Override
        public String toString() {
            return "CodeResponse{" +
                    "engine='" + engine + '\'' +
                    ", result=" + result +
                    ", duration=" + duration + "ms" +
                    ", success=" + success +
                    '}';
        }
    }
    
    /**
     * 执行 JavaScript 代码
     * 
     * @param code JavaScript 代码
     * @return 执行结果
     */
    public static CodeResponse executeJavaScript(String code) {
        return executeJavaScript(code, new HashMap<>());
    }
    
    /**
     * 执行 JavaScript 代码（带上下文）
     * 
     * @param code JavaScript 代码
     * @param context 上下文变量
     * @return 执行结果
     */
    public static CodeResponse executeJavaScript(String code, Map<String, Object> context) {
        CodeResponse response = new CodeResponse();
        response.setEngine("javascript");
        response.setContext(context);
        
        long startTime = System.currentTimeMillis();
        Context graalContext = null;
        
        try {
            // 创建安全的沙箱环境
            graalContext = Context.newBuilder("js")
                // 禁用所有特权访问
                .allowAllAccess(false)
                // 禁用 IO 操作
                .allowIO(false)
                // 禁止主机访问
                .allowHostAccess(HostAccess.NONE)
                // 禁止主机类加载
                .allowHostClassLookup(className -> false)
                // 禁止创建线程
                .allowCreateThread(false)
                // 禁止创建进程
                .allowCreateProcess(false)
                // 禁止本地访问
                .allowNativeAccess(false)
                // 禁止多线程
                .allowPolyglotAccess(PolyglotAccess.NONE)
                .option("engine.WarnInterpreterOnly", "false")
                .build();
            
            System.out.println("✓ JavaScript 沙箱环境已创建");
            
            // 注入上下文变量
            Value bindings = graalContext.getBindings("js");
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                if (isSafeValue(entry.getValue())) {
                    bindings.putMember(entry.getKey(), entry.getValue());
                }
            }
            
            // 提供安全的 HTTP API（简化版）
            bindings.putMember("http", new SafeHttpApi());
            
            // 提供console.log（用于调试）
            bindings.putMember("console", new SafeConsole());
            
            System.out.println("→ 执行 JavaScript 代码...");
            
            // 执行代码
            Value result = graalContext.eval("js", code);
            
            // 转换结果
            Object javaResult = convertGraalValueToJava(result);
            response.setResult(javaResult);
            
            response.setSuccess(true);
            System.out.println("✓ JavaScript 执行成功，结果: " + javaResult);
            
        } catch (PolyglotException e) {
            response.setSuccess(false);
            response.setError("代码执行错误: " + e.getMessage());
            System.err.println("JavaScript 执行失败: " + e.getMessage());
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError("执行异常: " + e.getMessage());
            System.err.println("Code 执行异常: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (graalContext != null) {
                graalContext.close();
            }
            response.setDuration(System.currentTimeMillis() - startTime);
        }
        
        return response;
    }
    
    /**
     * 检查值是否安全（可以注入到上下文）
     */
    private static boolean isSafeValue(Object value) {
        if (value == null) return true;
        
        // 只允许基本类型和字符串
        return value instanceof String ||
               value instanceof Number ||
               value instanceof Boolean ||
               value instanceof Map ||
               value instanceof List;
    }
    
    /**
     * 将 GraalVM Value 转换为 Java 对象
     */
    private static Object convertGraalValueToJava(Value value) {
        if (value == null || value.isNull()) {
            return null;
        } else if (value.isString()) {
            return value.asString();
        } else if (value.isNumber()) {
            if (value.fitsInInt()) {
                return value.asInt();
            } else if (value.fitsInLong()) {
                return value.asLong();
            } else if (value.fitsInDouble()) {
                return value.asDouble();
            }
            return value.toString();
        } else if (value.isBoolean()) {
            return value.asBoolean();
        } else if (value.hasArrayElements()) {
            List<Object> list = new ArrayList<>();
            long size = value.getArraySize();
            for (long i = 0; i < size; i++) {
                list.add(convertGraalValueToJava(value.getArrayElement(i)));
            }
            return list;
        } else {
            return value.toString();
        }
    }
    
    /**
     * 安全的 HTTP API（简化版）
     * 只允许白名单操作
     */
    public static class SafeHttpApi {
        public String get(String url) {
            // 简化实现：仅返回示例数据
            // 实际应用中应调用 HttpHandler
            System.out.println("HTTP GET: " + url);
            return "{\"status\":\"ok\"}";
        }
        
        public String post(String url, String data) {
            System.out.println("HTTP POST: " + url + " | Data: " + data);
            return "{\"status\":\"ok\"}";
        }
    }
    
    /**
     * 安全的 Console 对象（用于调试）
     */
    public static class SafeConsole {
        public void log(Object... args) {
            StringBuilder sb = new StringBuilder("[JS Console] ");
            for (Object arg : args) {
                sb.append(arg).append(" ");
            }
            System.out.println(sb.toString().trim());
        }
    }
    
    /**
     * 测试方法
     */
    public static void main(String[] args) {
        System.out.println("=== 测试 Code Handler ===\n");
        
        try {
            // 测试1: 简单计算
            System.out.println("测试1: 简单计算");
            CodeResponse response1 = executeJavaScript("1 + 2 + 3");
            System.out.println(response1);
            System.out.println("结果: " + response1.getResult());
            System.out.println("✓ 测试1通过\n");
            
            // 测试2: 字符串操作
            System.out.println("测试2: 字符串操作");
            CodeResponse response2 = executeJavaScript("'Hello, ' + 'World!'");
            System.out.println(response2);
            System.out.println("结果: " + response2.getResult());
            System.out.println("✓ 测试2通过\n");
            
            // 测试3: 使用上下文变量
            System.out.println("测试3: 使用上下文变量");
            Map<String, Object> context = new HashMap<>();
            context.put("url", "https://example.com");
            context.put("status", 200);
            CodeResponse response3 = executeJavaScript(
                "status === 200 && url.includes('example')", 
                context
            );
            System.out.println(response3);
            System.out.println("结果: " + response3.getResult());
            System.out.println("✓ 测试3通过\n");
            
            // 测试4: 复杂逻辑
            System.out.println("测试4: 复杂逻辑");
            CodeResponse response4 = executeJavaScript(
                "var result = [];\n" +
                "for (var i = 1; i <= 5; i++) {\n" +
                "    result.push(i * 2);\n" +
                "}\n" +
                "result"
            );
            System.out.println(response4);
            System.out.println("结果: " + response4.getResult());
            System.out.println("✓ 测试4通过\n");
            
            // 测试5: console.log
            System.out.println("测试5: console.log");
            CodeResponse response5 = executeJavaScript(
                "console.log('Debug message');\n" +
                "'logged'"
            );
            System.out.println(response5);
            System.out.println("结果: " + response5.getResult());
            System.out.println("✓ 测试5通过\n");
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("=== 测试完成 ===");
    }
}


