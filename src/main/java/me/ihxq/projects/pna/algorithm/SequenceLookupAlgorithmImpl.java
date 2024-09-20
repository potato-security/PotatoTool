package me.ihxq.projects.pna.algorithm;

import lombok.extern.slf4j.Slf4j;
import me.ihxq.projects.pna.Attribution;
import me.ihxq.projects.pna.ISP;
import me.ihxq.projects.pna.PhoneNumberInfo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * @author xq.h
 * 2019/10/19 00:12
 **/
@Slf4j
public class SequenceLookupAlgorithmImpl implements LookupAlgorithm {
    private ByteBuffer originalByteBuffer;
    private int indicesOffset;

    @Override
    public void loadData(byte[] data) {
        originalByteBuffer = ByteBuffer.wrap(data).asReadOnlyBuffer();
        originalByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        //noinspection unused
        int dataVersion = originalByteBuffer.getInt();
        indicesOffset = originalByteBuffer.getInt(4);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Optional<PhoneNumberInfo> lookup(String phoneNo) {
        ByteBuffer byteBuffer = originalByteBuffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        log.trace("try resolve attribution of: {}", phoneNo);
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

        for (int i = indicesOffset; i < byteBuffer.limit(); i = i + 8 + 1) {

            byteBuffer.position(i);
            int phonePrefix = byteBuffer.getInt();
            int infoStart = byteBuffer.getInt();
            byte ispMark = byteBuffer.get();
            if (phonePrefix == attributionIdentity) {
                ISP isp = ISP.of(ispMark).orElse(ISP.UNKNOWN);
                byteBuffer.position(infoStart);
                //noinspection StatementWithEmptyBody
                while ((byteBuffer.get()) != 0) {
                }
                int infoEnd = byteBuffer.position() - 1;
                byteBuffer.position(infoStart);
                int length = infoEnd - infoStart;
                byte[] bytes = new byte[length];
                byteBuffer.get(bytes, 0, length);
                String oriString = new String(bytes, StandardCharsets.UTF_8);
                String[] split = oriString.split("\\|");
                Attribution build = Attribution.builder()
                        .province(split[0])
                        .city(split[1])
                        .zipCode(split[2])
                        .areaCode(split[3])
                        .build();
                return Optional.of(new PhoneNumberInfo(phoneNo, build, isp));
            }
        }
        return Optional.empty();
    }
}
