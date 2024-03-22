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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
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

    private double offsetX,offsetY;


    @FXML
    void initialize() throws IOException {

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
        p2.prefWidthProperty().bind(topBar.widthProperty().multiply(0.64));
        p2.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p3 = FXMLLoader.load(getClass().getResource("/fxml/pane_getIpInfo.fxml"));
        RXCarouselPane ipInFoPane = new RXCarouselPane(p3);
        p3.prefWidthProperty().bind(topBar.widthProperty().multiply(0.64));
        p3.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p4 = FXMLLoader.load(getClass().getResource("/fxml/pane_aiAnswer.fxml"));
        RXCarouselPane aiAnswerPane = new RXCarouselPane(p4);
        p4.prefWidthProperty().bind(topBar.widthProperty().multiply(0.64));
        p4.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p5 = FXMLLoader.load(getClass().getResource("/fxml/pane_decompile.fxml"));
        RXCarouselPane decompilePane = new RXCarouselPane(p5);
        p5.prefWidthProperty().bind(topBar.widthProperty().multiply(0.64));
        p5.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p6 = FXMLLoader.load(getClass().getResource("/fxml/pane_blockchain.fxml"));
        RXCarouselPane blockchainPane = new RXCarouselPane(p6);
        p6.prefWidthProperty().bind(topBar.widthProperty().multiply(0.8));
        p6.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p7 = FXMLLoader.load(getClass().getResource("/fxml/pane_locationQuery.fxml"));
        RXCarouselPane locationQueryPane = new RXCarouselPane(p7);
        p7.prefWidthProperty().bind(topBar.widthProperty().multiply(0.8));
        p7.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p8 = FXMLLoader.load(getClass().getResource("/fxml/pane_extension.fxml"));
        RXCarouselPane extensionPane = new RXCarouselPane(p8);
        p8.prefWidthProperty().bind(topBar.widthProperty().multiply(0.8));
        p8.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p9 = FXMLLoader.load(getClass().getResource("/fxml/pane_about.fxml"));
        RXCarouselPane aboutPane = new RXCarouselPane(p9);
        p9.prefWidthProperty().bind(topBar.widthProperty().multiply(0.64));
        p9.prefHeightProperty().bind(mainCarousel.heightProperty().subtract(20));

        Pane p10 = FXMLLoader.load(getClass().getResource("/fxml/pane_infoSearch.fxml"));
        RXCarouselPane infoSearchPane = new RXCarouselPane(p10);
        p10.prefWidthProperty().bind(topBar.widthProperty().multiply(0.64));
        p10.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p11 = FXMLLoader.load(getClass().getResource("/fxml/pane_vulScan.fxml"));
        RXCarouselPane vulScanPane = new RXCarouselPane(p11);
        p11.prefWidthProperty().bind(topBar.widthProperty().multiply(0.64));
        p11.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p12 = FXMLLoader.load(getClass().getResource("/fxml/pane_webshellGeneration.fxml"));
        RXCarouselPane webshellGenerationPane = new RXCarouselPane(p12);
        p12.prefWidthProperty().bind(topBar.widthProperty().multiply(0.64));
        p12.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p13 = FXMLLoader.load(getClass().getResource("/fxml/pane_customMemoryCode.fxml"));
        RXCarouselPane customMemoryCodePane = new RXCarouselPane(p13);
        p13.prefWidthProperty().bind(topBar.widthProperty().multiply(0.64));
        p13.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p14 = FXMLLoader.load(getClass().getResource("/fxml/pane_customCommandGeneration.fxml"));
        RXCarouselPane customCommandGenerationPane = new RXCarouselPane(p14);
        p14.prefWidthProperty().bind(topBar.widthProperty().multiply(0.64));
        p14.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p15 = FXMLLoader.load(getClass().getResource("/fxml/pane_kbRootQuery.fxml"));
        RXCarouselPane kbRootQueryPane = new RXCarouselPane(p15);
        p15.prefWidthProperty().bind(topBar.widthProperty().multiply(0.64));
        p15.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p16 = FXMLLoader.load(getClass().getResource("/fxml/pane_processQuery.fxml"));
        RXCarouselPane processQueryPane = new RXCarouselPane(p16);
        p16.prefWidthProperty().bind(topBar.widthProperty().multiply(0.64));
        p16.prefHeightProperty().bind(mainCarousel.heightProperty());

        Pane p17 = FXMLLoader.load(getClass().getResource("/fxml/pane_infoGeneration.fxml"));
        RXCarouselPane infoGenerationPane = new RXCarouselPane(p17);
        p17.prefWidthProperty().bind(topBar.widthProperty().multiply(0.64));
        p17.prefHeightProperty().bind(mainCarousel.heightProperty());

        mainCarousel.setPaneList(webshellDecodePane, separateDecodePane, ipInFoPane, aiAnswerPane, decompilePane, blockchainPane, locationQueryPane, extensionPane, aboutPane,
                infoSearchPane, vulScanPane, webshellGenerationPane, customMemoryCodePane, customCommandGenerationPane, kbRootQueryPane, processQueryPane, infoGenerationPane);
        mainCarousel.setCarouselAnimation(new AnimNone());  // AnimFade
        mainCarousel.setAnimationTime(Duration.seconds(0));  // 0.2
        navGroup.selectedToggleProperty().addListener((ob, ov, nv) -> {
            int index = navGroup.getToggles().indexOf(nv);
            mainCarousel.setSelectedIndex(index);
        });

        topBar.widthProperty().addListener((observable, oldValue, newValue) -> {
            redBar.setTranslateX(newValue.doubleValue() - 320);
        });

    }

    private boolean isChangeModePaneRight = false;  // 是否向右移动
    private boolean isChangeModePaneMoving = false; // 是否开始移动动画
    @FXML
    void changeMode() {
        if(isChangeModePaneMoving) return;  // 如果动画正在播放，不响应按钮点击事件

        isChangeModePaneMoving = true;

        double barWidthDistance = topBar.getPrefWidth() - 320;

        TranslateTransition blueTransition = new TranslateTransition(Duration.seconds(0.2), blueModePane);
        TranslateTransition redTransition = new TranslateTransition(Duration.seconds(0.2), redModePane);
        TranslateTransition blueBarTransition = new TranslateTransition(Duration.seconds(0.2), blueBar);
        TranslateTransition redBarTransition = new TranslateTransition(Duration.seconds(0.2), redBar);
        TranslateTransition topBarLeftTransition = new TranslateTransition(Duration.seconds(0.2), topBarLeft);

        Scene scene = root.getScene();

        if (isChangeModePaneRight) {
            //  此时切换为蓝队
            mainCarousel.setSelectedIndex(0);
            blueTransition.setByX(-55);
            redTransition.setByX(-55);
            redBarTransition.setByX(barWidthDistance);
            blueBarTransition.setByX(barWidthDistance);
            topBarLeftTransition.setByX(200);

            scene.getRoot().getStyleClass().remove("redStyle");
            scene.getRoot().getStyleClass().add("blueStyle");

        }else {
            //  此时切换为红队
            mainCarousel.setSelectedIndex(9);
            blueTransition.setByX(55);
            redTransition.setByX(55);
            redBarTransition.setByX(-barWidthDistance);
            blueBarTransition.setByX(-barWidthDistance);
            topBarLeftTransition.setByX(-200);

            scene.getRoot().getStyleClass().remove("blueStyle");
            scene.getRoot().getStyleClass().add("redStyle");

        }

        blueTransition.setOnFinished(e -> {
            isChangeModePaneMoving = false;

//            if(isChangeModePaneRight){
//                delDialog();
//            }

        });
        blueTransition.play();
        redTransition.play();
        redBarTransition.play();
        blueBarTransition.play();
        topBarLeftTransition.play();

        isChangeModePaneRight = !isChangeModePaneRight;

    }

    private void delDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(root.getScene().getWindow());
        alert.initModality(Modality.WINDOW_MODAL);
        alert.initStyle(StageStyle.UTILITY);
        alert.getDialogPane().getStylesheets().add(Util.getResourceUrl("/css/common.css"));
        alert.setTitle("探索未开放界面");
        alert.setHeaderText("红队版暂不对外开放，相关代码已剔除");
        alert.setContentText("Red Team version is currently closed, please wait for updates……");

        Button ok = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        ok.setOnAction(e->{
            changeMode();
        });
        alert.show();
    }

    @FXML
    void showSet(ActionEvent event) {
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

}
