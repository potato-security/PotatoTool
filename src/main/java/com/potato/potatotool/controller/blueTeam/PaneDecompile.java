package com.potato.potatotool.controller.blueTeam;

import com.potato.potatotool.utils.core.I18nUtils;
import com.potato.potatotool.utils.ui.DefaultContextMenu;
import com.potato.potatotool.utils.ai.CodeAnalyzerUtils;
import com.potato.potatotool.utils.ui.CodeHighlightingAsync;
import com.potato.potatotool.utils.decompile.DecompileUtils;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import static com.potato.potatotool.ToStart.debugMode;

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

    @FXML
    private TextArea aiTextArea;
    @FXML
    private Pane aiPane;
    @FXML
    private Button showAI;


    String res = "";

    public void initialize() {
        new CodeHighlightingAsync().codeHighlighting(result);

        //  CodeArea添加高自适应
        sPane.heightProperty().addListener((obs, oldValue, newValue) -> {
            double prefHeight = newValue.doubleValue() - 150;
            virScrollPane.setMinHeight(prefHeight);

            aiTextAreHeightProperty = new SimpleDoubleProperty(prefHeight + 12);
            aiTextArea.prefHeightProperty().bind(aiTextAreHeightProperty);
        });

        //  CodeArea添加右键菜单
        result.setContextMenu(new DefaultContextMenu());

        //  设置默认第一个选项
        rulesComboBox.getSelectionModel().selectFirst();


        aiTextArea.prefWidthProperty().bind(aiTextAreWidthProperty);
        aiTextArea.layoutXProperty().bind(aiTextAreWidthProperty.negate());

        aiTextArea.setOnMouseMoved(event -> {
            if (isInLeftResizeZone(aiTextArea, event)) {
                aiTextArea.setCursor(Cursor.H_RESIZE);
            } else if (isInBottomResizeZone(aiTextArea, event)) {
                aiTextArea.setCursor(Cursor.V_RESIZE);
            } else {
                aiTextArea.setCursor(Cursor.DEFAULT);
            }
        });

        aiTextArea.setOnMousePressed(event -> {
            aiTextAreaStartX = event.getSceneX();
            aiTextAreaStartY = event.getSceneY();
        });
        aiTextArea.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - aiTextAreaStartX;
            double deltaY = event.getSceneY() - aiTextAreaStartY;

            double newX = aiTextAreWidthProperty.get() - deltaX;
            double newY = aiTextAreHeightProperty.get() + deltaY;

            if( newX > 0 && newX < sPane.getPrefWidth() * 0.9 ){
                aiTextAreWidthProperty.set(newX);
                aiTextAreHeightProperty.set(newY);
            }

            aiTextAreaStartX = event.getSceneX();
            aiTextAreaStartY = event.getSceneY();
        });
        
        // 绑定国际化
        Platform.runLater(() -> I18nUtils.bindComponents(sPane));
    }


    private boolean isInLeftResizeZone(TextArea textArea, MouseEvent event) {
        return event.getX() < RESIZE_MARGIN;
    }

    private boolean isInBottomResizeZone(TextArea textArea, MouseEvent event) {
        return event.getY() > (textArea.getHeight() - RESIZE_MARGIN);
    }

    @FXML
    void toDecompile(ActionEvent e){
        result.clear();

        FileChooser chooser = new FileChooser();
        FileChooser.ExtensionFilter filter =
                new FileChooser.ExtensionFilter("Class文件", "*.*");
        chooser.getExtensionFilters().add(filter);

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

        result.replaceText(I18nUtils.getString("decompile.processing"));

        String decompileMode = (String) rulesComboBox.getSelectionModel().getSelectedItem();


        String finalPath = path;
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                res = DecompileUtils.Decompile(finalPath, decompileMode);
                Platform.runLater(() -> {
                    result.replaceText(res);
                });
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


    @FXML
    public void toDecompileBat(ActionEvent e) {
        result.clear();

        DirectoryChooser chooser = new DirectoryChooser();

        Stage stage = (Stage) ((Node)e.getSource()).getScene().getWindow();
        String path = null;
        try {
            path = chooser.showDialog(stage).getAbsolutePath();
        }catch (Exception exception){
            if(debugMode) System.out.println("没有文件被选择");
            return;
        }

        if (path == null) {
            if(debugMode) System.out.println("没有文件被选择");
            return;
        }
        System.out.println(path);

        result.replaceText(I18nUtils.getString("decompile.processing"));

        String decompileMode = (String) rulesComboBox.getSelectionModel().getSelectedItem();


        String finalPath = path;
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                res = DecompileUtils.Decompile(finalPath, decompileMode);
                Platform.runLater(() -> {
                    result.replaceText(res);
                });
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


    private String oldData = "";
    private boolean isAiVisible = false;
    private boolean isAiCD = false;

    private double aiTextAreaStartX, aiTextAreaStartY;
    private DoubleProperty aiTextAreWidthProperty = new SimpleDoubleProperty(0);
    private DoubleProperty aiTextAreHeightProperty = new SimpleDoubleProperty(0);
    private static final double RESIZE_MARGIN = 10;
    @FXML
    void showAI(ActionEvent e) throws Exception {

        double targetWidth = isAiVisible ? 0 : sPane.getPrefWidth() * 0.8;
        Duration duration = Duration.seconds(0.2);

        if (!isAiVisible){
            aiPane.setVisible(true);
            showAI.setStyle("-fx-background-color: #87CEFA");
        }else {
            showAI.setStyle("-fx-background-color: transparent");
        }

        Timeline animation = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(aiTextAreWidthProperty, aiTextAreWidthProperty.get())),
                new KeyFrame(duration, new KeyValue(aiTextAreWidthProperty, targetWidth))
        );

        animation.setOnFinished(event -> {
            isAiVisible = !isAiVisible;

            if (!isAiVisible) {
                aiPane.setVisible(false);
            }
        });

        animation.play();

        String resultStr = result.getText();
        //  结果非空 且 结果更新 且 未到AI冷却CD 才触发AI接口
        if(res.isEmpty()){

            aiTextArea.clear();
            aiTextArea.appendText(I18nUtils.getString("decompile.ai.empty"));
            oldData = "";

        }else if (!resultStr.equals(oldData) && !isAiCD){

            oldData = resultStr;
            isAiCD = true;

            aiTextArea.clear();
            aiTextArea.appendText("AI分析中，请稍等……");

            // 另起线程调用AI接口
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    CodeAnalyzerUtils.optimizedCode(resultStr, aiTextArea);
                    return null;
                }
            };
            task.setOnFailed(event -> {
                Throwable error = task.getException();
                error.printStackTrace();
            });
            task.setOnSucceeded(event -> {
                isAiCD = false;
            });
            new Thread(task).start();

        }

    }

    @FXML
    void refreshAI(ActionEvent e) throws Exception { //结果未更新也可以强行重新调用AI
        String resultStr = result.getText();
        //  结果非空 且 未到AI冷却CD 才触发AI接口
        if(res.isEmpty()){

            aiTextArea.clear();
            aiTextArea.appendText(I18nUtils.getString("decompile.ai.analyze.empty"));
            oldData = "";

        }else if (!isAiCD){

            oldData = resultStr;
            isAiCD = true;

            aiTextArea.clear();
            aiTextArea.appendText("AI分析中，请稍等……");

            // 另起线程调用AI接口
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    CodeAnalyzerUtils.optimizedCode(resultStr, aiTextArea);
                    return null;
                }
            };
            task.setOnFailed(event -> {
                Throwable error = task.getException();
                error.printStackTrace();
            });
            task.setOnSucceeded(event -> {
                isAiCD = false;
            });
            new Thread(task).start();

        }
    }

}
