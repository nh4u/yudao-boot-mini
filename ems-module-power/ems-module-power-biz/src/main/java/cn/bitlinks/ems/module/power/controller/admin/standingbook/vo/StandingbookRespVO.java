package cn.bitlinks.ems.module.power.controller.admin.standingbook.vo;

import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeRespVO;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 台账属性 Response VO")
@Data
@ExcelIgnoreUnannotated
public class StandingbookRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "29246")
    @ExcelProperty("编号")
    private Long id;
    @Schema(description = "类型ID", example = "10220")
    private Long typeId;
    @Schema(description = "属性名字", example = "王五")
    @ExcelProperty("属性名字")
    private String name;

    @Schema(description = "简介", example = "你猜")
    @ExcelProperty("简介")
    private String description;

    /**
     * 采集频率
     */
    @Schema(description = "采集频率", example = "1")
    private Integer frequency;
    /**
     * 采集频率单位
     */
    @Schema(description = "采集频率单位", example = "秒")
    private String frequencyUit;
    /**
     * 数据来源分类
     */
    @Schema(description = "数据来源分类", example = "1")
    private Integer sourceType;
    /**
     * 开关（0：关；1开。）
     */
    @Schema(description = "开关（0：关；1开。）", example = "0")
    private Boolean status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
    List<StandingbookAttributeRespVO> children = new ArrayList<>();
}
