package com.potato.potatotool.content.redTeam.vulnScanner.extractors;

import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor.DslEvaluatorRefactored;
import net.sf.saxon.xpath.XPathFactoryImpl;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 变量提取器，用于从HTTP响应中提取变量
 */
public class VariableExtractor {

    /**
     * 从HTTP响应中提取变量（支持 Object 类型的变量映射）
     * @param response HTTP响应对象
     * @param extractors 提取器列表
     * @param extractedValues 提取的变量映射
     */
    public static void extractVariablesObj(CustomHttpResponse response, List<PocObj.Matcher> extractors, Map<String, Object> extractedValues) {
        if (response == null || extractors == null || extractors.isEmpty() || extractedValues == null) {
            return;
        }

        for (PocObj.Matcher extractor : extractors) {
            if (extractor == null || extractor.getName() == null || extractor.getName().isEmpty()) {
                continue;
            }

            String part = resolveExtractorPart(extractor);
            String content = getResponsePart(response, part);

            if (content == null || content.isEmpty()) {
                continue;
            }

            String extractedValue = null;
            PocObj.MatcherType type = extractor.getType();
            List<String> values = extractor.getValues();
            int group = extractor.getGroup();
            String attribute = extractor.getAttribute();

            switch (type) {
                case REGEX:
                    extractedValue = extractRegex(content, values, group);
                    break;
                case JSON:
                    // Goby使用JsonPath语法（$开头），Xray使用jq语法（.开头）
                    if (values != null && !values.isEmpty()) {
                        String expr = values.get(0);
                        if (expr != null && expr.trim().startsWith("$")) {
                            // JsonPath语法（Goby）
                            extractedValue = JsonExtractor.extractByJsonPath(content, expr.trim());
                        } else {
                            // jq语法（Xray）
                            extractedValue = JsonExtractor.extractJson(content, values);
                        }
                    }
                    break;
                case XPATH:
                    extractedValue = extractXpath(content, values, attribute);
                    break;
                case KVAL:
                    extractedValue = extractKval(content, values);
                    break;
                case DSL:
                    extractedValue = extractDsl(content, values, response, part);
                    break;
                default:
                    System.out.println("不支持的提取器类型");
                    break;
            }

            // internal=true: 仅参与内部变量链路，不进入最终输出
            if (isInternalExtractor(extractor)) {
                continue;
            }

            // 如果成功提取了值，则保存到变量映射中
            if (extractedValue != null) {
                extractedValues.put(extractor.getName(), extractedValue);
            }
        }
    }

    /**
     * 从纯文本响应中提取变量（用于非HTTP协议）
     */
    public static void extractVariablesFromTextObj(String rawContent, List<PocObj.Matcher> extractors, Map<String, Object> extractedValues) {
        if (rawContent == null || extractors == null || extractors.isEmpty() || extractedValues == null) {
            return;
        }

        for (PocObj.Matcher extractor : extractors) {
            if (extractor == null || extractor.getName() == null || extractor.getName().isEmpty()) {
                continue;
            }

            String part = resolveExtractorPart(extractor);
            String content = getTextPart(rawContent, part);
            if (content == null || content.isEmpty()) {
                continue;
            }

            String extractedValue = null;
            PocObj.MatcherType type = extractor.getType();
            List<String> values = extractor.getValues();
            int group = extractor.getGroup();
            String attribute = extractor.getAttribute();

            switch (type) {
                case REGEX:
                    extractedValue = extractRegex(content, values, group);
                    break;
                case JSON:
                    if (values != null && !values.isEmpty()) {
                        String expr = values.get(0);
                        if (expr != null && expr.trim().startsWith("$")) {
                            extractedValue = JsonExtractor.extractByJsonPath(content, expr.trim());
                        } else {
                            extractedValue = JsonExtractor.extractJson(content, values);
                        }
                    }
                    break;
                case XPATH:
                    extractedValue = extractXpath(content, values, attribute);
                    break;
                case KVAL:
                    extractedValue = extractKval(content, values);
                    break;
                case DSL:
                    extractedValue = extractDsl(content, values, null, part);
                    break;
                default:
                    break;
            }

            if (isInternalExtractor(extractor)) {
                continue;
            }

            if (extractedValue != null) {
                extractedValues.put(extractor.getName(), extractedValue);
            }
        }
    }

    /**
     * 从HTTP响应中提取变量
     * @param response HTTP响应对象
     * @param extractors 提取器列表
     * @param extractedValues 提取的变量映射
     */
    public static void extractVariables(CustomHttpResponse response, List<PocObj.Matcher> extractors, Map<String, String> extractedValues) {
        if (response == null || extractors == null || extractors.isEmpty() || extractedValues == null) {
            return;
        }
        
        for (PocObj.Matcher extractor : extractors) {
            if (extractor == null || extractor.getName() == null || extractor.getName().isEmpty()) {
                continue;
            }

            String part = resolveExtractorPart(extractor);
            String content = getResponsePart(response, part);
            
            if (content == null || content.isEmpty()) {
                continue;
            }

            String extractedValue = null;
            PocObj.MatcherType type = extractor.getType();
            List<String> values = extractor.getValues();
            int group = extractor.getGroup();
            String attribute = extractor.getAttribute();

            switch (type) {
                case REGEX:
                    extractedValue = extractRegex(content, values, group);
                    break;
                case JSON:
                    if (values != null && !values.isEmpty()) {
                        String expr = values.get(0);
                        if (expr != null && expr.trim().startsWith("$")) {
                            extractedValue = JsonExtractor.extractByJsonPath(content, expr.trim());
                        } else {
                            extractedValue = JsonExtractor.extractJson(content, values);
                        }
                    }
                    break;
                case XPATH:
                    extractedValue = extractXpath(content, values, attribute);
                    break;
                case KVAL:
                    extractedValue = extractKval(content, values);
                    break;
                case DSL:
                    extractedValue = extractDsl(content, values, response, part);
                    break;
                default:
                    // System.out.println("不支持的提取器类型");
                    break;
            }

            // internal=true: 仅控制最终展示，不影响变量提取链路
            if (extractedValue != null) {
                extractedValues.put(extractor.getName(), extractedValue);
            }
        }
    }

    private static boolean isInternalExtractor(PocObj.Matcher extractor) {
        if (extractor == null) {
            return false;
        }
        String internal = extractor.getInternal();
        return internal != null && "true".equalsIgnoreCase(internal.trim());
    }

    private static String resolveExtractorPart(PocObj.Matcher extractor) {
        if (extractor == null) {
            return null;
        }
        String part = extractor.getPart();
        if (part != null && !part.trim().isEmpty()) {
            return part;
        }

        if (extractor.getType() == PocObj.MatcherType.KVAL) {
            return "header";
        }
        return "body";
    }

    private static String getTextPart(String rawContent, String part) {
        if (rawContent == null || rawContent.isEmpty()) {
            return null;
        }
        if (part == null || part.trim().isEmpty()) {
            return rawContent;
        }

        String normalizedPart = part.toLowerCase();
        switch (normalizedPart) {
            case "body":
            case "all":
                return rawContent;
            case "header":
            case "status":
            case "set_cookie":
            case "set-cookie":
            case "location":
            case "content_type":
            case "content-type":
            case "content_length":
            case "content-length":
                return rawContent;
            default:
                return rawContent;
        }
    }

    /**
     * 获取响应的指定部分
     * @param response HTTP响应
     * @param part 部分名称（body, header, status等）
     * @return 响应内容
     */
    public static String getResponsePart(CustomHttpResponse response, String part) {
        if (response == null || part == null || part.isEmpty()) {
            return null;
        }

        try {
            switch (part.toLowerCase()) {
                case "body":
                    return response.getTextStr();
                case "header":
                    return response.getHeaderFieldsText();
                case "status":
                    try {
                        return String.valueOf(response.getResponseCode());
                    } catch (Exception e) {
                        System.err.println("获取响应状态码失败: " + e.getMessage());
                        return "0"; // 返回默认值
                    }
                case "all":
                    return response.getAllResponseText();
                // Nuclei 特殊部分名称支持
                case "set_cookie":
                case "set-cookie":
                    return getHeaderCaseInsensitive(response, "Set-Cookie");
                case "location":
                    return getHeaderCaseInsensitive(response, "Location");
                case "content_type":
                case "content-type":
                    return getHeaderCaseInsensitive(response, "Content-Type");
                case "content_length":
                case "content-length":
                    return getHeaderCaseInsensitive(response, "Content-Length");
                default:
                    // 尝试获取特定的响应头（大小写不敏感）
                    String headerValue = getHeaderCaseInsensitive(response, part);
                    if (headerValue != null) {
                        return headerValue;
                    }
                    return null;
            }
        } catch (Exception e) {
            System.err.println("获取响应部分失败: " + part + ", 错误: " + e.getMessage());
            return null;
        }
    }

    /**
     * 大小写不敏感地获取响应头
     * @param response HTTP响应对象
     * @param headerName 头名称（任意大小写）
     * @return 头值，如果不存在返回 null
     */
    private static String getHeaderCaseInsensitive(CustomHttpResponse response, String headerName) {
        if (response == null || headerName == null) {
            return null;
        }
        Map<String, List<String>> fields = response.getHeaderFields();
        if (fields == null) {
            return null;
        }
        // 遍历所有头，进行大小写不敏感匹配
        for (Map.Entry<String, List<String>> entry : fields.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(headerName)) {
                List<String> values = entry.getValue();
                if (values != null && !values.isEmpty()) {
                    return String.join("; ", values);
                }
            }
        }
        return null;
    }

    /**
     * 使用正则表达式提取变量
     * 支持命名分组：
     * - Java风格: (?<name>pattern)
     * - Python风格: (?P<name>pattern) - 自动转换为Java风格
     * 
     * @param content 内容
     * @param patterns 正则表达式列表
     * @param group 捕获组索引（0=完整匹配，1=第一组，-1=自动检测命名组）
     * @return 提取的值
     */
    private static String extractRegex(String content, List<String> patterns, int group) {
        if (content == null || patterns == null || patterns.isEmpty()) {
            return null;
        }

        for (String regex : patterns) {
            try {
                // 转换Python风格命名分组 (?P<name>) 为Java风格 (?<name>)
                String javaRegex = convertPythonNamedGroups(regex);
                
                // 检测是否包含命名分组
                String namedGroupName = extractNamedGroupName(javaRegex);
                
                Pattern pattern = Pattern.compile(javaRegex);
                Matcher matcher = pattern.matcher(content);

                if (matcher.find()) {
                    // 优先使用命名分组
                    if (namedGroupName != null && !namedGroupName.isEmpty()) {
                        try {
                            return matcher.group(namedGroupName);
                        } catch (IllegalArgumentException e) {
                            // 命名分组不存在，降级到数字分组
                            System.err.println("命名分组不存在: " + namedGroupName);
                        }
                    }
                    
                    // 使用数字分组
                    int effectiveGroup = group;
                    
                    // group=-1 表示自动选择：优先第一个捕获组，否则完整匹配
                    if (effectiveGroup == -1) {
                        effectiveGroup = matcher.groupCount() > 0 ? 1 : 0;
                    }
                    
                    // 确保组号有效
                    if (effectiveGroup < 0 || effectiveGroup > matcher.groupCount()) {
                        effectiveGroup = matcher.groupCount() > 0 ? 1 : 0;
                    }
                    
                    return matcher.group(effectiveGroup);
                }
            } catch (PatternSyntaxException e) {
                System.err.println("无效的正则表达式: " + regex + " - " + e.getMessage());
            } catch (Exception e) {
                System.err.println("正则提取失败: " + e.getMessage());
            }
        }
        
        return null;
    }
    
    /**
     * 将Python风格的命名分组转换为Java风格
     * Python: (?P<name>pattern)
     * Java:   (?<name>pattern)
     * 
     * @param pythonRegex Python风格正则表达式
     * @return Java风格正则表达式
     */
    private static String convertPythonNamedGroups(String pythonRegex) {
        if (pythonRegex == null || !pythonRegex.contains("(?P<")) {
            return pythonRegex;
        }
        
        // 将 (?P<name> 替换为 (?<name>
        return pythonRegex.replaceAll("\\(\\?P<", "(?<");
    }
    
    /**
     * 从正则表达式中提取命名分组的名称
     * 
     * @param regex 正则表达式
     * @return 第一个命名分组的名称，如果没有则返回null
     */
    private static String extractNamedGroupName(String regex) {
        if (regex == null || regex.isEmpty()) {
            return null;
        }
        
        // 匹配Java风格命名分组: (?<name>
        Pattern namedGroupPattern = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>");
        Matcher matcher = namedGroupPattern.matcher(regex);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }

    /**
     * 使用XPath提取变量，支持XPath 2.0
     * @param content XML内容
     * @param xpaths XPath表达式列表
     * @param attribute 要提取的属性名（可选）
     * @return 提取的值
     */
    private static String extractXpath(String content, List<String> xpaths, String attribute) {
        if (content == null || xpaths == null || xpaths.isEmpty()) {
            return null;
        }

        // 如果内容不像XML，直接返回
        String trimmed = content.trim();
        if (!trimmed.startsWith("<")) {
            return null;
        }

        // ========== HTML 内容检测（避免将 HTML 当作 XML 严格解析）==========
        if (isHtmlContentForXPath(trimmed)) {
            // HTML 内容不适合严格 XML 解析，静默返回 null
            return null;
        }

        try {
            // 创建DOM解析器
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // ========== 设置自定义 ErrorHandler 抑制 [Fatal Error] 输出 ==========
            builder.setErrorHandler(new org.xml.sax.ErrorHandler() {
                @Override
                public void warning(org.xml.sax.SAXParseException e) {
                    // 抑制警告
                }
                @Override
                public void error(org.xml.sax.SAXParseException e) {
                    // 抑制错误
                }
                @Override
                public void fatalError(org.xml.sax.SAXParseException e) {
                    // 抑制致命错误（不打印 [Fatal Error]）
                }
            });

            Document doc = builder.parse(new InputSource(new StringReader(content)));

            // 创建Saxon XPath对象以支持XPath 2.0
            XPathFactory xPathfactory = new XPathFactoryImpl();
            XPath xpath = xPathfactory.newXPath();

            for (String xpathExpr : xpaths) {
                try {
                    // 执行XPath查询
                    Object result;
                    if (attribute != null && !attribute.isEmpty()) {
                        // 查询属性
                        result = xpath.evaluate(xpathExpr + "/@" + attribute, doc, XPathConstants.STRING);
                    } else {
                        // 查询节点内容
                        result = xpath.evaluate(xpathExpr, doc, XPathConstants.STRING);
                    }

                    if (result != null && !result.toString().isEmpty()) {
                        return result.toString();
                    }
                } catch (Exception e) {
                    // XPath 查询失败，尝试下一个表达式
                }
            }
        } catch (Exception e) {
            // XML 解析失败（可能是 HTML 内容），静默返回 null
        }

        return null;
    }

    /**
     * 检测内容是否为 HTML（用于 XPath 提取场景）
     * @param content 待检测内容
     * @return true 如果是 HTML 内容
     */
    private static boolean isHtmlContentForXPath(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        String lower = content.toLowerCase();

        // 1. 检测 HTML DOCTYPE 声明
        if (lower.startsWith("<!doctype html")) {
            return true;
        }

        // 2. 检测 <html 标签
        if (lower.startsWith("<html") || lower.contains("<html ") || lower.contains("<html>")) {
            return true;
        }

        // 3. 检测常见的 HTML 专属标签
        String[] htmlTags = {
            "<head", "<body", "<div", "<span", "<table", "<form",
            "<input", "<button", "<script", "<style", "<meta", "<link"
        };
        for (String tag : htmlTags) {
            if (lower.contains(tag)) {
                return true;
            }
        }

        // 4. 检测 HTML 实体引用
        String[] htmlEntities = {"&nbsp;", "&copy;", "&reg;", "&middot;", "&mdash;", "&ndash;"};
        for (String entity : htmlEntities) {
            if (lower.contains(entity)) {
                return true;
            }
        }

        // 5. 检测 HTML5 布尔属性
        if (lower.contains(" defer") || lower.contains(" async") ||
            lower.contains(" checked") || lower.contains(" disabled") ||
            lower.contains(" crossorigin")) {
            return true;
        }

        return false;
    }

    /**
     * 使用DSL表达式提取变量
     * @param content 响应内容
     * @param dslExpressions DSL表达式列表
     * @param response HTTP响应对象（可为null）
     * @param part 提取部位
     * @return 提取值
     */
    private static String extractDsl(String content, List<String> dslExpressions, CustomHttpResponse response, String part) {
        if (dslExpressions == null || dslExpressions.isEmpty()) {
            return null;
        }

        Map<String, Object> context = new HashMap<>();
        if (content != null) {
            context.put("body", content);
            context.put("raw", content);
            context.put("data", content);
        }

        if (response != null) {
            context.put("status_code", response.getResponseCode());
            context.put("response_time", response.getResponseTime());

            String headersText = response.getHeaderFieldsText();
            context.put("all_headers", headersText != null ? headersText : "");
            context.put("header", headersText != null ? headersText : "");

            String allText = response.getAllResponseText();
            context.put("all", allText != null ? allText : "");
        }

        if (part != null && content != null) {
            context.put(part, content);
        }

        for (String dslExpr : dslExpressions) {
            if (dslExpr == null || dslExpr.trim().isEmpty()) {
                continue;
            }

            String evaluated = DslEvaluatorRefactored.resolveValueOrFunction(dslExpr, context);
            if (evaluated != null) {
                return evaluated;
            }
        }

        return null;
    }

    /**
     * 从键值对格式内容中提取变量
     * @param content 键值对格式内容
     * @param kvExpressions 键表达式列表
     * @return 提取的值
     */
    private static String extractKval(String content, List<String> kvExpressions) {
        if (content == null || kvExpressions == null || kvExpressions.isEmpty()) {
            return null;
        }

        // 将内容解析为键值对映射
        Map<String, String> kvMap = new HashMap<>();
        String[] lines = content.split("\\r?\\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            
            String[] kv = null;
            
            // 优先检查是否是Set-Cookie格式（以Set-Cookie:开头）
            if (line.trim().startsWith("Set-Cookie:") || line.trim().startsWith("set-cookie:")) {
                String[] cookieKv = line.split(":", 2);
                if (cookieKv.length == 2) {
                    String value = cookieKv[1].trim();
                    // 处理 Set-Cookie 头的所有属性
                    String[] cookieAttrs = value.split(";\\s*");
                    for (String attr : cookieAttrs) {
                        if (attr.contains("=")) {
                            String[] attrPair = attr.split("=", 2);
                            String attrKey = attrPair[0].trim();
                            String attrValue = (attrPair.length == 2) ? attrPair[1].trim() : "";
                            kvMap.put(attrKey, attrValue);
                        }
                    }
                }
                continue; // 已处理此行，继续下一行
            }
            
            // 优先检查是否是Cookie格式（包含 ; 分隔的多个键值对）
            if (line.contains("; ") && line.contains("=")) {
                // 处理HTTP Cookie格式: name=value; name2=value2
                String[] parts = line.split("; ");
                for (String part : parts) {
                    if (part.contains("=")) {
                        String[] cookiePair = part.split("=", 2);
                        if (cookiePair.length == 2) {
                            kvMap.put(cookiePair[0].trim(), cookiePair[1].trim());
                        }
                    }
                }
                continue; // 已处理此行，继续下一行
            }
            
            // 尝试不同的分隔符
            if (line.contains(":")) {
                kv = line.split(":", 2);
            } else if (line.contains("=")) {
                kv = line.split("=", 2);
            } else if (line.contains("\t")) {
                kv = line.split("\t", 2);
            } else if (line.contains(" - ")) {
                kv = line.split(" - ", 2);
            }

            if (kv != null && kv.length == 2) {
                String key = kv[0].trim();
                String value = kv[1].trim();
                kvMap.put(key, value);
            }
        }
        
        // 查找匹配的键
        for (String keyExpr : kvExpressions) {
            String value = kvMap.get(keyExpr.trim());
            if (value != null) {
                return value;
            }
        }
        
        return null;
    }

} 