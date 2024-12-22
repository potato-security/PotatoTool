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

        if(!Files.exists(Paths.get(classPath))){
            System.out.println("classPath路径存在问题，请重新选择位置");
            return null;
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
}
