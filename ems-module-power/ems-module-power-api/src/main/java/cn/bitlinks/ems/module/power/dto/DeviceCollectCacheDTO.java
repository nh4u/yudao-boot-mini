package cn.bitlinks.ems.module.power.dto;

import cn.bitlinks.ems.framework.common.core.StandingbookAcquisitionDetailDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 台账id->数采映射关系
 */
@Schema(description = "RPC 服务 - 设备数采配置")
@Data
public class DeviceCollectCacheDTO {
    /**
     * 台账id
     */
    @NotNull(message = "台账id不能为空")
    private Long standingbookId;

    /**
     * 任务开始时间
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime jobStartTime;
    /**
     * 采集频率（等同于多少秒）
     */
    @NotNull(message = "采集频率不能为空")
    private Long frequency;

    /**
     * 详情
     */
    private List<StandingbookAcquisitionDetailDTO> details;
    private List<String> dataSites;

}