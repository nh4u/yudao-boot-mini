package cn.bitlinks.ems.module.power.service.coalfactorhistory;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.coalfactorhistory.vo.CoalFactorHistoryPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.coalfactorhistory.vo.CoalFactorHistorySaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.coalfactorhistory.CoalFactorHistoryDO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.mysql.coalfactorhistory.CoalFactorHistoryMapper;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.framework.security.core.util.SecurityFrameworkUtils.getLoginUserNickname;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.COAL_FACTOR_HISTORY_NOT_EXISTS;

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

//    @Override
//    public Long createCoalFactorHistory(CoalFactorHistorySaveReqVO createReqVO) {
//        //// 查询当前能源ID下最新的折标煤系数记录
//        CoalFactorHistoryDO latestCoalFactorHistory = coalFactorHistoryMapper.findLatestByEnergyId(createReqVO.getEnergyId());
//
//        // 如果存在最新的记录，更新其生效结束时间
//        if (latestCoalFactorHistory != null) {
//            latestCoalFactorHistory.setEndTime(LocalDateTime.now()); // 设置为当前时间
//            coalFactorHistoryMapper.updateEndTime(latestCoalFactorHistory);
//        }
//
//
//        // 插入新数据
//        CoalFactorHistoryDO coalFactorHistory = BeanUtils.toBean(createReqVO, CoalFactorHistoryDO.class);
//        coalFactorHistory.setStartTime(LocalDateTime.now()); // 设置为当前时间
//        coalFactorHistory.setEndTime(null); // null，表示“至今”
//        coalFactorHistoryMapper.insert(coalFactorHistory);
//
//        // 更新能源配置
//        EnergyConfigurationSaveReqVO updateVo = new EnergyConfigurationSaveReqVO();
//        updateVo.setId(coalFactorHistory.getEnergyId());
//        updateVo.setFactor(coalFactorHistory.getFactor());
//        if (createReqVO.getEnergyId() != null) {
//            energyConfigurationService.updateEnergyConfiguration(updateVo);
//        }
//
//        // 返回
//        return coalFactorHistory.getId();
//    }
//
//    @DS("slave")
//    @Override
//    public Long createCoalFactorHistory07(CoalFactorHistorySaveReqVO createReqVO) {
//        // 插入新数据
//        CoalFactorHistoryDO coalFactorHistory = BeanUtils.toBean(createReqVO, CoalFactorHistoryDO.class);
//        coalFactorHistory.setStartTime(LocalDateTime.now()); // 设置为当前时间
//        coalFactorHistory.setEndTime(null); // null，表示“至今”
//        return (long) coalFactorHistoryMapper.insert(coalFactorHistory);
//
//
//    }



    @Override
    public Long createCoalFactorHistory(CoalFactorHistorySaveReqVO createReqVO, boolean use3307) {
        if (use3307) {
            // 切换到 3307 数据源创建记录
            return createCoalFactorHistory07(createReqVO);
        } else {
            // 默认数据源创建记录
            return createCoalFactorHistory(createReqVO);
        }
    }


    public Long createCoalFactorHistory(CoalFactorHistorySaveReqVO createReqVO) {
        // 查询当前能源ID下最新的折标煤系数记录
        CoalFactorHistoryDO latestCoalFactorHistory = coalFactorHistoryMapper.findLatestByEnergyId(createReqVO.getEnergyId());

        // 如果存在最新的记录，更新其生效结束时间
        if (latestCoalFactorHistory != null) {
            latestCoalFactorHistory.setEndTime(LocalDateTime.now()); // 设置为当前时间
            coalFactorHistoryMapper.updateEndTime(latestCoalFactorHistory);
        }

        // 插入新数据
        String nickname = getLoginUserNickname();
        CoalFactorHistoryDO coalFactorHistory = BeanUtils.toBean(createReqVO, CoalFactorHistoryDO.class);
        Long energyId = coalFactorHistory.getEnergyId();
        coalFactorHistory.setStartTime(LocalDateTime.now()); // 设置为当前时间
        coalFactorHistory.setEndTime(null); // null，表示“至今“
        coalFactorHistory.setUpdater(nickname);
        EnergyConfigurationDO energyConfiguration=energyConfigurationService.getEnergyConfiguration(energyId);
        coalFactorHistory.setFormula(energyConfiguration.getCoalFormula());
        coalFactorHistoryMapper.insert(coalFactorHistory);

        // 更新能源配置
        EnergyConfigurationSaveReqVO updateVo = new EnergyConfigurationSaveReqVO();
        updateVo.setId(energyId);
        updateVo.setFactor(coalFactorHistory.getFactor());
        if (createReqVO.getEnergyId() != null) {
            energyConfigurationService.updateEnergyConfiguration(updateVo);
        }

        // 返回生成的 ID
        return coalFactorHistory.getId();
    }

    @DS("slave")
    @Override
    public Long createCoalFactorHistory07(CoalFactorHistorySaveReqVO createReqVO) {
        // 插入新数据
        CoalFactorHistoryDO coalFactorHistory = BeanUtils.toBean(createReqVO, CoalFactorHistoryDO.class);
        coalFactorHistory.setStartTime(LocalDateTime.now()); // 设置为当前时间
        coalFactorHistory.setEndTime(null); // null，表示“至今”
        coalFactorHistoryMapper.insert(coalFactorHistory);

        // 返回生成的 ID
        return coalFactorHistory.getId();
    }



//    @Override
//    public void updateCoalFactorHistory(CoalFactorHistorySaveReqVO updateReqVO) {
//        // 校验存在
//        validateCoalFactorHistoryExists(updateReqVO.getId());
//        // 更新
//        CoalFactorHistoryDO updateObj = BeanUtils.toBean(updateReqVO, CoalFactorHistoryDO.class);
//        coalFactorHistoryMapper.updateById(updateObj);
//    }

    @Override
        public void updateCoalFactorHistory(CoalFactorHistorySaveReqVO updateReqVO) {
        // 校验记录是否存在
        validateCoalFactorHistoryExists(updateReqVO.getId());
        // 更新数据
        CoalFactorHistoryDO updateObj = BeanUtils.toBean(updateReqVO, CoalFactorHistoryDO.class);
        coalFactorHistoryMapper.updateById(updateObj);
    }

    @DS("slave")
    public void updateCoalFactorHistory07(CoalFactorHistorySaveReqVO updateReqVO) {
        // 校验记录是否存在（切换到 3307 数据源）
        validateCoalFactorHistoryExists(updateReqVO.getId());
        // 更新数据
        CoalFactorHistoryDO updateObj = BeanUtils.toBean(updateReqVO, CoalFactorHistoryDO.class);
        coalFactorHistoryMapper.updateById(updateObj);
    }

    private void validateCoalFactorHistoryExists3307(Long id) {
        if (coalFactorHistoryMapper.selectById(id) == null) {
            throw exception(COAL_FACTOR_HISTORY_NOT_EXISTS);
        }
    }


    @Override
    public void deleteCoalFactorHistory(Long id) {
        // 校验存在
        validateCoalFactorHistoryExists(id);
        // 删除
        coalFactorHistoryMapper.deleteById(id);
    }

    @DS("slave")
    @Override
    public void deleteCoalFactorHistory07(Long id) {
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

    @DS("slave")
    @Override
    public CoalFactorHistoryDO getCoalFactorHistory07(Long id) {
        return coalFactorHistoryMapper.selectById(id);
    }

    @Override
    public PageResult<CoalFactorHistoryDO> getCoalFactorHistoryPage(CoalFactorHistoryPageReqVO pageReqVO) {
        return coalFactorHistoryMapper.selectPage(pageReqVO);
    }

}