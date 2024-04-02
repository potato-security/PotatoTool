package com.potato.potatotool.content.blueTeam;

import com.potato.potatotool.utils.*;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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

    public byte[] classData;

    public ArrayList<ArrayList<String>> encodeModeList = new ArrayList<ArrayList<String>>();


    public String inputKey = null;
    public String inputIv = null;
    public boolean traverse = false;
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

        if ( conText.startsWith("POST ") && conText.contains("Host") && conText.contains("Content-Length")) {
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
                e.printStackTrace();
                System.out.println("json格式错误，确定是json格式么？");
            }
        }



        if(conText.contains("Cookie: rememberMe=") && conText.contains("Host") && conText.contains("Content-Length")){
            // 针对于全流量包中存在shiro
            Pattern pattern = Pattern.compile("rememberMe=(\\S+)");
            Matcher matcher = pattern.matcher(conText);
            String rememberMeValue = "";
            while (matcher.find()) {
                rememberMeValue = matcher.group(1);
            }
            if(rememberMeValue != "") {  // 尽可能筛除非攻击shiro-> && rememberMeValue.length() > 20
                String tmpResultData = bodyDecrypt(rememberMeValue);
                conText = conText.replace(rememberMeValue,tmpResultData);

            }
        }


        if(jsonData.length()==0){
            // 单value 或 单key+value情况下
            String tmpResultData = bodyDecrypt(postData);
            resultData = conText.replace(postData,tmpResultData);
        }else{
            // 存在多个key+value情况下
            String newPostData = postData;
            for (String key : jsonData.keySet()) {
                String value = jsonData.getString(key);
                String tmpResultData = bodyDecrypt(value);
                if(!tmpResultData.isEmpty()){  // && tmpResultData!="探测加密方式失败，请使用专项解密\\AES爆破"
                    newPostData = newPostData.replace(value,tmpResultData);
                }
            }
            resultData = conText.replace(postData,newPostData);
        }


        if(str.totalListSize(encodeModeList) > 0){

            System.out.println("Tips:解密成功！");
            System.out.println(encodeModeList);
            resultDict.put("encodeModeList", new ArrayList<>(encodeModeList));
            resultDict.put("data", resultData);

        }else {

            resultDict.put("error", 1);

            if(traverse){
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
            System.out.println("--------------------------------------");
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
                e.printStackTrace();
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
            if(str.listContantsStr((ArrayList<String>) encodeMode, "AES") || (traverse && i!=3) ){
                conText20 = conText9;
            }else{
                classData = aes.aesWebShellDecode(conText9, inputKey, inputIv, traverse, customPath);
                conText20 = new String(classData, StandardCharsets.UTF_8);
            }
            String conText21;
            if(str.listContantsStr((ArrayList<String>) encodeMode, "DES") || (traverse && i!=3) ){
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



}
