package com.potato.potatotool;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.controller.publicPane.PaneLoad;
import com.potato.potatotool.controller.publicPane.PanePasswd;
import com.potato.potatotool.controller.publicPane.PaneUpdateDialog;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.content.redTeam.vulnScanner.http.DnsLogService;
import com.potato.potatotool.content.redTeam.vulnScanner.http.HttpLogService;
import com.potato.potatotool.content.redTeam.vulnScanner.http.PythonHandler;
import com.potato.potatotool.content.redTeam.vulnScanner.storage.PocDatabaseInitializer;
import com.potato.potatotool.storage.PathManager;
import com.potato.potatotool.update.ResourceUpdate;
import com.potato.potatotool.update.UpdateInfo;
import com.potato.potatotool.update.UpdateManager;
import com.potato.potatotool.update.resource.ResourceUpdater;
import com.potato.potatotool.utils.browser.BrowserRuntimeConfig;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.core.ExecutorServiceManager;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.crypto.SecurityInitializer;
import com.potato.potatotool.utils.data.GzipUtils;
import com.potato.potatotool.utils.network.ProxyUtils;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.core.Constants.*;

public class MainApplication extends Application {
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static volatile String startupProxyWarningMessage;

    @Override
    public void start(Stage stage) throws IOException {
        ClassLoader appClassLoader = MainApplication.class.getClassLoader();
        Thread.currentThread().setContextClassLoader(appClassLoader);
        FXMLLoader.setDefaultClassLoader(appClassLoader);
        hostServices = getHostServices();
        final boolean skipStartupPages = ToStart.isStartupPageTestEnabled();

//        double screenWidth = Screen.getPrimary().getVisualBounds().getWidth();
//        double screenHeight = Screen.getPrimary().getVisualBounds().getHeight();
//        Screen screen = Screen.getPrimary();
//        double dpi = screen.getDpi();
//        double scale = dpi / 151; // 测试机DPI为151

        // 初始化配置及BC.jar文件
        Task<Void> taskInit = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                initEnvFile();
                BrowserRuntimeConfig.initializeAtStartup();
                PythonHandler.initializeAtStartup();
                ProxyUtils.ProxyReachabilityResult proxyResult = ProxyUtils.ensureMainProxyAvailability();
                if (!proxyResult.isReachable()) {
                    startupProxyWarningMessage = ProxyUtils.buildUnavailableMessage(proxyResult);
                    if (debugMode) {
                        System.out.println("启动阶段检测到代理不可用，已自动关闭主代理: " + I18nUtils.getString(ProxyUtils.getReasonKey(proxyResult)));
                    }
                }
                applyOobSettings();
                checkForUpdatesOnStartup();
                return null;
            }
        };

        executor.submit(taskInit);

        BooleanProperty preload = new SimpleBooleanProperty(false);
        AtomicReference<FadeTransition> fadeTransition1 = new AtomicReference<FadeTransition>();
        if (!skipStartupPages) {
            //  输入密码界面stage
            Stage passwdStage = new Stage();
            passwdStage.setAlwaysOnTop(true);
            passwdStage.initStyle(StageStyle.TRANSPARENT);
            FXMLLoader passwdLoader = new FXMLLoader(getClass().getResource("/fxml/publicPane/passwd.fxml"));
            Scene passwdScene = new Scene(passwdLoader.load());
            passwdScene.setCamera(new PerspectiveCamera());
            passwdScene.setFill(null);
            passwdStage.setScene(passwdScene);
            String iconPath = "/img/logo.png";
            try {
                // 为 JavaFX 窗口设置图标（这会影响 Windows 任务栏和 Linux 的dock）
                InputStream iconStream = getClass().getResourceAsStream(iconPath);
                if (iconStream != null) {
                    Image image = new Image(iconStream);
                    passwdStage.getIcons().add(image);
                    passwdStage.setTitle("PotatoTool");
                    stage.getIcons().add(image);
                    stage.setTitle("PotatoTool");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            passwdStage.show();

            PanePasswd pwdController = passwdLoader.getController();
            pwdController.passwdProperty().addListener((obs_x, oldValue_x, newValue_x) -> {
                // 提前隐藏展示，防止动画卡顿
                if(preload.get()) {
                    stage.show();
                    loadMainStage(passwdStage, fadeTransition1);
                }else {
                    preload.addListener((observable_y, oldValue_y, newValue_y) -> {
                        stage.show();
                        loadMainStage(passwdStage, fadeTransition1);
                    });
                }
            });
        } else {
            String iconPath = "/img/logo.png";
            try {
                InputStream iconStream = getClass().getResourceAsStream(iconPath);
                if (iconStream != null) {
                    Image image = new Image(iconStream);
                    stage.getIcons().add(image);
                    stage.setTitle("PotatoTool");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        // 提前加载主界面
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws IOException {

                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
                Scene scene = new Scene(fxmlLoader.load());
                Platform.runLater(() -> {
                    try {
//                        scene.getRoot().setScaleX(scale);
//                        scene.getRoot().setScaleY(scale);

                        stage.setFullScreenExitHint("");

                        scene.getStylesheets().add(Constants.getResourceUrl("/css/common.css"));
                        scene.setCamera(new PerspectiveCamera());   //  添加摄像机
                        stage.initStyle(StageStyle.TRANSPARENT);    //  边框透明
                        scene.setFill(null);    //  背景透明
                        scene.getRoot().getStyleClass().add("blueStyle");   //  默认蓝队样式
                        stage.setScene(scene);
                        scene.getRoot().setOpacity(0);
                        fadeTransition1.set(new FadeTransition(Duration.seconds(0.3), scene.getRoot()));
                        fadeTransition1.get().setFromValue(0);
                        fadeTransition1.get().setToValue(1);
                        fadeTransition1.get().setCycleCount(1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                return null;
            }
        };
        task.setOnFailed(e -> {
            ExecutorServiceManager.shutdownAll();
            Throwable error = task.getException();
            error.printStackTrace();
            System.exit(0);
        });
        task.setOnSucceeded(e -> {
            if (skipStartupPages) {
                Platform.runLater(() -> showMainStage(stage, fadeTransition1));
            } else {
                preload.set(true);
            }
        });
        executor.submit(task);
    }

    private void showMainStage(Stage stage, AtomicReference<FadeTransition> fadeTransition1) {
        if (stage.getScene() == null) {
            Platform.runLater(() -> showMainStage(stage, fadeTransition1));
            return;
        }
        stage.show();
        FadeTransition fadeTransition = fadeTransition1.get();
        if (fadeTransition != null) {
            fadeTransition.play();
        } else if (stage.getScene() != null && stage.getScene().getRoot() != null) {
            stage.getScene().getRoot().setOpacity(1);
        }
    }

    private void loadMainStage(Stage passwdStage, AtomicReference<FadeTransition> fadeTransition1) {
        try {
            Stage loadStage = new Stage();
            loadStage.initStyle(StageStyle.TRANSPARENT);
            loadStage.setAlwaysOnTop(true);  // 加载页面置顶
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/publicPane/load.fxml"));
            Scene loadScene = new Scene(loader.load());
            loadScene.setCamera(new PerspectiveCamera());
            loadScene.setFill(null);
            loadStage.setScene(loadScene);
            String iconPath = "/img/logo.png";
            try {
                // 为 JavaFX 窗口设置图标（这会影响 Windows 任务栏和 Linux 的dock）
                InputStream iconStream = getClass().getResourceAsStream(iconPath);
                if (iconStream != null) {
                    Image image = new Image(iconStream);
                    loadStage.getIcons().add(image);
                    loadStage.setTitle("PotatoTool");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            passwdStage.close();
            loadStage.show();

            PaneLoad controller = loader.getController();
            FadeTransition fadeTransition = new FadeTransition(Duration.seconds(0.3), loadScene.getRoot());
            fadeTransition.setFromValue(1);
            fadeTransition.setToValue(0);
            fadeTransition.setCycleCount(1);

            fadeTransition.setOnFinished(event -> {
                loadStage.close();
                controller.stopAnimations();
                fadeTransition1.get().play();
            });

            controller.loadedProperty().addListener((obs, oldValue, newValue) -> {
                fadeTransition.play();
            });
        }catch (Exception e){
            if(debugMode)e.printStackTrace();
        }
    }


    private void initEnvFile() {
        // 使用PathManager管理路径
        PathManager pathManager = PathManager.getInstance();
        
        Path configFolder = pathManager.getConfigBasePath();
        Path resourceFolder = pathManager.getResourceBasePath();

        // 1. 初始化配置文件（简化逻辑，只判断是否存在）
        try {
            Files.createDirectories(configFolder);
            Path configFile = pathManager.getConfigFilePath();
            
            if (!Files.exists(configFile)) {
                if (debugMode) System.out.println("检测到本地配置文件不存在，正在初始化...");
                JsonObject config = com.google.gson.JsonParser.parseString(getResourceString("config")).getAsJsonObject();
                Files.write(configFile, (new Gson()).toJson(config).getBytes(StandardCharsets.UTF_8));
                Constants.cachedConfig = null;
                if (debugMode) System.out.println("本地配置文件初始化完成");
            } else {
                // 配置文件存在，尝试合并新增字段（向后兼容）
                try {
                    JsonElement localConfig = (JsonElement) Constants.getOutsideConfig(null);
                    JsonObject resourceConfig = com.google.gson.JsonParser.parseString(getResourceString("config")).getAsJsonObject();
                    // 合并配置（保留本地值，添加新字段）
                    JsonElement merged = mergeJsonElements(localConfig, resourceConfig);
                    JsonElement normalized = normalizeAiConfigAfterMerge(localConfig, merged, resourceConfig);
                    
                    // 只有在配置真正发生变化时才保存（避免每次启动都重写文件）
                    // 使用 JSON 字符串比较，忽略格式差异
                    Gson gson = new Gson();
                    String localJson = gson.toJson(localConfig);
                    String mergedJson = gson.toJson(normalized);
                    
                    if (!localJson.equals(mergedJson)) {
                        if (debugMode) System.out.println("检测到配置结构变化，正在更新本地配置文件...");
                        saveConfig(normalized);
                        Constants.cachedConfig = null;
                        if (debugMode) System.out.println("本地配置文件已更新");
                    }
                } catch (Exception e) {
                    if(debugMode) {
                        System.err.println("合并配置文件失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("初始化配置文件失败");
            e.printStackTrace();
        }

        // 2. 释放BC加密库（必需，每次检查完整性）
        try {
            String propertyName = "bcprov";
            String fileName = Paths.get(getConfigInfo(propertyName)).getFileName().toString();
            Path targetPath = pathManager.getBcprovPath();
            
            // 确保资源目录存在
            Files.createDirectories(resourceFolder);
            
            // 使用常量检查文件是否完整
            if (!Files.exists(targetPath) || 
                Files.size(targetPath) < PathManager.BCPROV_EXPECTED_SIZE) {
                System.out.println("释放BC加密库: " + fileName);
                copyResourceToFile(propertyName, targetPath, 
                                 PathManager.BCPROV_EXPECTED_SIZE);
            }
            
            SecurityInitializer.initializeSecurityProvider();
        } catch (Exception e) {
            System.err.println("初始化BC加密库失败");
            e.printStackTrace();
        }

        // 3. 释放IP地理位置库（必需，每次检查完整性）
        try {
            String propertyName = "ip2region";
            String fileName = Paths.get(getConfigInfo(propertyName)).getFileName().toString();
            Path targetPath = pathManager.getIp2RegionPath();
            
            Files.createDirectories(resourceFolder);
            
            // 使用常量检查文件是否完整
            if (!Files.exists(targetPath) || 
                Files.size(targetPath) < PathManager.IP2REGION_EXPECTED_SIZE) {
                System.out.println("释放IP地理位置库: " + fileName);
                copyResourceToFile(propertyName, targetPath, 
                                 PathManager.IP2REGION_EXPECTED_SIZE);
            }
        } catch (Exception e) {
            System.err.println("初始化IP地理位置库失败");
            e.printStackTrace();
        }

        // 4. 释放Windows补丁信息库基础版本（必需资源，但可在线更新）
        try {
            String propertyName = "winKbInfo";
            String fileName = Paths.get(getConfigInfo(propertyName)).getFileName().toString();
            
            // 检查是否已存在任何版本的winKbInfo文件
            if (!hasFileWithPrefix(resourceFolder, propertyName)) {
                System.out.println("释放Windows补丁信息库基础版本: " + fileName);
                Path targetPath = resourceFolder.resolve(fileName);
                
                // 使用常量
                copyResourceToFile(propertyName, targetPath, 
                                 PathManager.WINKB_EXPECTED_SIZE);
                
                // 更新资源配置
                ResourceUpdater.updateLocalResourceConfig(
                    propertyName, targetPath.toString());
            }
        } catch (Exception e) {
            System.err.println("初始化Windows补丁信息库失败");
            e.printStackTrace();
        }

        // 5. 处理之前下载的MD5数据库压缩包（如果存在则解压）
        try {
            Path md5GzipPath = pathManager.getMd5DatabaseGzipPath();
            if (Files.exists(md5GzipPath)) {
                if (debugMode) System.out.println("检测到已下载的MD5数据库压缩包，正在解压...");
                Path md5DbPath = pathManager.getMd5DatabasePath();

                // 解压并删除压缩包
                GzipUtils.unGzipFile(md5GzipPath.toString(), md5DbPath.toString(), true);
                if (debugMode) System.out.println("MD5数据库解压完成");
            }
        } catch (Exception e) {
            System.err.println("解压MD5数据库失败");
            e.printStackTrace();
        }

        // 6. 异步初始化POC数据库（不阻塞启动，提升启动速度）
        executor.submit(() -> {
            try {
                if (debugMode) System.out.println("正在后台初始化POC数据库...");
                PocDatabaseInitializer initializer = PocDatabaseInitializer.getInstance();

                PocDatabaseInitializer.InitResult result = initializer.initialize(
                    new PocDatabaseInitializer.InitCallback() {
                        @Override
                        public void onProgress(String message) {
                            if (debugMode) System.out.println("  " + message);
                        }

                        @Override
                        public void onFailures(java.util.List<com.potato.potatotool.content.redTeam
                                .vulnScanner.storage.PocDatabaseManager.FailedEntry> failedEntries) {
                            if (failedEntries != null && !failedEntries.isEmpty()) {
                                System.err.println("⚠ 以下 POC 加载失败 (" + failedEntries.size() + " 个):");
                                // 只显示前 10 个失败项，避免输出过多
                                int showCount = Math.min(failedEntries.size(), 10);
                                for (int i = 0; i < showCount; i++) {
                                    System.err.println("  - " + failedEntries.get(i));
                                }
                                if (failedEntries.size() > 10) {
                                    System.err.println("  ... 还有 " + (failedEntries.size() - 10) + " 个未显示");
                                }
                            }
                        }
                    }
                );

                if (result.isSuccess()) {
                    StringBuilder sb = new StringBuilder();
                    if(result.getPocCount()>0 || result.getSkipCount()>0 || result.getFailedCount() > 0) {
                        sb.append("✓ POC数据库初始化完成: 成功 ").append(result.getPocCount()).append(" 个");
                    }
                    if (result.getSkipCount() > 0) {
                        sb.append(", 跳过 ").append(result.getSkipCount()).append(" 个");
                    }
                    if (result.getFailedCount() > 0) {
                        sb.append(", 失败 ").append(result.getFailedCount()).append(" 个");
                    }
                    System.out.println(sb.toString());
                } else {
                    System.err.println("✗ POC数据库初始化失败: " + result.getMessage());
                }
            } catch (Exception e) {
                System.err.println("初始化POC数据库失败: " + e.getMessage());
                if (debugMode) e.printStackTrace();
            }
        });
    }

    private JsonElement normalizeAiConfigAfterMerge(JsonElement localConfig,
                                                    JsonElement mergedConfig,
                                                    JsonObject resourceConfig) {
        if (mergedConfig == null || !mergedConfig.isJsonObject()) {
            return mergedConfig;
        }

        JsonObject mergedRoot = mergedConfig.getAsJsonObject().deepCopy();
        JsonObject mergedAi = getAiConfig(mergedRoot);
        JsonObject resourceAi = getAiConfig(resourceConfig);
        JsonObject localAi = localConfig != null && localConfig.isJsonObject()
                ? getAiConfig(localConfig.getAsJsonObject())
                : null;
        if (mergedAi == null || resourceAi == null) {
            return mergedRoot;
        }

        boolean localHasBuiltinMode = localAi != null
                && localAi.has(ConfigConstants.AI_USE_BUILTIN_GATEWAY)
                && !localAi.get(ConfigConstants.AI_USE_BUILTIN_GATEWAY).isJsonNull();
        boolean useBuiltinGateway;
        if (!localHasBuiltinMode) {
            useBuiltinGateway = !hasPlainAiConfig(localAi);
            mergedAi.addProperty(ConfigConstants.AI_USE_BUILTIN_GATEWAY, useBuiltinGateway);
            if (useBuiltinGateway) {
                copyAiField(mergedAi, resourceAi, ConfigConstants.AI_LOCAL_BASE_URL);
                copyAiField(mergedAi, resourceAi, ConfigConstants.AI_LOCAL_API_KEY);
                copyAiField(mergedAi, resourceAi, ConfigConstants.AI_LOCAL_MODEL_NAME);
            }
        } else {
            useBuiltinGateway = readBoolean(mergedAi, ConfigConstants.AI_USE_BUILTIN_GATEWAY, true);
        }

        copyAiFieldIfMissing(mergedAi, resourceAi, ConfigConstants.AI_LOCAL_BASE_URL);
        copyAiFieldIfMissing(mergedAi, resourceAi, ConfigConstants.AI_LOCAL_API_KEY);
        copyAiFieldIfMissing(mergedAi, resourceAi, ConfigConstants.AI_LOCAL_MODEL_NAME);

        if (useBuiltinGateway) {
            mergedAi.addProperty(ConfigConstants.AI_PROVIDER, "OPENAI");
            mergedAi.addProperty(ConfigConstants.AI_BASE_URL, "");
            mergedAi.addProperty(ConfigConstants.AI_API_KEY, "");
            mergedAi.addProperty(ConfigConstants.AI_MODEL_NAME, "");
        }

        return mergedRoot;
    }

    private JsonObject getAiConfig(JsonObject root) {
        if (root == null || !root.has(ConfigConstants.AI) || !root.get(ConfigConstants.AI).isJsonObject()) {
            return null;
        }
        return root.getAsJsonObject(ConfigConstants.AI);
    }

    private boolean hasPlainAiConfig(JsonObject aiConfig) {
        if (aiConfig == null) {
            return false;
        }
        return hasText(aiConfig, ConfigConstants.AI_BASE_URL)
                || hasText(aiConfig, ConfigConstants.AI_API_KEY)
                || hasText(aiConfig, ConfigConstants.AI_MODEL_NAME);
    }

    private boolean hasText(JsonObject aiConfig, String key) {
        if (aiConfig == null || !aiConfig.has(key) || aiConfig.get(key).isJsonNull()) {
            return false;
        }
        String value = aiConfig.get(key).getAsString();
        return value != null && !value.trim().isEmpty();
    }

    private boolean readBoolean(JsonObject config, String key, boolean defaultValue) {
        if (config == null || !config.has(key) || config.get(key).isJsonNull()) {
            return defaultValue;
        }
        try {
            return config.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private void copyAiFieldIfMissing(JsonObject targetAi, JsonObject sourceAi, String key) {
        if (targetAi == null || sourceAi == null || !sourceAi.has(key)) {
            return;
        }
        if (!hasText(targetAi, key)) {
            targetAi.add(key, sourceAi.get(key).deepCopy());
        }
    }

    private void copyAiField(JsonObject targetAi, JsonObject sourceAi, String key) {
        if (targetAi == null || sourceAi == null || !sourceAi.has(key)) {
            return;
        }
        targetAi.add(key, sourceAi.get(key).deepCopy());
    }

    private static HostServices hostServices;

    public static void setHostServices(HostServices services) {
        hostServices = services;
    }
    public static HostServices letGetHostServices() {
        return hostServices;
    }

    public static String consumeStartupProxyWarningMessage() {
        String message = startupProxyWarningMessage;
        startupProxyWarningMessage = null;
        return message;
    }

    /**
     * 启动时检查更新
     */
    private void checkForUpdatesOnStartup() {
        try {
            // 检查是否开启自动检查更新
            JsonObject config = (JsonObject) Constants.getOutsideConfig(null);
            if (config != null && config.has("UpDate")) {
                JsonObject updateConfig = config.getAsJsonObject("UpDate");
                if (!shouldCheckForUpdatesOnStartup(updateConfig)) {
                    if (debugMode) System.out.println("当前启动条件下跳过自动检查更新");
                    return;
                }
            }
            
            if (debugMode) System.out.println("开始检查更新...");
            
            UpdateManager updateManager = UpdateManager.getInstance();
            
            UpdateInfo updateInfo = updateManager.checkForUpdates();
            
            if (updateInfo.hasAnyUpdate()) {
                boolean shouldShowDialog = false;
                
                // 检查软件更新
                boolean hasAppUpdate = false;
                if (updateInfo.isAppNeedUpdate()) {
                    String remoteVersion = updateInfo.getAppVersion().getVersion();
                    if (isVersionSkipped(remoteVersion)) {
                        if (debugMode) System.out.println("软件版本 " + remoteVersion + " 已被跳过，不提示软件更新");
                    } else {
                        hasAppUpdate = true;
                        shouldShowDialog = true;
                    }
                }
                
                // 检查资源更新（即使APP被跳过，资源更新仍要显示）
                // 但需要过滤掉被跳过的资源
                int totalResourceUpdates = updateInfo.getResourceUpdates().size();
                int unskippedResourceCount = 0;
                if (updateInfo.hasResourceUpdates()) {
                    for (ResourceUpdate ru : updateInfo.getResourceUpdates()) {
                        if (!isResourceSkipped(ru.getResourceName(), ru.getRemoteVersion())) {
                            unskippedResourceCount++;
                        }
                    }
                    
                    if (unskippedResourceCount > 0) {
                        if (debugMode) System.out.println("发现 " + unskippedResourceCount + " 个资源需要更新（" + 
                                         (totalResourceUpdates - unskippedResourceCount) + " 个已跳过）");
                        shouldShowDialog = true;
                    } else {
                        if (debugMode) System.out.println("有 " + totalResourceUpdates + " 个资源更新，但全部已被跳过");
                    }
                }
                
                // 只要有需要显示的更新（APP未跳过 或 有资源更新），就显示对话框
                if (shouldShowDialog) {
                    if (debugMode) {
                        System.out.println("发现可用更新:");
                        if (hasAppUpdate) {
                            System.out.println("  - 软件更新: " + updateInfo.getAppVersion().getVersion());
                        }
                        if (updateInfo.hasResourceUpdates()) {
                            System.out.println("  - 资源更新: " + updateInfo.getResourceUpdates().size() + " 个");
                        }
                    }
                    
                    // 在UI线程显示更新对话框
                    Platform.runLater(() -> showUpdateDialogOnStartup(updateInfo));
                } else {
                    if (debugMode) System.out.println("所有更新都已被跳过，不显示更新窗口");
                }
            }
            
        } catch (Exception e) {
            // 检查更新失败不输出到控制台（避免用户困扰），只在debug模式输出
            if (debugMode) {
                System.err.println("检查更新失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean shouldCheckForUpdatesOnStartup(JsonObject updateConfig) {
        if (updateConfig == null) {
            return true;
        }

        if (updateConfig.has(ConfigConstants.UPDATE_AUTO_CHECK)
                && !updateConfig.get(ConfigConstants.UPDATE_AUTO_CHECK).getAsBoolean()) {
            return false;
        }

        String interval = "startup";
        if (updateConfig.has(ConfigConstants.UPDATE_CHECK_INTERVAL)) {
            interval = updateConfig.get(ConfigConstants.UPDATE_CHECK_INTERVAL).getAsString();
        }

        if ("manual".equalsIgnoreCase(interval)) {
            return false;
        }
        if ("startup".equalsIgnoreCase(interval)) {
            return true;
        }
        if (!updateConfig.has(ConfigConstants.UPDATE_LAST_CHECK_TIME)) {
            return true;
        }

        try {
            Instant lastCheckTime = Instant.parse(
                    updateConfig.get(ConfigConstants.UPDATE_LAST_CHECK_TIME).getAsString());
            java.time.Duration elapsed = java.time.Duration.between(lastCheckTime, Instant.now());
            if ("daily".equalsIgnoreCase(interval)) {
                return elapsed.toHours() >= 24;
            }
            if ("weekly".equalsIgnoreCase(interval)) {
                return elapsed.toDays() >= 7;
            }
        } catch (Exception e) {
            if (debugMode) {
                System.err.println("解析更新时间失败，按需要检查处理: " + e.getMessage());
            }
            return true;
        }

        return true;
    }
    
    /**
     * 检查版本是否被跳过
     */
    private boolean isVersionSkipped(String version) {
        try {
            JsonObject config = (JsonObject) Constants.getOutsideConfig(null);
            if (config != null && config.has("UpDate")) {
                JsonObject updateConfig = config.getAsJsonObject("UpDate");
                if (updateConfig.has("skippedVersions")) {
                    JsonArray skippedVersions = updateConfig.getAsJsonArray("skippedVersions");
                    for (JsonElement elem : skippedVersions) {
                        if (elem.getAsString().equals(version)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return false;
    }
    
    /**
     * 检查资源是否被跳过
     */
    private boolean isResourceSkipped(String resourceName, String version) {
        try {
            JsonObject updateConfig = (JsonObject) 
                    Constants.getOutsideConfig(ConfigConstants.UPDATE);
            if (updateConfig != null && updateConfig.has(ConfigConstants.UPDATE_RESOURCES)) {
                JsonObject resources = updateConfig.getAsJsonObject(
                        ConfigConstants.UPDATE_RESOURCES);
                if (resources.has(resourceName)) {
                    JsonObject resourceConfig = resources.getAsJsonObject(resourceName);
                    // 检查skippedVersions数组
                    if (resourceConfig.has(ConfigConstants.UPDATE_SKIPPED_VERSIONS)) {
                        JsonElement skippedElem = resourceConfig.get(
                                ConfigConstants.UPDATE_SKIPPED_VERSIONS);
                        if (skippedElem.isJsonArray()) {
                            for (JsonElement elem : skippedElem.getAsJsonArray()) {
                                if (elem.getAsString().equals(version)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return false;
    }
    
    /**
     * 启动时显示更新对话框
     */
    private void showUpdateDialogOnStartup(UpdateInfo updateInfo) {
        try {
            FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/publicPane/update_dialog.fxml"));
            Scene scene = new Scene(loader.load());
            
            scene.setFill(null);
            scene.getStylesheets().add(Constants.getResourceUrl("/css/common.css"));
            
            Stage updateStage = new Stage();
            updateStage.initStyle(StageStyle.TRANSPARENT);
            updateStage.setScene(scene);
            updateStage.setTitle("PotatoTool - " + 
                                I18nUtils.getString("update.check.title"));
            updateStage.setAlwaysOnTop(true);  // 更新对话框置顶
            
            // 设置图标
            try {
                InputStream iconStream = getClass().getResourceAsStream("/img/logo.png");
                if (iconStream != null) {
                    Image image = new Image(iconStream);
                    updateStage.getIcons().add(image);
                }
            } catch (Exception e) {
                // 忽略图标设置错误
            }
            
            // 设置更新信息
            PaneUpdateDialog controller = 
                    loader.getController();
            controller.setUpdateInfo(updateInfo);
            
            updateStage.show();
            
        } catch (Exception e) {
            System.err.println("显示更新对话框失败: " + e.getMessage());
            if (debugMode) {
                e.printStackTrace();
            }
        }
    }
    
    private void applyOobSettings() {
        try {
            JsonObject root = (JsonObject) Constants.getOutsideConfig(null);
            if (root == null || !root.has(ConfigConstants.OOB)) {
                return;
            }

            JsonObject oob = root.getAsJsonObject(ConfigConstants.OOB);
            if (oob.has(ConfigConstants.OOB_HTTP)) {
                JsonObject http = oob.getAsJsonObject(ConfigConstants.OOB_HTTP);

                String platform = http.has(ConfigConstants.OOB_HTTP_PLATFORM)
                        ? http.get(ConfigConstants.OOB_HTTP_PLATFORM).getAsString()
                        : HttpLogService.Platform.INTERACTSH.name();

                String interactshServer = http.has(ConfigConstants.OOB_HTTP_INTERACTSH_SERVER)
                        ? http.get(ConfigConstants.OOB_HTTP_INTERACTSH_SERVER).getAsString()
                        : "oast.pro";
                String interactshToken = http.has(ConfigConstants.OOB_HTTP_INTERACTSH_TOKEN)
                        ? http.get(ConfigConstants.OOB_HTTP_INTERACTSH_TOKEN).getAsString()
                        : null;
                HttpLogService.configureInteractsh(interactshServer, interactshToken);

                String customServer = http.has(ConfigConstants.OOB_HTTP_CUSTOM_SERVER)
                        ? http.get(ConfigConstants.OOB_HTTP_CUSTOM_SERVER).getAsString()
                        : null;
                String customToken = http.has(ConfigConstants.OOB_HTTP_CUSTOM_TOKEN)
                        ? http.get(ConfigConstants.OOB_HTTP_CUSTOM_TOKEN).getAsString()
                        : null;
                if (customServer != null && !customServer.trim().isEmpty()) {
                    HttpLogService.configureCustom(customServer, customToken);
                }

                long cacheTtlSeconds = http.has(ConfigConstants.OOB_HTTP_CACHE_TTL_SECONDS)
                        ? http.get(ConfigConstants.OOB_HTTP_CACHE_TTL_SECONDS).getAsLong()
                        : 3600L;
                HttpLogService.setCacheDurationSeconds(cacheTtlSeconds);

                HttpLogService.Platform httpPlatform;
                try {
                    httpPlatform = HttpLogService.Platform.valueOf(platform.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    httpPlatform = HttpLogService.Platform.INTERACTSH;
                }
                HttpLogService.setPlatform(httpPlatform);
            }

            if (oob.has(ConfigConstants.OOB_DNS)) {
                JsonObject dns = oob.getAsJsonObject(ConfigConstants.OOB_DNS);
                String dnsPlatform = dns.has(ConfigConstants.OOB_DNS_PLATFORM)
                        ? dns.get(ConfigConstants.OOB_DNS_PLATFORM).getAsString()
                        : DnsLogService.Platform.DNSLOG_CN.name();
                try {
                    DnsLogService.setPlatform(DnsLogService.Platform.valueOf(dnsPlatform.toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    DnsLogService.setPlatform(DnsLogService.Platform.DNSLOG_CN);
                }

                String ceyeIdentifier = dns.has(ConfigConstants.OOB_DNS_CEYE_IDENTIFIER)
                        ? dns.get(ConfigConstants.OOB_DNS_CEYE_IDENTIFIER).getAsString()
                        : null;
                String ceyeToken = dns.has(ConfigConstants.OOB_DNS_CEYE_TOKEN)
                        ? dns.get(ConfigConstants.OOB_DNS_CEYE_TOKEN).getAsString()
                        : null;
                if (ceyeIdentifier != null && !ceyeIdentifier.trim().isEmpty()) {
                    DnsLogService.configureCeye(ceyeIdentifier, ceyeToken);
                }
            }
        } catch (Exception e) {
            if (debugMode) {
                System.err.println("应用 OOB 配置失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
