package cn.bitlinks.ems.module.power.service.additionalrecording;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.additionalrecording.AdditionalRecordingDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;

import cn.bitlinks.ems.module.power.dal.mysql.additionalrecording.AdditionalRecordingMapper;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

/**
 * 补录 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class AdditionalRecordingServiceImpl implements AdditionalRecordingService {

    @Resource
    private AdditionalRecordingMapper additionalRecordingMapper;

    @Override
    public List<Long> createAdditionalRecording(List<AdditionalRecordingSaveReqVO> createReqVOs) {
        List<Long> createdIds = new ArrayList<>();

        // 查询最后采集点的数据
        QueryWrapper<AdditionalRecordingDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("this_collect_time");
        queryWrapper.last("LIMIT 1");
        AdditionalRecordingDO lastRecord = additionalRecordingMapper.selectOne(queryWrapper);

        for (AdditionalRecordingSaveReqVO createReqVO : createReqVOs) {
            // 设置“上次采集时间”和“上次数值”
            if (lastRecord != null) {
                createReqVO.setLastCollectTime(lastRecord.getThisCollectTime());
                createReqVO.setLastValue(lastRecord.getThisValue());
            }

            AdditionalRecordingDO additionalRecording = BeanUtils.toBean(createReqVO, AdditionalRecordingDO.class);

            // 根据 voucherId 存在与否设置补录方式
            if (createReqVO.getVoucherId() != null) {
                additionalRecording.setRecordMethod(2); // 凭证信息导入
                additionalRecording.setVoucherId(createReqVO.getVoucherId());
            } else {
                additionalRecording.setRecordMethod(1); // 手动录入
            }

            // 插入数据库
            additionalRecordingMapper.insert(additionalRecording);
            createdIds.add(additionalRecording.getId());
        }
        return createdIds;
    }

    @Override
    public void updateAdditionalRecording(AdditionalRecordingSaveReqVO updateReqVO) {
        // 校验存在
        validateAdditionalRecordingExists(updateReqVO.getId());
        // 更新
        AdditionalRecordingDO updateObj = BeanUtils.toBean(updateReqVO, AdditionalRecordingDO.class);
        additionalRecordingMapper.updateById(updateObj);
    }

    @Override
    public void deleteAdditionalRecording(Long id) {
        // 校验存在
        validateAdditionalRecordingExists(id);
        // 删除
        additionalRecordingMapper.deleteById(id);
    }

    private void validateAdditionalRecordingExists(Long id) {
        if (additionalRecordingMapper.selectById(id) == null) {
            throw exception(ADDITIONAL_RECORDING_NOT_EXISTS);
        }
    }

    @Override
    public AdditionalRecordingDO getAdditionalRecording(Long id) {
        return additionalRecordingMapper.selectById(id);
    }

    @Override
    public PageResult<AdditionalRecordingDO> getAdditionalRecordingPage(AdditionalRecordingPageReqVO pageReqVO) {
        return additionalRecordingMapper.selectPage(pageReqVO);
    }

    @Override
    public List<AdditionalRecordingDO> selectByCondition(
            BigDecimal minThisValue, BigDecimal maxThisValue,
            String recordPerson,
            Integer recordMethod,
            LocalDateTime startThisCollectTime, LocalDateTime endThisCollectTime,
            LocalDateTime startCreateTime, LocalDateTime endCreateTime) {
        QueryWrapper<AdditionalRecordingDO> queryWrapper = new QueryWrapper<>();
        // 本次数值范围查询
        if (minThisValue != null && maxThisValue != null) {
            queryWrapper.ge("this_value", minThisValue);
            queryWrapper.le("this_value", maxThisValue);
        }
        // 补录人查询
        if (recordPerson != null && !recordPerson.isEmpty()) {
            queryWrapper.eq("record_person", recordPerson);
        }
        // 补录方式查询
        if (recordMethod != null) {
            queryWrapper.eq("record_method", recordMethod);
        }
        // 本次采集时间范围查询
        if (startThisCollectTime != null && endThisCollectTime != null) {
            queryWrapper.ge("this_collect_time", startThisCollectTime);
            queryWrapper.le("this_collect_time", endThisCollectTime);
        }
        // 创建时间范围查询
        if (startCreateTime != null && endCreateTime != null) {
            queryWrapper.ge("create_time", startCreateTime);
            queryWrapper.le("create_time", endCreateTime);
        }
        return additionalRecordingMapper.selectList(queryWrapper);
    }
}