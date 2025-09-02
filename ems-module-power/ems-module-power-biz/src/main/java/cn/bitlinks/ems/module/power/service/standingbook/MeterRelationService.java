package cn.bitlinks.ems.module.power.service.standingbook;

import cn.bitlinks.ems.framework.common.util.json.JsonUtils;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.framework.dict.core.DictFrameworkUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.MeterRelationExcelDTO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookDTO;
import cn.bitlinks.ems.module.power.dal.dataobject.measurementassociation.MeasurementAssociationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.measurementdevice.MeasurementDeviceDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.mysql.measurementassociation.MeasurementAssociationMapper;
import cn.bitlinks.ems.module.power.dal.mysql.measurementdevice.MeasurementDeviceMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.StandingbookMapper;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cn.bitlinks.ems.module.power.enums.DictTypeConstants.STAGE;
import static cn.bitlinks.ems.module.power.enums.RedisKeyConstants.STANDING_BOOK_EXCEL_RELATION;

/**
 * 计量器具关联关系业务服务（实际业务需对接数据库/缓存）
 */
@Slf4j
@Service("meterRelationService")
public class MeterRelationService {
    @Resource
    @Lazy
    private StandingbookService standingbookService;
    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;
    @Resource
    private MeasurementAssociationMapper measurementAssociationMapper;
    @Resource
    private MeasurementDeviceMapper measurementDeviceMapper;

    @Resource
    private StandingbookMapper standingbookMapper;

    /**
     * 校验计量器具编号是否在系统中存在
     *
     * @param meterCode 计量器具编号
     * @return true=存在，false=不存在
     */
    public boolean checkMeterCodeExists(String meterCode) {
        Set<String> codeList = standingbookService.getStandingbookCodeMeasurementSet();
        if (CollUtil.isEmpty(codeList)) {
            return false;
        }
        return codeList.contains(meterCode);
    }

    /**
     * 校验多个下级计量器具编号是否存在（返回不存在的编号）
     *
     * @param subMeterCodes 下级计量器具编号数组
     * @return 不存在的编号集合（空=全部存在）
     */
    public Set<String> checkSubMeterCodesExists(String[] subMeterCodes) {
        Set<String> codeList = standingbookService.getStandingbookCodeMeasurementSet();
        if (CollUtil.isEmpty(codeList)) {
            return new HashSet<>(Arrays.asList(subMeterCodes));
        }
        return Arrays.asList(subMeterCodes).stream()
                .filter(code -> !codeList.contains(code))
                .collect(Collectors.toSet());
    }

    /**
     * 校验关联设备是否存在
     */
    public boolean checkDeviceExists(String device) {
        Set<String> codeList = standingbookService.getStandingbookCodeDeviceSet();
        if (CollUtil.isEmpty(codeList)) {
            return false;
        }
        return codeList.contains(device);
    }

    /**
     * 校验环节是否存在
     */
    public boolean checkLinkExists(String link) {
        List<String> systemLinks = DictFrameworkUtils.getDictDataLabelList(
                STAGE);
        return systemLinks.contains(link);
    }

    /**
     * 批量暂存合法数据（避免内存溢出，解析完成后统一入库）
     */
    public void batchSaveTemp(List<MeterRelationExcelDTO> validDataList) {
        // 实际逻辑：存入Redis缓存
        byteArrayRedisTemplate.opsForValue().set(STANDING_BOOK_EXCEL_RELATION, StrUtils.compressGzip(JsonUtils.toJsonString(validDataList)));
        log.info("暂存合法数据{}条", validDataList.size());
    }

    /**
     * 最终入库（Excel解析完成且无错误时调用）
     */
    @Transactional
    public int batchSaveToDb() {
        // 1. 从 Redis 读取缓存数据
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(STANDING_BOOK_EXCEL_RELATION);
        if (compressed == null) {
            log.warn("缓存中无待入库的计量器具关联数据");
            return 0;
        }

        List<MeterRelationExcelDTO> serverStandingbookList = null;
        try {
            // 2. 解压并解析缓存数据
            String cacheRes = StrUtils.decompressGzip(compressed);
            if (CharSequenceUtil.isEmpty(cacheRes)) {
                log.warn("缓存数据解压后为空");
                return 0;
            }
            serverStandingbookList = JsonUtils.parseArray(cacheRes, MeterRelationExcelDTO.class);
            if (CollUtil.isEmpty(serverStandingbookList)) {
                log.warn("解析后的缓存数据为空列表");
                return 0;
            }

            // 3. 核心入库逻辑（编码转ID、批量插入）
            List<StandingbookDTO> allStandingbookDTOList = standingbookService.getStandingbookDTOList();
            Map<String, Long> sbCodeIdMapping = allStandingbookDTOList.stream()
                    .collect(Collectors.toMap(
                            StandingbookDTO::getCode,
                            StandingbookDTO::getStandingbookId,
                            (k1, k2) -> k2
                    ));

            // 3.1 处理下级计量器具关联关系（编码转ID）
            List<Long> delMeasurementIds = new ArrayList<>();
            List<MeasurementAssociationDO> associationList = serverStandingbookList.stream()
                    .filter(dto -> StringUtils.hasText(dto.getSubMeterCodes()))
                    .flatMap(dto -> {
                        String[] subCodes = dto.getSubMeterCodes().split(";");
                        Long parentId = sbCodeIdMapping.get(dto.getMeterCode());
                        if (parentId == null) {
                            log.error("计量器具编码{}未找到对应的ID，跳过", dto.getMeterCode());
                            return Stream.empty();
                        }
                        delMeasurementIds.add(parentId);
                        return Arrays.stream(subCodes)
                                .map(subCode -> {
                                    Long subId = sbCodeIdMapping.get(subCode);
                                    if (subId == null) {
                                        log.error("下级计量器具编码{}未找到对应的ID，跳过", subCode);
                                        return null;
                                    }
                                    MeasurementAssociationDO association = new MeasurementAssociationDO();
                                    association.setMeasurementInstrumentId(parentId);
                                    association.setMeasurementId(subId);
                                    // 补充其他字段（如创建时间、状态等）
                                    return association;
                                })
                                .filter(Objects::nonNull); // 过滤空对象
                    })
                    .collect(Collectors.toList());
            if (CollUtil.isNotEmpty(associationList)) {
                // 先删后增
                measurementAssociationMapper.delete(new LambdaQueryWrapperX<MeasurementAssociationDO>()
                        .in(MeasurementAssociationDO::getMeasurementInstrumentId, delMeasurementIds));
                measurementAssociationMapper.insertBatch(associationList);
                log.info("批量插入计量器具关联关系{}条", associationList.size());
            }
            // 3.2 构建计量器具-设备的关联关系列表
            List<Long> delDeviceMeasurementIds = new ArrayList<>();
            List<MeasurementDeviceDO> deviceAssociationList = serverStandingbookList.stream()
                    // 过滤条件：1.关联设备编码非空；2.计量器具ID存在（避免空指针）；3.设备ID存在
                    .filter(dto -> {
                        String deviceCode = dto.getRelatedDevice();
                        Long deviceId = sbCodeIdMapping.get(deviceCode);
                        Long sbId = sbCodeIdMapping.get(dto.getMeterCode()); // sbCodeIdMapping 是原有计量器具编码→ID映射
                        return StringUtils.hasText(deviceCode)
                                && sbId != null
                                && deviceId != null;
                    })
                    // 转换为关联关系实体
                    .map(dto -> {
                        MeasurementDeviceDO association = new MeasurementDeviceDO();
                        // 计量器具ID：从计量器具编码转换（原有映射 sbCodeIdMapping）
                        association.setMeasurementInstrumentId(sbCodeIdMapping.get(dto.getMeterCode()));
                        // 设备ID：从设备编码转换（新查询的 deviceCodeIdMapping）
                        association.setDeviceId(sbCodeIdMapping.get(dto.getRelatedDevice()));
                        delDeviceMeasurementIds.add(sbCodeIdMapping.get(dto.getMeterCode()));
                        return association;
                    })
                    .collect(Collectors.toList());
            if (CollUtil.isNotEmpty(deviceAssociationList)) {
                // 先删后增
                measurementDeviceMapper.delete(new LambdaQueryWrapperX<MeasurementDeviceDO>()
                        .in(MeasurementDeviceDO::getMeasurementInstrumentId, delDeviceMeasurementIds));
                measurementDeviceMapper.insertBatch(deviceAssociationList);
                log.info("批量插入计量器具关联关系{}条", deviceAssociationList.size());
            }
            // 3.3 构建计量器具-环节列表

            // 步骤3：构建批量更新DTO列表（定位记录+设置新值）
            List<StandingbookDO> updateStageDTOList = serverStandingbookList.stream()
                    // 过滤条件：1.环节名称非空；2.计量器具ID存在；3.环节值有效
                    .filter(dto -> {
                        String linkName = dto.getStage().trim();
                        Long sbId = sbCodeIdMapping.get(dto.getMeterCode()); // 计量器具ID（前文已构建）

                        return StringUtils.hasText(linkName)
                                && sbId != null
                                && DictFrameworkUtils.getDictDataLabelList(STAGE).contains(linkName);
                    })
                    // 转换为更新DTO
                    .map(dto -> {
                        StandingbookDO updateDTO = new StandingbookDO();
                        updateDTO.setId(sbCodeIdMapping.get(dto.getMeterCode()));
                        updateDTO.setStage(Integer.valueOf(DictFrameworkUtils.parseDictDataValue(STAGE, dto.getStage().trim())));
                        return updateDTO;
                    })
                    .collect(Collectors.toList());
            // 步骤4：批量更新stage字段（避免循环单条更新，提高性能）
            if (CollUtil.isNotEmpty(updateStageDTOList)) {
                standingbookMapper.batchUpdateStage(updateStageDTOList);
                log.info("批量更新计量器具表stage字段{}条", updateStageDTOList.size());
            }

            // 4. 注册事务同步：事务提交后删除缓存（关键！避免事务回滚导致的不一致）
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 事务提交成功后，删除缓存
                    try {
                        byteArrayRedisTemplate.delete(STANDING_BOOK_EXCEL_RELATION);
                        log.info("计量器具关联数据入库成功，删除Redis缓存：{}", STANDING_BOOK_EXCEL_RELATION);
                    } catch (Exception e) {
                        log.error("删除Redis缓存失败，key: {}", STANDING_BOOK_EXCEL_RELATION, e);
                        // 兜底：给缓存设置短期过期时间，避免长期脏数据
                        byteArrayRedisTemplate.expire(STANDING_BOOK_EXCEL_RELATION, 5, TimeUnit.MINUTES);
                    }
                }
            });
            return serverStandingbookList.size();
        } catch (Exception e) {
            log.error("计量器具关联数据入库失败", e);
            // 入库失败，无需删除缓存（后续可重试入库）
            throw new RuntimeException("批量入库失败", e); // 抛出异常触发事务回滚
        }
    }

}