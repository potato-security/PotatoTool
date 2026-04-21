package com.potato.potatotool.utils.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AiPromptUtils {
    private static final String DEFAULT_ATTACHMENT_ANALYSIS_REQUEST =
            "请结合我上传的文件进行分析，概述主要内容，指出安全风险、异常点或值得关注的线索，并给出后续建议。";

    private AiPromptUtils() {
    }

    public static String generalAssistantSystemPrompt() {
        return "你是 PotatoTool 内置的中文安全分析助手。\n"
                + "请遵守以下规则：\n"
                + "1. 直接回答用户目标，先给结论，再补充依据。\n"
                + "2. 若用户提供了代码、日志、配置、流量或样本片段，优先围绕这些内容分析，不要泛泛而谈。\n"
                + "3. 涉及安全判断时，明确区分“已确认 / 高度疑似 / 需要更多信息”，不要编造未观察到的事实。\n"
                + "4. 如内容存在截断、脱敏或二进制摘录，需明确说明分析边界。\n"
                + "5. 默认使用中文，结构清晰，适度使用小标题或列表。";
    }

    public static String buildConversationPrompt(String userQuestion, List<AttachmentContext> attachments) {
        String normalizedQuestion = trimToEmpty(userQuestion);
        List<AttachmentContext> safeAttachments = attachments == null
                ? Collections.<AttachmentContext>emptyList()
                : attachments;
        if (safeAttachments.isEmpty()) {
            return normalizedQuestion;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("【用户目标】\n")
                .append(normalizedQuestion.isEmpty() ? DEFAULT_ATTACHMENT_ANALYSIS_REQUEST : normalizedQuestion)
                .append("\n\n")
                .append("【本地附件上下文】\n")
                .append("以下内容由桌面工具从用户本地文件读取后注入，仅作为分析上下文。")
                .append("若内容被截断、为二进制摘录或缺少上下文，请在结论中明确说明边界。\n\n");

        for (int i = 0; i < safeAttachments.size(); i++) {
            AttachmentContext attachment = safeAttachments.get(i);
            if (attachment == null) {
                continue;
            }
            builder.append("### 附件")
                    .append(i + 1)
                    .append("\n")
                    .append("文件名: ")
                    .append(trimToEmpty(attachment.getFileName()))
                    .append("\n")
                    .append("路径: ")
                    .append(trimToEmpty(attachment.getAbsolutePath()))
                    .append("\n")
                    .append("大小: ")
                    .append(attachment.getFileSize())
                    .append(" bytes\n")
                    .append("内容类型: ")
                    .append(attachment.isBinarySnippet() ? "二进制摘录" : "文本内容");
            if (attachment.isTruncated()) {
                builder.append("（已截断）");
            }
            builder.append("\n内容如下:\n<attachment-content>\n")
                    .append(trimToEmpty(attachment.getContent()))
                    .append("\n</attachment-content>\n\n");
        }

        builder.append("请优先围绕上述附件完成分析；如果用户问题与附件不完全一致，请同时覆盖两者。");
        return builder.toString().trim();
    }

    public static String buildConversationHistoryLabel(String userQuestion, List<AttachmentContext> attachments) {
        String normalizedQuestion = trimToEmpty(userQuestion);
        if (attachments == null || attachments.isEmpty()) {
            return normalizedQuestion;
        }

        List<String> fileNames = new ArrayList<String>();
        for (AttachmentContext attachment : attachments) {
            if (attachment == null) {
                continue;
            }
            String fileName = trimToEmpty(attachment.getFileName());
            if (!fileName.isEmpty()) {
                fileNames.add(fileName);
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append(normalizedQuestion.isEmpty() ? DEFAULT_ATTACHMENT_ANALYSIS_REQUEST : normalizedQuestion);
        if (!fileNames.isEmpty()) {
            builder.append("\n[附件: ");
            for (int i = 0; i < fileNames.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(fileNames.get(i));
            }
            builder.append("]");
        }
        return builder.toString();
    }

    public static String attachmentAnalysisRequest() {
        return DEFAULT_ATTACHMENT_ANALYSIS_REQUEST;
    }

    public static String codeOptimizationSystemPrompt() {
        return "你是资深代码审阅与逆向可读性优化助手，擅长整理反编译结果。\n"
                + "请遵守以下规则：\n"
                + "1. 以提升可读性和理解效率为目标，不要杜撰不存在的业务背景或方法体。\n"
                + "2. 保留原始逻辑语义，无法确定含义的部分要明确标注不确定点。\n"
                + "3. 优先解释核心流程、关键分支、外部依赖、异常处理与潜在风险。\n"
                + "4. 默认使用中文，输出结果应可直接给安全研究人员阅读。";
    }

    public static String buildCodeOptimizationPrompt(String code) {
        return "请整理下面的反编译代码，并输出更易读的结果：\n"
                + "1. 尽量把明显无语义的变量、字段和中间值解释清楚。\n"
                + "2. 为关键逻辑补充简洁中文注释，重点说明入口、核心处理流程、重要条件判断和副作用。\n"
                + "3. 如果局部语义无法确定，请保留原状并指出原因。\n\n"
                + "【待整理代码】\n"
                + "<decompiled-code>\n"
                + trimToEmpty(code)
                + "\n</decompiled-code>";
    }

    public static String securityAnalysisSystemPrompt() {
        return "你是资深蓝队安全分析与应急响应专家，擅长研判脚本、代码、流量和样本片段。\n"
                + "请遵守以下规则：\n"
                + "1. 先给结论，再给依据；不要输出空泛套话。\n"
                + "2. 若证据不足，必须明确说明为什么暂时无法定性，以及还需要哪些上下文。\n"
                + "3. 对恶意或高风险内容，要说明攻击目的、关键能力、触发条件、影响面和检测线索。\n"
                + "4. 对正常或灰色内容，也要解释为何容易误报，并给出审计关注点。\n"
                + "5. 默认使用中文，保持结论可直接用于研判或处置。";
    }

    public static String buildSecurityAnalysisPrompt(String evilCode, String encodeModes) {
        return "请对以下内容进行一次完整安全研判，并直接输出最终结果。\n\n"
                + "【编码/混淆特征】\n"
                + trimToEmpty(encodeModes)
                + "\n\n【待分析内容】\n"
                + "<analysis-content>\n"
                + trimToEmpty(evilCode)
                + "\n</analysis-content>\n\n"
                + "请按以下结构输出：\n"
                + "1. 性质初判：在“恶意 / 可疑 / 正常 / 无法判断”中给出结论，并给出置信度。\n"
                + "2. 关键依据：列出支撑结论的核心迹象、行为或语义特征。\n"
                + "3. 行为与技术解析：说明主要功能、执行链路、利用方式或依赖条件。\n"
                + "4. 风险与影响面：说明可能造成的安全后果、受影响资产或攻击阶段；如判定为恶意，补充相关 ATT&CK 技术或攻击链位置。\n"
                + "5. 检测与审计线索：给出可观察的 IoC / IoA、日志点、配置项、进程或网络线索。\n"
                + "6. 处置建议：按“遏制 -> 排查 -> 根除 -> 恢复/加固”组织建议。\n"
                + "7. 不确定点：说明当前结论的边界，以及建议补充的样本、日志或运行上下文。";
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class AttachmentContext {
        private final String fileName;
        private final String absolutePath;
        private final long fileSize;
        private final String content;
        private final boolean binarySnippet;
        private final boolean truncated;

        public AttachmentContext(String fileName,
                                 String absolutePath,
                                 long fileSize,
                                 String content,
                                 boolean binarySnippet,
                                 boolean truncated) {
            this.fileName = fileName == null ? "" : fileName;
            this.absolutePath = absolutePath == null ? "" : absolutePath;
            this.fileSize = Math.max(0L, fileSize);
            this.content = content == null ? "" : content;
            this.binarySnippet = binarySnippet;
            this.truncated = truncated;
        }

        public String getFileName() {
            return fileName;
        }

        public String getAbsolutePath() {
            return absolutePath;
        }

        public long getFileSize() {
            return fileSize;
        }

        public String getContent() {
            return content;
        }

        public boolean isBinarySnippet() {
            return binarySnippet;
        }

        public boolean isTruncated() {
            return truncated;
        }
    }
}
