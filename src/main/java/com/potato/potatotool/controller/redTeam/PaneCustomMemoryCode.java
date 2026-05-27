package com.potato.potatotool.controller.redTeam;

import com.potato.potatotool.content.redTeam.memshell.GenerateMemoryShell;
import com.potato.potatotool.content.redTeam.memshell.config.MemoryObj;
import com.potato.potatotool.content.redTeam.memshell.util.ClassNameUtil;
import com.potato.potatotool.content.redTeam.memshell.util.RandomHeaderUtil;
import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.data.StrUtils;
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
    private ComboBox gadgetTypeBox;
    @FXML
    private ComboBox exprEncoderBox;

    @FXML
    private TextArea outTextArea;

    // Custom choose classFilePath
    String classFilePath = null;

    public void initialize() {
        initRender();
        Platform.runLater(() -> {I18nUtils.bindComponents(sPane);});
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
            if(toolType.equals(TOOL_ANTSWORD)){
                serverTypeBox.setItems(FXCollections.observableArrayList(SERVERS_TOOL_ANTSWORD));
            }else if(toolType.equals(TOOL_BEHINDER)){
                serverTypeBox.setItems(FXCollections.observableArrayList(SERVERS_TOOL_BEHINDER));
            }else if(toolType.equals(TOOL_GODZILLA)){
                serverTypeBox.setItems(FXCollections.observableArrayList(SERVERS_TOOL_GODZILLA));
            }else if(toolType.equals(TOOL_CUSTOM)){
                serverTypeBox.setItems(FXCollections.observableArrayList(SERVERS_TOOL_CUSTOM));
                toChooseClassFile();
            }else if(toolType.equals(TOOL_NEOREGEORG)){
                serverTypeBox.setItems(FXCollections.observableArrayList(SERVERS_TOOL_NEOREGEORG));
            }else if(toolType.equals(TOOL_SUO5)){
                serverTypeBox.setItems(FXCollections.observableArrayList(SERVERS_TOOL_SUO5));
            }
            serverTypeBox.setValue(serverTypeBox.getItems().get(0));
        });

        serverTypeBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue==null) return;
            String serverType = newValue.toString();

            if(serverType.equals(SERVER_SPRING_MVC)){
                shellTypeBox.setItems(FXCollections.observableArrayList(SHELLTYPES_SERVER_SPRING_MVC));
            }else if(serverType.equals(SERVER_SPRING_WEBFLUX)){
                shellTypeBox.setItems(FXCollections.observableArrayList(SHELLTYPES_SERVER_SPRING_WEBFLUX));
            }else{
                shellTypeBox.setItems(FXCollections.observableArrayList(SHELLTYPES));
            }

            if(serverType.equals(SERVER_TOMCAT)){
                outputFormatBox.setItems(FXCollections.observableArrayList(OUTPUTFORMATS_SERVER_TOMCAT));
            }else if(serverType.equals(SERVER_SPRING_MVC)){
                outputFormatBox.setItems(FXCollections.observableArrayList(OUTPUTFORMATS_SERVER_SPRING_MVC));
            }else{
                outputFormatBox.setItems(FXCollections.observableArrayList(OUTPUTFORMATS));
            }

            shellTypeBox.setValue(shellTypeBox.getItems().get(0));
            outputFormatBox.setValue(outputFormatBox.getItems().get(0));
        });
    }

    private void toChooseClassFile(){
        FileChooser chooser = new FileChooser();
        FileChooser.ExtensionFilter filter =
                new FileChooser.ExtensionFilter("Class文件", "*.class");
        chooser.getExtensionFilters().add(filter);

        Stage stage = (Stage) shellTypeBox.getScene().getWindow();
        try {
            classFilePath = chooser.showOpenDialog(stage).getAbsolutePath();
        }catch (Exception exception){}

        if (classFilePath == null) {
            outTextArea.setText(I18nUtils.getString("memshell.error.select"));
        }
    }

    @FXML
    public void toGenerateMemoryShell(ActionEvent event) {
        outTextArea.setText("");
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    MemoryObj memoryObj = initMemoryObj();

                    memoryObj.buildMemoryShellAndInjector();

                    Map<String, String> showResultMap = memoryObj.getShowResultMap();

                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<String, String> entry : showResultMap.entrySet()) {
                        sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                    }
                    Platform.runLater(() -> {
                        outTextArea.setText(sb.toString());
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        task.setOnFailed(e -> {
            Throwable error = task.getException();
            error.printStackTrace();
        });
        Thread customMemoryCodeThread = new Thread(task);
        customMemoryCodeThread.setDaemon(true);
        customMemoryCodeThread.start();

    }

    private MemoryObj initMemoryObj() {
        MemoryObj memoryObj = new MemoryObj();

        memoryObj.setToolType(toolTypeBox.getValue().toString());
        memoryObj.setServerType(serverTypeBox.getValue().toString());
        memoryObj.setShellType(shellTypeBox.getValue().toString());
        memoryObj.setOutputFormat(outputFormatBox.getValue().toString());

        if(memoryObj.getToolType().equals(TOOL_CUSTOM)){
            memoryObj.setClassFilePath(classFilePath);
        }

        if(gadgetTypeBox.getValue().toString().equals("无") || gadgetTypeBox.getValue().toString().equals("")){
            memoryObj.setGadgetType(null);
        }else {
            memoryObj.setGadgetType(gadgetTypeBox.getValue().toString());
        }

        if(exprEncoderBox.getValue().toString().equals("无") || exprEncoderBox.getValue().toString().equals("")){
            memoryObj.setExprEncoder(null);
        }else {
            memoryObj.setExprEncoder(exprEncoderBox.getValue().toString());
        }

        if(passTextField.getText().isEmpty()){
            passTextField.setText(StrUtils.generateRandomString(6, 10));
        }
        memoryObj.setPass(passTextField.getText());

        if(keyTextField.getText().isEmpty()){
            keyTextField.setText(StrUtils.generateRandomString(6, 10));
        }
        if(memoryObj.getToolType().equals(TOOL_NEOREGEORG)){
            keyTextField.setText("key");
        }
        memoryObj.setKey(keyTextField.getText());

        Map.Entry<String, String> header = RandomHeaderUtil.generateRandomHeader();
        if(headerNameTextField.getText().isEmpty()){
            headerNameTextField.setText(header.getKey());
        }
        memoryObj.setHeaderName(headerNameTextField.getText());

        if(headerValueTextField.getText().isEmpty()){
            headerValueTextField.setText(header.getValue());
        }
        memoryObj.setHeaderValue(headerValueTextField.getText());

        if(shellClassNameTextField.getText().isEmpty()){
            shellClassNameTextField.setText(ClassNameUtil.getRandomShellClassName(memoryObj.getShellType()));
        }
        memoryObj.setShellClassName(shellClassNameTextField.getText());

        if(injectorClassNameTextField.getText().isEmpty()){
            injectorClassNameTextField.setText(ClassNameUtil.getRandomInjectorClassName());
        }
        memoryObj.setInjectorClassName(injectorClassNameTextField.getText());

        if(!urlPatternTextField.getText().isEmpty() || urlPatternTextField.getText().equals("/*") || urlPatternTextField.getText().equals("/")){
            if (memoryObj.getShellType().equals(SHELLTYPE_WFHANDLERMETHOD)) {
                urlPatternTextField.setText("/" + StrUtils.generateRandomString(6, 6).toLowerCase());
            } else {
                urlPatternTextField.setText("/*");
            }
        }
        memoryObj.setUrlPattern(urlPatternTextField.getText());

        if (outputFormatBox.getValue().toString().contains(OUTPUTFORMAT_BCEL)) {
            memoryObj.setLoaderClassName(ClassNameUtil.getRandomLoaderClassName());
        }

        memoryObj.setInjectorSimpleClassName(GenerateMemoryShell.getSimpleName(memoryObj.getInjectorClassName()));

        return memoryObj;
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
    }
}
