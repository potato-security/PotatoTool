package com.potato.potatotool.utils.browser;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

/**
 * Thin Chrome DevTools Protocol transport over a WebSocket connection.
 */
public final class CdpClient implements AutoCloseable {
    private final Socket socket;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final int socketReadTimeoutMillis;
    private int requestId;

    private CdpClient(Socket socket, InputStream inputStream, OutputStream outputStream, int socketReadTimeoutMillis) {
        this.socket = socket;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.socketReadTimeoutMillis = socketReadTimeoutMillis;
    }

    public static CdpClient connect(String webSocketDebuggerUrl, int connectTimeoutMillis, int readTimeoutMillis)
            throws CdpException {
        if (webSocketDebuggerUrl == null || webSocketDebuggerUrl.trim().isEmpty()) {
            throw new CdpException(CdpErrorType.INVALID_RESPONSE, "DevTools websocket URL is empty");
        }

        Socket socket = new Socket();
        try {
            String httpUrl = webSocketDebuggerUrl.replace("ws://", "http://");
            if (httpUrl.startsWith("wss://")) {
                throw new CdpException(CdpErrorType.INVALID_RESPONSE,
                        "Unsupported secure DevTools websocket URL: " + webSocketDebuggerUrl);
            }

            URL url = new URL(httpUrl);
            String host = url.getHost();
            int port = url.getPort() > 0 ? url.getPort() : url.getDefaultPort();
            String path = url.getFile();

            socket.connect(new InetSocketAddress(host, port), connectTimeoutMillis);
            socket.setSoTimeout(readTimeoutMillis);

            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();
            String secKey = Base64.getEncoder()
                    .encodeToString(UUID.randomUUID().toString().getBytes("UTF-8"));
            String request = "GET " + path + " HTTP/1.1\r\n"
                    + "Host: " + host + ":" + port + "\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Sec-WebSocket-Key: " + secKey + "\r\n"
                    + "Sec-WebSocket-Version: 13\r\n\r\n";
            outputStream.write(request.getBytes("UTF-8"));
            outputStream.flush();

            String response = readHttpHeaders(inputStream);
            if (!response.contains("101")) {
                closeQuietly(socket);
                throw new CdpException(CdpErrorType.WEBSOCKET_HANDSHAKE_FAILED,
                        "DevTools websocket handshake failed: " + response.replace("\r", " ").replace("\n", " ").trim());
            }

            return new CdpClient(socket, inputStream, outputStream, readTimeoutMillis);
        } catch (CdpException e) {
            closeQuietly(socket);
            throw e;
        } catch (Exception e) {
            closeQuietly(socket);
            throw new CdpException(CdpErrorType.CONNECTION_CLOSED,
                    "Failed to connect to DevTools websocket: " + webSocketDebuggerUrl, e);
        }
    }

    public synchronized JsonObject request(String method, JsonObject params, int timeoutMillis) throws CdpException {
        int id = ++requestId;
        JsonObject message = new JsonObject();
        message.addProperty("id", id);
        message.addProperty("method", method);
        if (params != null) {
            message.add("params", params);
        }
        sendText(message.toString());

        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                break;
            }

            try {
                socket.setSoTimeout((int) Math.max(1L, Math.min(remaining, socketReadTimeoutMillis)));
                JsonObject frame = readFrame();
                if (frame == null || !frame.has("id") || frame.get("id").getAsInt() != id) {
                    continue;
                }
                if (frame.has("error") && frame.get("error").isJsonObject()) {
                    throw new CdpException(CdpErrorType.PROTOCOL_ERROR,
                            "DevTools request failed: " + method + " - "
                                    + readJsonString(frame.getAsJsonObject("error"), "message"));
                }
                return frame;
            } catch (SocketTimeoutException ignored) {
            } catch (CdpException e) {
                throw e;
            } catch (Exception e) {
                throw new CdpException(CdpErrorType.CONNECTION_CLOSED,
                        "Failed while waiting for DevTools response: " + method, e);
            }
        }

        throw CdpException.timeout("DevTools request timeout: " + method);
    }

    private void sendText(String text) throws CdpException {
        try {
            byte[] payload = text.getBytes("UTF-8");
            ByteArrayOutputStream frame = new ByteArrayOutputStream();
            frame.write(0x81);
            writeLength(frame, payload.length, true);

            byte[] mask = new byte[4];
            new Random().nextBytes(mask);
            frame.write(mask);
            for (int i = 0; i < payload.length; i++) {
                frame.write(payload[i] ^ mask[i % 4]);
            }

            outputStream.write(frame.toByteArray());
            outputStream.flush();
        } catch (Exception e) {
            throw new CdpException(CdpErrorType.CONNECTION_CLOSED, "Failed to write DevTools websocket frame", e);
        }
    }

    private JsonObject readFrame() throws Exception {
        int first = inputStream.read();
        if (first < 0) {
            throw new CdpException(CdpErrorType.BROWSER_CLOSED, "DevTools websocket closed by browser");
        }

        int second = inputStream.read();
        if (second < 0) {
            throw new CdpException(CdpErrorType.BROWSER_CLOSED, "DevTools websocket closed by browser");
        }

        int opcode = first & 0x0F;
        boolean masked = (second & 0x80) != 0;
        long length = second & 0x7F;
        if (length == 126) {
            length = ((inputStream.read() & 0xFF) << 8) | (inputStream.read() & 0xFF);
        } else if (length == 127) {
            length = 0;
            for (int i = 0; i < 8; i++) {
                length = (length << 8) | (inputStream.read() & 0xFF);
            }
        }

        byte[] mask = masked ? readBytes(4) : null;
        byte[] payload = readBytes((int) length);
        if (masked && mask != null) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] = (byte) (payload[i] ^ mask[i % 4]);
            }
        }

        if (opcode == 0x8) {
            throw new CdpException(CdpErrorType.BROWSER_CLOSED, "DevTools websocket closed by browser");
        }
        if (opcode != 0x1) {
            return null;
        }

        try {
            return new JsonParser().parse(new String(payload, "UTF-8")).getAsJsonObject();
        } catch (Exception e) {
            throw new CdpException(CdpErrorType.INVALID_RESPONSE, "Invalid DevTools JSON frame", e);
        }
    }

    private byte[] readBytes(int length) throws Exception {
        byte[] buffer = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = inputStream.read(buffer, offset, length - offset);
            if (read < 0) {
                throw new CdpException(CdpErrorType.CONNECTION_CLOSED, "Unexpected EOF while reading DevTools frame");
            }
            offset += read;
        }
        return buffer;
    }

    private void writeLength(ByteArrayOutputStream frame, int length, boolean masked) {
        int prefix = masked ? 0x80 : 0x00;
        if (length < 126) {
            frame.write(prefix | length);
            return;
        }
        if (length <= 0xFFFF) {
            frame.write(prefix | 126);
            frame.write((length >> 8) & 0xFF);
            frame.write(length & 0xFF);
            return;
        }
        frame.write(prefix | 127);
        for (int i = 7; i >= 0; i--) {
            frame.write((length >> (8 * i)) & 0xFF);
        }
    }

    @Override
    public void close() {
        closeQuietly(socket);
    }

    private static String readHttpHeaders(InputStream inputStream) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int matched = 0;
        int[] expected = new int[]{'\r', '\n', '\r', '\n'};
        while (matched < expected.length) {
            int ch = inputStream.read();
            if (ch < 0) {
                break;
            }
            baos.write(ch);
            if (ch == expected[matched]) {
                matched++;
            } else {
                matched = ch == expected[0] ? 1 : 0;
            }
        }
        return new String(baos.toByteArray(), "UTF-8");
    }

    private static void closeQuietly(Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (Exception ignored) {
        }
    }

    private static String readJsonString(JsonObject jsonObject, String key) {
        if (jsonObject == null || key == null || !jsonObject.has(key) || jsonObject.get(key).isJsonNull()) {
            return "";
        }
        try {
            return jsonObject.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }
}
