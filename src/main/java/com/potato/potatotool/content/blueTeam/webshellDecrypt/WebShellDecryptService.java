package com.potato.potatotool.content.blueTeam.webshellDecrypt;

import com.potato.potatotool.content.blueTeam.webshellDecrypt.parser.HttpRequestParser;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.parser.RequestParseResult;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.ContentDecoder;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.matcher.WebShellMatcher;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.model.DecryptResult;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.model.DecryptConfig;
import com.potato.potatotool.utils.crypto.SecurityInitializer;
import org.json.JSONObject;

import java.util.*;

/**
 * WebShell解密服务主类
 *
 * @author Potato
 * @date 2023/4/30 09:00
 * @version 2.0
 */
public class WebShellDecryptService {
    
    private final HttpRequestParser requestParser;
    private final ContentDecoder contentDecoder;
    private final WebShellMatcher webShellMatcher;
    private final List<List<String>> encodeModes;
    
    public WebShellDecryptService() {
        this.requestParser = new HttpRequestParser();
        this.contentDecoder = new ContentDecoder();
        this.webShellMatcher = new WebShellMatcher();
        this.encodeModes = new ArrayList<>();
    }
    
    /**
     * 处理HTTP请求体/响应体解密
     * 与原dealBody方法逻辑完全一致
     * 
     * @param content 待解密的内容
     * @param config 解密配置
     * @return 解密结果
     */
    public DecryptResult decryptContent(String content, DecryptConfig config) {
        if (content == null || content.trim().isEmpty()) {
            return DecryptResult.error("解密的内容不可为空");
        }
        
        try {
            encodeModes.clear();
            String resultData = "";

            // 1. 特殊处理头部->Shiro Cookie
            if (content.contains("Cookie: rememberMe=") && content.contains("Host")) {
                String rememberMeValue = requestParser.extractShiroRememberMe(content);
                if (!rememberMeValue.isEmpty()) {
                    //   尽可能筛除非攻击shiro-> && rememberMeValue.length() > 20
                    String tmpResultData = contentDecoder.decode(rememberMeValue, config, encodeModes);
                    if (!tmpResultData.equals(rememberMeValue)) {
                        resultData = content.replace(rememberMeValue, tmpResultData);
                    }
                }
            }

            // 2. 解析HTTP请求并提取数据
            RequestParseResult parseResult = requestParser.extractAndParseRequest(content);
            String postData = parseResult.getExtractedData();
            JSONObject jsonData = parseResult.getJsonData();

            // 3. 处理POST/GET数据
            if (!parseResult.hasJsonData()) {
                // 单value或单key+value情况
                String tmpResultData = contentDecoder.decode(postData, config, encodeModes);
                if (!tmpResultData.equals(postData)) {
                    resultData = content.replace(postData, tmpResultData);
                }
            } else {
                // 存在多个key+value情况，逐个解密
                String newPostData = postData;
                boolean hasDecrypted = false;

                for (String key : jsonData.keySet()) {
                    String value = jsonData.getString(key);
                    String tmpResultData = contentDecoder.decode(value, config, encodeModes);
                    if (!tmpResultData.isEmpty() && !tmpResultData.equals(value)) {
                        newPostData = newPostData.replace(value, tmpResultData);
                        hasDecrypted = true;
                    }
                }

                if (hasDecrypted) {
                    resultData = content.replace(postData, newPostData);
                }
            }
            
            // 4. 检查是否解密成功，失败跳出
            if (encodeModes.isEmpty()) {
                return handleDecryptFailure(config);
            }
            
            // 5. 进行WebShell特征匹配
            webShellMatcher.matchWebShellFeatures(content, resultData, encodeModes, config.getInputKey());
            
            // 6. 构建成功结果
            return DecryptResult.success(resultData, encodeModes);
            
        } catch (Exception e) {
            return DecryptResult.error("解密过程中发生异常: " + e.getMessage());
        }
    }
    
    /**
     * 处理解密失败的情况
     */
    private DecryptResult handleDecryptFailure(DecryptConfig config) {
        if (config.getTraverseList().isEmpty()) {
            return DecryptResult.error("探测加密方式失败，请使用专项解密\\AES爆破");
        } else {
            return DecryptResult.error("探测加密方式失败，请留言评论提供更多信息，让我们一起优化程序");
        }
    }
    
    /**
     * 快速解密方法，使用默认配置
     */
    public DecryptResult quickDecrypt(String content) {
        return decryptContent(content, DecryptConfig.defaultConfig());
    }
    
    /**
     * 使用自定义密钥解密
     */
    public DecryptResult decryptWithCustomKey(String content, String key, String iv) {
        DecryptConfig config = DecryptConfig.builder()
                .inputKey(key)
                .inputIv(iv)
                .build();
        return decryptContent(content, config);
    }
    
    /**
     * 使用字典爆破解密
     */
    public DecryptResult decryptWithDictionary(String content, List<String> traverseList, String customPath) {
        DecryptConfig config = DecryptConfig.builder()
                .traverseList(traverseList)
                .customPath(customPath)
                .build();
        return decryptContent(content, config);
    }

    public static void main(String[] args) {
        // 非项目启动调用，需要单独初始化安全证书套件
        SecurityInitializer.initializeSecurityProvider();
        DecryptConfig config = DecryptConfig.builder()
                .build();
        WebShellDecryptService decryptContent = new WebShellDecryptService();
        String content = "pass=fL1tMGI4YTljO/79NDQm7r9PZzBiOA%3D%3D&orderid=0mQU%2BS1pFnTz3ttVTnAgJf4rvU9E3tQySxwinpW%2F0fAQrVXMjQo9j5ZOKitj8eSk6AsEf1uVaNfGq0Q584SlVfSSQO824oYh0qWY81PjflvpzffSw4%2F%2BNENDkTrxoonglOaOKQNIfNs%2FM%2BdSgSeKGA%3D%3D";
        DecryptResult sss = decryptContent.decryptContent(content, config);
        System.out.println(sss.getEncodeModes());
        System.out.println(sss.getErrorMessage());
        System.out.println(sss.getData());
    }
}