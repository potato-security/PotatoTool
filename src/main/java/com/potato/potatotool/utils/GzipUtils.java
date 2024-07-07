package com.potato.potatotool.utils;

/**
 * @author Potato
 * @date 2024/7/1 09:43
 */
import java.io.*;
import java.util.zip.*;

import static com.potato.potatotool.ToStart.debugMode;

public class GzipUtils {

    /**
     *  对目标文件进行GZIP压缩
     */
    public static void GzipFile(String sourceFilePath, String gzipFilePath) {
        try (FileInputStream fis = new FileInputStream(sourceFilePath);
             FileOutputStream fos = new FileOutputStream(gzipFilePath);
             GZIPOutputStream gzipOS = new GZIPOutputStream(fos)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                gzipOS.write(buffer, 0, length);
            }

            gzipOS.finish();
            System.out.println("File compressed to: " + gzipFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  对目标文件进行GZIP解压
     */
    public static void unGzipFile(String gzipFilePath, String destFilePath) {
        try (FileInputStream fis = new FileInputStream(gzipFilePath);
             GZIPInputStream gzipIS = new GZIPInputStream(fis);
             FileOutputStream fos = new FileOutputStream(destFilePath)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = gzipIS.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            System.out.println("File decompressed to: " + destFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     *  数据进行GZIP压缩
     * @param data   需要压缩的byte数组
     * @return       进行gzip压缩
     */
    public static byte[] GzipGetCompressedData(byte[] data) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
            gzipOutputStream.write(data);
        } catch (IOException e) {
            if(debugMode)e.printStackTrace();
        }
        return outputStream.toByteArray();
    }

    /**
     *  数据进行GZIP解压   [hax特征开头:1f8b]
     * @param compressedData   被压缩的byte数组
     * @return                 解gzip压缩
     */
    public static byte[] GzipDecompress(byte[] compressedData) {
        if(!strUtils.byteToHex(compressedData).toLowerCase().startsWith("1f8b")) return null;
        byte[] buffer = new byte[1024];
        try{
            ByteArrayInputStream inputStream = new ByteArrayInputStream(compressedData);
            GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            int len;
            while ((len = gzipInputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }

            return outputStream.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }
}
