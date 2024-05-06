package com.potato.potatotool.utils;


import static com.potato.potatotool.ToStart.debugMode;

/**
 * @author Potato
 * @date 2023/4/19 18:23
 */

// 用友NC DES加解密
public class ncDesUtils {

    private static long key = 1231234234L;

    public ncDesUtils() {
    }

    public static void setKey(long k) {
        key = k;
    }


    public byte[] decode(String s, long k) throws Exception {
        ncDES des = new ncDES(k);
        byte[] sBytes = s.getBytes();
        int bytesLength = sBytes.length / 16;
        byte[] byteList = new byte[bytesLength * 8];

        for (int i = 0; i < bytesLength; ++i) {
            byte[] theBytes = new byte[8];

            for (int j = 0; j <= 7; ++j) {
                byte byte1 = (byte) (sBytes[16 * i + 2 * j] - 97);
                byte byte2 = (byte) (sBytes[16 * i + 2 * j + 1] - 97);
                theBytes[j] = (byte) (byte1 * 16 + byte2);
            }

            long x = des.bytes2long(theBytes);
            byte[] result = new byte[8];
            des.long2bytes(des.decrypt(x), result);
            System.arraycopy(result, 0, byteList, i * 8, 8);
        }

        byte[] res = this.subArr(byteList);

        boolean readability = ReadabilityChecker.assessReadability(res);
        return (!readability)? null : res;
    }

    public byte[] decode(String s) {
        try {
            return this.decode(s, key);
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
            return null;
        }
    }

    public String decodeToStr(String s) {
        try {
            return new String(this.decode(s, key));
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
            return null;
        }
    }
    public String decodeToStr(String s, long k) {
        try {
            return new String(this.decode(s, k));
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
            return null;
        }
    }

    // byte[]结果格式化
    private byte[] subArr(byte[] a) {
        int al = a.length;
        int end = this.checkEnd(a);
        if (end == 0) {
            return a;
        } else {
            byte[] rtn = new byte[al - end];
            System.arraycopy(a, 0, rtn, 0, al - end);
            return rtn;
        }
    }
    private int checkEnd(byte[] arr) {
        int rtn = 0;

        for (int i = arr.length - 1; i > 0 && arr[i] == 32; --i) {
            ++rtn;
        }

        return rtn;
    }

    public static void main(String []args) {
        String res= strUtils.seeyonDbDecode("/1.0/YmNtdTMxMjhB");
        System.out.println(res);
    }

}
