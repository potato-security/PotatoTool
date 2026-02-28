package com.potato.potatotool.content.redTeam.vulnScanner.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.gson.Gson;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.*;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.util.constructor.NucleiConstructor;
import com.potato.potatotool.content.redTeam.vulnScanner.util.constructor.XrayConstructor;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.PocConverterFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * @author Potato
 * @date 2025/2/19 16:30
 * POC格式转换工具类，用于将不同格式的POC转换为通用PocObj
 * 该类已重构为门面模式，实际转换逻辑已移至converter包下的具体转换器实现类
 */
public class PocConverter {
    private static final Gson gson = new Gson();

    /**
     * 将Nuclei YAML POC转换为通用PocObj
     * @param nucleiPoc Nuclei YAML POC对象
     * @return 通用PocObj对象
     */
    public static PocObj.Poc fromNuclei(NucleiYamlObj.Poc nucleiPoc) {
        return PocConverterFactory.fromNuclei(nucleiPoc);
    }
    
    /**
     * 将Xray YAML POC转换为通用PocObj
     * @param xrayPoc Xray YAML POC对象
     * @return 通用PocObj对象
     */
    public static PocObj.Poc fromXray(XrayYamlObj.Poc xrayPoc) {
        return PocConverterFactory.fromXray(xrayPoc);
    }
    
    /**
     * 将Goby JSON POC转换为通用PocObj
     * @param gobyPoc Goby JSON POC对象
     * @return 通用PocObj对象
     */
    public static PocObj.Poc fromGoby(GobyJsonObj.PocJson gobyPoc) {
        return PocConverterFactory.fromGoby(gobyPoc);
    }
    
    /**
     * 将Pocsuite JSON POC转换为通用PocObj
     * @param pocsuitePoc Pocsuite JSON POC对象
     * @return 通用PocObj对象
     */
    public static PocObj.Poc fromPocsuite(PocsuiteJsonObj.PocJson pocsuitePoc) {
        return PocConverterFactory.fromPocsuite(pocsuitePoc);
    }

    /**
     * 通用JSON文件加载方法
     * @param fileName 文件名
     * @param classOfT 目标类型
     * @param <T> 泛型类型
     * @return 解析后的对象
     * @throws IOException 文件读取异常
     */
    private static <T> T loadJsonFile(String fileName, Class<T> classOfT) throws IOException {
        try {
            byte[] fileContent = Files.readAllBytes(Paths.get(fileName));
            return gson.fromJson(new String(fileContent, StandardCharsets.UTF_8), classOfT);
        } catch (Exception e) {
            System.err.println("Error loading JSON file: " + e.getMessage());
            throw new IOException("Failed to load JSON file: " + fileName, e);
        }
    }
    
    public static GobyJsonObj.PocJson loadGobyJsonPocFile(String fileName) throws IOException {
        return loadJsonFile(fileName, GobyJsonObj.PocJson.class);
    }

    public static PocsuiteJsonObj.PocJson loadPocsuiteJsonPocFile(String fileName) throws IOException {
        return loadJsonFile(fileName, PocsuiteJsonObj.PocJson.class);
    }

    public static NucleiYamlObj.Poc loadNucleiYamlPocFile(String fileName) throws IOException {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        NucleiConstructor constructor = new NucleiConstructor(NucleiYamlObj.Poc.class, loaderOptions);

        Yaml yaml = new Yaml(constructor);
        NucleiYamlObj.Poc poc = new NucleiYamlObj.Poc();
        try (InputStream input = new FileInputStream(fileName)) {
            poc = yaml.loadAs(input, NucleiYamlObj.Poc.class);
            
            // 添加详细的调试日志
            if (poc != null) {
                System.out.println("=== YAML 解析成功 ===");
                System.out.println("文件: " + new File(fileName).getName());
                System.out.println("ID: " + poc.getId());
                System.out.println("协议检测:");
                System.out.println("  - HTTP: " + (poc.getHttp() != null ? poc.getHttp().size() + " 个" : "null"));
                System.out.println("  - TCP: " + (poc.getTcp() != null ? poc.getTcp().size() + " 个" : "null"));
                System.out.println("  - DNS: " + (poc.getDns() != null ? poc.getDns().size() + " 个" : "null"));
                System.out.println("  - SSL: " + (poc.getSsl() != null ? poc.getSsl().size() + " 个" : "null"));
                System.out.println("  - WebSocket: " + (poc.getWebsocket() != null ? poc.getWebsocket().size() + " 个" : "null"));
                System.out.println("  - File: " + (poc.getFile() != null ? poc.getFile().size() + " 个" : "null"));
                System.out.println("  - Code: " + (poc.getCode() != null ? poc.getCode().size() + " 个" : "null"));
                System.out.println("  - Headless: " + (poc.getHeadless() != null ? poc.getHeadless().size() + " 个" : "null"));
                System.out.println("  - Flow: " + poc.getFlow());
            }
        } catch (Exception e) {
            System.err.println("Error loading Nuclei YAML POC file: " + e.getMessage());
            e.printStackTrace(); // 打印完整堆栈信息
        }
        return poc;
    }

    public static XrayYamlObj.Poc loadXrayYamlPocFile(String fileName) throws IOException {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        XrayConstructor constructor = new XrayConstructor(XrayYamlObj.Poc.class, loaderOptions);

        Yaml yaml = new Yaml(constructor);
        XrayYamlObj.Poc poc = new XrayYamlObj.Poc();
        try (InputStream input = new FileInputStream(fileName)) {
            poc = yaml.loadAs(input, XrayYamlObj.Poc.class);
        } catch (Exception e) {
            System.err.println("Error loading Xray YAML POC file: " + e.getMessage());
        }
        return poc;
    }

    // ==================== 从内容加载 ====================
    
    public static NucleiYamlObj.Poc loadNucleiYamlFromContent(String content) {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        NucleiConstructor constructor = new NucleiConstructor(NucleiYamlObj.Poc.class, loaderOptions);
        Yaml yaml = new Yaml(constructor);
        try {
            return yaml.loadAs(content, NucleiYamlObj.Poc.class);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static XrayYamlObj.Poc loadXrayYamlFromContent(String content) {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        XrayConstructor constructor = new XrayConstructor(XrayYamlObj.Poc.class, loaderOptions);
        Yaml yaml = new Yaml(constructor);
        try {
            return yaml.loadAs(content, XrayYamlObj.Poc.class);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static GobyJsonObj.PocJson loadGobyJsonFromContent(String content) {
        try {
            return new Gson().fromJson(content, GobyJsonObj.PocJson.class);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static PocsuiteJsonObj.PocJson loadPocsuiteJsonFromContent(String content) {
        try {
            return new Gson().fromJson(content, PocsuiteJsonObj.PocJson.class);
        } catch (Exception e) {
            return null;
        }
    }

}