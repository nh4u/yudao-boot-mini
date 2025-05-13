package cn.bitlinks.ems.module.power.dal.dataobject.warningstrategy;

import cn.bitlinks.ems.framework.common.enums.CommonStatusEnum;
import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import cn.bitlinks.ems.framework.mybatis.core.type.StringListTypeHandler;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.util.List;

/**
 * 告警策略 DO
 *
 * @author bitlinks
 */
@TableName(value = "power_warning_strategy", autoResultMap = true)
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
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 规则名称
     */
    private String name;
    /**
     * 描述
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;
    /**
     * 设备范围
     */
    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private List<Long> deviceScope;
    /**
     * 设备分类范围
     */
    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private List<Long> deviceTypeScope;

//    /**
//     * 触发告警的参数编码集合-冗余字段
//     */
//    @TableField(typeHandler = StringListTypeHandler.class)
//    private List<String> paramCodes;
//
//    /**
//     * 真正触发告警的设备id集合-冗余字段
//     */
//    @TableField(typeHandler = StringListTypeHandler.class)
//    private List<String> sbIds;

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
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long mailTemplateId;
    /**
     * 站内信人员
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> siteStaff;
    /**
     * 邮件人员
     */
    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private List<Long> mailStaff;

    /**
     * 告警间隔
     */
    @TableField(value = "`interval`")
    private String interval;

    /**
     * 状态
     * <p>
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