package cn.bitlinks.ems.module.power.service.standingbook.type;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.type.vo.StandingbookTypeListReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.type.vo.StandingbookTypeSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.attribute.StandingbookAttributeMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.templ.StandingbookTmplDaqAttrMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.type.StandingbookTypeMapper;
import cn.bitlinks.ems.module.power.enums.ApiConstants;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ApiConstants.PARENT_ATTR_AUTO;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

/**
 * 台账类型 Service 实现类
 *
 * @author bitlinks
 */
@Slf4j
@Service
@Validated
public class StandingbookTypeServiceImpl implements StandingbookTypeService {
    @Resource
    private StandingbookTypeMapper standingbookTypeMapper;
    @Resource
    private StandingbookService standingbookService;
    @Resource
    private StandingbookAttributeMapper standingbookAttributeMapper;
    @Resource
    private StandingbookTmplDaqAttrMapper standingbookTmplDaqAttrMapper;

    @Transactional
    @Override
    public Long createStandingbookType(StandingbookTypeSaveReqVO createReqVO) {
        // 校验父级类型编号的有效性
        validateParentStandingbookType(null, createReqVO.getSuperId());

        // 校验允许最多五层节点
        validateOnlyFive(createReqVO.getSuperId());
        // 插入
        StandingbookTypeDO standingbookType = BeanUtils.toBean(createReqVO, StandingbookTypeDO.class);
        standingbookTypeMapper.insert(standingbookType);
        //创建分类的属性
        createTypeAttrFromParent(standingbookType);
        //创建分类的数采属性
        createTypeDaqAttrFromParent(standingbookType);
        //此处只对重点设备和计量器具进行属性固定的控制

        // 返回
        return standingbookType.getId();
    }

    @Transactional
    @Override
    public void updateStandingbookType(StandingbookTypeSaveReqVO updateReqVO) {
        // 校验存在
        validateStandingbookTypeExists(updateReqVO.getId());
        // 校验父级类型编号的有效性
        validateParentStandingbookType(updateReqVO.getId(), updateReqVO.getSuperId());
        // 校验名字的唯一性
        // 更新
        StandingbookTypeDO updateObj = BeanUtils.toBean(updateReqVO, StandingbookTypeDO.class);
        standingbookTypeMapper.updateById(updateObj);
    }

    void recursiveDeletion(List<Long> ids, Long id) {
        // 校验存在
        validateStandingbookTypeExists(id);
        // 校验是否有子台账类型 一并删了
        if (standingbookTypeMapper.selectCountBySuperId(id) > 0) {
            StandingbookTypeListReqVO listReqVO = new StandingbookTypeListReqVO();
            listReqVO.setSuperId(id);
            List<StandingbookTypeDO> standingbookTypeDOS = standingbookTypeMapper.selectList(listReqVO);
            if (standingbookTypeDOS != null && standingbookTypeDOS.size() > 0) {
                for (StandingbookTypeDO standingbookTypeDO : standingbookTypeDOS) {
                    recursiveDeletion(ids, standingbookTypeDO.getId());
                }
            }
        }
        List<StandingbookDO> standingbookDOS = standingbookService.getStandingbookList(new HashMap<String, String>() {{
            put("typeId", String.valueOf(id));
        }});
        if (standingbookDOS != null && standingbookDOS.size() > 0) {
            throw exception(STANDINGBOOK_EXISTS);
        }
        ids.add(id);
    }

    @Override
    public void deleteStandingbookType(Long id) {
        ArrayList<Long> ids = new ArrayList<>();
        recursiveDeletion(ids, id);
        // 删除
        standingbookTypeMapper.deleteByIds(ids);
        standingbookAttributeMapper.delete(new LambdaQueryWrapper<StandingbookAttributeDO>().in(StandingbookAttributeDO::getTypeId, ids));
        standingbookTmplDaqAttrMapper.delete(new LambdaQueryWrapper<StandingbookTmplDaqAttrDO>().in(StandingbookTmplDaqAttrDO::getTypeId, ids));

    }

    private void validateStandingbookTypeExists(Long id) {
        if (standingbookTypeMapper.selectById(id) == null) {
            throw exception(STANDINGBOOK_TYPE_NOT_EXISTS);
        }
    }

    private void validateParentStandingbookType(Long id, Long superId) {
        if (superId == null || StandingbookTypeDO.SUPER_ID_ROOT.equals(superId)) {
            return;
        }
        // 1. 不能设置自己为父台账类型
        if (Objects.equals(id, superId)) {
            throw exception(STANDINGBOOK_TYPE_PARENT_ERROR);
        }
        // 2. 父台账类型不存在
        StandingbookTypeDO parentStandingbookType = standingbookTypeMapper.selectById(superId);
        if (parentStandingbookType == null) {
            throw exception(STANDINGBOOK_TYPE_PARENT_NOT_EXITS);
        }
        // 3. 递归校验父台账类型，如果父台账类型是自己的子台账类型，则报错，避免形成环路
        if (id == null) { // id 为空，说明新增，不需要考虑环路
            return;
        }
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            // 3.1 校验环路
            superId = parentStandingbookType.getSuperId();
            if (Objects.equals(id, superId)) {
                throw exception(STANDINGBOOK_TYPE_PARENT_IS_CHILD);
            }
            // 3.2 继续递归下一级父台账类型
            if (superId == null || StandingbookTypeDO.SUPER_ID_ROOT.equals(superId)) {
                break;
            }
            parentStandingbookType = standingbookTypeMapper.selectById(superId);
            if (parentStandingbookType == null) {
                break;
            }
        }
    }

    private void validateOnlyFive(Long superId) {

        if (Objects.isNull(superId)) {
            return;
        }

        for (int i = 0; i < Short.MAX_VALUE; i++) {
            StandingbookTypeDO standingbookTypeDO = standingbookTypeMapper.selectById(superId);
            if (standingbookTypeDO == null) {
                break;
            }

            // 允许最多五层节点
            if (i > 3) {
                throw exception(STANDINGBOOK_TYPE_ONLY_FIVE);
            }
            superId = standingbookTypeDO.getSuperId();
        }
    }

    @Override
    public StandingbookTypeDO getStandingbookType(Long id) {
        return standingbookTypeMapper.selectById(id);
    }


    @Override
    public List<StandingbookTypeDO> getStandingbookType(String name) {
        return standingbookTypeMapper.selectByName(name);
    }

    @Override
    public List<StandingbookTypeDO> getStandingbookTypeList(StandingbookTypeListReqVO listReqVO) {
        return standingbookTypeMapper.selectList(listReqVO);
    }

    @Override
    public List<StandingbookTypeDO> getStandingbookTypeNode() {
        List<StandingbookTypeDO> nodes = standingbookTypeMapper.selectNotDelete();

        // 构建树形结构
        Map<Long, StandingbookTypeDO> nodeMap = new HashMap<>();
        Queue<StandingbookTypeDO> queue = new LinkedList<>();

        for (StandingbookTypeDO node : nodes) {
            nodeMap.put(node.getId(), node);
            if (node.getSuperId() == null) {
                queue.add(node);
            } else {
                StandingbookTypeDO parent = nodeMap.get(node.getSuperId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }

        // 通过队列构建树形结构
        List<StandingbookTypeDO> rootNodes = new ArrayList<>();
        while (!queue.isEmpty()) {
            StandingbookTypeDO currentNode = queue.poll();
            rootNodes.add(currentNode);
        }
        return rootNodes;
    }

    @Override
    public Map<Long, StandingbookTypeDO> getStandingbookTypeIdMap(List<Long> typeIds) {
        List<StandingbookTypeDO> allList;
        if (CollUtil.isEmpty(typeIds)) {
            allList = standingbookTypeMapper.selectList();
        } else {
            allList = standingbookTypeMapper.selectList(new LambdaQueryWrapper<StandingbookTypeDO>().in(StandingbookTypeDO::getId, typeIds));
        }

        if (CollUtil.isEmpty(allList)) {
            return new HashMap<>();
        }
        return allList.stream()
                .collect(Collectors.toMap(StandingbookTypeDO::getId, type -> type));
    }

    /**
     * 继承数采属性
     *
     * @param standingbookTypeDO
     */
    private void createTypeDaqAttrFromParent(StandingbookTypeDO standingbookTypeDO) {
        Long superId = standingbookTypeDO.getSuperId();
        List<StandingbookTmplDaqAttrDO> superDaqAttributes =
                standingbookTmplDaqAttrMapper.selectList(StandingbookTmplDaqAttrDO::getTypeId, superId);
        if (CollUtil.isEmpty(superDaqAttributes)) {
            return;
        }
        superDaqAttributes.forEach(attr -> {
            attr.setTypeId(standingbookTypeDO.getId())
                    .setAutoGenerated(ApiConstants.YES)
                    .setRawAttrId(attr.getRawAttrId() == null ? attr.getId() : attr.getRawAttrId());
            attr.setId(null);
        });
        standingbookTmplDaqAttrMapper.insertBatch(superDaqAttributes);
    }

    /**
     * 继承基础参数
     *
     * @param standingbookTypeDO
     */
    private void createTypeAttrFromParent(StandingbookTypeDO standingbookTypeDO) {
        Long superId = standingbookTypeDO.getSuperId();
        List<StandingbookAttributeDO> superAttributes = standingbookAttributeMapper.selectTypeId(superId);
        if (CollUtil.isEmpty(superAttributes)) {
            return;
        }
        superAttributes.forEach(attr -> {
            attr.setTypeId(standingbookTypeDO.getId())
                    .setAutoGenerated(ApiConstants.YES)
                    .setDescription(PARENT_ATTR_AUTO)
                    .setRawAttrId(attr.getRawAttrId() == null ? attr.getId() : attr.getRawAttrId());
            ;
            attr.setId(null);
        });
        standingbookAttributeMapper.insertBatch(superAttributes);
    }

    /**
     * 递归查询子节点 id
     */
    @Override
    public List<Long> getSubtreeIds(List<StandingbookTypeDO> typeList, Long targetId) {
        List<Long> subtreeIds = new ArrayList<>();

        StandingbookTypeDO node = findNode(typeList, targetId);
        getSubtreeIdsRecursive(node, subtreeIds);

        return subtreeIds;
    }

    // 树状结构中查询当前节点
    private StandingbookTypeDO findNode(List<StandingbookTypeDO> typeList, Long targetId) {
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
    private void getSubtreeIdsRecursive(StandingbookTypeDO node, List<Long> subtreeIds) {
        if (node == null) {
            return;
        }
        subtreeIds.add(node.getId()); // 添加当前节点 id

        for (StandingbookTypeDO child : node.getChildren()) {
            getSubtreeIdsRecursive(child, subtreeIds); // 递归遍历子节点
        }
    }
}
