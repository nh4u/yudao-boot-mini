package cn.bitlinks.ems.module.power.service.additionalrecording;

import cn.bitlinks.ems.framework.common.enums.FullIncrementEnum;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggDataSplitDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.MinuteAggregateDataApi;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.AdditionalRecordingLastVO;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.AdditionalRecordingPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.AdditionalRecordingSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.additionalrecording.AdditionalRecordingDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import cn.bitlinks.ems.module.power.dal.mysql.additionalrecording.AdditionalRecordingMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.type.StandingbookTypeMapper;
import cn.bitlinks.ems.module.power.enums.RecordMethodEnum;
import cn.bitlinks.ems.module.power.service.standingbook.tmpl.StandingbookTmplDaqAttrService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
@Slf4j
public class AdditionalRecordingServiceImpl implements AdditionalRecordingService {

    @Resource
    private AdditionalRecordingMapper additionalRecordingMapper;
    @Resource
    private StandingbookTypeMapper standingbookTypeMapper;
    @Resource
    @Lazy
    private StandingbookTmplDaqAttrService standingbookTmplDaqAttrService;
    //    @Resource
//    private VoucherService voucherService;
//    @Resource
//    private EnergyConfigurationMapper energyConfigurationMapper;
//    @Resource
//    private EnergyParametersMapper energyParametersMapper;
    @Resource
    private MinuteAggregateDataApi minuteAggregateDataApi;


    @Override
    public Long createAdditionalRecording(AdditionalRecordingSaveReqVO createReqVO) {

        // 1.获取能源用量参数，如果没有，不可补录
        StandingbookTmplDaqAttrDO daqAttrDO =
                standingbookTmplDaqAttrService.getUsageAttrBySbId(createReqVO.getStandingbookId());

        if (Objects.isNull(daqAttrDO)) {
            throw exception(ADDITIONAL_RECORDING_ENERGY_NOT_EXISTS);
        }
        // 进行数据拆分时校验补录的时间是否合法，不合法则提示无法补录
        try {
            splitData(daqAttrDO, createReqVO.getStandingbookId(), createReqVO.getValueType(),
                    createReqVO.getPreCollectTime(),
                    createReqVO.getThisCollectTime(), createReqVO.getPreValue(), createReqVO.getThisValue());
        } catch (Exception e) {
            log.error("补录拆分数据失败，失败原因:{}", e.getMessage(), e);
        }
        AdditionalRecordingDO additionalRecording = BeanUtils.toBean(createReqVO, AdditionalRecordingDO.class);
        if (createReqVO.getRecordPerson() == null) {
            createReqVO.setRecordPerson(getLoginUserNickname());
        }
        // 设置录入时间为当前时间
        additionalRecording.setEnterTime(LocalDateTime.now());
        additionalRecording.setRecordMethod(RecordMethodEnum.IMPORT_MANUAL.getCode()); // 手动录入
        // 插入数据库
        additionalRecordingMapper.insert(additionalRecording);

        return additionalRecording.getId();
    }

    /**
     * 数据拆分
     *
     * @param standingbookId
     * @param valueType
     * @param preCollectTime
     * @param currentCollectTime
     */
    private void splitData(StandingbookTmplDaqAttrDO daqAttrDO, Long standingbookId, Integer valueType,
                           LocalDateTime preCollectTime,
                           LocalDateTime currentCollectTime, BigDecimal preValue, BigDecimal thisValue) {
        // 获取此时聚合数据的最新和最老数据
        MinuteAggregateDataDTO oldestData =
                minuteAggregateDataApi.selectOldestByStandingBookId(standingbookId);
        MinuteAggregateDataDTO latestData =
                minuteAggregateDataApi.selectLatestByStandingBookId(standingbookId);
        // 0.0本次采集时间必须要小于上次采集时间且要小于当前时间的前十分钟点
        if (!LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusMinutes(10L).isAfter(currentCollectTime)) {
            throw exception(CURRENT_TIME_ERROR);
        }
        //
        // 0.1 如果聚合表无历史数据。按照本次采集点进行补录, 忽略上次采集时间，全量增量都按照全量
        if (Objects.isNull(oldestData)) {
            if (FullIncrementEnum.INCREMENT.getCode().equals(valueType)) {
                throw exception(INCREMENT_HISTORY_NOT_EXISTS);
            }
            // 全量进行单条补录
            MinuteAggregateDataDTO minuteAggregateDataDTO = new MinuteAggregateDataDTO();
            minuteAggregateDataDTO.setAggregateTime(currentCollectTime.truncatedTo(ChronoUnit.MINUTES));
            minuteAggregateDataDTO.setStandingbookId(standingbookId);
            minuteAggregateDataDTO.setFullValue(thisValue);
            minuteAggregateDataDTO.setIncrementalValue(BigDecimal.ZERO);
            minuteAggregateDataDTO.setEnergyFlag(daqAttrDO.getEnergyFlag());
            minuteAggregateDataDTO.setParamCode(daqAttrDO.getCode());
            minuteAggregateDataApi.insertSingleData(minuteAggregateDataDTO);
            return;
        }

        // 0.2 如果聚合表有历史数据，
        if (FullIncrementEnum.FULL.getCode().equals(valueType)) {
            // 1.1 判断本次采集时间是否在oldestData和latestData之内，
            if (!currentCollectTime.isBefore(oldestData.getAggregateTime()) && !currentCollectTime.isAfter(latestData.getAggregateTime())) {
                // 1.2.0 如果本次采集时间在 oldestData 和 latestData 之间，提示已有数据
                throw exception(THIS_TIME_EXISTS_DATA);
            }
            // 1.2.1 如果本次采集时间在oldestData之前，需要修改oldestData的增量，
            if (currentCollectTime.isBefore(oldestData.getAggregateTime())) {
                //1.2.1.1 是全量
                // 本次值如果大于oldestData的值，
                if (thisValue.compareTo(oldestData.getFullValue()) > 0) {
                    throw exception(CURRENT_TIME_TOO_BIG_ERROR);
                }

                // 进行补录拆分到分钟
                // 进行补录
                MinuteAggregateDataDTO minuteAggregateDataDTO = new MinuteAggregateDataDTO();
                minuteAggregateDataDTO.setAggregateTime(currentCollectTime.truncatedTo(ChronoUnit.MINUTES));
                minuteAggregateDataDTO.setStandingbookId(standingbookId);
                minuteAggregateDataDTO.setFullValue(thisValue);
                minuteAggregateDataDTO.setDataSite(oldestData.getDataSite());
                minuteAggregateDataDTO.setIncrementalValue(BigDecimal.ZERO);
                minuteAggregateDataDTO.setEnergyFlag(daqAttrDO.getEnergyFlag());
                minuteAggregateDataDTO.setParamCode(daqAttrDO.getCode());
                MinuteAggDataSplitDTO minuteAggDataSplitDTO = new MinuteAggDataSplitDTO();
                minuteAggDataSplitDTO.setStartDataDO(minuteAggregateDataDTO);
                minuteAggDataSplitDTO.setEndDataDO(oldestData);
                minuteAggregateDataApi.insertDelRangeData(minuteAggDataSplitDTO);
                return;
            }
            // 1.2.2 如果本次采集时间在latestData之后，补录到currentCollectTime此分钟,不需要修改历史数据
            if (thisValue.compareTo(latestData.getFullValue()) < 0) {
                throw exception(CURRENT_TIME_TOO_SMALL_ERROR);
            }
            // 进行补录拆分到分钟
            // 进行补录
            MinuteAggregateDataDTO minuteAggregateDataDTO = new MinuteAggregateDataDTO();
            minuteAggregateDataDTO.setAggregateTime(currentCollectTime.truncatedTo(ChronoUnit.MINUTES));
            minuteAggregateDataDTO.setStandingbookId(standingbookId);
            minuteAggregateDataDTO.setFullValue(thisValue);
            minuteAggregateDataDTO.setDataSite(oldestData.getDataSite());
            //minuteAggregateDataDTO.setIncrementalValue(BigDecimal.ZERO);需要拆分计算出来
            minuteAggregateDataDTO.setEnergyFlag(daqAttrDO.getEnergyFlag());
            minuteAggregateDataDTO.setParamCode(daqAttrDO.getCode());
            MinuteAggDataSplitDTO minuteAggDataSplitDTO = new MinuteAggDataSplitDTO();
            minuteAggDataSplitDTO.setStartDataDO(latestData);
            minuteAggDataSplitDTO.setEndDataDO(minuteAggregateDataDTO);
            minuteAggregateDataApi.insertRangeData(minuteAggDataSplitDTO);

        } else if (FullIncrementEnum.INCREMENT.getCode().equals(valueType)) {
            // 1.2.1 只允许存在两种情况，，本次采集时间为oldestData的时间上次采集时间在oldestdata之前，或者 上次采集时间为latestData，本次采集时间在latestData之后
            // 1.2.1 如果本次采集时间在oldestData之前，需要修改oldestData的增量，
            if (preCollectTime == null) {
                throw exception(RANGE_TIME_NOT_NULL);
            }

            boolean validCase1 =
                    currentCollectTime.equals(oldestData.getAggregateTime()) && preCollectTime.isBefore(oldestData.getAggregateTime());

            boolean validCase2 =
                    preCollectTime.equals(latestData.getAggregateTime()) && currentCollectTime.isAfter(latestData.getAggregateTime());

            if (validCase1) {
                //本次采集时间为oldestData的时间上次采集时间在oldestdata之前
                // 则上次采集时间的值为oldestData-本次值
                if (thisValue.compareTo(oldestData.getFullValue()) > 0) {
                    throw exception(CURRENT_TIME_TOO_BIG_ERROR);
                }
                //
                BigDecimal initValue = oldestData.getFullValue().subtract(thisValue);
                // 进行补录
                MinuteAggregateDataDTO minuteAggregateDataDTO = new MinuteAggregateDataDTO();
                minuteAggregateDataDTO.setAggregateTime(currentCollectTime.truncatedTo(ChronoUnit.MINUTES));
                minuteAggregateDataDTO.setStandingbookId(standingbookId);
                minuteAggregateDataDTO.setFullValue(initValue);
                minuteAggregateDataDTO.setDataSite(oldestData.getDataSite());
                minuteAggregateDataDTO.setIncrementalValue(BigDecimal.ZERO);
                minuteAggregateDataDTO.setEnergyFlag(daqAttrDO.getEnergyFlag());
                minuteAggregateDataDTO.setParamCode(daqAttrDO.getCode());
                MinuteAggDataSplitDTO minuteAggDataSplitDTO = new MinuteAggDataSplitDTO();
                minuteAggDataSplitDTO.setStartDataDO(minuteAggregateDataDTO);
                minuteAggDataSplitDTO.setEndDataDO(oldestData);
                minuteAggregateDataApi.insertDelRangeData(minuteAggDataSplitDTO);
                return;
            } else if (validCase2) {
                //上次采集时间为latestData，本次采集时间在latestData之后

                BigDecimal latestValue = latestData.getFullValue().add(thisValue);

                MinuteAggregateDataDTO minuteAggregateDataDTO = new MinuteAggregateDataDTO();
                minuteAggregateDataDTO.setAggregateTime(currentCollectTime.truncatedTo(ChronoUnit.MINUTES));
                minuteAggregateDataDTO.setStandingbookId(standingbookId);
                minuteAggregateDataDTO.setFullValue(latestValue);
                minuteAggregateDataDTO.setDataSite(oldestData.getDataSite());
                //minuteAggregateDataDTO.setIncrementalValue();需要拆分计算出来
                minuteAggregateDataDTO.setEnergyFlag(daqAttrDO.getEnergyFlag());
                minuteAggregateDataDTO.setParamCode(daqAttrDO.getCode());
                MinuteAggDataSplitDTO minuteAggDataSplitDTO = new MinuteAggDataSplitDTO();
                minuteAggDataSplitDTO.setStartDataDO(latestData);
                minuteAggDataSplitDTO.setEndDataDO(minuteAggregateDataDTO);
                minuteAggregateDataApi.insertRangeData(minuteAggDataSplitDTO);
                return;
            }
            throw exception(RANGE_TIME_ERROR);
        }
        throw exception(VALUE_TYPE_REQUIRED);

    }


//    private boolean isCollectTimeDuplicate(Long standingbookId, LocalDateTime collectTime) {
//        QueryWrapper<AdditionalRecordingDO> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("standingbook_id", standingbookId)
//                .eq("this_collect_time", collectTime);
//        return additionalRecordingMapper.selectCount(queryWrapper) > 0;
//    }


//    /**
//     * 校验抄表数值是否符合规则：本次值 ≥ 上次值 且 ≤ 下次值
//     */
//    private void checkMeterReadingValue(Long standingbookId, LocalDateTime currentCollectTime, BigDecimal currentValue) {
//
//        // 获取上次记录（早于当前时间的最新记录）
//        AdditionalRecordingDO lastRecord = getNeighborRecord(standingbookId, currentCollectTime, false);
//        // 获取下次记录（晚于当前时间的最早记录）
//        AdditionalRecordingDO nextRecord = getNeighborRecord(standingbookId, currentCollectTime, true);
//
//        // 校验：当前值 ≥ 上次值
//        if (lastRecord != null && currentValue.compareTo(lastRecord.getThisValue()) < 0) {
//            throw exception(THIS_VALUE_NOT_LESS);
//        }
//
//        // 校验：当前值 ≤ 下次值
//        if (nextRecord != null && currentValue.compareTo(nextRecord.getThisValue()) > 0) {
//            throw exception(THIS_VALUE_NOT_MORE);
//        }
//    }

//    /**
//     * 获取相邻记录（上次或下次）
//     * @param isNext true表示查询下次记录，false表示查询上次记录
//     */
//    private AdditionalRecordingDO getNeighborRecord(Long standingbookId, LocalDateTime currentCollectTime, boolean isNext) {
//        QueryWrapper<AdditionalRecordingDO> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("standingbook_id", standingbookId)
//                .eq("value_type", "全量")  // 只处理全量数据
//                .select("this_collect_time", "this_value");
//
//        // 时间条件：上次记录（<当前时间）或下次记录（>当前时间）
//        if (isNext) {
//            queryWrapper.gt("this_collect_time", currentCollectTime)
//                    .orderByAsc("this_collect_time");
//        } else {
//            queryWrapper.lt("this_collect_time", currentCollectTime)
//                    .orderByDesc("this_collect_time");
//        }
//
//        queryWrapper.last("LIMIT 1");
//        return additionalRecordingMapper.selectOne(queryWrapper);
//    }

    @Override
    public AdditionalRecordingLastVO getLastRecord(Long standingbookId, LocalDateTime currentCollectTime) {

        MinuteAggregateDataDTO minuteAggregateDataDTO =
                minuteAggregateDataApi.selectLatestByAggTime(standingbookId,
                        currentCollectTime);
        if (Objects.isNull(minuteAggregateDataDTO)) {
            return null;
        }
        AdditionalRecordingLastVO additionalRecordingLastVO = new AdditionalRecordingLastVO();
        additionalRecordingLastVO.setLastValue(minuteAggregateDataDTO.getFullValue());
        additionalRecordingLastVO.setLastCollectTime(minuteAggregateDataDTO.getAggregateTime());
        return additionalRecordingLastVO;
    }

    @Override
    public List<Long> createAdditionalRecordingByVoucherId(List<Long> voucherIds, Long standingbookId) {
//        String nickname = getLoginUserNickname();
//        String energyId = standingbookTypeMapper.selectAttributeValueByCode(standingbookId, "energy");
//        String unit = energyConfigurationMapper.selectUnitByEnergyNameAndChinese(energyId);
//        for(Long voucherId : voucherIds){
//            VoucherDO voucherDO = voucherService.getVoucher(voucherId);
//            if (voucherDO.getPurchaseTime().isAfter(LocalDateTime.now())) {
//                throw exception(PURCHASE_TIME_OVER_CURRENT); // 替换为你的具体业务异常
//            }
//            AdditionalRecordingSaveReqVO saveReqVO = new AdditionalRecordingSaveReqVO();
//            saveReqVO.setRecordMethod(2); //凭证补录
//            saveReqVO.setThisValue(voucherDO.getUsage());
//            saveReqVO.setThisCollectTime(voucherDO.getPurchaseTime());
//            saveReqVO.setVoucherId(voucherId);
//            saveReqVO.setUnit(unit);
//            saveReqVO.setRecordPerson(nickname);
//            saveReqVO.setStandingbookId(standingbookId);
//            saveReqVO.setEnterTime(LocalDateTime.now());
//            if (isCollectTimeDuplicate(saveReqVO.getStandingbookId(), saveReqVO.getThisCollectTime())) {
//                throw exception(THIS_TIME_EXISTS_DATA);
//            }
//            if ("全量".equals(saveReqVO.getValueType())) {
//                checkMeterReadingValue(saveReqVO.getStandingbookId(),
//                        saveReqVO.getThisCollectTime(),
//                        saveReqVO.getThisValue());
//            }
//            AdditionalRecordingDO additionalRecording = BeanUtils.toBean(saveReqVO, AdditionalRecordingDO.class);
//            additionalRecordingMapper.insert(additionalRecording);
//        }
//        return voucherIds;
        return Collections.emptyList();
    }

    @Override
    public List<Long> getVoucherIdsByStandingbookId(Long standingbookId) {
//        if (standingbookId == null) {
//            return Collections.emptyList(); // 参数校验
//        }
//        return additionalRecordingMapper.selectVoucherIdsByStandingbookId(standingbookId);
        return Collections.emptyList();
    }

    @Override
    public void updateAdditionalRecording(AdditionalRecordingSaveReqVO updateReqVO) {
//        // 校验存在
//        validateAdditionalRecordingExists(updateReqVO.getId());
//        updateReqVO.setEnterTime(LocalDateTime.now());
//
//        if ("全量".equals(updateReqVO.getValueType())) {
//            checkMeterReadingValue(updateReqVO.getStandingbookId(),
//                    updateReqVO.getThisCollectTime(),
//                    updateReqVO.getThisValue());
//        }
//        AdditionalRecordingDO updateObj = BeanUtils.toBean(updateReqVO, AdditionalRecordingDO.class);
//        if(updateReqVO.getRecordPerson() == null){
//            updateReqVO.setRecordPerson(getLoginUserNickname());
//        }
//        String energyId = standingbookTypeMapper.selectAttributeValueByCode(updateReqVO.getStandingbookId(),"energy");
//        String unit = energyConfigurationMapper.selectUnitByEnergyNameAndChinese(energyId);
//        // 设置录入时间为当前时间
//        updateObj.setEnterTime(LocalDateTime.now());
//        updateObj.setRecordMethod(1); // 手动录入
//        updateObj.setUnit(unit);
//
//        // 更新
//        additionalRecordingMapper.updateById(updateObj);
    }

    @Override
    public void deleteAdditionalRecording(Long id) {
//        // 校验存在
//        validateAdditionalRecordingExists(id);
//        // 删除
//        additionalRecordingMapper.deleteById(id);
    }

    @Override
    public void deleteAdditionalRecordings(List<Long> ids) {
//        for (Long id : ids) {
//            // 校验存在
//            validateAdditionalRecordingExists(id);
//        }
//        // 删除
//        additionalRecordingMapper.deleteByIds(ids);
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