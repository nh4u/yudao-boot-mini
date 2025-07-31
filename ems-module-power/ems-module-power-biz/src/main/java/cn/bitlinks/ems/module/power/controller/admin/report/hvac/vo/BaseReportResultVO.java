package cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;


@Schema(description = "管理后台 - 报表统计结果 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class BaseReportResultVO<T> {

    /**
     * report数据信息
     */
    private List<T> reportDataList;

    /**
     * 表头
     */
    private List<String> header;

    /**
     * 数据最后更新时间
     */
    private LocalDateTime dataTime;


}
