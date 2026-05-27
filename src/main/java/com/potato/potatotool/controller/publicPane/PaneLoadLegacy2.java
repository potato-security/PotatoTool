package com.potato.potatotool.controller.publicPane;

import com.potato.potatotool.utils.core.I18nUtils;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * PotatoTool 第二版历史启动加载页控制器 - Cinematic Boot Sequence。
 * <p>
 * 时间轴（总约 4.2s）：
 * <ul>
 *     <li>0.0s   背景与 HUD 装饰相继淡入</li>
 *     <li>0.5s   极光光晕、粒子、扫描线启动</li>
 *     <li>0.7s   玻璃面板缩放入场</li>
 *     <li>1.1s   Logo 光环 + 主体揭示，进入呼吸状态</li>
 *     <li>1.7s   主标题 "POTATO TOOL" 字符逐字浮入</li>
 *     <li>2.35s  副标题与分隔线浮入</li>
 *     <li>2.55s  进度条充能 + 阶段文本与 HUD 进度同步</li>
 *     <li>4.2s   触发 loadedProperty，让 MainApplication 切换至主舞台</li>
 * </ul>
 * <p>
 * 更早旧版动画保留在 {@link PaneLoadLegacy} / {@code load_legacy.fxml} / {@code load_legacy.css}
 * 中，可以通过修改 {@code MainApplication.loadMainStage} 的 fxml 路径切回。
 *
 * @author Potato
 * @date 2026/05/23
 */
public class PaneLoadLegacy2 {

    // ===================== 场景层 =====================
    @FXML private StackPane root;
    @FXML private Region bgLayer;
    @FXML private Region gridLayer;
    @FXML private Pane auroraLayer;
    @FXML private Pane particleLayer;
    @FXML private Pane scanLayer;

    // ===================== 玻璃面板 =====================
    @FXML private StackPane glassPanel;
    @FXML private VBox glassContent;
    @FXML private StackPane logoStack;
    @FXML private Region logoHaloOuter;
    @FXML private Region logoHaloInner;
    @FXML private Region logoRingOuter;
    @FXML private Region logoRingInner;
    @FXML private Region logoCore;
    @FXML private ImageView logoImage;
    @FXML private HBox titleBox;
    @FXML private VBox subtitleBox;
    @FXML private Label subtitleLine1;
    @FXML private Region subtitleDividerLeft;
    @FXML private Region subtitleDiamond;
    @FXML private Region subtitleDividerRight;
    @FXML private Label subtitleLine2;
    @FXML private StackPane progressContainer;
    @FXML private Region progressTrack;
    @FXML private Region progressFill;
    @FXML private Region progressShine;
    @FXML private Region stageDot;
    @FXML private Label stageLabel;
    @FXML private Region stageDotMirror;

    // ===================== HUD =====================
    @FXML private BorderPane hudLayer;
    @FXML private VBox hudTopLeft;
    @FXML private VBox hudTopRight;
    @FXML private VBox hudBottomLeft;
    @FXML private VBox hudBottomRight;
    @FXML private Region hudStatusDot;
    @FXML private Label hudVersionLabel;
    @FXML private Label hudTimeLabel;
    @FXML private Label hudPhaseLabel;
    @FXML private Label hudPercentLabel;

    // ===================== 状态 =====================
    private final BooleanProperty overLoading = new SimpleBooleanProperty(false);
    private final List<Animation> registered = new ArrayList<Animation>();
    private final List<Particle> particles = new ArrayList<Particle>();
    private final Random random = new Random();
    private AnimationTimer particleTimer;
    private AnimationTimer hudClockTimer;
    private DropShadow logoShadow;

    private String[] stageTexts;
    private String[] hudPhases;

    private static final double PROGRESS_WIDTH = 440;
    private static final double PROGRESS_DURATION_MS = 1700;
    private static final double TOTAL_DURATION_MS = 4200;
    private static final int PARTICLE_COUNT = 72;
    private static final String VERSION = "2.5";
    private static final Interpolator CINEMATIC_EASE = Interpolator.SPLINE(0.16, 1, 0.3, 1);

    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** 供 MainApplication 监听加载完成。 */
    public BooleanProperty loadedProperty() {
        return overLoading;
    }

    // ===================== 初始化 =====================
    public void initialize() {
        loadI18nText();
        applyStaticText();
        prepareInitialState();

        buildAurora();
        buildParticles();
        buildScanLine();
        buildTitle();

        startHudClock();

        Platform.runLater(this::runMasterTimeline);
    }

    private void loadI18nText() {
        stageTexts = new String[] {
                safeI18n("load.stage.boot", "Booting secure kernel"),
                safeI18n("load.stage.poc", "Loading POC database"),
                safeI18n("load.stage.ui", "Calibrating UI engine"),
                safeI18n("load.stage.ready", "System ready")
        };
        hudPhases = new String[] {
                safeI18n("load.hud.boot", "KERNEL_BOOT"),
                safeI18n("load.hud.poc", "POC_DB_LOAD"),
                safeI18n("load.hud.ui", "UI_CALIBRATE"),
                safeI18n("load.hud.ready", "READY_TO_RUN")
        };
    }

    private void applyStaticText() {
        subtitleLine1.setText(safeI18n("load.subtitle.line1", "Red & Blue Team · Security Workbench"));
        String tagTemplate = safeI18n("load.subtitle.tag", "v %s · CINEMATIC BOOT SEQUENCE");
        try {
            subtitleLine2.setText(String.format(tagTemplate, VERSION));
        } catch (Exception ignore) {
            subtitleLine2.setText("v " + VERSION + " · CINEMATIC BOOT SEQUENCE");
        }
        hudVersionLabel.setText("POTATOTOOL // " + VERSION);
        hudPhaseLabel.setText(safeI18n("load.hud.standby", "STAND_BY"));
        hudPercentLabel.setText("0%");
        stageLabel.setText(" ");
    }

    /** 设定初始状态，避免动画开始前出现闪屏。 */
    private void prepareInitialState() {
        bgLayer.setOpacity(0);
        gridLayer.setOpacity(0);
        auroraLayer.setOpacity(0);
        particleLayer.setOpacity(0);
        scanLayer.setOpacity(0);

        glassPanel.setOpacity(0);
        glassPanel.setScaleX(0.92);
        glassPanel.setScaleY(0.92);

        logoHaloOuter.setOpacity(0);
        logoHaloInner.setOpacity(0);
        logoRingOuter.setOpacity(0);
        logoRingInner.setOpacity(0);
        logoCore.setOpacity(0);
        logoImage.setOpacity(0);
        logoImage.setScaleX(0.5);
        logoImage.setScaleY(0.5);
        logoImage.setRotate(-12);

        subtitleLine1.setOpacity(0);
        subtitleLine1.setTranslateY(10);
        subtitleDividerLeft.setOpacity(0);
        subtitleDividerLeft.setScaleX(0.3);
        subtitleDividerRight.setOpacity(0);
        subtitleDividerRight.setScaleX(0.3);
        subtitleDiamond.setOpacity(0);
        subtitleDiamond.setScaleX(0.3);
        subtitleDiamond.setScaleY(0.3);
        subtitleLine2.setOpacity(0);
        subtitleLine2.setTranslateY(8);

        progressContainer.setOpacity(0);
        Rectangle progressClip = new Rectangle(PROGRESS_WIDTH, 35);
        progressClip.setY(-16);
        progressContainer.setClip(progressClip);
        progressFill.setMinWidth(0);
        progressFill.setPrefWidth(0);
        progressFill.setMaxWidth(0);
        progressShine.setOpacity(0);
        progressShine.setTranslateX(-80);

        stageDot.setOpacity(0);
        stageDotMirror.setOpacity(0);
        stageLabel.setOpacity(0);

        hudTopLeft.setOpacity(0);
        hudTopLeft.setTranslateY(-8);
        hudTopRight.setOpacity(0);
        hudTopRight.setTranslateY(-8);
        hudBottomLeft.setOpacity(0);
        hudBottomLeft.setTranslateY(8);
        hudBottomRight.setOpacity(0);
        hudBottomRight.setTranslateY(8);
        hudStatusDot.setOpacity(0);
    }

    // ===================== L2 Aurora 极光 =====================
    private void buildAurora() {
        double w = root.getPrefWidth();
        double h = root.getPrefHeight();

        Region cyan = makeOrb(720, "load-aurora-cyan");
        cyan.setLayoutX(w * 0.05 - 360);
        cyan.setLayoutY(h * 0.10 - 360);

        Region violet = makeOrb(640, "load-aurora-violet");
        violet.setLayoutX(w * 0.85 - 320);
        violet.setLayoutY(h * 0.45 - 320);

        Region rose = makeOrb(560, "load-aurora-rose");
        rose.setLayoutX(w * 0.15 - 280);
        rose.setLayoutY(h * 0.85 - 280);

        auroraLayer.getChildren().addAll(cyan, violet, rose);

        drift(cyan, 60, 40, 7.5);
        drift(violet, -50, 50, 9.0);
        drift(rose, 70, -45, 8.2);
    }

    private Region makeOrb(double size, String styleClass) {
        Region r = new Region();
        r.setMinSize(size, size);
        r.setPrefSize(size, size);
        r.setMaxSize(size, size);
        r.getStyleClass().addAll("load-aurora-orb", styleClass);
        r.setEffect(new GaussianBlur(60));
        r.setMouseTransparent(true);
        r.setPickOnBounds(false);
        return r;
    }

    private void drift(Region node, double dx, double dy, double seconds) {
        TranslateTransition tt = new TranslateTransition(Duration.seconds(seconds), node);
        tt.setFromX(0);
        tt.setToX(dx);
        tt.setFromY(0);
        tt.setToY(dy);
        tt.setAutoReverse(true);
        tt.setCycleCount(Animation.INDEFINITE);
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.play();
        registered.add(tt);
    }

    // ===================== L3 粒子 =====================
    private void buildParticles() {
        double w = root.getPrefWidth();
        double h = root.getPrefHeight();
        String[] variants = {"load-particle-white", "load-particle-cyan", "load-particle-violet"};

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double size = 1.0 + random.nextDouble() * 2.6;
            Region r = new Region();
            r.setMinSize(size, size);
            r.setPrefSize(size, size);
            r.setMaxSize(size, size);
            r.getStyleClass().addAll("load-particle-base", variants[random.nextInt(variants.length)]);
            r.setMouseTransparent(true);
            r.setPickOnBounds(false);

            double x = random.nextDouble() * w;
            double y = random.nextDouble() * h;
            r.setLayoutX(x);
            r.setLayoutY(y);
            double baseOpacity = 0.25 + random.nextDouble() * 0.5;
            r.setOpacity(baseOpacity);

            Particle p = new Particle(r, baseOpacity);
            p.x = x;
            p.y = y;
            p.vx = (random.nextDouble() - 0.5) * 12;
            p.vy = -8 - random.nextDouble() * 18;
            p.phase = random.nextDouble() * Math.PI * 2;
            p.swayMag = 4 + random.nextDouble() * 8;
            p.swaySpeed = 0.6 + random.nextDouble() * 1.1;
            particles.add(p);
            particleLayer.getChildren().add(r);
        }

        final double width = w;
        final double height = h;
        particleTimer = new AnimationTimer() {
            long last = 0;
            @Override
            public void handle(long now) {
                if (last == 0) {
                    last = now;
                    return;
                }
                double dt = (now - last) / 1_000_000_000.0;
                last = now;
                if (dt > 0.1) dt = 0.1;
                for (Particle p : particles) {
                    p.phase += dt * p.swaySpeed;
                    double sway = Math.sin(p.phase) * p.swayMag;
                    p.x += (p.vx + sway) * dt;
                    p.y += p.vy * dt;
                    if (p.y < -12) {
                        p.y = height + 12;
                        p.x = random.nextDouble() * width;
                    }
                    if (p.x < -12) p.x = width + 12;
                    if (p.x > width + 12) p.x = -12;
                    p.node.setLayoutX(p.x);
                    p.node.setLayoutY(p.y);
                    p.node.setOpacity(p.baseOpacity * (0.65 + 0.35 * Math.sin(p.phase * 2)));
                }
            }
        };
        particleTimer.start();
    }

    private static final class Particle {
        final Region node;
        final double baseOpacity;
        double x, y, vx, vy, phase, swayMag, swaySpeed;
        Particle(Region node, double baseOpacity) {
            this.node = node;
            this.baseOpacity = baseOpacity;
        }
    }

    // ===================== L4 扫描线 =====================
    private void buildScanLine() {
        double w = root.getPrefWidth();
        double h = root.getPrefHeight();
        Region line = new Region();
        line.setMinSize(w, 2);
        line.setPrefSize(w, 2);
        line.setMaxSize(w, 2);
        line.getStyleClass().add("load-scan-line");
        line.setMouseTransparent(true);
        line.setPickOnBounds(false);
        line.setLayoutY(-20);
        scanLayer.getChildren().add(line);

        TranslateTransition tt = new TranslateTransition(Duration.seconds(6.5), line);
        tt.setFromY(0);
        tt.setToY(h + 40);
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.setCycleCount(Animation.INDEFINITE);

        PauseTransition pre = new PauseTransition(Duration.millis(700));
        pre.setOnFinished(e -> tt.play());
        pre.play();

        registered.add(tt);
        registered.add(pre);
    }

    // ===================== 标题字符 =====================
    private void buildTitle() {
        String word = "POTATOTOOL";
        // POTATO 用主色白 + 微青阴影；TOOL 用青色强调
        for (int i = 0; i < word.length(); i++) {
            if (i == 6) {
                Region gap = new Region();
                gap.getStyleClass().add("load-title-gap");
                gap.setMinWidth(28);
                gap.setPrefWidth(28);
                gap.setMaxWidth(28);
                titleBox.getChildren().add(gap);
            }
            Label ch = new Label(String.valueOf(word.charAt(i)));
            ch.getStyleClass().add("load-title-char");
            if (i >= 6) {
                ch.getStyleClass().add("load-title-char-accent");
            }
            ch.setOpacity(0);
            ch.setTranslateY(44);
            titleBox.getChildren().add(ch);
        }
    }

    // ===================== HUD 时钟 =====================
    private void startHudClock() {
        hudClockTimer = new AnimationTimer() {
            long lastSec = -1;
            @Override
            public void handle(long now) {
                long sec = now / 1_000_000_000L;
                if (sec != lastSec) {
                    lastSec = sec;
                    try {
                        hudTimeLabel.setText(LocalDateTime.now().format(CLOCK_FORMAT));
                    } catch (Exception ignore) {
                        // 不阻塞动画
                    }
                }
            }
        };
        hudClockTimer.start();
    }

    // ===================== 主时间轴 =====================
    private void runMasterTimeline() {
        fadeIn(bgLayer, 360, 0, 1.0);
        revealHud(160);
        fadeIn(gridLayer, 800, 360, 1.0);
        fadeIn(auroraLayer, 700, 460, 1.0);
        fadeIn(particleLayer, 700, 520, 1.0);
        fadeIn(scanLayer, 600, 720, 1.0);

        revealGlassPanel(640);
        revealLogo(1080);
        revealTitle(1700);
        revealSubtitle(2360);
        runProgress(2560);
        runStages(2620);

        PauseTransition done = new PauseTransition(Duration.millis(TOTAL_DURATION_MS));
        done.setOnFinished(e -> overLoading.set(true));
        done.play();
        registered.add(done);
    }

    // ===================== HUD 入场 =====================
    private void revealHud(double startMs) {
        slideFadeIn(hudTopLeft, 460, startMs, -8);
        slideFadeIn(hudTopRight, 460, startMs + 80, -8);
        slideFadeIn(hudBottomLeft, 460, startMs + 160, 8);
        slideFadeIn(hudBottomRight, 460, startMs + 240, 8);

        PauseTransition dotDelay = new PauseTransition(Duration.millis(startMs + 320));
        dotDelay.setOnFinished(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(360), hudStatusDot);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
            registered.add(ft);

            Timeline blink = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(hudStatusDot.opacityProperty(), 1.0)),
                    new KeyFrame(Duration.seconds(1.1), new KeyValue(hudStatusDot.opacityProperty(), 0.35, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.seconds(2.2), new KeyValue(hudStatusDot.opacityProperty(), 1.0, Interpolator.EASE_BOTH))
            );
            blink.setCycleCount(Animation.INDEFINITE);
            PauseTransition wait = new PauseTransition(Duration.millis(400));
            wait.setOnFinished(ev -> blink.play());
            wait.play();
            registered.add(blink);
            registered.add(wait);
        });
        dotDelay.play();
        registered.add(dotDelay);
    }

    // ===================== 玻璃面板入场 =====================
    private void revealGlassPanel(double startMs) {
        PauseTransition delay = new PauseTransition(Duration.millis(startMs));
        delay.setOnFinished(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(700), glassPanel);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setInterpolator(Interpolator.EASE_OUT);

            ScaleTransition st = new ScaleTransition(Duration.millis(800), glassPanel);
            st.setFromX(0.92);
            st.setFromY(0.92);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setInterpolator(CINEMATIC_EASE);

            ParallelTransition pt = new ParallelTransition(ft, st);
            pt.play();
            registered.add(pt);
        });
        delay.play();
        registered.add(delay);
    }

    // ===================== Logo 揭示 =====================
    private void revealLogo(double startMs) {
        PauseTransition delay = new PauseTransition(Duration.millis(startMs));
        delay.setOnFinished(e -> {
            playFade(logoHaloOuter, 950, 1.0);
            playFade(logoHaloInner, 850, 1.0);
            playFade(logoRingOuter, 800, 1.0);
            playFade(logoRingInner, 850, 1.0);
            playFade(logoCore, 700, 1.0);

            FadeTransition ftLogo = new FadeTransition(Duration.millis(900), logoImage);
            ftLogo.setFromValue(0);
            ftLogo.setToValue(1);
            ftLogo.setInterpolator(Interpolator.EASE_OUT);
            ftLogo.play();

            ScaleTransition st = new ScaleTransition(Duration.millis(950), logoImage);
            st.setFromX(0.5);
            st.setFromY(0.5);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setInterpolator(CINEMATIC_EASE);
            st.play();

            RotateTransition rotIn = new RotateTransition(Duration.millis(950), logoImage);
            rotIn.setFromAngle(-12);
            rotIn.setToAngle(0);
            rotIn.setInterpolator(CINEMATIC_EASE);
            rotIn.play();

            registered.add(ftLogo);
            registered.add(st);
            registered.add(rotIn);

            logoShadow = new DropShadow();
            logoShadow.setColor(Color.web("#3ECEE3"));
            logoShadow.setRadius(20);
            logoShadow.setSpread(0.18);
            logoImage.setEffect(logoShadow);

            Timeline pulse = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(logoShadow.radiusProperty(), 18.0, Interpolator.EASE_BOTH),
                            new KeyValue(logoShadow.spreadProperty(), 0.18, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.seconds(2.2),
                            new KeyValue(logoShadow.radiusProperty(), 38.0, Interpolator.EASE_BOTH),
                            new KeyValue(logoShadow.spreadProperty(), 0.32, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.seconds(4.4),
                            new KeyValue(logoShadow.radiusProperty(), 18.0, Interpolator.EASE_BOTH),
                            new KeyValue(logoShadow.spreadProperty(), 0.18, Interpolator.EASE_BOTH))
            );
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.play();
            registered.add(pulse);

            RotateTransition spinOuter = new RotateTransition(Duration.seconds(22), logoRingOuter);
            spinOuter.setByAngle(360);
            spinOuter.setCycleCount(Animation.INDEFINITE);
            spinOuter.setInterpolator(Interpolator.LINEAR);
            spinOuter.play();

            RotateTransition spinInner = new RotateTransition(Duration.seconds(28), logoRingInner);
            spinInner.setByAngle(-360);
            spinInner.setCycleCount(Animation.INDEFINITE);
            spinInner.setInterpolator(Interpolator.LINEAR);
            spinInner.play();

            registered.add(spinOuter);
            registered.add(spinInner);
        });
        delay.play();
        registered.add(delay);
    }

    private void playFade(Node node, double ms, double to) {
        FadeTransition ft = new FadeTransition(Duration.millis(ms), node);
        ft.setFromValue(0);
        ft.setToValue(to);
        ft.setInterpolator(Interpolator.EASE_OUT);
        ft.play();
        registered.add(ft);
    }

    // ===================== 标题字符入场 =====================
    private void revealTitle(double startMs) {
        List<Node> chars = new ArrayList<Node>();
        for (Node n : titleBox.getChildren()) {
            if (n instanceof Label) chars.add(n);
        }
        int count = chars.size();
        for (int i = 0; i < count; i++) {
            Node ch = chars.get(i);
            FadeTransition ft = new FadeTransition(Duration.millis(420), ch);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition tt = new TranslateTransition(Duration.millis(620), ch);
            tt.setFromY(44);
            tt.setToY(0);
            tt.setInterpolator(CINEMATIC_EASE);

            ParallelTransition pt = new ParallelTransition(ft, tt);
            pt.setDelay(Duration.millis(startMs + i * 62));
            pt.play();
            registered.add(pt);
        }
    }

    // ===================== 副标题入场 =====================
    private void revealSubtitle(double startMs) {
        PauseTransition delay = new PauseTransition(Duration.millis(startMs));
        delay.setOnFinished(e -> {
            slideFadeIn(subtitleLine1, 520, 0, 10);

            PauseTransition dividerDelay = new PauseTransition(Duration.millis(120));
            dividerDelay.setOnFinished(ev -> {
                expand(subtitleDividerLeft, 420);
                expand(subtitleDividerRight, 420);
                popIn(subtitleDiamond, 360);
            });
            dividerDelay.play();
            registered.add(dividerDelay);

            slideFadeIn(subtitleLine2, 520, 220, 8);
        });
        delay.play();
        registered.add(delay);
    }

    private void expand(Region node, double ms) {
        FadeTransition ft = new FadeTransition(Duration.millis(ms), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
        ScaleTransition st = new ScaleTransition(Duration.millis(ms), node);
        st.setFromX(0.3);
        st.setToX(1.0);
        st.setInterpolator(CINEMATIC_EASE);
        st.play();
        registered.add(ft);
        registered.add(st);
    }

    private void popIn(Region node, double ms) {
        FadeTransition ft = new FadeTransition(Duration.millis(ms), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
        ScaleTransition stUp = new ScaleTransition(Duration.millis(ms * 0.62), node);
        stUp.setFromX(0.3);
        stUp.setFromY(0.3);
        stUp.setToX(1.35);
        stUp.setToY(1.35);
        stUp.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition stSettle = new ScaleTransition(Duration.millis(ms * 0.38), node);
        stSettle.setFromX(1.35);
        stSettle.setFromY(1.35);
        stSettle.setToX(1.0);
        stSettle.setToY(1.0);
        stSettle.setInterpolator(Interpolator.EASE_BOTH);

        SequentialTransition st = new SequentialTransition(stUp, stSettle);
        st.play();
        registered.add(ft);
        registered.add(st);
    }

    // ===================== 进度条 =====================
    private void runProgress(double startMs) {
        PauseTransition delay = new PauseTransition(Duration.millis(startMs));
        delay.setOnFinished(e -> {
            FadeTransition containerFade = new FadeTransition(Duration.millis(380), progressContainer);
            containerFade.setFromValue(0);
            containerFade.setToValue(1);
            containerFade.play();
            registered.add(containerFade);

            FadeTransition stageFade = new FadeTransition(Duration.millis(380), stageLabel);
            stageFade.setFromValue(0);
            stageFade.setToValue(1);
            stageFade.play();
            registered.add(stageFade);

            FadeTransition dotFade = new FadeTransition(Duration.millis(380), stageDot);
            dotFade.setFromValue(0);
            dotFade.setToValue(1);
            dotFade.play();
            FadeTransition dotMirrorFade = new FadeTransition(Duration.millis(380), stageDotMirror);
            dotMirrorFade.setFromValue(0);
            dotMirrorFade.setToValue(1);
            dotMirrorFade.play();
            registered.add(dotFade);
            registered.add(dotMirrorFade);

            // 脉冲呼吸
            Timeline pulse = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(stageDot.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                            new KeyValue(stageDot.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.seconds(0.8),
                            new KeyValue(stageDot.scaleXProperty(), 1.5, Interpolator.EASE_BOTH),
                            new KeyValue(stageDot.scaleYProperty(), 1.5, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.seconds(1.6),
                            new KeyValue(stageDot.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                            new KeyValue(stageDot.scaleYProperty(), 1.0, Interpolator.EASE_BOTH))
            );
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.play();
            registered.add(pulse);

            // 进度条填充
            Timeline fill = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(progressFill.minWidthProperty(), 0.0),
                            new KeyValue(progressFill.prefWidthProperty(), 0.0),
                            new KeyValue(progressFill.maxWidthProperty(), 0.0)),
                    new KeyFrame(Duration.millis(PROGRESS_DURATION_MS),
                            new KeyValue(progressFill.minWidthProperty(), PROGRESS_WIDTH, CINEMATIC_EASE),
                            new KeyValue(progressFill.prefWidthProperty(), PROGRESS_WIDTH, CINEMATIC_EASE),
                            new KeyValue(progressFill.maxWidthProperty(), PROGRESS_WIDTH, CINEMATIC_EASE))
            );
            fill.play();
            registered.add(fill);

            // shine 光点横扫
            FadeTransition shineFade = new FadeTransition(Duration.millis(280), progressShine);
            shineFade.setFromValue(0);
            shineFade.setToValue(0.9);
            shineFade.play();
            registered.add(shineFade);

            TranslateTransition shine = new TranslateTransition(Duration.millis(1200), progressShine);
            shine.setFromX(-80);
            shine.setToX(PROGRESS_WIDTH);
            shine.setCycleCount(Animation.INDEFINITE);
            shine.setInterpolator(Interpolator.EASE_BOTH);
            shine.play();
            registered.add(shine);

            // 百分比数字
            IntegerProperty pct = new SimpleIntegerProperty(0);
            pct.addListener((obs, ov, nv) -> hudPercentLabel.setText(nv.intValue() + "%"));
            Timeline pctTl = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(pct, 0)),
                    new KeyFrame(Duration.millis(PROGRESS_DURATION_MS),
                            new KeyValue(pct, 100, CINEMATIC_EASE))
            );
            pctTl.play();
            registered.add(pctTl);
        });
        delay.play();
        registered.add(delay);
    }

    // ===================== 阶段文本 =====================
    private void runStages(double startMs) {
        PauseTransition delay = new PauseTransition(Duration.millis(startMs));
        delay.setOnFinished(e -> {
            SequentialTransition seq = new SequentialTransition();
            int n = Math.min(stageTexts.length, hudPhases.length);
            for (int i = 0; i < n; i++) {
                final int idx = i;
                PauseTransition gap = new PauseTransition(Duration.millis(i == 0 ? 0 : 360));
                gap.setOnFinished(ev -> {
                    if (idx == 0) {
                        stageLabel.setText(stageTexts[idx]);
                        stageLabel.setOpacity(1);
                        hudPhaseLabel.setText(hudPhases[idx]);
                        hudPhaseLabel.setOpacity(1);
                    } else {
                        fadeText(stageLabel, stageTexts[idx]);
                        fadeText(hudPhaseLabel, hudPhases[idx]);
                    }
                });
                seq.getChildren().add(gap);
            }
            seq.play();
            registered.add(seq);
        });
        delay.play();
        registered.add(delay);
    }

    // ===================== 工具方法 =====================
    private void fadeIn(Node node, double ms, double delayMs, double to) {
        FadeTransition ft = new FadeTransition(Duration.millis(ms), node);
        ft.setFromValue(0);
        ft.setToValue(to);
        ft.setDelay(Duration.millis(delayMs));
        ft.setInterpolator(Interpolator.EASE_OUT);
        ft.play();
        registered.add(ft);
    }

    private void slideFadeIn(Node node, double ms, double delayMs, double fromY) {
        FadeTransition ft = new FadeTransition(Duration.millis(ms), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setDelay(Duration.millis(delayMs));
        ft.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition tt = new TranslateTransition(Duration.millis(ms), node);
        tt.setFromY(fromY);
        tt.setToY(0);
        tt.setDelay(Duration.millis(delayMs));
        tt.setInterpolator(CINEMATIC_EASE);

        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.play();
        registered.add(pt);
    }

    private void fadeText(Label label, String newText) {
        if (label == null || newText == null) return;
        FadeTransition out = new FadeTransition(Duration.millis(110), label);
        out.setFromValue(label.getOpacity());
        out.setToValue(0);
        out.setOnFinished(ev -> {
            label.setText(newText);
            FadeTransition in = new FadeTransition(Duration.millis(180), label);
            in.setFromValue(0);
            in.setToValue(1);
            in.play();
            registered.add(in);
        });
        out.play();
        registered.add(out);
    }

    private static String safeI18n(String key, String fallback) {
        try {
            String v = I18nUtils.getString(key);
            if (v == null || v.isEmpty() || v.equals(key)) {
                return fallback;
            }
            return v;
        } catch (Exception ignore) {
            return fallback;
        }
    }

    // ===================== 资源回收 =====================
    public void stopAnimations() {
        for (Animation a : registered) {
            try {
                a.stop();
            } catch (Exception ignore) {
            }
        }
        registered.clear();

        if (particleTimer != null) {
            try { particleTimer.stop(); } catch (Exception ignore) {}
            particleTimer = null;
        }
        if (hudClockTimer != null) {
            try { hudClockTimer.stop(); } catch (Exception ignore) {}
            hudClockTimer = null;
        }
        particles.clear();
    }
}
