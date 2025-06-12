package com.potato.potatotool.utils.ai;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.fxmisc.richtext.CodeArea;

/**
 * @author Potato
 * @date 2023/5/11 09:11
 */

public class CodeAnalyzerUtils {

    public static void evilCodeAnalysis(String evilCode , String encodeModes, Object node) throws Exception {
        AIUtil aiObj=new AIUtil();

        aiObj.isFirstResponse = true;

        String analysisPrompt = String.format(
                "你是具备丰富经验的安全代码分析专家，请使用中文对以下代码或可疑片段进行全面的安全分析：\n\n" +
                        "【编码/混淆特征】：%s\n" +
                        "【待分析内容】如下（可能为代码、交互流量、脚本等）：\n```%s```\n\n" +
                        "请分析以下方面（保持客观中立）：\n" +
                        "1. **性质初判**：\n" +
                        "   - 是否具有恶意特征？请在“恶意 / 正常 / 可疑 / 无法判断”中给出判定，并说明依据和置信度。\n" +
                        "2. **技术解析**：\n" +
                        "   - 描述其功能实现细节，包括用途、行为、涉及的技术。\n" +
                        "3. **安全风险评估**：\n" +
                        "   - 如果存在潜在风险，请描述其威胁模型及可能的攻击场景（如WebShell交互、信息泄露、命令执行等）。\n" +
                        "4. **若被判为恶意代码**：\n" +
                        "   - 关联的 Mitre ATT&CK 技术编号\n" +
                        "   - 所处攻击链环节\n" +
                        "5. **若为正常或可疑但非恶意代码**：\n" +
                        "   - 说明可能引发安全误报的特征\n" +
                        "   - 提出加固建议或代码审计关注点"
                , encodeModes, evilCode
        );

        aiObj.askAi(analysisPrompt, node);

        if (node instanceof TextArea) {
            TextArea textArea = (TextArea) node;
            Platform.runLater(() -> {
                textArea.appendText("\n\n");
            });
        } else {
            CodeArea textArea = (CodeArea) node;
            Platform.runLater(() -> {
                textArea.appendText("\n\n");
            });
        }

        String responsePrompt =
                "你是资深的安全响应专家，请根据前述分析结果，进一步提供响应与防护建议（请区分代码是否具有威胁）：\n\n" +
                        "【1. 若存在安全威胁】\n" +
                        "   - 分阶段处置策略（遏制 → 根除 → 恢复）\n" +
                        "   - 威胁狩猎指标（IoC / IoA），用于检测类似行为\n\n" +
                        "【2. 若为正常或低风险代码】\n" +
                        "   - 说明可能造成误报的原因\n" +
                        "   - 提出误报规避建议（如规则优化、白名单）\n\n" +
                        "【3. 通用建议】\n" +
                        "   - 安全监控和告警策略建议\n" +
                        "   - 审计过程中应重点关注的风险要素"
                ;
        aiObj.askAi(responsePrompt, node);

    }


    //  优化代码，如反编译后的代码
    public static void optimizedCode(String code, Object node) throws Exception {
        AIUtil aiObj=new AIUtil();

        aiObj.isFirstResponse = true;

        aiObj.askAi("这是反编译后得到的代码，当前可读性较差。请帮我对其进行结构优化，提高可读性，并为关键部分添加详细注释，以便理解其逻辑和功能。以下是需要优化的代码：```" + code + "```", node);

    }

    public static void main(String[] args) {
        AIUtil aiObj=new AIUtil();

        aiObj.askAi("请告诉我关于```获取当前系统用户```的命令", null);
    }

}