package com.potato.potatotool.utils.crypto;

import com.potato.potatotool.utils.core.ExecutorServiceManager;
import com.potato.potatotool.utils.data.DeserializerUtils;
import com.potato.potatotool.utils.data.GzipUtils;
import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.misc.ReadabilityChecker;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.core.Constants.getResourceStream;

/**
 * @author Potato
 * @date 2023/4/19 18:23
 */

public class AESUtils {
    private static final int IV_SIZE = 16;
    private static final String CIPHER_ALGORITHM = "AES/%s/%s";
    public static final String AES_MODE_CBC = "CBC";
    public static final String AES_MODE_ECB = "ECB";
    public static final String AES_MODE_GCM = "GCM";
    public static final String AES_MODE_CFB = "CFB";
    public static final String AES_MODE_OFB = "OFB";
    public static final String AES_MODE_CTR = "CTR";
    public static final String PADDING_NO_PADDING = "NoPadding";
    public static final String PADDING_PKCS5_PADDING = "PKCS5Padding";
    public static final String PADDING_PKCS7_PADDING = "PKCS7Padding";
    public static final String PADDING_ZERO_PADDING = "ZeroBytePadding";
    /**
     * 对输入的明文进行AES加密
     *
     * @param plainText 密文byte数组
     * @param keyBytes  密钥byte数组
     * @param iv        iv向量byte数组
     * @param mode      加密模式（如CBC、ECB、GCM等）
     * @param padding   填充方式（如NoPadding、PKCS7Padding、ZeroBytePadding等）
     * @return 加密后的密文字符串
     * @throws Exception 加密过程中的异常
     */
    public static byte[] encrypt(byte[] plainText, byte[] keyBytes,byte[] iv, String mode, String padding) throws Exception {
        validateMode(mode);
        validatePadding(padding);

        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance(String.format(CIPHER_ALGORITHM, mode, padding), "BC");
        if (mode.equals(AES_MODE_ECB) || mode.equals(AES_MODE_CTR)) {

            // ECB及CTR不需要传入iv
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        } else if (mode.equals(AES_MODE_GCM)) {

            // GCM解密可自动提取iv，故不需要固定，自动生成
            byte[] ivTmp = generateRandomBytes(IV_SIZE);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(IV_SIZE * 8, ivTmp);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            byte[] encrypted = cipher.doFinal(plainText);
            byte[] cipherText = new byte[IV_SIZE + encrypted.length];
            System.arraycopy(ivTmp, 0, cipherText, 0, IV_SIZE);
            System.arraycopy(encrypted, 0, cipherText, IV_SIZE, encrypted.length);
            return cipherText;

        } else if (mode.equals(AES_MODE_CBC) || mode.equals(AES_MODE_CFB) || mode.equals(AES_MODE_OFB)) {

            //  该模式必须传入iv值，且解密iv同步
            if(iv==null||iv.length==0){
                throw new IllegalArgumentException("该模式必须传入iv值");
            }
            IvParameterSpec parameterSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        }

        return cipher.doFinal(plainText);
    }

    /**
     * 对输入的密文进行AES解密
     *
     * @param cipherText 密文byte数组
     * @param key        密钥byte数组
     * @param iv         iv向量byte数组
     * @param mode       加密模式（如CBC、ECB、GCM等）
     * @param padding    填充方式（如NoPadding、PKCS7Padding、ZeroBytePadding等）
     * @return 解密后的明文byte数组
     * @throws Exception 解密过程中的异常
     */
    public boolean classCode = false;
    public boolean serializeCode = false;
    public boolean gzipCode = false;
    public byte[] decrypt(byte[] cipherText, byte[] key, byte[] iv, String mode, String padding) throws Exception {
        try {
            validateMode(mode);
            validatePadding(padding);

            if(key==null){
                return null;
            }

            // CryptoJS的cipherText，需要拆分出来新cipherText和iv
            if (new String(cipherText, 0, 8).equals("Salted__")) {
                byte[] salt = Arrays.copyOfRange(cipherText, 8, 16);
                cipherText = Arrays.copyOfRange(cipherText, 16, cipherText.length);
                byte[] keyAndIv = deriveKeyAndIv(key, salt, 32, 16);
                key = Arrays.copyOfRange(keyAndIv, 0, 32);
                iv = Arrays.copyOfRange(keyAndIv, 32, 48);
            }

            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance(String.format(CIPHER_ALGORITHM, mode, padding), "BC");
            if (mode.equals(AES_MODE_ECB) || mode.equals(AES_MODE_CTR)) {

                //  ECB和CTR不需要传输iv
                cipher.init(Cipher.DECRYPT_MODE, secretKey);

            } else if (mode.equals(AES_MODE_GCM)) {

                //  GCM不需要传输iv，可以提取出iv
                byte[] ivBytes = new byte[IV_SIZE];
                System.arraycopy(cipherText, 0, ivBytes, 0, IV_SIZE);
                GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(IV_SIZE * 8, ivBytes);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
                byte[] encrypted = new byte[cipherText.length - IV_SIZE];
                System.arraycopy(cipherText, IV_SIZE, encrypted, 0, encrypted.length);
                cipherText = encrypted;
            } else if (mode.equals(AES_MODE_CBC) || mode.equals(AES_MODE_CFB) || mode.equals(AES_MODE_OFB)) {

                //  必须手动传入iv
                if (iv == null || iv.length == 0) {
                    throw new IllegalArgumentException("该模式必须传入iv值");
                }
                IvParameterSpec parameterSpec = new IvParameterSpec(iv);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            }

            byte[] decryptedTextBytes = cipher.doFinal(cipherText);

            //是否存在Gzip压缩特征
            if(StrUtils.byteStartsWith(decryptedTextBytes, 0, new byte[]{(byte) 0x1F, (byte) 0x8B})) {
                decryptedTextBytes = GzipUtils.GzipDecompress(decryptedTextBytes);

                if(StrUtils.byteArrayContains(decryptedTextBytes, new byte[]{0, 0, 0}) != -1 && !StrUtils.byteStartsWith(decryptedTextBytes, 0, new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}) && !StrUtils.byteStartsWith(decryptedTextBytes, 0, new byte[]{(byte) 0xAC, (byte) 0xED, 0x00, 0x05}) ){  // 针对于哥斯拉key和value空字符需要转换为等号
                    decryptedTextBytes = StrUtils.byteReplaceZeroToD3(decryptedTextBytes);
                }

                gzipCode = true;
            }

            // 检查是否存在class/反序列化
            byte[] tmpDecryptedTextBytes = DeserializerUtils.classDataCheck(decryptedTextBytes);
            byte[] tmpSerDecryptedTextBytes = null;
            if (tmpDecryptedTextBytes == null) {
                tmpSerDecryptedTextBytes = DeserializerUtils.serializeCheck(decryptedTextBytes);
            } else {
                decryptedTextBytes = tmpDecryptedTextBytes;
                classCode = true;
            }

            if (tmpSerDecryptedTextBytes != null) {
                decryptedTextBytes = tmpSerDecryptedTextBytes;
                serializeCode = true;
            }

            boolean readability = ReadabilityChecker.assessReadability(decryptedTextBytes);

            return (!classCode && !serializeCode && !readability) ? null : decryptedTextBytes;

        }catch (Exception e){
            if(debugMode)e.printStackTrace();
            return null;
        }
    }

    // 根据密码和 salt 生成密钥和 IV
    public static byte[] deriveKeyAndIv(byte[] password, byte[] salt, int keyLength, int ivLength) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] keyAndIv = new byte[keyLength + ivLength];
        byte[] previous = new byte[0];

        int i = 0;
        while (i < keyLength + ivLength) {
            md5.update(previous);
            md5.update(password);
            md5.update(salt);

            byte[] hash = md5.digest();
            int remaining = keyLength + ivLength - i;
            System.arraycopy(hash, 0, keyAndIv, i, Math.min(remaining, hash.length));
            i += hash.length;

            previous = hash;
        }

        return keyAndIv;
    }

    // 验证加密模式是否合法
    private static void validateMode(String mode) {
        if (!mode.equals(AES_MODE_CBC) && !mode.equals(AES_MODE_ECB) && !mode.equals(AES_MODE_GCM)
                && !mode.equals(AES_MODE_CFB) && !mode.equals(AES_MODE_OFB)) {
            throw new IllegalArgumentException("无效的 AES 模式");
        }
    }

    // 验证填充方式是否合法
    private static void validatePadding(String padding) {
        if (!padding.equals(PADDING_NO_PADDING) && !padding.equals(PADDING_PKCS5_PADDING) && !padding.equals(PADDING_PKCS7_PADDING) && !padding.equals(PADDING_ZERO_PADDING)) {
            throw new IllegalArgumentException("无效的 padding 填充方式");
        }
    }


    // 生成随机字节
    public static byte[] generateRandomBytes(int size) {
        byte[] randomBytes = new byte[size];
        new SecureRandom().nextBytes(randomBytes);
        return randomBytes;
    }


    //  AtomicReference<>原子性更新，多线程环境下安全的类型
    public AtomicReference<String> mode_AES = new AtomicReference<>("");
    public AtomicReference<String> padding_AES = new AtomicReference<>("");
    public AtomicReference<String> key_AES = new AtomicReference<>("");
    public AtomicReference<String> iv_AES = new AtomicReference<>("Null");
    public Set<String> keyArray_AES = new LinkedHashSet<>();
    //  50w字典爆破调用方法：
    //  AESUtils aes=new AESUtils();
    //  String res = aes.aesWebShellDecode(encodeStr, null,true);
    /**
     *  AES解密尝试【兼容+Gzip】     放了3个常见key，两种常见iv,两个常见mode CBC/ECB，填充方式PKCS5Padding
     * @param conText       原始字符串
     * @param inputKeyStr   指定key的值，NULL时尝试webShell最常见的3个key
     * @param traverse      [可不传]调用50w字典进行爆破，将忽略inputKeyStr传入值
     * @param customPath    自定义字典路径
     * @return              解密后结果--最好返回byte[]数据，而非string，防止后续传输存在问题
     * @throws Exception
     */
    public byte[] aesWebShellDecode(String conText, String inputKeyStr, String inputIv, List traverse, String customPath) {

        // 排除非AES加密格式字符串传入
        String aesPattern = "^[A-Za-z0-9+/]+={0,2}$";
        Pattern pattern = Pattern.compile(aesPattern);
        Matcher matcher = pattern.matcher(conText.replace("\n","").replace("\r","").replace("\t",""));

        if (!matcher.matches()) {
            return conText.getBytes(StandardCharsets.UTF_8);
        }

        byte[] res = null;
        Set<String> keyArray = new LinkedHashSet<>();


        if(traverse.contains("AES")){
            if(customPath != null && !customPath.equals("")){
                try (BufferedReader reader = new BufferedReader(new FileReader(customPath))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        keyArray.add(line);
                    }
                    keyArray_AES = keyArray;
                } catch (Exception e) {
                    if(debugMode)e.printStackTrace();
                }
            }else {
                if(keyArray_AES.isEmpty()){ // 优先读取缓存数据
                    try (InputStream aesKeyInputStream = getResourceStream("aesKey");
                         BufferedReader reader = new BufferedReader(new InputStreamReader(aesKeyInputStream, StandardCharsets.UTF_8))) {

                        String line;
                        while ((line = reader.readLine()) != null) {
                            keyArray.add(line);
                        }
                        keyArray_AES = keyArray;
                    } catch (Exception e) {
                        if(debugMode)e.printStackTrace();
                    }
                }else {
                    keyArray = keyArray_AES;
                }
            }
        } else if( inputKeyStr == null ){
            //  webShell常见默认秘钥
            keyArray.add("e45e329feb5d925b");
            keyArray.add("3c6e0b8a9c15224a");
            keyArray.add("1a1dc91c907325c6");
            keyArray.add("ab645dd196197df7");
            keyArray.add("5f4dcc3b5aa765d6");
            keyArray.add("changeit");
            keyArray.add("whir2014");
            keyArray.add("1234567890123456");
        } else if( inputKeyStr != null ){
            keyArray.add(inputKeyStr);
            // 为符合部分加解密方案，强行指定key为16位
            if(inputKeyStr.length() < 16){
                byte[] keyBytes = new byte[16];
                byte[] originalKeyBytes = inputKeyStr.getBytes();
                System.arraycopy(originalKeyBytes, 0, keyBytes, 0, Math.min(originalKeyBytes.length, keyBytes.length));
                keyArray.add(new String(keyBytes));
            }else if (inputKeyStr.length() > 16){
                keyArray.add(inputKeyStr.substring(0, 16));
            }
        }

        String[] modeArray = {"CBC", "ECB"};    //  webShell常见两种模式
        String[] paddingArray = {PADDING_PKCS5_PADDING, PADDING_ZERO_PADDING}; // 数据加密常见的两种padding

        String poolName = ExecutorServiceManager.ExecutorPoolNames.AES_DECRYPT;
        ExecutorService executor = ExecutorServiceManager.getOrCreateExecutor(poolName);
        List<CompletableFuture<byte[]>> futures = new ArrayList<>();

        for (String keyStr : keyArray) {
            for (String mode : modeArray) {
                for (String padding : paddingArray) {
                    for (int i = 0; i < 2; i++) {
                        if (i == 1 && mode.equals("ECB")) continue;

                        int finalI = i;
                        CompletableFuture<byte[]> future = CompletableFuture.supplyAsync(() -> {
                            try {
                                byte[] encryptData = StrUtils.base64Decode(conText.getBytes(StandardCharsets.UTF_8));
                                byte[] key = keyStr.getBytes(StandardCharsets.UTF_8);
                                byte[] iv = inputIv != null ? inputIv.getBytes(StandardCharsets.UTF_8) : finalI == 0 ? new byte[16] : key;

                                AESUtils aes = new AESUtils();
                                byte[] result = aes.decrypt(
                                        encryptData,
                                        key,
                                        iv,
                                        mode,
                                        padding
                                );
                                if(result != null && !result.equals("")){
                                    mode_AES.set(mode);
                                    if (new String(encryptData, 0, 8).equals("Salted__")) {
                                        mode_AES.set("(CryptoJS)\\" + mode);
                                    }
                                    padding_AES.set(padding);
                                    key_AES.set(keyStr);
                                    iv_AES.set(!iv.equals("Null") ? (mode.equals("ECB") ? "Null" : new String(iv, StandardCharsets.UTF_8)) : "Null");
                                    classCode = aes.classCode;
                                    serializeCode = aes.serializeCode;

                                    // 停止所有线程
                                    ExecutorServiceManager.shutdownExecutor(poolName);
                                }

                                return result;
                            } catch (Exception e) {
                                if (debugMode) e.printStackTrace();
                                return null;
                            }
                        }, executor);

                        futures.add(future);
                    }
                }
            }
        }

        for (Future<?> future : futures) {
            try {
                byte[] result = (byte[]) future.get();
                if (result != null && !result.equals("")) {
                    res = result;
                    break;
                }
            } catch (Exception e) {
                if(debugMode)e.printStackTrace();
            }
        }

        // 停止所有线程
        ExecutorServiceManager.shutdownExecutor(poolName);

        return res==null ? conText.getBytes(StandardCharsets.UTF_8) : res;
    }

    public static String encryptLocalConfig(String data){
        String res3 = null;
        try {
            String res1 = StrUtils.strRev(data);
            String res2 = StrUtils.base64Encode(new AESUtils().encrypt(res1.getBytes(StandardCharsets.UTF_8), ("PotatoTool"+"Is"+"Good").getBytes(StandardCharsets.UTF_8), ("ILikeYou"+"ILikeYou").getBytes(StandardCharsets.UTF_8), AES_MODE_CBC, PADDING_PKCS5_PADDING));
            res3 = StrUtils.strRev(res2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res3;
    }

    public static String decryptLocalConfig(String data){
        String res3 = null;
        try {
            String res1 = StrUtils.strRev(data);
            String res2 = new String((new AESUtils()).decrypt((new StrUtils()).base64Decode(res1.getBytes(StandardCharsets.UTF_8)), ("PotatoTool"+"Is"+"Good").getBytes(StandardCharsets.UTF_8), ("ILikeYou"+"ILikeYou").getBytes(StandardCharsets.UTF_8), AES_MODE_CBC, PADDING_PKCS5_PADDING), StandardCharsets.UTF_8);
            res3 = StrUtils.strRev(res2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res3;
    }

    public static void main(String []args) {
        SecurityInitializer.initializeSecurityProvider();
        System.out.println(encryptLocalConfig("http://192.168.4.69:2444"));
//        byte[] encryptData = StrUtils.base64Decode("qK+uRdRsYAa2jdP6kGdhEg==".getBytes(StandardCharsets.UTF_8));
//        byte[] key = "1234567890123456".getBytes(StandardCharsets.UTF_8);
//        byte[] iv = key;//new byte[16];//"1234567890123456".getBytes(StandardCharsets.UTF_8);//inputIv!=null ? inputIv.getBytes(StandardCharsets.UTF_8) : finalI == 0 ? new byte[16] : key;
//
//        AESUtils aes = new AESUtils();
//        try {
//            byte[] result = aes.decrypt(
//                    encryptData,
//                    key,
//                    iv,
//                    "CBC",
//                    PADDING_NO_PADDING
//            );
//            System.out.println("result:"+new String(result, StandardCharsets.UTF_8));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    /**
     * 知识点记录
     *
     * 【NoPadding】
     * 描述: NoPadding 不会对数据进行任何填充操作。
     * 要求: 数据长度必须是加密算法块大小的整数倍。如果不是整数倍，数据将无法正确加密。
     * 使用场景: 适用于数据已经按照块大小分组的场景，比如一些固定格式的数据（图像、音频等）。
     * 【ZeroBytePadding】
     * 描述: ZeroBytePadding 会在数据的末尾填充零字节（0x00）直到数据长度达到块大小的整数倍。
     * 要求: 如果原始数据包含 0x00 字节，解密时可能会产生混淆，因为无法区分哪些零字节是填充的，哪些是数据的一部分。
     * 使用场景: 适合于填充零字节不会影响数据内容的场景。
     *
     *
     * 【是否可以使用 ZeroBytePadding 解密 NoPadding 加密的密文？】
     * 数据块完整: 如果使用 NoPadding 加密的数据长度已经是块大小的整数倍（例如 16 字节、32 字节等），那么可以直接解密，不需要填充，也不需要去除填充。因此，这种情况下，ZeroBytePadding 和 NoPadding 的解密方式不会产生冲突，可以用 ZeroBytePadding 解密。
     *
     * 数据长度非整数倍: 如果数据长度不是块大小的整数倍，不能直接使用 NoPadding 加密，因为它不会进行任何填充，数据长度必须自己处理成块大小的整数倍。在这种情况下，NoPadding 和 ZeroBytePadding 无法互相替代。
     * 但是数据长度非整数倍的场景本身应该不会存在，因为 NoPadding 要求数据长度是 16 字节的倍数，故不存在该场景。
     *
     * 填充数据的干扰: 使用 NoPadding 加密的数据不会有任何填充，而 ZeroBytePadding 解密会试图将末尾的零字节移除作为填充。因此，如果原始数据末尾刚好是 0x00 字节，在解密过程中，ZeroBytePadding 可能会错误地将其识别为填充并移除，导致数据不完整。
     *
     * 由于我们是数据解密，故不需要考虑填充数据的干扰。
     * 综上所述，【可以使用 ZeroBytePadding 解密 NoPadding 加密的密文】
     *
     **/

}
