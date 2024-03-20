package com.potato.potatotool.utils;

/**
 * @author Potato
 * @date 2023/5/11 09:11
 */

public class codeAnalyzerUtils {

    static aiUtil aiObj=new aiUtil();

    public static void evilCodeAnalysis(String evilCode,Object node) throws Exception {

        aiObj.historyList.clear();
        aiObj.isFirstResponse = true;

        System.out.println("------------------------------------------------------------------------");
        aiObj.askAi("你现在作为一名资深恶意代码分析工程师。用中文回答，以下代码可能是攻击者留下的恶意代码，详细分析它具体实现的功能：```" + evilCode + "```", node);
        System.out.println("------------------------------------------------------------------------");
        aiObj.askAi("你现在作为一名资深应急响应工程师，针对以上攻击者的恶意代码实现的功能，针对性地应急处理的具体措施是什么", node);
        System.out.println("------------------------------------------------------------------------");

    }


    //  优化代码，如反编译后的代码
    public static void optimizedCode(String code, Object node) throws Exception {

        aiObj.historyList.clear();
        aiObj.isFirstResponse = true;

        System.out.println("------------------------------------------------------------------------");
        aiObj.askAi("你现在作为一名资深开发者，请针对以下代码进行优化名称变量、添加单行注释：```" + code + "```", node);
        System.out.println("------------------------------------------------------------------------");

    }

}