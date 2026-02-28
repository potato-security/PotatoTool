package com.potato.potatotool.content.redTeam.vulnScanner.http;

import java.io.*;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;

/**
 * TLS数据包构造器
 * 负责手动构造TLS Client Hello报文和解析Server Hello响应
 */
public class TlsPacketBuilder {

    private static final SecureRandom random = new SecureRandom();

    // GREASE值 (RFC 8701)
    private static final int[] GREASE_VALUES = {
        0x0a0a, 0x1a1a, 0x2a2a, 0x3a3a, 0x4a4a, 0x5a5a, 0x6a6a, 0x7a7a,
        0x8a8a, 0x9a9a, 0xaaaa, 0xbaba, 0xcaca, 0xdada, 0xeaea, 0xfafa
    };

    /**
     * TLS扩展类型
     */
    public static class Extension {
        public static final int SERVER_NAME = 0x0000;
        public static final int STATUS_REQUEST = 0x0005;
        public static final int SUPPORTED_GROUPS = 0x000a;
        public static final int EC_POINT_FORMATS = 0x000b;
        public static final int SIGNATURE_ALGORITHMS = 0x000d;
        public static final int ALPN = 0x0010;
        public static final int ENCRYPT_THEN_MAC = 0x0016;
        public static final int EXTENDED_MASTER_SECRET = 0x0017;
        public static final int SESSION_TICKET = 0x0023;
        public static final int SUPPORTED_VERSIONS = 0x002b;
        public static final int PSK_KEY_EXCHANGE_MODES = 0x002d;
        public static final int KEY_SHARE = 0x0033;
    }

    /**
     * 构造TLS Client Hello数据包
     *
     * @param hostname 目标主机名
     * @param probe JARM探测配置
     * @return TLS Client Hello字节数组
     */
    public static byte[] buildClientHello(String hostname, JarmProbe probe) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 生成随机数
        byte[] random32 = new byte[32];
        random.nextBytes(random32);

        // Session ID (空)
        byte sessionIdLength = 0;

        // 获取排序后的密码套件
        int[] cipherSuites = probe.getOrderedCipherSuites();

        // 如果使用GREASE，在密码套件列表开头插入GREASE值
        if (probe.isUseGrease()) {
            int greaseValue = GREASE_VALUES[random.nextInt(GREASE_VALUES.length)];
            int[] withGrease = new int[cipherSuites.length + 1];
            withGrease[0] = greaseValue;
            System.arraycopy(cipherSuites, 0, withGrease, 1, cipherSuites.length);
            cipherSuites = withGrease;
        }

        // 构造扩展
        List<byte[]> extensions = buildExtensions(hostname, probe);

        // 计算Client Hello长度
        int clientHelloLength = 2 + 32 + 1 + sessionIdLength + 2 + (cipherSuites.length * 2)
                + 1 + 1; // TLS version + random + session id length + session id + cipher length + ciphers + compression length + compression

        // 添加扩展长度
        int extensionsLength = 0;
        for (byte[] ext : extensions) {
            extensionsLength += ext.length;
        }
        clientHelloLength += 2 + extensionsLength; // extensions length + extensions

        // TLS Record Layer Header
        baos.write(0x16); // Content Type: Handshake
        baos.write(probe.getTlsVersion().value >> 8); // Record version (high byte)
        baos.write(probe.getTlsVersion().value & 0xFF); // Record version (low byte)

        int recordLength = 4 + clientHelloLength; // handshake type (1) + length (3) + client hello
        baos.write(recordLength >> 8);
        baos.write(recordLength & 0xFF);

        // Handshake Protocol: Client Hello
        baos.write(0x01); // Handshake Type: Client Hello
        baos.write(clientHelloLength >> 16); // Length (3 bytes)
        baos.write(clientHelloLength >> 8);
        baos.write(clientHelloLength & 0xFF);

        // Client Hello
        baos.write(probe.getTlsVersion().value >> 8); // Client Version (high byte)
        baos.write(probe.getTlsVersion().value & 0xFF); // Client Version (low byte)
        baos.write(random32); // Random (32 bytes)

        // Session ID
        baos.write(sessionIdLength);

        // Cipher Suites
        baos.write((cipherSuites.length * 2) >> 8);
        baos.write((cipherSuites.length * 2) & 0xFF);
        for (int cipher : cipherSuites) {
            baos.write(cipher >> 8);
            baos.write(cipher & 0xFF);
        }

        // Compression Methods (null compression only)
        baos.write(0x01); // Length
        baos.write(0x00); // Null compression

        // Extensions
        baos.write(extensionsLength >> 8);
        baos.write(extensionsLength & 0xFF);
        for (byte[] ext : extensions) {
            baos.write(ext);
        }

        return baos.toByteArray();
    }

    /**
     * 构造TLS扩展
     */
    private static List<byte[]> buildExtensions(String hostname, JarmProbe probe) throws IOException {
        List<byte[]> extensions = new ArrayList<>();

        // 扩展构造顺序
        List<Integer> extensionOrder = new ArrayList<>();

        // 根据扩展排序配置决定顺序
        if (probe.getExtensionOrder() == JarmProbe.ExtensionOrder.FORWARD) {
            extensionOrder = Arrays.asList(
                Extension.SERVER_NAME,
                Extension.EXTENDED_MASTER_SECRET,
                Extension.SUPPORTED_GROUPS,
                Extension.EC_POINT_FORMATS,
                Extension.SESSION_TICKET,
                Extension.ALPN,
                Extension.STATUS_REQUEST,
                Extension.SIGNATURE_ALGORITHMS,
                Extension.SUPPORTED_VERSIONS
            );
        } else { // REVERSE
            extensionOrder = Arrays.asList(
                Extension.SUPPORTED_VERSIONS,
                Extension.SIGNATURE_ALGORITHMS,
                Extension.STATUS_REQUEST,
                Extension.ALPN,
                Extension.SESSION_TICKET,
                Extension.EC_POINT_FORMATS,
                Extension.SUPPORTED_GROUPS,
                Extension.EXTENDED_MASTER_SECRET,
                Extension.SERVER_NAME
            );
        }

        // 如果使用GREASE，添加GREASE扩展
        if (probe.isUseGrease()) {
            int greaseValue = GREASE_VALUES[random.nextInt(GREASE_VALUES.length)];
            extensions.add(buildGreaseExtension(greaseValue));
        }

        for (int extType : extensionOrder) {
            byte[] ext = null;

            switch (extType) {
                case Extension.SERVER_NAME:
                    ext = buildServerNameExtension(hostname);
                    break;

                case Extension.EXTENDED_MASTER_SECRET:
                    ext = buildEmptyExtension(Extension.EXTENDED_MASTER_SECRET);
                    break;

                case Extension.SUPPORTED_GROUPS:
                    ext = buildSupportedGroupsExtension();
                    break;

                case Extension.EC_POINT_FORMATS:
                    ext = buildEcPointFormatsExtension();
                    break;

                case Extension.SESSION_TICKET:
                    ext = buildEmptyExtension(Extension.SESSION_TICKET);
                    break;

                case Extension.ALPN:
                    ext = buildAlpnExtension(probe.getAlpnType());
                    break;

                case Extension.STATUS_REQUEST:
                    ext = buildStatusRequestExtension();
                    break;

                case Extension.SIGNATURE_ALGORITHMS:
                    ext = buildSignatureAlgorithmsExtension();
                    break;

                case Extension.SUPPORTED_VERSIONS:
                    if (probe.getVersionSupport() != JarmProbe.VersionSupport.NO_SUPPORT) {
                        ext = buildSupportedVersionsExtension(probe.getVersionSupport());
                    }
                    break;
            }

            if (ext != null) {
                extensions.add(ext);
            }
        }

        return extensions;
    }

    /**
     * 构造SNI扩展
     */
    private static byte[] buildServerNameExtension(String hostname) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] hostnameBytes = hostname.getBytes("UTF-8");

        // Extension Type
        baos.write(Extension.SERVER_NAME >> 8);
        baos.write(Extension.SERVER_NAME & 0xFF);

        // Extension Length
        int extLength = 2 + 1 + 2 + hostnameBytes.length; // list length + name type + name length + name
        baos.write(extLength >> 8);
        baos.write(extLength & 0xFF);

        // Server Name List Length
        int listLength = 1 + 2 + hostnameBytes.length;
        baos.write(listLength >> 8);
        baos.write(listLength & 0xFF);

        // Server Name Type (0 = hostname)
        baos.write(0x00);

        // Server Name Length
        baos.write(hostnameBytes.length >> 8);
        baos.write(hostnameBytes.length & 0xFF);

        // Server Name
        baos.write(hostnameBytes);

        return baos.toByteArray();
    }

    /**
     * 构造空扩展
     */
    private static byte[] buildEmptyExtension(int extensionType) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(extensionType >> 8);
        baos.write(extensionType & 0xFF);
        baos.write(0x00); // Length high
        baos.write(0x00); // Length low
        return baos.toByteArray();
    }

    /**
     * 构造Supported Groups扩展 (椭圆曲线)
     */
    private static byte[] buildSupportedGroupsExtension() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Common elliptic curves
        int[] groups = {0x001d, 0x0017, 0x0018, 0x0019, 0x001e, 0x001f, 0x0100};

        baos.write(Extension.SUPPORTED_GROUPS >> 8);
        baos.write(Extension.SUPPORTED_GROUPS & 0xFF);

        int extLength = 2 + (groups.length * 2);
        baos.write(extLength >> 8);
        baos.write(extLength & 0xFF);

        baos.write((groups.length * 2) >> 8);
        baos.write((groups.length * 2) & 0xFF);

        for (int group : groups) {
            baos.write(group >> 8);
            baos.write(group & 0xFF);
        }

        return baos.toByteArray();
    }

    /**
     * 构造EC Point Formats扩展
     */
    private static byte[] buildEcPointFormatsExtension() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        baos.write(Extension.EC_POINT_FORMATS >> 8);
        baos.write(Extension.EC_POINT_FORMATS & 0xFF);
        baos.write(0x00); // Length high
        baos.write(0x02); // Length low
        baos.write(0x01); // EC point format length
        baos.write(0x00); // uncompressed

        return baos.toByteArray();
    }

    /**
     * 构造ALPN扩展
     */
    private static byte[] buildAlpnExtension(JarmProbe.AlpnType alpnType) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        baos.write(Extension.ALPN >> 8);
        baos.write(Extension.ALPN & 0xFF);

        String[] protocols;
        if (alpnType == JarmProbe.AlpnType.RARE) {
            protocols = new String[]{"h2c", "http/0.9"};
        } else {
            protocols = new String[]{"h2", "http/1.1"};
        }

        int totalLength = 0;
        for (String protocol : protocols) {
            totalLength += 1 + protocol.length();
        }

        baos.write((totalLength + 2) >> 8);
        baos.write((totalLength + 2) & 0xFF);

        baos.write(totalLength >> 8);
        baos.write(totalLength & 0xFF);

        for (String protocol : protocols) {
            byte[] protocolBytes = protocol.getBytes("UTF-8");
            baos.write(protocolBytes.length);
            baos.write(protocolBytes);
        }

        return baos.toByteArray();
    }

    /**
     * 构造Status Request扩展 (OCSP Stapling)
     */
    private static byte[] buildStatusRequestExtension() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        baos.write(Extension.STATUS_REQUEST >> 8);
        baos.write(Extension.STATUS_REQUEST & 0xFF);
        baos.write(0x00); // Length high
        baos.write(0x05); // Length low
        baos.write(0x01); // status_type: ocsp
        baos.write(0x00); // responder_id_list length high
        baos.write(0x00); // responder_id_list length low
        baos.write(0x00); // request_extensions length high
        baos.write(0x00); // request_extensions length low

        return baos.toByteArray();
    }

    /**
     * 构造Signature Algorithms扩展
     */
    private static byte[] buildSignatureAlgorithmsExtension() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int[] algorithms = {
            0x0403, 0x0804, 0x0401, 0x0503, 0x0805, 0x0501,
            0x0806, 0x0601, 0x0201
        };

        baos.write(Extension.SIGNATURE_ALGORITHMS >> 8);
        baos.write(Extension.SIGNATURE_ALGORITHMS & 0xFF);

        int extLength = 2 + (algorithms.length * 2);
        baos.write(extLength >> 8);
        baos.write(extLength & 0xFF);

        baos.write((algorithms.length * 2) >> 8);
        baos.write((algorithms.length * 2) & 0xFF);

        for (int alg : algorithms) {
            baos.write(alg >> 8);
            baos.write(alg & 0xFF);
        }

        return baos.toByteArray();
    }

    /**
     * 构造Supported Versions扩展
     */
    private static byte[] buildSupportedVersionsExtension(JarmProbe.VersionSupport versionSupport) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        baos.write(Extension.SUPPORTED_VERSIONS >> 8);
        baos.write(Extension.SUPPORTED_VERSIONS & 0xFF);

        List<Integer> versions = new ArrayList<>();

        if (versionSupport == JarmProbe.VersionSupport.TLS_1_3_SUPPORT) {
            versions.add(0x0304); // TLS 1.3
            versions.add(0x0303); // TLS 1.2
        } else if (versionSupport == JarmProbe.VersionSupport.TLS_1_2_SUPPORT) {
            versions.add(0x0303); // TLS 1.2
            versions.add(0x0302); // TLS 1.1
        }

        int extLength = 1 + (versions.size() * 2);
        baos.write(extLength >> 8);
        baos.write(extLength & 0xFF);

        baos.write(versions.size() * 2);

        for (int version : versions) {
            baos.write(version >> 8);
            baos.write(version & 0xFF);
        }

        return baos.toByteArray();
    }

    /**
     * 构造GREASE扩展
     */
    private static byte[] buildGreaseExtension(int greaseValue) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        baos.write(greaseValue >> 8);
        baos.write(greaseValue & 0xFF);
        baos.write(0x00); // Length high
        baos.write(0x00); // Length low

        return baos.toByteArray();
    }

    /**
     * 解析Server Hello响应
     *
     * @param responseBytes 服务器响应字节
     * @return ServerHelloResult包含解析结果
     */
    public static ServerHelloResult parseServerHello(byte[] responseBytes) {
        if (responseBytes == null || responseBytes.length < 6) {
            return new ServerHelloResult();
        }

        try {
            ByteBuffer buffer = ByteBuffer.wrap(responseBytes);

            // TLS Record Layer
            int contentType = buffer.get() & 0xFF;
            if (contentType != 0x16) { // Not a handshake
                return new ServerHelloResult();
            }

            int recordVersion = buffer.getShort() & 0xFFFF;
            int recordLength = buffer.getShort() & 0xFFFF;

            // Handshake Protocol
            if (buffer.remaining() < 4) {
                return new ServerHelloResult();
            }

            int handshakeType = buffer.get() & 0xFF;
            if (handshakeType != 0x02) { // Not Server Hello
                return new ServerHelloResult();
            }

            // Handshake length (3 bytes)
            int handshakeLength = ((buffer.get() & 0xFF) << 16)
                                | ((buffer.get() & 0xFF) << 8)
                                | (buffer.get() & 0xFF);

            if (buffer.remaining() < handshakeLength) {
                return new ServerHelloResult();
            }

            // Server Hello
            int serverVersion = buffer.getShort() & 0xFFFF;

            // Random (32 bytes)
            byte[] serverRandom = new byte[32];
            buffer.get(serverRandom);

            // Session ID
            int sessionIdLength = buffer.get() & 0xFF;
            if (sessionIdLength > 0 && buffer.remaining() >= sessionIdLength) {
                buffer.position(buffer.position() + sessionIdLength);
            }

            // Cipher Suite
            int cipherSuite = buffer.getShort() & 0xFFFF;

            // Compression Method
            int compressionMethod = buffer.get() & 0xFF;

            // Extensions (optional)
            List<Integer> extensions = new ArrayList<>();
            String alpnProtocol = null;

            if (buffer.remaining() >= 2) {
                int extensionsLength = buffer.getShort() & 0xFFFF;

                while (buffer.remaining() >= 4 && extensionsLength > 0) {
                    int extType = buffer.getShort() & 0xFFFF;
                    int extLength = buffer.getShort() & 0xFFFF;

                    extensions.add(extType);

                    if (extType == Extension.ALPN && extLength > 0 && buffer.remaining() >= extLength) {
                        // Parse ALPN protocol
                        int alpnListLength = buffer.getShort() & 0xFFFF;
                        if (alpnListLength > 0 && buffer.remaining() >= alpnListLength) {
                            int protocolLength = buffer.get() & 0xFF;
                            if (protocolLength > 0 && buffer.remaining() >= protocolLength) {
                                byte[] protocolBytes = new byte[protocolLength];
                                buffer.get(protocolBytes);
                                alpnProtocol = new String(protocolBytes, "UTF-8");
                            }
                        }
                    } else if (extLength > 0 && buffer.remaining() >= extLength) {
                        buffer.position(buffer.position() + extLength);
                    }

                    extensionsLength -= (4 + extLength);
                }
            }

            ServerHelloResult result = new ServerHelloResult();
            result.success = true;
            result.serverVersion = serverVersion;
            result.cipherSuite = cipherSuite;
            result.extensions = extensions;
            result.alpnProtocol = alpnProtocol;

            return result;

        } catch (Exception e) {
            return new ServerHelloResult();
        }
    }

    /**
     * Server Hello解析结果
     */
    public static class ServerHelloResult {
        public boolean success = false;
        public int serverVersion = 0;
        public int cipherSuite = 0;
        public List<Integer> extensions = new ArrayList<>();
        public String alpnProtocol = null;

        public String getVersionString() {
            switch (serverVersion) {
                case 0x0300: return "a"; // SSL 3.0
                case 0x0301: return "b"; // TLS 1.0
                case 0x0302: return "c"; // TLS 1.1
                case 0x0303: return "d"; // TLS 1.2
                case 0x0304: return "e"; // TLS 1.3
                default: return "0";
            }
        }

        public String getCipherHex() {
            if (!success) {
                return "000";
            }
            // JARM cipher index (simplified - matches cipher to index in ALL_CIPHER_SUITES)
            // This is a simplified version; real JARM requires exact cipher index lookup
            return String.format("%03x", cipherSuite & 0xFFFF);
        }
    }
}
