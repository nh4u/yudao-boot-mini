package cn.bitlinks.ems.module.power.service.additionalrecording;

import cn.bitlinks.ems.module.power.controller.admin.energyparameters.vo.EnergyParametersSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyparameters.EnergyParametersDO;
import cn.bitlinks.ems.module.power.dal.dataobject.voucher.VoucherDO;
import cn.bitlinks.ems.module.power.dal.mysql.energyconfiguration.EnergyConfigurationMapper;
import cn.bitlinks.ems.module.power.dal.mysql.energyparameters.EnergyParametersMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.type.StandingbookTypeMapper;
import cn.bitlinks.ems.module.power.service.voucher.VoucherService;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.nacos.common.utils.CollectionUtils;
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
import static cn.bitlinks.ems.framework.security.core.util.SecurityFrameworkUtils.getLoginUserNickname;
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
    @Resource
    private VoucherService voucherService;
    @Resource
    private EnergyConfigurationMapper energyConfigurationMapper;
    @Resource
    private EnergyParametersMapper energyParametersMapper;

    @Override
    public Long createAdditionalRecording(AdditionalRecordingSaveReqVO createReqVO) {
        if (isCollectTimeDuplicate(createReqVO.getStandingbookId(), createReqVO.getThisCollectTime())) {
            throw exception(THIS_TIME_EXISTS_DATA);
        }
        String energyId = standingbookTypeMapper.selectAttributeValueByCode(createReqVO.getStandingbookId(),"energy");

        List<EnergyParametersDO> parameters = energyParametersMapper.selectByEnergyId(Long.valueOf(energyId));
        Integer dataFeature = parseDataFeatureFromParams(parameters);

        if (dataFeature != null && dataFeature == 1) { // 1表示累积值
            if (StringUtils.isBlank(createReqVO.getValueType())) {
                throw exception(VALUE_TYPE_REQUIRED);
            }
        }

        if ("全量".equals(createReqVO.getValueType())) {
            checkMeterReadingValue(createReqVO.getStandingbookId(),
                    createReqVO.getThisCollectTime(),
                    createReqVO.getThisValue());
        }
        AdditionalRecordingDO additionalRecording = BeanUtils.toBean(createReqVO, AdditionalRecordingDO.class);
        if(createReqVO.getRecordPerson() == null){
            createReqVO.setRecordPerson(getLoginUserNickname());
        }

        String unit = energyConfigurationMapper.selectUnitByEnergyNameAndChinese(energyId);
        // 设置录入时间为当前时间
        additionalRecording.setEnterTime(LocalDateTime.now());
        additionalRecording.setRecordMethod(1); // 手动录入
        additionalRecording.setUnit(unit);
        // 插入数据库
        additionalRecordingMapper.insert(additionalRecording);

        return additionalRecording.getId();
    }

    private boolean isCollectTimeDuplicate(Long standingbookId, LocalDateTime collectTime) {
        QueryWrapper<AdditionalRecordingDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("standingbook_id", standingbookId)
                .eq("this_collect_time", collectTime);
        return additionalRecordingMapper.selectCount(queryWrapper) > 0;
    }

    private Integer parseDataFeatureFromParams(List<EnergyParametersDO> parameters) {
        return parameters.stream()
                .filter(p -> p.getUsage() != null && p.getUsage() == 1) // 用量参数
                .findFirst()
                .map(EnergyParametersDO::getDataFeature)
                .orElse(null); // 无用量参数返回null
    }

    /**
     * 校验抄表数值是否符合规则：本次值 ≥ 上次值 且 ≤ 下次值
     */
    private void checkMeterReadingValue(Long standingbookId, LocalDateTime currentCollectTime, BigDecimal currentValue) {

        // 获取上次记录（早于当前时间的最新记录）
        AdditionalRecordingDO lastRecord = getNeighborRecord(standingbookId, currentCollectTime, false);
        // 获取下次记录（晚于当前时间的最早记录）
        AdditionalRecordingDO nextRecord = getNeighborRecord(standingbookId, currentCollectTime, true);

        // 校验：当前值 ≥ 上次值
        if (lastRecord != null && currentValue.compareTo(lastRecord.getThisValue()) < 0) {
            throw exception(THIS_VALUE_NOT_LESS);
        }

        // 校验：当前值 ≤ 下次值
        if (nextRecord != null && currentValue.compareTo(nextRecord.getThisValue()) > 0) {
            throw exception(THIS_VALUE_NOT_MORE);
        }
    }

    /**
     * 获取相邻记录（上次或下次）
     * @param isNext true表示查询下次记录，false表示查询上次记录
     */
    private AdditionalRecordingDO getNeighborRecord(Long standingbookId, LocalDateTime currentCollectTime, boolean isNext) {
        QueryWrapper<AdditionalRecordingDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("standingbook_id", standingbookId)
                .eq("value_type", "全量")  // 只处理全量数据
                .select("this_collect_time", "this_value");

        // 时间条件：上次记录（<当前时间）或下次记录（>当前时间）
        if (isNext) {
            queryWrapper.gt("this_collect_time", currentCollectTime)
                    .orderByAsc("this_collect_time");
        } else {
            queryWrapper.lt("this_collect_time", currentCollectTime)
                    .orderByDesc("this_collect_time");
        }

        queryWrapper.last("LIMIT 1");
        return additionalRecordingMapper.selectOne(queryWrapper);
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
    public List<Long> createAdditionalRecordingByVoucherId(List<Long> voucherIds,Long standingbookId) {
        String nickname = getLoginUserNickname();
        String energyId = standingbookTypeMapper.selectAttributeValueByCode(standingbookId, "energy");
        String unit = energyConfigurationMapper.selectUnitByEnergyNameAndChinese(energyId);
        for(Long voucherId : voucherIds){
            VoucherDO voucherDO = voucherService.getVoucher(voucherId);
            if (voucherDO.getPurchaseTime().isAfter(LocalDateTime.now())) {
                throw exception(PURCHASE_TIME_OVER_CURRENT); // 替换为你的具体业务异常
            }
            AdditionalRecordingSaveReqVO saveReqVO = new AdditionalRecordingSaveReqVO();
            saveReqVO.setRecordMethod(2); //凭证补录
            saveReqVO.setThisValue(voucherDO.getUsage());
            saveReqVO.setThisCollectTime(voucherDO.getPurchaseTime());
            saveReqVO.setVoucherId(voucherId);
            saveReqVO.setUnit(unit);
            saveReqVO.setRecordPerson(nickname);
            saveReqVO.setStandingbookId(standingbookId);
            saveReqVO.setEnterTime(LocalDateTime.now());
            if (isCollectTimeDuplicate(saveReqVO.getStandingbookId(), saveReqVO.getThisCollectTime())) {
                throw exception(THIS_TIME_EXISTS_DATA);
            }
            if ("全量".equals(saveReqVO.getValueType())) {
                checkMeterReadingValue(saveReqVO.getStandingbookId(),
                        saveReqVO.getThisCollectTime(),
                        saveReqVO.getThisValue());
            }
            AdditionalRecordingDO additionalRecording = BeanUtils.toBean(saveReqVO, AdditionalRecordingDO.class);
            additionalRecordingMapper.insert(additionalRecording);
        }
        return voucherIds;
    }

    @Override
    public List<Long> getVoucherIdsByStandingbookId(Long standingbookId) {
        if (standingbookId == null) {
            return Collections.emptyList(); // 参数校验
        }
        return additionalRecordingMapper.selectVoucherIdsByStandingbookId(standingbookId);
    }

    @Override
    public void updateAdditionalRecording(AdditionalRecordingSaveReqVO updateReqVO) {
        // 校验存在
        validateAdditionalRecordingExists(updateReqVO.getId());
        updateReqVO.setEnterTime(LocalDateTime.now());

        if ("全量".equals(updateReqVO.getValueType())) {
            checkMeterReadingValue(updateReqVO.getStandingbookId(),
                    updateReqVO.getThisCollectTime(),
                    updateReqVO.getThisValue());
        }
        AdditionalRecordingDO updateObj = BeanUtils.toBean(updateReqVO, AdditionalRecordingDO.class);
        if(updateReqVO.getRecordPerson() == null){
            updateReqVO.setRecordPerson(getLoginUserNickname());
        }
        String energyId = standingbookTypeMapper.selectAttributeValueByCode(updateReqVO.getStandingbookId(),"energy");
        String unit = energyConfigurationMapper.selectUnitByEnergyNameAndChinese(energyId);
        // 设置录入时间为当前时间
        updateObj.setEnterTime(LocalDateTime.now());
        updateObj.setRecordMethod(1); // 手动录入
        updateObj.setUnit(unit);

        // 更新
        additionalRecordingMapper.updateById(updateObj);
    }

    @Override
    public void deleteAdditionalRecording(Long id) {
        // 校验存在
        validateAdditionalRecordingExists(id);
        // 删除
        additionalRecordingMapper.deleteById(id);
    }

    @Override
    public void deleteAdditionalRecordings(List<Long> ids) {
        for (Long id : ids) {
            // 校验存在
            validateAdditionalRecordingExists(id);
        }
        // 删除
        additionalRecordingMapper.deleteByIds(ids);
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