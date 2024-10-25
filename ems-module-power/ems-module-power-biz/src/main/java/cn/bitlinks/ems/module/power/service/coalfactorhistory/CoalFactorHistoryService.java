package cn.bitlinks.ems.module.power.service.coalfactorhistory;

import java.util.*;
import javax.validation.*;
import cn.bitlinks.ems.module.power.controller.admin.coalfactorhistory.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.coalfactorhistory.CoalFactorHistoryDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;

/**
 * 折标煤系数历史 Service 接口
 *
 * @author bitlinks
 */
public interface CoalFactorHistoryService {

    /**
     * 创建折标煤系数历史
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCoalFactorHistory(@Valid CoalFactorHistorySaveReqVO createReqVO);

    /**
     * 更新折标煤系数历史
     *
     * @param updateReqVO 更新信息
     */
    void updateCoalFactorHistory(@Valid CoalFactorHistorySaveReqVO updateReqVO);

    /**
     * 删除折标煤系数历史
     *
     * @param id 编号
     */
    void deleteCoalFactorHistory(Long id);

    /**
     * 获得折标煤系数历史
     *
     * @param id 编号
     * @return 折标煤系数历史
     */
    CoalFactorHistoryDO getCoalFactorHistory(Long id);

    /**
     * 获得折标煤系数历史分页
     *
     * @param pageReqVO 分页查询
     * @return 折标煤系数历史分页
     */
    PageResult<CoalFactorHistoryDO> getCoalFactorHistoryPage(CoalFactorHistoryPageReqVO pageReqVO);

}