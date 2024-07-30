package com.potato.potatotool.controller;

import com.dlsc.gemsfx.CFSwitch;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.Constants;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

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
}
