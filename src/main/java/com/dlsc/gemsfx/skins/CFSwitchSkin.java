package com.dlsc.gemsfx.skins;

import com.dlsc.gemsfx.CFSwitch;
import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.ToggleButtonBehavior;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.concurrent.Callable;

/**
 * @author Potato
 * @date 2024/7/5 09:34
 */
public class CFSwitchSkin extends CFLabelSkin<CFSwitch> {

    private final Rectangle back;
    private final Circle circle;
    //
    private final double size = 20;
    private final TranslateTransition TT;
    //
    private final BehaviorBase<CFSwitch> behavior;

    public CFSwitchSkin(CFSwitch control) {
        super(control);
        behavior = new ToggleButtonBehavior<>(control);
        back = new Rectangle();
        circle = new Circle();
        double backSize = size - 6;
        back.setHeight(backSize);
        back.setWidth(size * 2);
        back.setArcHeight(backSize);
        back.setArcWidth(backSize);
        circle.setRadius(size / 2);
        StackPane.setAlignment(circle, Pos.CENTER_LEFT);
        TT = new TranslateTransition(Duration.millis(200), circle);
        //
        textProperty().bind(control.textProperty());
        setGraphic(new StackPane(back, circle));
        initEvent();
    }

    private void initEvent() {
        CFSwitch skinnable = getSkinnable();
        BooleanProperty selected = skinnable.selectedProperty();
        circle.setTranslateX(selected.get() ? size : 0);
        // 监听是否选中，启动动画
        selected.addListener((observable, oldValue, newValue) -> {
            TT.stop();
            TT.setInterpolator(Interpolator.EASE_BOTH);
            TT.setToX(newValue ? size : 0);
            TT.setFromX(newValue ? 0 : size);
            TT.play();
        });
        //颜色绑定
        back.fillProperty().bind(Bindings.createObjectBinding((Callable<Paint>) () -> {
            Color cfColor = skinnable.getCfColor();
            return cfColor.deriveColor(1, 1, 1, 0.4);
        }, skinnable.cfColorProperty()));
        circle.fillProperty().bind(skinnable.cfColorProperty());
    }

    @Override
    public void dispose() {
        super.dispose();

        if (behavior != null) {
            behavior.dispose();
        }
    }
}
