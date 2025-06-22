package cn.bitlinks.ems.module.power.controller.admin.standingbook.vo;

import lombok.Data;

import java.util.List;

@Data
public class StandingBookTypeTreeRespVO {

    /**
     * 节点id#show
     */
    private String nodeId;
    /**
     * 节点id#show 上一级节点id
     */
    private String parentNodeId;

    /**
     * 分类id/计量器具id
     */
    private Long rawId;
    /**
     * 节点名称（分类名称或计量器具名称（编号））
     */
    private String nodeName;

    /**
     * 分类：false, 计量器具：true
     */
    private boolean show;
    /**
     * 子节点
     */
    private List<StandingBookTypeTreeRespVO> children;
}
