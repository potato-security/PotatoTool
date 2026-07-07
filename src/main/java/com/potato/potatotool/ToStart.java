package com.potato.potatotool;

import javafx.fxml.FXMLLoader;
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
    private static final String TEST_WINDOW_ARG_PREFIX = "testWindow=";
    // 启动页独立预览模式，两个别名都可以，高频视觉迭代专用
    private static final String LOAD_PREVIEW_SHORT_ALIAS = "load";
    private static final String LOAD_PREVIEW_FULL_ALIAS = "__loadpage__";
    private static StartupPage startupTestPage;
    private static boolean loadPagePreviewMode = false;
    private static TestWindowSize startupTestWindowSize;

    public static class TestWindowSize {
        private final double width;
        private final double height;

        public TestWindowSize(double width, double height) {
            this.width = width;
            this.height = height;
        }

        public double getWidth() {
            return width;
        }

        public double getHeight() {
            return height;
        }
    }

    public enum StartupPage {
        BLUE_WEBSHELL_DECODE(true, 0, "web", "蓝队 / 一键解密"),
        BLUE_WEBSHELL_DECODE_RULES(true, 0, "webrules", "蓝队 / 一键解密 / 规则选择态"),
        BLUE_WEBSHELL_DECODE_OPTIONS(true, 0, "weboptions", "蓝队 / 一键解密 / Key/Iv/字典配置态"),
        BLUE_WEBSHELL_DECODE_RESULT(true, 0, "webresult", "蓝队 / 一键解密 / 解密结果态"),
        BLUE_WEBSHELL_DECODE_AI(true, 0, "webai", "蓝队 / 一键解密 / AI分析面板态"),
        BLUE_SEPARATE_DECODE(true, 1, "sep", "蓝队 / 专项加解密"),
        BLUE_SEPARATE_DECODE_CHECKED(true, 1, "sepchecked", "蓝队 / 专项加解密 / 多编码勾选态"),
        BLUE_SEPARATE_DECODE_ENCODE_RESULT(true, 1, "sepencode", "蓝队 / 专项加解密 / 加密结果态"),
        BLUE_SEPARATE_DECODE_DECODE_RESULT(true, 1, "sepdecode", "蓝队 / 专项加解密 / 解密结果态"),
        BLUE_GET_IP_INFO(true, 2, "ip", "蓝队 / IP信息筛选"),
        BLUE_GET_IP_INFO_EXTRACT_RESULT(true, 2, "ipresult", "蓝队 / IP信息筛选 / IP提取结果态"),
        BLUE_GET_IP_INFO_LOCATION_RESULT(true, 2, "iploc", "蓝队 / IP信息筛选 / IP提取+归属态"),
        BLUE_GET_IP_INFO_RULES(true, 2, "iprules", "蓝队 / IP信息筛选 / 规则下拉态"),
        BLUE_GET_IP_INFO_EMPTY(true, 2, "ipempty", "蓝队 / IP信息筛选 / 空提取结果态"),
        BLUE_AI_ANSWER(true, 3, "ai", "蓝队 / AI分析"),
        BLUE_DECOMPILE(true, 4, "dec", "蓝队 / 反编译"),
        BLUE_BLOCKCHAIN(true, 5, "chain", "蓝队 / 区块链溯源"),
        BLUE_BLOCKCHAIN_RESULT_LIST(true, 5, "chainresult", "蓝队 / 区块链溯源 / 查询结果列表态"),
        BLUE_BLOCKCHAIN_DETAIL(true, 5, "chaindetail", "蓝队 / 区块链溯源 / 详情页态"),
        BLUE_BLOCKCHAIN_EMPTY(true, 5, "chainempty", "蓝队 / 区块链溯源 / 空结果态"),
        BLUE_LOCATION_QUERY(true, 6, "loc", "蓝队 / 归属地查询"),
        BLUE_LOCATION_QUERY_BANKCARD(true, 6, "locbank", "蓝队 / 归属地查询 / 银行卡查询区"),
        BLUE_LOCATION_QUERY_PHONE(true, 6, "locphone", "蓝队 / 归属地查询 / 手机号查询区"),
        BLUE_EXIF(true, 7, "exif", "蓝队 / 文件元信息"),
        BLUE_EXIF_RESULT(true, 7, "exifresult", "蓝队 / 文件元信息 / 结果态"),
        BLUE_EXIF_FAILURE(true, 7, "exiffail", "蓝队 / 文件元信息 / 失败态"),
        BLUE_EXTENSION(true, 8, "ext", "蓝队 / 扩展模块"),
        BLUE_ABOUT(true, 9, "about", "蓝队 / 关于"),
        RED_AI_PENTEST(false, 10, "aipentest", "红队 / AI自动渗透"),
        RED_AI_PENTEST_PREFLIGHT(false, 10, "aipentestpreflight", "红队 / AI自动渗透 / 预检态"),
        RED_AI_PENTEST_FINDINGS(false, 10, "aipentestfindings", "红队 / AI自动渗透 / 发现列表态"),
        RED_AI_PENTEST_RUNNING(false, 10, "aipentestrunning", "红队 / AI自动渗透 / 执行中态"),
        RED_AI_PENTEST_DONE(false, 10, "aipentestdone", "红队 / AI自动渗透 / 已完成态"),
        RED_AI_PENTEST_APPROVAL(false, 10, "aipentestapproval", "红队 / AI自动渗透 / 审批态"),
        RED_AI_PENTEST_STOPPED(false, 10, "aipenteststopped", "红队 / AI自动渗透 / 已停止态"),
        RED_AI_PENTEST_FAILURE(false, 10, "aipentestfailure", "红队 / AI自动渗透 / 失败态"),
        RED_AI_PENTEST_TIMELINE(false, 10, "aipentesttimeline", "红队 / AI自动渗透 / 技能时间线态"),
        RED_AI_PENTEST_EVIDENCE(false, 10, "aipentestevidence", "红队 / AI自动渗透 / 证据态"),
        RED_AI_PENTEST_LOGS(false, 10, "aipentestlogs", "红队 / AI自动渗透 / 日志态"),
        RED_INFO_SEARCH(false, 11, "info", "红队 / 资产测绘"),
        RED_INFO_SEARCH_PLATFORMS(false, 11, "infoplatforms", "红队 / 资产测绘 / 平台选择态"),
        RED_INFO_SEARCH_ADVANCED(false, 11, "infoadvanced", "红队 / 资产测绘 / 高级设置展开态"),
        RED_INFO_SEARCH_RESULT(false, 11, "inforesult", "红队 / 资产测绘 / 结果态"),
        RED_INFO_SEARCH_PROXY_WARNING(false, 11, "infoproxywarn", "红队 / 资产测绘 / 代理冲突警告态"),
        RED_PORT_SCAN(false, 12, "port", "红队 / 端口扫描"),
        RED_PORT_SCAN_CONFIG(false, 12, "portconfig", "红队 / 端口扫描 / 配置展开态"),
        RED_PORT_SCAN_RESULTS(false, 12, "portresult", "红队 / 端口扫描 / 结果态"),
        RED_PORT_SCAN_HISTORY(false, 12, "porthistory", "红队 / 端口扫描 / 历史面板态"),
        RED_PORT_SCAN_RUNNING(false, 12, "portrunning", "红队 / 端口扫描 / 运行中态"),
        RED_PORT_SCAN_EMPTY(false, 12, "portempty", "红队 / 端口扫描 / 空结果态"),
        RED_VUL_SCAN(false, 13, "scan", "红队 / 漏洞扫描"),
        RED_VUL_SCAN_CONFIG(false, 13, "scanconfig", "红队 / 漏洞扫描 / 配置展开态"),
        RED_VUL_SCAN_ADVANCED(false, 13, "scanadvanced", "红队 / 漏洞扫描 / 高级配置态"),
        RED_VUL_SCAN_PROGRESS_STATS(false, 13, "scanprogress", "红队 / 漏洞扫描 / 进度统计态"),
        RED_VUL_SCAN_RUNNING(false, 13, "scanrunning", "红队 / 漏洞扫描 / 运行中态"),
        RED_VUL_SCAN_EMPTY(false, 13, "scanempty", "红队 / 漏洞扫描 / 空结果态"),
        RED_VUL_SCAN_EXPORT(false, 13, "scanexport", "红队 / 漏洞扫描 / 导出选择态"),
        RED_VUL_SCAN_RESULTS(false, 13, "scanresult", "红队 / 漏洞扫描 / 结果态"),
        RED_VUL_SCAN_POC_MANAGE(false, 13, "scanpoc", "红队 / 漏洞扫描 / POC 管理态"),
        RED_VUL_SCAN_POC_DETAIL(false, 13, "scanpocdetail", "红队 / 漏洞扫描 / POC 详情态"),
        RED_VUL_SCAN_LOGS(false, 13, "scanlogs", "红队 / 漏洞扫描 / 日志态"),
        RED_VUL_SCAN_HISTORY(false, 13, "scanhistory", "红队 / 漏洞扫描 / 历史态"),
        RED_FREE_KILL(false, 14, "kill", "红队 / 免杀"),
        RED_FREE_KILL_SIGNATURE(false, 14, "killsign", "红队 / 免杀 / 伪造签名态"),
        RED_FREE_KILL_QRCODE(false, 14, "killqr", "红队 / 免杀 / 二维码生成态"),
        RED_FREE_KILL_PROTECT(false, 14, "killprotect", "红队 / 免杀 / 加壳配置态"),
        RED_FREE_KILL_RUNNING(false, 14, "killrunning", "红队 / 免杀 / 运行中态"),
        RED_FREE_KILL_DONE(false, 14, "killdone", "红队 / 免杀 / 完成态"),
        RED_FREE_KILL_FAILURE(false, 14, "killfail", "红队 / 免杀 / 失败态"),
        RED_CUSTOM_MEMORY_CODE(false, 15, "mem", "红队 / 自定义内存马"),
        RED_CUSTOM_MEMORY_CODE_TYPE_OPTIONS(false, 15, "memtype", "红队 / 自定义内存马 / 类型选项态"),
        RED_CUSTOM_MEMORY_CODE_CUSTOM_OPTIONS(false, 15, "memcustom", "红队 / 自定义内存马 / 自定义选项态"),
        RED_CUSTOM_MEMORY_CODE_GADGET(false, 15, "memgadget", "红队 / 自定义内存马 / 专项利用态"),
        RED_CUSTOM_MEMORY_CODE_RESULT(false, 15, "memresult", "红队 / 自定义内存马 / 生成结果态"),
        RED_PAYLOAD_TOOLBOX(false, 16, "payload", "红队 / Payload 工具箱"),
        RED_PAYLOAD_TOOLBOX_YSO(false, 16, "payloadyso", "红队 / Payload 工具箱 / Yso 默认态"),
        RED_PAYLOAD_TOOLBOX_YSO_HELPER(false, 16, "payloadhelper", "红队 / Payload 工具箱 / 结构化辅助态"),
        RED_PAYLOAD_TOOLBOX_RESULT(false, 16, "payloadresult", "红队 / Payload 工具箱 / 结果态"),
        RED_CUSTOM_COMMAND_GENERATION(false, 17, "cmdg", "红队 / 命令生成"),
        RED_CUSTOM_COMMAND_GENERATION_OPTIONS(false, 17, "cmdgoptions", "红队 / 命令生成 / 参数选择态"),
        RED_CUSTOM_COMMAND_GENERATION_RESULT(false, 17, "cmdgresult", "红队 / 命令生成 / 结果列表态"),
        RED_COMMAND_QUERY(false, 18, "cmd", "红队 / 命令查询"),
        RED_COMMAND_QUERY_RESULT(false, 18, "cmdresult", "红队 / 命令查询 / 查询结果态"),
        RED_KB_ROOT_QUERY(false, 19, "kb", "红队 / KB提权查询"),
        RED_KB_ROOT_QUERY_RESULT(false, 19, "kbresult", "红队 / KB提权查询 / 提取结果态"),
        RED_PROCESS_QUERY(false, 20, "proc", "红队 / 进程分析"),
        RED_PROCESS_QUERY_RESULT(false, 20, "procresult", "红队 / 进程分析 / 提取结果态"),
        RED_INFO_GENERATION(false, 21, "gen", "红队 / 信息生成"),
        RED_INFO_GENERATION_RESULT(false, 21, "genresult", "红队 / 信息生成 / 结果态"),
        RED_EXTENSION(false, 22, "rext", "红队 / 扩展模块"),
        RED_ABOUT(false, 23, "rabout", "红队 / 关于"),
        // 弹窗直达（测试用）：先落到一个基础页(navIndex 0)，加载后再弹出对应弹窗
        PUBLIC_SETTING_DIALOG(true, 0, "set", "公共 / 设置弹窗", "set"),
        PUBLIC_DELETE_CONFIRM_DIALOG(true, 0, "delete", "公共 / 危险确认弹窗", "delete");

        private final boolean blueMode;
        private final int navIndex;
        private final String alias;
        private final String description;
        private final String dialogType;

        StartupPage(boolean blueMode, int navIndex, String alias, String description) {
            this(blueMode, navIndex, alias, description, null);
        }

        StartupPage(boolean blueMode, int navIndex, String alias, String description, String dialogType) {
            this.blueMode = blueMode;
            this.navIndex = navIndex;
            this.alias = alias;
            this.description = description;
            this.dialogType = dialogType;
        }

        public String getDialogType() {
            return dialogType;
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
        ClassLoader appClassLoader = ToStart.class.getClassLoader();
        Thread.currentThread().setContextClassLoader(appClassLoader);
        FXMLLoader.setDefaultClassLoader(appClassLoader);
        // 为 macOS dock 设置图标
        setMacDockIcon();
        // 关闭Optional.or告警
        setOptionalException();
        startupTestPage = null;
        startupTestWindowSize = null;
        loadPagePreviewMode = false;
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
                } else if (isLoadPreviewArg(pageValue)) {
                    // 启动页独立预览：仅渲染 PaneLoad，不加载主界面，fade out 后退出 JVM
                    loadPagePreviewMode = true;
                } else {
                    StartupPage parsedPage = parseStartupPage(pageValue);
                    if (parsedPage == null) {
                        guideReason = "未识别的 testPage 参数值: " + pageValue;
                        shouldShowGuide = true;
                    } else {
                        startupTestPage = parsedPage;
                    }
                }
            } else if (arg.contains(TEST_WINDOW_ARG_PREFIX)) {
                String windowValue = arg.substring(TEST_WINDOW_ARG_PREFIX.length());
                TestWindowSize parsedWindowSize = parseTestWindowSize(windowValue);
                if (parsedWindowSize == null) {
                    guideReason = "未识别的 testWindow 参数值: " + windowValue;
                    shouldShowGuide = true;
                } else {
                    startupTestWindowSize = parsedWindowSize;
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
        if (loadPagePreviewMode) {
            System.out.println("------------[F1] 启动页独立预览模式：仅渲染 PaneLoad、动画结束后退出------------");
        }
        MainApplication.main(args);
    }

    public static boolean isStartupPageTestEnabled() {
        return startupTestPage != null;
    }

    public static StartupPage getStartupTestPage() {
        return startupTestPage;
    }

    /**
     * 是否在启动页独立预览模式。
     * MainApplication 检测到后只渲染 PaneLoad，动画结束即退出 JVM。
     */
    public static boolean isLoadPagePreviewMode() {
        return loadPagePreviewMode;
    }

    public static TestWindowSize getStartupTestWindowSize() {
        return startupTestWindowSize;
    }

    private static boolean isLoadPreviewArg(String arg) {
        if (arg == null) return false;
        String trimmed = arg.trim();
        return LOAD_PREVIEW_SHORT_ALIAS.equalsIgnoreCase(trimmed)
                || LOAD_PREVIEW_FULL_ALIAS.equalsIgnoreCase(trimmed);
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

    private static TestWindowSize parseTestWindowSize(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String normalized = rawValue.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return null;
        }
        String[] parts = normalized.split("x");
        if (parts.length != 2) {
            return null;
        }
        try {
            int width = Integer.parseInt(parts[0].trim());
            int height = Integer.parseInt(parts[1].trim());
            if (width <= 0 || height <= 0) {
                return null;
            }
            return new TestWindowSize(width, height);
        } catch (NumberFormatException ex) {
            return null;
        }
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
        builder.append("  testWindow=<宽>x<高>\n");
        builder.append("\n");
        builder.append("示例:\n");
        builder.append("  testPage=ai\n");
        builder.append("  testPage=scan\n");
        builder.append("  testWindow=1280x800\n");
        builder.append("  testPage=ai testWindow=1280x800\n");
        builder.append("  testPage=BLUE_AI_ANSWER\n");
        builder.append("  testPage=load        # 启动页独立预览，动画结束即退出\n");
        builder.append("  testPage=__loadpage__ # 同上，完整别名\n");
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
        builder.append("  3. testWindow 仅用于视觉测试窗口尺寸，例如 1280x800。\n");
        builder.append("  4. 如果值写错，会输出本指南并终止启动，避免误以为已生效。\n");
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
