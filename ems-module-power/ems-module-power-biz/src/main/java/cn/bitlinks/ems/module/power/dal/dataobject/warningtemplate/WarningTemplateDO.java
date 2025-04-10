package cn.bitlinks.ems.module.power.dal.dataobject.warningtemplate;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.util.List;

/**
 * 告警模板 DO
 *
 * @author bitlinks
 */
@TableName(value = "power_warning_template", autoResultMap = true)
@KeySequence("power_warning_template_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarningTemplateDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 模板名称
     */
    private String name;
    /**
     * 模板编码
     */
    private String code;
    /**
     * 模板内容
     */
    private String content;
    /**
     * 模板标题
     */
    private String title;
    /**
     * 标题参数数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tParams;
    /**
     * 内容参数数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> params;
    /**
     * 备注
     */
    private String remark;
    /**
     * 模板类型:0-站内信1-邮件
     */
    private Integer type;

}