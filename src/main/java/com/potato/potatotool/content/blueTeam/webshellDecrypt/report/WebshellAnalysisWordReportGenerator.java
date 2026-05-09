package com.potato.potatotool.content.blueTeam.webshellDecrypt.report;

import com.potato.potatotool.utils.core.I18nUtils;
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
import java.util.List;

/**
 * 一键解密 AI 分析 Word 报告生成器
 */
public class WebshellAnalysisWordReportGenerator {

    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public File generate(ReportData data, String outputPath) throws IOException {
        XWPFDocument document = new XWPFDocument();

        configurePage(document);
        createHeaderFooter(document);
        createCover(document, data);
        createOverview(document, data);
        createSchemeSection(document, data);
        createCodeSection(document, "03", I18nUtils.getString("webshell.report.section.original"),
                safeText(data.getOriginalContent()));
        createCodeSection(document, "04", I18nUtils.getString("webshell.report.section.decrypted"),
                safeText(data.getDecryptedContent()));
        createAiSection(document, data);

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
            headerPara.setSpacingAfter(0);
            PotatoReportBranding.appendIcon(headerPara, 18, 18);

            XWPFRun headerRun = headerPara.createRun();
            headerRun.setText("  " + I18nUtils.getString("webshell.report.header"));
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
            footerRun.setText(I18nUtils.getString("webshell.report.footer") + "    ");
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

    private void createCover(XWPFDocument document, ReportData data) {
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
        tagRun.setText(I18nUtils.getString("webshell.report.cover.tag"));
        tagRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        tagRun.setBold(true);
        tagRun.setFontSize(11);
        tagRun.setColor(PotatoReportBranding.COLOR_ACCENT);

        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        titlePara.setSpacingAfter(90);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText(I18nUtils.getString("webshell.report.title"));
        titleRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        titleRun.setBold(true);
        titleRun.setFontSize(26);
        titleRun.setColor(PotatoReportBranding.COLOR_PRIMARY);

        XWPFParagraph subtitlePara = document.createParagraph();
        subtitlePara.setAlignment(ParagraphAlignment.CENTER);
        subtitlePara.setSpacingAfter(180);
        XWPFRun subtitleRun = subtitlePara.createRun();
        subtitleRun.setText(I18nUtils.getString("webshell.report.subtitle"));
        subtitleRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        subtitleRun.setFontSize(11);
        subtitleRun.setColor(PotatoReportBranding.COLOR_TEXT_MUTED);

        createNarrativeCard(document,
                I18nUtils.getString("webshell.report.cover.summary"),
                PotatoReportBranding.COLOR_ACCENT_SOFT, PotatoReportBranding.COLOR_PRIMARY_DARK);

        XWPFTable metaTable = document.createTable(2, 2);
        metaTable.setCellMargins(110, 180, 110, 180);
        styleTable(metaTable, new int[]{4500, 4500});
        setCardCell(metaTable.getRow(0).getCell(0),
                I18nUtils.getString("webshell.report.meta.generated"),
                dateTimeFormat.format(data.getGeneratedAt()),
                PotatoReportBranding.COLOR_SURFACE_SOFT,
                PotatoReportBranding.COLOR_PRIMARY);
        setCardCell(metaTable.getRow(0).getCell(1),
                I18nUtils.getString("webshell.report.meta.module"),
                I18nUtils.getString("main.blue.webshell") + " / " + I18nUtils.getString("webshell.ai"),
                PotatoReportBranding.COLOR_SURFACE_SOFT,
                PotatoReportBranding.COLOR_PRIMARY);
        setCardCell(metaTable.getRow(1).getCell(0),
                I18nUtils.getString("webshell.report.meta.status"),
                I18nUtils.getString("webshell.report.status.success"),
                PotatoReportBranding.COLOR_SUCCESS_SOFT,
                PotatoReportBranding.COLOR_SUCCESS);
        setCardCell(metaTable.getRow(1).getCell(1),
                I18nUtils.getString("webshell.report.meta.rule"),
                nonEmptyOrDash(data.getRuleSummary()),
                PotatoReportBranding.COLOR_PRIMARY_SOFT,
                PotatoReportBranding.COLOR_PRIMARY);

        addSpacing(document, 300);
        XWPFParagraph divider = document.createParagraph();
        divider.setBorderBottom(Borders.SINGLE);
        divider.setSpacingAfter(0);

        XWPFParagraph pageBreak = document.createParagraph();
        pageBreak.setPageBreak(true);
    }

    private void createOverview(XWPFDocument document, ReportData data) {
        createSectionHeading(document, "01", I18nUtils.getString("webshell.report.section.overview"));

        XWPFTable metricTable = document.createTable(2, 2);
        metricTable.setCellMargins(110, 180, 110, 180);
        styleTable(metricTable, new int[]{4500, 4500});

        setMetricCell(metricTable.getRow(0).getCell(0),
                I18nUtils.getString("webshell.report.metric.original.length"),
                String.valueOf(safeText(data.getOriginalContent()).length()),
                PotatoReportBranding.COLOR_PRIMARY, PotatoReportBranding.COLOR_PRIMARY_SOFT);
        setMetricCell(metricTable.getRow(0).getCell(1),
                I18nUtils.getString("webshell.report.metric.decrypted.length"),
                String.valueOf(safeText(data.getDecryptedContent()).length()),
                PotatoReportBranding.COLOR_ACCENT, PotatoReportBranding.COLOR_ACCENT_SOFT);
        setMetricCell(metricTable.getRow(1).getCell(0),
                I18nUtils.getString("webshell.report.metric.scheme.count"),
                String.valueOf(countValidSchemes(data.getSchemeLines())),
                PotatoReportBranding.COLOR_SUCCESS, PotatoReportBranding.COLOR_SUCCESS_SOFT);
        setMetricCell(metricTable.getRow(1).getCell(1),
                I18nUtils.getString("webshell.report.metric.ai.length"),
                String.valueOf(safeText(data.getAiAnalysis()).length()),
                PotatoReportBranding.COLOR_PRIMARY_DARK, PotatoReportBranding.COLOR_SURFACE_SOFT);

        addSpacing(document, 140);

        createNarrativeCard(document, buildExecutiveSummary(data),
                PotatoReportBranding.COLOR_PRIMARY_SOFT, PotatoReportBranding.COLOR_TEXT_DARK);
        addSpacing(document, 120);
    }

    private void createSchemeSection(XWPFDocument document, ReportData data) {
        createSectionHeading(document, "02", I18nUtils.getString("webshell.report.section.scheme"));

        XWPFTable infoTable = document.createTable(2, 2);
        infoTable.setCellMargins(110, 180, 110, 180);
        styleTable(infoTable, new int[]{2400, 6600});

        setInfoLabelCell(infoTable.getRow(0).getCell(0), I18nUtils.getString("webshell.report.strategy.rule"));
        writeParagraphCell(infoTable.getRow(0).getCell(1), nonEmptyOrDash(data.getRuleSummary()),
                PotatoReportBranding.FONT_BODY, 10, PotatoReportBranding.COLOR_TEXT_DARK, false);

        setInfoLabelCell(infoTable.getRow(1).getCell(0), I18nUtils.getString("webshell.report.strategy.chain"));
        writeListCell(infoTable.getRow(1).getCell(1), normalizeSchemeLines(data.getSchemeLines()),
                PotatoReportBranding.FONT_BODY, 10, PotatoReportBranding.COLOR_TEXT_DARK);

        addSpacing(document, 120);
    }

    private void createCodeSection(XWPFDocument document, String sectionNo, String title, String content) {
        createSectionHeading(document, sectionNo, title);

        XWPFParagraph summary = document.createParagraph();
        summary.setSpacingAfter(60);
        XWPFRun summaryRun = summary.createRun();
        summaryRun.setText(I18nUtils.getString("webshell.report.code.note"));
        summaryRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        summaryRun.setFontSize(9);
        summaryRun.setColor(PotatoReportBranding.COLOR_TEXT_MUTED);

        XWPFTable table = document.createTable(1, 1);
        table.setCellMargins(120, 170, 120, 170);
        styleTable(table, new int[]{9000});
        XWPFTableCell cell = table.getRow(0).getCell(0);
        cell.setColor(PotatoReportBranding.COLOR_CODE_BG);
        writeParagraphCell(cell, content, PotatoReportBranding.FONT_MONO, 9,
                PotatoReportBranding.COLOR_TEXT_DARK, false);

        addSpacing(document, 120);
    }

    private void createAiSection(XWPFDocument document, ReportData data) {
        createSectionHeading(document, "05", I18nUtils.getString("webshell.report.section.ai"));

        List<AiBlock> aiBlocks = splitAiBlocks(safeText(data.getAiAnalysis()));
        createNarrativeCard(document, I18nUtils.getString("webshell.report.ai.blocks.summary", aiBlocks.size()),
                PotatoReportBranding.COLOR_ACCENT_SOFT, PotatoReportBranding.COLOR_TEXT_DARK);
        addSpacing(document, 80);

        if (aiBlocks.isEmpty()) {
            createNarrativeCard(document, I18nUtils.getString("webshell.report.none"),
                    PotatoReportBranding.COLOR_SURFACE_SOFT, PotatoReportBranding.COLOR_TEXT_MUTED);
            return;
        }

        for (int i = 0; i < aiBlocks.size(); i++) {
            AiBlock block = aiBlocks.get(i);
            XWPFTable blockTable = document.createTable(2, 1);
            blockTable.setCellMargins(120, 170, 120, 170);
            styleTable(blockTable, new int[]{9000});

            XWPFTableCell titleCell = blockTable.getRow(0).getCell(0);
            titleCell.setColor(PotatoReportBranding.COLOR_PRIMARY_SOFT);
            writeParagraphCell(titleCell,
                    block.getTitle().isEmpty()
                            ? I18nUtils.getString("webshell.report.ai.point", padIndex(i + 1))
                            : block.getTitle(),
                    PotatoReportBranding.FONT_BODY, 11, PotatoReportBranding.COLOR_PRIMARY, true);

            XWPFTableCell contentCell = blockTable.getRow(1).getCell(0);
            contentCell.setColor(PotatoReportBranding.COLOR_SURFACE_SOFT);
            writeParagraphCell(contentCell, block.getContent(), PotatoReportBranding.FONT_BODY, 10,
                    PotatoReportBranding.COLOR_TEXT_DARK, false);
            addSpacing(document, 110);
        }
    }

    private String buildExecutiveSummary(ReportData data) {
        int schemeCount = countValidSchemes(data.getSchemeLines());
        int aiBlocks = splitAiBlocks(safeText(data.getAiAnalysis())).size();
        return I18nUtils.getString(
                "webshell.report.executive.summary",
                schemeCount,
                safeText(data.getOriginalContent()).length(),
                safeText(data.getDecryptedContent()).length(),
                aiBlocks
        );
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

    private void createNarrativeCard(XWPFDocument document, String text, String backgroundColor, String textColor) {
        XWPFTable table = document.createTable(1, 1);
        table.setCellMargins(120, 170, 120, 170);
        styleTable(table, new int[]{9000});
        XWPFTableCell cell = table.getRow(0).getCell(0);
        cell.setColor(backgroundColor);
        writeParagraphCell(cell, text, PotatoReportBranding.FONT_BODY, 10, textColor, false);
    }

    private void setCardCell(XWPFTableCell cell, String label, String value, String backgroundColor, String accentColor) {
        cell.setColor(backgroundColor);
        clearCell(cell);

        XWPFParagraph labelPara = cell.addParagraph();
        labelPara.setSpacingAfter(45);
        labelPara.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun labelRun = labelPara.createRun();
        labelRun.setText(label);
        labelRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        labelRun.setFontSize(9);
        labelRun.setColor(PotatoReportBranding.COLOR_TEXT_MUTED);

        XWPFParagraph valuePara = cell.addParagraph();
        valuePara.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun valueRun = valuePara.createRun();
        valueRun.setText(nonEmptyOrDash(value));
        valueRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        valueRun.setBold(true);
        valueRun.setFontSize(12);
        valueRun.setColor(accentColor);
    }

    private void setMetricCell(XWPFTableCell cell, String label, String value, String accentColor, String backgroundColor) {
        cell.setColor(backgroundColor);
        clearCell(cell);

        XWPFParagraph labelPara = cell.addParagraph();
        labelPara.setSpacingAfter(50);
        labelPara.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun labelRun = labelPara.createRun();
        labelRun.setText(label);
        labelRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        labelRun.setFontSize(9);
        labelRun.setColor(PotatoReportBranding.COLOR_TEXT_MUTED);

        XWPFParagraph valuePara = cell.addParagraph();
        valuePara.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun valueRun = valuePara.createRun();
        valueRun.setText(nonEmptyOrDash(value));
        valueRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        valueRun.setBold(true);
        valueRun.setFontSize(18);
        valueRun.setColor(accentColor);
    }

    private void setInfoLabelCell(XWPFTableCell cell, String label) {
        cell.setColor(PotatoReportBranding.COLOR_PRIMARY_SOFT);
        writeParagraphCell(cell, label, PotatoReportBranding.FONT_BODY, 10,
                PotatoReportBranding.COLOR_PRIMARY_DARK, true);
    }

    private void writeParagraphCell(XWPFTableCell cell, String content, String fontFamily,
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
        appendMultilineText(run, normalizedCellText(content));
    }

    private void writeListCell(XWPFTableCell cell, List<String> lines, String fontFamily,
                               int fontSize, String color) {
        clearCell(cell);
        List<String> items = (lines == null || lines.isEmpty())
                ? normalizeSchemeLines(null) : lines;
        for (int i = 0; i < items.size(); i++) {
            XWPFParagraph paragraph = cell.addParagraph();
            paragraph.setAlignment(ParagraphAlignment.LEFT);
            paragraph.setSpacingAfter(i == items.size() - 1 ? 0 : 35);
            XWPFRun run = paragraph.createRun();
            run.setFontFamily(fontFamily);
            run.setFontSize(fontSize);
            run.setColor(color);
            run.setText("• " + normalizedCellText(items.get(i)));
        }
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

    private List<AiBlock> splitAiBlocks(String content) {
        List<AiBlock> blocks = new ArrayList<AiBlock>();
        if (content.trim().isEmpty()) {
            return blocks;
        }

        String[] lines = content.split("\\r?\\n");
        String currentTitle = "";
        StringBuilder currentContent = new StringBuilder();
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (isAiHeading(trimmed)) {
                flushAiBlock(blocks, currentTitle, currentContent);
                currentTitle = normalizeAiHeading(trimmed);
                currentContent = new StringBuilder();
            } else {
                if (currentContent.length() > 0) {
                    currentContent.append("\n");
                }
                currentContent.append(line == null ? "" : line);
            }
        }
        flushAiBlock(blocks, currentTitle, currentContent);
        return blocks;
    }

    private void flushAiBlock(List<AiBlock> blocks, String title, StringBuilder content) {
        String text = safeText(content == null ? "" : content.toString());
        if (!title.isEmpty() || !text.isEmpty()) {
            blocks.add(new AiBlock(title, text.isEmpty() ? I18nUtils.getString("webshell.report.none") : text));
        }
    }

    private boolean isAiHeading(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return (text.startsWith("【") && text.endsWith("】")) || (text.startsWith("[") && text.endsWith("]"));
    }

    private String normalizeAiHeading(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("【", "").replace("】", "").replace("[", "").replace("]", "").trim();
    }

    private int countValidSchemes(List<String> schemeLines) {
        List<String> normalized = normalizeSchemeLines(schemeLines);
        if (normalized.size() == 1 && I18nUtils.getString("webshell.report.none").equals(normalized.get(0))) {
            return 0;
        }
        return normalized.size();
    }

    private List<String> normalizeSchemeLines(List<String> lines) {
        List<String> normalized = new ArrayList<String>();
        if (lines != null) {
            for (String line : lines) {
                String text = safeText(line);
                if (!text.isEmpty()) {
                    normalized.add(text);
                }
            }
        }
        if (normalized.isEmpty()) {
            normalized.add(I18nUtils.getString("webshell.report.none"));
        }
        return normalized;
    }

    private String normalizedCellText(String value) {
        String text = safeContent(value);
        return text.trim().isEmpty() ? I18nUtils.getString("webshell.report.none") : text;
    }

    private String nonEmptyOrDash(String value) {
        String text = safeText(value);
        return text.isEmpty() ? I18nUtils.getString("webshell.report.none") : text;
    }

    private String safeContent(String value) {
        return value == null ? "" : value;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String padIndex(int index) {
        return index < 10 ? "0" + index : String.valueOf(index);
    }

    private static final class AiBlock {
        private final String title;
        private final String content;

        private AiBlock(String title, String content) {
            this.title = title == null ? "" : title;
            this.content = content == null ? "" : content;
        }

        private String getTitle() {
            return title;
        }

        private String getContent() {
            return content;
        }
    }

    public static final class ReportData {
        private final String originalContent;
        private final String decryptedContent;
        private final String aiAnalysis;
        private final String ruleSummary;
        private final List<String> schemeLines;
        private final Date generatedAt;

        public ReportData(String originalContent,
                          String decryptedContent,
                          String aiAnalysis,
                          String ruleSummary,
                          List<String> schemeLines,
                          Date generatedAt) {
            this.originalContent = originalContent == null ? "" : originalContent;
            this.decryptedContent = decryptedContent == null ? "" : decryptedContent;
            this.aiAnalysis = aiAnalysis == null ? "" : aiAnalysis;
            this.ruleSummary = ruleSummary == null ? "" : ruleSummary;
            this.schemeLines = schemeLines == null ? new ArrayList<String>() : new ArrayList<String>(schemeLines);
            this.generatedAt = generatedAt == null ? new Date() : new Date(generatedAt.getTime());
        }

        public String getOriginalContent() {
            return originalContent;
        }

        public String getDecryptedContent() {
            return decryptedContent;
        }

        public String getAiAnalysis() {
            return aiAnalysis;
        }

        public String getRuleSummary() {
            return ruleSummary;
        }

        public List<String> getSchemeLines() {
            return new ArrayList<String>(schemeLines);
        }

        public Date getGeneratedAt() {
            return new Date(generatedAt.getTime());
        }
    }
}
