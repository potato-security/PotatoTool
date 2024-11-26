package com.potato.potatotool.controller;

import com.potato.potatotool.utils.QRCodeDecoder;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Map;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.content.blueTeam.exifUtils.*;

/**
 * @author Potato
 * @date 2024/4/27 19:14
 */
public class PaneExif {

    @FXML
    private StackPane sPane;

    @FXML
    private ListView listView;

    @FXML
    public void getExifFx(ActionEvent e) {
        listView.getItems().clear();

        FileChooser chooser = new FileChooser();

        Stage stage = (Stage) ((Node)e.getSource()).getScene().getWindow();
        String path = null;
        try {
            path = chooser.showOpenDialog(stage).getAbsolutePath();
        }catch (Exception exception){
            if(debugMode) System.out.println("没有文件被选择");
            return;
        }

        if (path == null) {
            if(debugMode) System.out.println("没有文件被选择");
            return;
        }

        Label tipLabel = new Label("检索信息中……");
        tipLabel.setId("tipTitle");
        HBox hbox = new HBox(tipLabel);
        hbox.setPrefHeight(sPane.getHeight()-220);
        hbox.setAlignment(Pos.CENTER);
        listView.getItems().add(hbox);


        String finalPath = path;
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws IOException {

                String qrText = QRCodeDecoder.qrToText(finalPath);
                Map<String, String> metadataMap = getExif(finalPath);


                Platform.runLater(() -> {
                    if (metadataMap.isEmpty() && qrText.equals("读取错误")) {
                        listView.getItems().clear();
                        Label tipLabel_tmp = new Label("不支持该类型文件");
                        tipLabel_tmp.setId("tipTitle");
                        HBox hbox_tmp = new HBox(tipLabel_tmp);
                        hbox_tmp.setPrefHeight(sPane.getHeight() - 220);
                        hbox_tmp.setAlignment(Pos.CENTER);
                        listView.getItems().add(hbox_tmp);

                        return;
                    } else {
                        listView.getItems().clear();
                    }

                    if (!qrText.equals("读取错误")) {
                        Label tipLabel_tmp = new Label("二维码解析");
                        tipLabel_tmp.setId("tipTitle");
                        Label tipSymbolLabel = new Label("：");
                        tipSymbolLabel.setId("tipSymbol");
                        TextField tipTextField = new TextField(qrText);
                        tipTextField.setPrefWidth((sPane.getWidth() / 2 - 200));
                        HBox hbox_tmp = new HBox(tipLabel_tmp, tipSymbolLabel, tipTextField);
                        hbox_tmp.setSpacing(20);
                        hbox_tmp.setAlignment(Pos.CENTER);
                        hbox_tmp.getStyleClass().add("linenoPane");

                        listView.getItems().add(hbox_tmp);
                    }

                    if (metadataMap.containsKey("GPS经度") && metadataMap.containsKey("GPS纬度")) {
                        String position = getPosition(metadataMap.get("GPS经度"), metadataMap.get("GPS纬度"));

                        Label tipLabel_tmp = new Label("GPS具体定位");
                        tipLabel_tmp.setId("tipTitle");
                        Label tipSymbolLabel = new Label("：");
                        tipSymbolLabel.setId("tipSymbol");
                        TextField tipTextField = new TextField(position);
                        tipTextField.setPrefWidth(sPane.getWidth() / 2 - 200);
                        HBox hbox_tmp = new HBox(tipLabel_tmp, tipSymbolLabel, tipTextField);
                        hbox_tmp.setSpacing(20);
                        hbox_tmp.setAlignment(Pos.CENTER);
                        hbox_tmp.getStyleClass().add("linenoPane");

                        listView.getItems().add(hbox_tmp);
                    }

                    for (Map.Entry<String, String> entry : metadataMap.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();

                        Label tipLabel_tmp = new Label(key);
                        tipLabel_tmp.setId("tipTitle");
                        tipLabel_tmp.setPrefWidth(200);
                        tipLabel_tmp.setAlignment(Pos.CENTER);
                        Label tipSymbolLabel = new Label("：");
                        tipSymbolLabel.setId("tipSymbol");
                        TextField tipTextField = new TextField(value);
                        tipTextField.setPrefWidth(sPane.getWidth() / 2 - 200);
                        HBox hbox_tmp = new HBox(tipLabel_tmp, tipSymbolLabel, tipTextField);
                        hbox_tmp.setSpacing(20);
                        hbox_tmp.setAlignment(Pos.CENTER);

                        listView.getItems().add(hbox_tmp);
                    }
                });
                return null;
            }
        };
        task.setOnFailed(er -> {
            Throwable error = task.getException();
            error.printStackTrace();
        });
        new Thread(task).start();

    }
}
