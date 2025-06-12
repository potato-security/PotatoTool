package com.potato.potatotool.utils.ui.highlighters.examples;

import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.ui.highlighters.CodeTypeDetector;
import com.potato.potatotool.utils.ui.highlighters.HighlighterFactory;
import com.potato.potatotool.utils.ui.highlighters.MultiLanguageHighlighter;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 智能代码类型检测示例
 * 展示如何使用CodeTypeDetector和自动检测功能
 * @author Potato
 */
public class AutoDetectionExample extends Application {

    private CodeArea codeArea;
    private Label languageLabel;
    private MultiLanguageHighlighter highlighter;
    private CodeTypeDetector detector;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            detector = HighlighterFactory.getTypeDetector();
            
            // 创建UI组件
            codeArea = new CodeArea();
            codeArea.setWrapText(true);
            
            // 初始化高亮器（默认Java）
//            highlighter = HighlighterFactory.createHighlighter(codeArea, "py");
            highlighter = HighlighterFactory.createMarkdownWithoutMarkersHighlighter(codeArea);
            
            // 创建控制面板
            HBox controlPanel = createControlPanel();
            
            // 创建状态栏
            HBox statusBar = createStatusBar();
            
            // 布局
            BorderPane root = new BorderPane();
            root.setTop(controlPanel);
            root.setCenter(codeArea);
            root.setBottom(statusBar);
            
            // 添加滚动支持
            VBox.setVgrow(codeArea, Priority.ALWAYS);
            
            // 场景和舞台
            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(Constants.getResourceUrl("/styles/code-highlighting.css"));
            primaryStage.setTitle("智能代码类型检测示例");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("初始化失败", "应用程序初始化失败: " + e.getMessage());
        }
    }
    
    private HBox createControlPanel() {
        Button openButton = new Button("打开文件");
        Button detectButton = new Button("检测代码类型");
        Button saveButton = new Button("保存文件");
        ComboBox<String> languageComboBox = new ComboBox<>();
        
        // 获取所有支持的语言
        languageComboBox.getItems().addAll(
                "java", "python", "javascript", "xml", "json", 
                "yaml", "markdown", "groovy", "css", "properties"
        );
        languageComboBox.setValue("java");
        
        // 打开文件按钮事件
        openButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("打开代码文件");
            
            // 添加文件过滤器
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("所有文件", "*.*"),
                    new FileChooser.ExtensionFilter("Java文件", "*.java"),
                    new FileChooser.ExtensionFilter("Python文件", "*.py"),
                    new FileChooser.ExtensionFilter("JavaScript文件", "*.js"),
                    new FileChooser.ExtensionFilter("XML文件", "*.xml"),
                    new FileChooser.ExtensionFilter("JSON文件", "*.json"),
                    new FileChooser.ExtensionFilter("YAML文件", "*.yml", "*.yaml"),
                    new FileChooser.ExtensionFilter("Markdown文件", "*.md")
            );
            
            File file = fileChooser.showOpenDialog(null);
            if (file != null) {
                try {
                    String content = new String(Files.readAllBytes(Paths.get(file.getPath())), StandardCharsets.UTF_8);
                    codeArea.replaceText(content);
                    
                    // 自动检测代码类型并应用高亮
                    String detectedType = detector.detect(file.getName(), content);
                    updateHighlighter(detectedType);
                    languageLabel.setText("检测到的语言: " + detectedType);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showErrorAlert("文件读取错误", "无法读取文件: " + ex.getMessage());
                }
            }
        });
        
        // 保存文件按钮事件
        saveButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("保存代码文件");
            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                try {
                    Files.write(Paths.get(file.getPath()), codeArea.getText().getBytes(StandardCharsets.UTF_8));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showErrorAlert("文件保存错误", "无法保存文件: " + ex.getMessage());
                }
            }
        });
        
        // 检测按钮事件
        detectButton.setOnAction(e -> {
            try {
                String content = codeArea.getText();
                if (content.trim().isEmpty()) {
                    showErrorAlert("检测错误", "代码内容为空，无法检测类型");
                    return;
                }
                String detectedType = detector.detectFromContent(content);
                updateHighlighter(detectedType);
                languageLabel.setText("检测到的语言: " + detectedType);
            } catch (Exception ex) {
                ex.printStackTrace();
                showErrorAlert("检测错误", "代码类型检测失败: " + ex.getMessage());
            }
        });
        
        // 语言选择下拉框事件
        languageComboBox.setOnAction(e -> {
            try {
                String selectedLanguage = languageComboBox.getValue();
                updateHighlighter(selectedLanguage);
                languageLabel.setText("当前语言: " + selectedLanguage);
            } catch (Exception ex) {
                ex.printStackTrace();
                showErrorAlert("切换语言错误", "切换语言失败: " + ex.getMessage());
            }
        });
        
        HBox controlPanel = new HBox(10);
        controlPanel.setPadding(new Insets(10));
        controlPanel.getChildren().addAll(
                new Label("选择语言:"), languageComboBox,
                openButton, saveButton, detectButton
        );
        
        return controlPanel;
    }
    
    private HBox createStatusBar() {
        languageLabel = new Label("当前语言: java");
        
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5));
        statusBar.getChildren().add(languageLabel);
        
        return statusBar;
    }
    
    private void updateHighlighter(String language) {
        try {
            // 停止当前高亮器
            if (highlighter != null) {
                highlighter.close();
            }
            
            // 创建新的高亮器
            highlighter = HighlighterFactory.createHighlighter(codeArea, language);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("高亮器错误", "更新高亮器失败: " + e.getMessage());
        }
    }
    
    /**
     * 显示错误提示对话框
     * @param title 标题
     * @param message 错误信息
     */
    private void showErrorAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @Override
    public void stop() {
        // 确保资源被正确释放
        if (highlighter != null) {
            try {
                highlighter.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 主方法
     */
    public static void main(String[] args) {
        launch(args);
    }
}