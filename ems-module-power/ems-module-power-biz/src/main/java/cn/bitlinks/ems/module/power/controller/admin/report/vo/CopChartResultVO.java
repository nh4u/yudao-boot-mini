package cn.bitlinks.ems.module.power.controller.admin.report.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/06/22 16:48
 **/

@Schema(description = "管理后台 - cop chart VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CopChartResultVO {

    /**
     * y轴
     */
    private CopChartYData ydata;


    /**
     * 表头
     */
    private List<String> xdata;
}
