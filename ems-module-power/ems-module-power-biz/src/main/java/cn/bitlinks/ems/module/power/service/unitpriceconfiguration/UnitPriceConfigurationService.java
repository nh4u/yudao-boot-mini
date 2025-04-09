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
     * @param createReqVOList  创建信息
     * @return 编号
     */
    List<Long> createUnitPriceConfigurations(@Valid Long energyId, List<UnitPriceConfigurationSaveReqVO> createReqVOList);

    /**
     * 更新单价配置
     *
     * @param updateReqVOList 更新信息
     */
    List<Long> updateUnitPriceConfiguration(@Valid Long energyId, List<UnitPriceConfigurationSaveReqVO> updateReqVOList);

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

    List<UnitPriceConfigurationDO> getUnitPriceConfigurationVOByEnergyId(Long energyId);

    /**
     * 获得单价配置分页
     *
     * @param pageReqVO 分页查询
     * @return 单价配置分页
     */
    PageResult<UnitPriceConfigurationRespVO> getUnitPriceConfigurationPage(UnitPriceConfigurationPageReqVO pageReqVO);

    /**
     * 根据能源ID获取单价配置列表
     *
     * @param energyId 能源ID
     * @return 单价配置列表
     */
    List<UnitPriceConfigurationDO> getUnitPriceConfigurationByEnergyId(Long energyId);

    LocalDateTime getLatestEndTime(Long energyId);

    PriceResultDTO getPriceByTime(Long energyId, LocalDateTime targetTime);

}