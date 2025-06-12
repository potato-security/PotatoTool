package com.potato.potatotool.content.redTeam.memshell.util;

import com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants;
import com.potato.potatotool.utils.data.StrUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Potato
 * @date 2024/8/5 10:10
 */
public class ClassNameUtil {
    static String[] injectorClassNames = new String[]{"SignatureUtils", "NetworkUtils", "KeyUtils", "EncryptionUtils", "SessionDataUtil", "SOAPUtils", "ReflectUtil", "HttpClientUtil", "EncryptionUtil", "XMLUtil", "JSONUtil", "FileUtils", "DateUtil", "StringUtil", "MathUtil", "HttpUtil", "CSVUtil", "ImageUtil", "ThreadUtil", "ReportUtil", "EncodingUtil", "ConfigurationUtil", "HTMLUtil", "SerializationUtil"};
    static String[] prefixNames = new String[]{"AbstractMatcher", "WebSocketUpgrade", "Session", "WhiteBlackList", "Log4jConfig", "SecurityHandler", "ContextLoader", "ServletContext", "ServletContextAttribute", "ServletRequest"};
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
        Random random = new Random();
        int index = random.nextInt(classNames.size());
        return classNames.get(index);
    }

    // 生成一个随机的注入类名
    public static String getRandomInjectorClassName(){
        return getRandomPackageName() + "." + StrUtils.generateRandomString(1, 3) + "." + getRandomName(injectorClassNames);
    }

    // 从 prefixNames 数组中随机选择一个前缀名称
    public static String getRandomPrefixName(){
        return getRandomName(prefixNames);
    }

    // 根据给定的 shell 类型生成一个随机的 shell 类名
    public static String getRandomShellClassName(String shellType) {
        String randomName = getRandomPrefixName() + StrUtils.generateRandomString(2, 6);

        if (shellType.contains(MemoryShellConstants.SHELLTYPE_LISTENER)){
            return getRandomPackageName() + "." + randomName  + "Listener";
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
        return getRandomPackageName() + "." + StrUtils.generateRandomString(1, 3) + "." + getRandomName(injectorClassNames);
    }

    // 从 packageNames 数组中随机选择一个包名
    public static String getRandomPackageName() {
        Random random = new Random();
        String packageName = packageNames[random.nextInt(packageNames.length)];
        return packageName;
    }
}
