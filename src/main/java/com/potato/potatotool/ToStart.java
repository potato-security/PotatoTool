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
    /**
     * 内部测试启动页，仅通过启动参数启用。
     * 正常启动不传参数时为 null，不会影响正式流程。
     *
     * 示例：
     * testPage=ai
     * testPage=scan
     */
    private static final String TEST_PAGE_ARG_PREFIX = "testPage=";
    private static StartupPage startupTestPage;

    public enum StartupPage {
        BLUE_WEBSHELL_DECODE(true, 0, "web", "蓝队 / 一键解密"),
        BLUE_SEPARATE_DECODE(true, 1, "sep", "蓝队 / 专项加解密"),
        BLUE_GET_IP_INFO(true, 2, "ip", "蓝队 / IP信息筛选"),
        BLUE_AI_ANSWER(true, 3, "ai", "蓝队 / AI分析"),
        BLUE_DECOMPILE(true, 4, "dec", "蓝队 / 反编译"),
        BLUE_BLOCKCHAIN(true, 5, "chain", "蓝队 / 区块链溯源"),
        BLUE_LOCATION_QUERY(true, 6, "loc", "蓝队 / 归属地查询"),
        BLUE_EXIF(true, 7, "exif", "蓝队 / 文件元信息"),
        BLUE_EXTENSION(true, 8, "ext", "蓝队 / 扩展模块"),
        BLUE_ABOUT(true, 9, "about", "蓝队 / 关于"),
        RED_INFO_SEARCH(false, 10, "info", "红队 / 信息收集"),
        RED_VUL_SCAN(false, 11, "scan", "红队 / 漏洞扫描"),
        RED_FREE_KILL(false, 12, "kill", "红队 / 免杀"),
        RED_CUSTOM_MEMORY_CODE(false, 13, "mem", "红队 / 自定义内存马"),
        RED_CUSTOM_COMMAND_GENERATION(false, 14, "cmdg", "红队 / 命令生成"),
        RED_COMMAND_QUERY(false, 15, "cmd", "红队 / 命令查询"),
        RED_KB_ROOT_QUERY(false, 16, "kb", "红队 / KB提权查询"),
        RED_PROCESS_QUERY(false, 17, "proc", "红队 / 进程分析"),
        RED_INFO_GENERATION(false, 18, "gen", "红队 / 信息生成"),
        RED_EXTENSION(false, 19, "rext", "红队 / 扩展模块"),
        RED_ABOUT(false, 20, "rabout", "红队 / 关于");

        private final boolean blueMode;
        private final int navIndex;
        private final String alias;
        private final String description;

        StartupPage(boolean blueMode, int navIndex, String alias, String description) {
            this.blueMode = blueMode;
            this.navIndex = navIndex;
            this.alias = alias;
            this.description = description;
        }

        public boolean isBlueMode() {
            return blueMode;
        }

        public int getNavIndex() {
            return navIndex;
        }

        public String getAlias() {
            return alias;
        }

        public String getDescription() {
            return description;
        }
    }

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
        startupTestPage = null;
        String guideReason = null;
        boolean shouldShowGuide = false;

        // 检查命令行参数是否包含 "debug" 参数
        for (String arg : args) {
            if (arg.equals("debug") || arg.equals("-debug") || arg.equals("--debug")) {
                debugMode = true;
                System.out.println("------------已开启DEBUG模式，请注意报错信息，用于提取提交bug------------");
            } else if (isHelpArg(arg)) {
                shouldShowGuide = true;
            } else if (arg.contains(TEST_PAGE_ARG_PREFIX)) {
                String pageValue = arg.substring(TEST_PAGE_ARG_PREFIX.length());
                if (isHelpArg(pageValue)) {
                    shouldShowGuide = true;
                } else {
                    StartupPage parsedPage = parseStartupPage(pageValue);
                    if (parsedPage == null) {
                        guideReason = "未识别的 testPage 参数值: " + pageValue;
                        shouldShowGuide = true;
                    } else {
                        startupTestPage = parsedPage;
                    }
                }
            }
        }
        if (shouldShowGuide) {
            printTestPageGuide(guideReason);
            return;
        }
        if (startupTestPage != null) {
            isBlueMode = startupTestPage.isBlueMode();
            System.out.println("------------已启用内部测试页面直达：" + startupTestPage.name() + "------------");
        }
        MainApplication.main(args);
    }

    public static boolean isStartupPageTestEnabled() {
        return startupTestPage != null;
    }

    public static StartupPage getStartupTestPage() {
        return startupTestPage;
    }

    private static StartupPage parseStartupPage(String pageName) {
        if (pageName == null) {
            return null;
        }
        String normalizedName = pageName.trim().toUpperCase().replace('-', '_');
        if (normalizedName.isEmpty()) {
            return null;
        }
        for (StartupPage startupPage : StartupPage.values()) {
            if (startupPage.getAlias().equalsIgnoreCase(normalizedName)
                    || startupPage.name().equalsIgnoreCase(normalizedName)) {
                return startupPage;
            }
        }
        return null;
    }

    private static boolean isHelpArg(String arg) {
        if (arg == null) {
            return false;
        }
        String normalizedArg = arg.trim();
        return "help".equalsIgnoreCase(normalizedArg)
                || "-help".equalsIgnoreCase(normalizedArg)
                || "--help".equalsIgnoreCase(normalizedArg);
    }

    private static void printTestPageGuide(String reason) {
        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        builder.append("PotatoTool 内部测试页面启动指南\n");
        builder.append("================================\n");
        if (reason != null && !reason.trim().isEmpty()) {
            builder.append("原因: ").append(reason).append("\n");
            builder.append("\n");
        }
        builder.append("作用:\n");
        builder.append("  设置 testPage 后，会跳过密码页和加载页，直接打开主界面并选中对应顶部页面。\n");
        builder.append("\n");
        builder.append("用法:\n");
        builder.append("  testPage=<值>\n");
        builder.append("\n");
        builder.append("示例:\n");
        builder.append("  testPage=ai\n");
        builder.append("  testPage=scan\n");
        builder.append("  testPage=BLUE_AI_ANSWER\n");
        builder.append("\n");
        builder.append("帮助:\n");
        builder.append("  help\n");
        builder.append("  testPage=help\n");
        builder.append("\n");
        builder.append("可选值:\n");
        builder.append(buildStartupPageOptions());
        builder.append("\n");
        builder.append("说明:\n");
        builder.append("  1. 不传 testPage 时，按正常流程启动。\n");
        builder.append("  2. testPage 同时支持简写值和完整枚举名。\n");
        builder.append("  3. 如果值写错，会输出本指南并终止启动，避免误以为已生效。\n");
        System.out.println(builder.toString());
    }

    private static String buildStartupPageOptions() {
        StringBuilder builder = new StringBuilder();
        StartupPage[] pages = StartupPage.values();
        for (int i = 0; i < pages.length; i++) {
            builder.append("  ").append(pages[i].getAlias())
                    .append(" -> ")
                    .append(pages[i].getDescription())
                    .append(" (")
                    .append(pages[i].name())
                    .append(")")
                    .append("\n");
        }
        return builder.toString();
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
