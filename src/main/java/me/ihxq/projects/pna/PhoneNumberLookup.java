package me.ihxq.projects.pna;

import lombok.extern.slf4j.Slf4j;
import me.ihxq.projects.pna.algorithm.BinarySearchAlgorithmImpl;
import me.ihxq.projects.pna.algorithm.LookupAlgorithm;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * 电话号码归属信息查询
 *
 * @author xq.h
 * 2019/10/18 21:25
 **/
@Slf4j
public class PhoneNumberLookup {
    private static final String PHONE_NUMBER_GEO_PHONE_DAT = "conf/phone.dat";
    private final LookupAlgorithm lookupAlgorithm;
    /**
     * 数据版本hash值, 版本:202302
     */
    private static final int DATA_HASH = 1990190400;

    private void init() {
        try {
            byte[] allBytes;
            try (final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PHONE_NUMBER_GEO_PHONE_DAT);
                 final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                int n;
                byte[] buffer = new byte[1024 * 4];
                while (-1 != (n = requireNonNull(inputStream, "PhoneNumberLookup: Failed to get inputStream.").read(buffer))) {
                    output.write(buffer, 0, n);
                }
                allBytes = output.toByteArray();
            }
            int hashCode = Arrays.hashCode(allBytes);
            log.debug("loaded datasource, size: {}, hash: {}", allBytes.length, hashCode);
            if (hashCode != DATA_HASH) {
                throw new IllegalStateException("Hash of data not match, expect: " + DATA_HASH + ", actually: " + hashCode);
            }
            lookupAlgorithm.loadData(allBytes);
        } catch (Exception e) {
            log.error("failed to init PhoneNumberLookUp", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 使用默认算子
     */
    public PhoneNumberLookup() {
        this(new BinarySearchAlgorithmImpl());
    }

    /**
     * @param lookupAlgorithm 算子
     */
    public PhoneNumberLookup(LookupAlgorithm lookupAlgorithm) {
        this.lookupAlgorithm = lookupAlgorithm;
        init();
    }

    /**
     * @param phoneNumber 电话号码, 11位, 或前7位
     * @return 电话号码归属信息
     */
    public Optional<PhoneNumberInfo> lookup(String phoneNumber) {
        return lookupAlgorithm.lookup(phoneNumber);
    }
}
