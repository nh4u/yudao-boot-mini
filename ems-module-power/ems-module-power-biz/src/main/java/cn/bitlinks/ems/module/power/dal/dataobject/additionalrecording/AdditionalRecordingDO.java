package cn.bitlinks.ems.module.power.dal.dataobject.additionalrecording;

import com.sun.xml.bind.v2.TODO;
import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;

/**
 * 补录 DO
 *
 * @author bitlinks
 */
@TableName("ems_additional_recording")
@KeySequence("ems_additional_recording_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalRecordingDO extends BaseDO {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 凭证id
     */
    private Long voucherId;
    /**
     * 计量器具id
     */
    private Long standingbookId;
    /**
     * 数值类型
     */
    private String valueType;
    /**
     * 上次采集时间
     */
    private LocalDateTime lastCollectTime;
    /**
     * 上次数值
     */
    private BigDecimal lastValue;
    /**
     * 本次采集时间
     */
    private LocalDateTime thisCollectTime;
    /**
     * 本次数值
     */
    private BigDecimal thisValue;
    /**
     * 补录人
     */
    private String recordPerson;
    /**
     * 补录原因
     */
    private String recordReason;
    /**
     * 补录方式
     *
     * 枚举 {@link TODO record_method 对应的类}
     */
    private Integer recordMethod;
    /**
     * 录入时间
     */
    private LocalDateTime enterTime;
}