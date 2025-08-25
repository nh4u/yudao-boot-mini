package cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;


/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 用能统计结果图 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeeChartResultVO<T> {

    /**
     * y数据
     */
    private List<T> ydata;


    /**
     * 表头
     */
    private List<String> xdata;

    /**
     * 数据最后更新时间
     */
    private LocalDateTime dataTime;
}
