package cn.bitlinks.ems.module.power.service.additionalrecording;

import cn.bitlinks.ems.module.power.dal.mysql.standingbook.type.StandingbookTypeMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.additionalrecording.AdditionalRecordingDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
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
    @Resource
    private StandingbookTypeMapper standingbookTypeMapper;

    @Override
    public List<Long> createAdditionalRecording(List<AdditionalRecordingSaveReqVO> createReqVOs) {
        List<Long> createdIds = new ArrayList<>();

        for (AdditionalRecordingSaveReqVO createReqVO : createReqVOs) {
            AdditionalRecordingLastVO lastRecord = getLastRecord(createReqVO.getStandingbookId(),createReqVO.getThisCollectTime());
            String valueType = standingbookTypeMapper.selectAttributeValueByCode(createReqVO.getStandingbookId(), "valueType");
            createReqVO.setValueType(valueType);
            // 设置“上次采集时间”和“上次数值”
            if (lastRecord != null) {
                createReqVO.setLastCollectTime(lastRecord.getLastCollectTime());
                createReqVO.setLastValue(lastRecord.getLastValue());
            } else {
                // 如果没有记录，可以设置为null或根据业务需求处理
                createReqVO.setLastCollectTime(null);
                createReqVO.setLastValue(null);
            }

            AdditionalRecordingDO additionalRecording = BeanUtils.toBean(createReqVO, AdditionalRecordingDO.class);

            // 设置录入时间为当前时间
            additionalRecording.setEnterTime(LocalDateTime.now());

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
    public AdditionalRecordingLastVO getLastRecord(Long standingbookId, LocalDateTime currentCollectTime) {
        QueryWrapper<AdditionalRecordingDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("this_collect_time", "this_value")
                .eq("standingbook_id", standingbookId)
                .lt("this_collect_time", currentCollectTime)
                .orderByDesc("this_collect_time")
                .last("LIMIT 1");

        AdditionalRecordingDO lastRecord = additionalRecordingMapper.selectOne(queryWrapper);

        return new AdditionalRecordingLastVO()
                .setLastCollectTime(lastRecord != null ? lastRecord.getThisCollectTime() : null)
                .setLastValue(lastRecord != null ? lastRecord.getThisValue() : null);
    }

    @Override
    public void updateAdditionalRecording(AdditionalRecordingSaveReqVO updateReqVO) {
        // 校验存在
        validateAdditionalRecordingExists(updateReqVO.getId());
        updateReqVO.setEnterTime(LocalDateTime.now());
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
            LocalDateTime startEnterTime, LocalDateTime endEnterTime) {
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
        if (startEnterTime != null && endEnterTime != null) {
            queryWrapper.ge("enter_time", startEnterTime);
            queryWrapper.le("enter_time", endEnterTime);
        }
        return additionalRecordingMapper.selectList(queryWrapper);
    }
}