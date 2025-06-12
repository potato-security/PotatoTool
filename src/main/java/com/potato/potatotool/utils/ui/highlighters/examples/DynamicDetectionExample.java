package com.potato.potatotool.utils.ui.highlighters.examples;

import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.ui.highlighters.HighlighterFactory;
import com.potato.potatotool.utils.ui.highlighters.MultiLanguageHighlighter;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
import java.util.Set;

/**
 * 动态代码类型检测示例
 * 展示如何使用动态检测功能自动切换高亮器
 * @author Potato
 */
public class DynamicDetectionExample extends Application {

    private CodeArea codeArea;
    private Label languageLabel;
    private MultiLanguageHighlighter highlighter;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // 创建UI组件
            codeArea = new CodeArea();
            codeArea.setWrapText(true);
            
            // 初始化动态高亮器
            highlighter = HighlighterFactory.createDynamicHighlighter(codeArea);
            
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
            primaryStage.setTitle("动态代码类型检测示例");
            primaryStage.setScene(scene);
            primaryStage.show();
            
            // 添加内容变化监听器，更新状态栏
            codeArea.textProperty().addListener((observable, oldValue, newValue) -> {
                updateLanguageLabel();
            });
            
            // 加载示例代码
            loadSampleCode();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("初始化失败", "应用程序初始化失败: " + e.getMessage());
        }
    }
    
    private HBox createControlPanel() {
        Button openButton = new Button("打开文件");
        Button saveButton = new Button("保存文件");
        Button javaButton = new Button("Java示例");
        Button pythonButton = new Button("Python示例");
        Button markdownButton = new Button("Markdown示例");
        Button xmlButton = new Button("XML示例");
        
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
        
        // 示例代码按钮事件
        javaButton.setOnAction(e -> loadJavaExample());
        pythonButton.setOnAction(e -> loadPythonExample());
        markdownButton.setOnAction(e -> loadMarkdownExample());
        xmlButton.setOnAction(e -> loadXmlExample());
        
        HBox controlPanel = new HBox(10);
        controlPanel.setPadding(new Insets(10));
        controlPanel.getChildren().addAll(
                openButton, saveButton, 
                new Label("示例:"), javaButton, pythonButton, markdownButton, xmlButton
        );
        
        return controlPanel;
    }
    
    private HBox createStatusBar() {
        languageLabel = new Label("当前语言: 检测中...");
        
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5));
        statusBar.getChildren().add(languageLabel);
        
        return statusBar;
    }
    
    private void updateLanguageLabel() {
        if (highlighter != null && highlighter.getCurrentStyler() != null) {
            Set<String> languages = highlighter.getCurrentStyler().getLanguageNames();
            String language = languages.isEmpty() ? "未知" : String.join(", ", languages);
            languageLabel.setText("当前语言: " + language);
        } else {
            languageLabel.setText("当前语言: 未检测");
        }
    }
    
    private void loadSampleCode() {
        // 默认加载Java示例
        loadJavaExample();
    }
    
    private void loadJavaExample() {
        String javaCode = "package com.potato.potatotool.utils.ui.highlighters.stylers;\n" +
                "import com.potato.potatotool.utils.ui.highlighters.HighlighterConstants;\n" +
                "import com.potato.potatotool.utils.ui.highlighters.MultiLanguageHighlighter;\n" +
                "import org.fxmisc.richtext.model.StyleSpans;\n" +
                "import org.fxmisc.richtext.model.StyleSpansBuilder;\n" +
                "\n" +
                "import java.util.*;\n" +
                "import java.util.regex.Matcher;\n" +
                "import java.util.regex.Pattern;\n" +
                "\n" +
                "/**\n" +
                " * Markdown语言代码高亮实现\n" +
                " * 基于正则表达式匹配Markdown语法元素\n" +
                " * 支持h1-h6标题、加粗、斜体等丰富样式\n" +
                " * 增强版支持代码块内部语言检测和语法高亮\n" +
                " * @author Potato\n" +
                " */\n" +
                "public class MarkdownStyler implements MultiLanguageHighlighter.LanguageStyler {\n" +
                "    \n" +
                "    // 代码块语言检测正则表达式 - 匹配 ```语言名\n" +
                "    private static final Pattern CODE_LANG_PATTERN = Pattern.compile(\"```(\\\\w+)\\\\s\");\n" +
                "\n" +
                "    // 标题模式 - 匹配 # 开头的标题，分组捕获标题级别\n" +
                "    private static final String H1_PATTERN = \"^#\\\\s+.*$\";\n" +
                "    private static final String H2_PATTERN = \"^##\\\\s+.*$\";\n" +
                "    private static final String H3_PATTERN = \"^###\\\\s+.*$\";\n" +
                "    private static final String H4_PATTERN = \"^####\\\\s+.*$\";\n" +
                "    private static final String H5_PATTERN = \"^#####\\\\s+.*$\";\n" +
                "    private static final String H6_PATTERN = \"^######\\\\s+.*$\";\n" +
                "    \n" +
                "    // 强调模式 - 匹配 *文本* 或 _文本_\n" +
                "    private static final String EMPHASIS_PATTERN = \"\\\\*[^\\\\*\\\\n]+\\\\*|_[^_\\\\n]+_\";\n" +
                "    // 加粗模式 - 匹配 **文本** 或 __文本__\n" +
                "    private static final String STRONG_PATTERN = \"\\\\*\\\\*[^\\\\*\\\\n]+\\\\*\\\\*|__[^_\\\\n]+__\";\n" +
                "    // 代码块模式 - 匹配 ```代码块```\n" +
                "    private static final String CODE_BLOCK_PATTERN = \"```[\\\\s\\\\S]*?```\";\n" +
                "    // 行内代码模式 - 匹配 `代码`\n" +
                "    private static final String INLINE_CODE_PATTERN = \"`[^`\\\\n]+`\";\n" +
                "    // 链接模式 - 匹配 [文本](链接)\n" +
                "    private static final String LINK_PATTERN = \"\\\\[[^\\\\]\\\\n]+\\\\]\\\\([^\\\\)\\\\n]+\\\\)\";\n" +
                "    // 图片模式 - 匹配 ![文本](链接)\n" +
                "    private static final String IMAGE_PATTERN = \"!\\\\[[^\\\\]\\\\n]+\\\\]\\\\([^\\\\)\\\\n]+\\\\)\";\n" +
                "    // 列表模式 - 匹配 - 或 * 或 + 开头的列表项\n" +
                "    private static final String LIST_PATTERN = \"^[\\\\s]*[-*+]\\\\s+.*$\";\n" +
                "    // 引用模式 - 匹配 > 开头的引用\n" +
                "    private static final String QUOTE_PATTERN = \"^[\\\\s]*>.*$\";\n" +
                "    // 水平线模式 - 匹配 --- 或 *** 或 ___ 的水平线\n" +
                "    private static final String HORIZONTAL_RULE_PATTERN = \"^[\\\\s]*([*\\\\-_])\\\\s*\\\\1\\\\s*\\\\1[\\\\\\1\\\\s]*$\";\n" +
                "\n" +
                "    private static final Pattern PATTERN = Pattern.compile(\n" +
                "            \"(?<H1>\" + H1_PATTERN + \")\"\n" +
                "            + \"|(?<H2>\" + H2_PATTERN + \")\"\n" +
                "            + \"|(?<H3>\" + H3_PATTERN + \")\"\n" +
                "            + \"|(?<H4>\" + H4_PATTERN + \")\"\n" +
                "            + \"|(?<H5>\" + H5_PATTERN + \")\"\n" +
                "            + \"|(?<H6>\" + H6_PATTERN + \")\"\n" +
                "            + \"|(?<EMPHASIS>\" + EMPHASIS_PATTERN + \")\"\n" +
                "            + \"|(?<STRONG>\" + STRONG_PATTERN + \")\"\n" +
                "            + \"|(?<CODEBLOCK>\" + CODE_BLOCK_PATTERN + \")\"\n" +
                "            + \"|(?<INLINECODE>\" + INLINE_CODE_PATTERN + \")\"\n" +
                "            + \"|(?<LINK>\" + LINK_PATTERN + \")\"\n" +
                "            + \"|(?<IMAGE>\" + IMAGE_PATTERN + \")\"\n" +
                "            + \"|(?<LIST>\" + LIST_PATTERN + \")\"\n" +
                "            + \"|(?<QUOTE>\" + QUOTE_PATTERN + \")\"\n" +
                "            + \"|(?<HORIZONTALRULE>\" + HORIZONTAL_RULE_PATTERN + \")\",\n" +
                "            Pattern.MULTILINE\n" +
                "    );\n" +
                "\n" +
                "    @Override\n" +
                "    public Set<String> getLanguageNames() {\n" +
                "        Set<String> names = new HashSet<String>();\n" +
                "        names.add(\"markdown\");\n" +
                "        names.add(\"md\");\n" +
                "        return Collections.unmodifiableSet(names);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public StyleSpans<Collection<String>> computeStyles(String text) {\n" +
                "        Matcher matcher = PATTERN.matcher(text);\n" +
                "        int lastKwEnd = 0;\n" +
                "        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<Collection<String>>();\n" +
                "        \n" +
                "        while(matcher.find()) {\n" +
                "            // 添加前一段无样式文本\n" +
                "            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);\n" +
                "            \n" +
                "            // 确定当前匹配的样式类\n" +
                "            Collection<String> styleClasses = new ArrayList<String>();\n" +
                "            \n" +
                "            // 基本样式类 - 所有Markdown元素都添加md前缀\n" +
                "            styleClasses.add(HighlighterConstants.MD_STYLE);\n" +
                "            \n" +
                "            // 特定样式类\n" +
                "            if (matcher.group(\"H1\") != null) {\n" +
                "                styleClasses.add(HighlighterConstants.MD_H1_STYLE);\n" +
                "            } else if (matcher.group(\"H2\") != null) {\n" +
                "                styleClasses.add(HighlighterConstants.MD_H2_STYLE);\n" +
                "            } else if (matcher.group(\"H3\") != null) {\n" +
                "                styleClasses.add(HighlighterConstants.MD_H3_STYLE);\n" +
                "            } else if (matcher.group(\"H4\") != null) {\n" +
                "                styleClasses.add(HighlighterConstants.MD_H4_STYLE);\n" +
                "            } else if (matcher.group(\"H5\") != null) {\n" +
                "                styleClasses.add(HighlighterConstants.MD_H5_STYLE);\n" +
                "            } else if (matcher.group(\"H6\") != null) {\n" +
                "                styleClasses.add(HighlighterConstants.MD_H6_STYLE);\n" +
                "            } else if (matcher.group(\"EMPHASIS\") != null) {\n" +
                "                styleClasses.add(HighlighterConstants.MD_EMPHASIS_STYLE);\n" +
                "            } else if (matcher.group(\"STRONG\") != null) {\n" +
                "                styleClasses.add(HighlighterConstants.MD_STRONG_STYLE);\n" +
                "            } else if (matcher.group(\"CODEBLOCK\") != null) {\n" +
                "                styleClasses.add(HighlighterConstants.MD_CODE_STYLE);\n" +
                "                \n" +
                "                // 增强功能：检测代码块语言并应用对应高亮\n" +
                "                String codeBlock = matcher.group(\"CODEBLOCK\");\n" +
                "                processCodeBlock(codeBlock, styleClasses);\n" +
                "                \n" +
                "            } else if (matcher.group(\"INLINECODE\") != null) {\n" +
                "                styleClasses.add(HighlighterConstants.MD_CODE_STYLE);\n" +
                "            } else if (matcher.group(\"LINK\") != null) {\n" +
                "                styleClasses.add(HighlighterConstants.MD_LINK_STYLE);\n" +
                "            } else if (matcher.group(\"IMAGE\") != null) {\n" +
                "                styleClasses.add(HighlighterConstants.MD_IMAGE_STYLE);\n" +
                "            } else if (matcher.group(\"LIST\") != null) {\n" +
                "                styleClasses.add(HighlighterConstants.MD_LIST_STYLE);\n" +
                "            } else if (matcher.group(\"QUOTE\") != null) {\n" +
                "                styleClasses.add(HighlighterConstants.MD_QUOTE_STYLE);\n" +
                "            } else if (matcher.group(\"HORIZONTALRULE\") != null) {\n" +
                "                styleClasses.add(HighlighterConstants.MD_HR_STYLE);\n" +
                "            }\n" +
                "            \n" +
                "            // 添加带样式的文本段\n" +
                "            spansBuilder.add(styleClasses, matcher.end() - matcher.start());\n" +
                "            lastKwEnd = matcher.end();\n" +
                "        }\n" +
                "        \n" +
                "        // 添加最后一段无样式文本\n" +
                "        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);\n" +
                "        return spansBuilder.create();\n" +
                "    }\n" +
                "    \n" +
                "    /**\n" +
                "     * 处理代码块，检测语言并添加对应的样式类\n" +
                "     * @param codeBlock 代码块文本\n" +
                "     * @param styleClasses 样式类集合\n" +
                "     */\n" +
                "    private void processCodeBlock(String codeBlock, Collection<String> styleClasses) {\n" +
                "        try {\n" +
                "            // 检测代码块语言\n" +
                "            Matcher langMatcher = CODE_LANG_PATTERN.matcher(codeBlock);\n" +
                "            if (langMatcher.find()) {\n" +
                "                String language = langMatcher.group(1).toLowerCase();\n" +
                "                \n" +
                "                // 添加语言特定的样式类\n" +
                "                styleClasses.add(HighlighterConstants.MD_CODE_STYLE + \"-\" + language);\n" +
                "                \n" +
                "                // 根据不同语言添加特定样式\n" +
                "                switch (language) {\n" +
                "                    case \"java\":\n" +
                "                        styleClasses.add(HighlighterConstants.JAVA_STYLE);\n" +
                "                        break;\n" +
                "                    case \"python\":\n" +
                "                    case \"py\":\n" +
                "                        styleClasses.add(HighlighterConstants.PYTHON_STYLE);\n" +
                "                        break;\n" +
                "                    case \"javascript\":\n" +
                "                    case \"js\":\n" +
                "                        styleClasses.add(HighlighterConstants.JS_STYLE);\n" +
                "                        break;\n" +
                "                    case \"xml\":\n" +
                "                    case \"html\":\n" +
                "                        styleClasses.add(HighlighterConstants.XML_STYLE);\n" +
                "                        break;\n" +
                "                    case \"json\":\n" +
                "                        styleClasses.add(HighlighterConstants.JSON_STYLE);\n" +
                "                        break;\n" +
                "                    case \"css\":\n" +
                "                        styleClasses.add(HighlighterConstants.CSS_STYLE);\n" +
                "                        break;\n" +
                "                    case \"yaml\":\n" +
                "                    case \"yml\":\n" +
                "                        styleClasses.add(HighlighterConstants.YAML_STYLE);\n" +
                "                        break;\n" +
                "                    case \"groovy\":\n" +
                "                        styleClasses.add(HighlighterConstants.GROOVY_STYLE);\n" +
                "                        break;\n" +
                "                    case \"properties\":\n" +
                "                        styleClasses.add(HighlighterConstants.GROOVY_STYLE);\n" +
                "                        break;\n" +
                "                }\n" +
                "            }\n" +
                "        } catch (Exception e) {\n" +
                "            // 出现异常时不影响基本高亮功能\n" +
                "            // 异常已被捕获，无需额外处理\n" +
                "        }\n" +
                "    }\n" +
                "}";
        codeArea.replaceText(javaCode);
    }
    
    private void loadPythonExample() {
        String pythonCode = "def greet(name):\n" +
                "    print(f\"Hello, {name}!\")\n\n" +
                "if __name__ == \"__main__\":\n" +
                "    greet(\"World\")\n";
        codeArea.replaceText(pythonCode);
    }
    
    private void loadMarkdownExample() {
        String markdownCode = "# Markdown示例\n\n" +
                "这是一个**Markdown**示例文件，展示*各种*样式。\n\n" +
                "## 代码块示例\n\n" +
                "```java\n" +
                "public class Example {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello\");\n" +
                "    }\n" +
                "}\n" +
                "```\n\n" +
                "```python\n" +
                "def hello():\n" +
                "    print(\"Hello\")\n" +
                "```\n\n" +
                "## 列表示例\n\n" +
                "- 项目1\n" +
                "- 项目2\n" +
                "- 项目3\n\n" +
                "> 这是一段引用文本\n";
        codeArea.replaceText(markdownCode);
    }
    
    private void loadXmlExample() {
        String xmlCode = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<root>\n" +
                "    <person id=\"1\">\n" +
                "        <name>张三</name>\n" +
                "        <age>30</age>\n" +
                "        <skills>\n" +
                "            <skill>Java</skill>\n" +
                "            <skill>Python</skill>\n" +
                "        </skills>\n" +
                "    </person>\n" +
                "</root>\n";
        codeArea.replaceText(xmlCode);
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