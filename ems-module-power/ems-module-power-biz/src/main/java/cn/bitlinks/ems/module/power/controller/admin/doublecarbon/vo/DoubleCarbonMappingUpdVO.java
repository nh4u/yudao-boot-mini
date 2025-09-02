package cn.bitlinks.ems.module.power.controller.admin.doublecarbon.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 双碳接口 映射upd VO")
@Data
@ExcelIgnoreUnannotated
public class DoubleCarbonMappingUpdVO {

    @Schema(description = "映射id")
    private Long id;
    @Schema(description = "双碳编码")
    private String doubleCarbonCode;

}
