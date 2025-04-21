package cn.bitlinks.ems.module.power.service.energygroup;

import cn.bitlinks.ems.framework.common.exception.ErrorCode;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.text.StrPool;
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

        }

        // 删除操作
        if (CollectionUtil.isNotEmpty(ids)) {
            deleteEnergyGroups(ids);
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
        // 校验是否重复
        validateEnergyGroupDuplicate(updateReqVO);
        // 校验是否已经绑定
        validateEnergyGroupBind(Collections.singletonList(updateReqVO.getId()));
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

        // 校验是否已经绑定
        validateEnergyGroupBind(ids);
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
    public List<EnergyGroupRespVO> getEnergyGroups() {
        return energyGroupMapper.getEnergyGroups(null);
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

    private void validateEnergyGroupBind(List<Long> ids) {
        // 删除时需要校验是否已经绑定了能源  如果绑定了则不能删除，
        List<EnergyGroupRespVO> energyGroups = energyGroupMapper.getEnergyGroups(ids);

        StringBuilder strBuilder = new StringBuilder();
        for (EnergyGroupRespVO energyGroup : energyGroups) {
            Boolean edited = energyGroup.getEdited();
            if (edited == null || !edited) {
                // 如果不可编辑 说明有绑定能源，那么需要报异常
                strBuilder.append(energyGroup.getName()).append(StrPool.COMMA);
            }
        }

        // 如何有报错信息
        if (strBuilder.length() > 0) {
            // 删除多余，号
            strBuilder.deleteCharAt(strBuilder.length() - 1);

            //组装一下
            strBuilder.insert(0, "【");
            strBuilder.append("】已绑定能源");
            ErrorCode errorCode = new ErrorCode(1_001_301_104, strBuilder.toString());
            throw exception(errorCode);
        }
    }
}