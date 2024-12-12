package cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 台账属性新增/修改 Request VO")
@Data
public class StandingbookAttributeSaveMultipleReqVO {




    @Schema(description = "类型编号", example = "16688")
    private Long typeId;
    @Schema(description = "属性列表")
    List< StandingbookAttributeSaveReqVO> createReqVOs=new ArrayList<>();
}
