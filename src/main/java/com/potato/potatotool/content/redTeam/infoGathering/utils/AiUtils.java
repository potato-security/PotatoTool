package com.potato.potatotool.content.redTeam.infoGathering.utils;

import com.potato.potatotool.content.redTeam.infoGathering.imgSimilarity.ImgSimilarity;
import com.potato.potatotool.utils.ai.service.AiChatService;

import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Potato
 * @date 2023/10/8 13:51
 */
public class AiUtils {

    // 获取公司相近名称
    public static Set<String> getCompanyName_Ai(String company){
        if(company==null||company.isEmpty()) return new HashSet<>();
        Set<String> result = new HashSet<>();
        result.add(company);

        try {
            AiChatService aiChatService = new AiChatService();
            String companyNameListStr = aiChatService.askNoStream("用户输入的公司名为```"+company+"```，请你推测该公司的全称或其他简称(尽可能大于三个字)，尽可能全面，并使用方括号将结果括起来，以便强调。最多四个结果。例如：[[[国家能源投资集团有限责任公司]]]、[[[国家能源集团]]]、[[[国能集团]]]、[[[神华集团有限责任公司]]]");
            Set<String> extractedResult = aiChatService.extractBracketedResult(companyNameListStr);
            if (extractedResult != null && !extractedResult.isEmpty()) {
                result = extractedResult;
            }
        }catch (Exception ignored){}

        return result;
    }

    public static Set<String> getCompanyFullName_Ai(String company){
        if(company==null||company.isEmpty()) return new HashSet<>();
        Set<String> result = new HashSet<>();
        try {
            AiChatService aiChatService = new AiChatService();
            String response = aiChatService.askNoStream("用户输入的公司相关名称为```"+company+"```。请只输出可能对应的公司完整名称，尽量避免简称、品牌名、集团口号或部门名称。请使用方括号将每个完整公司名括起来，最多返回四个结果。例如：[[[中国交通建设股份有限公司]]]、[[[中国交通建设集团有限公司]]]");
            result = aiChatService.extractBracketedResult(response);
        }catch (Exception ignored){}

        if (result.isEmpty()) {
            result.add(company);
        }
        return result;
    }

    // 获取内容相关性   如果存在可靠原域名，则先判断图标相似性
    public static boolean getContentRelevance_Ai(Map<String, Object> webBaseInfoMap, String company, Map<String, Object> targetWebBaseInfoMap){
        if(webBaseInfoMap==null||webBaseInfoMap.isEmpty()) return false;

        try {

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

            String url = webBaseInfoMap.getOrDefault("url", "").toString();
            String title = webBaseInfoMap.getOrDefault("title", "").toString();
            String body = webBaseInfoMap.getOrDefault("body", "").toString();
            String url_Q = url.isEmpty()? url : "网址为```" + url + "```，";
            String title_Q = title.isEmpty()? title : "标题为```" + title + "```，";
            String body_Q = body.isEmpty()? body : "部分内容为```" + body + "```，";
            AiChatService aiChatService = new AiChatService();
            String companyNameListStr = aiChatService.askNoStream("有一个网站，" + url_Q+ title_Q + body_Q + "。请推断该网站是否与```" + company + "```相关，排除招标网，并使用方括号将结果括起来，以便强调。比如，像[[[相关]]]、[[[不相关]]]、[[[无法推断]]]");

            Set<String> result = aiChatService.extractBracketedResult(companyNameListStr);
            for(String item: result){
                if(item.equals("相关")){
                    return true;
                }
            }

        }catch (Exception e){}
        return false;
    }

    public static boolean getContentRelevance_Ai(String url, String company, Map<String, Object> targetWebBaseInfoMap, boolean isCrawlProxy){
        boolean hasIconUrl = true;
        Map<String, Object> webBaseInfoMap = Utils.getWebBaseInfo(url, hasIconUrl, isCrawlProxy);

        return getContentRelevance_Ai(webBaseInfoMap, company, targetWebBaseInfoMap);
    }

    public static boolean getGitRepoRelevance_Ai(String repoName, String repoDes, String companyNames){
        if((repoName.isEmpty() && repoDes.isEmpty()) || companyNames.isEmpty()) return false;

        try {
            AiChatService aiChatService = new AiChatService();
            String companyNameListStr = aiChatService.askNoStream("有一个git项目，项目名为：" + repoName + "，项目描述：" + repoDes +"。请推断该网站是否与```" + companyNames + "```相关，并使用方括号将结果括起来，以便强调。比如，像[[[相关]]]、[[[不相关]]]、[[[无法推断]]]");

            Set<String> result = aiChatService.extractBracketedResult(companyNameListStr);
            for(String item: result){
                if(item.equals("相关")){
                    return true;
                }
            }
        }catch (Exception e){}

        return false;
    }


    // 提取泄露的敏感信息
    public static Set<String> getLeakage_Ai(String content){
        Set<String> result = new HashSet<>();
        try {
            AiChatService aiChatService = new AiChatService();
            String companyNameListStr = aiChatService.askNoStream("请判断以下文本是否存在敏感信息泄露：```"+content+"```，如果发现泄露信息，请使用方括号将结果括起来，以便强调，例如：[[[泄露账号密码admin/1433223]]]、[[[泄露数据库密码sifk@da.]]]。如果没有泄露信息，则无需强调。");
            result = aiChatService.extractBracketedResult(companyNameListStr);
        }catch (Exception ignored){}

        return result;
    }


    public static void main(String[] args) {
        System.out.println(getCompanyName_Ai("国电"));

//        System.out.println(getContentRelevance_Ai("https://www.potato.gold", "国能", null));
//        System.out.println(getContentRelevance_Ai("https://www.potato.gold", "国家能源集团", null));
//        System.out.println(getContentRelevance_Ai("https://www.potato.gold", "中国", null));
//        System.out.println(getContentRelevance_Ai("https://www.potato.gold", "中国网站", null));
//        System.out.println(getContentRelevance_Ai("https://www.potato.gold", "中国网站", "https://www.potato.gold"));
//        System.out.println(getContentRelevance_Ai("https://www.potato.gold", "美国", null));
//        System.out.println(getContentRelevance_Ai("https://www.potato.gold", "土豆", null));
//        System.out.println(getContentRelevance_Ai("https://www.potato.gold", "山东", null));
        System.out.println(getGitRepoRelevance_Ai("18704476796/guonengyt-console", "国能英泰管理后台项目", "\"国家能源集团\" OR \"国能\""));
    }
}
