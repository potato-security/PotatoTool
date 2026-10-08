package com.potato.potatotool.controller.blueTeam;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.leewyatt.rxcontrols.controls.RXLineButton;
import com.potato.potatotool.ToStart;
import com.potato.potatotool.content.blueTeam.ReadPacketFile;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.WebShellDecryptService;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.model.DecryptConfig;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.model.DecryptResult;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.report.WebshellAnalysisWordReportGenerator;
import com.potato.potatotool.utils.ai.CodeAnalyzerUtils;
import com.potato.potatotool.utils.ai.service.AiChatService;
import com.potato.potatotool.utils.core.ExecutorServiceManager;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.ui.CodeHighlightingAsync;
import com.potato.potatotool.utils.ui.DefaultContextMenu;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.core.Constants.*;


/**
 * @author Potato
 * @date 2023/10/7 16:46
 */
public class PaneWebshellDecode {
    private static final String WEBSHELL_RESULT_PREVIEW_INPUT =
            "cGlwZV9kZWNvZGU9JGFfPVJFUVVFU1RbJ2EnXTskYj1iYXNlNjRfZGVjb2RlKCRhXyk7ZXZhbCgkYik7";
    private static final String WEBSHELL_RESULT_PREVIEW_OUTPUT =
            "<%@ page language=\"java\" pageEncoding=\"UTF-8\" %>\n" +
            "<%\n" +
            "String cmd = request.getParameter(\"cmd\");\n" +
            "if (cmd != null && cmd.length() > 0) {\n" +
            "    Process process = Runtime.getRuntime().exec(cmd);\n" +
            "    java.io.InputStream in = process.getInputStream();\n" +
            "    byte[] buffer = new byte[1024];\n" +
            "    int len;\n" +
            "    while ((len = in.read(buffer)) != -1) {\n" +
            "        out.print(new String(buffer, 0, len, \"UTF-8\"));\n" +
            "    }\n" +
            "}\n" +
            "%>";
    private static final String WEBSHELL_OPTION_PREVIEW_KEY = "rebeyond";
    private static final String WEBSHELL_OPTION_PREVIEW_IV = "5c6e0c2a9f4d1b3e";
    private static final String WEBSHELL_AI_PREVIEW_TEXT =
            "1. 识别到典型 Base64 + 默认密钥链路，解密结果为 JSP 命令执行逻辑。\n" +
            "2. 核心风险点在于直接读取 cmd 参数并调用 Runtime.exec 执行系统命令。\n" +
            "3. 建议优先结合访问日志、Web 容器进程与异常子进程记录进一步确认落地痕迹。";

    @FXML
    private StackPane sPane;

    @FXML
    private ListView testDataList;

    @FXML
    private TextArea inputText;

    @FXML
    private Label tipTitle;
    @FXML
    private Button tipTitleCopy;
    @FXML
    private ComboBox rulesComboBox;
    @FXML
    private ComboBox modeComboBox;
    @FXML
    private TextField customKey;
    @FXML
    private TextField customIv;
    @FXML
    private TextField customPath;

    @FXML
    private CodeArea result;
    @FXML
    private VirtualizedScrollPane virScrollPane;
    @FXML
    private TextArea aiTextArea;
    @FXML
    private Label aiStatusLabel;
    @FXML
    private Region aiStatusDivider;
    @FXML
    private Pane aiPane;
    @FXML
    private Button showAI;
    @FXML
    private Button downloadAI;
    @FXML
    private Button refreshAI;

    @FXML
    private Pane promptPane;
    @FXML
    private Label prompt;

    Map<String, Object> res = new HashMap<>();

    private static final double RESIZE_MARGIN = 10;
    private static final double AI_STATUS_INSET = 14;
    private static final double AI_STATUS_RESERVED_WIDTH = 120;
    private static final double AI_PANEL_WIDTH_RATIO = 0.8;
    private static final double AI_STALE_OPACITY = 0.45;

    private double aiTextAreaStartX, aiTextAreaStartY;
    private DoubleProperty aiTextAreWidthProperty = new SimpleDoubleProperty(0);
    private DoubleProperty aiTextAreHeightProperty = new SimpleDoubleProperty(0);
    private final WebshellAnalysisWordReportGenerator reportGenerator = new WebshellAnalysisWordReportGenerator();
    private FadeTransition promptFadeIn;
    private FadeTransition promptFadeOut;

    private enum PromptTone {
        INFO,
        SUCCESS,
        ERROR
    }

    private enum AiPanelState {
        NO_SOURCE,
        ANALYZING,
        DONE,
        STALE,
        FAILED
    }

    /**
     * 面板内容对应的“内容指纹”：解密结果文本 + 识别出的加解密链路。
     * 提示词由这两者决定，任一变化都必须重新分析。
     */
    private static final class AiSourceKey {
        private final String resultText;
        private final String encodeModes;

        private AiSourceKey(String resultText, String encodeModes) {
            this.resultText = resultText == null ? "" : resultText;
            this.encodeModes = encodeModes == null ? "" : encodeModes;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AiSourceKey)) {
                return false;
            }
            AiSourceKey other = (AiSourceKey) obj;
            return resultText.equals(other.resultText) && encodeModes.equals(other.encodeModes);
        }

        @Override
        public int hashCode() {
            return 31 * resultText.hashCode() + encodeModes.hashCode();
        }
    }

    /**
     * 分析发起时的输入快照。报告必须使用与面板内容同一代的快照，
     * 否则会出现“AI 正文来自 A、原始数据来自 B”的错配。
     */
    private static final class AiReportSnapshot {
        private final String sourceInput;
        private final String sourceResult;
        private final String ruleSummary;
        private final List<String> schemeLines;
        private final Date capturedAt;

        private AiReportSnapshot(String sourceInput,
                                 String sourceResult,
                                 String ruleSummary,
                                 List<String> schemeLines,
                                 Date capturedAt) {
            this.sourceInput = sourceInput;
            this.sourceResult = sourceResult;
            this.ruleSummary = ruleSummary;
            this.schemeLines = schemeLines;
            this.capturedAt = capturedAt;
        }
    }

    /**
     * 一次 AI 分析。会话自带代次与取消标记，既是流式写入的有效性判定，
     * 也用于丢弃已经排队、但属于旧会话的写入分片。
     */
    private final class AiSession implements CodeAnalyzerUtils.StreamGate {
        private final long token;
        private final AiSourceKey key;
        private final AiReportSnapshot snapshot;
        private final AiChatService service = new AiChatService();
        private final CodeAnalyzerUtils.StreamAppender appender;
        private volatile boolean cancelled;
        private volatile boolean succeeded;

        private AiSession(long token, AiSourceKey key, AiReportSnapshot snapshot) {
            this.token = token;
            this.key = key;
            this.snapshot = snapshot;
            this.appender = CodeAnalyzerUtils.createStreamAppender(aiTextArea, this);
        }

        @Override
        public boolean isActive() {
            return !cancelled && token == aiActiveToken;
        }

        private void cancel() {
            cancelled = true;
            try {
                service.cancelActiveStream();
            } catch (Throwable ignore) {
                // 取消上游连接是尽量而为：连接已被读线程关闭时可能抛异常，
                // 这里必须吞掉，否则会打断调用方（例如一键解密的入口流程）
            }
            try {
                appender.dispose();
            } catch (Throwable ignore) {
                // 同上，释放监听失败不影响业务
            }
        }
    }

    private AiSession activeSession;
    private AiSourceKey analyzedKey;
    private AiReportSnapshot analyzedSnapshot;
    private AiSourceKey attemptedKey;
    private boolean lastRunFailed;
    private long aiTokenSeq;
    private volatile long aiActiveToken;
    private boolean aiPanelOpen;
    private boolean decryptRunning;
    private long decryptTokenSeq;
    private long activeDecryptToken;

    @FXML
    void initialize() throws IOException {
        new CodeHighlightingAsync().codeHighlighting(result);

        initTextData();


//        // 定期监控任务，延迟0秒后开始，每隔5秒执行一次
//        ScheduledExecutorService monitorExecutor = Executors.newSingleThreadScheduledExecutor();
//        monitorExecutor.scheduleAtFixedRate(() -> monitorExecutorStatus(ExecutorServiceManager.getInstance().getExecutor()), 0, 5, TimeUnit.SECONDS);

        tipTitle.setCursor(Cursor.HAND);

        aiTextArea.prefWidthProperty().bind(aiTextAreWidthProperty);
        aiTextArea.layoutXProperty().bind(aiTextAreWidthProperty.negate());
        aiStatusLabel.layoutXProperty().bind(aiTextAreWidthProperty.negate().add(AI_STATUS_INSET));
        aiStatusLabel.maxWidthProperty().bind(Bindings.max(0,
                aiTextAreWidthProperty.subtract(AI_STATUS_INSET + AI_STATUS_RESERVED_WIDTH)));
        aiStatusDivider.layoutXProperty().bind(aiTextAreWidthProperty.negate());
        aiStatusDivider.prefWidthProperty().bind(aiTextAreWidthProperty);

        //  CodeArea添加宽高自适应
        sPane.widthProperty().addListener((obs, oldValue, newValue) -> {
            double prefWidth = newValue.doubleValue();
            virScrollPane.setMaxWidth(prefWidth);   //setMinWidth存在bug
        });
        sPane.heightProperty().addListener((obs, oldValue, newValue) -> {
            double prefHeight = newValue.doubleValue() * 0.43;
            virScrollPane.setMinHeight(prefHeight);

            aiTextAreHeightProperty = new SimpleDoubleProperty(prefHeight + 12);
            aiTextArea.prefHeightProperty().bind(aiTextAreHeightProperty);
        });

        //  CodeArea添加右键菜单
        result.setContextMenu(new DefaultContextMenu());

        aiTextArea.setOnMouseMoved(event -> {
            if (isInLeftResizeZone(aiTextArea, event)) {
                aiTextArea.setCursor(Cursor.H_RESIZE);
            } else if (isInBottomResizeZone(aiTextArea, event)) {
                aiTextArea.setCursor(Cursor.V_RESIZE);
            } else {
                aiTextArea.setCursor(Cursor.DEFAULT);
            }
        });

        aiTextArea.setOnMousePressed(event -> {
            aiTextAreaStartX = event.getSceneX();
            aiTextAreaStartY = event.getSceneY();
        });
        aiTextArea.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - aiTextAreaStartX;
            double deltaY = event.getSceneY() - aiTextAreaStartY;

            double newX = aiTextAreWidthProperty.get() - deltaX;
            double newY = aiTextAreHeightProperty.get() + deltaY;

            if( newX > 0 && newX < sPane.getPrefWidth() * 0.9 ){
                aiTextAreWidthProperty.set(newX);
                aiTextAreHeightProperty.set(newY);
            }

            aiTextAreaStartX = event.getSceneX();
            aiTextAreaStartY = event.getSceneY();
        });

        initAiControls();

        //  设置默认第一个选项
        rulesComboBox.getSelectionModel().selectFirst();
        modeComboBox.getSelectionModel().selectFirst();
        
        // 绑定国际化
        Platform.runLater(() -> {
            I18nUtils.bindComponents(sPane);
            applyStartupPreviewState();
        });
        //  状态条文案由状态派生，切语言时重新渲染一次
        I18nUtils.localeProperty().addListener((obs, oldLocale, newLocale) -> refreshAiUi());
    }

    private void applyStartupPreviewState() {
        ToStart.StartupPage startupPage = ToStart.getStartupTestPage();
        if (startupPage == null) {
            return;
        }

        if (startupPage == ToStart.StartupPage.BLUE_WEBSHELL_DECODE_RULES) {
            rulesComboBox.getSelectionModel().select(2);
            rulesComboChoose(null);
            inputText.setText(WEBSHELL_RESULT_PREVIEW_INPUT);
            return;
        }

        if (startupPage == ToStart.StartupPage.BLUE_WEBSHELL_DECODE_OPTIONS) {
            rulesComboBox.getSelectionModel().select(1);
            rulesComboChoose(null);
            inputText.setText(WEBSHELL_RESULT_PREVIEW_INPUT);
            customKey.setText(WEBSHELL_OPTION_PREVIEW_KEY);
            customIv.setText(WEBSHELL_OPTION_PREVIEW_IV);
            return;
        }

        if (startupPage == ToStart.StartupPage.BLUE_WEBSHELL_DECODE_RESULT
                || startupPage == ToStart.StartupPage.BLUE_WEBSHELL_DECODE_AI) {
            rulesComboBox.getSelectionModel().selectFirst();
            modeComboBox.getSelectionModel().selectFirst();
            inputText.setText(WEBSHELL_RESULT_PREVIEW_INPUT);
            result.replaceText(WEBSHELL_RESULT_PREVIEW_OUTPUT);
            tipTitle.setText(I18nUtils.getString("webshell.auto.recognize", "[Base64, DefaultKey]"));
            Tooltip tooltip = new Tooltip(tipTitle.getText());
            Tooltip.install(tipTitle, tooltip);
            tipTitle.setVisible(true);
            tipTitle.setManaged(true);
            res.put("error", 0);
            res.put("encodeModeList", Collections.singletonList(Collections.singletonList("Base64 -> DefaultKey")));
            if (startupPage == ToStart.StartupPage.BLUE_WEBSHELL_DECODE_AI) {
                applyAiPreviewState();
            } else {
                refreshAiUi();
            }
        }
    }

    private void applyAiPreviewState() {
        aiPanelOpen = true;
        aiPane.setVisible(true);
        aiTextAreWidthProperty.set(Math.max(0, sPane.getPrefWidth() * AI_PANEL_WIDTH_RATIO));
        aiTextArea.setText(WEBSHELL_AI_PREVIEW_TEXT);
        showAI.setStyle("-fx-background-color: #87CEFA");
        analyzedKey = currentKey();
        analyzedSnapshot = buildAiSnapshot();
        attemptedKey = null;
        lastRunFailed = false;
        refreshAiUi();
    }

    private final ObservableList<Node> contentObj = FXCollections.observableArrayList();
    private void initTextData() {

        String testDataJsonStr = getResourceString("testData");
        testDataJsonStr = StrUtils.ROT13Decode(testDataJsonStr);
        JsonArray jsonObject = (JsonArray) (new Gson()).fromJson(testDataJsonStr, JsonObject.class).get("webshellDecode");

        for (int i = 0; i < jsonObject.size(); i++) {
            JsonObject tmpDictData = jsonObject.get(i).getAsJsonObject();

            String data = tmpDictData.get("data").getAsString();
            String mode = tmpDictData.get("mode").getAsString();

            final int index = i + 1;
            
            RXLineButton rxLineButton = new RXLineButton();
            rxLineButton.setOnMouseClicked(event -> writeTestData(data));
            rxLineButton.setPrefWidth(100);
            rxLineButton.setPrefHeight(40);
            rxLineButton.setSpacing(3);
            // 使用便捷方法绑定国际化，支持动态语言切换
            I18nUtils.bindTextWithSuffix(rxLineButton, "app.sample.data", String.valueOf(index));
            Tooltip tooltip = new Tooltip(mode);
            Tooltip.install(rxLineButton, tooltip);

            contentObj.add(rxLineButton);

        }

        testDataList.setItems(contentObj);

    }

    private boolean isInLeftResizeZone(TextArea textArea, MouseEvent event) {
        return event.getX() < RESIZE_MARGIN;
    }

    private boolean isInBottomResizeZone(TextArea textArea, MouseEvent event) {
        return event.getY() > (textArea.getHeight() - RESIZE_MARGIN);
    }


    private void initAiControls() {
        downloadAI.setGraphic(createAiButtonIcon(MaterialDesign.MDI_DOWNLOAD));
        refreshAiUi();
    }

    private FontIcon createAiButtonIcon(MaterialDesign iconCode) {
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(20);
        icon.setIconColor(Color.web("#D9F6FF"));
        return icon;
    }

    /**
     * 当前待分析内容的指纹。只有“解密成功且结果非空”时才存在指纹，
     * 解密中、解密失败、流量包模式、结果为空都视为无内容可分析。
     */
    private AiSourceKey currentKey() {
        if (decryptRunning || res.isEmpty()) {
            return null;
        }
        Object errorFlag = res.get("error");
        if (!(errorFlag instanceof Integer) || ((Integer) errorFlag).intValue() != 0) {
            return null;
        }
        String resultStr = result.getText();
        if (resultStr == null || resultStr.trim().isEmpty()) {
            return null;
        }
        Object encodeModes = res.get("encodeModeList");
        return new AiSourceKey(resultStr, encodeModes == null ? "" : encodeModes.toString());
    }

    private AiPanelState deriveAiPanelState() {
        if (activeSession != null) {
            return AiPanelState.ANALYZING;
        }
        AiSourceKey key = currentKey();
        if (key == null) {
            return AiPanelState.NO_SOURCE;
        }
        if (lastRunFailed && key.equals(attemptedKey)) {
            return AiPanelState.FAILED;
        }
        if (key.equals(analyzedKey)) {
            return AiPanelState.DONE;
        }
        return AiPanelState.STALE;
    }

    /**
     * 面板文本只由 AI 会话写入，其它事件（无内容、解密失败、内容已更新）
     * 一律只更新状态条，避免把已有分析覆盖掉。
     */
    private void refreshAiUi() {
        AiPanelState state = deriveAiPanelState();
        if (aiStatusLabel != null) {
            aiStatusLabel.setText(resolveAiStatusText(state));
            aiStatusLabel.getStyleClass().removeAll("ai-status-warn");
            boolean warning = state == AiPanelState.STALE || state == AiPanelState.FAILED;
            if (warning) {
                aiStatusLabel.getStyleClass().add("ai-status-warn");
            }
        }
        refreshAI.setDisable(state == AiPanelState.NO_SOURCE);
        downloadAI.setDisable(state != AiPanelState.DONE);
        // 解密期间面板里的旧分析已不对应当前内容，置灰避免被误读（解密结束由这里恢复）
        boolean dimmed = decryptRunning && !aiTextArea.getText().isEmpty();
        aiTextArea.setOpacity(dimmed ? AI_STALE_OPACITY : 1.0);
    }

    private String resolveAiStatusText(AiPanelState state) {
        if (decryptRunning) {
            return I18nUtils.getString("webshell.decrypting");
        }
        switch (state) {
            case ANALYZING:
                return I18nUtils.getString("webshell.ai.analyzing");
            case FAILED:
                return I18nUtils.getString("ai.status.error");
            case NO_SOURCE:
                return I18nUtils.getString("webshell.ai.detect.empty");
            case STALE:
                return I18nUtils.getString("webshell.ai.status.stale");
            case DONE:
            default:
                return I18nUtils.getString("ai.status.completed");
        }
    }

    private AiReportSnapshot buildAiSnapshot() {
        return new AiReportSnapshot(
                inputText.getText(),
                result.getText(),
                resolveDecryptRuleSummary(),
                collectSchemeLines(),
                new Date()
        );
    }

    private boolean isCurrentSession(AiSession session) {
        return session != null && session == activeSession && session.isActive();
    }

    /**
     * 当前内容已确定无法分析（解密失败、解密结果为空、流量包模式）时，
     * 面板里上一份分析不再对应任何可见内容，必须丢弃，
     * 否则会出现"上一轮 AI 输出看起来像这次输入的分析结果"。
     */
    private void discardAiAnalysis() {
        boolean empty = analyzedKey == null && analyzedSnapshot == null && aiTextArea.getText().isEmpty();
        if (empty) {
            return;
        }
        analyzedKey = null;
        analyzedSnapshot = null;
        aiTextArea.clear();
        refreshAiUi();
    }

    private void cancelActiveSession() {
        AiSession session = activeSession;
        if (session == null) {
            return;
        }
        activeSession = null;
        session.cancel();
    }

    private void startAnalysis(AiSourceKey key) {
        cancelActiveSession();

        AiSession session = new AiSession(++aiTokenSeq, key, buildAiSnapshot());
        activeSession = session;
        aiActiveToken = session.token;
        session.appender.setText(I18nUtils.getString("webshell.ai.analyzing"));
        refreshAiUi();

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() {
                session.succeeded = CodeAnalyzerUtils.streamEvilCodeAnalysis(
                        key.resultText,
                        key.encodeModes,
                        session.service,
                        session.appender,
                        session
                );
                return null;
            }
        };
        task.setOnSucceeded(event -> finishAnalysis(session));
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            if (debugMode && error != null) {
                error.printStackTrace();
            }
            if (!isCurrentSession(session)) {
                return;
            }
            String message = error == null || error.getMessage() == null || error.getMessage().trim().isEmpty()
                    ? I18nUtils.getString("ai.status.error")
                    : error.getMessage();
            session.appender.setText(message);
            lastRunFailed = true;
            attemptedKey = session.key;
            releaseSession(session);
        });
        Thread aiTaskThread = new Thread(task);
        aiTaskThread.setDaemon(true);
        aiTaskThread.start();
    }

    private void finishAnalysis(AiSession session) {
        if (!isCurrentSession(session)) {
            return;
        }
        lastRunFailed = !session.succeeded;
        attemptedKey = session.key;
        if (session.succeeded) {
            analyzedKey = session.key;
            analyzedSnapshot = session.snapshot;
        }
        releaseSession(session);
    }

    private void releaseSession(AiSession session) {
        if (activeSession == session) {
            activeSession = null;
        }
        session.appender.dispose();
        refreshAiUi();
    }

    /**
     * 面板打开、或解密产出新结果时调用：决定复用已有分析、还是按新内容重新分析。
     */
    private void syncAiWithCurrentResult() {
        AiSourceKey key = currentKey();
        if (activeSession != null && key != null && activeSession.key.equals(key)) {
            refreshAiUi();
            return;
        }
        if (key == null) {
            refreshAiUi();
            return;
        }
        if (activeSession != null) {
            cancelActiveSession();
        }
        if (key.equals(analyzedKey)) {
            refreshAiUi();
            return;
        }
        if (lastRunFailed && key.equals(attemptedKey)) {
            refreshAiUi();
            return;
        }
        startAnalysis(key);
    }

    @FXML
    void showAI(ActionEvent e) {
        boolean opening = !aiPanelOpen;
        setAiPanelVisible(opening);
        if (opening) {
            syncAiWithCurrentResult();
        }
    }

    private void setAiPanelVisible(boolean visible) {
        if (visible == aiPanelOpen) {
            return;
        }
        aiPanelOpen = visible;
        double targetWidth = visible ? sPane.getPrefWidth() * AI_PANEL_WIDTH_RATIO : 0;

        if (visible) {
            aiPane.setVisible(true);
            showAI.setStyle("-fx-background-color: #87CEFA");
        } else {
            showAI.setStyle("-fx-background-color: transparent");
        }
        if (aiStatusLabel != null) {
            // 关闭动画期间面板宽度趋近 0，状态条会漂到右边缘外，直接隐藏避免残影
            aiStatusLabel.setVisible(visible);
        }

        Timeline animation = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(aiTextAreWidthProperty, aiTextAreWidthProperty.get())),
                new KeyFrame(Duration.seconds(0.2), new KeyValue(aiTextAreWidthProperty, targetWidth))
        );

        animation.setOnFinished(event -> {
            if (!aiPanelOpen) {
                aiPane.setVisible(false);
            }
        });

        animation.play();
        refreshAiUi();
    }

    @FXML
    void refreshAI(ActionEvent e) {
        if (!aiPanelOpen) {
            setAiPanelVisible(true);
        }
        AiSourceKey key = currentKey();
        if (key == null) {
            refreshAiUi();
            return;
        }
        startAnalysis(key);
    }

    @FXML
    void downloadAIReport(ActionEvent event) {
        if (deriveAiPanelState() != AiPanelState.DONE) {
            showPrompt(I18nUtils.getString("webshell.report.not.ready"));
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18nUtils.getString("webshell.report.dialog.title"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18nUtils.getString("webshell.report.filetype"), "*.docx")
        );

        File defaultDir = new File(StrUtils.getCurrentJarDir(), "WebshellReports");
        if (!defaultDir.exists()) {
            defaultDir.mkdirs();
        }
        if (defaultDir.exists() && defaultDir.isDirectory()) {
            chooser.setInitialDirectory(defaultDir);
        }
        chooser.setInitialFileName("webshell_ai_report_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".docx");

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedFile = chooser.showSaveDialog(stage);
        if (selectedFile == null) {
            return;
        }

        if (!selectedFile.getName().toLowerCase().endsWith(".docx")) {
            selectedFile = new File(selectedFile.getParentFile(), selectedFile.getName() + ".docx");
        }

        final File targetFile = selectedFile;
        final WebshellAnalysisWordReportGenerator.ReportData reportData = buildReportData();

        downloadAI.setDisable(true);
        Task<File> exportTask = new Task<File>() {
            @Override
            protected File call() throws Exception {
                return reportGenerator.generate(reportData, targetFile.getAbsolutePath());
            }
        };
        exportTask.setOnSucceeded(e -> {
            refreshAiUi();
            File exportedFile = exportTask.getValue();
            showSuccessPrompt(I18nUtils.getString("webshell.report.export.success.named",
                    exportedFile.getName(), exportedFile.getAbsolutePath()));
        });
        exportTask.setOnFailed(e -> {
            refreshAiUi();
            Throwable error = exportTask.getException();
            if (debugMode && error != null) {
                error.printStackTrace();
            }
            showErrorPrompt(I18nUtils.getString("webshell.report.export.failed",
                    error == null ? I18nUtils.getString("app.unknown") : error.getMessage()));
        });
        Thread exportTaskThread = new Thread(exportTask);
        exportTaskThread.setDaemon(true);
        exportTaskThread.start();
    }

    private WebshellAnalysisWordReportGenerator.ReportData buildReportData() {
        AiReportSnapshot snapshot = analyzedSnapshot;
        if (snapshot == null) {
            snapshot = buildAiSnapshot();
        }
        return new WebshellAnalysisWordReportGenerator.ReportData(
                snapshot.sourceInput,
                snapshot.sourceResult,
                aiTextArea.getText(),
                snapshot.ruleSummary,
                snapshot.schemeLines,
                snapshot.capturedAt
        );
    }

    private String resolveDecryptRuleSummary() {
        String rule = getComboBoxSelection(rulesComboBox);
        String mode = (modeComboBox.isManaged() || modeComboBox.isVisible()) ? getComboBoxSelection(modeComboBox) : "";

        if (rule == null || rule.trim().isEmpty()) {
            return mode == null ? "" : mode;
        }
        if (mode == null || mode.trim().isEmpty()) {
            return rule;
        }
        return rule + " / " + mode;
    }

    private String getComboBoxSelection(ComboBox comboBox) {
        Object value = comboBox == null ? null : comboBox.getSelectionModel().getSelectedItem();
        return value == null ? "" : value.toString();
    }

    private List<String> collectSchemeLines() {
        Object encodeModes = res.get("encodeModeList");
        if (!(encodeModes instanceof List)) {
            return Collections.singletonList(I18nUtils.getString("webshell.report.none"));
        }

        List<String> lines = new ArrayList<>();
        List outerList = (List) encodeModes;
        int index = 1;
        for (Object item : outerList) {
            if (item instanceof List) {
                List innerList = (List) item;
                List<String> parts = new ArrayList<>();
                for (Object part : innerList) {
                    if (part != null && !part.toString().trim().isEmpty()) {
                        parts.add(part.toString().trim());
                    }
                }
                if (!parts.isEmpty()) {
                    lines.add(I18nUtils.getString("webshell.report.scheme.item",
                            index++, String.join(" -> ", parts)));
                }
            } else if (item != null && !item.toString().trim().isEmpty()) {
                lines.add(I18nUtils.getString("webshell.report.scheme.item",
                        index++, item.toString().trim()));
            }
        }

        if (lines.isEmpty()) {
            lines.add(I18nUtils.getString("webshell.report.none"));
        }
        return lines;
    }

    // 检查内容及模式
    boolean checkContent(){
        String Content = inputText.getText();

        if(Content.length() < 1){
            return false;
        }else{
            return true;
        }
    }

    // 输出进程池数
    private static void monitorExecutorStatus(ExecutorService executor) {
        if (executor instanceof ForkJoinPool) {
            // CPU 密集型任务
            ForkJoinPool forkJoinPool = (ForkJoinPool) executor;
            int runningThreadCount = forkJoinPool.getRunningThreadCount();
            int activeThreadCount = forkJoinPool.getActiveThreadCount();
            long queuedTaskCount = forkJoinPool.getQueuedTaskCount();
            long queuedSubmissionCount = forkJoinPool.getQueuedSubmissionCount();
            int poolSize = forkJoinPool.getPoolSize();
            int parallelism = forkJoinPool.getParallelism();
            int activeTaskCount = forkJoinPool.getActiveThreadCount();

            System.out.println("==== ExecutorService 状态 ====");
            System.out.println("运行中的线程数: " + runningThreadCount);
            System.out.println("活跃线程数: " + activeThreadCount);
            System.out.println("已排队的任务数: " + queuedTaskCount);
            System.out.println("已排队的提交数: " + queuedSubmissionCount);
            System.out.println("线程池大小: " + poolSize);
            System.out.println("并行数: " + parallelism);
            System.out.println("活跃任务数: " + activeTaskCount);
            System.out.println("============================");
        } else if (executor instanceof ThreadPoolExecutor) {
            //  IO 密集型任务
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
            System.out.println("==== ThreadPoolExecutor 状态 ====");
            System.out.println("核心线程数: " + threadPoolExecutor.getCorePoolSize());
            System.out.println("最大线程数: " + threadPoolExecutor.getMaximumPoolSize());
            System.out.println("当前线程数: " + threadPoolExecutor.getPoolSize());
            System.out.println("活跃线程数: " + threadPoolExecutor.getActiveCount());
            System.out.println("已完成任务数: " + threadPoolExecutor.getCompletedTaskCount());
            System.out.println("任务队列大小: " + threadPoolExecutor.getQueue().size());
            System.out.println("==============================");
        } else {
            System.out.println("提供的 executor 不是 ForkJoinPool 或 ThreadPoolExecutor 的实例。");
        }
    }

    private Task<Void> currentTask;
    private Thread currentThread;

    String inputKey = null;
    String inputIv = null;
    List<String> traverse = new ArrayList<>();
    String filePath = null;
    @FXML
    void toDecode(ActionEvent event) throws Exception {
        if(!checkContent()) return;

        final long decryptToken = ++decryptTokenSeq;
        activeDecryptToken = decryptToken;
        decryptRunning = true;
        cancelActiveSession();
        res.clear();
        refreshAiUi();
        traverse.clear();
        ExecutorServiceManager.shutdownExecutor(ExecutorServiceManager.ExecutorPoolNames.DECRYPT_ARRAY);

        if (currentTask != null && !currentTask.isDone()) {
            currentThread.stop();
        }

        tipTitle.setText("");
        tipTitle.setVisible(false);
        tipTitle.setManaged(false);

        //  配置参数
        int selectedIndex = rulesComboBox.getSelectionModel().getSelectedIndex();
        int modeIndex = modeComboBox.getSelectionModel().getSelectedIndex();

        if(modeIndex == 0){
            traverse.add("AES");
        }else if(modeIndex == 1){
            traverse.add("DES");
        }else if(modeIndex == 2){
            traverse.add("XOR");
        }else if(modeIndex == 3){
            traverse.add("AES");
            traverse.add("DES");
            traverse.add("XOR");
        }

        if(selectedIndex == 0){
            inputKey = null;
            inputIv = null;
            traverse.clear();
        }else if(selectedIndex == 1){
            inputKey = customKey.getText();
            inputIv = customIv.getText();
            traverse.clear();
        }else if(selectedIndex == 2){
            inputKey = null;
            inputIv = null;
        }else if(selectedIndex == 3){
            inputKey = null;
            inputIv = null;
        }

        result.replaceText(!traverse.isEmpty()? 
            I18nUtils.getString("webshell.decrypting.dict") : 
            I18nUtils.getString("webshell.decrypting"));

        final DecryptConfig decryptConfig = DecryptConfig.builder()
                .inputKey(inputKey)
                .inputIv(inputIv)
                .traverseList(new ArrayList<>(traverse))
                .customPath(filePath)
                .build();
        final String decryptInput = inputText.getText();

        Task<Void> decryptTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                WebShellDecryptService decryptContent = new WebShellDecryptService();
                final DecryptResult decryptResult = decryptContent.decryptContent(decryptInput, decryptConfig);
                Platform.runLater(() -> {
                    if (decryptToken != activeDecryptToken) {
                        return;
                    }
                    res = decryptResult.toMap();
                    decryptRunning = false;

                    String data = (String) res.get("data");
                    String encodeModeList = res.get("encodeModeList").toString();
                    int error = (int) res.get("error");

                    result.replaceText(data);

                    if (currentKey() == null) {
                        // 解密失败或结果为空：面板里上一份分析已不对应任何内容
                        discardAiAnalysis();
                        refreshAiUi();
                    } else {
                        String recognizeText = I18nUtils.getString("webshell.auto.recognize", encodeModeList);
                        tipTitle.setText(recognizeText);
                        Tooltip tooltip = new Tooltip(recognizeText);
                        Tooltip.install(tipTitle, tooltip);
                        tipTitle.setVisible(true);
                        tipTitle.setManaged(true);
                        if (aiPanelOpen) {
                            syncAiWithCurrentResult();
                        }
                        refreshAiUi();
                    }

                });
                ExecutorServiceManager.shutdownExecutor(ExecutorServiceManager.ExecutorPoolNames.DECRYPT_ARRAY);
                return null;
            }
        };
        currentTask = decryptTask;
        decryptTask.setOnFailed(e -> {
            Throwable error = decryptTask.getException();
            if (debugMode && error != null) error.printStackTrace();
            if (decryptToken == activeDecryptToken) {
                decryptRunning = false;
                refreshAiUi();
            }
        });
        // 启动任务
        currentThread = new Thread(decryptTask);
        currentThread.setDaemon(true);
        currentThread.start();
    }

    //    筛选栏事件
    @FXML
    void rulesComboChoose(ActionEvent e){
        // 0-使用默认Key快速解密 1-指定Key快速解密 2-使用内置50wKey字典解密 3-指定Key字典解密ƒ

        int selectedIndex = rulesComboBox.getSelectionModel().getSelectedIndex();

        customKey.setVisible(selectedIndex==1? true : false);
        customKey.setManaged(selectedIndex==1? true : false);
        customIv.setVisible(selectedIndex==1? true : false);
        customIv.setManaged(selectedIndex==1? true : false);
        customPath.setVisible(selectedIndex==3? true : false);
        customPath.setManaged(selectedIndex==3? true : false);
        modeComboBox.setVisible(selectedIndex==2||selectedIndex==3? true : false);
        modeComboBox.setManaged(selectedIndex==2||selectedIndex==3? true : false);

        if(selectedIndex == 3){
            FileChooser chooser = new FileChooser();
            FileChooser.ExtensionFilter filter =
                    new FileChooser.ExtensionFilter(I18nUtils.getString("webshell.filetype.txt"), "*.txt");
            FileChooser.ExtensionFilter datFilter =
                    new FileChooser.ExtensionFilter(I18nUtils.getString("webshell.filetype.dat"), "*.dat");
            chooser.getExtensionFilters().add(filter);
            chooser.getExtensionFilters().add(datFilter);

            Stage stage = (Stage) ((Node)e.getSource()).getScene().getWindow();
            try {
                filePath = chooser.showOpenDialog(stage).getAbsolutePath();
                customPath.setText(filePath);
            }catch (Exception exception){
                if(debugMode) System.out.println("没有文件被选择");
            }
        }else {
            filePath = null;
        }

    }

    @FXML
    private void handleMouseEntered() {
        tipTitleCopy.setVisible(true);
        tipTitleCopy.setManaged(true);
    }
    @FXML
    private void handleMouseExited() {
        tipTitleCopy.setVisible(false);
        tipTitleCopy.setManaged(false);
    }


    @FXML
    private void copyTip(){
        String tip = tipTitle.getText();
        StrUtils.setClipboardString(tip);
        showPrompt(I18nUtils.getString("webshell.copied"));
    }

    private void showPrompt(String message) {
        showPrompt(message, PromptTone.INFO, Duration.seconds(1.1));
    }

    private void showSuccessPrompt(String message) {
        showPrompt(message, PromptTone.SUCCESS, Duration.seconds(2.0));
    }

    private void showErrorPrompt(String message) {
        showPrompt(message, PromptTone.ERROR, Duration.seconds(1.8));
    }

    private void showPrompt(String message, PromptTone tone, Duration visibleDuration) {
        if (prompt != null) {
            prompt.setText(message);
            applyPromptTone(tone);
        }
        copyAnimation(visibleDuration);
    }

    private void applyPromptTone(PromptTone tone) {
        if (promptPane == null) {
            return;
        }
        promptPane.getStyleClass().removeAll("prompt-info", "prompt-success", "prompt-error");
        if (prompt != null) {
            prompt.setStyle(null);
        }
        if (tone == PromptTone.SUCCESS) {
            promptPane.getStyleClass().add("prompt-success");
        } else if (tone == PromptTone.ERROR) {
            promptPane.getStyleClass().add("prompt-error");
        } else {
            promptPane.getStyleClass().add("prompt-info");
        }
    }

    void copyAnimation(Duration visibleDuration) {
        if (promptPane == null) {
            return;
        }
        if (promptFadeIn != null) {
            promptFadeIn.stop();
        }
        if (promptFadeOut != null) {
            promptFadeOut.stop();
        }

        promptPane.toFront();
        promptPane.setOpacity(0);
        promptPane.setVisible(true);
        promptPane.setManaged(true);

        promptFadeIn = new FadeTransition(Duration.seconds(0.2), promptPane);
        promptFadeIn.setFromValue(0);
        promptFadeIn.setToValue(1);

        promptFadeOut = new FadeTransition(Duration.seconds(0.2), promptPane);
        promptFadeOut.setFromValue(1);
        promptFadeOut.setToValue(0);
        promptFadeOut.setDelay(visibleDuration);

        promptFadeIn.setOnFinished(event -> promptFadeOut.playFromStart());
        promptFadeIn.playFromStart();

        promptFadeOut.setOnFinished(event -> {
            promptPane.setVisible(false);
            promptPane.setManaged(false);
        });
    }

    void writeTestData(String data) {
        inputText.setText(data);
    }

    @FXML
    void toDecodePcap(ActionEvent event) {
        traverse.clear();
        FileChooser chooser = new FileChooser();
        FileChooser.ExtensionFilter filterPcap = new FileChooser.ExtensionFilter(I18nUtils.getString("webshell.filetype.pcap"), "*.pcap");
        FileChooser.ExtensionFilter filterPcapng = new FileChooser.ExtensionFilter(I18nUtils.getString("webshell.filetype.pcapng"), "*.pcapng");
        chooser.getExtensionFilters().addAll(filterPcap, filterPcapng);

        Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        String path = null;
        try {
            path = chooser.showOpenDialog(stage).getAbsolutePath();
        }catch (Exception exception){
            if(debugMode) System.out.println("没有文件被选择");
            return;
        }

        if (path == null) {
            if(debugMode) System.out.println("没有文件被选择");
            return;
        }

        final long decryptToken = ++decryptTokenSeq;
        activeDecryptToken = decryptToken;
        decryptRunning = true;
        cancelActiveSession();
        res.clear();
        refreshAiUi();

        ExecutorServiceManager.shutdownExecutor(ExecutorServiceManager.ExecutorPoolNames.DECRYPT_ARRAY);

        if (currentTask != null && !currentTask.isDone()) {
            currentThread.stop();
        }

        tipTitle.setText("");
        tipTitle.setVisible(false);
        tipTitle.setManaged(false);

        tipTitle.setText("");
        tipTitle.setVisible(false);
        tipTitle.setManaged(false);

        //  配置参数
        int selectedIndex = rulesComboBox.getSelectionModel().getSelectedIndex();
        int modeIndex = modeComboBox.getSelectionModel().getSelectedIndex();

        if(modeIndex == 0){
            traverse.add("AES");
        }else if(modeIndex == 1){
            traverse.add("DES");
        }else if(modeIndex == 2){
            traverse.add("XOR");
        }else if(modeIndex == 3){
            traverse.add("AES");
            traverse.add("DES");
            traverse.add("XOR");
        }

        if(selectedIndex == 0){
            inputKey = null;
            inputIv = null;
            traverse.clear();
        }else if(selectedIndex == 1){
            inputKey = customKey.getText();
            inputIv = customIv.getText();
            traverse.clear();
        }else if(selectedIndex == 2){
            inputKey = null;
            inputIv = null;
        }else if(selectedIndex == 3){
            inputKey = null;
            inputIv = null;
        }

        result.replaceText(!traverse.isEmpty()? 
            I18nUtils.getString("webshell.pcap.decrypting.dict") : 
            I18nUtils.getString("webshell.pcap.decrypting"));

        String finalPath = path;
        final String pcapKey = inputKey;
        final String pcapIv = inputIv;
        final List<String> pcapTraverse = new ArrayList<>(traverse);
        final String pcapCustomPath = filePath;
        Task<Void> pcapTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                ReadPacketFile reader = new ReadPacketFile(finalPath, pcapKey, pcapIv, pcapTraverse, pcapCustomPath);
                String outputFilePath = reader.getPackets();
                Platform.runLater(() -> {
                    if (decryptToken != activeDecryptToken) {
                        return;
                    }
                    decryptRunning = false;
                    result.replaceText(I18nUtils.getString("webshell.pcap.success", outputFilePath));
                    // 流量包解密不回填 res，AI 无内容可分析，同时清掉上一份分析
                    discardAiAnalysis();
                    refreshAiUi();
                });
                ExecutorServiceManager.shutdownExecutor(ExecutorServiceManager.ExecutorPoolNames.DECRYPT_ARRAY);
                return null;
            }
        };
        currentTask = pcapTask;
        pcapTask.setOnFailed(e -> {
            Throwable error = pcapTask.getException();
            if (debugMode && error != null) error.printStackTrace();
            if (decryptToken == activeDecryptToken) {
                decryptRunning = false;
                refreshAiUi();
            }
        });
        // 启动任务
        currentThread = new Thread(pcapTask);
        currentThread.setDaemon(true);
        currentThread.start();
    }
}
