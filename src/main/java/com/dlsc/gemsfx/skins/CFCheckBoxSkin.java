package com.dlsc.gemsfx.skins;

import com.dlsc.gemsfx.CFCheckBox;
import javafx.animation.ScaleTransition;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class CFCheckBoxSkin extends CFLabelSkin<CFCheckBox> {

    private final StackPane box;
    private final StackPane mark;
    // 动画
    private final ScaleTransition scaleTransition;

    public CFCheckBoxSkin(CFCheckBox control) {
        super(control);
        // 布局
        mark = new StackPane();
        box = new StackPane(mark);
        textProperty().bind(control.textProperty());
        mark.getStyleClass().add("mark");
        box.getStyleClass().add("box");
        setGraphic(box);
        // 动画
        scaleTransition = new ScaleTransition(Duration.millis(200), mark);
        setAnimationInfo();
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    private void setAnimationInfo() {
        // 测试：
        CFCheckBox skinnable = getSkinnable();
        if (!skinnable.isSelected()) {
            mark.setScaleX(0);
            mark.setScaleY(0);
        }
        skinnable.selectedProperty().addListener((observableValue, aBoolean, t1) -> {
            scaleTransition.stop();
            if (t1) {
                scaleTransition.setToX(1);
                scaleTransition.setToY(1);
                scaleTransition.setFromX(0);
                scaleTransition.setFromY(0);
                scaleTransition.play();
            } else {
                mark.setScaleX(0);
                mark.setScaleY(0);
            }
        });
    }
}
