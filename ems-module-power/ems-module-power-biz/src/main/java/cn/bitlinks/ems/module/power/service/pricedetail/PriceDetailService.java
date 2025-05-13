package cn.bitlinks.ems.module.power.service.pricedetail;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.pricedetail.vo.PriceDetailPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.pricedetail.PriceDetailDO;

import java.util.List;
import java.util.Map;

/**
 * 单价详细 Service 接口
 *
 * @author bitlinks
 */
public interface PriceDetailService {


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