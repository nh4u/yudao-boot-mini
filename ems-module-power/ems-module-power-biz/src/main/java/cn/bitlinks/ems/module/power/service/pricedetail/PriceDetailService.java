package cn.bitlinks.ems.module.power.service.pricedetail;

import java.util.*;
import javax.validation.*;
import cn.bitlinks.ems.module.power.controller.admin.pricedetail.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.pricedetail.PriceDetailDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;

/**
 * 单价详细 Service 接口
 *
 * @author bitlinks
 */
public interface PriceDetailService {

    /**
     * 创建单价详细
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPriceDetail(@Valid PriceDetailSaveReqVO createReqVO);

    /**
     * 更新单价详细
     *
     * @param updateReqVO 更新信息
     */
    void updatePriceDetail(@Valid PriceDetailSaveReqVO updateReqVO);

    /**
     * 删除单价详细
     *
     * @param id 编号
     */
    void deletePriceDetail(Long id);

    /**
     * 获得单价详细
     *
     * @param id 编号
     * @return 单价详细
     */
    PriceDetailDO getPriceDetail(Long id);

    /**
     * 获得单价详细分页
     *
     * @param pageReqVO 分页查询
     * @return 单价详细分页
     */
    PageResult<PriceDetailDO> getPriceDetailPage(PriceDetailPageReqVO pageReqVO);

    // 实现类新增
    List<PriceDetailDO> getDetailsByPriceId(Long priceId);

    void deleteByPriceId(Long priceId);

    Map<Long, List<PriceDetailDO>> getDetailsByPriceIds(List<Long> priceIds);
}