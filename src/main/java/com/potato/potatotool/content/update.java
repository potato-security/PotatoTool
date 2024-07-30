package com.potato.potatotool.content;

import com.potato.potatotool.utils.RequestObj;
import com.potato.potatotool.utils.unZipUtils;
import com.potato.potatotool.utils.CustomHttpResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import static com.potato.potatotool.utils.requestUtils.requests;

/**
 * @author Potato
 * @date 2023/4/13 11:22
 */
public class update {

    /**
     * 测试调用，接入时请模拟传参
     */
    public static void main(String []args) throws IOException {

//        List<Map<String, String>> cves = init();
//        updateResource( "https://raw.githubusercontent.com/HotBoy-java/Resource/main/winKbInfo20230410.csv","./src/content/conf/winKbInfo20230411.csv");
        updateResource( "https://codeload.github.com/HotBoy-java/Resource/zip/refs/heads/main","./src/content/conf/main.zip");
        List<String> filePathFromZip = unZipUtils.unZip(Paths.get(".", "src", "content", "conf", "main.zip").toString(), "./src/content/conf/", "Resource-main");
        deleteOldResources(filePathFromZip, "winKbInfo");

    }


    public static void updateResource(String urlPath, String savePath) {

        try {

            RequestObj obj = new RequestObj();
            obj.setUrl(urlPath);

            CustomHttpResponse con = requests(obj);

            System.out.println(con.getResponseCode());
            System.out.println(con.getHeaderFields());
//            System.out.println(con.getTextStr());
            System.out.println(con.getContentEncoding());
            System.out.println(con.getContentLength());
            System.out.println(con.getContentType());
            System.out.println(con.getURL());
//            System.out.println(con.getJson());
            String savePathResult = con.saveToFile(savePath,false);

            if (savePathResult != null){
                System.out.println("文件写入成功");
            }else {
                System.out.println("文件写入失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }




    /**
     * 用于删除带有时间戳的老旧资源，Such as :winKbInfo20230410.csv
     * @param filePathList  新资源所有路径
     * @param feature       新老资源的共同特征
     */
    public static void deleteOldResources(List<String> filePathList, String feature) {

        for (String path : filePathList) {
            if (path.matches(".*" + feature + ".*")) {
                File[] peerDirFile = new File(path).getParentFile().listFiles();
                for (File f : peerDirFile){
                    if ( f.isFile() && f.getPath().matches(".*" + feature + ".*") && !f.getPath().equals(new File(path).getPath()) ){
                        System.out.println(f.getPath());
                        System.out.println(new File(path).getPath());
                        f.delete();
                    }
                }
            }
        }

    }


    /**
     *  更新config.properties文件内容
     * @param key       更新指定的键
     * @param newValue  更新指定的新值
     */
    public static void updateConfigProperties(String key, String newValue){

        String configPath = "config.properties";

        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(configPath)) {
            props.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }

        props.setProperty(key, newValue);

        try (FileOutputStream out = new FileOutputStream(configPath)) {
            props.store(out, null);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
