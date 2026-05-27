package com.potato.potatotool.content.redTeam.portScanner.report;

import com.potato.potatotool.content.redTeam.portScanner.model.PortResult;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanResult;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class PortScanCsvWriter {
    public void write(PortScanResult result, File file) throws IOException {
        try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write("host,port,state,service,tls,rtt_ms,banner\n");
            for (PortResult port : result.getResults()) {
                writer.write(csv(port.getHost()) + "," + port.getPort() + "," + port.getState() + "," + csv(port.getService()) + "," + port.isTls() + "," + port.getRttMs() + "," + csv(port.getBanner()) + "\n");
            }
        }
    }

    private String csv(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"").replace('\n', ' ').replace('\r', ' ') + "\"";
    }
}
