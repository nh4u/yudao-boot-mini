package cn.bitlinks.ems.module.power.service.energygroup;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.energygroup.vo.EnergyGroupPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.energygroup.vo.EnergyGroupSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.energygroup.EnergyGroupDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 能源分组 Service 接口
 *
 * @author hero
 */
public interface EnergyGroupService {

    /**
     * 创建能源分组
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createEnergyGroup(EnergyGroupSaveReqVO createReqVO);

    /**
     * 能源分组修改
     *
     * @param energyGroups
     * @return
     */
    Boolean change(List<EnergyGroupSaveReqVO> energyGroups);

    /**
     * 更新能源分组
     *
     * @param updateReqVO 更新信息
     */
    void updateEnergyGroup(@Valid EnergyGroupSaveReqVO updateReqVO);

    /**
     * 删除能源分组
     *
     * @param id 编号
     */
    void deleteEnergyGroup(Long id);

    /**
     * 批量删除能源分组
     *
     * @param ids 编号
     */
    void deleteEnergyGroups(List<Long> ids);

    /**
     * 获得能源分组
     *
     * @param id 编号
     * @return 能源分组
     */
    EnergyGroupDO getEnergyGroup(Long id);

    /**
     * 获得能源分组分页
     *
     * @param pageReqVO 分页查询
     * @return 能源分组分页
     */
    PageResult<EnergyGroupDO> getEnergyGroupPage(EnergyGroupPageReqVO pageReqVO);

    /**
     * 获取能源分组list
     * @return
     */
    List<EnergyGroupDO>  getEnergyGroups();
}