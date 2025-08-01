package cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
@Data
public class NaturalGasChartResVO {

    @Schema(description = "锅炉1")
    private List<BigDecimal> rqBlr1;
    @Schema(description = "锅炉2")
    private List<BigDecimal> rqBlr2;
    @Schema(description = "锅炉3")
    private List<BigDecimal> rqBlr3;
    @Schema(description = "VOCA")
    private List<BigDecimal> voca;
    @Schema(description = "VOCB")
    private List<BigDecimal> vocb;
    @Schema(description = "VOCC")
    private List<BigDecimal> vocc;

}
