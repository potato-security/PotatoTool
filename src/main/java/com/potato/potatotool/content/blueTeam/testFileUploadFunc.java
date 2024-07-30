package com.potato.potatotool.content.blueTeam;

import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.potato.potatotool.utils.Constants.getResourceFilePath;
import static com.potato.potatotool.utils.requestUtils.requests;

/**
 * @author Potato
 * @date 2023/5/30 09:00
 */
public class testFileUploadFunc {
    public static void main(String[] args){
        RequestObj obj = new RequestObj();
        obj.setUrl("http://192.168.10.120/vul/unsafeupload/clientcheck.php");
        obj.setMethod("POST");

        File file = null;
        try {
            file = new File(testFileUploadFunc.class.getResource("/img/PotatoTool.png").toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        Map<String, Object> formMap = new HashMap<>();
        formMap.put("submit", "开始上传");
        formMap.put("uploadfile", file);
        obj.setFormParameters(formMap);

//        obj.setPostMethod("Form");
//        obj.setFile(file);
//        obj.set(file);

        obj.setProxies("127.0.0.1:8080");

        try {
            CustomHttpResponse con = requests(obj);
            System.out.println(con.getTextStr());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
