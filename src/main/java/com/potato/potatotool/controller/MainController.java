package com.potato.potatotool.controller;

import com.leewyatt.rxcontrols.animation.carousel.*;
import com.leewyatt.rxcontrols.controls.RXCarousel;
import com.leewyatt.rxcontrols.pane.RXCarouselPane;
import com.potato.potatotool.utils.Util;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
    private Rectangle blueModePane;
    @FXML
    private Rectangle redModePane;

    @FXML
    private Button reduceScreen;
    @FXML
    private Button fullScreen;
    @FXML
    private ImageView fullScreenImgView;

    private double offsetX,offsetY;

    private int selectedBlueIndex = 0;
    private int selectedRedIndex = 10;

    private boolean isFullScreen = false;

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

        Pane p1 = FXMLLoader.load(getClass().getResource("/fxml/pane_webshellDecode.fxml"));
        RXCarouselPane webshellDecodePane = new RXCarouselPane(p1);
        p1.prefWidthProperty().bind(topBar.widthProperty().multiply(0.64));
        p1.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p2 = FXMLLoader.load(getClass().getResource("/fxml/pane_separateDecode.fxml"));
        RXCarouselPane separateDecodePane = new RXCarouselPane(p2);
        p2.prefWidthProperty().bind(topBar.widthProperty().multiply(0.7));
        p2.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p3 = FXMLLoader.load(getClass().getResource("/fxml/pane_getIpInfo.fxml"));
        RXCarouselPane ipInFoPane = new RXCarouselPane(p3);
        p3.prefWidthProperty().bind(topBar.widthProperty().multiply(0.7));
        p3.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p4 = FXMLLoader.load(getClass().getResource("/fxml/pane_aiAnswer.fxml"));
        RXCarouselPane aiAnswerPane = new RXCarouselPane(p4);
        p4.prefWidthProperty().bind(topBar.widthProperty().multiply(0.7));
        p4.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p5 = FXMLLoader.load(getClass().getResource("/fxml/pane_decompile.fxml"));
        RXCarouselPane decompilePane = new RXCarouselPane(p5);
        p5.prefWidthProperty().bind(topBar.widthProperty().multiply(0.7));
        p5.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p6 = FXMLLoader.load(getClass().getResource("/fxml/pane_blockchain.fxml"));
        RXCarouselPane blockchainPane = new RXCarouselPane(p6);
        p6.prefWidthProperty().bind(topBar.widthProperty().multiply(0.64));
        p6.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p7 = FXMLLoader.load(getClass().getResource("/fxml/pane_locationQuery.fxml"));
        RXCarouselPane locationQueryPane = new RXCarouselPane(p7);
        p7.prefWidthProperty().bind(topBar.widthProperty().multiply(0.7));
        p7.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p8 = FXMLLoader.load(getClass().getResource("/fxml/pane_exif.fxml"));
        RXCarouselPane exifPane = new RXCarouselPane(p8);
        p8.prefWidthProperty().bind(topBar.widthProperty().multiply(0.7));
        p8.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p9 = FXMLLoader.load(getClass().getResource("/fxml/pane_extension.fxml"));
        RXCarouselPane extensionPane = new RXCarouselPane(p9);
        p9.prefWidthProperty().bind(topBar.widthProperty().multiply(0.8));
        p9.prefHeightProperty().bind(mainCarousel.heightProperty());

        FXMLLoader loader_pane_about = new FXMLLoader(getClass().getResource("/fxml/pane_about.fxml"));
        Pane p_pane_about = loader_pane_about.load();
        PaneAbout PaneAbout = loader_pane_about.getController();
        RXCarouselPane aboutPane = new RXCarouselPane(p_pane_about);
        p_pane_about.prefWidthProperty().bind(topBar.widthProperty().multiply(0.7));
        p_pane_about.prefHeightProperty().bind(mainCarousel.heightProperty().subtract(20));

        Pane p11 = FXMLLoader.load(getClass().getResource("/fxml/pane_infoSearch.fxml"));
        RXCarouselPane infoSearchPane = new RXCarouselPane(p11);
        p11.prefWidthProperty().bind(topBar.widthProperty().multiply(0.7));
        p11.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p12 = FXMLLoader.load(getClass().getResource("/fxml/pane_vulScan.fxml"));
        RXCarouselPane vulScanPane = new RXCarouselPane(p12);
        p12.prefWidthProperty().bind(topBar.widthProperty().multiply(0.7));
        p12.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p13 = FXMLLoader.load(getClass().getResource("/fxml/pane_webshellGeneration.fxml"));
        RXCarouselPane webshellGenerationPane = new RXCarouselPane(p13);
        p13.prefWidthProperty().bind(topBar.widthProperty().multiply(0.7));
        p13.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p14 = FXMLLoader.load(getClass().getResource("/fxml/pane_customMemoryCode.fxml"));
        RXCarouselPane customMemoryCodePane = new RXCarouselPane(p14);
        p14.prefWidthProperty().bind(topBar.widthProperty().multiply(0.7));
        p14.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p15 = FXMLLoader.load(getClass().getResource("/fxml/pane_customCommandGeneration.fxml"));
        RXCarouselPane customCommandGenerationPane = new RXCarouselPane(p15);
        p15.prefWidthProperty().bind(topBar.widthProperty().multiply(0.8));
        p15.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p16 = FXMLLoader.load(getClass().getResource("/fxml/pane_kbRootQuery.fxml"));
        RXCarouselPane kbRootQueryPane = new RXCarouselPane(p16);
        p16.prefWidthProperty().bind(topBar.widthProperty().multiply(0.8));
        p16.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p17 = FXMLLoader.load(getClass().getResource("/fxml/pane_processQuery.fxml"));
        RXCarouselPane processQueryPane = new RXCarouselPane(p17);
        p17.prefWidthProperty().bind(topBar.widthProperty().multiply(0.8));
        p17.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p18 = FXMLLoader.load(getClass().getResource("/fxml/pane_infoGeneration.fxml"));
        RXCarouselPane infoGenerationPane = new RXCarouselPane(p18);
        p18.prefWidthProperty().bind(topBar.widthProperty().multiply(0.8));
        p18.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p19 = FXMLLoader.load(getClass().getResource("/fxml/pane_extension.fxml"));
        RXCarouselPane extensionPane_1 = new RXCarouselPane(p19);
        p19.prefWidthProperty().bind(topBar.widthProperty().multiply(0.8));
        p19.prefHeightProperty().bind(mainCarousel.heightProperty());

        FXMLLoader loader_pane_about_1 = new FXMLLoader(getClass().getResource("/fxml/pane_about.fxml"));
        Pane p_pane_about_1 = loader_pane_about_1.load();
        PaneAbout PaneAbout_1 = loader_pane_about_1.getController();
        RXCarouselPane aboutPane_1 = new RXCarouselPane(p_pane_about_1);
        p_pane_about_1.prefWidthProperty().bind(topBar.widthProperty().multiply(0.7));
        p_pane_about_1.prefHeightProperty().bind(mainCarousel.heightProperty().subtract(20));

        mainCarousel.setPaneList(webshellDecodePane, separateDecodePane, ipInFoPane, aiAnswerPane, decompilePane, blockchainPane, locationQueryPane, exifPane, extensionPane, aboutPane,
                infoSearchPane, vulScanPane, webshellGenerationPane, customMemoryCodePane, customCommandGenerationPane, kbRootQueryPane, processQueryPane, infoGenerationPane, extensionPane_1, aboutPane_1);
        mainCarousel.setCarouselAnimation(new AnimNone());  // AnimFade
        mainCarousel.setAnimationTime(Duration.seconds(0));  // 0.2



        navGroup.selectedToggleProperty().addListener((ob, ov, nv) -> {
            int index = navGroup.getToggles().indexOf(nv);
            mainCarousel.setSelectedIndex(index);

            // 点击关于界面时 触发滚动信息
            if(mainCarousel.getPaneList().get(index) == aboutPane){
                PaneAbout.startScrolling();
            }else{
                PaneAbout.stopScrolling();
            }
            if(mainCarousel.getPaneList().get(index) == aboutPane_1){
                PaneAbout_1.startScrolling();
            }else{
                PaneAbout_1.stopScrolling();
            }

            if (isBlueMode) {
                selectedBlueIndex = index;
            } else {
                selectedRedIndex = index;
            }
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

    }

    private boolean isChangeModePaneRight = false;  // 是否向右移动
    private boolean isChangeModePaneMoving = false; // 是否开始移动动画
    private boolean isBlueMode = true;
    @FXML
    void changeMode() {
        if(isChangeModePaneMoving) return;  // 如果动画正在播放，不响应按钮点击事件

        isChangeModePaneMoving = true;

        double barWidthDistance = topBar.getWidth() - 320;

        TranslateTransition blueTransition = new TranslateTransition(Duration.seconds(0.2), blueModePane);
        TranslateTransition redTransition = new TranslateTransition(Duration.seconds(0.2), redModePane);
        TranslateTransition blueBarTransition = new TranslateTransition(Duration.seconds(0.2), blueBar);
        TranslateTransition redBarTransition = new TranslateTransition(Duration.seconds(0.2), redBar);
        TranslateTransition topBarLeftTransition = new TranslateTransition(Duration.seconds(0.2), topBarLeft);

        Scene scene = root.getScene();

        if (isChangeModePaneRight) {
            //  此时切换为蓝队
            isBlueMode = true;
            blueTransition.setByX(-55);
            redTransition.setByX(-55);
            redBarTransition.setByX(barWidthDistance);
            blueBarTransition.setByX(barWidthDistance);
            topBarLeftTransition.setByX(200);

            scene.getRoot().getStyleClass().remove("redStyle");
            scene.getRoot().getStyleClass().add("blueStyle");
            navGroup.getToggles().get(selectedBlueIndex).setSelected(true);

        }else {
            //  此时切换为红队
            isBlueMode = false;
            blueTransition.setByX(55);
            redTransition.setByX(55);
            redBarTransition.setByX(-barWidthDistance);
            blueBarTransition.setByX(-barWidthDistance);
            topBarLeftTransition.setByX(-200);

            scene.getRoot().getStyleClass().remove("blueStyle");
            scene.getRoot().getStyleClass().add("redStyle");
            navGroup.getToggles().get(selectedRedIndex).setSelected(true);

        }

        blueTransition.setOnFinished(e -> {
            isChangeModePaneMoving = false;

        });
        blueTransition.play();
        redTransition.play();
        redBarTransition.play();
        blueBarTransition.play();
        topBarLeftTransition.play();

        isChangeModePaneRight = !isChangeModePaneRight;

    }

    @FXML
    void showSet(ActionEvent event) {
        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                stage.initOwner(root.getScene().getWindow());
                stage.initModality(Modality.WINDOW_MODAL);
                stage.initStyle(StageStyle.TRANSPARENT);
                stage.setAlwaysOnTop(true);

                AnchorPane dialogRoot = new FXMLLoader(getClass().getResource("/fxml/setting.fxml")).load();
                Scene scene = new Scene(dialogRoot);
                scene.getStylesheets().add(Util.getResourceUrl("/css/common.css"));
                scene.setFill(null);    //  背景透明
                stage.setScene(scene);
                stage.setTitle("修改配置信息");
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    void exitAction(ActionEvent event) {
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
