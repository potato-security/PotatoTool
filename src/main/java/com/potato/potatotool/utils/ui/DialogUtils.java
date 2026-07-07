package com.potato.potatotool.utils.ui;

import com.potato.potatotool.controller.publicPane.PaneDeleteConfirmDialog;
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
                if (setStage != null) {
                    setStage.close();
                }

                setStage = new Stage();
                setStage.initOwner(owner);
                setStage.initModality(Modality.WINDOW_MODAL);
                setStage.initStyle(StageStyle.TRANSPARENT);
                setStage.setAlwaysOnTop(true);

                FXMLLoader loader = new FXMLLoader(DialogUtils.class.getResource("/fxml/publicPane/setting.fxml"));
                loader.setClassLoader(DialogUtils.class.getClassLoader());
                AnchorPane dialogRoot = loader.load();
                PaneSetting setController = loader.getController();

                Scene scene = new Scene(dialogRoot);
                scene.getStylesheets().add(Constants.getResourceUrl("/css/theme.css"));
                scene.getStylesheets().add(Constants.getResourceUrl("/css/common.css"));
                scene.getStylesheets().add(Constants.getResourceUrl("/css/components.css"));
                scene.setFill(null);    //  背景透明
                setStage.setScene(scene);
                setStage.setTitle("修改配置信息");
                setStage.setOnHidden(event -> {
                    if (setStage != null && !setStage.isShowing()) {
                        setStage = null;
                    }
                });

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

    /**
     * 显示危险确认弹窗（非阻塞 show，供测试直达/复用；业务侧删除流程仍走 PaneExtension 内 showAndWait）。
     * @param owner    父窗口
     * @param title    删除目标标题
     * @param describe 删除说明
     */
    public static void showDeleteConfirm(Window owner, String title, String describe) {
        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                stage.initOwner(owner);
                stage.initModality(Modality.WINDOW_MODAL);
                stage.initStyle(StageStyle.TRANSPARENT);
                stage.setAlwaysOnTop(true);

                FXMLLoader loader = new FXMLLoader(DialogUtils.class.getResource("/fxml/publicPane/deleteConfirmDialog.fxml"));
                loader.setClassLoader(DialogUtils.class.getClassLoader());
                AnchorPane dialogRoot = loader.load();
                PaneDeleteConfirmDialog controller = loader.getController();
                controller.setDeleteInfo(title, describe);

                Scene scene = new Scene(dialogRoot);
                scene.getStylesheets().add(Constants.getResourceUrl("/css/theme.css"));
                scene.getStylesheets().add(Constants.getResourceUrl("/css/common.css"));
                scene.getStylesheets().add(Constants.getResourceUrl("/css/components.css"));
                scene.setFill(null);
                stage.setScene(scene);
                stage.setTitle("危险操作确认");
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
