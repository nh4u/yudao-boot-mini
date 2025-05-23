package cn.bitlinks.ems.module.power.service.warningstrategy;

import cn.bitlinks.ems.framework.common.enums.CommonStatusEnum;
import cn.bitlinks.ems.framework.dict.core.DictFrameworkUtils;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.CollectRawDataApi;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.CollectRawDataDTO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.type.vo.StandingbookTypeListReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.warninginfo.WarningInfoDO;
import cn.bitlinks.ems.module.power.dal.dataobject.warningstrategy.WarningStrategyConditionDO;
import cn.bitlinks.ems.module.power.dal.dataobject.warningstrategy.WarningStrategyDO;
import cn.bitlinks.ems.module.power.dal.dataobject.warningtemplate.WarningTemplateDO;
import cn.bitlinks.ems.module.power.dal.mysql.warninginfo.WarningInfoMapper;
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
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.module.power.enums.ApiConstants.*;
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

    @Resource
    private WarningStrategyMapper warningStrategyMapper;
    @Resource
    private WarningStrategyConditionMapper warningStrategyConditionMapper;
    @Resource
    private WarningInfoMapper warningInfoMapper;
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


    @Override
    @Transactional
    public void triggerWarning(Long strategyId, LocalDateTime triggerTime) {
        // 查询该策略
        WarningStrategyDO warningStrategyDO = warningStrategyMapper.selectById(strategyId);
        // 如果该策略的状态是关闭
        if (CommonStatusEnum.DISABLE.getStatus().equals(warningStrategyDO.getStatus())) {
            return;
        }
        // 获取该策略对应的设备和设备分类下的所有设备
        List<Long> deviceScopeIds = warningStrategyDO.getDeviceScope();

        // 1.1 查询台账分类范围下的所有设备id todo 循环递归问题, 待优化
        List<Long> typeIds = warningStrategyDO.getDeviceTypeScope();
        // 1.1.1 获取台账分类下所有的台账ids
        Map<Long, List<Long>> typeIdToAllStandingbookIdsMap = new HashMap<>();
        if (CollUtil.isNotEmpty(warningStrategyDO.getDeviceTypeScope())) {
            Map<Long, List<Long>> cascadeTypeIdMap = new HashMap<>();
            // 查询范围里包含的所有台账分类ids
            List<StandingbookTypeDO> typeList = standingbookTypeService.getStandingbookTypeNode();
            List<Long> typeIdSet = new ArrayList<>();
            for (Long typeId : typeIds) {
                List<Long> subtreeIds = standingbookTypeService.getSubtreeIds(typeList, typeId);
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
            return;
        }
        // 2. 查询涉及到的所有的设备参数的实时数据
        List<CollectRawDataDTO> collectRawDataDTOList =
                collectRawDataApi.getCollectRawDataListByStandingBookIds(deviceScopeIds).getData();

        if (CollUtil.isEmpty(collectRawDataDTOList)) {
            return;
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
        // 2.1 查询 台账id->台账基础属性
        Map<Long, List<StandingbookAttributeDO>> attrsMap = standingbookAttributeService.getAttributesBySbIds(deviceScopeIds);
        Map<Long, List<StandingbookTmplDaqAttrDO>> daqAttrsMap =
                standingbookTmplDaqAttrService.getDaqAttrsBySbIds(deviceScopeIds);
        // 2.2 查询 台账id-> 台账
        List<StandingbookDO> standingbookDOList = standingbookService.getByStandingbookIds(deviceScopeIds);
        Map<Long, StandingbookDO> standingbookDOMap = standingbookDOList.stream()
                .collect(Collectors.toMap(StandingbookDO::getId, item -> item));
        // 2.2 查询 台账分类id-> 台账分类
        List<StandingbookTypeDO> standingbookTypeList = standingbookTypeService.getStandingbookTypeList(new StandingbookTypeListReqVO());
        Map<Long, StandingbookTypeDO> standingbookTypeDOMap = standingbookTypeList.stream()
                .collect(Collectors.toMap(StandingbookTypeDO::getId, item -> item));
        // 3. 查询策略中需要满足的所有条件
        List<WarningStrategyConditionDO> conditionVOS =
                warningStrategyConditionMapper.selectList(WarningStrategyConditionDO::getStrategyId, strategyId);
        if (CollUtil.isEmpty(conditionVOS)) {
            return;
        }

        // 4.!!!!根据实时数据判断条件是否全都被满足
        Map<WarningStrategyConditionDO, List<CollectRawDataDTO>> matchCollectRawDataMap =
                filterMatchCollectRawDataDTO(conditionVOS,
                        collectRawDataMap, typeIdToAllStandingbookIdsMap);
        //无实时数据可满足该策略的所有条件
        if (CollUtil.isEmpty(matchCollectRawDataMap)) {
            log.info("该策略 id {},未满足条件", strategyId);
            return;
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
                    conditionParamsMap.put(WARNING_DEVICE_TYPE.getKeyWord(), StringPool.EMPTY);
                } else {
                    StandingbookTypeDO standingbookTypeDO =
                            standingbookTypeDOMap.get(Long.valueOf(conditionSbOrTypeId));
                    conditionParamsMap.put(WARNING_DEVICE_TYPE.getKeyWord(), standingbookTypeDO.getName());
                }

                List<StandingbookAttributeDO> standingbookAttributeDOS = attrsMap.get(collectRawDataDTO.getStandingbookId());
                Optional<StandingbookAttributeDO> measureNameOptional = standingbookAttributeDOS.stream()
                        .filter(attribute -> ATTR_MEASURING_INSTRUMENT_MAME.equals(attribute.getCode()))
                        .findFirst();
                Optional<StandingbookAttributeDO> measureIdOptional = standingbookAttributeDOS.stream()
                        .filter(attribute -> ATTR_MEASURING_INSTRUMENT_ID.equals(attribute.getCode()))
                        .findFirst();
                String sbName = measureNameOptional.map(StandingbookAttributeDO::getValue).orElse(StringPool.EMPTY);
                String sbCode = measureIdOptional.map(StandingbookAttributeDO::getValue).orElse(StringPool.EMPTY);
                conditionParamsMap.put(WARNING_DEVICE_NAME.getKeyWord(), sbName);
                conditionParamsMap.put(WARNING_DEVICE_CODE.getKeyWord(), sbCode);
                conditionParamsMap.put(WARNING_STRATEGY_NAME.getKeyWord(), warningStrategyDO.getName());
                conditionParamsMap.put(WARNING_DETAIL_LINK.getKeyWord(), String.format(SB_MONITOR_DETAIL,
                        collectRawDataDTO.getStandingbookId()));
                conditionParamsMapList.add(conditionParamsMap);
                String deviceRel = String.format("%s(%s)", sbName, sbCode);
                deviceRelList.add(deviceRel);
            });
        });


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
        warningInfoDO.setStrategyId(warningStrategyDO.getId());
        warningInfoDO.setDeviceRel(String.join(StringPool.COMMA, deviceRelList));
        sendSiteMsg(siteUserIds, siteTemplateDO, conditionParamsMapList, warningInfoDO);

        // 二、如果有邮件模板，发送邮件
        //邮件模板通知人员
        List<Long> mailUserIds = warningStrategyDO.getMailStaff();
        WarningTemplateDO mailTemplateDO = warningTemplateMapper.selectById(warningStrategyDO.getMailTemplateId());
        if (mailTemplateDO != null) {
            sendSiteMsg(mailUserIds, mailTemplateDO, conditionParamsMapList, null);
        }


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
            List<WarningInfoDO> warningInfoList = new ArrayList<>();
            userIds.forEach(userId -> {
                AdminUserRespDTO adminUserRespDTO = mailUserMap.get(userId);
                String userName = adminUserRespDTO.getNickname();
                // 处理收件人关键字参数
                List<Map<String, String>> newConditionParamsMapList = new ArrayList<>(conditionParamsMapList);
                newConditionParamsMapList.forEach(conditionParamsMap -> conditionParamsMap.put(WARNING_USER_NAME.getKeyWord(), userName));
                // 每个用户的告警信息构造标题和内容
                String title = warningTemplateService.buildTitleOrContentByParams(templateDO.getTParams(), templateDO.getTitle(), conditionParamsMapList);
                if (Objects.isNull(title)) {
                    // 模板填充参数失败，此策略不进行告警，直接完全退出
                    log.error("告警模板id{}，解析模板标题失败，告警信息解析异常", templateDO.getId());
                    return;
                }
                String content = warningTemplateService.buildTitleOrContentByParams(templateDO.getParams(), templateDO.getContent(), conditionParamsMapList);
                if (Objects.isNull(content)) {
                    // 模板填充参数失败，此策略不进行告警，直接完全退出
                    log.error("告警模板id{}，解析模板内容失败，告警信息解析异常", templateDO.getId());
                    return;
                }
                if (warningInfoDO == null) {
                    // 发送告警邮件，组装邮件参数结构。
                    MailSendSingleToUserCustomReqDTO mailSendSingleToUserCustomReqDTO = new MailSendSingleToUserCustomReqDTO();
                    mailSendSingleToUserCustomReqDTO.setUserId(userId);
                    mailSendSingleToUserCustomReqDTO.setMail(adminUserRespDTO.getEmail());
                    mailSendSingleToUserCustomReqDTO.setTitle(title);
                    mailSendSingleToUserCustomReqDTO.setContent(content);
                    mailSendSingleToUserCustomReqDTO.setTemplateCode(templateDO.getCode());

                    mailSendSingleToUserCustomReqDTO.setTemplateId(templateDO.getId());
                    mailSendSingleToUserCustomReqDTO.setTemplateName(templateDO.getName());
                    mailSendApi.sendSingleMailToAdminCustom(mailSendSingleToUserCustomReqDTO);
                } else {
                    // 发送告警信息，组装告警信息参数结构
                    WarningInfoDO warningInfo = new WarningInfoDO();
                    BeanUtil.copyProperties(warningInfoDO, warningInfo);
                    warningInfo.setUserId(userId);
                    warningInfo.setTitle(title);
                    warningInfo.setContent(content);
                    warningInfoList.add(warningInfo);
                }
            });
            if (CollUtil.isEmpty(warningInfoList)) {
                return;
            }
             warningInfoMapper.insertBatch(warningInfoList);
        } catch (Exception e) {
            log.error("告警模板id{},告警信息发送异常", templateDO.getId(), e);
        }
    }


}