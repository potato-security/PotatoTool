package com.potato.potatotool.utils.data;

import java.io.*;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.potato.potatotool.utils.core.Constants.getResourceFilePath;
import static com.potato.potatotool.utils.core.Constants.listFiles;

/**
 * @author Potato
 * @date 2023/4/17 16:32
 */


public class UnZipUtils {

    private static final int BUFFER_SIZE = 4096;


    /**
     *      解压zip到指定目录，并删除该zip
     *
     * @param zipFilePath   zip文件路径
     * @param destDirectory 目标解压路径
     * @param removeFolder  剔除某文件夹（该变量为选填项）
     * @throws IOException
     */
    public static List<String> unZip(String zipFilePath, String destDirectory, String... removeFolder) throws IOException {

        List<String> pathAll = new ArrayList<String>();

        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }

        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();

        while (entry != null) {

            String entryName = entry.getName();
            if (removeFolder.length == 1) {
//                entryName = entryName.replace(removeFolder[0],"");
                if(entryName.startsWith(removeFolder[0])){
                    zipIn.closeEntry();
                    entry = zipIn.getNextEntry();
                    continue;
                }
            }

            String filePath = destDirectory + File.separator + entryName;

            if (!entryName.equals("/.gitignore")) {

                if (!entry.isDirectory()) {
                    pathAll.add(filePath);
                    extractFile(zipIn, filePath);
                } else {
                    File dir = new File(filePath);
                    dir.mkdir();
                }

            }

            zipIn.closeEntry();
            entry = zipIn.getNextEntry();

        }

        zipIn.close();

        new File(zipFilePath).delete();

        return pathAll;

    }


    /**
     *  压缩文件中读取的数据。使用一循环来读取压缩文件中的数据并存储对应路径结构。
     *
     * @param zipIn     zip内文件数据流
     * @param filePath  文件路径信息
     * @throws IOException
     */
    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {

        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();

    }


    /**
     *      解压指定jar到指定目录/读取资源文件相对路径
     *
     * @param zipFilePath   zip文件路径
     * @param destDirectory 目标解压路径（该变量为选填项）
     * @param getFolder     筛选某文件夹（该变量为选填项）
     * @throws IOException
     */
    public static ArrayList<String> unZipGetJarResourcesPath(String zipFilePath, String destDirectory, String... getFolder) {
        ArrayList<String> pathAll = new ArrayList<String>();

        try {
            ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
            ZipEntry entry = zipIn.getNextEntry();


            while (entry != null) {
                if (destDirectory != null) {

                    File destDir = new File(destDirectory);
                    if (!destDir.exists()) {
                        destDir.mkdir();
                    }

                    String entryName = entry.getName();
                    if (getFolder.length == 1) {
                        if (!entryName.startsWith(getFolder[0])) {
                            zipIn.closeEntry();
                            entry = zipIn.getNextEntry();
                            continue;
                        }
                    }

                    String filePath = destDirectory + File.separator + entryName;

                    if (!entryName.equals("/.gitignore")) {

                        if (!entry.isDirectory()) {
                            pathAll.add(filePath);
                            extractFile(zipIn, filePath);
                        } else {
                            File dir = new File(filePath);
                            dir.mkdir();
                        }

                    }

                } else {

                    String entryName = entry.getName();
                    if (getFolder.length == 1) {
                        if (!entryName.startsWith(getFolder[0])) {
                            zipIn.closeEntry();
                            entry = zipIn.getNextEntry();
                            continue;
                        }
                    }

                    String filePath = entryName;

                    if (!entryName.equals("/.gitignore")) {

                        if (!entry.isDirectory()) {
                            pathAll.add(filePath);
                        }

                    }

                }

                zipIn.closeEntry();
                entry = zipIn.getNextEntry();

            }

            zipIn.close();
        }catch (Exception e) {

            String resourcesPath = "";

            if (getFolder.length == 1) {
                resourcesPath = getResourceFilePath(getFolder[0]);
            }else {
                try {
                    resourcesPath = URLDecoder.decode(UnZipUtils.class.getClassLoader().getResource("").getPath(), "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    ex.printStackTrace();
                }
            }

            pathAll = listFiles(resourcesPath);

        }

        return pathAll;

    }

    /**
     *      解压当前jar到指定目录/读取资源文件相对路径
     *
     * @param destDirectory 目标解压路径（该变量为选填项）
     * @param getFolder     筛选某文件夹（该变量为选填项）
     * @throws IOException  异常时，不应读取jar,应读取绝对目录（不打包时运行）
     */
    public static ArrayList<String> unZipGetCurrentJarResourcesPath(String destDirectory, String... getFolder){
        ArrayList<String> pathAll = new ArrayList<String>();

        try {
            String zipFilePath = new File(UnZipUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getAbsolutePath();

            ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
            ZipEntry entry = zipIn.getNextEntry();


            while (entry != null) {

                if (destDirectory != null) {

                    File destDir = new File(destDirectory);
                    if (!destDir.exists()) {
                        destDir.mkdir();
                    }

                    String entryName = entry.getName();
                    if (getFolder.length == 1) {
                        if (!entryName.startsWith(getFolder[0])) {
                            zipIn.closeEntry();
                            entry = zipIn.getNextEntry();
                            continue;
                        }
                    }

                    String filePath = destDirectory + File.separator + entryName;

                    if (!entryName.equals("/.gitignore")) {

                        if (!entry.isDirectory()) {
                            pathAll.add(filePath);
                            extractFile(zipIn, filePath);
                        } else {
                            File dir = new File(filePath);
                            dir.mkdir();
                        }

                    }

                } else {

                    String entryName = entry.getName();
                    if (getFolder.length == 1) {
                        if (!entryName.startsWith(getFolder[0])) {
                            zipIn.closeEntry();
                            entry = zipIn.getNextEntry();
                            continue;
                        }
                    }

                    String filePath = entryName;

                    if (!entryName.equals("/.gitignore")) {

                        if (!entry.isDirectory()) {
                            pathAll.add(filePath);
                        }

                    }

                }

                zipIn.closeEntry();
                entry = zipIn.getNextEntry();

            }

            zipIn.close();
        }catch (Exception e) {

            String resourcesPath = "";

            if (getFolder.length == 1) {
                resourcesPath = getResourceFilePath(getFolder[0]);
            }else {
                try {
                    resourcesPath = URLDecoder.decode(UnZipUtils.class.getClassLoader().getResource("").getPath(), "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    ex.printStackTrace();
                }
            }

            pathAll = listFiles(resourcesPath);

        }

        Collections.sort(pathAll); // list内容按顺序排序

        return pathAll;

    }

}