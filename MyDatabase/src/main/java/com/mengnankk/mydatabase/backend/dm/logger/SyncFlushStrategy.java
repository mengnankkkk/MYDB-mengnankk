package com.mengnankk.mydatabase.backend.dm.logger;

import com.mengnankk.mydatabase.backend.utils.Panic;

import java.io.IOException;
import java.nio.channels.FileChannel;

public class SyncFlushStrategy implements FlushStrategy {
    @Override
    public void flush(FileChannel fc) {
        try {
            fc.force(false); // 同步刷盘
        } catch (IOException e) {
            Panic.panic(e);
        }
    }
}
