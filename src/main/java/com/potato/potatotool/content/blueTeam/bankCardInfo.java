package com.potato.potatotool.content.blueTeam;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;

import java.util.HashMap;
import java.util.Map;

import static com.potato.potatotool.utils.Constants.getConfigInfo;
import static com.potato.potatotool.utils.Constants.getResourceString;
import static com.potato.potatotool.utils.requestUtils.requests;

/**
 * @author Potato
 * @date 2024/4/18 16:26
 */
public class bankCardInfo {

    public static String blockUrl = getConfigInfo("blockchainUrl");
    public static HashMap<String, String> headers = new HashMap();
    static {
        headers.put("AuthToken", "MHg2ZCwweDcwLDB4NzMsMHg3OSwweDdhLDB4NDQsMHgzMywweDc0LDB4NmQsMHg0NiwweDc1LDB4MzIsMHg3NywweDQ4LDB4NzEsMHg2YywweDZmLDB4NzUsMHgzOCwweDc3LDB4NmMsMHg0NiwweDY4LDB4MmYsMHg0OSwweDQ4LDB4NTEsMHgzNywweDQ3LDB4MzksMHg0NywweDRiLDB4NDcsMHg0YiwweDcxLDB4MzYsMHg2MSwweDM1LDB4NDMsMHg3MiwweDY1LDB4NmUsMHg2MywweDU0LDB4NDQsMHgzMiwweDRiLDB4NTAsMHg2NywweDc4LDB4NWEsMHg0ZCwweDM5LDB4NjEsMHg0ZCwweDRjLDB4MzksMHg1YSwweDJiLDB4NGEsMHg0NCwweDM2LDB4NGIsMHg2ZCwweDVhLDB4NTIsMHg0YywweDcxLDB4NGQsMHgzNiwweDczLDB4NDIsMHg2NywweDM5LDB4NzMsMHg3NCwweDRkLDB4NjUsMHg0MiwweDY3LDB4NmQsMHgzOCwweDUyLDB4NzIsMHg0NSwweDUyLDB4NTcsMHg1OCwweDU4LDB4NzYsMHgzNywweDcwLDB4NGMsMHg3MiwweDc3LDB4NGYsMHg0NiwweDM3LDB4NzMsMHg0OSwweDMyLDB4NTcsMHgzNCwweDZiLDB4NzUsMHg1OSwweDRkLDB4M2Q=");
    }

    public static Map<String,String>cardTypeMap = new HashMap<>();
    public static JsonObject mapping;
    static {
        cardTypeMap.put("DC", "储蓄卡");
        cardTypeMap.put("CC", "信用卡");
        cardTypeMap.put("SCC", "准贷记卡");
        cardTypeMap.put("PC", "预付费卡");
        mapping = JsonParser.parseString(getResourceString("bankname")).getAsJsonObject();;
    }

    /**
     * 通过银行卡号获取银行卡归属地等信息
     * 返回格式：
     * @param bankcardid 银行卡号
     * @return
     */
    public static JsonObject getBankCardInfo(String bankcardid) {
        RequestObj obj = new RequestObj().setMethod("GET").setHeaders(headers).setUrl(blockUrl + "/getBankCardInfo?bankcardid=" + bankcardid);

        try {
            CustomHttpResponse con = requests(obj);
            JsonObject res = con.getJson();

            // 映射字段，存在则替换
            String bank = res.get("bank").getAsString();
            if (mapping.has(bank)) {
                String mappedBank = mapping.get(bank).getAsString();
                res.addProperty("bank", mappedBank);
            }

            String cardType = res.get("cardType").getAsString();
            if (cardTypeMap.containsKey(cardType)) {
                String mappedBank = cardTypeMap.get(cardType);
                res.addProperty("cardType", mappedBank);
            }

            return res;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String []args) {
        JsonObject str= getBankCardInfo("9558880200001667777");
        System.out.println(str);
    }

}
