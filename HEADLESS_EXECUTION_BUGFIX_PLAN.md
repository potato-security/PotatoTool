# Headless 执行链缺陷修复计划

## 最优方案

本轮不做跨 `HeadlessStep` 的浏览器会话复用。

原因：

- 当前 `HeadlessStep` 语义默认是步骤级隔离，直接复用浏览器会话会引入 Cookie、DOM、登录态、跳转历史串扰
- 这类改动会改变 POC 执行语义，收益虽高，但不适合在缺少回归样本的情况下直接落到主线

因此，本轮最优可落地方案是：

1. 在执行层补 `enableHeadless` 兜底，确保关闭开关后不会再意外拉起浏览器
2. 修复 `HeadlessStep.url` 未做变量替换的问题
3. 修复标准 Nuclei Headless 的重复导航问题
4. 将 Headless 超时接入现有扫描配置，而不是写死 `30s`
5. 去掉执行链中的重复兼容性检测，保留 UI 预检和执行器最终校验
6. 将 `requiresHeadless` 判定从“仅靠路径”收敛为“路径 + 协议 + 实际步骤”
7. 补回归测试，覆盖上述缺陷

## 验收清单

- [x] 执行层关闭 `enableHeadless` 后，`HeadlessStep` 不再执行
- [x] `HeadlessStep.url` 支持变量替换
- [x] 首个动作为 `navigate` 时，不再发生隐式预导航 + 动作导航双重执行
- [x] Headless 超时使用步骤 / 扫描配置，而不是硬编码 30 秒
- [x] `PocExecutor` 不再对同一 Headless 执行重复做兼容性检测
- [x] `requiresHeadless` 可由协议或实际步骤正确推断，不再只依赖 `/headless/` 路径
- [x] 新增或更新单测，覆盖执行期开关、导航语义、分类推断

## 实施顺序

1. 修 `PocExecutor`
2. 修 `CdpHeadlessExecutor`
3. 修 `PocClassifier`
4. 补测试并编译验证
