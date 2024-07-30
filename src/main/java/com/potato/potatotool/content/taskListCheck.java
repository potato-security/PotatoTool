package com.potato.potatotool.content;

import org.json.JSONObject;
import com.potato.potatotool.utils.jsonUtils;

import java.io.InputStream;

import static com.potato.potatotool.utils.Constants.getResourceStream;

/**
 * @author Potato
 * @date 2023/4/11 11:49
 */
public class taskListCheck {


    /**
     * 测试调用，接入时请模拟传参
     * @inputStr    用户传入进程列表 (String)
     */
    public static void main(String []args) {

        JSONObject taskListJsonObject = init();  //初始化调用一次就行! !!!不要每次搜索都调用!!!
        JSONObject avJsonOjb = (JSONObject) taskListJsonObject.get("avList");
        JSONObject tqJsonOjb = (JSONObject) taskListJsonObject.get("tqList");

        String inputStr = "tc.exe\nwkufind.exe wrctrl.exe swsbgate.exe pCloud.exe wupdt.exesdwupdt.exe";


        JSONObject antivirusProcesses = getAntivirusProcesses(avJsonOjb, inputStr);
        System.out.println("存在的杀软进程：");
        System.out.println( jsonUtils.formatOneDimensionJsonToStr(antivirusProcesses, 20, "center") );

        JSONObject canRootProcesses = getCanRootProcesses(tqJsonOjb, inputStr);
        System.out.println("存在可能提权的进程：");
        System.out.println( jsonUtils.formatOneDimensionJsonToStr(canRootProcesses, 20, "center") );

    }

    /**
     * 初始化命令集
     * !软件初始化调用一次就行!
     * !!!不要每次搜索都调用!!!
     * @return  json数据
     */
    public static JSONObject init() {

        try (InputStream taskListStream = getResourceStream("taskList")) {
            return jsonUtils.readJsonFile(taskListStream);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }


    /**
     * @getAntivirusProcesses 获取杀软进程信息
     * @param jsonObject      原始jsonObj对象
     * @param inputStr        传入检索内容
     * @return                返回匹配成功的信息，拼接String
     */
    public static JSONObject getAntivirusProcesses(JSONObject jsonObject, String inputStr) {

        return jsonUtils.iterativeJsonFindMatches(jsonObject, inputStr);

    }


    /**
     * @getCanRootProcesses   获取提取进程参考信息
     * @param jsonObject      原始jsonObj对象
     * @param inputStr        传入检索内容
     * @return                返回匹配成功的信息，拼接String
     */
    public static JSONObject getCanRootProcesses(JSONObject jsonObject, String inputStr) {

        return jsonUtils.iterativeJsonFindMatches(jsonObject, inputStr);

    }


}
