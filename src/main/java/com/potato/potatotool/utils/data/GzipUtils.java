package com.potato.potatotool.utils.data;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.*;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * Gzip压缩/解压工具类
 * 
 * @author Potato
 * @date 2024/7/1 09:43
 */

public class GzipUtils {

    public static void main(String[] args) {
        GzipFile("/Users/a/.PotatoTool/md5_database.db","/Users/a/.PotatoTool/md5_database_gzip.db");
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
    public static void unGzipFile(String gzipFilePath, String destFilePath, boolean deleteGzipFile) {
        // 参数校验
        if (gzipFilePath == null || destFilePath == null) {
            if(debugMode) throw new IllegalArgumentException("File paths cannot be null");
        }

        Path sourcePath = Paths.get(gzipFilePath);
        Path targetPath = Paths.get(destFilePath);

        // 检查源文件
        if (!Files.exists(sourcePath)) {
            if(debugMode) throw new IllegalArgumentException("Source file not found: " + gzipFilePath);
        }

        // 确保目标目录存在
        try {
            Files.createDirectories(targetPath.getParent());
        } catch (IOException e) {
            if(debugMode) throw new IllegalArgumentException("Failed to create target directory: " + targetPath.getParent(), e);
        }

        // 使用嵌套的 try-with-resources 确保正确的关闭顺序
        try (FileInputStream fis = new FileInputStream(gzipFilePath);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            try (GZIPInputStream gzipIS = new GZIPInputStream(bis);
                 FileOutputStream fos = new FileOutputStream(destFilePath);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {

                byte[] buffer = new byte[16 * 1024];
                int length;
                while ((length = gzipIS.read(buffer)) > 0) {
                    bos.write(buffer, 0, length);
                }

                // 确保所有数据都写入磁盘
                bos.flush();

                System.out.println("File decompressed successfully to: " + destFilePath);
            }
        } catch (IOException e) {
            // 删除可能部分写入的目标文件
            try {
                Files.deleteIfExists(targetPath);
            } catch (IOException deleteEx) {
                e.addSuppressed(deleteEx);
            }
            if(debugMode) throw new IllegalArgumentException("Failed to decompress file: " + gzipFilePath, e);
        }

        // 如果需要删除源文件
        if (deleteGzipFile) {
            try {
                Files.delete(sourcePath);
            } catch (IOException e) {
                if(debugMode) throw new IllegalArgumentException("Failed to delete source file: " + gzipFilePath, e);
            }
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
