package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.strategy;

import com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils.*;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.model.DecryptConfig;
import com.potato.potatotool.utils.crypto.BlowfishUtils;
import com.potato.potatotool.utils.crypto.DESUtils;
import com.potato.potatotool.utils.crypto.RSAUtils;
import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.misc.ReadabilityChecker;

import java.util.List;

/**
 * 数据库解密策略
 * 处理各种数据库相关的解密
 * 
 * @author Potato
 * @version 2.0
 */
public class DatabaseDecryptStrategy implements DecryptStrategy {
    
    @Override
    public String decrypt(String content, DecryptConfig config, List<String> encodeMode) {
        if (!isApplicable(content)) {
            return null;
        }
        
        // 尝试各种数据库解密
        String result = tryDatabaseDecryption(content, config, encodeMode);

        if (result != null) {
            return result;
        }
        
        return null;
    }
    
    /**
     * 尝试各种数据库解密
     */
    private String tryDatabaseDecryption(String content, DecryptConfig config, List<String> encodeMode) {
        if(content.length() < 5) return null;
        // 新华三imc解密尝试
        String result = tryH3imcDecrypt(content, encodeMode);
        if (result != null) return result;

        // 用友数据库解密尝试
        result = tryYongyouDecrypt(content, encodeMode);
        if (result != null) return result;
        
        // 致远数据库解密尝试
        result = tryZhiyuanDBDecrypt(content, encodeMode);
        if (result != null) return result;
        
        // 帆软解密尝试
        result = tryFanruanDecrypt(content, encodeMode);
        if (result != null) return result;
        
        // Druid解密尝试
        result = tryDruidDecrypt(content, encodeMode);
        if (result != null) return result;
        
        // Finalshell解密尝试
        result = tryFinalshellDecrypt(content, encodeMode);
        if (result != null) return result;
        
        // Ivms解密尝试
        result = tryIvmsDecrypt(content, encodeMode);
        if (result != null) return result;
        
        // JBoss解密尝试
        result = tryJbossDecrypt(content, encodeMode);
        if (result != null) return result;
        
        // 强智解密尝试
        result = tryQiangzhiDecrypt(content, encodeMode);
        if (result != null) return result;
        
        // Navicat解密尝试
        result = tryNavicatDecrypt(content, encodeMode);
        if (result != null) return result;
        
        // Realor解密尝试
        result = tryRealorDecrypt(content, encodeMode);
        if (result != null) return result;
        
        // Spring解密尝试  可自定义盐值
        result = trySpringDecrypt(content, config, encodeMode);
        if (result != null) return result;
        
        // Weblogic解密尝试
        // 尝试针对weblogic数据库密码解密   需要上传对应SerializedSystemIni.dat文件
        result = tryWeblogicDecrypt(content, config, encodeMode);
        
        return result;
    }

    private String tryH3imcDecrypt(String content, List<String> encodeMode) {
        if(content.startsWith("-")) {
            String result = ImcDecUtils.decode(content);
            if (result != null) {
                encodeMode.add("新华三imcDB");
                return result;
            }
        }
        return null;
    }

    private String tryYongyouDecrypt(String content, List<String> encodeMode) {
        NcDecUtils ncDes = new NcDecUtils();
        String result = ncDes.decodeToStr(content);
        if (result != null) {
            encodeMode.add("用友DB");
            return result;
        }
        return null;
    }


    private String tryZhiyuanDBDecrypt(String content, List<String> encodeMode) {
        if(content.startsWith("/1.0/")) {

            StrUtils str = new StrUtils();
            String result = str.seeyonDbDecode(content);
            if (result != null) {
                encodeMode.add("致远DB_base64");
                return result;
            }

        } else if (content.startsWith("/2.4/")){

            StrUtils str = new StrUtils();
            String result = str.seeyonDbDecode(content);
            if (result != null) {
                encodeMode.add("致远DB_SM4");
                return result;
            }

        }
        return null;
    }
    
    private String tryFanruanDecrypt(String content, List<String> encodeMode) {
        if (content.startsWith("___")) {
            StrUtils str = new StrUtils();
            String result = str.fineReportDecode(content);
            if (result != null && !result.equals(content)) {
                encodeMode.add("帆软DB");
                return result;
            }
        }
        return null;
    }

    private String tryDruidDecrypt(String content, List<String> encodeMode) {
        String result = RSAUtils.decrypt("MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAINRom1IY639dDMD0FFw7zMsxRVABYGJnKxSpO84dyJgXaIkoTZkE1JaWE2/gtgli28vgM72UHf2EGhxbLZwzhsCAwEAAQ==", content);

        // 判断不存在乱码，防止误报
        boolean isReadable = ReadabilityChecker.assessReadability(result, 1, 0);

        if (result != null && isReadable) {
            encodeMode.add("Druid_rsa");
            return result;
        }
        return null;
    }
    
    private String tryFinalshellDecrypt(String content, List<String> encodeMode) {
        DESUtils des = new DESUtils();
        String result = des.finalshellDecode(content);
        if (result != null) {
            encodeMode.add("Finalshell(DES\\"+des.mode_DES.get()+"\\"+des.padding_DES.get()+"<key:iv>"+des.key_DES.get()+":"+des.iv_DES.get()+")");
            return result;
        }
        return null;
    }
    
    private String tryIvmsDecrypt(String content, List<String> encodeMode) {
        String result = IvmsDec.decrypt(content);
        if (result != null) {
            encodeMode.add("ivms");
            return result;
        }
        return null;
    }

    
    private String tryJbossDecrypt(String content, List<String> encodeMode) {
        BlowfishUtils blowfish = new BlowfishUtils();
        String result = blowfish.jbossDecode(content);
        if (result != null) {
            encodeMode.add("Jboss_DB(Blowfish\\"+blowfish.mode_Blowfish.get()+"\\"+blowfish.padding_Blowfish.get()+"<key:iv>"+blowfish.key_Blowfish.get()+":"+blowfish.iv_Blowfish.get());
            return result;
        }
        return null;
    }
    
    private String tryQiangzhiDecrypt(String content, List<String> encodeMode) {
        if(content.startsWith("#!@")){
            String result = QiangzhiDec.desDecode(content);
            if (result != null) {
                encodeMode.add("qiangzhi_DES(DES<key:iv>02A46370BC76:null])");
                return result;
            }
        }
        return null;
    }
    
    private String tryNavicatDecrypt(String content, List<String> encodeMode) {
        String result = Navicat11Dec.decryptString(content);
        if (result != null && result.length() > 3) {
            encodeMode.add("Navicat11(Blowfish\\ECB\\NoPadding<key:iv>7A3F4B8A1C2E6A4A9B3A6B8FA6A0B3F2C0A4F4C7:3B2E68F7D4CCD6E3)\\误报概率大");
            return result;
        }

        result = Navicat12Dec.decryptString(content);
        if (result != null) {
            encodeMode.add("Navicat12(AES\\CBC\\PKCS5Padding<key:iv>libcckeylibcckey:libcciv libcciv )");
            return result;
        }
        return null;
    }
    
    private String tryRealorDecrypt(String content, List<String> encodeMode) {
        String result = RealorDec.decode(content);
        if(result != null){
            encodeMode.add("realor_db");
            return result;
        }
        return null;
    }
    
    private String trySpringDecrypt(String content, DecryptConfig config, List<String> encodeMode) {
        String inputKey = config.getInputKey();
        String result = SpringDec.decode(content, inputKey);
        if(result != null){
            String tmp_key = inputKey == null? "EbfYkitulv73I2p0mXI50JMXoaxZTKJ7" : inputKey;
            encodeMode.add("spring_db(key:" + tmp_key + ")");
            return result;
        }
        return null;
    }

    private String tryWeblogicDecrypt(String content, DecryptConfig config, List<String> encodeMode) {
        String customPath = config.getCustomPath();
        if(customPath!=null && customPath.toLowerCase().endsWith(".dat")){
            WeblogicDecUtils weblogicDec = new WeblogicDecUtils();
            String result = weblogicDec.decrypt(customPath, content);
            if (result != null) {
                encodeMode.add("Weblogic("+weblogicDec.cipher.get()+"\\"+weblogicDec.mode.get()+"\\"+weblogicDec.padding_mode.get()+"<key:iv>"+weblogicDec.key_Str.get()+":"+weblogicDec.iv_Str.get()+")");
                return result;
            }
        }
        return null;
    }
    
    @Override
    public boolean isApplicable(String content) {
        return content != null;
    }
}