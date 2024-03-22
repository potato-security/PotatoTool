package com.potato.potatotool.controller;

import javafx.animation.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.potato.potatotool.controller.MainController.clipRect;

/**
 * @author Potato
 * @date 2023/10/24 16:51
 */
public class PaneLoad {

    @FXML
    private StackPane root;

    @FXML
    private StackPane codePane;

    @FXML
    private Label redImg;

    @FXML
    private Label blueImg;

    @FXML
    private ImageView potatoImg;

    @FXML
    private ImageView toolImg;

    private List<AnimatedText> animatedTexts = new ArrayList<>();

    private Random random = new Random();

    private BooleanProperty overLoading = new SimpleBooleanProperty(false);
    public BooleanProperty loadedProperty() {
        return overLoading;
    }

    private static final int NUM_ITEMS = 40;  // 数量
    private static final double RADIUS = 50;  // 半径
    private static final double CENTER_X = 250;  // 中心X坐标
    private static final double CENTER_Y = 250;  // 中心Y坐标
    private static final double ANGLE_OFFSET = -90;  // 起始位置

    public void initialize() {

        SimpleDoubleProperty arcProperty = new SimpleDoubleProperty(40.0);
        Rectangle clip = clipRect(
                blueImg, arcProperty
        );
        blueImg.setClip(clip);

        initAnimatedText();

        initAnimatedRedBlueLog();


//         虚拟动态眼镜Demo
//        AnchorPane pane = new AnchorPane();
//        for (int i = 0; i < NUM_ITEMS; i++) {
//            double angle = Math.toRadians(360 / NUM_ITEMS * i + ANGLE_OFFSET);
//            double x = CENTER_X + RADIUS * Math.cos(angle);
//            double y = CENTER_Y + RADIUS * Math.sin(angle);
//
//            Text text = new Text(String.valueOf(i + 1));
//            text.setFont(Font.font(20));
//            text.setLayoutX(x);
//            text.setLayoutY(y);
//            text.setRotate(360 / NUM_ITEMS * i); // Rotate text
//            pane.getChildren().add(text);
//        }
//        root.getChildren().add(pane);
    }

    private void initAnimatedText() {
        for (int i = 0; i < 20; i++) {
            AnimatedText animatedText =new AnimatedText();
            animatedTexts.add(animatedText);
            codePane.getChildren().addAll(animatedText.getNode());
        }
    }

    private void initAnimatedRedBlueLog(){
        potatoImg.setTranslateX(2000);
        toolImg.setTranslateX(2000);
        blueImg.setTranslateX(2000);
        redImg.setTranslateX(2000);

        FadeTransition fadeTransition1 = new FadeTransition(Duration.seconds(1), redImg); //循环，渐入渐出分别1秒
        fadeTransition1.setFromValue(0);
        fadeTransition1.setToValue(1);
        fadeTransition1.setCycleCount(1);

        FadeTransition fadeTransition2 = new FadeTransition(Duration.seconds(1), blueImg);
        fadeTransition2.setFromValue(0);
        fadeTransition2.setToValue(1);
        fadeTransition2.setCycleCount(1);

        FadeTransition fadeTransition3 = new FadeTransition(Duration.seconds(1), potatoImg);
        fadeTransition3.setFromValue(0);
        fadeTransition3.setToValue(1);
        fadeTransition3.setCycleCount(1);

        FadeTransition fadeTransition4 = new FadeTransition(Duration.seconds(1), toolImg);
        fadeTransition4.setFromValue(0);
        fadeTransition4.setToValue(1);
        fadeTransition4.setCycleCount(1);

        TranslateTransition translateTransition1 = new TranslateTransition(Duration.seconds(1), blueImg);
        translateTransition1.setFromX(-450);
        translateTransition1.setToX(-200);
        translateTransition1.setFromY(-350);
        translateTransition1.setToY(-100);
        translateTransition1.setCycleCount(1);

        TranslateTransition translateTransition2 = new TranslateTransition(Duration.seconds(1), redImg);
        translateTransition2.setFromX(450);
        translateTransition2.setToX(175);
        translateTransition2.setFromY(350);
        translateTransition2.setToY(100);
        translateTransition2.setCycleCount(1);

        TranslateTransition translateTransition3 = new TranslateTransition(Duration.seconds(1), potatoImg);
        translateTransition3.setFromX(450);
        translateTransition3.setToX(100);
        translateTransition3.setFromY(-350);
        translateTransition3.setToY(-150);
        translateTransition3.setCycleCount(1);

        TranslateTransition translateTransition4 = new TranslateTransition(Duration.seconds(1), toolImg);
        translateTransition4.setFromX(-450);
        translateTransition4.setToX(-150);
        translateTransition4.setFromY(350);
        translateTransition4.setToY(150);
        translateTransition4.setCycleCount(1);

        //  动画集
        ParallelTransition parallelTransition = new ParallelTransition(fadeTransition1, fadeTransition2, fadeTransition3 , fadeTransition4, translateTransition1, translateTransition2, translateTransition3, translateTransition4);

        //  延时
        PauseTransition delay = new PauseTransition(Duration.seconds(1));
        PauseTransition delayOver = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(event -> {
            parallelTransition.play();
            delayOver.play();
        });
        delayOver.setOnFinished(event -> {
            overLoading.set(true);
        });
        delay.play();


    }


    public void stopAnimations() {
        for (AnimatedText text : animatedTexts) {
            text.stop();
        }
    }

    /**
     * 淡入淡出+横向移动+内容变化 Text
     */
    class AnimatedText {

        private Text text;

        private ParallelTransition parallelTransition;
        private FadeTransition fadeTransition;
        private TranslateTransition translateTransition;
        private Timeline textUpdateTimeline;
        private Timeline timer;

        public void stop() {
            if(parallelTransition!=null){
                parallelTransition.stop();
                textUpdateTimeline = null;
                timer = null;
                parallelTransition = null;
                fadeTransition = null;
                translateTransition = null;
            }
            if(textUpdateTimeline!=null){
                textUpdateTimeline.stop();
            }
            if(timer!=null) {
                timer.stop();
            }
            if(fadeTransition!=null) {
                fadeTransition.stop();
            }
            if(translateTransition!=null) {
                translateTransition.stop();
            }
        }


        public AnimatedText() {

            text = new Text();

            int fontSize = random.nextInt(11) + 20;

            String[] textStyle={
                    ("-fx-font-size: "+fontSize+";-fx-fill: #3ECEE3;-fx-effect: dropshadow(gaussian, #3ECEE3, 20, 0, 0, 0);-fx-font-family: \"PingFang SC\", \"Microsoft YaHei\", Arial, sans-serif;"),
                    ("-fx-font-size: "+fontSize+";-fx-fill: #D85550;-fx-effect: dropshadow(gaussian, #D85550, 20, 0, 0, 0);-fx-font-family: \"PingFang SC\", \"Microsoft YaHei\", Arial, sans-serif;")};

            text.setStyle(textStyle[random.nextInt(textStyle.length)]);
            text.setTranslateY((random.nextDouble() - 0.45) * root.getPrefHeight());


            fadeTransition = new FadeTransition(Duration.seconds(1), text); //循环，渐入渐出分别1秒
            fadeTransition.setFromValue(0);
            fadeTransition.setToValue(1);
            fadeTransition.setCycleCount(Animation.INDEFINITE);
            fadeTransition.setAutoReverse(true);

            int max = (int) (root.getPrefWidth()/2);
            int min = (int) (root.getPrefWidth()*1/4);
            double currentX = random.nextInt(max - min) + min;
            int desMax = (int) (root.getPrefWidth()/2);
            int desMin = (int) (root.getPrefWidth()*1/3);
            double desX = random.nextInt(desMax - desMin) + desMin;
            translateTransition = new TranslateTransition(Duration.seconds(2), text);
            translateTransition.setFromX(-currentX + 100);
            translateTransition.setToX(desX - 200);
            translateTransition.setCycleCount(Animation.INDEFINITE);

            timer = new Timeline(
                    new KeyFrame(Duration.seconds(2), event -> {
                        // 在每两秒钟执行一次的代码
                        text.setTranslateY((random.nextDouble() - 0.45) * root.getPrefHeight());
                        text.setStyle(textStyle[random.nextInt(textStyle.length)]);
                    })
            );
            timer.setCycleCount(Animation.INDEFINITE);

            textUpdateTimeline = new Timeline(
                    new KeyFrame(Duration.seconds(0.2),
                            event -> {
                                StringBuilder randomCode = new StringBuilder();
                                for (int i = 0; i < 40 + random.nextDouble() * 20; i++) {
                                    randomCode.append(random.nextInt(2));
                                }
                                text.setText(randomCode.toString());
                            }
                    )
            );
            textUpdateTimeline.setCycleCount(Animation.INDEFINITE);

            //  动画集
            parallelTransition = new ParallelTransition(fadeTransition, translateTransition, timer, textUpdateTimeline);

            //  延时
            PauseTransition delay = new PauseTransition(Duration.seconds(random.nextDouble() * 5 + 0.5));
            delay.setOnFinished(event -> {
                if(parallelTransition!=null){
                    parallelTransition.play();
                }
            });
            delay.play();

        }

        public Node getNode() {
            return text;
        }

    }

}
