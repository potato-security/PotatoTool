package com.potato.potatotool.controller;

import com.potato.potatotool.content.blueTeam.webShellDecrypt;
import com.potato.potatotool.utils.DefaultContextMenu;
import com.potato.potatotool.utils.codeHighlightingAsync;
import com.potato.potatotool.utils.decompileUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

/**
 * @author Potato
 * @date 2023/10/24 16:51
 */
public class PaneDecompile {

    @FXML
    private StackPane sPane;

    @FXML
    private VirtualizedScrollPane virScrollPane;

    @FXML
    private CodeArea result;

    @FXML
    private ComboBox rulesComboBox;

    public void initialize() {

        //  CodeArea添加高自适应
        sPane.heightProperty().addListener((obs, oldValue, newValue) -> {
            double prefHeight = newValue.doubleValue() - 150;
            virScrollPane.setMinHeight(prefHeight);
        });

        //  CodeArea添加右键菜单
        result.setContextMenu(new DefaultContextMenu());

        //  设置默认第一个选项
        rulesComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    void toDecompile(ActionEvent e){
        result.clear();

        FileChooser chooser = new FileChooser();
        FileChooser.ExtensionFilter filter =
                new FileChooser.ExtensionFilter("Class文件", "*.class");
        chooser.getExtensionFilters().add(filter);

        Stage stage = (Stage) ((Node)e.getSource()).getScene().getWindow();
        String path = null;
        try {
            path = chooser.showOpenDialog(stage).getAbsolutePath();
        }catch (Exception exception){
            System.out.println("没有文件被选择");
            return;
        }

        if (path == null) {
            System.out.println("没有文件被选择");
            return;
        }

        result.replaceText("反编译中，请稍等……");

        String decompileMode = (String) rulesComboBox.getSelectionModel().getSelectedItem();


        String finalPath = path;
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                String res = decompileUtils.Decompile(finalPath, decompileMode);
                Platform.runLater(() -> {
                    result.replaceText(res);
                });
                new codeHighlightingAsync().codeHighlighting(result);
                return null;
            }
        };
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            error.printStackTrace();
        });

        // 启动任务
        new Thread(task).start();
    }

}
