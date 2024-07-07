package com.potato.potatotool.controller;

import com.dlsc.gemsfx.CFCheckBox;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.leewyatt.rxcontrols.controls.RXPasswordField;
import com.potato.potatotool.utils.Constants;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;

/**
 * @author Potato
 * @date 2024/3/20 23:43
 */
public class PanePasswd {

    @FXML
    private AnchorPane an;

    @FXML
    private Label tipTitle;

    @FXML
    private BorderPane topBar;

    @FXML
    private RXPasswordField passwd;

    @FXML
    private CFCheckBox checkBox;

    private double offsetX,offsetY;

    private BooleanProperty isPasswdCorrect = new SimpleBooleanProperty(false);
    public BooleanProperty passwdProperty() {
        return isPasswdCorrect;
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

    String initPassword = "";

    @FXML
    public void start() {
        if(passwd.getText().equals("potato520")){
            Platform.runLater(() -> {
                boolean isRememberMe = checkBox.isSelected();
                if (isRememberMe && !initPassword.equals(passwd.getText())) {
                    rememberMe();
                }
                if (!isRememberMe &&  !initPassword.equals("")) {
                    deleteMe();
                }
                tipTitle.setText("密码正确，加载中……");
                isPasswdCorrect.set(true);
            });
        }else {
            tipTitle.setText("密码错误，请重试！");
        }
    }

    public void initialize() {
        passwd.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER) {
                    start();
                }
            }
        });

        JsonElement passwordObj = (JsonElement) Constants.getOutsideConfig("StartPassword");
        if(passwordObj != null){
            initPassword = passwordObj.getAsString();
            if (initPassword.equals("")){
                checkBox.setSelected(false);
            }else {
                passwd.setText(initPassword);
                checkBox.setSelected(true);
            }
        }
    }

    public void rememberMe(){
        String password = passwd.getText();
        Constants.saveConfig("StartPassword", password);
    }

    public void deleteMe(){
        String password = "";
        Constants.saveConfig("StartPassword", password);
    }

}
