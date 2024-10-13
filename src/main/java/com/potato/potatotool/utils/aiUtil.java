package com.potato.potatotool.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.fxmisc.richtext.CodeArea;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.potato.potatotool.utils.Constants.getConfigInfo;
import static com.potato.potatotool.utils.requestUtils.requests;


/**
 * @author Potato
 * @date 2023/5/6 16:52
 */

// V2.0 优化版，支持历史会话
public class aiUtil {

    private JsonArray historyList = new JsonArray();

    public boolean isFirstResponse = true;

    public void askAi(String question, Object node){

        //  初始化默认GPT配置
        JsonObject tmpJsonObj_AI = (JsonObject) Constants.getOutsideConfig("AI");
        String GPT_Model = tmpJsonObj_AI.getAsJsonPrimitive("GPT_Model").getAsString();
        String GPT_API_Key = tmpJsonObj_AI.getAsJsonPrimitive("GPT_API_Key").getAsString();

        RequestObj obj;
        if (!GPT_API_Key.equals("")){
            String aiUrl = "https://api.openai.com/v1/chat/completions";

            HashMap<String, String> headers = new HashMap();
            headers.put("Content-Type", "application/json");
            headers.put("Authorization", "Bearer " + GPT_API_Key);

            JsonObject data = new JsonObject();
            data.addProperty("model", GPT_Model);

            JsonArray messages = new JsonArray();
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", "你是个乐于助人的助手。");
            messages.add(systemMessage);

            // 添加历史对话
            for (int i = 0; i < historyList.size(); i++) {
                messages.add(historyList.get(i).getAsJsonObject());
            }

            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", question);
            messages.add(userMessage);

            data.add("messages", messages);
            data.addProperty("stream", true);

            String jsonData = new Gson().toJson(data);

            obj = new RequestObj().setMethod("POST").setUrl(aiUrl).setHeaders(headers).setPostData(jsonData);

        }else {

            String aiUrl = getConfigInfo("aiUrl");

            HashMap<String, String> headers = new HashMap();
            headers.put("Content-Type", "application/json");

            JsonObject jsonData = new JsonObject();
            jsonData.addProperty("query", question);
            jsonData.add("history", historyList);

            obj = new RequestObj().setMethod("POST").setUrl(aiUrl).setHeaders(headers).setPostData(jsonData);

        }

        try {

            CustomHttpResponse con = requests(obj);
            con.getSSEStreamingJson(new CustomHttpResponse.ResponseCallback() {//回调函数
                @Override
                public void onResponse(String line) {
                    // 处理每次响应的line
                    try {
                        String res = "";
                        if(line != null && !line.equals("")) {
                            if (!GPT_API_Key.equals("")) {
                                if (line.startsWith("data: [DONE]")) {
                                    res = "";
                                } else {
                                    JsonObject responseJson = JsonParser.parseString(line.replaceAll("^data: ", "")).getAsJsonObject();

                                    String errorMsg = responseJson.has("error") ? responseJson.getAsJsonObject("error").get("message").getAsString() : null;

                                    if (errorMsg != null) {
                                        res = errorMsg;
                                    } else {
                                        String idValue = responseJson.get("id").getAsString();
                                        JsonArray choicesArray = responseJson.getAsJsonArray("choices");
                                        JsonObject firstChoice = choicesArray.get(0).getAsJsonObject();

                                        String contentValue = firstChoice.has("delta") && firstChoice.getAsJsonObject("delta").has("content") ? firstChoice.getAsJsonObject("delta").get("content").getAsString() : null;
                                        Boolean finishReasonValue = firstChoice.get("finish_reason").isJsonNull() ? false : true;

                                        if (finishReasonValue) {
                                            addHistory("user", question);
                                            addHistory("assistant", contentValue);
                                            res = "";
                                        } else {
                                            //System.out.println("Received ResponseJson: " + reponseJson);
                                            res = contentValue;
                                        }
                                    }
                                }

                            } else {
                                if (line.equals("[[Premature EOF]]")) {
                                    res = "【AI服务器显存炸裂了~ 买不起~ 传输小点儿的东西吧】";
                                } else if (line.equals("[[Response code 502]]")) {
                                    res = "【内部免费AI服务器可能已关停/您当前处于国外IP环境，请在设置中自行配置AI模型及对应Key】";
                                }else if (line.equals("[[Read timed out]]")) {
                                    res = "【您提交的消息太长/内部免费AI服务器可能已关停/您当前处于国外IP环境，请在设置中自行配置AI模型及对应Key】";
                                }else if (line.startsWith("data: ")){
                                    JsonObject responseJson = JsonParser.parseString(line.replaceAll("^data: ", "")).getAsJsonObject();

                                    if (responseJson.get("finished").getAsBoolean()) {
                                        addHistory("user", question);
                                        addHistory("assistant", responseJson.get("delta").getAsString());
                                        res = "";
                                    } else {
//                                        System.out.println("Received ResponseJson: " + responseJson);
                                        res = responseJson.get("delta").getAsString();
                                    }
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
                            } else if(node instanceof CodeArea) {
                                CodeArea textArea = (CodeArea) node;
                                String finalRes = res;
                                Platform.runLater(() -> {
                                    if (isFirstResponse) {
                                        textArea.clear();
                                        isFirstResponse = false;
                                    }
                                    if (finalRes.equals("【AI服务器显存炸裂了~ 买不起~ 传输小点儿的东西吧】")) {
                                        textArea.append(finalRes, "-fx-fill: red;");
                                    } else {
                                        textArea.appendText(finalRes);
                                    }
                                });
                            } else if(node instanceof Label) {
                                Label textArea = (Label) node;
                                String finalRes = res;
                                Platform.runLater(() -> {
                                    textArea.setText(textArea.getText() + finalRes);
                                });
                            } else {
                                System.out.print(res);
                            }
                        }

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            if (e.toString().contains("Connect timed out")) {
                String res = "【GPT访问连接超时，请检查您的网络或代理】";
                if (node instanceof TextArea) {
                    TextArea textArea = (TextArea) node;
                    Platform.runLater(() -> {
                        if (isFirstResponse) {
                            textArea.clear();
                            isFirstResponse = false;
                        }
                        textArea.appendText(res);
                    });
                } else if(node instanceof CodeArea) {
                    CodeArea textArea = (CodeArea) node;
                    Platform.runLater(() -> {
                        if (isFirstResponse) {
                            textArea.clear();
                            isFirstResponse = false;
                        }
                        textArea.appendText(res);
                    });
                } else if(node instanceof Label) {
                    Label textArea = (Label) node;
                    Platform.runLater(() -> {
                        textArea.setText(textArea.getText() + res);
                    });
                } else {
                    System.out.println(res);
                }
            }
        }

    }

    String result = "";
    public String askAi_NoStream(String question){

        //  初始化默认GPT配置
        JsonObject tmpJsonObj_AI = (JsonObject) Constants.getOutsideConfig("AI");
        String GPT_Model = tmpJsonObj_AI.getAsJsonPrimitive("GPT_Model").getAsString();
        String GPT_API_Key = tmpJsonObj_AI.getAsJsonPrimitive("GPT_API_Key").getAsString();

        RequestObj obj;
        if (!GPT_API_Key.equals("")){
            String aiUrl = "https://api.openai.com/v1/chat/completions";

            HashMap<String, String> headers = new HashMap();
            headers.put("Content-Type", "application/json");
            headers.put("Authorization", "Bearer " + GPT_API_Key);

            JsonObject data = new JsonObject();
            data.addProperty("model", GPT_Model);

            JsonArray messages = new JsonArray();
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", "你是个乐于助人的助手。");
            messages.add(systemMessage);

            // 添加历史对话
            for (int i = 0; i < historyList.size(); i++) {
                messages.add(historyList.get(i).getAsJsonObject());
            }

            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", question);
            messages.add(userMessage);

            data.add("messages", messages);
            data.addProperty("stream", true);

            String jsonData = new Gson().toJson(data);

            obj = new RequestObj().setMethod("POST").setUrl(aiUrl).setHeaders(headers).setPostData(jsonData);

        }else {

            String aiUrl = getConfigInfo("aiUrl");

            HashMap<String, String> headers = new HashMap();
            headers.put("Content-Type", "application/json");

            JsonObject jsonData = new JsonObject();
            jsonData.addProperty("query", question);
            jsonData.add("history", historyList);

            obj = new RequestObj().setMethod("POST").setUrl(aiUrl).setHeaders(headers).setPostData(jsonData);

        }

        try {

            CustomHttpResponse con = requests(obj);
            con.getSSEStreamingJson(new CustomHttpResponse.ResponseCallback() {//回调函数
                @Override
                public void onResponse(String line) {
                    // 处理每次响应的line
                    try {
                        String res = "";
                        if(line != null && !line.equals("")) {
                            if (!GPT_API_Key.equals("")) {
                                if (line.startsWith("data: [DONE]")) {
                                    res = "";
                                } else {
                                    JsonObject responseJson = JsonParser.parseString(line.replaceAll("^data: ", "")).getAsJsonObject();

                                    String errorMsg = responseJson.has("error") ? responseJson.getAsJsonObject("error").get("message").getAsString() : null;

                                    if (errorMsg != null) {
                                        res = errorMsg;
                                    } else {
                                        String idValue = responseJson.get("id").getAsString();
                                        JsonArray choicesArray = responseJson.getAsJsonArray("choices");
                                        JsonObject firstChoice = choicesArray.get(0).getAsJsonObject();

                                        String contentValue = firstChoice.has("delta") && firstChoice.getAsJsonObject("delta").has("content") ? firstChoice.getAsJsonObject("delta").get("content").getAsString() : null;
                                        Boolean finishReasonValue = firstChoice.get("finish_reason").isJsonNull() ? false : true;

                                        if (finishReasonValue) {
                                            addHistory("user", question);
                                            addHistory("assistant", contentValue);
                                            res = "";
                                        } else {
                                            //System.out.println("Received ResponseJson: " + reponseJson);
                                            res = contentValue;
                                        }
                                    }
                                }

                            } else {
                                if (line.equals("[[Premature EOF]]")) {
                                    res = "【AI服务器显存炸裂了~ 买不起~ 传输小点儿的东西吧】";
                                } else if (line.equals("[[Response code 502]]")) {
                                    res = "【内部免费AI服务器可能已关停/您当前处于国外IP环境，请在设置中自行配置AI模型及对应Key】";
                                }else if (line.equals("[[Read timed out]]")) {
                                    res = "【您提交的消息太长/内部免费AI服务器可能已关停/您当前处于国外IP环境，请在设置中自行配置AI模型及对应Key】";
                                }else if (line.startsWith("data: ")){
                                    JsonObject responseJson = JsonParser.parseString(line.replaceAll("^data: ", "")).getAsJsonObject();

                                    if (responseJson.get("finished").getAsBoolean()) {
                                        addHistory("user", question);
                                        addHistory("assistant", responseJson.get("delta").getAsString());
                                        res = "";
                                    } else {
//                                        System.out.println("Received ResponseJson: " + responseJson);
                                        res = responseJson.get("delta").getAsString();
                                    }
                                }

                            }
                            result = result+res;
                        }

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            if (e.toString().contains("Connect timed out")) {
                result = "【GPT访问连接超时，请检查您的网络或代理】";
            }
        }

        return result;

    }

    public Set<String> get_re_result(String aiJudgement){
        Set<String> result = new LinkedHashSet<>();
        String[] patterns = { "\\[\\[\\[([^\\[\\]]*)\\]\\]\\]", "\\[\\[([^\\[\\]]*)\\]\\]", "\\[([^\\[\\]]*)\\]" };

        for (String patternString : patterns) {
            Pattern pattern = Pattern.compile(patternString);
            Matcher matcher = pattern.matcher(aiJudgement);

            while (matcher.find()) {
                result.add(matcher.group(1));
            }
        }

        return result;
    }

    // 添加历史记录的方法
    private void addHistory(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        historyList.add(message);

        if (historyList.size() > 10) {
            historyList.remove(0);
        }
    }

    public void clearHistory() {
        historyList = new JsonArray();
    }
}
