package cn.bitlinks.ems.module.power.service.energygroup;

import cn.hutool.core.collection.CollectionUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import cn.bitlinks.ems.module.power.controller.admin.energygroup.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.energygroup.EnergyGroupDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;

import cn.bitlinks.ems.module.power.dal.mysql.energygroup.EnergyGroupMapper;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

/**
 * 能源分组 Service 实现类
 *
 * @author hero
 */
@Service
@Validated
public class EnergyGroupServiceImpl implements EnergyGroupService {

    @Resource
    private EnergyGroupMapper energyGroupMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean change(List<EnergyGroupSaveReqVO> energyGroups) {

        if (CollectionUtil.isEmpty(energyGroups)) {
            throw exception(ENERGY_GROUP_LIST_NOT_EXISTS);
        }

        // 获取现有分组id
        List<Long> ids = energyGroupMapper.selectIds();

        // 对传来的list进行分类处理
        for (EnergyGroupSaveReqVO energyGroup : energyGroups) {
            Long id = energyGroup.getId();
            if (Objects.isNull(id)) {
                //新增  名称判重
                createEnergyGroup(energyGroup);
            } else {
                // 修改  名称判重
                ids.remove(id);
                updateEnergyGroup(energyGroup);
            }

            // 删除操作
            if (CollectionUtil.isNotEmpty(ids)) {
                deleteEnergyGroups(ids);
            }


        }

        return true;
    }

    @Override
    public Long createEnergyGroup(EnergyGroupSaveReqVO createReqVO) {

        // 校验
        validateEnergyGroupDuplicate(createReqVO);
        // 插入
        EnergyGroupDO energyGroup = BeanUtils.toBean(createReqVO, EnergyGroupDO.class);
        energyGroupMapper.insert(energyGroup);
        // 返回
        return energyGroup.getId();
    }

    @Override
    public void updateEnergyGroup(EnergyGroupSaveReqVO updateReqVO) {
        // 校验存在
        validateEnergyGroupExists(updateReqVO.getId());
        validateEnergyGroupDuplicate(updateReqVO);
        // 更新
        EnergyGroupDO updateObj = BeanUtils.toBean(updateReqVO, EnergyGroupDO.class);
        energyGroupMapper.updateById(updateObj);
    }

    @Override
    public void deleteEnergyGroup(Long id) {
        // 校验存在
        validateEnergyGroupExists(id);
        // 删除
        energyGroupMapper.deleteById(id);
    }

    @Override
    public void deleteEnergyGroups(List<Long> ids) {
        // 删除
        energyGroupMapper.deleteByIds(ids);
    }

    @Override
    public EnergyGroupDO getEnergyGroup(Long id) {
        return energyGroupMapper.selectById(id);
    }

    @Override
    public PageResult<EnergyGroupDO> getEnergyGroupPage(EnergyGroupPageReqVO pageReqVO) {
        return energyGroupMapper.selectPage(pageReqVO);
    }

    @Override
    public List<EnergyGroupDO> getEnergyGroups() {

        return energyGroupMapper.selectList();
    }

    private void validateEnergyGroupExists(Long id) {
        if (energyGroupMapper.selectById(id) == null) {
            throw exception(ENERGY_GROUP_NOT_EXISTS);
        }
    }

    private void validateEnergyGroupDuplicate(EnergyGroupSaveReqVO reqVO) {
        if (energyGroupMapper.selectOne(reqVO) != null) {
            throw exception(ENERGY_GROUP_EXISTS);
        }
    }
}