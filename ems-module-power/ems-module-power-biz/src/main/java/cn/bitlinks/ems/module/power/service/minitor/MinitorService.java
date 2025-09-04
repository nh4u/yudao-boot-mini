package cn.bitlinks.ems.module.power.service.minitor;

import cn.bitlinks.ems.module.power.controller.admin.minitor.vo.MinitorDetailRespVO;
import cn.bitlinks.ems.module.power.controller.admin.minitor.vo.MinitorParamReqVO;
import cn.bitlinks.ems.module.power.controller.admin.minitor.vo.MinitorRespVO;

import java.util.Map;

/**
 * 台账属性 Service 接口
 *
 * @author bitlinks
 */
public interface MinitorService {


    /**
     * 条件查询台账（标签、属性、分类ids、分类id、topType、环节、创建时间） 查询监控list
     *
     * @param pageReqVO 条件map
     * @return 台账列表
     */
    MinitorRespVO getMinitorList(Map<String, String> pageReqVO);

    MinitorDetailRespVO deviceDetail(MinitorParamReqVO paramVO);
}
