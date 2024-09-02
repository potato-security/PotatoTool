package com.potato.potatotool.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.leewyatt.rxcontrols.controls.RXLineButton;
import com.potato.potatotool.content.blueTeam.webShellDecrypt;
import com.potato.potatotool.utils.*;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.Constants.*;


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
    private Pane promptPane;

    Map<String, Object> res = new HashMap<>();

    private double aiTextAreaStartX, aiTextAreaStartY;
    private DoubleProperty aiTextAreWidthProperty = new SimpleDoubleProperty(0);
    private DoubleProperty aiTextAreHeightProperty = new SimpleDoubleProperty(0);
    private static final double RESIZE_MARGIN = 10;
    @FXML
    void initialize() throws IOException {
        new codeHighlightingAsync().codeHighlighting(result);

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

        //  设置默认第一个选项
        rulesComboBox.getSelectionModel().selectFirst();
        modeComboBox.getSelectionModel().selectFirst();

    }

    private final ObservableList<Node> contentObj = FXCollections.observableArrayList();
    private void initTextData() {

        String testDataJsonStr = getResourceString("testData");
        JsonArray jsonObject = (JsonArray) (new Gson()).fromJson(testDataJsonStr, JsonObject.class).get("webshellDecode");

        for (int i = 0; i < jsonObject.size(); i++) {
            JsonObject tmpDictData = jsonObject.get(i).getAsJsonObject();

            String data = tmpDictData.get("data").getAsString();
            String mode = tmpDictData.get("mode").getAsString();

            RXLineButton rxLineButton = new RXLineButton();
            rxLineButton.setOnMouseClicked(event -> writeTestData(data));
            rxLineButton.setPrefWidth(100);
            rxLineButton.setPrefHeight(40);
            rxLineButton.setSpacing(3);
            rxLineButton.setText("样本数据" + (i+1));
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

            aiTextArea.clear();
            aiTextArea.appendText("探测加密结果为空，AI暂无内容分析");
            oldData = "";

        }else if((int)res.get("error") == 1){

            aiTextArea.clear();
            aiTextArea.appendText((String) res.get("data"));
            oldData = "";

        }else if (!resultStr.equals(oldData) && !isAiCD){

            oldData = resultStr;
            isAiCD = true;

            aiTextArea.clear();
            aiTextArea.appendText("AI分析中，请稍等……");

            // 另起线程调用AI接口
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    codeAnalyzerUtils.evilCodeAnalysis(resultStr, aiTextArea);
                    return null;
                }
            };
            task.setOnFailed(event -> {
                Throwable error = task.getException();
                error.printStackTrace();
            });
            task.setOnSucceeded(event -> {
                isAiCD = false;
            });
            new Thread(task).start();

        }

    }

    @FXML
    void refreshAI(ActionEvent e) throws Exception { //结果未更新也可以强行重新调用AI
        String resultStr = result.getText();
        //  结果非空 且 未到AI冷却CD 才触发AI接口
        if(res.isEmpty()){

            aiTextArea.clear();
            aiTextArea.appendText("探测加密结果为空，AI暂无内容分析");
            oldData = "";

        }else if((int)res.get("error") == 1){

            aiTextArea.clear();
            aiTextArea.appendText((String) res.get("data"));
            oldData = "";

        }else if (!isAiCD){

            oldData = resultStr;
            isAiCD = true;

            aiTextArea.clear();
            aiTextArea.appendText("AI分析中，请稍等……");

            // 另起线程调用AI接口
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    codeAnalyzerUtils.evilCodeAnalysis(resultStr, aiTextArea);
                    return null;
                }
            };
            task.setOnFailed(event -> {
                Throwable error = task.getException();
                error.printStackTrace();
            });
            task.setOnSucceeded(event -> {
                isAiCD = false;
            });
            new Thread(task).start();

        }
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
            ForkJoinPool forkJoinPool = (ForkJoinPool) executor;
            int runningThreadCount = forkJoinPool.getRunningThreadCount();
            int activeThreadCount = forkJoinPool.getActiveThreadCount();
            long queuedTaskCount = forkJoinPool.getQueuedTaskCount();
            long queuedSubmissionCount = forkJoinPool.getQueuedSubmissionCount();
            int poolSize = forkJoinPool.getPoolSize();
            int parallelism = forkJoinPool.getParallelism();
            int activeTaskCount = forkJoinPool.getActiveThreadCount();

            System.out.println("==== ExecutorService Status ====");
            System.out.println("Running Thread Count: " + runningThreadCount);
            System.out.println("Active Thread Count: " + activeThreadCount);
            System.out.println("Queued Task Count: " + queuedTaskCount);
            System.out.println("Queued Submission Count: " + queuedSubmissionCount);
            System.out.println("Pool Size: " + poolSize);
            System.out.println("Parallelism: " + parallelism);
            System.out.println("Active Task Count: " + activeTaskCount);
            System.out.println("===============================");
        } else {
            System.out.println("The provided executor is not an instance of ForkJoinPool.");
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

        ExecutorServiceManager.getInstance().forceShutdown();

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

        result.replaceText(!traverse.isEmpty()? "请稍等，正在使用大型字典进行解密，可能需要一些时间……" : "解密进行中，请稍等……");

        currentTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                webShellDecrypt wsd = new webShellDecrypt();
                wsd.inputKey = inputKey;
                wsd.inputIv = inputIv;
                wsd.traverse = traverse;
                wsd.customPath = filePath;
                res = wsd.dealBody(inputText.getText());
                Platform.runLater(() -> {

                    System.out.println(res);

                    String data = (String) res.get("data");
                    String encodeModeList = res.get("encodeModeList").toString();
                    int error = (int) res.get("error");

                    result.replaceText(data);

                    if(error == 1){
                        aiTextArea.clear();
                        aiTextArea.appendText(data);
                    }else {
                        tipTitle.setText("自动识别："+encodeModeList);
                        Tooltip tooltip = new Tooltip("自动识别："+encodeModeList);
                        Tooltip.install(tipTitle, tooltip);
                        tipTitle.setVisible(true);
                        tipTitle.setManaged(true);
                    }

                });
                ExecutorServiceManager.getInstance().forceShutdown();
                return null;
            }
        };
        currentTask.setOnFailed(e -> {
            Throwable error = currentTask.getException();
            if (debugMode) error.printStackTrace();
        });
        // 启动任务
        currentThread = new Thread(currentTask);
        currentThread.start();
    }

    //    筛选栏事件
    @FXML
    void rulesComboChoose(ActionEvent e){

        // 0-使用默认Key快速解密 1-指定Key快速解密 2-使用内置50wKey字典解密 3-指定Key字典解密

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
                    new FileChooser.ExtensionFilter("txt文件(*.txt)", "*.txt");
            FileChooser.ExtensionFilter datFilter =
                    new FileChooser.ExtensionFilter("数据文件(*.dat)", "*.dat");
            chooser.getExtensionFilters().add(filter);
            chooser.getExtensionFilters().add(datFilter);

            Stage stage = (Stage) ((Node)e.getSource()).getScene().getWindow();
            try {
                filePath = chooser.showOpenDialog(stage).getAbsolutePath();
                customPath.setText(filePath);
            }catch (Exception exception){
                System.out.println("没有文件被选择");
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
        strUtils.setClipboardString(tip);
        copyAnimation();
    }

    void copyAnimation() {
        // 显示提示组件
        promptPane.setVisible(true);
        promptPane.setManaged(true);

        // 创建渐入动画
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.2), promptPane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        // 创建渐出动画
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.2), promptPane);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.seconds(0.5)); // 延迟1秒执行渐出动画

        // 播放渐入动画，完成后播放渐出动画
        fadeIn.setOnFinished(event -> fadeOut.play());
        fadeIn.play();

        fadeOut.setOnFinished(event -> {
            promptPane.setVisible(false);
            promptPane.setManaged(false);
        });
    }

    void writeTestData(String data) {
        inputText.setText(data);
    }

}
