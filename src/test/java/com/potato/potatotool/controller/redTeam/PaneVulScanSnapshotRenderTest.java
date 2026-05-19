package com.potato.potatotool.controller.redTeam;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PaneVulScan 可视化快照测试")
class PaneVulScanSnapshotRenderTest {

    private static final AtomicBoolean FX_STARTED = new AtomicBoolean(false);

    @Test
    @DisplayName("pane_vulScan.fxml 应可离屏渲染并输出非空快照")
    void shouldRenderPaneVulScanSnapshotOffscreen() throws Exception {
        ensureFxStarted();

        final int width = 1600;
        final int height = 1200;
        final Path evidenceDir = Paths.get("target", "ui-evidence");
        final Path imagePath = evidenceDir.resolve("pane-vulscan-snapshot.png");
        final Path summaryPath = evidenceDir.resolve("pane-vulscan-snapshot-summary.txt");
        Files.createDirectories(evidenceDir);

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> errorRef = new AtomicReference<Throwable>();
        final AtomicReference<SnapshotStats> statsRef = new AtomicReference<SnapshotStats>();

        Platform.runLater(() -> {
            try {
                Parent root = loadPaneWithoutHeavyInitialize();
                if (root instanceof Region) {
                    ((Region) root).setPrefSize(width, height);
                }

                JFXPanel panel = new JFXPanel();
                Scene scene = new Scene(root, width, height);
                scene.getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
                panel.setScene(scene);
                root.applyCss();
                root.layout();

                WritableImage image = root.snapshot(new SnapshotParameters(), new WritableImage(width, height));
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
                ImageIO.write(bufferedImage, "png", imagePath.toFile());

                SnapshotStats stats = SnapshotStats.from(bufferedImage);
                Files.write(summaryPath, stats.toSummary(width, height).getBytes(StandardCharsets.UTF_8));
                statsRef.set(stats);
            } catch (Throwable throwable) {
                errorRef.set(throwable);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(60, TimeUnit.SECONDS), "PaneVulScan 快照测试超时");
        if (errorRef.get() != null) {
            throw new AssertionError("PaneVulScan 离屏渲染失败", errorRef.get());
        }

        SnapshotStats stats = statsRef.get();
        assertTrue(Files.exists(imagePath), "快照图片未生成");
        assertTrue(Files.size(imagePath) > 0, "快照图片为空");
        assertTrue(stats != null, "快照统计信息缺失");
        assertTrue(stats.nonTransparentPixels > 50000, "快照可见像素过少: " + stats.nonTransparentPixels);
        assertTrue(stats.distinctSampleColors >= 8, "快照颜色过于单一，疑似空白渲染: " + stats.distinctSampleColors);
    }

    private void ensureFxStarted() throws Exception {
        if (FX_STARTED.compareAndSet(false, true)) {
            final CountDownLatch latch = new CountDownLatch(1);
            new Thread(() -> {
                new JFXPanel();
                latch.countDown();
            }, "pane-vulscan-fx-bootstrap").start();
            assertTrue(latch.await(15, TimeUnit.SECONDS), "JavaFX Toolkit 初始化超时");
        }
    }

    private Parent loadPaneWithoutHeavyInitialize() throws Exception {
        String fxml = readClasspath("/fxml/redTeam/pane_vulScan.fxml");
        String withoutController = fxml.replace(
                "fx:controller=\"com.potato.potatotool.controller.redTeam.PaneVulScan\"",
                "");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/redTeam/pane_vulScan.fxml"));
        loader.setController(new SnapshotController());
        try (ByteArrayInputStream inputStream =
                     new ByteArrayInputStream(withoutController.getBytes(StandardCharsets.UTF_8))) {
            return loader.load(inputStream);
        }
    }

    private String readClasspath(String path) throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream(path)) {
            assertTrue(inputStream != null, "资源不存在: " + path);
            byte[] buffer = new byte[4096];
            StringBuilder builder = new StringBuilder();
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                builder.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
            }
            return builder.toString();
        }
    }

    public static class SnapshotController extends PaneVulScan {
        @FXML
        void initialize() {
            // 离屏渲染只验证 FXML 可视结构，不触发后台 POC 加载和运行时服务初始化。
        }
    }

    private static class SnapshotStats {
        private final int nonTransparentPixels;
        private final int distinctSampleColors;

        private SnapshotStats(int nonTransparentPixels, int distinctSampleColors) {
            this.nonTransparentPixels = nonTransparentPixels;
            this.distinctSampleColors = distinctSampleColors;
        }

        private static SnapshotStats from(BufferedImage image) {
            int nonTransparent = 0;
            Set<Integer> sampleColors = new HashSet<Integer>();
            int sampleStepX = Math.max(1, image.getWidth() / 40);
            int sampleStepY = Math.max(1, image.getHeight() / 30);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    int alpha = (argb >>> 24) & 0xff;
                    if (alpha > 0) {
                        nonTransparent++;
                    }
                    if (x % sampleStepX == 0 && y % sampleStepY == 0) {
                        sampleColors.add(argb);
                    }
                }
            }
            return new SnapshotStats(nonTransparent, sampleColors.size());
        }

        private String toSummary(int width, int height) {
            return "width=" + width + "\n"
                    + "height=" + height + "\n"
                    + "non_transparent_pixels=" + nonTransparentPixels + "\n"
                    + "distinct_sample_colors=" + distinctSampleColors + "\n";
        }
    }
}
