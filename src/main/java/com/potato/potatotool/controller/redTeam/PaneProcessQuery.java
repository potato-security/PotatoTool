package com.potato.potatotool.controller.redTeam;

import com.potato.potatotool.ToStart;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.data.JsonUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.json.JSONObject;

import static com.potato.potatotool.content.redTeam.TaskListCheck.*;

/**
 * @author Potato
 * @date 2023/3/21 17:15
 */
public class PaneProcessQuery {
    private static final String PROCESS_RESULT_PREVIEW_TEXT =
            "System Idle Process\n" +
            "System\n" +
            "smss.exe\n" +
            "csrss.exe\n" +
            "wininit.exe\n" +
            "services.exe\n" +
            "lsass.exe\n" +
            "svchost.exe\n" +
            "explorer.exe\n" +
            "QQProtect.exe\n" +
            "360tray.exe\n" +
            "360sd.exe\n" +
            "ZhuDongFangYu.exe\n" +
            "HipsMain.exe\n" +
            "HipsTray.exe\n" +
            "AliYunDun.exe\n" +
            "AliYunDunUpdate.exe\n" +
            "aegis_cli.exe\n" +
            "aegis_update.exe\n" +
            "vmtoolsd.exe\n" +
            "msbuild.exe\n" +
            "powershell.exe\n" +
            "cmd.exe\n" +
            "rundll32.exe\n" +
            "regsvr32.exe\n" +
            "wmic.exe\n" +
            "certutil.exe\n" +
            "bitsadmin.exe\n" +
            "mshta.exe\n" +
            "wscript.exe\n" +
            "cscript.exe\n" +
            "curl.exe\n" +
            "nc.exe";

    static JSONObject avJsonOjb;
    static JSONObject tqJsonOjb;

    static {
        JSONObject taskListJsonObject = init();
        avJsonOjb = (JSONObject) taskListJsonObject.get("avList");
        tqJsonOjb = (JSONObject) taskListJsonObject.get("processList");
    }
    
    @FXML
    private StackPane sPane;

    @FXML
    private TextArea inputText;

    @FXML
    private VBox antivirus;

    @FXML
    private VBox canRoot;
    
    @FXML
    void initialize() {
        // 绑定国际化
        Platform.runLater(() -> {
            I18nUtils.bindComponents(sPane);
            applyStartupPreviewState();
        });
    }

    private void applyStartupPreviewState() {
        if (ToStart.getStartupTestPage() == ToStart.StartupPage.RED_PROCESS_QUERY_RESULT) {
            inputText.setText(PROCESS_RESULT_PREVIEW_TEXT);
            getInfo(null);
        }
    }

    @FXML
    public void getInfo(ActionEvent event) {
        String inputStr = inputText.getText();
        renderRows(antivirus, getAntivirusProcesses(avJsonOjb, inputStr));
        renderRows(canRoot, getCanRootProcesses(tqJsonOjb, inputStr));
    }

    /** 将匹配结果渲染为 NAME/TYPE 双列样式行（对齐 Penpot 稿的逐行呈现，数据仍为工具匹配到的项） */
    private void renderRows(VBox container, JSONObject data) {
        if (container == null) return;
        container.getChildren().clear();
        if (data == null) return;
        java.util.Iterator<String> keys = data.keys();
        while (keys.hasNext()) {
            String name = keys.next();
            String value = data.get(name) == null ? "" : String.valueOf(data.get(name));
            Label nameLabel = new Label(name);
            nameLabel.getStyleClass().add("proc-row-name");
            nameLabel.setMinWidth(150);
            nameLabel.setPrefWidth(150);
            Label valueLabel = new Label(value);
            valueLabel.getStyleClass().add("proc-row-type");
            valueLabel.setWrapText(true);
            HBox row = new HBox(8, nameLabel, valueLabel);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("proc-row");
            container.getChildren().add(row);
        }
    }


}
