package cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo;

import cn.bitlinks.ems.module.power.controller.admin.standingbook.tmpl.vo.StandingbookTmplDaqAttrSbRespVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Schema(description = "管理后台 - 台账属性树形结构-台账和台账分类节点 Request VO")
@Data
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class AttributeTreeNode {
    @Schema(description = "父级编号")
    private Long pId;
    @Schema(description = "编号")
    private Long id;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "节点类型0-设备分类 1-重点设备 2-计量器具")
    private Integer type;
    @Schema(description = "下一级节点")
    private List<AttributeTreeNode> children;

    @Schema(description = "数采参数节点")
    private List<StandingbookTmplDaqAttrDO> attrChildren;

}
