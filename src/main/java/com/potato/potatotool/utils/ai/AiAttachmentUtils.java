package com.potato.potatotool.utils.ai;

import com.potato.potatotool.utils.ai.model.AiAttachment;
import com.potato.potatotool.utils.ai.model.AiProviderType;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AiAttachmentUtils {
    public static final int MAX_ATTACHMENT_COUNT = 10;
    public static final long MAX_ATTACHMENT_SIZE_BYTES = 100L * 1024L * 1024L;
    public static final long MAX_TOTAL_ATTACHMENT_SIZE_BYTES = 300L * 1024L * 1024L;
    public static final long MAX_INLINE_ATTACHMENT_SIZE_BYTES = 10L * 1024L * 1024L;
    public static final long MAX_INLINE_TOTAL_ATTACHMENT_SIZE_BYTES = 20L * 1024L * 1024L;
    public static final long MAX_TEXT_CONTEXT_ATTACHMENT_SIZE_BYTES = 256L * 1024L;
    public static final long MAX_TEXT_CONTEXT_TOTAL_SIZE_BYTES = 512L * 1024L;

    private static final int MAX_TEXT_SNIFF_BYTES = 64 * 1024;
    private static final double MIN_PRINTABLE_CHAR_RATIO = 0.98D;

    private static final Set<String> TEXT_EXTENSIONS = unmodifiableSet(
            "txt", "log", "md", "rtf", "odt", "epub",
            "html", "htm", "json", "xml", "yaml", "yml", "csv",
            "java", "js", "ts", "py", "go", "php", "rb", "c", "cpp",
            "h", "hpp", "cs", "rs", "sh", "bat", "ps1", "sql",
            "properties", "ini", "conf"
    );
    private static final Set<String> DOCUMENT_EXTENSIONS = unmodifiableSet("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx");
    private static final Set<String> IMAGE_EXTENSIONS = unmodifiableSet("png", "jpg", "jpeg", "webp", "gif", "bmp", "svg");
    private static final Set<String> ARCHIVE_EXTENSIONS = unmodifiableSet("zip");
    private static final Map<String, String> MIME_BY_EXTENSION = buildMimeByExtension();

    private AiAttachmentUtils() {
    }

    public static AiAttachment createAttachment(File file) {
        String fileName = file == null ? "" : file.getName();
        String absolutePath = file == null ? "" : file.getAbsolutePath();
        long fileSize = file == null ? 0L : Math.max(0L, file.length());
        return new AiAttachment(fileName, absolutePath, fileSize, resolveMimeType(file));
    }

    public static String resolveMimeType(File file) {
        if (file == null) {
            return "application/octet-stream";
        }

        String extension = getExtension(file.getName());
        String explicitMimeType = MIME_BY_EXTENSION.get(extension);
        if (explicitMimeType != null && !explicitMimeType.trim().isEmpty()) {
            return explicitMimeType;
        }

        try {
            String detected = Files.probeContentType(file.toPath());
            if (detected != null && !detected.trim().isEmpty()) {
                return detected;
            }
        } catch (Exception ignored) {
        }

        if (isHighConfidenceTextFile(file)) {
            return "text/plain";
        }

        return "application/octet-stream";
    }

    public static boolean isSupportedForUpload(AiAttachment attachment) {
        if (attachment == null) {
            return false;
        }
        String extension = getExtension(attachment.getFileName());
        if (DOCUMENT_EXTENSIONS.contains(extension)
                || IMAGE_EXTENSIONS.contains(extension)
                || ARCHIVE_EXTENSIONS.contains(extension)) {
            return true;
        }
        String mimeType = normalizeMimeType(attachment.getMimeType());
        if (mimeType.startsWith("image/")
                || "application/json".equals(mimeType)
                || "application/pdf".equals(mimeType)
                || "application/zip".equals(mimeType)) {
            return true;
        }
        if (TEXT_EXTENSIONS.contains(extension) || isDeclaredTextMimeType(mimeType)) {
            return isHighConfidenceTextFile(attachment);
        }
        return isHighConfidenceTextFile(attachment);
    }

    public static boolean isSupportedByProvider(AiProviderType providerType, AiAttachment attachment) {
        if (attachment == null) {
            return false;
        }
        if (!isSupportedForUpload(attachment)) {
            return false;
        }
        if (providerType == AiProviderType.ANTHROPIC) {
            return isImage(attachment) || isPdf(attachment) || isTextLike(attachment);
        }
        return true;
    }

    public static boolean supportsRelayInline(AiProviderType providerType, AiAttachment attachment) {
        if (attachment == null) {
            return false;
        }
        if (providerType == AiProviderType.GEMINI) {
            return isImage(attachment) || isPdf(attachment);
        }
        return isImage(attachment);
    }

    public static boolean isTextLike(AiAttachment attachment) {
        if (attachment == null) {
            return false;
        }
        String extension = getExtension(attachment.getFileName());
        String mimeType = normalizeMimeType(attachment.getMimeType());
        if (TEXT_EXTENSIONS.contains(extension) || isDeclaredTextMimeType(mimeType)) {
            return isHighConfidenceTextFile(attachment);
        }
        return isHighConfidenceTextFile(attachment);
    }

    public static boolean isImage(AiAttachment attachment) {
        if (attachment == null) {
            return false;
        }
        String extension = getExtension(attachment.getFileName());
        if (IMAGE_EXTENSIONS.contains(extension)) {
            return true;
        }
        return normalizeMimeType(attachment.getMimeType()).startsWith("image/");
    }

    public static boolean isPdf(AiAttachment attachment) {
        if (attachment == null) {
            return false;
        }
        return "pdf".equals(getExtension(attachment.getFileName()))
                || "application/pdf".equals(normalizeMimeType(attachment.getMimeType()));
    }

    public static String getExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    public static String normalizeMimeType(String mimeType) {
        return mimeType == null ? "" : mimeType.trim().toLowerCase(Locale.ROOT);
    }

    public static String[] buildSupportedChooserPatterns() {
        return new String[]{
                "*.pdf", "*.doc", "*.docx", "*.txt", "*.md", "*.rtf", "*.odt", "*.epub",
                "*.html", "*.htm", "*.json", "*.xml", "*.yaml", "*.yml", "*.csv",
                "*.xls", "*.xlsx", "*.ppt", "*.pptx",
                "*.java", "*.js", "*.ts", "*.py", "*.go", "*.php", "*.rb", "*.c", "*.cpp",
                "*.h", "*.hpp", "*.cs", "*.rs", "*.sh", "*.bat", "*.ps1", "*.sql",
                "*.properties", "*.ini", "*.conf",
                "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif", "*.bmp", "*.svg",
                "*.zip"
        };
    }

    public static String readBase64(AiAttachment attachment) throws Exception {
        File file = requireFile(attachment);
        byte[] bytes = Files.readAllBytes(file.toPath());
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String readDataUrl(AiAttachment attachment) throws Exception {
        String mimeType = normalizeMimeType(attachment == null ? "" : attachment.getMimeType());
        if (mimeType.isEmpty()) {
            mimeType = "application/octet-stream";
        }
        return "data:" + mimeType + ";base64," + readBase64(attachment);
    }

    public static boolean isHighConfidenceTextFile(AiAttachment attachment) {
        try {
            File file = requireFile(attachment);
            return isHighConfidenceTextFile(file);
        } catch (Exception ignored) {
            return false;
        }
    }

    public static TextContextResult readTextContext(AiAttachment attachment) throws Exception {
        File file = requireFile(attachment);
        long fileSize = Math.max(0L, file.length());
        String fileName = attachment == null ? "" : attachment.getFileName();
        if (fileSize <= 0L) {
            return TextContextResult.unsupported(fileName);
        }
        if (fileSize > MAX_TEXT_CONTEXT_ATTACHMENT_SIZE_BYTES) {
            return TextContextResult.tooLarge(fileName, fileSize, MAX_TEXT_CONTEXT_ATTACHMENT_SIZE_BYTES);
        }

        byte[] bytes = Files.readAllBytes(file.toPath());
        DecodedText decodedText = decodeHighConfidenceText(bytes, true);
        if (decodedText == null) {
            return TextContextResult.unsupported(fileName);
        }
        return TextContextResult.supported(fileName, decodedText.text, decodedText.charsetName, fileSize);
    }

    private static Set<String> unmodifiableSet(String... values) {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(values)));
    }

    private static Map<String, String> buildMimeByExtension() {
        Map<String, String> mimeTypes = new HashMap<String, String>();
        mimeTypes.put("pdf", "application/pdf");
        mimeTypes.put("doc", "application/msword");
        mimeTypes.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        mimeTypes.put("xls", "application/vnd.ms-excel");
        mimeTypes.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        mimeTypes.put("ppt", "application/vnd.ms-powerpoint");
        mimeTypes.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        mimeTypes.put("txt", "text/plain");
        mimeTypes.put("log", "text/plain");
        mimeTypes.put("md", "text/markdown");
        mimeTypes.put("rtf", "application/rtf");
        mimeTypes.put("odt", "application/vnd.oasis.opendocument.text");
        mimeTypes.put("epub", "application/epub+zip");
        mimeTypes.put("html", "text/html");
        mimeTypes.put("htm", "text/html");
        mimeTypes.put("json", "application/json");
        mimeTypes.put("xml", "application/xml");
        mimeTypes.put("yaml", "application/x-yaml");
        mimeTypes.put("yml", "application/x-yaml");
        mimeTypes.put("csv", "text/csv");
        mimeTypes.put("java", "text/x-java-source");
        mimeTypes.put("js", "application/javascript");
        mimeTypes.put("ts", "text/plain");
        mimeTypes.put("py", "text/x-python");
        mimeTypes.put("go", "text/x-go");
        mimeTypes.put("php", "application/x-httpd-php");
        mimeTypes.put("rb", "application/x-ruby");
        mimeTypes.put("c", "text/x-c");
        mimeTypes.put("cpp", "text/x-c++");
        mimeTypes.put("h", "text/x-c");
        mimeTypes.put("hpp", "text/x-c++");
        mimeTypes.put("cs", "text/plain");
        mimeTypes.put("rs", "text/plain");
        mimeTypes.put("sh", "application/x-sh");
        mimeTypes.put("bat", "text/plain");
        mimeTypes.put("ps1", "text/plain");
        mimeTypes.put("sql", "application/sql");
        mimeTypes.put("properties", "text/plain");
        mimeTypes.put("ini", "text/plain");
        mimeTypes.put("conf", "text/plain");
        mimeTypes.put("png", "image/png");
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("jpeg", "image/jpeg");
        mimeTypes.put("webp", "image/webp");
        mimeTypes.put("gif", "image/gif");
        mimeTypes.put("bmp", "image/bmp");
        mimeTypes.put("svg", "image/svg+xml");
        mimeTypes.put("zip", "application/zip");
        return Collections.unmodifiableMap(mimeTypes);
    }

    private static boolean isDeclaredTextMimeType(String mimeType) {
        return mimeType.startsWith("text/")
                || "application/json".equals(mimeType)
                || "application/xml".equals(mimeType)
                || "application/x-yaml".equals(mimeType)
                || "application/yaml".equals(mimeType)
                || "application/javascript".equals(mimeType)
                || "application/sql".equals(mimeType)
                || "application/x-sh".equals(mimeType)
                || "text/x-java-source".equals(mimeType)
                || "text/x-python".equals(mimeType)
                || "text/x-go".equals(mimeType)
                || "text/x-c".equals(mimeType)
                || "text/x-c++".equals(mimeType);
    }

    private static boolean isHighConfidenceTextFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }
        long fileSize = Math.max(0L, file.length());
        if (fileSize <= 0L) {
            return false;
        }
        byte[] bytes = readPrefixBytes(file, MAX_TEXT_SNIFF_BYTES);
        if (bytes.length == 0) {
            return false;
        }
        return decodeHighConfidenceText(bytes, false) != null;
    }

    private static byte[] readPrefixBytes(File file, int maxBytes) {
        int targetLength = (int) Math.min(Integer.MAX_VALUE, Math.min(Math.max(1, maxBytes), Math.max(0L, file.length())));
        byte[] buffer = new byte[targetLength];
        int read = 0;
        try (InputStream inputStream = new FileInputStream(file)) {
            while (read < targetLength) {
                int current = inputStream.read(buffer, read, targetLength - read);
                if (current < 0) {
                    break;
                }
                read += current;
            }
        } catch (Exception ignored) {
            return new byte[0];
        }
        if (read == buffer.length) {
            return buffer;
        }
        return Arrays.copyOf(buffer, Math.max(0, read));
    }

    private static DecodedText decodeHighConfidenceText(byte[] bytes, boolean allowFullText) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        CharsetCandidate[] candidates = new CharsetCandidate[]{
                new CharsetCandidate(StandardCharsets.UTF_8, utf8BomLength(bytes), false),
                new CharsetCandidate(StandardCharsets.UTF_8, 0, false),
                new CharsetCandidate(StandardCharsets.UTF_16LE, utf16LeBomLength(bytes), true),
                new CharsetCandidate(StandardCharsets.UTF_16BE, utf16BeBomLength(bytes), true)
        };

        for (CharsetCandidate candidate : candidates) {
            if (!candidate.isPossible(bytes)) {
                continue;
            }
            String decoded = tryDecode(bytes, candidate);
            if (decoded == null) {
                continue;
            }
            if (looksLikeText(decoded, bytes, candidate.allowNullBytes, allowFullText)) {
                return new DecodedText(decoded, candidate.charset.name());
            }
        }
        return null;
    }

    private static String tryDecode(byte[] bytes, CharsetCandidate candidate) {
        try {
            int offset = Math.max(0, candidate.skipBytes);
            if (offset >= bytes.length) {
                return "";
            }
            CharsetDecoder decoder = candidate.charset.newDecoder();
            decoder.onMalformedInput(CodingErrorAction.REPORT);
            decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            return decoder.decode(ByteBuffer.wrap(bytes, offset, bytes.length - offset)).toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean looksLikeText(String text,
                                         byte[] rawBytes,
                                         boolean allowNullBytes,
                                         boolean allowFullText) {
        if (text == null) {
            return false;
        }
        if (!allowNullBytes && containsZeroByte(rawBytes)) {
            return false;
        }
        if (containsSuspiciousBinaryControl(rawBytes, allowNullBytes)) {
            return false;
        }

        int totalChars = text.length();
        if (totalChars == 0) {
            return allowFullText;
        }

        int printableChars = 0;
        for (int i = 0; i < totalChars; i++) {
            char current = text.charAt(i);
            if (current == '\uFFFD') {
                return false;
            }
            if (isAllowedTextChar(current)) {
                printableChars++;
                continue;
            }
            return false;
        }

        return printableChars >= Math.ceil(totalChars * MIN_PRINTABLE_CHAR_RATIO);
    }

    private static boolean isAllowedTextChar(char value) {
        if (value == '\n' || value == '\r' || value == '\t' || value == '\f') {
            return true;
        }
        return !Character.isISOControl(value);
    }

    private static boolean containsZeroByte(byte[] bytes) {
        if (bytes == null) {
            return false;
        }
        for (byte value : bytes) {
            if (value == 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsSuspiciousBinaryControl(byte[] bytes, boolean allowNullBytes) {
        if (bytes == null) {
            return false;
        }
        for (byte value : bytes) {
            int current = value & 0xFF;
            if (current == 0 && allowNullBytes) {
                continue;
            }
            if (current == '\n' || current == '\r' || current == '\t' || current == '\f') {
                continue;
            }
            if (current < 0x20 || current == 0x7F) {
                return true;
            }
        }
        return false;
    }

    private static int utf8BomLength(byte[] bytes) {
        return startsWith(bytes, (byte) 0xEF, (byte) 0xBB, (byte) 0xBF) ? 3 : 0;
    }

    private static int utf16LeBomLength(byte[] bytes) {
        return startsWith(bytes, (byte) 0xFF, (byte) 0xFE) ? 2 : 0;
    }

    private static int utf16BeBomLength(byte[] bytes) {
        return startsWith(bytes, (byte) 0xFE, (byte) 0xFF) ? 2 : 0;
    }

    private static boolean startsWith(byte[] bytes, byte... prefix) {
        if (bytes == null || prefix == null || bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static File requireFile(AiAttachment attachment) {
        if (attachment == null) {
            throw new IllegalArgumentException("附件不能为空");
        }
        File file = attachment.toFile();
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("附件不存在: " + attachment.getFileName());
        }
        return file;
    }

    private static final class CharsetCandidate {
        private final Charset charset;
        private final int skipBytes;
        private final boolean allowNullBytes;

        private CharsetCandidate(Charset charset, int skipBytes, boolean allowNullBytes) {
            this.charset = charset;
            this.skipBytes = Math.max(0, skipBytes);
            this.allowNullBytes = allowNullBytes;
        }

        private boolean isPossible(byte[] bytes) {
            if (bytes == null || bytes.length == 0) {
                return false;
            }
            if (StandardCharsets.UTF_16LE.equals(charset)) {
                return skipBytes > 0 || looksLikeUtf16(bytes, true);
            }
            if (StandardCharsets.UTF_16BE.equals(charset)) {
                return skipBytes > 0 || looksLikeUtf16(bytes, false);
            }
            return true;
        }
    }

    private static boolean looksLikeUtf16(byte[] bytes, boolean littleEndian) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }
        int evenZeroCount = 0;
        int oddZeroCount = 0;
        int pairCount = bytes.length / 2;
        for (int i = 0; i + 1 < bytes.length; i += 2) {
            if (bytes[i] == 0) {
                evenZeroCount++;
            }
            if (bytes[i + 1] == 0) {
                oddZeroCount++;
            }
        }
        double evenRatio = pairCount == 0 ? 0D : (double) evenZeroCount / (double) pairCount;
        double oddRatio = pairCount == 0 ? 0D : (double) oddZeroCount / (double) pairCount;
        if (littleEndian) {
            return oddRatio >= 0.25D && evenRatio <= 0.10D;
        }
        return evenRatio >= 0.25D && oddRatio <= 0.10D;
    }

    private static final class DecodedText {
        private final String text;
        private final String charsetName;

        private DecodedText(String text, String charsetName) {
            this.text = text == null ? "" : text;
            this.charsetName = charsetName == null ? "" : charsetName;
        }
    }

    public static final class TextContextResult {
        private final boolean supported;
        private final boolean tooLarge;
        private final String fileName;
        private final String text;
        private final String charsetName;
        private final long originalSizeBytes;
        private final long limitBytes;

        private TextContextResult(boolean supported,
                                  boolean tooLarge,
                                  String fileName,
                                  String text,
                                  String charsetName,
                                  long originalSizeBytes,
                                  long limitBytes) {
            this.supported = supported;
            this.tooLarge = tooLarge;
            this.fileName = fileName == null ? "" : fileName;
            this.text = text == null ? "" : text;
            this.charsetName = charsetName == null ? "" : charsetName;
            this.originalSizeBytes = Math.max(0L, originalSizeBytes);
            this.limitBytes = Math.max(0L, limitBytes);
        }

        public static TextContextResult supported(String fileName, String text, String charsetName, long originalSizeBytes) {
            return new TextContextResult(true, false, fileName, text, charsetName, originalSizeBytes, 0L);
        }

        public static TextContextResult unsupported(String fileName) {
            return new TextContextResult(false, false, fileName, "", "", 0L, 0L);
        }

        public static TextContextResult tooLarge(String fileName, long originalSizeBytes, long limitBytes) {
            return new TextContextResult(false, true, fileName, "", "", originalSizeBytes, limitBytes);
        }

        public boolean isSupported() {
            return supported;
        }

        public boolean isTooLarge() {
            return tooLarge;
        }

        public String getFileName() {
            return fileName;
        }

        public String getText() {
            return text;
        }

        public String getCharsetName() {
            return charsetName;
        }

        public long getOriginalSizeBytes() {
            return originalSizeBytes;
        }

        public long getLimitBytes() {
            return limitBytes;
        }
    }
}
