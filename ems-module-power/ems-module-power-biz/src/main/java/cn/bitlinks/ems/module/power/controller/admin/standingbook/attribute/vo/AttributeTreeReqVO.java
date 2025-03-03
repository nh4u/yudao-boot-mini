package cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Schema(description = "管理后台 - 台账属性树形查询 Request VO")
@Data
@ToString(callSuper = true)
public class AttributeTreeReqVO {

    @Schema(description = "台账ids")
    private List<Long> sbIds;
    @Schema(description = "台账分类ids")
    private List<Long> typeIds;
}
