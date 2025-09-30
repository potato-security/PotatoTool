package com.potato.potatotool.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.network.RequestObj;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.potato.potatotool.utils.core.Constants.getConfigInfo;
import static com.potato.potatotool.utils.network.RequestUtils.requests;

/**
 * @author Potato
 * @date 2023/4/13 11:22
 */
public class Update {

    /**
     * 测试调用，接入时请模拟传参
     */
    public static void main(String []args) throws IOException {

        // 文件名开头 以及 配置文件的二级key 需要一致
//        String argKey = "winKbInfo";
//        checkAndUpdateResource(argKey);


        String argKey_md5 = "md5";
        checkAndUpdateResource(argKey_md5);

    }

    // 文件名开头 以及 配置文件的二级key 需要一致
    public static void checkAndUpdateResource(String argKey){
        try {
            if(!argKey.equals("md5")) {
                String resourceUrl = getConfigInfo("resourceUrl");
                String downloadUrl = null;
                String fileName = null;

                RequestObj obj = new RequestObj();
                obj.setUrl(resourceUrl);
                try (CustomHttpResponse con = requests(obj)) {
                    JsonArray resJson = con.getJson().getAsJsonArray();

                    for (JsonElement element : resJson) {
                        JsonObject jsonObject = element.getAsJsonObject();
                        fileName = jsonObject.get("name").getAsString();

                        if (fileName.startsWith(argKey)) {
                            downloadUrl = jsonObject.get("download_url").getAsString();
                            break;
                        }
                    }
                }

                if (downloadUrl != null) {
                    boolean needUpdate = true;
                    try {
                        String topKey = "UpDate";
                        JsonObject tmpJsonObj_UpDate = (JsonObject) Constants.getOutsideConfig(topKey);
                        JsonObject tmpJsonObj_arg = tmpJsonObj_UpDate.getAsJsonObject(argKey);
                        String path = tmpJsonObj_arg.get("Path").getAsString();
                        String Date = tmpJsonObj_arg.get("Date").getAsString();
                        String newDate = extractDateFromFileName(fileName);
                        if (Date.equals(newDate) && Files.exists(Paths.get(path))) {
                            needUpdate = false;
                            System.out.println(fileName + "已经是最新的文件");
                        }
                    } catch (Exception e) {
                    }

                    if (needUpdate) downloadAndSaveResource(argKey, downloadUrl);

                } else {
                    throw new Exception("未找到以 " + argKey + " 开头的文件");
                }
            }else {
                String md5DownUrl = getConfigInfo("md5DownUrl");
                String TMP_FOLDER = ".PotatoTool";
                Path md5Path = Paths.get(System.getProperty("user.home"), TMP_FOLDER).resolve("md5_database.db");
                if(Files.exists(md5Path) && Files.size(md5Path) > (long) (1.66 * 1024 * 1024 * 1024)){
                    System.out.println("md5_database已经是最新的文件");
                }else {
                    downloadAndSaveResource(argKey, md5DownUrl);
                }
            }

        } catch (Exception e) {
            System.out.println("github访问失败，请检查尝试更换/关闭代理");
            e.printStackTrace();
        }
    }


    public static String checkResAndGetDownUrl(String argKey){
        String downUrl = null;
        try {
            if(!argKey.equals("md5")) {
                String resourceUrl = getConfigInfo("resourceUrl");
                String downloadUrl = null;
                String fileName = null;

                RequestObj obj = new RequestObj();
                obj.setUrl(resourceUrl);
                try (CustomHttpResponse con = requests(obj)) {
                    JsonArray resJson = con.getJson().getAsJsonArray();

                    for (JsonElement element : resJson) {
                        JsonObject jsonObject = element.getAsJsonObject();
                        fileName = jsonObject.get("name").getAsString();

                        if (fileName.startsWith(argKey)) {
                            downloadUrl = jsonObject.get("download_url").getAsString();
                            break;
                        }
                    }
                }

                if (downloadUrl != null) {
                    boolean needUpdate = true;
                    try {
                        String topKey = "UpDate";
                        JsonObject tmpJsonObj_UpDate = (JsonObject) Constants.getOutsideConfig(topKey);
                        JsonObject tmpJsonObj_arg = tmpJsonObj_UpDate.getAsJsonObject(argKey);
                        String path = tmpJsonObj_arg.get("Path").getAsString();
                        String Date = tmpJsonObj_arg.get("Date").getAsString();
                        String newDate = extractDateFromFileName(fileName);
                        if (Date.equals(newDate) && Files.exists(Paths.get(path))) {
                            needUpdate = false;
                            System.out.println(fileName + "已经是最新的文件");
                        }
                    } catch (Exception e) {}

                    if (needUpdate) downUrl = downloadUrl;

                } else {
                    throw new Exception("未找到以 " + argKey + " 开头的文件");
                }

            }else {
                String md5DownUrl = getConfigInfo("md5DownUrl");
                String TMP_FOLDER = ".PotatoTool";
                Path md5Path = Paths.get(System.getProperty("user.home"), TMP_FOLDER).resolve("md5_database.db");
                if(Files.exists(md5Path) && Files.size(md5Path) > (long) (1.66 * 1024 * 1024 * 1024)){
                    System.out.println("md5_database已经是最新的文件");
                }else {
                    downUrl = md5DownUrl;
                }
            }

        } catch (Exception e) {
            System.out.println("github访问失败，请检查尝试更换/关闭代理");
            downUrl = "[Error]github访问失败，请检查尝试更换/关闭代理";
            e.printStackTrace();
        }

        return downUrl;
    }


    // 写入本地目录
    public static String downloadAndSaveResource(String argKey, String urlPath) {

        String TMP_FOLDER = ".PotatoTool";
        Path configFolder = Paths.get(System.getProperty("user.home"), TMP_FOLDER);

        String savePathResult = "";

        try {
            Map<String ,String> headers = new HashMap<>();
            if(!argKey.equals("md5")){
                headers.put("Accept-Encoding","gzip");
            }

            RequestObj obj = new RequestObj();
            obj.setUrl(urlPath);
            obj.setHeaders(headers);
            obj.setFollowRedirects(true);
            try (CustomHttpResponse con = requests(obj)) {
                // 上传的md5也是gzip加工后的
                savePathResult = con.saveToFileByGzip(configFolder.toString(), false);
            }

            if (savePathResult != null){
                System.out.println("文件写入成功");
                if(argKey.equals("md5")){
                    Constants.saveConfig("Md5", savePathResult);
                }else {
                    updateLocalResourceConfig(argKey, savePathResult);
                }
            }else {
                System.out.println("文件写入失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return savePathResult;
    }

    public static String downloadAndSaveResource(String argKey, String urlPath, ProgressBar progressBar, Label progressLabel) {

        String TMP_FOLDER = ".PotatoTool";
        Path configFolder = Paths.get(System.getProperty("user.home"), TMP_FOLDER);

        String savePathResult = "";

        try {
            Map<String ,String> headers = new HashMap<>();
            if(!argKey.equals("md5")){
                headers.put("Accept-Encoding","gzip");
            }

            RequestObj obj = new RequestObj();
            obj.setUrl(urlPath);
            obj.setHeaders(headers);
            obj.setFollowRedirects(true);
            try (CustomHttpResponse con = requests(obj)) {
                // 上传的md5也是gzip加工后的
                savePathResult = con.saveToFileByGzip(configFolder.toString(), progressBar, progressLabel);
            }

            if (savePathResult != null){
                System.out.println("文件写入成功");
                if(argKey.equals("md5")){
                    Constants.saveConfig("Md5", savePathResult);
                }else {
                    updateLocalResourceConfig(argKey, savePathResult);
                }
            }else {
                System.out.println("文件写入失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return savePathResult;
    }


    // 更新本地配置文件  并删除原有旧索引文件
    public static void updateLocalResourceConfig(String argKey, String filePath){
        String topKey = "UpDate";
        JsonObject tmpJsonObj_UpDate;
        JsonObject tmpJsonObj_arg;
        try {
            tmpJsonObj_UpDate = (JsonObject) Constants.getOutsideConfig(topKey);
        } catch (Exception e){
            tmpJsonObj_UpDate = new JsonObject();
            tmpJsonObj_UpDate.add(argKey, new JsonObject());
        }
        try {
            tmpJsonObj_arg = tmpJsonObj_UpDate.getAsJsonObject(argKey);
        } catch (Exception e){
            tmpJsonObj_arg = new JsonObject();
            tmpJsonObj_arg.addProperty("Path", "");
            tmpJsonObj_arg.addProperty("Date", "");
        }


        String Path = tmpJsonObj_arg.get("Path").getAsString();

        if(!Path.isEmpty() && !filePath.equals(Path)) {
            try {
                Files.delete(Paths.get(Path));
                System.out.println("旧文件删除成功！");
            } catch (IOException e) {
                System.out.println("旧文件删除失败：" + Path);
                e.printStackTrace();
            }
        }


        Map<String, Object> configMap = new HashMap<>();
        Map<String, String> argMap = new HashMap<>();
        argMap.put("Path", filePath);
        argMap.put("Date", extractDateFromFileName(filePath));
        configMap.put(argKey, argMap);

        Constants.saveConfig(configMap, topKey);
    }


    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d{4,})");

    public static String extractDateFromFileName(String filePath) {
        // 从路径中提取文件名
        String fileName = Paths.get(filePath).getFileName().toString();

        // 使用正则表达式提取文件名中的连续数字（至少4位）
        Matcher matcher = NUMBER_PATTERN.matcher(fileName);
        if (matcher.find()) {
            return matcher.group(1); // 返回匹配的数字字符串
        }
        return null; // 如果没有找到匹配项，返回null
    }


}
