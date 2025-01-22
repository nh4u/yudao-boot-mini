package cn.bitlinks.ems.module.power.dal.dataobject.deviceassociationconfiguration;

import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;

/**
 * 设备关联配置 DO
 *
 * @author bitlinks
 */
@TableName("ems_device_association_configuration")
@KeySequence("ems_device_association_configuration_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAssociationConfigurationDO extends BaseDO {

    /**
     * id
     */
    @TableId
    @Schema(description = "id")
    @ExcelProperty("id")
    private Long id;
    /**
     * 能源id
     */
    @Schema(description = "能源id")
    @ExcelProperty("能源id")
    private Long energyId;
    /**
     * 计量器具id
     */
    @Schema(description = "计量器具id")
    @ExcelProperty("计量器具id")
    private Long measurementInstrumentId;
    /**
     * 关联下级计量
     */
    @Schema(description = "关联下级计量")
    @ExcelProperty("关联下级计量")
    private String measurementIds;
    /**
     * 关联设备
     */
    @Schema(description = "关联设备")
    @ExcelProperty("关联设备")
    private Long deviceId;

}