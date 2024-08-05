package me.gv7.woodpecker.plugin.utils;

import me.gv7.woodpecker.bcel.HackBCELs;
import me.gv7.woodpecker.tools.codec.BASE64Encoder;

import java.io.IOException;
import java.math.BigInteger;

public class MemShellClassFactory {
    public static final int BASE64 = 1;
    public static final int BCEL = 2;
    public static final int BIGINTEGER = 3;
    public static final int UNSAFE = 4;

    private BASE64Encoder base64Encoder = new BASE64Encoder();
    private byte[] _classFile;
    int _encodeMode = 1;

    public MemShellClassFactory(byte[] classFile, int encodeMode) {
        this._classFile = classFile;
        this._encodeMode = encodeMode;
    }

    public String getClassName() throws Exception {
        return Utils.getFullClassName(_classFile);
    }

    public String getPayload() throws IOException {
        switch (_encodeMode) {
            case BASE64:
                return base64Encoder.encode(_classFile).replace(System.getProperty("line.separator"), "");
            case BCEL:
                return HackBCELs.encode(_classFile);
            case BIGINTEGER:
                return new BigInteger(_classFile).toString(36);
        }
        return null;
    }
}
