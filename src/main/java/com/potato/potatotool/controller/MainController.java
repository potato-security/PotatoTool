package com.potato.potatotool.controller;

import com.potato.potatotool.ToStart;
import com.potato.potatotool.MainApplication;
import com.leewyatt.rxcontrols.animation.carousel.*;
import com.leewyatt.rxcontrols.controls.RXCarousel;
import com.leewyatt.rxcontrols.pane.RXCarouselPane;
import com.potato.potatotool.controller.publicPane.PaneAbout;
import com.potato.potatotool.controller.redTeam.PanePortScan;
import com.potato.potatotool.controller.redTeam.PaneVulScan;
import com.potato.potatotool.utils.ui.PaneFactory;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.core.ExecutorServiceManager;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.ui.DialogUtils;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.*;
import javafx.util.Duration;

import java.io.IOException;
import java.util.concurrent.Callable;

import static com.potato.potatotool.ToStart.isBlueMode;


/**
 * @author Potato
 * @date 2023/10/7 11:16
 */
public class MainController {

    @FXML
    private AnchorPane root;

    @FXML
    private Screen screen;

    @FXML
    private BorderPane topBar;

    @FXML
    private HBox topBarLeft;

    @FXML
    private FlowPane redBar;

    @FXML
    private FlowPane blueBar;

    @FXML
    private BorderPane borderPane;

    @FXML
    private ToggleGroup navGroup;

    @FXML
    private RXCarousel mainCarousel;

    @FXML
    private HBox changeBtn;
    @FXML
    private Button btnBlueMode;
    @FXML
    private Button btnRedMode;
    @FXML
    private Rectangle blueModePane;
    @FXML
    private Rectangle redModePane;

    @FXML
    private Button reduceScreen;
    @FXML
    private Button fullScreen;
    @FXML
    private ImageView fullScreenImgView;
    @FXML
    private StackPane promptPane;
    @FXML
    private Label promptLabel;
    @FXML
    private Label modeSectionLabel;
    @FXML
    private DropShadow wordmarkGlow;
    @FXML
    private Label proxyStatusLabel;

    private double offsetX,offsetY;

    private int selectedBlueIndex = 0;
    private int selectedRedIndex = 10;

    private boolean isFullScreen = false;
    private FadeTransition promptFadeIn;
    private FadeTransition promptFadeOut;
    private PaneVulScan vulScanController;
    private PanePortScan portScanController;

    /**
     * 在主界面多个子 FXML 串行加载之间主动让出 CPU，
     * 给 JavaFX UI 线程的 Pulse 渲染和启动页动画执行机会，避免长时间独占后台加载线程。
     *
     * - UI 线程直接返回，不引起反向卡顿
     * - 后台线程：通过 Thread.yield() 和微小的 sleep(2) 让出 CPU，降低掉帧几率
     */
    private static void yieldToUi() {
        if (Platform.isFxApplicationThread()) return;
        Thread.yield();
        try { Thread.sleep(2L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    @FXML
    void initialize() throws IOException {

//        root.setPrefWidth(screen.getVisualBounds().getWidth()* 0.8);
//        root.setPrefHeight(screen.getVisualBounds().getHeight()* 0.8);
//        root.setPrefWidth(2048);
//        root.setPrefWidth(1280);

        SimpleDoubleProperty arcProperty = new SimpleDoubleProperty(20.0);
        Rectangle clip = clipRect(
                borderPane, arcProperty
        );
        borderPane.setClip(clip);
        DropShadow dropShadow = new DropShadow();
        dropShadow.setColor(Color.BLACK);
        dropShadow.setRadius(10);
        dropShadow.setOffsetX(5);
        dropShadow.setOffsetY(5);
        borderPane.setEffect(dropShadow);

        // 内容列基准宽度 = 窗口宽 - 左侧栏(230) - 外边距(18)，各页据此按比例取宽（稳定值，避免与 carousel 自身宽度形成绑定环）
        DoubleProperty contentWidth = new SimpleDoubleProperty();
        contentWidth.bind(root.widthProperty().subtract(248));

        Pane p1 = FXMLLoader.load(getClass().getResource("/fxml/blueTeam/pane_webshellDecode.fxml"));
        RXCarouselPane webshellDecodePane = new RXCarouselPane(p1);
        p1.prefWidthProperty().bind(contentWidth.subtract(48));
        p1.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p2 = FXMLLoader.load(getClass().getResource("/fxml/blueTeam/pane_separateDecode.fxml"));
        RXCarouselPane separateDecodePane = new RXCarouselPane(p2);
        p2.prefWidthProperty().bind(contentWidth.subtract(48));
        p2.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p3 = FXMLLoader.load(getClass().getResource("/fxml/blueTeam/pane_getIpInfo.fxml"));
        RXCarouselPane ipInFoPane = new RXCarouselPane(p3);
        p3.prefWidthProperty().bind(contentWidth.subtract(48));
        p3.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p4 = FXMLLoader.load(getClass().getResource("/fxml/blueTeam/pane_aiAnswer.fxml"));
        RXCarouselPane aiAnswerPane = new RXCarouselPane(p4);
        p4.prefWidthProperty().bind(contentWidth.subtract(48));
        p4.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p5 = FXMLLoader.load(getClass().getResource("/fxml/blueTeam/pane_decompile.fxml"));
        RXCarouselPane decompilePane = new RXCarouselPane(p5);
        p5.prefWidthProperty().bind(contentWidth.subtract(48));
        p5.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p6 = FXMLLoader.load(getClass().getResource("/fxml/blueTeam/pane_blockchain.fxml"));
        RXCarouselPane blockchainPane = new RXCarouselPane(p6);
        p6.prefWidthProperty().bind(contentWidth.subtract(48));
        p6.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p7 = FXMLLoader.load(getClass().getResource("/fxml/blueTeam/pane_locationQuery.fxml"));
        RXCarouselPane locationQueryPane = new RXCarouselPane(p7);
        p7.prefWidthProperty().bind(contentWidth.subtract(48));
        p7.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p8 = FXMLLoader.load(getClass().getResource("/fxml/blueTeam/pane_exif.fxml"));
        RXCarouselPane exifPane = new RXCarouselPane(p8);
        p8.prefWidthProperty().bind(contentWidth.subtract(48));
        p8.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        // 使用工厂创建 Extension 面板（蓝色模式）
        RXCarouselPane extensionPane = PaneFactory.createExtensionPane(null, 0.95, mainCarousel.heightProperty(), contentWidth);
        yieldToUi();

        // 使用工厂创建 About 面板（蓝色模式）
        PaneFactory.AboutPaneResult aboutResult = PaneFactory.createAboutPane(null, 0.95, mainCarousel.heightProperty().subtract(20), contentWidth);
        RXCarouselPane aboutPane = aboutResult.pane;
        PaneAbout PaneAbout = aboutResult.controller;
        yieldToUi();

        FXMLLoader aiPentestLoader = new FXMLLoader(getClass().getResource("/fxml/redTeam/pane_aiPentest.fxml"));
        Pane pAiPentest = aiPentestLoader.load();
        RXCarouselPane aiPentestPane = new RXCarouselPane(pAiPentest);
        aiPentestPane.getStylesheets().add(Constants.getResourceUrl("/css/redStyle.css"));
        pAiPentest.prefWidthProperty().bind(contentWidth.subtract(48));
        pAiPentest.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p11 = FXMLLoader.load(getClass().getResource("/fxml/redTeam/pane_infoSearch.fxml"));
        RXCarouselPane infoSearchPane = new RXCarouselPane(p11);
        infoSearchPane.getStylesheets().add(Constants.getResourceUrl("/css/redStyle.css"));
        p11.prefWidthProperty().bind(contentWidth.subtract(48));
        p11.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        FXMLLoader portScanLoader = new FXMLLoader(getClass().getResource("/fxml/redTeam/pane_portScan.fxml"));
        Pane pPortScan = portScanLoader.load();
        portScanController = portScanLoader.getController();
        RXCarouselPane portScanPane = new RXCarouselPane(pPortScan);
        portScanPane.getStylesheets().add(Constants.getResourceUrl("/css/redStyle.css"));
        pPortScan.prefWidthProperty().bind(contentWidth.subtract(48));
        pPortScan.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        FXMLLoader vulScanLoader = new FXMLLoader(getClass().getResource("/fxml/redTeam/pane_vulScan.fxml"));
        Pane p12 = vulScanLoader.load();
        vulScanController = vulScanLoader.getController();
        RXCarouselPane vulScanPane = new RXCarouselPane(p12);
        vulScanPane.getStylesheets().add(Constants.getResourceUrl("/css/redStyle.css"));
        p12.prefWidthProperty().bind(contentWidth.subtract(48));
        p12.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p13 = FXMLLoader.load(getClass().getResource("/fxml/redTeam/pane_freeKill.fxml"));
        RXCarouselPane freeKillPane = new RXCarouselPane(p13);
        freeKillPane.getStylesheets().add(Constants.getResourceUrl("/css/redStyle.css"));
        p13.prefWidthProperty().bind(contentWidth.subtract(48));
        p13.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p14 = FXMLLoader.load(getClass().getResource("/fxml/redTeam/pane_customMemoryCode.fxml"));
        RXCarouselPane customMemoryCodePane = new RXCarouselPane(p14);
        customMemoryCodePane.getStylesheets().add(Constants.getResourceUrl("/css/redStyle.css"));
        p14.prefWidthProperty().bind(contentWidth.subtract(48));
        p14.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane pPayloadToolbox = FXMLLoader.load(getClass().getResource("/fxml/redTeam/pane_payloadToolbox.fxml"));
        RXCarouselPane payloadToolboxPane = new RXCarouselPane(pPayloadToolbox);
        payloadToolboxPane.getStylesheets().add(Constants.getResourceUrl("/css/redStyle.css"));
        pPayloadToolbox.prefWidthProperty().bind(contentWidth.subtract(48));
        pPayloadToolbox.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p15 = FXMLLoader.load(getClass().getResource("/fxml/redTeam/pane_customCommandGeneration.fxml"));
        RXCarouselPane customCommandGenerationPane = new RXCarouselPane(p15);
        customCommandGenerationPane.getStylesheets().add(Constants.getResourceUrl("/css/redStyle.css"));
        p15.prefWidthProperty().bind(contentWidth.subtract(48));
        p15.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p16 = FXMLLoader.load(getClass().getResource("/fxml/redTeam/pane_commandQuery.fxml"));
        RXCarouselPane commandQueryPane = new RXCarouselPane(p16);
        commandQueryPane.getStylesheets().add(Constants.getResourceUrl("/css/redStyle.css"));
        p16.prefWidthProperty().bind(contentWidth.subtract(48));
        p16.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p17 = FXMLLoader.load(getClass().getResource("/fxml/redTeam/pane_kbRootQuery.fxml"));
        RXCarouselPane kbRootQueryPane = new RXCarouselPane(p17);
        kbRootQueryPane.getStylesheets().add(Constants.getResourceUrl("/css/redStyle.css"));
        p17.prefWidthProperty().bind(contentWidth.subtract(48));
        p17.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p18 = FXMLLoader.load(getClass().getResource("/fxml/redTeam/pane_processQuery.fxml"));
        RXCarouselPane processQueryPane = new RXCarouselPane(p18);
        processQueryPane.getStylesheets().add(Constants.getResourceUrl("/css/redStyle.css"));
        p18.prefWidthProperty().bind(contentWidth.subtract(48));
        p18.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p19 = FXMLLoader.load(getClass().getResource("/fxml/redTeam/pane_infoGeneration.fxml"));
        RXCarouselPane infoGenerationPane = new RXCarouselPane(p19);
        infoGenerationPane.getStylesheets().add(Constants.getResourceUrl("/css/redStyle.css"));
        p19.prefWidthProperty().bind(contentWidth.subtract(48));
        p19.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        // 使用工厂创建 Extension 面板（红色模式）
        RXCarouselPane extensionPane_1 = PaneFactory.createExtensionPane("/css/redStyle.css", 0.95, mainCarousel.heightProperty(), contentWidth);
        yieldToUi();

        // 使用工厂创建 About 面板（红色模式）
        PaneFactory.AboutPaneResult aboutResult_1 = PaneFactory.createAboutPane("/css/redStyle.css", 0.95, mainCarousel.heightProperty().subtract(20), contentWidth);
        RXCarouselPane aboutPane_1 = aboutResult_1.pane;
        PaneAbout PaneAbout_1 = aboutResult_1.controller;
        yieldToUi();

        mainCarousel.setPaneList(webshellDecodePane, separateDecodePane, ipInFoPane, aiAnswerPane, decompilePane, blockchainPane, locationQueryPane, exifPane, extensionPane, aboutPane,
                aiPentestPane, infoSearchPane, portScanPane, vulScanPane, freeKillPane, customMemoryCodePane, payloadToolboxPane, customCommandGenerationPane, commandQueryPane, kbRootQueryPane, processQueryPane, infoGenerationPane, extensionPane_1, aboutPane_1);
        mainCarousel.setCarouselAnimation(new AnimNone());  // AnimFade
        mainCarousel.setAnimationTime(Duration.seconds(0));  // 0.2



        navGroup.selectedToggleProperty().addListener((ob, ov, nv) -> {
            if (nv == null) {
                return;
            }
            int index = navGroup.getToggles().indexOf(nv);
            applySelectedIndex(index, extensionPane, extensionPane_1, aboutPane, aboutPane_1, PaneAbout, PaneAbout_1);
        });

        // 左侧竖向导航：红/蓝两套导航在侧栏中通过显隐切换，不再随窗宽做横向滑动
        decorateNavDots();
        refreshProxyStatus();

        // 绑定国际化
        Platform.runLater(() -> {
            applyStartupTestPage(extensionPane, extensionPane_1, aboutPane, aboutPane_1, PaneAbout, PaneAbout_1);
            I18nUtils.bindComponents(root);
            String startupProxyWarning = MainApplication.consumeStartupProxyWarningMessage();
            if (startupProxyWarning != null && !startupProxyWarning.trim().isEmpty()) {
                showPrompt(startupProxyWarning, true);
            }
        });
    }

    private void showPrompt(String message, boolean keepVisible) {
        if (promptPane == null || promptLabel == null) {
            return;
        }
        promptLabel.setText(message);
        promptPane.toFront();
        promptPane.setOpacity(0);
        promptPane.setVisible(true);
        promptPane.setManaged(true);

        if (promptFadeIn == null) {
            promptFadeIn = new FadeTransition(Duration.seconds(0.2), promptPane);
            promptFadeIn.setFromValue(0);
            promptFadeIn.setToValue(1);
        }
        if (promptFadeOut == null) {
            promptFadeOut = new FadeTransition(Duration.seconds(0.2), promptPane);
            promptFadeOut.setFromValue(1);
            promptFadeOut.setToValue(0);
            promptFadeOut.setDelay(Duration.seconds(3));
            promptFadeOut.setOnFinished(event -> {
                promptPane.setVisible(false);
                promptPane.setManaged(false);
            });
        }

        promptFadeIn.stop();
        promptFadeOut.stop();
        promptFadeIn.setOnFinished(event -> {
            if (!keepVisible) {
                promptFadeOut.playFromStart();
            }
        });
        promptFadeIn.playFromStart();
    }

    @FXML
    public void closePromptPane(MouseEvent event) {
        if (promptFadeIn != null) {
            promptFadeIn.stop();
        }
        if (promptFadeOut != null) {
            promptFadeOut.stop();
        }
        if (promptPane != null) {
            promptPane.setVisible(false);
            promptPane.setManaged(false);
        }
    }

    public PaneVulScan getVulScanPane() {
        return vulScanController;
    }

    public PanePortScan getPortScanPane() {
        return portScanController;
    }

    public void selectNavIndex(int index) {
        if (index < 0 || index >= navGroup.getToggles().size()) {
            return;
        }
        navGroup.getToggles().get(index).setSelected(true);
    }

    private void applySelectedIndex(int index, RXCarouselPane extensionPane, RXCarouselPane extensionPane_1,
                                    RXCarouselPane aboutPane, RXCarouselPane aboutPane_1,
                                    PaneAbout paneAbout, PaneAbout paneAbout_1) {
        if (index < 0 || index >= mainCarousel.getPaneList().size()) {
            return;
        }

        mainCarousel.setSelectedIndex(index);

        // 懒同步：当切换到 Extension 面板时才真正同步数据
        RXCarouselPane currentPane = mainCarousel.getPaneList().get(index);
        if(currentPane == extensionPane || currentPane == extensionPane_1) {
            // 懒同步机制：只在切换到面板时才同步
            // 如果有待更新的数据，这时才会真正执行同步
            // 注：实际同步在 PaneExtension.initialize() 中已经处理
        }

        // 点击关于界面时 触发滚动信息
        if(currentPane == aboutPane || currentPane == aboutPane_1){
            // 启动当前面板的滚动，停止其他面板的滚动
            PaneFactory.stopAllAboutScrolling();
            if(currentPane == aboutPane){
                paneAbout.startScrolling();
            } else {
                paneAbout_1.startScrolling();
            }
        } else {
            // 停止所有关于面板的滚动
            PaneFactory.stopAllAboutScrolling();
        }

        if (isBlueMode) {
            selectedBlueIndex = index;
        } else {
            selectedRedIndex = index;
        }
    }

    private void applyStartupTestPage(RXCarouselPane extensionPane, RXCarouselPane extensionPane_1,
                                      RXCarouselPane aboutPane, RXCarouselPane aboutPane_1,
                                      PaneAbout paneAbout, PaneAbout paneAbout_1) {
        ToStart.StartupPage startupPage = ToStart.getStartupTestPage();
        if (startupPage == null) {
            return;
        }

        applyModeState(startupPage.isBlueMode());

        int index = startupPage.getNavIndex();
        if (index < 0 || index >= navGroup.getToggles().size()) {
            return;
        }

        Toggle targetToggle = navGroup.getToggles().get(index);
        if (navGroup.getSelectedToggle() != targetToggle) {
            targetToggle.setSelected(true);
        } else {
            applySelectedIndex(index, extensionPane, extensionPane_1, aboutPane, aboutPane_1, paneAbout, paneAbout_1);
        }
    }

    private void applyModeState(boolean blueMode) {
        isBlueMode = blueMode;
        isChangeModePaneRight = !blueMode;

        topBar.getStyleClass().removeAll("blueStyle", "redStyle");
        if (changeBtn != null) changeBtn.getStyleClass().removeAll("blueStyle", "redStyle");
        borderPane.getStyleClass().removeAll("blueStyle", "redStyle");

        // 头部 mode pill 激活态
        if (btnBlueMode != null) {
            btnBlueMode.getStyleClass().removeAll("mode-pill-active");
            if (blueMode) btnBlueMode.getStyleClass().add("mode-pill-active");
        }
        if (btnRedMode != null) {
            btnRedMode.getStyleClass().removeAll("mode-pill-active");
            if (!blueMode) btnRedMode.getStyleClass().add("mode-pill-active");
        }

        // 竖向侧栏：红/蓝导航通过显隐切换
        setBarVisible(blueBar, blueMode);
        setBarVisible(redBar, !blueMode);
        updateModeSectionLabel(blueMode);

        if (blueMode) {
            topBar.getStyleClass().add("blueStyle");
            if (changeBtn != null) changeBtn.getStyleClass().add("blueStyle");
        } else {
            topBar.getStyleClass().add("redStyle");
            if (changeBtn != null) changeBtn.getStyleClass().add("redStyle");
            borderPane.getStyleClass().add("redStyle");
        }
    }

    /** 头部代理状态胶囊：读取主代理开关，显示 PROXY ON / OFF */
    public void refreshProxyStatus() {
        if (proxyStatusLabel == null) return;
        boolean on = com.potato.potatotool.utils.network.ProxyUtils.isMainProxyEnabled();
        proxyStatusLabel.setText(on ? "ON" : "OFF");
        proxyStatusLabel.getStyleClass().remove("status-pill-on");
        if (on) proxyStatusLabel.getStyleClass().add("status-pill-on");
    }

    /** 给每个导航条目加左侧圆点指示（对齐 Penpot 侧栏：未选中空心、选中实心，由 CSS 控制） */
    private void decorateNavDots() {
        if (navGroup == null) return;
        for (Toggle t : navGroup.getToggles()) {
            if (t instanceof ToggleButton) {
                ToggleButton tb = (ToggleButton) t;
                Region dot = new Region();
                dot.getStyleClass().add("nav-dot");
                tb.setGraphic(dot);
                tb.setContentDisplay(ContentDisplay.LEFT);
                tb.setGraphicTextGap(10);
            }
        }
    }

    private void setBarVisible(FlowPane bar, boolean show) {
        if (bar == null) return;
        bar.setVisible(show);
        bar.setManaged(show);
        bar.setTranslateX(0);
    }

    private void updateModeSectionLabel(boolean blueMode) {
        if (modeSectionLabel != null) {
            modeSectionLabel.setText(blueMode ? "BLUE OPERATIONS" : "RED OPERATIONS");
            modeSectionLabel.getStyleClass().removeAll("side-nav-section-blue", "side-nav-section-red");
            modeSectionLabel.getStyleClass().add(blueMode ? "side-nav-section-blue" : "side-nav-section-red");
        }
        if (wordmarkGlow != null) {
            wordmarkGlow.setColor(Color.web(blueMode ? "#4FE3FF" : "#FF5C66"));
        }
    }

    private boolean isChangeModePaneRight = false;  // 是否处于红队模式（保留供其他调用方兼容）
    @FXML
    void changeModeBlue() {
        applyModeState(true);
        navGroup.getToggles().get(selectedBlueIndex).setSelected(true);
    }
    @FXML
    void changeModeRed() {
        applyModeState(false);
        navGroup.getToggles().get(selectedRedIndex).setSelected(true);
    }
    /** 兼容旧调用（已废弃，不再使用） */
    void changeMode() {
        if (isChangeModePaneRight) changeModeBlue(); else changeModeRed();
    }

    @FXML
    void showSet(ActionEvent event) {
        DialogUtils.showSet(root.getScene().getWindow());
    }

    @FXML
    void exitAction(ActionEvent event) {
        ExecutorServiceManager.shutdownAll();
        Platform.exit();
        System.exit(0);
    }

    @FXML
    void topBarDraggedAction(MouseEvent event) {
        Window window = topBar.getScene().getWindow();
        window.setX(event.getScreenX()-offsetX);
        window.setY(event.getScreenY()-offsetY);
    }

    @FXML
    void topBarPressedAction(MouseEvent event) {
        offsetX = event.getSceneX();
        offsetY = event.getSceneY();
    }

    public static Rectangle clipRect(Node node, DoubleProperty bindArc){
        Rectangle rectangle = new Rectangle();
        rectangle.widthProperty().bind(Bindings.createObjectBinding((Callable<Number>) () ->
                node.getLayoutBounds().getWidth(), node.layoutBoundsProperty()));
        rectangle.heightProperty().bind(Bindings.createObjectBinding((Callable<Number>) () ->
                node.getLayoutBounds().getHeight(), node.layoutBoundsProperty()));
        rectangle.arcWidthProperty().bind(bindArc);
        rectangle.arcHeightProperty().bind(bindArc);
        node.setClip(rectangle);
        return rectangle;
    }

    @FXML
    void fullScreenAction(ActionEvent event) {
        isFullScreen = !isFullScreen;
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        reduceScreen.setDisable(isFullScreen);
        fullScreen.setId( isFullScreen ? "selectedColor" : "btnExit");
        fullScreenImgView.setImage(new Image(getClass().getResourceAsStream(isFullScreen ? "/img/nofullScreen.png" : "/img/fullScreen.png")));


        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("mac")){
            Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
            if (isFullScreen){
                Platform.runLater(() -> {
                    stage.setX(primaryScreenBounds.getMinX());
                    stage.setY(primaryScreenBounds.getMinY());
                    stage.setWidth(primaryScreenBounds.getWidth());
                    stage.setHeight(primaryScreenBounds.getHeight());
                });
            }else {
                Platform.runLater(() -> {
                    stage.setX(primaryScreenBounds.getWidth() * 0.1);
                    stage.setY(primaryScreenBounds.getHeight() * 0.1);
                    stage.setWidth(primaryScreenBounds.getWidth() * 0.8);
                    stage.setHeight(primaryScreenBounds.getHeight() * 0.8);
                });
            }
        }else {
            Platform.runLater(() -> {
                stage.setMaximized(isFullScreen);
            });
        }
    }

    @FXML
    void reduceScreenAction(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setIconified(true);
    }

}
