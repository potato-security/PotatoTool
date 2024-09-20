package me.ihxq.projects.pna.algorithm;

import lombok.extern.slf4j.Slf4j;
import me.ihxq.projects.pna.Attribution;
import me.ihxq.projects.pna.ISP;
import me.ihxq.projects.pna.PhoneNumberInfo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author xq.h
 * 2019/10/19 00:12
 **/
@Slf4j
public class AnotherBinarySearchAlgorithmImpl implements LookupAlgorithm {
    private ByteBuffer originalByteBuffer;
    private int indicesStartOffset;
    private int indicesEndOffset;

    @Override
    public void loadData(byte[] data) {
        originalByteBuffer = ByteBuffer.wrap(data).asReadOnlyBuffer();
        originalByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        //noinspection unused
        int dataVersion = originalByteBuffer.getInt();
        indicesStartOffset = originalByteBuffer.getInt(4);
        indicesEndOffset = originalByteBuffer.limit();
    }

    /**
     * 对齐
     */
    private int alignPosition(int pos) {
        int remain = (pos - indicesStartOffset) % 9;
        if (pos - indicesStartOffset < 9) {
            return pos - remain;
        } else if (remain != 0) {
            return pos + 9 - remain;
        } else {
            return pos;
        }
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Optional<PhoneNumberInfo> lookup(String phoneNo) {
        log.trace("try to resolve attribution of: {}", phoneNo);
        ByteBuffer byteBuffer = originalByteBuffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        if (phoneNo == null) {
            log.debug("phoneNo is null");
            return Optional.empty();
        }
        int phoneNoLength = phoneNo.length();
        if (phoneNoLength < 7 || phoneNoLength > 11) {
            log.debug("phoneNo {} is not acceptable, length invalid, length should range 7 to 11, actual: {}",
                    phoneNo, phoneNoLength);
            return Optional.empty();
        }

        int attributionIdentity;
        try {
            attributionIdentity = Integer.parseInt(phoneNo.substring(0, 7));
        } catch (NumberFormatException e) {
            log.debug("phoneNo {} is invalid, is it numeric?", phoneNo);
            return Optional.empty();
        }
        int left = indicesStartOffset;
        int right = indicesEndOffset;
        int mid = (left + right) / 2;
        mid = alignPosition(mid);
        while (mid >= left && mid <= right) {
            if (mid == right) {
                return Optional.empty();
            }
            int compare = compare(mid, attributionIdentity, byteBuffer);
            if (compare == 0) {
                break;
            }
            if (mid == left) {
                return Optional.empty();
            }

            if (compare > 0) {
                int tempMid = (mid + left) / 2;
                tempMid = alignPosition(tempMid);
                right = mid;
                int remain = (tempMid - indicesStartOffset) % 9;
                if (tempMid - indicesStartOffset < 9) {
                    mid = tempMid - remain;
                    continue;
                }
                if (remain != 0) {
                    mid = tempMid + 9 - remain;
                } else {
                    mid = tempMid;
                }
            } else {
                int tempMid = (mid + right) / 2;
                tempMid = alignPosition(tempMid);
                left = mid;
                int remain = (tempMid - indicesStartOffset) % 9;
                if (tempMid - indicesStartOffset < 9) {
                    mid = tempMid - remain;
                    continue;
                }
                if (remain != 0) {
                    mid = tempMid + 9 - remain;
                } else {
                    mid = tempMid;
                }
            }
        }

        byteBuffer.position(mid);
        //noinspection unused
        int prefix = byteBuffer.getInt();
        int infoStartIndex = byteBuffer.getInt();
        byte ispMark = byteBuffer.get();
        Optional<ISP> isp = ISP.of(ispMark);
        byteBuffer.position(infoStartIndex);
        int resultBufferSize = 200;
        int increase = 100;
        byte[] bytes = new byte[resultBufferSize];
        byte b;
        int i;
        for (i = 0; (b = byteBuffer.get()) != 0; i++) {
            bytes[i] = b;
            if (i == resultBufferSize - 1) {
                resultBufferSize = resultBufferSize + increase;
                bytes = Arrays.copyOf(bytes, resultBufferSize);
            }
        }
        String oriString = new String(bytes, 0, i, StandardCharsets.UTF_8);
        String[] split = oriString.split("\\|");
        Attribution build = Attribution.builder()
                .province(split[0])
                .city(split[1])
                .zipCode(split[2])
                .areaCode(split[3])
                .build();
        return Optional.of(new PhoneNumberInfo(phoneNo, build, isp.orElse(ISP.UNKNOWN)));
    }

    private int compare(int position, int key, ByteBuffer byteBuffer) {
        byteBuffer.position(position);
        int phonePrefix;
        try {
            phonePrefix = byteBuffer.getInt();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Integer.compare(phonePrefix, key);
    }
}
