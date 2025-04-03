package cn.bitlinks.ems.module.power.service.warningstrategy;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.common.util.object.PageUtils;
import cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.warningstrategy.WarningStrategyConditionDO;
import cn.bitlinks.ems.module.power.dal.dataobject.warningstrategy.WarningStrategyDO;
import cn.bitlinks.ems.module.power.dal.mysql.warninginfo.WarningInfoMapper;
import cn.bitlinks.ems.module.power.dal.mysql.warningstrategy.WarningStrategyConditionMapper;
import cn.bitlinks.ems.module.power.dal.mysql.warningstrategy.WarningStrategyMapper;
import cn.bitlinks.ems.module.power.service.standingbook.attribute.StandingbookAttributeService;
import cn.bitlinks.ems.module.power.service.standingbook.type.StandingbookTypeService;
import cn.bitlinks.ems.module.system.api.user.AdminUserApi;
import cn.bitlinks.ems.module.system.api.user.dto.AdminUserRespDTO;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ApiConstants.ATTR_EQUIPMENT_NAME;
import static cn.bitlinks.ems.module.power.enums.ApiConstants.ATTR_MEASURING_INSTRUMENT_MAME;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.WARNING_STRATEGY_CONDITION_NOT_NULL;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.WARNING_STRATEGY_NOT_EXISTS;

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
    private AdminUserApi adminUserApi;

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
        // 触发告警的设备参数集合和参数编码集合
        //        buildParams(warningStrategy.getCondition(), warningStrategy);
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
//        buildParams(updateObj.getCondition(), updateObj);
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
                .eq(WarningStrategyConditionDO::getStrategyId,warningStrategyDO.getId()));
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


}