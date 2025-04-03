package com.potato.potatotool.content.redTeam.vulnScanner.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocsuiteJsonObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.XrayYamlObj;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 根据文件格式统一解析 PoC 内容
 */
public class PocFileParser {
    /**
     * 读取本地文件内容，并根据文件扩展名自动选择解析器进行 PoC 解析
     *
     * @param filePath 本地 PoC 文件路径
     * @return 解析后的 Map 对象
     * @throws Exception 当文件读取或解析失败时抛出异常
     */
    public static PocObj.Poc parsePocFromFile(String filePath) throws Exception {
        String lowerFilePath = filePath.toLowerCase();
        PocObj.Poc pocObj = null;
        
        if (lowerFilePath.endsWith(".json")) {
//            JsonPocParser runner = new JsonPocParser();
//            GobyJsonObj.PocJson gobyPoc = runner.loadGobyJsonPocFile(filePath);
//            PocObj.Poc xpoc = runner.run(gobyPoc);
//
//            // 获取文件名称（去掉后缀）
//            Path tmp_filePath = Paths.get(lowerFilePath);
//            String fileNameOnly = tmp_filePath.getFileName().toString().replaceFirst("\\.json$", "");
//
//            // 确定输出目录
//            Path outputDir = Paths.get("/Users/a/Desktop/项目开发/PotatoTool/src/main/java/com/potato/potatotool/content/redTeam/vulnScanner/tmpPoc/");
//            Path newPathFileName = outputDir.resolve(fileNameOnly + ".yml");
//            runner.saveXrayPocFile(newPathFileName.toString(), xpoc);
//            GobyJsonObj.PocJson gobyJsonObj = PocConverter.loadGobyJsonPocFile(filePath);
//            pocObj = PocConverter.fromGoby(gobyJsonObj);

            PocsuiteJsonObj.PocJson pocsuiteJsonObj = PocConverter.loadPocsuiteJsonPocFile(filePath);
            pocObj = PocConverter.fromPocsuite(pocsuiteJsonObj);
        } else if (lowerFilePath.endsWith(".yaml") || lowerFilePath.endsWith(".yml")) {
            // Path tmp_filePath = Paths.get(lowerFilePath);
            // String fileNameOnly = tmp_filePath.getFileName().toString().replaceFirst("\\.json$", "");

            // Path outputDir = Paths.get("/Users/a/Desktop/项目开发/PotatoTool/src/main/java/com/potato/potatotool/content/redTeam/vulnScanner/tmpPoc/");
            // Path newPathFileName = outputDir.resolve(fileNameOnly + ".yml");

            // YamlPocParser.parser(filePath);
//            XrayYamlObj.Poc xrayYamlObj = PocConverter.loadXrayYamlPocFile(filePath);
//            pocObj = PocConverter.fromXray(xrayYamlObj);
            NucleiYamlObj.Poc nucleiYamlObj = PocConverter.loadNucleiYamlPocFile(filePath);
            pocObj = PocConverter.fromNuclei(nucleiYamlObj);
        } else {
            System.out.println("Unsupported PoC file format: " + filePath);
        }
        
        // 如果成功解析了POC，则保存到transform目录
        if (pocObj != null) {
            savePocToTransformDir(filePath, pocObj);
        }
        
        return pocObj;
    }
    
    /**
     * 将解析后的POC对象保存到transform目录下的同名JSON文件
     * 
     * @param originalFilePath 原始文件路径
     * @param pocObj 解析后的POC对象
     * @throws Exception 当保存失败时抛出异常
     */
    private static void savePocToTransformDir(String originalFilePath, PocObj.Poc pocObj) throws Exception {
        // 获取原始文件名（不含扩展名）
        Path originalPath = Paths.get(originalFilePath);
        String fileName = originalPath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String fileNameWithoutExt = (dotIndex > 0) ? fileName.substring(0, dotIndex) : fileName;
        
        // 创建transform目录（如果不存在）
        Path transformDir = Paths.get("/Users/a/Desktop/项目开发/PotatoTool/src/main/java/com/potato/potatotool/content/redTeam/vulnScanner/transform/");
        if (!Files.exists(transformDir)) {
            Files.createDirectories(transformDir);
        }
        
        // 构建目标文件路径
        Path targetFilePath = transformDir.resolve(fileNameWithoutExt + ".json");
        
        // 使用Gson将对象转换为JSON并保存
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(targetFilePath.toFile())) {
            gson.toJson(pocObj, writer);
        }
        
        System.out.println("POC已保存到: " + targetFilePath.toAbsolutePath());
    }
}
