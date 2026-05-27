package com.potato.potatotool.content.redTeam.portScanner.report;

import com.potato.potatotool.content.redTeam.portScanner.model.PortScanResult;

import java.io.File;
import java.io.IOException;

public class PortScanReportWriter {
    private final PortScanJsonWriter jsonWriter = new PortScanJsonWriter();
    private final PortScanCsvWriter csvWriter = new PortScanCsvWriter();
    private final PortScanTxtWriter txtWriter = new PortScanTxtWriter();
    private final PortScanMarkdownWriter markdownWriter = new PortScanMarkdownWriter();

    public void write(PortScanResult result, String format, File file) throws IOException {
        String normalized = format == null ? "json" : format.trim().toLowerCase();
        if (normalized.contains("csv")) {
            csvWriter.write(result, file);
        } else if (normalized.contains("txt") || normalized.contains("grep")) {
            txtWriter.write(result, file);
        } else if (normalized.contains("md") || normalized.contains("markdown")) {
            markdownWriter.write(result, file);
        } else {
            jsonWriter.write(result, file);
        }
    }
}
