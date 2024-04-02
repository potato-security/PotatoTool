package com.potato.potatotool;

import com.potato.potatotool.utils.strUtils;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import java.net.URI;

/**
 * @author Potato
 * @date 2023/11/23 17:16
 */
public class ToStart {

    // 此处借鉴了冰蝎的启动环境监测
    public static void main(String[] args) {
        try {

            ClassLoader.getSystemClassLoader().loadClass("javafx.application.Application");
            MainApplication.main(args);

        } catch (ClassNotFoundException classE) {

            try {
                String javaVersion = System.getProperty("java.version");

                boolean supportedJavaVersion = true;
                // 判断当前Java版本是否大于等于17
                if (javaVersion.startsWith("1.")) {
                    // 处理旧版本的情况，如Java 8、Java 11等
                    int version = Integer.parseInt(javaVersion.substring(2, javaVersion.indexOf('.')));
                    if (version < 17) {
                        supportedJavaVersion = false;
                    }
                } else {
                    // 处理新版本的情况，如Java 17、Java 18等
                    String[] versionParts = javaVersion.split("\\.");
                    int version = Integer.parseInt(versionParts[0]);
                    if (version < 17) {
                        supportedJavaVersion = false;
                    }
                }

                if(!supportedJavaVersion){
                    int response = JOptionPane.showConfirmDialog((Component)null, "当前Java版本过低，本项目需要Java17或更高版本\nAzul Zulu 是一个兼容性更强的OpenJDK发行版，高版本也内置有javafx\n是否打开网页下载？", "错误", 0);
                    if (response == 0) {
                        String url = "https://www.azul.com/downloads/?package=jdk#zulu";
                        openWebpage(new URI(url));
                    }
                    return;
                }

                String javafxPath = strUtils.getSelfPath() + File.separator + "lib";
                String cmd = "java --module-path \"" + javafxPath + "\" --add-modules=javafx.controls --add-modules=javafx.fxml -jar";
                cmd = cmd + " " + strUtils.getSelfJarPath();
                Process p = null;
                if (System.getProperty("os.name").toLowerCase().indexOf("windows") >= 0) {
                    Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", cmd});
                } else {
                    p = Runtime.getRuntime().exec(new String[]{"bash", "-c", cmd});
                }

                if (p.waitFor() == 1) {
                    strUtils.setClipboardString(cmd);
                    int response = JOptionPane.showConfirmDialog((Component)null, "本地未检测到JavaFX环境，本项目需要Java17/17+，Java1.8以后的版本不再集成Javafx，需要单独下载Javafx_v21.0.2\n下载后可将javaFX SDK的lib目录拷贝至同目录下，本工具会自动调用；也可通过命令行手动指定SDK目录(命令已拷贝至系统剪切板)\n是否打开网页下载？", "错误", 0);
                    if (response == 0) {
                        String url = "https://gluonhq.com/products/javafx/";
                        openWebpage(new URI(url));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }

    public static boolean openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Action.BROWSE)) {
            try {
                desktop.browse(uri);
                return true;
            } catch (Exception var3) {
                var3.printStackTrace();
            }
        }
        return false;
    }

}
