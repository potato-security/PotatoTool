package com.potato.potatotool.controller;

import com.potato.potatotool.utils.jsonUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import org.json.JSONObject;

import static com.potato.potatotool.content.taskListCheck.*;

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
    private TextArea inputText;

    @FXML
    private TextArea antivirus;

    @FXML
    private TextArea canRoot;

    @FXML
    public void getInfo(ActionEvent event) {
        String inputStr = inputText.getText();
        JSONObject antivirusProcesses = getAntivirusProcesses(avJsonOjb, inputStr);
        String antivirusProcessesStr = jsonUtils.formatOneDimensionJsonToStr(antivirusProcesses, 20, "center");
        antivirus.setText(antivirusProcessesStr);

        JSONObject canRootProcesses = getCanRootProcesses(tqJsonOjb, inputStr);
        String canRootProcessesStr = jsonUtils.formatOneDimensionJsonToStr(canRootProcesses, 20, "center");
        canRoot.setText(canRootProcessesStr);
    }


}
