package com.potato.potatotool.utils;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.security.Provider;
import java.security.Security;

/**
 * @author Potato
 * @date 2024/4/13 17:18
 */
public class SecurityInitializer {

    // 初始化BC算法支持（该算法存在签名校验，部分jdk不支持mvn引入，故必须采用该方式）
    public static void initializeSecurityProvider() {
        try {
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{Paths.get(System.getProperty("user.home"), ".PotatoTool", "bcprov.jar").toUri().toURL()}
            );
            Class<?> dynamicClass = classLoader.loadClass("org.bouncycastle.jce.provider.BouncyCastleProvider");
            Object BCProvider = dynamicClass.newInstance();
            Security.addProvider((Provider) BCProvider);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
