package com.potato.potatotool.content.blueTeam;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;

import java.util.HashMap;
import java.util.Map;

import static com.potato.potatotool.utils.Constants.getResourceString;
import static com.potato.potatotool.utils.requestUtils.requests;

/**
 * @author Potato
 * @date 2024/4/18 16:26
 */
public class bankCardInfo {

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
     * @param bankcardId 银行卡号
     * @return
     */
    public static JsonObject getBankCardInfo(String bankcardId) {
        bankcardId = bankcardId.replace(" ","");
        RequestObj obj = new RequestObj().setMethod("GET").setUrl("https://ccdcapi.alipay.com/validateAndCacheCardInfo.json?_input_charset=utf-8&cardNo=" + bankcardId + "&cardBinCheck=true");

        try {
            CustomHttpResponse con = requests(obj);
            JsonObject res = con.getJson().getAsJsonObject();

            // 映射字段，存在则替换
            if( res!=null && res.has("bank") && res.has("cardType")){
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
            }else {
                return null;
            }

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
