package com.potato.potatotool;

/**
 * @author Potato
 * @date 2023/11/23 17:16
 */
public class ToStart {

    public static boolean debugMode = false;
    public static boolean isBlueMode = true;

    public static void main(String[] args) {

        System.setProperty("java.net.useSystemProxies", "false");// 关闭使用代理（无法关闭jar启动前的全局代理）
        System.setProperty("prism.lcdtext", "false");// 关闭字体锯齿效果
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");// 关闭Polyglot告警
        System.setProperty("http.keepAlive", "false");   // 禁用 Keep-Alive

        // 检查命令行参数是否包含 "debug" 参数
        for (String arg : args) {
            if (arg.equals("debug") || arg.equals("-debug") || arg.equals("--debug")) {
                debugMode = true;
                System.out.println("------------已开启DEBUG模式，请注意报错信息，用于提取提交bug------------");
                break;
            }
        }
        MainApplication.main(args);
    }

}
