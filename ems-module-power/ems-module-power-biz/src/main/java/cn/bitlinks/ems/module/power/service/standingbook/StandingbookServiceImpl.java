package cn.bitlinks.ems.module.power.service.standingbook;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.AssociationData;
import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.StandingbookWithAssociations;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookAssociationReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookRespVO;
import cn.bitlinks.ems.module.power.dal.dataobject.measurementassociation.MeasurementAssociationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.measurementdevice.MeasurementDeviceDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookLabelInfoDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import cn.bitlinks.ems.module.power.dal.mysql.measurementassociation.MeasurementAssociationMapper;
import cn.bitlinks.ems.module.power.dal.mysql.measurementdevice.MeasurementDeviceMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.StandingbookLabelInfoMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.StandingbookMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.attribute.StandingbookAttributeMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.templ.StandingbookTmplDaqAttrMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.type.StandingbookTypeMapper;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.enums.ErrorCodeConstants;
import cn.bitlinks.ems.module.power.enums.standingbook.StandingbookTypeTopEnum;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.bitlinks.ems.module.power.service.standingbook.attribute.StandingbookAttributeService;
import cn.bitlinks.ems.module.power.service.standingbook.tmpl.StandingbookTmplDaqAttrService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ApiConstants.*;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.STANDINGBOOK_NOT_EXISTS;

/**
 * 台账属性 Service 实现类
 *
 * @author bitlinks
 */
@Service("standingbookService")
@Validated
public class StandingbookServiceImpl implements StandingbookService {

    @Resource
    private StandingbookMapper standingbookMapper;
    @Resource
    private StandingbookLabelInfoMapper standingbookLabelInfoMapper;
    @Resource
    private StandingbookAttributeService standingbookAttributeService;


    @Resource
    private StandingbookTypeMapper standingbookTypeMapper;
    @Resource
    private  StandingbookTmplDaqAttrMapper standingbookTmplDaqAttrMapper;
    @Resource
    private StandingbookAttributeMapper standingbookAttributeMapper;
    @Resource
    private MeasurementDeviceMapper measurementDeviceMapper;
    @Resource
    private MeasurementAssociationMapper measurementAssociationMapper;


    @Override
    public Long count(Long typeId) {

        // 对应类型【重点设备、计量器具、其他设备】下的所有类型id
        List<Long> typeIdList;
        if (typeId.equals(CommonConstants.OTHER_EQUIPMENT_ID)) {
            // 查询对应所有类型id
            typeIdList = standingbookTypeMapper.selectList(new LambdaQueryWrapperX<StandingbookTypeDO>().ne(StandingbookTypeDO::getTopType, CommonConstants.MEASUREMENT_INSTRUMENT_ID).ne(StandingbookTypeDO::getTopType, CommonConstants.KEY_EQUIPMENT_ID)).stream().map(StandingbookTypeDO::getId).collect(Collectors.toList());
        } else {

            // 差对应id
            typeIdList = standingbookTypeMapper.selectList(new LambdaQueryWrapperX<StandingbookTypeDO>().eq(StandingbookTypeDO::getTopType, typeId)).stream().map(StandingbookTypeDO::getId).collect(Collectors.toList());
        }
        return typeIdList.size() > 0 ? standingbookMapper.selectCount(new LambdaQueryWrapperX<StandingbookDO>().in(StandingbookDO::getTypeId, typeIdList)) : 0;
    }


    @Override
    public List<StandingbookRespVO> listSbAllWithAssociations(StandingbookAssociationReqVO reqVO) {

        Map<String, String> paramMap = reqVO.getPageReqVO();
        if (CollUtil.isEmpty(paramMap)) {
            paramMap = new HashMap<>();
        }
        paramMap.put(SB_TYPE_ATTR_TOP_TYPE, reqVO.getTopType() + "");

        Long sbId = reqVO.getSbId();

        List<StandingbookDO> sbList = getStandingbookList(paramMap);


        // 如果是重点设备的话，可以直接拉出来所有设备，任意关联
        // 如果是计量器具的话，需要根据已关联的计量器具进行筛选，筛选出该计量器具可关联的计量器具列表
        List<MeasurementAssociationDO> list = measurementAssociationMapper.selectList();
        Set<Long> parentIds = new HashSet<>(); // 使用 Set 去重
        findAllParentsRecursive(sbId, list, parentIds);
        parentIds.add(sbId);

        // 从 sbList 中筛选掉 id 在 已关联的parentIds 中的元素
        if (CollUtil.isEmpty(sbList)) {
            return new ArrayList<>();
        }

        List<StandingbookDO> standingbookDOS = sbList.stream().filter(standingbookDO -> !parentIds.contains(standingbookDO.getId())).collect(Collectors.toList());
        // 添加计量器具类型
        if (CollUtil.isEmpty(standingbookDOS)) {
            return new ArrayList<>();
        }
        // 查询所有台账分类id列表
        List<Long> sbTypeIds = standingbookDOS.stream().map(StandingbookDO::getTypeId).collect(Collectors.toList());
        List<StandingbookTypeDO> typeList = standingbookTypeMapper.selectList(new LambdaQueryWrapper<StandingbookTypeDO>().in(StandingbookTypeDO::getId, sbTypeIds));
        Map<Long, StandingbookTypeDO> typeMap = typeList.stream().collect(Collectors.toMap(StandingbookTypeDO::getId, standingbookTypeDO -> standingbookTypeDO));
        List<StandingbookRespVO> result = BeanUtils.toBean(standingbookDOS, StandingbookRespVO.class);
        result.forEach(sb -> {
            sb.setStandingbookTypeId(typeMap.get(sb.getTypeId()).getId());
            sb.setStandingbookTypeName(typeMap.get(sb.getTypeId()).getName());
        });

        return result;

    }

    /**
     * 递归查询出上级计量器具的id
     *
     * @param childId      当前计量器具id
     * @param associations 所有计量器具关联关系
     * @param parentIds    上级计量器具id集合
     */
    private static void findAllParentsRecursive(Long childId, List<MeasurementAssociationDO> associations, Set<Long> parentIds) {
        for (MeasurementAssociationDO association : associations) {
            if (association.getMeasurementId().equals(childId)) {
                Long parentId = association.getMeasurementInstrumentId();
                if (parentId != null && !parentIds.contains(parentId)) {
                    parentIds.add(parentId); // 添加父节点 ID
                    // 递归查找父节点的父节点
                    findAllParentsRecursive(parentId, associations, parentIds);
                }
            }
        }
    }

    @Override
    @Transactional
    public Long createStandingbook(Map<String, String> createReqVO) {
        // 插入
        if (!createReqVO.containsKey(ATTR_TYPE_ID)) {
            throw exception(ErrorCodeConstants.STANDINGBOOK_TYPE_NOT_EXISTS);
        }

        Long typeId = Long.valueOf(createReqVO.get(ATTR_TYPE_ID));
        StandingbookTypeDO sb = standingbookTypeMapper.selectById(typeId);
        if (sb == null) {
            throw exception(ErrorCodeConstants.STANDINGBOOK_TYPE_NOT_EXISTS);
        }
        // 判断设备编号/计量器具编号是否重复
        if (StandingbookTypeTopEnum.EQUIPMENT.getCode().equals(sb.getTopType())) {
            String attrEquipmentId = createReqVO.get(ATTR_EQUIPMENT_ID);
            validateSbCodeUnique(attrEquipmentId);
        } else if (StandingbookTypeTopEnum.MEASURING_INSTRUMENT.getCode().equals(sb.getTopType())) {
            String measuringInstrumentId = createReqVO.get(ATTR_MEASURING_INSTRUMENT_ID);
            validateSbCodeUnique(measuringInstrumentId);
        }
        // 新增
        StandingbookDO standingbook = new StandingbookDO();
        standingbook.setTypeId(typeId);
        standingbookMapper.insert(standingbook);
        // 新增标签信息
        createLabelInfoList(createReqVO.get(ATTR_LABEL_INFO), standingbook.getId());

        createReqVO.remove(ATTR_TYPE_ID);
        createReqVO.remove(ATTR_LABEL_INFO);

        List<StandingbookAttributeDO> children = new ArrayList<>();
        // 查询属性分类部分的关联属性
        List<StandingbookAttributeDO> standingbookAttributeByTypeId = standingbookAttributeService.getStandingbookAttributeByTypeId(typeId);
        createReqVO.forEach((key, value) -> {
            //根据code查询分类属性，找不到的话就直接抛出异常
            Optional<StandingbookAttributeDO> rawAttrOptional = standingbookAttributeByTypeId.stream().filter(standingbookAttributeDO -> key.equals(standingbookAttributeDO.getCode())).findFirst();
            if (!rawAttrOptional.isPresent()) {
                throw exception(ErrorCodeConstants.STANDINGBOOK_ATTRIBUTE_NOT_EXISTS);
            }
            StandingbookAttributeDO attribute = BeanUtils.toBean(rawAttrOptional.get(), StandingbookAttributeDO.class);
            attribute.setValue(value);
            attribute.setStandingbookId(standingbook.getId());
            attribute.setId(null);
            attribute.setCreateTime(null);
            attribute.setUpdateTime(null);
            children.add(attribute);
        });
        // 新增台账属性
        standingbookAttributeMapper.insertBatch(children);

        return standingbook.getId();
    }

    /**
     * 新增标签信息
     */
    private void createLabelInfoList(String labelInfoJson, Long standingbookId) {
        JSONObject labelInfoObj = JSONUtil.parseObj(labelInfoJson);
        if (!labelInfoObj.isEmpty()) {
            List<StandingbookLabelInfoDO> standingbookLabelInfoDOList = new ArrayList<>();
            labelInfoObj.forEach((key, value) -> {
                StandingbookLabelInfoDO standingbookLabelInfoDO = new StandingbookLabelInfoDO();
                standingbookLabelInfoDO.setStandingbookId(standingbookId);
                standingbookLabelInfoDO.setName(key);
                standingbookLabelInfoDO.setValue(value.toString());
                standingbookLabelInfoDOList.add(standingbookLabelInfoDO);
            });
            standingbookLabelInfoMapper.insertBatch(standingbookLabelInfoDOList);
        }
    }

    /**
     * 台账编码值
     *
     * @param codeValue
     */
    private void validateSbCodeUnique(String codeValue) {
        List<StandingbookAttributeDO> exists = standingbookAttributeMapper.selectList(new LambdaQueryWrapper<StandingbookAttributeDO>().eq(StandingbookAttributeDO::getValue, codeValue).in(StandingbookAttributeDO::getCode, Arrays.asList(ATTR_MEASURING_INSTRUMENT_ID, ATTR_EQUIPMENT_ID)));
        if (CollUtil.isNotEmpty(exists)) {
            throw exception(ErrorCodeConstants.STANDINGBOOK_CODE_EXISTS);
        }

    }

    @Override
    @Transactional
    public void updateStandingbook(Map<String, String> updateReqVO) {
        // 校验存在
        validateStandingbookExists(Long.valueOf(updateReqVO.get("id")));
        // 更新
        StandingbookDO standingbook = new StandingbookDO();
        standingbook.setTypeId(Long.valueOf(updateReqVO.get("typeId")));
        standingbook.setId(Long.valueOf(updateReqVO.get("id")));
        // 修改标签信息 先删后增
        standingbookLabelInfoMapper.delete(new LambdaQueryWrapper<StandingbookLabelInfoDO>().eq(StandingbookLabelInfoDO::getStandingbookId, standingbook.getId()));
        createLabelInfoList(updateReqVO.get("labelInfo"), standingbook.getId());
        if (updateReqVO.get("stage") != null) {
            standingbook.setStage(Integer.valueOf(updateReqVO.get("stage")));
        }
        standingbookMapper.updateById(standingbook);
        // 使用 entrySet() 遍历键和值
        List<StandingbookAttributeSaveReqVO> children = new ArrayList<>();
        for (Map.Entry<String, String> entry : updateReqVO.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!entry.getKey().equals("typeId") && !entry.getKey().equals("id") && !entry.getKey().equals("labelInfo")) {
                StandingbookAttributeSaveReqVO attribute = new StandingbookAttributeSaveReqVO();
                attribute.setCode(key).setValue(value);
                attribute.setStandingbookId(standingbook.getId());
                children.add(attribute);
            }
        }
        // 更新属性
        if (children.size() > 0) {
            children.forEach(child -> standingbookAttributeService.update(child));
        }

    }

    @Override
    public void deleteStandingbook(Long id) {
        // 校验存在
        validateStandingbookExists(id);
        // 删除
        standingbookMapper.deleteById(id);
        // 删除标签信息
        standingbookLabelInfoMapper.delete(StandingbookLabelInfoDO::getStandingbookId, id);
        // 删除属性
        standingbookAttributeMapper.deleteStandingbookId(id);

    }

    private void validateStandingbookExists(Long id) {
        if (standingbookMapper.selectById(id) == null) {
            throw exception(STANDINGBOOK_NOT_EXISTS);
        }
    }

    @Override
    public StandingbookDO getStandingbook(Long id) {
        StandingbookDO standingbookDO = standingbookMapper.selectById(id);
        // 获取标签信息
        if (standingbookDO == null) {
            return null;
        } else {
            List<StandingbookAttributeDO> standingbookAttributeDOList =
                    standingbookAttributeMapper.selectStandingbookId(standingbookDO.getId());
            standingbookDO.addChildAll(standingbookAttributeDOList);
        }
        // 查询标签信息
        List<StandingbookLabelInfoDO> standingbookLabelInfoDOList =
                standingbookLabelInfoMapper.selectList(StandingbookLabelInfoDO::getStandingbookId, id);
        standingbookDO.setLabelInfo(standingbookLabelInfoDOList);
        return standingbookDO;
    }



    @Override
    public List<StandingbookDO> getStandingbookList(Map<String, String> pageReqVO) {
        //过滤空条件
        pageReqVO.entrySet().removeIf(entry -> StringUtils.isEmpty(entry.getValue()));

        // 取出查询条件
        // 能源条件（可能为空）
        String energy = pageReqVO.get(ATTR_ENERGY);
        List<Long> energyTypeIds = new ArrayList<>();
        if (StringUtils.isNotEmpty(energy)) {
            energyTypeIds = standingbookTmplDaqAttrMapper.selectSbTypeIdsByEnergyId(Long.valueOf(energy));
        }

        // 分类多选条件(可能为空)
        List<String> sbTypeIdList = new ArrayList<>();
        String sbTypeIds = pageReqVO.get(ATTR_SB_TYPE_ID);
        if (StringUtils.isNotEmpty(sbTypeIds)) {
            sbTypeIdList = Arrays.asList(sbTypeIds.split(StringPool.COMMA));
        }

        // 根据分类和topType查询台账
        List<StandingbookTypeDO> sbTypeDOS = standingbookTypeMapper.selectList(new LambdaQueryWrapperX<StandingbookTypeDO>()
                .inIfPresent(StandingbookTypeDO::getId, sbTypeIdList)
                .inIfPresent(StandingbookTypeDO::getId, energyTypeIds)
                .eqIfPresent(StandingbookTypeDO::getTopType, pageReqVO.get(SB_TYPE_ATTR_TOP_TYPE)));
        if (CollUtil.isEmpty(sbTypeDOS)) {
            return Collections.emptyList();
        }
        List<Long> sbTypeIdLongList = sbTypeDOS.stream().map(StandingbookTypeDO::getId).collect(Collectors.toList());
        // 分类单选条件(可能为空)
        Long typeId = pageReqVO.get(ATTR_TYPE_ID) != null ? Long.valueOf(pageReqVO.get(ATTR_TYPE_ID)) : null;


        // 环节单选条件(可能为空)
        Integer stage = pageReqVO.get(ATTR_STAGE) != null ? Integer.valueOf(pageReqVO.get(ATTR_STAGE)) : null;
        // 创建时间条件(可能为空)
        String createTimes = pageReqVO.get(ATTR_CREATE_TIME);
        List<String> createTimeArr = new ArrayList<>();
        if (StringUtils.isNotEmpty(createTimes)) {
            createTimeArr = Arrays.asList(createTimes.split(StringPool.COMMA));
        }
        pageReqVO.remove(ATTR_ENERGY);
        pageReqVO.remove(SB_TYPE_ATTR_TOP_TYPE);
        pageReqVO.remove(ATTR_SB_TYPE_ID);
        pageReqVO.remove(ATTR_STAGE);
        pageReqVO.remove(ATTR_TYPE_ID);
        pageReqVO.remove(ATTR_CREATE_TIME);
        Map<String, List<String>> childrenConditions = new HashMap<>();
        Map<String, List<String>> labelInfoConditions = new HashMap<>();
        // 构造标签数组 和 属性表code条件数组
        pageReqVO.forEach((k, v) -> {
            if (k.startsWith(ATTR_LABEL_INFO_PREFIX)) {
                if (v.contains(StringPool.HASH)) {
                    labelInfoConditions.put(k, Arrays.asList(v.split(StringPool.HASH)));
                } else {
                    labelInfoConditions.put(k, Collections.singletonList(v));
                }
            } else {
                if (v.contains(StringPool.COMMA)) {
                    childrenConditions.put(k, Arrays.asList(v.split(StringPool.COMMA)));
                } else {
                    childrenConditions.put(k, Collections.singletonList(v));
                }
            }
        });
        // 根据台账属性查询台账id
        List<Long> sbIds = standingbookMapper.selectStandingbookIdByCondition(typeId, sbTypeIdLongList, stage, createTimeArr);
        if (CollUtil.isEmpty(sbIds)) {
            return new ArrayList<>();
        }
        if(CollUtil.isNotEmpty(labelInfoConditions)){
            // 根据标签属性查询台账id
            List<Long> labelSbIds = standingbookLabelInfoMapper.selectStandingbookIdByLabelCondition(labelInfoConditions, sbIds);
            sbIds.retainAll(labelSbIds);
            if (CollUtil.isEmpty(sbIds)) {
                return new ArrayList<>();
            }
        }
        // 根据台账id、台账属性条件查询台账属性
        List<Long> attrSbIds = standingbookAttributeService.getStandingbookIdByCondition(childrenConditions, sbIds);

        sbIds.retainAll(attrSbIds);
        if (CollUtil.isEmpty(sbIds)) {
            return new ArrayList<>();
        }
        // 组装每个台账节点结构，可与上合起来优化，暂不敢动
        List<StandingbookDO> result = new ArrayList<>();
        sbIds.forEach(sbId -> result.add(getStandingbook(sbId)));

        return result;
    }


    @Override
    public List<StandingbookWithAssociations> getStandingbookListWithAssociations(Map<String, String> pageReqVO) {
        // 获取台账列表
        List<StandingbookDO> standingbookDOS = getStandingbookList(pageReqVO);

        if (CollUtil.isEmpty(standingbookDOS)) {
            return new ArrayList<>();
        }
        // 查询所有台账id列表
        List<Long> sbIds = standingbookDOS.stream().map(StandingbookDO::getId).collect(Collectors.toList());
        // 查询所有台账分类id列表
        List<Long> sbTypeIds = standingbookDOS.stream().map(StandingbookDO::getTypeId).collect(Collectors.toList());
        List<StandingbookTypeDO> typeList = standingbookTypeMapper.selectList(new LambdaQueryWrapper<StandingbookTypeDO>().in(StandingbookTypeDO::getId, sbTypeIds));
        Map<Long, StandingbookTypeDO> typeMap = typeList.stream().collect(Collectors.toMap(StandingbookTypeDO::getId, standingbookTypeDO -> standingbookTypeDO));

        // 查询所有台账关联的下级计量器具
        List<MeasurementAssociationDO> assosicationSbList = measurementAssociationMapper.selectList(new LambdaQueryWrapper<MeasurementAssociationDO>().in(MeasurementAssociationDO::getMeasurementInstrumentId, sbIds));
        // 所有下级计量器具分组属性map
        Map<Long, List<StandingbookAttributeDO>> measurementAttrsMap = new HashMap<>();
        Map<Long, List<MeasurementAssociationDO>> assosicationSbMap = new HashMap<>();
        if (CollUtil.isNotEmpty(assosicationSbList)) {
            // 分组 台账id-下级计量器具们
            assosicationSbMap = assosicationSbList.stream().collect(Collectors.groupingBy(MeasurementAssociationDO::getMeasurementInstrumentId));
            List<Long> measurementIds = assosicationSbList.stream().map(MeasurementAssociationDO::getMeasurementId).collect(Collectors.toList());
            measurementAttrsMap = standingbookAttributeService.getAttributesBySbIds(measurementIds);

        }
        // 查询所有台账关联的上级设备
        List<MeasurementDeviceDO> assosicationDeviceList = measurementDeviceMapper.selectList(new LambdaQueryWrapper<MeasurementDeviceDO>().in(MeasurementDeviceDO::getMeasurementInstrumentId, sbIds));
        Map<Long, List<StandingbookAttributeDO>> deviceAttrsMap = new HashMap<>();
        Map<Long, List<MeasurementDeviceDO>> assosicationDeviceMap = new HashMap<>();
        if (CollUtil.isNotEmpty(assosicationDeviceList)) {
            // 分组 台账id-下级计量器具们
            assosicationDeviceMap = assosicationDeviceList.stream().collect(Collectors.groupingBy(MeasurementDeviceDO::getMeasurementInstrumentId));
            List<Long> deviceIds = assosicationDeviceList.stream().map(MeasurementDeviceDO::getDeviceId).collect(Collectors.toList());
            deviceAttrsMap = standingbookAttributeService.getAttributesBySbIds(deviceIds);
        }

        // 返回结果
        List<StandingbookWithAssociations> result = new ArrayList<>();

        // 填充返回结果
        for (StandingbookDO standingbookDO : standingbookDOS) {

            List<StandingbookAttributeDO> attributes = standingbookDO.getChildren();

            Optional<StandingbookAttributeDO> measuringInstrumentNameOptional = attributes.stream().filter(attribute -> ATTR_MEASURING_INSTRUMENT_MAME.equals(attribute.getCode())).findFirst();
            Optional<StandingbookAttributeDO> measuringInstrumentIdOptional = attributes.stream().filter(attribute -> ATTR_MEASURING_INSTRUMENT_ID.equals(attribute.getCode())).findFirst();
            Optional<StandingbookAttributeDO> tableTypeOptional = attributes.stream().filter(attribute -> ATTR_TABLE_TYPE.equals(attribute.getCode())).findFirst();
            Optional<StandingbookAttributeDO> valueTypeOptional = attributes.stream().filter(attribute -> ATTR_VALUE_TYPE.equals(attribute.getCode())).findFirst();
            Long StandingbookId = standingbookDO.getId();

            StandingbookWithAssociations standingbookWithAssociations = new StandingbookWithAssociations();
            standingbookWithAssociations.setStandingbookId(StandingbookId);
            standingbookWithAssociations.setStandingbookName(measuringInstrumentNameOptional.map(StandingbookAttributeDO::getValue).orElse(StringPool.EMPTY));
            standingbookWithAssociations.setStandingbookTypeId(standingbookDO.getTypeId());
            standingbookWithAssociations.setStandingbookTypeName(typeMap.get(standingbookDO.getTypeId()).getName());
            standingbookWithAssociations.setMeasuringInstrumentId(measuringInstrumentIdOptional.map(StandingbookAttributeDO::getValue).orElse(StringPool.EMPTY));
            standingbookWithAssociations.setTableType(tableTypeOptional.map(StandingbookAttributeDO::getValue).orElse(StringPool.EMPTY));
            standingbookWithAssociations.setValueType(valueTypeOptional.map(StandingbookAttributeDO::getValue).orElse(StringPool.EMPTY));
            standingbookWithAssociations.setStage(standingbookDO.getStage());
            standingbookWithAssociations.setLabelInfo(standingbookDO.getLabelInfo());


            // 获取关联下级计量器具和上级设备
            List<MeasurementAssociationDO> measurementAssociationDOS = assosicationSbMap.get(StandingbookId);
            List<AssociationData> children = new ArrayList<>();
            if (CollUtil.isNotEmpty(measurementAssociationDOS)) {
                Map<Long, List<StandingbookAttributeDO>> finalMeasurementAttrsMap = measurementAttrsMap;
                measurementAssociationDOS.forEach(association -> {
                    AssociationData associationData = new AssociationData();
                    associationData.setStandingbookId(association.getMeasurementId());

                    // 查询下级计量器具名称、编码
                    List<StandingbookAttributeDO> attributeDOS = finalMeasurementAttrsMap.get(association.getMeasurementId());
                    Optional<StandingbookAttributeDO> nameOptional = attributeDOS.stream().filter(attribute -> ATTR_MEASURING_INSTRUMENT_MAME.equals(attribute.getCode())).findFirst();
                    Optional<StandingbookAttributeDO> codeOptional = attributeDOS.stream().filter(attribute -> ATTR_MEASURING_INSTRUMENT_ID.equals(attribute.getCode())).findFirst();

                    associationData.setStandingbookName(nameOptional.map(StandingbookAttributeDO::getValue).orElse(StringPool.EMPTY));
                    associationData.setStandingbookCode(codeOptional.map(StandingbookAttributeDO::getValue).orElse(StringPool.EMPTY));
                    children.add(associationData);
                });
                standingbookWithAssociations.setChildren(children);
            }
            List<MeasurementDeviceDO> deviceDOList = assosicationDeviceMap.get(StandingbookId);
            if (CollUtil.isNotEmpty(deviceDOList)) {
                // 上级设备-台账id
                Long deviceId = deviceDOList.get(0).getDeviceId();
                // 查询上级设备编码
                List<StandingbookAttributeDO> attributeDOS = deviceAttrsMap.get(deviceId);
                Optional<StandingbookAttributeDO> nameOptional = attributeDOS.stream().filter(attribute -> ATTR_EQUIPMENT_NAME.equals(attribute.getCode())).findFirst();
                Optional<StandingbookAttributeDO> codeOptional = attributeDOS.stream().filter(attribute -> ATTR_EQUIPMENT_ID.equals(attribute.getCode())).findFirst();
                standingbookWithAssociations.setDeviceId(deviceId);
                standingbookWithAssociations.setDeviceName(nameOptional.map(StandingbookAttributeDO::getValue).orElse(StringPool.EMPTY));
                standingbookWithAssociations.setDeviceCode(codeOptional.map(StandingbookAttributeDO::getValue).orElse(StringPool.EMPTY));
            }
            result.add(standingbookWithAssociations);
        }

        return result;
    }


}
