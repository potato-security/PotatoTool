package com.potato.potatotool.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * @author Potato
 * @date 2023/4/25 11:36
 */
public class processUtils {

    public static String processBuild(String[] command, String outFilePath) throws Exception {

        String result = "";

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result += line + "\n";
            }
        }

        if (outFilePath!=null && outFilePath!="") {
            strUtils.createFile(result ,outFilePath);
        }

        return result;

    }

}
