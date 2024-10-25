package cn.bitlinks.ems.module.power.service.coalfactorhistory;

import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationSaveReqVO;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import cn.bitlinks.ems.module.power.controller.admin.coalfactorhistory.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.coalfactorhistory.CoalFactorHistoryDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;

import cn.bitlinks.ems.module.power.dal.mysql.coalfactorhistory.CoalFactorHistoryMapper;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

/**
 * 折标煤系数历史 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class CoalFactorHistoryServiceImpl implements CoalFactorHistoryService {

    @Resource
    private CoalFactorHistoryMapper coalFactorHistoryMapper;
    @Resource
    private EnergyConfigurationService energyConfigurationService;

    @Override
    public Long createCoalFactorHistory(CoalFactorHistorySaveReqVO createReqVO) {
        //// 查询当前能源ID下最新的折标煤系数记录
        CoalFactorHistoryDO latestCoalFactorHistory = coalFactorHistoryMapper.findLatestByEnergyId(createReqVO.getEnergyId());

        // 如果存在最新的记录，更新其生效结束时间
        if (latestCoalFactorHistory != null) {
            latestCoalFactorHistory.setEndTime(LocalDateTime.now()); // 设置为当前时间
            coalFactorHistoryMapper.updateEndTime(latestCoalFactorHistory);
        }


        // 插入新数据
        CoalFactorHistoryDO coalFactorHistory = BeanUtils.toBean(createReqVO, CoalFactorHistoryDO.class);
        coalFactorHistory.setStartTime(LocalDateTime.now()); // 设置为当前时间
        coalFactorHistory.setEndTime(null); // null，表示“至今”
        coalFactorHistoryMapper.insert(coalFactorHistory);

        // 更新能源配置
        EnergyConfigurationSaveReqVO updateVo = new EnergyConfigurationSaveReqVO();
        updateVo.setId(coalFactorHistory.getEnergyId());
        updateVo.setFactor(coalFactorHistory.getFactor());
        if (createReqVO.getEnergyId() != null) {
            energyConfigurationService.updateEnergyConfiguration(updateVo);
        }

        // 返回
        return coalFactorHistory.getId();
    }

    @Override
    public void updateCoalFactorHistory(CoalFactorHistorySaveReqVO updateReqVO) {
        // 校验存在
        validateCoalFactorHistoryExists(updateReqVO.getId());
        // 更新
        CoalFactorHistoryDO updateObj = BeanUtils.toBean(updateReqVO, CoalFactorHistoryDO.class);
        coalFactorHistoryMapper.updateById(updateObj);
    }

    @Override
    public void deleteCoalFactorHistory(Long id) {
        // 校验存在
        validateCoalFactorHistoryExists(id);
        // 删除
        coalFactorHistoryMapper.deleteById(id);
    }

    private void validateCoalFactorHistoryExists(Long id) {
        if (coalFactorHistoryMapper.selectById(id) == null) {
            throw exception(COAL_FACTOR_HISTORY_NOT_EXISTS);
        }
    }

    @Override
    public CoalFactorHistoryDO getCoalFactorHistory(Long id) {
        return coalFactorHistoryMapper.selectById(id);
    }

    @Override
    public PageResult<CoalFactorHistoryDO> getCoalFactorHistoryPage(CoalFactorHistoryPageReqVO pageReqVO) {
        return coalFactorHistoryMapper.selectPage(pageReqVO);
    }

}