package com.mengnankk.mydatabase.backend.parser;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class Tokenizer {
    private byte[] stat;
    private int pos;
    private String nowToken;
    private boolean flushToken;
    private int errStat;

    public Tokenizer(byte[] stat) {
        this.stat = stat;
        this.pos = 0;
        this.flushToken = true;
        this.errStat = 0;
    }

    public String peek() throws Exception {
        if(flushToken) {
            String token = null;
            try {
                token = next();
                nowToken = token;
                flushToken = false;
            } catch(Exception e) {
                nowToken = null;
                throw e;
            }
            return nowToken;
        } else {
            return nowToken;
        }
    }

    public void pop() {
        flushToken = true;
    }

    private String next() throws Exception {
        if(errStat != 0) {
            throw new RuntimeException("Tokenizer error");
        }
        return nextMetaState();
    }

    private String nextMetaState() throws Exception {
        // Skip whitespace
        while(pos < stat.length && (stat[pos] == ' ' || stat[pos] == '\n' || stat[pos] == '\t')) {
            pos ++;
        }
        if(pos == stat.length) {
            return "";
        }

        // Handle different token types
        byte b = stat[pos];
        if(b == '(' || b == ')' || b == ',' || b == '=' || b == '>' || b == '<' || b == '*' || b == '+' || b == '-') {
            pos ++;
            return new String(new byte[]{b}, StandardCharsets.UTF_8);
        } else if(b == '\'' || b == '"') {
            return nextStringState();
        } else if(isLetter(b) || isNumber(b)) {
            return nextWordState();
        } else {
            errStat = pos;
            throw new RuntimeException("Invalid token: " + new String(new byte[]{b}, StandardCharsets.UTF_8));
        }
    }

    private String nextStringState() throws Exception {
        byte quote = stat[pos];
        pos ++;
        int start = pos;
        while(pos < stat.length) {
            if(stat[pos] == quote && (pos == start || stat[pos-1] != '\\')) {
                String str = new String(Arrays.copyOfRange(stat, start, pos), StandardCharsets.UTF_8);
                pos ++;
                return str;
            }
            pos ++;
        }
        errStat = start;
        throw new RuntimeException("String not properly terminated");
    }

    private String nextWordState() throws Exception {
        int start = pos;
        while(pos < stat.length) {
            byte b = stat[pos];
            if(isLetter(b) || isNumber(b) || b == '_') {
                pos ++;
            } else {
                break;
            }
        }
        return new String(Arrays.copyOfRange(stat, start, pos), StandardCharsets.UTF_8);
    }

    private boolean isLetter(byte b) {
        return (b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z');
    }

    private boolean isNumber(byte b) {
        return b >= '0' && b <= '9';
    }

    public static boolean isAlphaBeta(byte b) {
        return (b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z');
    }

    public static boolean isDigit(byte b) {
        return b >= '0' && b <= '9';
    }

    public byte[] errStat() {
        return Arrays.copyOfRange(stat, errStat, stat.length);
    }
}
