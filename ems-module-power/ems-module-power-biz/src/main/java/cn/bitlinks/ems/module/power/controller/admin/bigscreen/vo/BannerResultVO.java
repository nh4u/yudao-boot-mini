package cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * @author liumingqiang
 */
@Schema(description = "统计总览 能源消耗 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class BannerResultVO {

    @Schema(description = "今日消耗")
    private List<BannerData> today;

    @Schema(description = "本月消耗")
    private List<BannerData> month;
}