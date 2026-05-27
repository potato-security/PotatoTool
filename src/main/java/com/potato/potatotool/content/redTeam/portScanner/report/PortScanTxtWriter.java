package com.potato.potatotool.content.redTeam.portScanner.report;

import com.potato.potatotool.content.redTeam.portScanner.model.PortResult;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanResult;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class PortScanTxtWriter {
    public void write(PortScanResult result, File file) throws IOException {
        try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            for (PortResult port : result.getResults()) {
                writer.write(port.getHost() + ":" + port.getPort() + "\t" + port.getState() + "\t" + safe(port.getService()) + "\t" + safe(port.getBanner()) + "\n");
            }
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
    }
}
