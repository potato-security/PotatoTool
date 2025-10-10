package com.potato.potatotool.utils.ui;

import com.potato.potatotool.controller.publicPane.PaneSetting;
import com.potato.potatotool.utils.core.Constants;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * @author Potato
 * @date 2025/9/30 16:54
 */
public class DialogUtils {
    private static Stage setStage;
    private static PaneSetting setController;

    /**
     * 显示设置对话框
     * @param owner 父窗口
     */
    public static void showSet(Window owner) {
        showSet(owner, null);
    }
    
    /**
     * 显示设置对话框并定位到指定配置项
     * @param owner 父窗口
     * @param configKey 配置项的 key（如 "Fofa_Key"），null 则使用默认展开
     */
    public static void showSet(Window owner, String configKey) {
        Platform.runLater(() -> {
            try {
                if (setStage == null) {
                    setStage = new Stage();
                    setStage.initOwner(owner);
                    setStage.initModality(Modality.WINDOW_MODAL);
                    setStage.initStyle(StageStyle.TRANSPARENT);
                    setStage.setAlwaysOnTop(true);

                    FXMLLoader loader = new FXMLLoader(DialogUtils.class.getResource("/fxml/publicPane/setting.fxml"));
                    AnchorPane dialogRoot = loader.load();
                    setController = loader.getController();  // 保存控制器引用
                    
                    Scene scene = new Scene(dialogRoot);
                    scene.getStylesheets().add(Constants.getResourceUrl("/css/common.css"));
                    scene.setFill(null);    //  背景透明
                    setStage.setScene(scene);
                    setStage.setTitle("修改配置信息");
                }
                
                // 如果指定了 configKey，则定位到该配置项
                if (configKey != null && setController != null) {
                    setController.navigateToConfig(configKey);
                }
                
                setStage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
