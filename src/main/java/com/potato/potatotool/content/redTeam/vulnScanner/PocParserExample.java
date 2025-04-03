package com.potato.potatotool.content.redTeam.vulnScanner;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocFileParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Potato
 * @date 2025/3/12 15:38
 */
public class PocParserExample {
    public static void main(String[] args) {
        try (Stream<Path> paths = Files.walk(Paths.get("/Users/a/Desktop/项目开发/PotatoTool/src/main/java/com/potato/potatotool/content/redTeam/vulnScanner/nucleipoc"))) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        String filePath = path.toString();
                        try {
                            System.out.println("Start Parsed file: " + filePath);
                            PocObj.Poc pocObj = PocFileParser.parsePocFromFile(filePath);
                            System.out.println(pocObj);
                            System.out.println("End Parsed file: " + filePath);
                        } catch (Exception e) {
                            System.err.println("Error parsing file: " + filePath);
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
