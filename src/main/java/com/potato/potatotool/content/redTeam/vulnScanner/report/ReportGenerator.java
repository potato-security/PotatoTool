package com.potato.potatotool.content.redTeam.vulnScanner.report;

import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 报告生成器统一入口
 * 支持多种格式：HTML, Word, Excel, CSV, JSON
 * 
 * @author Potato
 * @date 2025/11/03
 */
public class ReportGenerator {
    
    private final HtmlReportGenerator htmlGenerator;
    private final WordReportGenerator wordGenerator;
    private final ExcelReportGenerator excelGenerator;
    private final CsvReportGenerator csvGenerator;
    private final JsonReportGenerator jsonGenerator;
    
    public ReportGenerator() {
        this.htmlGenerator = new HtmlReportGenerator();
        this.wordGenerator = new WordReportGenerator();
        this.excelGenerator = new ExcelReportGenerator();
        this.csvGenerator = new CsvReportGenerator();
        this.jsonGenerator = new JsonReportGenerator();
    }
    
    /**
     * 生成报告（不带扫描耗时 - 兼容旧版本）
     * @param format 格式：HTML, Word, Excel, CSV, JSON
     * @param results 扫描结果列表
     * @param outputPath 输出路径
     * @return 生成的文件
     */
    public File generateReport(String format, List<ScanResult> results, String outputPath)
            throws Exception {
        return generateReport(format, results, outputPath, "未知");
    }

    /**
     * 生成报告（带扫描耗时）
     * @param format 格式：HTML, Word, Excel, CSV, JSON
     * @param results 扫描结果列表
     * @param outputPath 输出路径
     * @param scanDuration 扫描耗时（格式: "05分32秒" 或 "00:05:32"）
     * @return 生成的文件
     */
    public File generateReport(String format, List<ScanResult> results, String outputPath, String scanDuration)
            throws Exception {
        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("扫描结果不能为空");
        }

        switch (format.toUpperCase()) {
            case "HTML":
                return htmlGenerator.generate(results, outputPath, scanDuration);
            case "WORD":
            case "DOCX":
                return wordGenerator.generate(results, outputPath, scanDuration);
            case "EXCEL":
            case "XLSX":
                return excelGenerator.generate(results, outputPath, scanDuration);
            case "CSV":
                return csvGenerator.generate(results, outputPath);
            case "JSON":
                return jsonGenerator.generate(results, outputPath);
            default:
                throw new IllegalArgumentException("不支持的格式: " + format);
        }
    }
    
    /**
     * 生成报告（带扫描配置信息）
     */
    public File generateReport(String format, List<ScanResult> results,
                              String outputPath, Map<String, Object> scanConfig)
            throws Exception {
        // 扫描配置可在后续版本中添加到报告元数据中
        return generateReport(format, results, outputPath);
    }
    
    /**
     * 生成所有格式的报告
     */
    public List<File> generateAllFormats(List<ScanResult> results, String baseDir) 
            throws Exception {
        List<File> files = new ArrayList<>();
        files.add(generateReport("HTML", results, baseDir + "/report.html"));
        files.add(generateReport("Word", results, baseDir + "/report.docx"));
        files.add(generateReport("Excel", results, baseDir + "/report.xlsx"));
        files.add(generateReport("CSV", results, baseDir + "/report.csv"));
        files.add(generateReport("JSON", results, baseDir + "/report.json"));
        return files;
    }
}


