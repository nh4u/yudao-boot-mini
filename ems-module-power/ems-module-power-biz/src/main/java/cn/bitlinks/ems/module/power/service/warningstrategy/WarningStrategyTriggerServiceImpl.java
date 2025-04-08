package cn.bitlinks.ems.module.power.service.warningstrategy;

import cn.bitlinks.ems.framework.common.enums.CommonStatusEnum;
import cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo.SbDataTriggerVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.warninginfo.WarningInfoDO;
import cn.bitlinks.ems.module.power.dal.dataobject.warningstrategy.WarningStrategyConditionDO;
import cn.bitlinks.ems.module.power.dal.dataobject.warningstrategy.WarningStrategyDO;
import cn.bitlinks.ems.module.power.dal.dataobject.warningtemplate.WarningTemplateDO;
import cn.bitlinks.ems.module.power.dal.mysql.warninginfo.WarningInfoMapper;
import cn.bitlinks.ems.module.power.dal.mysql.warningstrategy.WarningStrategyConditionMapper;
import cn.bitlinks.ems.module.power.dal.mysql.warningstrategy.WarningStrategyMapper;
import cn.bitlinks.ems.module.power.dal.mysql.warningtemplate.WarningTemplateMapper;
import cn.bitlinks.ems.module.power.enums.warninginfo.WarningIntervalUnitEnum;
import cn.bitlinks.ems.module.power.enums.warninginfo.WarningStrategyConnectorEnum;
import cn.bitlinks.ems.module.power.service.standingbook.attribute.StandingbookAttributeService;
import cn.bitlinks.ems.module.power.service.warningtemplate.WarningTemplateService;
import cn.bitlinks.ems.module.system.api.mail.MailSendApi;
import cn.bitlinks.ems.module.system.api.mail.dto.MailSendSingleToUserCustomReqDTO;
import cn.bitlinks.ems.module.system.api.user.AdminUserApi;
import cn.bitlinks.ems.module.system.api.user.dto.AdminUserRespDTO;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.module.power.enums.ApiConstants.*;
import static cn.bitlinks.ems.module.power.enums.warninginfo.WarningIntervalUnitEnum.calculateThresholdTime;
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

    //    @Resource
//    private EnergyConfigurationService energyConfigurationService;

    @Resource
    @Lazy
    private MailSendApi mailSendApi;

    @Resource
    private StandingbookAttributeService standingbookAttributeService;
    private static final Integer batchSize = 2000;


    @Override
    public void triggerWarning(List<SbDataTriggerVO> sbDataTriggerVOList) {
        LocalDateTime triggerTime = LocalDateTime.now();
        // todo 只匹配条件中完全对应的实体设备, 等待虚拟设备逻辑完善。
        if (CollUtil.isEmpty(sbDataTriggerVOList)) {
            return;
        }
        /* ---------------------------- 实体设备触发告警 ---------------------------------------- */
        // 获取设备编码和设备参数
        Map<String, List<SbDataTriggerVO>> codeParamMap = sbDataTriggerVOList.stream()
                .collect(Collectors.groupingBy(SbDataTriggerVO::getSbCode));
        // 获取对应台账信息-所有的设备id
        Map<Long, List<StandingbookAttributeDO>> attrsMap = standingbookAttributeService.getSbAttrBySbCode(new ArrayList<>(codeParamMap.keySet()));

        // 查询所有能源 todo 等能源设置好之后，再填充能源参数相关的问题。
//        List<EnergyConfigurationDO> energyConfigurationDOS = energyConfigurationService.getAllEnergyConfiguration(null);
//        Map<Long, List<WarningStrategyConditionDO>> energyConfigurationMap = energyConfigurationDOS.stream()
//                .collect(Collectors.groupingBy(EnergyConfigurationDO::get));


        // 1.获取未触发的告警策略
        List<WarningStrategyDO> warningStrategyDOList = warningStrategyMapper.selectList(new LambdaQueryWrapper<WarningStrategyDO>()
                .eq(WarningStrategyDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                .eq(WarningStrategyDO::getDeleted, CommonStatusEnum.ENABLE.getStatus())
        );
        if (CollUtil.isEmpty(warningStrategyDOList)) {
            return;
        }
        List<Long> strategyIds = warningStrategyDOList.stream().map(WarningStrategyDO::getId).collect(Collectors.toList());

        List<WarningStrategyConditionDO> conditionDOS = warningStrategyConditionMapper.selectList(new LambdaQueryWrapper<WarningStrategyConditionDO>()
                .in(WarningStrategyConditionDO::getStrategyId, strategyIds)
        );
        Map<Long, List<WarningStrategyConditionDO>> strategyConditionMap = conditionDOS.stream()
                .collect(Collectors.groupingBy(WarningStrategyConditionDO::getStrategyId));
        // 获取所有模板信息
        List<Long> siteTemplateId = warningStrategyDOList.stream().map(WarningStrategyDO::getSiteTemplateId).collect(Collectors.toList());
        if (CollUtil.isEmpty(siteTemplateId)) {
            return;
        }
        List<Long> mailTemplateId = warningStrategyDOList.stream().map(WarningStrategyDO::getMailTemplateId).collect(Collectors.toList());
        siteTemplateId.addAll(mailTemplateId);
        List<WarningTemplateDO> templateDOS = warningTemplateMapper.selectList(new LambdaQueryWrapper<WarningTemplateDO>()
                .in(WarningTemplateDO::getId, siteTemplateId));
        if (CollUtil.isEmpty(templateDOS)) {
            return;
        }
        Map<Long, WarningTemplateDO> templatesMap = templateDOS.stream()
                .collect(Collectors.toMap(WarningTemplateDO::getId, template -> template));
        // 1.1 策略分批处理
        List<List<WarningStrategyDO>> batches = Lists.partition(warningStrategyDOList, batchSize);
        // 1.2 获取告警信息中策略触发最新时间。
        Map<Long, LocalDateTime> strategyTimeMap = warningInfoMapper.selectLatestByStrategy();
        for (List<WarningStrategyDO> batchStrategy : batches) {
            batchStrategy.forEach(warningStrategyDO -> {

                // 1）每条策略，检查时间间隔是否触发过
                LocalDateTime latestTime = strategyTimeMap.get(warningStrategyDO.getId());
                boolean isTrigger = checkStrategyTrigger(warningStrategyDO.getInterval(), warningStrategyDO.getIntervalUnit(), latestTime, triggerTime);
                // 时间间隔内触发过了，不必考虑此策略
                if (isTrigger) {
                    return;
                }
                // 3）循环策略中每个条件，检查设备、参数是否包含，如果包含，根据关键字生成告警信息
                List<WarningStrategyConditionDO> conditionVOS = strategyConditionMap.get(warningStrategyDO.getId());
                if (CollUtil.isEmpty(conditionVOS)) {
                    return;
                }

                List<String> deviceRelList = new ArrayList<>();

                // 检查告警策略是否满足条件，补充告警信息内容（多行部分）
                List<Map<String, String>> conditionParamsMapList = new ArrayList<>();
                for (WarningStrategyConditionDO conditionVO : conditionVOS) {
                    try {
                        List<String> paramIds = conditionVO.getParamId();
                        // 获取参数编码
                        String conditionCode = paramIds.get(paramIds.size() - 1);
                        // 获取设备id
                        String conditionSbId = paramIds.get(paramIds.size() - 2);
                        List<SbDataTriggerVO> sbDataTriggerVOS = codeParamMap.get(conditionSbId);
                        if (CollUtil.isEmpty(sbDataTriggerVOS)) {
                            // 设备id不存在，跳出此策略
                            return;
                        }
                        // 查找匹配的参数编码
                        Optional<SbDataTriggerVO> foundParamData = sbDataTriggerVOS.stream()
                                .filter(vo -> vo.getParamCode().equals(conditionCode))
                                .findFirst();
                        if (!foundParamData.isPresent()) {
                            // 未找到匹配的参数编码，跳出此策略
                            return;
                        }
                        SbDataTriggerVO sbDataTriggerVO = foundParamData.get();
                        // 组合条件进行判断
                        boolean isMatch = evaluateCondition(WarningStrategyConnectorEnum.codeOf(conditionVO.getConnector()), conditionVO.getValue(), sbDataTriggerVO.getValue());
                        if (!isMatch) {
                            return;
                        }
                        // 条件符合，填充所有关键字参数
                        Map<String, String> conditionParamsMap = new HashMap<>();
                        conditionParamsMap.put(WARNING_TIME.getKeyWord(), triggerTime.format(NORM_DATETIME_FORMATTER));
                        conditionParamsMap.put(WARNING_LEVEL.getKeyWord(), warningStrategyDO.getLevel() + "");
                        conditionParamsMap.put(WARNING_EXCEPTION_TIME.getKeyWord(), sbDataTriggerVO.getDataTime().format(NORM_DATETIME_FORMATTER));
                        conditionParamsMap.put(WARNING_VALUE.getKeyWord(), sbDataTriggerVO.getValue());


                        conditionParamsMap.put(WARNING_PARAM.getKeyWord(), sbDataTriggerVO.getParamCode());
                        // todo 等能源参数补充好之后，再进行填充的修改
                        conditionParamsMap.put(WARNING_UNIT.getKeyWord(), StringPool.EMPTY);
                        conditionParamsMap.put(WARNING_CONDITION_VALUE.getKeyWord(), conditionVO.getValue());

                        // todo 设备分类关键字不做处理,暂时填充空串
                        conditionParamsMap.put(WARNING_DEVICE_TYPE.getKeyWord(), StringPool.EMPTY);

                        List<StandingbookAttributeDO> standingbookAttributeDOS = attrsMap.get(Long.valueOf(conditionSbId));
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
                        conditionParamsMap.put(WARNING_DETAIL_LINK.getKeyWord(), String.format(SB_MONITOR_DETAIL, conditionSbId));
                        conditionParamsMapList.add(conditionParamsMap);
                        String deviceRel = String.format("%s(%s)", sbName, sbCode);
                        deviceRelList.add(deviceRel);
                    } catch (Exception e) {
                        log.error("告警策略id{}条件解析异常", conditionVO.getId(), e);
                        return;
                    }
                }
                if (CollUtil.isEmpty(conditionParamsMapList)) {
                    return;
                }
                if (CollUtil.isEmpty(deviceRelList)) {
                    return;
                }
                //------------------------------（还差收件人、标题、内容未填充）------------------------
                // 一、发送站内信
                //站内信模板通知人员
                List<Long> siteUserIds = warningStrategyDO.getSiteStaff();
                WarningTemplateDO siteTemplateDO = templatesMap.get(warningStrategyDO.getSiteTemplateId());
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
                WarningTemplateDO mailTemplateDO = templatesMap.get(warningStrategyDO.getMailTemplateId());
                if (mailTemplateDO != null) {
                    sendSiteMsg(mailUserIds, mailTemplateDO, conditionParamsMapList, null);
                }

            });
        }

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


    /**
     * 判断告警间隔内是否触发过
     *
     * @param interval     告警间隔
     * @param intervalUnit 告警间隔时间单位
     * @param latestTime   上次触发时间
     * @param triggerTime  本次触发时间
     * @return 是否触发过
     */
    private boolean checkStrategyTrigger(String interval, Integer intervalUnit, LocalDateTime latestTime, LocalDateTime triggerTime) {
        if (latestTime == null) {
            // 上次触发时间为空，说明从未触发过，本次肯定需要触发
            return false;
        }
        int intervalValue = Integer.parseInt(interval);

        //计算间隔时间
        LocalDateTime thresholdTime = calculateThresholdTime(WarningIntervalUnitEnum.codeOf(intervalUnit), latestTime, intervalValue);
        if (thresholdTime == null) {
            // 该策略系统不支持处理，简单返回true，当成已触发。
            return true;
        }
        // 如果本次触发时间在阈值时间之前，则说明在间隔内已经触发过
        return triggerTime.isBefore(thresholdTime);
    }

}