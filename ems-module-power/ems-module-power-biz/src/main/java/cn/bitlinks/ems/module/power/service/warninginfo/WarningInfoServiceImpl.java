package cn.bitlinks.ems.module.power.service.warninginfo;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.WarningInfoPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.WarningInfoStatisticsRespVO;
import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.WarningInfoStatusUpdReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.warninginfo.WarningInfoDO;
import cn.bitlinks.ems.module.power.dal.dataobject.warninginfo.WarningInfoUserDO;
import cn.bitlinks.ems.module.power.dal.mysql.warninginfo.WarningInfoMapper;
import cn.bitlinks.ems.module.power.dal.mysql.warninginfo.WarningInfoUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

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
    @Resource
    private WarningInfoUserMapper warningInfoUserMapper;


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


}