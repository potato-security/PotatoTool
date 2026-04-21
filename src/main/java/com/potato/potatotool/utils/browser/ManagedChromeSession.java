package com.potato.potatotool.utils.browser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Owns a temporary direct Chrome/Chromium process with a DevTools port.
 */
public class ManagedChromeSession implements AutoCloseable {
    private final Process process;
    private final File userDataDir;
    private final int devToolsPort;

    private ManagedChromeSession(Process process, File userDataDir, int devToolsPort) {
        this.process = process;
        this.userDataDir = userDataDir;
        this.devToolsPort = devToolsPort;
    }

    public static ManagedChromeSession start(String browserPath, String initialUrl, boolean directMode, long readyTimeoutMillis) throws IOException {
        if (browserPath == null || browserPath.trim().isEmpty()) {
            throw new IOException("Browser path is empty");
        }

        int port = allocateLocalPort();
        File userDataDir = Files.createTempDirectory("potatotool-chrome-").toFile();

        List<String> command = new ArrayList<String>();
        command.add(browserPath);
        command.add("--remote-debugging-port=" + port);
        command.add("--user-data-dir=" + userDataDir.getAbsolutePath());
        command.add("--no-first-run");
        command.add("--no-default-browser-check");
        command.add("--disable-background-networking");
        if (directMode) {
            command.add("--no-proxy-server");
            command.add("--proxy-server=direct://");
            command.add("--proxy-bypass-list=*");
        }
        if (initialUrl != null && !initialUrl.trim().isEmpty()) {
            command.add(initialUrl);
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        drainProcessOutput(process);

        ManagedChromeSession session = new ManagedChromeSession(process, userDataDir, port);
        if (!session.waitUntilReady(readyTimeoutMillis)) {
            session.close();
            throw new IOException("Chrome DevTools port is not ready: " + port);
        }
        return session;
    }

    public int getDevToolsPort() {
        return devToolsPort;
    }

    public boolean waitUntilReady(long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (!process.isAlive()) {
                return false;
            }
            if (isDevToolsReady()) {
                return true;
            }
            try {
                Thread.sleep(200L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private boolean isDevToolsReady() {
        HttpURLConnection connection = null;
        try {
            URL url = new URL("http://127.0.0.1:" + devToolsPort + "/json/version");
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(500);
            connection.setReadTimeout(500);
            connection.setRequestMethod("GET");
            return connection.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    public void close() {
        try {
            process.destroy();
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (Exception ignored) {
            process.destroyForcibly();
        }
        deleteRecursively(userDataDir);
    }

    private static int allocateLocalPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))) {
            return socket.getLocalPort();
        }
    }

    private static void drainProcessOutput(final Process process) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = null;
                try {
                    inputStream = process.getInputStream();
                    byte[] buffer = new byte[1024];
                    while (inputStream.read(buffer) != -1) {
                        // Drain Chrome logs so the child process cannot block on a full pipe.
                    }
                } catch (Exception ignored) {
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        }, "potatotool-managed-chrome-drain");
        thread.setDaemon(true);
        thread.start();
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        try {
            file.delete();
        } catch (Exception ignored) {
        }
    }
}
