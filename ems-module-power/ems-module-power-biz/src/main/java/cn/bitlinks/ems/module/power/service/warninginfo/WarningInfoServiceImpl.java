package cn.bitlinks.ems.module.power.service.warninginfo;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.WarningInfoPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.WarningInfoSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.WarningInfoStatisticsRespVO;
import cn.bitlinks.ems.module.power.dal.dataobject.warninginfo.WarningInfoDO;
import cn.bitlinks.ems.module.power.dal.mysql.warninginfo.WarningInfoMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
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

    @Override
    public Long createWarningInfo(WarningInfoSaveReqVO createReqVO) {
        // 插入
        WarningInfoDO warningInfo = BeanUtils.toBean(createReqVO, WarningInfoDO.class);
        warningInfoMapper.insert(warningInfo);
        // 返回
        return warningInfo.getId();
    }

    @Override
    public void updateWarningInfo(WarningInfoSaveReqVO updateReqVO) {
        // 校验存在
        validateWarningInfoExists(updateReqVO.getId());
        // 更新
        WarningInfoDO updateObj = BeanUtils.toBean(updateReqVO, WarningInfoDO.class);
        warningInfoMapper.updateById(updateObj);
    }

    @Override
    public void deleteWarningInfo(Long id) {
        // 校验存在
        validateWarningInfoExists(id);
        // 删除
        warningInfoMapper.deleteById(id);
    }

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
        return warningInfoMapper.selectPage(pageReqVO);
    }

    @Override
    public WarningInfoStatisticsRespVO statistics() {
        return warningInfoMapper.countWarningsByLevel();
    }


}