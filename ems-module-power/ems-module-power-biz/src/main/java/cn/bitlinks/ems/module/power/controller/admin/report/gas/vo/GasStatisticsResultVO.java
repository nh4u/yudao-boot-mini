package cn.bitlinks.ems.module.power.controller.admin.report.gas.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author bmqi
 * @date 2025年08月07日 14:12
 */
@Schema(description = "管理后台 - 气化科报表统计结果 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class GasStatisticsResultVO<T> {

    /**
     * 统计信息
     */
    private List<T> statisticsInfoList;

    /**
     * 表头
     */
    private List<String> header;

    /**
     * 数据最后更新时间
     */
    private LocalDateTime dataTime;


}
