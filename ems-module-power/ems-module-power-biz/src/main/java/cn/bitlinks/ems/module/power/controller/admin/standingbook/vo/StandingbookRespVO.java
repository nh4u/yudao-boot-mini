package cn.bitlinks.ems.module.power.controller.admin.standingbook.vo;

import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeRespVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookLabelInfoDO;
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

    @Schema(description = "标签信息")
    @ExcelProperty("标签信息")
    private List<StandingbookLabelInfoDO> labelInfo;

    @Schema(description = "环节 | 1：外购存储  2：加工转换 3：传输分配 4：终端使用 5：回收利用")
    @ExcelProperty("环节 | 1：外购存储  2：加工转换 3：传输分配 4：终端使用 5：回收利用")
    private Integer stage;


    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
    List<StandingbookAttributeRespVO> children = new ArrayList<>();


    @Schema(description = "计量器具类型id")
    private Long standingbookTypeId;
    @Schema(description = "计量器具类型名称")
    private String standingbookTypeName;
}
