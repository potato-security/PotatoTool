package com.potato.potatotool.content.redTeam.portScanner.report;

import com.potato.potatotool.content.redTeam.portScanner.model.PortResult;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanResult;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class PortScanMarkdownWriter {
    public void write(PortScanResult result, File file) throws IOException {
        try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write("# Port Scan Report\n\n");
            writer.write("- Scan ID: `" + result.getScanId() + "`\n");
            writer.write("- Total Tasks: " + result.getTotalTasks() + "\n");
            writer.write("- Completed Tasks: " + result.getCompletedTasks() + "\n\n");
            writer.write("| Host | Port | State | Service | RTT(ms) | Banner |\n");
            writer.write("| --- | ---: | --- | --- | ---: | --- |\n");
            for (PortResult port : result.getResults()) {
                writer.write("| " + safe(port.getHost()) + " | " + port.getPort() + " | " + port.getState() + " | " + safe(port.getService()) + " | " + port.getRttMs() + " | " + safe(port.getBanner()) + " |\n");
            }
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace('\n', ' ').replace('\r', ' ');
    }
}
