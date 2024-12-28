package com.potato.potatotool.content.blueTeam;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.oracle.truffle.regex.tregex.util.json.Json;
import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

import static com.potato.potatotool.content.redTeam.infoGathering.utils.Utils.getElementText;
import static com.potato.potatotool.content.redTeam.infoGathering.utils.Utils.getElements;
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

        JsonObject res = getBankCardInfo_alipay(bankcardId);
        JsonObject res_Guabu = getBankCardInfo_guabu(bankcardId);
        if(res_Guabu!=null && res_Guabu.has("position")) res.addProperty("position", res_Guabu.get("position").getAsString());
        if(res_Guabu!=null && res_Guabu.has("bank")) {
            String bank_Guabu = res_Guabu.get("bank").getAsString();
            String bank = (res!=null)? "" : res.get("bank").getAsString();
            if(bank_Guabu.length() > bank.length()){
                res.addProperty("bank", bank_Guabu);
            }
        }

        return res;
    }

    private static JsonObject getBankCardInfo_alipay(String bankcardId){
        RequestObj obj = new RequestObj().setMethod("GET").setUrl("https://ccdcapi.alipay.com/validateAndCacheCardInfo.json?_input_charset=utf-8&cardNo=" + bankcardId + "&cardBinCheck=true");
        JsonObject res = new JsonObject();

        try {
            CustomHttpResponse con = requests(obj);
            res = con.getJson().getAsJsonObject();

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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    private static JsonObject getBankCardInfo_guabu(String bankcardId){
        RequestObj obj = new RequestObj().setMethod("GET").setUrl("http://www.guabu.com/api/bank/?cardid=" + bankcardId);

        JsonObject res = new JsonObject();

        try {
            CustomHttpResponse con = requests(obj);

            int statusCode = con.getResponseCode();
            if (statusCode != 200) {
                con.disconnect();
                return res;
            }

            Document doc = con.getDocument();
            String position = getElementText(doc, "guishu");
            String bank = getElementText(doc, "miaoshu");
            if(!position.isEmpty() && !position.equals("-")) res.addProperty("position", position);
            if(!bank.isEmpty() && !bank.equals("-")) res.addProperty("bank", bank);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return res;
    }

    public static void main(String []args) {
        JsonObject str= getBankCardInfo("95588802000016677771");
        System.out.println(str);
    }

}
