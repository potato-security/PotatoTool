package com.potato.potatotool.content.redTeam;

import org.json.JSONObject;
import com.potato.potatotool.utils.data.JsonUtils;

import java.io.InputStream;

import static com.potato.potatotool.utils.core.Constants.getResourceStream;

/**
 * @author Potato
 * @date 2023/4/11 11:49
 */
public class TaskListCheck {


    /**
     * 测试调用，接入时请模拟传参
     * @inputStr    用户传入进程列表 (String)
     */
    public static void main(String []args) {

        JSONObject taskListJsonObject = init();  //初始化调用一次就行! !!!不要每次搜索都调用!!!
        JSONObject avJsonOjb = (JSONObject) taskListJsonObject.get("avList");
        JSONObject tqJsonOjb = (JSONObject) taskListJsonObject.get("processList");

        String inputStr = "tc.exe\nwkufind.exe wrctrl.exe swsbgate.exe /usr/local/aegis/aegis_update/AliYunDunUpdate pCloud.exe wupdt.exesdwupdt.exe";


        JSONObject antivirusProcesses = getAntivirusProcesses(avJsonOjb, inputStr);
        System.out.println("存在的杀软进程：");
        System.out.println( JsonUtils.formatOneDimensionJsonToStr(antivirusProcesses, 20, "center") );

        JSONObject canRootProcesses = getCanRootProcesses(tqJsonOjb, inputStr);
        System.out.println("存在可能提权的进程：");
        System.out.println( JsonUtils.formatOneDimensionJsonToStr(canRootProcesses, 20, "center") );

    }

    /**
     * 初始化命令集
     * !软件初始化调用一次就行!
     * !!!不要每次搜索都调用!!!
     * @return  json数据
     */
    public static JSONObject init() {

        try (InputStream taskListStream = getResourceStream("taskList")) {
            return JsonUtils.readJsonFile(taskListStream);
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

        return JsonUtils.iterativeJsonFindMatches(jsonObject, inputStr);

    }


    /**
     * @getCanRootProcesses   获取提取进程参考信息
     * @param jsonObject      原始jsonObj对象
     * @param inputStr        传入检索内容
     * @return                返回匹配成功的信息，拼接String
     */
    public static JSONObject getCanRootProcesses(JSONObject jsonObject, String inputStr) {

        return JsonUtils.iterativeJsonFindMatches(jsonObject, inputStr);

    }


}
