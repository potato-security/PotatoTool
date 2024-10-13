package com.potato.potatotool.content.redTeam.infoGathering;

import com.google.common.hash.Hashing;
import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;
import com.potato.potatotool.utils.strUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.requestUtils.requests;

/**
 * @author Potato
 * @date 2023/9/28 11:31
 */
public class Utils {

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

    public static Map<String, String> getWebInfo(String url){
        Map<String, String> webInfoMap = new HashMap<>();

        try {
            RequestObj obj = new RequestObj().setUrl(url)
                    .setMethod("GET").setRetries(3).setFollowRedirects(true);

            CustomHttpResponse con = requests(obj);

            int statusCode = con.getResponseCode();
            if (statusCode != 200) {
                return webInfoMap;
            }

            Document doc = con.getDocument();

            String bodyText = doc.body().text();
            String preview = bodyText.length() > 500 ? bodyText.substring(0, 500) : bodyText;
            Element iconElement = doc.select("link[rel~=(?i)^(shortcut icon|icon|apple-touch-icon)$]").first();
            String iconUrl = (iconElement != null && iconElement.hasAttr("href")) ? iconElement.attr("href") : "/favicon.ico";
            if (!iconUrl.startsWith("http")) iconUrl = new URL(new URL(url), iconUrl).toString();
            webInfoMap.put("url", url);
            webInfoMap.put("title", doc.title());
            webInfoMap.put("body", preview);

            try {
                RequestObj obj_icon = new RequestObj().setUrl(iconUrl)
                        .setMethod("GET").setRetries(3);

                CustomHttpResponse con_icon = requests(obj_icon);

                int statusCode_icon = con_icon.getResponseCode();
                if (statusCode_icon != 200) {
                    return webInfoMap;
                }

                byte[] iconBytes = con_icon.getByteArray();
                String iconMd5 = strUtils.md5(iconBytes);

                String tmpData = strUtils.base64Encode_codesc(iconBytes);
                int iconSha1 = Hashing.murmur3_32().hashString(tmpData, StandardCharsets.UTF_8).asInt();

                webInfoMap.put("iconBase64", strUtils.base64Encode(iconBytes));
                webInfoMap.put("iconMd5", iconMd5);
                webInfoMap.put("iconMmh3", String.valueOf(iconSha1));

            }catch (Exception e) {
                if(debugMode) System.out.println(e);
            }

        } catch (Exception e) {
            if(debugMode) System.out.println(e);
        }

        return webInfoMap;
    }

    // 完善拼接URL
    public static String completeUrl(String host) {
        if(host.toLowerCase(Locale.ROOT).startsWith("http")) return host;
        // 拼接 HTTPS 和 HTTP 的URL
        String httpsUrl = "https://" + host;
        String httpUrl = "http://" + host;

        // 先尝试 HTTPS
        if (isReachable(httpsUrl)) {
            return httpsUrl;
        }
        // 如果 HTTPS 失败，尝试 HTTP
        else if (isReachable(httpUrl)) {
            return httpUrl;
        }

        // 两种协议都不可达
        else {
            return null;
        }
    }

    private static boolean isReachable(String urlStr) {
        try {
            RequestObj obj = new RequestObj().setUrl(urlStr)
                    .setMethod("HEAD");
            CustomHttpResponse con = requests(obj);
            int statusCode = con.getResponseCode();
            return (statusCode >= 200 && statusCode < 400); // 2xx 或 3xx 响应码表示服务器正常响应
        } catch (Exception e) {
            return false;  // 连接失败或不可达
        }
    }

    public static void main(String[] args) {
        System.out.println(completeUrl("www.potato.gold"));
        System.out.println(completeUrl("www.potato.gold:3000"));
        Map<String, String> webInfoMap = getWebInfo("https://www.potato.gold");
        System.out.println(webInfoMap);
    }
}
