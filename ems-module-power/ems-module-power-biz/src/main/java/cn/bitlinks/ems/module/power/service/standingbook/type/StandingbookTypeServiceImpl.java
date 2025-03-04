package cn.bitlinks.ems.module.power.service.standingbook.type;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.type.vo.StandingbookTypeListReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.type.vo.StandingbookTypeSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.attribute.StandingbookAttributeMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.type.StandingbookTypeMapper;
import cn.bitlinks.ems.module.power.enums.ApiConstants;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.bitlinks.ems.module.power.service.standingbook.attribute.StandingbookAttributeService;
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
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
    private StandingbookAttributeService attributeService;
    @Resource
    private StandingbookTypeMapper standingbookTypeMapper;
    @Resource
    private StandingbookService standingbookService;
    @Resource
    private StandingbookAttributeMapper standingbookAttributeMapper;

    @Transactional
    @Override
    public Long createStandingbookType(StandingbookTypeSaveReqVO createReqVO) {
        // 校验父级类型编号的有效性
        validateParentStandingbookType(null, createReqVO.getSuperId());
        // 校验名字的唯一性
//        validateStandingbookTypeNameUnique(null, createReqVO.getSuperId(), createReqVO.getName());

        // 插入
        StandingbookTypeDO standingbookType = BeanUtils.toBean(createReqVO, StandingbookTypeDO.class);
        standingbookTypeMapper.insert(standingbookType);
        //创建分类的属性
        createTypeAttrFromParent(standingbookType);
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
//        validateStandingbookTypeNameUnique(updateReqVO.getId(), updateReqVO.getSuperId(), updateReqVO.getName());

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
        for (Long aLong : ids) {
            // 删除
            standingbookTypeMapper.deleteById(aLong);
            try {
                attributeService.deleteStandingbookAttributeByTypeId(aLong);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

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


    @Override
    public StandingbookTypeDO getStandingbookType(Long id) {
        return standingbookTypeMapper.selectById(id);
    }


    @Override
    public List<StandingbookTypeDO> getStandingbookType(String name) {
        return standingbookTypeMapper.selectByName(name);
    }
    // Recursive method to find nodes by fuzzy name matching

    @Override
    public List<StandingbookTypeDO> getStandingbookTypeList(StandingbookTypeListReqVO listReqVO) {
        return standingbookTypeMapper.selectList(listReqVO);
    }

    @Override
    public List<StandingbookTypeDO> getStandingbookTypeNode() {
        List<StandingbookTypeDO> nodes = fetchNodesFromDatabase();

        // 将rootNodes序列化为JSON并发送给前端
//        log.info("StandingbookTypeNode: " + JSONUtil.toJsonStr(rootNodes));
        return createStandingbookTypeTreeNode(nodes);
    }

    private List<StandingbookTypeDO> fetchNodesFromDatabase() {
        return standingbookTypeMapper.selectNotDelete();
    }

    List<StandingbookTypeDO> createStandingbookTypeTreeNode(List<StandingbookTypeDO> nodes) {
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


    @Transactional(rollbackFor = Exception.class)
    public void createTypeAttrFromParent(StandingbookTypeDO standingbookTypeDO) {
        Long superId = standingbookTypeDO.getSuperId();
        List<StandingbookAttributeDO> superAttributes = standingbookAttributeMapper.selectTypeId(superId);
        if (CollUtil.isEmpty(superAttributes)) {
            return;
        }
        superAttributes.forEach(attr -> {
            attr.setTypeId(standingbookTypeDO.getId())
                    .setAutoGenerated(ApiConstants.YES)
                    .setDescription("父节点属性自动生成")
                    .setRawAttrId(attr.getRawAttrId() == null ?attr.getId(): attr.getRawAttrId());
            ;
            attr.setId(null);
        });
        attributeService.createStandingbookAttributeBatch(superAttributes);
    }

}
