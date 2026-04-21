package com.potato.potatotool.content.redTeam.infoGathering.utils;

import com.google.common.hash.Hashing;
import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.ProxyUtils;
import com.potato.potatotool.utils.network.RequestObj;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.network.RequestUtils.requests;

/**
 * @author Potato
 * @date 2023/9/28 11:31
 */
public class Utils {

    private static final String NULL_URL_CACHE_VALUE = "__NULL__";
    private static final int COMPLETE_URL_CACHE_LIMIT = 1024;
    private static final int WEB_BASE_CACHE_LIMIT = 512;
    private static final int PAGE_SNAPSHOT_CACHE_LIMIT = 256;
    private static final int ICON_CACHE_LIMIT = 512;

    private static final int WEB_CONNECT_TIMEOUT_SECONDS = 5;
    private static final int WEB_READ_TIMEOUT_SECONDS = 8;
    private static final int WEB_WRITE_TIMEOUT_SECONDS = 8;
    private static final int WEB_CALL_TIMEOUT_SECONDS = 10;
    private static final int WEB_REQUEST_RETRIES = 0;

    private static final int ICON_CONNECT_TIMEOUT_SECONDS = 4;
    private static final int ICON_READ_TIMEOUT_SECONDS = 6;
    private static final int ICON_WRITE_TIMEOUT_SECONDS = 6;
    private static final int ICON_CALL_TIMEOUT_SECONDS = 8;
    private static final int ICON_REQUEST_RETRIES = 0;

    private static final int CRAWL_CONNECT_TIMEOUT_SECONDS = 4;
    private static final int CRAWL_READ_TIMEOUT_SECONDS = 6;
    private static final int CRAWL_WRITE_TIMEOUT_SECONDS = 6;
    private static final int CRAWL_CALL_TIMEOUT_SECONDS = 8;
    private static final int CRAWL_REQUEST_RETRIES = 0;

    private static final Map<String, String> COMPLETE_URL_CACHE = createLruCache(COMPLETE_URL_CACHE_LIMIT);
    private static final Map<String, Map<String, Object>> WEB_BASE_INFO_CACHE = createLruCache(WEB_BASE_CACHE_LIMIT);
    private static final Map<String, PageSnapshot> PAGE_SNAPSHOT_CACHE = createLruCache(PAGE_SNAPSHOT_CACHE_LIMIT);
    private static final Map<String, Map<String, String>> ICON_INFO_CACHE = createLruCache(ICON_CACHE_LIMIT);

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

    public static Map<String, Object> getWebBaseInfo(String url, boolean hasIconUrl, boolean isCrawlProxy){
        Map<String, Object> webBaseInfoMap = new HashMap<>();
        String resolvedUrl = resolveUrl(url, isCrawlProxy);
        if (resolvedUrl == null) {
            return webBaseInfoMap;
        }

        Map<String, Object> cachedBaseInfo = getCachedWebBaseInfo(resolvedUrl, isCrawlProxy);
        if (cachedBaseInfo.isEmpty()) {
            return webBaseInfoMap;
        }

        webBaseInfoMap.putAll(cachedBaseInfo);
        if (!hasIconUrl) {
            webBaseInfoMap.remove("iconUrl");
            return webBaseInfoMap;
        }

        String iconUrl = stringValue(cachedBaseInfo.get("iconUrl"));
        if (!iconUrl.isEmpty()) {
            webBaseInfoMap.putAll(getCachedIconInfo(iconUrl, isCrawlProxy, false));
        }
        return webBaseInfoMap;
    }

    // hasCrawlLinks 默认 true // 是否爬取链接
    // hasFindSensitiveInfo 默认 true // 是否获取敏感信息
    // maxDepth 默认 2; // 设置深度
    // maxSubPathCount 默认 30; // 设置最大子链接数
    // isCrawlProxy // 是否使用代理
    // TODO 指纹识别
    public static Map<String, Object> getWebInfo(String url, boolean hasCrawlLinks, boolean hasFindSensitiveInfo, int maxDepth, int maxSubPathCount, boolean isCrawlProxy){
        Map<String, Object> webInfoMap = new HashMap<>();
        List<Map<String, Object>> allSensitiveInfo = new ArrayList<>();
        Set<String> allInternalLinks = new HashSet<>();
        Set<String> visitedLinks = new HashSet<>();

        String resolvedUrl = resolveUrl(url, isCrawlProxy);
        if (resolvedUrl == null) {
            return webInfoMap;
        }

        if (!hasCrawlLinks && !hasFindSensitiveInfo) {
            Map<String, Object> cachedBaseInfo = getCachedWebBaseInfo(resolvedUrl, isCrawlProxy);
            if (cachedBaseInfo.isEmpty()) {
                return webInfoMap;
            }

            webInfoMap.putAll(cachedBaseInfo);
            String iconUrl = stringValue(cachedBaseInfo.get("iconUrl"));
            if (!iconUrl.isEmpty()) {
                webInfoMap.putAll(getCachedIconInfo(iconUrl, isCrawlProxy, true));
            }
            webInfoMap.put("sensitive", allSensitiveInfo);
            webInfoMap.put("internalLinks", allInternalLinks);
            return webInfoMap;
        }

        PageSnapshot pageSnapshot = getCachedPageSnapshot(resolvedUrl, isCrawlProxy, false);
        if (pageSnapshot == null) {
            return webInfoMap;
        }

        webInfoMap.put("url", resolvedUrl);
        webInfoMap.put("statusCode", pageSnapshot.statusCode);
        webInfoMap.put("title", pageSnapshot.title);
        webInfoMap.put("body", pageSnapshot.preview);
        if (!pageSnapshot.iconUrl.isEmpty()) {
            webInfoMap.put("iconUrl", pageSnapshot.iconUrl);
            webInfoMap.putAll(getCachedIconInfo(pageSnapshot.iconUrl, isCrawlProxy, true));
        }

        // 匹配特定敏感信息
        if(hasFindSensitiveInfo) {
            allSensitiveInfo.add(buildSensitiveInfo(pageSnapshot));
        }

        // 爬取网站内子链接并匹配敏感信息
        if(hasCrawlLinks) {
            List<String> internalLinks = reserveInternalLinks(pageSnapshot.internalLinks, allInternalLinks, maxSubPathCount);

            for (String link : internalLinks) {
                if (!visitedLinks.contains(link)) { // 判断链接是否已访问
                    visitedLinks.add(link); // 将链接添加到已访问集合
                    crawlAndExtract(link, maxDepth - 1, maxSubPathCount, allSensitiveInfo, allInternalLinks, visitedLinks, hasFindSensitiveInfo, isCrawlProxy);
                }
            }
        }

        webInfoMap.put("sensitive", allSensitiveInfo);
        webInfoMap.put("internalLinks", allInternalLinks);

        return webInfoMap;
    }

    // 获取网站图标信息
    private static Map<String, String> getIconInfo(String iconUrl, boolean isCrawlProxy) {
        Map<String, String> iconInfo = new HashMap<>();

        RequestObj obj_icon = new RequestObj()
                .setUrl(iconUrl)
                .setMethod("GET")
                .setRetries(ICON_REQUEST_RETRIES)
                .setFollowRedirects(true)
                .setTimeOut(ICON_CONNECT_TIMEOUT_SECONDS)
                .setReadTimeout(ICON_READ_TIMEOUT_SECONDS)
                .setWriteTimeout(ICON_WRITE_TIMEOUT_SECONDS)
                .setCallTimeout(ICON_CALL_TIMEOUT_SECONDS);
        ProxyUtils.applyProxy(obj_icon, isCrawlProxy);

        try (CustomHttpResponse con_icon = requests(obj_icon)) {
            int statusCode_icon = con_icon.getResponseCode();
            String contentType = con_icon.getContentType();
            if (statusCode_icon != 200 || !contentType.startsWith("image/")) {
                return iconInfo;
            }

            byte[] iconBytes = con_icon.getByteArray();
            String iconMd5 = StrUtils.md5(iconBytes);
            String tmpData = StrUtils.base64Encode_codesc(iconBytes);
            int iconSha1 = Hashing.murmur3_32().hashString(tmpData, StandardCharsets.UTF_8).asInt();

            iconInfo.put("iconUrl", iconUrl);
            iconInfo.put("iconBase64", StrUtils.base64Encode(iconBytes));
            iconInfo.put("iconMd5", iconMd5);
            iconInfo.put("iconMmh3", String.valueOf(iconSha1));
        } catch (Exception e) {
            if (debugMode) e.printStackTrace();
        }
        return iconInfo;
    }

    // 获取网站内的子链接
    public static Set<String> getInternalLinks(Document doc, CustomHttpResponse con) throws Exception {
        return getInternalLinks(doc, con.getURL());
    }

    private static Set<String> getInternalLinks(Document doc, URL baseUrl) throws Exception {
        Set<String> internalLinks = new LinkedHashSet<>();
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
    private static void crawlAndExtract(String url, int depth, int maxSubPathCount, List<Map<String, Object>> allSensitiveInfo, Set<String> allInternalLinks, Set<String> visitedLinks, boolean hasFindSensitiveInfo, boolean isCrawlProxy) {
        if (depth < 0) {
            return;
        }

        PageSnapshot pageSnapshot = getCachedPageSnapshot(url, isCrawlProxy, true);
        if (pageSnapshot == null) {
            return;
        }

        // 匹配特定敏感信息
        if(hasFindSensitiveInfo) {
            allSensitiveInfo.add(buildSensitiveInfo(pageSnapshot));
        }

        if (allInternalLinks.size() >= maxSubPathCount) return; // 达到最大深度，停止爬取
        // 获取网站内的子链接并继续爬取
        List<String> internalLinks = reserveInternalLinks(pageSnapshot.internalLinks, allInternalLinks, maxSubPathCount);
        for (String link : internalLinks) {
            if (!visitedLinks.contains(link)) {
                visitedLinks.add(link);
                crawlAndExtract(link, depth - 1, maxSubPathCount, allSensitiveInfo, allInternalLinks, visitedLinks, hasFindSensitiveInfo, isCrawlProxy);
            }
        }
    }

    // 匹配敏感信息并返回Map
    public static Map<String, Object> findSensitiveInformation(String content, URL url) {
        Map<String, Object> sensitiveInfoMap = new HashMap<>();

        // 定义正则表达式
        String phonePattern = "(?<!\\d)(1[3-9]\\d{9})(?!\\d)";  // 中国手机号
        String idCardPattern = "(?<!\\d)([1-9]\\d{5}[1-9]\\d{3}(0[1-9]|1[0-2])(0[1-9]|[1-2]\\d|3[0-1])\\d{3}[\\dXx])(?!\\d)";  // 身份证号
        String passwordPattern = "(?i)['\"]?(password|pwd|pass|secret|passwd|auth|token)['\"]?\\s*[:=]\\s*['\"][a-zA-Z0-9_\\-!@#$%^&*()+=]{3,64}['\"]";  // 密码
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
    public static String completeUrl(String host, boolean isCrawlProxy) {
        if(host.toLowerCase(Locale.ROOT).startsWith("http")) return host;

        String cacheKey = buildCacheKey("resolve", host, isCrawlProxy);
        String cachedUrl = COMPLETE_URL_CACHE.get(cacheKey);
        if (cachedUrl != null) {
            return NULL_URL_CACHE_VALUE.equals(cachedUrl) ? null : cachedUrl;
        }

        String[] hostParts = host.split(":");
        if (!host.contains("[") && hostParts.length == 2) { // 排除了ipV6 和 没有端口的host
            try {
                int port = Integer.parseInt(hostParts[1]);
                // 排除明显不是 HTTP/HTTPS 的端口
                if (isInvalidHttpPort(port)) {
                    COMPLETE_URL_CACHE.put(cacheKey, NULL_URL_CACHE_VALUE);
                    return null;
                }
            } catch (Exception e) {}
        }

        // 拼接 HTTPS 和 HTTP 的URL
        String httpsUrl = "https://" + host;
        String httpUrl = "http://" + host;

        // 先尝试 HTTPS
        String resUrl_https = isReachableUrl(httpsUrl, isCrawlProxy, true);
        if (resUrl_https != null) {
            COMPLETE_URL_CACHE.put(cacheKey, resUrl_https);
            return resUrl_https;
        }
        // 尝试 HTTP
        String resUrl_http = isReachableUrl(httpUrl, isCrawlProxy, true);
        if (resUrl_http != null) {
            COMPLETE_URL_CACHE.put(cacheKey, resUrl_http);
            return resUrl_http;
        }

        // 都不可达
        COMPLETE_URL_CACHE.put(cacheKey, NULL_URL_CACHE_VALUE);
        return null;
    }

    // 判断是否是无效的 HTTP/HTTPS 端口
    private static boolean isInvalidHttpPort(int port) {
        // 常见 HTTP/HTTPS 默认端口：80 和 443
        // 如果有明确的无效端口规则可以在这里补充
        return port <= 0 || port > 65535 || (port < 1024 && port != 80 && port != 443);
    }

    private static String isReachableUrl(String urlStr, boolean isCrawlProxy, boolean isHttps) {
        RequestObj obj = new RequestObj().setUrl(urlStr)
                .setFollowRedirects(true)
                .setMethod("HEAD")
                .setTimeOut(3)
                .setReadTimeout(3)
                .setWriteTimeout(3)
                .setCallTimeout(6)
                .setRetries(0);
        ProxyUtils.applyProxy(obj, isCrawlProxy);

        try (CustomHttpResponse con = requests(obj)) {
            int statusCode = con.getResponseCode();
            String content = con.getTextStr();
            // 2xx 或 3xx 响应码 并且 不能是burp中间层代错  表示服务器正常响应
            if((statusCode >= 200 && statusCode < 400) && !content.startsWith("<html><head><title>Burp Suite Professional</title>")) {
                return obj.getUrl();    // 返回url，获取SSL降级/升级后的url-可能和传入的urlStr不一致
            }
        } catch (Exception e) {
            return null;  // 连接失败或不可达
        }
        return null;
    }


    private static String joinList(List<String> list) {
        return String.join("\n", list);
    }

    static void clearCachesForTest() {
        COMPLETE_URL_CACHE.clear();
        WEB_BASE_INFO_CACHE.clear();
        PAGE_SNAPSHOT_CACHE.clear();
        ICON_INFO_CACHE.clear();
    }

    private static String resolveUrl(String url, boolean isCrawlProxy) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        if (url.startsWith("http")) {
            return url;
        }
        return completeUrl(url, isCrawlProxy);
    }

    private static Map<String, Object> getCachedWebBaseInfo(String resolvedUrl, boolean isCrawlProxy) {
        String cacheKey = buildCacheKey("base", resolvedUrl, isCrawlProxy);
        Map<String, Object> cached = WEB_BASE_INFO_CACHE.get(cacheKey);
        if (cached != null) {
            return new HashMap<>(cached);
        }

        Map<String, Object> fetched = fetchWebBaseInfo(resolvedUrl, isCrawlProxy);
        WEB_BASE_INFO_CACHE.put(cacheKey, new HashMap<>(fetched));
        return new HashMap<>(fetched);
    }

    private static Map<String, Object> fetchWebBaseInfo(String resolvedUrl, boolean isCrawlProxy) {
        Map<String, Object> webBaseInfoMap = new HashMap<>();

        PageSnapshot pageSnapshot = getCachedPageSnapshot(resolvedUrl, isCrawlProxy, false);
        if (pageSnapshot == null) {
            return webBaseInfoMap;
        }

        cacheResolvedWebBaseInfo(resolvedUrl, isCrawlProxy, pageSnapshot.statusCode, pageSnapshot.title, pageSnapshot.preview, pageSnapshot.iconUrl);
        webBaseInfoMap.put("url", resolvedUrl);
        webBaseInfoMap.put("statusCode", pageSnapshot.statusCode);
        webBaseInfoMap.put("title", pageSnapshot.title);
        webBaseInfoMap.put("body", pageSnapshot.preview);
        if (!pageSnapshot.iconUrl.isEmpty()) {
            webBaseInfoMap.put("iconUrl", pageSnapshot.iconUrl);
        }

        return webBaseInfoMap;
    }

    private static PageSnapshot getCachedPageSnapshot(String resolvedUrl, boolean isCrawlProxy, boolean useCrawlRequest) {
        String cacheKey = buildCacheKey("page", resolvedUrl, isCrawlProxy);
        PageSnapshot cached = PAGE_SNAPSHOT_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        PageSnapshot snapshot = fetchPageSnapshot(resolvedUrl, isCrawlProxy, useCrawlRequest);
        if (snapshot != null) {
            PAGE_SNAPSHOT_CACHE.put(cacheKey, snapshot);
            return snapshot;
        }
        return null;
    }

    private static PageSnapshot fetchPageSnapshot(String resolvedUrl, boolean isCrawlProxy, boolean useCrawlRequest) {
        RequestObj obj = useCrawlRequest ? createCrawlRequest(resolvedUrl, isCrawlProxy) : createWebRequest(resolvedUrl, isCrawlProxy);

        try (CustomHttpResponse con = requests(obj)) {
            int statusCode = con.getResponseCode();
            if (statusCode != 200) {
                return null;
            }

            Document doc = con.getDocument();
            if (doc == null || doc.body() == null) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            for (Element element : doc.body().children()) {
                String tmpStr = element.text();
                if (!tmpStr.isEmpty()) {
                    sb.append(tmpStr).append("\n");
                }
            }

            String bodyText = doc.body().text();
            String preview = bodyText.length() > 500 ? bodyText.substring(0, 500) : bodyText;
            String iconUrl = extractIconUrl(doc, resolvedUrl);
            Set<String> internalLinks = getInternalLinks(doc, con.getURL());
            return new PageSnapshot(statusCode, doc.title(), preview, sb.toString(), iconUrl, con.getURL(), internalLinks);
        } catch (Exception e) {
            if(debugMode) e.printStackTrace();
        }
        return null;
    }

    private static void cacheResolvedWebBaseInfo(String resolvedUrl, boolean isCrawlProxy, int statusCode, String title, String preview, String iconUrl) {
        Map<String, Object> baseInfo = new HashMap<>();
        baseInfo.put("url", resolvedUrl);
        baseInfo.put("statusCode", statusCode);
        baseInfo.put("title", title);
        baseInfo.put("body", preview);
        if (iconUrl != null && !iconUrl.isEmpty()) {
            baseInfo.put("iconUrl", iconUrl);
        }
        WEB_BASE_INFO_CACHE.put(buildCacheKey("base", resolvedUrl, isCrawlProxy), new HashMap<>(baseInfo));
    }

    private static Map<String, String> getCachedIconInfo(String iconUrl, boolean isCrawlProxy, boolean includeBase64) {
        String cacheKey = buildCacheKey("icon", iconUrl, isCrawlProxy);
        Map<String, String> cached = ICON_INFO_CACHE.get(cacheKey);
        if (cached == null) {
            cached = getIconInfo(iconUrl, isCrawlProxy);
            ICON_INFO_CACHE.put(cacheKey, new HashMap<>(cached));
        }

        Map<String, String> result = new HashMap<>(cached);
        if (!includeBase64) {
            result.remove("iconBase64");
        }
        return result;
    }

    private static Map<String, Object> buildSensitiveInfo(PageSnapshot pageSnapshot) {
        return findSensitiveInformation(pageSnapshot.pageContent, pageSnapshot.responseUrl);
    }

    private static List<String> reserveInternalLinks(Collection<String> links, Set<String> allInternalLinks, int maxSubPathCount) {
        List<String> reservedLinks = new ArrayList<>();
        if (links == null || links.isEmpty() || maxSubPathCount <= 0) {
            return reservedLinks;
        }

        for (String link : links) {
            if (allInternalLinks.size() >= maxSubPathCount) {
                break;
            }
            if (allInternalLinks.add(link)) {
                reservedLinks.add(link);
            }
        }
        return reservedLinks;
    }

    private static RequestObj createWebRequest(String url, boolean isCrawlProxy) {
        RequestObj obj = new RequestObj()
                .setUrl(url)
                .setMethod("GET")
                .setRetries(WEB_REQUEST_RETRIES)
                .setFollowRedirects(true)
                .setTimeOut(WEB_CONNECT_TIMEOUT_SECONDS)
                .setReadTimeout(WEB_READ_TIMEOUT_SECONDS)
                .setWriteTimeout(WEB_WRITE_TIMEOUT_SECONDS)
                .setCallTimeout(WEB_CALL_TIMEOUT_SECONDS);
        ProxyUtils.applyProxy(obj, isCrawlProxy);
        return obj;
    }

    private static RequestObj createCrawlRequest(String url, boolean isCrawlProxy) {
        RequestObj obj = new RequestObj()
                .setUrl(url)
                .setMethod("GET")
                .setRetries(CRAWL_REQUEST_RETRIES)
                .setFollowRedirects(true)
                .setTimeOut(CRAWL_CONNECT_TIMEOUT_SECONDS)
                .setReadTimeout(CRAWL_READ_TIMEOUT_SECONDS)
                .setWriteTimeout(CRAWL_WRITE_TIMEOUT_SECONDS)
                .setCallTimeout(CRAWL_CALL_TIMEOUT_SECONDS);
        ProxyUtils.applyProxy(obj, isCrawlProxy);
        return obj;
    }

    private static String extractIconUrl(Document doc, String url) throws Exception {
        Element iconElement = doc.select("link[rel~=(?i)^(shortcut icon|icon|apple-touch-icon)$]").first();
        String iconUrl = (iconElement != null && iconElement.hasAttr("href")) ? iconElement.attr("href") : "/favicon.ico";
        if (!iconUrl.startsWith("http")) {
            iconUrl = new URL(new URL(url), iconUrl).toString();
        }
        return iconUrl;
    }

    private static String buildCacheKey(String prefix, String value, boolean isCrawlProxy) {
        return prefix + "|" + (isCrawlProxy ? "1|" : "0|") + value;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private static <K, V> Map<K, V> createLruCache(final int maxSize) {
        return Collections.synchronizedMap(new LinkedHashMap<K, V>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxSize;
            }
        });
    }

    private static boolean isEmpty(String... values) {
        for (String value : values) {
            if (!value.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static final class PageSnapshot {
        private final int statusCode;
        private final String title;
        private final String preview;
        private final String pageContent;
        private final String iconUrl;
        private final URL responseUrl;
        private final Set<String> internalLinks;

        private PageSnapshot(int statusCode,
                             String title,
                             String preview,
                             String pageContent,
                             String iconUrl,
                             URL responseUrl,
                             Set<String> internalLinks) {
            this.statusCode = statusCode;
            this.title = title == null ? "" : title;
            this.preview = preview == null ? "" : preview;
            this.pageContent = pageContent == null ? "" : pageContent;
            this.iconUrl = iconUrl == null ? "" : iconUrl;
            this.responseUrl = responseUrl;
            this.internalLinks = internalLinks == null ? Collections.<String>emptySet() : new LinkedHashSet<>(internalLinks);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println(getWebInfo("potato.gold:3000",false,false,2,2, false));
        System.out.println(getWebInfo("potato.gold",false,false,2,2, false));
        System.out.println(completeUrl("potato.gold:3000", true));
        System.out.println(completeUrl("potato.gold:3000", false));
        System.out.println(completeUrl("potato.gold", true));
        System.out.println(completeUrl("potato.gold", false));
        System.out.println(completeUrl("self-signed.badssl.com", true));
        System.out.println(completeUrl("self-signed.badssl.com", false));
        System.out.println(completeUrl("wrong.host.badssl.com", true));
        System.out.println(completeUrl("wrong.host.badssl.com", false));
//        System.out.println(findSensitiveInformation("手机号测试：18666677777\n" +
//                "身份证号：441400198203221497\n" +
//                "`password='1433223'`\n" +
//                "`\"password\":\"a132a3a3\"`\n" +
//                "IP：152.23.24.25\n" +
//                "内网IP：192.168.5.17\n" +
//                "邮箱：TestEmail@126.com\n" +
//                "`access_key=\"potatoTestData\"`", new URL("http://www.baidu.com")));
    }
}
