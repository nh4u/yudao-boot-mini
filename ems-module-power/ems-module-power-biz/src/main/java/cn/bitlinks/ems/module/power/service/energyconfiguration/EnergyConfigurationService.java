package cn.bitlinks.ems.module.power.service.energyconfiguration;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationRespVO;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 能源配置 Service 接口
 *
 * @author bitlinks
 */
public interface EnergyConfigurationService {

    /**
     * 创建能源配置
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createEnergyConfiguration(@Valid EnergyConfigurationSaveReqVO createReqVO);

    /**
     * 更新能源配置
     *
     * @param updateReqVO 更新信息
     */
    void updateEnergyConfiguration(@Valid EnergyConfigurationSaveReqVO updateReqVO);

    /**
     * 删除能源配置
     *
     * @param id 编号
     */
    void deleteEnergyConfiguration(Long id);

    /**
     * 删除能源配置
     *
     * @param ids 编号
     */
    void deleteEnergyConfigurations(List<Long> ids);

    /**
     * 获得能源配置
     *
     * @param id 编号
     * @return 能源配置
     */
    EnergyConfigurationRespVO getEnergyConfiguration(Long id);

    /**
     * 获得能源配置分页
     *
     * @param pageReqVO 分页查询
     * @return 能源配置分页
     */
    PageResult<EnergyConfigurationRespVO> getEnergyConfigurationPage(EnergyConfigurationPageReqVO pageReqVO);

    List<EnergyConfigurationDO> getAllEnergyConfiguration(EnergyConfigurationSaveReqVO queryVO);

    /**
     * 获得能源配置分页
     *
     * @param queryVO 分页查询
     * @return 能源配置分页
     */
    List<EnergyConfigurationDO> getEnergyConfigurationList(EnergyConfigurationPageReqVO queryVO);

    /**
     * 根据条件查询能源配置列表。
     *
     * @param energyName     能源名称
     * @param energyClassify 能源分类
     * @param code           编码
     * @return 符合条件的能源配置记录列表，如果没有找到任何记录则返回空列表。
     */
    List<EnergyConfigurationRespVO> selectByCondition(String energyName, String energyClassify, String code);

    Map<Integer, List<EnergyConfigurationDO>> getEnergyMenu();

    /**
     * 提交指标煤公式/用能成本公式接口
     *
     * @param updateReqVO
     * @return
     */
    void submitFormula(EnergyConfigurationSaveReqVO updateReqVO);

    List<EnergyConfigurationDO> getEnergyTree();
}