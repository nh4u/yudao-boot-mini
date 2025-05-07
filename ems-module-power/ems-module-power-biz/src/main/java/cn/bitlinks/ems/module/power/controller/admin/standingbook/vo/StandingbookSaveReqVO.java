package cn.bitlinks.ems.module.power.controller.admin.standingbook.vo;

import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeSaveReqVO;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 台账属性新增/修改 Request VO")
@Data
public class StandingbookSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "29246")
    private Long id;

    @Schema(description = "属性名字", example = "王五")
    private String name;

    @Schema(description = "类型ID", example = "10220")

    private Long typeId;
    @Schema(description = "简介", example = "你猜")
    private String description;

    @Schema(description = "标签信息")
    @ExcelProperty("标签信息")
    private String labelInfo;
    @Schema(description = "环节 | 1：外购存储  2：加工转换 3：传输分配 4：终端使用 5：回收利用")
    private Integer stage;


    List<StandingbookAttributeSaveReqVO> children = new ArrayList<>();

    public StandingbookSaveReqVO(List<StandingbookAttributeSaveReqVO> children) {
        this.children = children;
    }
}
