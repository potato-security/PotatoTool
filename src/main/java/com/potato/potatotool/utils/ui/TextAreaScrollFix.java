package com.potato.potatotool.utils.ui;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;

import static com.potato.potatotool.utils.ui.ScrollOptimizationConstants.*;

/**
 * TextArea 平滑滚动优化
 * 查找内部 ScrollPane 并应用 ScrollSpeedFix
 *
 * @author Potato
 * @since 2.5.1
 */
public class TextAreaScrollFix {

    public static void apply(TextArea textArea) {
        apply(textArea, DEFAULT_SENSITIVITY, DEFAULT_FRICTION);
    }

    public static void apply(TextArea textArea, double sensitivity, double friction) {
        if (textArea.getSkin() != null) {
            applyToInternal(textArea, sensitivity, friction);
        } else {
            textArea.skinProperty().addListener((obs, oldSkin, newSkin) -> {
                if (newSkin != null) {
                    applyToInternal(textArea, sensitivity, friction);
                }
            });
        }
    }

    private static void applyToInternal(TextArea textArea, double sensitivity, double friction) {
        ScrollPane scrollPane = findScrollPane(textArea);
        if (scrollPane != null) {
            ScrollSpeedFix.apply(scrollPane, sensitivity, friction);
        }
    }

    private static ScrollPane findScrollPane(Node node) {
        if (node instanceof ScrollPane) {
            return (ScrollPane) node;
        }
        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                ScrollPane result = findScrollPane(child);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}