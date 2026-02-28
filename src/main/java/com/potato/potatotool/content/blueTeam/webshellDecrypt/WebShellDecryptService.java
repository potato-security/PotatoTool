package com.potato.potatotool.content.blueTeam.webshellDecrypt;

import com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils.BinaryDeserializer;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils.BinaryDeserializerFactory;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.parser.HttpRequestParser;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.parser.RequestParseResult;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.ContentDecoder;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.matcher.WebShellMatcher;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.model.DecryptResult;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.model.DecryptConfig;
import com.potato.potatotool.content.classObj.BinaryFormatConstants;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.crypto.SecurityInitializer;
import com.potato.potatotool.utils.data.JsonUtils;
import org.json.JSONObject;
import org.json.JSONArray;
import org.w3c.dom.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * WebShell解密服务主类
 *
 * @author Potato
 * @date 2023/4/30 09:00
 * @version 2.0
 */
public class WebShellDecryptService {
    
    private final HttpRequestParser requestParser;
    private final ContentDecoder contentDecoder;
    private final WebShellMatcher webShellMatcher;
    private final List<List<String>> encodeModes;
    
    public WebShellDecryptService() {
        this.requestParser = new HttpRequestParser();
        this.contentDecoder = new ContentDecoder();
        this.webShellMatcher = new WebShellMatcher();
        this.encodeModes = new ArrayList<>();
    }
    
    /**
     * 处理HTTP请求体/响应体解密
     * 与原dealBody方法逻辑完全一致
     * 
     * @param content 待解密的内容
     * @param config 解密配置
     * @return 解密结果
     */
    public DecryptResult decryptContent(String content, DecryptConfig config) {
        if (content == null || content.trim().isEmpty()) {
            return DecryptResult.error(I18nUtils.getString("webshell.error.empty"));
        }
        content = content.trim();

        try {
            encodeModes.clear();
            String resultData = "";

            // 1. 尝试根据提取Content-Type中特征尝试MIME类型解密
            String contentType = extractContentType(content);
            if (contentType != null) {
                String mimeResult = decryptByMimeType(content, contentType, config);
                if (mimeResult != null && !mimeResult.equals(content)) {
                    encodeModes.add(Arrays.asList("MIME: " + contentType));
                    return DecryptResult.success(mimeResult, encodeModes);
                }
            }

            // 2. 特殊处理头部->Shiro Cookie
            if (content.contains("Cookie: rememberMe=") && content.contains("Host")) {
                String rememberMeValue = requestParser.extractShiroRememberMe(content);
                if (!rememberMeValue.isEmpty()) {
                    //   尽可能筛除非攻击shiro-> && rememberMeValue.length() > 20
                    String tmpResultData = contentDecoder.decode(rememberMeValue, config, encodeModes);
                    if (!tmpResultData.equals(rememberMeValue)) {
                        resultData = content.replace(rememberMeValue, tmpResultData);
                    }
                }
            }

            // 3. 检测并处理XML格式
            if (isXmlFormat(content)) {
                List<List<String>> xmlEncodeModes = new ArrayList<>();
                String xmlResult = decryptXml(content, config, xmlEncodeModes);
                if (!xmlResult.equals(content)) {
                    encodeModes.addAll(xmlEncodeModes);
                    return DecryptResult.success(xmlResult, encodeModes);
                }
            }

            // 4. 检测并处理Multipart格式
            if (isMultipartFormat(content)) {
                List<List<String>> multipartEncodeModes = new ArrayList<>();
                String multipartResult = decryptMultipart(content, config, multipartEncodeModes);
                if (!multipartResult.equals(content)) {
                    encodeModes.addAll(multipartEncodeModes);
                    return DecryptResult.success(multipartResult, encodeModes);
                }
            }

            // 5. 解析HTTP请求并提取数据
            RequestParseResult parseResult = requestParser.extractAndParseRequest(content);
            String postData = parseResult.getExtractedData();
            JSONObject jsonData = parseResult.getJsonData();

            // 6. 检测是否是JSON数组
            JSONArray jsonArray = null;
            if (postData.trim().startsWith("[") && postData.trim().endsWith("]")) {
                try {
                    jsonArray = new JSONArray(postData);
                } catch (Exception e) {
                    // 不是有效的JSON数组
                }
            }

            // 7. 处理POST/GET数据
            if (jsonArray != null) {
                // 处理JSON数组
                List<List<String>> allEncodeModes = new ArrayList<>();
                JSONArray decryptedArray = (JSONArray) recursiveDecryptJsonValue(jsonArray, config, allEncodeModes);

                String newPostData = decryptedArray.toString();

                encodeModes.addAll(allEncodeModes);

                if (!newPostData.equals(postData)) {
                    resultData = content.replace(postData, newPostData);
                }
            }
            // 非Json
            else if (!parseResult.hasJsonData()) {
                // 单value或单key+value情况
                String tmpResultData = contentDecoder.decode(postData, config, encodeModes);
                if (!tmpResultData.equals(postData)) {
                    resultData = content.replace(postData, tmpResultData);
                }
            } else {
                // 存在多个key+value情况，使用递归解密
                List<List<String>> allEncodeModes = new ArrayList<>();
                Object decryptedJson = recursiveDecryptJsonValue(jsonData, config, allEncodeModes);

                String newPostData;
                if (decryptedJson instanceof JSONObject) {
                    // 判断原始数据格式，保持原格式输出
                    if (!postData.trim().startsWith("{")) {
                        // 原始不是JSON格式，转回URL参数格式（支持单参数和多参数）
                        newPostData = jsonToUrlParams((JSONObject) decryptedJson);
                    } else {
                        // 原始是JSON格式，保持JSON格式
                        newPostData = ((JSONObject) decryptedJson).toString();
                    }
                } else if (decryptedJson instanceof JSONArray) {
                    newPostData = ((JSONArray) decryptedJson).toString();
                } else {
                    newPostData = decryptedJson.toString();
                }

                encodeModes.addAll(allEncodeModes);

                if (!newPostData.equals(postData)) {
                    resultData = content.replace(postData, newPostData);
                }
            }

            // 8. 检查是否解密成功，失败跳出
            if (encodeModes.isEmpty()) {
                return handleDecryptFailure(config);
            }

            // 9. 进行WebShell特征匹配
            webShellMatcher.matchWebShellFeatures(content, resultData, encodeModes, config.getInputKey());

            // 10. 构建成功结果
            return DecryptResult.success(resultData, encodeModes);

        } catch (Exception e) {
            return DecryptResult.error(I18nUtils.getString("webshell.error.exception", e.getMessage()));
        }
    }
    
    /**
     * 处理解密失败的情况
     */
    private DecryptResult handleDecryptFailure(DecryptConfig config) {
        if (config.getTraverseList().isEmpty()) {
            return DecryptResult.error(I18nUtils.getString("webshell.error.detect.fail.default"));
        } else {
            return DecryptResult.error(I18nUtils.getString("webshell.error.detect.fail.dict"));
        }
    }
    
    /**
     * 快速解密方法，使用默认配置
     */
    public DecryptResult quickDecrypt(String content) {
        return decryptContent(content, DecryptConfig.defaultConfig());
    }
    
    /**
     * 使用自定义密钥解密
     */
    public DecryptResult decryptWithCustomKey(String content, String key, String iv) {
        DecryptConfig config = DecryptConfig.builder()
                .inputKey(key)
                .inputIv(iv)
                .build();
        return decryptContent(content, config);
    }
    
    /**
     * 使用字典爆破解密
     */
    public DecryptResult decryptWithDictionary(String content, List<String> traverseList, String customPath) {
        DecryptConfig config = DecryptConfig.builder()
                .traverseList(traverseList)
                .customPath(customPath)
                .build();
        return decryptContent(content, config);
    }

    /**
     * 将JSONObject转换为URL参数格式
     *
     * @param jsonObj JSON对象
     * @return URL参数格式字符串（key1=value1&key2=value2）
     */
    private String jsonToUrlParams(JSONObject jsonObj) {
        StringBuilder params = new StringBuilder();
        boolean first = true;

        for (String key : jsonObj.keySet()) {
            if (!first) {
                params.append("&");
            }
            first = false;

            Object value = jsonObj.get(key);
            String valueStr;

            // 处理不同类型的value
            if (value instanceof JSONObject) {
                valueStr = ((JSONObject) value).toString();
            } else if (value instanceof JSONArray) {
                valueStr = ((JSONArray) value).toString();
            } else {
                valueStr = String.valueOf(value);
            }

            params.append(key).append("=").append(valueStr);
        }

        return params.toString();
    }

    /**
     * 递归解密JSON对象/数组/字符串
     *
     * @param obj 待解密的对象(可能是JSONObject、JSONArray、String等)
     * @param config 解密配置
     * @param allEncodeModes 收集所有解密过程
     * @return 解密后的对象
     */
    private Object recursiveDecryptJsonValue(Object obj, DecryptConfig config, List<List<String>> allEncodeModes) {
        if (obj instanceof String) {
            String strValue = (String) obj;

            if (strValue.isEmpty()) {
                return obj;
            }

            // 记录当前的encodeModes大小
            int beforeSize = allEncodeModes.size();

            // 1. 首先检查字符串本身是否是JSON格式（解密前）
            if (isJsonString(strValue)) {
                try {
                    JSONObject jsonObj = new JSONObject(strValue);
                    Object decryptedObj = recursiveDecryptJsonValue(jsonObj, config, allEncodeModes);
                    // 如果有新的解密操作发生，返回解密后的JSON字符串
                    if (allEncodeModes.size() > beforeSize) {
                        if (decryptedObj instanceof JSONObject) {
                            return ((JSONObject) decryptedObj).toString();
                        } else if (decryptedObj instanceof JSONArray) {
                            return ((JSONArray) decryptedObj).toString();
                        }
                        return decryptedObj.toString();
                    }
                    // 如果没有解密操作，继续尝试对字符串本身解密
                } catch (Exception e) {
                    try {
                        JSONArray jsonArray = new JSONArray(strValue);
                        Object decryptedArr = recursiveDecryptJsonValue(jsonArray, config, allEncodeModes);
                        if (allEncodeModes.size() > beforeSize) {
                            if (decryptedArr instanceof JSONArray) {
                                return ((JSONArray) decryptedArr).toString();
                            } else if (decryptedArr instanceof JSONObject) {
                                return ((JSONObject) decryptedArr).toString();
                            }
                            return decryptedArr.toString();
                        }
                    } catch (Exception ex) {
                        // 不是有效JSON，继续后续处理
                    }
                }
            }

            // 2. 尝试解密字符串本身
            List<List<String>> tempEncodeModes = new ArrayList<>();
            String decrypted = contentDecoder.decode(strValue, config, tempEncodeModes);

            if (!decrypted.equals(strValue) && !tempEncodeModes.isEmpty()) {
                allEncodeModes.addAll(tempEncodeModes);

                // 3. 检查解密后是否是JSON，如果是则继续递归
                if (isJsonString(decrypted)) {
                    try {
                        JSONObject nestedJson = new JSONObject(decrypted);
                        Object nestedResult = recursiveDecryptJsonValue(nestedJson, config, allEncodeModes);
                        if (nestedResult instanceof JSONObject) {
                            return ((JSONObject) nestedResult).toString();
                        } else if (nestedResult instanceof JSONArray) {
                            return ((JSONArray) nestedResult).toString();
                        }
                        return nestedResult.toString();
                    } catch (Exception e) {
                        try {
                            JSONArray nestedArray = new JSONArray(decrypted);
                            Object nestedResult = recursiveDecryptJsonValue(nestedArray, config, allEncodeModes);
                            if (nestedResult instanceof JSONArray) {
                                return ((JSONArray) nestedResult).toString();
                            } else if (nestedResult instanceof JSONObject) {
                                return ((JSONObject) nestedResult).toString();
                            }
                            return nestedResult.toString();
                        } catch (Exception ex) {
                            return decrypted;
                        }
                    }
                }

                return decrypted;
            }

            return obj;
        }
        else if (obj instanceof JSONObject) {
            JSONObject jsonObj = (JSONObject) obj;
            JSONObject result = new JsonUtils.OrderedJSONObject();

            for (String key : jsonObj.keySet()) {
                Object value = jsonObj.get(key);
                Object decryptedValue = recursiveDecryptJsonValue(value, config, allEncodeModes);
                result.put(key, decryptedValue);
            }

            return result;
        }
        else if (obj instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) obj;
            JSONArray result = new JSONArray();

            for (int i = 0; i < jsonArray.length(); i++) {
                Object item = jsonArray.get(i);
                Object decryptedItem = recursiveDecryptJsonValue(item, config, allEncodeModes);
                result.put(decryptedItem);
            }

            return result;
        }
        else {
            return obj;
        }
    }

    /**
     * 判断字符串是否是JSON格式
     */
    private boolean isJsonString(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        str = str.trim();
        return (str.startsWith("{") && str.endsWith("}")) ||
               (str.startsWith("[") && str.endsWith("]"));
    }

    /**
     * 检测是否是XML格式
     */
    private boolean isXmlFormat(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        String trimmed = content.trim();
        return trimmed.startsWith("<") && trimmed.contains(">") &&
               (trimmed.contains("</") || trimmed.endsWith("/>"));
    }

    /**
     * 解密XML格式数据
     */
    private String decryptXml(String content, DecryptConfig config, List<List<String>> encodeModes) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(content.getBytes("UTF-8")));

            boolean hasDecrypted = decryptXmlNode(doc.getDocumentElement(), config, encodeModes);

            if (hasDecrypted) {
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");

                StringWriter writer = new StringWriter();
                transformer.transform(new DOMSource(doc), new StreamResult(writer));

                // 解码HTML实体（XML转换器会将\r等字符编码为&#xD;）
                String result = writer.toString();
                result = decodeHtmlEntities(result);
                return result;
            }
        } catch (Exception e) {
            return content;
        }
        return content;
    }

    /**
     * 解码HTML实体
     */
    private String decodeHtmlEntities(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // 解码常见的HTML数字实体（十六进制）
        text = text.replaceAll("&#xD;", "\r");
        text = text.replaceAll("&#xA;", "\n");
        text = text.replaceAll("&#x9;", "\t");

        // 解码常见的HTML数字实体（十进制）
        text = text.replaceAll("&#13;", "\r");
        text = text.replaceAll("&#10;", "\n");
        text = text.replaceAll("&#9;", "\t");

        // 解码常见的HTML命名实体（&amp;必须最后解码，避免冲突）
        text = text.replaceAll("&lt;", "<");
        text = text.replaceAll("&gt;", ">");
        text = text.replaceAll("&quot;", "\"");
        text = text.replaceAll("&apos;", "'");
        text = text.replaceAll("&amp;", "&");  // 必须最后

        return text;
    }

    /**
     * 递归解密XML节点
     */
    private boolean decryptXmlNode(Node node, DecryptConfig config, List<List<String>> encodeModes) {
        boolean hasDecrypted = false;

        if (node.getNodeType() == Node.TEXT_NODE) {
            String value = node.getNodeValue();
            if (value != null && !value.trim().isEmpty()) {
                List<List<String>> tempEncodeModes = new ArrayList<>();
                String decryptedValue = contentDecoder.decode(value.trim(), config, tempEncodeModes);

                if (!decryptedValue.equals(value.trim()) && !tempEncodeModes.isEmpty()) {
                    node.setNodeValue(decryptedValue);
                    encodeModes.addAll(tempEncodeModes);
                    hasDecrypted = true;
                }
            }
        }

        if (node.hasAttributes()) {
            NamedNodeMap attributes = node.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attr = attributes.item(i);
                String value = attr.getNodeValue();
                if (value != null && !value.isEmpty()) {
                    List<List<String>> tempEncodeModes = new ArrayList<>();
                    String decryptedValue = contentDecoder.decode(value, config, tempEncodeModes);

                    if (!decryptedValue.equals(value) && !tempEncodeModes.isEmpty()) {
                        attr.setNodeValue(decryptedValue);
                        encodeModes.addAll(tempEncodeModes);
                        hasDecrypted = true;
                    }
                }
            }
        }

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (decryptXmlNode(children.item(i), config, encodeModes)) {
                hasDecrypted = true;
            }
        }

        return hasDecrypted;
    }

    /**
     * 检测是否是Multipart格式
     */
    private boolean isMultipartFormat(String content) {
        // 使用更严格的正则：必须包含边界和Content-Disposition（不区分大小写）
        return content.matches("(?si).*--.*[\\r\\n]+.*Content-Disposition:.*name.*");
    }

    /**
     * 解密Multipart格式数据
     */
    private String decryptMultipart(String content, DecryptConfig config, List<List<String>> encodeModes) {
        String boundary = extractBoundary(content);
        if (boundary == null) {
            return content;
        }

        String[] parts = content.split(Pattern.quote(boundary));
        StringBuilder result = new StringBuilder();
        boolean hasDecrypted = false;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            if (i == 0) {
                result.append(part);
                if (i < parts.length - 1) {
                    result.append(boundary);
                }
                continue;
            }

            int bodyStart = part.indexOf("\r\n\r\n");
            boolean hasCR = true;
            if (bodyStart == -1) {
                bodyStart = part.indexOf("\n\n");
                hasCR = false;
            }

            if (bodyStart != -1) {
                String headers = part.substring(0, bodyStart);
                String separator = hasCR ? "\r\n\r\n" : "\n\n";
                String bodyWithTrailing = part.substring(bodyStart + separator.length());

                // 提取body和尾部换行符
                String trailingNewlines = "";
                String body = bodyWithTrailing;

                // 匹配尾部的换行符
                Pattern trailingPattern = Pattern.compile("([\\r\\n]+)$");
                Matcher trailingMatcher = trailingPattern.matcher(bodyWithTrailing);
                if (trailingMatcher.find()) {
                    trailingNewlines = trailingMatcher.group(1);
                    body = bodyWithTrailing.substring(0, bodyWithTrailing.length() - trailingNewlines.length());
                }

                List<List<String>> tempEncodeModes = new ArrayList<>();
                String decryptedBody = body;

                // 先尝试直接解密
                String tmpDecrypted = contentDecoder.decode(body, config, tempEncodeModes);
                if (!tmpDecrypted.equals(body) && !tempEncodeModes.isEmpty()) {
                    decryptedBody = tmpDecrypted;
                    encodeModes.addAll(tempEncodeModes);
                    hasDecrypted = true;
                } else {
                    // 如果直接解密失败，检查body是否是JSON格式
                    if (isJsonString(body)) {
                        try {
                            JSONObject jsonObj = new JSONObject(body);
                            Object decryptedJson = recursiveDecryptJsonValue(jsonObj, config, tempEncodeModes);
                            if (!tempEncodeModes.isEmpty()) {
                                decryptedBody = decryptedJson instanceof JSONObject
                                    ? ((JSONObject) decryptedJson).toString()
                                    : decryptedJson.toString();
                                encodeModes.addAll(tempEncodeModes);
                                hasDecrypted = true;
                            }
                        } catch (Exception e) {
                            try {
                                JSONArray jsonArray = new JSONArray(body);
                                Object decryptedJson = recursiveDecryptJsonValue(jsonArray, config, tempEncodeModes);
                                if (!tempEncodeModes.isEmpty()) {
                                    decryptedBody = decryptedJson instanceof JSONArray
                                        ? ((JSONArray) decryptedJson).toString()
                                        : decryptedJson.toString();
                                    encodeModes.addAll(tempEncodeModes);
                                    hasDecrypted = true;
                                }
                            } catch (Exception ex) {
                                // 不是有效JSON，保持原样
                            }
                        }
                    }
                }

                result.append(headers).append(separator).append(decryptedBody).append(trailingNewlines);
            } else {
                result.append(part);
            }

            if (i < parts.length - 1) {
                result.append(boundary);
            }
        }

        return hasDecrypted ? result.toString() : content;
    }

    /**
     * 提取Multipart分隔符
     */
    private String extractBoundary(String content) {
        // 优先从内容中直接提取边界符
        Pattern pattern = Pattern.compile("(------\\w+)");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // 从Content-Type头中提取boundary（不区分大小写）
        pattern = Pattern.compile("boundary=([^\\s;]+)", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(content);
        if (matcher.find()) {
            return "--" + matcher.group(1);
        }

        return null;
    }

    /**
     * 从HTTP请求中提取Content-Type
     *
     * @param content HTTP请求内容
     * @return Content-Type值（小写），如果没有则返回null
     */
    private String extractContentType(String content) {
        // 匹配 Content-Type 头（不区分大小写）
        Pattern pattern = Pattern.compile(
            "Content-Type:\\s*([^\\s;]+)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).toLowerCase();
        }
        return null;
    }

    /**
     * 根据MIME类型反序列化二进制数据
     *
     * @param content 完整的HTTP请求内容
     * @param contentType Content-Type MIME类型
     * @param config 解密配置
     * @return 反序列化后的内容，如果失败则返回null
     */
    private String decryptByMimeType(String content, String contentType, DecryptConfig config) {
        // 从常量类获取MIME映射
        String format = BinaryFormatConstants.MIME_TYPE_MAP.get(contentType);
        if (format == null) {
            return null;
        }

        try {
            // 提取请求体数据（假设在两个换行后）
            int bodyStart = content.indexOf("\n\n");
            if (bodyStart == -1) {
                bodyStart = content.indexOf("\r\n\r\n");
                if (bodyStart != -1) {
                    bodyStart += 4;  // \r\n\r\n 长度
                }
            } else {
                bodyStart += 2;  // \n\n 长度
            }

            if (bodyStart == -1 || bodyStart >= content.length()) {
                return null;
            }

            String body = content.substring(bodyStart).trim();
            if (body.isEmpty()) {
                return null;
            }

            // 转换为字节数组（假设是ISO-8859-1编码的二进制数据）
            byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);

            // 使用工厂类获取反序列化器
            BinaryDeserializer deserializer = BinaryDeserializerFactory.getDeserializer(format);

            if (deserializer != null && deserializer.canDeserialize(bytes)) {
                String result = deserializer.deserialize(bytes);
                if (result != null && !result.isEmpty()) {
                    // 替换原始请求体部分
                    return content.substring(0, bodyStart) + result;
                }
            }

        } catch (Exception e) {
            // 解析失败，返回null
            if (debugMode) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static void main(String[] args) {
        // 非项目启动调用，需要单独初始化安全证书套件
        SecurityInitializer.initializeSecurityProvider();
        DecryptConfig config = DecryptConfig.builder()
                .build();
        WebShellDecryptService decryptContent = new WebShellDecryptService();
//        String content = "pass=fL1tMGI4YTljO/79NDQm7r9PZzBiOA%3D%3D&orderid=0mQU%2BS1pFnTz3ttVTnAgJf4rvU9E3tQySxwinpW%2F0fAQrVXMjQo9j5ZOKitj8eSk6AsEf1uVaNfGq0Q584SlVfSSQO824oYh0qWY81PjflvpzffSw4%2F%2BNENDkTrxoonglOaOKQNIfNs%2FM%2BdSgSeKGA%3D%3D";
        String content = "JMK83aAgUCrm2fHdvJWIEQ==";
        DecryptResult sss = decryptContent.decryptContent(content, config);
        System.out.println(sss.getEncodeModes());
        System.out.println(sss.getErrorMessage());
        System.out.println(sss.getData());
    }
}