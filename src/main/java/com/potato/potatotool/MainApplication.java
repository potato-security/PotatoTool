package com.potato.potatotool;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.controller.MainController;
import com.potato.potatotool.controller.publicPane.PaneLoad;
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
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.core.Constants.*;

public class MainApplication extends Application {
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final double SPLASH_BACKGROUND_INIT_DELAY_MS = 350;
    private static final double SPLASH_MAIN_SCENE_FALLBACK_DELAY_MS = 6500;
    /**
     * [BOOT-L2 第 2 层] 主界面加载专用低优先级单线程池。
     * 关键：setPriority(Thread.MIN_PRIORITY) → OS 调度永远优先 JavaFX UI 线程，
     * 即使主界面 20+ FXML 同步加载吃满 CPU，也不会让启动动画 Pulse 掉帧。
     * 单线程是因为 main.fxml 的加载本身串行（MainController.initialize 内串行 load 子 FXML），
     * 不需要并发；并发反而会和 UI 线程抢更多核。
     */
    private static final ExecutorService mainSceneLoader = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "potato-main-loader");
            t.setPriority(Thread.MIN_PRIORITY);
            t.setDaemon(true);
            return t;
        }
    });
    private static volatile String startupProxyWarningMessage;
    private static volatile MainController mainController;

    // ============================================================
    // [启动期更新对话框延后展示]
    //   原行为：checkForUpdatesOnStartup 一旦发现更新就立刻 Platform.runLater 弹窗，
    //          可能落在 PaneLoad 5s 启动动画期间，或主舞台 stage.show() 后 fadeIn 还没起来时，
    //          视觉上是"启动页/黑屏 + 更新弹窗抢焦点"。
    //   现在：发现更新先写入 pendingStartupUpdateInfo，等到主界面真正进入 fadeIn 时
    //        （playSplashFadeOut 触发主舞台 fadeIn / showMainStage 触发 fadeIn）
    //        再异步消费，保证：
    //          1) 与主界面同时激活展示；
    //          2) 通过 Platform.runLater 排到下一个 Pulse，不阻塞 fadeIn 关键帧。
    // ============================================================
    private static volatile UpdateInfo pendingStartupUpdateInfo;
    private static volatile boolean mainStagePresentedForStartupUpdate = false;
    private static final Object startupUpdateLock = new Object();

    @Override
    public void start(Stage stage) throws IOException {
        ClassLoader appClassLoader = MainApplication.class.getClassLoader();
        Thread.currentThread().setContextClassLoader(appClassLoader);
        FXMLLoader.setDefaultClassLoader(appClassLoader);
        hostServices = getHostServices();
        // [F1] 启动页独立预览模式：仅渲染 PaneLoad，跳过所有主界面初始化，动画结束即退出 JVM
        if (ToStart.isLoadPagePreviewMode()) {
            showLoadPagePreviewOnly(stage);
            return;
        }
        final boolean skipStartupPages = ToStart.isStartupPageTestEnabled();
        final String effectiveBootMode = resolveBootMode();
        // useSplash = true 表示启用 PaneLoad；skipStartupPages / off 模式跳过 splash，原同步路径
        final boolean useSplash = !skipStartupPages && !"off".equals(effectiveBootMode);

        // 设置主窗口图标（原本在密码页/skip 两个分支各做一次，密码门取消后在此统一设置）
        {
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

        // ============================================================
        // [密码门已废弃 2026-05-25]
        //   passwd 是硬编码 "potato520"（见 PanePasswd.start），对桌面安全工具无实际防护意义；
        //   取消密码门后启动页 PaneLoad 成为用户第一接触点，节省一次点击。
        //   原代码已整段删除，如需恢复请从 git 历史回溯（2026-05-25 之前版本）。
        // ============================================================

        // 共享状态：mainSceneReady = main.fxml 加载完成且 setScene 成功；mainStageShown = stage.show() 已执行
        // 用 AtomicBoolean 而不是 BooleanProperty，因为 setter 不依赖 JavaFX 线程
        final AtomicReference<FadeTransition> fadeTransition1 = new AtomicReference<FadeTransition>();
        final AtomicBoolean mainSceneReady = new AtomicBoolean(false);
        final AtomicBoolean mainStageShown = new AtomicBoolean(false);
        final AtomicBoolean mainSceneLoadSubmitted = new AtomicBoolean(false);
        final AtomicReference<Stage> loadStageRef = new AtomicReference<Stage>();
        final AtomicReference<PaneLoad> loadCtrlRef = new AtomicReference<PaneLoad>();

        // ============================================================
        // 启动顺序倒置：在 start() 入口立即创建并 show PaneLoad，
        // 不等任何后台任务，让用户在 100ms 内看到启动动画首帧。
        // ============================================================
        if (useSplash) {
            try {
                Stage loadStage = new Stage();
                loadStage.initStyle(StageStyle.TRANSPARENT);
                loadStage.setAlwaysOnTop(true);
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/publicPane/load.fxml"));
                Scene loadScene = new Scene(loader.load());
                loadScene.setCamera(new PerspectiveCamera());
                loadScene.setFill(null);
                loadStage.setScene(loadScene);
                try {
                    InputStream iconStream = getClass().getResourceAsStream("/img/logo.png");
                    if (iconStream != null) {
                        Image image = new Image(iconStream);
                        loadStage.getIcons().add(image);
                        loadStage.setTitle("PotatoTool");
                    }
                } catch (Exception ignored) {}
                PaneLoad loadController = loader.getController();
                loadController.setBootMode(effectiveBootMode);
                loadStage.show();
                loadStageRef.set(loadStage);
                loadCtrlRef.set(loadController);
            } catch (Exception e) {
                if (debugMode) e.printStackTrace();
            }
        }

        // ============================================================
        // [BOOT-L6 第 6 层] 正常 splash 模式下不再提前 show 主舞台。
        //                  main.fxml 延后到 PaneLoad loadedProperty=true 后再加载，
        //                  避免启动动画期间被主界面 FXML/CSS/Controller 初始化抢占 Pulse。
        // ============================================================
        final Runnable maybeShowMainStage = new Runnable() {
            @Override public void run() {
                if (mainStageShown.get()) return;
                if (!mainSceneReady.get()) return;
                if (useSplash) {
                    PaneLoad pl = loadCtrlRef.get();
                    if (pl != null && !pl.loadedProperty().get()) return;
                }
                Runnable showAction = new Runnable() {
                    @Override public void run() {
                        if (mainStageShown.get()) return;
                        try {
                            stage.show();
                            mainStageShown.set(true);
                        } catch (Exception e) {
                            if (debugMode) e.printStackTrace();
                        }
                    }
                };
                if (Platform.isFxApplicationThread()) showAction.run();
                else Platform.runLater(showAction);
            }
        };

        if (useSplash) {
            final PaneLoad pl = loadCtrlRef.get();
            if (pl != null) {
                // PaneLoad loadedProperty 触发 = 启动动画结束（full +5000ms / minimal +2200ms），需要切场
                pl.loadedProperty().addListener(new ChangeListener<Boolean>() {
                    @Override public void changed(ObservableValue<? extends Boolean> obs, Boolean ov, Boolean nv) {
                        if (!Boolean.TRUE.equals(nv)) return;
                        Runnable swap = new Runnable() {
                            @Override public void run() {
                                // 兜底 1：主舞台 scene 还没就绪（极慢机器 / L3 性能降级提前触发）→ 等
                                if (!mainSceneReady.get()) {
                                    return;
                                }
                                // 兜底 2：强制 show（即使 maybeShowMainStage 路径错过也保底）
                                if (!mainStageShown.get()) {
                                    try {
                                        stage.show();
                                        mainStageShown.set(true);
                                    } catch (Exception ex) {
                                        if (debugMode) ex.printStackTrace();
                                    }
                                }
                                playSplashFadeOut(loadStageRef.get(), loadCtrlRef.get(), fadeTransition1);
                            }
                        };
                        if (Platform.isFxApplicationThread()) swap.run();
                        else Platform.runLater(swap);
                    }
                });
            }
        }

        // 初始化配置 / 网络 / 更新检查任务（IO/网络任务，与 UI 线程关系不大，沿用 cached pool）
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

        // scene 完成后的回调：标记 mainSceneReady=true，并按情况触发 show / 切场
        final Runnable onSceneAttached = new Runnable() {
            @Override public void run() {
                mainSceneReady.set(true);
                if (!useSplash) {
                    // skipStartupPages / off 模式：原行为 - 直接 show 主舞台
                    if (!mainStageShown.get()) {
                        showMainStage(stage, fadeTransition1);
                        mainStageShown.set(true);
                    }
                    return;
                }
                PaneLoad pl = loadCtrlRef.get();
                // 兜底 3：极快机器场景 - PaneLoad 动画在 mainSceneReady 之前就到了结束 / 闸门已开
                if (pl != null && pl.loadedProperty().get()) {
                    if (!mainStageShown.get()) {
                        try {
                            stage.show();
                            mainStageShown.set(true);
                        } catch (Exception ex) {
                            if (debugMode) ex.printStackTrace();
                        }
                    }
                    playSplashFadeOut(loadStageRef.get(), pl, fadeTransition1);
                } else {
                    // 正常路径：主舞台等待 PaneLoad 完成后再展示
                    maybeShowMainStage.run();
                }
            }
        };

        // UI 线程操作分帧：原 task 里一个大 runLater 拆成 4 步嵌套 runLater，
        // 每步落在不同 Pulse，避免单帧承担全部 stage.setScene + CSS apply 开销
        // 主界面加载任务
        Task<Void> taskMain = new Task<Void>() {
            @Override
            protected Void call() throws IOException {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
                final Scene scene = new Scene(fxmlLoader.load());
                mainController = fxmlLoader.getController();
                applySceneToStageInSteps(stage, scene, fadeTransition1, onSceneAttached);
                return null;
            }
        };
        taskMain.setOnFailed(e -> {
            ExecutorServiceManager.shutdownAll();
            Throwable error = taskMain.getException();
            error.printStackTrace();
            System.exit(0);
        });

        final Runnable submitMainSceneLoad = new Runnable() {
            @Override public void run() {
                if (!mainSceneLoadSubmitted.compareAndSet(false, true)) return;
                PaneLoad pl = loadCtrlRef.get();
                if (pl != null) pl.enterMainLoadHold();
                // 主界面 FXML/CSS/Controller 构建延后到 PaneLoad 完成后，避免正常启动时抢占启动动画的 Pulse。
                mainSceneLoader.submit(taskMain);
            }
        };

        if (useSplash) {
            // 配置、代理、更新检查仍可后台进行；主界面树构建延后，保证 0~5s 动画窗口稳定。
            PauseTransition initDelay = new PauseTransition(Duration.millis(SPLASH_BACKGROUND_INIT_DELAY_MS));
            initDelay.setOnFinished(e -> {
                executor.submit(taskInit);
            });
            initDelay.play();

            final PaneLoad pl = loadCtrlRef.get();
            if (pl != null) {
                pl.loadedProperty().addListener(new ChangeListener<Boolean>() {
                    @Override public void changed(ObservableValue<? extends Boolean> obs, Boolean oldValue, Boolean newValue) {
                        if (Boolean.TRUE.equals(newValue)) {
                            submitMainSceneLoad.run();
                        }
                    }
                });
                // 极端异常兜底：如果 PaneLoad 信号没有触发，也不要让主程序永远停在启动页。
                PauseTransition mainLoadFallback = new PauseTransition(Duration.millis(SPLASH_MAIN_SCENE_FALLBACK_DELAY_MS));
                mainLoadFallback.setOnFinished(e -> {
                    if (!pl.loadedProperty().get()) pl.forceCompleteForStartupFallback();
                    submitMainSceneLoad.run();
                });
                mainLoadFallback.play();
            } else {
                submitMainSceneLoad.run();
            }
        } else {
            // off / skipStartupPages 模式：原行为，无 splash，立即开始
            executor.submit(taskInit);
            submitMainSceneLoad.run();
        }
    }

    /**
     * UI 线程操作分帧执行：把原本一个大 Platform.runLater 中的操作
     * 拆成 4 个嵌套 runLater，每步落在不同的 Pulse，避免单帧承担全部 CSS apply + 整树首次 layout 开销。
     *
     * step1: 基础属性设置
     * step2: 样式表注册
     * step3: 窗口透明度、样式和 Scene 绑定（触发 heaviest CSS apply）
     * step4: 淡入动画创建并触发回调
     */
    private void applySceneToStageInSteps(final Stage stage, final Scene scene,
                                          final AtomicReference<FadeTransition> fadeTransition1,
                                          final Runnable onComplete) {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                try {
                    stage.setFullScreenExitHint("");
                    scene.setCamera(new PerspectiveCamera());
                    scene.setFill(null);
                } catch (Exception e) { if (debugMode) e.printStackTrace(); }
                Platform.runLater(new Runnable() {
                    @Override public void run() {
                        try {
                            scene.getStylesheets().add(Constants.getResourceUrl("/css/theme.css"));
                            scene.getStylesheets().add(Constants.getResourceUrl("/css/common.css"));
                            scene.getStylesheets().add(Constants.getResourceUrl("/css/components.css"));
                        } catch (Exception e) { if (debugMode) e.printStackTrace(); }
                        Platform.runLater(new Runnable() {
                            @Override public void run() {
                                try {
                                    stage.initStyle(StageStyle.TRANSPARENT);
                                    scene.getRoot().getStyleClass().add("blueStyle");
                                    scene.getRoot().setOpacity(0);
                                    stage.setScene(scene);
                                    applyStartupTestWindowSize(stage);
                                } catch (Exception e) { if (debugMode) e.printStackTrace(); }
                                Platform.runLater(new Runnable() {
                                    @Override public void run() {
                                        try {
                                            FadeTransition ft = new FadeTransition(Duration.seconds(0.3), scene.getRoot());
                                            ft.setFromValue(0);
                                            ft.setToValue(1);
                                            ft.setCycleCount(1);
                                            fadeTransition1.set(ft);
                                            if (onComplete != null) onComplete.run();
                                        } catch (Exception e) { if (debugMode) e.printStackTrace(); }
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    private void applyStartupTestWindowSize(Stage stage) {
        ToStart.TestWindowSize windowSize = ToStart.getStartupTestWindowSize();
        if (windowSize == null) {
            return;
        }
        stage.setWidth(windowSize.getWidth());
        stage.setHeight(windowSize.getHeight());
        stage.centerOnScreen();
    }

    /**
     * 启动页淡出 + 主舞台淡入。
     * 顺序：PaneLoad fadeOut(300ms) -> close loadStage -> controller.stopAnimations() -> 主舞台 fadeIn(300ms)
     */
    private void playSplashFadeOut(final Stage loadStage, final PaneLoad controller,
                                   final AtomicReference<FadeTransition> fadeTransition1) {
        if (loadStage == null || controller == null) return;
        Scene loadScene = loadStage.getScene();
        if (loadScene == null) return;
        FadeTransition fadeTransition = new FadeTransition(Duration.seconds(0.3), loadScene.getRoot());
        fadeTransition.setFromValue(1);
        fadeTransition.setToValue(0);
        fadeTransition.setCycleCount(1);
        fadeTransition.setOnFinished(event -> {
            loadStage.close();
            controller.stopAnimations();
            FadeTransition ft = fadeTransition1.get();
            if (ft != null) ft.play();
            // 主舞台 fadeIn 已经在 UI 线程触发，可以释放启动期暂存的更新对话框。
            // onMainStagePresentedForStartupUpdate 内部用 Platform.runLater 异步排到下一帧，
            // 不阻塞当前 fadeIn 的关键帧。
            onMainStagePresentedForStartupUpdate();
        });
        fadeTransition.play();
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
        // 非 splash 路径（off / skipStartupPages 模式）此处即"主界面激活展示"时机，
        // 同样消费启动期暂存的更新对话框，保证两端路径行为一致。
        onMainStagePresentedForStartupUpdate();
    }

    /**
     * 启动页独立预览：仅渲染 PaneLoad，跳过所有主界面初始化，动画结束即退出 JVM。
     * 不切换主舞台、不进行渐变淡入。
     */
    private void showLoadPagePreviewOnly(Stage stage) {
        try {
            Stage loadStage = new Stage();
            loadStage.initStyle(StageStyle.TRANSPARENT);
            loadStage.setAlwaysOnTop(true);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/publicPane/load.fxml"));
            Scene loadScene = new Scene(loader.load());
            loadScene.setCamera(new PerspectiveCamera());
            loadScene.setFill(null);
            loadStage.setScene(loadScene);
            try {
                InputStream iconStream = getClass().getResourceAsStream("/img/logo.png");
                if (iconStream != null) {
                    Image image = new Image(iconStream);
                    loadStage.getIcons().add(image);
                    loadStage.setTitle("PotatoTool · LoadPage Preview");
                }
            } catch (Exception ignored) {}
            loadStage.show();

            PaneLoad controller = loader.getController();
            String previewMode = resolveBootMode();
            if ("off".equals(previewMode)) previewMode = "full";
            controller.setBootMode(previewMode);

            FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.3), loadScene.getRoot());
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setCycleCount(1);
            fadeOut.setOnFinished(event -> {
                loadStage.close();
                controller.stopAnimations();
                Platform.exit();
                System.exit(0);
            });
            controller.loadedProperty().addListener((obs, oldValue, newValue) -> fadeOut.play());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 解析有效启动动画模式：full / minimal / off，默认 full。
     * 仅读取用户主动设定的 BOOT_ANIMATION 配置；任何异常都安全回退到 full。
     * 不做基于历史启动次数或时间间隔的自动降级。
     */
    private String resolveBootMode() {
        String mode = "full";
        try {
            Object modeObj = Constants.getOutsideConfig(ConfigConstants.BOOT_ANIMATION);
            if (modeObj instanceof JsonElement) {
                JsonElement el = (JsonElement) modeObj;
                if (el.isJsonPrimitive()) {
                    String v = el.getAsString();
                    if ("minimal".equals(v) || "off".equals(v) || "full".equals(v)) {
                        mode = v;
                    }
                }
            }
        } catch (Exception e) {
            if (debugMode) e.printStackTrace();
        }
        return mode;
    }

    // [BOOT-L1/L6 重构] 旧 loadMainStage
    //   1. start() 入口直接 show PaneLoad（不再等密码页或 preload）
    //   2. applySceneToStageInSteps：UI 线程操 主舞台淡入
    // 历史代码请从 git 回溯 2026-05-25 之前版本。


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
                    
                    // 不再立刻弹窗：缓存到 pendingStartupUpdateInfo，等主界面 fadeIn 触发时
                    // 由 onMainStagePresentedForStartupUpdate 异步消费，
                    // 避免在 PaneLoad 启动动画期间或主舞台 fadeIn 之前抢焦点。
                    deferStartupUpdateDialog(updateInfo);
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
     * 把启动期检测到的更新信息延后到主界面 fadeIn 触发时再展示。
     *   - 主界面尚未激活：写入 pendingStartupUpdateInfo，等待 onMainStagePresentedForStartupUpdate 消费；
     *   - 主界面已激活（例如检查更新比 fadeIn 慢）：直接通过 Platform.runLater 异步展示。
     * 全程非阻塞：调用方（taskInit 后台线程）无需等待。
     */
    private void deferStartupUpdateDialog(UpdateInfo updateInfo) {
        if (updateInfo == null) return;
        boolean showNow = false;
        synchronized (startupUpdateLock) {
            if (mainStagePresentedForStartupUpdate) {
                showNow = true;
            } else {
                pendingStartupUpdateInfo = updateInfo;
            }
        }
        if (showNow) {
            final UpdateInfo info = updateInfo;
            Platform.runLater(() -> showUpdateDialogOnStartup(info));
        }
    }

    /**
     * 主界面 fadeIn 触发时调用，消费 pendingStartupUpdateInfo。
     * 必须保持非阻塞：弹窗本身通过 Platform.runLater 排到下一个 Pulse，
     * 让主界面 fadeIn 的关键帧先排出去，避免对话框 FXML.load + CSS apply 吃掉首帧。
     * 重入安全：mainStagePresentedForStartupUpdate 一旦为 true，后续调用立即返回，
     * 不会因 maybeShowMainStage / playSplashFadeOut / showMainStage 多路径触发而重复弹窗。
     */
    private void onMainStagePresentedForStartupUpdate() {
        UpdateInfo pending;
        synchronized (startupUpdateLock) {
            if (mainStagePresentedForStartupUpdate) return;
            mainStagePresentedForStartupUpdate = true;
            pending = pendingStartupUpdateInfo;
            pendingStartupUpdateInfo = null;
        }
        if (pending != null) {
            final UpdateInfo info = pending;
            Platform.runLater(() -> showUpdateDialogOnStartup(info));
        }
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
            scene.getStylesheets().add(Constants.getResourceUrl("/css/theme.css"));
            scene.getStylesheets().add(Constants.getResourceUrl("/css/common.css"));
            scene.getStylesheets().add(Constants.getResourceUrl("/css/components.css"));

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
        launch(args);
    }

    public static MainController getMainController() {
        return mainController;
    }
}
