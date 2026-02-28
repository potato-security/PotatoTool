package com.potato.potatotool.content.redTeam.vulnScanner.http;

/**
 * JARM探测配置类
 * 定义10种TLS Client Hello探测的参数配置
 *
 * JARM通过发送10个不同配置的TLS Client Hello来生成指纹
 * 每个探测使用不同的TLS版本、密码套件顺序、扩展配置等
 */
public class JarmProbe {

    /**
     * 密码套件排序模式
     */
    public enum CipherOrder {
        FORWARD,      // 正序
        REVERSE,      // 逆序
        TOP_HALF,     // 前半部分逆序
        BOTTOM_HALF,  // 后半部分
        MIDDLE_OUT    // 从中间向两端
    }

    /**
     * TLS版本
     */
    public enum TlsVersion {
        TLS_1_0(0x0301),
        TLS_1_1(0x0302),
        TLS_1_2(0x0303),
        TLS_1_3(0x0304);

        public final int value;

        TlsVersion(int value) {
            this.value = value;
        }
    }

    /**
     * 扩展排序模式
     */
    public enum ExtensionOrder {
        FORWARD,  // 正序
        REVERSE   // 逆序
    }

    // TLS版本支持类型
    public enum VersionSupport {
        NO_SUPPORT,      // 不支持
        TLS_1_2_SUPPORT, // 支持TLS 1.2
        TLS_1_3_SUPPORT  // 支持TLS 1.3
    }

    // ALPN协议类型
    public enum AlpnType {
        STANDARD,  // 标准ALPN (h2, http/1.1)
        RARE       // 罕见ALPN用于测试
    }

    // JARM使用的完整密码套件列表（70个）
    public static final int[] ALL_CIPHER_SUITES = {
        // Legacy SSL/TLS
        0x0004, 0x0005, 0x0007, 0x000a, 0x002f, 0x0033, 0x0035, 0x0039,
        0x003c, 0x003d, 0x0041, 0x0067, 0x006b, 0x0084, 0x0088, 0x009c,
        0x009d, 0x009e, 0x009f, 0x00a2, 0x00a3, 0x1301, 0x1302, 0x1303,

        // TLS 1.2 cipher suites
        0xc007, 0xc009, 0xc00a, 0xc011, 0xc012, 0xc013, 0xc014, 0xc023,
        0xc024, 0xc027, 0xc028, 0xc02b, 0xc02c, 0xc02f, 0xc030, 0xc060,
        0xc061, 0xc072, 0xc073, 0xc076, 0xc077, 0xcca8, 0xcca9, 0xccac,

        // ECDHE suites
        0xc008, 0xc016, 0xc02e, 0xc074, 0xc075, 0xccaa, 0xccab, 0x003e,
        0x003f, 0x0040, 0x0045, 0x00a4, 0x00a5, 0x00a8, 0x00a9, 0x00aa,
        0x00ab, 0x00ac, 0x00ad, 0x00ae, 0x00af,

        // ChaCha20 and TLS 1.3
        0xcc13, 0xcc14, 0x1304, 0x1305
    };

    // 不包含TLS 1.3密码套件的列表
    public static final int[] NO_TLS_1_3_CIPHER_SUITES = {
        0x0004, 0x0005, 0x0007, 0x000a, 0x002f, 0x0033, 0x0035, 0x0039,
        0x003c, 0x003d, 0x0041, 0x0067, 0x006b, 0x0084, 0x0088, 0x009c,
        0x009d, 0x009e, 0x009f, 0x00a2, 0x00a3, 0xc007, 0xc009, 0xc00a,
        0xc011, 0xc012, 0xc013, 0xc014, 0xc023, 0xc024, 0xc027, 0xc028,
        0xc02b, 0xc02c, 0xc02f, 0xc030, 0xc060, 0xc061, 0xc072, 0xc073,
        0xc076, 0xc077, 0xcca8, 0xcca9, 0xccac, 0xc008, 0xc016, 0xc02e,
        0xc074, 0xc075, 0xccaa, 0xccab, 0x003e, 0x003f, 0x0040, 0x0045,
        0x00a4, 0x00a5, 0x00a8, 0x00a9, 0x00aa, 0x00ab, 0x00ac, 0x00ad,
        0x00ae, 0x00af, 0xcc13, 0xcc14
    };

    // 探测配置
    private final int probeNumber;              // 探测编号 (1-10)
    private final TlsVersion tlsVersion;        // TLS版本
    private final int[] cipherSuites;           // 密码套件列表
    private final CipherOrder cipherOrder;      // 密码套件排序
    private final boolean useGrease;            // 是否使用GREASE
    private final AlpnType alpnType;            // ALPN类型
    private final VersionSupport versionSupport; // 版本支持
    private final ExtensionOrder extensionOrder; // 扩展排序

    public JarmProbe(int probeNumber, TlsVersion tlsVersion, int[] cipherSuites,
                     CipherOrder cipherOrder, boolean useGrease, AlpnType alpnType,
                     VersionSupport versionSupport, ExtensionOrder extensionOrder) {
        this.probeNumber = probeNumber;
        this.tlsVersion = tlsVersion;
        this.cipherSuites = cipherSuites;
        this.cipherOrder = cipherOrder;
        this.useGrease = useGrease;
        this.alpnType = alpnType;
        this.versionSupport = versionSupport;
        this.extensionOrder = extensionOrder;
    }

    /**
     * 获取排序后的密码套件
     */
    public int[] getOrderedCipherSuites() {
        int[] ordered = cipherSuites.clone();

        switch (cipherOrder) {
            case REVERSE:
                // 完全逆序
                reverse(ordered, 0, ordered.length);
                break;

            case TOP_HALF:
                // 前半部分逆序
                int mid = ordered.length / 2;
                reverse(ordered, 0, mid);
                break;

            case BOTTOM_HALF:
                // 只保留后半部分
                int half = ordered.length / 2;
                int[] bottom = new int[ordered.length - half];
                System.arraycopy(ordered, half, bottom, 0, bottom.length);
                return bottom;

            case MIDDLE_OUT:
                // 从中间向两端展开
                return middleOut(ordered);

            case FORWARD:
            default:
                // 保持原序
                break;
        }

        return ordered;
    }

    /**
     * 数组逆序
     */
    private void reverse(int[] array, int start, int end) {
        for (int i = start, j = end - 1; i < j; i++, j--) {
            int temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }

    /**
     * 从中间向两端排序
     */
    private int[] middleOut(int[] array) {
        int[] result = new int[array.length];
        int mid = array.length / 2;
        int left = mid - 1;
        int right = mid;
        int index = 0;

        while (left >= 0 || right < array.length) {
            if (right < array.length) {
                result[index++] = array[right++];
            }
            if (left >= 0) {
                result[index++] = array[left--];
            }
        }

        return result;
    }

    // Getters
    public int getProbeNumber() { return probeNumber; }
    public TlsVersion getTlsVersion() { return tlsVersion; }
    public int[] getCipherSuites() { return cipherSuites; }
    public CipherOrder getCipherOrder() { return cipherOrder; }
    public boolean isUseGrease() { return useGrease; }
    public AlpnType getAlpnType() { return alpnType; }
    public VersionSupport getVersionSupport() { return versionSupport; }
    public ExtensionOrder getExtensionOrder() { return extensionOrder; }

    /**
     * 创建10个标准JARM探测配置
     */
    public static JarmProbe[] createStandardProbes() {
        return new JarmProbe[] {
            // Probe 1: TLS 1.2, ALL, FORWARD, NO GREASE, STANDARD ALPN, 1.2 SUPPORT, REVERSE EXT
            new JarmProbe(1, TlsVersion.TLS_1_2, ALL_CIPHER_SUITES, CipherOrder.FORWARD,
                         false, AlpnType.STANDARD, VersionSupport.TLS_1_2_SUPPORT, ExtensionOrder.REVERSE),

            // Probe 2: TLS 1.2, ALL, REVERSE, NO GREASE, STANDARD ALPN, 1.2 SUPPORT, FORWARD EXT
            new JarmProbe(2, TlsVersion.TLS_1_2, ALL_CIPHER_SUITES, CipherOrder.REVERSE,
                         false, AlpnType.STANDARD, VersionSupport.TLS_1_2_SUPPORT, ExtensionOrder.FORWARD),

            // Probe 3: TLS 1.2, ALL, TOP_HALF, NO GREASE, STANDARD ALPN, NO SUPPORT, FORWARD EXT
            new JarmProbe(3, TlsVersion.TLS_1_2, ALL_CIPHER_SUITES, CipherOrder.TOP_HALF,
                         false, AlpnType.STANDARD, VersionSupport.NO_SUPPORT, ExtensionOrder.FORWARD),

            // Probe 4: TLS 1.2, ALL, BOTTOM_HALF, NO GREASE, RARE ALPN, NO SUPPORT, FORWARD EXT
            new JarmProbe(4, TlsVersion.TLS_1_2, ALL_CIPHER_SUITES, CipherOrder.BOTTOM_HALF,
                         false, AlpnType.RARE, VersionSupport.NO_SUPPORT, ExtensionOrder.FORWARD),

            // Probe 5: TLS 1.2, ALL, MIDDLE_OUT, GREASE, RARE ALPN, NO SUPPORT, REVERSE EXT
            new JarmProbe(5, TlsVersion.TLS_1_2, ALL_CIPHER_SUITES, CipherOrder.MIDDLE_OUT,
                         true, AlpnType.RARE, VersionSupport.NO_SUPPORT, ExtensionOrder.REVERSE),

            // Probe 6: TLS 1.1, ALL, FORWARD, NO GREASE, STANDARD ALPN, NO SUPPORT, FORWARD EXT
            new JarmProbe(6, TlsVersion.TLS_1_1, ALL_CIPHER_SUITES, CipherOrder.FORWARD,
                         false, AlpnType.STANDARD, VersionSupport.NO_SUPPORT, ExtensionOrder.FORWARD),

            // Probe 7: TLS 1.3, ALL, FORWARD, NO GREASE, STANDARD ALPN, 1.3 SUPPORT, REVERSE EXT
            new JarmProbe(7, TlsVersion.TLS_1_3, ALL_CIPHER_SUITES, CipherOrder.FORWARD,
                         false, AlpnType.STANDARD, VersionSupport.TLS_1_3_SUPPORT, ExtensionOrder.REVERSE),

            // Probe 8: TLS 1.3, ALL, REVERSE, NO GREASE, STANDARD ALPN, 1.3 SUPPORT, FORWARD EXT
            new JarmProbe(8, TlsVersion.TLS_1_3, ALL_CIPHER_SUITES, CipherOrder.REVERSE,
                         false, AlpnType.STANDARD, VersionSupport.TLS_1_3_SUPPORT, ExtensionOrder.FORWARD),

            // Probe 9: TLS 1.3, NO1.3, FORWARD, NO GREASE, STANDARD ALPN, 1.3 SUPPORT, FORWARD EXT
            new JarmProbe(9, TlsVersion.TLS_1_3, NO_TLS_1_3_CIPHER_SUITES, CipherOrder.FORWARD,
                         false, AlpnType.STANDARD, VersionSupport.TLS_1_3_SUPPORT, ExtensionOrder.FORWARD),

            // Probe 10: TLS 1.3, ALL, MIDDLE_OUT, GREASE, STANDARD ALPN, 1.3 SUPPORT, REVERSE EXT
            new JarmProbe(10, TlsVersion.TLS_1_3, ALL_CIPHER_SUITES, CipherOrder.MIDDLE_OUT,
                         true, AlpnType.STANDARD, VersionSupport.TLS_1_3_SUPPORT, ExtensionOrder.REVERSE)
        };
    }
}
