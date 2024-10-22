package cn.bitlinks.ems.module.power.service.unitpricehistory;

import java.util.*;
import javax.validation.*;
import cn.bitlinks.ems.module.power.controller.admin.unitpricehistory.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.unitpricehistory.UnitPriceHistoryDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;

/**
 * 单价历史 Service 接口
 *
 * @author bitlinks
 */
public interface UnitPriceHistoryService {

    /**
     * 创建单价历史
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createUnitPriceHistory(@Valid UnitPriceHistorySaveReqVO createReqVO);

    /**
     * 更新单价历史
     *
     * @param updateReqVO 更新信息
     */
    void updateUnitPriceHistory(@Valid UnitPriceHistorySaveReqVO updateReqVO);

    /**
     * 删除单价历史
     *
     * @param id 编号
     */
    void deleteUnitPriceHistory(Long id);

    /**
     * 获得单价历史
     *
     * @param id 编号
     * @return 单价历史
     */
    UnitPriceHistoryDO getUnitPriceHistory(Long id);

    /**
     * 获得单价历史分页
     *
     * @param pageReqVO 分页查询
     * @return 单价历史分页
     */
    PageResult<UnitPriceHistoryDO> getUnitPriceHistoryPage(UnitPriceHistoryPageReqVO pageReqVO);

}