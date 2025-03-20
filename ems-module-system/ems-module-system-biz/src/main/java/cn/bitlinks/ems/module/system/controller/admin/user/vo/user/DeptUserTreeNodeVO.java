package cn.bitlinks.ems.module.system.controller.admin.user.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


/**
 * 叶子节点为人员/空部门id
 */
@Schema(description = "管理后台 - 部门-用户树形结构 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeptUserTreeNodeVO {

    @Schema(description = "节点id(部门id/用户id)")
    private Long id;

    @Schema(description = "节点名称(部门名称/用户名称)")
    private String name;

    @Schema(description = "父级节点id")
    private Long pId;

    @Schema(description = "节点类型0-部门 1-人员")
    private Integer type;


    @Schema(description = "下一级节点")
    private List<DeptUserTreeNodeVO> children;

}
