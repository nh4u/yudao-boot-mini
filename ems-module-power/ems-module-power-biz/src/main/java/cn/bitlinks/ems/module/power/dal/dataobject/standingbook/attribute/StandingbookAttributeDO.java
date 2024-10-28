package cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 台账属性 DO
 *
 * @author bitlinks
 */
@TableName("power_standingbook_attribute")
@KeySequence("power_standingbook_attribute_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandingbookAttributeDO extends BaseDO {

    /**
     * 编号
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 属性名字
     */
    private String name;
    /**
     * 属性值
     */
    private String value;
    /**
     * 类型编号
     */
    private Long typeId;
    /**
     * 台账编号
     */
    private Long standingbookId;
    /**
     * 文件编号
     */
    private Long fileId;
    /**
     * 是否必填
     */
    private String isRequired;
    /**
     * 编码
     */
    private String code;
    /**
     * 排序
     */
    private Long sort;
    /**
     * 格式
     */
    private String format;
    /**
     * 归属节点
     */
    private String node;
    /**
     * 简介
     */
    private String description;
    /**
     * 下拉框选项
     */
    private String options;

}
