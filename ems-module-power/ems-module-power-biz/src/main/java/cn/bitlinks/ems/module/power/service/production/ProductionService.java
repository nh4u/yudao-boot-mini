package cn.bitlinks.ems.module.power.service.production;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.externalapi.vo.ProductionPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.externalapi.vo.ProductionSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.production.ProductionDO;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;


/**
 * @author liumingqiang
 */
public interface ProductionService {

    /**
     * 创建外部接口
     *
     * @param createReqVO 创建信息
     * @return ProductionDO
     */
    ProductionDO createProduction (@Valid ProductionSaveReqVO createReqVO);

    /**
     * 更新外部接口
     *
     * @param updateReqVO 更新信息
     */
    void updateProduction(@Valid ProductionSaveReqVO updateReqVO);

    /**
     * 删除外部接口
     *
     * @param id 编号
     */
    void deleteProduction(Long id);

    /**
     * 获得外部接口
     *
     * @param id 编号
     * @return 外部接口
     */
    ProductionDO getProduction(Long id);

    /**
     * 获得外部接口分页
     *
     * @param pageReqVO 分页查询
     * @return 外部接口分页
     */
    PageResult<ProductionDO> getProductionPage(ProductionPageReqVO pageReqVO);

    ProductionDO getHomeProduction(ProductionPageReqVO pageReqVO);

    List<ProductionDO> getBigScreenProduction(LocalDateTime startDate, LocalDateTime endDate);

    ProductionDO getLastProduction(Integer size);
}