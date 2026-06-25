package com.potato.potatotool.controller.redTeam;

import com.potato.potatotool.ToStart;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.data.JsonUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
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
    private TextArea antivirus;

    @FXML
    private TextArea canRoot;
    
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
        JSONObject antivirusProcesses = getAntivirusProcesses(avJsonOjb, inputStr);
        String antivirusProcessesStr = JsonUtils.formatOneDimensionJsonToStr(antivirusProcesses, 20, "center");
        antivirus.setText(antivirusProcessesStr);

        JSONObject canRootProcesses = getCanRootProcesses(tqJsonOjb, inputStr);
        String canRootProcessesStr = JsonUtils.formatOneDimensionJsonToStr(canRootProcesses, 20, "center");
        canRoot.setText(canRootProcessesStr);
    }


}
