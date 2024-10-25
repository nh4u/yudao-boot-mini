package cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 台账类型 DO
 *
 * @author bitlinks
 */
@TableName("power_standingbook_type")
@KeySequence("power_standingbook_type_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandingbookTypeDO extends BaseDO {

    public static final Long SUPER_ID_ROOT = 0L;

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 名字
     */
    private String name;
    /**
     * 父级类型编号
     */
    private Long superId;
    /**
     * 父级名字
     */
    private String superName;
    /**
     * 类型
     */
    private String topType;
    /**
     * 排序
     */
    private Long sort;
    /**
     * 当前层级
     */
    private Long level;
    /**
     * 编码
     */
    private String code;
    /**
     * 简介
     */
    private String description;
    @TableField(exist = false)
    List<StandingbookTypeDO> children = new ArrayList<>();

    public StandingbookTypeDO(String name) {
    }


    public void addChild(StandingbookTypeDO child) {
        children.add(child);
    }
}
