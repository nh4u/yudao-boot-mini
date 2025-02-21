package cn.bitlinks.ems.module.power.service.warningstrategy;

import cn.bitlinks.ems.framework.common.util.object.PageUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.bitlinks.ems.module.system.api.user.AdminUserApi;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.warningstrategy.WarningStrategyDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;

import cn.bitlinks.ems.module.power.dal.mysql.warningstrategy.WarningStrategyMapper;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

/**
 * 告警策略 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class WarningStrategyServiceImpl implements WarningStrategyService {

    @Resource
    private WarningStrategyMapper warningStrategyMapper;

    @Override
    public Long createWarningStrategy(WarningStrategySaveReqVO createReqVO) {
        // 插入
        WarningStrategyDO warningStrategy = BeanUtils.toBean(createReqVO, WarningStrategyDO.class);
        warningStrategyMapper.insert(warningStrategy);
        // 返回
        return warningStrategy.getId();
    }

    @Override
    public void updateWarningStrategy(WarningStrategySaveReqVO updateReqVO) {
        // 校验存在
        validateWarningStrategyExists(updateReqVO.getId());
        // 更新
        WarningStrategyDO updateObj = BeanUtils.toBean(updateReqVO, WarningStrategyDO.class);
        warningStrategyMapper.updateById(updateObj);
    }

    @Override
    public void deleteWarningStrategy(Long id) {
        // 校验存在
        validateWarningStrategyExists(id);
        // 删除
        warningStrategyMapper.deleteById(id);
    }

    private void validateWarningStrategyExists(Long id) {
        if (warningStrategyMapper.selectById(id) == null) {
            throw exception(WARNING_STRATEGY_NOT_EXISTS);
        }
    }

    @Override
    public WarningStrategyDO getWarningStrategy(Long id) {
        return warningStrategyMapper.selectById(id);
    }

    @Override
    public PageResult<WarningStrategyRespVO> getWarningStrategyPage(WarningStrategyPageReqVO pageReqVO) {

        Long count = warningStrategyMapper.getCount(pageReqVO);
        if (Objects.isNull(count) || count == 0L) {
            return new PageResult<>();
        }
        List<WarningStrategyRespVO> deviceApiResVOS = warningStrategyMapper.getPage(pageReqVO, PageUtils.getStart(pageReqVO));

        PageResult<WarningStrategyRespVO> result = new PageResult<>();
        result.setList(deviceApiResVOS);
        result.setTotal(count);
        return result;

    }

    @Override
    public void deleteWarningStrategyBatch(List<Long> ids) {
        warningStrategyMapper.deleteByIds(ids);
    }

    @Override
    public void updateWarningStrategyStatusBatch(WarningStrategyBatchUpdStatusReqVO updateReqVO) {
        warningStrategyMapper.update(new LambdaUpdateWrapper<>(WarningStrategyDO.class)
                .in(WarningStrategyDO::getId, updateReqVO.getIds())
                .set(WarningStrategyDO::getStatus, updateReqVO.getStatus()));
    }

    @Override
    public void updateWarningStrategyIntervalBatch(WarningStrategyBatchUpdIntervalReqVO updateReqVO) {
        warningStrategyMapper.update(new LambdaUpdateWrapper<>(WarningStrategyDO.class)
                .in(WarningStrategyDO::getId, updateReqVO.getIds())
                .set(WarningStrategyDO::getInterval, updateReqVO.getInterval())
                .set(WarningStrategyDO::getIntervalUnit, updateReqVO.getIntervalUnit())
        );
    }

}