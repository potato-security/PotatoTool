package com.potato.potatotool.controller.redTeam;

import com.dlsc.gemsfx.CFCheckBox;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.redTeam.infoGathering.AssetMapper;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.AssetConstants;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.AssetObj;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.DataTypeConstants;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.DomainInfo;
import com.potato.potatotool.content.redTeam.infoGathering.utils.AssetExcelExporter;
import com.potato.potatotool.utils.core.ExecutorServiceManager;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.ui.DialogUtils;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.content.redTeam.infoGathering.classObj.AssetObj.*;
import static com.potato.potatotool.content.redTeam.infoGathering.infoLeakage.gitLeakage.GitHubLeakage.getError_Github;
import static com.potato.potatotool.content.redTeam.infoGathering.infoLeakage.searchEnginesLeakage.GoogleSearch.getError_Google;
import static com.potato.potatotool.content.redTeam.infoGathering.tools.FofaSearch.getError_Fofa;
import static com.potato.potatotool.content.redTeam.infoGathering.tools.HunterSearch.getError_Hunter;
import static com.potato.potatotool.content.redTeam.infoGathering.tools.QuakeSearch.getError_Quake;
import static com.potato.potatotool.content.redTeam.infoGathering.tools.ShodanSearch.getError_Shodan;
import static com.potato.potatotool.content.redTeam.infoGathering.tools.ZoomeyeSearch.getError_Zoomeye;
import static com.potato.potatotool.content.redTeam.infoGathering.utils.AssetExcelExporter.generateRepIng;

/**
 * @author Potato
 * @date 2023/3/21 10:53
 */
public class PaneInfoSearch {
    @FXML
    private StackPane sPane;
    @FXML
    private ComboBox searchModeBox;
    @FXML
    private TextField question;
    @FXML
    private CFCheckBox fofaBox;
    @FXML
    private CFCheckBox hunterBox;
    @FXML
    private CFCheckBox quakeBox;
    @FXML
    private CFCheckBox zoomeyeBox;
    @FXML
    private CFCheckBox shodanBox;
    @FXML
    private CFCheckBox googleBox;
    @FXML
    private CFCheckBox githubBox;
    @FXML
    private CFCheckBox weightBox;
    @FXML
    private CFCheckBox bruteForceSubdomainBox;
    @FXML
    private CFCheckBox searchSslSubdomainBox;
    @FXML
    private CFCheckBox searchShadowAssetsBox;
    @FXML
    private CFCheckBox hasCrawlLinksBox;
    @FXML
    private CFCheckBox hasFindSensitiveInfoBox;
    @FXML
    private CFCheckBox hasCipAggregatorBox;
    @FXML
    private CFCheckBox hasLocalFullDetectionBox;
    @FXML
    private CFCheckBox hasIconSearchBox;
    @FXML
    private TextField weightStr;
    @FXML
    private TextField maxDepth;
    @FXML
    private TextField maxSubPathCount;
    @FXML
    private TextField maxGoogleSearchCount;
    @FXML
    private TextField maxGithubSearchCount;
    @FXML
    private TextField cipThreshold;
    @FXML
    private TextField localFullDetectionThreshold;
    @FXML
    private TextField shadowAssetsThreshold;
    @FXML
    private HBox weightHBox;
    @FXML
    private HBox maxDepthHBox;
    @FXML
    private HBox maxSubPathCountHBox;
    @FXML
    private HBox maxGoogleSearchCountHBox;
    @FXML
    private HBox maxGithubSearchCountHBox;
    @FXML
    private HBox cipThresholdHBox;
    @FXML
    private HBox localFullDetectionThresholdHBox;
    @FXML
    private HBox shadowAssetsThresholdHBox;
    @FXML
    private StackPane promptPane;
    @FXML
    private Label prompt;
    @FXML
    private VBox companyNameVbox;
    @FXML
    private VBox companyNameChoosePaneBox;
    @FXML
    private VBox iconChoosePaneBox;
    @FXML
    private FlowPane iconFlowPane;
    @FXML
    public VBox domainChoosePaneBox;
    @FXML
    public VBox domainVbox;
    @FXML
    private Button companyNameSave;
    @FXML
    private Button domainSave;
    @FXML
    private Button iconSave;
    @FXML
    private HBox advancedSetting;
    @FXML
    private Label uploadLabel;
    @FXML
    private Label sendLabel;
    @FXML
    private Label stopLabel;
    @FXML
    private ScrollPane scroll;

    private final Object lock = new Object(); // 用于线程同步

    @FXML
    private VBox echoVbox;

    private final List<CFCheckBox> checkBoxList = new ArrayList<>();

    public void initialize() {
        listenSearch();
        searchModeBox.getSelectionModel().select(0);

        checkBoxList.add(fofaBox);
        checkBoxList.add(hunterBox);
        checkBoxList.add(quakeBox);
        checkBoxList.add(zoomeyeBox);
        checkBoxList.add(shodanBox);
        
        // 绑定国际化
        Platform.runLater(() -> I18nUtils.bindComponents(sPane));
    }

    //  监听输入时回车
    private void listenSearch() {
        question.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !stopLabel.isVisible()) {
                searchInput(null);
            }
        });
    }


    @FXML
    public void chooseSearcchMode(ActionEvent event) {
        echoVbox.getChildren().clear();
        boolean isIntelligentMode = searchModeBox.getSelectionModel().getSelectedIndex() == 0;

        advancedSetting.setManaged(isIntelligentMode);
        advancedSetting.setVisible(isIntelligentMode);
        googleBox.setManaged(isIntelligentMode);
        googleBox.setVisible(isIntelligentMode);
        githubBox.setManaged(isIntelligentMode);
        githubBox.setVisible(isIntelligentMode);
        uploadLabel.setVisible(isIntelligentMode);

        for (CFCheckBox checkbox : checkBoxList) {
            checkbox.setSelected(false);
        }

        if (isIntelligentMode) {
            for (CFCheckBox checkbox : checkBoxList) {
                checkbox.selectedProperty().removeListener(singleSelectListener); // 移除监听器
            }
        } else {
            for (CFCheckBox checkbox : checkBoxList) {
                checkbox.selectedProperty().addListener(singleSelectListener); // 添加监听器
            }
        }

    }

    private final ChangeListener<Boolean> singleSelectListener = (observable, oldValue, newValue) -> {
        CFCheckBox currentCheckbox = (CFCheckBox) ((ReadOnlyBooleanProperty) observable).getBean();

        if (newValue) { // 当前复选框被选中时
            for (CFCheckBox checkbox : checkBoxList) {
                if (checkbox != currentCheckbox) {
                    checkbox.setSelected(false); // 取消其他复选框选中状态
                }
            }
            // 再次确保当前复选框的选中状态为 true，防止状态被覆盖
            currentCheckbox.setSelected(true);
        }
    };


    private Task<Void> currentTask;
    private Thread currentThread;
    @FXML
    public void searchInput(MouseEvent mouseEvent) {
        echoVbox.getChildren().clear();
        uploadLabel.setVisible(false);
        sendLabel.setVisible(false);
        sendLabel.setManaged(false);
        stopLabel.setVisible(true);
        stopLabel.setManaged(true);

        // 初始化任务状态
        if (currentTask != null && !currentTask.isDone()) {
            currentThread.stop();
        }
        ExecutorServiceManager.shutdownExecutor(ExecutorServiceManager.ExecutorPoolNames.ASSET_ARRAY);

        boolean isSmart = searchModeBox.getSelectionModel().getSelectedIndex() == 0;
        currentTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                search(question.getText(), isSmart);

                Platform.runLater(() -> {
                    uploadLabel.setVisible(true);
                    sendLabel.setVisible(true);
                    sendLabel.setManaged(true);
                    stopLabel.setVisible(false);
                    stopLabel.setManaged(false);
                });
                return null;
            }
        };
        currentTask.setOnFailed(e -> {
            Throwable exception = currentTask.getException();
            if (exception != null) exception.printStackTrace();
        });
        currentThread = new Thread(currentTask);
        currentThread.start();
    }

    @FXML
    public void searchUploadInput(MouseEvent event) {
        FileChooser chooser = new FileChooser();
        FileChooser.ExtensionFilter filter =
                new FileChooser.ExtensionFilter("TXT文件", "*.txt");
        chooser.getExtensionFilters().add(filter);

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

        echoVbox.getChildren().clear();
        uploadLabel.setVisible(false);
        sendLabel.setVisible(false);
        sendLabel.setManaged(false);
        stopLabel.setVisible(true);
        stopLabel.setManaged(true);

        // 初始化任务状态
        if (currentTask != null && !currentTask.isDone()) {
            currentThread.stop();
        }
        ExecutorServiceManager.shutdownExecutor(ExecutorServiceManager.ExecutorPoolNames.ASSET_ARRAY);

        String finalPath = path;
        boolean isSmart = searchModeBox.getSelectionModel().getSelectedIndex() == 0;
        currentTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                try (BufferedReader reader = Files.newBufferedReader(Paths.get(finalPath), StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine())!= null) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            try {
                                search(line, isSmart);
                            }catch (Exception e){
                                if(debugMode) e.printStackTrace();
                            }
                        }
                    }
                } catch (IOException e) {
                    if(debugMode) e.printStackTrace();
                }
                Platform.runLater(() -> {
                    uploadLabel.setVisible(true);
                    sendLabel.setVisible(true);
                    sendLabel.setManaged(true);
                    stopLabel.setVisible(false);
                    stopLabel.setManaged(false);
                });
                return null;
            }
        };
        currentTask.setOnFailed(e -> {
            Throwable exception = currentTask.getException();
            if (exception != null) exception.printStackTrace();
        });
        currentThread = new Thread(currentTask);
        currentThread.start();

    }

    AssetMapper assetMapper = null;
    AssetObj assetObj = null;
    private void search(String questionStr, boolean isSmart){
        currentLoadingBox = null;
        currentInput = questionStr;

        assetMapper = new AssetMapper();
        assetMapper.setController(PaneInfoSearch.this);
        assetObj = new AssetObj();
        if(!fofaBox.isSelected()) assetObj.setFofa_Key(null);
        if(!hunterBox.isSelected()) assetObj.setHunter_Key(new JsonArray());
        if(!quakeBox.isSelected()) assetObj.setQuake_Key(new JsonArray());
        if(!zoomeyeBox.isSelected()) assetObj.setZoomeye_Key(null);
        if(!shodanBox.isSelected()) assetObj.setShodan_Key(null);

        if(isSmart) {
            try {
//                if (!googleBox.isSelected()) assetObj.setGoogle_API(new JsonArray());
//                if (!githubBox.isSelected()) assetObj.setGitHub_Token(new JsonArray());
                if (weightBox.isSelected()) {
                    String splitRegex = "[,，]";
                    List<Integer> weightList = Arrays.stream(weightStr.getText().split(splitRegex))
                            .map(s -> Integer.parseInt(s))
                            .collect(Collectors.toList());
                    if (weightList.size() > 0) assetObj.setWeightThresholdList(weightList);
                }
                assetObj.setUseGoogle(googleBox.isSelected());
                assetObj.setUseGithub(githubBox.isSelected());
                assetObj.setMaxDepth(Integer.parseInt(maxDepth.getText()));
                assetObj.setMaxSubPathCount(Integer.parseInt(maxSubPathCount.getText()));
                assetObj.setMaxGoogleSearchCount(Integer.parseInt(maxGoogleSearchCount.getText()));
                assetObj.setMaxGithubSearchCount(Integer.parseInt(maxGithubSearchCount.getText()));
                assetObj.setBruteForceSubdomain(bruteForceSubdomainBox.isSelected());
                assetObj.setSearchSslSubdomainBox(searchSslSubdomainBox.isSelected());
                assetObj.setSearchShadowAssets(searchShadowAssetsBox.isSelected());
                assetObj.setHasCrawlLinks(hasCrawlLinksBox.isSelected());
                assetObj.setHasFindSensitiveInfo(hasFindSensitiveInfoBox.isSelected());
                assetObj.setHasCipAggregator(hasCipAggregatorBox.isSelected());
                assetObj.setCipThreshold(Integer.parseInt(cipThreshold.getText()));
                assetObj.setHasLocalFullDetection(hasLocalFullDetectionBox.isSelected());
                assetObj.setHasIconSearch(hasIconSearchBox.isSelected());
                assetObj.setLocalFullDetectionThreshold(Integer.parseInt(localFullDetectionThreshold.getText()));
                assetObj.setShadowAssetsThreshold(Integer.parseInt(shadowAssetsThreshold.getText()));
            }catch (Exception e){
                showTip(e.getMessage(), true);
                if(debugMode) e.printStackTrace();
            }

            assetMapper.searchInfo(questionStr, assetObj);
        }else {
            assetMapper.searchInfo_standard(questionStr, assetObj);
        }
    }

    @FXML
    public void checkFofa(MouseEvent event) {
        fofaBox.setSelected(!fofaBox.isSelected());
        if(fofaBox.isSelected()){
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    if (!hasSetKey(AssetConstants.FOFA_KEY)) {
                        Platform.runLater(() -> {
                            fofaBox.setSelected(false);
                            showTip("Fofa_Key未设置", true, AssetConstants.FOFA_KEY);
                        });
                    } else {
                        String error = getError_Fofa();
                        if (error != null) {
                            Platform.runLater(() -> {
                                fofaBox.setSelected(!fofaBox.isSelected());
                                showTip(error, true, AssetConstants.FOFA_KEY);
                            });
                        }
                    }
                    return null;
                }
            };
            task.setOnFailed(e -> {
                Throwable exception = task.getException();
                if (exception != null) exception.printStackTrace();
            });
            new Thread(task).start();
        }
    }

    @FXML
    public void checkHunter(MouseEvent event) {
        hunterBox.setSelected(!hunterBox.isSelected());
        if(hunterBox.isSelected()){
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    if(!hasSetKey(AssetConstants.HUNTER_KEY)){
                        Platform.runLater(() -> {
                            hunterBox.setSelected(false);
                            showTip("Hunter_Key未设置", true, AssetConstants.HUNTER_KEY);
                        });
                    } else {
                        String error = getError_Hunter();
                        if (error != null) {
                            Platform.runLater(() -> {
                                hunterBox.setSelected(!hunterBox.isSelected());
                                showTip(error, true, AssetConstants.HUNTER_KEY);
                            });
                        }
                    }
                    return null;
                }
            };
            new Thread(task).start();
        }
    }

    @FXML
    public void checkQuake(MouseEvent event) {
        quakeBox.setSelected(!quakeBox.isSelected());
        if(quakeBox.isSelected()){
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    if(!hasSetKey(AssetConstants.QUAKE_KEY)){
                        Platform.runLater(() -> {
                            quakeBox.setSelected(false);
                            showTip("Quake_Key未设置", true, AssetConstants.QUAKE_KEY);
                        });
                    } else {
                        String error = getError_Quake();
                        if (error != null) {
                            Platform.runLater(() -> {
                                quakeBox.setSelected(!quakeBox.isSelected());
                                showTip(error, true, AssetConstants.QUAKE_KEY);
                            });
                        }
                    }
                    return null;
                }
            };
            new Thread(task).start();
        }
    }

    @FXML
    public void checkZoomeye(MouseEvent event) {
        zoomeyeBox.setSelected(!zoomeyeBox.isSelected());
        if(zoomeyeBox.isSelected()){
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    if(!hasSetKey(AssetConstants.ZOOMEYE_KEY)){
                        Platform.runLater(() -> {
                            zoomeyeBox.setSelected(false);
                            showTip("Zoomeye_Key未设置", true, AssetConstants.ZOOMEYE_KEY);
                        });
                    } else {
                        String error = getError_Zoomeye();
                        if (error != null) {
                            Platform.runLater(() -> {
                                zoomeyeBox.setSelected(!zoomeyeBox.isSelected());
                                showTip(error, true, AssetConstants.ZOOMEYE_KEY);
                            });
                        }
                    }
                    return null;
                }
            };
            new Thread(task).start();
        }
    }

    @FXML
    public void checkShodan(MouseEvent event) {
        shodanBox.setSelected(!shodanBox.isSelected());
        if(shodanBox.isSelected()){
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    if(!hasSetKey(AssetConstants.SHODAN_KEY)){
                        Platform.runLater(() -> {
                            shodanBox.setSelected(false);
                            showTip("Shodan_Key未设置", true, AssetConstants.SHODAN_KEY);
                        });
                    } else {
                        String error = getError_Shodan();
                        if (error != null) {
                            Platform.runLater(() -> {
                                shodanBox.setSelected(!shodanBox.isSelected());
                                showTip(error, true, AssetConstants.SHODAN_KEY);
                            });
                        }
                    }
                    return null;
                }
            };
            new Thread(task).start();
        }
    }


    @FXML
    public void checkGoogle(MouseEvent event) {
        googleBox.setSelected(!googleBox.isSelected());
        if(googleBox.isSelected()){
            maxGoogleSearchCountHBox.setVisible(true);
            maxGoogleSearchCountHBox.setManaged(true);

            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    if(!hasSetKey(AssetConstants.GOOGLE_API)){
                        Platform.runLater(() -> {
                            googleBox.setSelected(false);
                            showTip("Google_Api未设置", true, AssetConstants.GOOGLE_API);
                            maxGoogleSearchCountHBox.setVisible(false);
                            maxGoogleSearchCountHBox.setManaged(false);
                        });
                    } else {
                        String error = getError_Google();
                        if (error != null) {
                            Platform.runLater(() -> {
                                googleBox.setSelected(!googleBox.isSelected());
                                showTip(error, true, AssetConstants.GOOGLE_API);
                                maxGoogleSearchCountHBox.setVisible(false);
                                maxGoogleSearchCountHBox.setManaged(false);
                            });
                        }
                    }
                    return null;
                }
            };
            new Thread(task).start();
        }else {
            maxGoogleSearchCountHBox.setVisible(false);
            maxGoogleSearchCountHBox.setManaged(false);
        }
    }

    @FXML
    public void checkGithub(MouseEvent event) {
        githubBox.setSelected(!githubBox.isSelected());
        if(githubBox.isSelected()){
            maxGithubSearchCountHBox.setVisible(true);
            maxGithubSearchCountHBox.setManaged(true);

            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    if(!hasSetKey(AssetConstants.GITHUB_TOKEN)){
                        Platform.runLater(() -> {
                            githubBox.setSelected(false);
                            showTip("Github_Token未设置", true, AssetConstants.GITHUB_TOKEN);
                            maxGithubSearchCountHBox.setVisible(false);
                            maxGithubSearchCountHBox.setManaged(false);
                        });
                    } else {
                        String error = getError_Github();
                        if (error != null) {
                            Platform.runLater(() -> {
                                githubBox.setSelected(!githubBox.isSelected());
                                showTip(error, true, AssetConstants.GITHUB_TOKEN);
                                maxGithubSearchCountHBox.setVisible(false);
                                maxGithubSearchCountHBox.setManaged(false);
                            });
                        }
                    }
                    return null;
                }
            };
            new Thread(task).start();
        }else {
            maxGithubSearchCountHBox.setVisible(false);
            maxGithubSearchCountHBox.setManaged(false);
        }
    }

    @FXML
    public void checkWeight(MouseEvent event) {
        weightBox.setSelected(!weightBox.isSelected());
        weightHBox.setManaged(weightBox.isSelected());
        weightHBox.setVisible(weightBox.isSelected());
    }

    @FXML
    public void checkhasCrawlLinks(MouseEvent event) {
        hasCrawlLinksBox.setSelected(!hasCrawlLinksBox.isSelected());
        maxDepthHBox.setManaged(hasCrawlLinksBox.isSelected());
        maxDepthHBox.setVisible(hasCrawlLinksBox.isSelected());
        maxSubPathCountHBox.setManaged(hasCrawlLinksBox.isSelected());
        maxSubPathCountHBox.setVisible(hasCrawlLinksBox.isSelected());
    }

    @FXML
    public void checkSearchShadowAssets(MouseEvent event) {
        searchShadowAssetsBox.setSelected(!searchShadowAssetsBox.isSelected());
        shadowAssetsThresholdHBox.setManaged(searchShadowAssetsBox.isSelected());
        shadowAssetsThresholdHBox.setVisible(searchShadowAssetsBox.isSelected());
    }

    @FXML
    public void checkhasLocalFullDetection(MouseEvent event) {
        hasLocalFullDetectionBox.setSelected(!hasLocalFullDetectionBox.isSelected());
        localFullDetectionThresholdHBox.setManaged(!hasLocalFullDetectionBox.isSelected() && hasIconSearchBox.isSelected());
        localFullDetectionThresholdHBox.setVisible(!hasLocalFullDetectionBox.isSelected() && hasIconSearchBox.isSelected());
    }

    @FXML
    public void checkhasIconSearch(MouseEvent event) {
        hasIconSearchBox.setSelected(!hasIconSearchBox.isSelected());
        localFullDetectionThresholdHBox.setManaged(!hasLocalFullDetectionBox.isSelected() && hasIconSearchBox.isSelected());
        localFullDetectionThresholdHBox.setVisible(!hasLocalFullDetectionBox.isSelected() && hasIconSearchBox.isSelected());
    }

    @FXML
    public void checkhasCipAggregator(MouseEvent event) {
        hasCipAggregatorBox.setSelected(!hasCipAggregatorBox.isSelected());
        cipThresholdHBox.setManaged(hasCipAggregatorBox.isSelected());
        cipThresholdHBox.setVisible(hasCipAggregatorBox.isSelected());
    }

    public void showTip(String tip, boolean showSet){
        showTip(tip, showSet, null);
    }

    public void showTip(String tip, boolean showSet, String configKey){
        Platform.runLater(() -> {
            prompt.setText(tip);
            copyAnimation(showSet, configKey);
        });
    }

    public void copyAnimation(boolean showSet, String configKey) {
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
        fadeOut.setDelay(Duration.seconds(1)); // 延迟1秒执行渐出动画

        // 播放渐入动画，完成后播放渐出动画
        fadeIn.setOnFinished(event -> fadeOut.play());
        fadeIn.play();

        fadeOut.setOnFinished(event -> {
            promptPane.setVisible(false);
            promptPane.setManaged(false);
            if(showSet) showSet(configKey);
        });
    }
    
    @FXML
    public void showSet() {
        showSet(null);
    }

    private void showSet(String configKey) {
        DialogUtils.showSet(sPane.getScene().getWindow(), configKey);
    }

    private void createCompanyNameHBox(String companyName, int index) {
        HBox hBox = new HBox();
        hBox.setSpacing(10);
        hBox.setAlignment(Pos.CENTER);
        Label indexLabel = new Label(String.valueOf(companyNameVbox.getChildren().size() + 1) + "、");
        StackPane stackPane = new StackPane();
        if(index!=0) {
            stackPane.setOnMouseEntered(this::showDel);
            stackPane.setOnMouseExited(this::hiddenDel);
        }

        TextField textField = new TextField();
        textField.getStyleClass().add("paddingRightButton");
        textField.setText(companyName);
        StackPane.setAlignment(textField, Pos.CENTER);

        if(index==0){
            Region region = new Region();
            region.getStyleClass().add("addIcon");
            Label addLabel = new Label();
            addLabel.getStyleClass().add("rightButton");
            addLabel.getStyleClass().add("rightAddButton");

            addLabel.setGraphic(region);
            Tooltip tooltip = new Tooltip(I18nUtils.getString("tooltip.add.company"));
            addLabel.setTooltip(tooltip);
            addLabel.setOnMouseClicked(event -> addField(null));
            addLabel.setTranslateX(35);
            StackPane.setAlignment(addLabel, Pos.CENTER_RIGHT);

            stackPane.getChildren().addAll(textField, addLabel);
        }else {
            Region region = new Region();
            region.getStyleClass().add("delIcon");
            Label deleteLabel = new Label();
            deleteLabel.setManaged(false);
            deleteLabel.setVisible(false);
            deleteLabel.getStyleClass().add("rightButton");

            deleteLabel.setGraphic(region);
            Tooltip tooltip = new Tooltip(I18nUtils.getString("tooltip.delete.company"));
            deleteLabel.setTooltip(tooltip);
            deleteLabel.setOnMouseClicked(event -> deleteField(hBox));
            StackPane.setAlignment(deleteLabel, Pos.CENTER_RIGHT);

            stackPane.getChildren().addAll(textField, deleteLabel);
        }
        hBox.getChildren().addAll(indexLabel, stackPane);
        companyNameVbox.getChildren().add(hBox);
        updateAddButtonVisibility();
    }

    public void showDel(MouseEvent event) {
        Parent parent = (Parent) event.getSource();
        for (Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof Label) {
                node.setManaged(true);
                node.setVisible(true);
                break;
            }
        }
    }

    public void hiddenDel(MouseEvent event) {
        Parent parent = (Parent) event.getSource();
        for (Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof Label) {
                node.setManaged(false);
                node.setVisible(false);
                break;
            }
        }
    }

    private void deleteField(HBox hBoxToRemove) {
        companyNameVbox.getChildren().remove(hBoxToRemove);
        // 更新序号
        for (int i = 0; i < companyNameVbox.getChildren().size(); i++) {
            HBox hBox = (HBox) companyNameVbox.getChildren().get(i);
            Label indexLabel = (Label) hBox.getChildren().get(0);
            indexLabel.setText(String.valueOf(i + 1) + "、");
        }
        updateAddButtonVisibility();
    }


    private void updateAddButtonVisibility() {
        if (companyNameVbox.getChildren().size() >= 5) {
            Label addLable = (Label) ((StackPane)((HBox)companyNameVbox.getChildren().get(0)).getChildren().get(1)).getChildren().get(1);
            addLable.setVisible(false);
            addLable.setManaged(false);
        } else if (companyNameVbox.getChildren().size() > 0)  {
            Label addLable = (Label) ((StackPane)((HBox)companyNameVbox.getChildren().get(0)).getChildren().get(1)).getChildren().get(1);
            addLable.setVisible(true);
            addLable.setManaged(true);
        }
    }

    @FXML
    public void addField(ActionEvent event) {
        createCompanyNameHBox("", 1);
    }

    private Set<String> companyNameSet = new HashSet<>();
    @FXML
    public void saveCompanyNameField(ActionEvent event) {
        for (int i = 0; i < companyNameVbox.getChildren().size(); i++) {
            HBox hBox = (HBox) companyNameVbox.getChildren().get(i);
            StackPane stackPane = (StackPane) hBox.getChildren().get(1);
            TextField textField = (TextField) stackPane.getChildren().get(0);
            String companyName = textField.getText().trim();
            if (!companyName.isEmpty()) {
                companyNameSet.add(companyName);
            }
        }
        hideCompanyNameChoosePaneBox();

        // 唤醒等待的线程
        synchronized (lock) {
            lock.notify();
        }

        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
    }

    public Set<String> waitAndGetCompanyNameSet(){
        synchronized (lock) {
            try {
                // 等待保存
                lock.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("线程中断：" + e.getMessage());
            }
        }
        return companyNameSet;
    }

    public void showCompanyNameChoosePaneBox(Set<String> companyNamesSet){
        Platform.runLater(() -> {
            int index = 0;
            for (String companyName : companyNamesSet) {
                createCompanyNameHBox(companyName, index);
                index++;
            }
            companyNameSave.setText(I18nUtils.getString("infosearch.next") + "(" + countdown + ")");

            companyNameChoosePaneBox.setVisible(true);
            companyNameChoosePaneBox.setManaged(true);

            startCountdown(companyNameSave, ()->saveCompanyNameField(null));
        });
    }

    private static final int COUNTDOWN_TIME = 10;
    private int countdown = COUNTDOWN_TIME;
    private Timeline countdownTimeline;
    private void startCountdown(Button button, Runnable onCountdownComplete) {
        countdown = COUNTDOWN_TIME;
        countdownTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    if (countdown > 0) {
                        button.setText(I18nUtils.getString("infosearch.next") + "(" + countdown + ")");
                        countdown--;
                    } else {
                        onCountdownComplete.run();
                    }
                })
        );
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    public void hideCompanyNameChoosePaneBox(){
        Platform.runLater(() -> {
            companyNameChoosePaneBox.setVisible(false);
            companyNameChoosePaneBox.setManaged(false);
            companyNameVbox.getChildren().clear();
        });
    }

    private List<DomainInfo> domainInfoList = new ArrayList<>();
    @FXML
    public void saveIconField(ActionEvent event) {
        hideIconChoosePaneBox();

        // 唤醒等待的线程
        synchronized (lock) {
            lock.notify();
        }

        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
    }

    public void showIconChoosePaneBox(List<DomainInfo> oldDomainInfoList){
        domainInfoList.clear();
        Map<String, Integer> iconCountMap = new HashMap<>(); // 记录iconMd5和出现次数
        Platform.runLater(() -> {
            String firstIconMd5 = null; // 记录首个图标的MD5

            for (DomainInfo domainInfo : oldDomainInfoList) {
                Map<String, Object> webInfoMap = domainInfo.getWebInfoMap();
                if(webInfoMap==null || !webInfoMap.containsKey("iconBase64")) continue;

                String iconMd5 = webInfoMap.get("iconMd5").toString();
                String iconBase64 = webInfoMap.get("iconBase64").toString();
                iconCountMap.put(iconMd5, iconCountMap.getOrDefault(iconMd5, 0) + 1);

                if (firstIconMd5 == null) {
                    firstIconMd5 = iconMd5;
                }
                // 如果已经存在相同的图标，直接更新计数并跳过创建新的图标
                if (iconCountMap.get(iconMd5) > 1) {
                    // 查找已有的numberLabel更新计数
                    for (Node node : iconFlowPane.getChildren()) {
                        if (node instanceof StackPane) {
                            StackPane stackPane = (StackPane) node;
                            if (stackPane.getUserData() != null && stackPane.getUserData().equals(iconMd5)) {
                                Label numberLabel = (Label) stackPane.getChildren().stream()
                                        .filter(child -> child instanceof Label)
                                        .findFirst()
                                        .orElse(null);
                                if (numberLabel != null) {
                                    numberLabel.setText(String.valueOf(iconCountMap.get(iconMd5)));
                                }
                                break;
                            }
                        }
                    }
                    continue;
                }

                // 加载图标，转换ico为png
                Image iconImage = convertIcoBase64ToImage(iconBase64);
                if(iconImage == null) continue;

                // 图标容器
                StackPane iconPane = new StackPane();
                iconPane.setAlignment(Pos.CENTER);
                iconPane.setMaxSize(40, 40);
                iconPane.setMinSize(40, 40);
                iconPane.setCursor(Cursor.HAND);
                iconPane.setUserData(iconMd5); // 用于标识图标

                // 图标
                ImageView imageView = new ImageView(iconImage);
                imageView.setFitWidth(30);
                imageView.setFitHeight(30);

                // 选中效果
                Region selectionRegion = new Region();
                selectionRegion.getStyleClass().add("selectImg");
                selectionRegion.setVisible(false); // 初始不可见
                StackPane.setAlignment(selectionRegion, Pos.BOTTOM_RIGHT);

                Label numberLabel = new Label("1");
                numberLabel.setAlignment(Pos.CENTER);
                numberLabel.setMaxSize(30, 30);
                numberLabel.setMinSize(30, 30);
                numberLabel.getStyleClass().add("numberLabel");
                numberLabel.getTransforms().add(new Scale(0.5, 0.5));
                StackPane.setAlignment(numberLabel, Pos.TOP_LEFT);

                // 点击事件处理
                iconPane.setOnMouseClicked(event -> {
                    selectionRegion.setVisible(!selectionRegion.isVisible());
                    if(selectionRegion.isVisible()){
                        domainInfoList.add(domainInfo);
                    }else {
                        domainInfoList.remove(domainInfo);
                    }
                });

                iconPane.getChildren().addAll(imageView, selectionRegion, numberLabel);
                iconFlowPane.getChildren().add(iconPane);

                // 如果是第一个图标（计数最多），设置为选中状态
                if (iconMd5.equals(firstIconMd5)) {
                    selectionRegion.setVisible(true); // 显示选中效果
                    domainInfoList.add(domainInfo); // 添加到选中列表
                }
            }

            iconSave.setText(I18nUtils.getString("infosearch.next") + "(" + countdown + ")");

            iconChoosePaneBox.setVisible(true);
            iconChoosePaneBox.setManaged(true);

            startCountdown(iconSave, ()->saveIconField(null));
        });
    }

    private Image convertIcoBase64ToImage(String base64Data) {
        try {
            // 解码 Base64 字符串为字节数组
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            // 尝试解析图片格式
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (bufferedImage == null) {
                throw new IllegalArgumentException("无法解析图片，可能格式不支持");
            }

            // 转换 BufferedImage 为 InputStream（统一为 PNG 格式）
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", outputStream);

            // 将 PNG 数据加载为 JavaFX Image
            return new Image(new ByteArrayInputStream(outputStream.toByteArray()));

        }catch (Exception e){}

        return null;
    }


    public void hideIconChoosePaneBox(){
        Platform.runLater(() -> {
            iconChoosePaneBox.setVisible(false);
            iconChoosePaneBox.setManaged(false);
            iconFlowPane.getChildren().clear();
        });
    }

    public List<DomainInfo> waitAndGetIconDomainInfoList() {
        synchronized (lock) {
            try {
                // 等待保存
                lock.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("线程中断：" + e.getMessage());
            }
        }
        return domainInfoList;
    }


    // 存储当前的加载 HBox，用于重复调用时检查状态
    private HBox currentLoadingBox = null;
    private RotateTransition rotateTransition = null;
    public void updateEchoVBox(String message, boolean isComplete, Map<String, Object> dataMap) {
        // 避免多次重复执行相同状态的更新
        if (Platform.isFxApplicationThread()) {
            updateUI(message, isComplete, dataMap);
        } else {
            Platform.runLater(() -> updateUI(message, isComplete, dataMap));
        }
    }
    public void updateUI(String message, boolean isComplete,  Map<String, Object> dataMap) {
        Platform.runLater(() -> {
            if (currentLoadingBox == null) {
                currentLoadingBox = new HBox(10);
                currentLoadingBox.setAlignment(Pos.TOP_LEFT);

                Label loadingImg = new Label();
                loadingImg.setMaxSize(20, 20);
                loadingImg.setMinSize(20, 20);
                loadingImg.getStyleClass().add("loaddingImg");

                rotateTransition = new RotateTransition(Duration.seconds(1), loadingImg);
                rotateTransition.setByAngle(360);
                rotateTransition.setCycleCount(RotateTransition.INDEFINITE);
                rotateTransition.play();

                Label textLabel = new Label(message);
                currentLoadingBox.getChildren().addAll(loadingImg, textLabel);
                echoVbox.getChildren().add(currentLoadingBox);
            }

            Label loadingImg = (Label) currentLoadingBox.getChildren().get(0);
            Node dynamicContent = currentLoadingBox.getChildren().get(1);

            if (isComplete) {
                rotateTransition.stop();
                loadingImg.getStyleClass().remove("loaddingImg");
                loadingImg.getStyleClass().add("endImg");
                loadingImg.setRotate(0);

                if (dataMap != null) {

                    // 替换为可点击下拉抽屉
                    TitledPane drawer = createDrawer(message, dataMap);
                    currentLoadingBox.getChildren().set(1, drawer);
                } else {
                    ((Label) dynamicContent).setText(message);
                }

                currentLoadingBox = null;
            } else {
                rotateTransition.play();

                if (dynamicContent instanceof Label) {
                    ((Label) dynamicContent).setText(message);
                }
            }
            scroll.setVvalue(1.0);
        });
    }

    // 根据 dataMap 创建抽屉
    private TitledPane createDrawer(String message, Map<String, Object> dataMap) {
        // 渲染内容
        VBox contentBox = new VBox(5);
        contentBox.setStyle("-fx-padding: 10;");

        String type = (String) dataMap.get("type");
        Object data = dataMap.get("data");

        if (DataTypeConstants.SEO.equalsIgnoreCase(type)) {
            @SuppressWarnings("unchecked")
            JsonObject seoMap = (JsonObject) data;
            TableView<Map.Entry<String, String>> seoTable = createSEODataTable(seoMap);
            contentBox.getChildren().add(seoTable);
        } else if (DataTypeConstants.ICP.equalsIgnoreCase(type)) {
            @SuppressWarnings("unchecked")
            JsonObject icpMap = (JsonObject) data;

            // 添加工商信息表格
            JsonObject businessInfo = icpMap.getAsJsonObject("工商信息");
            TableView<Map.Entry<String, String>> businessTable = createGenericDataTable(businessInfo, "工商信息");
            contentBox.getChildren().add(businessTable);

            // 添加微信公众号表格
            JsonArray wechatArray = icpMap.getAsJsonArray("微信公众号");
            TableView<Map<String, String>> wechatTable = createArrayDataTable(wechatArray, new String[]{"公众号名称", "微信号", "公众号简介"}, "微信公众号");
            contentBox.getChildren().add(wechatTable);

            // 添加软件著作表格
            JsonArray softwareArray = icpMap.getAsJsonArray("软件著作");
            TableView<Map<String, String>> softwareTable = createArrayDataTable(softwareArray, new String[]{"软件简称", "版本号"}, "软件著作");
            contentBox.getChildren().add(softwareTable);

            // 添加网站备案表格
            JsonArray websiteArray = icpMap.getAsJsonArray("网站备案");
            TableView<Map<String, String>> websiteTable = createArrayDataTable(websiteArray, new String[]{"网站域名", "备案号", "网站名称"}, "网站备案");
            contentBox.getChildren().add(websiteTable);
        } else if (DataTypeConstants.WXAPPSUBCOM.equals(type)) {
            @SuppressWarnings("unchecked")
            JsonArray companiesArray = (JsonArray) data;

            companiesArray.forEach(companyElement -> {
                JsonObject company = companyElement.getAsJsonObject();
                company.entrySet().forEach(entry -> {
                    String companyName = entry.getKey(); // 公司名称
                    JsonObject companyData = entry.getValue().getAsJsonObject();

                    // 创建标题标签
                    Label companyLabel = new Label(companyName);
                    companyLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                    contentBox.getChildren().add(companyLabel);

                    // App 数据表格
                    JsonArray appArray = companyData.getAsJsonArray("app");
                    if (appArray != null && appArray.size() > 0) {
                        TableView<Map<String, String>> appTable = createArrayDataTable(
                                appArray,
                                new String[]{"name", "classify", "logoWord", "logoBrief"},
                                "App 应用信息"
                        );
                        contentBox.getChildren().add(appTable);
                    }

                    // 微信公众号数据表格
                    JsonArray wxArray = companyData.getAsJsonArray("wx");
                    if (wxArray != null && wxArray.size() > 0) {
                        TableView<Map<String, String>> wxTable = createArrayDataTable(
                                wxArray,
                                new String[]{"principalName", "wechatId", "wechatName", "wechatIntruduction"},
                                "微信公众号信息"
                        );
                        contentBox.getChildren().add(wxTable);
                    }

                    // 域名与备案信息表格
                    JsonArray domainAndIcpArray = companyData.getAsJsonArray("domainAndIcp");
                    if (domainAndIcpArray != null && domainAndIcpArray.size() > 0) {
                        TableView<Map<String, String>> domainTable = createArrayDataTable(
                                domainAndIcpArray,
                                new String[]{"domain", "homeSite", "icpNo", "siteName"},
                                "域名与备案信息"
                        );
                        contentBox.getChildren().add(domainTable);
                    }

                    // 权重公司信息表格
                    JsonArray weightCompanyArray = companyData.getAsJsonArray("weightCompany");
                    if (weightCompanyArray != null && weightCompanyArray.size() > 0) {
                        TableView<Map<String, String>> weightTable = createArrayDataTable(
                                weightCompanyArray,
                                new String[]{"entName", "regRate", "regCapital", "investment"},
                                "权重公司信息"
                        );
                        contentBox.getChildren().add(weightTable);
                    }
                });
            });
        } else {
            contentBox.getChildren().add(new Label(I18nUtils.getString("infosearch.error.unknown")));
        }

        // 创建 TitledPane
        TitledPane titledPane = new TitledPane(message, contentBox);
        titledPane.setExpanded(false); // 默认折叠状态

        return titledPane;
    }

    private TableView<Map.Entry<String, String>> createSEODataTable(JsonObject seoData) {
        TableView<Map.Entry<String, String>> tableView = new TableView<>();
        tableView.setPrefWidth(sPane.getPrefWidth() - 100);

        TableColumn<Map.Entry<String, String>, String> keyColumn = createTableColumn(I18nUtils.getString("infosearch.field"), tableView.getPrefWidth() * 0.2, true);
        TableColumn<Map.Entry<String, String>, String> valueColumn = createTableColumn(I18nUtils.getString("infosearch.value"), tableView.getPrefWidth() * 0.8, false);

        // 将列添加到表格中
        tableView.getColumns().addAll(keyColumn, valueColumn);

        // 填充数据
        ObservableList<Map.Entry<String, String>> tableData = FXCollections.observableArrayList();

        // "域名信息" 数据
        JsonObject domainInfo = seoData.getAsJsonObject("域名信息");
        domainInfo.entrySet().forEach(entry -> tableData.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().getAsString())));

        // "备案信息" 数据
        JsonObject icpInfo = seoData.getAsJsonObject("备案信息");
        icpInfo.entrySet().forEach(entry -> tableData.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().getAsString())));

        // "网站信息" 数据
        JsonObject websiteInfo = seoData.getAsJsonObject("网站信息");
        websiteInfo.entrySet().forEach(entry -> tableData.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().getAsString())));

        // 设置表格数据
        tableView.setItems(tableData);

        // 设置表格样式
        tableView.setRowFactory(tv -> {
            TableRow<Map.Entry<String, String>> row = new TableRow<>();
            row.setStyle("-fx-background-color: transparent;");
            return row;
        });

        return tableView;
    }

    // 创建键值对表格
    private TableView<Map.Entry<String, String>> createGenericDataTable(JsonObject data, String header) {
        TableView<Map.Entry<String, String>> tableView = new TableView<>();
        tableView.setPrefWidth(sPane.getPrefWidth() - 100);

        TableColumn<Map.Entry<String, String>, String> keyColumn = createTableColumn(I18nUtils.getString("infosearch.field"), tableView.getPrefWidth() * 0.2, true);
        TableColumn<Map.Entry<String, String>, String> valueColumn = createTableColumn(I18nUtils.getString("infosearch.value"), tableView.getPrefWidth() * 0.8, false);

        tableView.getColumns().addAll(keyColumn, valueColumn);

        ObservableList<Map.Entry<String, String>> tableData = FXCollections.observableArrayList();
        data.entrySet().forEach(entry -> tableData.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().getAsString())));
        tableView.setItems(tableData);

        // 设置表格样式
        tableView.setRowFactory(tv -> {
            TableRow<Map.Entry<String, String>> row = new TableRow<>();
            row.setStyle("-fx-background-color: transparent;");
            return row;
        });

        return tableView;
    }

    // 创建数组类型表格
    private TableView<Map<String, String>> createArrayDataTable(JsonArray dataArray, String[] keys, String header) {
        TableView<Map<String, String>> tableView = new TableView<>();
        tableView.setPrefWidth(sPane.getPrefWidth() - 100);
        // 列宽自动
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        for (String key : keys) {
            TableColumn<Map<String, String>, String> column = createTableColumn(key, 0, false);
            tableView.getColumns().add(column);
        }

        ObservableList<Map<String, String>> tableData = FXCollections.observableArrayList();
        dataArray.forEach(item -> {
            JsonObject obj = item.getAsJsonObject();
            Map<String, String> row = new HashMap<>();
            for (String key : keys) {
                row.put(key, obj.has(key) ? handleJsonElement(obj.get(key)) : "-");
            }
            tableData.add(row);
        });
        tableView.setItems(tableData);

        // 设置表格样式
        tableView.setRowFactory(tv -> {
            TableRow<Map<String, String>> row = new TableRow<>();
            row.setStyle("-fx-background-color: transparent;");
            return row;
        });

        return tableView;
    }

    private <T> TableCell<T, String> createTableCell() {
        return new TableCell<T, String>() {
            private final Text text = new Text();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    text.getStyleClass().add("text");
                    text.setText(item);
                    text.setWrappingWidth(getTableColumn().getWidth() - 10); // 设置换行宽度
                    text.setTextAlignment(TextAlignment.CENTER);
                    VBox vBox = new VBox(text);
                    vBox.setStyle("-fx-padding: 5;");
                    vBox.setAlignment(Pos.CENTER);
                    setGraphic(vBox);
                }
            }
        };
    }

    private <T> TableColumn<T, String> createTableColumn(String header, double widthFraction, boolean isKeyColumn) {
        TableColumn<T, String> column = new TableColumn<>(header);

        if (isKeyColumn) {
            column.setCellValueFactory(cellData -> {
                return new SimpleStringProperty(((Map.Entry<String, String>) cellData.getValue()).getKey());
            });
        } else {
            column.setCellValueFactory(cellData -> {
                if (cellData.getValue() instanceof Map.Entry) {
                    return new SimpleStringProperty(((Map.Entry<String, String>) cellData.getValue()).getValue());
                } else if (cellData.getValue() instanceof Map) {
                    return new SimpleStringProperty(((Map<String, String>) cellData.getValue()).get(header));
                }
                return new SimpleStringProperty(""); // 默认返回
            });
        }

        // 自动换行
        column.setCellFactory(param -> createTableCell());
        if (widthFraction > 0) {
            column.setPrefWidth(widthFraction - 7);  // 动态宽度
        }

        return column;
    }

    public static String handleJsonElement(JsonElement element) {
        if (element.isJsonArray()) {
            JsonArray jsonArray = element.getAsJsonArray();
            List<String> elements = new ArrayList<>();
            for (JsonElement e : jsonArray) {
                elements.add(e.getAsString());
            }
            return String.join("\n", elements);
        }
        return element.getAsString();
    }

    private JsonArray domainListChoose = new JsonArray();
    public void showDomainChoosePaneBox(JsonArray domainList) {
        Platform.runLater(() -> {
            domainListChoose = new JsonArray();
            if (domainList == null || domainList.size() == 0) return;
            HBox header = createRow(I18nUtils.getString("infosearch.domain.header.domain"), 
                                      I18nUtils.getString("infosearch.domain.header.belong"), 
                                      I18nUtils.getString("infosearch.domain.header.addtime"), 
                                      I18nUtils.getString("infosearch.domain.header.uptime"), true);
            domainVbox.getChildren().add(header);
            for (JsonElement jsonElement : domainList) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                String domain = jsonObject.get("domain").getAsString();
                String addtime = jsonObject.get("addtime").getAsString();
                String uptime = jsonObject.get("uptime").getAsString();
                JsonObject seoMap = jsonObject.getAsJsonObject("seoMap");
                String companyName = seoMap.getAsJsonObject("备案信息").get("备案所属").getAsString();
                if(companyName.isEmpty()||companyName.equals("-")) companyName = "-";

                HBox row = createRow(domain , companyName, addtime, uptime, false);

                row.setOnMouseClicked(event -> {
                    StackPane domainCell = (StackPane) row.getChildren().get(0);
                    Region selectionRegion = (Region) domainCell.getChildren().get(1);
                    selectionRegion.setVisible(!selectionRegion.isVisible());
                    if (selectionRegion.isVisible()) {
                        domainListChoose.add(jsonObject);
                    } else {
                        domainListChoose.remove(jsonObject);
                    }
                });

                domainVbox.getChildren().add(row);
            }
            domainSave.setText(I18nUtils.getString("infosearch.next") + "(" + countdown + ")");

            domainChoosePaneBox.setVisible(true);
            domainChoosePaneBox.setManaged(true);

            startCountdown(domainSave, ()->saveDomainField(null));
        });
    }

    private HBox createRow(String domain, String companyName, String addtime, String uptime, boolean isHeader) {
        HBox row = new HBox();
        row.setSpacing(5);
        row.setAlignment(Pos.CENTER_LEFT);

        StackPane domainCell = createCell(domain, isHeader);
        StackPane companyNameCell = createCell(companyName, isHeader);
        StackPane addtimeCell = createCell(addtime, isHeader);
        StackPane uptimeCell = createCell(uptime, isHeader);

        row.getChildren().addAll(domainCell, companyNameCell, addtimeCell, uptimeCell);

        return row;
    }

    private StackPane createCell(String text, boolean isHeader) {
        StackPane cell = new StackPane();
        cell.setPrefSize(200, 30);

        Label cellText = new Label(text);
        if (isHeader) {
            cellText.setStyle("-fx-underline: true;");
        }

        Region selectionRegion = new Region();
        selectionRegion.getStyleClass().add("selectImg");
        selectionRegion.setVisible(false); // 初始不可见

        cell.getChildren().addAll(cellText, selectionRegion);
        StackPane.setAlignment(selectionRegion, Pos.BOTTOM_RIGHT);
        return cell;
    }

    public void hideDomainChoosePaneBox(){
        Platform.runLater(() -> {
            domainChoosePaneBox.setVisible(false);
            domainChoosePaneBox.setManaged(false);
            domainVbox.getChildren().clear();
        });
    }

    public JsonArray waitAndGetDomainSet() {
        synchronized (lock) {
            try {
                // 等待保存
                lock.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("线程中断：" + e.getMessage());
            }
        }
        return domainListChoose;
    }

    @FXML
    public void saveDomainField(ActionEvent event) {
        hideDomainChoosePaneBox();

        // 唤醒等待的线程
        synchronized (lock) {
            lock.notify();
        }

        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
    }

    private String currentInput = "";

    @FXML
    public void stopSearch(MouseEvent event) {
        updateEchoVBox("主动结束查询", !generateRepIng, null);
        stopLabel.setDisable(true);

        if(!generateRepIng) {
            if (currentTask != null && !currentTask.isDone()) {
                currentThread.stop();
            }
            ExecutorServiceManager.shutdownExecutor(ExecutorServiceManager.ExecutorPoolNames.ASSET_ARRAY);

            currentTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    if(assetMapper!=null) {
                        updateEchoVBox("导出报告中……", false, null);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                        String timeStr = sdf.format(new Date(System.currentTimeMillis()));
                        String outXlsxFile = StrUtils.getCurrentJarDir() + File.separator + "AssetResult" + File.separator + currentInput.replaceAll("[/\\\\:*?\"<>|]", "_") + "_" + timeStr + ".xlsx";
                        String error = new AssetExcelExporter().exportToExcel(assetObj, outXlsxFile);
                        updateEchoVBox(error == null ? "报告导出至:" + outXlsxFile : "导出失败_[Error]：" + error, true, null);
                    }

                    Platform.runLater(() -> {
                        uploadLabel.setVisible(true);
                        sendLabel.setVisible(true);
                        sendLabel.setManaged(true);
                        stopLabel.setVisible(false);
                        stopLabel.setManaged(false);
                        stopLabel.setDisable(false);
                    });
                    return null;
                }
            };
            currentTask.setOnFailed(e -> {
                Throwable exception = currentTask.getException();
                if (exception != null) exception.printStackTrace();
            });
            currentThread = new Thread(currentTask);
            currentThread.start();
        }
    }
}
