package cn.bitlinks.ems.module.power.service.report.gas;

import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.report.gas.vo.GasMeasurementInfo;
import cn.bitlinks.ems.module.power.dal.dataobject.report.gas.PowerGasMeasurementDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import cn.bitlinks.ems.module.power.dal.mysql.report.gas.PowerGasMeasurementMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.StandingbookMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.attribute.StandingbookAttributeMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.templ.StandingbookTmplDaqAttrMapper;
import cn.bitlinks.ems.module.power.dal.mysql.report.gas.PowerTankSettingsMapper;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.report.gas.PowerTankSettingsDO;
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

        if (measurements.isEmpty()) {
            log.warn("未找到有效的计量器具配置");
            return new ArrayList<>();
        }

        // 批量查询台账属性信息（计量器具编号和名称）
        List<StandingbookAttributeDO> standingbookAttributes = standingbookAttributeMapper.selectList(
                new LambdaQueryWrapperX<StandingbookAttributeDO>()
                        .in(StandingbookAttributeDO::getName, Arrays.asList("计量器具名称", "计量器具编号"))
                        .eq(StandingbookAttributeDO::getDeleted, false)
        );

        // 构建台账ID到属性信息的映射，同时构建 code -> standingbookId / name 的快速查询索引
        Map<Long, Map<String, String>> standingbookAttrMap = new HashMap<>();
        Map<String, Long> codeToStandingbookId = new HashMap<>();
        Map<String, String> codeToAttrName = new HashMap<>();
        for (StandingbookAttributeDO attr : standingbookAttributes) {
            Long standingbookId = attr.getStandingbookId();
            String name = attr.getName();
            String value = attr.getValue();

            // 构建 standingbookId -> {name -> value} 的映射
            standingbookAttrMap.computeIfAbsent(standingbookId, k -> new HashMap<>()).put(name, value);

            // 如果是计量器具编号，构建 code -> standingbookId 的映射
            if ("计量器具编号".equals(name)) {
                codeToStandingbookId.put(value, standingbookId);
            }
            // 如果是计量器具名称，构建 code -> name 的映射
            else if ("计量器具名称".equals(name)) {
                // 需要找到对应的计量器具编号
                Map<String, String> attrMap = standingbookAttrMap.get(standingbookId);
                String code = attrMap.get("计量器具编号");
                if (code != null) {
                    codeToAttrName.put(code, value);
                }
            }
        }

        // 批量查询 Standingbook，构建 standingbookId -> typeId 的映射，再得到 code -> typeId
        Set<Long> standingbookIds = new HashSet<>(codeToStandingbookId.values());
        Map<Long, StandingbookDO> standingbookById = standingbookIds.isEmpty() ? Collections.emptyMap() :
                standingbookMapper.selectList(
                        new LambdaQueryWrapperX<StandingbookDO>().in(StandingbookDO::getId, standingbookIds)
                ).stream().collect(Collectors.toMap(StandingbookDO::getId, sb -> sb));

        Map<String, Long> codeToTypeId = new HashMap<>();
        for (Map.Entry<String, Long> e : codeToStandingbookId.entrySet()) {
            StandingbookDO sb = standingbookById.get(e.getValue());
            if (sb != null && sb.getTypeId() != null) {
                codeToTypeId.put(e.getKey(), sb.getTypeId());
            }
        }

        // 预取模板 data_feature：按 typeId 批量查询，构建 typeId -> dataFeature
        Set<Long> typeIds = codeToTypeId.values().stream().filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Integer> typeIdToDataFeature;
        if (typeIds.isEmpty()) {
            typeIdToDataFeature = Collections.emptyMap();
        } else {
            List<StandingbookTmplDaqAttrDO> tmplList = standingbookTmplDaqAttrMapper.selectList(
                    new cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX<StandingbookTmplDaqAttrDO>()
                            .in(StandingbookTmplDaqAttrDO::getTypeId, typeIds)
                            .eq(StandingbookTmplDaqAttrDO::getDeleted, false)
            );
            typeIdToDataFeature = tmplList.stream()
                    .filter(t -> t.getTypeId() != null && t.getDataFeature() != null)
                    .collect(Collectors.toMap(StandingbookTmplDaqAttrDO::getTypeId, StandingbookTmplDaqAttrDO::getDataFeature, (a, b) -> a));
        }

        // 预取能源参数信息：批量查询power_standingbook_tmpl_daq_attr表，构建typeId+parameter -> code的映射
        Map<String, String> typeIdAndParamToCode = new HashMap<>();
        if (!typeIds.isEmpty()) {
            // 获取所有需要的能源参数名称
            Set<String> energyParams = measurements.stream()
                    .map(PowerGasMeasurementDO::getEnergyParam)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            
            if (!energyParams.isEmpty()) {
                List<StandingbookTmplDaqAttrDO> energyTmplAttrs = standingbookTmplDaqAttrMapper.selectList(
                        new LambdaQueryWrapperX<StandingbookTmplDaqAttrDO>()
                                .in(StandingbookTmplDaqAttrDO::getTypeId, typeIds)
                                .in(StandingbookTmplDaqAttrDO::getParameter, energyParams)
                                .eq(StandingbookTmplDaqAttrDO::getEnergyFlag, true) // 只查询能源数采参数
                                .eq(StandingbookTmplDaqAttrDO::getDeleted, false)
                );
                
                // 构建 typeId + parameter -> code 的映射
                for (StandingbookTmplDaqAttrDO attr : energyTmplAttrs) {
                    String key = attr.getTypeId() + "_" + attr.getParameter();
                    typeIdAndParamToCode.put(key, attr.getCode());
                }
                
                log.info("批量查询到{}条能源参数模板配置", energyTmplAttrs.size());
            }
        }

        // 预取储罐设置编码集合：判断是否液压
        Set<String> tankCodes = powerTankSettingsMapper.selectList().stream()
                .map(PowerTankSettingsDO::getCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

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

            // 使用预构建索引，O(1) 获取 standingbookId / attrName / typeId
            String code = measurement.getMeasurementCode();
            Long mappedSbId = codeToStandingbookId.get(code);
            if (mappedSbId != null) {
                standingbookId = mappedSbId;
                String attrName = codeToAttrName.get(code);
                if (attrName != null && !attrName.trim().isEmpty()) {
                    measurementName = attrName;
                }
                typeId = codeToTypeId.get(code);
            }

            info.setStandingbookId(standingbookId);
            info.setTypeId(typeId);
            info.setMeasurementName(measurementName);

            if (standingbookId == null) {
                log.debug("未找到计量器具编码 {} 对应的台账信息，使用默认名称", measurement.getMeasurementCode());
            }

            // 设置参数编码 - 修改逻辑：通过power_standingbook_tmpl_daq_attr表查询
            String paramCode = null;
            if (typeId != null) {
                // 使用预构建的映射关系查询参数编码
                String key = typeId + "_" + measurement.getEnergyParam();
                paramCode = typeIdAndParamToCode.get(key);
                
                if (paramCode != null) {
                    log.debug("找到计量器具 {} 对应的参数编码: {}", measurement.getMeasurementCode(), paramCode);
                } else {
                    log.debug("未找到计量器具 {} 对应的参数编码，typeId: {}, energyParam: {}", 
                            measurement.getMeasurementCode(), typeId, measurement.getEnergyParam());
                }
            } else {
                log.debug("计量器具 {} 没有对应的typeId，无法查询参数编码", measurement.getMeasurementCode());
            }
            
            info.setParamCode(paramCode);

            // 动态计算计算类型（使用预取的 data_feature 与储罐设置编码，避免 N+1 查询）
            Integer calculateType = null;
            if (typeId != null) {
                Integer dataFeature = typeIdToDataFeature.get(typeId);
                if (dataFeature != null) {
                    if (dataFeature == 1) {
                        calculateType = 1; // 累计值
                    } else if (dataFeature == 2) {
                        // 稳态/液压：若存在对应储罐设置编码，则为液压，否则稳态
                        calculateType = tankCodes.contains(code) ? 2 : 0;
                    }
                }
            }
            info.setCalculateType(calculateType);

            // 如果计算类型为null，设置为3，无需计算，数值统一设置为0
            if (calculateType == null) {
                info.setCalculateType(3);
            }

            result.add(info);
        }

        log.info("处理完成，返回{}条计量器具信息", result.size());

        // 记录返回的计量器具信息条数
        log.info("返回{}条计量器具信息", result.size());
        
        // 添加调试信息：统计参数编码获取情况
        long successCount = result.stream().filter(info -> info.getParamCode() != null).count();
        long failCount = result.size() - successCount;
        log.info("参数编码获取统计 - 成功: {}, 失败: {}", successCount, failCount);
        
        // 如果有失败的，输出详细信息
        if (failCount > 0) {
            result.stream()
                    .filter(info -> info.getParamCode() == null)
                    .forEach(info -> log.warn("未获取到参数编码 - 计量器具: {}, typeId: {}, energyParam: {}", 
                            info.getMeasurementCode(), info.getTypeId(), info.getEnergyParam()));
        }

        return result;
    }

    /**
     * 动态计算计算类型
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
