package com.potato.potatotool.controller;

import com.dlsc.gemsfx.CFSwitch;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.Constants;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static com.potato.potatotool.content.update.checkResAndGetDownUrl;
import static com.potato.potatotool.content.update.downloadAndSaveResource;

/**
 * @author Potato
 * @date 2023/11/8 14:47
 */
public class PaneSetting {
    @FXML
    private AnchorPane an;

    @FXML
    private TextField proxy;

    @FXML
    private CFSwitch proxyButton;

    @FXML
    private ComboBox decompileType;

    @FXML
    private ComboBox gptModel;

    @FXML
    private TextField gptApiKey;

    @FXML
    private BorderPane topBar;

    @FXML
    private Label md5Tips;
    @FXML
    private Label kbTips;
    @FXML
    private Button md5CheckButton;
    @FXML
    private Button md5UpdateButton;
    @FXML
    private Button kbCheckButton;
    @FXML
    private Button kbUpdateButton;
    @FXML
    private Button md5StopUpdateButton;
    @FXML
    private Button kbStopUpdateButton;
    @FXML
    private VBox md5ProgressVBox;
    @FXML
    private ProgressBar md5ProgressBar;
    @FXML
    private Label md5ProgressLabel;
    @FXML
    private VBox kbProgressVBox;
    @FXML
    private ProgressBar kbProgressBar;
    @FXML
    private Label kbProgressLabel;
    
    private double offsetX,offsetY;

    public void initialize() {
        proxy.setFocusTraversable(false);

        //  初始化默认代理配置
        JsonObject tmpJsonObj_Proxy = (JsonObject) Constants.getOutsideConfig("Proxy");
        boolean isProxy = tmpJsonObj_Proxy.getAsJsonPrimitive("enable").getAsBoolean();
        String address = tmpJsonObj_Proxy.getAsJsonPrimitive("address").getAsString();
        proxy.setText(address);
        proxyButton.setSelected(isProxy);

        //  初始化默认反编译模式配置
        JsonObject tmpJsonObj_Decompile = (JsonObject) Constants.getOutsideConfig("Decompile");
        String decompileMode = tmpJsonObj_Decompile.getAsJsonPrimitive("decompileMode").getAsString();
        decompileType.setValue(decompileMode);

        //  初始化默认AI配置
        JsonObject tmpJsonObj_AI = (JsonObject) Constants.getOutsideConfig("AI");
        String GPT_Model = tmpJsonObj_AI.getAsJsonPrimitive("GPT_Model").getAsString();
        String GPT_API_Key = tmpJsonObj_AI.getAsJsonPrimitive("GPT_API_Key").getAsString();
        gptModel.setValue(GPT_Model);
        gptApiKey.setText(GPT_API_Key);

    }

    @FXML
    void topBarDraggedAction(MouseEvent event) {
        Window window = topBar.getScene().getWindow();
        window.setX(event.getScreenX()-offsetX);
        window.setY(event.getScreenY()-offsetY);
    }

    @FXML
    void topBarPressedAction(MouseEvent event) {
        offsetX = event.getSceneX();
        offsetY = event.getSceneY();
    }

    @FXML
    public void exitAction(){
        Stage stage = (Stage) an.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void viewProfile() {
        String os = System.getProperty("os.name").toLowerCase();
        String startCommand = os.contains("windows") ? "start" :
                              os.contains("mac") ? "open" :
                              os.contains("linux") ? "xdg-open" : null;
        String command = Paths.get(System.getProperty("user.home"), ".PotatoTool", "config.json").toString();
        try {
            new ProcessBuilder(startCommand, command).start();
        } catch (Exception e) {
            System.out.println(startCommand + " " + command);
            e.printStackTrace();
        }
    }


    @FXML
    void save(){
        Map<String, Object> configMap = new HashMap<>();
        Map<String, Object> proxyMap = new HashMap<>();
        proxyMap.put("enable", proxyButton.isSelected());
        proxyMap.put("address", proxy.getText());
        configMap.put("Proxy", proxyMap);

        Map<String, Object> decompileMap = new HashMap<>();
        decompileMap.put("decompileMode", (String)decompileType.getSelectionModel().getSelectedItem());
        decompileMap.put("AI_optimization", false);
        configMap.put("Decompile", decompileMap);

        Map<String, Object> aiMap = new HashMap<>();
        aiMap.put("GPT_Model", (String)gptModel.getSelectionModel().getSelectedItem());
        aiMap.put("GPT_API_Key", gptApiKey.getText());
        configMap.put("AI", aiMap);

        Constants.saveConfig(configMap);
        exitAction();
    }

    private Thread md5CheckThread;
    String md5DownUrl = null;
    @FXML
    public void checkMd5(ActionEvent event) {
        md5CheckButton.setText("检测中");

        if (md5CheckThread != null && md5CheckThread.isAlive()) {
            md5CheckThread.stop();   // 强行中断当前线程
        }
        if (md5UpdateThread != null && md5UpdateThread.isAlive()) {
            md5UpdateThread.stop();
        }

        md5CheckThread = new Thread(() -> {
            md5DownUrl = checkResAndGetDownUrl("md5");

            Platform.runLater(() -> {
                if (md5DownUrl==null){
                    md5Tips.setText("已存在");
                    md5Tips.setVisible(true);
                    md5Tips.setManaged(true);
                    md5CheckButton.setVisible(false);
                    md5CheckButton.setManaged(false);
                }else if (md5DownUrl.startsWith("[Error]")) {
                    md5Tips.setText(md5DownUrl.replace("[Error]",""));
                    md5Tips.setVisible(true);
                    md5Tips.setManaged(true);
                    md5CheckButton.setVisible(true);
                    md5CheckButton.setManaged(true);
                }else {
                    md5Tips.setText("不存在");
                    md5Tips.setVisible(true);
                    md5Tips.setManaged(true);
                    md5CheckButton.setVisible(false);
                    md5CheckButton.setManaged(false);
                    md5UpdateButton.setVisible(true);
                    md5UpdateButton.setManaged(true);
                }
                md5CheckButton.setText("检测");
            });
        });
        md5CheckThread.start();
    }

    private Thread kbCheckThread;
    String kbDownUrl = null;
    @FXML
    public void checkKb(ActionEvent event) {
        kbCheckButton.setText("检测中");

        if (kbCheckThread != null && kbCheckThread.isAlive()) {
            kbCheckThread.stop();   // 强行中断当前线程
        }
        if (kbUpdateThread != null && kbUpdateThread.isAlive()) {
            kbUpdateThread.stop();
        }

        kbCheckThread = new Thread(() -> {
            kbDownUrl = checkResAndGetDownUrl("winKbInfo");

            Platform.runLater(() -> {
                if (kbDownUrl==null){
                    kbTips.setText("已为最新版");
                    kbTips.setVisible(true);
                    kbTips.setManaged(true);
                    kbCheckButton.setVisible(false);
                    kbCheckButton.setManaged(false);
                }else if (kbDownUrl.startsWith("[Error]")) {
                    kbTips.setText(kbDownUrl.replace("[Error]",""));
                    kbTips.setVisible(true);
                    kbTips.setManaged(true);
                    kbCheckButton.setVisible(true);
                    kbCheckButton.setManaged(true);
                }else {
                    kbTips.setText("不存在");
                    kbTips.setVisible(true);
                    kbTips.setManaged(true);
                    kbCheckButton.setVisible(false);
                    kbCheckButton.setManaged(false);
                    kbUpdateButton.setVisible(true);
                    kbUpdateButton.setManaged(true);
                }
                kbCheckButton.setText("检测");
            });
        });
        kbCheckThread.start();
    }

    private Thread md5UpdateThread;
    @FXML
    public void updateMd5(ActionEvent event) {

        if (md5CheckThread != null && md5CheckThread.isAlive()) {
            md5CheckThread.stop();   // 强行中断当前线程
        }
        if (md5UpdateThread != null && md5UpdateThread.isAlive()) {
            md5UpdateThread.stop();
        }

        md5ProgressLabel.setText("下载进度: 0%, 速度: -, 预估时间: -");
        md5ProgressBar.setProgress(0);
        md5Tips.setVisible(false);
        md5Tips.setManaged(false);
        md5UpdateButton.setManaged(false);
        md5UpdateButton.setVisible(false);
        md5ProgressVBox.setManaged(true);
        md5ProgressVBox.setVisible(true);
        md5StopUpdateButton.setManaged(true);
        md5StopUpdateButton.setVisible(true);

        md5UpdateThread = new Thread(() -> {
            downloadAndSaveResource("md5", md5DownUrl, md5ProgressBar, md5ProgressLabel);

            Platform.runLater(() -> {
                md5Tips.setText("已存在");
                md5ProgressVBox.setManaged(false);
                md5ProgressVBox.setVisible(false);
                md5StopUpdateButton.setManaged(false);
                md5StopUpdateButton.setVisible(false);
                md5Tips.setManaged(true);
                md5Tips.setVisible(true);
            });
        });
        md5UpdateThread.start();
    }

    private Thread kbUpdateThread;
    @FXML
    public void updateKb(ActionEvent event) {

        if (kbCheckThread != null && kbCheckThread.isAlive()) {
            kbCheckThread.stop();   // 强行中断当前线程
        }
        if (kbUpdateThread != null && kbUpdateThread.isAlive()) {
            kbUpdateThread.stop();
        }

        kbProgressLabel.setText("下载进度: 0%, 速度: -, 预估时间: -");
        kbProgressBar.setProgress(0);
        kbTips.setVisible(false);
        kbTips.setManaged(false);
        kbUpdateButton.setManaged(false);
        kbUpdateButton.setVisible(false);
        kbProgressVBox.setManaged(true);
        kbProgressVBox.setVisible(true);
        kbStopUpdateButton.setManaged(true);
        kbStopUpdateButton.setVisible(true);

        kbUpdateThread = new Thread(() -> {
            downloadAndSaveResource("winKbInfo", kbDownUrl, kbProgressBar, kbProgressLabel);

            Platform.runLater(() -> {
                kbTips.setText("已为最新版");
                kbProgressVBox.setManaged(false);
                kbProgressVBox.setVisible(false);
                kbStopUpdateButton.setManaged(false);
                kbStopUpdateButton.setVisible(false);
                kbTips.setManaged(true);
                kbTips.setVisible(true);
            });
        });
        kbUpdateThread.start();
    }

    @FXML
    public void stopUpdateMd5(ActionEvent event) {
        md5StopUpdateButton.setVisible(false);
        md5StopUpdateButton.setManaged(false);
        md5ProgressVBox.setManaged(false);
        md5ProgressVBox.setVisible(false);
        md5CheckButton.setVisible(true);
        md5CheckButton.setManaged(true);

        if (md5CheckThread != null && md5CheckThread.isAlive()) {
            md5CheckThread.stop();   // 强行中断当前线程
        }
        if (md5UpdateThread != null && md5UpdateThread.isAlive()) {
            md5UpdateThread.stop();
        }
    }

    @FXML
    public void stopUpdateKb(ActionEvent event) {

        kbStopUpdateButton.setVisible(false);
        kbStopUpdateButton.setManaged(false);
        kbProgressVBox.setManaged(false);
        kbProgressVBox.setVisible(false);
        kbCheckButton.setVisible(true);
        kbCheckButton.setManaged(true);

        if (kbCheckThread != null && kbCheckThread.isAlive()) {
            kbCheckThread.stop();   // 强行中断当前线程
        }
        if (kbUpdateThread != null && kbUpdateThread.isAlive()) {
            kbUpdateThread.stop();
        }
    }
}
