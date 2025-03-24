package cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Schema(description = "管理后台 - 台账属性树形结构-台账属性 Request VO")
@Data
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class AttributeTreeNode {
    @Schema(description = "父级编号")
    private String pId;
    @Schema(description = "编号")
    private String id;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "节点类型")
    private Integer type; //0-台账分类 1-台账 2-能源参数
    @Schema(description = "下一级节点")
    private List<AttributeTreeNode> children;
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        AttributeTreeNode node = (AttributeTreeNode) o;
//        return id.equals(node.id);
//    }
//
//    @Override
//    public int hashCode() {
//        return id.hashCode();
//    }
}
