package cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 设备数据触发接口参数VO，多个设备
 */
@Data
public class SbDataTriggerVO {

    @NotBlank(message = "设备编码不能为空")
    private String sbCode;

    @NotBlank(message = "设备参数编码不能为空")
    private String paramCode;

    @NotNull(message = "设备参数值不能为空")
    private String value;

    @NotNull(message = "设备数据采集时间不能为空")
    private LocalDateTime dataTime;

}
