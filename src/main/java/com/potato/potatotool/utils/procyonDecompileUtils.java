package com.potato.potatotool.utils;

import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.ITextOutput;
import com.strobel.decompiler.PlainTextOutput;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import static com.potato.potatotool.utils.strUtils.createFile;

/**
 * @author Potato
 * @date 2023/4/21 17:13
 */


public class procyonDecompileUtils {


    /**
     * @param classPath     class文件路径
     * @param outfilePath   [选填]输出文件路径
     * @return
     */
    public static String Decompile(String classPath, String... outfilePath){
        Path path = Paths.get(classPath);

        if(!Files.exists(path)){
            System.out.println("classPath路径存在问题，请重新选择位置");
            return null;
        }

        if (Files.isDirectory(path)) {
            String dirName = path.getFileName().toString();
            String jarDir = strUtils.getCurrentJarDir();
            String newDirPath = jarDir + File.separator + "Decompile" + File.separator + dirName;
            return decompileFolder(classPath, newDirPath);
        }

        final ITextOutput output = new PlainTextOutput();
        final DecompilerSettings settings = DecompilerSettings.javaDefaults();

        Decompiler.decompile(classPath, output, settings);

        String result = output.toString();

        if (outfilePath.length == 1){
            createFile(result, outfilePath[0]);
        }else{
            String javaTempFilePath = strUtils.getCurrentJarDir() + File.separator + "Decompile" + File.separator + classPath.substring(classPath.lastIndexOf(File.separator) + 1, classPath.lastIndexOf(".")) + ".java";
            Path outputDirPath = Paths.get(javaTempFilePath).getParent();
            if (!Files.exists(outputDirPath)) {
                try {
                    Files.createDirectories(outputDirPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            createFile(result, javaTempFilePath);
        }

        return result;

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

    public static String decompileFolder(String inputFolderPath, String outputFolderPath) {
        Path inputPath = Paths.get(inputFolderPath);
        Path outputPath = Paths.get(outputFolderPath);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        try {
            // 确保输出目录存在
            Files.createDirectories(outputPath);

            // 遍历所有class文件
            Files.walk(inputPath)
                    .filter(path -> path.toString().endsWith(".class"))
                    .forEach(classFile -> {
                        try {
                            // 计算相对路径，保持目录结构
                            Path relativePath = inputPath.relativize(classFile);
                            Path outputFile = outputPath.resolve(
                                    relativePath.toString().replace(".class", ".java")
                            );

                            // 确保输出文件的父目录存在
                            Files.createDirectories(outputFile.getParent());

                            // 执行反编译
                            Decompile(classFile.toString(), outputFile.toString());

                            System.out.println("Successfully decompiled: " + relativePath);
                            successCount.incrementAndGet();

                        } catch (Exception e) {
                            System.err.println("Failed to decompile: " + classFile);
                            e.printStackTrace();
                            failCount.incrementAndGet();
                        }
                    });

            // 输出统计信息
            String res = "反编译完成:" + outputFolderPath + "\n已成功反编译: " + successCount.get() + " files" + "\n反编译失败: " + failCount.get() + " files";
            return res;

        } catch (IOException e) {
            e.printStackTrace();
            return "遍历目录时出错: " + e.getMessage();
        }
    }

    public static void main(String[] args) {
        final ITextOutput output = new PlainTextOutput();
        final DecompilerSettings settings = DecompilerSettings.javaDefaults();

        decompileFolder("/Users/a/Desktop/项目开发/PotatoTool/target/classes/com/potato/potatotool", "/Users/a/Desktop/项目开发/PotatoTool/target/classes/com/potato/123");

        String result = output.toString();
    }
}
