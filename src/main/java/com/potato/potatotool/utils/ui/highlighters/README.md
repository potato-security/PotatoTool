# 多语言代码高亮工具_测试版_暂未使用 By Potato

这个包提供了基于RichTextFX的多语言代码高亮功能，支持Java、Python等多种编程语言的语法高亮。

## 功能特点

- 支持多种编程语言的语法高亮
- 可扩展的语言高亮器接口，方便添加新的语言支持
- 异步处理高亮，不阻塞UI线程
- 智能代码类型检测，自动选择合适的高亮方案
- 动态内容检测，根据编辑内容实时切换高亮器
- Markdown代码块语言检测，支持代码块内部语法高亮

## 目录结构

```
highlighters/
├── MultiLanguageHighlighter.java       # 核心高亮引擎
├── HighlighterFactory.java             # 高亮器工厂
├── CodeTypeDetector.java               # 代码类型检测器
├── AbstractStyler.java                 # 高亮器抽象基类
├── HighlighterConstants.java           # 高亮器常量
├── PatternUtils.java                   # 正则表达式工具类
├── stylers/                            # 语言高亮器实现
│   ├── JavaStyler.java                 # Java高亮器
│   ├── PythonStyler.java               # Python高亮器
│   ├── JavaScriptStyler.java           # JavaScript高亮器
│   ├── JsonStyler.java                 # JSON高亮器
│   ├── XmlStyler.java                  # XML高亮器
│   ├── MarkdownStyler.java             # Markdown高亮器
│   ├── CssStyler.java                  # CSS高亮器
│   ├── YamlStyler.java                 # YAML高亮器
│   ├── GroovyStyler.java               # Groovy高亮器
│   ├── PropertiesStyler.java           # Properties高亮器
│   └── ImageJMacroStyler.java          # ImageJ宏高亮器
└── examples/                           # 示例代码
    ├── AutoDetectionExample.java       # 自动检测示例
    └── DynamicDetectionExample.java    # 动态检测示例
```

## 使用方法

### 1. 基本用法

```java
// 创建CodeArea组件
CodeArea codeArea = new CodeArea();

// 使用工厂类创建高亮器并初始化
MultiLanguageHighlighter highlighter = HighlighterFactory.createHighlighter(codeArea, "java");

// 或者直接使用Java高亮器
MultiLanguageHighlighter javaHighlighter = HighlighterFactory.createJavaHighlighter(codeArea);

// 使用智能代码类型检测
String code = "public class Test { ... }";
MultiLanguageHighlighter autoHighlighter = HighlighterFactory.createHighlighterWithAutoDetection(codeArea, "Test.java", code);
```

### 2. 切换语言

```java
// 获取指定语言的高亮器
MultiLanguageHighlighter.LanguageStyler pythonStyler = HighlighterFactory.getStylerForLanguage("python");

// 切换高亮器
highlighter.changeLanguageStyler(pythonStyler);
```

## 架构说明

代码高亮器采用了分层架构设计：

1. `MultiLanguageHighlighter` - 核心高亮引擎，负责监听文本变化并应用高亮样式
2. `AbstractStyler` - 高亮器抽象基类，提供通用的高亮实现逻辑
3. `HighlighterConstants` - 常量类，存储各种语言高亮器共用的常量
4. `PatternUtils` - 工具类，提供创建和管理高亮器正则表达式的通用方法
5. 具体语言高亮器 - 继承自AbstractStyler或直接实现LanguageStyler接口，定义特定语言的高亮规则
6. `HighlighterFactory` - 工厂类，管理和创建各种语言的高亮器实例
7. `CodeTypeDetector` - 代码类型检测器，智能识别代码类型

## 添加新的语言支持

要添加新的语言支持，只需继承AbstractStyler类或直接实现LanguageStyler接口，并在HighlighterFactory中注册：

```java
// 1. 创建新的语言高亮器类
public class NewLanguageStyler extends AbstractStyler {
    // 定义语言关键字和模式
    private static final Pattern PATTERN = PatternUtils.createNewLanguagePattern();
    
    @Override
    public Set<String> getLanguageNames() {
        Set<String> names = new HashSet<>();
        names.add("newlang");
        return Collections.unmodifiableSet(names);
    }
    
    @Override
    protected Pattern getPattern() {
        return PATTERN;
    }
    
    @Override
    protected String getStyleClass(Matcher matcher) {
        return matcher.group("KEYWORD") != null ? HighlighterConstants.STYLE_KEYWORD :
               matcher.group("STRING") != null ? HighlighterConstants.STYLE_STRING :
               matcher.group("COMMENT") != null ? HighlighterConstants.STYLE_COMMENT :
               null;
    }
}

// 2. 在PatternUtils中添加新语言的模式创建方法
public static Map<String, String> createNewLanguagePatterns() {
    Map<String, String> patterns = new HashMap<>();
    patterns.put("KEYWORD", createKeywordPattern(NEW_LANGUAGE_KEYWORDS));
    patterns.put("STRING", STRING_PATTERN);
    patterns.put("COMMENT", COMMENT_PATTERN);
    return patterns;
}

public static Pattern createNewLanguagePattern() {
    return buildPattern(createNewLanguagePatterns());
}

// 3. 在HighlighterFactory中注册新的高亮器
static {
    // 自动注册所有语言高亮器
    registerStyler(new JavaStyler());
    registerStyler(new PythonStyler());
    // ...
    registerStyler(new NewLanguageStyler());
}
```