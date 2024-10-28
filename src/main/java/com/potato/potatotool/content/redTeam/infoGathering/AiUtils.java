package com.potato.potatotool.content.redTeam.infoGathering;

import com.potato.potatotool.content.redTeam.infoGathering.imgSimilarity.ImgSimilarity;
import com.potato.potatotool.utils.aiUtil;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Potato
 * @date 2023/10/8 13:51
 */
public class AiUtils {

    // 获取公司相近名称
    public static Set<String> getCompanyName_Ai(String company){
        aiUtil aiObj=new aiUtil();
        String companyNameListStr = aiObj.askAi_NoStream("用户输入的公司名为```"+company+"```，请你推测该公司的全称或其他简称(尽可能大于三个字)，尽可能全面，并使用方括号将结果括起来，以便强调。例如：[[[国家能源投资集团有限责任公司]]]、[[[国家能源集团]]]、[[[国能集团]]]、[[[神华集团有限责任公司]]]、[[[国能投]]]");

        System.out.println(companyNameListStr);
        Set<String> result = aiObj.get_re_result(companyNameListStr);
        return result;
    }

    // 获取内容相关性   如果存在可靠原域名，则先判断图标相似性
    public static boolean getContentRelevance_Ai(Map<String, Object> webBaseInfoMap, String company, Map<String, Object> targetWebBaseInfoMap){
        if(webBaseInfoMap==null||webBaseInfoMap.isEmpty()) return false;
        aiUtil aiObj=new aiUtil();

        String url = webBaseInfoMap.getOrDefault("url", "").toString();
        String title = webBaseInfoMap.getOrDefault("title", "").toString();
        String body = webBaseInfoMap.getOrDefault("body", "").toString();
        String url_Q = url.isEmpty()? url : "网址为```" + url + "```，";
        String title_Q = url.isEmpty()? url : "标题为```" + title + "```，";
        String body_Q = url.isEmpty()? url : "部分内容为```" + body + "```，";
        String companyNameListStr = aiObj.askAi_NoStream("有一个网站，" + url_Q+ title_Q + body_Q + "。请推断该网站是否与```" + company + "```相关，并使用方括号将结果括起来，以便强调。比如，像[[[相关]]]、[[[不相关]]]、[[[无法推断]]]");

        if(targetWebBaseInfoMap!=null){
            String iconUrl = webBaseInfoMap.getOrDefault("iconUrl", "").toString();
            String targetIconUrl = targetWebBaseInfoMap.getOrDefault("iconUrl", "").toString();
            if(!iconUrl.isEmpty() && !targetIconUrl.isEmpty()){
                try {
                    boolean similarity = new ImgSimilarity().matchSimilar(new URL(iconUrl), new URL(targetIconUrl));
                    if(similarity) return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        Set<String> result = aiObj.get_re_result(companyNameListStr);
        for(String item: result){
            if(item.equals("相关")){
                return true;
            }
        }
        return false;
    }
    public static boolean getContentRelevance_Ai(String url, String company,String targetUrl){
        boolean hasIconUrl = false;
        Map<String, Object> targetWebBaseInfoMap = null;
        if ( targetUrl!=null && !targetUrl.isEmpty()){
            hasIconUrl = true;
            targetWebBaseInfoMap = Utils.getWebBaseInfo(targetUrl, hasIconUrl);
        }
        Map<String, Object> webBaseInfoMap = Utils.getWebBaseInfo(url, hasIconUrl);

        return getContentRelevance_Ai(webBaseInfoMap, company, targetWebBaseInfoMap);
    }


    // 提取泄露的敏感信息
    public static Set<String> getLeakage_Ai(String content){
        aiUtil aiObj=new aiUtil();
        String companyNameListStr = aiObj.askAi_NoStream("请判断以下文本是否存在敏感信息泄露：```"+content+"```，如果发现泄露信息，请使用方括号将结果括起来，以便强调，例如：[[[泄露账号密码admin/1433223]]]、[[[泄露数据库密码sifk@da.]]]。如果没有泄露信息，则无需强调。");

        Set<String> result = aiObj.get_re_result(companyNameListStr);
        return result;
    }


    public static void main(String[] args) {
        System.out.println(getCompanyName_Ai("国电"));

        System.out.println(getContentRelevance_Ai("https://www.potato.gold", "国能", null));
        System.out.println(getContentRelevance_Ai("https://www.potato.gold", "国家能源集团", null));
        System.out.println(getContentRelevance_Ai("https://www.potato.gold", "中国", null));
        System.out.println(getContentRelevance_Ai("https://www.potato.gold", "中国网站", null));
        System.out.println(getContentRelevance_Ai("https://www.potato.gold", "中国网站", "https://www.potato.gold"));
        System.out.println(getContentRelevance_Ai("https://www.potato.gold", "美国", null));
        System.out.println(getContentRelevance_Ai("https://www.potato.gold", "土豆", null));
        System.out.println(getContentRelevance_Ai("https://www.potato.gold", "山东", null));
    }
}
