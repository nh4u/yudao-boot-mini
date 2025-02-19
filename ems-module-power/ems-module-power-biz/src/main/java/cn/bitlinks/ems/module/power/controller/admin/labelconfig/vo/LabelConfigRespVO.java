package cn.bitlinks.ems.module.power.controller.admin.labelconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import com.alibaba.excel.annotation.*;

@Schema(description = "管理后台 - 配置标签 Response VO")
@Data
@ExcelIgnoreUnannotated
public class LabelConfigRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "12042")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "标签名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "赵六")
    @ExcelProperty("标签名称")
    private String labelName;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("排序")
    private Integer sort;

    @Schema(description = "备注", requiredMode = Schema.RequiredMode.REQUIRED, example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("编码")
    private String code;

    @Schema(description = "父标签ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "26722")
    @ExcelProperty("父标签ID")
    private Long parentId;

    @Schema(description = "是否为默认标签")
    @ExcelProperty("是否为默认标签")
    private String ifDefault;

}