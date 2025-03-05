package cn.bitlinks.ems.module.power.service.standingbook.attribute;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.AttributeTreeNode;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributePageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.StandingbookMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.attribute.StandingbookAttributeMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.type.StandingbookTypeMapper;
import cn.bitlinks.ems.module.power.enums.ApiConstants;
import cn.bitlinks.ems.module.power.enums.standingbook.AttributeTreeNodeTypeEnum;
import cn.bitlinks.ems.module.power.service.standingbook.type.StandingbookTypeService;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ApiConstants.PARENT_ATTR_AUTO;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.STANDINGBOOK_ATTRIBUTE_NOT_EXISTS;

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
    private StandingbookTypeMapper standingbookTypeMapper;
    @Resource
    private StandingbookMapper standingbookMapper;

    @Resource
    @Lazy
    private StandingbookTypeService standingbookTypeService;

    @Transactional
    @Override
    public Long createStandingbookAttribute(StandingbookAttributeSaveReqVO createReqVO) {
        // 插入
        StandingbookAttributeDO standingbookAttribute = BeanUtils.toBean(createReqVO, StandingbookAttributeDO.class);
        if ((standingbookAttribute.getId() == null)) {
            standingbookAttributeMapper.insert(standingbookAttribute);
        }else {
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
    public List<StandingbookDO> getStandingbook(List<StandingbookAttributePageReqVO> children, Long typeId) {
        return standingbookAttributeMapper.selectStandingbook(children, typeId);
    }

    @Override
    public List<StandingbookDO> getStandingbookIntersection(List<StandingbookAttributePageReqVO> children, Long typeId) {
        return standingbookAttributeMapper.selectStandingbookIntersection(children, typeId);
    }

    @Override
    @Transactional
    public void saveMultiple(List<StandingbookAttributeSaveReqVO> createReqVOs) {
        Long typeId = createReqVOs.get(0).getTypeId();
        List<StandingbookAttributeDO> optAttrList = BeanUtils.toBean(createReqVOs, StandingbookAttributeDO.class);

        // 找出当前属性列表
        List<StandingbookAttributeDO> rawAttrList = getStandingbookAttributeByTypeId(typeId);
        // 级联删除
        deleteAttrCascade(optAttrList, rawAttrList);
        // 将数据分为2组，分成 createList 和 updateList
        // 进行级联创建操作
        List<StandingbookAttributeDO> createList = optAttrList.stream()
                .filter(attribute -> attribute.getId() == null)
                .collect(Collectors.toList());
        createAttrCascade(createList, typeId);
        // 进行级联修改操作
        List<StandingbookAttributeDO> updateList = optAttrList.stream()
                .filter(attribute -> attribute.getId() != null)
                .collect(Collectors.toList());
        updateAttrCascade(updateList);

    }

    /**
     * 级联修改属性列表
     * @param updateList 修改属性列表
     */
    private void updateAttrCascade(List<StandingbookAttributeDO> updateList) {
        if(CollUtil.isEmpty(updateList)){
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
                .in(StandingbookAttributeDO::getRawAttrId,updIds));
        // 2.2 根据 rawAttrId 分组
        Map<Long, List<StandingbookAttributeDO>> groupedByRawAttrId = attrList.stream()
                .collect(Collectors.groupingBy(StandingbookAttributeDO::getRawAttrId));
        groupedByRawAttrId.entrySet().removeIf(entry -> CollUtil.isEmpty(entry.getValue()));

        // 2.3 级联修改操作节点的属性，组成List
        List<StandingbookAttributeDO> cascadeAttrList = new ArrayList<>();
        groupedByRawAttrId.forEach((rawAttrId,updAttrList) -> {
            // 处理新增的属性列表
            Optional<StandingbookAttributeDO> foundAttribute = updateList.stream().filter(updAttr -> updAttr.getId().equals(rawAttrId)).findFirst();
            if (foundAttribute.isPresent()) {
                StandingbookAttributeDO updAttribute = foundAttribute.get();
                updAttrList.forEach(attribute -> {
                    attribute.setName(updAttribute.getName())
                            .setIsRequired(updAttribute.getIsRequired())
                            .setFormat(updAttribute.getFormat());
                });
                cascadeAttrList.addAll(updAttrList);
            }
        });
        if(CollUtil.isEmpty(cascadeAttrList)){
            return;
        }
        // 2.4 执行修改级联节点属性操作
        standingbookAttributeMapper.updateBatch(cascadeAttrList);
    }

    /**
     * 级联新增属性列表
     * @param createList 新增属性列表
     * @param typeId 台账类型id
     */
    private void createAttrCascade(List<StandingbookAttributeDO> createList, Long typeId) {
        if(CollUtil.isEmpty(createList)){
            return;
        }
        // 1.新增当前分类的属性
        standingbookAttributeMapper.insertBatch(createList);
        // 2.获取所有级联的typeId
        List<StandingbookTypeDO> typeList = standingbookTypeService.getStandingbookTypeNode();
        List<Long> subtreeIds = getSubtreeIds(typeList, typeId);

        if(CollUtil.isEmpty(subtreeIds)){
            return;
        }
        // 3.新增所有级联的typeId的属性
        List<StandingbookAttributeDO> cascadeAttrList = new ArrayList<>();
        subtreeIds.forEach(subId->{
            // 处理新增的属性列表
            List<StandingbookAttributeDO> subAttrList = new ArrayList<>();
            createList.forEach(attribute -> {
                attribute.setDescription(PARENT_ATTR_AUTO)
                        .setAutoGenerated(ApiConstants.YES)
                        .setTypeId(subId)
                        .setRawAttrId(attribute.getId());
                subAttrList.add(attribute);
            });
            cascadeAttrList.addAll(subAttrList);
        });
        // 4.执行新增操作
        standingbookAttributeMapper.insertBatch(cascadeAttrList);

    }
    // 递归查询子节点 id
    public static List<Long> getSubtreeIds(List<StandingbookTypeDO> typeList, Long targetId) {
        List<Long> subtreeIds = new ArrayList<>();

        for (StandingbookTypeDO node : typeList) {
            if (node.getId().equals(targetId)) {
                // 找到了目标节点，开始递归遍历子节点
                getSubtreeIdsRecursive(node, subtreeIds);
                break; // 找到目标节点后就可以结束循环了
            }
        }

        return subtreeIds;
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
     * @param optAttrList 新属性列表
     * @param rawAttrList 原属性列表
     */
    private void deleteAttrCascade(List<StandingbookAttributeDO> optAttrList, List<StandingbookAttributeDO> rawAttrList) {
        if(CollUtil.isEmpty(rawAttrList)){
            return;
        }
        // 找出被删除的属性id集合
        Set<Long> deleteIds = rawAttrList.stream()
                .map(StandingbookAttributeDO::getId)
                .filter(id -> optAttrList.stream().noneMatch(reqVO -> reqVO.getId().equals(id)))
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(deleteIds)) {
            return;
        }
        // 删除操作节点属性
        standingbookAttributeMapper.deleteByIds(deleteIds);
        // 删除关联的节点属性
        standingbookAttributeMapper.delete(StandingbookAttributeDO::getRawAttrId,deleteIds);
    }

    @Override
    public List<AttributeTreeNode> queryAttributeTreeNodeByTypeAndSb(List<Long> standingBookIds, List<Long> typeIds) {

        //包含了台账类型节点和台账节点+叶子节点（台账属性），需要再次处理成树形结构，因为台账节点的父节点可能会和台账类型一致，
        List<AttributeTreeNode> centerNodeList = new ArrayList<>();

        //一、台账类型ids
        if (CollUtil.isNotEmpty(typeIds)) {
            // 查询直接关联的台账属性表，返回叶子节点，
            List<AttributeTreeNode> sbTypeAttrNodeList = getAttrNodeListByParent(typeIds, AttributeTreeNodeTypeEnum.SB_TYPE);
            if (CollUtil.isNotEmpty(sbTypeAttrNodeList)) {
                //1. 直接查询台账类型
                List<StandingbookTypeDO> standingBookTypeDOS = standingbookTypeMapper.selectBatchIds(typeIds);
                //1.1 转格式，组成台账节点（叶子节点的上一级）
                List<AttributeTreeNode> sbTypeNodeList = standingBookTypeDOS.stream()
                        .map(bookType -> new AttributeTreeNode(
                                bookType.getSuperId(),
                                bookType.getId(),
                                bookType.getName(),
                                AttributeTreeNodeTypeEnum.SB_TYPE.getCode(),
                                null)
                        )
                        .collect(Collectors.toList());
                //把叶子节点根据pId放到台账节点中
                sbTypeNodeList.forEach(node -> {
                    node.setChildren(sbTypeAttrNodeList.stream().filter(attrNode -> attrNode.getPId().equals(node.getId())).collect(Collectors.toList()));
                });
                sbTypeNodeList.removeIf(node -> node.getChildren().isEmpty());
                centerNodeList.addAll(sbTypeNodeList);
            }
        }
        //二、台账ids
        if (CollUtil.isNotEmpty(standingBookIds)) {
            // 查询直接关联的台账属性表，返回叶子节点，
            List<AttributeTreeNode> sbAttrNodeList = getAttrNodeListByParent(standingBookIds, AttributeTreeNodeTypeEnum.SB);
            if (CollUtil.isNotEmpty(sbAttrNodeList)) {
                //1. 直接查询台账
                List<StandingbookDO> standingBookDOS = standingbookMapper.selectBatchIds(standingBookIds);
                //1.1 转格式，组成台账节点（叶子节点的上一级）
                List<AttributeTreeNode> sbNodeList = standingBookDOS.stream()
                        .map(bookType -> new AttributeTreeNode(
                                bookType.getTypeId(),
                                bookType.getId(),
                                bookType.getName(),
                                AttributeTreeNodeTypeEnum.SB.getCode(),
                                null)
                        )
                        .collect(Collectors.toList());
                //把叶子节点根据pId放到台账节点中
                sbNodeList.forEach(node -> {
                    node.setChildren(sbAttrNodeList.stream().filter(attrNode -> attrNode.getPId().equals(node.getId())).collect(Collectors.toList()));
                });
                sbNodeList.removeIf(node -> node.getChildren().isEmpty());
                centerNodeList.addAll(sbNodeList);
            }
        }
        //所有的台账分类
        List<StandingbookTypeDO> sbTypeAllList =  standingbookTypeMapper.selectList();
        //循环中间节点，根据每个节点的pId在standingBookTypeMapper中查询出节点，直到节点的pId为null，构造树形结构
        enhanceTree(centerNodeList,sbTypeAllList);
        return centerNodeList;
    }

    /**
     * 根据父节点ids获取台账属性节点
     *
     * @param pIds                      台账属性的上一级节点
     * @param attributeTreeNodeTypeEnum 父节点类型
     * @return 台账属性节点 可能为null
     */
    private List<AttributeTreeNode> getAttrNodeListByParent(List<Long> pIds, AttributeTreeNodeTypeEnum attributeTreeNodeTypeEnum) {
        //1. 构造查询条件
        LambdaQueryWrapper<StandingbookAttributeDO> queryWrapper = new LambdaQueryWrapper<>();
        if (AttributeTreeNodeTypeEnum.SB_TYPE.equals(attributeTreeNodeTypeEnum)) {
            queryWrapper.in(StandingbookAttributeDO::getTypeId, pIds)
                    .isNull(StandingbookAttributeDO::getStandingbookId);
        } else if (AttributeTreeNodeTypeEnum.SB.equals(attributeTreeNodeTypeEnum)) {
            queryWrapper.in(StandingbookAttributeDO::getStandingbookId, pIds)
                    .isNotNull(StandingbookAttributeDO::getStandingbookId);
        } else {
            return null;
        }

        //2. 查询台账类型下直接关联的台账属性
        List<StandingbookAttributeDO> bookAttrDOs = standingbookAttributeMapper.selectList(queryWrapper);
        if (CollUtil.isEmpty(bookAttrDOs)) {
            return null;
        }
        //3. 转格式，组成台账属性节点（叶子节点）
        return bookAttrDOs.stream()
                .map(attributeDO ->
                        new AttributeTreeNode(
                                AttributeTreeNodeTypeEnum.SB.equals(attributeTreeNodeTypeEnum) ? attributeDO.getStandingbookId() : attributeDO.getTypeId(),
                                attributeDO.getId(),
                                attributeDO.getName(),
                                AttributeTreeNodeTypeEnum.ATTR.getCode(),
                                null)

                )
                .collect(Collectors.toList());
    }

    /**
     * 合并节点+> 构造树形结构
     * @param centerList 中间树形结构
     * @param sourceList 所有台账分类列表
     */
    private void enhanceTree(List<AttributeTreeNode> centerList, List<StandingbookTypeDO> sourceList) {
            // 1. 将 sourceList 转换为 Map
            Map<Long, StandingbookTypeDO> sourceMap = new HashMap<>();
            for (StandingbookTypeDO node : sourceList) {
                sourceMap.put(node.getId(), node);
            }

            // 2. 使用 Map 存储所有节点，避免重复
            Map<Long, AttributeTreeNode> nodeMap = new HashMap<>();

            // 3. 只处理 centerList 中的顶级节点，不考虑 children
            for (AttributeTreeNode topNode : centerList) {
                // 获取或创建当前节点，不复制 children
                AttributeTreeNode currentNode = nodeMap.computeIfAbsent(topNode.getId(),
                        k -> new AttributeTreeNode( topNode.getPId(),topNode.getId(), topNode.getName(), topNode.getType(), topNode.getChildren()));

                // 向上查找父节点
                Long parentId = topNode.getPId();
                AttributeTreeNode lastNode = currentNode;

                while (parentId != null && sourceMap.containsKey(parentId)) {
                    StandingbookTypeDO sourceNode = sourceMap.get(parentId);
                    // 获取或创建父节点
                    AttributeTreeNode pNode = nodeMap.computeIfAbsent(sourceNode.getId(),
                            k -> new AttributeTreeNode(sourceNode.getSuperId(),sourceNode.getId(), sourceNode.getName(), AttributeTreeNodeTypeEnum.SB_TYPE.getCode(), new ArrayList<>()));

                    // 如果父节点的 children 中还没有当前节点，则添加
                    if (!pNode.getChildren().contains(lastNode)) {
                        pNode.getChildren().add(lastNode);
                    }

                    lastNode = pNode;
                    parentId = sourceNode.getSuperId();
                }
            }

            // 4. 找到所有根节点（pId 为 null 的节点）
            centerList.clear();
            for (AttributeTreeNode node : nodeMap.values()) {
                if (node.getPId() == null) {
                    centerList.add(node);
                }
            }
        }

}
