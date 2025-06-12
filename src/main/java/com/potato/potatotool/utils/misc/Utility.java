/*
 * Copyright (c) 2017, 2023, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// 因com.sun.org.apache.bcel.internal.classfile.Utility未在包中对外开放，打包后运行需要添加vm指令
// 故对该方法提出，微改
package com.potato.potatotool.utils.misc;


import java.io.*;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility functions that do not really belong to any class in particular.
 *
 * @LastModified: Feb 2023
 */
// @since 6.0 methods are no longer final
public abstract class Utility {

    /**
     * Decode characters into bytes. Used by <a href="Utility.html#decode(java.lang.String, boolean)">decode()</a>
     */
    private static class JavaReader extends FilterReader {

        public JavaReader(final Reader in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            final int b = in.read();
            if (b != ESCAPE_CHAR) {
                return b;
            }
            final int i = in.read();
            if (i < 0) {
                return -1;
            }
            if (i >= '0' && i <= '9' || i >= 'a' && i <= 'f') { // Normal escape
                final int j = in.read();
                if (j < 0) {
                    return -1;
                }
                final char[] tmp = {(char) i, (char) j};
                return Integer.parseInt(new String(tmp), 16);
            }
            return MAP_CHAR[i];
        }

        @Override
        public int read(final char[] cbuf, final int off, final int len) throws IOException {
            for (int i = 0; i < len; i++) {
                cbuf[off + i] = (char) read();
            }
            return len;
        }
    }

    // A-Z, g-z, _, $
    private static final int FREE_CHARS = 48;

    private static final int[] CHAR_MAP = new int[FREE_CHARS];

    private static final int[] MAP_CHAR = new int[256]; // Reverse map

    private static final char ESCAPE_CHAR = '$';

    static {
        int j = 0;
        for (int i = 'A'; i <= 'Z'; i++) {
            CHAR_MAP[j] = i;
            MAP_CHAR[i] = j;
            j++;
        }
        for (int i = 'g'; i <= 'z'; i++) {
            CHAR_MAP[j] = i;
            MAP_CHAR[i] = j;
            j++;
        }
        CHAR_MAP[j] = '$';
        MAP_CHAR['$'] = j;
        j++;
        CHAR_MAP[j] = '_';
        MAP_CHAR['_'] = j;
    }

    /**
     * Decode a string back to a byte array.
     *
     * @param s the string to convert
     * @param uncompress use gzip to uncompress the stream of bytes
     *
     * @throws IOException if there's a gzip exception
     */
    public static byte[] decode(final String s, final boolean uncompress) throws IOException {
        byte[] bytes;
        try (JavaReader jr = new JavaReader(new CharArrayReader(s.toCharArray())); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            int ch;
            while ((ch = jr.read()) >= 0) {
                bos.write(ch);
            }
            bytes = bos.toByteArray();
        }
        if (uncompress) {
            final GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bytes));
            final byte[] tmp = new byte[bytes.length * 3]; // Rough estimate
            int count = 0;
            int b;
            while ((b = gis.read()) >= 0) {
                tmp[count++] = (byte) b;
            }
            bytes = Arrays.copyOf(tmp, count);
        }
        return bytes;
    }

    /**
     * Replace all occurrences of <em>old</em> in <em>str</em> with <em>new</em>.
     *
     * @param str String to permute
     * @param old String to be replaced
     * @param new_ Replacement string
     * @return new String object
     */
    public static String replace(String str, final String old, final String new_) {
        int index;
        int oldIndex;
        try {
            if (str.contains(old)) { // 'old' found in str
                final StringBuilder buf = new StringBuilder();
                oldIndex = 0; // String start offset
                // While we have something to replace
                while ((index = str.indexOf(old, oldIndex)) != -1) {
                    buf.append(str, oldIndex, index); // append prefix
                    buf.append(new_); // append replacement
                    oldIndex = index + old.length(); // Skip 'old'.length chars
                }
                buf.append(str.substring(oldIndex)); // append rest of string
                str = buf.toString();
            }
        } catch (final StringIndexOutOfBoundsException e) { // Should not occur
            System.err.println(e);
        }
        return str;
    }


    private static class JavaWriter extends FilterWriter {

        public JavaWriter(final Writer out) {
            super(out);
        }


        @Override
        public void write( final int b ) throws IOException {
            if (isJavaIdentifierPart((char) b) && (b != ESCAPE_CHAR)) {
                out.write(b);
            } else {
                out.write(ESCAPE_CHAR); // Escape character
                // Special escape
                if (b >= 0 && b < FREE_CHARS) {
                    out.write(CHAR_MAP[b]);
                } else { // Normal escape
                    final char[] tmp = Integer.toHexString(b).toCharArray();
                    if (tmp.length == 1) {
                        out.write('0');
                        out.write(tmp[0]);
                    } else {
                        out.write(tmp[0]);
                        out.write(tmp[1]);
                    }
                }
            }
        }


        @Override
        public void write( final char[] cbuf, final int off, final int len ) throws IOException {
            for (int i = 0; i < len; i++) {
                write(cbuf[off + i]);
            }
        }


        @Override
        public void write( final String str, final int off, final int len ) throws IOException {
            write(str.toCharArray(), off, len);
        }
    }

    public static boolean isJavaIdentifierPart( final char ch ) {
        return ((ch >= 'a') && (ch <= 'z')) || ((ch >= 'A') && (ch <= 'Z'))
                || ((ch >= '0') && (ch <= '9')) || (ch == '_');
    }

    public static String encode(byte[] bytes, final boolean compress) throws IOException {
        if (compress) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 GZIPOutputStream gos = new GZIPOutputStream(baos)) {
                gos.write(bytes, 0, bytes.length);
                bytes = baos.toByteArray();
            }
        }
        final CharArrayWriter caw = new CharArrayWriter();
        try (JavaWriter jw = new JavaWriter(caw)) {
            for (final byte b : bytes) {
                final int in = b & 0x000000ff; // Normalize to unsigned
                jw.write(in);
            }
        }
        return caw.toString();
    }

}
