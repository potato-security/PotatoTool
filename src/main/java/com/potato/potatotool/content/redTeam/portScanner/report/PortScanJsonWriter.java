package com.potato.potatotool.content.redTeam.portScanner.report;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanResult;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class PortScanJsonWriter {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void write(PortScanResult result, File file) throws IOException {
        try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            gson.toJson(result, writer);
        }
    }
}
