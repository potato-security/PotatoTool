package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils;

import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.misc.ReadabilityChecker;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.Objects;

/**
 * @author Potato
 * @date 2024/3/28 13:13
 */
public class QiangzhiDec {

    public static String desDecode(String str){
        String key = "02A46370BC76";

        if (str.startsWith("#!@")) {
            String res = decrypt(str.substring(3), key);

            String res_bs64 = new StrUtils().base64Decode(res);
            res_bs64 = ReadabilityChecker.assessReadability(res_bs64) ? res_bs64 : null;

            return res_bs64 != null ? res_bs64 : res;
        }

        return null;
    }

    public static String decrypt(String var0, String var1) {
        return new String(decrypt(var0.getBytes(), var1.getBytes()));
    }

    public static byte[] decrypt(byte[] var0, byte[] var1) {
        var0 = encodeKey(Objects.requireNonNull(Base64_decode(var0, 0, var0.length)), var1);
        ByteArrayOutputStream var2 = new ByteArrayOutputStream();
        for (int var3 = 0; var3 < var0.length; ++var3) {
            byte var4 = var0[var3];
            var2.write(var0[++var3] ^ var4);
        }
        return var2.toByteArray();
    }

    public static byte[] encodeKey(byte[] var0, byte[] var1) {
        var1 = CalcMD5(new String(var1)).toLowerCase().getBytes();
        int var2 = 0;
        byte[] var3 = new byte[var0.length];
        for (int var4 = 0; var4 < var0.length; ++var4) {
            int n = var2 = var2 == var1.length ? 0 : var2;
            var2 = (byte)(var2 + 1);
            var3[var4] = (byte)(var0[var4] ^ var1[n]);
        }
        return var3;
    }

    private static byte[] digesta;
    public static String CalcMD5(String var1) {
        try {
            MessageDigest var2 = MessageDigest.getInstance("MD5");
            var2.update(var1.getBytes());
            digesta = var2.digest();
        } catch (Exception var3) {
            var3.printStackTrace();
        }
        return byte2hex(digesta);
    }

    private static String byte2hex(byte[] var1) {
        String var2 = "";
        String var3 = "";
        for (int var4 = 0; var4 < var1.length; ++var4) {
            var3 = Integer.toHexString(var1[var4] & 0xFF);
            var2 = var3.length() == 1 ? var2 + "0" + var3 : var2 + var3;
        }
        return var2;
    }

    static byte[] DECODABET = new byte[]{-9, -9, -9, -9, -9, -9, -9, -9, -9, -5, -5, -9, -9, -5, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -5, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, 62, -9, -9, -9, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -9, -9, -9, -1, -9, -9, -9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -9, -9, -9, -9, -9, -9, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -9, -9, -9, -9};
    public static byte[] Base64_decode(byte[] var0, int var1, int var2) {
        int var3 = var2 * 3 / 4;
        byte[] var4 = new byte[var3];
        int var5 = 0;
        byte[] var6 = new byte[4];
        int var7 = 0;
        boolean var8 = false;
        boolean var9 = false;
        boolean var10 = false;
        for (int var12 = var1; var12 < var1 + var2; ++var12) {
            byte var13 = (byte)(var0[var12] & 0x7F);
            byte var14 = DECODABET[var13];
            if (var14 < -5) {
                System.err.println("Bad Base64 input character at " + var12 + ": " + var0[var12] + "(decimal)");
                return null;
            }
            if (var14 < -1) continue;
            var6[var7++] = var13;
            if (var7 <= 3) continue;
            var5 += decode4to3(var6, 0, var4, var5);
            var7 = 0;
            if (var13 == 61) break;
        }
        byte[] var11 = new byte[var5];
        System.arraycopy(var4, 0, var11, 0, var5);
        return var11;
    }
    private static int decode4to3(byte[] var0, int var1, byte[] var2, int var3) {
        if (var0[var1 + 2] == 61) {
            int var4 = (DECODABET[var0[var1]] & 0xFF) << 18 | (DECODABET[var0[var1 + 1]] & 0xFF) << 12;
            var2[var3] = (byte)(var4 >>> 16);
            return 1;
        }
        if (var0[var1 + 3] == 61) {
            int var4 = (DECODABET[var0[var1]] & 0xFF) << 18 | (DECODABET[var0[var1 + 1]] & 0xFF) << 12 | (DECODABET[var0[var1 + 2]] & 0xFF) << 6;
            var2[var3] = (byte)(var4 >>> 16);
            var2[var3 + 1] = (byte)(var4 >>> 8);
            return 2;
        }
        try {
            int var4 = (DECODABET[var0[var1]] & 0xFF) << 18 | (DECODABET[var0[var1 + 1]] & 0xFF) << 12 | (DECODABET[var0[var1 + 2]] & 0xFF) << 6 | DECODABET[var0[var1 + 3]] & 0xFF;
            var2[var3] = (byte)(var4 >> 16);
            var2[var3 + 1] = (byte)(var4 >> 8);
            var2[var3 + 2] = (byte)var4;
            return 3;
        } catch (Exception var5) {
            return -1;
        }
    }

    public static void main(String []args) {
        String res = QiangzhiDec.desDecode("#!@QndcNV8ySyZWZV8yUGFKcA==");
        System.out.println(res);
        String ress = QiangzhiDec.desDecode("#!@Qk9cAF8bSyhWDl9EUBdKPVIeBStKFF9i");
        System.out.println(ress);

    }

}
