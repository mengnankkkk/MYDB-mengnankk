package com.mengnankk.mydatabase.backend.dm.logger;

import java.nio.channels.FileChannel;

public interface FlushStrategy {
    void flush(FileChannel fc);
}
