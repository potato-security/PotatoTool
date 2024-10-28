package com.potato.potatotool.content.redTeam.infoGathering.infoLeakage.gitLeakage;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.Constants;
import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;
import com.potato.potatotool.utils.strUtils;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.requestUtils.requests;

/**
 * @author Potato
 * @date 2023/10/1 15:53
 */
public class GitHubLeakage {
    private static String GitHub_Token;
    public GitHubLeakage(String GitHub_Token){
        this.GitHub_Token = GitHub_Token;
    }

//    public JsonArray search(String qInfo) {
//        return qInfo;
//    }

    // 再模糊点检索，可以直接检索code而非repo
    public JsonArray getRepo(String companyName, String domain){
        JsonArray repoArray = new JsonArray();
        String question = "";
        if(companyName!=null && !companyName.isEmpty()) {
            question += "\"" + companyName + "\"";
        }
        if(domain!=null && !domain.isEmpty()){
            if(!question.isEmpty()) question += " OR ";
            question += "\"" + domain + "\"";
        }
        if(question.isEmpty()){
            return repoArray;
        }

        try {
            RequestObj obj = new RequestObj()
                    .setUrl("https://api.github.com/search/repositories?per_page=100&q="+ strUtils.urlEncode(question))
                    .setMethod("GET")
                    .setBearerToken(GitHub_Token)
                    .setRetries(3);

            CustomHttpResponse con = requests(obj);

            int statusCode = con.getResponseCode();
            // 检查请求状态码
            if (statusCode != 200) {
                return repoArray;
            }

            JsonObject repoArrayObj = con.getJson().getAsJsonObject();

            if(repoArrayObj.has("items")){
                JsonArray items = repoArrayObj.getAsJsonArray("items");
                for(JsonElement item : items){
                    JsonObject repoObj = new JsonObject();
                    repoObj.addProperty("repoName", item.getAsJsonObject().get("full_name").getAsString());
                    repoObj.addProperty("repoUrl", item.getAsJsonObject().get("html_url").getAsString());
                    repoObj.addProperty("repoDes", item.getAsJsonObject().get("description").getAsString());
                    repoArray.add(repoObj);
                }
            }

        }catch (Exception e){
            if(debugMode) e.printStackTrace();
        }

        return repoArray;
    }


    // TODO 目前API不支持正则
    public JsonArray getLeakageCode(JsonArray repoArray){
//        JsonObject repoInfo = new JsonObject();
        for(JsonElement repoJsonElement : repoArray){
            JsonObject repoObj = repoJsonElement.getAsJsonObject();
            String repoName = repoObj.get("repoName").getAsString();
            String[] questions = {
                    "repo:" + repoName +" /(?i)((access_key|access_token|admin_pass|admin_user|algolia_admin_key|algolia_api_key|alias_pass|alicloud_access_key|amazon_secret_access_key|amazonaws|ansible_vault_password|aos_key|api_key|api_key_secret|api_key_sid|api_secret|api.googlemaps AIza|apidocs|apikey|apiSecret|app_debug|app_id|app_key|app_log_level|app_secret|appkey|appkeysecret|application_key|appsecret|appspot|auth_token|authorizationToken|authsecret|aws_access|aws_access_key_id|aws_bucket|aws_key|aws_secret|aws_secret_key|aws_token|AWSSecretKey|b2_app_key|bashrc password|bintray_apikey|bintray_gpg_password|bintray_key|bintraykey|bluemix_api_key|bluemix_pass|browserstack_access_key|bucket_password|bucketeer_aws_access_key_id|bucketeer_aws_secret_access_key|built_branch_deploy_key|bx_password|cache_driver|cache_s3_secret_key|cattle_access_key|cattle_secret_key|certificate_password)[a-z0-9_ .\\-,]{0,25})(=|>|:=|\\|\\|:|<=|=>|:).{0,5}['\\\"]([0-9a-zA-Z\\-_=]{8,64})['\\\"]/",
                    "repo:" + repoName +" /(?i)((ci_deploy_password|client_secret|client_zpk_secret_key|clojars_password|cloud_api_key|cloud_watch_aws_access_key|cloudant_password|cloudflare_api_key|cloudflare_auth_key|cloudinary_api_secret|cloudinary_name|codecov_token|config|conn.login|connectionstring|consumer_key|consumer_secret|credentials|cypress_record_key|database_password|database_schema_test|datadog_api_key|datadog_app_key|db_password|db_server|db_username|dbpasswd|dbpassword|dbuser|deploy_password|digitalocean_ssh_key_body|digitalocean_ssh_key_ids|docker_hub_password|docker_key|docker_pass|docker_passwd|docker_password|dockerhub_password|dockerhubpassword|dot-files|dotfiles|droplet_travis_password|dynamoaccesskeyid|dynamosecretaccesskey|elastica_host|elastica_port|elasticsearch_password|encryption_key|encryption_password|env.heroku_api_key|env.sonatype_password|eureka.awssecretkey)[a-z0-9_ .\\-,]{0,25})(=|>|:=|\\|\\|:|<=|=>|:).{0,5}['\\\"]([0-9a-zA-Z\\-_=]{8,64})['\\\"]/"

            };


            for(String question : questions) {
                try {
                    RequestObj obj = new RequestObj()
                            .setUrl("https://api.github.com/search/code?per_page=100&q=" + strUtils.urlEncode(question))
                            .setMethod("GET")
                            .setBearerToken(GitHub_Token)
                            .setProxies("https://127.0.0.1:8080")
                            .setRetries(3);

                    CustomHttpResponse con = requests(obj);

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
        JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig("Asset");
        GitHubLeakage gitHubLeakage = new GitHubLeakage(tmpJsonObj.getAsJsonPrimitive("GitHub_Token").getAsString());

        JsonArray xxx= new JsonArray();
        JsonObject qqq = new JsonObject();
        qqq.addProperty("repoName","sendgrid/sendgrid-csharp");
        xxx.add(qqq);
        System.out.println(gitHubLeakage.getLeakageCode(xxx));
    }
}
