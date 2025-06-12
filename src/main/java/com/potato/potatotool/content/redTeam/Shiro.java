package com.potato.potatotool.content.redTeam;

import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;
import com.potato.potatotool.utils.data.StrUtils;

import java.util.HashMap;

import static com.potato.potatotool.utils.network.RequestUtils.requests;

/**
 * @author Potato
 * @date 2023/4/21 11:47
 */
public class Shiro {

    public static String shiroKeyWord = "rememberMe=";
    public static String reqMethod = "GET";
    public static String postData = "";
    public static int timeOut = 10;

    public static void main(String[] args) { //.toLowerCase().equals("cookie")

        String iputUrl = "http://bs.citymedia.cn/";
        String inputKey = "";

        checkIsShiro(iputUrl);

    }

    public static boolean checkIsShiro(String iputUrl) {

        HashMap<String, String> headers = new HashMap();
        headers.put("Cookie", shiroKeyWord + StrUtils.generateRandomString(2, 6));


        try {
            RequestObj obj = new RequestObj();
            obj.setMethod(reqMethod);
            obj.setUrl(iputUrl);
            obj.setHeaders(headers);
            obj.setPostData(postData);
            obj.setTimeOut(timeOut);

            CustomHttpResponse con = requests(obj);

            System.out.println(con.getTextStr());

            if (con.getHeaderField("Set-Cookie").toString().contains("=deleteMe")) {
                System.out.println("[√] 存在shiro框架！");
            }else {
                System.out.println("[×] 不存在shiro框架！");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;

    }


}
