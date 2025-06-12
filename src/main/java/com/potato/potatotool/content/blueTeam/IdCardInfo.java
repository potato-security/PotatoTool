package com.potato.potatotool.content.blueTeam;

import com.google.gson.JsonObject;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;

import java.util.HashMap;

import static com.potato.potatotool.utils.core.Constants.getConfigInfo;
import static com.potato.potatotool.utils.network.RequestUtils.requests;

/**
 * @author Potato
 * @date 2024/4/18 16:26
 */
public class IdCardInfo {
    public static String blockUrl = getConfigInfo("blockchainUrl");
    public static HashMap<String, String> headers = new HashMap();
    static {
        headers.put("AuthToken", "MHg2ZCwweDcwLDB4NzMsMHg3OSwweDdhLDB4NDQsMHgzMywweDc0LDB4NmQsMHg0NiwweDc1LDB4MzIsMHg3NywweDQ4LDB4NzEsMHg2YywweDZmLDB4NzUsMHgzOCwweDc3LDB4NmMsMHg0NiwweDY4LDB4MmYsMHg0OSwweDQ4LDB4NTEsMHgzNywweDQ3LDB4MzksMHg0NywweDRiLDB4NDcsMHg0YiwweDcxLDB4MzYsMHg2MSwweDM1LDB4NDMsMHg3MiwweDY1LDB4NmUsMHg2MywweDU0LDB4NDQsMHgzMiwweDRiLDB4NTAsMHg2NywweDc4LDB4NWEsMHg0ZCwweDM5LDB4NjEsMHg0ZCwweDRjLDB4MzksMHg1YSwweDJiLDB4NGEsMHg0NCwweDM2LDB4NGIsMHg2ZCwweDVhLDB4NTIsMHg0YywweDcxLDB4NGQsMHgzNiwweDczLDB4NDIsMHg2NywweDM5LDB4NzMsMHg3NCwweDRkLDB4NjUsMHg0MiwweDY3LDB4NmQsMHgzOCwweDUyLDB4NzIsMHg0NSwweDUyLDB4NTcsMHg1OCwweDU4LDB4NzYsMHgzNywweDcwLDB4NGMsMHg3MiwweDc3LDB4NGYsMHg0NiwweDM3LDB4NzMsMHg0OSwweDMyLDB4NTcsMHgzNCwweDZiLDB4NzUsMHg1OSwweDRkLDB4M2Q=");
    }

    /**
     * 通过身份证号获取归属地等信息
     * 返回格式：{"success":"1","result":{"status":"ALREADY_ATT","par":"342501","idcard":"342501199402242817","born":"1994年02月24日","sex":"男","att":"安徽省宣城市宣州区","postno":"242000","areano":"0563","style_simcall":"中国,安徽,宣城","style_citynm":"中华人民共和国,安徽省,宣城市","msg":""}}
     * @param idcard 身份证号
     * @return
     */
    public static JsonObject getIdCardInfo(String idcard) {
        idcard = idcard.replace(" ","");
        RequestObj obj = new RequestObj().setMethod("GET").setHeaders(headers).setUrl(blockUrl + "/getIdCardInfo?idcard=" + idcard);

        try {
            CustomHttpResponse con = requests(obj);
            JsonObject res = con.getJson().getAsJsonObject();
            if( res!=null && res.has("result")){
                return res.getAsJsonObject("result");
            }else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String []args) {
        JsonObject str= getIdCardInfo("342501199402242817");
        System.out.println(str);
    }
}
