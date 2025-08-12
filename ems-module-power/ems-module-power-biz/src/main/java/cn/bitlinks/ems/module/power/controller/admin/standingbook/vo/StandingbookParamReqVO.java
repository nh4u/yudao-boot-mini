package cn.bitlinks.ems.module.power.controller.admin.standingbook.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class StandingbookParamReqVO {

    /**
     * 台账编码（模糊搜索）
     */
    @Schema(description = "台账编码（模糊搜索）")
    private String sbCode;

    /**
     * 台账名称（模糊搜索）
     */
    @Schema(description = "台账名称（模糊搜索）")
    private String sbName;


    /**
     * 台账名称（模糊搜索
     */
    @Schema(description = "台账名称（模糊搜索")
    @NotBlank
    private String actualSbName;
}
