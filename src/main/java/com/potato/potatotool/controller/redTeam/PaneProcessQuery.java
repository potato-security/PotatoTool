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

    /** 常见杀软/EDR 清单（真实进程名），用于对齐 Penpot 稿的「检测到正常 / 未检测灰显」核对表 */
    private static final String[][] COMMON_AVS = {
            {"Windows Defender", "msmpeng.exe", "AV"},
            {"360 Total Security", "360tray.exe", "AV"},
            {"Kaspersky", "avp.exe", "AV"},
            {"McAfee", "mcshield.exe", "AV"},
            {"Avast", "aswidsagent.exe", "AV"},
            {"MalwareBytes", "mbamservice.exe", "AM"},
            {"Carbon Black", "cb.exe", "EDR"},
            {"CrowdStrike Falcon", "csfalconservice.exe", "EDR"},
            {"SentinelOne", "sentinelagent.exe", "EDR"},
            {"Symantec EP", "ccsvchst.exe", "AV"},
    };

    @FXML
    public void getInfo(ActionEvent event) {
        String inputStr = inputText.getText();
        renderAvChecklist(antivirus, inputStr, getAntivirusProcesses(avJsonOjb, inputStr));
        renderRows(canRoot, getCanRootProcesses(tqJsonOjb, inputStr));
    }

    /**
     * 安全软件检测：先列常见杀软/EDR 核对表（真实检测=正常、未检测=灰显，对齐 Penpot 稿），
     * 再把命中但不在常见清单里的杀软追加为正常行，保证不丢任何真实检测结果。未检测=诚实报告"输入里没发现"，非伪造。
     */
    private void renderAvChecklist(VBox container, String input, JSONObject matched) {
        if (container == null) return;
        container.getChildren().clear();
        String low = input == null ? "" : input.toLowerCase();
        java.util.Set<String> shownProc = new java.util.HashSet<String>();
        for (String[] av : COMMON_AVS) {
            boolean detected = low.contains(av[1]);
            shownProc.add(av[1]);
            addAvRow(container, av[0], av[2], detected);
        }
        if (matched != null) {
            java.util.Iterator<String> keys = matched.keys();
            while (keys.hasNext()) {
                String proc = keys.next();
                if (proc != null && shownProc.contains(proc.toLowerCase())) continue;
                String desc = matched.get(proc) == null ? "" : String.valueOf(matched.get(proc));
                addAvRow(container, desc + " (" + proc + ")", "AV", true);
            }
        }
    }

    private void addAvRow(VBox container, String name, String type, boolean detected) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("proc-row-name");
        nameLabel.setMinWidth(210);
        nameLabel.setPrefWidth(210);
        Label typeLabel = new Label(type);
        typeLabel.getStyleClass().add("proc-row-type");
        HBox row = new HBox(8, nameLabel, typeLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("proc-row");
        if (!detected) row.getStyleClass().add("proc-row-undetected");
        container.getChildren().add(row);
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
