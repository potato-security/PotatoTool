package com.potato.potatotool.content.redTeam.portScanner.port;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServiceCatalogTest {
    @Test
    public void infer_shouldRecognizeTypicalServiceBanners() {
        assertEquals("http", ServiceCatalog.infer(18080, "HTTP/1.1 200 OK Server: nginx/1.18", false));
        assertEquals("ssh", ServiceCatalog.infer(10022, "SSH-2.0-OpenSSH_8.9", false));
        assertEquals("mysql", ServiceCatalog.infer(13306, "5.7.31-log MySQL Community Server", false));
        assertEquals("redis", ServiceCatalog.infer(16379, "-NOAUTH Authentication required. redis_version:6.2.0", false));
        assertEquals("redis", ServiceCatalog.infer(16379, "+PONG", false));
        assertEquals("vnc", ServiceCatalog.infer(15900, "RFB 003.008", false));
    }

    @Test
    public void infer_shouldFallbackToPortCatalogWhenBannerIsEmpty() {
        assertEquals("http", ServiceCatalog.infer(80, "", false));
        assertEquals("mysql", ServiceCatalog.infer(3306, null, false));
        assertEquals("redis", ServiceCatalog.infer(6379, null, false));
    }

    @Test
    public void inferFromBytes_shouldRecognizeBinaryServiceHandshakes() {
        assertEquals("mysql", ServiceCatalog.inferFromBytes(13306, new byte[]{0x4a, 0x00, 0x00, 0x00, 0x0a, 0x35, 0x2e, 0x37}, false));
        assertEquals("smb", ServiceCatalog.inferFromBytes(1445, new byte[]{0x00, 0x00, 0x00, 0x20, (byte) 0xff, 'S', 'M', 'B'}, false));
        assertEquals("smb", ServiceCatalog.inferFromBytes(1445, new byte[]{0x00, 0x00, 0x00, 0x20, (byte) 0xfe, 'S', 'M', 'B'}, false));
    }
}
