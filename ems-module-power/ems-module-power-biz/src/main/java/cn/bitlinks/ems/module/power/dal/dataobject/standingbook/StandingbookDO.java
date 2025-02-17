package cn.bitlinks.ems.module.power.dal.dataobject.standingbook;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import com.alibaba.excel.annotation.ExcelProperty;
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
    /**
     * 标签信息
     */
    @Schema(description = "标签信息")
    private String labelInfo;
    /**
     * 采集频率
     */
    @Schema(description = "采集频率")
    private Integer frequency;
    /**
     * 采集频率单位
     */
    @Schema(description = "采集频率单位")
    private String frequencyUnit;
    /**
     * 数据来源分类
     */
    @Schema(description = "数据来源分类")
    private Integer sourceType;
    /**
     * 数据来源分类=关联计量器具时 相关信息json
     */
    @Schema(description = "数据来源分类=关联计量器具时 相关信息json")
    private String associationMeasurementJson;
    /**
     * 开关（0：关；1开。）
     */
    @Schema(description = "开关（0：关；1开。）")
    private Boolean status;
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
