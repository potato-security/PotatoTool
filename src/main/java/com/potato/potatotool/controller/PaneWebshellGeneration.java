package com.potato.potatotool.controller;

import com.potato.potatotool.utils.strUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
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
    private ComboBox godComboBox;
    @FXML
    private ComboBox godModeComboBox;
    @FXML
    private TextField godKey;
    @FXML
    private Label tipTitleGod;

    @FXML
    private ComboBox behComboBox;
    @FXML
    private ComboBox behModeComboBox;
    @FXML
    private TextField behKey;
    @FXML
    private Label tipTitleBeh;

    @FXML
    private ComboBox antComboBox;
    @FXML
    private ComboBox antModeComboBox;
    @FXML
    private TextField antKey;
    @FXML
    private Label tipTitleAnt;

    @FXML
    private ComboBox cmdComboBox;
    @FXML
    private ComboBox cmdModeComboBox;
    @FXML
    private TextField cmdKey;
    @FXML
    private Label tipTitleCmd;

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
            tipTitle.setText("全部webShell已生成到当前目录下：./webshell_KeyIs_" + pass + "/");

        }catch (Exception e){
            e.printStackTrace();
            tipTitle.setText("webShell生成失败，详细请查看命令窗口报错");
        }
        tipTitle.setVisible(true);

    }

    @FXML
    public void godChoose(ActionEvent event) {
        if(godComboBox.getValue().equals("jsp")){
            ObservableList<String> items = FXCollections.observableArrayList("AES_RAW", "AES_RAW_unicode");
            godModeComboBox.setItems(items);
        }else if(godComboBox.getValue().equals("jspx")){
            ObservableList<String> items = FXCollections.observableArrayList("AES_RAW", "AES_RAW_unicode");
            godModeComboBox.setItems(items);
        }else if(godComboBox.getValue().equals("php")){
            ObservableList<String> items = FXCollections.observableArrayList("XOR_base64");
            godModeComboBox.setItems(items);
        }else if(godComboBox.getValue().equals("asp")){
            ObservableList<String> items = FXCollections.observableArrayList("RAW");
            godModeComboBox.setItems(items);
        }else if(godComboBox.getValue().equals("aspx")){
            ObservableList<String> items = FXCollections.observableArrayList("CSHARP_AES_RAW");
            godModeComboBox.setItems(items);
        }else if(godComboBox.getValue().equals("ashx")){
            ObservableList<String> items = FXCollections.observableArrayList("CSHARP_AES_RAW");
            godModeComboBox.setItems(items);
        }
    }

    @FXML
    public void antChoose(ActionEvent event) {
        if(antComboBox.getValue().equals("jsp")){
            ObservableList<String> items = FXCollections.observableArrayList("default", "default_unicode");
            antModeComboBox.setItems(items);
        }else if(antComboBox.getValue().equals("jspx")){
            ObservableList<String> items = FXCollections.observableArrayList("default", "default_unicode");
            antModeComboBox.setItems(items);
        }else if(antComboBox.getValue().equals("php")){
            ObservableList<String> items = FXCollections.observableArrayList("default", "base64");
            antModeComboBox.setItems(items);
        }else if(antComboBox.getValue().equals("asp")){
            ObservableList<String> items = FXCollections.observableArrayList("default");
            antModeComboBox.setItems(items);
        }else if(antComboBox.getValue().equals("aspx")){
            ObservableList<String> items = FXCollections.observableArrayList("default");
            antModeComboBox.setItems(items);
        }
    }

    @FXML
    public void behChoose(ActionEvent event) {
        if(behComboBox.getValue().equals("jsp")){
            ObservableList<String> items = FXCollections.observableArrayList("default", "default_unicode");
            behModeComboBox.setItems(items);
        }else if(behComboBox.getValue().equals("jspx")){
            ObservableList<String> items = FXCollections.observableArrayList("default", "default_unicode");
            behModeComboBox.setItems(items);
        }else if(behComboBox.getValue().equals("php")){
            ObservableList<String> items = FXCollections.observableArrayList("default");
            behModeComboBox.setItems(items);
        }else if(behComboBox.getValue().equals("asp")){
            ObservableList<String> items = FXCollections.observableArrayList("default");
            behModeComboBox.setItems(items);
        }else if(behComboBox.getValue().equals("aspx")){
            ObservableList<String> items = FXCollections.observableArrayList("default");
            behModeComboBox.setItems(items);
        }
    }

    @FXML
    public void cmdChoose(ActionEvent event) {
        if(cmdComboBox.getValue().equals("jsp")){
            ObservableList<String> items = FXCollections.observableArrayList("default", "default_unicode", "reflect", "reflect_unicode");
            cmdModeComboBox.setItems(items);
        }else if(cmdComboBox.getValue().equals("jspx")){
            ObservableList<String> items = FXCollections.observableArrayList("default", "default_unicode", "reflect", "reflect_unicode");
            cmdModeComboBox.setItems(items);
        }else if(cmdComboBox.getValue().equals("ashx")){
            ObservableList<String> items = FXCollections.observableArrayList("default");
            cmdModeComboBox.setItems(items);
        }
    }

    @FXML
    public void godWebshellGeneration(ActionEvent event) {
        String pass = godKey.getText().isEmpty()? "potato" : godKey.getText();
        String webShell_Manager = "Godzilla";

        if(godComboBox.getValue() == null){
            tipTitleGod.setText("请选择脚本语言！");
            tipTitleGod.setVisible(true);
            return;
        }
        if(godModeComboBox.getValue() == null){
            tipTitleGod.setText("请选择编码方式！");
            tipTitleGod.setVisible(true);
            return;
        }

        String scriptMethod = (String) godComboBox.getValue();
        String enMothod_Str = (String) godModeComboBox.getValue();

        try {

            String[] enMethod = enMothod_Str.split("_");
            String aesKey = strUtils.md5(pass).substring(0, 16);

            String webShellData = getWebShell(webShell_Manager, scriptMethod, enMethod, pass, aesKey);

            String fileName = "." + File.separator + "webshell_KeyIs_" + pass + File.separator + scriptMethod  + File.separator + webShell_Manager + "_" + enMothod_Str + "." + scriptMethod;
            fileName = fileName.replace("__", "_").replace("_.", ".");
            strUtils.createFile(webShellData, fileName);

            tipTitleGod.setText("webShell已生成到当前目录下：./webshell_KeyIs_" + pass + "/");

        }catch (Exception e){
            e.printStackTrace();
            tipTitleGod.setText("webShell生成失败，详细请查看命令窗口报错");
        }
        tipTitleGod.setVisible(true);
    }

    @FXML
    public void behWebshellGeneration(ActionEvent event) {
        String pass = behKey.getText().isEmpty()? "potato" : behKey.getText();
        String webShell_Manager = "Behinder";

        if(behComboBox.getValue() == null){
            tipTitleBeh.setText("请选择脚本语言！");
            tipTitleBeh.setVisible(true);
            return;
        }
        if(behModeComboBox.getValue() == null){
            tipTitleBeh.setText("请选择编码方式！");
            tipTitleBeh.setVisible(true);
            return;
        }

        String scriptMethod = (String) behComboBox.getValue();
        String enMothod_Str = (String) behModeComboBox.getValue();

        try {

            String[] enMethod = enMothod_Str.split("_");
            String aesKey = strUtils.md5(pass).substring(0, 16);

            String webShellData = getWebShell(webShell_Manager, scriptMethod, enMethod, pass, aesKey);

            String fileName = "." + File.separator + "webshell_KeyIs_" + pass + File.separator + scriptMethod  + File.separator + webShell_Manager + "_" + enMothod_Str + "." + scriptMethod;
            fileName = fileName.replace("__", "_").replace("_.", ".");
            strUtils.createFile(webShellData, fileName);

            tipTitleBeh.setText("webShell已生成到当前目录下：./webshell_KeyIs_" + pass + "/");

        }catch (Exception e){
            e.printStackTrace();
            tipTitleBeh.setText("webShell生成失败，详细请查看命令窗口报错");
        }
        tipTitleBeh.setVisible(true);

    }

    @FXML
    public void antWebshellGeneration(ActionEvent event) {
        String pass = antKey.getText().isEmpty()? "potato" : antKey.getText();
        String webShell_Manager = "AntSword";

        if(antComboBox.getValue() == null){
            tipTitleAnt.setText("请选择脚本语言！");
            tipTitleAnt.setVisible(true);
            return;
        }
        if(antModeComboBox.getValue() == null){
            tipTitleAnt.setText("请选择编码方式！");
            tipTitleAnt.setVisible(true);
            return;
        }

        String scriptMethod = (String) antComboBox.getValue();
        String enMothod_Str = (String) antModeComboBox.getValue();

        try {

            String[] enMethod = enMothod_Str.split("_");
            String aesKey = strUtils.md5(pass).substring(0, 16);

            String webShellData = getWebShell(webShell_Manager, scriptMethod, enMethod, pass, aesKey);

            String fileName = "." + File.separator + "webshell_KeyIs_" + pass + File.separator + scriptMethod  + File.separator + webShell_Manager + "_" + enMothod_Str + "." + scriptMethod;
            fileName = fileName.replace("__", "_").replace("_.", ".");
            strUtils.createFile(webShellData, fileName);

            tipTitleAnt.setText("webShell已生成到当前目录下：./webshell_KeyIs_" + pass + "/");

        }catch (Exception e){
            e.printStackTrace();
            tipTitleAnt.setText("webShell生成失败，详细请查看命令窗口报错");
        }
        tipTitleAnt.setVisible(true);
    }

    @FXML
    public void cmdWebshellGeneration(ActionEvent event) {
        String pass = cmdKey.getText().isEmpty()? "potato" : cmdKey.getText();
        String webShell_Manager = "Cmd";

        if(cmdComboBox.getValue() == null){
            tipTitleCmd.setText("请选择脚本语言！");
            tipTitleCmd.setVisible(true);
            return;
        }
        if(cmdModeComboBox.getValue() == null){
            tipTitleCmd.setText("请选择编码方式！");
            tipTitleCmd.setVisible(true);
            return;
        }

        String scriptMethod = (String) cmdComboBox.getValue();
        String enMothod_Str = (String) cmdModeComboBox.getValue();

        try {

            String[] enMethod = enMothod_Str.split("_");
            String aesKey = strUtils.md5(pass).substring(0, 16);

            String webShellData = getWebShell(webShell_Manager, scriptMethod, enMethod, pass, aesKey);

            String fileName = "." + File.separator + "webshell_KeyIs_" + pass + File.separator + scriptMethod  + File.separator + webShell_Manager + "_" + enMothod_Str + "." + scriptMethod;
            fileName = fileName.replace("__", "_").replace("_.", ".");
            strUtils.createFile(webShellData, fileName);

            tipTitleCmd.setText("webShell已生成到当前目录下：./webshell_KeyIs_" + pass + "/");

        }catch (Exception e){
            e.printStackTrace();
            tipTitleCmd.setText("webShell生成失败，详细请查看命令窗口报错");
        }
        tipTitleCmd.setVisible(true);
    }
}
