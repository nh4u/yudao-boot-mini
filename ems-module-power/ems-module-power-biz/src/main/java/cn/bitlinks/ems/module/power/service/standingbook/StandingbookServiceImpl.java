package cn.bitlinks.ems.module.power.service.standingbook;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.AssociationData;
import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.StandingbookWithAssociations;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.measurementassociation.MeasurementAssociationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.measurementdevice.MeasurementDeviceDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import cn.bitlinks.ems.module.power.dal.mysql.measurementassociation.MeasurementAssociationMapper;
import cn.bitlinks.ems.module.power.dal.mysql.measurementdevice.MeasurementDeviceMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.StandingbookMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.attribute.StandingbookAttributeMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.type.StandingbookTypeMapper;
import cn.bitlinks.ems.module.power.enums.ApiConstants;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.enums.ErrorCodeConstants;
import cn.bitlinks.ems.module.power.enums.standingbook.StandingbookTypeTopEnum;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.bitlinks.ems.module.power.service.standingbook.attribute.StandingbookAttributeService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
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
    @Autowired
    private ApplicationContext context;
    @Resource
    private StandingbookMapper standingbookMapper;
    @Resource
    private StandingbookAttributeService standingbookAttributeService;
    @Resource
    private LabelConfigService labelService;

    @Resource
    private StandingbookTypeMapper standingbookTypeMapper;


    @Resource
    private StandingbookAttributeMapper standingbookAttributeMapper;
    @Resource
    private MeasurementDeviceMapper measurementDeviceMapper;
    @Resource
    private MeasurementAssociationMapper measurementAssociationMapper;

    @Transactional
    public Long create(StandingbookSaveReqVO createReqVO) {
        // 插入
        StandingbookDO standingbook = BeanUtils.toBean(createReqVO, StandingbookDO.class);
        standingbookMapper.insert(standingbook);
        // 返回
        if (createReqVO.getChildren() != null && createReqVO.getChildren().size() > 0) {
            createReqVO.getChildren().forEach(child -> {
                child.setStandingbookId(standingbook.getId());
                standingbookAttributeService.createStandingbookAttribute(child);
            });
        }
        return standingbook.getId();
    }

    @Override
    public Long count(Long typeId) {

        // 对应类型【重点设备、计量器具、其他设备】下的所有类型id
        List<Long> typeIdList;
        if (typeId.equals(CommonConstants.OTHER_EQUIPMENT_ID)) {
            // 查询对应所有类型id
            typeIdList = standingbookTypeMapper.selectList(new LambdaQueryWrapperX<StandingbookTypeDO>()
                            .ne(StandingbookTypeDO::getTopType, CommonConstants.MEASUREMENT_INSTRUMENT_ID)
                            .ne(StandingbookTypeDO::getTopType, CommonConstants.KEY_EQUIPMENT_ID))
                    .stream()
                    .map(StandingbookTypeDO::getId)
                    .collect(Collectors.toList());
        } else {

            // 差对应id
            typeIdList = standingbookTypeMapper.selectList(new LambdaQueryWrapperX<StandingbookTypeDO>()
                            .eq(StandingbookTypeDO::getTopType, typeId))
                    .stream()
                    .map(StandingbookTypeDO::getId)
                    .collect(Collectors.toList());
        }
        return typeIdList.size() > 0 ? standingbookMapper.selectCount(new LambdaQueryWrapperX<StandingbookDO>()
                .in(StandingbookDO::getTypeId, typeIdList)) : 0;
    }

    @Override
    public List<StandingbookDO> listSbAll(Map<String, String> pageReqVO) {

        // 根据topType查询所有台账，也可以查询所有台账
        List<StandingbookTypeDO> sbTypeDOS = standingbookTypeMapper.selectList(new LambdaQueryWrapperX<StandingbookTypeDO>()
                .eqIfPresent(StandingbookTypeDO::getTopType, pageReqVO.get(SB_TYPE_ATTR_TOP_TYPE)));
        if (CollUtil.isEmpty(sbTypeDOS)) {
            return new ArrayList<>();
        }
        // 取出分类ids当成条件，去复用的方法中进行条件查询台账。
        List<Long> sbTypeIds = sbTypeDOS.stream()
                .map(StandingbookTypeDO::getId)
                .collect(Collectors.toList());
        String idsString = sbTypeIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(StringPool.COMMA));
        pageReqVO.put(ATTR_SB_TYPE_ID, idsString);
        pageReqVO.remove(SB_TYPE_ATTR_TOP_TYPE);
        // 多条件查询台账
        return getStandingbookList(pageReqVO);

    }


    @Override
    public List<StandingbookDO> listSbAllWithAssociations(StandingbookAssociationReqVO reqVO) {

        Map<String,String> paramMap = reqVO.getPageReqVO();
        if(CollUtil.isEmpty(paramMap)){
            paramMap = new HashMap<>();
        }
        paramMap.put(SB_TYPE_ATTR_TOP_TYPE, reqVO.getTopType() + "");

        Long sbId = reqVO.getSbId();

        List<StandingbookDO> sbList = listSbAll(paramMap);


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

        return sbList.stream()
                .filter(standingbookDO -> !parentIds.contains(standingbookDO.getId()))
                .collect(Collectors.toList());

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
        standingbook.setLabelInfo(createReqVO.get(ATTR_LABEL_INFO));
        standingbookMapper.insert(standingbook);

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
     * 台账编码值
     *
     * @param codeValue
     */
    private void validateSbCodeUnique(String codeValue) {
        List<StandingbookAttributeDO> exists = standingbookAttributeMapper.selectList(new LambdaQueryWrapper<StandingbookAttributeDO>()
                .eq(StandingbookAttributeDO::getValue, codeValue)
                .in(StandingbookAttributeDO::getCode, Arrays.asList(ATTR_MEASURING_INSTRUMENT_ID, ATTR_EQUIPMENT_ID))
        );
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
        standingbook.setLabelInfo(updateReqVO.get("labelInfo"));
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
        // 删除属性
        try {
            standingbookAttributeService.deleteStandingbookAttributeByStandingbookId(id);
        } catch (Exception e) {
            // 忽略属性删除失败 因为可能不存在
            e.printStackTrace();
        }
    }

    private void validateStandingbookExists(Long id) {
        if (standingbookMapper.selectById(id) == null) {
            throw exception(STANDINGBOOK_NOT_EXISTS);
        }
    }

    @Override
    public StandingbookDO getStandingbook(Long id) {
        StandingbookDO standingbookDO = standingbookMapper.selectById(id);
        if (standingbookDO == null) {
            return null;
        } else {
            addChildAll(standingbookDO);
        }
        return standingbookDO;
    }

    void addChildAll(StandingbookDO standingbookDO) {
        standingbookDO.addChildAll(standingbookAttributeService.getStandingbookAttributeByStandingbookId(standingbookDO.getId()));
    }

    @Override
    public PageResult<StandingbookDO> getStandingbookPage(StandingbookPageReqVO pageReqVO) {
        PageResult<StandingbookDO> standingbookDOPageResult = standingbookMapper.selectPage(pageReqVO);
        standingbookDOPageResult.getList().forEach(this::addChildAll);
        return standingbookDOPageResult;
    }

    @Override
    public List<StandingbookDO> getStandingbookList(Map<String, String> pageReqVO) {
        //过滤空条件
        pageReqVO.entrySet().removeIf(entry -> StringUtils.isEmpty(entry.getValue()));

        // 取出查询条件
        // 分类多选条件(可能为空)
        List<String> sbTypeIdList = new ArrayList<>();
        String sbTypeIds = pageReqVO.get(ATTR_SB_TYPE_ID);
        if (StringUtils.isNotEmpty(sbTypeIds)) {
            sbTypeIdList = Arrays.asList(sbTypeIds.split(StringPool.COMMA));
        }
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
        List<Long> sbIds = standingbookMapper.selectStandingbookIdByCondition(labelInfoConditions, typeId, sbTypeIdList, stage, createTimeArr);
        if (CollUtil.isEmpty(sbIds)) {
            return new ArrayList<>();
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

//    @Override
//    public List<StandingbookDO> getStandingbookListBy(Map<String, String> pageReqVO) {
//        List<StandingbookAttributePageReqVO> children = new ArrayList<>();
//        Long typeId = null;
//
//        for (Map.Entry<String, String> entry : pageReqVO.entrySet()) {
//            String key = entry.getKey();
//            String value = entry.getValue();
//            if ("typeId".equals(key)) {
//                typeId = Long.parseLong(value);
//            } else {
//                StandingbookAttributePageReqVO attribute = new StandingbookAttributePageReqVO();
//                attribute.setCode(key).setValue(value);
//                children.add(attribute);
//            }
//        }
//
//        // 调用新方法获取交集结果
//        List<StandingbookDO> standingbookDOS = standingbookAttributeService.getStandingbookIntersection(children, typeId);
//        List<StandingbookDO> result = new ArrayList<>();
//        for (StandingbookDO standingbookDO : standingbookDOS) {
//            result.add(getStandingbook(standingbookDO.getId()));
//        }
//        return result;
//    }

    @Override
    public List<StandingbookWithAssociations> getStandingbookListWithAssociations(Map<String, String> pageReqVO) {
        // 获取台账列表
        List<StandingbookDO> standingbookDOS = getStandingbookList(pageReqVO);

        // 查询所有台账id列表
        List<Long> sbIds = standingbookDOS.stream()
                .map(StandingbookDO::getId)
                .collect(Collectors.toList());
        // 查询所有台账分类id列表
        List<Long> sbTypeIds = standingbookDOS.stream()
                .map(StandingbookDO::getTypeId)
                .collect(Collectors.toList());
        List<StandingbookTypeDO> typeList = standingbookTypeMapper.selectList(new LambdaQueryWrapper<StandingbookTypeDO>()
                .in(StandingbookTypeDO::getId, sbTypeIds));
        Map<Long, StandingbookTypeDO> typeMap = typeList.stream()
                .collect(Collectors.toMap(
                        StandingbookTypeDO::getId,
                        standingbookTypeDO -> standingbookTypeDO
                ));

        // 查询所有台账关联的下级计量器具
        List<MeasurementAssociationDO> assosicationSbList = measurementAssociationMapper.selectList(new LambdaQueryWrapper<MeasurementAssociationDO>()
                .in(MeasurementAssociationDO::getMeasurementInstrumentId, sbIds)
        );
        // 所有下级计量器具分组属性map
        Map<Long, List<StandingbookAttributeDO>> measurementAttrsMap = new HashMap<>();
        Map<Long, List<MeasurementAssociationDO>> assosicationSbMap = new HashMap<>();
        if (CollUtil.isNotEmpty(assosicationSbList)) {
            // 分组 台账id-下级计量器具们
            assosicationSbMap = assosicationSbList.stream()
                    .collect(Collectors.groupingBy(MeasurementAssociationDO::getMeasurementInstrumentId));
            List<Long> measurementIds = assosicationSbList.stream()
                    .map(MeasurementAssociationDO::getMeasurementId)
                    .collect(Collectors.toList());
            measurementAttrsMap = standingbookAttributeService.getAttributesBySbIds(measurementIds);

        }
        // 查询所有台账关联的上级设备
        List<MeasurementDeviceDO> assosicationDeviceList = measurementDeviceMapper.selectList(new LambdaQueryWrapper<MeasurementDeviceDO>()
                .in(MeasurementDeviceDO::getMeasurementInstrumentId, sbIds)
        );
        Map<Long, List<StandingbookAttributeDO>> deviceAttrsMap = new HashMap<>();
        Map<Long, List<MeasurementDeviceDO>> assosicationDeviceMap = new HashMap<>();
        if (CollUtil.isNotEmpty(assosicationDeviceList)) {
            // 分组 台账id-下级计量器具们
            assosicationDeviceMap = assosicationDeviceList.stream()
                    .collect(Collectors.groupingBy(MeasurementDeviceDO::getMeasurementInstrumentId));
            List<Long> deviceIds = assosicationDeviceList.stream()
                    .map(MeasurementDeviceDO::getDeviceId)
                    .collect(Collectors.toList());
            deviceAttrsMap = standingbookAttributeService.getAttributesBySbIds(deviceIds);
        }

        // 返回结果
        List<StandingbookWithAssociations> result = new ArrayList<>();

        // 填充返回结果
        for (StandingbookDO standingbookDO : standingbookDOS) {

            List<StandingbookAttributeDO> attributes = standingbookDO.getChildren();

            Optional<StandingbookAttributeDO> measuringInstrumentNameOptional = attributes.stream()
                    .filter(attribute -> ATTR_MEASURING_INSTRUMENT_MAME.equals(attribute.getCode()))
                    .findFirst();
            Optional<StandingbookAttributeDO> measuringInstrumentIdOptional = attributes.stream()
                    .filter(attribute -> ATTR_MEASURING_INSTRUMENT_ID.equals(attribute.getCode()))
                    .findFirst();
            Optional<StandingbookAttributeDO> tableTypeOptional = attributes.stream()
                    .filter(attribute -> ATTR_TABLE_TYPE.equals(attribute.getCode()))
                    .findFirst();
            Optional<StandingbookAttributeDO> valueTypeOptional = attributes.stream()
                    .filter(attribute -> ATTR_VALUE_TYPE.equals(attribute.getCode()))
                    .findFirst();
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
                    Optional<StandingbookAttributeDO> nameOptional = attributeDOS.stream()
                            .filter(attribute -> ATTR_MEASURING_INSTRUMENT_MAME.equals(attribute.getCode()))
                            .findFirst();
                    Optional<StandingbookAttributeDO> codeOptional = attributeDOS.stream()
                            .filter(attribute -> ATTR_MEASURING_INSTRUMENT_ID.equals(attribute.getCode()))
                            .findFirst();

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
                Optional<StandingbookAttributeDO> nameOptional = attributeDOS.stream()
                        .filter(attribute -> ATTR_EQUIPMENT_NAME.equals(attribute.getCode()))
                        .findFirst();
                Optional<StandingbookAttributeDO> codeOptional = attributeDOS.stream()
                        .filter(attribute -> ATTR_EQUIPMENT_ID.equals(attribute.getCode()))
                        .findFirst();
                standingbookWithAssociations.setDeviceId(deviceId);
                standingbookWithAssociations.setDeviceName(nameOptional.map(StandingbookAttributeDO::getValue).orElse(StringPool.EMPTY));
                standingbookWithAssociations.setDeviceCode(codeOptional.map(StandingbookAttributeDO::getValue).orElse(StringPool.EMPTY));
            }
            result.add(standingbookWithAssociations);
        }

        return result;
    }

    @Override
    public Object importStandingbook(MultipartFile file, StandingbookRespVO pageReqVO) {
        StandingbookService proxy = context.getBean(StandingbookService.class);
        // TODO 导入功能实现
        Long typeId = pageReqVO.getTypeId();
        //模板
        List<StandingbookAttributeDO> attributes = standingbookAttributeService.getStandingbookAttributeByTypeId(typeId);

        // 获取标签信息
        String labelInfo = pageReqVO.getLabelInfo();
        JSONObject labelJson = JSONUtil.parseObj(labelInfo); // 使用JSONUtil解析JSON字符串

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                List<StandingbookAttributeSaveReqVO> children = new ArrayList<>();
                StandingbookSaveReqVO saveReq = new StandingbookSaveReqVO(children);
                Row row = sheet.getRow(i);
                if (row == null) continue;
                for (int j = 0; j <= attributes.size(); j++) {
                    Cell cell = row.getCell(j);
                    if (cell == null) continue;
                    StandingbookAttributeSaveReqVO attribute = new StandingbookAttributeSaveReqVO();
                    BeanUtils.copyProperties(attributes.get(j), attribute);
                    attribute.setStandingbookId(null);
                    if (!attribute.getFormat().equals(ApiConstants.FILE)) {
                        String cellValue = getCellValue(cell);
                        System.out.println(cellValue);
                        attribute.setValue(cellValue);
                    } else {
                        // TODO 读取图片文件
//                        if (cell.getDrawings() != null) {
//                            XSSFPicture picture = (XSSFPicture) cell.getDrawingPatriarch().getAllPictures().get(0);
//                            byte[] pictureData = picture.getPictureData().getData();
//                            // 处理图片数据
//                        }

                    }
                    children.add(attribute);
                }
                // 处理标签信息
                for (Map.Entry<String, Object> entry : labelJson.entrySet()) {
                    StandingbookAttributeSaveReqVO attribute = new StandingbookAttributeSaveReqVO();
                    attribute.setName(entry.getKey());
                    attribute.setValue(entry.getValue().toString());
                    children.add(attribute);
                }
                for (int j = 0; j < attributes.size(); j++) {
                    Cell cell = row.getCell(j);
                    if (cell == null) continue;
                    StandingbookAttributeSaveReqVO attribute = new StandingbookAttributeSaveReqVO();
                    BeanUtils.copyProperties(attributes.get(j), attribute);
                    attribute.setStandingbookId(null);
                    if (!attribute.getFormat().equals(ApiConstants.FILE)) {
                        String cellValue = getCellValue(cell);
                        attribute.setValue(cellValue);
                    } else {
                        // TODO 读取图片文件
                    }
                    children.add(attribute);
                }
                proxy.create(saveReq);

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return "success";
    }

    @Override
    public void exportStandingbookExcel(Map<String, String> pageReqVO, HttpServletResponse response) {
        List<StandingbookDO> list = getStandingbookList(pageReqVO);
        Long typeId = Long.valueOf(pageReqVO.get("typeId"));
        List<StandingbookAttributeDO> attributes = standingbookAttributeService.getStandingbookAttributeByTypeId(typeId);
        StandingbookTypeDO standingbookTypeDO = standingbookTypeMapper.selectById(typeId);
        // 获取标签信息
        String labelInfo = pageReqVO.get("labelInfo");
        JSONObject labelJson = JSONUtil.parseObj(labelInfo); // 使用JSONUtil解析JSON字符串

        if (attributes == null || attributes.isEmpty()) {
            throw new IllegalArgumentException("台账属性不能为空");
        }
        // 导出 Excel
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("数据");
            // 第一行
            Row firstRow = sheet.createRow(0);
            Cell templateNumberCell = firstRow.createCell(0);
            templateNumberCell.setCellValue("模板编号: ");
            Cell templateNumberCell1 = firstRow.createCell(1);
            templateNumberCell1.setCellValue(typeId);
            // 创建表头
            Row headerRow = sheet.createRow(1);
            int attributeCount = attributes.size();
            for (int i = 0; i < attributes.size(); i++) {
                sheet.setColumnWidth(i, 5000);
                StandingbookAttributeDO column = attributes.get(i);
                Cell cell = headerRow.createCell(i); // 从第1列开始
                cell.setCellValue(column.getName());

                // 设置单元格样式
                CellStyle style = workbook.createCellStyle();
                cell.setCellStyle(style);
                if (ApiConstants.YES.equals(column.getIsRequired())) {
                    style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
                    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                } else {
                    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                }
            }
            // 添加标签信息的表头
            int labelIndex = attributeCount;
            for (String labelKey : labelJson.keySet()) {
                Cell labelHeaderCell = headerRow.createCell(labelIndex++);
                labelHeaderCell.setCellValue(labelKey);
            }

            // 创建数据行
            for (int i = 0; i < list.size(); i++) {
                Row dataRow = sheet.createRow(i + 2);
                dataRow.setHeightInPoints(50); // 设置行高
                List<StandingbookAttributeDO> children = list.get(i).getChildren();
                for (int j = 0; j < children.size(); j++) {
                    StandingbookAttributeDO standingbookAttributeDO = children.get(j);
                    Cell cell = dataRow.createCell(j); // 从第1列开始
                    if (ApiConstants.FILE.equals(standingbookAttributeDO.getFormat())) {
//                        CommonResult result = fileApi.getFile(standingbookAttributeDO.getFileId());
//                        if (result == null || result.getData() == null)continue;
//                        JSONObject file = (JSONObject) JSONUtil.parse(result.getData());
//                        CommonResult<byte[]> fileId = fileApi.getFileContent(Long.valueOf(file.getStr("configId")),file.getStr("path"));
//                        if (fileId == null || fileId.getData() == null)continue;
//                        byte[] bytes = fileId.getData();
//                        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
//                        // 缩放图片
//                        ScaledImageDataVO scaledImageData = scaleImage(image);
//
//                        //将缩放后的图片转换为bytes
//                        byte[] imageBytes = processImageToBytes(scaledImageData.getScaledImage());
//
//                        int pictureIdx = workbook.addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG);
//                        CreationHelper helper = workbook.getCreationHelper();
//                        // 创建绘图对象
//                        Drawing<?> drawing = sheet.createDrawingPatriarch();
//
//                        // 定位图片 (行1, 列1, 宽度和高度可根据需要调整)
//
//                        ClientAnchor anchor = helper.createClientAnchor();
//                        anchor.setCol1(j); // 列号从0开始，1表示第一列
//                        anchor.setRow1(i + 2); // 行号从0开始，1表示第一行
//                        anchor.setCol2(j); // 列号从0开始，1表示第一列
//                        anchor.setRow2(i + 2); // 行号从0开始，1表示第一行
//                        // 插入图片
//                        Picture pict = drawing.createPicture(anchor, pictureIdx);
//                        pict.resize(); // 调整图片大小以适应单元格
                    } else {
                        cell.setCellValue(standingbookAttributeDO.getValue());
                    }
                }
                // 添加标签信息的数据
                int labelDataIndex = attributeCount;
                for (Map.Entry<String, Object> entry : labelJson.entrySet()) {
                    Cell labelCell = dataRow.createCell(labelDataIndex++);
                    labelCell.setCellValue(entry.getValue().toString());
                }
            }

            // 输出到文件

            response.reset();
            response.setContentType("application/octet-stream; charset=utf-8");
            // 当前时间格式化
            DateTime now = DateTime.now();
            String dateStr = now.toString("yyyyMMdd");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(standingbookTypeDO.getName() + "台账数据" + dateStr + ".xlsx", "UTF-8"));
            OutputStream os = response.getOutputStream();
            workbook.write(os);
            os.flush();
            os.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void template(Long typeId, HttpServletResponse response) {
        List<StandingbookAttributeDO> attributes = standingbookAttributeService.getStandingbookAttributeByTypeId(typeId);
        StandingbookTypeDO standingbookTypeDO = standingbookTypeMapper.selectById(typeId);
        if (attributes == null || attributes.isEmpty()) {
            throw exception(ErrorCodeConstants.STANDINGBOOK_ATTRIBUTE_NOT_EXISTS);
        }

        List<Tree<Long>> labelTree = labelService.getLabelTree(false, null, null);
        // 提取根标签的 code 作为表头
        List<String> rootLabelNames = new ArrayList<>();
        for (Tree<Long> tree : labelTree) {
            if (tree.getParentId() == 0) { // 只处理根标签
                rootLabelNames.add(String.valueOf(tree.getName())); // 提取 code 字段
            }
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("数据模板");

            // 第一行
            Row firstRow = sheet.createRow(0);
            Cell templateNumberCell = firstRow.createCell(0);
            templateNumberCell.setCellValue("模板编号: ");
            Cell templateNumberCell1 = firstRow.createCell(1);
            templateNumberCell1.setCellValue(typeId);

            // 合并第二、三、四、五列
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 2, 10));
            Cell hintCell = firstRow.createCell(2);
            hintCell.setCellValue("表头黄色的为必填项，请勿修改模板编号，否则无法导入数据。请从第三行开始填写数据，多选时请使用&符号分隔。（暂不支持导入图片和文件）");
            CellStyle s = workbook.createCellStyle();
            s.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            hintCell.setCellStyle(s);
            // 创建表头
            Row headerRow = sheet.createRow(1);
//             Cell headerCell = headerRow.createCell(0);
//             headerCell.setCellValue("台账名称");
            for (int i = 0; i < attributes.size(); i++) {
                StandingbookAttributeDO column = attributes.get(i);
                sheet.setColumnWidth(i, 5000);
//                 Cell cell = headerRow.createCell(i+1); // 从第二列开始
                Cell cell = headerRow.createCell(i); // 从第1列开始
                if (ApiConstants.MULTIPLE.equals(column.getFormat())) {
                    cell.setCellValue(column.getName() + "(" + column.getOptions().replaceAll(";", "&") + ")");
                } else {
                    cell.setCellValue(column.getName());
                }
                // 设置单元格样式
                CellStyle style = workbook.createCellStyle();
                if (ApiConstants.YES.equals(column.getIsRequired()) && !column.getFormat().equals(ApiConstants.FILE) && !column.getFormat().equals(ApiConstants.PICTURE)) {
                    style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
                    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                }
//                 CellRangeAddressList addressList = new CellRangeAddressList(2, 1000, i+1 , i+1);
                CellRangeAddressList addressList = new CellRangeAddressList(2, 1000, i, i);
                // 根据format设置数据验证
                if (ApiConstants.SELECT.equals(column.getFormat())) {
                    XSSFDataValidationHelper validationHelper = new XSSFDataValidationHelper(sheet);
                    String[] options = column.getOptions().split(";");
                    DataValidationConstraint constraint = validationHelper.createExplicitListConstraint(options);
                    XSSFDataValidation validation = (XSSFDataValidation) validationHelper.createValidation(constraint, addressList);
                    sheet.addValidationData(validation);
                }
                // 注意: Apache POI 目前不支持直接设置为多选下拉框。对于多选，用户需要通过VBA或者手动配置。
                cell.setCellStyle(style);
            }
            // 添加根标签作为表头
            int labelStartColumn = attributes.size(); // 标签从台账属性之后开始
            for (int i = 0; i < rootLabelNames.size(); i++) {
                Cell labelHeaderCell = headerRow.createCell(labelStartColumn + i);
                labelHeaderCell.setCellValue(rootLabelNames.get(i));
            }

            // 输出
            response.setContentType("application/octet-stream; charset=utf-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(standingbookTypeDO.getName() + "导入模板.xlsx", "UTF-8"));

            OutputStream os = response.getOutputStream();
            workbook.write(os);
            os.flush();
            os.close();
//            try (FileOutputStream fileOut = new FileOutputStream("D:\\破烂\\项目\\"+typeId+"-template.xlsx")) {
//                workbook.write(fileOut);
//            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    /**
     * 缩放图片
     */
    private static ScaledImageDataVO scaleImage(BufferedImage targetImage) {
        int desiredWidth = 50; // 图片宽度
        double aspectRatio = (double) targetImage.getHeight() / targetImage.getWidth();
        int desiredHeight = (int) (desiredWidth * aspectRatio); // 保持纵横比

        BufferedImage scaledImage = Scalr.resize(targetImage, Scalr.Method.QUALITY, desiredWidth, desiredHeight);
        return new ScaledImageDataVO(desiredHeight, scaledImage);
    }

    /**
     * 图片转bytes
     */
    private static byte[] processImageToBytes(BufferedImage targetImage) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            ImageIO.write(targetImage, "png", stream);
            return stream.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }
}
