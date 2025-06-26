package cn.bitlinks.ems.module.power.dal.dataobject.additionalrecording;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    @TableId
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
     * 增量/全量
     */
    private Integer valueType;
    /**
     * 上次采集时间（增量补录的时间段）
     */
    private LocalDateTime preCollectTime;

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
     */
    private Integer recordMethod;
    /**
     * 录入时间
     */
    private LocalDateTime enterTime;

}