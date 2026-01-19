package cn.bitlinks.ems.module.power.service.airconditioner;

import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.airconditioner.vo.AirConditionerSettingsReqVO;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ConsumptionStatisticsChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ConsumptionStatisticsChartYInfo;
import cn.bitlinks.ems.module.power.dal.dataobject.airconditioner.AirConditionerSettingsDO;
import cn.bitlinks.ems.module.power.dal.dataobject.airconditioner.AirConditionerStatusDataDO;
import cn.bitlinks.ems.module.power.dal.mysql.airconditioner.AirConditionerSettingsMapper;
import cn.bitlinks.ems.module.power.dal.mysql.airconditioner.AirConditionerStatusDataMapper;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;


@Slf4j
@Service
@Validated
public class AirConditionerServiceImpl implements AirConditionerService {
    @Resource
    private AirConditionerStatusDataMapper airConditionerStatusDataMapper;

    @Resource
    private AirConditionerSettingsMapper airConditionerSettingsMapper;


    @Override
    public List<String> getOptions() {
        return airConditionerSettingsMapper.selectDistinctItemNames();
    }

    @Override
    public ConsumptionStatisticsChartResultVO<ConsumptionStatisticsChartYInfo> getChart(AirConditionerSettingsReqVO paramVO) {
        // 1.校验时间范围
        LocalDateTime[] range = validateRange(paramVO.getRange());
        if (StringUtils.isEmpty(paramVO.getItemName())) {
            throw exception(PARAM_ITEM_NAME_NECESSARY);
        }
        // 2.根据统计项查询出数采点位（缓存中）
        List<AirConditionerSettingsDO> settingsList = airConditionerSettingsMapper.selectList(new LambdaQueryWrapperX<AirConditionerSettingsDO>()
                .eq(AirConditionerSettingsDO::getItemName, paramVO.getItemName())
        );
        if (CollUtil.isEmpty(settingsList)) {
            throw exception(AIR_CONDITIONER_NO_DATA_SITE);
        }
        // 【新增/修改】建立 dataSite -> Name 的映射关系，方便后续 Y 轴展示
        Map<String, String> siteNameMap = settingsList.stream()
                .collect(Collectors.toMap(
                        AirConditionerSettingsDO::getDataSite,
                        AirConditionerSettingsDO::getItemName, // 如果是其他字段请替换，如 getName()
                        (v1, v2) -> v1 // 防止重复 key
                ));

        List<String> dataSites = new ArrayList<>(siteNameMap.keySet());

        // 组装时间区间序列
        List<String> tableHeader = LocalDateTimeUtils.getTimeRangeListByStep(range[0], range[1]);

        //返回结果
        if (CollUtil.isEmpty(dataSites)) {
            return null;
        }

        // 3.去starrocks表中查询数采点位对应的值 15天的
        List<AirConditionerStatusDataDO> airConditionerStatusDataDOS = airConditionerStatusDataMapper.selectList(new LambdaQueryWrapperX<AirConditionerStatusDataDO>()
                .in(AirConditionerStatusDataDO::getDataSite, dataSites)
                .between(AirConditionerStatusDataDO::getSyncTime, range[0], range[1])
        );
        if (CollUtil.isEmpty(airConditionerStatusDataDOS)) {
            return null;
        }
        // 4.组装结构：统计项名称，X轴数据，Y轴数据
        // 4.1 按 dataSite 分组，每组内再按时间点映射数值
        // Map结构: <DataSite, <TimeString, Value>>
        // 修正后的数据处理逻辑
        Map<String, Map<String, BigDecimal>> siteDataMap = new HashMap<>();

        airConditionerStatusDataDOS.stream()
                .filter(data -> data != null && data.getSyncTime() != null)
                .forEach(data -> {
                    // 获取或创建该点位的 Map
                    Map<String, BigDecimal> timeMap = siteDataMap.computeIfAbsent(
                            data.getDataSite(),
                            k -> new LinkedHashMap<>()
                    );

                    String timeStr = DateUtil.format(data.getSyncTime(), "yyyy-MM-dd HH:mm:ss");
                    // 如果存在重复时间点，这里保留第一个（即 ifAbsent 的逻辑）
                    // 如果想保留最新的，可以直接用 timeMap.put(timeStr, ...)
                    BigDecimal value = data.getRawValue() != null ? new BigDecimal(data.getRawValue()) : BigDecimal.ZERO;
                    timeMap.putIfAbsent(timeStr, value);
                });
        // 4.2 为每个 dataSite 构建一条 Y 轴折线数据
        List<ConsumptionStatisticsChartYInfo> yDataList = siteDataMap.entrySet().stream().map(entry -> {
            String dataSite = entry.getKey();
            Map<String, BigDecimal> timeValueMap = entry.getValue();

            // 核心：遍历 tableHeader 确保时间点对齐，缺失点补 0.0
            List<BigDecimal> yAxisData = tableHeader.stream()
                    .map(timePoint -> timeValueMap.getOrDefault(timePoint, BigDecimal.ZERO))
                    .collect(Collectors.toList());

            // 组装单条折线信息
            ConsumptionStatisticsChartYInfo yInfo = new ConsumptionStatisticsChartYInfo();
            yInfo.setName(siteNameMap.getOrDefault(dataSite, dataSite));
            yInfo.setData(yAxisData);
            return yInfo;
        }).collect(Collectors.toList());

        // 4.3 组装最终结果
        ConsumptionStatisticsChartResultVO<ConsumptionStatisticsChartYInfo> result = new ConsumptionStatisticsChartResultVO<>();
        result.setXdata(tableHeader); // X轴：公共时间序列
        result.setYdata(yDataList);         // Y轴：多条折线列表

        return result;
    }

    /**
     * 校验时间范围
     *
     * @param rangeOrigin
     * @return
     */
    private LocalDateTime[] validateRange(LocalDateTime[] rangeOrigin) {
        // 1.校验时间范围
        // 1.1.校验结束时间必须大于开始时间
        LocalDateTime startTime = rangeOrigin[0];
        LocalDateTime endTime = rangeOrigin[1];
        if (!startTime.isBefore(endTime)) {
            throw exception(END_TIME_MUST_AFTER_START_TIME);
        }
        // starTime不能在15天前
        if (!LocalDateTimeUtils.isWithinDays(startTime, LocalDateTime.now(), 15)) {
            throw exception(PARAM_START_TIME_15_DAY_LIMIT);
        }

        // 统一的对齐逻辑：开始时间和结束时间都使用相同的规则
        rangeOrigin[0] = alignToHalfMinute(startTime, true);   // 开始时间
        rangeOrigin[1] = alignToHalfMinute(endTime, false);    // 结束时间

        return rangeOrigin;
    }

    /**
     * 统一的半分钟对齐逻辑【查询时间】
     */
    private LocalDateTime alignToHalfMinute(LocalDateTime time, boolean start) {
        int second = time.getSecond();
        if (second == 30 || second == 0) {
            return time;
        }
        //
        if(start){
            if (second < 30) {
                return time.withSecond(0).withNano(0).plusSeconds(30L);
            } else {
                return time.withSecond(30).withNano(0).plusSeconds(30L);
            }
        }else{
            if (second < 30) {
                return time.withSecond(0).withNano(0);
            } else {
                return time.withSecond(30).withNano(0);
            }
        }

    }
}
