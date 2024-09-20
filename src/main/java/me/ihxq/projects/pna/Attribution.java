package me.ihxq.projects.pna;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 归属信息
 *
 * @author xq.h
 * 2019/10/18 20:58
 **/
@Data
@AllArgsConstructor
@Builder
public class Attribution {
    /**
     * 省份
     */
    private String province;
    /**
     * 城市
     */
    private String city;
    /**
     * 邮政编码
     */
    private String zipCode;
    /**
     * 区号
     */
    private String areaCode;
}
