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
 * @date 2025/3/20 10:30
 */
public class PocManager {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String TRANSFORM_DIR = "/Users/a/Desktop/项目开发/PotatoTool/src/main/java/com/potato/potatotool/content/redTeam/vulnScanner/transform/";
    // private static final String TRANSFORM_DIR = "C:\\Users\\potato\\Desktop\\PotatoTool\\src\\main\\java\\com\\potato\\potatotool\\content\\redTeam\\vulnScanner\\transform";
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
        
        // 如果成功解析了POC，则保存到transform目录 DEBUG方法
        if (pocObj != null) {
//            savePocToTransformDir(filePath, pocObj);
        } else {
            System.err.println("无法识别的POC格式或解析失败: " + filePath);
        }
        
        return pocObj;
    }
    
    /**
     * 将解析后的POC对象保存到transform目录下的同名JSON文件
     * 
     * @param originalFilePath 原始文件路径
     * @param pocObj 解析后的POC对象
     * @throws IOException 当保存失败时抛出异常
     */
    private static void savePocToTransformDir(String originalFilePath, PocObj.Poc pocObj) throws IOException {
        // 获取原始文件名（不含扩展名）
        Path originalPath = Paths.get(originalFilePath);
        String fileName = originalPath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String fileNameWithoutExt = (dotIndex > 0) ? fileName.substring(0, dotIndex) : fileName;
        
        // 创建transform目录（如果不存在）
        Path transformDir = Paths.get(TRANSFORM_DIR);
        if (!Files.exists(transformDir)) {
            Files.createDirectories(transformDir);
        }
        
        // 构建目标文件路径
        Path targetFilePath = transformDir.resolve(fileNameWithoutExt + ".json");
        
        // 使用Gson将对象转换为JSON并保存
        try (FileWriter writer = new FileWriter(targetFilePath.toFile())) {
            gson.toJson(pocObj, writer);
        }
        
        System.out.println("POC已保存到: " + targetFilePath.toAbsolutePath());
    }
}