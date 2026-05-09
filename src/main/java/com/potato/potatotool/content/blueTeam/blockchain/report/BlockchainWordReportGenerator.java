package com.potato.potatotool.content.blueTeam.blockchain.report;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

public class BlockchainWordReportGenerator {

    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public File generate(ReportData data, String outputPath) throws IOException {
        XWPFDocument document = new XWPFDocument();
        configurePage(document);
        createHeaderFooter(document);
        createCover(document, data);
        createOverview(document, data);
        createInvestigationSection(document, data);
        createSummarySection(document, data);
        createSemanticSection(document, data);
        createEvidenceSection(document, data);
        createAppendix(document, data);

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
            headerRun.setText("  " + I18nUtils.getString("blockchain.export.header"));
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
            footerRun.setText(I18nUtils.getString("blockchain.export.footer") + "    ");
            footerRun.setFontFamily(PotatoReportBranding.FONT_BODY);
            footerRun.setFontSize(8);
            footerRun.setColor(PotatoReportBranding.COLOR_TEXT_MUTED);
            addPageNumberField(footerPara);
        } catch (Exception ignored) {
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
        tagRun.setText(I18nUtils.getString("main.blue.blockchain"));
        tagRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        tagRun.setBold(true);
        tagRun.setFontSize(11);
        tagRun.setColor(PotatoReportBranding.COLOR_ACCENT);

        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        titlePara.setSpacingAfter(90);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText(nonEmptyOrDash(data.getReportTitle()));
        titleRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        titleRun.setBold(true);
        titleRun.setFontSize(26);
        titleRun.setColor(PotatoReportBranding.COLOR_PRIMARY);

        XWPFParagraph subtitlePara = document.createParagraph();
        subtitlePara.setAlignment(ParagraphAlignment.CENTER);
        subtitlePara.setSpacingAfter(180);
        XWPFRun subtitleRun = subtitlePara.createRun();
        subtitleRun.setText(I18nUtils.getString("blockchain.export.subtitle"));
        subtitleRun.setFontFamily(PotatoReportBranding.FONT_BODY);
        subtitleRun.setFontSize(11);
        subtitleRun.setColor(PotatoReportBranding.COLOR_TEXT_MUTED);

        createNarrativeCard(document, I18nUtils.getString("blockchain.export.cover.summary"),
                PotatoReportBranding.COLOR_ACCENT_SOFT, PotatoReportBranding.COLOR_PRIMARY_DARK);

        XWPFTable metaTable = document.createTable(2, 2);
        metaTable.setCellMargins(110, 180, 110, 180);
        styleTable(metaTable, new int[]{4500, 4500});
        setCardCell(metaTable.getRow(0).getCell(0),
                I18nUtils.getString("blockchain.export.meta.generated"),
                data.getGeneratedAtText(),
                PotatoReportBranding.COLOR_SURFACE_SOFT,
                PotatoReportBranding.COLOR_PRIMARY);
        setCardCell(metaTable.getRow(0).getCell(1),
                I18nUtils.getString("blockchain.export.meta.type"),
                data.getReportTypeLabel(),
                PotatoReportBranding.COLOR_SURFACE_SOFT,
                PotatoReportBranding.COLOR_PRIMARY);
        setCardCell(metaTable.getRow(1).getCell(0),
                I18nUtils.getString("blockchain.export.meta.network"),
                data.getNetwork(),
                PotatoReportBranding.COLOR_PRIMARY_SOFT,
                PotatoReportBranding.COLOR_PRIMARY);
        setCardCell(metaTable.getRow(1).getCell(1),
                I18nUtils.getString("blockchain.export.meta.target"),
                data.getTarget(),
                PotatoReportBranding.COLOR_SUCCESS_SOFT,
                PotatoReportBranding.COLOR_SUCCESS);

        addSpacing(document, 300);
        XWPFParagraph divider = document.createParagraph();
        divider.setBorderBottom(Borders.SINGLE);
        divider.setSpacingAfter(0);

        XWPFParagraph pageBreak = document.createParagraph();
        pageBreak.setPageBreak(true);
    }

    private void createOverview(XWPFDocument document, ReportData data) {
        createSectionHeading(document, "01", I18nUtils.getString("blockchain.export.section.overview"));

        XWPFTable metricTable = document.createTable(2, 2);
        metricTable.setCellMargins(110, 180, 110, 180);
        styleTable(metricTable, new int[]{4500, 4500});

        setMetricCell(metricTable.getRow(0).getCell(0),
                I18nUtils.getString("blockchain.report.completeness"),
                nonEmptyOrDash(data.getCompleteness()),
                PotatoReportBranding.COLOR_PRIMARY, PotatoReportBranding.COLOR_PRIMARY_SOFT);
        setMetricCell(metricTable.getRow(0).getCell(1),
                I18nUtils.getString("blockchain.export.metric.findings"),
                String.valueOf(data.getFindingCount()),
                PotatoReportBranding.COLOR_ACCENT, PotatoReportBranding.COLOR_ACCENT_SOFT);
        setMetricCell(metricTable.getRow(1).getCell(0),
                I18nUtils.getString("blockchain.export.metric.warnings"),
                String.valueOf(data.getWarnings().size()),
                PotatoReportBranding.COLOR_MEDIUM, PotatoReportBranding.COLOR_MEDIUM_SOFT);
        setMetricCell(metricTable.getRow(1).getCell(1),
                I18nUtils.getString("blockchain.export.metric.errors"),
                String.valueOf(data.getErrors().size()),
                PotatoReportBranding.COLOR_CRITICAL, PotatoReportBranding.COLOR_CRITICAL_SOFT);

        addSpacing(document, 90);
        createNarrativeCard(document, nonEmptyOrDash(data.getExecutiveSummary()),
                PotatoReportBranding.COLOR_SURFACE_SOFT, PotatoReportBranding.COLOR_TEXT_DARK);
    }

    private void createSummarySection(XWPFDocument document, ReportData data) {
        createSectionHeading(document, "03", I18nUtils.getString("blockchain.export.section.analysis"));
        if (!data.getAiAnalysis().trim().isEmpty()) {
            createNarrativeCard(document, I18nUtils.getString("blockchain.report.ai.notice"),
                    PotatoReportBranding.COLOR_ACCENT_SOFT, PotatoReportBranding.COLOR_TEXT_DARK);
            addSpacing(document, 60);
            createListCard(document, splitLines(data.getAiAnalysis()),
                    PotatoReportBranding.COLOR_SURFACE_SOFT, PotatoReportBranding.COLOR_TEXT_DARK);
        } else {
            createListCard(document, data.getAnalysisLines(),
                    PotatoReportBranding.COLOR_SURFACE_SOFT, PotatoReportBranding.COLOR_TEXT_DARK);
        }
    }

    private void createInvestigationSection(XWPFDocument document, ReportData data) {
        createSectionHeading(document, "02", I18nUtils.getString("blockchain.export.section.investigation"));
        createNamedList(document, I18nUtils.getString("blockchain.report.section.investigation"), data.getInvestigationLines());
        createNamedList(document, I18nUtils.getString("blockchain.report.section.entities"), data.getNarrativeEntityLines());
        createNamedList(document, I18nUtils.getString("blockchain.report.section.attack.steps"), data.getNarrativeStepLines());
        createNamedList(document, I18nUtils.getString("blockchain.report.section.asset.movement"), data.getNarrativeAssetMovementLines());
        createNamedList(document, I18nUtils.getString("blockchain.report.diagram.fund"), data.getFlowLines());
        createNamedList(document, I18nUtils.getString("blockchain.report.diagram.interaction"), data.getSequenceLines());
        createNamedList(document, I18nUtils.getString("blockchain.report.section.attack.analysis"), data.getNarrativeAttackAnalysisLines());
        createNamedList(document, I18nUtils.getString("blockchain.report.section.conclusion"), data.getNarrativeConclusionLines());
    }

    private void createFindingsSection(XWPFDocument document, ReportData data) {
        createSectionHeading(document, "04", I18nUtils.getString("blockchain.report.section.findings"));
        createListCard(document, data.getFindingLines(),
                PotatoReportBranding.COLOR_SURFACE_SOFT, PotatoReportBranding.COLOR_TEXT_DARK);
    }

    private void createEvidenceSection(XWPFDocument document, ReportData data) {
        createSectionHeading(document, "05", I18nUtils.getString("blockchain.report.section.evidence"));
        createNamedList(document, I18nUtils.getString("blockchain.report.drawer.events"), data.getEventLines());
        createNamedList(document, I18nUtils.getString("blockchain.export.subsection.sources"), data.getSources());
        createNamedList(document, I18nUtils.getString("blockchain.export.subsection.warnings"), data.getWarnings());
        createNamedList(document, I18nUtils.getString("blockchain.export.subsection.errors"), data.getErrors());
    }

    private void createStructuredDataSection(XWPFDocument document, ReportData data) {
        createSectionHeading(document, "05", I18nUtils.getString("blockchain.export.section.structured"));
        createNamedList(document, data.isTransactionReport()
                        ? I18nUtils.getString("blockchain.report.section.transaction")
                        : I18nUtils.getString("blockchain.report.section.overview"),
                data.getOverviewLines());
        createNamedList(document, I18nUtils.getString("blockchain.report.section.roles"), data.getRoleLines());
        if (!data.getAssetLines().isEmpty()) {
            createNamedList(document, I18nUtils.getString("blockchain.report.asset.list"), data.getAssetLines());
        }
    }

    private void createPathSection(XWPFDocument document, ReportData data) {
        createSectionHeading(document, "06", I18nUtils.getString("blockchain.export.section.path"));
        createNamedList(document, I18nUtils.getString("blockchain.report.section.flow"), data.getFlowLines());
        createNamedList(document, I18nUtils.getString("blockchain.report.section.timeline"), data.getTimelineLines());
        createNamedList(document, I18nUtils.getString("blockchain.report.section.sequence"), data.getSequenceLines());
    }

    private void createRiskAndFollowUpSection(XWPFDocument document, ReportData data) {
        createSectionHeading(document, "08", I18nUtils.getString("blockchain.export.section.risk"));
        createNamedList(document, I18nUtils.getString("blockchain.report.section.risk"), data.getRiskLines());
        createNamedList(document, I18nUtils.getString("blockchain.report.followup"), data.getFollowUps());
        createNamedList(document, I18nUtils.getString("blockchain.export.subsection.missing"), data.getMissingFields());
    }

    private void createSemanticSection(XWPFDocument document, ReportData data) {
        createSectionHeading(document, "04", I18nUtils.getString("blockchain.export.section.semantic"));
        createNamedList(document, I18nUtils.getString("blockchain.report.section.patterns"), data.getPatternLines());
        createNamedList(document, I18nUtils.getString("blockchain.report.section.evidence.assessment"), data.getEvidenceAssessmentLines());
        createNamedList(document, I18nUtils.getString("blockchain.report.section.context"), data.getContextLines());
    }

    private void createAppendix(XWPFDocument document, ReportData data) {
        createSectionHeading(document, "06", I18nUtils.getString("blockchain.export.section.raw"));
        createCodeBlock(document, data.getRawJson());
    }

    private void createNamedList(XWPFDocument document, String title, List<String> lines) {
        addSpacing(document, 80);
        XWPFTable table = document.createTable(2, 1);
        table.setCellMargins(120, 170, 120, 170);
        styleTable(table, new int[]{9000});

        XWPFTableCell titleCell = table.getRow(0).getCell(0);
        titleCell.setColor(PotatoReportBranding.COLOR_PRIMARY_SOFT);
        writeParagraphCell(titleCell, title, PotatoReportBranding.FONT_BODY, 10,
                PotatoReportBranding.COLOR_PRIMARY_DARK, true);

        XWPFTableCell contentCell = table.getRow(1).getCell(0);
        contentCell.setColor(PotatoReportBranding.COLOR_SURFACE_SOFT);
        writeListCell(contentCell, lines, PotatoReportBranding.FONT_BODY, 10,
                PotatoReportBranding.COLOR_TEXT_DARK);
    }

    private void createListCard(XWPFDocument document, List<String> lines, String backgroundColor, String textColor) {
        XWPFTable table = document.createTable(1, 1);
        table.setCellMargins(120, 170, 120, 170);
        styleTable(table, new int[]{9000});
        XWPFTableCell cell = table.getRow(0).getCell(0);
        cell.setColor(backgroundColor);
        writeListCell(cell, lines, PotatoReportBranding.FONT_BODY, 10, textColor);
    }

    private void createCodeBlock(XWPFDocument document, String content) {
        XWPFTable table = document.createTable(1, 1);
        table.setCellMargins(120, 170, 120, 170);
        styleTable(table, new int[]{9000});
        XWPFTableCell cell = table.getRow(0).getCell(0);
        cell.setColor(PotatoReportBranding.COLOR_CODE_BG);
        writeParagraphCell(cell, content, PotatoReportBranding.FONT_MONO, 9,
                PotatoReportBranding.COLOR_TEXT_DARK, false);
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
        List<String> items = normalizeLines(lines);
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

    private List<String> splitLines(String text) {
        List<String> lines = new ArrayList<String>();
        String[] array = safeContent(text).split("\\r?\\n");
        for (String line : array) {
            String value = safeText(line);
            if (!value.isEmpty()) {
                lines.add(value);
            }
        }
        return lines;
    }

    private List<String> normalizeLines(List<String> lines) {
        List<String> normalized = new ArrayList<String>();
        if (lines != null) {
            for (String line : lines) {
                String value = safeText(line);
                if (!value.isEmpty()) {
                    normalized.add(value);
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

    public static class ReportData {
        private final String reportType;
        private final String reportTypeLabel;
        private final String reportTitle;
        private final String network;
        private final String target;
        private final String generatedAtText;
        private final String completeness;
        private final String executiveSummary;
        private final String aiAnalysis;
        private final String rawJson;
        private final int findingCount;
        private final List<String> analysisLines;
        private final List<String> findingLines;
        private final List<String> warnings;
        private final List<String> errors;
        private final List<String> sources;
        private final List<String> missingFields;
        private final List<String> followUps;
        private final List<String> overviewLines;
        private final List<String> roleLines;
        private final List<String> timelineLines;
        private final List<String> flowLines;
        private final List<String> sequenceLines;
        private final List<String> riskLines;
        private final List<String> patternLines;
        private final List<String> evidenceAssessmentLines;
        private final List<String> contextLines;
        private final List<String> investigationLines;
        private final List<String> narrativeEntityLines;
        private final List<String> narrativeStepLines;
        private final List<String> narrativeAssetMovementLines;
        private final List<String> narrativeAttackAnalysisLines;
        private final List<String> narrativeConclusionLines;
        private final List<String> eventLines;
        private final List<String> assetLines;

        public ReportData(String reportType, String reportTypeLabel, String reportTitle,
                          String network, String target,
                          Date generatedAt, String completeness, String executiveSummary,
                          String aiAnalysis, String rawJson, int findingCount,
                          List<String> analysisLines, List<String> findingLines,
                          List<String> warnings, List<String> errors, List<String> sources,
                          List<String> missingFields, List<String> followUps,
                          List<String> overviewLines, List<String> roleLines,
                          List<String> timelineLines, List<String> flowLines,
                          List<String> sequenceLines, List<String> riskLines,
                          List<String> patternLines, List<String> evidenceAssessmentLines,
                          List<String> contextLines, List<String> investigationLines,
                          List<String> narrativeEntityLines, List<String> narrativeStepLines,
                          List<String> narrativeAssetMovementLines, List<String> narrativeAttackAnalysisLines,
                          List<String> narrativeConclusionLines, List<String> eventLines,
                          List<String> assetLines) {
            this.reportType = reportType;
            this.reportTypeLabel = reportTypeLabel;
            this.reportTitle = reportTitle == null || reportTitle.trim().isEmpty()
                    ? I18nUtils.getString("blockchain.export.title") : reportTitle;
            this.network = network;
            this.target = target;
            this.generatedAtText = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(generatedAt == null ? new Date() : generatedAt);
            this.completeness = completeness == null ? "" : completeness;
            this.executiveSummary = executiveSummary == null ? "" : executiveSummary;
            this.aiAnalysis = aiAnalysis == null ? "" : aiAnalysis;
            this.rawJson = rawJson == null ? "" : rawJson;
            this.findingCount = findingCount;
            this.analysisLines = analysisLines == null ? new ArrayList<String>() : analysisLines;
            this.findingLines = findingLines == null ? new ArrayList<String>() : findingLines;
            this.warnings = warnings == null ? new ArrayList<String>() : warnings;
            this.errors = errors == null ? new ArrayList<String>() : errors;
            this.sources = sources == null ? new ArrayList<String>() : sources;
            this.missingFields = missingFields == null ? new ArrayList<String>() : missingFields;
            this.followUps = followUps == null ? new ArrayList<String>() : followUps;
            this.overviewLines = overviewLines == null ? new ArrayList<String>() : overviewLines;
            this.roleLines = roleLines == null ? new ArrayList<String>() : roleLines;
            this.timelineLines = timelineLines == null ? new ArrayList<String>() : timelineLines;
            this.flowLines = flowLines == null ? new ArrayList<String>() : flowLines;
            this.sequenceLines = sequenceLines == null ? new ArrayList<String>() : sequenceLines;
            this.riskLines = riskLines == null ? new ArrayList<String>() : riskLines;
            this.patternLines = patternLines == null ? new ArrayList<String>() : patternLines;
            this.evidenceAssessmentLines = evidenceAssessmentLines == null ? new ArrayList<String>() : evidenceAssessmentLines;
            this.contextLines = contextLines == null ? new ArrayList<String>() : contextLines;
            this.investigationLines = investigationLines == null ? new ArrayList<String>() : investigationLines;
            this.narrativeEntityLines = narrativeEntityLines == null ? new ArrayList<String>() : narrativeEntityLines;
            this.narrativeStepLines = narrativeStepLines == null ? new ArrayList<String>() : narrativeStepLines;
            this.narrativeAssetMovementLines = narrativeAssetMovementLines == null ? new ArrayList<String>() : narrativeAssetMovementLines;
            this.narrativeAttackAnalysisLines = narrativeAttackAnalysisLines == null ? new ArrayList<String>() : narrativeAttackAnalysisLines;
            this.narrativeConclusionLines = narrativeConclusionLines == null ? new ArrayList<String>() : narrativeConclusionLines;
            this.eventLines = eventLines == null ? new ArrayList<String>() : eventLines;
            this.assetLines = assetLines == null ? new ArrayList<String>() : assetLines;
        }

        public boolean isTransactionReport() {
            return "transaction".equals(reportType);
        }

        public String getReportType() {
            return reportType;
        }

        public String getReportTypeLabel() {
            return reportTypeLabel;
        }

        public String getReportTitle() {
            return reportTitle;
        }

        public String getNetwork() {
            return network;
        }

        public String getTarget() {
            return target;
        }

        public String getGeneratedAtText() {
            return generatedAtText;
        }

        public String getCompleteness() {
            return completeness;
        }

        public String getExecutiveSummary() {
            return executiveSummary;
        }

        public String getAiAnalysis() {
            return aiAnalysis;
        }

        public String getRawJson() {
            return rawJson;
        }

        public int getFindingCount() {
            return findingCount;
        }

        public List<String> getAnalysisLines() {
            return analysisLines;
        }

        public List<String> getFindingLines() {
            return findingLines;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getSources() {
            return sources;
        }

        public List<String> getMissingFields() {
            return missingFields;
        }

        public List<String> getFollowUps() {
            return followUps;
        }

        public List<String> getOverviewLines() {
            return overviewLines;
        }

        public List<String> getRoleLines() {
            return roleLines;
        }

        public List<String> getTimelineLines() {
            return timelineLines;
        }

        public List<String> getFlowLines() {
            return flowLines;
        }

        public List<String> getSequenceLines() {
            return sequenceLines;
        }

        public List<String> getRiskLines() {
            return riskLines;
        }

        public List<String> getPatternLines() {
            return patternLines;
        }

        public List<String> getEvidenceAssessmentLines() {
            return evidenceAssessmentLines;
        }

        public List<String> getContextLines() {
            return contextLines;
        }

        public List<String> getInvestigationLines() {
            return investigationLines;
        }

        public List<String> getNarrativeEntityLines() {
            return narrativeEntityLines;
        }

        public List<String> getNarrativeStepLines() {
            return narrativeStepLines;
        }

        public List<String> getNarrativeAssetMovementLines() {
            return narrativeAssetMovementLines;
        }

        public List<String> getNarrativeAttackAnalysisLines() {
            return narrativeAttackAnalysisLines;
        }

        public List<String> getNarrativeConclusionLines() {
            return narrativeConclusionLines;
        }

        public List<String> getEventLines() {
            return eventLines;
        }

        public List<String> getAssetLines() {
            return assetLines;
        }
    }

    public static List<String> jsonArrayToLines(JsonArray array) {
        List<String> result = new ArrayList<String>();
        if (array == null) {
            return result;
        }
        for (JsonElement element : array) {
            if (element == null || element.isJsonNull()) {
                continue;
            }
            if (element.isJsonPrimitive()) {
                result.add(element.getAsString());
            } else {
                result.add(element.toString());
            }
        }
        return result;
    }

    public static List<String> jsonObjectsToLines(JsonArray array, String primaryKey, String secondaryKey) {
        List<String> result = new ArrayList<String>();
        if (array == null) {
            return result;
        }
        for (JsonElement element : array) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            StringBuilder line = new StringBuilder();
            appendIfPresent(line, object, primaryKey);
            appendIfPresent(line, object, secondaryKey);
            if (line.length() == 0) {
                line.append(object.toString());
            }
            result.add(line.toString());
        }
        return result;
    }

    private static void appendIfPresent(StringBuilder builder, JsonObject object, String key) {
        if (object == null || key == null || key.trim().isEmpty() || !object.has(key) || object.get(key).isJsonNull()) {
            return;
        }
        String value;
        try {
            value = object.get(key).getAsString();
        } catch (Exception e) {
            value = object.get(key).toString();
        }
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(" | ");
        }
        builder.append(key).append("=").append(value.trim());
    }
}
