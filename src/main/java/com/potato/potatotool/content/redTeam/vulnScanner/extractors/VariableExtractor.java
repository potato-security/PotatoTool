package com.potato.potatotool.content.redTeam.vulnScanner.extractors;

import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
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

            String part = extractor.getPart();
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
                    extractedValue = JsonExtractor.extractJson(content, values);
                    break;
                case XPATH:
                    extractedValue = extractXpath(content, values, attribute);
                    break;
//                case DSL:
//                    extractedValue = DslEvaluator.extractDsl(response, values);
//                    break;
                case KVAL:
                    extractedValue = extractKval(content, values);
                    break;
                default:
                    System.out.println("不支持的提取器类型");
                    break;
            }

            // 如果成功提取了值，则保存到变量映射中
            if (extractedValue != null) {
                extractedValues.put(extractor.getName(), extractedValue);
            }
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
                        // 确保连接对象不为空
                        if (response == null) {
                            return "0";
                        }
                        return String.valueOf(response.getResponseCode());
                    } catch (Exception e) {
                        System.err.println("获取响应状态码失败: " + e.getMessage());
                        return "0"; // 返回默认值
                    }
                case "all":
                    return response.getAllResponseText();
                default:
                    // 尝试获取特定的响应头
                    Map<String, List<String>> fields = response.getHeaderFields();
                    if (fields != null) {
                        List<String> headerValues = fields.get(part);
                        if (headerValues != null && !headerValues.isEmpty()) {
                            return String.join("; ", headerValues);
                        }
                    }
                    return null;
            }
        } catch (Exception e) {
            System.err.println("获取响应部分失败: " + part + ", 错误: " + e.getMessage());
            return null;
        }
    }


    /**
     * 使用正则表达式提取变量
     * @param content 内容
     * @param patterns 正则表达式列表
     * @param group 捕获组索引
     * @return 提取的值
     */
    private static String extractRegex(String content, List<String> patterns, int group) {
        if (content == null || patterns == null || patterns.isEmpty()) {
            return null;
        }

        for (String regex : patterns) {
            try {
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(content);

                if (matcher.find()) {
                    // 确保组号有效
                    int effectiveGroup = group;
                    if (effectiveGroup < 0 || effectiveGroup > matcher.groupCount()) {
                        effectiveGroup = matcher.groupCount() > 0 ? 1 : 0;
                    }
                    return matcher.group(effectiveGroup);
                }
            } catch (PatternSyntaxException e) {
                // 忽略非法正则表达式
            }
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
        if (!content.trim().startsWith("<")) {
            return null;
        }

        try {
            // 创建DOM解析器
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
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
                    System.err.println("XPath提取失败: " + xpathExpr + ", 错误: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("XML解析失败: " + e.getMessage());
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
            
            // 尝试不同的分隔符
            if (line.contains(":")) {
                kv = line.split(":", 2);
            } else if (line.contains("=")) {
                kv = line.split("=", 2);
            } else if (line.contains("\t")) {
                kv = line.split("\t", 2);
            } else if (line.contains(" - ")) {
                kv = line.split(" - ", 2);
            } else if (line.contains("; ")) {
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

            if (kv != null && kv.length == 2) {
                String key = kv[0].trim();
                String value = kv[1].trim();
                kvMap.put(key, value);

                // 处理 Set-Cookie 头的所有属性
                if ("Set-Cookie".equalsIgnoreCase(key)) {
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
            }
        }
        System.out.println(kvMap);
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