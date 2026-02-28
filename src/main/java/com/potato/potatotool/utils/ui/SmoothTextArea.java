package com.potato.potatotool.utils.ui;

import javafx.scene.control.TextArea;

/**
 * 平滑滚动 TextArea
 *
 * @author Potato
 * @since 2.5.1
 */
public class SmoothTextArea extends TextArea {

    public SmoothTextArea() {
        TextAreaScrollFix.apply(this);
    }

    public SmoothTextArea(String text) {
        super(text);
        TextAreaScrollFix.apply(this);
    }

    public void setScrollParams(double sensitivity, double friction) {
        TextAreaScrollFix.apply(this, sensitivity, friction);
    }
}