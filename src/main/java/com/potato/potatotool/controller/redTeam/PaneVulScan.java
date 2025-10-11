package com.potato.potatotool.controller.redTeam;

import com.potato.potatotool.utils.core.I18nUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

/**
 * @author Potato
 * @date 2023/3/21 17:08
 */
public class PaneVulScan {
    @FXML
    private StackPane sPane;

    @FXML
    void initialize() {
        // 绑定国际化
        Platform.runLater(() -> {
                I18nUtils.bindComponents(sPane);
        });
    }
}
