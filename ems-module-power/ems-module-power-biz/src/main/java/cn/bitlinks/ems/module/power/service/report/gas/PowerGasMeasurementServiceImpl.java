package cn.bitlinks.ems.module.power.service.report.gas;

import cn.bitlinks.ems.module.power.controller.admin.report.gas.vo.GasMeasurementInfo;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.report.gas.PowerGasMeasurementDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import cn.bitlinks.ems.module.power.dal.mysql.energyconfiguration.EnergyConfigurationMapper;
import cn.bitlinks.ems.module.power.dal.mysql.report.gas.PowerGasMeasurementMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.StandingbookMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.attribute.StandingbookAttributeMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.templ.StandingbookTmplDaqAttrMapper;
import cn.bitlinks.ems.module.power.dal.mysql.report.gas.PowerTankSettingsMapper;
import cn.bitlinks.ems.module.power.dal.mysql.energyparameters.EnergyParametersMapper;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.report.gas.PowerTankSettingsDO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyparameters.EnergyParametersDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 气化科固定43条计量器具配置 Service 实现类
 *
 * @author bmqi
 */
@Service
@Validated
@Slf4j
public class PowerGasMeasurementServiceImpl implements PowerGasMeasurementService {

    @Resource
    private PowerGasMeasurementMapper powerGasMeasurementMapper;

    @Resource
    private StandingbookMapper standingbookMapper;

    @Resource
    private StandingbookAttributeMapper standingbookAttributeMapper;

    @Resource
    private StandingbookTmplDaqAttrMapper standingbookTmplDaqAttrMapper;

    @Resource
    private PowerTankSettingsMapper powerTankSettingsMapper;

    @Resource
    private EnergyParametersMapper energyParametersMapper;

    @Resource
    private EnergyConfigurationMapper energyConfigurationMapper;

    @Override
    public List<PowerGasMeasurementDO> getAllValidMeasurements() {
        return powerGasMeasurementMapper.selectAllValid();
    }

    @Override
    public List<PowerGasMeasurementDO> getMeasurementsByCodes(List<String> measurementCodes) {
        return powerGasMeasurementMapper.selectByMeasurementCodes(measurementCodes);
    }

    @Override
    public List<GasMeasurementInfo> getGasMeasurementInfos() {
        // 获取所有有效的计量器具配置
        List<PowerGasMeasurementDO> measurements = getAllValidMeasurements();
        log.info("从数据库获取到{}条计量器具配置", measurements.size());

        // 打印前几条记录的编码，用于调试
        if (!measurements.isEmpty()) {
            log.info("前5条记录的编码: {}", measurements.stream()
                    .limit(5)
                    .map(PowerGasMeasurementDO::getMeasurementCode)
                    .collect(Collectors.toList()));
        }

        if (measurements.isEmpty()) {
            log.warn("未找到有效的计量器具配置");
            return new ArrayList<>();
        }

        // 提取所有计量器具编码
        List<String> measurementCodes = measurements.stream()
                .map(PowerGasMeasurementDO::getMeasurementCode)
                .collect(Collectors.toList());

        // 批量查询台账属性信息（计量器具编号和名称）
        List<StandingbookAttributeDO> standingbookAttributes = standingbookAttributeMapper.selectList(
                new cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX<StandingbookAttributeDO>()
                        .in(StandingbookAttributeDO::getName, Arrays.asList("计量器具名称", "计量器具编号"))
                        .eq(StandingbookAttributeDO::getDeleted, false)
        );

        // 构建台账ID到属性信息的映射
        Map<Long, Map<String, String>> standingbookAttrMap = new HashMap<>();
        for (StandingbookAttributeDO attr : standingbookAttributes) {
            Long standingbookId = attr.getStandingbookId();
            String name = attr.getName();
            String value = attr.getValue();

            standingbookAttrMap.computeIfAbsent(standingbookId, k -> new HashMap<>()).put(name, value);
        }

        // 批量查询能源参数信息
        List<String> energyParams = measurements.stream()
                .map(PowerGasMeasurementDO::getEnergyParam)
                .distinct()
                .collect(Collectors.toList());

        log.info("需要查询的能源参数: {}", energyParams);

        List<Long> energyIdS = energyConfigurationMapper
                .selectList(new LambdaQueryWrapper<EnergyConfigurationDO>().in(EnergyConfigurationDO::getEnergyName, Arrays.asList("气化-正累积", "气化-压力", "气化-压差")))
                .stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList());

        List<EnergyParametersDO> energyParametersList = energyParametersMapper.selectList(
                new LambdaQueryWrapper<EnergyParametersDO>()
                        .in(EnergyParametersDO::getParameter, energyParams)
                        .in(EnergyParametersDO::getEnergyId, energyIdS)
        );

        log.info("查询到{}条能源参数", energyParametersList.size());

        // 构建能源参数中文名到参数信息的映射
        Map<String, EnergyParametersDO> energyParamMap = energyParametersList.stream()
                .collect(Collectors.toMap(EnergyParametersDO::getParameter, param -> param));

        // 构建结果
        List<GasMeasurementInfo> result = new ArrayList<>();
        log.info("开始处理{}条计量器具配置", measurements.size());

        for (PowerGasMeasurementDO measurement : measurements) {
            GasMeasurementInfo info = new GasMeasurementInfo();

            // 设置基本信息
            info.setMeasurementCode(measurement.getMeasurementCode());
            info.setEnergyParam(measurement.getEnergyParam());
            info.setSortNo(measurement.getSortNo());

            // 根据计量器具编码查找对应的台账ID和类型ID
            Long standingbookId = null;
            Long typeId = null;
            String measurementName = measurement.getMeasurementName(); // 优先使用表中的名称
            if (measurementName == null || measurementName.trim().isEmpty()) {
                measurementName = measurement.getMeasurementCode(); // 如果名称为空，使用编码
            }

            // 遍历所有台账属性，找到匹配的计量器具编号
            for (Map.Entry<Long, Map<String, String>> entry : standingbookAttrMap.entrySet()) {
                Long sbId = entry.getKey();
                Map<String, String> attrs = entry.getValue();
                String attrCode = attrs.get("计量器具编号");

                if (measurement.getMeasurementCode().equals(attrCode)) {
                    standingbookId = sbId;
                    // 如果有台账，优先使用台账中的名称
                    String attrName = attrs.get("计量器具名称");
                    if (attrName != null && !attrName.trim().isEmpty()) {
                        measurementName = attrName;
                    }

                    // 获取台账类型ID
                    StandingbookDO standingbook = standingbookMapper.selectById(sbId);
                    if (standingbook != null) {
                        typeId = standingbook.getTypeId();
                    }
                    break;
                }
            }

            info.setStandingbookId(standingbookId);
            info.setTypeId(typeId);
            info.setMeasurementName(measurementName);

            if (standingbookId == null) {
                log.debug("未找到计量器具编码 {} 对应的台账信息，使用默认名称", measurement.getMeasurementCode());
            }

            // 设置参数编码
            EnergyParametersDO energyParam = energyParamMap.get(measurement.getEnergyParam());
            if (energyParam != null) {
                info.setParamCode(energyParam.getCode());
            } else {
                info.setParamCode(null);
                log.debug("未找到能源参数中文名 {} 对应的参数编码", measurement.getEnergyParam());
            }

            // 动态计算计算类型
            Integer calculateType = calculateTypeByViewLogic(info);
            info.setCalculateType(calculateType);

            // 如果计算类型为null，设置为默认值0（稳态值）
            if (calculateType == null) {
                info.setCalculateType(0);
            }

            result.add(info);
        }

        log.info("处理完成，返回{}条计量器具信息", result.size());

        // 记录返回的计量器具信息条数
        log.info("返回{}条计量器具信息", result.size());

        return result;
    }

    /**
     * 根据视图逻辑动态计算计算类型
     * 参考视图 v_power_measurement_attributes 的逻辑
     */
    private Integer calculateTypeByViewLogic(GasMeasurementInfo info) {
        if (info.getStandingbookId() == null || info.getTypeId() == null) {
            return null;
        }

        try {
            // 获取模板属性
            List<StandingbookTmplDaqAttrDO> tmplAttrs = standingbookTmplDaqAttrMapper.selectList(
                    new cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX<StandingbookTmplDaqAttrDO>()
                            .eq(StandingbookTmplDaqAttrDO::getTypeId, info.getTypeId())
                            .eq(StandingbookTmplDaqAttrDO::getDeleted, false)
            );

            if (tmplAttrs.isEmpty()) {
                return null;
            }

            // 获取第一个模板属性的 data_feature
            Integer dataFeature = tmplAttrs.get(0).getDataFeature();
            if (dataFeature == null) {
                return null;
            }

            // 根据视图逻辑判断计算类型
            if (dataFeature == 1) {
                // 累计值
                return 1;
            } else if (dataFeature == 2) {
                // 稳态值，需要判断是否有储罐设置
                PowerTankSettingsDO tankSettings = powerTankSettingsMapper.selectOne(
                        new cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX<PowerTankSettingsDO>()
                                .eq(PowerTankSettingsDO::getCode, info.getMeasurementCode())
                                .eq(PowerTankSettingsDO::getDeleted, false)
                );

                if (tankSettings != null) {
                    // 有储罐设置，为液压计算
                    return 2;
                } else {
                    // 无储罐设置，为稳态值
                    return 0;
                }
            } else {
                // 其他情况
                return null;
            }
        } catch (Exception e) {
            log.error("计算计算类型失败，standingbookId: {}, typeId: {}", info.getStandingbookId(), info.getTypeId(), e);
            return null;
        }
    }
}
