package cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Schema(description = "RPC 服务 - 实时数据")
@Data
public class CollectRawDataDTO implements Serializable {
    private static final long serialVersionUID = 1L; // 推荐指定序列化版本
    /**
     * OPC_DA:IO地址/
     */
    private String dataSite;
    /**
     * 数据同步时间
     */
    private LocalDateTime syncTime;
    /**
     * 参数 code
     */
    private String paramCode;
    /**
     * 是否能源数采参数 0自定义 1能源参数
     */
    private Boolean energyFlag;
    /**
     * 台账id
     */
    private Long standingbookId;
    /**
     * 公式计算值
     */
    private String calcValue;
    /**
     * 采集值（原始）
     */
    private String rawValue;
    /**
     * 数据采集时间（原始）
     */
    private LocalDateTime collectTime;
}
