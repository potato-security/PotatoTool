package com.potato.potatotool.content.blueTeam;

import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;

import java.io.File;

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

        File file = new File(getResourceFilePath("imgs")+"np/shadow_bg_tooltip2.9.png");

//        obj.setPostMethod("Form");
//        Map<String, Object> formMap = new HashMap<>();
//        formMap.put("submit", "开始上传");
//        formMap.put("uploadfile", file);
//        obj.setFormParameters(formMap);
        obj.setPostMethod("Chunked");
        obj.setPostData(file);

        obj.setProxies("127.0.0.1:8080");

        try {
            CustomHttpResponse con = requests(obj);
            System.out.println(con.getTextStr());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
