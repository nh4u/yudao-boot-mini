package cn.bitlinks.ems.module.power.service.unitpriceconfiguration;

import java.time.LocalDateTime;
import java.util.*;
import javax.validation.*;
import cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.unitpricehistory.vo.UnitPriceHistorySaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.unitpriceconfiguration.UnitPriceConfigurationDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;

/**
 * 单价配置 Service 接口
 *
 * @author bitlinks
 */
public interface UnitPriceConfigurationService {

    /**
     * 创建单价配置
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createUnitPriceConfiguration(@Valid UnitPriceConfigurationSaveReqVO createReqVO);

    /**
     * 更新单价配置
     *
     * @param updateReqVO 更新信息
     */
    void updateUnitPriceConfiguration(@Valid UnitPriceConfigurationSaveReqVO updateReqVO);

    /**
     * 删除单价配置
     *
     * @param id 编号
     */
    void deleteUnitPriceConfiguration(Long id);

    /**
     * 获得单价配置
     *
     * @param id 编号
     * @return 单价配置
     */
    UnitPriceConfigurationDO getUnitPriceConfiguration(Long id);

    /**
     * 获得单价配置分页
     *
     * @param pageReqVO 分页查询
     * @return 单价配置分页
     */
    PageResult<UnitPriceConfigurationDO> getUnitPriceConfigurationPage(UnitPriceConfigurationPageReqVO pageReqVO);


}