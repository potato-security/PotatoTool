package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils;


import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.misc.ReadabilityChecker;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.concurrent.atomic.AtomicReference;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * @author Potato
 * @date 2023/3/27 10:59
 */
public class WeblogicDecUtils {

    public AtomicReference<String> cipher = new AtomicReference<>("");
    public AtomicReference<String> mode = new AtomicReference<>("");
    public AtomicReference<String> padding_mode = new AtomicReference<>("");
    public AtomicReference<String> key_Str = new AtomicReference<>("");
    public AtomicReference<String> iv_Str = new AtomicReference<>("Null");
    public String decrypt(String customPath, String ciphertext) {
        String cleartext = null;
        try {
            cipher.set("AES");
            mode.set("PBEWITHSHAAND128BITRC2-CBC");
            padding_mode.set("PKCS5Padding");
            cleartext = decryptAES(customPath, ciphertext.replaceAll("^[{AES}]+", ""));
            if (cleartext == null) {
                cipher.set("3DES");
                cleartext = decrypt3DES(customPath, ciphertext.replaceAll("^[{3DES}]+", ""));
            }
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }

        // 判断不存在乱码，防止误报
        boolean isReadable = ReadabilityChecker.assessReadability(cleartext, 1, 0);
        if(isReadable){
            return cleartext;
        }
        return null;
    }

    public String decryptAES(String customPath, String ciphertext) throws Exception {
        byte[] encryptedPassword1 = StrUtils.base64Decode(ciphertext.getBytes(StandardCharsets.UTF_8));
        byte[] salt = null;
        byte[] encryptionKey = null;
        char[] password = new char["0xccb97558940b82637c8bec3c770f86fa3a391a56".length()];
        "0xccb97558940b82637c8bec3c770f86fa3a391a56".getChars(0, password.length, password, 0);
        try {
            FileInputStream is = new FileInputStream(customPath);
            salt = readBytes(is);
            int version = is.read();
            if (version != -1) {
                encryptionKey = readBytes(is);
                if (version >= 2) {
                    encryptionKey = readBytes(is);
                }
            }
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWITHSHAAND128BITRC2-CBC");
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password, salt, 5);
        SecretKey secretKey = keyFactory.generateSecret(pbeKeySpec);
        PBEParameterSpec pbeParameterSpec = new PBEParameterSpec(salt, 0);
        Cipher cipher = Cipher.getInstance("PBEWITHSHAAND128BITRC2-CBC");
        cipher.init(2, (Key)secretKey, pbeParameterSpec);
        byte[] key = cipher.doFinal(encryptionKey);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        key_Str.set(new String(key, StandardCharsets.UTF_8));
        byte[] iv = new byte[16];
        System.arraycopy(encryptedPassword1, 0, iv, 0, 16);
        int encryptedPasswordlength = encryptedPassword1.length - 16;
        byte[] encryptedPassword2 = new byte[encryptedPasswordlength];
        System.arraycopy(encryptedPassword1, 16, encryptedPassword2, 0, encryptedPasswordlength);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        iv_Str.set(new String(iv, StandardCharsets.UTF_8));
        Cipher outCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        outCipher.init(2, (Key)secretKeySpec, ivParameterSpec);
        byte[] cleartext = outCipher.doFinal(encryptedPassword2);
        return new String(cleartext, StandardCharsets.UTF_8);
    }

    public String decrypt3DES(String customPath, String ciphertext) throws Exception {
        byte[] encryptedPassword1 = StrUtils.base64Decode(ciphertext.getBytes(StandardCharsets.UTF_8));
        byte[] salt = null;
        byte[] encryptionKey = null;
        char[] password = new char["0xccb97558940b82637c8bec3c770f86fa3a391a56".length()];
        "0xccb97558940b82637c8bec3c770f86fa3a391a56".getChars(0, password.length, password, 0);
        try {
            FileInputStream is = new FileInputStream(customPath);
            salt = readBytes(is);
            int version = is.read();
            if (version != -1) {
                encryptionKey = readBytes(is);
                if (version >= 2) {
                    encryptionKey = readBytes(is);
                }
            }
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWITHSHAAND128BITRC2-CBC");
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password, salt, 5);
        SecretKey secretKey = keyFactory.generateSecret(pbeKeySpec);
        PBEParameterSpec pbeParameterSpec = new PBEParameterSpec(salt, 0);
        Cipher cipher = Cipher.getInstance("PBEWITHSHAAND128BITRC2-CBC");
        cipher.init(2, (Key)secretKey, pbeParameterSpec);

        byte[] key = cipher.doFinal(encryptionKey);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "DESEDE");
        key_Str.set(new String(key, StandardCharsets.UTF_8));
        byte[] iv = new byte[8];
        System.arraycopy(salt, 0, iv, 0, 4);
        System.arraycopy(salt, 0, iv, 4, 4);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        iv_Str.set(new String(iv, StandardCharsets.UTF_8));
        Cipher outCipher = Cipher.getInstance("DESEDE/CBC/PKCS5Padding");
        outCipher.init(2, (Key)secretKeySpec, ivParameterSpec);
        byte[] cleartext = outCipher.doFinal(encryptedPassword1);
        return new String(cleartext, StandardCharsets.UTF_8);
    }

    public static byte[] readBytes(InputStream stream) throws Exception {
        int justread;
        int in;
        int length = stream.read();
        byte[] bytes = new byte[length];
        int i = 0;
        while ((in = i) < length && (justread = stream.read(bytes, in, length - in)) != -1) {
            i = in + justread;
        }
        return bytes;
    }

}
