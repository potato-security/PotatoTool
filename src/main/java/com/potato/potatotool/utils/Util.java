package com.potato.potatotool.utils;

import javafx.animation.FadeTransition;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * @author Potato
 * @date 2023/9/22 14:29
 */
public class Util {

    private Util() {

    }

    public static String getResourceUrl(String path){
        return Util.class.getResource(path).toExternalForm();
    }

    public void switchScene(Stage stage, StackPane newLayout) {
        Scene newScene = new Scene(newLayout);
        stage.setScene(newScene);

        // 添加淡入淡出效果
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(1000), newLayout);
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        fadeTransition.play();
    }

}
