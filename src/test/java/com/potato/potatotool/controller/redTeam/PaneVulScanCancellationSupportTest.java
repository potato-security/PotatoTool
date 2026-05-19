package com.potato.potatotool.controller.redTeam;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PaneVulScan 取消路径支持测试")
class PaneVulScanCancellationSupportTest {

    @Test
    @DisplayName("扫描引擎尚未启动时应取消预扫描后台任务并中断线程")
    void shouldCancelPreScanTaskBeforeEngineStart() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean(false);
        FutureTask<Void> task = new FutureTask<Void>(() -> {
            started.countDown();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                interrupted.set(true);
                Thread.currentThread().interrupt();
            }
            return null;
        });
        Thread worker = new Thread(task, "pane-vulscan-pre-cancel");
        worker.start();

        assertTrue(started.await(1, TimeUnit.SECONDS));

        boolean cancelled = PaneVulScanSupport.cancelTaskBeforeEngineStart(true, task, worker, false);

        worker.join(1000);

        assertTrue(cancelled);
        assertTrue(task.isCancelled());
        assertTrue(interrupted.get(), "预扫描取消应中断后台线程");
    }

    @Test
    @DisplayName("扫描引擎已启动时不应误取消预扫描任务分支")
    void shouldNotCancelPreScanTaskWhenEngineAlreadyRunning() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        FutureTask<Void> task = new FutureTask<Void>(() -> {
            started.countDown();
            return null;
        });
        Thread worker = new Thread(task, "pane-vulscan-pre-running");
        worker.start();

        assertTrue(started.await(1, TimeUnit.SECONDS));
        worker.join(1000);

        boolean cancelled = PaneVulScanSupport.cancelTaskBeforeEngineStart(true, task, worker, true);

        assertFalse(cancelled);
        assertFalse(task.isCancelled());
    }

    @Test
    @DisplayName("停止扫描时应取消当前任务并中断线程")
    void shouldCancelRunningTaskAndInterruptWorker() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean(false);
        FutureTask<Void> task = new FutureTask<Void>(() -> {
            started.countDown();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                interrupted.set(true);
                Thread.currentThread().interrupt();
            }
            return null;
        });
        Thread worker = new Thread(task, "pane-vulscan-stop-cancel");
        worker.start();

        assertTrue(started.await(1, TimeUnit.SECONDS));

        boolean cancelled = PaneVulScanSupport.cancelRunningTask(true, task, worker);

        worker.join(1000);

        assertTrue(cancelled);
        assertTrue(task.isCancelled());
        assertTrue(interrupted.get(), "停止扫描应中断后台线程");
    }
}
