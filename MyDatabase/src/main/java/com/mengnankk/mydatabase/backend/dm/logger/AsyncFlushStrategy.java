package com.mengnankk.mydatabase.backend.dm.logger;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class AsyncFlushStrategy implements FlushStrategy{
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final FileChannel fc;
    private final long flushIntervalMillis;
    private final long flushThresholdBytes;

    private final ScheduledExecutorService scheduler;
    private final AtomicLong pendingBytes = new AtomicLong(0);
    private volatile long lastFlushTime;

    public AsyncFlushStrategy(FileChannel fc, long flushIntervalMillis, ScheduledExecutorService scheduler,long flushThresholdBytes) {
        this.fc = fc;
        this.flushIntervalMillis = flushIntervalMillis;
        this.scheduler = scheduler;
        this.flushThresholdBytes = flushThresholdBytes;
        scheduler.scheduleAtFixedRate(this::tryFlush, flushIntervalMillis,
                flushIntervalMillis, TimeUnit.MILLISECONDS);

    }
    private volatile boolean pendingFlush = false;


    @Override
    public void flush(FileChannel fc) {
        pendingBytes.addAndGet(1);
        tryFlush();

    }
    public void shutdown(){
        scheduler.shutdown();
    }
    public void recordWriteLength(int len){
        pendingBytes.addAndGet(len);
        tryFlush();
    }
    private void tryFlush(){
        long bytes = pendingBytes.get();
        long now =System.currentTimeMillis();

        boolean timeCondition= (now-lastFlushTime)>=flushIntervalMillis;
        boolean sizeCondition = bytes>=flushThresholdBytes;
        if (timeCondition||sizeCondition){
            synchronized (this){
                if (pendingBytes.get()>=flushThresholdBytes||(System.currentTimeMillis()-lastFlushTime)>=flushIntervalMillis){
                    try {
                        fc.force(false);
                        pendingBytes.set(0);
                        lastFlushTime = System.currentTimeMillis();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
