package com.potato.potatotool.content.redTeam.memshell.util;

import com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants;

import java.util.HashMap;
import java.util.Map;

public class ResponseCodeUtil {
    private static final Map<String, String> RESPONSE_CODE_MAP = new HashMap<>();

    // 初始化RESPONSE_CODE_MAP，为不同的服务器类型映射相应的HTTP响应获取方法体字符串。
    static {
        RESPONSE_CODE_MAP.put(MemoryShellConstants.SERVER_TOMCAT, generateCommonResponseCode());
        RESPONSE_CODE_MAP.put(MemoryShellConstants.SERVER_WEBLOGIC, generateCommonResponseCode());
        RESPONSE_CODE_MAP.put(MemoryShellConstants.SERVER_GLASSFISH, generateCommonResponseCode());
        RESPONSE_CODE_MAP.put(MemoryShellConstants.SERVER_JBOSS, generateCommonResponseCode());
        RESPONSE_CODE_MAP.put(MemoryShellConstants.SERVER_RESIN, generateResinResponseCode());
        RESPONSE_CODE_MAP.put(MemoryShellConstants.SERVER_JETTY, generateJettyResponseCode());
        RESPONSE_CODE_MAP.put(MemoryShellConstants.SERVER_WEBSPHERE, generateWebsphereResponseCode());
        RESPONSE_CODE_MAP.put(MemoryShellConstants.SERVER_UNDERTOW, generateUndertowResponseCode());
    }

    // 根据服务器类型返回相应的HTTP响应获取方法体。
    public static String getResponseCode(String serverType) {
        return RESPONSE_CODE_MAP.getOrDefault(serverType, "");
    }

    // 返回通用的HTTP响应获取方法体。
    private static String generateCommonResponseCode() {
        return "{\n" +
                "    javax.servlet.http.HttpServletResponse response = null;\n" +
                "    try {\n" +
                "        response = (javax.servlet.http.HttpServletResponse) getFieldValue(getFieldValue($1, \"request\"), \"response\");\n" +
                "    } catch (Exception exc) {\n" +
                "        try {\n" +
                "            response = (javax.servlet.http.HttpServletResponse) getFieldValue($1, \"response\");\n" +
                "        } catch (Exception exc1) {}\n" +
                "    }\n" +
                "    return response;\n" +
                "}";
    }

    // 返回Resin服务器特定的HTTP响应获取方法体。
    private static String generateResinResponseCode() {
        return "{\n" +
                "    javax.servlet.http.HttpServletResponse response;\n" +
                "    response = (javax.servlet.http.HttpServletResponse) getFieldValue($1, \"_response\");\n" +
                "    return response;\n" +
                "}";
    }

    // 返回Jetty服务器特定的HTTP响应获取方法体。
    private static String generateJettyResponseCode() {
        return "{\n" +
                "    javax.servlet.http.HttpServletResponse response;\n" +
                "    try {\n" +
                "        response = (javax.servlet.http.HttpServletResponse) getFieldValue(getFieldValue($1, \"_channel\"), \"_response\");\n" +
                "    } catch (Exception e) {\n" +
                "        response = (javax.servlet.http.HttpServletResponse) getFieldValue(getFieldValue($1, \"_connection\"), \"_response\");\n" +
                "    }\n" +
                "    return response;\n" +
                "}";
    }

    // 返回WebSphere服务器特定的HTTP响应获取方法体。
    private static String generateWebsphereResponseCode() {
        return "{\n" +
                "    javax.servlet.http.HttpServletResponse response;\n" +
                "    response = (javax.servlet.http.HttpServletResponse) getFieldValue(getFieldValue($1, \"_connContext\"), \"_response\");\n" +
                "    return response;\n" +
                "}";
    }

    // 返回Undertow服务器特定的HTTP响应获取方法体。
    private static String generateUndertowResponseCode() {
        return "{\n" +
                "    javax.servlet.http.HttpServletResponse response = null;\n" +
                "    java.util.Map map = (java.util.Map) getFieldValue(getFieldValue($1, \"exchange\"), \"attachments\");\n" +
                "    Object[] keys = map.keySet().toArray();\n" +
                "    for (int i = 0; i < keys.length; i++) {\n" +
                "        Object key = keys[i];\n" +
                "        if (map.get(key).toString().contains(\"ServletRequestContext\")) {\n" +
                "            response = (javax.servlet.http.HttpServletResponse) getFieldValue(map.get(key), \"servletResponse\");\n" +
                "            break;\n" +
                "        }\n" +
                "    }\n" +
                "    return response;\n" +
                "}";
    }

}
