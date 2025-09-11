package cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo;

import cn.bitlinks.ems.module.power.controller.admin.report.vo.BigScreenCopChartData;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 大屏 Response VO")
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class BigScreenRespVO {

    // 右部分

    @Schema(description = "室外工况")
    private OutsideEnvData outside;

    @Schema(description = "cop图")
    private BigScreenCopChartData cop;

    @Schema(description = "纯废水单价图")
    private BigScreenChartData pureWasteWater;

    @Schema(description = "压缩空气单价图")
    private BigScreenChartData gas;

    // 底部分

    @Schema(description = "单位产品综合能耗")
    private BigScreenChartData productConsumption;

    // 中间部分

    @Schema(description = "4#宿舍楼")
    private OriginMiddleData dormitory4;

    @Schema(description = "2#生产厂房")
    private OriginMiddleData factory2;

    @Schema(description = "3#办公楼")
    private OriginMiddleData office3;

    @Schema(description = "5#CUB")
    private OriginMiddleData cub5;

    @Schema(description = "1#生产厂房")
    private OriginMiddleData factory1;

    // 顶部

    @Schema(description = "banner")
    BannerResultVO bannerResultVO;

    // 左部

    @Schema(description = "近7日能源数据")
    private List<RecentSevenDayResultVO> recentSevenDayList;

}
