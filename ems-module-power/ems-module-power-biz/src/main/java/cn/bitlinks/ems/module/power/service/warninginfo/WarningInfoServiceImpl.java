package cn.bitlinks.ems.module.power.service.warninginfo;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.warninginfo.WarningInfoDO;
import cn.bitlinks.ems.module.power.dal.dataobject.warninginfo.WarningInfoUserDO;
import cn.bitlinks.ems.module.power.dal.mysql.warninginfo.WarningInfoMapper;
import cn.bitlinks.ems.module.power.enums.warninginfo.WarningInfoLevelEnum;
import cn.bitlinks.ems.module.power.enums.warninginfo.WarningInfoStatusEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        // 连表用户id查询分页
        MPJLambdaWrapperX<WarningInfoDO> query = new MPJLambdaWrapperX<WarningInfoDO>()
                .selectAll(WarningInfoDO.class)
                .eqIfPresent(WarningInfoDO::getLevel, pageReqVO.getLevel())
                .betweenIfPresent(WarningInfoDO::getWarningTime, pageReqVO.getWarningTime())
                .eqIfPresent(WarningInfoDO::getStatus, pageReqVO.getStatus())
                .likeIfPresent(WarningInfoDO::getDeviceRel, pageReqVO.getDeviceRel())
                .orderByDesc(WarningInfoDO::getWarningTime);
        query.rightJoin(WarningInfoUserDO.class, WarningInfoUserDO::getInfoId, WarningInfoDO::getId)
                .eq(WarningInfoUserDO::getUserId, getLoginUserId());

        return warningInfoMapper.selectJoinPage(pageReqVO, WarningInfoDO.class, query);
    }

    @Override
    public WarningInfoStatisticsRespVO statistics() {
        return warningInfoMapper.countWarningsByLevel(getLoginUserId());
    }

    @Override
    public void updateWarningInfoStatus(WarningInfoStatusUpdReqVO updateReqVO) {
        // 校验存在
        validateWarningInfoExists(updateReqVO.getId());
        // 更新处理状态
        warningInfoMapper.updateStatusById(updateReqVO.getId(), updateReqVO.getStatus());

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarningInfoStatusBatch(WarningInfoStatusBatchUpdReqVO reqVO) {
        warningInfoMapper.updateStatusAndOpinionByGroups(reqVO.getStatus(), reqVO.getHandleOpinion(), reqVO.getIds());
    }

    @Override
    public List<WarningInfoDO> getWarningList() {
        return warningInfoMapper.getWarningList();
    }

    @Override
    public List<WarningInfoDO> getMonitorListBySbCode(LocalDateTime[] range, String sbCode) {

        return warningInfoMapper.selectList(new LambdaQueryWrapperX<WarningInfoDO>()
                .betweenIfPresent(WarningInfoDO::getWarningTime, range)
                .like(WarningInfoDO::getDeviceRel, "(" + sbCode + ")")
                .orderByDesc(WarningInfoDO::getCreateTime)
        );
    }

    @Override
    public WarningInfoMonitorStatisticsRespVO getMonitorStatisticsBySbCode(String sbCode) {
        WarningInfoStatisticsRespVO resp;
        if (StringUtils.isEmpty(sbCode)) {
            resp = warningInfoMapper.getMonitorStatisticsBySbCode(sbCode);
            if (resp == null) {
                resp = new WarningInfoStatisticsRespVO();
            }
        } else {
            resp = new WarningInfoStatisticsRespVO();
        }
        WarningInfoMonitorStatisticsRespVO respVO = new WarningInfoMonitorStatisticsRespVO();
        respVO.setTotal(resp.getTotal());
        List<WarningInfoStatisticsDetailRespVO> list = new ArrayList<>();
        list.add(new WarningInfoStatisticsDetailRespVO(WarningInfoLevelEnum.TIP.getDesc(), resp.getCount0()));
        list.add(new WarningInfoStatisticsDetailRespVO(WarningInfoLevelEnum.WARNING.getDesc(), resp.getCount1()));
        list.add(new WarningInfoStatisticsDetailRespVO(WarningInfoLevelEnum.MINOR.getDesc(), resp.getCount2()));
        list.add(new WarningInfoStatisticsDetailRespVO(WarningInfoLevelEnum.IMPORTANT.getDesc(), resp.getCount3()));
        list.add(new WarningInfoStatisticsDetailRespVO(WarningInfoLevelEnum.URGENT.getDesc(), resp.getCount4()));

        respVO.setList(list);
        return respVO;
    }

    @Override
    public long countMonitorBySbCode(String sbCode) {
        return warningInfoMapper.selectCount(new LambdaQueryWrapper<WarningInfoDO>()
                .like(WarningInfoDO::getDeviceRel, "(" + sbCode + ")")
                .ne(WarningInfoDO::getStatus, WarningInfoStatusEnum.PROCESSED.getCode()));
    }

}