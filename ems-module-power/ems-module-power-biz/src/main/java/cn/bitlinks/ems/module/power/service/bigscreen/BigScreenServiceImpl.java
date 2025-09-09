package cn.bitlinks.ems.module.power.service.bigscreen;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.BigScreenParamReqVO;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.BigScreenRespVO;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.OutsideEnvData;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.BigScreenCopChartData;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.ReportParamVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.UsageCostData;
import cn.bitlinks.ems.module.power.dal.dataobject.bigscreen.PowerPureWasteWaterGasSettingsDO;
import cn.bitlinks.ems.module.power.dal.dataobject.collectrawdata.CollectRawDataDO;
import cn.bitlinks.ems.module.power.dal.mysql.bigscreen.PowerPureWasteWaterGasSettingsMapper;
import cn.bitlinks.ems.module.power.service.collectrawdata.CollectRawDataService;
import cn.bitlinks.ems.module.power.service.cophouraggdata.CopHourAggDataService;
import cn.bitlinks.ems.module.power.service.usagecost.UsageCostService;
import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.module.power.enums.CommonConstants.*;

/**
 * 台账属性 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class BigScreenServiceImpl implements BigScreenService {

    @Resource
    private CopHourAggDataService copHourAggDataService;

    @Resource
    private CollectRawDataService collectRawDataService;

    @Resource
    private UsageCostService usageCostService;

    @Resource
    private PowerPureWasteWaterGasSettingsMapper powerPureWasteWaterGasSettingsMapper;

    @Override
    public BigScreenRespVO getBigScreenDetails(BigScreenParamReqVO paramVO) {

        BigScreenRespVO resultVO = new BigScreenRespVO();

        // 1. 中部
        // 中1 4#宿舍楼
        // 中2 2#生产厂房
        // 中3 3#办公楼
        // 中4 5#CUB
        // 中5 1#生产厂房

        // 2. 右部
        // 2.1. 右1 室外工况
        OutsideEnvData outsideEnvData = new OutsideEnvData();
        List<String> dataSites = Arrays.asList(WIND_DIRECTION_IO, WIND_SPEED_IO, TEMPERATURE_IO, HUMIDITY_IO, DEW_POINT_IO, ATMOSPHERIC_PRESSURE_IO, NOISE_IO);
        List<CollectRawDataDO> outsideDataList = collectRawDataService.getOutsideDataByDataSite(dataSites);
        if (CollUtil.isNotEmpty(outsideDataList)) {
            Map<String, CollectRawDataDO> outsideDataMap = outsideDataList
                    .stream()
                    .collect(Collectors.toMap(CollectRawDataDO::getDataSite, Function.identity()));

//          todo   outsideEnvData.setWindDirection();
            outsideEnvData.setWindDirectionValue(new BigDecimal(outsideDataMap.get(WIND_DIRECTION_IO).getRawValue()));
            outsideEnvData.setWindSpeed((new BigDecimal(outsideDataMap.get(WIND_SPEED_IO).getRawValue())));
            outsideEnvData.setTemperature(new BigDecimal(outsideDataMap.get(TEMPERATURE_IO).getRawValue()));
            outsideEnvData.setHumidity(new BigDecimal(outsideDataMap.get(HUMIDITY_IO).getRawValue()));
            outsideEnvData.setDewPoint(new BigDecimal(outsideDataMap.get(DEW_POINT_IO).getRawValue()));
            outsideEnvData.setAtmosphericPressure(new BigDecimal(outsideDataMap.get(ATMOSPHERIC_PRESSURE_IO).getRawValue()));
            outsideEnvData.setNoise(new BigDecimal(outsideDataMap.get(NOISE_IO).getRawValue()));

        }

        // 2.2. 右2 获取cop数据
        ReportParamVO reportParamVO = BeanUtils.toBean(paramVO, ReportParamVO.class);
        BigScreenCopChartData copChart = copHourAggDataService.copChartForBigScreen(reportParamVO);
        resultVO.setCop(copChart);

        // 2.3. 右3 纯废水单价
        List<String> system = Arrays.asList(PURE, WASTE);
        List<PowerPureWasteWaterGasSettingsDO> pureWasteWaterList = powerPureWasteWaterGasSettingsMapper.selectList(new LambdaQueryWrapperX<PowerPureWasteWaterGasSettingsDO>()
                .in(PowerPureWasteWaterGasSettingsDO::getSystem, system));

        if (CollUtil.isNotEmpty(pureWasteWaterList)) {
            List<Long> sbList = pureWasteWaterList
                    .stream()
                    .map(PowerPureWasteWaterGasSettingsDO::getStandingbookIds)
                    .filter(Objects::nonNull)
                    .map(s -> {
                        String[] split = s.split(",");
                        return Arrays.stream(split).map(Long::valueOf).collect(Collectors.toList());
                    })
                    .flatMap(List::stream)
                    .collect(Collectors.toList());


            // 按台账和日分组求成本和
            List<UsageCostData> list = usageCostService.getList(
                    paramVO.getRange()[0],
                    paramVO.getRange()[1],
                    sbList);

            // 加上化学品的成本

            // 查找用量
        }


        // 2.4. 右4 压缩空气单价


        // 3. 底部
        // 3.1. 单位产品综合能耗


        return resultVO;
    }
}
