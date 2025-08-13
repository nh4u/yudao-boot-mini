package cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 供水温度统计结果 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class SupplyWaterTmpTableResultVO {

    /**
     * 统计信息
     */
    private List<Map<String, Object>> list;

    /**
     * 数据最后更新时间
     */
    private LocalDateTime dataTime;


}
