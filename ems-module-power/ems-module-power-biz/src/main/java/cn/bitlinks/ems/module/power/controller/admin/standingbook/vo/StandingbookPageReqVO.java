package cn.bitlinks.ems.module.power.controller.admin.standingbook.vo;

import cn.bitlinks.ems.framework.common.pojo.PageParam;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributePageReqVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 台账属性分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StandingbookPageReqVO extends PageParam {

    @Schema(description = "属性名字", example = "王五")
    private String name;

    @Schema(description = "类型ID", example = "10220")
    private Long typeId;

    @Schema(description = "简介", example = "你猜")
    private String description;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;
    List<StandingbookAttributePageReqVO> children = new ArrayList<>();
}
