package me.gv7.woodpecker.plugin.utils;

import java.io.IOException;
import java.io.InputStream;

// modified for jdk.internal.org.objectweb.asm.ClassReader
public class ClassNameReader {
    public final byte[] b;
    private final int[] items;
    private final String[] strings;
    private final int maxStringLength;
    public final int header;

    public ClassNameReader(byte[] var1) {
        this(var1, 0, var1.length);
    }

    public ClassNameReader(byte[] var1, int var2, int var3) {
        this.b = var1;
        if (this.readShort(var2 + 6) > 52) {
            throw new IllegalArgumentException();
        } else {
            this.items = new int[this.readUnsignedShort(var2 + 8)];
            int var4 = this.items.length;
            this.strings = new String[var4];
            int var5 = 0;
            int var6 = var2 + 10;

            for (int var7 = 1; var7 < var4; ++var7) {
                this.items[var7] = var6 + 1;
                int var8;
                switch (var1[var6]) {
                    case 1:
                        var8 = 3 + this.readUnsignedShort(var6 + 1);
                        if (var8 > var5) {
                            var5 = var8;
                        }
                        break;
                    case 2:
                    case 7:
                    case 8:
                    case 13:
                    case 14:
                    case 16:
                    case 17:
                    default:
                        var8 = 3;
                        break;
                    case 3:
                    case 4:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 18:
                        var8 = 5;
                        break;
                    case 5:
                    case 6:
                        var8 = 9;
                        ++var7;
                        break;
                    case 15:
                        var8 = 4;
                }

                var6 += var8;
            }

            this.maxStringLength = var5;
            this.header = var6;
        }
    }

    public int getAccess() {
        return this.readUnsignedShort(this.header);
    }

    public String getClassName() {
        return this.readClass(this.header + 2, new char[this.maxStringLength]);
    }

    public String getSuperName() {
        return this.readClass(this.header + 4, new char[this.maxStringLength]);
    }

    public String[] getInterfaces() {
        int var1 = this.header + 6;
        int var2 = this.readUnsignedShort(var1);
        String[] var3 = new String[var2];
        if (var2 > 0) {
            char[] var4 = new char[this.maxStringLength];

            for (int var5 = 0; var5 < var2; ++var5) {
                var1 += 2;
                var3[var5] = this.readClass(var1, var4);
            }
        }

        return var3;
    }


    public ClassNameReader(InputStream var1) throws IOException {
        this(readClass(var1, false));
    }

    public ClassNameReader(String var1) throws IOException {
        this(readClass(ClassLoader.getSystemResourceAsStream(var1.replace('.', '/') + ".class"), true));
    }

    private static byte[] readClass(InputStream var0, boolean var1) throws IOException {
        if (var0 == null) {
            throw new IOException("Class not found");
        } else {
            try {
                byte[] var2 = new byte[var0.available()];
                int var3 = 0;

                while (true) {
                    int var4 = var0.read(var2, var3, var2.length - var3);
                    if (var4 == -1) {
                        byte[] var10;
                        if (var3 < var2.length) {
                            var10 = new byte[var3];
                            System.arraycopy(var2, 0, var10, 0, var3);
                            var2 = var10;
                        }

                        var10 = var2;
                        return var10;
                    }

                    var3 += var4;
                    if (var3 == var2.length) {
                        int var5 = var0.read();
                        byte[] var6;
                        if (var5 < 0) {
                            var6 = var2;
                            return var6;
                        }

                        var6 = new byte[var2.length + 1000];
                        System.arraycopy(var2, 0, var6, 0, var3);
                        var6[var3++] = (byte) var5;
                        var2 = var6;
                    }
                }
            } finally {
                if (var1) {
                    var0.close();
                }

            }
        }
    }

    public int readUnsignedShort(int var1) {
        byte[] var2 = this.b;
        return (var2[var1] & 255) << 8 | var2[var1 + 1] & 255;
    }

    public short readShort(int var1) {
        byte[] var2 = this.b;
        return (short) ((var2[var1] & 255) << 8 | var2[var1 + 1] & 255);
    }

    public int readInt(int var1) {
        byte[] var2 = this.b;
        return (var2[var1] & 255) << 24 | (var2[var1 + 1] & 255) << 16 | (var2[var1 + 2] & 255) << 8 | var2[var1 + 3] & 255;
    }

    public long readLong(int var1) {
        long var2 = (long) this.readInt(var1);
        long var4 = (long) this.readInt(var1 + 4) & 4294967295L;
        return var2 << 32 | var4;
    }

    public String readUTF8(int var1, char[] var2) {
        int var3 = this.readUnsignedShort(var1);
        if (var1 != 0 && var3 != 0) {
            String var4 = this.strings[var3];
            if (var4 != null) {
                return var4;
            } else {
                var1 = this.items[var3];
                return this.strings[var3] = this.readUTF(var1 + 2, this.readUnsignedShort(var1), var2);
            }
        } else {
            return null;
        }
    }

    private String readUTF(int var1, int var2, char[] var3) {
        int var4 = var1 + var2;
        byte[] var5 = this.b;
        int var6 = 0;
        byte var8 = 0;
        char var9 = 0;

        while (true) {
            while (var1 < var4) {
                int var7 = var5[var1++];
                switch (var8) {
                    case 0:
                        var7 &= 255;
                        if (var7 < 128) {
                            var3[var6++] = (char) var7;
                        } else {
                            if (var7 < 224 && var7 > 191) {
                                var9 = (char) (var7 & 31);
                                var8 = 1;
                                continue;
                            }

                            var9 = (char) (var7 & 15);
                            var8 = 2;
                        }
                        break;
                    case 1:
                        var3[var6++] = (char) (var9 << 6 | var7 & 63);
                        var8 = 0;
                        break;
                    case 2:
                        var9 = (char) (var9 << 6 | var7 & 63);
                        var8 = 1;
                }
            }

            return new String(var3, 0, var6);
        }
    }

    public String readClass(int var1, char[] var2) {
        return this.readUTF8(this.items[this.readUnsignedShort(var1)], var2);
    }
}
