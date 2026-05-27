package com.potato.potatotool.content.redTeam.portScanner.nio;

import com.potato.potatotool.content.redTeam.portScanner.model.PortResult;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanConfig;
import com.potato.potatotool.content.redTeam.portScanner.model.PortState;
import com.potato.potatotool.content.redTeam.portScanner.model.ScanTask;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class SelectorWorker implements Runnable {
    private final Iterator<ScanTask> tasks;
    private final PortScanConfig config;
    private final ScanCallback callback;
    private final DeadlineQueue deadlines = new DeadlineQueue();
    private final AdaptiveLimiter limiter;
    private final RateLimiter rateLimiter;
    private volatile boolean running = true;
    private volatile boolean paused;
    private Selector selector;
    private int live;

    public SelectorWorker(Iterator<ScanTask> tasks, PortScanConfig config, ScanCallback callback) {
        this.tasks = tasks;
        this.config = config;
        this.callback = callback;
        int configured = config.getBatchSize();
        int fdBased = AdaptiveLimiter.estimateInitialBatch();
        int initial = Math.max(configured, fdBased);
        int upper = Math.max(initial, 4096);
        this.limiter = new AdaptiveLimiter(initial, 64, upper);
        this.rateLimiter = config.isPublicMode() ? new RateLimiter(config.getPublicRateLimit()) : null;
    }

    @Override
    public void run() {
        try {
            selector = Selector.open();
            while (running) {
                if (!paused) {
                    fillWindow();
                }
                long now = System.currentTimeMillis();
                long delay = Math.min(50L, deadlines.nextDelay(now));
                selector.select(delay);
                processSelectedKeys();
                final long expireNow = System.currentTimeMillis();
                deadlines.expireUntil(expireNow, this::onTimeout);
                if (!tasks.hasNext() && live == 0) {
                    break;
                }
            }
        } catch (IOException e) {
            callback.onError(null, e);
        } finally {
            closeAll();
            callback.onWorkerFinished();
        }
    }

    public void pause() {
        paused = true;
        wakeup();
    }

    public void resume() {
        paused = false;
        wakeup();
    }

    public void stop() {
        running = false;
        wakeup();
    }

    private void wakeup() {
        Selector current = selector;
        if (current != null) {
            current.wakeup();
        }
    }

    private void fillWindow() {
        while (running && !paused && live < limiter.getCurrent() && tasks.hasNext()) {
            ScanTask task = tasks.next();
            if (rateLimiter != null) {
                rateLimiter.acquire();
            }
            tryConnect(task);
        }
    }

    private void tryConnect(ScanTask task) {
        SocketChannel channel = null;
        try {
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            Socket socket = channel.socket();
            socket.setReuseAddress(true);
            try {
                socket.setSoLinger(true, 0);
            } catch (SocketException ignored) {
            }
            TaskAttachment attachment = new TaskAttachment(task, System.currentTimeMillis(), System.currentTimeMillis() + config.getConnectTimeoutMs());
            boolean connected = channel.connect(new InetSocketAddress(task.getHost(), task.getPort()));
            if (connected) {
                onOpen(task, 0L);
                closeQuiet(channel);
                callback.onTaskDone();
                return;
            }
            SelectionKey key = channel.register(selector, SelectionKey.OP_CONNECT);
            attachment.setKey(key);
            key.attach(attachment);
            deadlines.add(attachment);
            live++;
        } catch (IOException e) {
            closeQuiet(channel);
            if (isAddressExhaustion(e)) {
                limiter.backoff();
                sleepQuiet(200L);
            }
            callback.onClosedOrFiltered(task, mapExceptionState(e));
            callback.onTaskDone();
        }
    }

    private void processSelectedKeys() {
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> iterator = selectedKeys.iterator();
        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            iterator.remove();
            if (!key.isValid()) {
                continue;
            }
            if (key.isConnectable()) {
                onConnect(key);
            }
        }
    }

    private void onConnect(SelectionKey key) {
        TaskAttachment attachment = (TaskAttachment) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            if (channel.finishConnect()) {
                long rtt = System.currentTimeMillis() - attachment.getStartTime();
                limiter.recordSuccess(rtt);
                onOpen(attachment.getTask(), rtt);
            }
        } catch (ConnectException e) {
            callback.onClosedOrFiltered(attachment.getTask(), PortState.CLOSED);
        } catch (NoRouteToHostException e) {
            callback.onClosedOrFiltered(attachment.getTask(), PortState.FILTERED);
        } catch (IOException e) {
            limiter.recordError();
            callback.onClosedOrFiltered(attachment.getTask(), mapExceptionState(e));
        } finally {
            deadlines.remove(attachment);
            key.cancel();
            closeQuiet(channel);
            live--;
            callback.onTaskDone();
        }
    }

    private void onTimeout(TaskAttachment attachment) {
        SelectionKey key = attachment.getKey();
        if (key != null) {
            key.cancel();
            closeQuiet((SocketChannel) key.channel());
        }
        callback.onClosedOrFiltered(attachment.getTask(), PortState.FILTERED);
        live--;
        callback.onTaskDone();
    }

    private void onOpen(ScanTask task, long rtt) {
        PortResult result = new PortResult(task.getHost(), task.getPort(), PortState.OPEN);
        result.setRttMs(rtt);
        callback.onOpen(result);
    }

    private void closeAll() {
        deadlines.clear();
        if (selector != null) {
            for (SelectionKey key : selector.keys()) {
                try {
                    key.channel().close();
                } catch (IOException ignored) {
                }
            }
            try {
                selector.close();
            } catch (IOException ignored) {
            }
        }
    }

    private PortState mapExceptionState(IOException e) {
        if (e instanceof ConnectException) {
            return PortState.CLOSED;
        }
        if (e instanceof NoRouteToHostException) {
            return PortState.FILTERED;
        }
        return PortState.FILTERED;
    }

    private boolean isAddressExhaustion(IOException e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        return message.contains("EADDRNOTAVAIL")
                || message.contains("Cannot assign requested address")
                || message.contains("EMFILE")
                || message.contains("Too many open files");
    }

    private void closeQuiet(SocketChannel channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void sleepQuiet(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public interface ScanCallback {
        void onOpen(PortResult result);

        void onClosedOrFiltered(ScanTask task, PortState state);

        void onTaskDone();

        void onError(ScanTask task, Throwable throwable);

        void onWorkerFinished();
    }
}
