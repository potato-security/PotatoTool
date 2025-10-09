package com.potato.potatotool.utils.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.ConfigConstants;
import com.potato.potatotool.utils.core.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;


/**
 * @author Potato
 * @date 2023/4/10 16:16
 */
public class JsonUtils {

    /**
     * Tip：支持顺序的JSONObject类
      */
    public static class OrderedJSONObject extends JSONObject {

        public OrderedJSONObject(String postData) {
            super(postData);
        }

        public OrderedJSONObject() {
            super();
        }

        @Override
        public JSONObject put(String key, Object value) throws JSONException {
            try {
                Field map = JSONObject.class.getDeclaredField("map");
                map.setAccessible(true);
                Object mapValue = map.get(this);
                if (!(mapValue instanceof LinkedHashMap)) {
                    map.set(this, new LinkedHashMap<>());
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            return super.put(key, value);
        }

    }


    /**
     * @param path  json文件路径
     * @return      返回jsonObject对象
     */
    public static JSONObject readJsonFile(String path) {

        JSONObject jsonObject = null;

        try {

            BufferedReader reader = new BufferedReader(new FileReader(path));
            String json = "";
            String line;

            while ((line = reader.readLine()) != null) {
                json += line;
            }

            reader.close();

            jsonObject = new JSONObject(json);

        } catch (Exception e) {

            e.printStackTrace();

        }

        return jsonObject;

    }


    /**
     * @param inputStream   json文件Stream数据
     * @return              返回jsonObject对象
     */
    public static JSONObject readJsonFile(InputStream inputStream) {

        JSONObject jsonObject = null;
        StringBuilder json = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            jsonObject = new JSONObject(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jsonObject;

    }


    /**
     * @param tmpJsonOjb 原始jsonObj对象
     * @param args       [0]：lastIndexStr上一次遍历序号 [1]: level当前遍历层级
     * @return           将jsonObj对象格式化，返回带序号的字符串
     */
    public static String formatJsonToStr(JSONObject tmpJsonOjb, Object... args) {
        String result = "";
        String tabStr = "";
        int indexNum = 0;

        // 初始化省略参数
        String lastIndexStr = "";
        int level = 1;
        if (args.length > 0) {
            lastIndexStr = (String) args[0];
        }
        if (args.length > 1) {
            level = (int) args[1];
        }

        for (int i = 0; i < (level-1); i++){
            tabStr += "\t";
        }

        for(String key : tmpJsonOjb.keySet()) {

            indexNum += 1;
            String tmplastIndexStr = "";

            if(level != 1){
                tmplastIndexStr = lastIndexStr + "." + indexNum ;
            }else {
                tmplastIndexStr = indexNum +".";
            }

            tmplastIndexStr = tmplastIndexStr.replace("..",".");

            result += tabStr + tmplastIndexStr + " " + key + "\n";

            Object value = tmpJsonOjb.get(key);

            if (value instanceof JSONObject){

                result += formatJsonToStr((JSONObject) value, tmplastIndexStr, level+1);

            }
            else if (value instanceof JSONArray) {

                JSONArray jsonArray = (JSONArray) value;

                // 根节点带序号-----此功能已关闭
                //int nextIndex = 0;
                //for (int i = 0; i < jsonArray.length(); i++){
                //    nextIndex += 1;
                //    result += (tabStr + "\t" + tmplastIndexStr + "." + nextIndex + " " + jsonArray.get(i)).replace("..", ".") + "\n";
                //}

                for (int i = 0; i < jsonArray.length(); i++){

                    result += tabStr + "\t" + jsonArray.get(i) + "\n";

                }

            }
            else if (value instanceof String) {

                // 根节点带序号-----此功能已关闭
                //result += (tabStr + "\t" + tmplastIndexStr + ".1 " + value +"\n").replace("..", ".");

                result += tabStr + "\t" + value +"\n";

            }

        }

        return result;

    }


    /**
     * --------------仅限一维JsonObj使用--------------
     * @param jsonObject    原始jsonObject对象
     * @param args          可变参数 args[0]->keyLen 输出的Key长度,不传参不限制,位数不够用空格补全 args[1]->type Key居中、左、右
     * @return              格式化jsonObject对象以字符串输出
     */
    public static String formatOneDimensionJsonToStr(JSONObject jsonObject, Object... args){

        String result = "";
        int keyLen = 0;
        String type = "center";

        if (args.length > 0){

            keyLen = (int) args[0];

        }
        if (args.length > 1){

            type = (String) args[1];

        }

        for (String key : jsonObject.keySet()){

            result += StrUtils.formatStr(key, keyLen, type) + ": " + jsonObject.get(key) + "\n";

        }

        return  result;

    }


    /**
     * 遍历原始jsonObject的key，匹配字符串inputStr中是否出现key，符合条件的jsonObject构成新
     * @param jsonObject      原始jsonObj对象
     * @param inputStr        传入检索内容
     * @return                inputStr中出现过的jsonObject的Key，构成新jsonObjec
     */
    public static JSONObject iterativeJsonFindMatches(JSONObject jsonObject, String inputStr) {

        JSONObject resultJsonObj = new JSONObject();

        for (String key : jsonObject.keySet()) {

            Pattern regular = Pattern.compile( "(?<!\\S)" + key.replace(".","\\.") );

            if ( regular.matcher(inputStr).find() ){

                resultJsonObj.put(key, jsonObject.get(key));

            }

        }

        return resultJsonObj;

    }


    public static String jsonArrayToString(JsonArray jsonArray, String delimiter) {
        if (jsonArray == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (JsonElement element : jsonArray) {
            result.append(element.toString());
            if (element!= jsonArray.get(jsonArray.size() - 1)) {
                result.append(delimiter);
            }
        }
        return result.toString();
    }


    public static ArrayList jsonArrayToArrayList(JSONArray jsonArray,Boolean... needUnicode){

        ArrayList arrayList = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            Object element = jsonArray.get(i);
            if (element instanceof JSONArray) {
                // 如果元素是 JSONArray，则递归处理
                arrayList.add( jsonArrayToArrayList((JSONArray) element, needUnicode));
            } else if (element instanceof String) {
                // 如果元素是 String，则添加到 ArrayList 中

                if(needUnicode.length==1){
                    if(needUnicode[0]){
                        arrayList.add(StrUtils.toUnicode((String) element));
                    }
                }else {
                    arrayList.add((String) element);
                }

            }
        }
        return arrayList;
    }

    public static boolean containsString(JsonArray jsonArray, String target) {
        for (JsonElement element : jsonArray) {
            if (element.getAsString().equals(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查指定服务是否启用代理
     * 从 Proxy.services.{serviceName} 读取配置
     * 
     * @param serviceName 服务名称（如 ConfigConstants.AI, ConfigConstants.FOFA_KEY 等）
     * @return 是否启用代理
     */
    public static boolean isProxyEnabled(String serviceName) {
        try {
            JsonObject proxyConfig = (JsonObject) Constants.getOutsideConfig(ConfigConstants.PROXY);
            
            if (proxyConfig.has(ConfigConstants.PROXY_SERVICES) && 
                proxyConfig.get(ConfigConstants.PROXY_SERVICES).isJsonObject()) {
                JsonObject services = proxyConfig.getAsJsonObject(ConfigConstants.PROXY_SERVICES);
                if (services != null && services.has(serviceName)) {
                    return services.get(serviceName).getAsBoolean();
                }
            }
            // 默认不启用代理
            return false;
        } catch (Exception e) {
            // 如果读取配置出错，默认不启用代理
            return false;
        }
    }

}
