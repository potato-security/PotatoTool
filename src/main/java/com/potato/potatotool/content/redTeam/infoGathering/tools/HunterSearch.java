package com.potato.potatotool.content.redTeam.infoGathering.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;
import com.potato.potatotool.utils.strUtils;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.content.redTeam.infoGathering.AssetMapper.isDomainName;
import static com.potato.potatotool.utils.requestUtils.requests;

/**
 * @author Potato
 * @date 2024/10/11 13:37
 * @desc 鹰图资产测绘。支持多Key传入，自动翻页并更换Key (免费key的api每页最大100条，500条/天/账户)
 */
public class HunterSearch {
    private static String[] HUNTER_KEY_LIST = {
            "ec5110203c89eb842caf712c183b23c51555c1196bad4579f051813eca6aa33b",
            "04f246682bad0d1d80fbd3e9317d982075b79cc7495f548adb3f0242eb841de6",
            "d0fbe77ef130f76bc95ca416b5f5a174a8fffe589d4a46a8a56e164402b8ab32",
            "0cf9f42e4d6ebd02bc6be8b7b9f3d5241dc52fa36da647619117d253592d4352",
            "b5b9c1a7443d344358934c71cffc971d4f225e286c60fe4bf55e556c64bb7c24",
            "02f2d92f4f8b26e3442fb4d75967fc998d6f7042ecb37668195bccddf1b9d8bc",
            "b99cbd1bb624aae31c22ed0a59a332e09bd4e96bb9e5799b9262176947df3663",
            "e951140a45219801d586ee89923a529b2ff67726fa6cb62324082c8a0c2b4930",
            "4477092ae1d42de75c9c15962c5f5ecdb8902a27fcf3af767c628e01fc4bf768"
    };
    private static int keyIndex = 0;
    public static int rest_quota = -1;

    public static JsonArray search_hunter(String qInfo) {
        JsonArray domainInfo = new JsonArray();

        if( qInfo==null || qInfo.isEmpty() ) return domainInfo;
        if(HUNTER_KEY_LIST.length==0||HUNTER_KEY_LIST[0].isEmpty()){
            System.out.println("未设置HUNTER_KEY，无法调用Hunter接口");
            return domainInfo;
        }

        if(rest_quota == 0 && keyIndex+1 == HUNTER_KEY_LIST.length){
            System.out.println("所有HUNTER_KEY均无积分可用，无法调用Hunter接口");
            return domainInfo;
        }

        for(int i=1; true ;i++){
            // 判断剩余积分是否够用
            if(rest_quota == 0 && keyIndex+1 == HUNTER_KEY_LIST.length){
                System.out.println("所有Hunter账号今日均无积分可使用。");
                // TODO 提示
                break;
            }

            int total = -1;
            try {
                String HUNTER_KEY = HUNTER_KEY_LIST.length > 0 ? HUNTER_KEY_LIST[keyIndex] : null;
                RequestObj obj = new RequestObj().setUrl("https://hunter.qianxin.com/openApi/search?api-key=" + HUNTER_KEY + "&search=" + strUtils.base64UrlEncoder(qInfo) + "&page=" + i +"&page_size=100&is_web=1")
                        .setMethod("GET").setRandomUserAgent(false).setRetries(3);
                System.out.println(obj.getUrl());

                CustomHttpResponse con = requests(obj);

                int statusCode = con.getResponseCode();
                // 检查请求状态码
                if (statusCode != 200) {
                    return domainInfo;
                }

                JsonObject json = con.getJson().getAsJsonObject();
                String message = json.get("message").getAsString();
                if((message.contains("积分用完了")||json.get("data")==null) && keyIndex+1 < HUNTER_KEY_LIST.length){
                    keyIndex += 1;
                    i -= 1;
                    continue;
                }

                JsonObject jsonData = json.get("data").getAsJsonObject();
                domainInfo = jsonData.get("arr").getAsJsonArray();
                total = jsonData.get("total").getAsInt();
                rest_quota = strUtils.extractNumber(jsonData.get("rest_quota").getAsString());

            }catch (Exception e){
                if(debugMode) System.out.println(e);
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

    public static JsonArray getDomainByCompanyOrDomain_hunter(String companyOrDomain) {
        String qInfo = "cert=\"" + companyOrDomain + "\"";

        if(!isDomainName(companyOrDomain)){
            qInfo += "||icp.name=\"" + companyOrDomain + "\"";
        }

        return search_hunter(qInfo);
    }

    // TODO web.icon="03439ca7d20660b7af45c10fe90ed793" ip="220.181.111.0/24" web.body="网络空间测绘" domain="qianxin.com"

    public static void main(String[] args) {
        System.out.println(getDomainByCompanyOrDomain_hunter("深圳湾科技发展有限公司"));
    }
}
