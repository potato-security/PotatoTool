package com.potato.potatotool.content.redTeam.portScanner.core;

import com.potato.potatotool.content.redTeam.portScanner.model.PortResult;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanConfig;
import com.potato.potatotool.content.redTeam.portScanner.port.ServiceCatalog;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ServiceProbe implements Runnable {
    private final PortResult result;
    private final PortScanConfig config;
    private final ProbeCallback callback;

    public ServiceProbe(PortResult result, PortScanConfig config, ProbeCallback callback) {
        this.result = result;
        this.config = config;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            if (ServiceCatalog.isLikelyTls(result.getPort())) {
                probeTls();
            } else {
                probeTcp();
            }
        } catch (Exception ignored) {
            result.setService(ServiceCatalog.infer(result.getPort(), result.getBanner(), result.isTls()));
        } finally {
            callback.onProbeDone(result);
        }
    }

    private void probeTcp() throws Exception {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(result.getHost(), result.getPort()), config.getProbeTimeoutMs());
            socket.setSoTimeout(config.getProbeTimeoutMs());
            if (ServiceCatalog.isLikelyHttp(result.getPort())) {
                writeProbe(socket, "GET / HTTP/1.0\r\nHost: " + result.getHost() + "\r\nUser-Agent: PotatoTool-PortScan\r\n\r\n");
            } else if (result.getPort() == 6379) {
                writeProbe(socket, "*1\r\n$4\r\nPING\r\n");
            }
            byte[] data = readBannerBytes(socket.getInputStream());
            result.setBanner(toBanner(data));
            result.setService(ServiceCatalog.infer(result.getPort(), data, result.getBanner(), false));
        } finally {
            closeQuiet(socket);
        }
    }

    private void probeTls() throws Exception {
        SSLSocket socket = null;
        try {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            socket = (SSLSocket) factory.createSocket();
            socket.connect(new InetSocketAddress(result.getHost(), result.getPort()), config.getProbeTimeoutMs());
            socket.setSoTimeout(config.getProbeTimeoutMs());
            socket.startHandshake();
            result.setTls(true);
            result.setBanner("TLS " + socket.getSession().getProtocol() + " " + socket.getSession().getCipherSuite());
        } finally {
            closeQuiet(socket);
        }
        result.setService(ServiceCatalog.infer(result.getPort(), result.getBanner(), true));
    }

    private byte[] readBannerBytes(InputStream inputStream) throws Exception {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int read = inputStream.read(buffer);
        if (read > 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private String toBanner(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        return new String(data, StandardCharsets.ISO_8859_1).replace('\r', ' ').replace('\n', ' ').replace('\0', ' ').trim();
    }

    private void writeProbe(Socket socket, String probe) throws Exception {
        OutputStream out = socket.getOutputStream();
        out.write(probe.getBytes(StandardCharsets.ISO_8859_1));
        out.flush();
    }

    private void closeQuiet(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    public interface ProbeCallback {
        void onProbeDone(PortResult result);
    }
}
