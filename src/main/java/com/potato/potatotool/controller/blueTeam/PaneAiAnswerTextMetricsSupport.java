package com.potato.potatotool.controller.blueTeam;

import javafx.geometry.VPos;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;

import java.lang.reflect.Method;

final class PaneAiAnswerTextMetricsSupport {
    private static final TextBoundsType DEFAULT_BOUNDS_TYPE = TextBoundsType.LOGICAL;
    private static final Text FALLBACK_TEXT = createFallbackText();
    private static final Method COMPUTE_TEXT_HEIGHT_METHOD = findMethod(
            "computeTextHeight",
            Font.class,
            String.class,
            double.class,
            TextBoundsType.class
    );

    private PaneAiAnswerTextMetricsSupport() {
    }

    static String normalizeLineSeparators(String text) {
        String safeText = text == null ? "" : text;
        return safeText.replace("\r\n", "\n").replace('\r', '\n');
    }

    static double computeTextHeight(Font font, String text, double wrappingWidth) {
        return computeMeasuredTextHeight(font, normalizeLineSeparators(text), wrappingWidth, DEFAULT_BOUNDS_TYPE);
    }

    static double clampHeight(double desiredHeight, double minHeight, double maxHeight) {
        double safeDesiredHeight = Math.max(0, desiredHeight);
        double safeMaxHeight = maxHeight > 0 ? maxHeight : safeDesiredHeight;
        return Math.max(minHeight, Math.min(safeMaxHeight, safeDesiredHeight));
    }

    private static double computeMeasuredTextHeight(Font font,
                                                    String text,
                                                    double wrappingWidth,
                                                    TextBoundsType boundsType) {
        if (font == null) {
            return 0;
        }
        String safeText = text == null ? "" : text;
        double safeWrappingWidth = Math.max(0, wrappingWidth);

        if (COMPUTE_TEXT_HEIGHT_METHOD != null) {
            try {
                Object value = COMPUTE_TEXT_HEIGHT_METHOD.invoke(null, font, safeText, safeWrappingWidth, boundsType);
                if (value instanceof Double) {
                    return ((Double) value).doubleValue();
                }
            } catch (Exception ignored) {
                // Fall back to a local Text node if internal JavaFX APIs are unavailable.
            }
        }

        synchronized (FALLBACK_TEXT) {
            FALLBACK_TEXT.setFont(font);
            FALLBACK_TEXT.setBoundsType(boundsType);
            FALLBACK_TEXT.setWrappingWidth(safeWrappingWidth);
            FALLBACK_TEXT.setText(safeText);
            return Math.ceil(FALLBACK_TEXT.getLayoutBounds().getHeight());
        }
    }

    private static Method findMethod(String name, Class<?>... parameterTypes) {
        try {
            Class<?> utilsClass = Class.forName("com.sun.javafx.scene.control.skin.Utils");
            Method method = utilsClass.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Text createFallbackText() {
        Text text = new Text();
        text.setTextOrigin(VPos.TOP);
        text.setBoundsType(DEFAULT_BOUNDS_TYPE);
        return text;
    }
}
