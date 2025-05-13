package cn.bitlinks.ems.module.power.dal.dataobject.standingbook.acquisition;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;

/**
 * 台账-数采设置 DO
 *
 * @author bitlinks
 */
@TableName("power_standingbook_acquisition")
@KeySequence("power_standingbook_acquisition_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandingbookAcquisitionDO extends BaseDO {

    /**
     * 编号
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 设备数采启停开关（0：关；1开。）
     */
    private Boolean status;
    /**
     * 台账id
     */
    private Long standingbookId;
    /**
     * 采集频率
     */
    private Long frequency;
    /**
     * 采集频率单位(秒、分钟、小时、天)
     */
    private Integer frequencyUnit;
    /**
     * 服务设置id
     */
    private Long serviceSettingsId;
    /**
     * 开始时间
     */
    private LocalDateTime startTime;

}