package cn.bitlinks.ems.module.power.service.externalapi;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.externalapi.vo.ExternalApiPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.externalapi.vo.ExternalApiSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.externalapi.ExternalApiDO;

import javax.validation.Valid;


/**
 * @author liumingqiang
 */
public interface ExternalApiService {

    /**
     * 创建外部接口
     *
     * @param createReqVO 创建信息
     * @return ExternalApiDO
     */
    ExternalApiDO createExternalApi (@Valid ExternalApiSaveReqVO createReqVO);

    /**
     * 更新外部接口
     *
     * @param updateReqVO 更新信息
     */
    void updateExternalApi(@Valid ExternalApiSaveReqVO updateReqVO);

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
    ExternalApiDO getExternalApi(Long id);

    /**
     * 获得外部接口分页
     *
     * @param pageReqVO 分页查询
     * @return 外部接口分页
     */
    PageResult<ExternalApiDO> getExternalApiPage(ExternalApiPageReqVO pageReqVO);

    Object testExternalApi(ExternalApiSaveReqVO createReqVO);

   String getProductYieldUrl();

    Object getAllOut();
}