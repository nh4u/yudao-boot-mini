package cn.bitlinks.ems.module.power.dal.dataobject.standingbook;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 台账属性 DO
 *
 * @author bitlinks
 */
@TableName("power_standingbook")
@KeySequence("power_standingbook_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandingbookDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 类型ID
     */
    private Long typeId;
    /**
     * 属性名字
     */
    private String name;
    /**
     * 简介
     */
    private String description;
    @TableField(exist = false)
    List<StandingbookAttributeDO> children = new ArrayList<>();


    public void addChild(StandingbookAttributeDO child) {
        children.add(child);
    }
    public void addChildAll(List<StandingbookAttributeDO> childs) {
        children.addAll(childs);
    }
}
