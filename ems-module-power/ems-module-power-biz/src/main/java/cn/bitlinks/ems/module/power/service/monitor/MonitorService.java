package cn.bitlinks.ems.module.power.service.monitor;

import cn.bitlinks.ems.module.power.controller.admin.monitor.vo.MonitorDetailData;
import cn.bitlinks.ems.module.power.controller.admin.monitor.vo.MonitorDetailRespVO;
import cn.bitlinks.ems.module.power.controller.admin.monitor.vo.MonitorParamReqVO;
import cn.bitlinks.ems.module.power.controller.admin.monitor.vo.MonitorRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.tmpl.vo.StandingbookTmplDaqAttrRespVO;

import java.util.List;
import java.util.Map;

/**
 * 台账属性 Service 接口
 *
 * @author bitlinks
 */
public interface MonitorService {


    /**
     * 条件查询台账（标签、属性、分类ids、分类id、topType、环节、创建时间） 查询监控list
     *
     * @param pageReqVO 条件map
     * @return 台账列表
     */
    MonitorRespVO getMinitorList(Map<String, String> pageReqVO);

    MonitorDetailRespVO deviceDetail(MonitorParamReqVO paramVO);

    List<StandingbookTmplDaqAttrRespVO> getDaqAttrs(Long standingbookId);

    List<MonitorDetailData> getDetailTable(MonitorParamReqVO paramVO);
}
