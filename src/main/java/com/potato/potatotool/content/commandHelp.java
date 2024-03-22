package com.potato.potatotool.content;

import org.json.JSONArray;
import org.json.JSONObject;
import com.potato.potatotool.utils.jsonUtils;


import java.io.InputStream;

import static com.potato.potatotool.utils.Constants.getResourceStream;

/**
 * @author Potato
 * @date 2023/4/10 14:06
 */
public class commandHelp {


    /**
     * 测试调用，接入时请模拟传参
     * @inputKey    用户传入检索词汇
     * @isRegular   是否检索正则语法
     */
    public static void main(String []args) {

        JSONObject commandHelpJsonObject = init();  //TODO !软件初始化调用一次就行! !!!不要每次搜索都调用!!!

        String inputKey = "Window";

        Boolean isRegular =Boolean.FALSE;

        JSONObject tmpJsonOjb = getCommand(commandHelpJsonObject, inputKey, isRegular);

        //输出方式一：存在json符号标识
        //String result = tmpJsonOjb.toString(4);

        //输出方式二：不存在json符号标识
        //String result = tmpJsonOjb.toString(4).replaceAll("\\{|\\}|\\[|\\]|\"|,", "");

        //输出方式三：不存在json符号标识，标识序列号
        String result = jsonUtils.formatJsonToStr(tmpJsonOjb);

        System.out.println(result);

    }

    /**
     * 初始化命令集
     * !软件初始化调用一次就行!
     * !!!不要每次搜索都调用!!!
     * @return  json数据
     */
    public static JSONObject init() {

        InputStream commandHelpStream = getResourceStream("commandHelp");

        JSONObject jsonObject = jsonUtils.readJsonFile(commandHelpStream);

        return jsonObject;
    }


    /**
     * @param jsonObject    原始jsonObj对象
     * @param inputKey      传入检索内容
     * @param isRegular     传入检索内容是否为正则语法
     * @return              返回匹配成功的obj，组合成新jsonObj
     */
    public static JSONObject getCommand(JSONObject jsonObject, String inputKey, Boolean isRegular) {

        String regular = ".*" + inputKey + ".*";

        if (isRegular){
            regular = inputKey;
        }

        JSONObject newJsonObj = new JSONObject();

        for (String key : jsonObject.keySet()) {

            Object value = jsonObject.get(key);


            if (key.matches(regular)) {

                newJsonObj.put( key, jsonObject.get(key) );

                continue;

            }

            if (value instanceof JSONObject) {

                JSONObject tmpJsonObj = getCommand((JSONObject) value, inputKey, isRegular);

                for (String tmpKey : tmpJsonObj.keySet()){

                    newJsonObj.put( tmpKey, tmpJsonObj.get(tmpKey) );

                }

            }
            else if (value instanceof JSONArray) {

                JSONArray jsonArray = (JSONArray) value;

                for (int i = 0; i < jsonArray.length(); i++){

                    Object arrayValue = jsonArray.get(i);

                    if ( ((String)arrayValue).matches(".*" + inputKey + ".*") ) {

                        newJsonObj.put( key, jsonObject.get(key) );

                    }

                }

            }

        }

        return newJsonObj;

    }



}
