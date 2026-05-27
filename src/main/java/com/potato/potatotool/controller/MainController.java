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
    private BorderPane changeBtn;
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
        borderPane.setStyle("-fx-background-color: lightblue;");
        DropShadow dropShadow = new DropShadow();
        dropShadow.setColor(Color.BLACK);
        dropShadow.setRadius(10);
        dropShadow.setOffsetX(5);
        dropShadow.setOffsetY(5);
        borderPane.setEffect(dropShadow);

        Pane p1 = FXMLLoader.load(getClass().getResource("/fxml/blueTeam/pane_webshellDecode.fxml"));
        RXCarouselPane webshellDecodePane = new RXCarouselPane(p1);
        p1.prefWidthProperty().bind(topBar.widthProperty().multiply(0.64));
        p1.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p2 = FXMLLoader.load(getClass().getResource("/fxml/blueTeam/pane_separateDecode.fxml"));
        RXCarouselPane separateDecodePane = new RXCarouselPane(p2);
        p2.prefWidthProperty().bind(topBar.widthProperty().multiply(0.7));
        p2.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p3 = FXMLLoader.load(getClass().getResource("/fxml/blueTeam/pane_getIpInfo.fxml"));
        RXCarouselPane ipInFoPane = new RXCarouselPane(p3);
        p3.prefWidthProperty().bind(topBar.widthProperty().multiply(0.7));
        p3.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p4 = FXMLLoader.load(getClass().getResource("/fxml/blueTeam/pane_aiAnswer.fxml"));
        RXCarouselPane aiAnswerPane = new RXCarouselPane(p4);
        p4.prefWidthProperty().bind(topBar.widthProperty().multiply(0.7));
        p4.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p5 = FXMLLoader.load(getClass().getResource("/fxml/blueTeam/pane_decompile.fxml"));
        RXCarouselPane decompilePane = new RXCarouselPane(p5);
        p5.prefWidthProperty().bind(topBar.widthProperty().multiply(0.7));
        p5.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p6 = FXMLLoader.load(getClass().getResource("/fxml/blueTeam/pane_blockchain.fxml"));
        RXCarouselPane blockchainPane = new RXCarouselPane(p6);
        p6.prefWidthProperty().bind(topBar.widthProperty().multiply(0.64));
        p6.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p7 = FXMLLoader.load(getClass().getResource("/fxml/blueTeam/pane_locationQuery.fxml"));
        RXCarouselPane locationQueryPane = new RXCarouselPane(p7);
        p7.prefWidthProperty().bind(topBar.widthProperty().multiply(0.7));
        p7.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p8 = FXMLLoader.load(getClass().getResource("/fxml/blueTeam/pane_exif.fxml"));
        RXCarouselPane exifPane = new RXCarouselPane(p8);
        p8.prefWidthProperty().bind(topBar.widthProperty().multiply(0.7));
        p8.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        // 使用工厂创建 Extension 面板（蓝色模式）
        RXCarouselPane extensionPane = PaneFactory.createExtensionPane(null, 0.8, mainCarousel.heightProperty(), topBar.widthProperty());
        yieldToUi();

        // 使用工厂创建 About 面板（蓝色模式）
        PaneFactory.AboutPaneResult aboutResult = PaneFactory.createAboutPane(null, 0.7, mainCarousel.heightProperty().subtract(20), topBar.widthProperty());
        RXCarouselPane aboutPane = aboutResult.pane;
        PaneAbout PaneAbout = aboutResult.controller;
        yieldToUi();

        Pane p11 = FXMLLoader.load(getClass().getResource("/fxml/redTeam/pane_infoSearch.fxml"));
        RXCarouselPane infoSearchPane = new RXCarouselPane(p11);
        infoSearchPane.getStylesheets().add(Constants.getResourceUrl("/css/redStyle.css"));
        p11.prefWidthProperty().bind(topBar.widthProperty().multiply(0.7));
        p11.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        FXMLLoader portScanLoader = new FXMLLoader(getClass().getResource("/fxml/redTeam/pane_portScan.fxml"));
        Pane pPortScan = portScanLoader.load();
        portScanController = portScanLoader.getController();
        RXCarouselPane portScanPane = new RXCarouselPane(pPortScan);
        portScanPane.getStylesheets().add(Constants.getResourceUrl("/css/redStyle.css"));
        pPortScan.prefWidthProperty().bind(topBar.widthProperty().multiply(0.7));
        pPortScan.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        FXMLLoader vulScanLoader = new FXMLLoader(getClass().getResource("/fxml/redTeam/pane_vulScan.fxml"));
        Pane p12 = vulScanLoader.load();
        vulScanController = vulScanLoader.getController();
        RXCarouselPane vulScanPane = new RXCarouselPane(p12);
        vulScanPane.getStylesheets().add(Constants.getResourceUrl("/css/redStyle.css"));
        p12.prefWidthProperty().bind(topBar.widthProperty().multiply(0.7));
        p12.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p13 = FXMLLoader.load(getClass().getResource("/fxml/redTeam/pane_freeKill.fxml"));
        RXCarouselPane freeKillPane = new RXCarouselPane(p13);
        freeKillPane.getStylesheets().add(Constants.getResourceUrl("/css/redStyle.css"));
        p13.prefWidthProperty().bind(topBar.widthProperty().multiply(0.8));
        p13.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p14 = FXMLLoader.load(getClass().getResource("/fxml/redTeam/pane_customMemoryCode.fxml"));
        RXCarouselPane customMemoryCodePane = new RXCarouselPane(p14);
        customMemoryCodePane.getStylesheets().add(Constants.getResourceUrl("/css/redStyle.css"));
        p14.prefWidthProperty().bind(topBar.widthProperty().multiply(0.7));
        p14.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p15 = FXMLLoader.load(getClass().getResource("/fxml/redTeam/pane_customCommandGeneration.fxml"));
        RXCarouselPane customCommandGenerationPane = new RXCarouselPane(p15);
        customCommandGenerationPane.getStylesheets().add(Constants.getResourceUrl("/css/redStyle.css"));
        p15.prefWidthProperty().bind(topBar.widthProperty().multiply(0.8));
        p15.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p16 = FXMLLoader.load(getClass().getResource("/fxml/redTeam/pane_commandQuery.fxml"));
        RXCarouselPane commandQueryPane = new RXCarouselPane(p16);
        commandQueryPane.getStylesheets().add(Constants.getResourceUrl("/css/redStyle.css"));
        p16.prefWidthProperty().bind(topBar.widthProperty().multiply(0.8));
        p16.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p17 = FXMLLoader.load(getClass().getResource("/fxml/redTeam/pane_kbRootQuery.fxml"));
        RXCarouselPane kbRootQueryPane = new RXCarouselPane(p17);
        kbRootQueryPane.getStylesheets().add(Constants.getResourceUrl("/css/redStyle.css"));
        p17.prefWidthProperty().bind(topBar.widthProperty().multiply(0.8));
        p17.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p18 = FXMLLoader.load(getClass().getResource("/fxml/redTeam/pane_processQuery.fxml"));
        RXCarouselPane processQueryPane = new RXCarouselPane(p18);
        processQueryPane.getStylesheets().add(Constants.getResourceUrl("/css/redStyle.css"));
        p18.prefWidthProperty().bind(topBar.widthProperty().multiply(0.8));
        p18.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        Pane p19 = FXMLLoader.load(getClass().getResource("/fxml/redTeam/pane_infoGeneration.fxml"));
        RXCarouselPane infoGenerationPane = new RXCarouselPane(p19);
        infoGenerationPane.getStylesheets().add(Constants.getResourceUrl("/css/redStyle.css"));
        p19.prefWidthProperty().bind(topBar.widthProperty().multiply(0.8));
        p19.prefHeightProperty().bind(mainCarousel.heightProperty());
        yieldToUi();

        // 使用工厂创建 Extension 面板（红色模式）
        RXCarouselPane extensionPane_1 = PaneFactory.createExtensionPane("/css/redStyle.css", 0.8, mainCarousel.heightProperty(), topBar.widthProperty());
        yieldToUi();

        // 使用工厂创建 About 面板（红色模式）
        PaneFactory.AboutPaneResult aboutResult_1 = PaneFactory.createAboutPane("/css/redStyle.css", 0.7, mainCarousel.heightProperty().subtract(20), topBar.widthProperty());
        RXCarouselPane aboutPane_1 = aboutResult_1.pane;
        PaneAbout PaneAbout_1 = aboutResult_1.controller;
        yieldToUi();

        mainCarousel.setPaneList(webshellDecodePane, separateDecodePane, ipInFoPane, aiAnswerPane, decompilePane, blockchainPane, locationQueryPane, exifPane, extensionPane, aboutPane,
                infoSearchPane, portScanPane, vulScanPane, freeKillPane, customMemoryCodePane, customCommandGenerationPane, commandQueryPane, kbRootQueryPane, processQueryPane, infoGenerationPane, extensionPane_1, aboutPane_1);
        mainCarousel.setCarouselAnimation(new AnimNone());  // AnimFade
        mainCarousel.setAnimationTime(Duration.seconds(0));  // 0.2



        navGroup.selectedToggleProperty().addListener((ob, ov, nv) -> {
            if (nv == null) {
                return;
            }
            int index = navGroup.getToggles().indexOf(nv);
            applySelectedIndex(index, extensionPane, extensionPane_1, aboutPane, aboutPane_1, PaneAbout, PaneAbout_1);
        });

        topBar.widthProperty().addListener((observable, oldValue, newValue) -> {
            if(isBlueMode) {
                blueBar.setTranslateX(0);
                redBar.setTranslateX(newValue.doubleValue() - 320);
            }else {
                blueBar.setTranslateX(-(newValue.doubleValue() - 320));
                redBar.setTranslateX(0);
            }
        });
        
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
        double barWidthDistance = Math.max(topBar.getWidth() - 320, 0);

        topBar.getStyleClass().removeAll("blueStyle", "redStyle");
        changeBtn.getStyleClass().removeAll("blueStyle", "redStyle");

        if (blueMode) {
            blueModePane.setTranslateX(0);
            redModePane.setTranslateX(-55);
            blueBar.setTranslateX(0);
            redBar.setTranslateX(barWidthDistance);
            topBarLeft.setTranslateX(0);
            topBar.getStyleClass().add("blueStyle");
            changeBtn.getStyleClass().add("blueStyle");
            isChangeModePaneRight = false;
        } else {
            blueModePane.setTranslateX(55);
            redModePane.setTranslateX(0);
            blueBar.setTranslateX(-barWidthDistance);
            redBar.setTranslateX(0);
            topBarLeft.setTranslateX(-200);
            topBar.getStyleClass().add("redStyle");
            changeBtn.getStyleClass().add("redStyle");
            isChangeModePaneRight = true;
        }
    }

    private boolean isChangeModePaneRight = false;  // 是否向右移动
    private boolean isChangeModePaneMoving = false; // 是否开始移动动画
    @FXML
    void changeMode() {
        if(isChangeModePaneMoving) return;  // 如果动画正在播放，不响应按钮点击事件

        isChangeModePaneMoving = true;
        double barWidthDistance = topBar.getWidth() - 320;
        Scene scene = root.getScene();

        TranslateTransition blueTransition = new TranslateTransition(Duration.seconds(0.2), blueModePane);
        TranslateTransition redTransition = new TranslateTransition(Duration.seconds(0.2), redModePane);
        TranslateTransition blueBarTransition = new TranslateTransition(Duration.seconds(0.2), blueBar);
        TranslateTransition redBarTransition = new TranslateTransition(Duration.seconds(0.2), redBar);
        TranslateTransition topBarLeftTransition = new TranslateTransition(Duration.seconds(0.2), topBarLeft);

        // 创建并行动画组
        ParallelTransition parallelTransition = new ParallelTransition();

        if (isChangeModePaneRight) {
            // 切换为蓝队
            isBlueMode = true;
            blueTransition.setByX(-55);
            redTransition.setByX(-55);
            redBarTransition.setByX(barWidthDistance);
            blueBarTransition.setByX(barWidthDistance);
            topBarLeftTransition.setByX(200);

            topBar.getStyleClass().remove("redStyle");
            topBar.getStyleClass().add("blueStyle");
            changeBtn.getStyleClass().remove("redStyle");
            changeBtn.getStyleClass().add("blueStyle");

            navGroup.getToggles().get(selectedBlueIndex).setSelected(true);
        } else {
            // 切换为红队
            isBlueMode = false;
            blueTransition.setByX(55);
            redTransition.setByX(55);
            redBarTransition.setByX(-barWidthDistance);
            blueBarTransition.setByX(-barWidthDistance);
            topBarLeftTransition.setByX(-200);

            topBar.getStyleClass().remove("blueStyle");
            topBar.getStyleClass().add("redStyle");
            changeBtn.getStyleClass().remove("blueStyle");
            changeBtn.getStyleClass().add("redStyle");

            navGroup.getToggles().get(selectedRedIndex).setSelected(true);
        }

        // 将所有动画添加到并行动画组
        parallelTransition.getChildren().addAll(
                blueTransition,
                redTransition,
                redBarTransition,
                blueBarTransition,
                topBarLeftTransition
        );

        // 设置动画完成后的操作
        parallelTransition.setOnFinished(e -> {
            isChangeModePaneMoving = false;
        });

        // 播放并行动画组
        parallelTransition.play();

        isChangeModePaneRight = !isChangeModePaneRight;

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
