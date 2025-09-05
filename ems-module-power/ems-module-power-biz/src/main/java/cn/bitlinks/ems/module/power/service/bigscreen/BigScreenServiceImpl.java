package cn.bitlinks.ems.module.power.service.bigscreen;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.BigScreenParamReqVO;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.BigScreenRespVO;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.CopChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.ReportParamVO;
import cn.bitlinks.ems.module.power.service.cophouraggdata.CopHourAggDataService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

/**
 * 台账属性 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class BigScreenServiceImpl implements BigScreenService {

    @Resource
    private CopHourAggDataService copHourAggDataService;

    @Override
    public BigScreenRespVO getBigScreenDetails(BigScreenParamReqVO paramVO) {

        BigScreenRespVO resultVO = new BigScreenRespVO();

        // 获取cop数据
        ReportParamVO reportParamVO = BeanUtils.toBean(paramVO, ReportParamVO.class);
        CopChartResultVO copChart = copHourAggDataService.copChart(reportParamVO);
        resultVO.setCopChart(copChart);

        return null;
    }
}
