package com.potato.potatotool.utils.ui;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.fxmisc.richtext.CodeArea;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * @author Potato
 * @date 2023/10/19 17:35
 */
public class ConsoleRedirect {
    private PrintStream originalOut;
    private PrintStream originalErr;
    private TextArea textArea;
    private CodeArea codeArea;

    public ConsoleRedirect(TextArea textArea) {
        this.textArea = textArea;
    }

    public ConsoleRedirect(CodeArea codeArea) {
        this.codeArea = codeArea;
    }

    public void enable() {
        originalOut = System.out;
        originalErr = System.err;

        if (textArea != null) {
            System.setOut(new PrintStream(new CustomOutputStream(textArea, originalOut)));
            System.setErr(new PrintStream(new CustomOutputStream(textArea, originalErr)));
        } else if (codeArea != null) {
            System.setOut(new PrintStream(new CustomOutputStream(codeArea, originalOut)));
            System.setErr(new PrintStream(new CustomOutputStream(codeArea, originalErr)));
        }
    }

    public void disable() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private static class CustomOutputStream extends OutputStream {
        private TextArea textArea;
        private CodeArea codeArea;
        private PrintStream originalStream;

        public CustomOutputStream(TextArea textArea, PrintStream originalStream) {
            this.textArea = textArea;
            this.originalStream = originalStream;
        }

        public CustomOutputStream(CodeArea codeArea, PrintStream originalStream) {
            this.codeArea = codeArea;
            this.originalStream = originalStream;
        }

        private StringBuilder buffer = new StringBuilder();
        @Override
        public void write(int b) {
            char c = (char) b;
            if (c == '\n') {
                flushBuffer();
            } else {
                buffer.append(c);
            }
        }
        public void flushBuffer() {
            String output = buffer.toString();
            buffer.setLength(0);  // 清空缓冲区
            Platform.runLater(() -> {
                if (textArea != null) {
                    textArea.appendText(output + "\n");
                } else if (codeArea != null) {
                    codeArea.appendText(output + "\n");
                }
                originalStream.print(output + "\n");
            });
        }
    }
}
