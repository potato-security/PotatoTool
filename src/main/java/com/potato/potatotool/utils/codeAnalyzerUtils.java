package com.potato.potatotool.utils;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.fxmisc.richtext.CodeArea;

/**
 * @author Potato
 * @date 2023/5/11 09:11
 */

public class codeAnalyzerUtils {

    public static void evilCodeAnalysis(String evilCode,Object node) throws Exception {
        aiUtil aiObj=new aiUtil();

        aiObj.isFirstResponse = true;

        aiObj.askAi("你现在作为一名资深恶意代码分析工程师。用中文回答，以下代码可能是攻击者留下的恶意代码，详细分析它具体实现的功能：```" + evilCode + "```", node);

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

        aiObj.askAi("你现在作为一名资深应急响应工程师，针对刚才攻击者的恶意代码实现的步骤及功能，针对性地应急处理的具体措施是什么", node);

    }


    //  优化代码，如反编译后的代码
    public static void optimizedCode(String code, Object node) throws Exception {
        aiUtil aiObj=new aiUtil();

        aiObj.isFirstResponse = true;

        aiObj.askAi("反编译后的代码可读性差，请优化代码逻辑并添加详细注释。以下是需要优化的代码：```" + code + "```", node);

    }

    public static void main(String[] args) {
        aiUtil aiObj=new aiUtil();

        aiObj.askAi("请告诉我关于```获取当前系统用户```的命令", null);
    }

}