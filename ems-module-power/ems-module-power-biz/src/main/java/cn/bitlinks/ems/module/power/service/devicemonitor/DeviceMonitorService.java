package cn.bitlinks.ems.module.power.service.devicemonitor;

import cn.bitlinks.ems.framework.common.enums.CommonStatusEnum;
import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.infra.api.config.ConfigApi;
import cn.bitlinks.ems.module.power.controller.admin.monitor.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseTimeDateParamVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookDTO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.UsageCostData;
import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.WarningInfoMonitorStatisticsRespVO;
import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.WarningInfoRespVO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.labelconfig.LabelConfigDO;
import cn.bitlinks.ems.module.power.dal.dataobject.measurementdevice.MeasurementDeviceDO;
import cn.bitlinks.ems.module.power.dal.dataobject.monitor.DeviceMonitorQrcodeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookLabelInfoDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.warninginfo.WarningInfoDO;
import cn.bitlinks.ems.module.power.dal.mysql.measurementdevice.MeasurementDeviceMapper;
import cn.bitlinks.ems.module.power.dal.mysql.monitor.DeviceMonitorQrcodeMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.attribute.StandingbookAttributeMapper;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.bitlinks.ems.module.power.service.standingbook.label.StandingbookLabelInfoService;
import cn.bitlinks.ems.module.power.service.standingbook.tmpl.StandingbookTmplDaqAttrService;
import cn.bitlinks.ems.module.power.service.standingbook.type.StandingbookTypeService;
import cn.bitlinks.ems.module.power.service.usagecost.UsageCostService;
import cn.bitlinks.ems.module.power.service.warninginfo.WarningInfoService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.DEFAULT_SCALE;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.module.power.enums.StatisticsCacheConstants.DEVICE_MONITOR_DEVICE_DATA;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.dealBigDecimalScale;

@Service
public class DeviceMonitorService {
    @Resource
    @Lazy
    private StandingbookService standingbookService;
    @Resource
    @Lazy
    private StandingbookTypeService standingbookTypeService;
    @Resource
    @Lazy
    private StandingbookLabelInfoService standingbookLabelInfoService;
    @Resource
    @Lazy
    private LabelConfigService labelConfigService;
    @Resource
    @Lazy
    private WarningInfoService warningInfoService;

    @Resource
    private StandingbookAttributeMapper standingbookAttributeMapper;
    @Resource
    @Lazy
    private StandingbookTmplDaqAttrService standingbookTmplDaqAttrService;

    @Resource
    private MeasurementDeviceMapper measurementDeviceMapper;
    @Resource
    private DeviceMonitorQrcodeMapper deviceMonitorQrcodeMapper;
    @Resource
    private ConfigApi configApi;

    @Resource
    @Lazy
    private UsageCostService usageCostService;
    static final String INIT_DEVICE_LINK = "power.device.monitor.qrcode.url";
    @Resource
    @Lazy
    private EnergyConfigurationService energyConfigurationService;
    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;

    public DeviceMonitorWarningRespVO getWarningInfo(@Valid DeviceMonitorWarningReqVO reqVO) {
        DeviceMonitorWarningRespVO respVO = new DeviceMonitorWarningRespVO();
        // 校验时间范围合法性
        LocalDateTime[] rangeOrigin = reqVO.getRange();
        LocalDateTime startTime = rangeOrigin[0];
        LocalDateTime endTime = rangeOrigin[1];
        if (!startTime.isBefore(endTime)) {
            throw exception(END_TIME_MUST_AFTER_START_TIME);
        }
        if (!LocalDateTimeUtils.isWithinDays(startTime, endTime, CommonConstants.YEAR)) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }
        // 查询设备信息
        List<StandingbookDTO> standingbookDTOS = standingbookService.getStandingbookDTOList();
        StandingbookDTO standingbookDTO = standingbookDTOS.stream()
                .filter(dto -> dto != null &&
                        Objects.equals(dto.getStandingbookId(), reqVO.getSbId()))
                .findFirst()
                .orElse(null);
        if (standingbookDTO == null) {
            respVO.setList(Collections.emptyList());
        } else {
            List<WarningInfoDO> warningInfoDOList = warningInfoService.getMonitorListBySbCode(reqVO.getRange(), standingbookDTO.getCode());
            if (CollUtil.isEmpty(warningInfoDOList)) {
                respVO.setList(Collections.emptyList());
            } else {
                respVO.setList(BeanUtils.toBean(warningInfoDOList, WarningInfoRespVO.class));
            }
        }
        WarningInfoMonitorStatisticsRespVO statisticsRespVO = warningInfoService.getMonitorStatisticsBySbCode(standingbookDTO == null ? StringPool.EMPTY : standingbookDTO.getCode());
        respVO.setStatistics(statisticsRespVO);

        return respVO;
    }

    /**
     * 获取设备信息
     *
     * @param reqVO
     * @return
     */
    public DeviceMonitorDeviceRespVO getDeviceInfo(@Valid DeviceMonitorDeviceReqVO reqVO) {
        DeviceMonitorDeviceRespVO respVO = new DeviceMonitorDeviceRespVO();
        // 查询设备信息
        List<StandingbookDTO> standingbookDTOS = standingbookService.getStandingbookDTOList();
        StandingbookDTO standingbookDTO = standingbookDTOS.stream()
                .filter(dto -> dto != null &&
                        Objects.equals(dto.getStandingbookId(), reqVO.getSbId()))
                .findFirst()
                .orElse(null);
        respVO.setCode(standingbookDTO.getCode());
        respVO.setName(standingbookDTO.getName());
        respVO.setSbId(standingbookDTO.getStandingbookId());
        // 查询设备能耗状态
        long count = warningInfoService.countMonitorBySbCode(standingbookDTO.getCode());
        if (count > 0) {
            respVO.setStatus(CommonStatusEnum.DISABLE.getStatus());
        } else {
            respVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        }
        // 查询设备图片信息
        List<StandingbookAttributeDO> attributeDOS =
                standingbookAttributeMapper.selectList(new LambdaQueryWrapperX<StandingbookAttributeDO>()
                        .eq(StandingbookAttributeDO::getStandingbookId, reqVO.getSbId()));
        Optional<StandingbookAttributeDO> paramOptional = attributeDOS.stream()
                .filter(attribute -> attribute.getCode().equals("picture"))

                .findFirst();
        paramOptional.ifPresent(standingbookAttributeDO -> respVO.setImage(standingbookAttributeDO.getValue()));

        // 查询设备动态标签属性值
        List<StandingbookLabelInfoDO> labels = standingbookLabelInfoService.getByStandingBookId(reqVO.getSbId());
        if (CollUtil.isEmpty(labels)) {
            return respVO;
        }

        List<LabelConfigDO> labelConfigDOList = labelConfigService.getAllLabelConfig();
        Map<Long, LabelConfigDO> labelConfigDOMap = labelConfigDOList.stream()
                .collect(Collectors.toMap(LabelConfigDO::getId, Function.identity()));
        List<DeviceMonitorDeviceLabel> labelList = new ArrayList<>();
        labels.forEach(labelInfo -> {
            DeviceMonitorDeviceLabel result = new DeviceMonitorDeviceLabel();
            String topLabelKey = labelInfo.getName();
            Long topLabelId = Long.valueOf(topLabelKey.substring(topLabelKey.indexOf("_") + 1));
            result.setName(labelConfigDOMap.get(topLabelId).getLabelName());
            String value = labelInfo.getValue();
            if (StringUtils.isNotBlank(value)) {
                // 取最后一个勾选的值
                String[] parts = value.split(StringPool.COMMA);
                if (parts.length > 0) {
                    result.setValue(labelConfigDOMap.get(Long.parseLong(parts[parts.length - 1].trim())).getLabelName());
                }
            }
            labelList.add(result);
        });
        respVO.setLabels(labelList);
        return respVO;
    }

    public String getQrCode(@Valid DeviceMonitorDeviceReqVO reqVO) {
        StandingbookDO standingbookDO = standingbookService.getById(reqVO.getSbId());
        StandingbookTypeDO standingbookTypeDO = standingbookTypeService.getStandingbookType(standingbookDO.getTypeId());
        // 设备详情跳转链接
        String initLink = configApi.getConfigValueByKey(INIT_DEVICE_LINK).getCheckedData();
        String url = String.format(initLink,
                reqVO.getSbId(), standingbookTypeDO.getTopType());
        String qrCode = url + "&token=" + UUID.randomUUID();
        // 拼接token
        DeviceMonitorQrcodeDO qrcodeDO = new DeviceMonitorQrcodeDO();
        qrcodeDO.setDeviceId(reqVO.getSbId());
        qrcodeDO.setQrcode(qrCode);

        // 删除所有的链接信息
        deviceMonitorQrcodeMapper.delete(new LambdaQueryWrapperX<DeviceMonitorQrcodeDO>().eq(DeviceMonitorQrcodeDO::getDeviceId, reqVO.getSbId()));
        // 保存新的链接信息
        deviceMonitorQrcodeMapper.insert(qrcodeDO);
        return qrCode;
    }

    public Boolean validQrCode(String code) {
        DeviceMonitorQrcodeDO exist = deviceMonitorQrcodeMapper.selectOne(new LambdaQueryWrapperX<DeviceMonitorQrcodeDO>().eq(DeviceMonitorQrcodeDO::getQrcode, code));
        return Objects.nonNull(exist);
    }

    public List<DeviceMonitorDeviceEnergyRespVO> energyList(Long sbId) {
        // 1.查询重点设备下关联的计量器具
        List<MeasurementDeviceDO> measurementDeviceDOS = measurementDeviceMapper.selectList(new LambdaQueryWrapperX<MeasurementDeviceDO>().eq(MeasurementDeviceDO::getDeviceId, sbId));
        if (CollUtil.isEmpty(measurementDeviceDOS)) {
            return Collections.emptyList();
        }
        // 筛选出关联的计量器具ids
        List<Long> subSbIds = measurementDeviceDOS.stream()
                .map(MeasurementDeviceDO::getMeasurementInstrumentId)
                .distinct()  // 去重，确保每个 ID 只出现一次
                .collect(Collectors.toList());
        // 2.查询计量器具们关联的能源列表
        Map<Long, List<StandingbookTmplDaqAttrDO>> allSbEnergyAttrs = standingbookTmplDaqAttrService.getEnergyDaqAttrsBySbIds(subSbIds);
        if (CollUtil.isEmpty(allSbEnergyAttrs) || CollUtil.isEmpty(allSbEnergyAttrs.values())) {
            return Collections.emptyList();
        }
        // 提取出energyId Set
        Set<Long> energyIds = allSbEnergyAttrs.values().stream()
                .flatMap(List::stream)
                .map(StandingbookTmplDaqAttrDO::getEnergyId)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(energyIds)) {
            return Collections.emptyList();
        }
        // 3.查询计量器具的能源配置
        List<EnergyConfigurationDO> energyConfigurationDOS = energyConfigurationService.getByEnergyClassify(energyIds, null);
        return BeanUtils.toBean(energyConfigurationDOS, DeviceMonitorDeviceEnergyRespVO.class);

    }

    public DeviceMonitorDetailRespVO deviceTableAndChart(@Valid DeviceMonitorParamReqVO paramVO) {

        // 校验时间范围合法性
        LocalDateTime[] rangeOrigin = paramVO.getRange();
        LocalDateTime startTime = rangeOrigin[0];
        LocalDateTime endTime = rangeOrigin[1];
        if (!startTime.isBefore(endTime)) {
            throw exception(END_TIME_MUST_AFTER_START_TIME);
        }
        if (!LocalDateTimeUtils.isWithinDays(startTime, endTime, CommonConstants.YEAR)) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }
        DataTypeEnum dataTypeEnum = DataTypeEnum.codeOf(paramVO.getDateType());
        if (paramVO.getFlag() == 1) {
            // 获取查询维度类型和时间类型
            if (Objects.isNull(dataTypeEnum)) {
                throw exception(DATE_TYPE_NOT_EXISTS);
            }
        }

        // 添加缓存
        String cacheKey = DEVICE_MONITOR_DEVICE_DATA + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (StrUtil.isNotEmpty(cacheRes)) {
            return JSON.parseObject(cacheRes, new TypeReference<DeviceMonitorDetailRespVO>() {
            });
        }

        // 返回结果
        DeviceMonitorDetailRespVO resultVO = new DeviceMonitorDetailRespVO();

        Long deviceId = paramVO.getStandingbookId();
        // 0.查询重点设备下关联的计量器具
        List<MeasurementDeviceDO> measurementDeviceDOS = measurementDeviceMapper.selectList(new LambdaQueryWrapperX<MeasurementDeviceDO>().eq(MeasurementDeviceDO::getDeviceId, deviceId));
        if (CollUtil.isEmpty(measurementDeviceDOS)) {
            return resultVO;
        }
        // 筛选出关联的计量器具ids
        List<Long> subSbIds = measurementDeviceDOS.stream()
                .map(MeasurementDeviceDO::getMeasurementInstrumentId)
                .distinct()  // 去重，确保每个 ID 只出现一次
                .collect(Collectors.toList());
        List<StandingbookDTO> allStandingbookDTOList = standingbookService.getStandingbookDTOList();
        Map<Long, String> sbNameMapping = allStandingbookDTOList.stream()
                .filter(dto -> subSbIds.contains(dto.getStandingbookId()))
                .collect(Collectors.toMap(
                        StandingbookDTO::getStandingbookId,
                        StandingbookDTO::getName
                ));
        // 1.查询左上聚合数据，用量、折标煤、成本
        DeviceMonitorAggData aggData = usageCostService.getAggStatisticsBySbIds(paramVO.getRange()[0], paramVO.getRange()[1], subSbIds);
        resultVO.setSumCoal(aggData == null ? null : dealBigDecimalScale(aggData.getAccCoal(), DEFAULT_SCALE));
        resultVO.setSumUsage(aggData == null ? null : dealBigDecimalScale(aggData.getAccUsage(), DEFAULT_SCALE));
        resultVO.setSumCost(aggData == null ? null : dealBigDecimalScale(aggData.getAccCost(), DEFAULT_SCALE));

        // 2.查询表格
        // 获取表格表头
        List<String> tableHeaders = new ArrayList<>();
        if(CollUtil.isNotEmpty(sbNameMapping)){
            subSbIds.forEach(sbId -> tableHeaders.add(sbNameMapping.get(sbId)));
        }
        resultVO.setTableHeaders(tableHeaders);


        // 图，x轴处理
        List<String> timeRangeList = LocalDateTimeUtils.getTimeRangeList(paramVO.getRange()[0], paramVO.getRange()[1], DataTypeEnum.codeOf(paramVO.getDateType()));
        resultVO.setXdata(timeRangeList);


        // 查询图标依赖的数据。
        BaseTimeDateParamVO baseTimeDateParamVO = BeanUtils.toBean(paramVO, BaseTimeDateParamVO.class);
        List<UsageCostData> usageCostDataList = usageCostService.getUsageByStandingboookIdGroup(baseTimeDateParamVO, paramVO.getRange()[0], paramVO.getRange()[1], subSbIds);
        if (CollUtil.isEmpty(usageCostDataList)) {
            return resultVO;
        }
        //转map
        Map<Long, Map<String, UsageCostData>> standingbookIdTimeCostMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(
                                UsageCostData::getStandingbookId,  // 按 standingbookId 分组
                                Collectors.toMap(
                                        UsageCostData::getTime,  // 时间作为键
                                        usageCostData -> usageCostData
                                )
                        )
                );
        // 循环时间列表查询数据
        List<DeviceMonitorRowData> usageTableDataList = new ArrayList<>();
        List<DeviceMonitorRowData> coalTableDataList = new ArrayList<>();
        List<DeviceMonitorRowData> costTableDataList = new ArrayList<>();

        for (String time : timeRangeList) {
            DeviceMonitorRowData usageTableRowData = new DeviceMonitorRowData();
            usageTableRowData.setTime(time);
            DeviceMonitorRowData coalTableRowData = new DeviceMonitorRowData();
            coalTableRowData.setTime(time);
            DeviceMonitorRowData costTableRowData = new DeviceMonitorRowData();
            costTableRowData.setTime(time);
            List<DeviceMonitorTimeRowData> coalDataList = new ArrayList<>();
            List<DeviceMonitorTimeRowData> costDataList = new ArrayList<>();
            List<DeviceMonitorTimeRowData> usageDataList = new ArrayList<>();
            BigDecimal sumCoal = null;
            BigDecimal sumCost = null;
            BigDecimal sumUsage = null;
            for (Long sbId : subSbIds) {
                DeviceMonitorTimeRowData coalRowData = new DeviceMonitorTimeRowData();
                DeviceMonitorTimeRowData costRowData = new DeviceMonitorTimeRowData();
                DeviceMonitorTimeRowData usageRowData = new DeviceMonitorTimeRowData();
                // 获取 UsageCostData 并判空
                UsageCostData usageCostData = standingbookIdTimeCostMap.get(sbId) != null ?
                        standingbookIdTimeCostMap.get(sbId).get(time) : null;
                coalRowData.setName(sbNameMapping.get(sbId));
                coalRowData.setSbId(sbId);
                costRowData.setName(sbNameMapping.get(sbId));
                costRowData.setSbId(sbId);
                usageRowData.setName(sbNameMapping.get(sbId));
                usageRowData.setSbId(sbId);
                // 判断 usageCostData 是否为 null
                if (usageCostData != null) {
                    // 添加到对应的数据列表中
                    coalRowData.setValue(dealBigDecimalScale(usageCostData.getTotalStandardCoalEquivalent(), DEFAULT_SCALE));
                    costRowData.setValue(dealBigDecimalScale(usageCostData.getTotalCost(), DEFAULT_SCALE));
                    usageRowData.setValue(dealBigDecimalScale(usageCostData.getCurrentTotalUsage(), DEFAULT_SCALE));
                    // 累加计算
                    if (usageCostData.getTotalStandardCoalEquivalent() != null) {
                        sumCoal = (sumCoal == null) ? usageCostData.getTotalStandardCoalEquivalent() : sumCoal.add(usageCostData.getTotalStandardCoalEquivalent());
                    }

                    if (usageCostData.getTotalCost() != null) {
                        sumCost = (sumCost == null) ? usageCostData.getTotalCost() : sumCost.add(usageCostData.getTotalCost());
                    }

                    if (usageCostData.getCurrentTotalUsage() != null) {
                        sumUsage = (sumUsage == null) ? usageCostData.getCurrentTotalUsage() : sumUsage.add(usageCostData.getCurrentTotalUsage());
                    }
                }
                coalDataList.add(coalRowData);
                costDataList.add(costRowData);
                usageDataList.add(usageRowData);
            }
            // 汇总值
            usageTableRowData.setSum(sumUsage);
            coalTableRowData.setSum(sumCoal);
            costTableRowData.setSum(sumCost);
            // 构造列表
            coalTableRowData.setDataList(coalDataList);
            costTableRowData.setDataList(costDataList);
            usageTableRowData.setDataList(usageDataList);

            usageTableDataList.add(usageTableRowData);
            coalTableDataList.add(coalTableRowData);
            costTableDataList.add(costTableRowData);
        }
        // 表格数据
        resultVO.setUsageData(usageTableDataList);
        resultVO.setCostData(costTableDataList);
        resultVO.setCoalData(coalTableDataList);
        // 图数据

        resultVO.setUsageChart(transformData(usageTableDataList));
        resultVO.setCostChart(transformData(costTableDataList));
        resultVO.setCoalChart(transformData(coalTableDataList));

        // 结果保存在缓存中
        String jsonStr = JSONUtil.toJsonStr(resultVO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);
        return resultVO;
    }
    // 转换 DeviceMonitorRowData 到 DeviceMonitorChartData
    private List<DeviceMonitorChartData> transformData(List<DeviceMonitorRowData> rowDataList) {
        Map<Long, DeviceMonitorChartData> deviceChartDataMap = new HashMap<>();
        DeviceMonitorChartData summaryData = new DeviceMonitorChartData();
        summaryData.setName("汇总值"); summaryData.setSbId(null);
        summaryData.setType("line"); // 设置为 bar 或 line，根据需求
        // 遍历所有时间点的数据
        List<BigDecimal> sumDataList = new ArrayList<>();
        for (DeviceMonitorRowData rowData : rowDataList) {
            // 遍历当前时间点的所有设备数据
            for (DeviceMonitorTimeRowData timeRowData : rowData.getDataList()) {
                // 获取设备ID，若设备不存在，创建新条目
                DeviceMonitorChartData chartData = deviceChartDataMap.computeIfAbsent(timeRowData.getSbId(), id -> {
                    DeviceMonitorChartData newChartData = new DeviceMonitorChartData();
                    newChartData.setName(timeRowData.getName()); // 设备名称
                    newChartData.setSbId(id); // 设备ID
                    newChartData.setDataList(new ArrayList<>()); // 初始化时间数据列表
                    newChartData.setType("bar"); // 设为 bar 类型
                    return newChartData;
                });

                // 将当前时间点的数据添加到设备的 dataList 中
                chartData.getDataList().add(timeRowData.getValue());
            }
            sumDataList.add(rowData.getSum()); // 汇总所有设备的 sum
        }
        summaryData.setDataList(sumDataList); // 汇总数据只有一个数据点
        // 转换后的结果列表
        return new ArrayList<>(deviceChartDataMap.values());
    }

}
