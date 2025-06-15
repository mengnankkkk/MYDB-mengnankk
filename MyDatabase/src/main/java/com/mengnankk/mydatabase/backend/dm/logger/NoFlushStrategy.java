package com.mengnankk.mydatabase.backend.dm.logger;

import java.nio.channels.FileChannel;

public class NoFlushStrategy implements FlushStrategy{

    @Override
    public void flush(FileChannel fc) {

    }
}
