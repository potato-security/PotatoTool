package com.potato.potatotool.content.redTeam.portScanner.nio;

import com.potato.potatotool.content.redTeam.portScanner.model.PortResult;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanConfig;
import com.potato.potatotool.content.redTeam.portScanner.model.PortState;
import com.potato.potatotool.content.redTeam.portScanner.model.ScanTask;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SelectorWorkerIntegrationTest {
    @Test
    public void run_shouldDetectOpenAndClosedLocalPorts() throws Exception {
        ServerSocket server = new ServerSocket(0);
        Thread acceptThread = new Thread(() -> {
            try {
                Socket socket = server.accept();
                socket.close();
            } catch (Exception ignored) {
            }
        });
        acceptThread.setDaemon(true);
        acceptThread.start();

        int openPort = server.getLocalPort();
        int closedPort = findClosedPort();
        PortScanConfig config = new PortScanConfig();
        config.setBatchSize(16);
        config.setConnectTimeoutMs(500);
        config.setPublicMode(false);
        List<ScanTask> tasks = Arrays.asList(new ScanTask("127.0.0.1", openPort), new ScanTask("127.0.0.1", closedPort));
        CountDownLatch finished = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicReference<PortResult> openResult = new AtomicReference<>();
        AtomicReference<PortState> closedState = new AtomicReference<>();
        AtomicInteger errors = new AtomicInteger();

        SelectorWorker worker = new SelectorWorker(tasks.iterator(), config, new SelectorWorker.ScanCallback() {
            @Override
            public void onOpen(PortResult result) {
                openResult.set(result);
            }

            @Override
            public void onClosedOrFiltered(ScanTask task, PortState state) {
                if (task.getPort() == closedPort) {
                    closedState.set(state);
                }
            }

            @Override
            public void onTaskDone() {
                done.countDown();
            }

            @Override
            public void onError(ScanTask task, Throwable throwable) {
                errors.incrementAndGet();
            }

            @Override
            public void onWorkerFinished() {
                finished.countDown();
            }
        });

        Thread workerThread = new Thread(worker);
        workerThread.start();

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertTrue(finished.await(5, TimeUnit.SECONDS));
        assertNotNull(openResult.get());
        assertEquals(openPort, openResult.get().getPort());
        assertEquals(PortState.CLOSED, closedState.get());
        assertEquals(0, errors.get());
        server.close();
    }

    @Test
    public void run_shouldDetectFilteredUnreachableAddress() throws Exception {
        PortScanConfig config = new PortScanConfig();
        config.setBatchSize(4);
        config.setConnectTimeoutMs(300);
        config.setPublicMode(false);
        ScanTask task = new ScanTask("2001:db8::1", 81);
        CountDownLatch finished = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<PortState> state = new AtomicReference<>();
        AtomicInteger errors = new AtomicInteger();

        SelectorWorker worker = new SelectorWorker(Arrays.asList(task).iterator(), config, new SelectorWorker.ScanCallback() {
            @Override
            public void onOpen(PortResult result) {
            }

            @Override
            public void onClosedOrFiltered(ScanTask task, PortState portState) {
                state.set(portState);
            }

            @Override
            public void onTaskDone() {
                done.countDown();
            }

            @Override
            public void onError(ScanTask task, Throwable throwable) {
                errors.incrementAndGet();
            }

            @Override
            public void onWorkerFinished() {
                finished.countDown();
            }
        });

        Thread workerThread = new Thread(worker);
        workerThread.start();

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertTrue(finished.await(5, TimeUnit.SECONDS));
        assertEquals(PortState.FILTERED, state.get());
        assertEquals(0, errors.get());
    }

    private int findClosedPort() throws Exception {
        ServerSocket socket = new ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        return port;
    }
}
