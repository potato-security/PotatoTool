package com.potato.potatotool.content.blueTeam;

import com.potato.potatotool.utils.*;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.strUtils.containsAllElements;


/**
 * @author Potato
 * @date 2023/4/30 09:00
 */
public class webShellDecrypt {

    strUtils str = new strUtils();
    aesUtils aes = new aesUtils();
    shiroDecrypt shiro = new shiroDecrypt();
    desUtils des = new desUtils();
    blowfishUtils blowfish = new blowfishUtils();
    md5Decrypt md5 = new md5Decrypt();

    public byte[] classData;

    public ArrayList<ArrayList<String>> encodeModeList = new ArrayList<ArrayList<String>>();


    public String inputKey = null;
    public String inputIv = null;
    public List<String> traverse = new ArrayList<>();
    public String customPath = null;


    /**
     * 处理http请求体/响应体框架结构体
     * @param conText
     * @return
     */
    public Map<String, Object> dealBody(String conText){
        Map<String, Object> resultDict = new HashMap<>();
        String resultData = "" ;

        resultDict.put("error", 0);
        resultDict.put("encodeModeList", encodeModeList);
        resultDict.put("data", resultData);

        JSONObject jsonData = new jsonUtils.OrderedJSONObject(); // 顺序存储的jsonObj
        String postData = conText;

        if ( conText.startsWith("POST ") && conText.contains("Host")) {
            // 若conText为整个流量包
            int startIndex = conText.indexOf("\n\n") + 2;
            // 判断时要注意这里的值+2、+4
            if(startIndex==1) startIndex = conText.indexOf("\r\n\r\n") + 4;
            if(startIndex==3){
                // 不为\n也不为\r\n
                System.out.println("请求头部与PostData衔接处换行字符存在问题，请修改");
                resultDict.put("error", 1);
                resultDict.put("data", "请求头部与PostData衔接处换行字符存在问题，请修改");
                return resultDict;
            }
            postData = conText.substring(startIndex);
        }


        if ( conText.startsWith("GET ") && conText.contains("Host")) {
            // 若conText为整个流量包
            int startIndex = conText.indexOf("?") + 1;
            int endIndex = conText.indexOf(" HTTP/");

            // 未检测到GetData开始和结束
            if(startIndex==0 || endIndex==-1){
                System.out.println("请求头部未检测到GetData开始和结束，请修改或手动提出");
                resultDict.put("error", 1);
                resultDict.put("data", "请求头部未检测到GetData开始和结束，请修改或手动提出");
                return resultDict;
            }
            postData = conText.substring(startIndex, endIndex);
        }



        if (postData.contains("&") && !(postData.startsWith("{") && postData.endsWith("}")) ){
            // Form-data格式
            String[] keyValuePairArray = postData.split("&");
            for (String keyValuePair : keyValuePairArray) {
                String[] keyValue = keyValuePair.split("=");

                if (keyValue.length == 2) {
                    String key = keyValue[0];
                    String value = keyValue[1];
                    jsonData.put(key, value);
                }else {
                    System.out.println("分割存在异常："+ Arrays.toString(keyValue));
                    System.out.println("分割存在异常，该组参数等号个数："+ keyValue.length);
                    jsonData.clear();
                }
            }

        }else if( postData.startsWith("{") && postData.endsWith("}") && postData.contains("\"") && postData.contains(":") ){
            // jsonStr格式
            try{
                jsonData = new jsonUtils.OrderedJSONObject(postData);
            } catch (Exception e) {
                if(debugMode)e.printStackTrace();
                System.out.println("json格式错误，确定是json格式么？");
            }
        }



        if(conText.contains("Cookie: rememberMe=") && conText.contains("Host")){
            // 针对于全流量包中存在shiro
            Pattern pattern = Pattern.compile("rememberMe=(\\S+)");
            Matcher matcher = pattern.matcher(conText);
            String rememberMeValue = "";
            while (matcher.find()) {
                rememberMeValue = matcher.group(1);
            }
            if(rememberMeValue != "") {  // 尽可能筛除非攻击shiro-> && rememberMeValue.length() > 20
                String tmpResultData = bodyDecrypt(rememberMeValue);
                resultData = conText.replace(rememberMeValue,tmpResultData);

            }
        }

        if(resultData=="") {
            if (jsonData.length() == 0) {
                // 单value 或 单key+value情况下
                String tmpResultData = bodyDecrypt(postData);
                resultData = conText.replace(postData, tmpResultData);
            } else {
                // 存在多个key+value情况下
                String newPostData = postData;
                for (String key : jsonData.keySet()) {
                    String value = jsonData.getString(key);
                    String tmpResultData = bodyDecrypt(value);
                    if (!tmpResultData.isEmpty()) {  // && tmpResultData!="探测加密方式失败，请使用专项解密\\AES爆破"
                        newPostData = newPostData.replace(value, tmpResultData);
                    }
                }
                resultData = conText.replace(postData, newPostData);
            }
        }


        if(str.totalListSize(encodeModeList) > 0){

            flagMatching(conText,resultData);

            System.out.println("Tips:解密成功！");
            System.out.println(encodeModeList);
            resultDict.put("encodeModeList", new ArrayList<>(encodeModeList));
            resultDict.put("data", resultData);

        }else {

            resultDict.put("error", 1);

            if(traverse.isEmpty()){
                resultDict.put("data", "探测加密方式失败，请使用专项解密\\AES爆破");
            }else {
                resultDict.put("data", "探测加密方式失败，请留言评论提供更多信息，让我们一起优化程序");
            }

            return resultDict;

        }

        encodeModeList.clear();

        return resultDict;
    }



    // 内容解密模块
    public String bodyDecrypt(String conText){
        String oldData = conText;
        ArrayList<String> encodeMode = new ArrayList<String>();
        String passStr = "";
        String md5PassKey = "";

        //  log4j解混淆
        if(conText.length()<1000){
            String log4jCode = log4jDecrypt.decrypt(conText);

            if(log4jCode!=null){
                encodeMode.add("log4j混淆");

                encodeModeList.add(encodeMode);

                return log4jCode;
            }
        }


        // bcel解密
        if(conText.toLowerCase().startsWith("$$bcel$$")){
            bcelDecrypt bcel = new bcelDecrypt();
            byte[] bcelByte = bcel.decrypt(conText);
            if(bcelByte!=null){
                String bcelData = new String(bcelByte ,StandardCharsets.UTF_8);
                encodeMode.add("$$bcel$$");

                if( bcel.classCode) encodeMode.add("Class编译");
                if( bcel.serializeCode) encodeMode.add("反序列化");

                encodeModeList.add(encodeMode);

                return bcelData;
            }
        }


        // byte组合解密
        if(conText.length()>100){
            byteDecrypt byteD = new byteDecrypt();
            byte[] byteByte = byteD.decrypt(conText);
            if(byteByte!=null){
                String byteData = new String(byteByte ,StandardCharsets.UTF_8);
                encodeMode.add("Byte转换");

                if( byteD.gzipCode) encodeMode.add("Gzip");
                if( byteD.classCode) encodeMode.add("Class编译");
                if( byteD.serializeCode) encodeMode.add("反序列化");

                encodeModeList.add(encodeMode);

                return byteData;
            }
        }

        // jwt解密
        if( conText.startsWith("eyJ") && conText.split("\\.").length == 3 ){
            String[] conTextList = conText.split("\\.");
            String context_first = str.base64Decode(conTextList[0]);
            String context_finaly = str.base64Decode(conTextList[1]);

            if(context_first!=null && context_finaly!=null){
                encodeMode.add("JWT");
                encodeModeList.add(encodeMode);

                String tmpContext = "header:\n" + context_first +"\n\npayload:\n" + context_finaly;

                return tmpContext;
            }

        }

        // cas解密
        if(conText.length()>100){
            byte[] casByte = casDecrypt.decrypt(conText, aes);
            if(casByte!=null){
                String casData = new String(casByte ,StandardCharsets.UTF_8);
                encodeMode.add("CAS(Base64+AES\\"+aes.mode_AES.get()+"\\"+aes.padding_AES.get()+"<key:iv>"+aes.key_AES.get()+":"+aes.iv_AES.get()+")");

                if( aes.gzipCode || str.gzipCode) encodeMode.add("Gzip");
                if( aes.classCode || str.classCode) encodeMode.add("Class编译");
                if( aes.serializeCode || str.serializeCode) encodeMode.add("反序列化");

                encodeModeList.add(encodeMode);

                return casData;
            }
        }

        // shiro解密尝试
        int indeShiro = conText.indexOf("rememberMe=")!=-1 ? conText.indexOf("rememberMe=") + 11 : 0 ;
        int indeAnd_1 = conText.contains("&") ? conText.indexOf("&") : conText.length(); // 针对奇葩的写入
        int indeAnd_2 = conText.contains(";") ? conText.indexOf(";") : conText.length(); // 针对奇葩的写入
        int indeAnd = indeAnd_1 < indeAnd_2 ? indeAnd_1 : indeAnd_2;

        String tmpData = conText.substring(indeShiro,indeAnd);
        byte[] shiroByte = shiro.decrypt(tmpData,inputKey, aes, str);
        if(shiroByte!=null){
            String shiroData = new String(shiroByte ,StandardCharsets.UTF_8);
            String Shiro = conText.replace(tmpData, shiroData);
            encodeMode.add("Shiro(Base64+AES\\"+aes.mode_AES.get()+"\\"+aes.padding_AES.get()+"<key:iv>"+aes.key_AES.get()+":"+aes.iv_AES.get()+")");

            if( aes.gzipCode || str.gzipCode) encodeMode.add("Gzip");
            if( aes.classCode || str.classCode) encodeMode.add("Class编译");
            if( aes.serializeCode || str.serializeCode) encodeMode.add("反序列化");

            encodeModeList.add(encodeMode);

            return Shiro;
        }

        // md5爆破尝试
        if(conText.length()==16 || conText.length()==32 || conText.length()==40) {
            String tmpConText_md5 = md5.decrypt(conText);
            if(tmpConText_md5 != null) {
                encodeMode.add(md5.mode);
                encodeModeList.add(encodeMode);
                return tmpConText_md5;
            }
        }


        // 尝试组合解密-URLdeocde+【(Base64+XOR)/AES】 【兼容+Gzip】
        for(int i=0; i<3 ; i++){
            String tmpConText = conText;

            // 针对开头可能存在pass=的情况
            String tmpPassStr="";
            if(i==1){
                if(!conText.contains("=")) continue;
                int indexEq = conText.indexOf("=") + 1;
                int indexAnd = conText.contains("&") ? conText.indexOf("&") : conText.length();
                tmpConText = conText.substring(indexEq, indexAnd);
                tmpPassStr = conText.substring(0,indexEq);
                if(tmpConText.contains("=")) continue;
            }

            // 针对开头结尾存在pass+key的情况
            String tempMd5PassKey = "";
            if(i==2){
                if(conText.length()<33 || conText.contains("&")) continue;
                tempMd5PassKey = "流量中提取到md5(pass+md5(key))=" + conText.substring(0,16) + conText.substring(conText.length()-16, conText.length()) + "\n";
                tmpConText = conText.substring(16, conText.length()-16 );
            }

            String conText1 = str.urlDecode(tmpConText);
            if( !conText1.equals(tmpConText) ) encodeMode.add("URLdeocde");

            String tmp_conText3 = null;
            try {

                byte[] tmp_conText2 = str.base64Decode(conText1.getBytes(StandardCharsets.UTF_8));

                tmp_conText3 = str.xorEncode(tmp_conText2, inputKey, traverse, customPath);
            }catch (Exception e){
                if(debugMode)e.printStackTrace();
            }

            if(tmp_conText3 != null){
                encodeMode.add("Base64");
                encodeMode.add("XOR/"+str.xorKey);

                if(i==1) passStr = tmpPassStr;
                if(i==2) md5PassKey = tempMd5PassKey;

                conText = tmp_conText3;
                break;
            }else{
                // 可能使用的AES
                classData = aes.aesWebShellDecode(conText1, inputKey, inputIv, traverse, customPath);
                String conText4 = new String(classData, StandardCharsets.UTF_8);

                if( !conText4.equals(conText1) ){
                    encodeMode.add("AES\\"+aes.mode_AES.get()+"\\"+aes.padding_AES.get()+"<key:iv>"+aes.key_AES.get()+":"+aes.iv_AES.get());

                    if(i==1) passStr = tmpPassStr;
                    if(i==2) md5PassKey = tempMd5PassKey;

                    conText = conText4;
                    break;
                }else{
                    encodeMode.clear();
                }
            }

        }
        // 常见key=base64(密文) 【小补丁】
        if(conText.contains("fileName=") && conText.contains("methodName=")){
            String tmpConText = str.urlDecode(conText);
            String[] parameters = tmpConText.split("&");

            for (String parameter : parameters) {
                String[] keyValue = parameter.split("=", 2);
                String newValue = str.base64Decode(keyValue[1]);
                if(newValue!=null) conText = conText.replace(keyValue[1], newValue);

                if(!tmpConText.equals(conText)) encodeMode.add("Base64");
            }
        }

        // 尝试新华三imc解密
        if(conText.startsWith("-")){
            String conText_imc = imcDesUtils.decode(conText);
            if(conText_imc != null) {
                encodeMode.add("新华三imcDB");
                encodeModeList.add(encodeMode);
                return conText_imc;
            }
        }

        // 尝试用友数据库解密
        ncDesUtils ncDes = new ncDesUtils();
        String conText_nc = ncDes.decodeToStr(conText);
        if(conText_nc != null) {
            encodeMode.add("用友DB");
            encodeModeList.add(encodeMode);
            return conText_nc;
        }

        // 尝试致远数据库解密
        if(conText.startsWith("/1.0/")){
            String conText_seeyon = str.seeyonDbDecode(conText);
            if(conText_seeyon!=null){
                encodeMode.add("致远DB_base64");
                encodeModeList.add(encodeMode);
                return conText_seeyon;
            }
        }
        if(conText.startsWith("/2.4/")){
            String conText_seeyon = str.seeyonDbDecode(conText);
            if(conText_seeyon!=null){
                encodeMode.add("致远DB_SM4");
                encodeModeList.add(encodeMode);
                return conText_seeyon;
            }
        }

        // 尝试帆软数据库解密
        if (conText.startsWith("___")) {
            String conText_fineReport = str.fineReportDecode(conText);
            if(!conText_fineReport.equals(conText)){
                encodeMode.add("帆软DB");
                encodeModeList.add(encodeMode);
                return conText_fineReport;
            }
        }

        // 尝试Druid_rsa解密
        String conText_druid = rsaUtils.decrypt("MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAINRom1IY639dDMD0FFw7zMsxRVABYGJnKxSpO84dyJgXaIkoTZkE1JaWE2/gtgli28vgM72UHf2EGhxbLZwzhsCAwEAAQ==",conText);
        if(conText_druid != null){
            encodeMode.add("Druid_rsa");
            encodeModeList.add(encodeMode);
            return conText_druid;
        }

        // 尝试Finalshell解密
        String conText_Finalshell = des.finalshellDecode(conText);
        if(conText_Finalshell != null){
            encodeMode.add("Finalshell(DES\\"+des.mode_DES.get()+"\\"+des.padding_DES.get()+"<key:iv>"+des.key_DES.get()+":"+des.iv_DES.get()+")");
            encodeModeList.add(encodeMode);
            return conText_Finalshell;
        }

        // 尝试ivms解密
        String conText_ivmsDes = ivmsDes.decrypt(conText);
        if(conText_ivmsDes != null){
            encodeMode.add("ivms");
            encodeModeList.add(encodeMode);
            return conText_ivmsDes;
        }

        // 尝试Jboss_db解密
        String conText_Jboss = blowfish.jbossDecode(conText);
        if(conText_Jboss != null){
            encodeMode.add("Jboss_DB(Blowfish\\"+blowfish.mode_Blowfish.get()+"\\"+blowfish.padding_Blowfish.get()+"<key:iv>"+blowfish.key_Blowfish.get()+":"+blowfish.iv_Blowfish.get());
            encodeModeList.add(encodeMode);
            return conText_Jboss;
        }

        // 尝试qiangzhi_db解密
        if(conText.startsWith("#!@")){
            String conText_qiangzhi = qiangzhiDec.desDecode(conText);
            if(conText_qiangzhi != null){
                encodeMode.add("qiangzhi_DES(DES<key:iv>02A46370BC76:null])");
                encodeModeList.add(encodeMode);
                return conText_qiangzhi;
            }
        }

        // 尝试Navicat解密
        String conText_navicat11 = navicat11Des.decryptString(conText);
        if(conText_navicat11 != null){
            encodeMode.add("Navicat11(Blowfish\\ECB\\NoPadding<key:iv>7A3F4B8A1C2E6A4A9B3A6B8FA6A0B3F2C0A4F4C7:3B2E68F7D4CCD6E3)");
            encodeModeList.add(encodeMode);
            return conText_navicat11;
        }
        String conText_navicat12 = navicat12Des.decryptString(conText);
        if(conText_navicat12 != null){
            encodeMode.add("Navicat12(AES\\CBC\\PKCS5Padding<key:iv>libcckeylibcckey:libcciv libcciv )");
            encodeModeList.add(encodeMode);
            return conText_navicat12;
        }

        // 尝试realor解密
        String conText_realor = realorDec.decode(conText);
        if(conText_realor != null){
            encodeMode.add("realor_db");
            encodeModeList.add(encodeMode);
            return conText_realor;
        }

        // 尝试spring解密 可自定义盐值
        String conText_spring = springDec.decode(conText, inputKey);
        if(conText_spring != null){
            String tmp_key = inputKey == null? "EbfYkitulv73I2p0mXI50JMXoaxZTKJ7" : inputKey;
            encodeMode.add("spring_db(key:" + tmp_key + ")");
            encodeModeList.add(encodeMode);
            return conText_spring;
        }

        // 尝试针对weblogic数据库密码解密   需要上传对应SerializedSystemIni.dat文件
        if(customPath!=null && customPath.toLowerCase().endsWith(".dat")){

            weblogicDecUtils weblogicDec = new weblogicDecUtils();
            String tmpConText = weblogicDec.decrypt(customPath, conText);

            if(tmpConText!=null){
                encodeMode.add("Weblogic("+weblogicDec.cipher.get()+"\\"+weblogicDec.mode.get()+"\\"+weblogicDec.padding_mode.get()+"<key:iv>"+weblogicDec.key_Str.get()+":"+weblogicDec.iv_Str.get()+")");
                encodeModeList.add(encodeMode);
                return tmpConText;
            }
        }


        // 尝试单解密
        if(oldData.equals(conText)){
            System.out.println("组合解密失败，开始尝试单解密");
        }
        for (int i = 0; i < 5; i++){
            String conText1 = str.urlDecode(conText);
            String conText2 = str.chrFuncDecode(conText1);
            String conText3 = str.base64FuncDecode(conText2);
            String conText4 = str.ROT13FuncDecode(conText3);
            String conText5 = str.strFuncRev(conText4);
            String conText6 = str.decodeUnicode(conText5);
            String conText7 = str.hexDecode(conText6);
            // 判断该解密后字符串可读性，默认阈值：可视化比例0.8 乱码5个
            conText7 = ReadabilityChecker.assessReadability(conText7) ? conText7 : conText6;
            String conText8 = str.base64Decode(conText7);
            conText8 = ReadabilityChecker.assessReadability(conText8) ? conText8 : conText7;
            String conText9 = str.seeyonBase64Decode(conText8);
            conText9 = ReadabilityChecker.assessReadability(conText9) ? conText9 : conText8;

            String conText20;
            //  已存在AES / 遍历字典并且非第4次循环(单解密3次+AES解密一次+单解密2次)
            if(str.listContantsStr((ArrayList<String>) encodeMode, "AES") || (!traverse.isEmpty() && i!=3) ){
                conText20 = conText9;
            }else{
                classData = aes.aesWebShellDecode(conText9, inputKey, inputIv, traverse, customPath);
                conText20 = new String(classData, StandardCharsets.UTF_8);
            }
            String conText21;
            if(str.listContantsStr((ArrayList<String>) encodeMode, "DES") || str.listContantsStr((ArrayList<String>) encodeMode, "AES") || (!traverse.isEmpty() && i!=3) ){
                conText21 = conText20;
            }else{
                classData = des.desWebShellDecode(conText20, inputKey, inputIv, traverse, customPath);
                conText21 = new String(classData, StandardCharsets.UTF_8);
            }
            //  不需要单独考虑XOR, XOR均为组合加密


            if( !conText1.equals(conText ) ) encodeMode.add("URLdeocde");
            if( !conText2.equals(conText1) ) encodeMode.add("ChrDecode");
            if( !conText3.equals(conText2) ) encodeMode.add("Base64");
            if( !conText4.equals(conText3) ) encodeMode.add("Rot13");
            if( !conText5.equals(conText4) ) encodeMode.add("strRev");
            if( !conText6.equals(conText5) ) encodeMode.add("Unicode");
            if( !conText7.equals(conText6) ){
                if(conText7.length()>5) {
                    encodeMode.add("HexDecode");
                }else{
                    encodeMode.add("MayBeHex");
                }
            }
            if( !conText8.equals(conText7) ) encodeMode.add("Base64");
            if( !conText9.equals(conText8) ) encodeMode.add("seeyonBase64");
            if( !conText20.equals(conText9) ) encodeMode.add("AES\\"+aes.mode_AES.get()+"\\"+aes.padding_AES.get()+"<key:iv>"+aes.key_AES.get()+":"+aes.iv_AES.get());
            if( !conText21.equals(conText20) ) encodeMode.add("DES\\"+des.mode_DES.get()+"\\"+des.padding_DES.get()+"<key:iv>"+des.key_DES.get()+":"+des.iv_DES.get());

            conText = conText21;
        }

        if(!passStr.equals("")) conText = passStr + conText;
        if(!md5PassKey.equals("")) conText = md5PassKey + conText;

        if( aes.gzipCode || des.gzipCode || str.gzipCode) encodeMode.add("Gzip");
        if( aes.classCode || des.classCode || str.classCode) encodeMode.add("Class编译");
        if( aes.serializeCode || des.serializeCode || str.serializeCode) encodeMode.add("反序列化");

        encodeModeList.add(encodeMode);

        return conText;
    }

    /**
     * webshell管理工具特征匹配
     * conText - 原数据
     * resultData - 解密后数据
    **/
    public void flagMatching(String conText,String resultData){
        conText = conText.toLowerCase(Locale.ROOT);
        resultData = resultData.toLowerCase(Locale.ROOT);

        ArrayList<String> encodeMode = new ArrayList<String>();

        String[] yijian_base64 = {
                "@ini_set(",
                "display_error",
                "@set_time_limit"
        };
        String[] yijian_base64B = {
                "=@eval(@base64_decode($_post["
        };
        String[] yijian_chr = {
                "=%40eval(chr("
        };
        String[] yijian_hex = {
                "unsafe",
                "hexasciiconver("
        };
        String[] yijian_rot13 = {
                "=@eval(@str_tor13($_post["
        };
        String[] yijian_url = {
                "unsafe",
                "response.write(",
                "response.end();",
        };
        String[] caidao = {
                "=@eval(base64_decode($_post["
        };
        String[] gesila_base64 = {
                "=eval(base64_decode(strrev(urldecode("
        };
        String[] gesila_jsp = {
                "string xc=",
                "getclass().getclassloader()",
        };
        String[] bingxie2 = {
                "success",
                "status",
                "msg",
        };
        String[] bingxie3 = {
                "txcwr1nnexzad0zaawmipazjh1bfbfththcjsluxwed",
                "dfaxqv1lorchrqtlrlwmahwftag/m",
        };
        String[] bingxie4 = {
                "$post=decrypt(file_get_contents(\"php://input\"));",
                "eval($post);"
        };
        if(resultData.startsWith("流量中提取到md5(pass+md5(key)")) {
            encodeMode.add("哥斯拉");
        }else if(resultData.startsWith("pass=")) {
            encodeMode.add("哥斯拉");
        }else if(containsAllElements(resultData, gesila_base64)){
            encodeMode.add("哥斯拉");
        }else if(containsAllElements(resultData, gesila_jsp)){
            encodeMode.add("哥斯拉");
        }else if(resultData.startsWith("methodname=")) {
            encodeMode.add("哥斯拉");
        }else if(aes.key_AES.equals("e45e329feb5d925b")) {
            encodeMode.add("冰蝎");
        }else if(resultData.contains("e45e329feb5d925b")) {
            encodeMode.add("冰蝎");
        }else if(containsAllElements(resultData, bingxie2)) {
            encodeMode.add("冰蝎");
        }else if(containsAllElements(resultData, bingxie3)) {
            encodeMode.add("冰蝎");
        }else if(containsAllElements(resultData, bingxie4)) {
            encodeMode.add("冰蝎");
        }else if(containsAllElements(resultData, yijian_base64)){
            encodeMode.add("蚁剑");
        }else if(containsAllElements(resultData, yijian_base64B)){
            encodeMode.add("蚁剑");
        }else if(containsAllElements(conText, yijian_chr)){
            encodeMode.add("蚁剑");
        }else if(containsAllElements(resultData, yijian_hex)){
            encodeMode.add("蚁剑");
        }else if(containsAllElements(resultData, yijian_rot13)){
            encodeMode.add("蚁剑");
        }else if(containsAllElements(resultData, yijian_url)){
            encodeMode.add("蚁剑");
        }else if(containsAllElements(resultData, caidao)){
            encodeMode.add("菜刀");
        }


        if(encodeMode.size() == 0){
            // 弱特征匹配
            String[] header_bingxie2 = {
                    "Accept: text/html, image/gif, image/jpeg, ; q=.2, /; q=.2",
                    "display_error", "Cookie: PHPSESSID=; path=/"
            };
            String[] header_bingxie3 = {
                    "Accept: applicaation/json, text/javascript, */*; q=0.01",
                    "Connection: Keep-Alive"
            };
            if(containsAllElements(conText, header_bingxie2)){
                encodeMode.add("弱特征:冰蝎2");
            }else if (containsAllElements(conText, header_bingxie3)){
                encodeMode.add("弱特征:冰蝎3");
            }
        }

        if(encodeMode.size() > 0){
            encodeModeList.add(0, encodeMode);
        }

    }


    public static void main(String[] args) {
        webShellDecrypt www=new webShellDecrypt();
        Map<String, Object> sss = www.dealBody("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");
        System.out.println(sss);
    }

}
