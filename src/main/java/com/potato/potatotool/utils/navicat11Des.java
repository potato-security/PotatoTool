package com.potato.potatotool.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * @author Potato
 * @date 2024/3/28 11:36
 */
public class navicat11Des {

    public static final String DefaultUserKey = "3DC5CA39";
    public static byte[] _IV;
    public static SecretKeySpec _Key;
    public static Cipher _Encryptor;
    public static Cipher _Decryptor;

    public static void initKey(String UserKey) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            byte[] userkey_data = UserKey.getBytes(StandardCharsets.UTF_8);
            sha1.update(userkey_data, 0, userkey_data.length);
            _Key = new SecretKeySpec(sha1.digest(), "Blowfish");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void initChiperEncrypt() {
        try {
            _Encryptor = Cipher.getInstance("Blowfish/ECB/NoPadding");
            _Encryptor.init(1, _Key);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void initChiperDecrypt() {
        try {
            _Decryptor = Cipher.getInstance("Blowfish/ECB/NoPadding");
            _Decryptor.init(2, _Key);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void initIV() {
        try {
            byte[] initVec = strUtils.hexToByteArray("FFFFFFFFFFFFFFFF");
            _IV = _Encryptor.doFinal(initVec);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void xorBytes(byte[] a, byte[] b) {
        for (int i = 0; i < a.length; ++i) {
            int aVal = a[i] & 0xFF;
            int bVal = b[i] & 0xFF;
            a[i] = (byte)(aVal ^ bVal);
        }
    }

    public static void xorBytes(byte[] a, byte[] b, int l) {
        for (int i = 0; i < l; ++i) {
            int aVal = a[i] & 0xFF;
            int bVal = b[i] & 0xFF;
            a[i] = (byte)(aVal ^ bVal);
        }
    }

    public static byte[] Encrypt(byte[] inData) {
        try {
            byte[] CV = Arrays.copyOf(_IV, _IV.length);
            byte[] ret = new byte[inData.length];
            int blocks_len = inData.length / 8;
            int left_len = inData.length % 8;
            for (int i = 0; i < blocks_len; ++i) {
                byte[] temp = Arrays.copyOfRange(inData, i * 8, i * 8 + 8);
                xorBytes(temp, CV);
                temp = _Encryptor.doFinal(temp);
                xorBytes(CV, temp);
                System.arraycopy(temp, 0, ret, i * 8, 8);
            }
            if (left_len != 0) {
                CV = _Encryptor.doFinal(CV);
                byte[] temp = Arrays.copyOfRange(inData, blocks_len * 8, blocks_len * 8 + left_len);
                xorBytes(temp, CV, left_len);
                System.arraycopy(temp, 0, ret, blocks_len * 8, temp.length);
            }
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String encryptString(String inputString) {
        try {
            byte[] inData = inputString.getBytes(StandardCharsets.UTF_8);
            byte[] outData = this.Encrypt(inData);
            return strUtils.byteToHex(outData);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] Decrypt(byte[] inData) {
        try {
            byte[] CV = Arrays.copyOf(_IV, _IV.length);
            byte[] ret = new byte[inData.length];
            int blocks_len = inData.length / 8;
            int left_len = inData.length % 8;
            for (int i = 0; i < blocks_len; ++i) {
                byte[] temp = Arrays.copyOfRange(inData, i * 8, i * 8 + 8);
                temp = _Decryptor.doFinal(temp);
                xorBytes(temp, CV);
                System.arraycopy(temp, 0, ret, i * 8, 8);
                for (int j = 0; j < CV.length; ++j) {
                    CV[j] = (byte)(CV[j] ^ inData[i * 8 + j]);
                }
            }
            if (left_len != 0) {
                CV = _Encryptor.doFinal(CV);
                byte[] temp = Arrays.copyOfRange(inData, blocks_len * 8, blocks_len * 8 + left_len);
                xorBytes(temp, CV, left_len);
                for (int j = 0; j < temp.length; ++j) {
                    ret[blocks_len * 8 + j] = temp[j];
                }
            }
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decryptString(String hexString) {
        try {
            byte[] inData = strUtils.hexToByteArray(hexString);
            byte[] outData = Decrypt(inData);
            String res = new String(outData, StandardCharsets.UTF_8);
            res = ReadabilityChecker.assessReadability(res) ? res : null;

            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static {
        navicat11Des.initKey(DefaultUserKey);
        navicat11Des.initChiperEncrypt();
        navicat11Des.initChiperDecrypt();
        navicat11Des.initIV();
    }

}
