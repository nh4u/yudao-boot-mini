package cn.bitlinks.ems.module.power.service.unitpriceconfiguration;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo.PriceResultDTO;
import cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo.UnitPriceConfigurationPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo.UnitPriceConfigurationRespVO;
import cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo.UnitPriceConfigurationSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.unitpriceconfiguration.UnitPriceConfigurationDO;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 单价配置 Service 接口
 *
 * @author bitlinks
 */
public interface UnitPriceConfigurationService {

    /**
     * 更新单价配置
     *
     * @param updateReqVOList 更新信息
     */
    void updateUnitPriceConfiguration(@Valid Long energyId, List<UnitPriceConfigurationSaveReqVO> updateReqVOList);


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

    /**
     * 获取当前的单价配置
     * @param energyId 能源ID
     * @return 单价配置
     */
    UnitPriceConfigurationDO getCurrentUnitConfigByEnergyId(Long energyId);

    /**
     * 获取当前能源配置的单价配置的周期结束时间
     *
     * @param energyId 能源id
     * @return 当前能源配置的单价配置的周期结束时间
     */
    LocalDateTime getLatestEndTime(Long energyId);

    /**
     * 获取当前生效单价
     *
     * @param energyId   能源id
     * @param targetTime 指定时间
     * @return 价格详细
     */
    PriceResultDTO getPriceByTime(Long energyId, LocalDateTime targetTime);

}