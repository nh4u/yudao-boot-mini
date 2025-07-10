package cn.bitlinks.ems.module.power.service.additionalrecording;

import cn.bitlinks.ems.framework.common.enums.AcqFlagEnum;
import cn.bitlinks.ems.framework.common.enums.FullIncrementEnum;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.calc.AggSplitUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggDataSplitDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinutePrevExistNextDataDTO;
import cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.MinuteAggregateDataApi;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.AdditionalRecordingExistAcqDataRespVO;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.AdditionalRecordingManualSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.AdditionalRecordingPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.AdditionalRecordingSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.additionalrecording.AdditionalRecordingDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import cn.bitlinks.ems.module.power.dal.mysql.additionalrecording.AdditionalRecordingMapper;
import cn.bitlinks.ems.module.power.enums.RecordMethodEnum;
import cn.bitlinks.ems.module.power.enums.additionalrecording.AdditionalRecordingScene;
import cn.bitlinks.ems.module.power.service.standingbook.tmpl.StandingbookTmplDaqAttrService;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

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
    @Lazy
    private StandingbookTmplDaqAttrService standingbookTmplDaqAttrService;
//    @Resource
//    private ExcelMeterDataProcessor excelMeterDataProcessor;
    //    @Resource
//    private VoucherService voucherService;
//    @Resource
//    private EnergyConfigurationMapper energyConfigurationMapper;
//    @Resource
//    private EnergyParametersMapper energyParametersMapper;
    @Resource
    private MinuteAggregateDataApi minuteAggregateDataApi;
    @Resource
    private SplitTaskDispatcher splitTaskDispatcher;

    @Override
    public void createAdditionalRecording(AdditionalRecordingManualSaveReqVO createReqVO) {

        // 1.获取能源用量参数，如果没有，不可补录
        StandingbookTmplDaqAttrDO daqAttrDO =
                standingbookTmplDaqAttrService.getUsageAttrBySbId(createReqVO.getStandingbookId());

        if (Objects.isNull(daqAttrDO)) {
            throw exception(ADDITIONAL_RECORDING_ENERGY_NOT_EXISTS);
        }
        if (FullIncrementEnum.codeOf(createReqVO.getValueType()) == null) {
            throw exception(FULL_INCREMENT_TYPE_ERROR);
        }
        // 0.参数校验
        // 0.1增量校验[两个时间不能为空&增量值必须要大于=0] 废弃了暂不做增量补录
        if (FullIncrementEnum.INCREMENT.getCode().equals(createReqVO.getValueType())) {
            return;
        }

        // 0.2全量校验[补录时间点不能为空&上一个全量值 <= 补录值 <=下一个全量值  没有值就单边比较即可，询问了无右边值的话数采采的值可能会出现增量为负数的情况，会影响后续的全量值，直到全量值>=该变更的全量值为止
        // 上一个业务点的全量值 <= 当前补录值 <=下一个业务点的全量值
        // 获取当前时间点上下两条业务点数据
        // 补录时间点不能为空
        if (Objects.isNull(createReqVO.getThisCollectTime())) {
            throw exception(FULL_TIME_NOT_NULL);
        }
        // 不能大于当前时间
        if (createReqVO.getThisCollectTime().isAfter(LocalDateTime.now())) {
            throw exception(CURRENT_TIME_ERROR);
        }
        // 获取上下业务点的两个全量值两个全量值（valueType == 0）与当前采集点可能相等
        MinutePrevExistNextDataDTO  minutePrevExistNextDataDTO= minuteAggregateDataApi.getUsagePrevExistNextFullValue(createReqVO.getStandingbookId(), createReqVO.getThisCollectTime());
        MinuteAggregateDataDTO prev = minutePrevExistNextDataDTO.getPrevFullValue();
        MinuteAggregateDataDTO next = minutePrevExistNextDataDTO.getNextFullValue();
        // 构造基础 DTO
        MinuteAggregateDataDTO baseDTO = buildBaseDTO(createReqVO, daqAttrDO);

        if (prev == null && next == null) {
            baseDTO.setIncrementalValue(BigDecimal.ZERO);
            insertAndRecord(Collections.singletonList(baseDTO), createReqVO);
            return;
        }

        if (prev != null && next != null) {
            validateRange(prev.getFullValue(), baseDTO.getFullValue(), next.getFullValue());

            // 计算增量
            baseDTO.setDataSite(prev.getDataSite());
            baseDTO.setIncrementalValue(AggSplitUtils.calculatePerMinuteIncrement(prev.getAggregateTime(), baseDTO.getAggregateTime(), prev.getFullValue(), baseDTO.getFullValue()));
            next.setIncrementalValue(AggSplitUtils.calculatePerMinuteIncrement(baseDTO.getAggregateTime(), next.getAggregateTime(), baseDTO.getFullValue(), next.getFullValue()));

            // 插入补录数据以及操作记录
            insertAndRecord(Arrays.asList(baseDTO, next), createReqVO);

            // 异步拆分
            List<MinuteAggDataSplitDTO> splitList = Arrays.asList(new MinuteAggDataSplitDTO(prev, baseDTO), new MinuteAggDataSplitDTO(baseDTO, next));
            splitTaskDispatcher.dispatchSplitTaskBatch(splitList);
            return;
        }

        if (prev != null) {
            validateLeft(prev.getFullValue(), baseDTO.getFullValue());

            // 计算增量
            baseDTO.setDataSite(prev.getDataSite());
            baseDTO.setIncrementalValue(AggSplitUtils.calculatePerMinuteIncrement(prev.getAggregateTime(), baseDTO.getAggregateTime(), prev.getFullValue(), baseDTO.getFullValue()));

            // 插入补录数据以及操作记录
            insertAndRecord(Collections.singletonList(baseDTO), createReqVO);
            // 异步拆分
            splitTaskDispatcher.dispatchSplitTask(new MinuteAggDataSplitDTO(prev, baseDTO));
            return;
        }

        // only next != null
        validateRight(baseDTO.getFullValue(), next.getFullValue());

        baseDTO.setDataSite(next.getDataSite());
        baseDTO.setIncrementalValue(BigDecimal.ZERO);
        next.setIncrementalValue(AggSplitUtils.calculatePerMinuteIncrement(baseDTO.getAggregateTime(), next.getAggregateTime(), baseDTO.getFullValue(), next.getFullValue()));

        // 插入补录数据以及操作记录
        insertAndRecord(Arrays.asList(baseDTO, next), createReqVO);
        // 异步拆分
        splitTaskDispatcher.dispatchSplitTask(new MinuteAggDataSplitDTO(baseDTO, next));
    }

    /**
     * 构建基础补录 DTO
     *
     * @param req
     * @param attr
     * @return
     */
    private MinuteAggregateDataDTO buildBaseDTO(AdditionalRecordingManualSaveReqVO req, StandingbookTmplDaqAttrDO attr) {
        MinuteAggregateDataDTO dto = new MinuteAggregateDataDTO();
        dto.setStandingbookId(req.getStandingbookId());
        dto.setEnergyFlag(attr.getEnergyFlag());
        dto.setParamCode(attr.getCode());
        dto.setUsage(attr.getUsage());
        dto.setDataType(attr.getDataType());
        dto.setFullIncrement(req.getValueType());
        dto.setDataFeature(attr.getDataType());
        dto.setAggregateTime(req.getThisCollectTime().truncatedTo(ChronoUnit.MINUTES));
        dto.setFullValue(req.getThisValue());
        dto.setAcqFlag(AcqFlagEnum.ACQ.getCode());
        return dto;
    }

    /**
     * 插入业务点数据 + 插入历史操作记录
     *
     * @param list
     * @param req
     */
    private void insertAndRecord(List<MinuteAggregateDataDTO> list, AdditionalRecordingManualSaveReqVO req) {
        minuteAggregateDataApi.insertDataBatch(list);
        saveAdditionalRecording(req);
    }

    // 抽取验证方法
    private void validateRange(BigDecimal left, BigDecimal current, BigDecimal right) {
        if (left.compareTo(current) > 0) {
            throw exception(FULL_VALUE_MUST_GT_LEFT, left);
        }
        if (current.compareTo(right) > 0) {
            throw exception(FULL_VALUE_MUST_LT_RIGHT, right);
        }
    }

    // 验证全量值
    private void validateLeft(BigDecimal left, BigDecimal current) {
        if (left.compareTo(current) > 0) {
            throw exception(FULL_VALUE_MUST_GT_LEFT, left);
        }
    }

    // 验证全量值
    private void validateRight(BigDecimal current, BigDecimal right) {
        if (current.compareTo(right) > 0) {
            throw exception(FULL_VALUE_MUST_LT_RIGHT, right);
        }
    }

    /**
     * 手动补录新增历史记录
     *
     * @param minuteAggAcqDataList
     */
    public void saveAdditionalRecordingBatch(List<MinuteAggregateDataDTO> minuteAggAcqDataList) {
        if (CollUtil.isEmpty(minuteAggAcqDataList)) {
            return;
        }
        List<AdditionalRecordingDO> additionalRecordings = new ArrayList<>();
        minuteAggAcqDataList.forEach(minuteAggDTO -> {
            // 1.补录数据
            AdditionalRecordingDO additionalRecording = new AdditionalRecordingDO();
            additionalRecording.setValueType(FullIncrementEnum.FULL.getCode());
            additionalRecording.setThisCollectTime(minuteAggDTO.getAggregateTime());
            additionalRecording.setThisValue(minuteAggDTO.getFullValue());
            additionalRecording.setStandingbookId(minuteAggDTO.getStandingbookId());
            // 设置录入时间为当前时间
            additionalRecording.setEnterTime(LocalDateTime.now());
            additionalRecording.setRecordMethod(RecordMethodEnum.IMPORT_MANUAL.getCode()); // 手动录入

            additionalRecordings.add(additionalRecording);
        });
        // 插入数据库
        additionalRecordingMapper.insert(additionalRecordings);
    }

    /**
     * 手动补录新增
     *
     * @param createReqVO
     */
    private void saveAdditionalRecording(AdditionalRecordingManualSaveReqVO createReqVO) {
        // 1.补录数据
        AdditionalRecordingDO additionalRecording = BeanUtils.toBean(createReqVO, AdditionalRecordingDO.class);
        if (createReqVO.getRecordPerson() == null) {
            createReqVO.setRecordPerson(getLoginUserNickname());
        }
        // 设置录入时间为当前时间
        additionalRecording.setEnterTime(LocalDateTime.now());
        additionalRecording.setRecordMethod(RecordMethodEnum.IMPORT_MANUAL.getCode()); // 手动录入
        // 插入数据库
        additionalRecordingMapper.insert(additionalRecording);
    }

    @Override
    public AdditionalRecordingExistAcqDataRespVO getExistDataRange(Long standingbookId, LocalDateTime currentCollectTime) {
        // 获取上下两个全量值（valueType == 0）与当前采集点可能相等
        MinutePrevExistNextDataDTO  minutePrevExistNextDataDTO= minuteAggregateDataApi.getUsagePrevExistNextFullValue(standingbookId, currentCollectTime);
        MinuteAggregateDataDTO existFullValue = minutePrevExistNextDataDTO.getExistFullValue();
        MinuteAggregateDataDTO prevFullValue = minutePrevExistNextDataDTO.getPrevFullValue();
        MinuteAggregateDataDTO nextFullValue = minutePrevExistNextDataDTO.getNextFullValue();
        minuteAggregateDataApi.getUsagePrevExistNextFullValue(standingbookId, currentCollectTime);
        int state = (prevFullValue != null ? 1 : 0) << 2
                | (existFullValue != null ? 1 : 0) << 1
                | (nextFullValue != null ? 1 : 0);

        AdditionalRecordingExistAcqDataRespVO respVO = new AdditionalRecordingExistAcqDataRespVO();

        switch (state) {
            case 0b000:
                respVO.setScene(AdditionalRecordingScene.NO_HIS.getCode());
                break;//无任何值
            case 0b001:
                respVO.setScene(AdditionalRecordingScene.ONE_NEXT.getCode());
                break;//只有后值
            case 0b010:
                respVO.setScene(AdditionalRecordingScene.NO_HIS_COVER.getCode());
                break;//只有当前值
            case 0b011:
                respVO.setScene(AdditionalRecordingScene.TWO_PRE_COVER.getCode());
                break;//当前 + 后值
            case 0b100:
                respVO.setScene(AdditionalRecordingScene.ONE_PRE.getCode());
                break;//只有前值
            case 0b101:
                respVO.setScene(AdditionalRecordingScene.TWO_POINT.getCode());
                break;//前 + 后（当前无）
            case 0b110:
                respVO.setScene(AdditionalRecordingScene.TWO_NEXT_COVER.getCode());
                break;//前 + 当前
            case 0b111:
                respVO.setScene(AdditionalRecordingScene.TWO_POINT_COVER.getCode());
                break;//三值全有
            default:
                throw new IllegalStateException("未识别的补录场景 state=" + state);
        }


        respVO.setPreTime(prevFullValue != null ? prevFullValue.getAggregateTime() : null);
        respVO.setCurTime(existFullValue != null ? existFullValue.getAggregateTime() : null);
        respVO.setNextTime(nextFullValue != null ? nextFullValue.getAggregateTime() : null);

        return respVO;
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