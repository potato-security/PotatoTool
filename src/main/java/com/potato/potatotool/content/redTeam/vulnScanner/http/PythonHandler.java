package com.potato.potatotool.content.redTeam.vulnScanner.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.potato.potatotool.utils.core.EnvPathConfig;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Python 代码执行器
 * 支持执行 Nuclei Code 协议和 Pocsuite 的 Python POC
 * 
 * 特点：
 * 1. 优先使用持久化配置的 Python 路径
 * 2. 未配置时自动检测并回写 EnvPath
 * 3. 通过 JSON 传递上下文变量
 * 4. 安全的临时文件管理
 * 
 * @author Potato
 * @date 2025-11-26
 */
public class PythonHandler {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Python 路径配置（可由用户设置）
    private static volatile String pythonPath = null;
    
    // 默认超时时间（秒）
    private static final int DEFAULT_TIMEOUT = 30;
    
    // 常见的 Python 路径
    private static final String[] COMMON_PYTHON_PATHS = {
        // macOS / Linux
        "/usr/bin/python3",
        "/usr/bin/python",
        "/usr/local/bin/python3",
        "/usr/local/bin/python",
        "/opt/homebrew/bin/python3",
        "/opt/homebrew/bin/python",
        // Windows
        "C:\\Python311\\python.exe",
        "C:\\Python310\\python.exe",
        "C:\\Python39\\python.exe",
        "C:\\Python38\\python.exe",
        "C:\\Python37\\python.exe",
        "python3",
        "python"
    };
    
    /**
     * Python 执行结果
     */
    public static class PythonResponse {
        private boolean success;
        private Object result;
        private String output;      // stdout
        private String error;       // stderr
        private int exitCode;
        private long executionTime; // 执行时间（毫秒）
        
        public PythonResponse(boolean success, Object result, String output, String error, int exitCode, long executionTime) {
            this.success = success;
            this.result = result;
            this.output = output;
            this.error = error;
            this.exitCode = exitCode;
            this.executionTime = executionTime;
        }
        
        public boolean isSuccess() { return success; }
        public Object getResult() { return result; }
        public String getOutput() { return output; }
        public String getError() { return error; }
        public int getExitCode() { return exitCode; }
        public long getExecutionTime() { return executionTime; }
        
        public String getResultString() {
            if (result == null) return output != null ? output : "";
            return result.toString();
        }
    }
    
    /**
     * 设置 Python 路径
     * @param path Python 可执行文件路径
     */
    public static synchronized void setPythonPath(String path) {
        String trimmed = path == null ? "" : path.trim();
        if (trimmed.isEmpty()) {
            pythonPath = null;
            return;
        }
        if (validatePythonPath(trimmed)) {
            pythonPath = trimmed;
            if (!trimmed.equals(EnvPathConfig.getPythonPath())) {
                EnvPathConfig.savePythonPath(trimmed);
            }
        }
    }

    public static synchronized void resetPythonPath() {
        pythonPath = null;
    }

    public static synchronized void initializeAtStartup() {
        String configuredPath = EnvPathConfig.getPythonPath();
        if (configuredPath != null && !configuredPath.trim().isEmpty() && validatePythonPath(configuredPath)) {
            pythonPath = configuredPath.trim();
            return;
        }
        pythonPath = resolvePythonPath();
    }
    
    /**
     * 获取当前 Python 路径
     * @return Python 路径，如果未设置则自动检测
     */
    public static synchronized String getPythonPath() {
        if (pythonPath != null) {
            return pythonPath;
        }
        pythonPath = resolvePythonPath();
        return pythonPath;
    }
    
    /**
     * 检查 Python 是否可用
     * @return 是否可用
     */
    public static boolean isPythonAvailable() {
        return getPythonPath() != null;
    }
    
    /**
     * 获取 Python 版本信息
     * @return 版本信息
     */
    public static String getPythonVersion() {
        String python = getPythonPath();
        if (python == null) {
            return "Python 不可用";
        }
        
        try {
            ProcessBuilder pb = new ProcessBuilder(python, "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String version = reader.readLine();
                process.waitFor(5, TimeUnit.SECONDS);
                return version != null ? version : "未知版本";
            }
        } catch (Exception e) {
            return "获取版本失败: " + e.getMessage();
        }
    }
    
    /**
     * 自动检测系统 Python 路径
     * @return Python 路径，如果未找到返回 null
     */
    public static String detectPythonPath() {
        // 1. 先检查环境变量
        String envPython = System.getenv("PYTHON_PATH");
        if (envPython != null && validatePythonPath(envPython)) {
            return envPython;
        }
        
        // 2. 检查常见路径
        for (String path : COMMON_PYTHON_PATHS) {
            if (validatePythonPath(path)) {
                return path;
            }
        }
        
        // 3. 尝试使用 which/where 命令
        String whichResult = tryWhichCommand();
        if (whichResult != null) {
            return whichResult;
        }
        return null;
    }

    private static String resolvePythonPath() {
        String configuredPath = EnvPathConfig.getPythonPath();
        if (configuredPath != null && !configuredPath.trim().isEmpty() && validatePythonPath(configuredPath)) {
            return configuredPath.trim();
        }

        String detectedPath = detectPythonPath();
        if (detectedPath != null && !detectedPath.trim().isEmpty()) {
            EnvPathConfig.savePythonPath(detectedPath);
            return detectedPath;
        }
        return null;
    }
    
    /**
     * 验证 Python 路径是否有效
     */
    private static boolean validatePythonPath(String path) {
        try {
            ProcessBuilder pb = new ProcessBuilder(path, "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }
    
    /**
     * 尝试使用 which/where 命令查找 Python
     */
    private static String tryWhichCommand() {
        String os = System.getProperty("os.name").toLowerCase();
        String[] commands;
        
        if (os.contains("win")) {
            commands = new String[]{"where", "python"};
        } else {
            commands = new String[]{"which", "python3"};
        }
        
        try {
            ProcessBuilder pb = new ProcessBuilder(commands);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                process.waitFor(5, TimeUnit.SECONDS);
                if (line != null && !line.isEmpty() && validatePythonPath(line.trim())) {
                    return line.trim();
                }
            }
        } catch (Exception ignored) {
        }
        
        // 尝试 python (不带3)
        if (!os.contains("win")) {
            try {
                ProcessBuilder pb = new ProcessBuilder("which", "python");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line = reader.readLine();
                    process.waitFor(5, TimeUnit.SECONDS);
                    if (line != null && !line.isEmpty() && validatePythonPath(line.trim())) {
                        return line.trim();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        
        return null;
    }
    
    /**
     * 执行 Python 代码
     * 
     * @param source Python 源代码
     * @param context 上下文变量（将作为 JSON 传递给脚本）
     * @return 执行结果
     */
    public static PythonResponse executePython(String source, Map<String, Object> context) {
        return executePython(source, context, DEFAULT_TIMEOUT);
    }
    
    /**
     * 执行 Python 代码（带超时）
     * 
     * @param source Python 源代码
     * @param context 上下文变量
     * @param timeoutSeconds 超时时间（秒）
     * @return 执行结果
     */
    public static PythonResponse executePython(String source, Map<String, Object> context, int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        
        String python = getPythonPath();
        if (python == null) {
            return new PythonResponse(false, null, "", "Python 不可用，请先配置 Python 路径", -1, 0);
        }
        
        Path tempScript = null;
        Path tempContext = null;
        Path tempResult = null;
        
        try {
            // 1. 创建临时文件
            tempScript = Files.createTempFile("poc_", ".py");
            tempContext = Files.createTempFile("poc_context_", ".json");
            tempResult = Files.createTempFile("poc_result_", ".json");
            
            // 2. 写入上下文 JSON
            String contextJson = GSON.toJson(context != null ? context : new HashMap<>());
            Files.write(tempContext, contextJson.getBytes(StandardCharsets.UTF_8));
            
            // 3. 包装 Python 代码，注入上下文读取和结果输出逻辑
            String wrappedSource = wrapPythonCode(source, tempContext.toString(), tempResult.toString());
            Files.write(tempScript, wrappedSource.getBytes(StandardCharsets.UTF_8));
            
            // 4. 执行 Python 脚本
            ProcessBuilder pb = new ProcessBuilder(python, tempScript.toString());
            applyContextToEnvironment(pb.environment(), context);
            pb.redirectErrorStream(false);
            
            Process process = pb.start();
            
            // 读取输出
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            
            Thread stdoutThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdout.append(line).append("\n");
                    }
                } catch (IOException ignored) {}
            });
            
            Thread stderrThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderr.append(line).append("\n");
                    }
                } catch (IOException ignored) {}
            });
            
            stdoutThread.start();
            stderrThread.start();
            
            // 等待执行完成
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return new PythonResponse(false, null, stdout.toString(), 
                    "执行超时（" + timeoutSeconds + "秒）", -1, System.currentTimeMillis() - startTime);
            }
            
            stdoutThread.join(1000);
            stderrThread.join(1000);
            
            int exitCode = process.exitValue();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 5. 读取结果
            Object result = null;
            if (Files.exists(tempResult) && Files.size(tempResult) > 0) {
                String resultJson = new String(Files.readAllBytes(tempResult), StandardCharsets.UTF_8);
                try {
                    result = GSON.fromJson(resultJson, Object.class);
                } catch (Exception e) {
                    result = resultJson;
                }
            }
            
            boolean success = exitCode == 0;
            return new PythonResponse(success, result, stdout.toString().trim(), 
                stderr.toString().trim(), exitCode, executionTime);
            
        } catch (Exception e) {
            return new PythonResponse(false, null, "", 
                "Python 执行异常: " + e.getMessage(), -1, System.currentTimeMillis() - startTime);
        } finally {
            // 清理临时文件
            deleteTempFile(tempScript);
            deleteTempFile(tempContext);
            deleteTempFile(tempResult);
        }
    }

    private static void applyContextToEnvironment(Map<String, String> environment, Map<String, Object> context) {
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
     * 包装 Python 代码，注入上下文和结果处理
     */
    private static String wrapPythonCode(String source, String contextPath, String resultPath) {
        StringBuilder sb = new StringBuilder();
        
        // 导入必要的模块
        sb.append("# -*- coding: utf-8 -*-\n");
        sb.append("import json\n");
        sb.append("import sys\n");
        sb.append("import os\n");
        sb.append("\n");
        
        // 读取上下文
        sb.append("# 读取上下文变量\n");
        sb.append("_context = {}\n");
        sb.append("try:\n");
        sb.append("    with open(r'").append(escapeString(contextPath)).append("', 'r', encoding='utf-8') as f:\n");
        sb.append("        _context = json.load(f)\n");
        sb.append("except Exception as e:\n");
        sb.append("    print(f'Warning: Failed to load context: {e}', file=sys.stderr)\n");
        sb.append("\n");
        
        // 将上下文变量注入到全局命名空间
        sb.append("# 注入上下文变量到全局命名空间\n");
        sb.append("for _k, _v in _context.items():\n");
        sb.append("    globals()[_k] = _v\n");
        sb.append("\n");
        
        // 提供 POC 辅助函数
        sb.append("# POC 辅助函数\n");
        sb.append("def set_result(data):\n");
        sb.append("    \"\"\"设置 POC 执行结果\"\"\"\n");
        sb.append("    global _poc_result\n");
        sb.append("    _poc_result = data\n");
        sb.append("\n");
        sb.append("def get_context(key, default=None):\n");
        sb.append("    \"\"\"获取上下文变量\"\"\"\n");
        sb.append("    return _context.get(key, default)\n");
        sb.append("\n");
        sb.append("_poc_result = None\n");
        sb.append("\n");
        
        // 用户代码
        sb.append("# ========== 用户代码开始 ==========\n");
        sb.append(source);
        sb.append("\n# ========== 用户代码结束 ==========\n");
        sb.append("\n");
        
        // 保存结果
        sb.append("# 保存执行结果\n");
        sb.append("try:\n");
        sb.append("    with open(r'").append(escapeString(resultPath)).append("', 'w', encoding='utf-8') as f:\n");
        sb.append("        json.dump(_poc_result, f, ensure_ascii=False)\n");
        sb.append("except Exception as e:\n");
        sb.append("    print(f'Warning: Failed to save result: {e}', file=sys.stderr)\n");
        
        return sb.toString();
    }
    
    /**
     * 转义字符串中的特殊字符
     */
    private static String escapeString(String str) {
        return str.replace("\\", "\\\\").replace("'", "\\'");
    }
    
    /**
     * 删除临时文件
     */
    private static void deleteTempFile(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {}
        }
    }
}
