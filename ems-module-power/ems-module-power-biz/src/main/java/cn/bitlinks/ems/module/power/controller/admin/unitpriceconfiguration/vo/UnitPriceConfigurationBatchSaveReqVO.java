package cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * @Title: identifier-carrier
 * @description:
 * @Author: Jiayun CUI
 * @Date 2025/01/06 15:11
 **/
@Schema(description = "管理后台 - 批量创建单价配置 Request VO")
@Data
@ToString(callSuper = true)
public class UnitPriceConfigurationBatchSaveReqVO {

    @Schema(description = "能源id", example = "29649")
    private Long energyId;

    @Schema(description = "单价配置列表")
    private List<UnitPriceConfigurationSaveReqVO> list;
}