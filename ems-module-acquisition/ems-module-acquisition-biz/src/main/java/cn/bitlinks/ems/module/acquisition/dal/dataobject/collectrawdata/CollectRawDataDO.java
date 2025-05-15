package cn.bitlinks.ems.module.acquisition.dal.dataobject.collectrawdata;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 实时数据
 */
@TableName(value = "collect_raw_data", autoResultMap = true)
@Data
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectRawDataDO {
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
     * 参数类型
     */
    private Integer paramType;
    /**
     * 台账id
     */
    private String standingbookId;
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
    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
