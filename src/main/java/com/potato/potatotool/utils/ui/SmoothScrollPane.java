package com.potato.potatotool.utils.ui;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

/**
 * 平滑滚动 ScrollPane
 *
 * @author Potato
 * @since 2.5.1
 */
public class SmoothScrollPane extends ScrollPane {

    public SmoothScrollPane() {
        ScrollSpeedFix.apply(this);
    }

    public SmoothScrollPane(Node content) {
        super(content);
        ScrollSpeedFix.apply(this);
    }

    public void setScrollParams(double sensitivity, double friction) {
        ScrollSpeedFix.apply(this, sensitivity, friction);
    }
}