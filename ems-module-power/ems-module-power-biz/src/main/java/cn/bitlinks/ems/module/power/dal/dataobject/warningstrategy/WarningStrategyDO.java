package cn.bitlinks.ems.module.power.dal.dataobject.warningstrategy;

import cn.bitlinks.ems.framework.common.enums.CommonStatusEnum;
import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 告警策略 DO
 *
 * @author bitlinks
 */
@TableName("power_warning_strategy")
@KeySequence("power_warning_strategy_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarningStrategyDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 规则名称
     */
    private String name;
    /**
     * 描述
     */
    private String description;
    /**
     * 设备范围
     */
    private String deviceScope;
    /**
     * 设备分类范围
     */
    private String deviceTypeScope;
    /**
     * 告警条件
     */
    @TableField(value = "`condition`")
    private String condition;
    /**
     * 告警等级：紧急4 重要3 次要2 警告1 提示0
     * <p>
     * 枚举 {@link TODO warning_level 对应的类}
     */
    @TableField(value = "`level`")
    private Integer level;
    /**
     * 站内信模板id
     */
    private Long siteTemplateId;
    /**
     * 邮件模板id
     */
    private Long mailTemplateId;
    /**
     * 站内信人员
     */
    private String siteStaff;
    /**
     * 邮件人员
     */
    private String mailStaff;
    /**
     * 公共人员
     */
    private String commonStaff;
    /**
     * 告警间隔
     */
    @TableField(value = "`interval`")
    private String interval;

    /**
     * 状态
     *
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;

    /**
     * 告警间隔单位
     * <p>
     * 枚举 {@link TODO warning_interval_unit 对应的类}
     */
    private Integer intervalUnit;

}