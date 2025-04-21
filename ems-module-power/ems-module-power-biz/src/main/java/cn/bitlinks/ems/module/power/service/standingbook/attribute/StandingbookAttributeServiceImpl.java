package cn.bitlinks.ems.module.power.service.standingbook.attribute;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.AttributeTreeNode;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributePageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.type.vo.StandingbookTypeListReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyparameters.EnergyParametersDO;
import cn.bitlinks.ems.module.power.dal.dataobject.measurementassociation.MeasurementAssociationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.measurementdevice.MeasurementDeviceDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import cn.bitlinks.ems.module.power.dal.mysql.measurementassociation.MeasurementAssociationMapper;
import cn.bitlinks.ems.module.power.dal.mysql.measurementdevice.MeasurementDeviceMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.StandingbookMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.attribute.StandingbookAttributeMapper;
import cn.bitlinks.ems.module.power.enums.ApiConstants;
import cn.bitlinks.ems.module.power.enums.standingbook.AttributeTreeNodeTypeEnum;
import cn.bitlinks.ems.module.power.enums.standingbook.StandingbookTypeTopEnum;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.standingbook.type.StandingbookTypeService;
import cn.bitlinks.ems.module.system.api.user.AdminUserApi;
import cn.bitlinks.ems.module.system.api.user.dto.AdminUserRespDTO;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ApiConstants.*;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static com.baomidou.mybatisplus.core.toolkit.StringPool.EMPTY;

/**
 * 台账属性 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class StandingbookAttributeServiceImpl implements StandingbookAttributeService {
    @Resource
    private StandingbookAttributeMapper standingbookAttributeMapper;

    @Resource
    private StandingbookMapper standingbookMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    @Lazy
    private StandingbookTypeService standingbookTypeService;

    @Resource
    @Lazy
    private EnergyConfigurationService EnergyConfigurationService;
    @Resource
    @Lazy
    private StandingbookAttributeService standingbookAttributeService;
    @Resource
    private MeasurementAssociationMapper measurementAssociationMapper;
    @Resource
    private MeasurementDeviceMapper measurementDeviceMapper;

    @Transactional
    @Override
    public Long createStandingbookAttribute(StandingbookAttributeSaveReqVO createReqVO) {
        // 插入
        StandingbookAttributeDO standingbookAttribute = BeanUtils.toBean(createReqVO, StandingbookAttributeDO.class);
        if ((standingbookAttribute.getId() == null)) {
            standingbookAttributeMapper.insert(standingbookAttribute);
        } else {
            standingbookAttributeMapper.updateById(standingbookAttribute);
        }
        // 返回
        return standingbookAttribute.getId();
    }

    @Transactional
    @Override
    public Long create(StandingbookAttributeSaveReqVO createReqVO) {
        // 插入
        StandingbookAttributeDO standingbookAttribute = BeanUtils.toBean(createReqVO, StandingbookAttributeDO.class);
        standingbookAttributeMapper.insert(standingbookAttribute);
        // 返回
        return standingbookAttribute.getId();
    }

    @Transactional
    @Override
    public void createStandingbookAttributeBatch(List<StandingbookAttributeDO> dos) {
        standingbookAttributeMapper.insertBatch(dos);
    }

    @Transactional
    @Override
    public void updateStandingbookAttribute(StandingbookAttributeSaveReqVO updateReqVO) {
        // 校验存在
        validateStandingbookAttributeExists(updateReqVO.getId());
        // 更新
        StandingbookAttributeDO updateObj = BeanUtils.toBean(updateReqVO, StandingbookAttributeDO.class);
        standingbookAttributeMapper.updateById(updateObj);
    }

    @Transactional
    @Override
    public void update(StandingbookAttributeSaveReqVO updateReqVO) {
        // 更新
        StandingbookAttributeDO updateObj = BeanUtils.toBean(updateReqVO, StandingbookAttributeDO.class);
        LambdaUpdateWrapper<StandingbookAttributeDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(StandingbookAttributeDO::getCode, updateObj.getCode()).eq(StandingbookAttributeDO::getStandingbookId, updateObj.getStandingbookId());
        standingbookAttributeMapper.update(updateObj, updateWrapper);
    }

    @Override
    public void deleteStandingbookAttribute(Long id) {
        // 校验存在
        validateStandingbookAttributeExists(id);
        // 删除
        standingbookAttributeMapper.deleteById(id);
    }

    @Override
    public void deleteStandingbookAttributeByTypeId(Long typeId) {
        // 校验存在
        validateStandingbookAttributeExistsByTypeId(typeId);
        // 删除
        standingbookAttributeMapper.deleteTypeId(typeId);
    }

    @Override
    public void deleteStandingbookAttributeByStandingbookId(Long standingbookId) {
        // 校验存在
        validateStandingbookAttributeExistsByStandingbookId(standingbookId);
        // 删除
        standingbookAttributeMapper.deleteStandingbookId(standingbookId);
    }

    private void validateStandingbookAttributeExists(Long id) {
        if (standingbookAttributeMapper.selectById(id) == null) {
            throw exception(STANDINGBOOK_ATTRIBUTE_NOT_EXISTS);
        }
    }

    private void validateStandingbookAttributeExistsByStandingbookId(Long standingbookId) {
        if (standingbookAttributeMapper.selectStandingbookId(standingbookId) == null) {
            throw exception(STANDINGBOOK_ATTRIBUTE_NOT_EXISTS);
        }
    }

    private void validateStandingbookAttributeExistsByTypeId(Long typeId) {
        if (standingbookAttributeMapper.selectTypeId(typeId) == null) {
            throw exception(STANDINGBOOK_ATTRIBUTE_NOT_EXISTS);
        }
    }

    @Override
    public StandingbookAttributeDO getStandingbookAttribute(Long id) {
        return standingbookAttributeMapper.selectById(id);
    }

    @Override
    public List<StandingbookAttributeDO> getStandingbookAttributeByStandingbookId(Long standingbookId) {
        return standingbookAttributeMapper.selectStandingbookId(standingbookId);
    }

    @Override
    public List<StandingbookAttributeDO> getStandingbookAttributeByTypeId(Long typeId) {
        return standingbookAttributeMapper.selectTypeId(typeId);
    }

    @Override
    public PageResult<StandingbookAttributeDO> getStandingbookAttributePage(StandingbookAttributePageReqVO pageReqVO) {
        return standingbookAttributeMapper.selectPage(pageReqVO);
    }

    @Override
    public List<Long> getStandingbookIdByCondition(Map<String, List<String>> children, List<Long> sbIds) {
        return standingbookAttributeMapper.selectStandingbookIdByAttrCondition(children, sbIds);
    }

//    @Override
//    public List<StandingbookDO> getStandingbookIntersection(List<StandingbookAttributePageReqVO> children, Long typeId) {
//        return standingbookAttributeMapper.selectStandingbookIntersection(children, typeId);
//    }

    @Override
    @Transactional
    public void saveMultiple(List<StandingbookAttributeSaveReqVO> createReqVOs) {
        Long typeId = createReqVOs.get(0).getTypeId();

        List<StandingbookAttributeDO> optAttrList = BeanUtils.toBean(createReqVOs, StandingbookAttributeDO.class);

        // 找出当前属性列表
        List<StandingbookAttributeDO> rawAttrList = getStandingbookAttributeByTypeId(typeId);
        // 级联删除
        deleteAttrCascade(optAttrList, rawAttrList);
        // 剔除父节点自动生成属性，
        optAttrList.removeIf(upd -> ApiConstants.YES.equals(upd.getAutoGenerated()));
        if (CollUtil.isEmpty(optAttrList)) {
            return;
        }
        // 将数据分为2组，分成 createList 和 updateList
        List<StandingbookAttributeDO> createList = optAttrList.stream()
                .filter(attribute -> attribute.getId() == null)
                .collect(Collectors.toList());
        List<StandingbookAttributeDO> updateList = optAttrList.stream()
                .filter(attribute -> attribute.getId() != null)
                .collect(Collectors.toList());
        // 进行级联创建操作
        createAttrCascade(createList, typeId);
        // 进行级联修改操作
        updateAttrCascade(updateList, rawAttrList);

    }


    /**
     * 过滤掉未修改的分类属性
     *
     * @param updateList  分类属性列表
     * @param rawAttrList 原始分类属性列表
     */
    public static void filterModifiedAttributes(List<StandingbookAttributeDO> updateList, List<StandingbookAttributeDO> rawAttrList) {
        // 使用迭代器，避免 ConcurrentModificationException
        Iterator<StandingbookAttributeDO> iterator = updateList.iterator();
        while (iterator.hasNext()) {
            StandingbookAttributeDO updatedAttr = iterator.next();
            boolean modified = false; // 默认没有修改
            for (StandingbookAttributeDO rawAttr : rawAttrList) {
                if (updatedAttr.getId().equals(rawAttr.getId())) { // 找到匹配的 ID
                    // 比较属性 (简化写法)
                    if (!Objects.equals(updatedAttr.getName(), rawAttr.getName()) ||
                            !Objects.equals(updatedAttr.getFormat(), rawAttr.getFormat()) ||
                            !Objects.equals(updatedAttr.getSort(), rawAttr.getSort()) ||
                            !Objects.equals(updatedAttr.getIsRequired(), rawAttr.getIsRequired())) {
                        modified = true;  // 至少有一个属性被修改
                    }
                    break; // 找到匹配的 rawAttr，结束内层循环
                }
            }
            if (!modified) {
                iterator.remove(); // 如果没有修改，从 updateList 中移除
            }
        }
    }

    /**
     * 级联修改属性列表
     *
     * @param updateList  修改属性列表
     * @param rawAttrList 原属性列表
     */
    @Transactional
    public void updateAttrCascade(List<StandingbookAttributeDO> updateList, List<StandingbookAttributeDO> rawAttrList) {
        if (CollUtil.isEmpty(updateList)) {
            return;
        }
        // 0. 修改列表中需要剔除掉实际没有修改的数据，需要与当前节点列表做对比，
        filterModifiedAttributes(updateList, rawAttrList);
        if (CollUtil.isEmpty(updateList)) {
            return;
        }
        // 1. 修改操作节点属性
        standingbookAttributeMapper.updateBatch(updateList);
        // 2. 修改影响节点属性
        Set<Long> updIds = updateList.stream()
                .map(StandingbookAttributeDO::getId)
                .collect(Collectors.toSet());
        // 2.1 获取影响到的节点属性
        List<StandingbookAttributeDO> attrList = standingbookAttributeMapper.selectList(new LambdaQueryWrapper<StandingbookAttributeDO>()
                .in(StandingbookAttributeDO::getRawAttrId, updIds));
        // 2.2 根据 rawAttrId 分组
        Map<Long, List<StandingbookAttributeDO>> groupedByRawAttrId = attrList.stream()
                .collect(Collectors.groupingBy(StandingbookAttributeDO::getRawAttrId));
        groupedByRawAttrId.entrySet().removeIf(entry -> CollUtil.isEmpty(entry.getValue()));

        // 2.3 级联修改操作节点的属性，组成List
        List<StandingbookAttributeDO> cascadeAttrList = new ArrayList<>();
        groupedByRawAttrId.forEach((rawAttrId, updAttrList) -> {
            // 需要处理修改的属性列表（同一属性id对应的关联属性列表）
            List<StandingbookAttributeDO> optUpdAttrList = BeanUtil.copyToList(updAttrList, StandingbookAttributeDO.class);
            // 变动修改的属性
            Optional<StandingbookAttributeDO> foundAttribute = updateList.stream().filter(updAttr -> updAttr.getId().equals(rawAttrId)).findFirst();
            if (foundAttribute.isPresent()) {
                StandingbookAttributeDO updAttribute = foundAttribute.get();
                optUpdAttrList.forEach(attribute -> {
                    //如果类型改变的话，属性值要清空(针对台账的属性列表)
                    if (!attribute.getFormat().equals(updAttribute.getFormat())) {
                        attribute.setValue(EMPTY);
                    }
                    attribute.setName(updAttribute.getName())
                            .setIsRequired(updAttribute.getIsRequired())
                            .setSort(updAttribute.getSort())
                            .setFormat(updAttribute.getFormat());

                });
                cascadeAttrList.addAll(optUpdAttrList);
            }
        });
        if (CollUtil.isEmpty(cascadeAttrList)) {
            return;
        }
        // 2.4 执行修改级联节点属性操作
        standingbookAttributeMapper.updateBatch(cascadeAttrList);
    }

    /**
     * 级联新增属性列表（分类编码重复）
     *
     * @param createList 新增属性列表
     * @param typeId     台账类型id
     */
    @Transactional
    public void createAttrCascade(List<StandingbookAttributeDO> createList, Long typeId) {
        if (CollUtil.isEmpty(createList)) {
            return;
        }

        // 1. 新增当前分类的属性
        standingbookAttributeMapper.insertBatch(createList);
        // 1.1 新增当前分类台账的属性
        List<StandingbookDO> sbList = standingbookMapper.selectList(new LambdaQueryWrapper<StandingbookDO>()
                .in(StandingbookDO::getTypeId, typeId)
        );
        List<StandingbookAttributeDO> attrList = new ArrayList<>();
        if (CollUtil.isNotEmpty(sbList)) {
            sbList.forEach(sb -> {
                createList.forEach(attribute -> {
                    StandingbookAttributeDO attributeCopy = BeanUtil.copyProperties(attribute, StandingbookAttributeDO.class);
                    attributeCopy.setDescription(PARENT_ATTR_AUTO)
                            .setAutoGenerated(ApiConstants.YES)
                            .setTypeId(typeId)
                            .setStandingbookId(sb.getId())
                            .setId(null)
                            .setRawAttrId(attribute.getId());
                    attrList.add(attributeCopy);
                });
            });
        }
        standingbookAttributeMapper.insertBatch(attrList);
        // 2. 获取所有typeId tree，查询所有的子级节点的typeId
        List<StandingbookTypeDO> typeList = standingbookTypeService.getStandingbookTypeNode();
        List<Long> subtreeIds = getSubtreeIds(typeList, typeId);
        subtreeIds.removeIf(typeId::equals);
        if (CollUtil.isEmpty(subtreeIds)) {
            return;
        }
        // 3. 校验编码是否与子孙分类编码重复(新增逻辑，后台校验编码唯一)
        Set<String> codeSet = createList.stream()
                .map(StandingbookAttributeDO::getCode)
                .collect(Collectors.toSet());
        List<StandingbookAttributeDO> childrenAttrList = standingbookAttributeMapper.selectList(new LambdaQueryWrapper<StandingbookAttributeDO>()
                .in(StandingbookAttributeDO::getTypeId, subtreeIds)
                .eq(StandingbookAttributeDO::getAutoGenerated, ApiConstants.NO));
        Set<String> childrenCodeSet = childrenAttrList.stream()
                .map(StandingbookAttributeDO::getCode)
                .collect(Collectors.toSet());
        codeSet.retainAll(childrenCodeSet);
        // 新增编码存在于子级手动新增的编码中
        if (!codeSet.isEmpty()) {
            throw exception(STANDINGBOOK_CODE_REPEAT_CHILDREN);
        }

        // 3.0.1 查询所有级联的typeId关联的台账Id列表
        List<StandingbookDO> cascadeTypeIdList = standingbookMapper.selectList(new LambdaQueryWrapper<StandingbookDO>()
                .in(StandingbookDO::getTypeId, subtreeIds)
        );
        Map<Long, List<StandingbookDO>> typeIdMapSb = new HashMap<>();
        if (CollUtil.isNotEmpty(cascadeTypeIdList)) {
            // 按照typeId-台账分组
            typeIdMapSb = cascadeTypeIdList.stream()
                    .collect(Collectors.groupingBy(StandingbookDO::getTypeId));
        }
        // 3. 新增所有级联的typeId的属性
        List<StandingbookAttributeDO> cascadeAttrList = new ArrayList<>();
        Map<Long, List<StandingbookDO>> finalTypeIdMapSb = typeIdMapSb;
        subtreeIds.forEach(subId -> {
            // 处理新增的属性列表
            List<StandingbookAttributeDO> subAttrList = new ArrayList<>();
            createList.forEach(attribute -> {
                StandingbookAttributeDO attributeCopy = BeanUtil.copyProperties(attribute, StandingbookAttributeDO.class);
                attributeCopy.setDescription(PARENT_ATTR_AUTO)
                        .setAutoGenerated(ApiConstants.YES)
                        .setTypeId(subId)
                        .setId(null)
                        .setRawAttrId(attribute.getId());
                subAttrList.add(attributeCopy);
            });
            // 通过台账分类去关联台账新增属性列表
            if (CollUtil.isNotEmpty(finalTypeIdMapSb) && finalTypeIdMapSb.containsKey(subId)) {
                List<StandingbookDO> typeSbList = finalTypeIdMapSb.get(subId);
                typeSbList.forEach(sb -> {
                    createList.forEach(attribute -> {
                        StandingbookAttributeDO attributeCopy = BeanUtil.copyProperties(attribute, StandingbookAttributeDO.class);
                        attributeCopy.setDescription(PARENT_ATTR_AUTO)
                                .setAutoGenerated(ApiConstants.YES)
                                .setTypeId(subId)
                                .setStandingbookId(sb.getId())
                                .setId(null)
                                .setRawAttrId(attribute.getId());
                        subAttrList.add(attributeCopy);
                    });
                });
            }
            cascadeAttrList.addAll(subAttrList);
        });

        // 4.执行新增操作
        standingbookAttributeMapper.insertBatch(cascadeAttrList);

    }

    // 递归查询子节点 id
    public static List<Long> getSubtreeIds(List<StandingbookTypeDO> typeList, Long targetId) {
        List<Long> subtreeIds = new ArrayList<>();

        StandingbookTypeDO node = findNode(typeList, targetId);
        getSubtreeIdsRecursive(node, subtreeIds);

        return subtreeIds;
    }

    // 树状结构中查询当前节点
    public static StandingbookTypeDO findNode(List<StandingbookTypeDO> typeList, Long targetId) {
        if (typeList == null || typeList.isEmpty()) {
            return null;
        }

        for (StandingbookTypeDO node : typeList) {
            if (node.getId().equals(targetId)) {
                return node; // 找到目标节点
            }

            // 递归查找子节点
            StandingbookTypeDO foundInChildren = findNode(node.getChildren(), targetId);
            if (foundInChildren != null) {
                return foundInChildren; // 在子节点中找到目标节点
            }
        }

        return null; // 没有找到目标节点
    }

    // 递归遍历子节点
    private static void getSubtreeIdsRecursive(StandingbookTypeDO node, List<Long> subtreeIds) {
        if (node == null) {
            return;
        }
        subtreeIds.add(node.getId()); // 添加当前节点 id

        for (StandingbookTypeDO child : node.getChildren()) {
            getSubtreeIdsRecursive(child, subtreeIds); // 递归遍历子节点
        }
    }

    /**
     * 级联删除属性列表
     *
     * @param optAttrList 新属性列表
     * @param rawAttrList 原属性列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAttrCascade(List<StandingbookAttributeDO> optAttrList, List<StandingbookAttributeDO> rawAttrList) {
        if (CollUtil.isEmpty(rawAttrList)) {
            return;
        }
        // 找出被删除的属性id集合
        Set<Long> deleteIds = rawAttrList.stream()
                .map(StandingbookAttributeDO::getId)
                .filter(id -> optAttrList.stream().noneMatch(reqVO -> id.equals(reqVO.getId())))
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(deleteIds)) {
            return;
        }
        // 删除操作节点属性
        standingbookAttributeMapper.deleteByIds(deleteIds);
        // 删除关联的节点属性
        standingbookAttributeMapper.delete(new LambdaQueryWrapper<StandingbookAttributeDO>()
                .in(StandingbookAttributeDO::getRawAttrId, deleteIds));
    }

    @Override
    public List<AttributeTreeNode> queryAttributeTreeNodeByTypeAndSb(List<Long> standingBookIds, List<Long> typeIds) {

        // 0.0 查询能源-参数全部列表
        List<EnergyConfigurationDO> energyConfigurationDOS = EnergyConfigurationService.getAllEnergyConfiguration(null);

        // 0. 获取所有计量器具关联关系
        List<MeasurementAssociationDO> measurementAssociationDOS = measurementAssociationMapper.selectList();
        // 0.1 获取所有计量器具属性列表
        List<Long> mIds1 = measurementAssociationDOS.stream().map(MeasurementAssociationDO::getMeasurementId).collect(Collectors.toList());
        List<Long> mIds2 = measurementAssociationDOS.stream().map(MeasurementAssociationDO::getMeasurementInstrumentId).collect(Collectors.toList());
        Set<Long> allAssociationIds = new HashSet<>();
        allAssociationIds.addAll(mIds1);
        allAssociationIds.addAll(mIds2);
        allAssociationIds.addAll(standingBookIds);

        Map<Long, List<StandingbookAttributeDO>> measureAssociationAttrMap = standingbookAttributeService.getAttributesBySbIds(new ArrayList<>( allAssociationIds));

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
        List<AttributeTreeNode> deviceNode = getDeviceNode(measureAssociationAttrMap, deviceList, measurementAssociationDOS, energyConfigurationDOS);
        List<AttributeTreeNode> result = new ArrayList<>(deviceNode);
        // 1.2 选中的计量器具
        List<StandingbookDO> measureList = groupSb.get(false);
        List<Long> mIds = measureList.stream().map(StandingbookDO::getId).collect(Collectors.toList());
        List<AttributeTreeNode> measureNode = getMeasureRelNode(measureAssociationAttrMap, 0L, mIds, measurementAssociationDOS, energyConfigurationDOS);
        result.addAll(measureNode);

        return result;

    }

    /**
     * 选择重点设备获取参数条件的树形节点
     *
     * @param measureAssociationAttrMap 所有关联计量器具的属性列表
     * @param deviceList                设备配置列表
     * @param measurementAssociationDOS 计量器具关联下级计量器具列表
     * @param energyConfigurationDOS    所有能源参数配置列表
     * @return 树形节点集合
     */
    private List<AttributeTreeNode> getDeviceNode(Map<Long, List<StandingbookAttributeDO>> measureAssociationAttrMap, List<StandingbookDO> deviceList, List<MeasurementAssociationDO> measurementAssociationDOS, List<EnergyConfigurationDO> energyConfigurationDOS) {

        if (CollUtil.isEmpty(deviceList)) {
            return Collections.emptyList();
        }

        List<AttributeTreeNode> result = new ArrayList<>();
        List<Long> deviceIds = deviceList.stream().map(StandingbookDO::getId).collect(Collectors.toList());
        // 设备与下级计量器具关联关系
        List<MeasurementDeviceDO> deviceRelList = measurementDeviceMapper.selectList(new LambdaQueryWrapper<MeasurementDeviceDO>()
                .in(MeasurementDeviceDO::getDeviceId, deviceIds));

        if (CollUtil.isNotEmpty(deviceRelList)) {
            // 获取所有的计量器具id
            List<Long> measureIds = deviceRelList.stream().map(MeasurementDeviceDO::getMeasurementInstrumentId).collect(Collectors.toList());
            Map<Long, List<StandingbookAttributeDO>> deviceAttrsMap = standingbookAttributeService.getAttributesBySbIds(deviceIds);
            // 按照设备id分组
            Map<Long, List<MeasurementDeviceDO>> groupedByDeviceId = deviceRelList.stream()
                    .collect(Collectors.groupingBy(MeasurementDeviceDO::getDeviceId));
            deviceList.forEach(device -> {
                // 获取该设备关联的计量器具
                List<MeasurementDeviceDO> measurementDeviceDOS = groupedByDeviceId.get(device.getId());

                if (CollUtil.isNotEmpty(measurementDeviceDOS)) {
                    List<Long> measureRelIds = measurementDeviceDOS.stream().map(MeasurementDeviceDO::getMeasurementInstrumentId).collect(Collectors.toList());
                    // 构造设备根节点
                    AttributeTreeNode deviceRoot = new AttributeTreeNode();
                    deviceRoot.setPId(0L + EMPTY);
                    deviceRoot.setType(AttributeTreeNodeTypeEnum.SB_TYPE.getCode());
                    deviceRoot.setId(device.getId() + EMPTY);

                    List<StandingbookAttributeDO> attributeDOS = deviceAttrsMap.get(device.getId());
                    Optional<StandingbookAttributeDO> nameOptional = attributeDOS.stream()
                            .filter(attribute -> ATTR_EQUIPMENT_NAME.equals(attribute.getCode()))
                            .findFirst();
                    deviceRoot.setName(nameOptional.map(StandingbookAttributeDO::getValue).orElse(StringPool.EMPTY));
                    // 构造下级计量器具节点
                    List<AttributeTreeNode> measureNode = getMeasureRelNode(measureAssociationAttrMap, device.getId(), measureRelIds, measurementAssociationDOS, energyConfigurationDOS);
                    if (CollUtil.isNotEmpty(measureNode)) {
                        deviceRoot.setChildren(measureNode);
                        result.add(deviceRoot);
                    }
                }
            });
        }
        return result;
    }

    /**
     * 根据计量器具关联关系和计量器具 组合参数树形结构
     *
     * @param measureAttrMap            所有关联计量器具的属性列表
     * @param pId                      父级编号
     * @param measureIds                需要构建节点的计量器具id
     * @param measurementAssociationDOS 计量器具关联下级计量器具列表
     * @param energyConfigurationDOS    能源配置列表
     * @return 参数树形结构 List<AttributeTreeNode>
     */
    private List<AttributeTreeNode> getMeasureRelNode(Map<Long, List<StandingbookAttributeDO>> measureAttrMap, Long pId, List<Long> measureIds, List<MeasurementAssociationDO> measurementAssociationDOS, List<EnergyConfigurationDO> energyConfigurationDOS) {
        if (CollUtil.isEmpty(measureIds)) {
            return Collections.emptyList();
        }

        List<AttributeTreeNode> result = new ArrayList<>();
        // 按照计量器具id分组
        Map<Long, List<MeasurementAssociationDO>> groupedByMeasureId = measurementAssociationDOS.stream()
                .collect(Collectors.groupingBy(MeasurementAssociationDO::getMeasurementInstrumentId));
        measureIds.forEach(measureId -> {
            // 构造id
            AttributeTreeNode measureNode = new AttributeTreeNode();
            measureNode.setPId(pId + EMPTY);
            measureNode.setType(AttributeTreeNodeTypeEnum.SB_TYPE.getCode());
            measureNode.setId(measureId + EMPTY);
            List<StandingbookAttributeDO> measureAttrDOS = measureAttrMap.get(measureId);
            Optional<StandingbookAttributeDO> measureNameOptional = measureAttrDOS.stream()
                    .filter(attribute -> ATTR_MEASURING_INSTRUMENT_MAME.equals(attribute.getCode()))
                    .findFirst();
            measureNode.setName(measureNameOptional.map(StandingbookAttributeDO::getValue).orElse(StringPool.EMPTY));
            List<AttributeTreeNode> allChildren = new ArrayList<>();
            Optional<StandingbookAttributeDO> energyOptional = measureAttrDOS.stream()
                    .filter(attribute -> ATTR_ENERGY.equals(attribute.getCode()))
                    .findFirst();
            String energy = energyOptional.map(StandingbookAttributeDO::getValue).orElse(StringPool.EMPTY);
            // 构建能源参数节点（多个）这是它本身的能源参数节点
            allChildren.addAll(buildEnergyNode(measureId, Long.valueOf(energy), energyConfigurationDOS));
            // 1.获取关联的id
            List<MeasurementAssociationDO> measureAssociationList = groupedByMeasureId.get(measureId);
            if (CollUtil.isNotEmpty(measureAssociationList)) {
                List<Long> measureRelIds = measureAssociationList.stream().map(MeasurementAssociationDO::getMeasurementId).collect(Collectors.toList());
                // 把下一级的计量器具节点补充完整
                List<AttributeTreeNode> childList = getMeasureRelNode(measureAttrMap, measureId, measureRelIds, measurementAssociationDOS, energyConfigurationDOS);
                allChildren.addAll(childList);
            }
            // 添加子节点（包含能源参数节点和）
            if (CollUtil.isNotEmpty(allChildren)) {
                measureNode.setChildren(allChildren);
                result.add(measureNode);
            }
        });
        return result;
    }

    /**
     * 根据计量器具id构造能源参数节点
     *
     * @param measureId 计量器具id
     * @param energyId 能源id
     * @param energyConfigurationDOS 能源配置列表
     * @return 节点列表
     */
    private List<AttributeTreeNode> buildEnergyNode(Long measureId, Long energyId, List<EnergyConfigurationDO> energyConfigurationDOS) {

        Optional<EnergyConfigurationDO> energyOptional = energyConfigurationDOS.stream()
                .filter(energyConfigurationDO -> energyId.equals(energyConfigurationDO.getId()))
                .findFirst();
        if (!energyOptional.isPresent()) {
            return Collections.emptyList();
        }
        EnergyConfigurationDO energyConfigurationDO = energyOptional.get();
        List<EnergyParametersDO> energyParameters = energyConfigurationDO.getEnergyParameters();
        List<AttributeTreeNode> result = new ArrayList<>();
        energyParameters.forEach(energyParameter -> {
            AttributeTreeNode energyNode = new AttributeTreeNode();
            energyNode.setPId(measureId + EMPTY);
            energyNode.setType(AttributeTreeNodeTypeEnum.ATTR.getCode());
            energyNode.setId(energyParameter.getCode());
            energyNode.setName(energyParameter.getParameter());
            result.add(energyNode);
        });
        return result;
    }


    @Override
    public List<StandingbookAttributeRespVO> getByTypeId(Long typeId) {
        List<StandingbookAttributeDO> standingbookAttributes = getStandingbookAttributeByTypeId(typeId);
        List<StandingbookAttributeRespVO> bean = BeanUtils.toBean(standingbookAttributes, StandingbookAttributeRespVO.class);
        //查询所有分类
        Map<Long, StandingbookTypeDO> allTypeMap = standingbookTypeService.getStandingbookTypeIdMap(null);
        // 查询所有用户
        Map<Long, AdminUserRespDTO> allUserMap = adminUserApi.getAllUserMap();
        IntStream.range(0, bean.size()).forEach(i -> {
            String creatorId = standingbookAttributes.get(i).getCreator();

            // 获取归属节点名称
            StandingbookTypeDO type = allTypeMap.get(bean.get(i).getNodeId());
            if (type != null) {
                bean.get(i).setNode(type.getName());
            }
            if (ApiConstants.YES.equals(bean.get(i).getDisplayFlag())) {
                bean.get(i).setCreateByName(SYSTEM_CREATE);
            } else {
                // 获取创建人名称
                AdminUserRespDTO user = allUserMap.get(Long.valueOf(creatorId));
                if (user != null) {
                    bean.get(i).setCreateByName(user.getNickname());
                }
            }

        });
        return bean;

    }

    @Override
    public List<Long> getSbIdBySbCode(List<String> codes) {
        List<StandingbookAttributeDO> attrs = standingbookAttributeMapper.selectList(new LambdaQueryWrapper<StandingbookAttributeDO>()
                        .in(StandingbookAttributeDO::getCode, ATTR_EQUIPMENT_ID, ATTR_MEASURING_INSTRUMENT_ID)
                .in(StandingbookAttributeDO::getValue, codes));
        if(CollUtil.isEmpty(attrs)){
            return null;
        }
        // 提取id
        return attrs.stream().map(StandingbookAttributeDO::getStandingbookId).collect(Collectors.toList());
    }
    @Override
    public Map<Long, List<StandingbookAttributeDO>> getSbAttrBySbCode(List<String> codes) {
        List<StandingbookAttributeDO> attrs = standingbookAttributeMapper.selectList(new LambdaQueryWrapper<StandingbookAttributeDO>()
                .in(StandingbookAttributeDO::getCode, ATTR_EQUIPMENT_ID, ATTR_MEASURING_INSTRUMENT_ID)
                .in(StandingbookAttributeDO::getValue, codes));
        if (CollUtil.isEmpty(attrs)) {
            return null;
        }
        return attrs.stream()
                .collect(Collectors.groupingBy(StandingbookAttributeDO::getStandingbookId));
    }

    @Override
    public Map<Long, List<StandingbookAttributeDO>> getAttributesBySbIds(List<Long> sbIds) {
        List<StandingbookAttributeDO> attrs = standingbookAttributeMapper.selectList(new LambdaQueryWrapper<StandingbookAttributeDO>().in(StandingbookAttributeDO::getStandingbookId, sbIds));

        if (CollUtil.isEmpty(attrs)) {
            throw exception(STANDINGBOOK_NO_ATTR);
        }

        return attrs.stream()
                .collect(Collectors.groupingBy(StandingbookAttributeDO::getStandingbookId));
    }


}
