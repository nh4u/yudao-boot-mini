package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/07/16 16:57
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimeAndNumData {
    /**
     * 时间
     */
    private String time;

    /**
     * 当前用量
     */
    private BigDecimal num;
}
