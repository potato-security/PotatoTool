package com.potato.potatotool.controller;

import com.potato.potatotool.utils.strUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.File;
import java.util.HashMap;

import static com.potato.potatotool.content.webShell.getWebShell;

/**
 * @author Potato
 * @date 2023/2/19 15:05
 */
public class PaneWebshellGeneration {
    @FXML
    private TextField webShellKey;

    @FXML
    private Label tipTitle;

    @FXML
    public void webshellGeneration() {
        String pass = webShellKey.getText().isEmpty()? "potato" : webShellKey.getText() ;
        try {
            HashMap<String, HashMap<String, String[][]>> multiDict = new HashMap<String, HashMap<String, String[][]>>();

            multiDict.put("", new HashMap<String, String[][]>());
            multiDict.put("Godzilla", new HashMap<String, String[][]>());
            multiDict.put("Behinder", new HashMap<String, String[][]>());
            multiDict.put("AntSword", new HashMap<String, String[][]>());
            multiDict.put("Cmd", new HashMap<String, String[][]>());

            multiDict.get("AntSword").put("jsp", new String[][]{ {"default"}, {"default", "unicode"} });
            multiDict.get("AntSword").put("jspx", new String[][]{ {"default"}, {"default", "unicode"} });
            multiDict.get("AntSword").put("php", new String[][]{ {"default"}, {"base64"} });
            multiDict.get("AntSword").put("asp", new String[][]{ {"default"} });
            multiDict.get("AntSword").put("aspx", new String[][]{ {"default"} });

            multiDict.get("Godzilla").put("jsp", new String[][]{ {"AES", "RAW"}, {"AES", "RAW", "unicode"} });
            multiDict.get("Godzilla").put("jspx", new String[][]{ {"AES", "RAW"}, {"AES", "RAW", "unicode"} });
            multiDict.get("Godzilla").put("php", new String[][]{ {"XOR", "base64"} });
            multiDict.get("Godzilla").put("asp", new String[][]{ {"RAW"} });
            multiDict.get("Godzilla").put("aspx", new String[][]{ {"CSHARP", "AES", "RAW"} });
            multiDict.get("Godzilla").put("ashx", new String[][]{ {"CSHARP", "AES", "RAW"} });

            multiDict.get("Behinder").put("jsp", new String[][]{ {"default"}, {"default", "unicode"} });
            multiDict.get("Behinder").put("jspx", new String[][]{ {"default"}, {"default", "unicode"} });
            multiDict.get("Behinder").put("php", new String[][]{ {"default"} });
            multiDict.get("Behinder").put("asp", new String[][]{ {"default"} });
            multiDict.get("Behinder").put("aspx", new String[][]{ {"default"} });

            multiDict.get("Cmd").put("jsp", new String[][]{ {"default"}, {"reflect"}, {"default", "unicode"}, {"reflect", "unicode"} });
            multiDict.get("Cmd").put("jspx", new String[][]{ {"default"}, {"reflect"}, {"default", "unicode"}, {"reflect", "unicode"} });
            multiDict.get("Cmd").put("ashx", new String[][]{ {"default"} });

            for (String outerKey : multiDict.keySet()) {
                for (String innerKey : multiDict.get(outerKey).keySet()) {
                    for (String[] value : multiDict.get(outerKey).get(innerKey)) {

                        String webShell_Manager = outerKey;
                        String scriptMethod = innerKey;
                        String[] enMethod = value;
                        String aesKey = strUtils.md5(pass).substring(0, 16);

                        String webShellData = getWebShell(webShell_Manager, scriptMethod, enMethod, pass, aesKey);

                        String enMothod_Str =  String.join("_", enMethod);
                        String fileName = "." + File.separator + "webshell_KeyIs_" + pass + File.separator + scriptMethod  + File.separator + webShell_Manager + "_" + enMothod_Str + "." + scriptMethod;
                        fileName = fileName.replace("__", "_").replace("_.", ".");
                        strUtils.createFile(webShellData, fileName);

                    }
                }
            }
            tipTitle.setText("webShell已生成到当前目录下：./webshell_KeyIs_" + pass + "/");

        }catch (Exception e){
            e.printStackTrace();
            tipTitle.setText("webShell生成失败，详细请查看命令窗口报错");
        }
        tipTitle.setVisible(true);

    }
}
