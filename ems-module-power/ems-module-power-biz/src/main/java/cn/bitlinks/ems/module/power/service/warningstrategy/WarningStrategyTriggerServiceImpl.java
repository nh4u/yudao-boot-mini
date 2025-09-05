package cn.bitlinks.ems.module.power.service.warningstrategy;

import cn.bitlinks.ems.framework.common.enums.CommonStatusEnum;
import cn.bitlinks.ems.framework.dict.core.DictFrameworkUtils;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.CollectRawDataApi;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.CollectRawDataDTO;
import cn.bitlinks.ems.module.infra.api.config.ConfigApi;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookDTO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.warninginfo.WarningInfoDO;
import cn.bitlinks.ems.module.power.dal.dataobject.warninginfo.WarningInfoUserDO;
import cn.bitlinks.ems.module.power.dal.dataobject.warningstrategy.WarningStrategyConditionDO;
import cn.bitlinks.ems.module.power.dal.dataobject.warningstrategy.WarningStrategyDO;
import cn.bitlinks.ems.module.power.dal.dataobject.warningtemplate.WarningTemplateDO;
import cn.bitlinks.ems.module.power.dal.mysql.warninginfo.WarningInfoMapper;
import cn.bitlinks.ems.module.power.dal.mysql.warninginfo.WarningInfoUserMapper;
import cn.bitlinks.ems.module.power.dal.mysql.warningstrategy.WarningStrategyConditionMapper;
import cn.bitlinks.ems.module.power.dal.mysql.warningstrategy.WarningStrategyMapper;
import cn.bitlinks.ems.module.power.dal.mysql.warningtemplate.WarningTemplateMapper;
import cn.bitlinks.ems.module.power.enums.DictTypeConstants;
import cn.bitlinks.ems.module.power.enums.warninginfo.WarningStrategyConnectorEnum;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.bitlinks.ems.module.power.service.standingbook.attribute.StandingbookAttributeService;
import cn.bitlinks.ems.module.power.service.standingbook.tmpl.StandingbookTmplDaqAttrService;
import cn.bitlinks.ems.module.power.service.standingbook.type.StandingbookTypeService;
import cn.bitlinks.ems.module.power.service.warningtemplate.WarningTemplateService;
import cn.bitlinks.ems.module.system.api.mail.MailSendApi;
import cn.bitlinks.ems.module.system.api.mail.dto.MailSendSingleToUserCustomReqDTO;
import cn.bitlinks.ems.module.system.api.user.AdminUserApi;
import cn.bitlinks.ems.module.system.api.user.dto.AdminUserRespDTO;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.module.power.enums.warninginfo.WarningStrategyConnectorEnum.evaluateCondition;
import static cn.bitlinks.ems.module.power.enums.warninginfo.WarningTemplateKeyWordEnum.*;
import static cn.hutool.core.date.DatePattern.NORM_DATETIME_FORMATTER;

/**
 * 告警策略触发告警 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
@Slf4j
public class WarningStrategyTriggerServiceImpl implements WarningStrategyTriggerService {
    static final String INIT_DEVICE_LINK = "power.device.monitor.url";

    @Resource
    private WarningStrategyConditionMapper warningStrategyConditionMapper;
    @Resource
    private WarningInfoMapper warningInfoMapper;
    @Resource
    private WarningInfoUserMapper warningInfoUserMapper;
    @Resource
    private WarningStrategyMapper warningStrategyMapper;
    @Resource
    private WarningTemplateMapper warningTemplateMapper;
    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private WarningTemplateService warningTemplateService;

    @Resource
    @Lazy
    private MailSendApi mailSendApi;
    @Resource
    private StandingbookTypeService standingbookTypeService;
    @Resource
    private StandingbookService standingbookService;
    @Resource
    private StandingbookAttributeService standingbookAttributeService;
    @Resource
    private StandingbookTmplDaqAttrService standingbookTmplDaqAttrService;
    @Resource
    private CollectRawDataApi collectRawDataApi;

    @Resource
    private ConfigApi configApi;
    private final static String customConnector = "->";

    @Override
    @Transactional
    public void triggerWarning(List<WarningStrategyDO> warningStrategyDOS, LocalDateTime triggerTime) {
        // 查询范围里包含的所有台账分类ids + 缓存
        List<StandingbookTypeDO> typeTreeList = standingbookTypeService.getStandingbookTypeNode();
        List<StandingbookTypeDO> allTypeList = standingbookTypeService.getStandingbookTypeList();
        Map<Long, String> tyepIdNameMap;
        Map<Long, String> tyepIdTopTypeMap;

        if (CollUtil.isEmpty(allTypeList)) {
            tyepIdNameMap = new HashMap<>();
            tyepIdTopTypeMap = new HashMap<>();
        } else {
            // 映射为id -> name的Map
            tyepIdNameMap = allTypeList.stream()
                    .collect(Collectors.toMap(
                            StandingbookTypeDO::getId,  // key为id
                            StandingbookTypeDO::getName  // value为name
                    ));
            // 映射为id -> name的Map
            tyepIdTopTypeMap = allTypeList.stream()
                    .collect(Collectors.toMap(
                            StandingbookTypeDO::getId,  // key为id
                            StandingbookTypeDO::getTopType  // value为name
                    ));
        }
        // 获取所有台账的id-DTO 映射
        List<StandingbookDTO> list = standingbookService.getStandingbookDTOList();
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Map<Long, StandingbookDTO> standingbookDTOMap = list.stream().collect(Collectors.toMap(StandingbookDTO::getStandingbookId, Function.identity()));
        for (WarningStrategyDO warningStrategyDO : warningStrategyDOS) {
            try {
                // 触发告警的最新异常时间
                AtomicReference<LocalDateTime> maxTime = new AtomicReference<>(null);
                Long strategyId = warningStrategyDO.getId();
                // 如果该策略的状态是关闭
                if (CommonStatusEnum.DISABLE.getStatus().equals(warningStrategyDO.getStatus())) {
                    continue;
                }
                // 获取该策略对应的设备和设备分类下的所有设备
                List<Long> deviceScopeIds = warningStrategyDO.getDeviceScope();
                if (CollUtil.isEmpty(deviceScopeIds)) {
                    deviceScopeIds = new ArrayList<>();
                }
                // 3. 查询策略中需要满足的所有条件
                List<WarningStrategyConditionDO> conditionVOS =
                        warningStrategyConditionMapper.selectList(WarningStrategyConditionDO::getStrategyId, strategyId);
                if (CollUtil.isEmpty(conditionVOS)) {
                    continue;
                }
                // 关联设备的需要条件中存在的所有设备
                if (CollUtil.isNotEmpty(deviceScopeIds)) {
                    deviceScopeIds.addAll(findConditionSbIds(conditionVOS));
                }

                // 1.1 查询台账分类范围下的所有设备id
                List<Long> typeIds = warningStrategyDO.getDeviceTypeScope();
                // 1.1.1 获取台账分类下所有的台账ids
                Map<Long, List<Long>> typeIdToAllStandingbookIdsMap = new HashMap<>();
                if (CollUtil.isNotEmpty(typeIds)) {
                    Map<Long, List<Long>> cascadeTypeIdMap = new HashMap<>();

                    List<Long> typeIdSet = new ArrayList<>();
                    for (Long typeId : typeIds) {
                        List<Long> subtreeIds = standingbookTypeService.getSubtreeIds(typeTreeList, typeId);
                        typeIdSet.addAll(subtreeIds);
                        cascadeTypeIdMap.put(typeId, subtreeIds);
                    }
                    // 查询范围内所有台账分类下的台账
                    List<StandingbookDO> standingbookDOS = standingbookService.getByTypeIds(typeIdSet);

                    if (CollUtil.isNotEmpty(standingbookDOS)) {

                        deviceScopeIds.addAll(standingbookDOS.stream().map(StandingbookDO::getId).collect(Collectors.toList()));
                        // 按照typeId分组
                        Map<Long, List<Long>> typeIdToStandingbookIds = standingbookDOS.stream()
                                .collect(Collectors.groupingBy(
                                        StandingbookDO::getTypeId,
                                        Collectors.mapping(
                                                StandingbookDO::getId,
                                                Collectors.toList()
                                        )
                                ));
                        // 合并 cascadeTypeIdMap 和 typeIdToStandingbookIds
                        typeIdToAllStandingbookIdsMap = cascadeTypeIdMap.entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> entry.getValue().stream()
                                                .filter(typeIdToStandingbookIds::containsKey)
                                                .flatMap(subTypeId -> typeIdToStandingbookIds.getOrDefault(subTypeId, Collections.emptyList()).stream())
                                                .distinct()
                                                .collect(Collectors.toList()),
                                        (existing, replacement) -> existing
                                ));
                    }
                }
                if (CollUtil.isEmpty(deviceScopeIds)) {
                    continue;
                }
                // 2. 查询涉及到的所有的设备参数的实时数据
                List<CollectRawDataDTO> collectRawDataDTOList =
                        collectRawDataApi.getCollectRawDataListByStandingBookIds(deviceScopeIds).getData();

                if (CollUtil.isEmpty(collectRawDataDTOList)) {
                    continue;
                }
                // 2.0 按照 台账id-> code#energyFlag->CollectRawDataDTO 的格式进行分组
                Map<Long, Map<String, CollectRawDataDTO>> collectRawDataMap = collectRawDataDTOList.stream()
                        .collect(Collectors.groupingBy(
                                CollectRawDataDTO::getStandingbookId, // Outer key: standingbookId as String
                                Collectors.toMap(
                                        dto -> dto.getParamCode() + StringPool.HASH + dto.getEnergyFlag(), // Inner key:
                                        // paramCode#energyFlag
                                        dto -> dto, // Value: CollectRawDataDTO
                                        (existing, replacement) -> existing // Merge function: keep existing if duplicate
                                )
                        ));
                // 2.1 查询 台账id->台账数采属性
                Map<Long, List<StandingbookTmplDaqAttrDO>> daqAttrsMap =
                        standingbookTmplDaqAttrService.getDaqAttrsBySbIds(deviceScopeIds);

                // 2.2 查询 台账分类id-> 台账分类


                // 4.!!!!根据实时数据判断条件是否全都被满足
                Map<WarningStrategyConditionDO, List<CollectRawDataDTO>> matchCollectRawDataMap =
                        filterMatchCollectRawDataDTO(conditionVOS,
                                collectRawDataMap, typeIdToAllStandingbookIdsMap);
                //无实时数据可满足该策略的所有条件
                if (CollUtil.isEmpty(matchCollectRawDataMap)) {
                    log.info("该策略 id {},未满足条件", strategyId);
                    continue;
                }
                // 5.组织关键字结构信息,准备发送告警信息/邮件
                List<String> deviceRelList = new ArrayList<>();
                // 补充告警信息内容（多行部分）
                List<Map<String, String>> conditionParamsMapList = new ArrayList<>();
                matchCollectRawDataMap.forEach((conditionVO, collectRawDataDTOS) -> {
                    List<String> paramIds = conditionVO.getParamId();
                    // 获取设备id or 分类id
                    String conditionSbOrTypeId = paramIds.get(paramIds.size() - 2);
                    // 获取参数编码
                    String conditionCodeAndEnergyFlag = paramIds.get(paramIds.size() - 1);
                    boolean deviceFlag = conditionVO.getDeviceFlag();
                    collectRawDataDTOS.forEach(collectRawDataDTO -> {
                        // 条件符合，填充所有关键字参数
                        Map<String, String> conditionParamsMap = new HashMap<>();
                        conditionParamsMap.put(WARNING_TIME.getKeyWord(), triggerTime.format(NORM_DATETIME_FORMATTER));
                        conditionParamsMap.put(WARNING_LEVEL.getKeyWord(), DictFrameworkUtils.getDictDataLabel(DictTypeConstants.WARNING_INFO_LEVEL, warningStrategyDO.getLevel()));
                        conditionParamsMap.put(WARNING_EXCEPTION_TIME.getKeyWord(),
                                collectRawDataDTO.getCollectTime().format(NORM_DATETIME_FORMATTER));
                        conditionParamsMap.put(WARNING_VALUE.getKeyWord(), collectRawDataDTO.getCalcValue());

                        // 补充参数相关数据
                        List<StandingbookTmplDaqAttrDO> standingbookTmplDaqAttrDOS =
                                daqAttrsMap.get(collectRawDataDTO.getStandingbookId());
                        Optional<StandingbookTmplDaqAttrDO> paramOptional = standingbookTmplDaqAttrDOS.stream()
                                .filter(attribute -> conditionCodeAndEnergyFlag.equals(attribute.getCode() + StringPool.HASH + attribute.getEnergyFlag()))
                                .findFirst();

                        String paramName =
                                paramOptional.map(StandingbookTmplDaqAttrDO::getParameter).orElse(StringPool.EMPTY);
                        String unit =
                                paramOptional.map(StandingbookTmplDaqAttrDO::getUnit).orElse(StringPool.EMPTY);
                        conditionParamsMap.put(WARNING_PARAM.getKeyWord(), paramName);
                        conditionParamsMap.put(WARNING_UNIT.getKeyWord(), unit);
                        conditionParamsMap.put(WARNING_CONDITION_VALUE.getKeyWord(), conditionVO.getValue());

                        if (deviceFlag) {
                            conditionParamsMap.put(WARNING_DEVICE_TYPE.getKeyWord(), StringPool.SLASH);
                        } else {
                            conditionParamsMap.put(WARNING_DEVICE_TYPE.getKeyWord(), tyepIdNameMap.get(Long.valueOf(conditionSbOrTypeId)));
                        }


                        StandingbookDTO standingbookDTO = standingbookDTOMap.get(collectRawDataDTO.getStandingbookId());
                        String sbName = Objects.isNull(standingbookDTO) ? StringPool.EMPTY : standingbookDTO.getCode();
                        String sbCode = Objects.isNull(standingbookDTO) ? StringPool.EMPTY : standingbookDTO.getCode();

                        if (deviceFlag) {
                            // 如果是设备选的需要展示全链路
                            conditionParamsMap.put(WARNING_DEVICE_NAME.getKeyWord(), allLinkName(conditionVO.getParamId(), standingbookDTOMap, paramName));
                        } else {
                            conditionParamsMap.put(WARNING_DEVICE_NAME.getKeyWord(), sbName);
                        }
                        // 条件的设备名称不对，需要连续起来，其中可能有分类有台账
                        conditionParamsMap.put(WARNING_DEVICE_CODE.getKeyWord(), sbCode);
                        conditionParamsMap.put(WARNING_STRATEGY_NAME.getKeyWord(), warningStrategyDO.getName());
                        // 获取api连接配置
                        String initLink = configApi.getConfigValueByKey(INIT_DEVICE_LINK).getCheckedData();
                        conditionParamsMap.put(WARNING_DETAIL_LINK.getKeyWord(), String.format(initLink,
                                collectRawDataDTO.getStandingbookId(), tyepIdTopTypeMap.get(standingbookDTO.getTypeId())));
                        conditionParamsMapList.add(conditionParamsMap);
                        String deviceRel = String.format("%s(%s)", sbName, sbCode);
                        deviceRelList.add(deviceRel);
                        // 处理最大时间
                        LocalDateTime curTime = collectRawDataDTO.getCollectTime();
                        // 更新最大时间
                        maxTime.updateAndGet(prev -> (prev == null || curTime.isAfter(prev)) ? curTime : prev);

                    });
                });

                //------------------------------ 该策略的最新异常时间不满足的话直接return ------------------------
                if (warningStrategyDO.getLastExpTime() != null && !maxTime.get().isAfter(warningStrategyDO.getLastExpTime())) {
                    log.info("该策略 id {},已触发过相同时间告警内容", strategyId);
                    continue;
                }


                //------------------------------（还差收件人、标题、内容未填充）------------------------
                // 一、发送站内信
                // 站内信模板通知人员
                List<Long> siteUserIds = warningStrategyDO.getSiteStaff();
                WarningTemplateDO siteTemplateDO = warningTemplateMapper.selectById(warningStrategyDO.getSiteTemplateId());
                //              //  (组装告警信息类)
                WarningInfoDO warningInfoDO = new WarningInfoDO();
                warningInfoDO.setLevel(warningStrategyDO.getLevel());
                warningInfoDO.setWarningTime(triggerTime);
                warningInfoDO.setTemplateId(warningStrategyDO.getSiteTemplateId());
                warningInfoDO.setStrategyId(strategyId);
                warningInfoDO.setDeviceRel(String.join(StringPool.COMMA, deviceRelList));
                sendSiteMsg(siteUserIds, siteTemplateDO, conditionParamsMapList, warningInfoDO);

                // 二、如果有邮件模板，发送邮件
                //邮件模板通知人员
                List<Long> mailUserIds = warningStrategyDO.getMailStaff();
                WarningTemplateDO mailTemplateDO = warningTemplateMapper.selectById(warningStrategyDO.getMailTemplateId());
                if (mailTemplateDO != null) {
                    sendSiteMsg(mailUserIds, mailTemplateDO, conditionParamsMapList, null);
                }
                // 更新告警规则的最新异常时间
                warningStrategyMapper.update(new LambdaUpdateWrapper<WarningStrategyDO>().set(WarningStrategyDO::getLastExpTime, triggerTime).eq(WarningStrategyDO::getId, strategyId));
            } catch (Exception e) {
                log.error("该策略 id {} ,告警触发异常", warningStrategyDO.getId(), e);
            }
        }

    }

    private List<Long> findConditionSbIds(List<WarningStrategyConditionDO> conditionVOS) {
        if (CollUtil.isEmpty(conditionVOS)) {
            return Collections.emptyList();
        }
        List<Long> sbIds = new ArrayList<>();
        for (WarningStrategyConditionDO warningStrategyConditionDO : conditionVOS) {
            List<String> paramIds = warningStrategyConditionDO.getParamId();
            String sbId = paramIds.get(paramIds.size() - 2);
            sbIds.add(Long.parseLong(sbId));
        }
        return sbIds;
    }

    /**
     * 拼接台账名称 + 参数名
     *
     * @param paramId            参数ID列表，前n-1是台账ID，最后一个是原始参数（不处理）
     * @param standingbookDTOMap 台账ID到DTO的映射
     * @param paramName          要拼接的参数名称
     * @return 拼接字符串，如："冷水流量计 > 热水流量计 > 瞬时流量"
     */
    private String allLinkName(List<String> paramId, Map<Long, StandingbookDTO> standingbookDTOMap, String paramName) {
        if (CollUtil.isEmpty(paramId)) {
            return StringPool.EMPTY;
        }

        List<String> nameParts = new ArrayList<>();
        int lastIndex = paramId.size() - 1;

        for (int i = 0; i < lastIndex; i++) {
            String rawId = paramId.get(i);
            try {
                Long id = Long.valueOf(rawId);
                StandingbookDTO dto = standingbookDTOMap.get(id);
                if (dto != null && StringUtils.isNotBlank(dto.getName())) {
                    nameParts.add(dto.getName());
                }
            } catch (NumberFormatException e) {
                log.error("台账ID格式错误: {}", rawId, e);
            }
        }

        // 拼上参数名（最后一位）
        nameParts.add(paramName);

        return String.join(customConnector, nameParts);
    }

    /**
     * 获取满足条件的实时数据
     *
     * @param conditionVOS
     * @param collectRawDataMap
     * @param typeIdToAllStandingbookIdsMap
     * @return
     */
    private Map<WarningStrategyConditionDO, List<CollectRawDataDTO>> filterMatchCollectRawDataDTO(List<WarningStrategyConditionDO> conditionVOS, Map<Long,
            Map<String, CollectRawDataDTO>> collectRawDataMap, Map<Long, List<Long>> typeIdToAllStandingbookIdsMap) {
        Map<WarningStrategyConditionDO, List<CollectRawDataDTO>> result = new HashMap<>();

        for (WarningStrategyConditionDO conditionVO : conditionVOS) {
            List<String> paramIds = conditionVO.getParamId();
            // 获取设备id or 分类id
            String conditionSbOrTypeId = paramIds.get(paramIds.size() - 2);

            boolean deviceFlag = conditionVO.getDeviceFlag();
            // 是设备策略
            if (deviceFlag) {
                // 查询实时数据
                CollectRawDataDTO collectRawDataDTO = filterMatchCollectRawDataDTOByStandingbookId(conditionVO,
                        Long.valueOf(conditionSbOrTypeId),
                        collectRawDataMap);
                // 该条件不被满足
                if (Objects.isNull(collectRawDataDTO)) {
                    return null;
                }
                result.put(conditionVO, Collections.singletonList(collectRawDataDTO));
            } else {
                // 是分类策略
                List<Long> standingbookIds = typeIdToAllStandingbookIdsMap.get(Long.valueOf(conditionSbOrTypeId));
                // 分类下无设备,无设备可以满足该条件
                if (CollUtil.isEmpty(standingbookIds)) {
                    return null;
                }
                List<CollectRawDataDTO> collectRawDataDTOS = new ArrayList<>();
                standingbookIds.forEach(standingbookId -> {
                    CollectRawDataDTO collectRawDataDTO = filterMatchCollectRawDataDTOByStandingbookId(conditionVO,
                            standingbookId,
                            collectRawDataMap);
                    if (Objects.nonNull(collectRawDataDTO)) {
                        collectRawDataDTOS.add(collectRawDataDTO);
                    }

                });
                // 该条件不被满足,分类下无设备可满足
                if (CollUtil.isEmpty(collectRawDataDTOS)) {
                    return null;
                }
                result.put(conditionVO, collectRawDataDTOS);
            }
        }
        return result;
    }

    /**
     * 查询匹配条件的的实时数据
     *
     * @param conditionVO
     * @param standingbookId
     * @param collectRawDataMap
     * @return
     */
    private CollectRawDataDTO filterMatchCollectRawDataDTOByStandingbookId(WarningStrategyConditionDO conditionVO,
                                                                           Long standingbookId, Map<Long, Map<String,
                    CollectRawDataDTO>> collectRawDataMap) {
        List<String> paramIds = conditionVO.getParamId();
        // 获取参数编码
        String conditionCodeAndEnergyFlag = paramIds.get(paramIds.size() - 1);

        // 查询实时数据
        Map<String, CollectRawDataDTO> paramValueMap = collectRawDataMap.get(Long.valueOf(standingbookId));
        // 无实时数据
        if (CollUtil.isEmpty(paramValueMap)) {
            return null;
        }
        // 无该参数实时数据
        if (!paramValueMap.containsKey(conditionCodeAndEnergyFlag)) {
            // 该条件对应的参数编码不存在，跳出此策略
            return null;
        }
        // 得到实时数据, 根据实时数据进行判断
        CollectRawDataDTO collectRawDataDTO = paramValueMap.get(conditionCodeAndEnergyFlag);
        // 组合条件进行判断
        boolean isMatch =
                evaluateCondition(WarningStrategyConnectorEnum.codeOf(conditionVO.getConnector()),
                        conditionVO.getValue(), collectRawDataDTO.getCalcValue());
        if (!isMatch) {
            return null;
        }
        return collectRawDataDTO;
    }

    /**
     * 针对每条策略对每位收件人发送邮件/告警信息（无能力优化）
     *
     * @param userIds                收件人们
     * @param templateDO             告警模板
     * @param conditionParamsMapList 关键字实际填充的参数
     * @param warningInfoDO          告警信息(部分填充)
     */
    private void sendSiteMsg(List<Long> userIds, WarningTemplateDO templateDO, List<Map<String, String>> conditionParamsMapList, WarningInfoDO warningInfoDO) {
        try {
            if (CollUtil.isEmpty(userIds)) {
                return;
            }
            Map<Long, AdminUserRespDTO> mailUserMap = adminUserApi.getUserMap(userIds);
            List<WarningInfoUserDO> warningInfoUserList = new ArrayList<>();

            // 邮件的话 ，加收件人信息填充
            if (warningInfoDO == null) {
                userIds.forEach(userId -> {
                    AdminUserRespDTO adminUserRespDTO = mailUserMap.get(userId);
                    String userName = adminUserRespDTO.getNickname();
                    // 处理收件人关键字参数
                    List<Map<String, String>> newConditionParamsMapList = new ArrayList<>(conditionParamsMapList);
                    newConditionParamsMapList.forEach(conditionParamsMap -> conditionParamsMap.put(WARNING_USER_NAME.getKeyWord(), userName));
                    // 每个用户的告警信息构造标题和内容
                    String userTitle = warningTemplateService.buildTitleOrContentByParams(templateDO.getTParams(), templateDO.getTitle(), conditionParamsMapList);
                    if (Objects.isNull(userTitle)) {
                        // 模板填充参数失败，此策略不进行告警，直接完全退出
                        log.error("告警模板id{}，解析模板标题失败，告警信息解析异常", templateDO.getId());
                        return;
                    }
                    String userContent = warningTemplateService.buildTitleOrContentByParams(templateDO.getParams(), templateDO.getContent(), conditionParamsMapList);
                    if (Objects.isNull(userContent)) {
                        // 模板填充参数失败，此策略不进行告警，直接完全退出
                        log.error("告警模板id{}，解析模板内容失败，告警信息解析异常", templateDO.getId());
                        return;
                    }
                    // 发送告警邮件，组装邮件参数结构。
                    MailSendSingleToUserCustomReqDTO mailSendSingleToUserCustomReqDTO = new MailSendSingleToUserCustomReqDTO();
                    mailSendSingleToUserCustomReqDTO.setUserId(userId);
                    mailSendSingleToUserCustomReqDTO.setMail(adminUserRespDTO.getEmail());
                    mailSendSingleToUserCustomReqDTO.setTitle(userTitle);
                    mailSendSingleToUserCustomReqDTO.setContent(userContent);
                    mailSendSingleToUserCustomReqDTO.setTemplateCode(templateDO.getCode());
                    mailSendSingleToUserCustomReqDTO.setTemplateId(templateDO.getId());
                    mailSendSingleToUserCustomReqDTO.setTemplateName(templateDO.getName());
                    mailSendApi.sendSingleMailToAdminCustom(mailSendSingleToUserCustomReqDTO);
                });
            } else {
                // 站内信的话，不加发送收件人信息
                WarningInfoDO warningInfo = new WarningInfoDO();
                BeanUtil.copyProperties(warningInfoDO, warningInfo);
                Long infoId = IdUtil.getSnowflakeNextId();
                warningInfo.setId(infoId);
                String title = warningTemplateService.buildTitleOrContentByParams(templateDO.getTParams(), templateDO.getTitle(), conditionParamsMapList);
                String content = warningTemplateService.buildTitleOrContentByParams(templateDO.getParams(), templateDO.getContent(), conditionParamsMapList);
                warningInfo.setTitle(title);
                warningInfo.setContent(content);
                warningInfoMapper.insert(warningInfo);
                userIds.forEach(userId -> {
                    WarningInfoUserDO warningInfoUserDO = new WarningInfoUserDO();
                    warningInfoUserDO.setUserId(userId);
                    warningInfoUserDO.setInfoId(warningInfo.getId());
                    warningInfoUserList.add(warningInfoUserDO);
                });
                warningInfoUserMapper.insertBatch(warningInfoUserList);
            }


        } catch (Exception e) {
            log.error("告警模板id{},告警信息发送异常", templateDO.getId(), e);
        }
    }


}