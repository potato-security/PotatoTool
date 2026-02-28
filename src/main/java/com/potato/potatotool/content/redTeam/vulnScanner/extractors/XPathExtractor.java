package com.potato.potatotool.content.redTeam.vulnScanner.extractors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * XPath提取工具类
 * 支持完整的XPath语法，用于从HTML/XML中提取数据
 * 
 * 支持的语法：
 * - //div[@class='content']              基础路径
 * - //div[contains(@class,'error')]      contains函数
 * - //input/@value                       属性提取
 * - //div/text()                         文本节点
 * - //div[@id='main']//p                 组合路径
 * 
 * @author Potato
 * @date 2025-11-01
 */
public class XPathExtractor {

    /**
     * 使用XPath从HTML/XML中提取值
     * 
     * @param htmlContent HTML或XML内容
     * @param xpathExpr XPath表达式
     * @return 提取的值（多个结果返回第一个，未找到返回null）
     */
    public static String extract(String htmlContent, String xpathExpr) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return null;
        }
        
        if (xpathExpr == null || xpathExpr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 方式1: 尝试作为XML解析（标准XPath）
            String result = extractByStandardXPath(htmlContent, xpathExpr);
            if (result != null) {
                return result;
            }
            
            // 方式2: 降级到HTML解析（使用Jsoup + CSS选择器模拟XPath）
            return extractByJsoup(htmlContent, xpathExpr);
            
        } catch (Exception e) {
            System.err.println("XPath提取失败: " + xpathExpr + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 使用标准XPath API提取（适用于格式良好的XML）
     *
     * @param content XML内容
     * @param xpathExpr XPath表达式
     * @return 提取的值
     */
    private static String extractByStandardXPath(String content, String xpathExpr) {
        // ========== 内容类型预检测（避免 HTML 被当作 XML 解析） ==========
        String trimmed = content.trim();

        // 检测 HTML 内容特征
        if (isHtmlContent(trimmed)) {
            // 直接跳过 XML 解析，让 Jsoup 处理
            return null;
        }

        try {
            // 创建XML解析器
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setValidating(false);
            // 禁用DTD加载，避免XXE攻击
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document document = builder.parse(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
            );
            
            // 创建XPath对象
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            
            // 执行XPath查询
            String result = xpath.evaluate(xpathExpr, document);
            
            return (result != null && !result.isEmpty()) ? result : null;
            
        } catch (Exception e) {
            // XML解析失败，返回null让降级方案处理
            return null;
        }
    }

    /**
     * 使用Jsoup提取（适用于HTML，更宽容的解析）
     * 将常见的XPath表达式转换为CSS选择器
     * 
     * @param htmlContent HTML内容
     * @param xpathExpr XPath表达式
     * @return 提取的值
     */
    private static String extractByJsoup(String htmlContent, String xpathExpr) {
        try {
            Document doc = Jsoup.parse(htmlContent);
            
            // 尝试将XPath转换为CSS选择器
            String cssSelector = convertXPathToCss(xpathExpr);
            
            if (cssSelector != null) {
                // 使用CSS选择器查询
                Elements elements = doc.select(cssSelector);
                if (!elements.isEmpty()) {
                    Element first = elements.first();
                    
                    // 处理属性提取
                    if (xpathExpr.contains("/@")) {
                        String attrName = extractAttributeName(xpathExpr);
                        if (attrName != null) {
                            return first.attr(attrName);
                        }
                    }
                    
                    // 处理文本节点
                    if (xpathExpr.contains("/text()")) {
                        return first.ownText();
                    }
                    
                    // 默认返回完整文本
                    return first.text();
                }
            }
            
            // 如果CSS转换失败，尝试简单路径匹配
            return extractBySimplePath(doc, xpathExpr);
            
        } catch (Exception e) {
            System.err.println("Jsoup XPath提取失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 将XPath表达式转换为CSS选择器
     * 
     * @param xpath XPath表达式
     * @return CSS选择器，不支持的返回null
     */
    private static String convertXPathToCss(String xpath) {
        if (xpath == null || xpath.isEmpty()) {
            return null;
        }
        
        // 移除开头的 // 或 /
        String path = xpath;
        if (path.startsWith("//")) {
            path = path.substring(2);
        } else if (path.startsWith("/")) {
            path = path.substring(1);
        }
        
        // 处理常见的XPath模式
        // //div[@class='content'] → div[class='content']
        path = path.replace("[@", "[");
        
        // //div[contains(@class,'error')] → div[class*='error']
        path = path.replaceAll("contains\\(@([^,]+),\\s*['\"]([^'\"]+)['\"]\\)", "$1*='$2'");
        
        // //input/@value → input (属性在extractByJsoup中单独处理)
        if (path.contains("/@")) {
            path = path.substring(0, path.indexOf("/@"));
        }
        
        // //div/text() → div (文本在extractByJsoup中单独处理)
        path = path.replace("/text()", "");
        
        // 将 / 替换为 > (直接子元素)
        // 注意：这是简化处理，不完全等价
        path = path.replace("/", " > ");
        
        return path;
    }

    /**
     * 从XPath表达式中提取属性名
     * 
     * @param xpath XPath表达式（如 //input/@value）
     * @return 属性名（如 value），未找到返回null
     */
    private static String extractAttributeName(String xpath) {
        if (!xpath.contains("/@")) {
            return null;
        }
        
        int index = xpath.lastIndexOf("/@");
        String attrName = xpath.substring(index + 2);
        
        // 移除可能的括号和空格
        attrName = attrName.trim().replaceAll("[()\\s]+", "");
        
        return attrName.isEmpty() ? null : attrName;
    }

    /**
     * 简单路径匹配（降级方案）
     * 
     * @param doc Jsoup Document
     * @param xpath XPath表达式
     * @return 提取的值
     */
    private static String extractBySimplePath(Document doc, String xpath) {
        try {
            // 简化的XPath解析
            // 支持 //tagname[@attr='value']
            
            // 提取标签名
            String tagName = null;
            if (xpath.contains("//")) {
                String temp = xpath.substring(xpath.indexOf("//") + 2);
                if (temp.contains("[")) {
                    tagName = temp.substring(0, temp.indexOf("["));
                } else if (temp.contains("/")) {
                    tagName = temp.substring(0, temp.indexOf("/"));
                } else {
                    tagName = temp.replace("/text()", "").replace("/@", "").trim();
                }
            }
            
            if (tagName != null && !tagName.isEmpty()) {
                Elements elements = doc.getElementsByTag(tagName);
                if (!elements.isEmpty()) {
                    return elements.first().text();
                }
            }
            
            return null;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检测内容是否为 HTML（而非格式良好的 XML）
     *
     * @param content 待检测内容
     * @return true 表示是 HTML 内容，应使用 Jsoup 解析
     */
    private static boolean isHtmlContent(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        String lower = content.toLowerCase();

        // 1. 检测 HTML DOCTYPE 声明
        if (lower.startsWith("<!doctype html") || lower.startsWith("<!doctype html>")) {
            return true;
        }

        // 2. 检测 <html 标签
        if (lower.startsWith("<html") || lower.contains("<html ") || lower.contains("<html>")) {
            return true;
        }

        // 3. 检测常见的 HTML 专属标签（非 XML 使用）
        String[] htmlTags = {
            "<head", "<body", "<div", "<span", "<table", "<form",
            "<input", "<button", "<script", "<style", "<meta", "<link"
        };

        for (String tag : htmlTags) {
            if (lower.contains(tag)) {
                return true;
            }
        }

        // 4. 检测 HTML 实体引用（XML 中需要声明才能使用）
        String[] htmlEntities = {"&nbsp;", "&copy;", "&reg;", "&middot;", "&mdash;", "&ndash;"};
        for (String entity : htmlEntities) {
            if (lower.contains(entity)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 测试主方法
     */
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║        XPath提取器测试                                         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        String html = "<html>\n" +
                "  <head><title>Test Page</title></head>\n" +
                "  <body>\n" +
                "    <div class='content main'>\n" +
                "      <h1 id='title'>Welcome</h1>\n" +
                "      <p class='error message'>Error: Invalid input</p>\n" +
                "      <input type='text' name='username' value='admin' />\n" +
                "      <input type='password' name='password' value='secret' />\n" +
                "    </div>\n" +
                "    <div class='footer'>Footer content</div>\n" +
                "  </body>\n" +
                "</html>";

        System.out.println("测试HTML:\n" + html + "\n");
        System.out.println("=== XPath测试 ===\n");

        // 测试1: 基础路径
        System.out.println("1. //title");
        System.out.println("   结果: " + extract(html, "//title"));

        // 测试2: 属性选择
        System.out.println("\n2. //div[@class='content']");
        System.out.println("   结果: " + extract(html, "//div[@class='content']"));

        // 测试3: contains函数
        System.out.println("\n3. //p[contains(@class,'error')]");
        System.out.println("   结果: " + extract(html, "//p[contains(@class,'error')]"));

        // 测试4: 属性提取
        System.out.println("\n4. //input[@name='username']/@value");
        System.out.println("   结果: " + extract(html, "//input[@name='username']/@value"));

        // 测试5: 文本节点
        System.out.println("\n5. //h1[@id='title']/text()");
        System.out.println("   结果: " + extract(html, "//h1[@id='title']/text()"));

        // 测试6: 组合路径
        System.out.println("\n6. //div[@class='content']//p");
        System.out.println("   结果: " + extract(html, "//div[@class='content']//p"));
    }
}

