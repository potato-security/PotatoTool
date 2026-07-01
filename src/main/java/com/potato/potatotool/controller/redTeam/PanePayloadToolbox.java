package com.potato.potatotool.controller.redTeam;

import com.dlsc.gemsfx.CFCheckBox;
import com.potato.potatotool.ToStart;
import com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants;
import com.potato.potatotool.content.redTeam.memshell.util.ClassNameUtil;
import com.potato.potatotool.content.redTeam.memshell.util.MemoryShellRandomUtil;
import com.potato.potatotool.content.redTeam.payload.PayloadFormat;
import com.potato.potatotool.content.redTeam.payload.PayloadOutputRequest;
import com.potato.potatotool.content.redTeam.payload.PayloadOutputResult;
import com.potato.potatotool.content.redTeam.payload.PayloadOutputService;
import com.potato.potatotool.content.redTeam.payload.detector.DetectorPayloadRequest;
import com.potato.potatotool.content.redTeam.payload.detector.DetectorPayloadService;
import com.potato.potatotool.content.redTeam.payload.detector.DetectorType;
import com.potato.potatotool.content.redTeam.payload.yso.YsoPayloadCategory;
import com.potato.potatotool.content.redTeam.payload.yso.YsoDisplayHintUtil;
import com.potato.potatotool.content.redTeam.payload.yso.YsoPayloadInputType;
import com.potato.potatotool.content.redTeam.payload.yso.YsoPayloadMetadata;
import com.potato.potatotool.content.redTeam.payload.yso.YsoPayloadRequest;
import com.potato.potatotool.content.redTeam.payload.yso.YsoPayloadService;
import com.potato.potatotool.content.redTeam.vulnScanner.http.DnsLogService;
import com.potato.potatotool.content.redTeam.vulnScanner.http.HttpLogService;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.ui.SmoothScrollPane;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PanePayloadToolbox {
    private static final String CATEGORY_ITEM_PREFIX = "payload.yso.category.item.";

    @FXML
    private StackPane sPane;
    @FXML
    private SmoothScrollPane rootScrollPane;
    @FXML
    private TabPane payloadTabPane;
    @FXML
    private Tab detectorTab;
    @FXML
    private Tab ysoTab;
    @FXML
    private ToggleGroup detectorTypeGroup;
    @FXML
    private Label leftLabel;
    @FXML
    private TextField detectorClassNameTextField;
    @FXML
    private TextField dnsDomainTextField;
    @FXML
    private TextField httpBaseUrlTextField;
    @FXML
    private TextField sleepSecondsTextField;
    @FXML
    private ComboBox serverTypeBox;
    @FXML
    private ComboBox ysoCategoryBox;
    @FXML
    private ComboBox ysoGadgetBox;
    @FXML
    private CFCheckBox ysoClassFileOnlyCheckBox;
    @FXML
    private TextField ysoInputTypeTextField;
    @FXML
    private TextField ysoCommandTextField;
    @FXML
    private TextField ysoCapabilityTextField;
    @FXML
    private TextField ysoDependencyHintTextField;
    @FXML
    private TextField ysoVerificationHintTextField;
    @FXML
    private ComboBox ysoHelperTypeBox;
    @FXML
    private TextField ysoHelperValueTextField;
    @FXML
    private Button ysoHelperApplyButton;
    @FXML
    private ComboBox outputFormatBox;
    @FXML
    private TextField outputPathTextField;
    @FXML
    private TextArea outTextArea;

    private final DetectorPayloadService detectorPayloadService = new DetectorPayloadService();
    private final YsoPayloadService ysoPayloadService = new YsoPayloadService();
    private boolean outputFormatsInitialized;

    public void initialize() {
        detectorTypeGroup.selectToggle(detectorTypeGroup.getToggles().get(0));
        serverTypeBox.setItems(FXCollections.observableArrayList(MemoryShellConstants.SERVER_TOMCAT,
                MemoryShellConstants.SERVER_WEBLOGIC,
                MemoryShellConstants.SERVER_RESIN,
                MemoryShellConstants.SERVER_JETTY,
                MemoryShellConstants.SERVER_WEBSPHERE,
                MemoryShellConstants.SERVER_UNDERTOW,
                MemoryShellConstants.SERVER_TONGWEB,
                MemoryShellConstants.SERVER_APUSIC,
                MemoryShellConstants.SERVER_SPRING_MVC));
        serverTypeBox.setValue(MemoryShellConstants.SERVER_TOMCAT);
        ysoCategoryBox.setItems(FXCollections.observableArrayList(categoryItems()));
        ysoCategoryBox.setValue(categoryItem(YsoPayloadCategory.ALL));
        ysoHelperTypeBox.setItems(FXCollections.observableArrayList(YsoCommandHelperType.values()));
        ysoHelperTypeBox.setValue(YsoCommandHelperType.DNSLOG);
        refreshYsoGadgets();
        refreshOutputFormats();
        refreshDetectorInputs();
        refreshYsoInputs();
        refreshOutputPathState();
        payloadTabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
            @Override
            public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
                refreshOutputFormats();
                refreshOutputPathState();
            }
        });
        detectorTypeGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            @Override
            public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) {
                refreshDetectorInputs();
            }
        });
        ysoCategoryBox.valueProperty().addListener(new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
                refreshYsoGadgets();
            }
        });
        ysoClassFileOnlyCheckBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                refreshYsoGadgets();
            }
        });
        ysoGadgetBox.valueProperty().addListener(new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
                refreshYsoInputs();
            }
        });
        ysoHelperTypeBox.valueProperty().addListener(new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
                refreshYsoHelperState();
            }
        });
        outputFormatBox.valueProperty().addListener(new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
                refreshOutputPathState();
            }
        });
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                I18nUtils.bindComponents(sPane);
                refreshYsoInputs();
                applyStartupPreviewState();
            }
        });
    }

    private void applyStartupPreviewState() {
        ToStart.StartupPage startupPage = ToStart.getStartupTestPage();
        if (startupPage == null) {
            return;
        }
        if (startupPage == ToStart.StartupPage.RED_PAYLOAD_TOOLBOX_YSO) {
            payloadTabPane.getSelectionModel().select(ysoTab);
            refreshYsoInputs();
            return;
        }
        if (startupPage == ToStart.StartupPage.RED_PAYLOAD_TOOLBOX_YSO_HELPER) {
            prepareYsoHelperPreview();
            return;
        }
        if (startupPage == ToStart.StartupPage.RED_PAYLOAD_TOOLBOX_RESULT) {
            prepareYsoResultPreview();
        }
    }

    private void prepareYsoHelperPreview() {
        payloadTabPane.getSelectionModel().select(ysoTab);
        ysoGadgetBox.setValue("CommonsBeanutils1");
        refreshYsoInputs();
        ysoHelperTypeBox.setValue(YsoCommandHelperType.DNSLOG);
        refreshYsoHelperState();
        ysoHelperValueTextField.setText("preview.dnslog.cn");
    }

    private void prepareYsoResultPreview() {
        prepareYsoHelperPreview();
        try {
            String command = buildStructuredYsoCommand(selectedYsoHelperType(), normalizedText(ysoHelperValueTextField));
            ysoCommandTextField.setText(command);
            byte[] payloadBytes = generateYsoBytes();
            showResult(PayloadOutputService.format(PayloadOutputRequest.builder(payloadBytes, selectedPayloadFormat())
                    .className("org.example.Payload")
                    .outputPath(normalizedOutputPath(selectedPayloadFormat()))
                    .build()));
            scrollToResultPreview();
        } catch (Exception e) {
            String detail = e.getMessage() == null ? e.toString() : e.getMessage();
            outTextArea.setText(I18nUtils.getString("payload.error.generate") + ": " + detail);
            scrollToResultPreview();
        }
    }

    private void scrollToResultPreview() {
        if (rootScrollPane == null) {
            return;
        }
        PauseTransition pauseTransition = new PauseTransition(Duration.millis(250));
        pauseTransition.setOnFinished(event -> Platform.runLater(new Runnable() {
            @Override
            public void run() {
                rootScrollPane.setVvalue(1.0);
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        rootScrollPane.setVvalue(1.0);
                    }
                });
            }
        }));
        pauseTransition.play();
    }

    @FXML
    public void chooseYsoClassFile(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18nUtils.getString("memshell.classfile.filter"), "*.class"));
        Stage stage = (Stage) ysoCommandTextField.getScene().getWindow();
        File selectedFile = chooser.showOpenDialog(stage);
        if (selectedFile != null) {
            YsoPayloadMetadata metadata = selectedYsoMetadata();
            if (metadata == null || !metadata.supportsClassFile()) {
                ysoGadgetBox.setValue(preferredCodeExecGadget());
            }
            ysoCommandTextField.setText("class_file:" + selectedFile.getAbsolutePath());
        }
    }

    @FXML
    public void generatePayload(ActionEvent event) {
        try {
            byte[] payloadBytes = isDetectorSelected() ? generateDetectorBytes() : generateYsoBytes();
            String className = isDetectorSelected() ? requireDetectorClassName() : "org.example.Payload";
            PayloadFormat format = selectedPayloadFormat();
            PayloadOutputResult result = PayloadOutputService.format(PayloadOutputRequest.builder(payloadBytes, format)
                    .className(className)
                    .outputPath(normalizedOutputPath(format))
                    .build());
            showResult(result);
        } catch (Exception e) {
            String detail = e.getMessage() == null ? e.toString() : e.getMessage();
            outTextArea.setText(I18nUtils.getString("payload.error.generate") + ": " + detail);
        }
    }

    @FXML
    public void applyYsoCommandHelper(ActionEvent event) {
        try {
            YsoPayloadMetadata metadata = selectedYsoMetadata();
            if (metadata == null || metadata.getInputType() != YsoPayloadInputType.WOODPECKER_COMMAND) {
                throw new IllegalArgumentException(I18nUtils.getString("payload.error.yso.helper.unsupported"));
            }
            YsoCommandHelperType helperType = selectedYsoHelperType();
            if (helperType == null) {
                throw new IllegalArgumentException(I18nUtils.getString("payload.error.yso.helper.unsupported"));
            }
            String command = buildStructuredYsoCommand(helperType, normalizedText(ysoHelperValueTextField));
            ysoCommandTextField.setText(command);
        } catch (Exception e) {
            String detail = e.getMessage() == null ? e.toString() : e.getMessage();
            outTextArea.setText(detail);
        }
    }

    private byte[] generateDetectorBytes() throws Exception {
        DetectorType detectorType = selectedDetectorType();
        String dnsDomain = normalizedText(dnsDomainTextField);
        String httpBaseUrl = normalizedText(httpBaseUrlTextField);
        if (detectorType == DetectorType.DNSLOG && dnsDomain.isEmpty()) {
            dnsDomain = DnsLogService.generateDnsLogDomain();
            dnsDomainTextField.setText(dnsDomain);
        }
        if (detectorType == DetectorType.HTTPLOG && httpBaseUrl.isEmpty()) {
            httpBaseUrl = HttpLogService.generateHttpLogUrl();
            httpBaseUrlTextField.setText(httpBaseUrl);
        }
        return detectorPayloadService.generate(new DetectorPayloadRequest(
                detectorType,
                requireDetectorClassName(),
                dnsDomain,
                httpBaseUrl,
                parseSleepSeconds(),
                selectedText(serverTypeBox)));
    }

    private byte[] generateYsoBytes() throws Exception {
        return ysoPayloadService.generate(new YsoPayloadRequest(selectedText(ysoGadgetBox), normalizedText(ysoCommandTextField)));
    }

    private void showResult(PayloadOutputResult result) {
        if (result.isFileOutput()) {
            outTextArea.setText(I18nUtils.getString("memshell.result.filepath") + ": " + result.getFilePath());
            return;
        }
        outTextArea.setText(new String(result.getBytes(), StandardCharsets.UTF_8));
    }

    private boolean isDetectorSelected() {
        return payloadTabPane.getSelectionModel().getSelectedItem() == detectorTab;
    }

    private DetectorType selectedDetectorType() {
        DetectorType t = selectedDetectorTypeOrNull();
        if (t == null) throw new IllegalArgumentException(I18nUtils.getString("memshell.error.invalid.option"));
        return t;
    }

    private DetectorType selectedDetectorTypeOrNull() {
        if (detectorTypeGroup == null) return null;
        int idx = detectorTypeGroup.getToggles().indexOf(detectorTypeGroup.getSelectedToggle());
        DetectorType[] values = DetectorType.values();
        return (idx >= 0 && idx < values.length) ? values[idx] : null;
    }

    @FXML
    void onDetectorTypeChanged(ActionEvent e) {
        refreshDetectorInputs();
    }

    private PayloadFormat selectedPayloadFormat() {
        Object value = outputFormatBox.getValue();
        if (value instanceof PayloadFormat) {
            return (PayloadFormat) value;
        }
        return PayloadOutputService.parseFormat(selectedText(outputFormatBox));
    }

    private String requireDetectorClassName() {
        String className = normalizedText(detectorClassNameTextField);
        if (className.isEmpty()) {
            className = "org.example.Detector" + MemoryShellRandomUtil.randomAlpha(6, 10);
            detectorClassNameTextField.setText(className);
        }
        return ClassNameUtil.requireValidJavaClassName(className, "detector class name");
    }

    private int parseSleepSeconds() {
        String text = normalizedText(sleepSecondsTextField);
        if (text.isEmpty()) {
            return 5;
        }
        int seconds = Integer.parseInt(text);
        if (seconds <= 0) {
            throw new IllegalArgumentException(I18nUtils.getString("payload.error.invalid.sleep"));
        }
        return seconds;
    }

    private String selectedText(ComboBox comboBox) {
        Object value = comboBox.getValue();
        if (value == null) {
            throw new IllegalArgumentException(I18nUtils.getString("memshell.error.invalid.option"));
        }
        return value.toString();
    }

    private String normalizedText(TextField textField) {
        String value = textField.getText();
        return value == null ? "" : value.trim();
    }

    private String normalizedOutputPath(PayloadFormat format) {
        String outputPath = normalizedText(outputPathTextField);
        if (outputPath.isEmpty() && PayloadOutputService.isFileFormat(format)) {
            outputPath = ".";
            outputPathTextField.setText(outputPath);
        }
        return outputPath;
    }

    private void refreshOutputFormats() {
        PayloadFormat selected = outputFormatsInitialized ? selectedPayloadFormatOrNull() : null;
        outputFormatBox.setItems(FXCollections.observableArrayList(supportedOutputFormats()));
        if (selected == null || !outputFormatBox.getItems().contains(selected)) {
            selected = PayloadFormat.BASE64;
        }
        outputFormatBox.setValue(selected);
        outputFormatsInitialized = true;
        refreshOutputPathState();
    }

    private PayloadFormat selectedPayloadFormatOrNull() {
        Object value = outputFormatBox.getValue();
        if (value == null) {
            return null;
        }
        if (value instanceof PayloadFormat) {
            return (PayloadFormat) value;
        }
        return PayloadOutputService.parseFormat(value.toString());
    }

    private List<PayloadFormat> supportedOutputFormats() {
        if (isDetectorSelected()) {
            return Arrays.asList(PayloadFormat.values());
        }
        List<PayloadFormat> formats = new ArrayList<PayloadFormat>();
        PayloadFormat[] values = PayloadFormat.values();
        for (int i = 0; i < values.length; i++) {
            PayloadFormat format = values[i];
            if (isYsoSupportedFormat(format)) {
                formats.add(format);
            }
        }
        return formats;
    }

    private boolean isYsoSupportedFormat(PayloadFormat format) {
        return format == PayloadFormat.RAW
                || format == PayloadFormat.BASE64
                || format == PayloadFormat.BIGINTEGER
                || format == PayloadFormat.HEX;
    }

    private void refreshDetectorInputs() {
        DetectorType detectorType = selectedDetectorTypeOrNull();
        dnsDomainTextField.setDisable(detectorType != DetectorType.DNSLOG);
        httpBaseUrlTextField.setDisable(detectorType != DetectorType.HTTPLOG);
        boolean sleepDetector = detectorType == DetectorType.SLEEP;
        sleepSecondsTextField.setDisable(!sleepDetector);
        serverTypeBox.setDisable(!sleepDetector);
    }

    private void refreshYsoGadgets() {
        String previous = selectedTextOrNull(ysoGadgetBox);
        ysoGadgetBox.setItems(FXCollections.observableArrayList(ysoPayloadService.getSupportedGadgets(selectedYsoCategory(), isClassFileOnlySelected())));
        if (previous != null && ysoGadgetBox.getItems().contains(previous)) {
            ysoGadgetBox.setValue(previous);
        } else if (!ysoGadgetBox.getItems().isEmpty()) {
            ysoGadgetBox.setValue(ysoGadgetBox.getItems().get(0));
        } else {
            ysoGadgetBox.setValue(null);
        }
        refreshYsoInputs();
    }

    private void refreshYsoInputs() {
        YsoPayloadMetadata metadata = selectedYsoMetadata();
        String promptKey = metadata == null ? "payload.yso.command.placeholder.generic" : metadata.getInputType().getPromptKey();
        applyPromptTextIfWritable(ysoCommandTextField, I18nUtils.getString(promptKey));
        ysoInputTypeTextField.setText(metadata == null ? "" : I18nUtils.getString(metadata.getInputType().getI18nKey()));
        ysoCapabilityTextField.setText(metadata == null ? "" : capabilityText(metadata));
        ysoDependencyHintTextField.setText(metadata == null ? "" : YsoDisplayHintUtil.formatDependencyHint(metadata.getDependencyHint()));
        ysoVerificationHintTextField.setText(metadata == null ? "" : YsoDisplayHintUtil.formatVerificationHint(metadata.getVerificationHint()));
        refreshYsoHelperState();
    }

    private void refreshYsoHelperState() {
        YsoPayloadMetadata metadata = selectedYsoMetadata();
        boolean woodpeckerInput = metadata != null && metadata.getInputType() == YsoPayloadInputType.WOODPECKER_COMMAND;
        ysoHelperTypeBox.setDisable(!woodpeckerInput);
        ysoHelperValueTextField.setDisable(!woodpeckerInput);
        ysoHelperApplyButton.setDisable(!woodpeckerInput);
        YsoCommandHelperType helperType = selectedYsoHelperType();
        if (helperType == null && ysoHelperTypeBox.getItems() != null && !ysoHelperTypeBox.getItems().isEmpty())
            ysoHelperTypeBox.setValue(ysoHelperTypeBox.getItems().get(0));
        helperType = selectedYsoHelperType();
        String promptKey = helperType == null ? "payload.yso.helper.value.placeholder" : helperType.getPlaceholderKey();
        applyPromptTextIfWritable(ysoHelperValueTextField, I18nUtils.getString(promptKey));
        if (!woodpeckerInput)
            ysoHelperValueTextField.clear();
    }

    private void applyPromptTextIfWritable(TextField textField, String promptText) {
        if (textField != null && !textField.promptTextProperty().isBound()) {
            textField.setPromptText(promptText);
        }
    }

    private void refreshOutputPathState() {
        PayloadFormat format = selectedPayloadFormatOrNull();
        boolean fileFormat = format != null && PayloadOutputService.isFileFormat(format);
        outputPathTextField.setDisable(!fileFormat);
    }

    private String preferredCodeExecGadget() {
        String preferred = ysoPayloadService.getPreferredClassFileGadget();
        if (preferred == null) {
            return selectedTextOrNull(ysoGadgetBox);
        }
        if (!ysoGadgetBox.getItems().contains(preferred)) {
            ysoCategoryBox.setValue(categoryItem(YsoPayloadCategory.ALL));
            refreshYsoGadgets();
        }
        return preferred;
    }

    private String selectedTextOrNull(ComboBox comboBox) {
        Object value = comboBox.getValue();
        return value == null ? null : value.toString();
    }

    private YsoPayloadCategory selectedYsoCategory() {
        Object value = ysoCategoryBox.getValue();
        if (value instanceof YsoPayloadCategory) {
            return (YsoPayloadCategory) value;
        }
        if (value instanceof YsoCategoryItem) {
            return ((YsoCategoryItem) value).getCategory();
        }
        return YsoPayloadCategory.ALL;
    }

    private YsoPayloadMetadata selectedYsoMetadata() {
        return ysoPayloadService.getMetadata(selectedTextOrNull(ysoGadgetBox));
    }

    private String capabilityText(YsoPayloadMetadata metadata) {
        StringBuilder builder = new StringBuilder();
        builder.append(categoryText(metadata));
        if (metadata.supportsClassFile()) {
            builder.append(" / class_file");
        }
        return builder.toString();
    }

    private String categoryText(YsoPayloadMetadata metadata) {
        List<String> labels = new ArrayList<String>();
        for (YsoPayloadCategory category : metadata.getCategories()) {
            labels.add(I18nUtils.getString(category.getI18nKey()));
        }
        if (labels.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < labels.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(labels.get(i));
        }
        return builder.toString();
    }

    private boolean isClassFileOnlySelected() {
        return ysoClassFileOnlyCheckBox != null && ysoClassFileOnlyCheckBox.isSelected();
    }

    private YsoCommandHelperType selectedYsoHelperType() {
        Object value = ysoHelperTypeBox.getValue();
        if (value instanceof YsoCommandHelperType) {
            return (YsoCommandHelperType) value;
        }
        if (value == null) {
            return null;
        }
        YsoCommandHelperType[] values = YsoCommandHelperType.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].toString().equals(value.toString())) {
                return values[i];
            }
        }
        return null;
    }

    private String buildStructuredYsoCommand(YsoCommandHelperType helperType, String value) {
        String helperValue = value == null ? "" : value.trim();
        if (helperType == YsoCommandHelperType.SLEEP) {
            if (helperValue.isEmpty()) {
                helperValue = "5";
            }
            int seconds = Integer.parseInt(helperValue);
            if (seconds <= 0) {
                throw new IllegalArgumentException(I18nUtils.getString("payload.error.invalid.sleep"));
            }
            helperValue = String.valueOf(seconds);
            ysoHelperValueTextField.setText(helperValue);
            return "sleep:" + helperValue;
        }
        if (helperType == YsoCommandHelperType.DNSLOG) {
            if (helperValue.isEmpty()) {
                helperValue = DnsLogService.generateDnsLogDomain();
            }
            ysoHelperValueTextField.setText(helperValue);
            return "dnslog:" + helperValue;
        }
        if (helperType == YsoCommandHelperType.HTTPLOG) {
            if (helperValue.isEmpty()) {
                helperValue = HttpLogService.generateHttpLogUrl();
            }
            ysoHelperValueTextField.setText(helperValue);
            return "httplog:" + helperValue;
        }
        if (helperValue.isEmpty()) {
            throw new IllegalArgumentException(I18nUtils.getString("payload.error.yso.helper.value.required"));
        }
        ysoHelperValueTextField.setText(helperValue);
        if (helperType == YsoCommandHelperType.AUTO_CMD) {
            return "auto_cmd:" + helperValue;
        }
        if (helperType == YsoCommandHelperType.RAW_CMD) {
            return "raw_cmd:" + helperValue;
        }
        if (helperType == YsoCommandHelperType.WIN_CMD) {
            return "win_cmd:" + helperValue;
        }
        if (helperType == YsoCommandHelperType.LINUX_CMD) {
            return "linux_cmd:" + helperValue;
        }
        throw new IllegalArgumentException(I18nUtils.getString("payload.error.yso.helper.unsupported"));
    }

    private List<YsoCategoryItem> categoryItems() {
        List<YsoCategoryItem> items = new ArrayList<YsoCategoryItem>();
        YsoPayloadCategory[] values = YsoPayloadCategory.values();
        for (int i = 0; i < values.length; i++) {
            items.add(categoryItem(values[i]));
        }
        return items;
    }

    private YsoCategoryItem categoryItem(YsoPayloadCategory category) {
        return new YsoCategoryItem(category);
    }

    private static class YsoCategoryItem {
        private final YsoPayloadCategory category;

        private YsoCategoryItem(YsoPayloadCategory category) {
            this.category = category;
        }

        private YsoPayloadCategory getCategory() {
            return category;
        }

        @Override
        public String toString() {
            return I18nUtils.getString(category.getI18nKey());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof YsoCategoryItem)) {
                return false;
            }
            YsoCategoryItem other = (YsoCategoryItem) obj;
            return category == other.category;
        }

        @Override
        public int hashCode() {
            return category == null ? 0 : category.hashCode();
        }
    }

    private enum YsoCommandHelperType {
        SLEEP("sleep:", "payload.yso.helper.type.sleep", "payload.yso.helper.value.sleep"),
        DNSLOG("dnslog:", "payload.yso.helper.type.dnslog", "payload.yso.helper.value.dnslog"),
        HTTPLOG("httplog:", "payload.yso.helper.type.httplog", "payload.yso.helper.value.httplog"),
        AUTO_CMD("auto_cmd:", "payload.yso.helper.type.auto_cmd", "payload.yso.helper.value.auto_cmd"),
        RAW_CMD("raw_cmd:", "payload.yso.helper.type.raw_cmd", "payload.yso.helper.value.raw_cmd"),
        WIN_CMD("win_cmd:", "payload.yso.helper.type.win_cmd", "payload.yso.helper.value.win_cmd"),
        LINUX_CMD("linux_cmd:", "payload.yso.helper.type.linux_cmd", "payload.yso.helper.value.linux_cmd");

        private final String prefix;
        private final String i18nKey;
        private final String placeholderKey;

        YsoCommandHelperType(String prefix, String i18nKey, String placeholderKey) {
            this.prefix = prefix;
            this.i18nKey = i18nKey;
            this.placeholderKey = placeholderKey;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getPlaceholderKey() {
            return placeholderKey;
        }

        @Override
        public String toString() {
            return I18nUtils.getString(i18nKey);
        }
    }
}
