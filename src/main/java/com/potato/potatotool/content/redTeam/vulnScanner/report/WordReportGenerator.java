package com.potato.potatotool.content.redTeam.vulnScanner.report;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.model.StepExecutionRecord;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.util.Units;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Word 报告生成器 - 增强版
 * 使用专业配色方案和优化的表格样式生成 DOCX 格式报告
 *
 * @author Potato
 * @date 2025/01/06
 */
public class WordReportGenerator {

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // 设计系统颜色常量
    private static final String COLOR_PRIMARY = "3B82F6";      // blue-500
    private static final String COLOR_CRITICAL = "DC2626";     // red-600
    private static final String COLOR_HIGH = "EA580C";         // orange-600
    private static final String COLOR_MEDIUM = "F59E0B";       // amber-500
    private static final String COLOR_LOW = "3B82F6";          // blue-500
    private static final String COLOR_INFO = "6B7280";         // gray-500
    private static final String COLOR_HEADER_BG = "F1F5F9";    // slate-100
    private static final String COLOR_TEXT_DARK = "1E293B";    // slate-800
    private static final String COLOR_TEXT_MUTED = "64748B";   // slate-500

    // 内置48x48 logo PNG (base64)
    private static final String LOGO_PNG_BASE64 = "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";

    /**
     * 生成 Word 报告
     */
    public File generate(List<ScanResult> results, String outputPath, String scanDuration) throws IOException {
        XWPFDocument document = new XWPFDocument();

        createHeaderFooter(document);

        // 生成报告标题
        createTitle(document, scanDuration);

        // 生成扫描摘要
        createSummary(document, results);

        // 生成漏洞详情
        createVulnDetails(document, results);

        // 生成页脚
        createFooter(document);

        // 写入文件
        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();

        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            document.write(out);
        }

        document.close();
        return outputFile;
    }

    /**
     * 兼容方法
     */
    public File generate(List<ScanResult> results, String outputPath) throws IOException {
        return generate(results, outputPath, "未知");
    }

    /**
     * 创建报告标题
     */
    private void createTitle(XWPFDocument document, String scanDuration) {
        // 主标题
        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        title.setSpacingAfter(200);

        XWPFRun titleRun = title.createRun();
        titleRun.setText("PotatoTool 漏洞扫描报告");
        titleRun.setFontFamily("Microsoft YaHei");
        titleRun.setBold(true);
        titleRun.setFontSize(28);
        titleRun.setColor(COLOR_PRIMARY);

        // 分隔线
        XWPFParagraph separator = document.createParagraph();
        separator.setBorderBottom(Borders.SINGLE);
        separator.setSpacingAfter(100);

        // 元数据行
        XWPFParagraph meta = document.createParagraph();
        meta.setAlignment(ParagraphAlignment.CENTER);
        meta.setSpacingAfter(400);

        XWPFRun metaRun = meta.createRun();
        metaRun.setFontFamily("Microsoft YaHei");
        metaRun.setText("生成时间: " + sdf.format(new Date()) + "    |    扫描耗时: " + scanDuration + "    |    版本: v2.5.1");
        metaRun.setFontSize(11);
        metaRun.setColor(COLOR_TEXT_MUTED);
    }

    /**
     * 创建扫描摘要
     */
    private void createSummary(XWPFDocument document, List<ScanResult> results) {
        // 统计数据
        int targetCount = (int) results.stream()
            .map(ScanResult::getTarget)
            .distinct()
            .count();
        int pocCount = (int) results.stream()
            .map(r -> r.getPoc().getId())
            .distinct()
            .count();
        int vulnCount = results.size();

        Map<PocObj.Severity, Long> severityCount = new HashMap<>();
        for (ScanResult result : results) {
            PocObj.Severity severity = result.getPoc().getSeverity();
            severityCount.put(severity, severityCount.getOrDefault(severity, 0L) + 1);
        }

        long criticalAndHighCount = severityCount.getOrDefault(PocObj.Severity.CRITICAL, 0L)
                                  + severityCount.getOrDefault(PocObj.Severity.HIGH, 0L);

        // 第一组：基础统计
        createSectionTitle(document, "基础统计");

        XWPFTable summaryTable = document.createTable(4, 2);
        setTableStyle(summaryTable, 4500, 3500);

        setStyledTableRow(summaryTable, 0, "扫描目标数", String.valueOf(targetCount), null);
        setStyledTableRow(summaryTable, 1, "使用POC数", String.valueOf(pocCount), null);
        setStyledTableRow(summaryTable, 2, "发现漏洞总数", String.valueOf(vulnCount), COLOR_HIGH);
        setStyledTableRow(summaryTable, 3, "高危漏洞数 (Critical + High)", String.valueOf(criticalAndHighCount), COLOR_CRITICAL);

        document.createParagraph().setSpacingAfter(200);

        // 第二组：严重度分布
        createSectionTitle(document, "严重度分布");

        XWPFTable severityTable = document.createTable(5, 3);
        setTableStyle(severityTable, 3000, 2500, 2500);

        // 表头
        setTableHeader(severityTable, 0, new String[]{"严重级别", "数量", "占比"});

        long total = vulnCount > 0 ? vulnCount : 1;
        setStyledSeverityRow(severityTable, 1, "CRITICAL",
            severityCount.getOrDefault(PocObj.Severity.CRITICAL, 0L), total, COLOR_CRITICAL);
        setStyledSeverityRow(severityTable, 2, "HIGH",
            severityCount.getOrDefault(PocObj.Severity.HIGH, 0L), total, COLOR_HIGH);
        setStyledSeverityRow(severityTable, 3, "MEDIUM",
            severityCount.getOrDefault(PocObj.Severity.MEDIUM, 0L), total, COLOR_MEDIUM);
        setStyledSeverityRow(severityTable, 4, "LOW / INFO",
            severityCount.getOrDefault(PocObj.Severity.LOW, 0L) + severityCount.getOrDefault(PocObj.Severity.INFO, 0L),
            total, COLOR_INFO);

        document.createParagraph().setSpacingAfter(300);
    }

    /**
     * 创建漏洞详情
     */
    private void createVulnDetails(XWPFDocument document, List<ScanResult> results) {
        createSectionTitle(document, "漏洞详情");

        for (int i = 0; i < results.size(); i++) {
            ScanResult result = results.get(i);
            PocObj.Poc poc = result.getPoc();

            // 漏洞标题
            XWPFParagraph vulnTitle = document.createParagraph();
            vulnTitle.setSpacingBefore(300);
            vulnTitle.setSpacingAfter(100);

            XWPFRun indexRun = vulnTitle.createRun();
            indexRun.setText((i + 1) + ". ");
            indexRun.setFontFamily("Microsoft YaHei");
            indexRun.setBold(true);
            indexRun.setFontSize(13);
            indexRun.setColor(COLOR_TEXT_DARK);

            XWPFRun nameRun = vulnTitle.createRun();
            nameRun.setText(poc.getName());
            nameRun.setFontFamily("Microsoft YaHei");
            nameRun.setBold(true);
            nameRun.setFontSize(13);
            nameRun.setColor(COLOR_TEXT_DARK);

            XWPFRun severityRun = vulnTitle.createRun();
            severityRun.setText("  [" + poc.getSeverity().name() + "]");
            severityRun.setFontFamily("Microsoft YaHei");
            severityRun.setBold(true);
            severityRun.setFontSize(12);
            severityRun.setColor(getSeverityColor(poc.getSeverity()));

            // 计算表格行数
            int tableRows = 5;
            if (poc.getDescription() != null && !poc.getDescription().isEmpty()) tableRows++;
            if (result.getCveId() != null && !result.getCveId().isEmpty()) tableRows++;
            if (result.getCweId() != null && !result.getCweId().isEmpty()) tableRows++;
            if (result.getCvssScore() != null && !result.getCvssScore().isEmpty()) tableRows++;
            if (result.getMatchedPath() != null && !result.getMatchedPath().isEmpty()) tableRows++;
            if (result.getMatchedPayload() != null && !result.getMatchedPayload().isEmpty()) tableRows++;
            String paramKeys = result.getFormattedParamKeys();
            if (paramKeys != null && !paramKeys.isEmpty()) tableRows++;
            String varValues = result.getFormattedVariableValues();
            if (varValues != null && !varValues.isEmpty()) tableRows++;
            String outputData = result.getFormattedOutputData();
            if (outputData != null && !outputData.isEmpty()) tableRows++;
            if (result.getRecommendation() != null && !result.getRecommendation().isEmpty()) tableRows++;

            XWPFTable detailTable = document.createTable(tableRows, 2);
            setTableStyle(detailTable, 2500, 6000);

            int tableRow = 0;
            setStyledTableRow(detailTable, tableRow++, "目标URL", result.getTarget(), null);
            setStyledTableRow(detailTable, tableRow++, "POC ID", poc.getId(), null);
            setStyledTableRow(detailTable, tableRow++, "POC格式", poc.getOriginalFormat(), null);
            setStyledTableRow(detailTable, tableRow++, "漏洞类型", poc.getVulType() != null ? poc.getVulType() : "-", null);
            setStyledTableRow(detailTable, tableRow++, "协议", poc.getProtocol(), null);

            if (poc.getDescription() != null && !poc.getDescription().isEmpty()) {
                setStyledTableRow(detailTable, tableRow++, "描述", poc.getDescription(), null);
            }

            // CVE/CWE/CVSS 信息
            if (result.getCveId() != null && !result.getCveId().isEmpty()) {
                setStyledTableRow(detailTable, tableRow++, "CVE ID", result.getCveId(), COLOR_CRITICAL);
            }
            if (result.getCweId() != null && !result.getCweId().isEmpty()) {
                setStyledTableRow(detailTable, tableRow++, "CWE ID", result.getCweId(), COLOR_HIGH);
            }
            if (result.getCvssScore() != null && !result.getCvssScore().isEmpty()) {
                setStyledTableRow(detailTable, tableRow++, "CVSS评分", result.getCvssScore(), null);
            }

            if (result.getMatchedPath() != null && !result.getMatchedPath().isEmpty()) {
                setStyledTableRow(detailTable, tableRow++, "检测路径", result.getMatchedPath(), null);
            }
            if (result.getMatchedPayload() != null && !result.getMatchedPayload().isEmpty()) {
                setStyledTableRow(detailTable, tableRow++, "Payload", result.getMatchedPayload(), null);
            }

            // 参数键名
            if (paramKeys != null && !paramKeys.isEmpty()) {
                setStyledTableRow(detailTable, tableRow++, "参数键名", paramKeys, null);
            }

            // 变量值
            if (varValues != null && !varValues.isEmpty()) {
                setStyledTableRow(detailTable, tableRow++, "变量值", varValues, null);
            }

            // 提取的数据
            if (outputData != null && !outputData.isEmpty()) {
                setStyledTableRow(detailTable, tableRow++, "提取数据", outputData, null);
            }

            // 修复建议
            if (result.getRecommendation() != null && !result.getRecommendation().isEmpty()) {
                setStyledTableRow(detailTable, tableRow++, "修复建议", result.getRecommendation(), COLOR_PRIMARY);
            }

            // 步骤执行详情
            List<StepExecutionRecord> stepRecords = result.getStepRecords();
            if (stepRecords != null && !stepRecords.isEmpty()) {
                createStepRecordsSection(document, stepRecords);
            }

            // HTTP 请求
            if (result.getRawRequest() != null && !result.getRawRequest().isEmpty()) {
                createCodeBlock(document, "HTTP请求", result.getRawRequest());
            }

            // HTTP 响应
            if (result.getRawResponseSnippet() != null && !result.getRawResponseSnippet().isEmpty()) {
                createCodeBlock(document, "HTTP响应片段", result.getRawResponseSnippet());
            }

            // 分隔线
            XWPFParagraph divider = document.createParagraph();
            divider.setBorderBottom(Borders.DASH_SMALL_GAP);
            divider.setSpacingAfter(100);
        }
    }

    /**
     * 创建步骤执行记录区域
     */
    private void createStepRecordsSection(XWPFDocument document, List<StepExecutionRecord> stepRecords) {
        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setSpacingBefore(150);
        titlePara.setSpacingAfter(50);

        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText("步骤执行详情 (共 " + stepRecords.size() + " 步):");
        titleRun.setFontFamily("Microsoft YaHei");
        titleRun.setBold(true);
        titleRun.setFontSize(11);
        titleRun.setColor(COLOR_PRIMARY);

        for (int i = 0; i < stepRecords.size(); i++) {
            StepExecutionRecord record = stepRecords.get(i);

            XWPFParagraph stepPara = document.createParagraph();
            stepPara.setSpacingBefore(80);

            // 步骤标题
            XWPFRun stepRun = stepPara.createRun();
            stepRun.setText("步骤 " + (i + 1));
            if (record.isMatched()) {
                stepRun.setText(" ✓");
            }
            stepRun.setFontFamily("Microsoft YaHei");
            stepRun.setBold(true);
            stepRun.setFontSize(10);
            stepRun.setColor(record.isMatched() ? "22C55E" : COLOR_TEXT_MUTED);

            // 步骤详情
            XWPFParagraph detailPara = document.createParagraph();
            detailPara.setSpacingAfter(50);

            StringBuilder details = new StringBuilder();
            details.append("URL: ").append(record.getRequestUrl()).append("\n");
            details.append("方法: ").append(record.getRequestMethod()).append(" | ");
            details.append("状态码: ").append(record.getResponseCode()).append(" | ");
            details.append("响应时间: ").append(record.getResponseTime()).append("ms");

            // 提取的变量
            if (record.getExtractedVariables() != null && !record.getExtractedVariables().isEmpty()) {
                details.append("\n提取变量: ");
                for (Map.Entry<String, Object> entry : record.getExtractedVariables().entrySet()) {
                    details.append(entry.getKey()).append("=").append(entry.getValue()).append("; ");
                }
            }

            XWPFRun detailRun = detailPara.createRun();
            detailRun.setText(details.toString());
            detailRun.setFontFamily("Consolas");
            detailRun.setFontSize(9);
            detailRun.setColor(COLOR_TEXT_DARK);

            // 请求头
            Map<String, String> reqHeaders = record.getRequestHeaders();
            if (reqHeaders != null && !reqHeaders.isEmpty()) {
                XWPFParagraph reqHdrTitle = document.createParagraph();
                XWPFRun reqHdrTitleRun = reqHdrTitle.createRun();
                reqHdrTitleRun.setText("请求头:");
                reqHdrTitleRun.setFontFamily("Microsoft YaHei");
                reqHdrTitleRun.setFontSize(9);
                reqHdrTitleRun.setBold(true);
                reqHdrTitleRun.setColor(COLOR_TEXT_MUTED);

                XWPFParagraph reqHdrPara = document.createParagraph();
                StringBuilder hdrSb = new StringBuilder();
                for (Map.Entry<String, String> h : reqHeaders.entrySet()) {
                    hdrSb.append(h.getKey()).append(": ").append(h.getValue()).append("\n");
                }
                XWPFRun reqHdrRun = reqHdrPara.createRun();
                reqHdrRun.setText(hdrSb.toString());
                reqHdrRun.setFontFamily("Consolas");
                reqHdrRun.setFontSize(8);
                reqHdrRun.setColor(COLOR_TEXT_DARK);
            }

            // 响应头
            Map<String, String> respHeaders = record.getResponseHeaders();
            if (respHeaders != null && !respHeaders.isEmpty()) {
                XWPFParagraph respHdrTitle = document.createParagraph();
                XWPFRun respHdrTitleRun = respHdrTitle.createRun();
                respHdrTitleRun.setText("响应头:");
                respHdrTitleRun.setFontFamily("Microsoft YaHei");
                respHdrTitleRun.setFontSize(9);
                respHdrTitleRun.setBold(true);
                respHdrTitleRun.setColor(COLOR_TEXT_MUTED);

                XWPFParagraph respHdrPara = document.createParagraph();
                StringBuilder hdrSb = new StringBuilder();
                for (Map.Entry<String, String> h : respHeaders.entrySet()) {
                    hdrSb.append(h.getKey()).append(": ").append(h.getValue()).append("\n");
                }
                XWPFRun respHdrRun = respHdrPara.createRun();
                respHdrRun.setText(hdrSb.toString());
                respHdrRun.setFontFamily("Consolas");
                respHdrRun.setFontSize(8);
                respHdrRun.setColor(COLOR_TEXT_DARK);
            }
        }
    }

    /**
     * 创建章节标题
     */
    private void createSectionTitle(XWPFDocument document, String title) {
        XWPFParagraph para = document.createParagraph();
        para.setSpacingBefore(400);
        para.setSpacingAfter(200);
        para.setBorderLeft(Borders.THICK);

        XWPFRun run = para.createRun();
        run.setText("  " + title);
        run.setFontFamily("Microsoft YaHei");
        run.setBold(true);
        run.setFontSize(16);
        run.setColor(COLOR_PRIMARY);
    }

    /**
     * 创建代码块
     */
    private void createCodeBlock(XWPFDocument document, String title, String content) {
        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setSpacingBefore(150);
        titlePara.setSpacingAfter(50);

        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText(title + ":");
        titleRun.setFontFamily("Microsoft YaHei");
        titleRun.setBold(true);
        titleRun.setFontSize(11);
        titleRun.setColor(COLOR_PRIMARY);

        XWPFParagraph codePara = document.createParagraph();
        codePara.setSpacingAfter(100);

        // 限制内容长度
        String displayContent = content;
        if (content.length() > 2000) {
            displayContent = content.substring(0, 2000) + "\n...(内容截断，共 " + content.length() + " 字符)";
        }

        XWPFRun codeRun = codePara.createRun();
        codeRun.setText(displayContent);
        codeRun.setFontFamily("Consolas");
        codeRun.setFontSize(9);
        codeRun.setColor(COLOR_TEXT_DARK);
    }

    /**
     * 创建Word页眉和页脚
     */
    private void createHeaderFooter(XWPFDocument document) {
        try {
            CTBody body = document.getDocument().getBody();
            CTSectPr sectPr = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
            XWPFHeaderFooterPolicy policy = new XWPFHeaderFooterPolicy(document, sectPr);

            // === 页眉 ===
            XWPFHeader header = policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT);
            XWPFParagraph headerPara = header.getParagraphArray(0);
            if (headerPara == null) {
                headerPara = header.createParagraph();
            }
            headerPara.setAlignment(ParagraphAlignment.LEFT);
            headerPara.setBorderBottom(Borders.SINGLE);

            // Logo图片 (内置base64)
            try {
                byte[] logoBytes = Base64.getDecoder().decode(LOGO_PNG_BASE64);
                InputStream logoStream = new ByteArrayInputStream(logoBytes);
                XWPFRun logoRun = headerPara.createRun();
                logoRun.addPicture(logoStream, XWPFDocument.PICTURE_TYPE_PNG,
                        "logo.png", Units.toEMU(24), Units.toEMU(24));
                logoStream.close();
            } catch (Exception ignored) {}

            XWPFRun headerTextRun = headerPara.createRun();
            headerTextRun.setText("  PotatoTool | 漏洞扫描报告");
            headerTextRun.setFontFamily("Microsoft YaHei");
            headerTextRun.setFontSize(9);
            headerTextRun.setColor(COLOR_TEXT_MUTED);

            // === 页脚 ===
            XWPFFooter footer = policy.createFooter(XWPFHeaderFooterPolicy.DEFAULT);
            XWPFParagraph footerPara = footer.getParagraphArray(0);
            if (footerPara == null) {
                footerPara = footer.createParagraph();
            }
            footerPara.setAlignment(ParagraphAlignment.CENTER);
            footerPara.setBorderTop(Borders.SINGLE);

            XWPFRun footerTextRun = footerPara.createRun();
            footerTextRun.setText("PotatoTool v2.5.1 | 专业安全审计工具    ");
            footerTextRun.setFontFamily("Microsoft YaHei");
            footerTextRun.setFontSize(8);
            footerTextRun.setColor(COLOR_TEXT_MUTED);

            // 页码字段
            XWPFRun pageRun = footerPara.createRun();
            pageRun.setFontFamily("Microsoft YaHei");
            pageRun.setFontSize(8);
            pageRun.setColor(COLOR_TEXT_MUTED);
            pageRun.getCTR().addNewFldChar().setFldCharType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType.BEGIN);
            XWPFRun pageInstr = footerPara.createRun();
            pageInstr.getCTR().addNewInstrText().setStringValue(" PAGE ");
            XWPFRun pageEnd = footerPara.createRun();
            pageEnd.getCTR().addNewFldChar().setFldCharType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType.END);

            XWPFRun slashRun = footerPara.createRun();
            slashRun.setText(" / ");
            slashRun.setFontSize(8);
            slashRun.setColor(COLOR_TEXT_MUTED);

            XWPFRun totalBegin = footerPara.createRun();
            totalBegin.getCTR().addNewFldChar().setFldCharType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType.BEGIN);
            XWPFRun totalInstr = footerPara.createRun();
            totalInstr.getCTR().addNewInstrText().setStringValue(" NUMPAGES ");
            XWPFRun totalEnd = footerPara.createRun();
            totalEnd.getCTR().addNewFldChar().setFldCharType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType.END);

        } catch (Exception e) {
            // 页眉页脚创建失败不影响报告主体
            e.printStackTrace();
        }
    }

    /**
     * 创建页脚
     */
    private void createFooter(XWPFDocument document) {
        // 页脚已通过 createHeaderFooter 中的 XWPFHeaderFooterPolicy 创建
    }

    /**
     * 设置表格样式（两列）
     */
    private void setTableStyle(XWPFTable table, int col1Width, int col2Width) {
        table.setWidth("100%");
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        if (tblPr == null) {
            tblPr = table.getCTTbl().addNewTblPr();
        }

        // 设置表格边框
        CTTblBorders borders = tblPr.addNewTblBorders();
        setBorder(borders.addNewTop(), STBorder.SINGLE, "CCCCCC", 4);
        setBorder(borders.addNewBottom(), STBorder.SINGLE, "CCCCCC", 4);
        setBorder(borders.addNewLeft(), STBorder.SINGLE, "CCCCCC", 4);
        setBorder(borders.addNewRight(), STBorder.SINGLE, "CCCCCC", 4);
        setBorder(borders.addNewInsideH(), STBorder.SINGLE, "E5E5E5", 4);
        setBorder(borders.addNewInsideV(), STBorder.SINGLE, "E5E5E5", 4);

        // 设置列宽
        for (XWPFTableRow row : table.getRows()) {
            if (row.getTableCells().size() >= 2) {
                setColumnWidth(row.getCell(0), col1Width);
                setColumnWidth(row.getCell(1), col2Width);
            }
        }
    }

    /**
     * 设置表格样式（三列）
     */
    private void setTableStyle(XWPFTable table, int col1Width, int col2Width, int col3Width) {
        table.setWidth("100%");
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        if (tblPr == null) {
            tblPr = table.getCTTbl().addNewTblPr();
        }

        CTTblBorders borders = tblPr.addNewTblBorders();
        setBorder(borders.addNewTop(), STBorder.SINGLE, "CCCCCC", 4);
        setBorder(borders.addNewBottom(), STBorder.SINGLE, "CCCCCC", 4);
        setBorder(borders.addNewLeft(), STBorder.SINGLE, "CCCCCC", 4);
        setBorder(borders.addNewRight(), STBorder.SINGLE, "CCCCCC", 4);
        setBorder(borders.addNewInsideH(), STBorder.SINGLE, "E5E5E5", 4);
        setBorder(borders.addNewInsideV(), STBorder.SINGLE, "E5E5E5", 4);

        for (XWPFTableRow row : table.getRows()) {
            if (row.getTableCells().size() >= 3) {
                setColumnWidth(row.getCell(0), col1Width);
                setColumnWidth(row.getCell(1), col2Width);
                setColumnWidth(row.getCell(2), col3Width);
            }
        }
    }

    /**
     * 设置边框样式
     */
    private void setBorder(CTBorder border, STBorder.Enum style, String color, int size) {
        border.setVal(style);
        border.setColor(color);
        border.setSz(BigInteger.valueOf(size));
    }

    /**
     * 设置列宽
     */
    private void setColumnWidth(XWPFTableCell cell, int width) {
        CTTcPr tcPr = cell.getCTTc().getTcPr();
        if (tcPr == null) {
            tcPr = cell.getCTTc().addNewTcPr();
        }
        CTTblWidth cellWidth = tcPr.addNewTcW();
        cellWidth.setW(BigInteger.valueOf(width));
        cellWidth.setType(STTblWidth.DXA);
    }

    /**
     * 设置表格表头行
     */
    private void setTableHeader(XWPFTable table, int rowIndex, String[] headers) {
        XWPFTableRow row = table.getRow(rowIndex);
        for (int i = 0; i < headers.length && i < row.getTableCells().size(); i++) {
            XWPFTableCell cell = row.getCell(i);
            cell.setColor(COLOR_PRIMARY);
            cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

            XWPFParagraph para = cell.getParagraphs().get(0);
            para.setAlignment(ParagraphAlignment.CENTER);

            XWPFRun run = para.createRun();
            run.setText(headers[i]);
            run.setFontFamily("Microsoft YaHei");
            run.setBold(true);
            run.setFontSize(11);
            run.setColor("FFFFFF");
        }
    }

    /**
     * 设置表格行样式
     */
    private void setStyledTableRow(XWPFTable table, int rowIndex, String key, String value, String valueColor) {
        XWPFTableRow row = table.getRow(rowIndex);

        // 键单元格
        XWPFTableCell keyCell = row.getCell(0);
        keyCell.setColor(COLOR_HEADER_BG);
        keyCell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

        XWPFParagraph keyPara = keyCell.getParagraphs().get(0);
        XWPFRun keyRun = keyPara.createRun();
        keyRun.setText(key);
        keyRun.setFontFamily("Microsoft YaHei");
        keyRun.setBold(true);
        keyRun.setFontSize(10);
        keyRun.setColor(COLOR_TEXT_DARK);

        // 值单元格
        XWPFTableCell valueCell = row.getCell(1);
        valueCell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

        XWPFParagraph valuePara = valueCell.getParagraphs().get(0);
        XWPFRun valueRun = valuePara.createRun();
        valueRun.setText(value);
        valueRun.setFontFamily("Microsoft YaHei");
        valueRun.setFontSize(10);
        if (valueColor != null) {
            valueRun.setBold(true);
            valueRun.setColor(valueColor);
        }
    }

    /**
     * 设置严重度统计行
     */
    private void setStyledSeverityRow(XWPFTable table, int rowIndex, String level, long count, long total, String color) {
        XWPFTableRow row = table.getRow(rowIndex);

        // 级别单元格
        XWPFTableCell levelCell = row.getCell(0);
        levelCell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        XWPFParagraph levelPara = levelCell.getParagraphs().get(0);
        levelPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun levelRun = levelPara.createRun();
        levelRun.setText(level);
        levelRun.setFontFamily("Microsoft YaHei");
        levelRun.setBold(true);
        levelRun.setFontSize(10);
        levelRun.setColor(color);

        // 数量单元格
        XWPFTableCell countCell = row.getCell(1);
        countCell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        XWPFParagraph countPara = countCell.getParagraphs().get(0);
        countPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun countRun = countPara.createRun();
        countRun.setText(String.valueOf(count));
        countRun.setFontFamily("Microsoft YaHei");
        countRun.setBold(true);
        countRun.setFontSize(10);

        // 占比单元格
        XWPFTableCell percentCell = row.getCell(2);
        percentCell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        XWPFParagraph percentPara = percentCell.getParagraphs().get(0);
        percentPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun percentRun = percentPara.createRun();
        double percent = (count * 100.0) / total;
        percentRun.setText(String.format("%.1f%%", percent));
        percentRun.setFontFamily("Microsoft YaHei");
        percentRun.setFontSize(10);
    }

    /**
     * 获取严重度颜色
     */
    private String getSeverityColor(PocObj.Severity severity) {
        switch (severity) {
            case CRITICAL: return COLOR_CRITICAL;
            case HIGH: return COLOR_HIGH;
            case MEDIUM: return COLOR_MEDIUM;
            case LOW: return COLOR_LOW;
            case INFO: return COLOR_INFO;
            default: return COLOR_TEXT_MUTED;
        }
    }
}