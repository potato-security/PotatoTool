package com.potato.potatotool.content.redTeam.vulnScanner.report;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.model.StepExecutionRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Excel 报告生成器 - 增强版
 * 使用 Dark Mode 设计系统的专业配色方案生成 XLSX 格式报告
 *
 * 功能特性：
 * - 冻结表头：漏洞详情Sheet表头固定
 * - 交替行颜色：提升数据可读性
 * - 专业行高：优化阅读体验
 * - 严重度配色：直观显示风险等级
 *
 * @author Potato
 * @date 2025/01/06
 */
public class ExcelReportGenerator {

    private static final int MAX_CELL_TEXT_LENGTH = 32767;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // 行高常量（单位：点）
    private static final short ROW_HEIGHT_HEADER = 400;    // 表头行高
    private static final short ROW_HEIGHT_DATA = 350;      // 数据行高
    private static final short ROW_HEIGHT_TITLE = 500;     // 标题行高

    // 交替行背景色
    private static final byte[] COLOR_ROW_ODD = new byte[]{(byte)255, (byte)255, (byte)255};   // #FFFFFF 白色
    private static final byte[] COLOR_ROW_EVEN = new byte[]{(byte)248, (byte)250, (byte)252};  // #F8FAFC slate-50
    
    public File generate(List<ScanResult> results, String outputPath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        
        // 创建各个Sheet
        createSummarySheet(workbook, results);
        createVulnDetailsSheet(workbook, results);
        createStatisticsSheet(workbook, results);
        
        // 写入文件
        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();
        
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            workbook.write(fos);
        }
        
        workbook.close();
        return outputFile;
    }
    
    /**
     * 生成扫描耗时参数
     */
    private String scanDuration = "未知";

    public File generate(List<ScanResult> results, String outputPath, String scanDuration) throws IOException {
        this.scanDuration = scanDuration;
        return generate(results, outputPath);
    }

    private void createSummarySheet(Workbook wb, List<ScanResult> results) {
        Sheet sheet = wb.createSheet("扫描摘要");

        CellStyle headerStyle = createHeaderStyle(wb);
        CellStyle dataStyle = createDataStyle(wb);
        CellStyle titleStyle = createTitleStyle(wb);

        // 标题
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("PotatoTool 漏洞扫描报告");
        titleCell.setCellStyle(titleStyle);

        // 生成时间和扫描耗时
        Row timeRow = sheet.createRow(1);
        Cell timeCell = timeRow.createCell(0);
        timeCell.setCellValue("生成时间: " + sdf.format(new Date()) + "  |  扫描耗时: " + scanDuration);

        // 空行
        sheet.createRow(2);

        // ===== 第一组：基础统计数据 =====
        int targetCount = (int) results.stream().map(ScanResult::getTarget).distinct().count();
        int pocCount = (int) results.stream().map(r -> r.getPoc().getId()).distinct().count();
        int vulnCount = results.size();

        // 统计高危漏洞数 (CRITICAL + HIGH)
        Map<PocObj.Severity, Long> severityCount = new HashMap<>();
        for (ScanResult result : results) {
            PocObj.Severity severity = result.getPoc().getSeverity();
            severityCount.put(severity, severityCount.getOrDefault(severity, 0L) + 1);
        }

        long criticalAndHighCount = severityCount.getOrDefault(PocObj.Severity.CRITICAL, 0L)
                                  + severityCount.getOrDefault(PocObj.Severity.HIGH, 0L);

        // 第一组标题
        int row = 3;
        Row section1TitleRow = sheet.createRow(row++);
        Cell section1TitleCell = section1TitleRow.createCell(0);
        section1TitleCell.setCellValue("📊 基础统计");
        section1TitleCell.setCellStyle(titleStyle);

        createSummaryRow(sheet, row++, "扫描目标数", String.valueOf(targetCount), headerStyle, dataStyle);
        createSummaryRow(sheet, row++, "使用POC数", String.valueOf(pocCount), headerStyle, dataStyle);
        createSummaryRow(sheet, row++, "发现漏洞总数", String.valueOf(vulnCount), headerStyle, dataStyle);
        createSummaryRow(sheet, row++, "高危漏洞数", String.valueOf(criticalAndHighCount), headerStyle, dataStyle);

        // 空行分隔
        sheet.createRow(row++);

        // ===== 第二组：严重度分布 =====
        Row section2TitleRow = sheet.createRow(row++);
        Cell section2TitleCell = section2TitleRow.createCell(0);
        section2TitleCell.setCellValue("🎯 严重度分布");
        section2TitleCell.setCellStyle(titleStyle);

        createSummaryRow(sheet, row++,
            "CRITICAL 漏洞",
            String.valueOf(severityCount.getOrDefault(PocObj.Severity.CRITICAL, 0L)),
            headerStyle, dataStyle);
        createSummaryRow(sheet, row++,
            "HIGH 漏洞",
            String.valueOf(severityCount.getOrDefault(PocObj.Severity.HIGH, 0L)),
            headerStyle, dataStyle);
        createSummaryRow(sheet, row++,
            "MEDIUM 漏洞",
            String.valueOf(severityCount.getOrDefault(PocObj.Severity.MEDIUM, 0L)),
            headerStyle, dataStyle);
        createSummaryRow(sheet, row++,
            "LOW 漏洞",
            String.valueOf(severityCount.getOrDefault(PocObj.Severity.LOW, 0L)),
            headerStyle, dataStyle);
        createSummaryRow(sheet, row++,
            "INFO 漏洞",
            String.valueOf(severityCount.getOrDefault(PocObj.Severity.INFO, 0L)),
            headerStyle, dataStyle);

        // 设置固定列宽
        sheet.setColumnWidth(0, 256 * 20);
        sheet.setColumnWidth(1, 256 * 30);
    }
    
    private void createVulnDetailsSheet(Workbook wb, List<ScanResult> results) {
        Sheet sheet = wb.createSheet("漏洞详情");

        CellStyle headerStyle = createHeaderStyle(wb);
        CellStyle codeStyle = createCodeStyle(wb);

        // 创建奇偶行样式
        XSSFCellStyle oddRowStyle = createDataStyleWithColor(wb, COLOR_ROW_ODD);
        XSSFCellStyle evenRowStyle = createDataStyleWithColor(wb, COLOR_ROW_EVEN);
        XSSFCellStyle oddCodeStyle = createCodeStyleWithColor(wb, COLOR_ROW_ODD);
        XSSFCellStyle evenCodeStyle = createCodeStyleWithColor(wb, COLOR_ROW_EVEN);

        // 表头（增强版，添加更多字段）
        Row headerRow = sheet.createRow(0);
        headerRow.setHeight(ROW_HEIGHT_HEADER);
        String[] headers = {
            "序号", "目标URL", "POC名称", "POC ID", "严重程度",
            "CVE ID", "CWE ID", "CVSS", "漏洞类型", "POC格式", "协议",
            "描述", "检测路径", "Payload", "参数键", "变量值",
            "提取数据", "修复建议", "步骤详情", "HTTP请求", "HTTP响应"
        };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 冻结表头（冻结第一行）
        sheet.createFreezePane(0, 1);

        // 数据行
        int rowNum = 1;
        for (ScanResult result : results) {
            PocObj.Poc poc = result.getPoc();
            Row row = sheet.createRow(rowNum);
            row.setHeight(ROW_HEIGHT_DATA);

            // 根据行号选择样式（交替行颜色）
            boolean isEvenRow = (rowNum % 2 == 0);
            CellStyle dataStyle = isEvenRow ? evenRowStyle : oddRowStyle;
            CellStyle currentCodeStyle = isEvenRow ? evenCodeStyle : oddCodeStyle;

            int col = 0;
            createDataCell(row, col++, String.valueOf(rowNum), dataStyle);
            createDataCell(row, col++, result.getTarget(), dataStyle);
            createDataCell(row, col++, poc.getName(), dataStyle);
            createDataCell(row, col++, poc.getId(), dataStyle);

            // 严重度单元格 - 使用设计系统配色（严重度样式优先于交替行颜色）
            Cell severityCell = row.createCell(col++);
            severityCell.setCellValue(poc.getSeverity().name());
            XSSFCellStyle severityStyle = (XSSFCellStyle) wb.createCellStyle();
            severityStyle.cloneStyleFrom(dataStyle);

            // 设置严重度文字颜色和背景色
            XSSFFont severityFont = (XSSFFont) wb.createFont();
            severityFont.setBold(true);
            severityFont.setColor(getSeverityTextColor(poc.getSeverity()));
            severityStyle.setFont(severityFont);
            severityStyle.setFillForegroundColor(getSeverityBackgroundColor(poc.getSeverity()));
            severityStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            severityCell.setCellStyle(severityStyle);

            // CVE ID
            createDataCell(row, col++, result.getCveId() != null ? result.getCveId() : "", dataStyle);

            // CWE ID
            createDataCell(row, col++, result.getCweId() != null ? result.getCweId() : "", dataStyle);

            // CVSS 评分
            createDataCell(row, col++, result.getCvssScore() != null ? result.getCvssScore() : "", dataStyle);

            createDataCell(row, col++, poc.getVulType() != null ? poc.getVulType() : "", dataStyle);
            createDataCell(row, col++, poc.getOriginalFormat(), dataStyle);
            createDataCell(row, col++, poc.getProtocol(), dataStyle);
            createDataCell(row, col++, poc.getDescription() != null ? poc.getDescription() : "", dataStyle);

            // 检测路径
            createDataCell(row, col++,
                result.getMatchedPath() != null ? result.getMatchedPath() : "", dataStyle);

            // Payload
            createDataCell(row, col++,
                result.getMatchedPayload() != null ? result.getMatchedPayload() : "", dataStyle);

            // 参数键名
            createDataCell(row, col++, result.getFormattedParamKeys(), dataStyle);

            // 变量值
            createDataCell(row, col++, result.getFormattedVariableValues(), dataStyle);

            // 提取的输出数据
            createDataCell(row, col++, result.getFormattedOutputData(), dataStyle);

            // 修复建议
            createDataCell(row, col++,
                result.getRecommendation() != null ? result.getRecommendation() : "", dataStyle);

            // 步骤详情（多步骤请求/响应摘要）
            String stepsInfo = formatStepRecords(result.getStepRecords());
            if (stepsInfo != null && !stepsInfo.isEmpty()) {
                if (stepsInfo.length() > 5000) {
                    stepsInfo = stepsInfo.substring(0, 5000) + "\n...(截断)";
                }
                createDataCell(row, col++, stepsInfo, currentCodeStyle);
            } else {
                createDataCell(row, col++, "", dataStyle);
            }

            // HTTP请求（限制长度以避免Excel单元格限制）
            String request = result.getRawRequest();
            if (request != null && !request.isEmpty()) {
                if (request.length() > 5000) {
                    request = request.substring(0, 5000) + "\n...(截断，总长度: " + request.length() + " 字符)";
                }
                createDataCell(row, col++, request, currentCodeStyle);
            } else {
                createDataCell(row, col++, "", dataStyle);
            }

            // HTTP响应（限制长度）
            String response = result.getRawResponseSnippet();
            if (response != null && !response.isEmpty()) {
                if (response.length() > 5000) {
                    response = response.substring(0, 5000) + "\n...(截断，总长度: " + response.length() + " 字符)";
                }
                createDataCell(row, col++, response, currentCodeStyle);
            } else {
                createDataCell(row, col++, "", dataStyle);
            }

            rowNum++;
        }

        // 设置列宽（手动计算，避免 autoSizeColumn 触发 AWT 导致 NPE）
        for (int i = 0; i < headers.length; i++) {
            // 特殊列使用固定宽度
            if (i == 11) { // 描述列
                sheet.setColumnWidth(i, 256 * 40);
            } else if (i == 12 || i == 13) { // 检测路径和Payload列
                sheet.setColumnWidth(i, 256 * 30);
            } else if (i == 17) { // 修复建议列
                sheet.setColumnWidth(i, 256 * 40);
            } else if (i >= 18) { // 步骤详情、HTTP请求和响应列
                sheet.setColumnWidth(i, 256 * 50);
            } else {
                // 其他列：手动计算列宽
                int maxWidth = headers[i].length() * 512; // 表头宽度（中文按双宽）
                for (Row r : sheet) {
                    Cell cell = r.getCell(i);
                    if (cell != null && cell.getCellType() == CellType.STRING) {
                        String val = cell.getStringCellValue();
                        if (val != null) {
                            // 取第一行内容计算宽度
                            String firstLine = val.contains("\n") ? val.substring(0, val.indexOf('\n')) : val;
                            int w = 0;
                            for (char c : firstLine.toCharArray()) {
                                w += c > 127 ? 512 : 256;
                            }
                            maxWidth = Math.max(maxWidth, w);
                        }
                    }
                }
                maxWidth = Math.max(maxWidth + 512, 256 * 15); // 最小15字符宽 + 边距
                maxWidth = Math.min(maxWidth, 256 * 60); // 最大60字符宽
                sheet.setColumnWidth(i, maxWidth);
            }
        }
    }

    /**
     * 格式化步骤执行记录为可读文本
     *
     * @param stepRecords 步骤执行记录列表
     * @return 格式化后的文本
     */
    private String formatStepRecords(List<StepExecutionRecord> stepRecords) {
        if (stepRecords == null || stepRecords.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stepRecords.size(); i++) {
            StepExecutionRecord record = stepRecords.get(i);
            sb.append("=== 步骤 ").append(i + 1).append(" ===\n");
            sb.append("URL: ").append(record.getRequestUrl()).append("\n");
            sb.append("方法: ").append(record.getRequestMethod()).append("\n");
            sb.append("状态码: ").append(record.getResponseCode()).append("\n");
            sb.append("响应时间: ").append(record.getResponseTime()).append("ms\n");
            sb.append("匹配: ").append(record.isMatched() ? "是" : "否").append("\n");

            // 提取的变量
            if (record.getExtractedVariables() != null && !record.getExtractedVariables().isEmpty()) {
                sb.append("提取变量: ");
                for (Map.Entry<String, Object> entry : record.getExtractedVariables().entrySet()) {
                    sb.append(entry.getKey()).append("=").append(entry.getValue()).append("; ");
                }
                sb.append("\n");
            }

            // 请求体摘要
            if (record.getRequestBody() != null && !record.getRequestBody().isEmpty()) {
                String body = record.getRequestBody();
                if (body.length() > 200) {
                    body = body.substring(0, 200) + "...";
                }
                sb.append("请求体: ").append(body).append("\n");
            }

            // 响应体摘要
            if (record.getResponseBody() != null && !record.getResponseBody().isEmpty()) {
                String body = record.getResponseBody();
                if (body.length() > 200) {
                    body = body.substring(0, 200) + "...";
                }
                sb.append("响应体: ").append(body).append("\n");
            }

            sb.append("\n");
        }

        return sb.toString().trim();
    }
    
    private void createStatisticsSheet(Workbook wb, List<ScanResult> results) {
        Sheet sheet = wb.createSheet("统计分析");

        CellStyle headerStyle = createHeaderStyle(wb);
        XSSFCellStyle oddRowStyle = createDataStyleWithColor(wb, COLOR_ROW_ODD);
        XSSFCellStyle evenRowStyle = createDataStyleWithColor(wb, COLOR_ROW_EVEN);

        // 标题行
        Row titleRow = sheet.createRow(0);
        titleRow.setHeight(ROW_HEIGHT_TITLE);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("按目标统计漏洞数");
        titleCell.setCellStyle(createTitleStyle(wb));

        Map<String, Long> targetStats = new HashMap<>();
        for (ScanResult result : results) {
            targetStats.put(result.getTarget(),
                targetStats.getOrDefault(result.getTarget(), 0L) + 1);
        }

        Row headerRow = sheet.createRow(1);
        headerRow.setHeight(ROW_HEIGHT_HEADER);
        Cell h1 = headerRow.createCell(0);
        h1.setCellValue("目标");
        h1.setCellStyle(headerStyle);
        Cell h2 = headerRow.createCell(1);
        h2.setCellValue("漏洞数");
        h2.setCellStyle(headerStyle);

        // 冻结表头
        sheet.createFreezePane(0, 2);

        int rowNum = 2;
        int dataRowIndex = 0;
        for (Map.Entry<String, Long> entry : targetStats.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.setHeight(ROW_HEIGHT_DATA);
            CellStyle dataStyle = (dataRowIndex % 2 == 0) ? oddRowStyle : evenRowStyle;
            createDataCell(row, 0, entry.getKey(), dataStyle);
            createDataCell(row, 1, String.valueOf(entry.getValue()), dataStyle);
            dataRowIndex++;
        }

        // 按POC格式统计
        sheet.createRow(rowNum++); // 空行
        Row formatTitleRow = sheet.createRow(rowNum++);
        formatTitleRow.setHeight(ROW_HEIGHT_TITLE);
        Cell formatTitleCell = formatTitleRow.createCell(0);
        formatTitleCell.setCellValue("按POC格式统计漏洞数");
        formatTitleCell.setCellStyle(createTitleStyle(wb));

        Map<String, Long> formatStats = new HashMap<>();
        for (ScanResult result : results) {
            String format = result.getPoc().getOriginalFormat();
            formatStats.put(format, formatStats.getOrDefault(format, 0L) + 1);
        }

        Row formatHeaderRow = sheet.createRow(rowNum++);
        formatHeaderRow.setHeight(ROW_HEIGHT_HEADER);
        Cell fh1 = formatHeaderRow.createCell(0);
        fh1.setCellValue("POC格式");
        fh1.setCellStyle(headerStyle);
        Cell fh2 = formatHeaderRow.createCell(1);
        fh2.setCellValue("漏洞数");
        fh2.setCellStyle(headerStyle);

        dataRowIndex = 0;
        for (Map.Entry<String, Long> entry : formatStats.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.setHeight(ROW_HEIGHT_DATA);
            CellStyle dataStyle = (dataRowIndex % 2 == 0) ? oddRowStyle : evenRowStyle;
            createDataCell(row, 0, entry.getKey(), dataStyle);
            createDataCell(row, 1, String.valueOf(entry.getValue()), dataStyle);
            dataRowIndex++;
        }

        // 设置列宽
        sheet.setColumnWidth(0, 256 * 40);
        sheet.setColumnWidth(1, 256 * 15);
    }
    
    private void createSummaryRow(Sheet sheet, int rowNum, String key, String value, 
                                 CellStyle keyStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowNum);
        Cell keyCell = row.createCell(0);
        keyCell.setCellValue(key);
        keyCell.setCellStyle(keyStyle);
        
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(valueStyle);
    }
    
    private void createDataCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(limitCellText(value));
        cell.setCellStyle(style);
    }

    private String limitCellText(String value) {
        if (value == null || value.length() <= MAX_CELL_TEXT_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_CELL_TEXT_LENGTH - 4) + "...";
    }
    
    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        return style;
    }
    
    /**
     * 创建表头样式
     * 表头背景: #3B82F6 (blue-500)
     * 字体: Microsoft YaHei, 12pt, 加粗, 白色文字
     */
    private CellStyle createHeaderStyle(Workbook wb) {
        XSSFCellStyle style = (XSSFCellStyle) wb.createCellStyle();

        // 字体设置
        Font font = wb.createFont();
        font.setFontName("Microsoft YaHei");
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        // 背景色 #3B82F6 (blue-500)
        XSSFColor headerBgColor = new XSSFColor(new byte[]{(byte)59, (byte)130, (byte)246}, null);
        style.setFillForegroundColor(headerBgColor);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 对齐方式
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // 边框
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }
    
    private CellStyle createDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 创建代码样式
     * 字体: Consolas, 9pt
     * 用于显示 HTTP 请求/响应等代码内容
     */
    private CellStyle createCodeStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        Font font = wb.createFont();
        font.setFontName("Consolas");
        font.setFontHeightInPoints((short) 9);
        style.setFont(font);

        return style;
    }

    /**
     * 创建带背景色的数据样式（用于交替行）
     *
     * @param wb Workbook实例
     * @param bgColor 背景色RGB数组
     * @return 带背景色的单元格样式
     */
    private XSSFCellStyle createDataStyleWithColor(Workbook wb, byte[] bgColor) {
        XSSFCellStyle style = (XSSFCellStyle) wb.createCellStyle();
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // 设置背景色
        XSSFColor color = new XSSFColor(bgColor, null);
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 设置边框颜色为浅灰色
        XSSFColor borderColor = new XSSFColor(new byte[]{(byte)229, (byte)231, (byte)235}, null); // #E5E7EB gray-200
        style.setBottomBorderColor(borderColor);
        style.setTopBorderColor(borderColor);
        style.setLeftBorderColor(borderColor);
        style.setRightBorderColor(borderColor);

        return style;
    }

    /**
     * 创建带背景色的代码样式（用于交替行中的代码单元格）
     *
     * @param wb Workbook实例
     * @param bgColor 背景色RGB数组
     * @return 带背景色的代码单元格样式
     */
    private XSSFCellStyle createCodeStyleWithColor(Workbook wb, byte[] bgColor) {
        XSSFCellStyle style = (XSSFCellStyle) wb.createCellStyle();
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // 设置背景色
        XSSFColor color = new XSSFColor(bgColor, null);
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 设置边框颜色为浅灰色
        XSSFColor borderColor = new XSSFColor(new byte[]{(byte)229, (byte)231, (byte)235}, null); // #E5E7EB gray-200
        style.setBottomBorderColor(borderColor);
        style.setTopBorderColor(borderColor);
        style.setLeftBorderColor(borderColor);
        style.setRightBorderColor(borderColor);

        // 设置等宽字体
        Font font = wb.createFont();
        font.setFontName("Consolas");
        font.setFontHeightInPoints((short) 9);
        style.setFont(font);

        return style;
    }
    
    /**
     * 获取严重度文字颜色（使用专业安全色板）
     *
     * @param severity 严重度级别
     * @return XSSFColor 文字颜色
     */
    private XSSFColor getSeverityTextColor(PocObj.Severity severity) {
        switch (severity) {
            case CRITICAL:
                return new XSSFColor(new byte[]{(byte)220, (byte)38, (byte)38}, null);    // #DC2626 red-600
            case HIGH:
                return new XSSFColor(new byte[]{(byte)234, (byte)88, (byte)12}, null);    // #EA580C orange-600
            case MEDIUM:
                return new XSSFColor(new byte[]{(byte)245, (byte)158, (byte)11}, null);   // #F59E0B amber-500
            case LOW:
                return new XSSFColor(new byte[]{(byte)59, (byte)130, (byte)246}, null);   // #3B82F6 blue-500
            case INFO:
                return new XSSFColor(new byte[]{(byte)107, (byte)114, (byte)128}, null);  // #6B7280 gray-500
            default:
                return new XSSFColor(new byte[]{(byte)100, (byte)116, (byte)139}, null);  // #64748B slate-500
        }
    }

    /**
     * 获取严重度背景颜色（浅色背景）
     *
     * @param severity 严重度级别
     * @return XSSFColor 背景颜色
     */
    private XSSFColor getSeverityBackgroundColor(PocObj.Severity severity) {
        switch (severity) {
            case CRITICAL:
                return new XSSFColor(new byte[]{(byte)254, (byte)242, (byte)242}, null);  // #FEF2F2 red-50
            case HIGH:
                return new XSSFColor(new byte[]{(byte)255, (byte)247, (byte)237}, null);  // #FFF7ED orange-50
            case MEDIUM:
                return new XSSFColor(new byte[]{(byte)255, (byte)251, (byte)235}, null);  // #FFFBEB amber-50
            case LOW:
                return new XSSFColor(new byte[]{(byte)239, (byte)246, (byte)255}, null);  // #EFF6FF blue-50
            case INFO:
                return new XSSFColor(new byte[]{(byte)249, (byte)250, (byte)251}, null);  // #F9FAFB gray-50
            default:
                return new XSSFColor(new byte[]{(byte)248, (byte)250, (byte)252}, null);  // #F8FAFC slate-50
        }
    }
}

