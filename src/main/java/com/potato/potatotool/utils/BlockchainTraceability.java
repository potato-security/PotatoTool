package com.potato.potatotool.utils;

import com.google.gson.JsonObject;
import java.util.HashMap;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.Constants.getConfigInfo;
import static com.potato.potatotool.utils.requestUtils.requests;

/**
 * @author Potato
 * @date 2023/11/17 17:32
 */
public class BlockchainTraceability {

    public static String blockUrl = getConfigInfo("blockchainUrl");
    public static HashMap<String, String> headers = new HashMap();
    public static RequestObj obj;
    static {
        headers.put("AuthToken", "MHg2ZCwweDcwLDB4NzMsMHg3OSwweDdhLDB4NDQsMHgzMywweDc0LDB4NmQsMHg0NiwweDc1LDB4MzIsMHg3NywweDQ4LDB4NzEsMHg2YywweDZmLDB4NzUsMHgzOCwweDc3LDB4NmMsMHg0NiwweDY4LDB4MmYsMHg0OSwweDQ4LDB4NTEsMHgzNywweDQ3LDB4MzksMHg0NywweDRiLDB4NDcsMHg0YiwweDcxLDB4MzYsMHg2MSwweDM1LDB4NDMsMHg3MiwweDY1LDB4NmUsMHg2MywweDU0LDB4NDQsMHgzMiwweDRiLDB4NTAsMHg2NywweDc4LDB4NWEsMHg0ZCwweDM5LDB4NjEsMHg0ZCwweDRjLDB4MzksMHg1YSwweDJiLDB4NGEsMHg0NCwweDM2LDB4NGIsMHg2ZCwweDVhLDB4NTIsMHg0YywweDcxLDB4NGQsMHgzNiwweDczLDB4NDIsMHg2NywweDM5LDB4NzMsMHg3NCwweDRkLDB4NjUsMHg0MiwweDY3LDB4NmQsMHgzOCwweDUyLDB4NzIsMHg0NSwweDUyLDB4NTcsMHg1OCwweDU4LDB4NzYsMHgzNywweDcwLDB4NGMsMHg3MiwweDc3LDB4NGYsMHg0NiwweDM3LDB4NzMsMHg0OSwweDMyLDB4NTcsMHgzNCwweDZiLDB4NzUsMHg1OSwweDRkLDB4M2Q=");

        obj = new RequestObj();
        obj.setMethod("GET");
        obj.setHeaders(headers);
    }


    //  检索用户输入
    public static JsonObject search(String arg){
        obj.setUrl(blockUrl + "/search?arg=" + strUtils.urlEncode(arg));

        try {
            CustomHttpResponse con = requests(obj);
            JsonObject res = con.getJson();
            if((con.getResponseCode()==200 && res.has("code") && res.get("code").getAsString().equals("0") || (con.getResponseCode()==200 && !res.has("code") ))) throw new Exception();
            return res;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    //  获取交易地址基础信息
    public static JsonObject address(String network, String address){
        obj.setUrl(blockUrl + "/address?address=" + address + "&network=" + network);

        try {
            CustomHttpResponse con = requests(obj);
            JsonObject res = con.getJson();
            if((con.getResponseCode()==200 && res.has("code") && res.get("code").getAsString().equals("0") || (con.getResponseCode()==200 && !res.has("code") ))) throw new Exception();
            return res;
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }


    //  代币余额
    public static JsonObject tokenbalance(String network, String address){
        obj.setUrl(blockUrl + "/tokenbalance?address=" + address + "&network=" + network);

        try {
            CustomHttpResponse con = requests(obj);
            JsonObject res = con.getJson();
            if((con.getResponseCode()==200 && res.has("code") && res.get("code").getAsString().equals("0") || (con.getResponseCode()==200 && !res.has("code") ))) throw new Exception();
            return res;
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }


    //  代币余额交换信息
    public static JsonObject tokentrans(String network, String address, String to, String sumNum){ // 预知总数
        obj.setUrl(blockUrl + "/tokentrans?address=" + address + "&to=" + to + "&sumNum=" + sumNum + "&network=" + network);

        try {
            CustomHttpResponse con = requests(obj);
            JsonObject res = con.getJson();
            if((con.getResponseCode()==200 && res.has("code") && res.get("code").getAsString().equals("0") || (con.getResponseCode()==200 && !res.has("code") ))) throw new Exception();
            return res;
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }
    public static JsonObject tokentrans(String network, String address, String to, String index, String size){ // 指定页数及条数
        obj.setUrl(blockUrl + "/tokentrans?address=" + address + "&to=" + to + "&index=" + index + "&size=" + size + "&network=" + network);

        try {
            CustomHttpResponse con = requests(obj);
            JsonObject res = con.getJson();
            if((con.getResponseCode()==200 && res.has("code") && res.get("code").getAsString().equals("0") || (con.getResponseCode()==200 && !res.has("code") ))) throw new Exception();
            return res;
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }


    //  近180天余额变化
    public static JsonObject balancetrend(String network, String address){
        obj.setUrl(blockUrl + "/balancetrend?address=" + address + "&network=" + network);

        try {
            CustomHttpResponse con = requests(obj);
            JsonObject res = con.getJson();
            if((con.getResponseCode()==200 && res.has("code") && res.get("code").getAsString().equals("0") || (con.getResponseCode()==200 && !res.has("code") ))) throw new Exception();
            return res;
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }


    //  地址交易信息
    public static JsonObject addressTransaction(String network, String address, String sumNum){ // 预知总数
        obj.setUrl(blockUrl + "/addressTransaction?address=" + address + "&sumNum=" + sumNum + "&network=" + network);

        try {
            CustomHttpResponse con = requests(obj);
            JsonObject res = con.getJson();
            if((con.getResponseCode()==200 && res.has("code") && res.get("code").getAsString().equals("0") || (con.getResponseCode()==200 && !res.has("code") ))) throw new Exception();
            return res;
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }
    public static JsonObject addressTransaction(String network, String address, String index, String size){ // 指定页数及条数
        obj.setUrl(blockUrl + "/addressTransaction?address=" + address + "&index=" + index + "&size=" + size + "&network=" + network);

        try {
            CustomHttpResponse con = requests(obj);
            JsonObject res = con.getJson();
            if((con.getResponseCode()==200 && res.has("code") && res.get("code").getAsString().equals("0") || (con.getResponseCode()==200 && !res.has("code") ))) throw new Exception();
            return res;
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }


    //  TRX波场链为基础的TRC10/TRC10代币类型分类
    public static JsonObject tokenClassification(String network, String address, String type){
        obj.setUrl(blockUrl + "/tokenClassification?address=" + address + "&network=" + network + "&type=" + type );

        try {
            CustomHttpResponse con = requests(obj);
            JsonObject res = con.getJson();
            if((con.getResponseCode()==200 && res.has("code") && res.get("code").getAsString().equals("0") || (con.getResponseCode()==200 && !res.has("code") ))) throw new Exception();
            return res;
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }


    //  获取交易块基本信息
    public static JsonObject block(String network, String block){
        obj.setUrl(blockUrl + "/block?block=" + block + "&network=" + network);

        try {
            CustomHttpResponse con = requests(obj);
            JsonObject res = con.getJson();
            if((con.getResponseCode()==200 && res.has("code") && res.get("code").getAsString().equals("0") || (con.getResponseCode()==200 && !res.has("code") ))) throw new Exception();
            return res;
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }


    //  合约调用转帐
    public static JsonObject getInternalData(String network, String block, String sumNum){ // 预知总数
        obj.setUrl(blockUrl + "/getInternalData?block=" + block + "&sumNum=" + sumNum + "&network=" + network);

        try {
            CustomHttpResponse con = requests(obj);
            JsonObject res = con.getJson();
            if((con.getResponseCode()==200 && res.has("code") && res.get("code").getAsString().equals("0") || (con.getResponseCode()==200 && !res.has("code") ))) throw new Exception();
            return res;
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }
    public static JsonObject getInternalData(String network, String block, String index, String size){ // 指定页数及条数
        obj.setUrl(blockUrl + "/getInternalData?block=" + block + "&index=" + index + "&size=" + size + "&network=" + network);

        try {
            CustomHttpResponse con = requests(obj);
            JsonObject res = con.getJson();
            if((con.getResponseCode()==200 && res.has("code") && res.get("code").getAsString().equals("0") || (con.getResponseCode()==200 && !res.has("code") ))) throw new Exception();
            return res;
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }


    //  代币交易
    public static JsonObject getTokentransferData(String network, String block, String sumNum){ // 预知总数
        obj.setUrl(blockUrl + "/getTokentransferData?block=" + block + "&sumNum=" + sumNum + "&network=" + network);

        try {
            CustomHttpResponse con = requests(obj);
            JsonObject res = con.getJson();
            if((con.getResponseCode()==200 && res.has("code") && res.get("code").getAsString().equals("0") || (con.getResponseCode()==200 && !res.has("code") ))) throw new Exception();
            return res;
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }
    public static JsonObject getTokentransferData(String network, String block, String index, String size){ // 指定页数及条数
        obj.setUrl(blockUrl + "/getTokentransferData?block=" + block + "&index=" + index + "&size=" + size + "&network=" + network);

        try {
            CustomHttpResponse con = requests(obj);
            JsonObject res = con.getJson();
            if((con.getResponseCode()==200 && res.has("code") && res.get("code").getAsString().equals("0") || (con.getResponseCode()==200 && !res.has("code") ))) throw new Exception();
            return res;
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }


    //  块相关交易
    public static JsonObject getTxData(String network, String block, String sumNum){ // 预知总数
        obj.setUrl(blockUrl + "/getTxData?block=" + block + "&sumNum=" + sumNum + "&network=" + network);

        try {
            CustomHttpResponse con = requests(obj);
            JsonObject res = con.getJson();
            if((con.getResponseCode()==200 && res.has("code") && res.get("code").getAsString().equals("0") || (con.getResponseCode()==200 && !res.has("code") ))) throw new Exception();
            return res;
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }
    public static JsonObject getTxData(String network, String block, String index, String size){ // 指定页数及条数
        obj.setUrl(blockUrl + "/getTxData?block=" + block + "&index=" + index + "&size=" + size + "&network=" + network);

        try {
            CustomHttpResponse con = requests(obj);
            JsonObject res = con.getJson();
            if((con.getResponseCode()==200 && res.has("code") && res.get("code").getAsString().equals("0") || (con.getResponseCode()==200 && !res.has("code") ))) throw new Exception();
            return res;
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
