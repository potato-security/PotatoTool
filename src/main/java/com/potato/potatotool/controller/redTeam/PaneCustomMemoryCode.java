package com.potato.potatotool.controller.redTeam;

import com.potato.potatotool.ToStart;
import com.potato.potatotool.content.redTeam.memshell.config.MemoryObj;
import com.potato.potatotool.content.redTeam.memshell.util.ClassNameUtil;
import com.potato.potatotool.content.redTeam.memshell.util.CustomClassAnalyzer;
import com.potato.potatotool.content.redTeam.memshell.util.CustomClassMetadata;
import com.potato.potatotool.content.redTeam.memshell.util.MemoryShellRandomUtil;
import com.potato.potatotool.content.redTeam.memshell.util.MemoryShellOptionUtil;
import com.potato.potatotool.content.redTeam.memshell.util.RandomHeaderUtil;
import com.potato.potatotool.content.redTeam.memshell.util.UrlPatternUtil;
import com.dlsc.gemsfx.CFCheckBox;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.ui.SmoothScrollPane;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants.*;

/**
 * @author Potato
 * @date 2023/3/21 17:10
 */
public class PaneCustomMemoryCode {
    @FXML
    private StackPane sPane;
    @FXML
    private SmoothScrollPane rootScrollPane;
    @FXML
    private ComboBox toolTypeBox;
    @FXML
    private ComboBox serverTypeBox;
    @FXML
    private ComboBox shellTypeBox;
    @FXML
    private ComboBox outputFormatBox;

    @FXML
    private TextField passTextField;
    @FXML
    private TextField keyTextField;
    @FXML
    private TextField headerNameTextField;
    @FXML
    private TextField headerValueTextField;
    @FXML
    private TextField shellClassNameTextField;
    @FXML
    private TextField injectorClassNameTextField;
    @FXML
    private TextField urlPatternTextField;
    @FXML
    private TextField outputPathTextField;
    @FXML
    private CFCheckBox bypassJdkModuleCheckBox;


    @FXML
    private ComboBox gadgetTypeBox;
    @FXML
    private ComboBox exprEncoderBox;

    @FXML
    private TextArea outTextArea;

    // Custom choose classFilePath
    String classFilePath = null;
    private boolean generationRunning = false;

    public void initialize() {
        initRender();
        Platform.runLater(() -> {
            I18nUtils.bindComponents(sPane);
            applyStartupPreviewState();
        });
    }

    private void initRender() {
        // 将 ComboBox 和对应的选项数组放入 Map 中
        Map<ComboBox<String>, String[]> comboBoxOptions = new HashMap<>();
        comboBoxOptions.put(toolTypeBox, TOOLS);
        comboBoxOptions.put(serverTypeBox, SERVERS_TOOL_ANTSWORD);
        comboBoxOptions.put(shellTypeBox, SHELLTYPES);
        comboBoxOptions.put(outputFormatBox, OUTPUTFORMATS_SERVER_TOMCAT);
        comboBoxOptions.put(gadgetTypeBox, GADGETS);
        comboBoxOptions.put(exprEncoderBox, EXPRENCODERS);

        // 使用循环为每个 ComboBox 设置选项和默认值
        comboBoxOptions.forEach((comboBox, options) -> {
            comboBox.setItems(FXCollections.observableArrayList(options));
            comboBox.setValue(options[0]); // 设置默认值为数组的第一个元素
        });

        toolTypeBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue==null) return;
            String toolType = newValue.toString();
            serverTypeBox.setItems(FXCollections.observableArrayList(MemoryShellOptionUtil.getServersForTool(toolType)));
            if(toolType.equals(TOOL_CUSTOM)){
                toChooseClassFile();
            }
            serverTypeBox.setValue(serverTypeBox.getItems().get(0));
        });

        serverTypeBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue==null) return;
            String serverType = newValue.toString();

            shellTypeBox.setItems(FXCollections.observableArrayList(MemoryShellOptionUtil.getShellTypesForServer(serverType, selectedValue(toolTypeBox))));
            outputFormatBox.setItems(FXCollections.observableArrayList(MemoryShellOptionUtil.getOutputFormatsForServer(serverType)));

            shellTypeBox.setValue(shellTypeBox.getItems().get(0));
            outputFormatBox.setValue(outputFormatBox.getItems().get(0));
        });
    }

    private void toChooseClassFile(){
        FileChooser chooser = new FileChooser();
        FileChooser.ExtensionFilter filter =
                new FileChooser.ExtensionFilter(I18nUtils.getString("memshell.classfile.filter"), "*.class");
        chooser.getExtensionFilters().add(filter);

        Stage stage = (Stage) shellTypeBox.getScene().getWindow();
        classFilePath = null;
        File selectedFile = chooser.showOpenDialog(stage);
        if (selectedFile != null) {
            classFilePath = selectedFile.getAbsolutePath();
            applyDetectedCustomClass(classFilePath);
        }

        if (classFilePath == null) {
            outTextArea.setText(I18nUtils.getString("memshell.error.select"));
        }
    }

    @FXML
    public void toGenerateMemoryShell(ActionEvent event) {
        if (generationRunning) {
            outTextArea.setText(I18nUtils.getString("memshell.error.running"));
            return;
        }
        outTextArea.setText("");
        if (TOOL_CUSTOM.equals(toolTypeBox.getValue()) && classFilePath == null) {
            outTextArea.setText(I18nUtils.getString("memshell.error.select"));
            return;
        }
        if (TOOL_CUSTOM.equals(toolTypeBox.getValue())) {
            File selectedClassFile = new File(classFilePath);
            if (!selectedClassFile.isFile()) {
                classFilePath = null;
                outTextArea.setText(I18nUtils.getString("memshell.error.select"));
                return;
            }
        }
        MemoryObj memoryObj;
        try {
            memoryObj = initMemoryObj();
        } catch (Exception e) {
            showGenerateError(e);
            return;
        }
        generationRunning = true;
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                memoryObj.buildMemoryShellAndInjector();

                Map<String, String> showResultMap = memoryObj.getShowResultMap();

                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> entry : showResultMap.entrySet()) {
                    sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                Platform.runLater(() -> outTextArea.setText(sb.toString()));
                return null;
            }
        };
        task.setOnFailed(e -> {
            generationRunning = false;
            showGenerateError(task.getException());
        });
        task.setOnSucceeded(e -> generationRunning = false);
        task.setOnCancelled(e -> generationRunning = false);
        Thread customMemoryCodeThread = new Thread(task);
        customMemoryCodeThread.setDaemon(true);
        customMemoryCodeThread.start();

    }

    private void showGenerateError(Throwable error) {
        String detail = error == null || error.getMessage() == null ? "" : ": " + error.getMessage();
        String errorMessage = I18nUtils.getString("memshell.error.generate") + detail;
        if (Platform.isFxApplicationThread()) {
            outTextArea.setText(errorMessage);
        } else {
            Platform.runLater(() -> outTextArea.setText(errorMessage));
        }
    }

    private MemoryObj initMemoryObj() throws Exception {
        MemoryObj memoryObj = new MemoryObj();

        memoryObj.setToolType(selectedValue(toolTypeBox));
        memoryObj.setServerType(selectedValue(serverTypeBox));
        memoryObj.setShellType(selectedValue(shellTypeBox));
        memoryObj.setOutputFormat(selectedValue(outputFormatBox));
        memoryObj.setSavePath(normalizedText(outputPathTextField));
        memoryObj.setEnableBypassJDKModule(bypassJdkModuleCheckBox != null && bypassJdkModuleCheckBox.isSelected());

        if(memoryObj.getToolType().equals(TOOL_CUSTOM)){
            memoryObj.setClassFilePath(classFilePath);
            CustomClassMetadata customClassMetadata = CustomClassAnalyzer.analyze(classFilePath);
            memoryObj.setShellType(customClassMetadata.getShellType());
            memoryObj.setShellClassName(customClassMetadata.getClassName());
            shellTypeBox.setValue(customClassMetadata.getShellType());
            shellClassNameTextField.setText(customClassMetadata.getClassName());
        }

        String gadgetType = selectedValue(gadgetTypeBox);
        if(isNoneOption(gadgetType)){
            memoryObj.setGadgetType(null);
        }else {
            memoryObj.setGadgetType(gadgetType);
        }

        String exprEncoder = selectedValue(exprEncoderBox);
        if(isNoneOption(exprEncoder)){
            memoryObj.setExprEncoder(null);
        }else {
            memoryObj.setExprEncoder(exprEncoder);
        }
        MemoryShellOptionUtil.normalizeAndRequireSupportedOptions(memoryObj);

        if(isBlankText(passTextField)){
            passTextField.setText(MemoryShellRandomUtil.randomAlpha(6, 10));
        }
        memoryObj.setPass(passTextField.getText());

        if(isBlankText(keyTextField)){
            keyTextField.setText(MemoryShellRandomUtil.randomAlpha(6, 10));
        }
        if(memoryObj.getToolType().equals(TOOL_NEOREGEORG)){
            keyTextField.setText("key");
        }
        memoryObj.setKey(keyTextField.getText());

        Map.Entry<String, String> header = RandomHeaderUtil.generateRandomHeader();
        if(isBlankText(headerNameTextField)){
            headerNameTextField.setText(header.getKey());
        }
        String headerName = requireValidHeaderName(headerNameTextField.getText());
        headerNameTextField.setText(headerName);
        memoryObj.setHeaderName(headerName);

        if(isBlankText(headerValueTextField)){
            headerValueTextField.setText(header.getValue());
        }
        String headerValue = requireValidHeaderValue(headerValueTextField.getText());
        headerValueTextField.setText(headerValue);
        memoryObj.setHeaderValue(headerValue);

        if (!TOOL_CUSTOM.equals(memoryObj.getToolType())) {
            if(isBlankText(shellClassNameTextField)){
                shellClassNameTextField.setText(ClassNameUtil.getRandomShellClassName(memoryObj.getShellType()));
            }
            String shellClassName = requireValidClassName(shellClassNameTextField.getText(), "memshell.error.invalid.shellclass");
            shellClassNameTextField.setText(shellClassName);
            memoryObj.setShellClassName(shellClassName);
        }

        if(isBlankText(injectorClassNameTextField)){
            injectorClassNameTextField.setText(ClassNameUtil.getRandomInjectorClassName());
        }
        String injectorClassName = requireValidClassName(injectorClassNameTextField.getText(), "memshell.error.invalid.injectorclass");
        injectorClassNameTextField.setText(injectorClassName);
        memoryObj.setInjectorClassName(injectorClassName);

        String currentUrlPattern = normalizedText(urlPatternTextField);
        if(currentUrlPattern.isEmpty() || currentUrlPattern.equals("/*") || currentUrlPattern.equals("/")){
            if (memoryObj.getShellType().equals(SHELLTYPE_WFHANDLERMETHOD)) {
                urlPatternTextField.setText("/" + MemoryShellRandomUtil.randomLowerAlpha(6));
            } else {
                urlPatternTextField.setText("/*");
            }
        }
        String urlPattern = requireValidUrlPattern(urlPatternTextField.getText());
        urlPatternTextField.setText(urlPattern);
        memoryObj.setUrlPattern(urlPattern);

        MemoryShellOptionUtil.normalizeAndPrepareForGeneration(memoryObj);
        applyPreparedValuesToInputs(memoryObj);

        return memoryObj;
    }

    private String selectedValue(ComboBox comboBox) {
        Object value = comboBox.getValue();
        if (value == null) {
            throw new IllegalArgumentException(I18nUtils.getString("memshell.error.invalid.option"));
        }
        return value.toString();
    }

    private String requireValidClassName(String className, String errorKey) {
        String normalized = ClassNameUtil.normalizeClassName(className);
        if (!ClassNameUtil.isValidJavaClassName(normalized)) {
            throw new IllegalArgumentException(I18nUtils.getString(errorKey) + ": " + className);
        }
        return normalized;
    }

    private String requireValidHeaderName(String headerName) {
        try {
            return RandomHeaderUtil.requireValidHeaderName(headerName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(I18nUtils.getString("memshell.error.invalid.headername") + ": " + headerName);
        }
    }

    private String requireValidHeaderValue(String headerValue) {
        try {
            return RandomHeaderUtil.requireValidHeaderValue(headerValue);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(I18nUtils.getString("memshell.error.invalid.headervalue") + ": " + headerValue);
        }
    }

    private String requireValidUrlPattern(String urlPattern) {
        try {
            return UrlPatternUtil.requireValidUrlPattern(urlPattern);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(I18nUtils.getString("memshell.error.invalid.urlpattern") + ": " + urlPattern);
        }
    }

    private boolean isBlankText(TextField textField) {
        return normalizedText(textField).isEmpty();
    }

    private String normalizedText(TextField textField) {
        String value = textField.getText();
        return value == null ? "" : value.trim();
    }

    private void applyPreparedValuesToInputs(MemoryObj memoryObj) {
        passTextField.setText(memoryObj.getPass());
        keyTextField.setText(memoryObj.getKey());
        headerNameTextField.setText(memoryObj.getHeaderName());
        headerValueTextField.setText(memoryObj.getHeaderValue());
        shellClassNameTextField.setText(memoryObj.getShellClassName() == null ? "" : memoryObj.getShellClassName());
        injectorClassNameTextField.setText(memoryObj.getInjectorClassName());
        urlPatternTextField.setText(memoryObj.getUrlPattern());
        if (memoryObj.getSavePath() != null) {
            outputPathTextField.setText(memoryObj.getSavePath());
        }
    }

    private void applyDetectedCustomClass(String path) {
        try {
            CustomClassMetadata customClassMetadata = CustomClassAnalyzer.analyze(path);
            if (shellTypeBox.getItems().contains(customClassMetadata.getShellType())) {
                shellTypeBox.setValue(customClassMetadata.getShellType());
            }
            shellClassNameTextField.setText(customClassMetadata.getClassName());
        } catch (Exception e) {
            outTextArea.setText(I18nUtils.getString("memshell.error.generate") + ": " + e.getMessage());
        }
    }

    private void applyStartupPreviewState() {
        PaneCustomMemoryCodePreviewSupport.PreviewState previewState =
                PaneCustomMemoryCodePreviewSupport.buildPreviewState(ToStart.getStartupTestPage());
        if (previewState == null) {
            return;
        }

        applyComboValue(toolTypeBox, previewState.getToolType());
        applyComboValue(serverTypeBox, previewState.getServerType());
        applyComboValue(shellTypeBox, previewState.getShellType());
        applyComboValue(outputFormatBox, previewState.getOutputFormat());
        applyText(passTextField, previewState.getPass());
        applyText(keyTextField, previewState.getKey());
        applyText(headerNameTextField, previewState.getHeaderName());
        applyText(headerValueTextField, previewState.getHeaderValue());
        applyText(shellClassNameTextField, previewState.getShellClassName());
        applyText(injectorClassNameTextField, previewState.getInjectorClassName());
        applyText(urlPatternTextField, previewState.getUrlPattern());
        applyText(outputPathTextField, previewState.getOutputPath());
        bypassJdkModuleCheckBox.setSelected(previewState.isEnableBypassJdkModule());
        applyComboValue(gadgetTypeBox, previewState.getGadgetType());
        applyComboValue(exprEncoderBox, previewState.getExprEncoder());
        if (previewState.getOutputText() != null) {
            outTextArea.setText(previewState.getOutputText());
        }

        if (rootScrollPane != null) {
            Platform.runLater(() -> rootScrollPane.setVvalue(previewState.getScrollValue()));
        }
    }

    private void applyComboValue(ComboBox comboBox, String value) {
        if (comboBox == null || value == null) {
            return;
        }
        if (comboBox.getItems().contains(value)) {
            comboBox.setValue(value);
        }
    }

    private void applyText(TextField textField, String value) {
        if (textField == null || value == null) {
            return;
        }
        textField.setText(value);
    }

    @FXML
    public void clearInput(ActionEvent event) {
        passTextField.setText("");
        keyTextField.setText("");
        headerNameTextField.setText("");
        headerValueTextField.setText("");
        shellClassNameTextField.setText("");
        injectorClassNameTextField.setText("");
        urlPatternTextField.setText("");
        outputPathTextField.setText("");
        bypassJdkModuleCheckBox.setSelected(false);
        classFilePath = null;
    }
}
