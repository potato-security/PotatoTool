package me.ihxq.projects.pna.algorithm;

import me.ihxq.projects.pna.PhoneNumberInfo;

import java.util.Optional;

/**
 * 查找算子
 *
 * @author xq.h
 * on 2019/10/18 21:24
 **/
public interface LookupAlgorithm {

    /**
     * 装载数据.
     *
     * @param data 来自phone.dat
     */
    void loadData(byte[] data);

    /**
     * 根据电话号码查找归属地
     * @param phoneNumber 电话号码, 11位或前7位
     * @return 电话号码归属信息
     */
    Optional<PhoneNumberInfo> lookup(String phoneNumber);

}
