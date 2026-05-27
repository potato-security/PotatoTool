package com.potato.potatotool.content.redTeam.portScanner.port;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class ServiceCatalog {
    private static final Map<Integer, String> SERVICES = new HashMap<>();

    static {
        put(21, "ftp");
        put(22, "ssh");
        put(23, "telnet");
        put(25, "smtp");
        put(53, "dns");
        put(80, "http");
        put(81, "http");
        put(88, "kerberos");
        put(110, "pop3");
        put(111, "rpcbind");
        put(135, "msrpc");
        put(139, "netbios");
        put(143, "imap");
        put(389, "ldap");
        put(443, "https");
        put(445, "smb");
        put(465, "smtps");
        put(587, "smtp-submission");
        put(636, "ldaps");
        put(873, "rsync");
        put(989, "ftps-data");
        put(990, "ftps");
        put(993, "imaps");
        put(995, "pop3s");
        put(1099, "java-rmi");
        put(1433, "mssql");
        put(1521, "oracle");
        put(2049, "nfs");
        put(2181, "zookeeper");
        put(2375, "docker");
        put(2376, "docker-tls");
        put(3000, "http-alt");
        put(3306, "mysql");
        put(3389, "rdp");
        put(5000, "http-alt");
        put(5001, "http-alt");
        put(5432, "postgresql");
        put(5601, "kibana");
        put(5672, "amqp");
        put(5900, "vnc");
        put(6379, "redis");
        put(7001, "http-alt");
        put(7002, "https-alt");
        put(8000, "http-alt");
        put(8008, "http-alt");
        put(8080, "http-proxy");
        put(8081, "http-alt");
        put(8082, "http-alt");
        put(8088, "http-alt");
        put(8090, "http-alt");
        put(8443, "https-alt");
        put(8888, "http-alt");
        put(9000, "http-alt");
        put(9001, "http-alt");
        put(9090, "http-alt");
        put(9092, "kafka");
        put(9200, "elasticsearch");
        put(9300, "elasticsearch");
        put(9443, "https-alt");
        put(10000, "http-alt");
        put(11211, "memcached");
        put(15672, "http-alt");
        put(27017, "mongodb");
        put(50070, "http-alt");
        put(50470, "https-alt");
        put(61616, "activemq");
    }

    private ServiceCatalog() {
    }

    public static String infer(int port, String banner, boolean tls) {
        return infer(port, null, banner, tls);
    }

    public static String inferFromBytes(int port, byte[] data, boolean tls) {
        String banner = data == null ? "" : normalizeBanner(new String(data, StandardCharsets.ISO_8859_1));
        return infer(port, data, banner, tls);
    }

    public static String infer(int port, byte[] data, String banner, boolean tls) {
        String byBytes = inferByBytes(data);
        if (byBytes != null) {
            return byBytes;
        }
        if (banner != null) {
            String lower = banner.toLowerCase();
            if (lower.contains("ssh-")) return "ssh";
            if (lower.contains("http/")) return tls ? "https" : "http";
            if (lower.contains("ftp")) return "ftp";
            if (lower.contains("smtp")) return "smtp";
            if (lower.contains("mysql") || lower.contains("mariadb") || lower.contains("mysql_native_password") || lower.contains("caching_sha2_password")) return "mysql";
            if (lower.contains("redis") || lower.startsWith("+pong") || lower.startsWith("-noauth")) return "redis";
            if (lower.contains("postgresql") || lower.contains("postgres")) return "postgresql";
            if (lower.contains("mongodb")) return "mongodb";
            if (lower.contains("memcached")) return "memcached";
            if (lower.contains("smb") || lower.contains("samba")) return "smb";
            if (lower.startsWith("rfb ")) return "vnc";
        }
        String service = SERVICES.get(port);
        if (service != null) {
            return service;
        }
        return tls ? "tls" : "unknown";
    }

    public static boolean isLikelyHttp(int port) {
        return port == 80 || port == 81 || port == 3000 || port == 5000 || port == 5001
                || port == 7001 || port == 8000 || port == 8008 || port == 8080 || port == 8081
                || port == 8082 || port == 8088 || port == 8090 || port == 8888 || port == 9000
                || port == 9001 || port == 9090 || port == 10000 || port == 15672 || port == 50070;
    }

    public static boolean isLikelyHttps(int port) {
        return port == 443 || port == 7002 || port == 8443 || port == 9443 || port == 10443 || port == 50470;
    }

    public static boolean isLikelyTls(int port) {
        return isLikelyHttps(port) || port == 465 || port == 636 || port == 989 || port == 990
                || port == 993 || port == 995 || port == 2376 || port == 5986;
    }

    private static void put(int port, String service) {
        SERVICES.put(port, service);
    }

    private static String inferByBytes(byte[] data) {
        if (data == null || data.length < 4) {
            return null;
        }
        if (looksLikeMysqlHandshake(data)) {
            return "mysql";
        }
        if (looksLikeSmb(data)) {
            return "smb";
        }
        return null;
    }

    private static boolean looksLikeMysqlHandshake(byte[] data) {
        if (data.length < 5) {
            return false;
        }
        int packetLength = (data[0] & 0xff) | ((data[1] & 0xff) << 8) | ((data[2] & 0xff) << 16);
        return packetLength > 0 && data[3] == 0 && data[4] == 10;
    }

    private static boolean looksLikeSmb(byte[] data) {
        return hasSmbSignatureAt(data, 0) || hasSmbSignatureAt(data, 4);
    }

    private static boolean hasSmbSignatureAt(byte[] data, int offset) {
        return data.length >= offset + 4
                && (data[offset] == (byte) 0xff || data[offset] == (byte) 0xfe)
                && data[offset + 1] == 'S'
                && data[offset + 2] == 'M'
                && data[offset + 3] == 'B';
    }

    private static String normalizeBanner(String banner) {
        return banner.replace('\r', ' ').replace('\n', ' ').replace('\0', ' ').trim();
    }
}
