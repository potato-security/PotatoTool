package com.potato.potatotool.utils;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.fxmisc.richtext.CodeArea;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

import static com.potato.potatotool.utils.Constants.getConfigInfo;
import static com.potato.potatotool.utils.requestUtils.requests;


/**
 * @author Potato
 * @date 2023/5/6 16:52
 */

// V1.0 第一版，不需要传session
public class aiUtil {

    public JSONArray historyList = new JSONArray();

    public boolean isFirstResponse = true;

    public void askAi(String question, Object node){

        String aiUrl = getConfigInfo("aiUrl");

        HashMap<String, String> headers = new HashMap();
        headers.put("Content-Type", "application/json");

        RequestObj obj = new RequestObj();
        obj.setMethod("POST");
        obj.setUrl(aiUrl);
        obj.setHeaders(headers);
//        obj.setProxies("127.0.0.1:8080");
        obj.setTimeOut(500);

        JSONObject jsonData = new JSONObject();
        jsonData.put("query", question);
        jsonData.put("history", historyList);
        obj.setPostData(jsonData);

        try {

            CustomHttpResponse con = requests(obj);
            con.getSSEStreamingJson(new CustomHttpResponse.ResponseCallback() {//回调函数
                @Override
                public void onResponse(String line) {
                    // 处理每次响应的line
                    try {

                        String res = "";

                        if(line.equals("[[Premature EOF]]")){
                            res = "【AI服务器显存炸裂了~ 买不起~ 传输小点儿的东西吧】";
                        }else if(line.equals("[[Response code 502]]")){
                            res = "【内部免费AI服务器可能已关停，请在设置中自行配置AI模型及对应Key】";
                        }else {
                            JSONObject reponseJson = new JSONObject(line.replaceAll("^data: ",""));

                            if( (boolean)reponseJson.get("finished") == true){
                                JSONArray historyJSONArray = reponseJson.getJSONArray("history");
                                historyList = historyJSONArray;
                                res = "\n\n";
                                //System.out.println("historyList:"+historyList);
                            }else {
                                //System.out.println("Received ResponseJson: " + reponseJson);
                                res = (String) reponseJson.get("delta");
                            }
                        }

                        if (node instanceof TextArea) {
                            TextArea textArea = (TextArea) node;
                            String finalRes = res;
                            Platform.runLater(() -> {
                                if (isFirstResponse) {
                                    textArea.clear();
                                    isFirstResponse = false;  // 将标志设置为 false，表示已经获取过响应
                                }
                                textArea.appendText(finalRes);

                            });
                        }else{
                            CodeArea textArea = (CodeArea) node;
                            String finalRes = res;
                            Platform.runLater(() -> {
                                if (isFirstResponse) {
                                    textArea.clear();
                                    isFirstResponse = false;
                                }
                                if (finalRes.equals("【AI服务器显存炸裂了~ 买不起~ 传输小点儿的东西吧】")){
                                    textArea.append(finalRes,"-fx-fill: red;");
                                }else {
                                    textArea.appendText(finalRes);
                                }
                            });
                        }
                        System.out.print(res);

                    }catch (Exception e){ }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
