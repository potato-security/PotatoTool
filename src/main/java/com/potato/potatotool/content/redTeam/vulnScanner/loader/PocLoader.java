package com.potato.potatotool.content.redTeam.vulnScanner.loader;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.exception.PocLoadException;
import com.potato.potatotool.content.redTeam.vulnScanner.exception.PocParseException;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.PocConverterRegistry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * POC加载器
 * 负责从文件系统加载和解析POC文件
 * 
 * @author Potato
 * @date 2024-10-28
 */
public class PocLoader {
    
    private final PocConverterRegistry converterRegistry;
    // 默认不跳过无效POC，以便在开发/调试时能发现问题。生产环境可设置为true。
    private boolean skipInvalidPocs = false;
    private boolean verbose = true;
    
    /**
     * 加载统计信息
     */
    private LoadStatistics lastLoadStatistics;
    
    public PocLoader() {
        this.converterRegistry = PocConverterRegistry.getInstance();
    }
    
    /**
     * 获取最近一次加载的统计信息
     * @return 加载统计对象
     */
    public LoadStatistics getLastLoadStatistics() {
        return lastLoadStatistics;
    }
    
    /**
     * 设置是否跳过无效的POC
     */
    public void setSkipInvalidPocs(boolean skipInvalidPocs) {
        this.skipInvalidPocs = skipInvalidPocs;
    }
    
    /**
     * 设置是否输出详细日志
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    /**
     * 从目录加载所有POC
     * 
     * @param directory POC目录路径
     * @return POC对象列表
     * @throws PocLoadException 当目录不存在或无法访问时
     */
    public List<PocObj.Poc> loadFromDirectory(String directory) throws PocLoadException {
        File dir = new File(directory);
        
        if (!dir.exists()) {
            throw new PocLoadException("POC目录不存在: " + directory);
        }
        
        if (!dir.isDirectory()) {
            throw new PocLoadException("路径不是目录: " + directory);
        }
        
        // 初始化统计信息
        LoadStatistics stats = new LoadStatistics();
        List<PocObj.Poc> pocList = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(Paths.get(directory))) {
            paths.filter(Files::isRegularFile)
                 .filter(this::isSupportedFile)
                 .forEach(path -> {
                     stats.incrementTotal();
                     try {
                         PocObj.Poc poc = loadFromFile(path.toString());
                         if (poc != null) {
                             pocList.add(poc);
                             stats.incrementSuccess();
                             stats.incrementByFormat(poc.getOriginalFormat());
                         }
                     } catch (PocLoadException e) {
                         stats.incrementFailed();
                         stats.addFailedFile(path.toString(), e.getMessage());
                         if (skipInvalidPocs) {
                             if (verbose) {
                                 System.err.println("跳过无效POC: " + path + " - " + e.getMessage());
                             }
                         } else {
                             throw new RuntimeException(e);
                         }
                     }
                 });
        } catch (IOException e) {
            throw new PocLoadException("遍历目录失败: " + directory, e);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof PocLoadException) {
                throw (PocLoadException) e.getCause();
            }
            throw new PocLoadException("加载POC时发生异常", e);
        }
        
        // 保存统计信息
        this.lastLoadStatistics = stats;
        
        // 输出加载报告
        if (verbose || stats.getFailedCount() > 0) {
            System.out.println(stats.generateReport());
        }
        
        return pocList;
    }
    
    /**
     * 从单个文件加载POC
     * 
     * @param filePath POC文件路径
     * @return POC对象，如果解析失败返回null
     * @throws PocLoadException 当文件不存在或解析失败时
     */
    public PocObj.Poc loadFromFile(String filePath) throws PocLoadException {
        File file = new File(filePath);
        
        if (!file.exists()) {
            throw new PocLoadException(filePath, "文件不存在", null);
        }
        
        if (!file.isFile()) {
            throw new PocLoadException(filePath, "路径不是文件", null);
        }
        
        try {
            return converterRegistry.convertPocFile(filePath);
        } catch (PocParseException e) {
            throw e;
        } catch (Exception e) {
            throw new PocLoadException(filePath, "解析POC文件失败", e);
        }
    }
    
    /**
     * 从内容加载 POC
     * @param content POC 文件内容
     * @param fileName 文件名（用于判断格式）
     * @return POC 对象，如果解析失败返回 null
     */
    public PocObj.Poc loadFromContent(String content, String fileName) {
        return loadFromContent(content, fileName, null);
    }

    /**
     * 从内容加载 POC（支持源路径用于分类）
     * @param content POC 文件内容
     * @param fileName 文件名（用于判断格式）
     * @param sourcePath 源文件完整路径（用于 POC 分类，如 /nucleiPoc/http/osint/example.yaml）
     * @return POC 对象，如果解析失败返回 null
     */
    public PocObj.Poc loadFromContent(String content, String fileName, String sourcePath) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        try {
            return converterRegistry.convertFromContent(content, fileName, sourcePath);
        } catch (Exception e) {
            if (verbose) {
                System.err.println("从内容加载 POC 失败: " + fileName + " - " + e.getMessage());
            }
            return null;
        }
    }
    
    /**
     * 判断文件是否是支持的POC文件格式
     */
    private boolean isSupportedFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".yaml") ||
               fileName.endsWith(".yml") ||
               fileName.endsWith(".json");
    }

    /**
     * 判断路径是否应该被包含
     * 默认包含所有支持的POC文件
     */
    private boolean shouldIncludePath(Path path) {
        // 包含所有路径（临时过滤已移除）
        return true;
    }
    
    /**
     * 批量加载POC文件
     * 
     * @param filePaths POC文件路径列表
     * @return POC对象列表
     */
    public List<PocObj.Poc> loadFromFiles(List<String> filePaths) {
        LoadStatistics stats = new LoadStatistics();
        List<PocObj.Poc> pocList = new ArrayList<>();
        
        for (String filePath : filePaths) {
            stats.incrementTotal();
            try {
                PocObj.Poc poc = loadFromFile(filePath);
                if (poc != null) {
                    pocList.add(poc);
                    stats.incrementSuccess();
                    stats.incrementByFormat(poc.getOriginalFormat());
                }
            } catch (PocLoadException e) {
                stats.incrementFailed();
                stats.addFailedFile(filePath, e.getMessage());
                if (skipInvalidPocs) {
                    if (verbose) {
                        System.err.println("跳过无效POC: " + filePath + " - " + e.getMessage());
                    }
                } else {
                    throw new RuntimeException(e);
                }
            }
        }
        
        // 保存统计信息
        this.lastLoadStatistics = stats;
        
        // 输出加载报告
        if (verbose || stats.getFailedCount() > 0) {
            System.out.println(stats.generateReport());
        }
        
        return pocList;
    }
    
    /**
     * POC加载统计信息
     */
    public static class LoadStatistics {
        private final AtomicInteger totalCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failedCount = new AtomicInteger(0);
        private final Map<String, AtomicInteger> formatCounts = new HashMap<>();
        private final Map<String, String> failedFiles = new HashMap<>();
        
        public void incrementTotal() {
            totalCount.incrementAndGet();
        }
        
        public void incrementSuccess() {
            successCount.incrementAndGet();
        }
        
        public void incrementFailed() {
            failedCount.incrementAndGet();
        }
        
        public void incrementByFormat(String format) {
            if (format != null) {
                formatCounts.computeIfAbsent(format, k -> new AtomicInteger(0)).incrementAndGet();
            }
        }
        
        public void addFailedFile(String path, String reason) {
            // 限制失败文件记录数量，避免内存溢出
            if (failedFiles.size() < 100) {
                failedFiles.put(path, reason);
            }
        }
        
        public int getTotalCount() { return totalCount.get(); }
        public int getSuccessCount() { return successCount.get(); }
        public int getFailedCount() { return failedCount.get(); }
        public Map<String, AtomicInteger> getFormatCounts() { return formatCounts; }
        public Map<String, String> getFailedFiles() { return failedFiles; }
        
        /**
         * 生成加载统计报告
         */
        public String generateReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n========== POC加载统计报告 ==========\n");
            sb.append(String.format("总计: %d 个文件\n", totalCount.get()));
            sb.append(String.format("成功: %d 个 (%.1f%%)\n", successCount.get(), 
                totalCount.get() > 0 ? (successCount.get() * 100.0 / totalCount.get()) : 0));
            sb.append(String.format("失败: %d 个 (%.1f%%)\n", failedCount.get(),
                totalCount.get() > 0 ? (failedCount.get() * 100.0 / totalCount.get()) : 0));
            
            // 按格式统计
            if (!formatCounts.isEmpty()) {
                sb.append("\n按格式统计:\n");
                formatCounts.forEach((format, count) -> 
                    sb.append(String.format("  - %s: %d 个\n", format, count.get())));
            }
            
            // 失败文件列表（最多显示10个）
            if (!failedFiles.isEmpty()) {
                sb.append("\n失败文件示例（最多10个）:\n");
                failedFiles.entrySet().stream()
                    .limit(10)
                    .forEach(entry -> sb.append(String.format("  - %s: %s\n", 
                        shortenPath(entry.getKey()), entry.getValue())));
                
                if (failedFiles.size() > 10) {
                    sb.append(String.format("  ... 及其他 %d 个失败文件\n", failedFiles.size() - 10));
                }
            }
            
            sb.append("=========================================\n");
            return sb.toString();
        }
        
        /**
         * 缩短文件路径以便显示
         */
        private String shortenPath(String path) {
            if (path.length() <= 60) {
                return path;
            }
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash == -1) {
                lastSlash = path.lastIndexOf('\\');
            }
            if (lastSlash > 0) {
                String fileName = path.substring(lastSlash + 1);
                return "..." + path.substring(Math.max(0, lastSlash - 30), lastSlash + 1) + fileName;
            }
            return path.substring(0, 57) + "...";
        }
    }
}

