package cn.bitlinks.ems.module.power.controller.admin.doublecarbon.vo;

import cn.bitlinks.ems.framework.common.pojo.PageParam;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 双碳接口 映射req VO")
@Data
@ExcelIgnoreUnannotated
public class DoubleCarbonMappingPageReqVO extends PageParam {

    @Schema(description = "台账编码")
    private String standingbookCode;

}
