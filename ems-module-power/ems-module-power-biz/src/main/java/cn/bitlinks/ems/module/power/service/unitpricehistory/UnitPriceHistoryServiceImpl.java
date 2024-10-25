package cn.bitlinks.ems.module.power.service.unitpricehistory;

import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationSaveReqVO;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import cn.bitlinks.ems.module.power.controller.admin.unitpricehistory.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.unitpricehistory.UnitPriceHistoryDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;

import cn.bitlinks.ems.module.power.dal.mysql.unitpricehistory.UnitPriceHistoryMapper;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

/**
 * 单价历史 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class UnitPriceHistoryServiceImpl implements UnitPriceHistoryService {

    @Resource
    private UnitPriceHistoryMapper unitPriceHistoryMapper;
    @Resource
    private EnergyConfigurationService energyConfigurationService;

    @Override
    public Long createUnitPriceHistory(UnitPriceHistorySaveReqVO createReqVO) {
        // 插入
        UnitPriceHistoryDO unitPriceHistory = BeanUtils.toBean(createReqVO, UnitPriceHistoryDO.class);
        unitPriceHistoryMapper.insert(unitPriceHistory);
        // 返回
        return unitPriceHistory.getId();
    }

    @Override
    public void updateUnitPriceHistory(UnitPriceHistorySaveReqVO updateReqVO) {
        // 校验存在
        validateUnitPriceHistoryExists(updateReqVO.getId());
        // 更新
        UnitPriceHistoryDO updateObj = BeanUtils.toBean(updateReqVO, UnitPriceHistoryDO.class);
        unitPriceHistoryMapper.updateById(updateObj);
    }

    @Override
    public void deleteUnitPriceHistory(Long id) {
        // 校验存在
        validateUnitPriceHistoryExists(id);
        // 删除
        unitPriceHistoryMapper.deleteById(id);
    }

    private void validateUnitPriceHistoryExists(Long id) {
        if (unitPriceHistoryMapper.selectById(id) == null) {
            throw exception(UNIT_PRICE_HISTORY_NOT_EXISTS);
        }
    }

    @Override
    public UnitPriceHistoryDO getUnitPriceHistory(Long id) {
        return unitPriceHistoryMapper.selectById(id);
    }

    @Override
    public PageResult<UnitPriceHistoryDO> getUnitPriceHistoryPage(UnitPriceHistoryPageReqVO pageReqVO) {
        return unitPriceHistoryMapper.selectPage(pageReqVO);
    }

}