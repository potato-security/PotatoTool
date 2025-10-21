package com.potato.potatotool.utils.crypto;

import com.potato.potatotool.storage.PathManager;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.security.Provider;
import java.security.Security;

/**
 * BC加密算法初始化器
 * 该算法存在签名校验，部分JDK不支持Maven引入，故采用动态加载方式
 * 
 * @author Potato
 * @date 2024/4/13 17:18
 */
public class SecurityInitializer {

    /**
     * 初始化BC算法支持
     * 从配置目录动态加载bcprov.jar
     */
    public static void initializeSecurityProvider() {
        try {
            // 使用PathManager获取BC库路径
            PathManager pathManager = PathManager.getInstance();
            Path bcprovPath = pathManager.getBcprovPath();
            
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{bcprovPath.toUri().toURL()}
            );
            Class<?> dynamicClass = classLoader.loadClass("org.bouncycastle.jce.provider.BouncyCastleProvider");
            Object BCProvider = dynamicClass.newInstance();
            Security.addProvider((Provider) BCProvider);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
