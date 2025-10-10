package com.potato.potatotool.controller.publicPane;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import static com.potato.potatotool.ToStart.isBlueMode;

/**
 * 删除确认对话框控制器
 * @author Potato
 * @date 2025/10/09
 */
public class PaneDeleteConfirmDialog {
    @FXML
    private AnchorPane an;

    @FXML
    private BorderPane topBar;
    
    @FXML
    private Label titleLabel;
    
    @FXML
    private Label describeLabel;

    private double offsetX, offsetY;
    
    // 标志位：是否确认删除
    public static boolean deleteConfirmed = false;

    public void initialize() {
        // 初始化时重置标志位
        deleteConfirmed = false;
        
        if (isBlueMode) {
            an.getStyleClass().remove("redStyle");
            an.getStyleClass().add("blueStyle");
        } else {
            an.getStyleClass().remove("blueStyle");
            an.getStyleClass().add("redStyle");
        }
    }
    
    /**
     * 设置要删除的项目信息
     */
    public void setDeleteInfo(String title, String describe) {
        titleLabel.setText(title);
        describeLabel.setText(describe);
    }

    @FXML
    void confirmDelete() {
        // 设置确认删除标志
        deleteConfirmed = true;
        ((Stage) an.getScene().getWindow()).close();
    }

    @FXML
    void topBarDraggedAction(MouseEvent event) {
        Window window = topBar.getScene().getWindow();
        window.setX(event.getScreenX() - offsetX);
        window.setY(event.getScreenY() - offsetY);
    }

    @FXML
    void topBarPressedAction(MouseEvent event) {
        offsetX = event.getSceneX();
        offsetY = event.getSceneY();
    }

    @FXML
    public void exitAction() {
        ((Stage) an.getScene().getWindow()).close();
    }
}

