package com.potato.potatotool.controller.redTeam;

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
        Platform.runLater(() -> I18nUtils.bindComponents(sPane));
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
