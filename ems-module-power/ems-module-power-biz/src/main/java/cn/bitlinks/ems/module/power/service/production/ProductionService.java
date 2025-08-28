package cn.bitlinks.ems.module.power.service.production;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.externalapi.vo.ProductionPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.externalapi.vo.ProductionSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.production.ProductionDO;

import javax.validation.Valid;


/**
 * @author liumingqiang
 */
public interface ProductionService {

    /**
     * 创建外部接口
     *
     * @param createReqVO 创建信息
     * @return ExternalApiDO
     */
    ProductionDO createProduction (@Valid ProductionSaveReqVO createReqVO);

    /**
     * 更新外部接口
     *
     * @param updateReqVO 更新信息
     */
    void updateExternalApi(@Valid ProductionSaveReqVO updateReqVO);

    /**
     * 删除外部接口
     *
     * @param id 编号
     */
    void deleteExternalApi(Long id);

    /**
     * 获得外部接口
     *
     * @param id 编号
     * @return 外部接口
     */
    ProductionDO getExternalApi(Long id);

    /**
     * 获得外部接口分页
     *
     * @param pageReqVO 分页查询
     * @return 外部接口分页
     */
    PageResult<ProductionDO> getExternalApiPage(ProductionPageReqVO pageReqVO);


}