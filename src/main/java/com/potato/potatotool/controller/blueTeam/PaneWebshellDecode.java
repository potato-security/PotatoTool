package com.potato.potatotool.controller.blueTeam;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.leewyatt.rxcontrols.controls.RXLineButton;
import com.potato.potatotool.content.blueTeam.ReadPacketFile;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.WebShellDecryptService;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.model.DecryptConfig;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.model.DecryptResult;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.report.WebshellAnalysisWordReportGenerator;
import com.potato.potatotool.utils.ai.CodeAnalyzerUtils;
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

    private double aiTextAreaStartX, aiTextAreaStartY;
    private DoubleProperty aiTextAreWidthProperty = new SimpleDoubleProperty(0);
    private DoubleProperty aiTextAreHeightProperty = new SimpleDoubleProperty(0);
    private static final double RESIZE_MARGIN = 10;
    private final WebshellAnalysisWordReportGenerator reportGenerator = new WebshellAnalysisWordReportGenerator();
    private volatile boolean lastAiTaskSuccess = false;
    private FadeTransition promptFadeIn;
    private FadeTransition promptFadeOut;

    private enum AiAnalysisState {
        IDLE,
        ANALYZING,
        SUCCESS,
        FAILED
    }

    private enum PromptTone {
        INFO,
        SUCCESS,
        ERROR
    }

    private AiAnalysisState aiAnalysisState = AiAnalysisState.IDLE;

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
        initStateInvalidationListeners();

        //  设置默认第一个选项
        rulesComboBox.getSelectionModel().selectFirst();
        modeComboBox.getSelectionModel().selectFirst();
        
        // 绑定国际化
        Platform.runLater(() -> I18nUtils.bindComponents(sPane));
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


    private String oldData = "";
    private boolean isAiVisible = false;
    private boolean isAiCD = false;

    private void initAiControls() {
        downloadAI.setGraphic(createAiButtonIcon(MaterialDesign.MDI_DOWNLOAD));
        setAiAnalysisState(AiAnalysisState.IDLE);
    }

    private FontIcon createAiButtonIcon(MaterialDesign iconCode) {
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(20);
        icon.setIconColor(Color.web("#D9F6FF"));
        return icon;
    }

    private void initStateInvalidationListeners() {
        inputText.textProperty().addListener((obs, oldValue, newValue) -> invalidateAiReport());
        customKey.textProperty().addListener((obs, oldValue, newValue) -> invalidateAiReport());
        customIv.textProperty().addListener((obs, oldValue, newValue) -> invalidateAiReport());
        customPath.textProperty().addListener((obs, oldValue, newValue) -> invalidateAiReport());
        rulesComboBox.valueProperty().addListener((obs, oldValue, newValue) -> invalidateAiReport());
        modeComboBox.valueProperty().addListener((obs, oldValue, newValue) -> invalidateAiReport());
    }

    private void invalidateAiReport() {
        oldData = "";
        lastAiTaskSuccess = false;
        setAiAnalysisState(AiAnalysisState.IDLE);
    }

    private void setAiAnalysisState(AiAnalysisState state) {
        aiAnalysisState = state;
        refreshAI.setDisable(state == AiAnalysisState.ANALYZING);
        downloadAI.setDisable(state != AiAnalysisState.SUCCESS);
    }

    private void executeAiAnalysis(String resultStr) {
        oldData = resultStr;
        isAiCD = true;
        lastAiTaskSuccess = false;
        setAiAnalysisState(AiAnalysisState.ANALYZING);

        aiTextArea.clear();
        aiTextArea.appendText(I18nUtils.getString("webshell.ai.analyzing"));

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() {
                lastAiTaskSuccess = CodeAnalyzerUtils.streamEvilCodeAnalysis(
                        resultStr,
                        res.get("encodeModeList").toString(),
                        aiTextArea
                );
                return null;
            }
        };
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            if (debugMode && error != null) {
                error.printStackTrace();
            }
            Platform.runLater(() -> {
                isAiCD = false;
                setAiAnalysisState(AiAnalysisState.FAILED);
                String message = error == null || error.getMessage() == null || error.getMessage().trim().isEmpty()
                        ? I18nUtils.getString("ai.status.error")
                        : error.getMessage();
                aiTextArea.setText(message);
            });
        });
        task.setOnSucceeded(event -> {
            isAiCD = false;
            setAiAnalysisState(lastAiTaskSuccess ? AiAnalysisState.SUCCESS : AiAnalysisState.FAILED);
        });
        Thread aiTaskThread = new Thread(task);
        aiTaskThread.setDaemon(true);
        aiTaskThread.start();
    }

    private void showAiUnavailable(String message) {
        aiTextArea.clear();
        aiTextArea.appendText(message);
        oldData = "";
        setAiAnalysisState(AiAnalysisState.FAILED);
    }

    @FXML
    void showAI(ActionEvent e) throws Exception {

        double targetWidth = isAiVisible ? 0 : sPane.getPrefWidth() * 0.8;
        Duration duration = Duration.seconds(0.2);

        if (!isAiVisible){
            aiPane.setVisible(true);
            showAI.setStyle("-fx-background-color: #87CEFA");
        }else {
            showAI.setStyle("-fx-background-color: transparent");
        }

        Timeline animation = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(aiTextAreWidthProperty, aiTextAreWidthProperty.get())),
                new KeyFrame(duration, new KeyValue(aiTextAreWidthProperty, targetWidth))
        );

        animation.setOnFinished(event -> {
            isAiVisible = !isAiVisible;

            if (!isAiVisible) {
                aiPane.setVisible(false);
            }
        });

        animation.play();

        String resultStr = result.getText();
        //  结果非空 且 结果更新 且 未到AI冷却CD 才触发AI接口
        if(res.isEmpty()){

            showAiUnavailable(I18nUtils.getString("webshell.ai.detect.empty"));

        }else if((int)res.get("error") == 1){

            showAiUnavailable((String) res.get("data"));

        }else if (!resultStr.equals(oldData) && !isAiCD){
            executeAiAnalysis(resultStr);

        }

    }

    @FXML
    void refreshAI(ActionEvent e) throws Exception { //结果未更新也可以强行重新调用AI
        String resultStr = result.getText();
        //  结果非空 且 未到AI冷却CD 才触发AI接口
        if(res.isEmpty()){

            showAiUnavailable(I18nUtils.getString("webshell.ai.detect.empty"));

        }else if((int)res.get("error") == 1){

            showAiUnavailable((String) res.get("data"));

        }else if (!isAiCD){
            executeAiAnalysis(resultStr);

        }
    }

    @FXML
    void downloadAIReport(ActionEvent event) {
        if (aiAnalysisState != AiAnalysisState.SUCCESS) {
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
            if (aiAnalysisState == AiAnalysisState.SUCCESS) {
                downloadAI.setDisable(false);
            }
            File exportedFile = exportTask.getValue();
            showSuccessPrompt(I18nUtils.getString("webshell.report.export.success.named",
                    exportedFile.getName(), exportedFile.getAbsolutePath()));
        });
        exportTask.setOnFailed(e -> {
            if (aiAnalysisState == AiAnalysisState.SUCCESS) {
                downloadAI.setDisable(false);
            }
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
        return new WebshellAnalysisWordReportGenerator.ReportData(
                inputText.getText(),
                result.getText(),
                aiTextArea.getText(),
                resolveDecryptRuleSummary(),
                collectSchemeLines(),
                new Date()
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

        invalidateAiReport();
        res.clear();
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

        currentTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                DecryptConfig config = DecryptConfig.builder()
                        .inputKey(inputKey)
                        .inputIv(inputIv)
                        .traverseList(traverse)
                        .customPath(filePath)
                        .build();
                WebShellDecryptService decryptContent = new WebShellDecryptService();
                DecryptResult decryptResult = decryptContent.decryptContent(inputText.getText(), config);
                res = decryptResult.toMap();
                Platform.runLater(() -> {

                    System.out.println(res);

                    String data = (String) res.get("data");
                    String encodeModeList = res.get("encodeModeList").toString();
                    int error = (int) res.get("error");

                    result.replaceText(data);

                    if(error == 1){
                        showAiUnavailable(data);
                    }else {
                        String recognizeText = I18nUtils.getString("webshell.auto.recognize", encodeModeList);
                        tipTitle.setText(recognizeText);
                        Tooltip tooltip = new Tooltip(recognizeText);
                        Tooltip.install(tipTitle, tooltip);
                        tipTitle.setVisible(true);
                        tipTitle.setManaged(true);
                    }

                });
                ExecutorServiceManager.shutdownExecutor(ExecutorServiceManager.ExecutorPoolNames.DECRYPT_ARRAY);
                return null;
            }
        };
        currentTask.setOnFailed(e -> {
            Throwable error = currentTask.getException();
            if (debugMode) error.printStackTrace();
        });
        // 启动任务
        currentThread = new Thread(currentTask);
        currentThread.setDaemon(true);
        currentThread.start();
    }

    //    筛选栏事件
    @FXML
    void rulesComboChoose(ActionEvent e){
        invalidateAiReport();

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
        invalidateAiReport();
        res.clear();
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
        currentTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                ReadPacketFile reader = new ReadPacketFile(finalPath, inputKey, inputIv, traverse, filePath);
                String outputFilePath = reader.getPackets();
                Platform.runLater(() -> {
                    result.replaceText(I18nUtils.getString("webshell.pcap.success", outputFilePath));
                });
                ExecutorServiceManager.shutdownExecutor(ExecutorServiceManager.ExecutorPoolNames.DECRYPT_ARRAY);
                return null;
            }
        };
        currentTask.setOnFailed(e -> {
            Throwable error = currentTask.getException();
            if (debugMode) error.printStackTrace();
        });
        // 启动任务
        currentThread = new Thread(currentTask);
        currentThread.setDaemon(true);
        currentThread.start();
    }
}
