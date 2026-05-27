package com.potato.potatotool.content.redTeam.portScanner.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class PortScanEventDispatcher {
    private final List<PortScanEventListener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "portscan-event-dispatcher");
        thread.setDaemon(true);
        return thread;
    });

    public void addListener(PortScanEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(PortScanEventListener listener) {
        listeners.remove(listener);
    }

    public void dispatchStarted(final PortScanStartedEvent event) {
        dispatch(listener -> listener.onScanStarted(event));
    }

    public void dispatchProgress(final PortScanProgressEvent event) {
        dispatch(listener -> listener.onScanProgress(event));
    }

    public void dispatchPortFound(final PortFoundEvent event) {
        dispatch(listener -> listener.onPortFound(event));
    }

    public void dispatchCompleted(final PortScanCompletedEvent event) {
        dispatch(listener -> listener.onScanCompleted(event));
    }

    public void dispatchError(final PortScanErrorEvent event) {
        dispatch(listener -> listener.onScanError(event));
    }

    public void shutdown() {
        executorService.shutdownNow();
    }

    private void dispatch(final ListenerAction action) {
        try {
            executorService.submit(() -> {
                for (PortScanEventListener listener : listeners) {
                    try {
                        action.call(listener);
                    } catch (Exception ignored) {
                    }
                }
            });
        } catch (RejectedExecutionException ignored) {
        }
    }

    private interface ListenerAction {
        void call(PortScanEventListener listener);
    }
}
