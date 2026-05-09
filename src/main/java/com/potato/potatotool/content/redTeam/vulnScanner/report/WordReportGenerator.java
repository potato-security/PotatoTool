package com.potato.potatotool.content.redTeam.vulnScanner.report;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.model.StepExecutionRecord;
import com.potato.potatotool.utils.report.PotatoReportBranding;
import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.Borders;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 漏洞扫描 Word 报告生成器
 */
public class WordReportGenerator {

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public File generate(List<ScanResult> results, String outputPath, String scanDuration) throws IOException {
        XWPFDocument document = new XWPFDocument();
        Summary summary = buildSummary(results, scanDuration);

        configurePage(document);
        createHeaderFooter(document);
        createCover(document, summary);
        createOverview(document, summary);
        createFindings(document, results);

        File outputFile = new File(outputPath);
        if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            document.write(out);
        } finally {
            document.close();
        }
        return outputFile;
    }

    public File generate(List<ScanResult> results, String outputPath) throws IOException {
        return generate(results, outputPath, "未知");
    }

    private void configurePage(XWPFDocument document) {
        CTBody body = document.getDocument().getBody();
        CTSectPr sectPr = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();

        CTPageSz pageSize = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
        pageSize.setW(BigInteger.valueOf(11906));
        pageSize.setH(BigInteger.valueOf(16838));
        pageSize.setOrient(STPageOrientation.PORTRAIT);

        CTPageMar pageMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        pageMar.setTop(BigInteger.valueOf(900));
        pageMar.setBottom(BigInteger.valueOf(880));
        pageMar.setLeft(BigInteger.valueOf(980));
        pageMar.setRight(BigInteger.valueOf(980));
        pageMar.setHeader(BigInteger.valueOf(540));
        pageMar.setFooter(BigInteger.valueOf(520));
    }

    private void createHeaderFooter(XWPFDocument document) {
        try {
            CTBody body = document.getDocument().getBody();
            CTSectPr sectPr = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
            XWPFHeaderFooterPolicy policy = new XWPFHeaderFooterPolicy(document, sectPr);

            XWPFHeader header = policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT);
            XWPFParagraph headerPara = header.getParagraphArray(0);
            if (headerPara == null) {
                headerPara = header.createParagraph();
            }
            headerPara.setAlignment(ParagraphAlignment.LEFT);
            headerPara.setBorderBottom(Borders.SINGLE);
            PotatoReportBranding.appendIcon(headerPara, 18, 18);

            XWPFRun headerRun = headerPara.createRun();
            headerRun.setText("  PotatoTool | 漏洞扫描报告");
            headerRun.setFontFamily(PotatoReportBranding.FONT_BODY);
            headerRun.setFontSize(9);
            headerRun.setColor(PotatoReportBranding.COLOR_TEXT_MUTED);

            XWPFFooter footer = policy.createFooter(XWPFHeaderFooterPolicy.DEFAULT);
            XWPFParagraph footerPara = footer.getParagraphArray(0);
            if (footerPara == null) {
                footerPara = footer.createParagraph();
            }
            footerPara.setAlignment(ParagraphAlignment.CENTER);
            footerPara.setBorderTop(Borders.SINGLE);

            XWPFRun footerRun = footerPara.createRun();
            footerRun.setText("PotatoTool v2.5 | Red Team · Vuln Scan    ");
            footerRun.setFontFamily(PotatoReportBranding.FONT_BODY);
            footerRun.setFontSize(8);
            footerRun.setColor(PotatoReportBranding.COLOR_TEXT_MUTED);

            addPageNumberField(footerPara);
        } catch (Exception ignored) {
            // 页眉页脚失败时不影响正文导出
        }
    }

    private void addPageNumberField(XWPFParagraph paragraph) {
        XWPFRun pageBegin = paragraph.createRun();
        pageBegin.getCTR().addNewFldChar().setFldCharType(STFldCharType.BEGIN);

        XWPFRun pageInstr = paragraph.createRun();
        pageInstr.getCTR().addNewInstrText().setStringValue(" PAGE ");

        XWPFRun pageEnd = paragraph.createRun();
        pageEnd.getCTR().addNewFldChar().setFldCharType(STFldCharType.END);

        XWPFRun slashRun = paragraph.createRun();
        slashRun.setText(" / ");
        slashRun.setFontSize(8);
        slashRun.setColor(PotatoReportBranding.COLOR_TEXT_MUTED);

        XWPFRun totalBegin = paragraph.createRun();
        totalBegin.getCTR().addNewFldChar().setFldCharType(STFldCharType.BEGIN);

        XWPFRun totalInstr = paragraph.createRun();
        totalInstr.getCTR().addNewInstrText().setStringValue(" NUMPAGES ");

        XWPFRun totalEnd = paragraph.createRun();
        totalEnd.getCTR().addNewFldChar().setFldCharType(STFldCharType.END);
    }

    private void createCover(XWPFDocument document, Summary summary) {
        addSpacing(document, 220);

        XWPFParagraph iconPara = document.createParagraph();
        iconPara.setAlignment(ParagraphAlignment.CENTER);
        PotatoReportBranding.appendIcon(iconPara, 52, 52);

        XWPFParagraph wordmarkPara = document.createParagraph();
        wordmarkPara.setAlignment(ParagraphAlignment.CENTER);
        wordmarkPara.setSpacingAfter(50);
        PotatoReportBranding.appendWordmark(wordmarkPara, 235, 38);

        XWPFParagraph tagPara = document.createParagraph();
        tagPara.setAlignment(ParagraphAlignment.CENTER);
        tagPara.setSpacingAfter(70);
        XWPFRun tagRun = tagPara.createRun();
        tagRun.setText("RED TEAM · VULN SCAN");
        tagRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        tagRun.setBold(true);
        tagRun.setFontSize(11);
        tagRun.setColor(PotatoReportBranding.COLOR_ACCENT);

        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        titlePara.setSpacingAfter(90);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText("PotatoTool 漏洞扫描报告");
        titleRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        titleRun.setBold(true);
        titleRun.setFontSize(26);
        titleRun.setColor(PotatoReportBranding.COLOR_PRIMARY);

        XWPFParagraph subtitlePara = document.createParagraph();
        subtitlePara.setAlignment(ParagraphAlignment.CENTER);
        subtitlePara.setSpacingAfter(180);
        XWPFRun subtitleRun = subtitlePara.createRun();
        subtitleRun.setText("统一输出扫描概览、严重度分布、漏洞详情与请求证据");
        subtitleRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        subtitleRun.setFontSize(11);
        subtitleRun.setColor(PotatoReportBranding.COLOR_TEXT_MUTED);

        createCalloutCard(document,
                "风险等级：" + summary.riskLevel + "。 " + summary.riskSummary,
                summary.riskSoftColor, summary.riskColor);

        XWPFTable metaTable = document.createTable(2, 3);
        metaTable.setCellMargins(110, 170, 110, 170);
        styleTable(metaTable, new int[]{3000, 3000, 3000});
        setMetaCard(metaTable.getRow(0).getCell(0), "生成时间", summary.generatedAt,
                PotatoReportBranding.COLOR_SURFACE_SOFT, PotatoReportBranding.COLOR_PRIMARY);
        setMetaCard(metaTable.getRow(0).getCell(1), "扫描耗时", summary.scanDuration,
                PotatoReportBranding.COLOR_SURFACE_SOFT, PotatoReportBranding.COLOR_PRIMARY);
        setMetaCard(metaTable.getRow(0).getCell(2), "扫描目标数", String.valueOf(summary.targetCount),
                PotatoReportBranding.COLOR_PRIMARY_SOFT, PotatoReportBranding.COLOR_PRIMARY);
        setMetaCard(metaTable.getRow(1).getCell(0), "POC 数量", String.valueOf(summary.pocCount),
                PotatoReportBranding.COLOR_SURFACE_SOFT, PotatoReportBranding.COLOR_PRIMARY_DARK);
        setMetaCard(metaTable.getRow(1).getCell(1), "漏洞结果数", String.valueOf(summary.vulnCount),
                PotatoReportBranding.COLOR_ACCENT_SOFT, PotatoReportBranding.COLOR_ACCENT);
        setMetaCard(metaTable.getRow(1).getCell(2), "高优先级风险", String.valueOf(summary.urgentCount),
                summary.riskSoftColor, summary.riskColor);

        addSpacing(document, 300);
        XWPFParagraph divider = document.createParagraph();
        divider.setBorderBottom(Borders.SINGLE);
        divider.setSpacingAfter(0);

        XWPFParagraph pageBreak = document.createParagraph();
        pageBreak.setPageBreak(true);
    }

    private void createOverview(XWPFDocument document, Summary summary) {
        createSectionHeading(document, "01", "风险概览");

        XWPFTable metricTable = document.createTable(2, 2);
        metricTable.setCellMargins(110, 180, 110, 180);
        styleTable(metricTable, new int[]{4500, 4500});
        setMetricCell(metricTable.getRow(0).getCell(0), "扫描目标数", String.valueOf(summary.targetCount),
                PotatoReportBranding.COLOR_PRIMARY, PotatoReportBranding.COLOR_PRIMARY_SOFT);
        setMetricCell(metricTable.getRow(0).getCell(1), "使用 POC 数", String.valueOf(summary.pocCount),
                PotatoReportBranding.COLOR_PRIMARY_DARK, PotatoReportBranding.COLOR_SURFACE_SOFT);
        setMetricCell(metricTable.getRow(1).getCell(0), "漏洞结果总数", String.valueOf(summary.vulnCount),
                PotatoReportBranding.COLOR_ACCENT, PotatoReportBranding.COLOR_ACCENT_SOFT);
        setMetricCell(metricTable.getRow(1).getCell(1), "高优先级风险", String.valueOf(summary.urgentCount),
                summary.riskColor, summary.riskSoftColor);

        addSpacing(document, 120);

        XWPFTable severityTable = document.createTable(1, 5);
        severityTable.setCellMargins(110, 130, 110, 130);
        styleTable(severityTable, new int[]{1800, 1800, 1800, 1800, 1800});
        setSeverityCell(severityTable.getRow(0).getCell(0), "CRITICAL", summary.critical,
                summary.vulnCount, PotatoReportBranding.COLOR_CRITICAL, PotatoReportBranding.COLOR_CRITICAL_SOFT);
        setSeverityCell(severityTable.getRow(0).getCell(1), "HIGH", summary.high,
                summary.vulnCount, PotatoReportBranding.COLOR_HIGH, PotatoReportBranding.COLOR_HIGH_SOFT);
        setSeverityCell(severityTable.getRow(0).getCell(2), "MEDIUM", summary.medium,
                summary.vulnCount, PotatoReportBranding.COLOR_MEDIUM, PotatoReportBranding.COLOR_MEDIUM_SOFT);
        setSeverityCell(severityTable.getRow(0).getCell(3), "LOW", summary.low,
                summary.vulnCount, PotatoReportBranding.COLOR_LOW, PotatoReportBranding.COLOR_LOW_SOFT);
        setSeverityCell(severityTable.getRow(0).getCell(4), "INFO", summary.info,
                summary.vulnCount, PotatoReportBranding.COLOR_INFO, PotatoReportBranding.COLOR_INFO_SOFT);

        addSpacing(document, 120);
        createCalloutCard(document, summary.actionGuide,
                PotatoReportBranding.COLOR_SURFACE_SOFT, PotatoReportBranding.COLOR_TEXT_DARK);
        addSpacing(document, 90);
    }

    private void createFindings(XWPFDocument document, List<ScanResult> results) {
        createSectionHeading(document, "02", "漏洞详情");

        for (int i = 0; i < results.size(); i++) {
            ScanResult result = results.get(i);
            PocObj.Poc poc = result.getPoc();
            String severityColor = getSeverityColor(poc.getSeverity());
            String severitySoftColor = getSeveritySoftColor(poc.getSeverity());

            XWPFTable headingTable = document.createTable(1, 2);
            headingTable.setCellMargins(110, 170, 110, 170);
            styleTable(headingTable, new int[]{6800, 2200});

            XWPFTableCell leftCell = headingTable.getRow(0).getCell(0);
            leftCell.setColor(severitySoftColor);
            clearCell(leftCell);
            XWPFParagraph titlePara = leftCell.addParagraph();
            titlePara.setAlignment(ParagraphAlignment.LEFT);
            titlePara.setSpacingAfter(35);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(padIndex(i + 1) + "  " + safeText(poc.getName()));
            titleRun.setFontFamily(PotatoReportBranding.FONT_BODY);
            titleRun.setBold(true);
            titleRun.setFontSize(14);
            titleRun.setColor(PotatoReportBranding.COLOR_TEXT_DARK);

            XWPFParagraph targetPara = leftCell.addParagraph();
            targetPara.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun targetRun = targetPara.createRun();
            targetRun.setText("目标地址： " + nonEmpty(result.getTarget()));
            targetRun.setFontFamily(PotatoReportBranding.FONT_BODY);
            targetRun.setFontSize(9);
            targetRun.setColor(PotatoReportBranding.COLOR_TEXT_MUTED);

            XWPFTableCell rightCell = headingTable.getRow(0).getCell(1);
            rightCell.setColor(severityColor);
            clearCell(rightCell);
            XWPFParagraph sevPara = rightCell.addParagraph();
            sevPara.setAlignment(ParagraphAlignment.CENTER);
            sevPara.setSpacingAfter(20);
            XWPFRun sevRun = sevPara.createRun();
            sevRun.setText(poc.getSeverity().name());
            sevRun.setFontFamily(PotatoReportBranding.FONT_BODY);
            sevRun.setBold(true);
            sevRun.setFontSize(13);
            sevRun.setColor(PotatoReportBranding.COLOR_SURFACE);

            XWPFParagraph protoPara = rightCell.addParagraph();
            protoPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun protoRun = protoPara.createRun();
            protoRun.setText(nonEmpty(poc.getProtocol()));
            protoRun.setFontFamily(PotatoReportBranding.FONT_BODY);
            protoRun.setFontSize(9);
            protoRun.setColor(PotatoReportBranding.COLOR_SURFACE);

            List<InfoRow> rows = buildBasicRows(result);
            XWPFTable infoTable = document.createTable(rows.size(), 2);
            infoTable.setCellMargins(110, 170, 110, 170);
            styleTable(infoTable, new int[]{2400, 6600});
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                applyInfoRow(infoTable.getRow(rowIndex), rows.get(rowIndex));
            }

            if (hasText(poc.getDescription())) {
                createLabeledBlock(document, "漏洞说明", poc.getDescription(),
                        PotatoReportBranding.COLOR_SURFACE_SOFT, PotatoReportBranding.FONT_BODY, 10);
            }
            if (hasText(result.getMatchedPath())) {
                createLabeledBlock(document, "检测路径", result.getMatchedPath(),
                        PotatoReportBranding.COLOR_SURFACE_SOFT, PotatoReportBranding.FONT_BODY, 10);
            }
            if (hasText(result.getMatchedPayload())) {
                createCodeBlock(document, "检测 Payload", result.getMatchedPayload());
            }
            if (hasText(result.getFormattedParamKeys())) {
                createLabeledBlock(document, "参数键名", result.getFormattedParamKeys(),
                        PotatoReportBranding.COLOR_SURFACE_SOFT, PotatoReportBranding.FONT_BODY, 10);
            }
            if (hasText(result.getFormattedVariableValues())) {
                createCodeBlock(document, "变量值", result.getFormattedVariableValues());
            }
            if (hasText(result.getFormattedOutputData())) {
                createCodeBlock(document, "提取数据", result.getFormattedOutputData());
            }
            if (hasText(result.getRecommendation())) {
                createCalloutCard(document, "修复建议： " + result.getRecommendation(),
                        PotatoReportBranding.COLOR_ACCENT_SOFT, PotatoReportBranding.COLOR_TEXT_DARK);
            }

            List<StepExecutionRecord> stepRecords = result.getStepRecords();
            if (stepRecords != null && !stepRecords.isEmpty()) {
                createStepRecordsSection(document, stepRecords);
            }

            if (hasText(result.getRawRequest())) {
                createCodeBlock(document, "HTTP 请求", trimCodeBlock(result.getRawRequest(), 5000));
            }
            if (hasText(result.getRawResponseSnippet())) {
                createCodeBlock(document, "HTTP 响应片段", trimCodeBlock(result.getRawResponseSnippet(), 5000));
            }

            XWPFParagraph divider = document.createParagraph();
            divider.setSpacingBefore(80);
            divider.setSpacingAfter(100);
            divider.setBorderBottom(Borders.DASH_SMALL_GAP);
        }
    }

    private List<InfoRow> buildBasicRows(ScanResult result) {
        List<InfoRow> rows = new ArrayList<InfoRow>();
        PocObj.Poc poc = result.getPoc();

        rows.add(new InfoRow("POC ID", poc.getId(), null, false));
        rows.add(new InfoRow("POC 格式", nonEmpty(poc.getOriginalFormat()), null, false));
        rows.add(new InfoRow("协议", nonEmpty(poc.getProtocol()), null, false));
        rows.add(new InfoRow("漏洞类型", nonEmpty(poc.getVulType()), null, false));

        if (hasText(result.getCveId())) {
            rows.add(new InfoRow("CVE ID", result.getCveId(), PotatoReportBranding.COLOR_CRITICAL, false));
        }
        if (hasText(result.getCweId())) {
            rows.add(new InfoRow("CWE ID", result.getCweId(), PotatoReportBranding.COLOR_HIGH, false));
        }
        if (hasText(result.getCvssScore())) {
            rows.add(new InfoRow("CVSS 评分", result.getCvssScore(), PotatoReportBranding.COLOR_MEDIUM, false));
        }

        String target = nonEmpty(result.getTarget());
        rows.add(0, new InfoRow("目标地址", target, null, false));
        return rows;
    }

    private void applyInfoRow(XWPFTableRow row, InfoRow infoRow) {
        XWPFTableCell keyCell = row.getCell(0);
        keyCell.setColor(PotatoReportBranding.COLOR_PRIMARY_SOFT);
        writeCellText(keyCell, infoRow.label, PotatoReportBranding.FONT_BODY, 10,
                PotatoReportBranding.COLOR_PRIMARY_DARK, true);

        XWPFTableCell valueCell = row.getCell(1);
        valueCell.setColor(PotatoReportBranding.COLOR_SURFACE);
        writeCellText(valueCell, infoRow.value,
                infoRow.monospace ? PotatoReportBranding.FONT_MONO : PotatoReportBranding.FONT_BODY,
                10, infoRow.color == null ? PotatoReportBranding.COLOR_TEXT_DARK : infoRow.color,
                infoRow.color != null);
    }

    private void createStepRecordsSection(XWPFDocument document, List<StepExecutionRecord> stepRecords) {
        createMiniHeading(document, "执行链路");

        for (int i = 0; i < stepRecords.size(); i++) {
            StepExecutionRecord record = stepRecords.get(i);
            XWPFTable stepTable = document.createTable(1, 1);
            stepTable.setCellMargins(110, 160, 110, 160);
            styleTable(stepTable, new int[]{9000});
            XWPFTableCell cell = stepTable.getRow(0).getCell(0);
            cell.setColor(record.isMatched() ? PotatoReportBranding.COLOR_SUCCESS_SOFT
                    : PotatoReportBranding.COLOR_SURFACE_SOFT);
            clearCell(cell);

            XWPFParagraph titlePara = cell.addParagraph();
            titlePara.setAlignment(ParagraphAlignment.LEFT);
            titlePara.setSpacingAfter(45);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText("步骤 " + padIndex(i + 1) + (record.isMatched() ? " · 已命中" : " · 未命中"));
            titleRun.setFontFamily(PotatoReportBranding.FONT_BODY);
            titleRun.setBold(true);
            titleRun.setFontSize(11);
            titleRun.setColor(record.isMatched() ? PotatoReportBranding.COLOR_SUCCESS : PotatoReportBranding.COLOR_PRIMARY);

            addStepDetailParagraph(cell, "URL", record.getRequestUrl());
            addStepDetailParagraph(cell, "方法", record.getRequestMethod());
            addStepDetailParagraph(cell, "状态码", String.valueOf(record.getResponseCode()));
            addStepDetailParagraph(cell, "响应时间", record.getResponseTime() + " ms");

            Map<String, Object> extractedVariables = record.getExtractedVariables();
            if (extractedVariables != null && !extractedVariables.isEmpty()) {
                addStepDetailParagraph(cell, "提取变量", mapToText(extractedVariables));
            }
            if (record.getRequestHeaders() != null && !record.getRequestHeaders().isEmpty()) {
                addCodeParagraph(cell, "请求头", mapToText(record.getRequestHeaders()));
            }
            if (hasText(record.getRequestBody())) {
                addCodeParagraph(cell, "请求体", trimCodeBlock(record.getRequestBody(), 1500));
            }
            if (record.getResponseHeaders() != null && !record.getResponseHeaders().isEmpty()) {
                addCodeParagraph(cell, "响应头", mapToText(record.getResponseHeaders()));
            }
            if (hasText(record.getResponseBody())) {
                addCodeParagraph(cell, "响应体", trimCodeBlock(record.getResponseBody(), 1500));
            }
            addSpacing(document, 80);
        }
    }

    private void addStepDetailParagraph(XWPFTableCell cell, String label, String value) {
        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.setAlignment(ParagraphAlignment.LEFT);
        paragraph.setSpacingAfter(20);

        XWPFRun labelRun = paragraph.createRun();
        labelRun.setText(label + "：");
        labelRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        labelRun.setBold(true);
        labelRun.setFontSize(9);
        labelRun.setColor(PotatoReportBranding.COLOR_TEXT_MUTED);

        XWPFRun valueRun = paragraph.createRun();
        valueRun.setText(nonEmpty(value));
        valueRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        valueRun.setFontSize(9);
        valueRun.setColor(PotatoReportBranding.COLOR_TEXT_DARK);
    }

    private void addCodeParagraph(XWPFTableCell cell, String title, String content) {
        XWPFParagraph titlePara = cell.addParagraph();
        titlePara.setAlignment(ParagraphAlignment.LEFT);
        titlePara.setSpacingAfter(15);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText(title);
        titleRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        titleRun.setBold(true);
        titleRun.setFontSize(9);
        titleRun.setColor(PotatoReportBranding.COLOR_PRIMARY);

        XWPFParagraph contentPara = cell.addParagraph();
        contentPara.setAlignment(ParagraphAlignment.LEFT);
        contentPara.setSpacingAfter(25);
        XWPFRun contentRun = contentPara.createRun();
        contentRun.setText(trimCodeBlock(content, 1500));
        contentRun.setFontFamily(PotatoReportBranding.FONT_MONO);
        contentRun.setFontSize(8);
        contentRun.setColor(PotatoReportBranding.COLOR_TEXT_DARK);
    }

    private void createMiniHeading(XWPFDocument document, String title) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(100);
        paragraph.setSpacingAfter(55);
        XWPFRun run = paragraph.createRun();
        run.setText(title);
        run.setFontFamily(PotatoReportBranding.FONT_BODY);
        run.setBold(true);
        run.setFontSize(11);
        run.setColor(PotatoReportBranding.COLOR_PRIMARY);
    }

    private void createCodeBlock(XWPFDocument document, String title, String content) {
        createMiniHeading(document, title);
        XWPFTable table = document.createTable(1, 1);
        table.setCellMargins(120, 170, 120, 170);
        styleTable(table, new int[]{9000});
        XWPFTableCell cell = table.getRow(0).getCell(0);
        cell.setColor(PotatoReportBranding.COLOR_CODE_BG);
        writeCellText(cell, content, PotatoReportBranding.FONT_MONO, 9,
                PotatoReportBranding.COLOR_TEXT_DARK, false);
        addSpacing(document, 70);
    }

    private void createLabeledBlock(XWPFDocument document, String title, String content,
                                    String backgroundColor, String fontFamily, int fontSize) {
        createMiniHeading(document, title);
        XWPFTable table = document.createTable(1, 1);
        table.setCellMargins(120, 170, 120, 170);
        styleTable(table, new int[]{9000});
        XWPFTableCell cell = table.getRow(0).getCell(0);
        cell.setColor(backgroundColor);
        writeCellText(cell, content, fontFamily, fontSize,
                PotatoReportBranding.COLOR_TEXT_DARK, false);
        addSpacing(document, 70);
    }

    private void createSectionHeading(XWPFDocument document, String sectionNo, String title) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(120);
        paragraph.setSpacingAfter(110);

        XWPFRun noRun = paragraph.createRun();
        noRun.setText(sectionNo + "  ");
        noRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        noRun.setBold(true);
        noRun.setFontSize(11);
        noRun.setColor(PotatoReportBranding.COLOR_ACCENT);

        XWPFRun titleRun = paragraph.createRun();
        titleRun.setText(title);
        titleRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        titleRun.setBold(true);
        titleRun.setFontSize(16);
        titleRun.setColor(PotatoReportBranding.COLOR_PRIMARY);
    }

    private void createCalloutCard(XWPFDocument document, String text, String backgroundColor, String textColor) {
        XWPFTable table = document.createTable(1, 1);
        table.setCellMargins(120, 170, 120, 170);
        styleTable(table, new int[]{9000});
        XWPFTableCell cell = table.getRow(0).getCell(0);
        cell.setColor(backgroundColor);
        writeCellText(cell, text, PotatoReportBranding.FONT_BODY, 10, textColor, false);
        addSpacing(document, 80);
    }

    private void setMetaCard(XWPFTableCell cell, String label, String value, String backgroundColor, String valueColor) {
        cell.setColor(backgroundColor);
        clearCell(cell);

        XWPFParagraph labelPara = cell.addParagraph();
        labelPara.setAlignment(ParagraphAlignment.LEFT);
        labelPara.setSpacingAfter(40);
        XWPFRun labelRun = labelPara.createRun();
        labelRun.setText(label);
        labelRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        labelRun.setFontSize(9);
        labelRun.setColor(PotatoReportBranding.COLOR_TEXT_MUTED);

        XWPFParagraph valuePara = cell.addParagraph();
        valuePara.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun valueRun = valuePara.createRun();
        valueRun.setText(nonEmpty(value));
        valueRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        valueRun.setBold(true);
        valueRun.setFontSize(12);
        valueRun.setColor(valueColor);
    }

    private void setMetricCell(XWPFTableCell cell, String label, String value, String valueColor, String backgroundColor) {
        cell.setColor(backgroundColor);
        clearCell(cell);

        XWPFParagraph labelPara = cell.addParagraph();
        labelPara.setAlignment(ParagraphAlignment.LEFT);
        labelPara.setSpacingAfter(45);
        XWPFRun labelRun = labelPara.createRun();
        labelRun.setText(label);
        labelRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        labelRun.setFontSize(9);
        labelRun.setColor(PotatoReportBranding.COLOR_TEXT_MUTED);

        XWPFParagraph valuePara = cell.addParagraph();
        valuePara.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun valueRun = valuePara.createRun();
        valueRun.setText(nonEmpty(value));
        valueRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        valueRun.setBold(true);
        valueRun.setFontSize(18);
        valueRun.setColor(valueColor);
    }

    private void setSeverityCell(XWPFTableCell cell, String title, long count, int total, String accentColor, String backgroundColor) {
        cell.setColor(backgroundColor);
        clearCell(cell);

        XWPFParagraph titlePara = cell.addParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        titlePara.setSpacingAfter(30);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText(title);
        titleRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        titleRun.setBold(true);
        titleRun.setFontSize(10);
        titleRun.setColor(accentColor);

        XWPFParagraph countPara = cell.addParagraph();
        countPara.setAlignment(ParagraphAlignment.CENTER);
        countPara.setSpacingAfter(18);
        XWPFRun countRun = countPara.createRun();
        countRun.setText(String.valueOf(count));
        countRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        countRun.setBold(true);
        countRun.setFontSize(16);
        countRun.setColor(PotatoReportBranding.COLOR_TEXT_DARK);

        XWPFParagraph percentPara = cell.addParagraph();
        percentPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun percentRun = percentPara.createRun();
        percentRun.setText(total <= 0 ? "0.0%" : String.format("%.1f%%", count * 100.0 / total));
        percentRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        percentRun.setFontSize(9);
        percentRun.setColor(PotatoReportBranding.COLOR_TEXT_MUTED);
    }

    private void writeCellText(XWPFTableCell cell, String content, String fontFamily,
                               int fontSize, String color, boolean bold) {
        clearCell(cell);
        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.setAlignment(ParagraphAlignment.LEFT);
        paragraph.setSpacingAfter(0);
        XWPFRun run = paragraph.createRun();
        run.setFontFamily(fontFamily);
        run.setFontSize(fontSize);
        run.setColor(color);
        run.setBold(bold);
        appendMultilineText(run, nonEmpty(content));
    }

    private void appendMultilineText(XWPFRun run, String content) {
        String[] lines = safeContent(content).split("\\r?\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                run.addBreak();
            }
            run.setText(lines[i]);
        }
    }

    private void styleTable(XWPFTable table, int[] widths) {
        table.setWidth("100%");
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        if (tblPr == null) {
            tblPr = table.getCTTbl().addNewTblPr();
        }

        CTTblBorders borders = tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();
        setBorder(borders.isSetTop() ? borders.getTop() : borders.addNewTop(), STBorder.SINGLE,
                PotatoReportBranding.COLOR_BORDER, 5);
        setBorder(borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom(), STBorder.SINGLE,
                PotatoReportBranding.COLOR_BORDER, 5);
        setBorder(borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft(), STBorder.SINGLE,
                PotatoReportBranding.COLOR_BORDER, 5);
        setBorder(borders.isSetRight() ? borders.getRight() : borders.addNewRight(), STBorder.SINGLE,
                PotatoReportBranding.COLOR_BORDER, 5);
        setBorder(borders.isSetInsideH() ? borders.getInsideH() : borders.addNewInsideH(), STBorder.SINGLE,
                PotatoReportBranding.COLOR_BORDER, 4);
        setBorder(borders.isSetInsideV() ? borders.getInsideV() : borders.addNewInsideV(), STBorder.SINGLE,
                PotatoReportBranding.COLOR_BORDER, 4);

        for (XWPFTableRow row : table.getRows()) {
            for (int i = 0; i < row.getTableCells().size(); i++) {
                int width = widths.length == 1 ? widths[0] : widths[Math.min(i, widths.length - 1)];
                setColumnWidth(row.getCell(i), width);
            }
        }
    }

    private void setColumnWidth(XWPFTableCell cell, int width) {
        CTTcPr tcPr = cell.getCTTc().getTcPr();
        if (tcPr == null) {
            tcPr = cell.getCTTc().addNewTcPr();
        }
        CTTblWidth cellWidth = tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW();
        cellWidth.setW(BigInteger.valueOf(width));
        cellWidth.setType(STTblWidth.DXA);
    }

    private void setBorder(CTBorder border, STBorder.Enum style, String color, int size) {
        border.setVal(style);
        border.setColor(color);
        border.setSz(BigInteger.valueOf(size));
    }

    private void clearCell(XWPFTableCell cell) {
        int paragraphCount = cell.getParagraphs().size();
        for (int i = paragraphCount - 1; i >= 0; i--) {
            cell.removeParagraph(i);
        }
    }

    private void addSpacing(XWPFDocument document, int after) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingAfter(after);
    }

    private Summary buildSummary(List<ScanResult> results, String scanDuration) {
        Summary summary = new Summary();
        summary.generatedAt = sdf.format(new Date());
        summary.scanDuration = scanDuration == null || scanDuration.trim().isEmpty() ? "未知" : scanDuration;
        summary.targetCount = distinctTargetCount(results);
        summary.pocCount = distinctPocCount(results);
        summary.vulnCount = results == null ? 0 : results.size();

        Map<PocObj.Severity, Long> severityCount = new HashMap<PocObj.Severity, Long>();
        if (results != null) {
            for (ScanResult result : results) {
                PocObj.Severity severity = result.getPoc().getSeverity();
                Long current = severityCount.get(severity);
                severityCount.put(severity, current == null ? 1L : current + 1L);
            }
        }

        summary.critical = getSeverityCount(severityCount, PocObj.Severity.CRITICAL);
        summary.high = getSeverityCount(severityCount, PocObj.Severity.HIGH);
        summary.medium = getSeverityCount(severityCount, PocObj.Severity.MEDIUM);
        summary.low = getSeverityCount(severityCount, PocObj.Severity.LOW);
        summary.info = getSeverityCount(severityCount, PocObj.Severity.INFO);
        summary.urgentCount = (int) (summary.critical + summary.high);

        if (summary.critical > 0) {
            summary.riskLevel = "极高";
            summary.riskColor = PotatoReportBranding.COLOR_CRITICAL;
            summary.riskSoftColor = PotatoReportBranding.COLOR_CRITICAL_SOFT;
        } else if (summary.high > 0) {
            summary.riskLevel = "高";
            summary.riskColor = PotatoReportBranding.COLOR_HIGH;
            summary.riskSoftColor = PotatoReportBranding.COLOR_HIGH_SOFT;
        } else if (summary.medium > 0) {
            summary.riskLevel = "中";
            summary.riskColor = PotatoReportBranding.COLOR_MEDIUM;
            summary.riskSoftColor = PotatoReportBranding.COLOR_MEDIUM_SOFT;
        } else if (summary.low > 0) {
            summary.riskLevel = "低";
            summary.riskColor = PotatoReportBranding.COLOR_LOW;
            summary.riskSoftColor = PotatoReportBranding.COLOR_LOW_SOFT;
        } else {
            summary.riskLevel = "信息";
            summary.riskColor = PotatoReportBranding.COLOR_INFO;
            summary.riskSoftColor = PotatoReportBranding.COLOR_INFO_SOFT;
        }

        if (summary.urgentCount > 0) {
            summary.riskSummary = "共发现 " + summary.vulnCount + " 条漏洞结果，其中 "
                    + summary.urgentCount + " 条属于高优先级风险，建议立即安排复核与修复。";
            summary.actionGuide = "建议先按 CRITICAL / HIGH 级别建立处置清单，优先完成外网资产、核心系统和存在公开编号漏洞的修复闭环。";
        } else if (summary.vulnCount > 0) {
            summary.riskSummary = "当前未发现高优先级风险，但存在 "
                    + summary.vulnCount + " 条结果需要持续验证与跟踪处置。";
            summary.actionGuide = "建议继续结合业务影响、暴露面与资产重要性完成分级处置，并保留请求证据用于后续复测。";
        } else {
            summary.riskSummary = "当前未发现可导出的漏洞结果。";
            summary.actionGuide = "建议保留本次扫描参数与资产范围，后续在新增指纹、POC 或版本变更后再次执行核验。";
        }
        return summary;
    }

    private int distinctTargetCount(List<ScanResult> results) {
        List<String> targets = new ArrayList<String>();
        if (results != null) {
            for (ScanResult result : results) {
                String target = safeText(result.getTarget());
                if (!target.isEmpty() && !targets.contains(target)) {
                    targets.add(target);
                }
            }
        }
        return targets.size();
    }

    private int distinctPocCount(List<ScanResult> results) {
        List<String> ids = new ArrayList<String>();
        if (results != null) {
            for (ScanResult result : results) {
                String id = safeText(result.getPoc().getId());
                if (!id.isEmpty() && !ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        return ids.size();
    }

    private long getSeverityCount(Map<PocObj.Severity, Long> severityCount, PocObj.Severity severity) {
        Long count = severityCount.get(severity);
        return count == null ? 0L : count.longValue();
    }

    private String getSeverityColor(PocObj.Severity severity) {
        switch (severity) {
            case CRITICAL:
                return PotatoReportBranding.COLOR_CRITICAL;
            case HIGH:
                return PotatoReportBranding.COLOR_HIGH;
            case MEDIUM:
                return PotatoReportBranding.COLOR_MEDIUM;
            case LOW:
                return PotatoReportBranding.COLOR_LOW;
            case INFO:
            default:
                return PotatoReportBranding.COLOR_INFO;
        }
    }

    private String getSeveritySoftColor(PocObj.Severity severity) {
        switch (severity) {
            case CRITICAL:
                return PotatoReportBranding.COLOR_CRITICAL_SOFT;
            case HIGH:
                return PotatoReportBranding.COLOR_HIGH_SOFT;
            case MEDIUM:
                return PotatoReportBranding.COLOR_MEDIUM_SOFT;
            case LOW:
                return PotatoReportBranding.COLOR_LOW_SOFT;
            case INFO:
            default:
                return PotatoReportBranding.COLOR_INFO_SOFT;
        }
    }

    private String mapToText(Map<?, ?> map) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(String.valueOf(entry.getKey())).append(": ").append(String.valueOf(entry.getValue()));
        }
        return builder.toString();
    }

    private String trimCodeBlock(String content, int maxLength) {
        String text = safeContent(content);
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "\n...(内容截断，共 " + text.length() + " 字符)";
    }

    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private String nonEmpty(String text) {
        String value = safeText(text);
        return value.isEmpty() ? "-" : value;
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }

    private String safeContent(String text) {
        return text == null ? "" : text;
    }

    private String padIndex(int index) {
        return index < 10 ? "0" + index : String.valueOf(index);
    }

    private static final class InfoRow {
        private final String label;
        private final String value;
        private final String color;
        private final boolean monospace;

        private InfoRow(String label, String value, String color, boolean monospace) {
            this.label = label;
            this.value = value == null ? "" : value;
            this.color = color;
            this.monospace = monospace;
        }
    }

    private static final class Summary {
        private String generatedAt;
        private String scanDuration;
        private int targetCount;
        private int pocCount;
        private int vulnCount;
        private int urgentCount;
        private long critical;
        private long high;
        private long medium;
        private long low;
        private long info;
        private String riskLevel;
        private String riskColor;
        private String riskSoftColor;
        private String riskSummary;
        private String actionGuide;
    }
}
