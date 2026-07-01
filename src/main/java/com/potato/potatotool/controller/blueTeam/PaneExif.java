package com.potato.potatotool.controller.blueTeam;

import com.potato.potatotool.ToStart;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.misc.QRCodeDecoder;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Map;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.content.blueTeam.ExifUtils.*;

public class PaneExif {

    @FXML
    private StackPane sPane;

    @FXML
    private ListView listView;

    @FXML
    void initialize() {
        Platform.runLater(() -> {
            I18nUtils.bindComponents(sPane);
            applyStartupPreviewState();
        });
    }

    private void applyStartupPreviewState() {
        PaneExifPreviewSupport.PreviewState previewState =
                PaneExifPreviewSupport.buildPreviewState(ToStart.getStartupTestPage());
        if (previewState == null) {
            return;
        }
        renderPreviewState(previewState);
    }

    private void renderPreviewState(PaneExifPreviewSupport.PreviewState previewState) {
        listView.getItems().clear();
        if (previewState.isUnsupported()) {
            listView.getItems().add(buildCenterLabel(I18nUtils.getString("exif.unsupported")));
            return;
        }

        boolean hasSpecial = false;
        String qrText = previewState.getQrText();
        if (qrText != null && !"读取错误".equals(qrText)) {
            addChipRow(I18nUtils.getString("exif.qrcode"), qrText, "exif-chip-qr");
            hasSpecial = true;
        }

        Map<String, String> metadataMap = previewState.getMetadataMap();
        if (metadataMap.containsKey("GPS经度") && metadataMap.containsKey("GPS纬度")) {
            addChipRow("GPS具体定位", getPosition(metadataMap.get("GPS经度"), metadataMap.get("GPS纬度")), "exif-chip-gps");
            hasSpecial = true;
        }

        if (!metadataMap.isEmpty()) {
            addSectionHeader(I18nUtils.getString("exif.section.metadata"), metadataMap.size());
            for (Map.Entry<String, String> entry : metadataMap.entrySet()) {
                addTableRow(entry.getKey(), entry.getValue());
            }
        }
    }

    private void populateFromResult(String qrText, Map<String, String> metadataMap) {
        listView.getItems().clear();
        if (metadataMap.isEmpty() && qrText.equals("读取错误")) {
            listView.getItems().add(buildCenterLabel(I18nUtils.getString("exif.unsupported")));
            return;
        }

        if (!qrText.equals("读取错误")) {
            addChipRow(I18nUtils.getString("exif.qrcode"), qrText, "exif-chip-qr");
        }

        if (metadataMap.containsKey("GPS经度") && metadataMap.containsKey("GPS纬度")) {
            addChipRow("GPS具体定位",
                    getPosition(metadataMap.get("GPS经度"), metadataMap.get("GPS纬度")),
                    "exif-chip-gps");
        }

        if (!metadataMap.isEmpty()) {
            addSectionHeader(I18nUtils.getString("exif.section.metadata"), metadataMap.size());
            for (Map.Entry<String, String> entry : metadataMap.entrySet()) {
                addTableRow(entry.getKey(), entry.getValue());
            }
        }
    }

    private void addChipRow(String title, String value, String chipStyle) {
        Label chipLabel = new Label(title);
        chipLabel.getStyleClass().addAll("exif-chip", chipStyle);

        TextField valueField = new TextField(value);
        valueField.setEditable(false);
        valueField.getStyleClass().add("exif-value-field");
        HBox.setHgrow(valueField, Priority.ALWAYS);

        HBox row = new HBox(10, chipLabel, valueField);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("exif-chip-row");
        listView.getItems().add(row);
    }

    private void addSectionHeader(String title, int count) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("exif-section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label countLabel = new Label(count + " " + I18nUtils.getString("exif.section.fields"));
        countLabel.getStyleClass().add("exif-section-count");

        HBox header = new HBox(8, titleLabel, spacer, countLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("exif-section-header");
        listView.getItems().add(header);
    }

    private void addTableRow(String field, String value) {
        Label fieldLabel = new Label(field);
        fieldLabel.getStyleClass().add("exif-field-label");
        fieldLabel.setMinWidth(160);
        fieldLabel.setMaxWidth(160);

        TextField valueField = new TextField(value);
        valueField.setEditable(false);
        valueField.getStyleClass().add("exif-value-field");
        HBox.setHgrow(valueField, Priority.ALWAYS);

        HBox row = new HBox(16, fieldLabel, valueField);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("exif-table-row");
        listView.getItems().add(row);
    }

    private HBox buildCenterLabel(String text) {
        Label tipLabel = new Label(text);
        tipLabel.setId("tipTitle");
        HBox hbox = new HBox(tipLabel);
        hbox.setPrefHeight(sPane.getHeight() - 220);
        hbox.setAlignment(Pos.CENTER);
        return hbox;
    }

    @FXML
    public void getExifFx(ActionEvent e) {
        listView.getItems().clear();

        FileChooser chooser = new FileChooser();
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        String path = null;
        try {
            path = chooser.showOpenDialog(stage).getAbsolutePath();
        } catch (Exception exception) {
            if (debugMode) System.out.println("没有文件被选择");
            return;
        }

        if (path == null) {
            if (debugMode) System.out.println("没有文件被选择");
            return;
        }

        listView.getItems().add(buildCenterLabel(I18nUtils.getString("exif.querying")));

        String finalPath = path;
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws IOException {
                String qrText = QRCodeDecoder.qrToText(finalPath);
                Map<String, String> metadataMap = getExif(finalPath);
                Platform.runLater(() -> populateFromResult(qrText, metadataMap));
                return null;
            }
        };
        task.setOnFailed(er -> {
            Throwable error = task.getException();
            if (debugMode && error != null) {
                error.printStackTrace();
            }
            Platform.runLater(() -> {
                listView.getItems().clear();
                String detail = error == null || error.getMessage() == null ? "" : error.getMessage();
                listView.getItems().add(buildCenterLabel(I18nUtils.getString("common.task.failed", detail)));
            });
        });
        Thread exifTaskThread = new Thread(task);
        exifTaskThread.setDaemon(true);
        exifTaskThread.start();
    }
}
