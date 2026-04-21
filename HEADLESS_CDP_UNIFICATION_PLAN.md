# Headless CDP 统一改造计划

## 1. 目标

将漏洞扫描模块的 Headless 执行链从 Selenium WebDriver + ChromeDriver 迁移为与爱企查一致的 Chrome DevTools Protocol, CDP 方案，最终达到：

- 不再依赖 ChromeDriver
- 不再受 Chrome / ChromeDriver 主版本匹配约束
- 浏览器运行时能力统一到一套底层
- 浏览器启动、复用、直连、关闭、清理逻辑统一
- Headless 执行动作保持现有语义兼容
- 对外产品行为稳定，不破坏现有漏洞扫描主链

## 2. 当前结论

- [x] 已确认当前漏洞扫描 Headless 依赖 Selenium `ChromeDriver`
- [x] 已确认当前爱企查链路使用 CDP 直连浏览器，不依赖 ChromeDriver
- [x] 已确认两条链路的浏览器运行时已部分统一到同一浏览器路径解析能力
- [x] 已确认长期最优方向是统一到 CDP，而不是继续维护两套浏览器自动化技术栈
- [x] 已创建本计划文档，作为后续改造和验收清单

## 3. 改造原则

- 不直接在现有 Headless 主链上大面积硬替换，先抽象，再迁移，再删除旧实现
- 保留可回滚能力，至少在一段过渡期内允许旧链路兜底
- 所有对外行为以“漏洞扫描结果语义一致”为第一优先级，不以“代码更优雅”替代稳定性
- 所有新增能力必须考虑 Java 8 兼容
- 商业产品视角下，必须覆盖失败场景、异常清理、并发、取消、代理、权限、资源泄漏、跨平台兼容

## 4. 范围

### 4.1 本次在范围内

- 漏洞扫描 Headless 执行器
- 浏览器会话启动 / 连接 / 关闭 / 清理
- Headless 动作执行层
- Headless 兼容性检测逻辑
- 设置与运行时提示文案
- 自动化测试、集成测试、手工回归清单

### 4.2 本次不在范围内

- 爱企查业务语义本身
- 非浏览器协议执行器，如 HTTP / DNS / TCP / SSL
- 无关 UI 重构

## 5. 当前能力盘点

### 5.1 当前 Headless 动作

当前漏洞扫描 Headless 已支持：

- `navigate`
- `waitload`
- `script`
- `click`
- `input`
- `screenshot`
- `sleep`
- `waitvisible`

### 5.2 当前爱企查 CDP 已有底层能力

- 通过 DevTools 端口连接浏览器
- 读取标签页列表
- 绑定 WebSocket
- 执行 `Page.navigate`
- 执行 `Runtime.evaluate`
- 读取 Cookies
- 获取当前页面 URL 和 HTML
- 托管临时 Chrome 会话
- 复用已有本地浏览器 DevTools 会话

### 5.3 结论

现有 CDP 基础可复用，但还不够直接支撑漏洞扫描 Headless，需要补：

- DOM 查询
- 节点可见性判断
- 元素点击
- 输入文本
- 截图
- 等待条件
- 更强的异常与超时处理

## 6. 实施分阶段清单

## Phase 0. 基线冻结与风险收敛

- [x] 冻结当前 Headless 语义基线，整理现有 Selenium 行为作为对照标准
- [x] 盘点所有 Headless 相关测试、POC 示例、真实使用场景
- [x] 列出当前 `HeadlessHandler` 对外接口、返回值、日志格式，不允许迁移后无意改变
- [x] 确认是否存在依赖 Selenium 特有行为的 POC
- [x] 确认当前 Nuclei Headless 语义映射范围，避免迁移时漏动作
- [x] 增加迁移开关设计，支持灰度和回滚

交付物：

- [x] Headless 语义基线说明
- [x] 迁移影响面清单
- [x] 回滚开关设计说明

### Phase 0 基线记录

Headless 当前对外入口：

- `HeadlessHandler.checkCompatibility()`
- `HeadlessHandler.checkCompatibility(String configuredBrowserPath)`
- `HeadlessHandler.execute(String url, List<BrowserStep> steps, int timeoutSeconds)`
- `HeadlessHandler.navigateAndGetTitle(String url)`
- `HeadlessHandler.executeScript(String url, String script)`

`HeadlessResponse` 必须保持的返回结构：

- `url`
- `success`
- `duration`
- `pageTitle`
- `pageSource`
- `screenshot`
- `scriptResults`
- `logs`
- `error`

当前步骤日志基线：

- 初始化阶段记录 `初始化无头浏览器...` / `✓ 浏览器初始化成功`
- 每步记录 `执行步骤 N: action`
- 成功记录使用 `✓`
- 失败记录使用 `✗`
- 末尾记录页面标题与浏览器关闭

当前 Nuclei Headless 语义映射范围：

- `navigate`
- `waitload`
- `script`
- `click`
- `input`
- `screenshot`
- `sleep`
- `waitvisible`

已确认的 Selenium 特有兼容点：

- `script` 现有语义允许 Selenium 风格脚本体，常见写法是顶层 `return ...`
- `navigate` 依赖 `driver.get()` 的阻塞式加载完成语义
- `click` / `input` 现有链路使用 CSS 选择器直接查找，不保留元素句柄到下一步

当前测试 / POC / 场景盘点：

- 单测：`HeadlessCompatibilityTest`
- 协议与转换测试：`NucleiHeadlessTest`
- 新 POC 验证：`NewPocValidationTest`
- 样例文件：`src/test/resources/nuclei-poc-samples/21-headless-protocol.yml`
- 样例文件：`src/test/resources/nuclei-poc-samples/22-headless-simple.yml`
- 真实使用场景：漏洞扫描 `PocExecutor.executeHeadlessStep()`

迁移影响面：

- `PocExecutor` 仍通过 `HeadlessHandler` 调度，无需修改上层步骤拼装
- 设置页与扫描页仍复用同一浏览器路径配置
- 兼容性预检文案需要同时兼容 Selenium / CDP 双引擎

回滚 / 灰度开关设计（历史记录，已于 2026-04-20 完成单栈收尾后下线）：

- 当前内部开关：JVM 属性 `potatotool.vulnscan.headless.engine`
- 当前环境变量兜底：`POTATOTOOL_HEADLESS_ENGINE`
- 可选值：`selenium` / `cdp`
- 当前默认值：`cdp`
- CDP 自动降级开关：`potatotool.vulnscan.headless.cdpFallback` / `POTATOTOOL_HEADLESS_CDP_FALLBACK`
- CDP 自动降级默认值：`true`
- 对照执行开关：`potatotool.vulnscan.headless.compare.enabled` / `POTATOTOOL_HEADLESS_COMPARE_ENABLED`
- 对照执行默认值：`false`
- 回滚方式：移除开关或显式设置为 `selenium`
- 最终状态：以上迁移开关、fallback、compare 遥测已删除，Headless 主链固定为 CDP

## Phase 1. 抽象统一的 CDP 浏览器运行时

- [x] 从爱企查内嵌实现中抽离通用 CDP Client，不再把 DevTools 实现藏在 `AiqichaSearch`
- [x] 抽离通用标签页发现与连接能力
- [x] 抽离通用页面快照能力
- [x] 抽离通用 JS 执行能力
- [x] 抽离通用 Cookie 读取能力
- [x] 补充通用页面导航能力
- [x] 补充通用等待机制
- [x] 补充通用错误分类
- [x] 补充通用超时控制
- [x] 补充连接断开后的安全回收
- [x] 补充 DevTools 端口不可用时的明确错误信息
- [x] 补充多标签页选择策略
- [x] 补充浏览器崩溃 / 被手动关闭时的检测

交付物：

- [x] 通用 `CdpClient`
- [x] 通用 `CdpBrowserSession`
- [x] 通用 `CdpPage`
- [x] 通用错误与超时模型

## Phase 2. 补齐 Headless 所需的 CDP 动作层

- [x] 实现 `navigate`
- [x] 实现 `waitload`
- [x] 实现 `script`
- [x] 实现 `click`
- [x] 实现 `input`
- [x] 实现 `screenshot`
- [x] 实现 `sleep`
- [x] 实现 `waitvisible`

### Phase 2 必须覆盖的细节

- [x] CSS 选择器查询失败时返回与现有链路等价的错误
- [x] 页面尚未完成动态渲染时可等待
- [x] JS 执行结果支持基础类型、对象、空值、异常
- [x] 输入前支持清空字段
- [x] 点击前考虑元素不可见、被遮挡、未挂载
- [x] 截图支持获取 Base64，与当前返回结构兼容
- [x] `waitvisible` 不能只判断元素存在，必须判断真实可见
- [x] 支持页面跳转后重新定位元素，避免节点句柄失效
- [x] 支持操作超时中断，不允许无限等待

交付物：

- [x] `CdpHeadlessExecutor`
- [x] Headless 动作适配层

## Phase 3. 双栈并行与灰度切换

- [x] 引入 Headless 执行引擎开关，支持 `selenium` / `cdp`
- [x] 默认先保留 Selenium 为兜底，CDP 先做灰度
- [x] 日志中明确当前使用的执行引擎
- [x] 关键失败场景下允许自动降级回 Selenium
- [x] 统计 CDP 与 Selenium 的成功率差异
- [x] 统计 CDP 与 Selenium 的耗时差异
- [x] 对比相同 POC 在两种引擎下的执行结果是否一致

交付物：

- [x] 可灰度切换的 Headless 执行入口
- [x] 对照日志与结果比对机制

说明：

- [x] 灰度与对照能力已落地，当前默认引擎已在 Phase 4 切换到 CDP
- [x] 双栈灰度与对照已完成，相关运行时开关与兼容壳已在最终收尾后删除

## Phase 4. 清理 Selenium / ChromeDriver 依赖

- [x] 当 CDP 通过验收后，将默认引擎切换到 CDP
- [x] 移除 Headless 兼容性检测中对 ChromeDriver 的强依赖
- [x] 移除 `WebDriverManager.chromedriver()` 相关准备逻辑
- [x] 删除 Selenium Headless 专有错误提示
- [x] 删除“浏览器版本需与 ChromeDriver 匹配”的产品提示
- [x] 清理无效依赖、无效文案、无效测试
- [x] 更新帮助文档与用户说明

交付物：

- [x] 不依赖 ChromeDriver 的 Headless 主链
- [x] 清理后的配置与提示体系

Phase 4 当前状态：

- [x] 默认主链已使用 CDP，不再要求用户为主流程准备 ChromeDriver
- [x] 已移除 WebDriverManager 自动下载逻辑
- [x] 已删除 legacy Selenium 兼容壳、引擎切换开关、fallback / compare 遥测
- [x] 已删除 `selenium-java` 运行时依赖

## 7. 必测边界清单

以下每项都必须明确验证，不能默认“应该没问题”。

### 7.1 浏览器运行时

- [ ] 未安装 Chrome / Chromium / Edge
- [ ] 配置路径为空
- [ ] 配置路径存在但不可执行
- [ ] 配置路径存在但无读取权限
- [ ] 浏览器路径包含空格
- [ ] 浏览器路径为软链接
- [ ] 浏览器已运行
- [ ] 浏览器未运行
- [ ] 临时浏览器启动失败
- [ ] DevTools 端口占用
- [ ] DevTools 端口未监听
- [ ] 浏览器启动后瞬间崩溃
- [ ] 临时目录无法创建
- [ ] 临时目录无法删除
- [ ] 大量并发时端口分配冲突

### 7.2 页面与网络

- [ ] 页面正常加载
- [ ] 页面 30x 跳转
- [ ] 页面长时间 loading
- [ ] 页面 JS 渲染后才出现目标元素
- [ ] 页面执行中主动跳转到新 URL
- [ ] 页面打开新标签页
- [ ] 页面包含 iframe
- [ ] 页面包含跨域 iframe
- [ ] 页面包含 Shadow DOM
- [ ] 页面标题为空但页面真实存在
- [ ] 页面 HTML 超大
- [ ] 页面编码非 UTF-8
- [ ] 页面执行时浏览器标签被用户关闭
- [ ] 页面执行时 DevTools 连接被断开

### 7.3 Headless 动作

- [ ] `navigate` 正常 URL
- [ ] `navigate` 非法 URL
- [ ] `waitload` 在超时前成功
- [ ] `waitload` 超时
- [ ] `script` 返回字符串
- [ ] `script` 返回数字
- [ ] `script` 返回布尔
- [ ] `script` 返回对象
- [ ] `script` 返回 `null`
- [ ] `script` 抛异常
- [ ] `click` 目标元素存在
- [ ] `click` 目标元素不存在
- [ ] `click` 目标元素不可见
- [ ] `click` 目标元素被遮挡
- [ ] `input` 普通文本
- [ ] `input` 空文本
- [ ] `input` 多行文本
- [ ] `input` 中文
- [ ] `input` 特殊字符
- [ ] `screenshot` 正常截图
- [ ] `screenshot` 页面过长
- [ ] `waitvisible` 成功
- [ ] `waitvisible` 超时
- [ ] `sleep` 中途取消

### 7.4 扫描执行链

- [ ] 扫描任务正常执行
- [ ] 扫描中取消任务
- [ ] 扫描线程中断
- [ ] 多目标并发扫描
- [ ] 多 POC 并发执行
- [ ] 单目标多个 Headless 步骤连续执行
- [ ] Headless 失败后不影响非 Headless 任务继续
- [ ] 日志输出线程安全
- [ ] 超时后资源能释放
- [ ] 异常后资源能释放

### 7.5 代理与网络环境

- [ ] 主代理关闭
- [ ] 主代理开启
- [ ] 子服务代理关闭
- [ ] 子服务代理开启
- [ ] 系统代理开启
- [ ] 强制直连模式生效
- [ ] 在国外代理环境下不误走代理
- [ ] 代理不可达
- [ ] 代理配置错误

### 7.6 跨平台

- [ ] macOS
- [ ] Windows
- [ ] Linux
- [ ] 中文路径
- [ ] 非管理员权限运行
- [ ] 低权限目录运行

## 8. 测试与验收矩阵

## 8.1 单元测试

- [ ] CDP 连接握手测试
- [x] DevTools 标签页发现测试
- [ ] 页面快照提取测试
- [x] JS 执行结果解析测试
- [ ] 选择器查询测试
- [ ] 可见性判断测试
- [ ] 点击测试
- [ ] 输入测试
- [ ] 截图测试
- [ ] 超时测试
- [ ] 取消测试
- [ ] 浏览器进程关闭清理测试

## 8.2 集成测试

- [ ] 本地测试页覆盖全部动作
- [ ] 动态 DOM 测试页
- [ ] 延迟渲染测试页
- [ ] 跳转测试页
- [ ] 截图测试页
- [ ] 表单输入测试页
- [ ] 多标签页测试页
- [ ] iframe 测试页

## 8.3 回归测试

- [x] 现有 Headless 测试全部通过
- [x] Aiqicha 现有浏览器辅助链路不受影响
- [ ] 浏览器路径自动探测不受影响
- [x] 设置页 Browser/Python 配置不受影响
- [x] 编译通过
- [ ] 目标平台手工回归通过

### 8.3.1 本轮已完成验证（2026-04-20）

- [x] `-DskipTests compile`
- [x] `HeadlessCompatibilityTest`
- [x] `CdpHeadlessExecutorTest`
- [x] `BrowserRuntimeConfigTest`
- [x] `CdpTargetSelectorTest`
- [x] `NucleiHeadlessTest`
- [x] `NewPocValidationTest`
- [x] `AiqichaSearchRetryPolicyTest`
- [x] `PaneSettingRuntimePathConfigTest`
- [x] `PaneSettingAiConfigTest`
- [x] `PaneSettingProxyValidationTest`
- [x] `PaneSettingOobDnsValidationTest`
- [x] `PaneVulScanProxyGateTest`

## 8.4 商业产品验收标准

- [x] 默认用户不再需要关心 ChromeDriver
- [x] 用户提示明确，不出现技术栈术语堆砌
- [x] 异常提示可执行，不是仅打印堆栈
- [ ] 失败能回退，不导致整条扫描流程卡死
- [ ] 资源泄漏可控，不残留大量 Chrome 进程
- [ ] 大批量扫描时性能与稳定性可接受

## 9. 风险清单

- [ ] CDP 自研动作层存在语义偏差，导致结果与 Selenium 不一致
- [ ] 某些复杂页面的点击 / 输入行为不如 Selenium 稳定
- [ ] DevTools 协议兼容差异导致不同 Chrome 版本行为不一致
- [ ] 多线程扫描下浏览器实例数暴涨，导致资源占用异常
- [ ] 迁移后部分旧 POC 的 Headless 语义失真
- [ ] 过早移除 Selenium 会导致回滚困难

应对策略：

- [x] 保留迁移开关（灰度完成后已移除）
- [x] 保留双栈对照周期
- [ ] 每个动作先做本地测试页验证，再做真实 POC 回归
- [ ] 对所有失败类型做结构化日志分类

## 10. 回滚条件

最终收尾后已不再保留 Selenium 运行时回滚；若出现以下任一条件，需回退代码版本或发布补丁修复：

- [ ] Headless 成功率明显低于现网基线
- [ ] 关键商业场景出现结果偏差
- [ ] 浏览器资源泄漏不可接受
- [ ] 大批量扫描下稳定性下降
- [ ] 短时间内无法修复的跨平台兼容问题出现

## 11. 当前执行进度

- [x] 方向确认：统一到 CDP
- [x] 风险前置分析完成
- [x] 计划文档创建完成
- [x] Phase 0 完成
- [x] Phase 1 完成
- [x] Phase 2 完成
- [x] Phase 3 完成
- [x] Phase 4 完成

## 12. 下一步建议

主线改造已完成。后续只剩发布后的手工 / 跨平台验收：

1. 补 macOS / Windows / Linux 手工回归
2. 观察真实批量扫描场景下的资源占用与稳定性
3. 如出现跨平台或复杂页面语义偏差，直接在 CDP 主链修复，不再回退到 Selenium 双栈
