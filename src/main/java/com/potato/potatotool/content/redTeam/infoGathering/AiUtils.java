package com.potato.potatotool.content.redTeam.infoGathering;

import com.potato.potatotool.utils.aiUtil;

import java.util.Map;
import java.util.Set;

/**
 * @author Potato
 * @date 2023/10/8 13:51
 */
public class AiUtils {

    public static Set<String> getCompanyName_Ai(String company){
        aiUtil aiObj=new aiUtil();
        String companyNameListStr = aiObj.askAi_NoStream("用户输入的公司名为```"+company+"```，请你推测该公司的全称或其他简称(尽可能大于三个字)，尽可能全面，并使用方括号将结果括起来，以便强调。例如：[[[国家能源投资集团有限责任公司]]]、[[[国家能源集团]]]、[[[国能集团]]]、[[[神华集团有限责任公司]]]、[[[国能投]]]");

        System.out.println(companyNameListStr);
        Set<String> result = aiObj.get_re_result(companyNameListStr);
        return result;
    }

    // 获取内容相关性
    public static Set<String> getContentRelevance_Ai(Map<String, String> webInfoMap, String company){
        aiUtil aiObj=new aiUtil();

        String url = webInfoMap.getOrDefault("url", "");
        String title = webInfoMap.getOrDefault("title", "");
        String body = webInfoMap.getOrDefault("body", "");
        String url_Q = url.isEmpty()? url : "网址为```" + url + "```，";
        String title_Q = url.isEmpty()? url : "标题为```" + title + "```，";
        String body_Q = url.isEmpty()? url : "部分内容为```" + body + "```，";
        String companyNameListStr = aiObj.askAi_NoStream("有一个网站，" + url_Q+ title_Q + body_Q + "。请推断该网站是否与```" + company + "```相关，并使用方括号将结果括起来，以便强调。比如，像[[[相关]]]、[[[不相关]]]、[[[无法推断]]]");

        System.out.println(companyNameListStr);
        Set<String> result = aiObj.get_re_result(companyNameListStr);
        return result;
    }

    public static void main(String[] args) {
        System.out.println(getCompanyName_Ai("国电"));

        System.out.println(getContentRelevance_Ai(Utils.getWebInfo("https://www.potato.gold"), "国能"));
        System.out.println(getContentRelevance_Ai(Utils.getWebInfo("https://www.potato.gold"), "国家能源集团"));
        System.out.println(getContentRelevance_Ai(Utils.getWebInfo("https://www.potato.gold"), "中国"));
        System.out.println(getContentRelevance_Ai(Utils.getWebInfo("https://www.potato.gold"), "中国网站"));
        System.out.println(getContentRelevance_Ai(Utils.getWebInfo("https://www.potato.gold"), "美国"));
        System.out.println(getContentRelevance_Ai(Utils.getWebInfo("https://www.potato.gold"), "土豆"));
        System.out.println(getContentRelevance_Ai(Utils.getWebInfo("https://www.potato.gold"), "山东鼎夏"));
    }
}
