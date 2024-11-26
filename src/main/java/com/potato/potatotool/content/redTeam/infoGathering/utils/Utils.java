package com.potato.potatotool.content.redTeam.infoGathering.utils;

import com.google.common.hash.Hashing;
import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;
import com.potato.potatotool.utils.strUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.requestUtils.requests;

/**
 * @author Potato
 * @date 2023/9/28 11:31
 */
public class Utils {

    // 正则表达式匹配IP地址
    private static final Pattern IP_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    // 正则表达式匹配域名
    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
            "^(?!-)([A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,63}$"
    );

    // 判断是否为IP地址
    public static boolean isIPAddress(String input) {
        return IP_PATTERN.matcher(input).matches();
    }

    // 判断是否为域名
    public static boolean isDomainName(String input) {
        return DOMAIN_PATTERN.matcher(input).matches();
    }

    public static Elements getElements(Document doc, String selector) {
        return doc.select(selector);
    }

    public static Elements getElements(Element element, String selector) {
        return element.select(selector);
    }

    public static String getElementText(Document doc, String selector) {
        Elements elements = doc.select(selector);
        return elements.isEmpty() ? "" : elements.first().text().trim();
    }

    public static String getElementText(Element element, String selector) {
        Elements elements = element.select(selector);
        return elements.isEmpty() ? "" : elements.first().text().trim();
    }

    public static String getElementAttr(Document doc, String selector, String attr){
        Elements elements = doc.select(selector);
        return elements.isEmpty() ? "" : elements.first().attr(attr).trim();
    }

    public static String getElementAttr(Element element, String selector, String attr){
        Elements elements = element.select(selector);
        return elements.isEmpty() ? "" : elements.first().attr(attr).trim();
    }

    public static Map<String, Object> getWebBaseInfo(String url, boolean hasIconUrl){
        Map<String, Object> webBaseInfoMap = new HashMap<>();

        if(!url.startsWith("http")) {
            url = completeUrl(url);
            if(url==null) return webBaseInfoMap;
        }

        try {
            RequestObj obj = new RequestObj().setUrl(url)
                    .setMethod("GET").setRetries(3).setFollowRedirects(true);

            CustomHttpResponse con = requests(obj);

            int statusCode = con.getResponseCode();
            if (statusCode != 200) {
                return webBaseInfoMap;
            }

            Document doc = con.getDocument();

            String bodyText = doc.body().text();
            String preview = bodyText.length() > 500 ? bodyText.substring(0, 500) : bodyText;

            if(hasIconUrl) {
                Element iconElement = doc.select("link[rel~=(?i)^(shortcut icon|icon|apple-touch-icon)$]").first();
                String iconUrl = (iconElement != null && iconElement.hasAttr("href")) ? iconElement.attr("href") : "/favicon.ico";
                if (!iconUrl.startsWith("http")) iconUrl = new URL(new URL(url), iconUrl).toString();
                webBaseInfoMap.put("iconUrl", iconUrl);
            }

            webBaseInfoMap.put("url", url);
            webBaseInfoMap.put("title", doc.title());
            webBaseInfoMap.put("body", preview);
        }catch (Exception e){
            if(debugMode) e.printStackTrace();
        }
        return webBaseInfoMap;
    }

    // hasCrawlLinks 默认 true // 是否爬取链接
    // hasFindSensitiveInfo 默认 true // 是否获取敏感信息
    // maxDepth 默认 2; // 设置深度
    // maxSubPathCount 默认 30; // 设置最大子链接数
    // TODO 指纹识别
    public static Map<String, Object> getWebInfo(String url, boolean hasCrawlLinks, boolean hasFindSensitiveInfo, int maxDepth, int maxSubPathCount){
        Map<String, Object> webInfoMap = new HashMap<>();
        List<Map<String, Object>> allSensitiveInfo = new ArrayList<>();
        Set<String> allInternalLinks = new HashSet<>();
        Set<String> visitedLinks = new HashSet<>();

        if(!url.startsWith("http")) {
            url = completeUrl(url);
            if(url==null) return webInfoMap;
        }

        try {
            RequestObj obj = new RequestObj().setUrl(url)
                    .setMethod("GET").setRetries(3).setFollowRedirects(true);

            CustomHttpResponse con = requests(obj);

            int statusCode = con.getResponseCode();
            if (statusCode != 200) {
                return webInfoMap;
            }

            Document doc = con.getDocument();

            StringBuilder sb = new StringBuilder();
            for (Element element : doc.body().children()) {
                String tmpStr = element.text();
                if(!tmpStr.isEmpty()) sb.append(tmpStr).append("\n"); // 每个元素文本后添加换行符
            }
            String pageContent = sb.toString();

            String bodyText = doc.body().text();
            String preview = bodyText.length() > 500 ? bodyText.substring(0, 500) : bodyText;
            Element iconElement = doc.select("link[rel~=(?i)^(shortcut icon|icon|apple-touch-icon)$]").first();
            String iconUrl = (iconElement != null && iconElement.hasAttr("href")) ? iconElement.attr("href") : "/favicon.ico";
            if (!iconUrl.startsWith("http")) iconUrl = new URL(new URL(url), iconUrl).toString();
            webInfoMap.put("url", url);
            webInfoMap.put("title", doc.title());
            webInfoMap.put("body", preview);
            webInfoMap.putAll(getIconInfo(iconUrl));

            // 匹配特定敏感信息
            if(hasFindSensitiveInfo) {
                Map<String, Object> sensitiveInfoMap = findSensitiveInformation(pageContent, con.getURL());
                allSensitiveInfo.add(sensitiveInfoMap);
            }

            // 爬取网站内子链接并匹配敏感信息
            if(hasCrawlLinks) {
                Set<String> internalLinks = getInternalLinks(doc, con);
                allInternalLinks.addAll(internalLinks);

                for (String link : internalLinks) {
                    if (!visitedLinks.contains(link)) { // 判断链接是否已访问
                        visitedLinks.add(link); // 将链接添加到已访问集合
                        crawlAndExtract(link, maxDepth - 1, maxSubPathCount, allSensitiveInfo, allInternalLinks, visitedLinks, hasFindSensitiveInfo);
                    }
                }
            }

        } catch (Exception e) {
            if(debugMode) e.printStackTrace();
        }

        webInfoMap.put("sensitive", allSensitiveInfo);
        webInfoMap.put("internalLinks", allInternalLinks);

        return webInfoMap;
    }

    // 获取网站图标信息
    private static Map<String, String> getIconInfo(String iconUrl) {
        Map<String, String> iconInfo = new HashMap<>();
        try {
            RequestObj obj_icon = new RequestObj().setUrl(iconUrl)
                    .setMethod("GET").setRetries(3);

            CustomHttpResponse con_icon = requests(obj_icon);
            int statusCode_icon = con_icon.getResponseCode();
            String contentType = con_icon.getContentType();
            if (statusCode_icon != 200 || !contentType.startsWith("image/")) {
                return iconInfo;
            }

            byte[] iconBytes = con_icon.getByteArray();
            String iconMd5 = strUtils.md5(iconBytes);
            String tmpData = strUtils.base64Encode_codesc(iconBytes);
            int iconSha1 = Hashing.murmur3_32().hashString(tmpData, StandardCharsets.UTF_8).asInt();

            iconInfo.put("iconUrl", iconUrl);
            iconInfo.put("iconBase64", strUtils.base64Encode(iconBytes));
            iconInfo.put("iconMd5", iconMd5);
            iconInfo.put("iconMmh3", String.valueOf(iconSha1));
        } catch (Exception e) {
            if (debugMode) System.out.println(e);
        }
        return iconInfo;
    }

    // 获取网站内的子链接
    public static Set<String> getInternalLinks(Document doc, CustomHttpResponse con) throws Exception {
        Set<String> internalLinks = new HashSet<>();
        URL baseUrl = con.getURL();
        String baseUrlStr = baseUrl.getProtocol() + "://" + baseUrl.getHost() + (baseUrl.getPort()!= -1? ":" + baseUrl.getPort() : "");
        if (baseUrl.getPath()!= null && baseUrl.getPath().length() > 0) {
            int lastIndex = baseUrl.getPath().lastIndexOf('/');
            if (lastIndex!= -1) {
                baseUrlStr += baseUrl.getPath().substring(0, lastIndex);
            } else {
                baseUrlStr += baseUrl.getPath();
            }
        }

        // 解析页面中的所有链接
        Elements links = doc.select("[href], [src]");
        for (Element link : links) {
            String linkUrl = link.attr("abs:href");

            // 如果 href 为空，再尝试从 src 属性获取 URL
            if (linkUrl.isEmpty()) {
                linkUrl = link.attr("abs:src");
            }

            if (linkUrl.isEmpty()) {
                linkUrl = link.hasAttr("href") ? link.attr("href") : link.attr("src");
                if (!linkUrl.isEmpty() && !linkUrl.startsWith("http")) {
                    linkUrl = new URL(new URL(baseUrlStr), linkUrl).toString();
                }
            }

            // 只保留当前域名下的链接
            if (!linkUrl.isEmpty() && isInternalLink(baseUrl, linkUrl) && isTextBasedLink(linkUrl)) {
                internalLinks.add(linkUrl);
            }
        }

        return internalLinks;
    }

    // 判断链接是否为文本文件链接
    private static boolean isTextBasedLink(String url) {
        // 定义允许的扩展名
        String[] textExtensions = {
                ".txt",      // 文本文件
                ".html",     // HTML 文件
                ".htm",      // HTML 文件
                ".jsp",      // Java Server Pages
                ".json",     // JSON 文件
                ".xml",      // XML 文件
                ".csv",      // 逗号分隔值文件
                ".log",      // 日志文件
                ".md",       // Markdown 文件
                ".php",      // PHP 文件
                ".asp",      // ASP 文件
                ".py",       // Python 文件
                ".rb",       // Ruby 文件
                ".pl",       // Perl 文件
                ".sh",       // Shell 脚本
                ".js",       // JavaScript 文件
                ".css",      // CSS 文件
                ".conf",     // 配置文件
                ".yaml",     // YAML 文件
                ".yml",      // YAML 文件
                ".txt.bak",  // 备份文本文件
                ".config",   // 配置文件
                ".txt.bat",  // 批处理文件
                ".cmd",      // Windows 命令文件
                ".json5",    // JSON5 文件
                ".env",      // 环境变量文件
                ".ini",      // 初始化文件
                ".info",     // 信息文件
                ".template",  // 模板文件
                ".key",      // 密钥文件
                ".pem",      // PEM 格式文件
                ".crt",      // 证书文件
                ".cer",      // 证书文件
                ".txt.tmp",  // 临时文本文件
                ".backup",   // 备份文件
                ".orig",     // 原始文件
        };
        // 检查链接是否以特定扩展名结束
        for (String extension : textExtensions) {
            if (url.toLowerCase().endsWith(extension)) {
                return true;
            }
        }

        return false;
    }

    // 判断链接是否为当前域名的链接
    private static boolean isInternalLink(URL baseUrl, String linkUrl) {
        try {
            URL link = new URL(linkUrl);
            return baseUrl.getHost().equals(link.getHost()); // 只比较域名
        } catch (Exception e) {
            return false; // 如果URL无效，则视为不匹配
        }
    }

    // 递归爬取链接并提取信息
    private static void crawlAndExtract(String url, int depth, int maxSubPathCount, List<Map<String, Object>> allSensitiveInfo, Set<String> allInternalLinks, Set<String> visitedLinks, boolean hasFindSensitiveInfo) {

        try {
            RequestObj obj = new RequestObj().setUrl(url)
                    .setMethod("GET").setRetries(3).setFollowRedirects(true);
            CustomHttpResponse con = requests(obj);

            int statusCode = con.getResponseCode();
            if (statusCode != 200) {
                return;
            }

            Document doc = con.getDocument();

            StringBuilder sb = new StringBuilder();
            for (Element element : doc.body().children()) {
                String tmpStr = element.text();
                if(!tmpStr.isEmpty()) sb.append(tmpStr).append("\n"); // 每个元素文本后添加换行符
            }
            String pageContent = sb.toString();

            // 匹配特定敏感信息
            if(hasFindSensitiveInfo) {
                Map<String, Object> sensitiveInfoMap = findSensitiveInformation(pageContent, con.getURL());
                allSensitiveInfo.add(sensitiveInfoMap);
            }

            if (depth < 0 || allInternalLinks.size() > maxSubPathCount) return; // 达到最大深度，停止爬取
            // 获取网站内的子链接并继续爬取
            Set<String> internalLinks = getInternalLinks(doc, con);
            allInternalLinks.addAll(internalLinks);
            for (String link : internalLinks) {
                if (!visitedLinks.contains(link)) {
                    visitedLinks.add(link);
                    crawlAndExtract(link, depth - 1, maxSubPathCount, allSensitiveInfo, allInternalLinks, visitedLinks, hasFindSensitiveInfo);
                }
            }

        } catch (Exception e) {
            if (debugMode) System.out.println(e);
        }
    }

    // 匹配敏感信息并返回Map
    public static Map<String, Object> findSensitiveInformation(String content, URL url) {
        Map<String, Object> sensitiveInfoMap = new HashMap<>();

        // 定义正则表达式
        String phonePattern = "(?<!\\d)(1[3-9]\\d{9})(?!\\d)";  // 中国手机号
        String idCardPattern = "(?<!\\d)([1-9]\\d{5}[1-9]\\d{3}(0[1-9]|1[0-2])(0[1-9]|[1-2]\\d|3[0-1])\\d{3}[\\dXx])(?!\\d)";  // 身份证号
        String passwordPattern = "(?i)['\"]?(password|pwd|pass|secret|passwd|auth|token)['\"]?\\s*[:=]\\s*['\\\"][a-zA-Z0-9_\\-!@#$%^&*()+=]{3,64}['\\\"]";  // 密码
        String ipPattern = "(?<!\\d)((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)(?!\\d)|(?:(?:(?:[0-9A-Fa-f]{1,4}:){7}[0-9A-Fa-f]{1,4})|(?:(?:[0-9A-Fa-f]{1,4}:){6}:[0-9A-Fa-f]{1,4})|(?:(?:[0-9A-Fa-f]{1,4}:){5}(?::[0-9A-Fa-f]{1,4}){1,2})|(?:(?:[0-9A-Fa-f]{1,4}:){4}(?::[0-9A-Fa-f]{1,4}){1,3})|(?:(?:[0-9A-Fa-f]{1,4}:){3}(?::[0-9A-Fa-f]{1,4}){1,4})|(?:(?:[0-9A-Fa-f]{1,4}:){2}(?::[0-9A-Fa-f]{1,4}){1,5})|(?:(?:[0-9A-Fa-f]{1,4}:){1}(?::[0-9A-Fa-f]{1,4}){1,6})|(?::(?::[0-9A-Fa-f]{1,4}){1,7})|(?:[Ff]{4}(?::0{1,4}){0,2}))\\b";  // IP
        String internalIpPattern = "(?<!\\d)(10\\.(?:[0-9]{1,2}|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.(?:[0-9]{1,2}|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.(?:[0-9]{1,2}|1[0-9]{2}|2[0-4][0-9]|25[0-5]))(?!\\d)|" +
                "(?<!\\d)(172\\.(?:1[6-9]|2[0-9]|3[0-1])\\.(?:[0-9]{1,2}|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.(?:[0-9]{1,2}|1[0-9]{2}|2[0-4][0-9]|25[0-5]))(?!\\d)|" +
                "(?<!\\d)(192\\.168\\.(?:[0-9]{1,2}|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.(?:[0-9]{1,2}|1[0-9]{2}|2[0-4][0-9]|25[0-5]))(?!\\d)|" +
                "(?<!\\d)(127\\.(?:[0-9]{1,2}|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.(?:[0-9]{1,2}|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.(?:[0-9]{1,2}|1[0-9]{2}|2[0-4][0-9]|25[0-5]))(?!\\d)";  // 内网IP
        String emailPattern = "(([a-zA-Z0-9][_|\\.])*[a-zA-Z0-9]+@([a-zA-Z0-9][-|_|\\.])*[a-zA-Z0-9]+\\.((?!js|css|jpg|jpeg|png|ico)[a-zA-Z]{2,}))";  // 邮箱
        String keyPattern = "(?i)((access_key|access_token|admin_pass|admin_user|algolia_admin_key|algolia_api_key|alias_pass|alicloud_access_key|amazon_secret_access_key|amazonaws|ansible_vault_password|aos_key|api_key|api_key_secret|api_key_sid|api_secret|api.googlemaps AIza|apidocs|apikey|apiSecret|app_debug|app_id|app_key|app_log_level|app_secret|appkey|appkeysecret|application_key|appsecret|appspot|auth_token|authorizationToken|authsecret|aws_access|aws_access_key_id|aws_bucket|aws_key|aws_secret|aws_secret_key|aws_token|AWSSecretKey|b2_app_key|bashrc password|bintray_apikey|bintray_gpg_password|bintray_key|bintraykey|bluemix_api_key|bluemix_pass|browserstack_access_key|bucket_password|bucketeer_aws_access_key_id|bucketeer_aws_secret_access_key|built_branch_deploy_key|bx_password|cache_driver|cache_s3_secret_key|cattle_access_key|cattle_secret_key|certificate_password|ci_deploy_password|client_secret|client_zpk_secret_key|clojars_password|cloud_api_key|cloud_watch_aws_access_key|cloudant_password|cloudflare_api_key|cloudflare_auth_key|cloudinary_api_secret|cloudinary_name|codecov_token|config|conn.login|connectionstring|consumer_key|consumer_secret|credentials|cypress_record_key|database_password|database_schema_test|datadog_api_key|datadog_app_key|db_password|db_server|db_username|dbpasswd|dbpassword|dbuser|deploy_password|digitalocean_ssh_key_body|digitalocean_ssh_key_ids|docker_hub_password|docker_key|docker_pass|docker_passwd|docker_password|dockerhub_password|dockerhubpassword|dot-files|dotfiles|droplet_travis_password|dynamoaccesskeyid|dynamosecretaccesskey|elastica_host|elastica_port|elasticsearch_password|encryption_key|encryption_password|env.heroku_api_key|env.sonatype_password|eureka.awssecretkey)[a-z0-9_ .\\-,]{0,25})(=|>|:=|\\|\\|:|<=|=>|:).{0,5}['\\\"]([0-9a-zA-Z\\-_=]{8,64})['\\\"]|['\"]?\\b\\w+['\"]?\\s*[=:]\\s*['\"](AKIA[A-Za-z0-9]{16}|GOOG[\\w!@#$%^&*()\\-+=~]{10,30}|AZ[A-Za-z0-9]{34,40}|IBM[A-Za-z0-9]{10,40}|[a-zA-Z0-9]{8}(-[a-zA-Z0-9]{4}){3}-[a-zA-Z0-9]{12}|OCID[A-Za-z0-9]{10,40}|LTAI[A-Za-z0-9]{12,20}|AKID[A-Za-z0-9]{13,20}|AK[A-Za-z0-9]{10,40}|JDC_[A-Z0-9]{28,32}|AKLT[a-zA-Z0-9-_]{0,252}|UC[A-Za-z0-9]{10,40}|QY[A-Za-z0-9]{10,40}|AKLT[a-zA-Z0-9-_]{16,28}|LTC[A-Za-z0-9]{10,60}|YD[A-Za-z0-9]{10,60}|CTC[A-Za-z0-9]{10,60}|YYT[A-Za-z0-9]{10,60}|YY[A-Za-z0-9]{10,40}|CI[A-Za-z0-9]{10,40}|gcore[A-Za-z0-9]{10,30})['\"]";  // OAuth令牌

        // 匹配并存储结果
        sensitiveInfoMap.put("Url", url.toString());
        sensitiveInfoMap.put("PhoneNumber", matchPattern(content, phonePattern));
        sensitiveInfoMap.put("IdCard", matchPattern(content, idCardPattern));
        sensitiveInfoMap.put("Password", matchPattern(content, passwordPattern));
        sensitiveInfoMap.put("IP", matchPattern(content, ipPattern));
        sensitiveInfoMap.put("InternalIP", matchPattern(content, internalIpPattern));
        sensitiveInfoMap.put("Email", matchPattern(content, emailPattern));
        sensitiveInfoMap.put("Key", matchPattern(content, keyPattern));

        return sensitiveInfoMap;
    }

    // 匹配正则表达式并将结果存入Map
    private static List<String> matchPattern(String content, String pattern) {
        Set<String> result = new HashSet<>();
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(content);

        while (matcher.find()) {
            String matched = matcher.group();
            result.add(matched);
        }
        return new ArrayList<>(result);
    }

    // 完善拼接URL
    public static String completeUrl(String host) {
        if(host.toLowerCase(Locale.ROOT).startsWith("http")) return host;
        // 拼接 HTTPS 和 HTTP 的URL
        String httpsUrl = "https://" + host;
        String httpUrl = "http://" + host;

        // 先尝试 HTTPS
        if (isReachable(httpsUrl, true)) {
            return httpsUrl;
        }
        // 如果 HTTPS 失败，尝试 HTTP
        else if (isReachable(httpUrl, false)) {
            return httpUrl;
        }

        // 两种协议都不可达
        else {
            return null;
        }
    }

    private static boolean isReachable(String urlStr, boolean isHttps) {
        try {
            RequestObj obj = new RequestObj().setUrl(urlStr)
                    .setFollowRedirects(true)
                    .setStrictSslValidation(isHttps)
                    .setMethod("GET");// 建议使用HEAD，但是部分网站单独设置不允许HEAD请求
            CustomHttpResponse con = requests(obj);
            int statusCode = con.getResponseCode();
            String content = con.getTextStr();
            // 2xx 或 3xx 响应码 并且 不能是burp中间层代错  表示服务器正常响应
            return (statusCode >= 200 && statusCode < 400) && !content.startsWith("<html><head><title>Burp Suite Professional</title>");
        } catch (Exception e) {
            return false;  // 连接失败或不可达
        }
    }


    private static String joinList(List<String> list) {
        return String.join("\n", list);
    }

    private static boolean isEmpty(String... values) {
        for (String value : values) {
            if (!value.isEmpty()) {
                return false;
            }
        }
        return true;
    }


    public static void main(String[] args) {
        System.out.println(getWebInfo("potato.gold:3000",false,false,2,2));
        System.out.println(getWebInfo("potato.gold",false,false,2,2));
    }
}
