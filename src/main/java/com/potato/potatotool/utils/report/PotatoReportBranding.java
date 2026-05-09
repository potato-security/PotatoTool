package com.potato.potatotool.utils.report;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

/**
 * PotatoTool 报告品牌资源与统一主题色
 */
public final class PotatoReportBranding {

    public static final String FONT_BODY = "Microsoft YaHei";
    public static final String FONT_MONO = "Consolas";

    public static final String COLOR_PRIMARY = "0F5EA8";
    public static final String COLOR_PRIMARY_DARK = "0B457A";
    public static final String COLOR_PRIMARY_SOFT = "E8F1FB";
    public static final String COLOR_ACCENT = "0EA5E9";
    public static final String COLOR_ACCENT_SOFT = "E6F6FD";
    public static final String COLOR_BACKGROUND = "F4F9FD";
    public static final String COLOR_SURFACE = "FFFFFF";
    public static final String COLOR_SURFACE_SOFT = "F7FBFF";
    public static final String COLOR_TEXT_DARK = "0F172A";
    public static final String COLOR_TEXT_MUTED = "475569";
    public static final String COLOR_BORDER = "D8E6F3";
    public static final String COLOR_CODE_BG = "F8FBFE";
    public static final String COLOR_SUCCESS = "15803D";
    public static final String COLOR_SUCCESS_SOFT = "EAF7EE";

    public static final String COLOR_CRITICAL = "DC2626";
    public static final String COLOR_HIGH = "EA580C";
    public static final String COLOR_MEDIUM = "D97706";
    public static final String COLOR_LOW = "2563EB";
    public static final String COLOR_INFO = "64748B";

    public static final String COLOR_CRITICAL_SOFT = "FDECEC";
    public static final String COLOR_HIGH_SOFT = "FEF1E8";
    public static final String COLOR_MEDIUM_SOFT = "FEF6E8";
    public static final String COLOR_LOW_SOFT = "EAF2FF";
    public static final String COLOR_INFO_SOFT = "F1F5F9";

    private static final String ICON_PATH = "/img/logo.png";
    private static final String WORDMARK_POTATO_PATH = "/img/potato.png";
    private static final String WORDMARK_TOOL_PATH = "/img/tool.png";

    private PotatoReportBranding() {
    }

    public static void appendIcon(XWPFParagraph paragraph, int width, int height) {
        appendPicture(paragraph, loadImageAsset(ICON_PATH, false), "logo.png", width, height);
    }

    public static void appendWordmark(XWPFParagraph paragraph, int width, int height) {
        ImageAsset potatoAsset = loadImageAsset(WORDMARK_POTATO_PATH, true);
        ImageAsset toolAsset = loadImageAsset(WORDMARK_TOOL_PATH, true);
        int safeHeight = Math.max(1, height);
        int safeWidth = Math.max(1, width);
        double potatoRatio = potatoAsset.getRatio();
        double toolRatio = toolAsset.getRatio();
        int baseGap = Math.max(4, safeHeight / 8);
        double totalWidth = potatoRatio * safeHeight + toolRatio * safeHeight + baseGap;
        double scale = totalWidth > safeWidth ? safeWidth / totalWidth : 1.0d;

        int scaledHeight = Math.max(1, (int) Math.round(safeHeight * scale));
        int scaledGap = Math.max(3, (int) Math.round(baseGap * scale));
        int potatoWidth = Math.max(1, (int) Math.round(potatoRatio * scaledHeight));
        int toolWidth = Math.max(1, (int) Math.round(toolRatio * scaledHeight));

        appendPicture(paragraph, potatoAsset, "potato.png", potatoWidth, scaledHeight);

        XWPFRun spacerRun = paragraph.createRun();
        spacerRun.setText(createWordmarkGap(scaledGap));

        appendPicture(paragraph, toolAsset, "tool.png", toolWidth, scaledHeight);
    }

    public static String getIconDataUri() {
        return toDataUri(loadImageAsset(ICON_PATH, false));
    }

    public static String getPotatoWordmarkDataUri() {
        return toDataUri(loadImageAsset(WORDMARK_POTATO_PATH, true));
    }

    public static String getToolWordmarkDataUri() {
        return toDataUri(loadImageAsset(WORDMARK_TOOL_PATH, true));
    }

    private static void appendPicture(XWPFParagraph paragraph, ImageAsset imageAsset,
                                      String pictureName, int width, int height) {
        if (imageAsset.isEmpty()) {
            return;
        }
        try (InputStream in = new ByteArrayInputStream(imageAsset.bytes)) {
            XWPFRun run = paragraph.createRun();
            run.addPicture(in, XWPFDocument.PICTURE_TYPE_PNG, pictureName,
                    Units.toEMU(width), Units.toEMU(height));
        } catch (Exception ignored) {
            // 报告图片资源失败时不阻断主流程
        }
    }

    private static String createWordmarkGap(int pixelGap) {
        int spaces = Math.max(3, pixelGap / 4);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < spaces; i++) {
            builder.append(' ');
        }
        return builder.toString();
    }

    private static String toDataUri(ImageAsset imageAsset) {
        if (imageAsset.isEmpty()) {
            return "";
        }
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageAsset.bytes);
    }

    private static ImageAsset loadImageAsset(String resourcePath, boolean trimTransparent) {
        try {
            byte[] bytes = readResourceBytes(resourcePath);
            if (bytes.length == 0) {
                return ImageAsset.empty();
            }

            if (!trimTransparent) {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                if (image == null) {
                    return new ImageAsset(bytes, 1, 1);
                }
                return new ImageAsset(bytes, image.getWidth(), image.getHeight());
            }

            BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
            if (source == null) {
                return ImageAsset.empty();
            }

            BufferedImage cropped = cropTransparentArea(source);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                ImageIO.write(cropped, "png", out);
                return new ImageAsset(out.toByteArray(), cropped.getWidth(), cropped.getHeight());
            } finally {
                out.close();
            }
        } catch (IOException ignored) {
            return ImageAsset.empty();
        }
    }

    private static byte[] readResourceBytes(String resourcePath) throws IOException {
        InputStream in = PotatoReportBranding.class.getResourceAsStream(resourcePath);
        if (in == null) {
            return new byte[0];
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        try {
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        } finally {
            in.close();
            out.close();
        }
    }

    private static BufferedImage cropTransparentArea(BufferedImage image) {
        if (image == null || !image.getColorModel().hasAlpha()) {
            return image;
        }

        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha > 0) {
                    if (x < minX) {
                        minX = x;
                    }
                    if (y < minY) {
                        minY = y;
                    }
                    if (x > maxX) {
                        maxX = x;
                    }
                    if (y > maxY) {
                        maxY = y;
                    }
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return image;
        }

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        BufferedImage cropped = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                cropped.setRGB(x, y, image.getRGB(minX + x, minY + y));
            }
        }
        return cropped;
    }

    private static final class ImageAsset {
        private final byte[] bytes;
        private final int width;
        private final int height;

        private ImageAsset(byte[] bytes, int width, int height) {
            this.bytes = bytes == null ? new byte[0] : bytes;
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
        }

        private static ImageAsset empty() {
            return new ImageAsset(new byte[0], 1, 1);
        }

        private boolean isEmpty() {
            return bytes.length == 0;
        }

        private double getRatio() {
            return width * 1.0d / height;
        }
    }
}
