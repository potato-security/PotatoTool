package com.potato.potatotool.content.redTeam.vulnScanner.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.*;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * POC管理器，用于统一管理POC文件的解析和转换
 * @author Potato
 * @date 2025/2/20 10:30
 */
public class PocManager {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    /**
     * 解析指定目录下的所有POC文件
     * @param pocDirPath POC文件目录路径
     * @return 解析成功的POC对象列表
     */
    public static List<PocObj.Poc> parseAllPocs(String pocDirPath) {
        List<PocObj.Poc> pocList = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(Paths.get(pocDirPath))) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        String filePath = path.toString();
                        try {
                            System.out.println("开始解析文件: " + filePath);
                            PocObj.Poc pocObj = parsePocFile(filePath);
                            if (pocObj != null) {
                                pocList.add(pocObj);
                                System.out.println("解析成功: " + filePath);
                            }
                        } catch (Exception e) {
                            System.err.println("解析文件失败: " + filePath);
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            System.err.println("遍历目录失败: " + pocDirPath);
            e.printStackTrace();
        }
        
        return pocList;
    }
    
    /**
     * 解析单个POC文件
     * @param filePath POC文件路径
     * @return 解析后的POC对象
     * @throws Exception 解析失败时抛出异常
     */
    public static PocObj.Poc parsePocFile(String filePath) throws Exception {
        String lowerFilePath = filePath.toLowerCase();
        PocObj.Poc pocObj = null;
        
        // 根据文件扩展名和内容特征选择合适的解析方法
        if (lowerFilePath.endsWith(".yaml") || lowerFilePath.endsWith(".yml")) {
            // 先尝试解析为Nuclei格式
            NucleiYamlObj.Poc nucleiYamlObj = PocConverter.loadNucleiYamlPocFile(filePath);
            pocObj = PocConverter.fromNuclei(nucleiYamlObj);
            // 如果不是Nuclei格式，尝试解析为Xray格式
            if (pocObj == null) {
                XrayYamlObj.Poc xrayYamlObj = PocConverter.loadXrayYamlPocFile(filePath);
                pocObj = PocConverter.fromXray(xrayYamlObj);
            }
        } else if (lowerFilePath.endsWith(".json")) {
            // 先尝试解析为Goby格式
            GobyJsonObj.PocJson gobyJsonObj = PocConverter.loadGobyJsonPocFile(filePath);
            pocObj = PocConverter.fromGoby(gobyJsonObj);
            // 如果不是Goby格式，尝试解析为Pocsuite格式
            if (pocObj == null) {
                PocsuiteJsonObj.PocJson pocsuiteJsonObj = PocConverter.loadPocsuiteJsonPocFile(filePath);
                pocObj = PocConverter.fromPocsuite(pocsuiteJsonObj);
            }
        }
        
        if (pocObj == null) {
            System.err.println("无法识别的POC格式或解析失败: " + filePath);
        }
        
        return pocObj;
    }
    
}