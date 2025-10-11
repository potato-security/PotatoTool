package com.potato.potatotool.controller.publicPane;

import com.dlsc.gemsfx.CFCheckBox;
import com.google.gson.JsonElement;
import com.leewyatt.rxcontrols.controls.RXPasswordField;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.core.ExecutorServiceManager;
import com.potato.potatotool.utils.core.I18nManager;
import com.potato.potatotool.utils.core.I18nUtils;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;

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
    
    private I18nManager i18n = I18nManager.getInstance();
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
        ExecutorServiceManager.shutdownAll();
        Platform.exit();
        System.exit(0);
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
                tipTitle.setText(i18n.getString("passwd.correct"));
                isPasswdCorrect.set(true);
            });
        }else {
            tipTitle.setText(i18n.getString("passwd.error"));
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

        JsonElement passwordObj = (JsonElement) Constants.getOutsideConfig(ConfigConstants.START_PASSWORD);
        if(passwordObj != null){
            initPassword = passwordObj.getAsString();
            if (initPassword.equals("")){
                checkBox.setSelected(false);
            }else {
                passwd.setText(initPassword);
                checkBox.setSelected(true);
            }
        }
        
        // 绑定国际化
        Platform.runLater(() -> I18nUtils.bindComponents(an));
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
