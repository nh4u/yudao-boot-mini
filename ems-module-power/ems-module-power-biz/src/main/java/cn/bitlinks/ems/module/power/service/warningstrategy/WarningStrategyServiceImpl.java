package cn.bitlinks.ems.module.power.service.warningstrategy;

import cn.bitlinks.ems.framework.common.enums.CommonStatusEnum;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.common.util.object.PageUtils;
import cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
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
import cn.bitlinks.ems.module.power.enums.warninginfo.WarningTemplateKeyWordEnum;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.standingbook.attribute.StandingbookAttributeService;
import cn.bitlinks.ems.module.power.service.standingbook.type.StandingbookTypeService;
import cn.bitlinks.ems.module.system.api.user.AdminUserApi;
import cn.bitlinks.ems.module.system.api.user.dto.AdminUserRespDTO;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ApiConstants.*;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.WARNING_STRATEGY_CONDITION_NOT_NULL;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.WARNING_STRATEGY_NOT_EXISTS;
import static cn.bitlinks.ems.module.power.enums.warninginfo.WarningIntervalUnitEnum.calculateThresholdTime;
import static cn.bitlinks.ems.module.power.enums.warninginfo.WarningStrategyConnectorEnum.evaluate;
import static cn.bitlinks.ems.module.power.enums.warninginfo.WarningTemplateKeyWordEnum.*;
import static cn.hutool.core.date.DatePattern.NORM_DATETIME_FORMATTER;

/**
 * 告警策略 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
@Slf4j
public class WarningStrategyServiceImpl implements WarningStrategyService {

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

//    @Resource
//    private EnergyConfigurationService energyConfigurationService;
    @Resource
    private StandingbookTypeService standingbookTypeService;

    @Resource
    private StandingbookAttributeService standingbookAttributeService;

    private static final Integer batchSize = 2000;

    @Transactional
    @Override
    public Long createWarningStrategy(WarningStrategySaveReqVO createReqVO) {
        // 插入
        WarningStrategyDO warningStrategy = BeanUtils.toBean(createReqVO, WarningStrategyDO.class);
        buildScope(createReqVO.getSelectScope(), warningStrategy);
        // 触发告警的设备参数集合和参数编码集合冗余字段
//        buildParams(createReqVO.getCondition(), warningStrategy);
        warningStrategyMapper.insert(warningStrategy);

        // 添加条件
        createCondition(createReqVO.getCondition(), warningStrategy.getId());

        // 返回
        return warningStrategy.getId();
    }

    /**
     * 创建关联条件数据
     *
     * @param conditionVOS 条件
     * @param strategyId   策略id
     */
    private void createCondition(List<ConditionVO> conditionVOS, Long strategyId) {
        List<WarningStrategyConditionDO> warningStrategyConditionDOS = new ArrayList<>();
        if (CollUtil.isEmpty(conditionVOS)) {
            throw exception(WARNING_STRATEGY_CONDITION_NOT_NULL);
        }
        conditionVOS.forEach(conditionVO -> {
            WarningStrategyConditionDO warningStrategyConditionDO = BeanUtils.toBean(conditionVO, WarningStrategyConditionDO.class);
            warningStrategyConditionDO.setStrategyId(strategyId);
            warningStrategyConditionDOS.add(warningStrategyConditionDO);
        });
        warningStrategyConditionMapper.insertBatch(warningStrategyConditionDOS);
    }

//    /**
//     * 插入/修改 触发告警的设备参数集合和参数编码集合（暂不需要）
//     *
//     * @param warningStrategyDO 策略
//     */
//    private void buildParams(List<ConditionVO> conditionVOS, WarningStrategyDO warningStrategyDO) {
//        List<String> paramCodes = new ArrayList<>();
//        List<String> deviceIds = new ArrayList<>();
//        if (CollUtil.isNotEmpty(conditionVOS)) {
//            for (ConditionVO conditionVO : conditionVOS) {
//                List<String> paramIdList = conditionVO.getParamId();
//                if (paramIdList != null && paramIdList.size() >= 2) {  // Ensure there are at least 2 elements
//                    int size = paramIdList.size();
//                    String paramCode = paramIdList.get(size - 1);   // Last element
//                    String deviceId = paramIdList.get(size - 2);    // Second to last element
//                    paramCodes.add(paramCode);
//                    deviceIds.add(deviceId);
//                }
//            }
//            warningStrategyDO.setParamCodes(paramCodes);
//            warningStrategyDO.setSbIds(deviceIds);
//        }
//    }

    /**
     * 插入/修改 处理设备范围结构
     *
     * @param deviceScopeVOS    type+id
     * @param warningStrategyDO 策略
     */
    private void buildScope(List<DeviceScopeVO> deviceScopeVOS, WarningStrategyDO warningStrategyDO) {
        if (CollUtil.isEmpty(deviceScopeVOS)) {
            return;
        }
        // 处理结构
        Map<Boolean, List<Long>> groupedMap = deviceScopeVOS.stream()
                .collect(Collectors.groupingBy(
                        DeviceScopeVO::getDeviceFlag, // Key：deviceFlag
                        Collectors.mapping(DeviceScopeVO::getScopeId, Collectors.toList()) // Value: scopeId 列表
                ));
        warningStrategyDO.setDeviceScope(groupedMap.get(true));
        warningStrategyDO.setDeviceTypeScope(groupedMap.get(false));
    }

    @Transactional
    @Override
    public void updateWarningStrategy(WarningStrategySaveReqVO updateReqVO) {
        // 校验存在
        validateWarningStrategyExists(updateReqVO.getId());
        // 更新
        WarningStrategyDO updateObj = BeanUtils.toBean(updateReqVO, WarningStrategyDO.class);
        buildScope(updateReqVO.getSelectScope(), updateObj);
//        buildParams(updateReqVO.getCondition(), updateObj);
        warningStrategyMapper.updateById(updateObj);
        // 删除条件
        warningStrategyConditionMapper.delete(new LambdaQueryWrapper<WarningStrategyConditionDO>()
                .eq(WarningStrategyConditionDO::getStrategyId, updateReqVO.getId()));
        // 重新添加条件
        createCondition(updateReqVO.getCondition(), updateReqVO.getId());
    }

    @Override
    public void deleteWarningStrategy(Long id) {
        // 校验存在
        validateWarningStrategyExists(id);
        // 删除
        warningStrategyMapper.deleteById(id);
    }

    private void validateWarningStrategyExists(Long id) {
        if (warningStrategyMapper.selectById(id) == null) {
            throw exception(WARNING_STRATEGY_NOT_EXISTS);
        }
    }

    @Override
    public WarningStrategyRespVO getWarningStrategy(Long id) {

        WarningStrategyDO warningStrategyDO = warningStrategyMapper.selectById(id);

        WarningStrategyRespVO strategyRespVO = BeanUtils.toBean(warningStrategyDO, WarningStrategyRespVO.class);
        // 0.关联条件
        List<WarningStrategyConditionDO> warningStrategyConditionDO = warningStrategyConditionMapper.selectList(new LambdaQueryWrapper<WarningStrategyConditionDO>()
                .eq(WarningStrategyConditionDO::getStrategyId, warningStrategyDO.getId()));
        List<ConditionVO> conditionVOS = BeanUtils.toBean(warningStrategyConditionDO, ConditionVO.class);
        strategyRespVO.setCondition(conditionVOS);
        // 1.需要展示勾选的设备名称 和勾引选的分类名称
        List<Long> sbIds = warningStrategyDO.getDeviceScope();

        List<Long> sbTypeIds = warningStrategyDO.getDeviceTypeScope();
        List<DeviceScopeVO> deviceScopeList = new ArrayList<>();
        // 1.1 选择设备
        if (CollUtil.isNotEmpty(sbIds)) {
            List<DeviceScopeVO> sbScopeList = new ArrayList<>();
            Map<Long, List<StandingbookAttributeDO>> sbAttrMap = standingbookAttributeService.getAttributesBySbIds(sbIds);
            sbIds.forEach(sbId -> {
                List<StandingbookAttributeDO> sbAttrDOS = sbAttrMap.get(sbId);
                Optional<StandingbookAttributeDO> measureNameOptional = sbAttrDOS.stream()
                        .filter(attribute -> ATTR_MEASURING_INSTRUMENT_MAME.equals(attribute.getCode()))
                        .findFirst();
                Optional<StandingbookAttributeDO> deviceNameOptional = sbAttrDOS.stream()
                        .filter(attribute -> ATTR_EQUIPMENT_NAME.equals(attribute.getCode()))
                        .findFirst();
                DeviceScopeVO vo = new DeviceScopeVO();
                vo.setScopeId(sbId);
                vo.setDeviceFlag(true);
                measureNameOptional.ifPresent(standingbookAttributeDO -> vo.setScopeName(standingbookAttributeDO.getValue()));
                deviceNameOptional.ifPresent(standingbookAttributeDO -> vo.setScopeName(standingbookAttributeDO.getValue()));
                sbScopeList.add(vo);
            });
            deviceScopeList.addAll(sbScopeList);
        }
        // 1.2 选择设备分类
        if (CollUtil.isNotEmpty(sbTypeIds)) {
            List<DeviceScopeVO> typeScopeList = new ArrayList<>();
            Map<Long, StandingbookTypeDO> standingbookTypeDOMap = standingbookTypeService.getStandingbookTypeIdMap(sbTypeIds);
            sbTypeIds.forEach(typeId -> {
                StandingbookTypeDO typeDO = standingbookTypeDOMap.get(typeId);
                if (typeDO == null) {
                    return;
                }
                DeviceScopeVO vo = new DeviceScopeVO();
                vo.setScopeId(typeId);
                vo.setScopeName(typeDO.getName());
                vo.setDeviceFlag(false);
                typeScopeList.add(vo);
            });
            deviceScopeList.addAll(typeScopeList);
        }
        strategyRespVO.setDeviceScopeList(deviceScopeList);
        // 2.需要展示勾选的人员和人员名称
        List<Long> siteStaff = warningStrategyDO.getSiteStaff();
        List<Long> mailStaff = warningStrategyDO.getMailStaff();
        List<Long> allUserId = new ArrayList<>(siteStaff);
        allUserId.addAll(mailStaff);


        Map<Long, AdminUserRespDTO> allUserMap = adminUserApi.getUserMap(allUserId);

        if (CollUtil.isNotEmpty(siteStaff)) {
            List<AdminUserRespDTO> siteUserList = siteStaff.stream().map(allUserMap::get).collect(Collectors.toList());
            strategyRespVO.setSiteStaffList(siteUserList);
        }
        if (CollUtil.isNotEmpty(mailStaff)) {
            List<AdminUserRespDTO> mailUserList = mailStaff.stream().map(allUserMap::get).collect(Collectors.toList());
            strategyRespVO.setMailStaffList(mailUserList);
        }
        return strategyRespVO;
    }

    @Override
    public PageResult<WarningStrategyPageRespVO> getWarningStrategyPage(WarningStrategyPageReqVO pageReqVO) {

        Long count = warningStrategyMapper.getCount(pageReqVO);
        if (Objects.isNull(count) || count == 0L) {
            return new PageResult<>();
        }
        List<WarningStrategyPageRespVO> deviceApiResVOS = warningStrategyMapper.getPage(pageReqVO, PageUtils.getStart(pageReqVO));

        PageResult<WarningStrategyPageRespVO> result = new PageResult<>();
        result.setList(deviceApiResVOS);
        result.setTotal(count);
        return result;

    }

    @Override
    public void deleteWarningStrategyBatch(List<Long> ids) {
        warningStrategyMapper.deleteByIds(ids);
    }

    @Override
    public void updateWarningStrategyStatusBatch(WarningStrategyBatchUpdStatusReqVO updateReqVO) {
        warningStrategyMapper.update(new LambdaUpdateWrapper<>(WarningStrategyDO.class)
                .in(WarningStrategyDO::getId, updateReqVO.getIds())
                .set(WarningStrategyDO::getStatus, updateReqVO.getStatus()));
    }

    @Override
    public void updateWarningStrategyIntervalBatch(WarningStrategyBatchUpdIntervalReqVO updateReqVO) {
        warningStrategyMapper.update(new LambdaUpdateWrapper<>(WarningStrategyDO.class)
                .in(WarningStrategyDO::getId, updateReqVO.getIds())
                .set(WarningStrategyDO::getInterval, updateReqVO.getInterval())
                .set(WarningStrategyDO::getIntervalUnit, updateReqVO.getIntervalUnit())
        );
    }

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
                        boolean isMatch = evaluate(WarningStrategyConnectorEnum.codeOf(conditionVO.getConnector()), conditionVO.getValue(), sbDataTriggerVO.getValue());
                        if (!isMatch) {
                            return;
                        }
                        // 条件符合，填充所有关键字参数
                        Map<String, String> conditionParamsMap = new HashMap<>();
                        conditionParamsMap.put(WARNING_TIME.getKeyWord(), triggerTime.format(NORM_DATETIME_FORMATTER));
                        conditionParamsMap.put(WARNING_LEVEL.getKeyWord(), warningStrategyDO.getLevel() + "");
                        conditionParamsMap.put(WARNING_EXCEPTION_TIME.getKeyWord(), sbDataTriggerVO.getDataTime().format(NORM_DATETIME_FORMATTER));
                        conditionParamsMap.put(WARNING_VALUE.getKeyWord(), sbDataTriggerVO.getValue());

                        // todo 等能源参数补充好之后，再进行填充的修改
                        conditionParamsMap.put(WARNING_PARAM.getKeyWord(), sbDataTriggerVO.getParamCode());
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
                // 1.1一条策略-组装好一条告警信息

                //------------------------------（还差收件人未填充）------------------------
                // 填充唯一关键字map参数（单行部分）
                Map<String, String> keyWordUniqueMap = new HashMap<>();
                keyWordUniqueMap.put(WARNING_TIME.getKeyWord(), triggerTime.format(NORM_DATETIME_FORMATTER));
                keyWordUniqueMap.put(WARNING_LEVEL.getKeyWord(), warningStrategyDO.getLevel() + "");
                keyWordUniqueMap.put(WARNING_STRATEGY_NAME.getKeyWord(), warningStrategyDO.getName());
                // 一、发送站内信
//                //站内信模板通知人员
//                WarningInfoDO warningInfoDO = new WarningInfoDO();
//                warningInfoDO.setLevel(warningStrategyDO.getLevel());
//                warningInfoDO.setWarningTime(triggerTime);
//                warningInfoDO.setTemplateId(warningStrategyDO.getSiteTemplateId());
//                warningInfoDO.setStrategyId(warningStrategyDO.getId());
//                warningInfoDO.setDeviceRel(String.join(StringPool.COMMA, deviceRelList));
//                List<Long> siteUserIds = warningStrategyDO.getSiteStaff();
                // 二、发送邮件
                //邮件模板通知人员
                List<Long> mailUserIds = warningStrategyDO.getMailStaff();
                WarningTemplateDO mailTemplateDO = templatesMap.get(warningStrategyDO.getMailTemplateId());
                if(mailTemplateDO != null){
                    sendMailMsg(mailUserIds, mailTemplateDO, conditionParamsMapList);
                }


            });
            //
        }

        // 2.从告警策略中删除不符合接口入参的告警策略，符合的告警策略，生成告警信息，
        // 告警策略的设备id需要全部包含在此批数据中，查询出来之后，去告警信息，筛选一遍，上次的告警时间与本次的告警间隔是否匹配。。

        // 最后这些告警策略，根据告警策略的设备id去填充模板数据。

    }

    /**
     * 针对每条策略对每位收件人发送邮件（无能力优化）
     * @param userIds  收件人们
     * @param templateDO 告警模板
     * @param conditionParamsMapList 关键字实际填充的参数
     */
    private void sendMailMsg(List<Long> userIds, WarningTemplateDO templateDO, List<Map<String, String>> conditionParamsMapList) {

        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        Map<Long, AdminUserRespDTO> mailUserMap = adminUserApi.getUserMap(userIds);
        userIds.forEach(userId -> {
            AdminUserRespDTO adminUserRespDTO = mailUserMap.get(userId);
            String userName = adminUserRespDTO.getNickname();
            // 处理关键字参数
            List<Map<String, String>> newConditionParamsMapList = new ArrayList<>(conditionParamsMapList);
            newConditionParamsMapList.forEach(conditionParamsMap -> conditionParamsMap.put(WARNING_USER_NAME.getKeyWord(), userName));
            // 每个人的告警信息构造完成
            String title = buildTitleOrContentByParams(templateDO.getTParams(), templateDO.getTitle(), conditionParamsMapList);
            String content = buildTitleOrContentByParams(templateDO.getParams(), templateDO.getContent(), conditionParamsMapList);
            // todo 发送邮件

        });


    }
//    private void sendSiteMsg(List<Long> userIds, Map<Long, WarningTemplateDO> templatesMap,Map<String, String> keyWordUniqueMap,List<Map<String, String>> conditionParamsMapList, WarningInfoDO warningInfoDO){
//        if(CollUtil.isEmpty(userIds)){
//            return;
//        }
//        List<Map<String, String>> keyWordUniqueMap
//        userIds.forEach(userId->{
//            keyWordUniqueMap.put(WARNING_USER_NAME.getKeyWord(), userId.toString());
//        });
//        // todo 看收件人怎么说
//        //keyWordUniqueMap.put(WARNING_USER_NAME.getKeyWord(), warningStrategyDO.getLevel());
//
//        // 5.1 发送站内信
//        // 5.2 发送邮件
//
//
//
//        // 根据模板填充 站内信标题和内容
//        WarningTemplateDO siteTemplate = templatesMap.get(warningStrategyDO.getSiteTemplateId());
//        warningInfoDO.setTitle(buildContentByParams(siteTemplate.getTParams(),siteTemplate.getTitle(), keyWordUniqueMap, conditionParamsMapList));
//        warningInfoDO.setContent(buildContentByParams(siteTemplate.getParams(), siteTemplate.getContent(),keyWordUniqueMap, conditionParamsMapList));
//    }

    /**
     * 构建单条模板字符串参数
     *
     * @param conditionParamsMapList 所有关键字组成的条件参数们。
     * @return 替换好关键字的字符串
     */
    private String buildTitleOrContentByParams(List<String> keyWord, String templateStr, List<Map<String, String>> conditionParamsMapList) {
        // 如果模板中参数都是唯一的，那么不需要循环，直接按照模板填充一次即可，否则，需要循环内容，
        StringBuilder sb = new StringBuilder();
        boolean isUnique = WarningTemplateKeyWordEnum.areAnyKeywordsOutsideUniqueRange(keyWord);
        if (isUnique) {
            sb.append(StrUtil.format(templateStr, conditionParamsMapList.get(0)));
            return sb.toString();
        }
        conditionParamsMapList.forEach(paramMap -> sb.append(StrUtil.format(templateStr, paramMap)));
        return sb.toString();
    }


    /**
     * 判断告警间隔内是否触发过，
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