package com.potato.potatotool.content.redTeam.infoGathering.infoLeakage.gitLeakage;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.AssetConstants;
import com.potato.potatotool.content.redTeam.infoGathering.utils.AiUtils;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.network.ProxyUtils;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.network.RequestUtils.requests;

/**
 * @author Potato
 * @date 2023/10/1 15:53
 */
public class GitHubLeakage {
    private static List<String> GitHub_Token;
    private static boolean Proxy = false;
    private static boolean isEffectiveKey = true;
    private static int keyIndex = 0;
    public GitHubLeakage(Set<String> GitHub_Token, boolean Proxy){
        this.GitHub_Token = new ArrayList<String>(GitHub_Token);
        this.Proxy = Proxy;
    }

    // 再模糊点检索，可以直接检索code而非repo
    public JsonArray getRepo(Set<String> companyNameList, String domain, int maxGithubSearchCount){
        JsonArray repoArray = new JsonArray();
        String question = "";
        if(companyNameList!=null && !companyNameList.isEmpty()) {
            question +=  "\"" + String.join("\" OR \"", companyNameList) + "\"";
        }
        if(domain!=null && !domain.isEmpty()){
            if(!question.isEmpty()) question += " OR ";
            question += "\"" + domain + "\"";
        }
        if(question.isEmpty() ||GitHub_Token.size()==0|| GitHub_Token.get(0).isEmpty() || !isEffectiveKey){
            return repoArray;
        }

        if(maxGithubSearchCount > 100) maxGithubSearchCount = 100;
        while (true) {

            RequestObj obj = new RequestObj()
                    .setUrl("https://api.github.com/search/repositories?per_page=" + maxGithubSearchCount + "&q=" + StrUtils.urlEncode(question))
                    .setMethod("GET")
                    .setBearerToken(GitHub_Token.get(keyIndex))
                    .setRetries(2);
            if(!Proxy) obj.setProxies(null);

            try (CustomHttpResponse con = requests(obj)){

                int statusCode = con.getResponseCode();
                // 检查请求状态码
                if (statusCode != 200) {
                    if (statusCode == 403 || statusCode == 401) {
                        keyIndex += 1;
                        if(keyIndex + 1 > GitHub_Token.size()){
                            System.out.println("所有GitHub_Token今日均无免费额度可使用。");
                            break;
                        }else {
                            continue;
                        }
                    }
                    break;
                }

                JsonObject repoArrayObj = con.getJson().getAsJsonObject();

                if (repoArrayObj.has("items")) {
                    JsonArray items = repoArrayObj.getAsJsonArray("items");
                    for (JsonElement item : items) {
                        JsonObject repoObj = new JsonObject();
                        String repoName = item.getAsJsonObject().get("full_name").getAsString();
                        String repoDes = (item.getAsJsonObject().get("description")==null || item.getAsJsonObject().get("description").isJsonNull()) ? "" : item.getAsJsonObject().get("description").getAsString() ;

                        if ((domain!=null && domain.equals("HotBoy-java/PotatoTool")) || AiUtils.getGitRepoRelevance_Ai(repoName, repoDes, question)) {
                            repoObj.addProperty("repoName", repoName);
                            repoObj.addProperty("repoUrl", item.getAsJsonObject().get("html_url").getAsString());
                            repoObj.addProperty("repoDes", repoDes);
                            repoArray.add(repoObj);
                        }
                    }
                }

            } catch (Exception e) {
                if (debugMode) e.printStackTrace();
            }
            break;
        }

        return repoArray;
    }

    public static String getError_Github() {
        isEffectiveKey = true;
        JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig(AssetConstants.ASSET);
        Set<String> GitHub_Token_Set = new HashSet<>();
        for (JsonElement element : tmpJsonObj.getAsJsonArray(AssetConstants.GITHUB_TOKEN)) {
            GitHub_Token_Set.add(element.getAsString());
        }
        boolean Proxy = ProxyUtils.isServiceProxyEnabled(AssetConstants.GITHUB_TOKEN);
        GitHubLeakage gitHubLeakage = new GitHubLeakage(GitHub_Token_Set, Proxy);
        String domain = "HotBoy-java/PotatoTool";
        if (gitHubLeakage.getRepo(null, domain, 1).isEmpty()){
            isEffectiveKey = false;
            return "无效的GitHub_Token";
        }
        return null;
    }


    // TODO 目前API不支持正则、不支持OR、不支持|  本地浏览器打开
    public JsonArray getLeakageCode(JsonArray repoArray){
        for(JsonElement repoJsonElement : repoArray){
            JsonObject repoObj = repoJsonElement.getAsJsonObject();
            String repoName = repoObj.get("repoName").getAsString();
            String[] questions = {
                    "repo:" + repoName +" /(?i)((access_key|access_token|admin_pass|admin_user|algolia_admin_key|algolia_api_key|alias_pass|alicloud_access_key|amazon_secret_access_key|amazonaws|ansible_vault_password|aos_key|api_key|api_key_secret|api_key_sid|api_secret|api.googlemaps AIza|apidocs|apikey|apiSecret|app_debug|app_id|app_key|app_log_level|app_secret|appkey|appkeysecret|application_key|appsecret|appspot|auth_token|authorizationToken|authsecret|aws_access|aws_access_key_id|aws_bucket|aws_key|aws_secret|aws_secret_key|aws_token|AWSSecretKey|b2_app_key|bashrc password|bintray_apikey|bintray_gpg_password|bintray_key|bintraykey|bluemix_api_key|bluemix_pass|browserstack_access_key|bucket_password|bucketeer_aws_access_key_id|bucketeer_aws_secret_access_key|built_branch_deploy_key|bx_password|cache_driver|cache_s3_secret_key|cattle_access_key|cattle_secret_key|certificate_password)[a-z0-9_ .\\-,]{0,25})(=|>|:=|\\|\\|:|<=|=>|:).{0,5}['\\\"]([0-9a-zA-Z\\-_=]{8,64})['\\\"]/",
                    "repo:" + repoName +" /(?i)((ci_deploy_password|client_secret|client_zpk_secret_key|clojars_password|cloud_api_key|cloud_watch_aws_access_key|cloudant_password|cloudflare_api_key|cloudflare_auth_key|cloudinary_api_secret|cloudinary_name|codecov_token|config|conn.login|connectionstring|consumer_key|consumer_secret|credentials|cypress_record_key|database_password|database_schema_test|datadog_api_key|datadog_app_key|db_password|db_server|db_username|dbpasswd|dbpassword|dbuser|deploy_password|digitalocean_ssh_key_body|digitalocean_ssh_key_ids|docker_hub_password|docker_key|docker_pass|docker_passwd|docker_password|dockerhub_password|dockerhubpassword|dot-files|dotfiles|droplet_travis_password|dynamoaccesskeyid|dynamosecretaccesskey|elastica_host|elastica_port|elasticsearch_password|encryption_key|encryption_password|env.heroku_api_key|env.sonatype_password|eureka.awssecretkey)[a-z0-9_ .\\-,]{0,25})(=|>|:=|\\|\\|:|<=|=>|:).{0,5}['\\\"]([0-9a-zA-Z\\-_=]{8,64})['\\\"]/"

            };


            for(String question : questions) {
                RequestObj obj = new RequestObj()
                        .setUrl("https://api.github.com/search/code?per_page=100&q=" + StrUtils.urlEncode(question))
                        .setMethod("GET")
                        .setBearerToken(GitHub_Token.get(keyIndex))
                        .setRetries(2);
                if(!Proxy) obj.setProxies(null);

                try (CustomHttpResponse con = requests(obj)){

                    int statusCode = con.getResponseCode();
                    // 检查请求状态码
                    if (statusCode != 200) {
                        continue;
                    }

                    JsonObject repoArrayObj = con.getJson().getAsJsonObject();

                    if (repoArrayObj.has("items")) {
                        JsonArray items = repoArrayObj.getAsJsonArray("items");
                        for (JsonElement item : items) {
                            repoObj.addProperty("keyLeakage", item.getAsJsonObject().get("html_url").getAsString());
                        }
                    }

                } catch (Exception e) {
                    if (debugMode) e.printStackTrace();
                }
            }
        }

        return repoArray;
    }


    public static void main(String[] args) {
        JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig(AssetConstants.ASSET);
        Set<String> GitHub_Token_Set = new HashSet<>();
        for (JsonElement element : tmpJsonObj.getAsJsonArray(AssetConstants.GITHUB_TOKEN)) {
            GitHub_Token_Set.add(element.getAsString());
        }
        boolean Proxy = ProxyUtils.isServiceProxyEnabled(AssetConstants.GITHUB_TOKEN);
        GitHubLeakage gitHubLeakage = new GitHubLeakage(GitHub_Token_Set, Proxy);

//        JsonArray xxx= new JsonArray();
//        JsonObject qqq = new JsonObject();
//        qqq.addProperty("repoName","sendgrid/sendgrid-csharp");
//        xxx.add(qqq);
//        System.out.println(gitHubLeakage.getLeakageCode(xxx));
        Set<String> companyNameList = new HashSet<>();
//        companyNameList.add("国家能源集团");
        companyNameList.add("国能");
        System.out.println(gitHubLeakage.getRepo(companyNameList, null, 10));
    }
}
