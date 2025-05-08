package cn.bitlinks.ems.module.power.dal.dataobject.measurementdevice;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;

/**
 * 计量器具上级设备配置 DO
 *
 * @author bitlinks
 */
@TableName("power_measurement_device")
@KeySequence("power_measurement_device_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasurementDeviceDO extends BaseDO {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 计量器具id
     */
    private Long measurementInstrumentId;
    /**
     * 关联设备
     */
    private Long deviceId;

}