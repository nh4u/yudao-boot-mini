package cn.bitlinks.ems.module.power.service.standingbook.type;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.type.vo.StandingbookTypeListReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.type.vo.StandingbookTypeSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.type.StandingbookTypeMapper;
import cn.bitlinks.ems.module.power.enums.ApiConstants;
import cn.bitlinks.ems.module.power.service.standingbook.attribute.StandingbookAttributeService;
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

    @Transactional
    @Override
    public Long createStandingbookType(StandingbookTypeSaveReqVO createReqVO) {
        // 校验父级类型编号的有效性
        validateParentStandingbookType(null, createReqVO.getSuperId());
        // 校验名字的唯一性
        validateStandingbookTypeNameUnique(null, createReqVO.getSuperId(), createReqVO.getName());

        // 插入
        StandingbookTypeDO standingbookType = BeanUtils.toBean(createReqVO, StandingbookTypeDO.class);
        standingbookTypeMapper.insert(standingbookType);
        //此处只对重点设备和计量器具进行属性固定的控制
        if (createReqVO.getTopType().equals("1")) {
            // 重点设备
            createKeyEquipment(standingbookType);
        } else if (createReqVO.getTopType().equals("2")) {
            // 计量器具
            createMeasuringInstrument(standingbookType);
        }
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
        validateStandingbookTypeNameUnique(updateReqVO.getId(), updateReqVO.getSuperId(), updateReqVO.getName());

        // 更新
        StandingbookTypeDO updateObj = BeanUtils.toBean(updateReqVO, StandingbookTypeDO.class);
        standingbookTypeMapper.updateById(updateObj);
    }

    @Override
    public void deleteStandingbookType(Long id) {
        // 校验存在
        validateStandingbookTypeExists(id);
        // 校验是否有子台账类型
        if (standingbookTypeMapper.selectCountBySuperId(id) > 0) {
            throw exception(STANDINGBOOK_TYPE_EXITS_CHILDREN);
        }
        // 删除
        standingbookTypeMapper.deleteById(id);
        try {
            attributeService.deleteStandingbookAttribute(id);
        } catch (Exception e) {
           e.printStackTrace();
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

    private void validateStandingbookTypeNameUnique(Long id, Long superId, String name) {
        StandingbookTypeDO standingbookType = standingbookTypeMapper.selectBySuperIdAndName(superId, name);
        if (standingbookType == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的台账类型
        if (id == null) {
            throw exception(STANDINGBOOK_TYPE_NAME_DUPLICATE);
        }
        if (!Objects.equals(standingbookType.getId(), id)) {
            throw exception(STANDINGBOOK_TYPE_NAME_DUPLICATE);
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

    void createMeasuringInstrument(StandingbookTypeDO standingbookTypeDO) {
        StandingbookAttributeDO do1 = new StandingbookAttributeDO();
        do1.setDescription("系统生成：表类型")
                .setName("表类型")
                .setCode("tableType")
                .setFormat(ApiConstants.SELECT).setOptions("实体表计;虚拟表计")
                .setIsRequired(ApiConstants.YES)
                .setNode("计量器具根节点")
                .setTypeId(standingbookTypeDO.getId())
                .setSort(1L);
        StandingbookAttributeDO do2 = new StandingbookAttributeDO();
        do2.setDescription("系统生成：计量器具编号")
                .setName("计量器具编号")
                .setCode("measuringInstrumentId")
                .setFormat(ApiConstants.TEXT)
                .setIsRequired(ApiConstants.YES)
                .setNode("计量器具根节点")
                .setTypeId(standingbookTypeDO.getId())
                .setSort(2L);
        StandingbookAttributeDO do3 = new StandingbookAttributeDO();
        do3.setDescription("系统生成：计量器具名称")
                .setName("计量器具名称")
                .setCode("measuringInstrumentName")
                .setFormat(ApiConstants.TEXT)
                .setIsRequired(ApiConstants.YES)
                .setNode("计量器具根节点")
                .setTypeId(standingbookTypeDO.getId())
                .setSort(3L);
        StandingbookAttributeDO do4 = new StandingbookAttributeDO();
        do4.setDescription("系统生成：能源")
                .setName("能源")
                .setCode("energy")
                .setFormat(ApiConstants.SELECT).setOptions("energy")
                .setIsRequired(ApiConstants.YES)
                .setNode("计量器具根节点")
                .setTypeId(standingbookTypeDO.getId())
                .setSort(4L);
        StandingbookAttributeDO do5 = new StandingbookAttributeDO();
        do5.setDescription("系统生成：数值类型")
                .setName("数值类型")
                .setCode("valueType")
                .setFormat(ApiConstants.SELECT).setOptions("抄表数;用量数")
                .setIsRequired(ApiConstants.YES)
                .setNode("计量器具根节点")
                .setTypeId(standingbookTypeDO.getId())
                .setSort(5L);
        ArrayList<StandingbookAttributeDO> standingbookAttributeDOS = new ArrayList<StandingbookAttributeDO>() {{
            add(do1);
            add(do2);
            add(do3);
            add(do4);
            add(do5);
        }};
        attributeService.createStandingbookAttributeBatch(standingbookAttributeDOS);
    }

    void createKeyEquipment(StandingbookTypeDO standingbookTypeDO) {
        StandingbookAttributeDO do1 = new StandingbookAttributeDO();
        do1.setDescription("系统生成：设备编号")
                .setName("设备编号")
                .setCode("equipmentId")
                .setFormat(ApiConstants.TEXT)
                .setIsRequired(ApiConstants.YES)
                .setNode("重点设备根节点")
                .setTypeId(standingbookTypeDO.getId())
                .setSort(1L);
        StandingbookAttributeDO do2 = new StandingbookAttributeDO();
        do2.setDescription("系统生成：设备名称")
                .setName("设备名称")
                .setCode("equipmentName")
                .setFormat(ApiConstants.TEXT)
                .setIsRequired(ApiConstants.YES)
                .setNode("重点设备根节点")
                .setTypeId(standingbookTypeDO.getId())
                .setSort(2L);
        ArrayList<StandingbookAttributeDO> standingbookAttributeDOS = new ArrayList<StandingbookAttributeDO>() {{
            add(do1);
            add(do2);
        }};
        attributeService.createStandingbookAttributeBatch(standingbookAttributeDOS);
    }

}
