package cn.bitlinks.ems.module.power.controller.admin.standingbook.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class StandingbookEnergyParamReqVO {

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
     * 能源参数中文名（完全匹配）
     */
    @Schema(description = "能源参数中文名（完全匹配）")
    @NotBlank
    private String energyParamCnName;
}
