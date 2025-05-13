package cn.bitlinks.ems.module.power.dal.dataobject.warninginfo;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 告警信息 DO
 *
 * @author bitlinks
 */
@TableName("power_warning_info")
@KeySequence("power_warning_info_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarningInfoDO extends BaseDO {

    /**
     * 编号
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 告警等级：紧急4 重要3 次要2 警告1 提示0
     */
    private Integer level;
    /**
     * 用户id （每条告警信息内容与收件人有关，暂不拆出）
     */
    private Long userId;
    /**
     * 告警时间
     */
    private LocalDateTime warningTime;
    /**
     * 处理状态:0-未处理1-处理中2-已处理
     */
    private Integer status;
    /**
     * 设备名称与编号
     */
    private String deviceRel;
    /**
     * 模板id
     */
    private Long templateId;
    /**
     * 策略id
     */
    private Long strategyId;
    /**
     * 标题
     */
    private String title;
    /**
     * 告警内容
     */
    private String content;

}