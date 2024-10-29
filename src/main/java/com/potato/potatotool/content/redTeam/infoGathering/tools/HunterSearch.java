package com.potato.potatotool.content.redTeam.infoGathering.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.Constants;
import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;
import com.potato.potatotool.utils.strUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.content.redTeam.infoGathering.utils.Utils.isDomainName;
import static com.potato.potatotool.utils.requestUtils.requests;

/**
 * @author Potato
 * @date 2024/10/11 13:37
 * @desc 鹰图资产测绘。支持多Key传入，自动翻页并更换Key (免费key的api每页最大100条，500条/天/账户)
 */
public class HunterSearch {
    private static List<String> HUNTER_KEY_LIST;
    private static int keyIndex = 0;
    public static int rest_quota = -1;

    public HunterSearch(Set<String> HUNTER_KEY_LIST){
        this.HUNTER_KEY_LIST = new ArrayList<String>(HUNTER_KEY_LIST);
    }

    public JsonArray search_Hunter(String qInfo) {
        JsonArray domainInfo = new JsonArray();

        if( qInfo==null || qInfo.isEmpty() ) return domainInfo;
        if(HUNTER_KEY_LIST.size()==0|| HUNTER_KEY_LIST.get(0).isEmpty()){
            System.out.println("未设置HUNTER_KEY，无法调用Hunter接口");
            return domainInfo;
        }

        for(int i=1; true ;i++){
            // 判断剩余积分是否够用
            if(rest_quota == 0 && keyIndex+1 == HUNTER_KEY_LIST.size()){
                System.out.println("所有Hunter账号今日均无积分可使用。");
                // TODO 提示
                break;
            }

            int total = -1;
            try {
                String HUNTER_KEY = HUNTER_KEY_LIST.size() > 0 ? HUNTER_KEY_LIST.get(keyIndex) : null;
                RequestObj obj = new RequestObj().setUrl("https://hunter.qianxin.com/openApi/search?api-key=" + HUNTER_KEY + "&search=" + strUtils.base64UrlEncoder(qInfo) + "&page=" + i +"&page_size=100&is_web=1")
                        .setMethod("GET").setRandomUserAgent(false).setRetries(3);

                CustomHttpResponse con = requests(obj);

                int statusCode = con.getResponseCode();
                // 检查请求状态码
                if (statusCode != 200) {
                    return domainInfo;
                }

                JsonObject json = con.getJson().getAsJsonObject();
                String message = json.get("message").getAsString();
                if((message.contains("积分用完了")||json.get("data")==null) && keyIndex+1 < HUNTER_KEY_LIST.size()){
                    keyIndex += 1;
                    i -= 1;
                    if(keyIndex+1 > HUNTER_KEY_LIST.size()){
                        System.out.println("所有Hunter账号今日均无积分可使用。");
                        // TODO 提示
                        break;
                    }else {
                        continue;
                    }
                }

                JsonObject jsonData = json.get("data").getAsJsonObject();
                domainInfo = jsonData.get("arr").getAsJsonArray();
                total = jsonData.get("total").getAsInt();
                rest_quota = strUtils.extractNumber(jsonData.get("rest_quota").getAsString());

            }catch (Exception e){
                if(debugMode) e.printStackTrace();
                e.printStackTrace();
            }

            //  判断是否需要翻页
            if(i * 100 >= total ){
                // 不需要翻页，结束循环
                break;
            }

        }

        return domainInfo;
    }

    public JsonArray getInfoByCompanyOrDomain_Hunter(String companyOrDomain) {
        if(companyOrDomain==null || companyOrDomain.isEmpty()) return new JsonArray();
        String qInfo = "cert=\"" + companyOrDomain + "\"";

        if(!isDomainName(companyOrDomain)){
            // companyName
            qInfo += "||icp.name=\"" + companyOrDomain + "\"";
        }else {
            // domain
            qInfo += "||domain=\"" + companyOrDomain + "\"";
        }

        return search_Hunter(qInfo);
    }

    public JsonArray getInfoByIcpNo_Hunter(String icpNo) {
        if(icpNo==null || icpNo.isEmpty()) return new JsonArray();
        String qInfo = "icp.number=\"" + icpNo + "\"";

        return search_Hunter(qInfo);
    }


    public JsonArray getInfoByBodyFilterIcp_Hunter(String companyNameStr, String domainStr) {
        if(companyNameStr==null || companyNameStr.isEmpty()) return new JsonArray();
        String qInfo = "web.body=\"" + companyNameStr + "\" && cert!=\"" + companyNameStr + "\" && icp.name!=\"" + companyNameStr + "\" && domain!=\"" + domainStr + "\"";

        return search_Hunter(qInfo);
    }

    // TODO web.icon="03439ca7d20660b7af45c10fe90ed793" ip="220.181.111.0/24" web.body="网络空间测绘" domain="qianxin.com"

    public static void main(String[] args) {
        JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig("Asset");
        Set<String> Hunter_Key_Set = new HashSet<>();
        for (JsonElement element : tmpJsonObj.getAsJsonArray("Hunter_Key")) {
            Hunter_Key_Set.add(element.getAsString());
        }
        HunterSearch hunterSearch = new HunterSearch(Hunter_Key_Set);
        System.out.println(hunterSearch.getInfoByCompanyOrDomain_Hunter("深圳湾科技发展有限公司"));
    }
}
