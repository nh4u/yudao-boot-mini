package cn.bitlinks.ems.module.power.service.unitpriceconfiguration;

import cn.bitlinks.ems.module.power.controller.admin.unitpricehistory.vo.UnitPriceHistoryRespVO;
import cn.bitlinks.ems.module.power.controller.admin.unitpricehistory.vo.UnitPriceHistorySaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.unitpricehistory.UnitPriceHistoryDO;
import cn.bitlinks.ems.module.power.dal.mysql.unitpricehistory.UnitPriceHistoryMapper;
import cn.bitlinks.ems.module.power.service.unitpricehistory.UnitPriceHistoryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.unitpriceconfiguration.UnitPriceConfigurationDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;

import cn.bitlinks.ems.module.power.dal.mysql.unitpriceconfiguration.UnitPriceConfigurationMapper;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

/**
 * 单价配置 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class UnitPriceConfigurationServiceImpl implements UnitPriceConfigurationService {

    @Resource
    private UnitPriceConfigurationMapper unitPriceConfigurationMapper;
    @Resource
    private UnitPriceHistoryService unitPriceHistoryService;
    @Resource
    private UnitPriceHistoryMapper unitPriceHistoryMapper;

    @Override
    public List<Long> createUnitPriceConfigurations(Long energyId, List<UnitPriceConfigurationSaveReqVO> createReqVOList) {
        List<Long> ids = new ArrayList<>();
        for (UnitPriceConfigurationSaveReqVO createReqVO : createReqVOList) {
            // 设置能源ID
            createReqVO.setEnergyId(energyId);

            // 处理时间范围
            if (createReqVO.getTimeRange() != null && createReqVO.getTimeRange().size() == 2) {
                createReqVO.setStartTime(createReqVO.getTimeRange().get(0));
                createReqVO.setEndTime(createReqVO.getTimeRange().get(1));
            }

            // 检查时间冲突
            if (isTimeConflict(energyId, createReqVO.getStartTime(), createReqVO.getEndTime())) {
                // 时间冲突，抛出异常或处理逻辑
                throw exception(TimeConflict);
            }

            // 插入
            UnitPriceConfigurationDO unitPriceConfiguration = BeanUtils.toBean(createReqVO, UnitPriceConfigurationDO.class);
            unitPriceConfigurationMapper.insertOrUpdate(unitPriceConfiguration);

            // 插入历史记录
            UnitPriceHistorySaveReqVO unitPriceHistory = BeanUtils.toBean(createReqVO, UnitPriceHistorySaveReqVO.class);
            unitPriceHistoryService.createUnitPriceHistory(unitPriceHistory);

            // 添加ID到列表
            ids.add(unitPriceConfiguration.getId());
        }
        return ids;
    }

    private boolean isTimeConflict(Long energyId, LocalDateTime startTime, LocalDateTime endTime) {
        List<UnitPriceConfigurationDO> existingConfigs = unitPriceConfigurationMapper.findByEnergyId(energyId);
        for (UnitPriceConfigurationDO config : existingConfigs) {
            if ((startTime.isBefore(config.getEndTime()) && endTime.isAfter(config.getStartTime())) ||
                    startTime.equals(config.getStartTime()) || endTime.equals(config.getEndTime())) {
                // 时间范围重叠
                return true;
            }
        }
        // 没有发现时间冲突
        return false;
    }

    @Override
    public void updateUnitPriceConfiguration(UnitPriceConfigurationSaveReqVO updateReqVO) {
        // 校验存在
        validateUnitPriceConfigurationExists(updateReqVO.getId());
        // 检查时间冲突
        if (isTimeConflict(updateReqVO.getEnergyId(), updateReqVO.getStartTime(), updateReqVO.getEndTime())) {
            // 时间冲突，抛出异常或处理逻辑
            throw exception(TimeConflict);
        }
        // 更新
        UnitPriceConfigurationDO updateObj = BeanUtils.toBean(updateReqVO, UnitPriceConfigurationDO.class);
        unitPriceConfigurationMapper.updateById(updateObj);
    }

    @Override
    public void deleteUnitPriceConfiguration(Long id) {
        // 校验存在
        validateUnitPriceConfigurationExists(id);
        // 删除
        unitPriceConfigurationMapper.deleteById(id);
    }

    private void validateUnitPriceConfigurationExists(Long id) {
        if (unitPriceConfigurationMapper.selectById(id) == null) {
            throw exception(UNIT_PRICE_CONFIGURATION_NOT_EXISTS);
        }
    }

    @Override
    public UnitPriceConfigurationDO getUnitPriceConfiguration(Long id) {
        return unitPriceConfigurationMapper.selectById(id);
    }

    @Override
    public PageResult<UnitPriceConfigurationDO> getUnitPriceConfigurationPage(UnitPriceConfigurationPageReqVO pageReqVO) {
        return unitPriceConfigurationMapper.selectPage(pageReqVO);
    }

}