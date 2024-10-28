package cn.bitlinks.ems.module.power.controller.admin.standingbook.vo;

import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeSaveReqVO;
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
    List<StandingbookAttributeSaveReqVO> children = new ArrayList<>();

    public StandingbookSaveReqVO(List<StandingbookAttributeSaveReqVO> children) {
        this.children = children;
    }
}
