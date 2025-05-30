package cn.bitlinks.ems.module.power.dal.dataobject.standingbook;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "编号")
    private Long id;
    /**
     * 类型ID
     */
    @Schema(description = "类型ID")
    private Long typeId;

    @Schema(description = "所属类型名称")
    @TableField(exist = false)
    private String typeName;
    /**
     * 属性名字
     */
    @Schema(description = "属性名字")
    private String name;
    /**
     * 简介
     */
    @Schema(description = "简介")
    private String description;

    @Schema(description = "标签信息")
    @TableField(exist = false)
    private List<StandingbookLabelInfoDO> labelInfo;
        /**
     * 环节 | 1：外购存储  2：加工转换 3：传输分配 4：终端使用 5：回收利用
     */
    @Schema(description = "环节")
    private Integer stage;

    @TableField(exist = false)
    List<StandingbookAttributeDO> children = new ArrayList<>();


    public void addChild(StandingbookAttributeDO child) {
        children.add(child);
    }

    public void addChildAll(List<StandingbookAttributeDO> childs) {
        children.addAll(childs);
    }

    public StandingbookDO(List<StandingbookAttributeDO> children) {
        this.children = children;
    }
}
