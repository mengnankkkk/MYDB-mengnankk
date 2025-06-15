package com.mengnankk.mydatabase.backend.utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.google.common.primitives.Bytes;

public class Parser {

    public static byte[] short2Byte(short value) {
        return ByteBuffer.allocate(Short.SIZE / Byte.SIZE).putShort(value).array();
    }

    public static short parseShort(byte[] buf) {
        ByteBuffer buffer = ByteBuffer.wrap(buf, 0, 2);
        return buffer.getShort();
    }

    public static byte[] int2Byte(int value) {
        return ByteBuffer.allocate(Integer.SIZE / Byte.SIZE).putInt(value).array();
    }

    public static int parseInt(byte[] buf) {
        ByteBuffer buffer = ByteBuffer.wrap(buf, 0, 4);
        return buffer.getInt();
    }

    public static long parseLong(byte[] buf) {
        ByteBuffer buffer = ByteBuffer.wrap(buf, 0, 8);
        return buffer.getLong();
    }

    public static byte[] long2Byte(long value) {
        return ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(value).array();
    }

    public static ParseStringRes parseString(byte[] raw) {
        int length = parseInt(Arrays.copyOf(raw, 4));
        String str = new String(Arrays.copyOfRange(raw, 4, 4+length), StandardCharsets.UTF_8);
        return new ParseStringRes(str, length+4);
    }

    public static byte[] string2Byte(String str) {
        byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
        byte[] l = int2Byte(strBytes.length);
        return Bytes.concat(l, strBytes);
    }

    public static long str2Uid(String key) {
        long seed = 13331;
        long res = 0;
        for(byte b : key.getBytes(StandardCharsets.UTF_8)) {
            res = res * seed + (long)b;
        }
        return res;
    }

    /**
     * encode long value 为 VarInt 返回写入字节长度
     * @param value
     * @param buf
     * @param offset
     * @return
     */
    public static int encodeVarInt(long value,byte[] buf,int offset){
        int pos = offset;
        while ((value&~0x7FL)!=0){
            buf[pos++] = (byte) ((value&0x7F)|0x80);
            value >>>=7;
        }
        buf[pos++] = (byte) value;
        return pos-offset;
    }

    /**
     * decode VarInt，从 offset 读取，返回实际值并将读取长度存入 outSize[0]
     * @param buf
     * @param offset
     * @param outsize
     * @return
     */
    public static long decodeVarInt(byte[] buf,int offset,int[] outsize){
        long result = 0;
        int shift = 0,pos = offset;
        while (true){
            byte b = buf[pos++];
            result |=(long)(b&0x7F)<<shift;
            if ((b&0x80)==0) break;
            shift +=7;
        }
        outsize[0] = pos-offset;
        return result;
    }

}
