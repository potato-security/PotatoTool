package com.potato.potatotool.content.redTeam.vulnScanner.util.converter;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.*;
import com.potato.potatotool.content.redTeam.vulnScanner.exception.PocParseException;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocClassifier;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocConverter;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * POC转换器注册表
 * 负责管理和调度不同格式的POC转换器
 * 
 * 新增功能：POC缓存机制
 * - 使用LRU缓存策略，避免重复解析相同POC文件
 * - 缓存基于文件路径和修改时间，确保文件更新后重新解析
 * 
 * @author Potato
 * @date 2024-10-28
 */
public class PocConverterRegistry {
    
    private static PocConverterRegistry instance;
    
    // 转换器映射
    private final Map<String, IPocConverter<?>> converterMap = new HashMap<>();
    
    // POC缓存 (LRU缓存)
    private static final int MAX_CACHE_SIZE = 500;
    private final Map<String, CachedPoc> pocCache = new LinkedHashMap<String, CachedPoc>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CachedPoc> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };
    
    // 缓存项
    private static class CachedPoc {
        final PocObj.Poc poc;
        final long lastModified;
        
        CachedPoc(PocObj.Poc poc, long lastModified) {
            this.poc = poc;
            this.lastModified = lastModified;
        }
    }
    
    // 各格式的转换器实例
    private final NucleiPocConverter nucleiConverter;
    private final XrayPocConverter xrayConverter;
    private final GobyPocConverter gobyConverter;
    private final PocsuitePocConverter pocsuiteConverter;
    
    private PocConverterRegistry() {
        nucleiConverter = new NucleiPocConverter();
        xrayConverter = new XrayPocConverter();
        gobyConverter = new GobyPocConverter();
        pocsuiteConverter = new PocsuitePocConverter();
        
        // 注册转换器
        registerDefaultConverters();
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized PocConverterRegistry getInstance() {
        if (instance == null) {
            instance = new PocConverterRegistry();
        }
        return instance;
    }
    
    /**
     * 注册默认转换器
     */
    private void registerDefaultConverters() {
        // Nuclei YAML格式
        converterMap.put("nuclei-yaml", nucleiConverter);
        converterMap.put("nuclei-yml", nucleiConverter);
        
        // Xray YAML格式
        converterMap.put("xray-yaml", xrayConverter);
        converterMap.put("xray-yml", xrayConverter);
        
        // Goby JSON格式
        converterMap.put("goby-json", gobyConverter);
        
        // Pocsuite JSON格式
        converterMap.put("pocsuite-json", pocsuiteConverter);
    }
    
    /**
     * 注册自定义转换器
     * 
     * @param format 格式标识
     * @param converter 转换器实例
     */
    public void registerConverter(String format, IPocConverter<?> converter) {
        if (format != null && converter != null) {
            converterMap.put(format.toLowerCase(), converter);
        }
    }
    
    /**
     * 获取转换器
     * 
     * @param format 格式标识
     * @return 转换器实例，如果不存在返回null
     */
    public IPocConverter<?> getConverter(String format) {
        if (format == null) {
            return null;
        }
        return converterMap.get(format.toLowerCase());
    }
    
    /**
     * 转换POC文件
     * 自动识别文件格式并转换
     * 支持缓存机制，避免重复解析
     * 
     * @param filePath POC文件路径
     * @return 转换后的通用POC对象
     * @throws PocParseException 当解析失败时
     */
    public PocObj.Poc convertPocFile(String filePath) throws PocParseException {
        File file = new File(filePath);
        
        // 检查缓存
        CachedPoc cached = pocCache.get(filePath);
        if (cached != null && file.lastModified() == cached.lastModified) {
            // 缓存命中且文件未修改
            return cached.poc;
        }
        
        String fileName = file.getName().toLowerCase();
        PocObj.Poc poc = null;
        
        try {
            // 根据文件扩展名选择转换策略
            if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
                poc = convertYamlFile(filePath);
            } else if (fileName.endsWith(".json")) {
                poc = convertJsonFile(filePath);
            }
            
            if (poc == null) {
                throw new PocParseException(filePath, "unknown", "无法识别的POC格式或解析失败");
            }
            
            // 分类和标签规范化
            PocClassifier.classifyPoc(poc, filePath);
            
            // 存入缓存
            pocCache.put(filePath, new CachedPoc(poc, file.lastModified()));
            
            return poc;
            
        } catch (PocParseException e) {
            throw e;
        } catch (Exception e) {
            throw new PocParseException(filePath, "unknown", "POC解析异常", e);
        }
    }
    
    /**
     * 清除POC缓存
     */
    public synchronized void clearCache() {
        pocCache.clear();
        System.out.println("POC缓存已清除，共清除 " + pocCache.size() + " 个缓存项");
    }
    
    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        return String.format("POC缓存: %d/%d (%.1f%% 已使用)", 
            pocCache.size(), MAX_CACHE_SIZE, 
            (pocCache.size() * 100.0 / MAX_CACHE_SIZE));
    }
    
    /**
     * 转换YAML格式的POC文件
     * 优先根据目录名判断格式，否则尝试Nuclei和Xray两种格式
     */
    private PocObj.Poc convertYamlFile(String filePath) {
        // 标准化路径分隔符
        String normalizedPath = filePath.replace("\\", "/");

        // 根据目录名优先判断POC类型
        if (normalizedPath.contains("/xrayPoc/") || normalizedPath.contains("/xray/")) {
            // Xray POC 目录，优先尝试 Xray 格式
            try {
                XrayYamlObj.Poc xrayPoc = PocConverter.loadXrayYamlPocFile(filePath);
                PocObj.Poc poc = xrayConverter.convert(xrayPoc);
                if (poc != null) {
                    return poc;
                }
            } catch (Exception e) {
                // Xray 格式失败，回退到通用逻辑
            }
        } else if (normalizedPath.contains("/nucleiPoc/") || normalizedPath.contains("/nuclei/")) {
            // Nuclei POC 目录，优先尝试 Nuclei 格式
            try {
                NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(filePath);
                PocObj.Poc poc = nucleiConverter.convert(nucleiPoc);
                if (poc != null) {
                    return poc;
                }
            } catch (Exception e) {
                // Nuclei 格式失败，回退到通用逻辑
            }
        }

        // 通用逻辑：依次尝试所有YAML格式
        // 1. 先尝试Nuclei格式
        try {
            NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlPocFile(filePath);
            PocObj.Poc poc = nucleiConverter.convert(nucleiPoc);
            if (poc != null) {
                return poc;
            }
        } catch (Exception e) {
            // 忽略，继续尝试其他格式
        }

        // 2. 尝试Xray格式
        try {
            XrayYamlObj.Poc xrayPoc = PocConverter.loadXrayYamlPocFile(filePath);
            PocObj.Poc poc = xrayConverter.convert(xrayPoc);
            if (poc != null) {
                return poc;
            }
        } catch (Exception e) {
            // 忽略
        }

        return null;
    }
    
    /**
     * 转换JSON格式的POC文件
     * 优先根据目录名判断格式，否则尝试Goby和Pocsuite两种格式
     */
    private PocObj.Poc convertJsonFile(String filePath) {
        // 标准化路径分隔符
        String normalizedPath = filePath.replace("\\", "/");

        // 根据目录名优先判断POC类型
        if (normalizedPath.contains("/gobyPoc/") || normalizedPath.contains("/goby/")) {
            // Goby POC 目录，优先尝试 Goby 格式
            try {
                GobyJsonObj.PocJson gobyPoc = PocConverter.loadGobyJsonPocFile(filePath);
                PocObj.Poc poc = gobyConverter.convert(gobyPoc);
                if (poc != null) {
                    return poc;
                }
            } catch (Exception e) {
                // Goby 格式失败，回退到通用逻辑
            }
        } else if (normalizedPath.contains("/pocsuitePoc/") || normalizedPath.contains("/pocsuite/")) {
            // Pocsuite POC 目录，优先尝试 Pocsuite 格式
            try {
                PocsuiteJsonObj.PocJson pocsuitePoc = PocConverter.loadPocsuiteJsonPocFile(filePath);
                PocObj.Poc poc = pocsuiteConverter.convert(pocsuitePoc);
                if (poc != null) {
                    return poc;
                }
            } catch (Exception e) {
                // Pocsuite 格式失败，回退到通用逻辑
            }
        }

        // 通用逻辑：依次尝试所有JSON格式
        // 1. 先尝试Goby格式
        try {
            GobyJsonObj.PocJson gobyPoc = PocConverter.loadGobyJsonPocFile(filePath);
            PocObj.Poc poc = gobyConverter.convert(gobyPoc);
            if (poc != null) {
                return poc;
            }
        } catch (Exception e) {
            // 忽略，继续尝试其他格式
        }

        // 2. 尝试Pocsuite格式
        try {
            PocsuiteJsonObj.PocJson pocsuitePoc = PocConverter.loadPocsuiteJsonPocFile(filePath);
            PocObj.Poc poc = pocsuiteConverter.convert(pocsuitePoc);
            if (poc != null) {
                return poc;
            }
        } catch (Exception e) {
            // 忽略
        }

        return null;
    }
    
    /**
     * 检查是否支持指定格式
     */
    public boolean supportsFormat(String format) {
        return format != null && converterMap.containsKey(format.toLowerCase());
    }
    
    /**
     * 获取所有支持的格式
     */
    public String[] getSupportedFormats() {
        return converterMap.keySet().toArray(new String[0]);
    }
    
    /**
     * 从内容转换 POC
     * @param content POC 文件内容
     * @param fileName 文件名（用于判断格式）
     * @return 转换后的 POC 对象
     */
    public PocObj.Poc convertFromContent(String content, String fileName) {
        return convertFromContent(content, fileName, null);
    }

    /**
     * 从内容转换 POC（支持源路径用于分类）
     * @param content POC 文件内容
     * @param fileName 文件名（用于判断格式）
     * @param sourcePath 源文件完整路径（用于 PocClassifier 分类，如 /nucleiPoc/http/osint/example.yaml）
     * @return 转换后的 POC 对象
     */
    public PocObj.Poc convertFromContent(String content, String fileName, String sourcePath) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        String lowerFileName = fileName.toLowerCase();
        PocObj.Poc poc = null;

        try {
            if (lowerFileName.endsWith(".yaml") || lowerFileName.endsWith(".yml")) {
                poc = convertYamlContent(content, fileName);
            } else if (lowerFileName.endsWith(".json")) {
                poc = convertJsonContent(content, fileName);
            }

            if (poc != null) {
                // 优先使用 sourcePath 进行分类（包含目录结构信息）
                String classifyPath = (sourcePath != null && !sourcePath.isEmpty()) ? sourcePath : fileName;
                PocClassifier.classifyPoc(poc, classifyPath);
            }

            return poc;
        } catch (Exception e) {
            return null;
        }
    }
    
    private PocObj.Poc convertYamlContent(String content, String fileName) {
        String normalizedPath = fileName.replace("\\", "/");
        
        // 根据路径判断格式
        if (normalizedPath.contains("/xrayPoc/") || normalizedPath.contains("/xray/")) {
            try {
                XrayYamlObj.Poc xrayPoc = PocConverter.loadXrayYamlFromContent(content);
                if (xrayPoc != null) {
                    PocObj.Poc poc = xrayConverter.convert(xrayPoc);
                    if (poc != null) return poc;
                }
            } catch (Exception e) { }
        }
        
        // 默认尝试 Nuclei 格式
        try {
            NucleiYamlObj.Poc nucleiPoc = PocConverter.loadNucleiYamlFromContent(content);
            if (nucleiPoc != null) {
                PocObj.Poc poc = nucleiConverter.convert(nucleiPoc);
                if (poc != null) return poc;
            }
        } catch (Exception e) { }
        
        // 回退到 Xray 格式
        try {
            XrayYamlObj.Poc xrayPoc = PocConverter.loadXrayYamlFromContent(content);
            if (xrayPoc != null) {
                return xrayConverter.convert(xrayPoc);
            }
        } catch (Exception e) { }
        
        return null;
    }
    
    private PocObj.Poc convertJsonContent(String content, String fileName) {
        String normalizedPath = fileName.replace("\\", "/");
        
        // 根据路径判断格式
        if (normalizedPath.contains("/pocsuitePoc/") || normalizedPath.contains("/pocsuite/")) {
            try {
                PocsuiteJsonObj.PocJson pocsuitePoc = PocConverter.loadPocsuiteJsonFromContent(content);
                if (pocsuitePoc != null) {
                    PocObj.Poc poc = pocsuiteConverter.convert(pocsuitePoc);
                    if (poc != null) return poc;
                }
            } catch (Exception e) { }
        }
        
        // 默认尝试 Goby 格式
        try {
            GobyJsonObj.PocJson gobyPoc = PocConverter.loadGobyJsonFromContent(content);
            if (gobyPoc != null) {
                PocObj.Poc poc = gobyConverter.convert(gobyPoc);
                if (poc != null) return poc;
            }
        } catch (Exception e) { }
        
        // 回退到 Pocsuite 格式
        try {
            PocsuiteJsonObj.PocJson pocsuitePoc = PocConverter.loadPocsuiteJsonFromContent(content);
            if (pocsuitePoc != null) {
                return pocsuiteConverter.convert(pocsuitePoc);
            }
        } catch (Exception e) { }
        
        return null;
    }
}

