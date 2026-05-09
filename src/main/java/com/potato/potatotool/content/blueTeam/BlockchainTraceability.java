package com.potato.potatotool.content.blueTeam;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.ProxyUtils;
import com.potato.potatotool.utils.network.RequestObj;
import com.potato.potatotool.utils.data.StrUtils;

import java.util.HashMap;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.core.Constants.getConfigInfo;
import static com.potato.potatotool.utils.network.RequestUtils.requests;

/**
 * @author Potato
 * @date 2023/11/17 17:32
 */
public class BlockchainTraceability {

    public static String blockUrl = getConfigInfo("blockchainUrl");
    public static HashMap<String, String> headers = new HashMap();
    static {
        headers.put("AuthToken", "MHg2ZCwweDcwLDB4NzMsMHg3OSwweDdhLDB4NDQsMHgzMywweDc0LDB4NmQsMHg0NiwweDc1LDB4MzIsMHg3NywweDQ4LDB4NzEsMHg2YywweDZmLDB4NzUsMHgzOCwweDc3LDB4NmMsMHg0NiwweDY4LDB4MmYsMHg0OSwweDQ4LDB4NTEsMHgzNywweDQ3LDB4MzksMHg0NywweDRiLDB4NDcsMHg0YiwweDcxLDB4MzYsMHg2MSwweDM1LDB4NDMsMHg3MiwweDY1LDB4NmUsMHg2MywweDU0LDB4NDQsMHgzMiwweDRiLDB4NTAsMHg2NywweDc4LDB4NWEsMHg0ZCwweDM5LDB4NjEsMHg0ZCwweDRjLDB4MzksMHg1YSwweDJiLDB4NGEsMHg0NCwweDM2LDB4NGIsMHg2ZCwweDVhLDB4NTIsMHg0YywweDcxLDB4NGQsMHgzNiwweDczLDB4NDIsMHg2NywweDM5LDB4NzMsMHg3NCwweDRkLDB4NjUsMHg0MiwweDY3LDB4NmQsMHgzOCwweDUyLDB4NzIsMHg0NSwweDUyLDB4NTcsMHg1OCwweDU4LDB4NzYsMHgzNywweDcwLDB4NGMsMHg3MiwweDc3LDB4NGYsMHg0NiwweDM3LDB4NzMsMHg0OSwweDMyLDB4NTcsMHgzNCwweDZiLDB4NzUsMHg1OSwweDRkLDB4M2Q=");
    }

    private static RequestObj createRequestObj() {
        RequestObj obj = new RequestObj()
                .setMethod("GET")
                .setHeaders(headers)
                .setRandomUserAgent(false)
                .setTimeOut(5)
                .setReadTimeout(45)
                .setCallTimeout(55)
                .setRetries(0);
        ProxyUtils.applyServiceProxy(obj, ConfigConstants.BLOCKCHAIN_SERVICE);
        return obj;
    }

    private static JsonObject requestJson(RequestObj obj) throws Exception {
        try (CustomHttpResponse con = requests(obj)){
            JsonElement json = con.getJson();
            if (json == null || !json.isJsonObject()) {
                JsonObject fallback = new JsonObject();
                fallback.addProperty("code", 0);
                if (con.getResponseCode() == 404) {
                    fallback.addProperty("msg", "区块链服务接口未部署或路径不存在");
                } else if (con.getResponseCode() == 401 || con.getResponseCode() == 403) {
                    fallback.addProperty("msg", "区块链服务认证失败，请检查服务端 AuthToken 配置");
                } else {
                    fallback.addProperty("msg", "区块链服务响应异常，HTTP " + con.getResponseCode());
                }
                fallback.add("data", JsonNull.INSTANCE);
                return fallback;
            }
            JsonObject res = json.getAsJsonObject();
            if (!res.has("code")) {
                res.addProperty("code", con.getResponseCode() == 200 ? 1 : 0);
            }
            if (!res.has("msg")) {
                res.addProperty("msg", con.getResponseCode() == 200 ? "成功" : "区块链服务响应异常，HTTP " + con.getResponseCode());
            }
            if (!res.has("data")) {
                res.add("data", JsonNull.INSTANCE);
            }
            return res;
        }
    }


    //  检索用户输入
    public static JsonObject search(String arg){
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/search?arg=" + StrUtils.urlEncode(arg));

        try {
            return requestJson(obj);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    //  获取交易地址基础信息
    public static JsonObject address(String network, String address){
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/address?address=" + address + "&network=" + network);

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }


    //  代币余额
    public static JsonObject tokenbalance(String network, String address){
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/tokenbalance?address=" + address + "&network=" + network);

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }


    //  代币余额交换信息
    public static JsonObject tokentrans(String network, String address, String to, String sumNum){ // 预知总数
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/tokentrans?address=" + address + "&to=" + to + "&sumNum=" + sumNum + "&network=" + network);

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }
    public static JsonObject tokentrans(String network, String address, String to, String index, String size){ // 指定页数及条数
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/tokentrans?address=" + address + "&to=" + to + "&index=" + index + "&size=" + size + "&network=" + network);

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }


    //  近180天余额变化
    public static JsonObject balancetrend(String network, String address){
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/balancetrend?address=" + address + "&network=" + network);

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }


    //  地址交易信息
    public static JsonObject addressTransaction(String network, String address, String sumNum){ // 预知总数
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/addressTransaction?address=" + address + "&sumNum=" + sumNum + "&network=" + network);

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }
    public static JsonObject addressTransaction(String network, String address, String index, String size){ // 指定页数及条数
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/addressTransaction?address=" + address + "&index=" + index + "&size=" + size + "&network=" + network);

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }


    //  TRX波场链为基础的TRC10/TRC10代币类型分类
    public static JsonObject tokenClassification(String network, String address, String type){
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/tokenClassification?address=" + address + "&network=" + network + "&type=" + type );

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }


    //  获取交易块基本信息
    public static JsonObject block(String network, String block){
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/block?block=" + block + "&network=" + network);

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }


    //  合约调用转帐
    public static JsonObject getInternalData(String network, String block, String sumNum){ // 预知总数
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/getInternalData?block=" + block + "&sumNum=" + sumNum + "&network=" + network);

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }
    public static JsonObject getInternalData(String network, String block, String index, String size){ // 指定页数及条数
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/getInternalData?block=" + block + "&index=" + index + "&size=" + size + "&network=" + network);

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }


    //  代币交易
    public static JsonObject getTokentransferData(String network, String block, String sumNum){ // 预知总数
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/getTokentransferData?block=" + block + "&sumNum=" + sumNum + "&network=" + network);

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }
    public static JsonObject getTokentransferData(String network, String block, String index, String size){ // 指定页数及条数
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/getTokentransferData?block=" + block + "&index=" + index + "&size=" + size + "&network=" + network);

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }


    //  块相关交易
    public static JsonObject getTxData(String network, String block, String sumNum){ // 预知总数
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/getTxData?block=" + block + "&sumNum=" + sumNum + "&network=" + network);

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }
    public static JsonObject getTxData(String network, String block, String index, String size){ // 指定页数及条数
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/getTxData?block=" + block + "&index=" + index + "&size=" + size + "&network=" + network);

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }

    // 免费行情聚合
    public static JsonObject marketAggregate(String base, String quote){
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/market/aggregate?base=" + StrUtils.urlEncode(base) + "&quote=" + StrUtils.urlEncode(quote));

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }

    // 币种详情
    public static JsonObject coinProfile(String symbol, String quote){
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/coin/profile?symbol=" + StrUtils.urlEncode(symbol) + "&quote=" + StrUtils.urlEncode(quote));

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }

    // 行情健康检查
    public static JsonObject marketHealth(){
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/health/market");

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }

    //  交易分析报告
    public static JsonObject reportTransaction(String network, String txid){
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/report/transaction?network="
                + StrUtils.urlEncode(network) + "&txid=" + StrUtils.urlEncode(txid));

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }

    //  交易详情
    public static JsonObject txDetail(String network, String txid){
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/tx/detail?network="
                + StrUtils.urlEncode(network) + "&txid=" + StrUtils.urlEncode(txid));

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }

    //  交易事件
    public static JsonObject txEvents(String network, String txid){
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/tx/events?network="
                + StrUtils.urlEncode(network) + "&txid=" + StrUtils.urlEncode(txid));

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }

    //  地址分析报告
    public static JsonObject reportAddress(String network, String address){
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/report/address?network="
                + StrUtils.urlEncode(network) + "&address=" + StrUtils.urlEncode(address));

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }

    //  合约信息
    public static JsonObject contractInfo(String network, String address){
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/contract/info?network="
                + StrUtils.urlEncode(network) + "&address=" + StrUtils.urlEncode(address));

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }

    //  Token 信息
    public static JsonObject tokenInfo(String network, String address){
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/token/info?network="
                + StrUtils.urlEncode(network) + "&address=" + StrUtils.urlEncode(address));

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }

    //  地址资产
    public static JsonObject addressPortfolio(String network, String address){
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/address/portfolio?network="
                + StrUtils.urlEncode(network) + "&address=" + StrUtils.urlEncode(address));

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }

    //  地址追踪
    public static JsonObject traceAddress(String network, String address){
        RequestObj obj = createRequestObj().setUrl(blockUrl + "/trace/address?network="
                + StrUtils.urlEncode(network) + "&address=" + StrUtils.urlEncode(address));

        try {
            return requestJson(obj);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }



    //  测试模块
    public static void main(String[] args) {
        JsonObject searchJsonObj = search("TV6MuMXfmLbBqPZvBHdwFsDnQeVfnmiuSi");
        System.out.println(searchJsonObj);

        JsonObject addressJsonObj = address("trx","TV6MuMXfmLbBqPZvBHdwFsDnQeVfnmiuSi");
        System.out.println(addressJsonObj);

        JsonObject tokenbalanceJsonObj = tokenbalance("trx","TV6MuMXfmLbBqPZvBHdwFsDnQeVfnmiuSi");
        System.out.println(tokenbalanceJsonObj);

        JsonObject tokentransJsonObj = tokentrans("trx","TV6MuMXfmLbBqPZvBHdwFsDnQeVfnmiuSi","TKibDMgVfQzFEReJL6PRne8fCDnNFm9D2e","30");
        System.out.println(tokentransJsonObj);
        JsonObject tokentrans2JsonObj = tokentrans("trx","TV6MuMXfmLbBqPZvBHdwFsDnQeVfnmiuSi","TKibDMgVfQzFEReJL6PRne8fCDnNFm9D2e","1","10");
        System.out.println(tokentrans2JsonObj);

        JsonObject balancetrendJsonObj = balancetrend("trx","TV6MuMXfmLbBqPZvBHdwFsDnQeVfnmiuSi");
        System.out.println(balancetrendJsonObj);

        JsonObject addressTransactionJsonObj = addressTransaction("btc","12YHXHbhSBY7D32hz4iFgSohxcWNEvWqKF","30");
        System.out.println(addressTransactionJsonObj);
        JsonObject addressTransaction2JsonObj = addressTransaction("trx","TLdNkJ6udtMWd8v3mm7GC3HuWo8BtFLEi2","1","10");
        System.out.println(addressTransaction2JsonObj);

        JsonObject blockJsonObj = block("eth","18245784");
        System.out.println(blockJsonObj);

        JsonObject tokenClassificationJsonObj = tokenClassification("trx","TV6MuMXfmLbBqPZvBHdwFsDnQeVfnmiuSi","TRC10");
        System.out.println(tokenClassificationJsonObj);
        JsonObject tokenClassification2JsonObj = tokenClassification("trx","TV6MuMXfmLbBqPZvBHdwFsDnQeVfnmiuSi","TRC20");
        System.out.println(tokenClassification2JsonObj);

        JsonObject getInternalDataJsonObj = getInternalData("eth","18245784","30");
        System.out.println(getInternalDataJsonObj);
        JsonObject getInternalData2JsonObj = getInternalData("eth","18245784","1","10");
        System.out.println(getInternalData2JsonObj);

        JsonObject getTokentransferDataJsonObj = getTokentransferData("eth","18245784","30");
        System.out.println(getTokentransferDataJsonObj);
        JsonObject getTokentransferData2JsonObj = getTokentransferData("eth","18245784","1","10");
        System.out.println(getTokentransferData2JsonObj);

        JsonObject getTxDataJsonObj = getTxData("eth","18245784","30");
        System.out.println(getTxDataJsonObj);
        JsonObject getTxData2JsonObj = getTxData("eth","18245784","1","10");
        System.out.println(getTxData2JsonObj);

    }

}
