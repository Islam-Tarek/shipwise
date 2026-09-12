package com.shipwise.order.config;

import io.temporal.worker.WorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class TemporalWorkerLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(TemporalWorkerLifecycle.class);

    private final WorkerFactory workerFactory;
    private volatile boolean running;

    TemporalWorkerLifecycle(WorkerFactory workerFactory) {
        this.workerFactory = workerFactory;
    }

    @Override
    public void start() {
        try {
            workerFactory.start();
            running = true;
            log.info("temporal worker started on task queue ORDER_SAGA");
        } catch (Exception e) {
            log.warn("temporal worker could not start (is start-temporal.sh running?): {}", e.getMessage());
        }
    }

    @Override
    public void stop() {
        if (running) {
            workerFactory.shutdown();
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}