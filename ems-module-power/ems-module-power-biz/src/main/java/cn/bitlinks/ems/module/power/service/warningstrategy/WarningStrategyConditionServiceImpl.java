package cn.bitlinks.ems.module.power.service.warningstrategy;

import cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo.AttributeTreeNode;
import cn.bitlinks.ems.module.power.dal.dataobject.measurementassociation.MeasurementAssociationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.measurementdevice.MeasurementDeviceDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import cn.bitlinks.ems.module.power.dal.mysql.measurementassociation.MeasurementAssociationMapper;
import cn.bitlinks.ems.module.power.dal.mysql.measurementdevice.MeasurementDeviceMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.StandingbookMapper;
import cn.bitlinks.ems.module.power.enums.standingbook.AttributeTreeNodeTypeEnum;
import cn.bitlinks.ems.module.power.enums.standingbook.StandingbookTypeTopEnum;
import cn.bitlinks.ems.module.power.service.standingbook.attribute.StandingbookAttributeService;
import cn.bitlinks.ems.module.power.service.standingbook.tmpl.StandingbookTmplDaqAttrService;
import cn.bitlinks.ems.module.power.service.standingbook.type.StandingbookTypeService;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.module.power.enums.ApiConstants.ATTR_EQUIPMENT_NAME;
import static cn.bitlinks.ems.module.power.enums.ApiConstants.ATTR_MEASURING_INSTRUMENT_MAME;

@Service
@Validated
@Slf4j
public class WarningStrategyConditionServiceImpl implements WarningStrategyConditionService {

    @Resource
    private StandingbookTmplDaqAttrService standingbookTmplDaqAttrService;
    @Resource
    private StandingbookAttributeService standingbookAttributeService;
    @Resource
    private StandingbookTypeService standingbookTypeService;
    @Resource
    private MeasurementAssociationMapper measurementAssociationMapper;
    @Resource
    private MeasurementDeviceMapper measurementDeviceMapper;

    @Resource
    private StandingbookMapper standingbookMapper;

    @Override
    public List<AttributeTreeNode> queryDaqTreeNodeByTypeAndSb(List<Long> standingBookIds, List<Long> typeIds) {
        List<AttributeTreeNode> sbNodes = buildAllNodeBySbIds(standingBookIds);
        List<AttributeTreeNode> typeNodes = buildAllNodeByTypeIds(typeIds);
        List<AttributeTreeNode> result = new ArrayList<>(sbNodes);
        result.addAll(typeNodes);
        return result;
    }

    /**
     * 组装设备分类+设备分类数采参数
     *
     * @param typeIds 设备分类ids
     * @return 节点列表
     */
    private List<AttributeTreeNode> buildAllNodeByTypeIds(List<Long> typeIds) {
        if (CollUtil.isEmpty(typeIds)) {
            return Collections.emptyList();
        }
        Map<Long, List<StandingbookTmplDaqAttrDO>> allDaqAttrMap =
                standingbookTmplDaqAttrService.getDaqAttrsByTypeIds(typeIds);
        Map<Long, StandingbookTypeDO> sbTypeDOMap =
                standingbookTypeService.getStandingbookTypeIdMap(typeIds);

        List<AttributeTreeNode> result = new ArrayList<>();

        typeIds.forEach(typeId -> {
            // 获取该分类的数采参数
            List<StandingbookTmplDaqAttrDO> attrDOS = allDaqAttrMap.get(typeId);
            if (CollUtil.isEmpty(attrDOS)) {
                return;
            }
            StandingbookTypeDO standingbookTypeDO = sbTypeDOMap.get(typeId);
            // 组装分类节点
            AttributeTreeNode typeNode = new AttributeTreeNode();
            typeNode.setPId(0L);
            typeNode.setType(AttributeTreeNodeTypeEnum.SB_TYPE.getCode());
            typeNode.setId(typeId);
            typeNode.setName(Objects.isNull(standingbookTypeDO) ? StringPool.EMPTY : standingbookTypeDO.getName());
            typeNode.setAttrChildren(attrDOS);
            result.add(typeNode);
        });
        return result;
    }

    /**
     * 组装设备+设备数采参数节点
     *
     * @param standingBookIds 台账ids
     * @return 节点列表
     */
    private List<AttributeTreeNode> buildAllNodeBySbIds(List<Long> standingBookIds) {
        if (CollUtil.isEmpty(standingBookIds)) {
            return Collections.emptyList();
        }
        // 0. 获取所有计量器具关联关系
        List<MeasurementAssociationDO> measurementAssociationDOS = measurementAssociationMapper.selectList();
        // 0.1 获取所有计量器具属性列表
        Set<Long> allAssociationIds = new HashSet<>();
        if (CollUtil.isNotEmpty(measurementAssociationDOS)) {
            List<Long> mIds1 = measurementAssociationDOS.stream().map(MeasurementAssociationDO::getMeasurementId).collect(Collectors.toList());
            List<Long> mIds2 = measurementAssociationDOS.stream().map(MeasurementAssociationDO::getMeasurementInstrumentId).collect(Collectors.toList());
            allAssociationIds.addAll(mIds1);
            allAssociationIds.addAll(mIds2);
        }
        List<MeasurementDeviceDO> measurementDeviceDOS =
                measurementDeviceMapper.selectList(new LambdaQueryWrapper<MeasurementDeviceDO>()
                        .in(MeasurementDeviceDO::getDeviceId, standingBookIds));
        if (CollUtil.isNotEmpty(measurementDeviceDOS)) {
            List<Long> dIds1 = measurementDeviceDOS.stream().map(MeasurementDeviceDO::getDeviceId).collect(Collectors.toList());
            List<Long> mIds3 = measurementDeviceDOS.stream().map(MeasurementDeviceDO::getMeasurementInstrumentId).collect(Collectors.toList());
            allAssociationIds.addAll(dIds1);
            allAssociationIds.addAll(mIds3);
        }
        allAssociationIds.addAll(standingBookIds);
        // 获取涉及的所有台账的基础属性列表映射

        Map<Long, List<StandingbookAttributeDO>> allAttrMap =
                standingbookAttributeService.getAttributesBySbIds(new ArrayList<>(allAssociationIds));
        // 获取涉及的所有台账的数采属性列表映射
        Map<Long, List<StandingbookTmplDaqAttrDO>> allDaqAttrMap =
                standingbookTmplDaqAttrService.getDaqAttrsBySbIds(new ArrayList<>(allAssociationIds));

        // 1.根据勾引的设备查询是计量器具还是设备，如果是重点设备，找出下边的计量器具；
        List<StandingbookDO> sbs = standingbookMapper.selectList(new LambdaQueryWrapper<StandingbookDO>().in(StandingbookDO::getId, standingBookIds));
        if (CollUtil.isEmpty(sbs)) {
            return Collections.emptyList();
        }
        // 1.1 查询台账对应的台账类型，
        List<Long> sbTypeIds = sbs.stream().map(StandingbookDO::getTypeId).collect(Collectors.toList());
        Map<Long, StandingbookTypeDO> typeIdMap = standingbookTypeService.getStandingbookTypeIdMap(sbTypeIds);

        // 2.根据台账类型将sbs分为两组
        Map<Boolean, List<StandingbookDO>> groupSb = sbs.stream().collect(Collectors.groupingBy(sb ->
                StandingbookTypeTopEnum.EQUIPMENT.getCode().equals(typeIdMap.get(sb.getTypeId()).getTopType())
        ));
        // 1.1 选中的重点设备
        List<StandingbookDO> deviceList = groupSb.get(true);
        List<AttributeTreeNode> deviceNode = getDeviceNode(deviceList, allAttrMap,
                allDaqAttrMap, measurementAssociationDOS, measurementDeviceDOS);
        List<AttributeTreeNode> result = new ArrayList<>(deviceNode);
        // 1.2 选中的计量器具
        List<StandingbookDO> measureList = groupSb.get(false);
        if (CollUtil.isNotEmpty(measureList)) {
            List<Long> mIds = measureList.stream().map(StandingbookDO::getId).collect(Collectors.toList());
            List<AttributeTreeNode> measureNode = getMeasureRelNode(0L, mIds, allAttrMap, allDaqAttrMap,
                    measurementAssociationDOS);
            result.addAll(measureNode);
        }


        return result;
    }

    /**
     * 选择重点设备获取参数条件的树形节点
     *
     * @param deviceList                设备配置列表
     * @param allAttrMap                所有基础属性map
     * @param allDaqAttrMap             所有数采属性map
     * @param measurementAssociationDOS 计量器具关联下级计量器具列表
     * @param measurementDeviceDOS      计量器具关联设备列表
     * @return 树形节点集合
     */
    private List<AttributeTreeNode> getDeviceNode(List<StandingbookDO> deviceList,
                                                  Map<Long, List<StandingbookAttributeDO>> allAttrMap,
                                                  Map<Long, List<StandingbookTmplDaqAttrDO>> allDaqAttrMap,
                                                  List<MeasurementAssociationDO> measurementAssociationDOS,
                                                  List<MeasurementDeviceDO> measurementDeviceDOS) {

        if (CollUtil.isEmpty(deviceList)) {
            return Collections.emptyList();
        }

        List<AttributeTreeNode> result = new ArrayList<>();

        deviceList.forEach(device -> {
            // 获取该设备的数采参数
            List<StandingbookTmplDaqAttrDO> attrDOS = allDaqAttrMap.get(device.getId());
            if (CollUtil.isEmpty(attrDOS)) {
                return;
            }
            // 组装设备节点
            AttributeTreeNode deviceRoot = new AttributeTreeNode();
            deviceRoot.setPId(0L);
            deviceRoot.setType(AttributeTreeNodeTypeEnum.EQUIPMENT.getCode());
            deviceRoot.setId(device.getId());
            deviceRoot.setAttrChildren(attrDOS);
            List<StandingbookAttributeDO> attributeDOS = allAttrMap.get(device.getId());
            Optional<StandingbookAttributeDO> nameOptional = attributeDOS.stream()
                    .filter(attribute -> ATTR_EQUIPMENT_NAME.equals(attribute.getCode()))
                    .findFirst();
            deviceRoot.setName(nameOptional.map(StandingbookAttributeDO::getValue).orElse(StringPool.EMPTY));
            // 获取该设备关联的计量器具
            if (CollUtil.isEmpty(measurementDeviceDOS)) {
                result.add(deviceRoot);
                return;
            }
            List<MeasurementDeviceDO> deviceMeasurements =
                    measurementDeviceDOS.stream().filter(measurementDeviceDO -> device.getId().equals(measurementDeviceDO.getDeviceId())).collect(Collectors.toList());
            if (CollUtil.isEmpty(deviceMeasurements)) {
                result.add(deviceRoot);
                return;
            }
            List<Long> measureRelIds = measurementDeviceDOS.stream().map(MeasurementDeviceDO::getMeasurementInstrumentId).collect(Collectors.toList());
            // 构造下级计量器具节点
            List<AttributeTreeNode> measureNode = getMeasureRelNode(device.getId(), measureRelIds, allAttrMap,
                    allDaqAttrMap, measurementAssociationDOS);
            if (CollUtil.isEmpty(measureNode)) {
                result.add(deviceRoot);
                return;
            }
            deviceRoot.setChildren(measureNode);
            result.add(deviceRoot);
        });
        return result;
    }

    /**
     * 根据计量器具关联关系和计量器具 组合数采参数树形结构
     *
     * @param pId                       父级编号
     * @param measureIds                需要构建节点的计量器具id
     * @param allAttrMap                所有基础属性map
     * @param allDaqAttrMap             所有数采属性map
     * @param measurementAssociationDOS 计量器具关联下级计量器具列表
     * @return 组合数采参数树形结构 List<AttributeTreeNode>
     */
    private List<AttributeTreeNode> getMeasureRelNode(Long pId,
                                                      List<Long> measureIds,
                                                      Map<Long, List<StandingbookAttributeDO>> allAttrMap,
                                                      Map<Long, List<StandingbookTmplDaqAttrDO>> allDaqAttrMap,
                                                      List<MeasurementAssociationDO> measurementAssociationDOS) {
        if (CollUtil.isEmpty(measureIds)) {
            return Collections.emptyList();
        }

        List<AttributeTreeNode> result = new ArrayList<>();

        measureIds.forEach(measureId -> {
            // 获取该计量器具的数采参数
            List<StandingbookTmplDaqAttrDO> attrDOS = allDaqAttrMap.get(measureId);
            if (CollUtil.isEmpty(attrDOS)) {
                return;
            }
            // 组装计量器具节点
            AttributeTreeNode measureNode = new AttributeTreeNode();
            measureNode.setPId(pId);
            measureNode.setType(AttributeTreeNodeTypeEnum.MEASURING.getCode());
            measureNode.setId(measureId);
            measureNode.setAttrChildren(attrDOS);

            List<StandingbookAttributeDO> measureAttrDOS = allAttrMap.get(measureId);
            Optional<StandingbookAttributeDO> measureNameOptional = measureAttrDOS.stream()
                    .filter(attribute -> ATTR_MEASURING_INSTRUMENT_MAME.equals(attribute.getCode()))
                    .findFirst();
            measureNode.setName(measureNameOptional.map(StandingbookAttributeDO::getValue).orElse(StringPool.EMPTY));

            // 获取计量器具关联的下级计量器具
            if (CollUtil.isEmpty(measurementAssociationDOS)) {
                result.add(measureNode);
                return;
            }
            List<MeasurementAssociationDO> measureAssociationList =
                    measurementAssociationDOS.stream().filter(measurementAssociationDO -> measureId.equals(measurementAssociationDO.getMeasurementId())).collect(Collectors.toList());
            if (CollUtil.isEmpty(measureAssociationList)) {
                result.add(measureNode);
                return;
            }
            // 1.获取关联的id
            List<Long> measureRelIds = measureAssociationList.stream().map(MeasurementAssociationDO::getMeasurementId).collect(Collectors.toList());
            // 把下一级的计量器具节点补充完整
            List<AttributeTreeNode> childList = getMeasureRelNode(measureId, measureRelIds, allAttrMap, allDaqAttrMap,
                    measurementAssociationDOS);
            if (CollUtil.isEmpty(childList)) {
                result.add(measureNode);
                return;
            }
            measureNode.setChildren(childList);
            result.add(measureNode);
        });
        return result;
    }

}
