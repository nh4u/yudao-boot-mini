package cn.bitlinks.ems.module.power.service.bigscreen;

import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.*;

import java.util.List;

/**
 * 台账属性 Service 接口
 *
 * @author bitlinks
 */
public interface BigScreenService {

    BigScreenRespVO getBigScreenDetails(BigScreenParamReqVO paramVO);

    OutsideEnvData getOutsideEnvData(BigScreenParamReqVO paramVO);

    BannerResultVO getBannerData(BigScreenParamReqVO paramVO);

    List<RecentSevenDayResultVO> getRecentSevenDay(BigScreenParamReqVO paramVO);

    ProductionFifteenDayResultVO getRecentFifteenDayProduction(BigScreenParamReqVO paramVO);
}
