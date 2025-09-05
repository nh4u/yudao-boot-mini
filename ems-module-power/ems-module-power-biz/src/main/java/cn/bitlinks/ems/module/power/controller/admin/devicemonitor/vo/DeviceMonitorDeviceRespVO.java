package cn.bitlinks.ems.module.power.controller.admin.devicemonitor.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 设备监控-设备名片 Response VO")
@Data
@ExcelIgnoreUnannotated
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DeviceMonitorDeviceRespVO {
    @Schema(description = "能耗状态 0 正常 1 异常")
    private Integer status;
    @Schema(description = "设备id")
    private Long sbId;
    @Schema(description = "设备名称")
    private String name;
    @Schema(description = "设备编号")
    private String code;
    @Schema(description = "图片")
    private String image;
    @Schema(description = "动态标签列表")
    private List<DeviceMonitorDeviceLabel> labels;
}

