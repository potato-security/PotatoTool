package com.potato.potatotool.utils;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.X509EncodedKeySpec;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * @author Potato
 * @date 2023/3/27 21:44
 */
public class rsaUtils {
    private static final String DEFAULT_PRIVATE_KEY_STRING = "MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEAocbCrurZGbC5GArEHKlAfDSZi7gFBnd4yxOt0rwTqKBFzGyhtQLu5PRKjEiOXVa95aeIIBJ6OhC2f8FjqFUpawIDAQABAkAPejKaBYHrwUqUEEOe8lpnB6lBAsQIUFnQI/vXU4MV+MhIzW0BLVZCiarIQqUXeOhThVWXKFt8GxCykrrUsQ6BAiEA4vMVxEHBovz1di3aozzFvSMdsjTcYRRo82hS5Ru2/OECIQC2fAPoXixVTVY7bNMeuxCP4954ZkXp7fEPDINCjcQDywIgcc8XLkkPcs3Jxk7uYofaXaPbg39wuJpEmzPIxi3k0OECIGubmdpOnin3HuCP/bbjbJLNNoUdGiEmFL5hDI4UdwAdAiEAtcAwbm08bKN7pwwvyqaCBC//VnEWaq39DCzxr+Z2EIk=";
    public static final String DEFAULT_PUBLIC_KEY_STRING = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKHGwq7q2RmwuRgKxBypQHw0mYu4BQZ3eMsTrdK8E6igRcxsobUC7uT0SoxIjl1WveWniCASejoQtn/BY6hVKWsCAwEAAQ==";

    public static String decrypt(String publicKeyText, String cipherText){
        try {
            PublicKey publicKey = rsaUtils.getPublicKey(publicKeyText);
            return rsaUtils.decrypt(publicKey, cipherText);
        }catch (Exception e){
            if(debugMode)e.printStackTrace();
            return null;
        }
    }

    public static String decrypt(PublicKey publicKey, String cipherText) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        try {
            cipher.init(2, publicKey);
        } catch (InvalidKeyException e) {
            RSAPublicKey rsaPublicKey = (RSAPublicKey)publicKey;
            RSAPrivateKeySpec spec = new RSAPrivateKeySpec(rsaPublicKey.getModulus(), rsaPublicKey.getPublicExponent());
            PrivateKey fakePrivateKey = KeyFactory.getInstance("RSA").generatePrivate(spec);
            cipher = Cipher.getInstance("RSA");
            cipher.init(2, fakePrivateKey);
        }
        if (cipherText == null || cipherText.length() == 0) {
            return cipherText;
        }
        byte[] cipherBytes = strUtils.base64Decode(cipherText.getBytes(StandardCharsets.UTF_8));
        byte[] plainBytes = cipher.doFinal(cipherBytes);
        return new String(plainBytes);
    }

    public static PublicKey getPublicKey(String publicKeyText) {
        if (publicKeyText == null || publicKeyText.length() == 0) {
            publicKeyText = DEFAULT_PUBLIC_KEY_STRING;
        }
        try {
            byte[] publicKeyBytes = strUtils.base64Decode(publicKeyText.getBytes(StandardCharsets.UTF_8));
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA", "SunRsaSign");
            return keyFactory.generatePublic(x509KeySpec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to get public key", e);
        }
    }

    public static void main(String []args) {
        String res = rsaUtils.decrypt("MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAINRom1IY639dDMD0FFw7zMsxRVABYGJnKxSpO84dyJgXaIkoTZkE1JaWE2/gtgli28vgM72UHf2EGhxbLZwzhsCAwEAAQ==","ai9lB7h4oR9AHrQzU8H38umcelX9dBmx4aSycDOgJWa/2sv5U0GzbyI9sx54sL3nJ0kGayGrTHl3N/Bp1sSJ4w==");
        System.out.println(res);

    }

}
