package com.potato.potatotool.controller.blueTeam;

import com.potato.potatotool.ToStart;
import com.potato.potatotool.content.blueTeam.IpInfo;
import com.potato.potatotool.utils.core.I18nUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fxmisc.richtext.*;
import org.lionsoul.ip2region.xdb.Searcher;
import org.fxmisc.flowless.VirtualizedScrollPane;

import static com.potato.potatotool.utils.data.StrUtils.joinList_r;

/**
 * @author Potato
 * @date 2023/10/7 16:46
 */
public class PaneGetIpInfo {
    private static final String PREVIEW_INPUT_WITH_IPS =
            "tcp        0      0 192.168.1.10:49732      8.8.8.8:53             ESTABLISHED\n" +
            "tcp        0      0 10.0.0.15:51514         1.1.1.1:443            ESTABLISHED\n" +
            "tcp        0      0 172.16.8.23:61200       114.114.114.114:53     ESTABLISHED\n" +
            "tcp        0      0 203.0.113.9:443         198.51.100.22:52344    TIME_WAIT";
    private static final String PREVIEW_INPUT_NO_IP =
            "Active Connections\n" +
            "Proto  Local Address          Foreign Address        State\n" +
            "tcp    localhost:app          remote:service         ESTABLISHED";

    @FXML
    private StackPane sPane;

    @FXML
    private TextArea inputText;

    @FXML
    private FlowPane flowPane;

    @FXML
    private VirtualizedScrollPane virScrollPane1;
    @FXML
    private VirtualizedScrollPane virScrollPane2;
    @FXML
    private VirtualizedScrollPane virScrollPane3;
    @FXML
    private VirtualizedScrollPane virScrollPane4;
    @FXML
    private VirtualizedScrollPane virScrollPane5;
    @FXML
    private VirtualizedScrollPane virScrollPane6;
    @FXML
    private VirtualizedScrollPane virScrollPane7;
    @FXML
    private VirtualizedScrollPane virScrollPane8;
    @FXML
    private CodeArea result1;
    @FXML
    private CodeArea result2;
    @FXML
    private CodeArea result3;
    @FXML
    private CodeArea result4;
    @FXML
    private CodeArea result5;
    @FXML
    private CodeArea result6;
    @FXML
    private CodeArea result7;
    @FXML
    private CodeArea result8;
    @FXML
    private Label ipLabel;
    @FXML
    private Label ipLabelSelect;
    @FXML
    private List<CodeArea> codeAreas = new ArrayList<>();
    @FXML
    private List<VirtualizedScrollPane> virScrollPanes = new ArrayList<>();
    @FXML
    private ComboBox rulesComboBox;
    @FXML
    private TextField customTextField;

    Searcher searcher;
    @FXML
    void initialize() throws IOException {

        searcher = IpInfo.init();

        codeAreas.addAll(Arrays.asList(result1, result2, result3, result4, result5, result6, result7, result8));
        virScrollPanes.addAll(Arrays.asList(virScrollPane1, virScrollPane2, virScrollPane3, virScrollPane4, virScrollPane5, virScrollPane6, virScrollPane7, virScrollPane8));

        //  协调ScrollPane>VirtualizedScrollPane>CodeArea嵌套时的滚动事件问题
        for (CodeArea codeArea : codeAreas) {
            codeArea.addEventFilter( ScrollEvent.ANY, scroll ->
            {
                if(!codeArea.isFocused()){
                    flowPane.fireEvent( scroll );
                    scroll.consume();
                }
            });
        }

        //  CodeArea添加宽高自适应
        sPane.widthProperty().addListener((obs, oldValue, newValue) -> {
            double prefWidth = newValue.doubleValue() * 0.5 - 35;
            for (VirtualizedScrollPane virScrollPane : virScrollPanes) {
                virScrollPane.setMinWidth(prefWidth);
                virScrollPane.setMaxWidth(prefWidth);
            }
        });
        sPane.heightProperty().addListener((obs, oldValue, newValue) -> {
            double prefHeight = newValue.doubleValue() * 0.4;
            for (VirtualizedScrollPane virScrollPane : virScrollPanes) {
                virScrollPane.setMinHeight(prefHeight);
            }
        });

        //  自定义筛选框添加事件
        customTextField.setOnKeyReleased(event -> {
            rulesComboChoose(null);
        });
        
        // 绑定国际化
        Platform.runLater(() -> {
            I18nUtils.bindComponents(sPane);
            applyStartupPreviewState();
        });
    }

    private void applyStartupPreviewState() {
        ToStart.StartupPage startupPage = ToStart.getStartupTestPage();
        if (startupPage == null) {
            return;
        }
        try {
            if (startupPage == ToStart.StartupPage.BLUE_GET_IP_INFO_EMPTY) {
                inputText.setText(PREVIEW_INPUT_NO_IP);
                getIpInfo(null);
                return;
            }
            if (startupPage == ToStart.StartupPage.BLUE_GET_IP_INFO_EXTRACT_RESULT
                    || startupPage == ToStart.StartupPage.BLUE_GET_IP_INFO_LOCATION_RESULT
                    || startupPage == ToStart.StartupPage.BLUE_GET_IP_INFO_RULES) {
                inputText.setText(PREVIEW_INPUT_WITH_IPS);
                getIpInfo(null);
            }
            if (startupPage == ToStart.StartupPage.BLUE_GET_IP_INFO_RULES) {
                rulesComboBox.getSelectionModel().select("国外IP");
                rulesComboChoose(null);
                Platform.runLater(() -> rulesComboBox.show());
            }
        } catch (Exception e) {
            if (ToStart.debugMode) {
                e.printStackTrace();
            }
        }
    }

    private LinkedHashMap<String, String> ipPosDict;
    @FXML
    void getIpInfo(ActionEvent event) throws Exception {

        String inputStr = inputText.getText();

        Set<String> ipList = (LinkedHashSet) IpInfo.getIpListFromReg(inputStr);
        ipPosDict = IpInfo.getIpPosDict(searcher, ipList);
        Set<String> posList = IpInfo.getSortedPosList(ipPosDict);

        result3.replaceText(inputStr);
        result4.replaceText(inputStr);
        result7.replaceText(inputStr);
        result8.replaceText(inputStr);

        ipLabel.textProperty().unbind();
        ipLabel.setText(I18nUtils.getString("ipinfo.extract.title"));
        ipLabelSelect.textProperty().unbind();
        ipLabelSelect.setText(I18nUtils.getString("ipinfo.filter.extract"));        

        if(ipList.size() < 1){
            result1.clear();
            result2.clear();
            result5.clear();
            result6.clear();
            return;
        }

        // 按照字母顺序排序posList
        ArrayList<String> newPosList = new ArrayList<>(posList);
        Collections.sort(newPosList);

        // 筛选栏
        rulesComboBox.getItems().clear();
        rulesComboBox.getItems().addAll("自定义筛选:头部包含","国内IP","国外IP","内网IP","外网IP");
        rulesComboBox.getItems().addAll(newPosList);
//        rulesComboBox.setValue(rulesComboBox.getItems().get(0));
        rulesComboBox.getSelectionModel().selectFirst();

        // result1 IP抽取
        result1.replaceText(joinList_r(ipList));
        // 使用 textProperty().unbind() 解除绑定后再设置
        ipLabel.textProperty().unbind();
        ipLabel.setText(I18nUtils.getString("ipinfo.extract.title") + "(" + ipList.size() + ")");
        ipLabelSelect.textProperty().unbind();
        ipLabelSelect.setText(I18nUtils.getString("ipinfo.filter.extract") + "(" + ipList.size() + ")");

        // result2 IP抽取+pos标记
        result2.clear();    //因为result2是append累加而非replaceText替换
        for(Map.Entry<String, String> entry : ipPosDict.entrySet()){
            String ip = entry.getKey();
            String pos = entry.getValue();
            result2.append(ip,"ipData");
            result2.append("[" + pos + "]\n","posData");
        }


        // result3 IP标记   result4 IP标记+pos标记
        Pattern pattern = Pattern.compile("\\b(" + String.join("|", ipList) + ")\\b");
        Matcher matcher = pattern.matcher(inputStr);
        int offset = 0; // 插入文本的偏移量
        while (matcher.find()) {
            int startIndex = matcher.start() + offset;
            int endIndex = matcher.end() + offset;

            result3.setStyleClass(matcher.start(), matcher.end(), "ipData");
            result4.setStyleClass(startIndex, endIndex, "ipData");

            // 获取匹配到的键对应的值
            String pos = "[" + ipPosDict.get(matcher.group()) + "]";

            result4.insert(endIndex, pos,"posData");

            offset += pos.length();
        }

    }

    //    筛选栏事件
    @FXML
    void rulesComboChoose(ActionEvent event){

        LinkedHashMap<String, String> newTmpipPosDict;

        String inputStr = inputText.getText();

        //  当前选中的下标
        int selectedIndex = rulesComboBox.getSelectionModel().getSelectedIndex();
        String selectedValue = (String) rulesComboBox.getSelectionModel().getSelectedItem();

        //  自定义输入框展示
        if (selectedIndex == 0) {
            customTextField.setVisible(true);
            customTextField.setManaged(true);
        } else {
            customTextField.setVisible(false);
            customTextField.setManaged(false);
        }
        if (selectedIndex == 0){
            //  自定义筛选:IP/Pos头部包含
            newTmpipPosDict = IpInfo.filterIpPos(ipPosDict, customTextField.getText().trim());
        }else{
            newTmpipPosDict = IpInfo.filterIpPos(ipPosDict, selectedValue);
        }
        // 使用 textProperty().unbind() 解除绑定后再设置
        ipLabelSelect.textProperty().unbind();
        ipLabelSelect.setText(I18nUtils.getString("ipinfo.filter.extract") + "(" + newTmpipPosDict.size() + ")");

        result7.replaceText(inputStr);
        result8.replaceText(inputStr);

        if(newTmpipPosDict.size() < 1){
            result6.clear();
            result5.clear();
            return;
        }

        // result5 IP标记
        result5.replaceText(joinList_r(newTmpipPosDict.keySet()));

        // result6 IP标记+pos标记
        result6.clear();    //因为result6是append累加而非replaceText替换
        for(Map.Entry<String, String> entry : newTmpipPosDict.entrySet()){
            String ip = entry.getKey();
            String pos = entry.getValue();
            result6.append(ip,"ipData");
            result6.append("[" + pos + "]\n","posData");
        }

        Pattern pattern = Pattern.compile("\\b(" + String.join("|", newTmpipPosDict.keySet()) + ")\\b");
        Matcher matcher = pattern.matcher(inputStr);
        int offset = 0; // 插入文本的偏移量
        while (matcher.find()) {
            int startIndex = matcher.start() + offset;
            int endIndex = matcher.end() + offset;

            result7.setStyleClass(matcher.start(), matcher.end(), "ipData");
            result8.setStyleClass(startIndex, endIndex, "ipData");


            // 获取匹配到的键对应的值
            String pos = "[" + newTmpipPosDict.get(matcher.group()) + "]";

            result8.insert(endIndex, pos,"posData");

            offset += pos.length();
        }


    }


}
