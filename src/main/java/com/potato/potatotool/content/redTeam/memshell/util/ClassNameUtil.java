package com.potato.potatotool.content.redTeam.memshell.util;

import com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Potato
 * @date 2024/8/5 10:10
 */
public class ClassNameUtil {
    static String[] injectorClassNames = new String[]{"SignatureUtils", "NetworkUtils", "KeyUtils", "EncryptionUtils", "SessionDataUtil", "SOAPUtils", "ReflectUtil", "HttpClientUtil", "EncryptionUtil", "XMLUtil", "JSONUtil", "FileUtils", "DateUtil", "StringUtil", "MathUtil", "HttpUtil", "CSVUtil", "ImageUtil", "ThreadUtil", "ReportUtil", "EncodingUtil", "ConfigurationUtil", "HTMLUtil", "SerializationUtil"};
    static String[] prefixNames = new String[]{"AbstractMatcher", "WebSocketUpgrade", "Session", "WhiteBlackList", "Log4jConfig", "SecurityHandler", "ContextLoader", "ServletContext", "ServletContextAttribute", "ServletRequest"};
    private static final String[] JAVA_RESERVED_WORDS = new String[]{
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "false", "final", "finally",
            "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long",
            "native", "new", "null", "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true",
            "try", "void", "volatile", "while"
    };
    static String[] packageNames = {
            "org.springframework",
            "org.apache.commons",
            "org.apache.logging",
            "org.apache",
            "com.fasterxml.jackson",
            "org.junit",
            "org.apache.commons.lang",
            "org.apache.http.client",
            "com.google.gso",
            "ch.qos.logback"
    };

    // 从传入的字符串数组中随机选择一个名称
    public static String getRandomName(String[]... arrays) {
        List<String> classNames = new ArrayList<>();
        for (String[] array : arrays) {
            for (String className : array) {
                classNames.add(className);
            }
        }
        return MemoryShellRandomUtil.randomChoice(classNames.toArray(new String[0]));
    }

    // 生成一个随机的注入类名
    public static String getRandomInjectorClassName(){
        return getRandomPackageName() + "." + MemoryShellRandomUtil.randomAlpha(1, 3) + "." + getRandomName(injectorClassNames);
    }

    // 从 prefixNames 数组中随机选择一个前缀名称
    public static String getRandomPrefixName(){
        return getRandomName(prefixNames);
    }

    // 根据给定的 shell 类型生成一个随机的 shell 类名
    public static String getRandomShellClassName(String shellType) {
        String randomName = getRandomPrefixName() + MemoryShellRandomUtil.randomAlpha(2, 6);

        if (shellType.contains(MemoryShellConstants.SHELLTYPE_LISTENER)){
            return getRandomPackageName() + "." + randomName  + "Listener";
        } else if (shellType.contains(MemoryShellConstants.SHELLTYPE_VALVE)){
            return getRandomPackageName() + "." + randomName  + "Valve";
        } else if (shellType.contains(MemoryShellConstants.SHELLTYPE_INTERCEPTOR)){
            return getRandomPackageName() + "." + randomName  + "Interceptor";
        } else if (shellType.contains(MemoryShellConstants.SHELLTYPE_WFHANDLERMETHOD)){
            return getRandomPackageName() + "." + randomName  + "Handler";
        } else {
            return getRandomPackageName() + "." + randomName + "Filter";
        }
    }

    // 生成一个随机的加载器类名
    public static String getRandomLoaderClassName(){
        return getRandomPackageName() + "." + MemoryShellRandomUtil.randomAlpha(1, 3) + "." + getRandomName(injectorClassNames);
    }

    // 从 packageNames 数组中随机选择一个包名
    public static String getRandomPackageName() {
        return MemoryShellRandomUtil.randomChoice(packageNames);
    }

    public static String requireValidJavaClassName(String className, String label) {
        String normalized = normalizeClassName(className);
        if (!isValidJavaClassName(normalized)) {
            throw new IllegalArgumentException("Invalid " + label + ": " + className);
        }
        return normalized;
    }

    public static boolean isValidJavaClassName(String className) {
        String normalized = normalizeClassName(className);
        if (normalized == null || normalized.isEmpty()
                || normalized.startsWith(".") || normalized.endsWith(".")
                || normalized.contains("..") || normalized.contains("/") || normalized.contains("\\")) {
            return false;
        }

        String[] parts = normalized.split("\\.");
        for (String part : parts) {
            if (!isValidJavaIdentifier(part)) {
                return false;
            }
        }
        return true;
    }

    public static String normalizeClassName(String className) {
        return className == null ? null : className.trim();
    }

    private static boolean isValidJavaIdentifier(String value) {
        if (value == null || value.isEmpty() || isJavaReservedWord(value)) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(value.charAt(0))) {
            return false;
        }
        for (int i = 1; i < value.length(); i++) {
            if (!Character.isJavaIdentifierPart(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isJavaReservedWord(String value) {
        for (String reservedWord : JAVA_RESERVED_WORDS) {
            if (reservedWord.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
