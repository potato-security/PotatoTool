package com.potato.potatotool.controller.blueTeam;

import com.potato.potatotool.ToStart;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.misc.QRCodeDecoder;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Map;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.content.blueTeam.ExifUtils.*;

public class PaneExif {

    @FXML private StackPane sPane;
    @FXML private ListView listView;
    @FXML private ScrollPane exifPanelScrollPane;
    @FXML private VBox exifSummaryBox;
    @FXML private VBox exifStatesBox;
    private boolean exifPanelVisible = false;

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

        populateExifPanel(qrText, metadataMap);
        showExifPanel();
    }

    private void populateExifPanel(String qrText, Map<String, String> metadataMap) {
        if (exifSummaryBox == null) return;
        exifSummaryBox.getChildren().clear();
        exifStatesBox.getChildren().clear();

        // Device info
        String model = metadataMap.getOrDefault("机型", metadataMap.getOrDefault("Model", null));
        if (model != null && !model.isEmpty()) {
            addExifSummaryItem(I18nUtils.getString("exif.panel.device"), model, true);
        }

        // GPS
        if (metadataMap.containsKey("GPS经度") && metadataMap.containsKey("GPS纬度")) {
            String gps = getPosition(metadataMap.get("GPS经度"), metadataMap.get("GPS纬度"));
            addExifSummaryItem(I18nUtils.getString("exif.panel.gps"), gps, false);
        }

        // QR
        if (!qrText.equals("读取错误") && !qrText.isEmpty()) {
            String displayQr = qrText.length() > 30 ? qrText.substring(0, 30) + "…" : qrText;
            addExifSummaryItem(I18nUtils.getString("exif.panel.qr"), displayQr, false);
        }

        // Photo params
        String iso = metadataMap.get("ISO感光度");
        String aperture = metadataMap.getOrDefault("光圈", metadataMap.get("光圈值"));
        if (iso != null || aperture != null) {
            String params = (iso != null ? "ISO " + iso : "") +
                            (iso != null && aperture != null ? " / " : "") +
                            (aperture != null ? "f/" + aperture : "");
            addExifSummaryItem(I18nUtils.getString("exif.panel.params"), params, false);
        }

        // States
        addExifStateChip(I18nUtils.getString("exif.panel.state.parsed"), "exif-state-dot-success");
        if (!metadataMap.isEmpty()) {
            addExifStateChip(metadataMap.size() + " " + I18nUtils.getString("exif.section.fields"), "exif-state-dot-info");
        }
    }

    private void addExifSummaryItem(String label, String value, boolean highlighted) {
        VBox item = new VBox(2);
        item.getStyleClass().add("exif-summary-item");
        if (highlighted) item.getStyleClass().add("exif-summary-item-selected");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("exif-summary-label");
        Label val = new Label(value);
        val.getStyleClass().add("exif-summary-value");
        val.setWrapText(true);
        item.getChildren().addAll(lbl, val);
        exifSummaryBox.getChildren().add(item);
    }

    private void addExifStateChip(String text, String dotStyle) {
        HBox chip = new HBox(6);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("exif-state-chip");
        Region dot = new Region();
        dot.getStyleClass().addAll("exif-state-dot", dotStyle);
        Label lbl = new Label(text);
        lbl.getStyleClass().add("exif-state-label");
        chip.getChildren().addAll(dot, lbl);
        exifStatesBox.getChildren().add(chip);
    }

    private void showExifPanel() {
        // EXIF 面板改为常驻并排(对齐设计稿),始终可见,仅确保就位。
        if (exifPanelScrollPane == null) return;
        exifPanelVisible = true;
        exifPanelScrollPane.setTranslateX(0);
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
