package me.gv7.woodpecker.plugin.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.Locale;

public class Utils {
    public static String escape(String payload) {
        return payload.replace("'", "\\'");
    }

    public static String escape(String payload, String c) {
        return payload.replace(c, "\\" + c);
    }

    public static String getFullClassName(byte[] classFile) throws Exception {
        ClassNameReader nameReader = new ClassNameReader(classFile);
        return nameReader.getClassName().replace("/", ".");
    }

    public static String encoder(String text, String encoder) {
        try {
            switch (encoder) {
                case "json":
                    return Utils.escape(text, "\"").replace("\r", "\\r").replace("\n", "\\n");
                case "url":
                    return URLEncoder.encode(text, "utf-8");
                case "unicode":
                    return Utils.escapeJavaStyleString(text);
                default:
                    return text;
            }
        } catch (Exception ex) {
            return text;
        }
    }

    public static String escapeJavaStyleString(String str) throws IOException {
        if (str == null) {
            return null;
        } else {
            StringWriter writer = new StringWriter(str.length() * 2);
            escapeJavaStyleString(writer, str);
            return writer.toString();
        }
    }

    public static void escapeJavaStyleString(Writer out, String str) throws IOException {
        if (out == null) {
            throw new IllegalArgumentException("The Writer must not be null");
        } else if (str != null) {
            int sz = str.length();
            for (int i = 0; i < sz; ++i) {
                char ch = str.charAt(i);
                if (ch > 4095) {
                    out.write("\\u" + hex(ch));
                } else if (ch > 255) {
                    out.write("\\u0" + hex(ch));
                } else if (ch > 127) {
                    out.write("\\u00" + hex(ch));
                } else if (ch < ' ') {
                    if (ch > 15) {
                        out.write("\\u00" + hex(ch));
                    } else {
                        out.write("\\u000" + hex(ch));
                    }
                } else {
                    out.write("\\u00" + hex(ch));
                }
            }

        }
    }

    public static String getErrDetail(Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }

    private static String hex(char ch) {
        return Integer.toHexString(ch).toUpperCase(Locale.ENGLISH);
    }
}
