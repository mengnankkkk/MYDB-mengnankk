package com.mengnankk.mydatabase.backend.dm.logger;

import com.google.common.primitives.Bytes;
import com.mengnankk.mydatabase.backend.utils.Panic;
import com.mengnankk.mydatabase.backend.utils.Parser;
import com.mengnankk.mydatabase.common.Error;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LoggerImpl implements Logger {

    private static final int SEED = 13331;
    private static final int HEADER_SIZE = 4; // xChecksum 占 4 字节

    private static final int OF_SIZE = 0;
    private static final int OF_CHECKSUM = OF_SIZE + 4;
    private static final int OF_DATA = OF_CHECKSUM + 4;

    private final RandomAccessFile file;
    private final FileChannel fc;
    private final Lock lock = new ReentrantLock();
    
    private final FlushStrategy flushStrategy;

    private int xChecksum;
    private long position = HEADER_SIZE;

    public static final String LOG_SUFFIX = ".log";

    LoggerImpl(RandomAccessFile raf, FileChannel fc, int xChecksum, FlushStrategy flushStrategy) {
        this.file = raf;
        this.fc = fc;
        this.xChecksum = xChecksum;
        this.flushStrategy = flushStrategy;
    }

    LoggerImpl(RandomAccessFile raf, FileChannel fc, int xChecksum) {
        this.file = raf;
        this.fc = fc;
        this.xChecksum = xChecksum;
        flushStrategy = null;
    }

    void init() {
        try {
            if (file.length() < HEADER_SIZE) {
                Panic.panic(Error.BadLogFileException);
            }

            ByteBuffer headerBuf = ByteBuffer.allocate(HEADER_SIZE);
            fc.position(0);
            fc.read(headerBuf);
            this.xChecksum = Parser.parseInt(headerBuf.array());

            validateAndTruncateTail();//校验每条日志、记录 validEnd
        } catch (IOException e) {
            Panic.panic(e);
        }
    }

    // 校验日志并截断非法尾部
    private void validateAndTruncateTail() {
        rewind();
        int calcXCheck = 0;
        long validEnd = HEADER_SIZE;

        while (true) {
            LogEntry entry = readNextLog();
            if (entry == null) break;

            calcXCheck = calChecksum(calcXCheck, entry.fullBytes);
            validEnd += entry.fullBytes.length;
        }

        if (calcXCheck != xChecksum) {
            Panic.panic(Error.BadLogFileException);
        }

        try {
            truncate(validEnd);
            file.seek(validEnd);
        } catch (Exception e) {
            Panic.panic(e);
        }

        rewind();
    }

    // 日志包装
    private byte[] wrapLog(byte[] data) {
        byte[] checksum = Parser.int2Byte(calChecksum(0, data));
        byte[] size = Parser.int2Byte(data.length);
        return Bytes.concat(size, checksum, data);
    }

    private int calChecksum(int base, byte[] data) {
        int result = base;
        for (byte b : data) {
            result = result * SEED + b;
        }
        return result;
    }

    @Override
    public void log(byte[] data) {
        byte[] logEntry = wrapLog(data);
        lock.lock();
        try {
            fc.position(fc.size());
            fc.write(ByteBuffer.wrap(logEntry));
            updateXChecksum(logEntry);
        } catch (IOException e) {
            Panic.panic(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 选择刷盘更新，同步/异步/不更新
     * @param logEntry
     */
    private void updateXChecksum(byte[] logEntry) {
        xChecksum = calChecksum(xChecksum, logEntry);
        try {
            fc.position(0);
            fc.write(ByteBuffer.wrap(Parser.int2Byte(xChecksum)));
            if (flushStrategy instanceof  AsyncFlushStrategy ){
                AsyncFlushStrategy strategy = (AsyncFlushStrategy)  flushStrategy;
                strategy.recordWriteLength(logEntry.length+4);
            }else {
                flushStrategy.flush(fc);
            }
        } catch (IOException e) {
            Panic.panic(e);
        }
    }

    // 内部封装：读取下一条完整日志
    private LogEntry readNextLog() {
        try {
            if (position + OF_DATA >= fc.size()) return null;

            ByteBuffer headerBuf = ByteBuffer.allocate(OF_DATA);
            fc.position(position);
            fc.read(headerBuf);
            byte[] header = headerBuf.array();

            int size = Parser.parseInt(Arrays.copyOfRange(header, OF_SIZE, OF_CHECKSUM));
            int expectedChecksum = Parser.parseInt(Arrays.copyOfRange(header, OF_CHECKSUM, OF_DATA));

            if (position + OF_DATA + size > fc.size()) return null;

            ByteBuffer fullBuf = ByteBuffer.allocate(OF_DATA + size);
            fc.position(position);
            fc.read(fullBuf);
            byte[] fullLog = fullBuf.array();

            byte[] data = Arrays.copyOfRange(fullLog, OF_DATA, OF_DATA + size);
            int actualChecksum = calChecksum(0, data);

            if (expectedChecksum != actualChecksum) {
                return null;
            }

            LogEntry entry = new LogEntry(fullLog, data);
            position += fullLog.length;
            return entry;

        } catch (IOException e) {
            Panic.panic(e);
            return null;
        }
    }

    @Override
    public byte[] next() {
        lock.lock();
        try {
            LogEntry entry = readNextLog();
            return entry == null ? null : entry.data;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void truncate(long x) throws Exception {
        lock.lock();
        try {
            fc.truncate(x);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void rewind() {
        position = HEADER_SIZE;
    }

    @Override
    public void close() {
        try {
            fc.close();
            file.close();
        } catch (IOException e) {
            Panic.panic(e);
        }
    }

    // 封装日志结构
    private static class LogEntry {
        byte[] fullBytes;
        byte[] data;

        LogEntry(byte[] fullBytes, byte[] data) {
            this.fullBytes = fullBytes;
            this.data = data;
        }
    }
}
