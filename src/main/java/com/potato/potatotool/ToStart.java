package com.potato.potatotool;

/**
 * @author Potato
 * @date 2023/11/23 17:16
 */
public class ToStart {

    public static boolean debugMode = false;

    public static void main(String[] args) {

        // 检查命令行参数是否包含 "debug" 参数
        for (String arg : args) {
            if (arg.equals("debug")) {
                debugMode = true;
                System.out.println("------------已开启DEBUG模式，请注意报错信息，用于提取提交bug------------");
                break;
            }
        }
        MainApplication.main(args);
    }

}
