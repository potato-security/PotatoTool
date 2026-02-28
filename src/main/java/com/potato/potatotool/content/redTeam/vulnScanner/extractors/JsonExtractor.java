package com.potato.potatotool.content.redTeam.vulnScanner.extractors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JSON提取工具类
 * 支持两种语法：
 * 1. jq语法（如 .data.name）- 用于Xray POC
 * 2. JsonPath语法（如 $.data.name）- 用于Goby POC
 * 
 * @author Potato
 * @date 2025/2/21 15:00
 * @updated 2025-11-01 新增JsonPath完整语法支持
 */
public class JsonExtractor {
    private static final Scope SCOPE = Scope.newEmptyScope();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    static {
        // 使用BuiltinFunctionLoader加载内置函数
        BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_6, SCOPE);
    }

    private JsonExtractor() {
        // 私有构造函数，防止实例化
    }

    /**
     * 从JSON内容中提取值
     * @param content JSON字符串内容
     * @param jqExprs jq表达式列表
     * @return 提取的内容（多个结果用逗号分隔）
     */
    public static String extractJson(String content, List<String> jqExprs) {
        // ========== 内容���型预检测 ==========
        // 1. 检查空内容
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        String trimmed = content.trim();

        // 2. 检测 HTML/XML 内容（以 < 开头）
        if (trimmed.startsWith("<") || trimmed.startsWith("<!DOCTYPE")) {
            // 跳过非 JSON 内容，避免抛出异常
            return null;
        }

        // 3. 快速验证 JSON 格式（必须以 { 或 [ 开头）
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            // 不是有效的 JSON 格式
            return null;
        }

        // ========== 正常的 JSON 解析流程 ==========
        try {
            JsonNode root = OBJECT_MAPPER.readTree(content);
            List<String> results = new ArrayList<>();

            for (String expr : jqExprs) {
                try {
                    JsonQuery query = JsonQuery.compile(expr, Versions.JQ_1_6);
                    List<JsonNode> nodes = new ArrayList<>();
                    query.apply(SCOPE, root, nodes::add);
                    nodes.stream()
                            .map(node -> node.isTextual() ? node.asText() : node.toString())
                            .forEach(results::add);
                } catch (JsonQueryException e) {
                    System.out.println("编译/应用jq-expr失败: "+ expr + e);
                }
            }
            return String.join(",", results);
        } catch (Exception e) {
            // 只在确实是 JSON 格式错误时才记录（已通过预检测的内容）
            System.out.println("JSON解析错误: " + e.getClass().getSimpleName());
            return null;
        }
    }

    public static boolean matchJson(String content, List<String> jqExprs) {
        String data = extractJson(content,jqExprs);
        return (data!=null && data!="")? true: false;
    }

    /**
     * 使用JsonPath语法从JSON中提取值（用于Goby POC）
     * 支持完整的JsonPath语法：
     * - $.data.name                简单路径
     * - $.data[*].name             通配符
     * - $.data[?(@.id>100)]        过滤表达式
     * - $..name                    递归搜索
     * - $.data[0:3]                数组切片
     * 
     * @param jsonContent JSON字符串
     * @param jsonPath JsonPath表达式（如 $.data.name）
     * @return 提取的值（如果是数组则返回第一个元素的字符串形式）
     */
    public static String extractByJsonPath(String jsonContent, String jsonPath) {
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            return null;
        }
        
        if (jsonPath == null || jsonPath.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 使用JsonPath提取数据
            Object result = JsonPath.read(jsonContent, jsonPath);
            
            if (result == null) {
                return null;
            }
            
            // 处理不同类型的返回值
            if (result instanceof List) {
                List<?> resultList = (List<?>) result;
                if (resultList.isEmpty()) {
                    return null;
                }
                // 返回第一个元素
                Object firstElement = resultList.get(0);
                return firstElement == null ? null : firstElement.toString();
            } else {
                return result.toString();
            }
            
        } catch (PathNotFoundException e) {
            // 路径未找到，返回null
            System.out.println("JsonPath路径未找到: " + jsonPath);
            return null;
        } catch (Exception e) {
            // 其他错误，尝试降级到简单提取
            System.out.println("JsonPath提取失败: " + jsonPath + ", 错误: " + e.getMessage());
            return extractBySimpleJsonPath(jsonContent, jsonPath);
        }
    }
    
    /**
     * 简单的JSON路径提取（降级方案）
     * 仅支持基本的路径语法，如 $.data.name 或 $.data[0].name
     * 
     * @param jsonContent JSON内容
     * @param jsonPath JsonPath表达式
     * @return 提取的值
     */
    private static String extractBySimpleJsonPath(String jsonContent, String jsonPath) {
        try {
            // 移除开头的 $. 或 $
            String path = jsonPath;
            if (path.startsWith("$.")) {
                path = path.substring(2);
            } else if (path.startsWith("$")) {
                path = path.substring(1);
            }
            
            // 使用Gson解析
            JsonElement root = JsonParser.parseString(jsonContent);
            
            // 逐级访问
            String[] parts = path.split("\\.");
            JsonElement current = root;
            
            for (String part : parts) {
                if (current == null || !current.isJsonObject()) {
                    return null;
                }
                
                // 处理数组索引 [0]
                if (part.contains("[")) {
                    String fieldName = part.substring(0, part.indexOf("["));
                    String indexStr = part.substring(part.indexOf("[") + 1, part.indexOf("]"));
                    int index = Integer.parseInt(indexStr);
                    
                    current = current.getAsJsonObject().get(fieldName);
                    if (current != null && current.isJsonArray()) {
                        current = current.getAsJsonArray().get(index);
                    } else {
                        return null;
                    }
                } else {
                    current = current.getAsJsonObject().get(part);
                }
            }
            
            if (current == null) {
                return null;
            }
            
            if (current.isJsonPrimitive()) {
                return current.getAsString();
            } else {
                return current.toString();
            }
            
        } catch (Exception e) {
            System.out.println("简单JSON提取失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 智能判断并使用合适的提取方法
     * - 如果以 $ 开头，使用JsonPath（Goby格式）
     * - 如果以 . 开头，使用jq（Xray格式）
     * 
     * @param jsonContent JSON内容
     * @param expression 提取表达式
     * @return 提取的值
     */
    public static String extract(String jsonContent, String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }
        
        // 判断使用哪种语法
        String trimmed = expression.trim();
        if (trimmed.startsWith("$")) {
            // JsonPath语法（Goby）
            return extractByJsonPath(jsonContent, trimmed);
        } else if (trimmed.startsWith(".")) {
            // jq语法（Xray）
            return extractJson(jsonContent, Arrays.asList(trimmed));
        } else {
            // 默认当作JsonPath处理
            return extractByJsonPath(jsonContent, "$." + trimmed);
        }
    }

    public static void main(String[] args) {
        // 测试jq语法
        String json = "{\n" +
                "  \"server\": {\n" +
                "    \"name\": \"nginx\",\n" +
                "    \"version\": \"1.23.4\"\n" +
                "  },\n" +
                "  \"data\": {\n" +
                "    \"users\": [\n" +
                "      {\"id\": 1, \"name\": \"Alice\", \"role\": \"admin\"},\n" +
                "      {\"id\": 2, \"name\": \"Bob\", \"role\": \"user\"},\n" +
                "      {\"id\": 3, \"name\": \"Charlie\", \"role\": \"admin\"}\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        
        System.out.println("=== jq语法测试 (Xray) ===");
        List<String> jqRules = Arrays.asList(".server.name");
        System.out.println("jq: .server.name => " + extractJson(json, jqRules));
        
        System.out.println("\n=== JsonPath语法测试 (Goby) ===");
        System.out.println("$.server.name => " + extractByJsonPath(json, "$.server.name"));
        System.out.println("$.data.users[0].name => " + extractByJsonPath(json, "$.data.users[0].name"));
        System.out.println("$.data.users[*].name => " + extractByJsonPath(json, "$.data.users[*].name"));
        System.out.println("$.data.users[?(@.role=='admin')].name => " + extractByJsonPath(json, "$.data.users[?(@.role=='admin')].name"));
        System.out.println("$..name => " + extractByJsonPath(json, "$..name"));
        
        System.out.println("\n=== 智能提取测试 ===");
        System.out.println("$.server.version => " + extract(json, "$.server.version"));
        System.out.println(".server.version => " + extract(json, ".server.version"));
    }
} 