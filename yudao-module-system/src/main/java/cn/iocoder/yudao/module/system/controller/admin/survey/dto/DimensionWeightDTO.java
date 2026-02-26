package cn.iocoder.yudao.module.system.controller.admin.survey.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DimensionWeightDTO {
    private Integer dimensionId;
    private BigDecimal weight;
}