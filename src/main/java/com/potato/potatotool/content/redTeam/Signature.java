package com.potato.potatotool.content.redTeam;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * @author Potato
 * @date 2024/7/26 16:22
 */

public class Signature {

    // 定义FileInfo类，存储PE文件相关信息
    public static class FileInfo {
        public int buffer = 0;
        public int JMPtoCodeAddress = 0;
        public int dis_frm_pehdrs_sectble = 248;
        public int pe_header_location;
        public int COFF_Start;
        public int MachineType;
        public int NumberOfSections;
        public int TimeDateStamp;
        public int SizeOfOptionalHeader;
        public int Characteristics;
        public int OptionalHeader_start;
        public int Magic;
        public int MajorLinkerVersion;
        public int MinorLinkerVersion;
        public int SizeOfCode;
        public int SizeOfInitializedData;
        public int SizeOfUninitializedData;
        public int AddressOfEntryPoint;
        public int PatchLocation;
        public int BaseOfCode;
        public int BaseOfData;
        public long ImageBase;
        public int SectionAlignment;
        public int FileAlignment;
        public int MajorOperatingSystemVersion;
        public int MinorOperatingSystemVersion;
        public int MajorImageVersion;
        public int MinorImageVersion;
        public int MajorSubsystemVersion;
        public int MinorSubsystemVersion;
        public int Win32VersionValue;
        public int SizeOfImageLoc;
        public int SizeOfImage;
        public int SizeOfHeaders;
        public int CheckSum;
        public int Subsystem;
        public int DllCharacteristics;
        public long SizeOfStackReserve;
        public long SizeOfStackCommit;
        public long SizeOfHeapReserve;
        public long SizeOfHeapCommit;
        public int LoaderFlags;
        public int NumberofRvaAndSizes;
        public int ExportTableRVA;
        public int ExportTableSize;
        public int ImportTableLOCInPEOptHdrs;
        public int ImportTableRVA;
        public int ImportTableSize;
        public long ResourceTable;
        public long ExceptionTable;
        public int CertTableLOC;
        public int CertLOC;
        public int CertSize;

    }

    // 收集PE文件信息
    public static FileInfo gatherFileInfoWin(String filePath) throws IOException {
        FileInfo fileInfo = new FileInfo();

        try (RandomAccessFile binary = new RandomAccessFile(filePath, "r")) {

            // 读取PE头位置
            binary.seek(0x3C);
            fileInfo.pe_header_location = readInt(binary);

            // 开始COFF部分
            fileInfo.COFF_Start = fileInfo.pe_header_location + 4;
            binary.seek(fileInfo.COFF_Start);
            fileInfo.MachineType = readUnsignedShort(binary);
            fileInfo.NumberOfSections = readUnsignedShort(binary);
            fileInfo.TimeDateStamp = readInt(binary);
            binary.seek(fileInfo.COFF_Start + 16);
            fileInfo.SizeOfOptionalHeader = readUnsignedShort(binary);
            fileInfo.Characteristics = readUnsignedShort(binary);
            fileInfo.OptionalHeader_start = fileInfo.COFF_Start + 20;

            // 开始读取可选头的标准字段部分
            binary.seek(fileInfo.OptionalHeader_start);
            fileInfo.Magic = readUnsignedShort(binary);
            fileInfo.MajorLinkerVersion = readByte(binary);
            fileInfo.MinorLinkerVersion = readByte(binary);
            fileInfo.SizeOfCode = readInt(binary);
            fileInfo.SizeOfInitializedData = readInt(binary);
            fileInfo.SizeOfUninitializedData = readInt(binary);
            fileInfo.AddressOfEntryPoint = readInt(binary);
            fileInfo.PatchLocation = fileInfo.AddressOfEntryPoint;
            fileInfo.BaseOfCode = readInt(binary);

            if (fileInfo.Magic != 0x20B) {
                fileInfo.BaseOfData = readInt(binary);
            }

            // 开始读取可选头的标准字段部分
            if (fileInfo.Magic == 0x20B) {
                fileInfo.ImageBase = readLong(binary);
            } else {
                fileInfo.ImageBase = readInt(binary);
            }
            fileInfo.SectionAlignment = readInt(binary);
            fileInfo.FileAlignment = readInt(binary);
            fileInfo.MajorOperatingSystemVersion = readUnsignedShort(binary);
            fileInfo.MinorOperatingSystemVersion = readUnsignedShort(binary);
            fileInfo.MajorImageVersion = readUnsignedShort(binary);
            fileInfo.MinorImageVersion = readUnsignedShort(binary);
            fileInfo.MajorSubsystemVersion = readUnsignedShort(binary);
            fileInfo.MinorSubsystemVersion = readUnsignedShort(binary);
            fileInfo.Win32VersionValue = readInt(binary);
            fileInfo.SizeOfImageLoc = (int) binary.getFilePointer();
            fileInfo.SizeOfImage = readInt(binary);
            fileInfo.SizeOfHeaders = readInt(binary);
            fileInfo.CheckSum = readInt(binary);
            fileInfo.Subsystem = readUnsignedShort(binary);
            fileInfo.DllCharacteristics = readUnsignedShort(binary);

            if (fileInfo.Magic == 0x20B) {
                fileInfo.SizeOfStackReserve = readLong(binary);
                fileInfo.SizeOfStackCommit = readLong(binary);
                fileInfo.SizeOfHeapReserve = readLong(binary);
                fileInfo.SizeOfHeapCommit = readLong(binary);
            } else {
                fileInfo.SizeOfStackReserve = readInt(binary);
                fileInfo.SizeOfStackCommit = readInt(binary);
                fileInfo.SizeOfHeapReserve = readInt(binary);
                fileInfo.SizeOfHeapCommit = readInt(binary);
            }

            fileInfo.LoaderFlags = readInt(binary);
            fileInfo.NumberofRvaAndSizes = readInt(binary);

            // 读取可选头的数据目录部分
            fileInfo.ExportTableRVA = readInt(binary);
            fileInfo.ExportTableSize = readInt(binary);
            fileInfo.ImportTableLOCInPEOptHdrs = (int) binary.getFilePointer();
            fileInfo.ImportTableRVA = readInt(binary);
            fileInfo.ImportTableSize = readInt(binary);
            fileInfo.ResourceTable = readLong(binary);
            fileInfo.ExceptionTable = readLong(binary);
            fileInfo.CertTableLOC = (int) binary.getFilePointer();
            fileInfo.CertLOC = readInt(binary);
            fileInfo.CertSize = readInt(binary);
        }

        return fileInfo;
    }

    // 读取四字节整型数（小端序）
    private static int readInt(RandomAccessFile file) throws IOException {
        byte[] buffer = new byte[4];
        file.readFully(buffer);
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        return byteBuffer.getInt();
    }

    // 读取无符号两字节整型数（小端序）
    private static int readUnsignedShort(RandomAccessFile file) throws IOException {
        byte[] buffer = new byte[2];
        file.readFully(buffer);
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        return byteBuffer.getShort() & 0xFFFF; // 转无符号类型
    }

    // 读取单字节数
    private static byte readByte(RandomAccessFile file) throws IOException {
        return file.readByte();
    }

    // 读取八字节长整型数（小端序）
    private static long readLong(RandomAccessFile file) throws IOException {
        byte[] buffer = new byte[8];
        file.readFully(buffer);
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        return byteBuffer.getLong();
    }

    // 复制签名
    public static byte[] copyCert(String exePath) throws IOException {
        // 收集可执行文件的信息
        FileInfo flItms = gatherFileInfoWin(exePath);

        // 检查是否存在签名
        if (flItms.CertLOC == 0 || flItms.CertSize == 0) {
            System.out.println("输入的文件未签名!");
            return null;
        }

        byte[] cert;
        try (RandomAccessFile binary = new RandomAccessFile(exePath, "r")) {
            // 移动到签名位置并读取签名
            binary.seek(flItms.CertLOC);
            cert = new byte[flItms.CertSize];
            binary.readFully(cert);
        }

        return cert;
    }

    // 写入签名
    public static void writeCert(byte[] cert, String exePath, String outputPath) throws IOException {
        // 收集可执行文件的信息
        FileInfo flItms = gatherFileInfoWin(exePath);

        // 确认输出路径
        if (outputPath == null || outputPath.isEmpty()) {
            outputPath = exePath + "_signed";
        }

        // 复制源文件到目标路径
        Files.copy(Paths.get(exePath), Paths.get(outputPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        System.out.println("输出文件: " + outputPath);

        try (RandomAccessFile inputFile = new RandomAccessFile(exePath, "r");
             RandomAccessFile outputFile = new RandomAccessFile(outputPath, "rw")) {

            // 读取整个文件内容
            byte[] buffer = new byte[(int) inputFile.length()];
            inputFile.readFully(buffer);
            outputFile.write(buffer);

            // 移动到证书表位置并写入证书
            outputFile.seek(flItms.CertTableLOC);
            ByteBuffer byteBuffer = ByteBuffer.allocate(8);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.putInt((int) inputFile.length());
            byteBuffer.putInt(cert.length);
            outputFile.write(byteBuffer.array());
            outputFile.seek(outputFile.length());
            outputFile.write(cert);
        }

        System.out.println("签名附加完成");
    }

    // 输出签名文件
    public static void outputCert(String exePath, String outputPath) throws IOException {
        byte[] cert = copyCert(exePath);
        if (cert == null) return;

        if (outputPath == null || outputPath.isEmpty()) {
            outputPath = exePath + "_sig";
        }

        System.out.println("输出文件: " + outputPath);
        Files.write(Paths.get(outputPath), cert);
        System.out.println("签名提取完成");
    }

    // 检查签名
    public static void checkSig(String exePath) throws IOException {
        FileInfo flItms = gatherFileInfoWin(exePath);

        if (flItms.CertLOC == 0 || flItms.CertSize == 0) {
            System.out.println("输入的文件未签名!");
        } else {
            System.out.println("输入的文件已签名!");
        }
    }

    // 移除签名
    public static void truncate(String exePath, String outputPath) throws IOException {
        FileInfo flItms = gatherFileInfoWin(exePath);

        if (flItms.CertLOC == 0 || flItms.CertSize == 0) {
            System.out.println("输入的文件未签名!");
            return;
        }

        if (outputPath == null || outputPath.isEmpty()) {
            outputPath = exePath + "_nosig";
        }

        System.out.println("输出文件: " + outputPath);
        Files.copy(Paths.get(exePath), Paths.get(outputPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        try (RandomAccessFile binary = new RandomAccessFile(outputPath, "rw")) {
            System.out.println("覆盖证书表指针并截断二进制文件");
            binary.seek(binary.length() - flItms.CertSize);
            binary.setLength(binary.length() - flItms.CertSize);
            binary.seek(flItms.CertTableLOC);
            binary.write(new byte[8]);
        }

        System.out.println("签名移除完成");
    }


    // 使用外部签名文件签名
    public static void signFile(String exePath, String sigFilePath, String outputPath) throws IOException {
        FileInfo flItms = gatherFileInfoWin(exePath);


        byte[] cert = Files.readAllBytes(Paths.get(sigFilePath));

        if (outputPath == null || outputPath.isEmpty()) {
            outputPath = exePath + "_signed";
        }

        Files.copy(Paths.get(exePath), Paths.get(outputPath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        System.out.println("输出文件: " + outputPath);

        try (RandomAccessFile inputFile = new RandomAccessFile(exePath, "r");
             RandomAccessFile outputFile = new RandomAccessFile(outputPath, "rw")) {

            byte[] buffer = new byte[(int) inputFile.length()];
            inputFile.readFully(buffer);
            outputFile.write(buffer);

            outputFile.seek(flItms.CertTableLOC);
            ByteBuffer byteBuffer = ByteBuffer.allocate(8);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.putInt((int) inputFile.length());
            byteBuffer.putInt(cert.length);
            outputFile.write(byteBuffer.array());
            outputFile.seek(outputFile.length());
            outputFile.write(cert);
        }

        System.out.println("签名附加完成");
    }


    // 提取exePath文件签名， 并给sigFilePath目标文件添加签名， 输出至outputPath
    public static void signature(String exePath, String sigFilePath, String outputPath) throws IOException {
        byte[] cert = copyCert(exePath);
        writeCert(cert, sigFilePath, outputPath);
    }



    public static void main(String[] args) throws IOException {
        // 示例调用
        String exePath = "./src/main/java/com/potato/potatotool/content/redTeam/explorer.exe";
        String sigFilePath = "./src/main/java/com/potato/potatotool/content/redTeam/123.exe";
        String outputPath = "./src/main/java/com/potato/potatotool/content/redTeam/345.exe";
        String signPath= "./src/main/java/com/potato/potatotool/content/redTeam/345.sign";

//        // 检查签名
//        checkSig(outputPath);

//        // 输出签名文件
//        outputCert(exePath, signPath);

//        // 签名文件签名
//        signFile(sigFilePath, signPath, outputPath);

//        // 移除签名
//        truncate(outputPath, outputPath+".exe");

        // 添加签名
        signature(exePath, sigFilePath, outputPath);
    }
}