package com.potato.potatotool.content.redTeam.portScanner.core;

import com.potato.potatotool.content.redTeam.portScanner.model.PortResult;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanConfig;
import com.potato.potatotool.content.redTeam.portScanner.model.PortState;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServiceProbeTest {
    @Test
    public void run_shouldRecognizeTypicalServiceBanners() throws Exception {
        assertService("HTTP/1.1 200 OK\r\nServer: nginx/1.18\r\n\r\n", "http");
        assertService("SSH-2.0-OpenSSH_8.9\r\n", "ssh");
        assertService("5.7.31-log MySQL Community Server\r\n", "mysql");
        assertService("-NOAUTH Authentication required redis_version:6.2.0\r\n", "redis");
        assertService("+PONG\r\n", "redis");
        assertService("RFB 003.008\n", "vnc");
    }

    @Test
    public void run_shouldRecognizeBinaryServiceBanners() throws Exception {
        assertService(new byte[]{0x4a, 0x00, 0x00, 0x00, 0x0a, 0x35, 0x2e, 0x37}, "mysql");
        assertService(new byte[]{0x00, 0x00, 0x00, 0x20, (byte) 0xff, 'S', 'M', 'B'}, "smb");
    }

    private void assertService(String banner, String expectedService) throws Exception {
        ServerSocket server = new ServerSocket(0);
        Thread acceptThread = new Thread(() -> {
            try {
                Socket socket = server.accept();
                OutputStream out = socket.getOutputStream();
                out.write(banner.getBytes(StandardCharsets.ISO_8859_1));
                out.flush();
                socket.close();
            } catch (Exception ignored) {
            }
        });
        acceptThread.setDaemon(true);
        acceptThread.start();

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<PortResult> probed = new AtomicReference<>();
        PortResult result = new PortResult("127.0.0.1", server.getLocalPort(), PortState.OPEN);
        PortScanConfig config = new PortScanConfig();
        config.setProbeTimeoutMs(1000);
        new ServiceProbe(result, config, portResult -> {
            probed.set(portResult);
            done.countDown();
        }).run();

        assertTrue(done.await(1, TimeUnit.SECONDS));
        assertNotNull(probed.get());
        assertEquals(expectedService, probed.get().getService());
        server.close();
    }

    private void assertService(byte[] banner, String expectedService) throws Exception {
        ServerSocket server = new ServerSocket(0);
        Thread acceptThread = new Thread(() -> {
            try {
                Socket socket = server.accept();
                OutputStream out = socket.getOutputStream();
                out.write(banner);
                out.flush();
                socket.close();
            } catch (Exception ignored) {
            }
        });
        acceptThread.setDaemon(true);
        acceptThread.start();

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<PortResult> probed = new AtomicReference<>();
        PortResult result = new PortResult("127.0.0.1", server.getLocalPort(), PortState.OPEN);
        PortScanConfig config = new PortScanConfig();
        config.setProbeTimeoutMs(1000);
        new ServiceProbe(result, config, portResult -> {
            probed.set(portResult);
            done.countDown();
        }).run();

        assertTrue(done.await(1, TimeUnit.SECONDS));
        assertNotNull(probed.get());
        assertEquals(expectedService, probed.get().getService());
        server.close();
    }
}
