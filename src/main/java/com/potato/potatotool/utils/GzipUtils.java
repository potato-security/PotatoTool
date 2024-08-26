package com.potato.potatotool.utils;

/**
 * @author Potato
 * @date 2024/7/1 09:43
 */
import java.io.*;
import java.util.zip.*;

import static com.potato.potatotool.ToStart.debugMode;

public class GzipUtils {

    public static void main(String[] args) {
        GzipFile("/Users/a/.PotatoTool/md5_database.db","/Users/a/.PotatoTool/md51_database.db");
    }

    /**
     *  对目标文件进行GZIP压缩
     */
    public static void GzipFile(String sourceFilePath, String gzipFilePath) {
        try (FileInputStream fis = new FileInputStream(sourceFilePath);
             FileOutputStream fos = new FileOutputStream(gzipFilePath);
             GZIPOutputStream gzipOS = new GZIPOutputStream(fos)) {

            byte[] buffer = new byte[16 * 1024];
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

            byte[] buffer = new byte[16 * 1024];
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
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {

            gzipOutputStream.write(data);

            return outputStream.toByteArray();
        } catch (IOException e) {
            if(debugMode)e.printStackTrace();
            return null;
        }
    }

    /**
     *  数据进行GZIP解压   [hax特征开头:1f8b]
     * @param compressedData   被压缩的byte数组
     * @return                 解gzip压缩
     */
    public static byte[] GzipDecompress(byte[] compressedData) {
        if (compressedData == null || compressedData.length < 2 || compressedData[0] != (byte) 0x1F || compressedData[1] != (byte) 0x8B) {
            return null;
        }
        byte[] buffer = new byte[16 * 1024];
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

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
