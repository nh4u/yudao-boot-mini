package cn.bitlinks.ems.module.power.service.energyparameters;

import java.util.*;
import javax.validation.*;
import cn.bitlinks.ems.module.power.controller.admin.energyparameters.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.energyparameters.EnergyParametersDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;

/**
 * 能源参数 Service 接口
 *
 * @author bitlinks
 */
public interface EnergyParametersService {

    /**
     * 创建能源参数
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createEnergyParameters(@Valid EnergyParametersSaveReqVO createReqVO);

    // 新增批量插入方法
    void batchCreateEnergyParameters(List<EnergyParametersSaveReqVO> list);

    /**
     * 更新能源参数
     *
     * @param updateReqVO 更新信息
     */
    void updateEnergyParameters(@Valid EnergyParametersSaveReqVO updateReqVO);

    /**
     * 删除能源参数
     *
     * @param id 编号
     */
    void deleteEnergyParameters(Long id);

    /**
     * 获得能源参数
     *
     * @param id 编号
     * @return 能源参数
     */
    EnergyParametersDO getEnergyParameters(Long id);

    /**
     * 获得能源参数分页
     *
     * @param pageReqVO 分页查询
     * @return 能源参数分页
     */
    PageResult<EnergyParametersDO> getEnergyParametersPage(EnergyParametersPageReqVO pageReqVO);

    List<EnergyParametersDO> getEnergyParametersByEnergyId(Long energyId);

    /**
     * 根据能源id列表和是否是用量查询能源参数
     * @param energyIds
     * @param usage
     * @return
     */
    List<EnergyParametersDO> getUsageParamsByEnergyIds(List<Long> energyIds, Boolean usage);

}