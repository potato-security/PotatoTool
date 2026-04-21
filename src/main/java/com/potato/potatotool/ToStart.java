package com.potato.potatotool;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.lang.reflect.Method;

/**
 * @author Potato
 * @date 2023/11/23 17:16
 */
public class ToStart {

    public static boolean debugMode = false;
    public static boolean isBlueMode = true;

    public static void main(String[] args) {
        System.setProperty("java.net.useSystemProxies", "false");// 关闭使用代理
        System.clearProperty("http.proxyHost");
        System.clearProperty("https.proxyHost");
        System.clearProperty("socksProxyHost");
        System.setProperty("prism.lcdtext", "false");// 关闭字体锯齿效果
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");// 关闭Polyglot告警
        System.setProperty("http.keepAlive", "false");   // 禁用 Keep-Alive
        // 为 macOS dock 设置图标
        setMacDockIcon();
        // 关闭Optional.or告警
        setOptionalException();

        // 检查命令行参数是否包含 "debug" 参数
        for (String arg : args) {
            if (arg.equals("debug") || arg.equals("-debug") || arg.equals("--debug")) {
                debugMode = true;
                System.out.println("------------已开启DEBUG模式，请注意报错信息，用于提取提交bug------------");
                break;
            }
        }
        MainApplication.main(args);
    }

    private static void setOptionalException() {
        // 在 MainApplication 或 ToStart 的 main/start 方法最开始添加
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            // 忽略 RichTextFX 在 Java 8 下的 Optional.or 兼容性问题
            if (throwable instanceof NoSuchMethodError
                    && throwable.getMessage() != null
                    && throwable.getMessage().contains("Optional.or")) {
                return; // 静默忽略
            }
            // 其他异常正常打印
            throwable.printStackTrace();
        });
    }

    private static boolean isMacOS() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("mac") || os.contains("darwin");
    }

    private static void setMacDockIcon() {
        if(!isMacOS()) return;
        try {
            InputStream iconStream = ToStart.class.getResourceAsStream("/img/logo.png");
            Image icon = new Image(iconStream);
            BufferedImage awtImage = convertToAWTImage(icon);
            if (isJava9OrLater()) {
                setDockIconJava9(awtImage);
            } else {
                setDockIconJava8(awtImage);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 将JavaFX图像转换为AWT图像
     */
    private static BufferedImage convertToAWTImage(Image fxImage) {
        BufferedImage awtImage = new BufferedImage(
                (int) fxImage.getWidth(),
                (int) fxImage.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        PixelReader pixelReader = fxImage.getPixelReader();
        for (int y = 0; y < fxImage.getHeight(); y++) {
            for (int x = 0; x < fxImage.getWidth(); x++) {
                javafx.scene.paint.Color fxColor = pixelReader.getColor(x, y);
                awtImage.setRGB(
                        x, y,
                        (int) (fxColor.getOpacity() * 255) << 24 |
                                (int) (fxColor.getRed() * 255) << 16 |
                                (int) (fxColor.getGreen() * 255) << 8 |
                                (int) (fxColor.getBlue() * 255)
                );
            }
        }
        return awtImage;
    }

    private static boolean isJava9OrLater() {
        try {
            String version = System.getProperty("java.version");
            if (version.startsWith("1.")) {
                version = version.substring(2, 3);
            } else {
                int dot = version.indexOf(".");
                if (dot != -1) {
                    version = version.substring(0, dot);
                }
            }
            return Integer.parseInt(version) >= 9;
        } catch (Exception e) {
            return false;
        }
    }

    private static void setDockIconJava8(BufferedImage icon) throws Exception {
        Class<?> appClass = Class.forName("com.apple.eawt.Application");
        Object appInstance = appClass.getMethod("getApplication").invoke(null);
        Method setDockIconMethod = appClass.getMethod("setDockIconImage", java.awt.Image.class);
        setDockIconMethod.invoke(appInstance, icon);
    }

    private static void setDockIconJava9(BufferedImage icon) throws Exception {
        Class<?> taskbarClass = Class.forName("java.awt.Taskbar");
        Method isTaskbarSupportedMethod = taskbarClass.getMethod("isTaskbarSupported");
        Boolean isSupported = (Boolean) isTaskbarSupportedMethod.invoke(null);

        if (isSupported) {
            Method getTaskbarMethod = taskbarClass.getMethod("getTaskbar");
            Object taskbar = getTaskbarMethod.invoke(null);

            Method setIconImageMethod = taskbarClass.getMethod("setIconImage", java.awt.Image.class);
            setIconImageMethod.invoke(taskbar, icon);
        }
    }

}
