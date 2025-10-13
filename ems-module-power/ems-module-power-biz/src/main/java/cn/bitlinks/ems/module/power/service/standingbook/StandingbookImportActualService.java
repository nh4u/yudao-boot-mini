package cn.bitlinks.ems.module.power.service.standingbook;

import cn.bitlinks.ems.framework.common.util.json.JsonUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookDTO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookExcelDTO;
import cn.bitlinks.ems.module.power.dal.dataobject.doublecarbon.DoubleCarbonMappingDO;
import cn.bitlinks.ems.module.power.dal.dataobject.labelconfig.LabelConfigDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookLabelInfoDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import cn.bitlinks.ems.module.power.dal.mysql.doublecarbon.DoubleCarbonMappingMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.StandingbookLabelInfoMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.StandingbookMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.attribute.StandingbookAttributeMapper;
import cn.bitlinks.ems.module.power.enums.RedisKeyConstants;
import cn.bitlinks.ems.module.power.enums.standingbook.ImportTemplateType;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.bitlinks.ems.module.power.service.standingbook.attribute.StandingbookAttributeService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.module.power.enums.ApiConstants.*;
import static cn.bitlinks.ems.module.power.enums.RedisKeyConstants.STANDING_BOOK_EXCEL;

/**
 * 台账标签导入业务服务（实际业务需对接数据库/缓存）
 */
@Slf4j
@Service("standingbookImportActualService")
public class StandingbookImportActualService {
    @Resource
    @Lazy
    private StandingbookService standingbookService;
    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;


    @Resource
    private StandingbookMapper standingbookMapper;

    @Resource
    @Lazy
    private StandingbookAttributeService standingbookAttributeService;
    @Resource
    private StandingbookLabelInfoMapper standingbookLabelMapper;
    @Resource
    private StandingbookAttributeMapper standingbookAttributeMapper;
    @Resource
    private DoubleCarbonMappingMapper doubleCarbonMappingMapper;
    @Autowired
    private LabelConfigService labelConfigService;


    /**
     * 批量暂存合法数据（避免内存溢出，解析完成后统一入库）
     */
    public void batchSaveTemp(List<StandingbookExcelDTO> validDataList) {
        // 实际逻辑：存入Redis缓存
        byteArrayRedisTemplate.opsForValue().set(STANDING_BOOK_EXCEL, StrUtils.compressGzip(JsonUtils.toJsonString(validDataList)));
        log.info("台账excel：暂存合法数据{}条", validDataList.size());
    }

    /**
     * 校验多个标签编码是否都在系统中存在
     *
     * @param labelCodes       待校验的标签编码集合
     * @param systemLabelCodes 系统已有的标签编码集合
     * @return true=全部存在，false=存在缺失
     */
    public boolean checkLabelCodesExists(List<String> labelCodes, List<String> systemLabelCodes) {
        if (CollUtil.isEmpty(labelCodes)) {
            return true;
        }
        if (CollUtil.isEmpty(systemLabelCodes)) {
            return false;
        }
        return new HashSet<>(systemLabelCodes).containsAll(labelCodes);
    }

    /**
     * 校验台账类型编码是否在系统中存在
     *
     * @param typeCode 台账类型编码
     * @return true=存在，false=不存在
     */
    public boolean checkTypeCodeExists(String typeCode, List<String> typeCodes) {
        if (CollUtil.isEmpty(typeCodes)) {
            return false;
        }
        return typeCodes.contains(typeCode);
    }

    public boolean checkMeterCodeExists(String meterCode, ImportTemplateType tmplEnum) {
        List<StandingbookDTO> allStandingbookDTOList = standingbookService.getStandingbookDTOList();
        if (CollUtil.isEmpty(allStandingbookDTOList)) {
            return false;
        }
        List<String> codes = allStandingbookDTOList.stream()
                .map(StandingbookDTO::getCode)
                .collect(Collectors.toList());


        if (CollUtil.isEmpty(codes)) {
            return false;
        }
        return codes.contains(meterCode);
    }

    /**
     * 最终入库（Excel解析完成且无错误时调用）
     */
    @Transactional
    @CacheEvict(value = {RedisKeyConstants.STANDING_BOOK_LIST,
            RedisKeyConstants.STANDING_BOOK_MEASUREMENT_CODE_LIST,
            RedisKeyConstants.STANDING_BOOK_DEVICE_CODE_LIST}, allEntries = true)
    public int batchSaveToDb(Map<String, StandingbookTypeDO> sysTypeMap,ImportTemplateType tmplEnum) {
        // 1. 从 Redis 读取缓存数据
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(STANDING_BOOK_EXCEL);
        if (compressed == null) {
            log.warn("台账标签：缓存中无待入库的数据");
            return 0;
        }

        List<StandingbookExcelDTO> serverStandingbookList = null;
        try {
            // 2. 解压并解析缓存数据
            String cacheRes = StrUtils.decompressGzip(compressed);
            if (CharSequenceUtil.isEmpty(cacheRes)) {
                log.warn("台账标签：缓存数据解压后为空");
                return 0;
            }
            serverStandingbookList = JsonUtils.parseArray(cacheRes, StandingbookExcelDTO.class);
            if (CollUtil.isEmpty(serverStandingbookList)) {
                log.warn("台账标签：解析后的缓存数据为空列表");
                return 0;
            }

            // 查询备用基础数据
            List<StandingbookAttributeDO> allTypeAttrList = standingbookAttributeService.getStandingbookAttributeList();
            Map<Long, List<StandingbookAttributeDO>> allTypeAttrMap = allTypeAttrList.stream()
                    .collect(Collectors.groupingBy(StandingbookAttributeDO::getTypeId));
            List<LabelConfigDO> labelConfigDOS = labelConfigService.getAllLabelConfig();
            Map<Long, LabelConfigDO> labelIdMap = labelConfigDOS.stream()
                    .collect(Collectors.toMap(LabelConfigDO::getId, Function.identity()));
            Map<String, LabelConfigDO> labelCodeMap = labelConfigDOS.stream()
                    .collect(Collectors.toMap(LabelConfigDO::getCode, Function.identity()));
            List<LabelConfigDO> topLevelLabelNamesList = standingbookService.loadTopLevelLabelNamesList();
            // 3. 入库
            List<StandingbookDO> standingbooks = new ArrayList<>();
            List<StandingbookAttributeDO> attributes = new ArrayList<>();
            List<StandingbookLabelInfoDO> labels = new ArrayList<>();
            List<DoubleCarbonMappingDO> doubleCarbonMappings = new ArrayList<>();

            for (StandingbookExcelDTO dto : serverStandingbookList) {
                Long sbId = IdUtil.getSnowflake().nextId();

                Long typeId = sysTypeMap.get(dto.getTypeCode()).getId();
                // 1. 生成台账数据
                StandingbookDO sb = new StandingbookDO();
                sb.setId(sbId);
                sb.setTypeId(typeId);
                standingbooks.add(sb);
                // 插入台账映射表 code
                DoubleCarbonMappingDO mappingDO = new DoubleCarbonMappingDO();
                mappingDO.setStandingbookCode(dto.getSbCode());
                mappingDO.setStandingbookId(sbId);
                doubleCarbonMappings.add(mappingDO);
                // 2. 生成台账属性表（根据分类编码查内置属性）
                // 查询属性分类部分的关联属性
                allTypeAttrMap.get(typeId).forEach(standingbookAttributeDO -> {
                    StandingbookAttributeDO attribute = BeanUtils.toBean(standingbookAttributeDO, StandingbookAttributeDO.class);
                    if (standingbookAttributeDO.getCode().equals(ATTR_MEASURING_INSTRUMENT_MAME) || standingbookAttributeDO.getCode().equals(ATTR_EQUIPMENT_NAME)) {
                        attribute.setValue(dto.getSbName());
                    } else if (standingbookAttributeDO.getCode().equals(ATTR_MEASURING_INSTRUMENT_ID) || standingbookAttributeDO.getCode().equals(ATTR_EQUIPMENT_ID)) {
                        attribute.setValue(dto.getSbCode());
                    } else if (standingbookAttributeDO.getCode().equals(ATTR_TABLE_TYPE)) {
                        attribute.setValue(dto.getTableType());
                    } else {
                        attribute.setValue(null);
                    }
                    attribute.setStandingbookId(sbId);
                    attribute.setId(null);
                    attribute.setCreateTime(null);
                    attribute.setUpdateTime(null);
                    attributes.add(attribute);
                });

                // 3. 插入台账标签表
                if (CollUtil.isNotEmpty(dto.getLabelMap())) {
                    for (Map.Entry<String, String> entry : dto.getLabelMap().entrySet()) {
                        String topLevelLabelName = entry.getKey();

                        Long labelId = labelCodeMap.get(entry.getValue()).getId();
                        Optional<LabelConfigDO> matchingLabelOpt = topLevelLabelNamesList.stream()
                                .filter(label -> label != null &&
                                        label.getLabelName() != null &&
                                        label.getLabelName().trim().equals(topLevelLabelName.trim())) // 比较 LabelName
                                .findFirst();
                        Long topLabelId = matchingLabelOpt.get().getId();

                        String labelFullPathId = getLabelFullIdPathById(labelId, labelIdMap);
                        if (labelFullPathId == null) {
                            continue;
                        }
                        StandingbookLabelInfoDO label = new StandingbookLabelInfoDO();
                        label.setStandingbookId(sbId);
                        label.setName(ATTR_LABEL_INFO_PREFIX + topLabelId);
                        label.setValue(labelFullPathId);
                        labels.add(label);
                    }
                }

            }

            // 批量入库
            standingbookMapper.insertBatch(standingbooks);
            standingbookAttributeMapper.insertBatch(attributes);
            standingbookLabelMapper.insertBatch(labels);
            if(ImportTemplateType.METER.equals(tmplEnum)) {
                doubleCarbonMappingMapper.insertBatch(doubleCarbonMappings);
            }

            // 4. 注册事务同步：事务提交后删除缓存（关键！避免事务回滚导致的不一致）
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 事务提交成功后，删除缓存
                    try {
                        byteArrayRedisTemplate.delete(STANDING_BOOK_EXCEL);
                        log.info("台账标签：数据入库成功，删除Redis缓存：{}", STANDING_BOOK_EXCEL);
                    } catch (Exception e) {
                        log.error("台账标签：删除Redis缓存失败，key: {}", STANDING_BOOK_EXCEL, e);
                        // 兜底：给缓存设置短期过期时间，避免长期脏数据
                        byteArrayRedisTemplate.expire(STANDING_BOOK_EXCEL, 5, TimeUnit.MINUTES);
                    }
                }
            });
            return standingbooks.size();
        } catch (Exception e) {
            log.error("台账标签：数据入库失败", e);
            // 入库失败，无需删除缓存（后续可重试入库）
            throw new RuntimeException("台账标签：批量入库失败", e); // 抛出异常触发事务回滚
        }

    }

    /**
     * 根据标签ID获取其层级全路径 (显示ID)
     *
     * @param startLabelId 要查找的起始标签ID
     * @param labelIdMap   所有标签ID到LabelConfigDO对象的Map
     * @return 标签的层级全路径 (例如: "2,4,6")，如果找不到或者标签不存在则返回null
     */
    public static String getLabelFullIdPathById(Long startLabelId, Map<Long, LabelConfigDO> labelIdMap) {
        if (startLabelId == null || labelIdMap == null || labelIdMap.isEmpty()) {
            return null;
        }

        LabelConfigDO currentLabel = labelIdMap.get(startLabelId);

        // 存储路径中的标签ID
        List<Long> pathIds = new ArrayList<>();

        // 追溯父级，直到找到顶级标签 (parentId is null)
        while (currentLabel != null) {
            // 如果没有父级了，就结束循环
            if (currentLabel.getParentId() == null || currentLabel.getParentId() == 0) {
                break;
            }
            // 将当前标签的 ID 添加到列表头部（或者先添加到尾部，最后反转）
            pathIds.add(currentLabel.getId()); // 使用 getId()
            // 获取下一个父级标签
            currentLabel = labelIdMap.get(currentLabel.getParentId());
        }

        // 如果 pathIds 为空，说明起始标签不存在或出现异常
        if (pathIds.isEmpty()) {
            return null;
        }

        // 反转列表，因为我们是从子到父追溯的，路径需要从父到子
        Collections.reverse(pathIds);

        // 将 ID 列表转换为字符串列表，然后拼接
        return pathIds.stream()
                .map(String::valueOf) // 将 Long ID 转换为 String
                .collect(Collectors.joining(StringPool.COMMA)); // 使用 "/" 作为分隔符拼接
    }


}