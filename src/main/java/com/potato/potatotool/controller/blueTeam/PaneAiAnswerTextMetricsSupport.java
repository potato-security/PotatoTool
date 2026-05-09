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
    private static final Method COMPUTE_TEXT_WIDTH_METHOD = findMethod(
            "computeTextWidth",
            Font.class,
            String.class,
            double.class
    );
    private static final Method COMPUTE_TEXT_HEIGHT_WITH_SPACING_METHOD = findMethod(
            "computeTextHeight",
            Font.class,
            String.class,
            double.class,
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
        return computeTextHeight(font, text, wrappingWidth, 0, DEFAULT_BOUNDS_TYPE);
    }

    static double computeTextHeight(Font font,
                                    String text,
                                    double wrappingWidth,
                                    double lineSpacing,
                                    TextBoundsType boundsType) {
        return computeMeasuredTextHeight(
                font,
                normalizeLineSeparators(text),
                wrappingWidth,
                lineSpacing,
                boundsType == null ? DEFAULT_BOUNDS_TYPE : boundsType
        );
    }

    static double computeTextWidth(Font font, String text) {
        if (font == null) {
            return 0;
        }
        String safeText = normalizeLineSeparators(text);
        String[] lines = safeText.split("\n", -1);
        double maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, computeSingleLineTextWidth(font, line));
        }
        return Math.ceil(maxWidth);
    }

    static double clampHeight(double desiredHeight, double minHeight, double maxHeight) {
        double safeDesiredHeight = Math.max(0, desiredHeight);
        double safeMaxHeight = maxHeight > 0 ? maxHeight : safeDesiredHeight;
        return Math.max(minHeight, Math.min(safeMaxHeight, safeDesiredHeight));
    }

    private static double computeMeasuredTextHeight(Font font,
                                                    String text,
                                                    double wrappingWidth,
                                                    double lineSpacing,
                                                    TextBoundsType boundsType) {
        if (font == null) {
            return 0;
        }
        String safeText = text == null ? "" : text;
        double safeWrappingWidth = Math.max(0, wrappingWidth);
        double safeLineSpacing = Math.max(0, lineSpacing);

        if (COMPUTE_TEXT_HEIGHT_WITH_SPACING_METHOD != null) {
            try {
                Object value = COMPUTE_TEXT_HEIGHT_WITH_SPACING_METHOD.invoke(
                        null,
                        font,
                        safeText,
                        safeWrappingWidth,
                        safeLineSpacing,
                        boundsType
                );
                if (value instanceof Double) {
                    return ((Double) value).doubleValue();
                }
            } catch (Exception ignored) {
                // Fall back to a local Text node if internal JavaFX APIs are unavailable.
            }
        }

        if (safeLineSpacing == 0 && COMPUTE_TEXT_HEIGHT_METHOD != null) {
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
            FALLBACK_TEXT.setLineSpacing(safeLineSpacing);
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

    private static double computeSingleLineTextWidth(Font font, String text) {
        String safeText = text == null ? "" : text;
        if (COMPUTE_TEXT_WIDTH_METHOD != null) {
            try {
                Object value = COMPUTE_TEXT_WIDTH_METHOD.invoke(null, font, safeText, 0.0d);
                if (value instanceof Double) {
                    return ((Double) value).doubleValue();
                }
            } catch (Exception ignored) {
                // Fall back to a local Text node if internal JavaFX APIs are unavailable.
            }
        }

        synchronized (FALLBACK_TEXT) {
            FALLBACK_TEXT.setFont(font);
            FALLBACK_TEXT.setBoundsType(DEFAULT_BOUNDS_TYPE);
            FALLBACK_TEXT.setLineSpacing(0);
            FALLBACK_TEXT.setWrappingWidth(0);
            FALLBACK_TEXT.setText(safeText);
            return FALLBACK_TEXT.getLayoutBounds().getWidth();
        }
    }
}
