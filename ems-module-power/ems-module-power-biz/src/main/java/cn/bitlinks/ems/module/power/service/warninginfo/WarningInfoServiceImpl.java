package cn.bitlinks.ems.module.power.service.warninginfo;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.warninginfo.WarningInfoDO;
import cn.bitlinks.ems.module.power.dal.mysql.warninginfo.WarningInfoMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.framework.web.core.util.WebFrameworkUtils.getLoginUserId;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.WARNING_INFO_NOT_EXISTS;

/**
 * 告警信息 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class WarningInfoServiceImpl implements WarningInfoService {

    @Resource
    private WarningInfoMapper warningInfoMapper;
//    @Resource
//    private WarningInfoUserMapper warningInfoUserMapper;


    private void validateWarningInfoExists(Long id) {
        if (warningInfoMapper.selectById(id) == null) {
            throw exception(WARNING_INFO_NOT_EXISTS);
        }
    }

    @Override
    public WarningInfoDO getWarningInfo(Long id) {
        return warningInfoMapper.selectById(id);
    }


    @Override
    public PageResult<WarningInfoDO> getWarningInfoPage(WarningInfoPageReqVO pageReqVO) {
//        // 连表用户id查询分页
//        MPJLambdaWrapperX<WarningInfoDO> query = new MPJLambdaWrapperX<WarningInfoDO>()
//                .selectAll(WarningInfoDO.class)
//                .eqIfPresent(WarningInfoDO::getLevel, pageReqVO.getLevel())
//                .betweenIfPresent(WarningInfoDO::getWarningTime, pageReqVO.getWarningTime())
//                .eqIfPresent(WarningInfoDO::getStatus, pageReqVO.getStatus())
//                .likeIfPresent(WarningInfoDO::getDeviceRel, pageReqVO.getDeviceRel())
//                .orderByDesc(WarningInfoDO::getWarningTime);
//        query.rightJoin(WarningInfoUserDO.class, WarningInfoUserDO::getInfoId, WarningInfoDO::getId)
//                .eq(WarningInfoUserDO::getUserId, getLoginUserId());
//
//        return warningInfoMapper.selectJoinPage(pageReqVO, WarningInfoDO.class, query);
        return warningInfoMapper.selectPage(pageReqVO);
    }

    @Override
    public WarningInfoStatisticsRespVO statistics() {
        return warningInfoMapper.countWarningsByLevel(getLoginUserId());
    }

    @Override
    public void updateWarningInfoStatus(WarningInfoStatusUpdReqVO updateReqVO) {
        // 校验存在
        validateWarningInfoExists(updateReqVO.getId());
        // 与产品协定修改逻辑：1.不同用户的告警消息，按照告警时间和告警规则一起更新处理状态，2. 过去时间的相同策略的告警信息如果未处理不需要更新处理状态。
        WarningInfoDO warningInfo = warningInfoMapper.selectById(updateReqVO.getId());

        warningInfoMapper.update(new LambdaUpdateWrapper<WarningInfoDO>()
                .set(WarningInfoDO::getStatus, updateReqVO.getStatus())
                .set(WarningInfoDO::getHandleOpinion, updateReqVO.getHandleOpinion())
                .eq(WarningInfoDO::getWarningTime, warningInfo.getWarningTime())
                .eq(WarningInfoDO::getStrategyId, warningInfo.getStrategyId()));

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarningInfoStatusBatch(WarningInfoStatusBatchUpdReqVO reqVO) {
        // 1) 批量存在性校验：一次查出所有 id
        List<WarningInfoDO> picked = warningInfoMapper.selectBatchIds(reqVO.getIds());
        if (picked == null || picked.isEmpty() || picked.size() != reqVO.getIds().size()) {
            // 与你的单条风格保持一致
            throw exception(WARNING_INFO_NOT_EXISTS);
        }

        // 2) 组装“去重后的组”：按 (strategyId, warningTime)
        //    ——这就是“与单条一致”的关键差异：只更新与被点记录同“告警时间+策略”的那批
        Map<String, WarningInfoGroupDTO> groupMap = new LinkedHashMap<>();
        for (WarningInfoDO it : picked) {
            Long strategyId = it.getStrategyId();
            LocalDateTime warningTime = it.getWarningTime();
            if (strategyId == null || warningTime == null) {
                continue;
            }
            String key = strategyId + "|" + warningTime;
            groupMap.putIfAbsent(key, new WarningInfoGroupDTO(strategyId, warningTime));
        }
        if (groupMap.isEmpty()) {
            return; // 无有效组，不更新
        }
        List<WarningInfoGroupDTO> groups = new ArrayList<>(groupMap.values());

        // 3) 批量更新
        warningInfoMapper.updateStatusAndOpinionByGroups(
                reqVO.getStatus(),
                reqVO.getHandleOpinion(),
                groups
        );
    }

    @Override
    public List<WarningInfoDO> getWarningList() {
        return warningInfoMapper.getWarningList();
    }

}