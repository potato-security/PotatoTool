package me.ihxq.projects.pna;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 电话号码信息
 *
 * @author xq.h
 * 2019/10/18 20:54
 **/
@Data
@AllArgsConstructor
public class PhoneNumberInfo {
    /**
     * 号码
     */
    private String number;
    /**
     * 归属地信息
     */
    private Attribution attribution;
    /**
     * 运营商
     */
    private ISP isp;
}
