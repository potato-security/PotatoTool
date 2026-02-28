package com.potato.potatotool.content.redTeam.vulnScanner.report;

import com.opencsv.CSVWriter;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * CSV报告生成器
 * 使用OpenCSV生成CSV格式报告
 * 
 * @author Potato
 * @date 2025/11/03
 */
public class CsvReportGenerator {
    
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public File generate(List<ScanResult> results, String outputPath) throws IOException {
        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();
        
        try (CSVWriter writer = new CSVWriter(
                new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8),
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.DEFAULT_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END)) {
            
            // 写入BOM以支持Excel正确显示中文
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            fos.close();
            
            // 重新打开写入数据
            try (CSVWriter csvWriter = new CSVWriter(
                    new OutputStreamWriter(new FileOutputStream(outputFile, true), StandardCharsets.UTF_8))) {
                
                // 写入标题行
                csvWriter.writeNext(new String[]{
                    "# PotatoTool 漏洞扫描报告"
                });
                csvWriter.writeNext(new String[]{
                    "# 生成时间: " + sdf.format(new Date())
                });
                csvWriter.writeNext(new String[]{}); // 空行
                
                // 写入表头 - 添加新字段
                csvWriter.writeNext(new String[]{
                    "序号", "目标URL", "POC名称", "POC ID", "严重程度",
                    "CVE ID", "CWE ID", "CVSS", "漏洞类型", "POC格式", "协议",
                    "描述", "发现时间", "检测路径", "Payload", "参数键", "变量值",
                    "提取数据", "修复建议", "步骤数", "HTTP请求", "HTTP响应"
                });

                // 写入数据
                int index = 1;
                for (ScanResult result : results) {
                    PocObj.Poc poc = result.getPoc();
                    csvWriter.writeNext(new String[]{
                        String.valueOf(index++),
                        result.getTarget(),
                        poc.getName(),
                        poc.getId(),
                        poc.getSeverity().name(),
                        result.getCveId() != null ? result.getCveId() : "",
                        result.getCweId() != null ? result.getCweId() : "",
                        result.getCvssScore() != null ? result.getCvssScore() : "",
                        poc.getVulType() != null ? poc.getVulType() : "",
                        poc.getOriginalFormat(),
                        poc.getProtocol(),
                        poc.getDescription() != null ? poc.getDescription() : "",
                        sdf.format(new Date()),
                        result.getMatchedPath() != null ? result.getMatchedPath() : "",
                        result.getMatchedPayload() != null ? result.getMatchedPayload() : "",
                        result.getFormattedParamKeys(),
                        result.getFormattedVariableValues(),
                        result.getFormattedOutputData(),
                        result.getRecommendation() != null ? result.getRecommendation() : "",
                        String.valueOf(result.getStepRecords() != null ? result.getStepRecords().size() : 0),
                        result.getRawRequest() != null ? result.getRawRequest() : "",
                        result.getRawResponseSnippet() != null ? result.getRawResponseSnippet() : ""
                    });
                }
            }
        }
        
        return outputFile;
    }
}

