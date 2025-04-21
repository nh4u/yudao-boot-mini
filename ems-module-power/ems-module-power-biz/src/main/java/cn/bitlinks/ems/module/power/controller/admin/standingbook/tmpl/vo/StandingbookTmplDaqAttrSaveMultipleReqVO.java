package cn.bitlinks.ems.module.power.controller.admin.standingbook.tmpl.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 台账数采属性 保存 Request VO")
@Data
public class StandingbookTmplDaqAttrSaveMultipleReqVO {




    @Schema(description = "属性列表")
    List<StandingbookTmplDaqAttrSaveReqVO> saveReqVOs;
}
