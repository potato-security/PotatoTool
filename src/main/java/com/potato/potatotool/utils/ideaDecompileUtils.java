package com.potato.potatotool.utils;

import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Potato
 * @date 2023/4/21 17:13
 */


public class ideaDecompileUtils {


    /**
     * @param classPath     class文件路径
     * @param outfilePath   [选填]输出文件路径
     * @return
     */
    public static String Decompile(String classPath, String... outfilePath){
        String res = null;

        if(!Files.exists(Paths.get(classPath))){
            System.out.println("classPath路径存在问题，请重新选择位置");
            return null;
        }

        String jarDir = strUtils.getCurrentJarDir();
        String javaTempFilePath = jarDir + File.separator + "Decompile" + File.separator + classPath.substring(classPath.lastIndexOf(File.separator) + 1, classPath.lastIndexOf(".")) + ".java";
        Path outputDirPath = Paths.get(javaTempFilePath).getParent();

        if (!Files.exists(outputDirPath)) {
            try {
                Files.createDirectories(outputDirPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String[] arguments = {classPath, String.valueOf(outputDirPath)};

        try {
            ConsoleDecompiler.main(arguments);

            if(!Files.exists(Paths.get(javaTempFilePath))){
                System.out.println("java文件不存在");
            }

            res = new String(strUtils.readFile(javaTempFilePath), StandardCharsets.UTF_8);

            if (outfilePath.length == 1) {
                strUtils.moveFile(javaTempFilePath, outfilePath[0]);
            }
        } catch (Exception e) {
            return null;
        }

        return res;

    }


    /**
     * @param classBytes     classBytes数据
     * @param outfilePath   [选填]输出文件路径
     * @return
     */
    public static String Decompile(byte[] classBytes, String... outfilePath) throws Exception {

        File tempFile = File.createTempFile("temp", ".class");
        tempFile.deleteOnExit();
        FileOutputStream outputStream = new FileOutputStream(tempFile);
        outputStream.write(classBytes);
        outputStream.close();

        return Decompile(tempFile.getAbsolutePath(), outfilePath);

    }

}
