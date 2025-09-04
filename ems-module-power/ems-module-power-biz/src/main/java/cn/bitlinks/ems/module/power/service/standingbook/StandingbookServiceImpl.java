package cn.bitlinks.ems.module.power.service.standingbook;

import cn.bitlinks.ems.framework.common.util.collection.CollectionUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.AssociationData;
import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.StandingbookWithAssociations;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.labelconfig.LabelConfigDO;
import cn.bitlinks.ems.module.power.dal.dataobject.measurementassociation.MeasurementAssociationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.measurementdevice.MeasurementDeviceDO;
import cn.bitlinks.ems.module.power.dal.dataobject.measurementvirtualassociation.MeasurementVirtualAssociationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookLabelInfoDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.acquisition.StandingbookAcquisitionDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import cn.bitlinks.ems.module.power.dal.mysql.energyconfiguration.EnergyConfigurationMapper;
import cn.bitlinks.ems.module.power.dal.mysql.labelconfig.LabelConfigMapper;
import cn.bitlinks.ems.module.power.dal.mysql.measurementassociation.MeasurementAssociationMapper;
import cn.bitlinks.ems.module.power.dal.mysql.measurementdevice.MeasurementDeviceMapper;
import cn.bitlinks.ems.module.power.dal.mysql.measurementvirtualassociation.MeasurementVirtualAssociationMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.StandingbookLabelInfoMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.StandingbookMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.attribute.StandingbookAttributeMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.templ.StandingbookTmplDaqAttrMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.type.StandingbookTypeMapper;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.enums.ErrorCodeConstants;
import cn.bitlinks.ems.module.power.enums.RedisKeyConstants;
import cn.bitlinks.ems.module.power.enums.standingbook.AttributeTreeNodeTypeEnum;
import cn.bitlinks.ems.module.power.enums.standingbook.StandingbookTypeTopEnum;
import cn.bitlinks.ems.module.power.service.doublecarbon.DoubleCarbonService;
import cn.bitlinks.ems.module.power.service.energyparameters.EnergyParametersService;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.bitlinks.ems.module.power.service.standingbook.acquisition.StandingbookAcquisitionService;
import cn.bitlinks.ems.module.power.service.standingbook.attribute.StandingbookAttributeService;
import cn.bitlinks.ems.module.power.service.standingbook.type.StandingbookTypeService;
import cn.bitlinks.ems.module.power.service.warningstrategy.WarningStrategyService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.util.ListUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ApiConstants.*;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.STANDINGBOOK_NOT_EXISTS;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.*;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.XLSX;
import static cn.bitlinks.ems.module.power.enums.standingbook.AttributeTreeNodeTypeEnum.*;

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
    private LabelConfigMapper labelConfigMapper;

    @Resource
    private LabelConfigService labelConfigService;
    @Resource
    private StandingbookTypeMapper standingbookTypeMapper;
    @Resource
    @Lazy
    private StandingbookTypeService standingbookTypeService;
    @Resource
    private StandingbookTmplDaqAttrMapper standingbookTmplDaqAttrMapper;
    @Resource
    private StandingbookAttributeMapper standingbookAttributeMapper;
    @Resource
    private MeasurementDeviceMapper measurementDeviceMapper;
    @Resource
    private MeasurementAssociationMapper measurementAssociationMapper;
    @Resource
    private MeasurementVirtualAssociationMapper measurementVirtualAssociationMapper;
    @Resource
    @Lazy
    private WarningStrategyService warningStrategyService;

    @Lazy
    @Resource
    private StandingbookAcquisitionService standingbookAcquisitionService;

    @Resource
    private EnergyConfigurationMapper energyConfigurationMapper;

    @Resource
    @Lazy
    private EnergyParametersService energyParametersService;

    @Resource
    @Lazy
    private DoubleCarbonService doubleCarbonService;

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

    @Override
    public List<StandingbookRespVO> listSbAllWithAssociationsVirtual(StandingbookAssociationReqVO reqVO) {

        Map<String, String> paramMap = reqVO.getPageReqVO();
        if (CollUtil.isEmpty(paramMap)) {
            paramMap = new HashMap<>();
        }
        paramMap.put(SB_TYPE_ATTR_TOP_TYPE, reqVO.getTopType() + "");
        Long sbId = reqVO.getSbId();

        List<StandingbookDO> sbList = getStandingbookList(paramMap);


        // 如果是计量器具的话，需要根据已关联的计量器具进行筛选，筛选出该计量器具可关联的计量器具列表
        // 查询出所有的关联关系，
        List<MeasurementVirtualAssociationDO> list = measurementVirtualAssociationMapper.selectList();
        Set<Long> parentIds = new HashSet<>(); // 使用 Set 去重
        findAllParentsRecursiveVirtual(sbId, list, parentIds);
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

        //根据typeIds获取能源ID关联
        List<StandingbookTmplDaqAttrDO> standingbookTmplDaqAttrDOS = standingbookTmplDaqAttrMapper.selectEnergyMapping(sbTypeIds);
        if (standingbookTmplDaqAttrDOS.isEmpty()) {
            return result;
        }
        //获取所有能源ID
        List<Long> energyIds = standingbookTmplDaqAttrDOS.stream().map(StandingbookTmplDaqAttrDO::getEnergyId).collect(Collectors.toList());
        //根据能源ID获取所有能源信息
        List<EnergyConfigurationDO> energyConfigurations = energyConfigurationMapper
                .selectList(new LambdaQueryWrapper<EnergyConfigurationDO>()
                        .in(EnergyConfigurationDO::getId, energyIds));
        //能源ID打包成Map
        Map<Long, EnergyConfigurationDO> energyMap = energyConfigurations
                .stream()
                .collect(Collectors.toMap(EnergyConfigurationDO::getId, energyConfigurationDO -> energyConfigurationDO));
        //Map typeID 与 能源ID关联
        Map<Long, Long> energyTypeIdMap = standingbookTmplDaqAttrDOS.stream().collect(Collectors.toMap(StandingbookTmplDaqAttrDO::getTypeId, StandingbookTmplDaqAttrDO::getEnergyId));

        //添加能源信息
        result.forEach(sb -> {
            sb.setEnergyId(energyTypeIdMap.get(sb.getTypeId()));
            if (energyMap.get(energyTypeIdMap.get(sb.getTypeId())) != null) {
                sb.setEnergyName(energyMap.get(energyTypeIdMap.get(sb.getTypeId())).getEnergyName());
            }
        });

        return result;

    }

    @Override
    @Transactional
    public void updAssociationMeasurementInstrument(MeasurementVirtualAssociationSaveReqVO createReqVO) {
        List<Long> ids = createReqVO.getMeasurementIds();

        List<MeasurementVirtualAssociationDO> list =
                measurementVirtualAssociationMapper.selectList(new LambdaQueryWrapper<MeasurementVirtualAssociationDO>()
                        .eq(MeasurementVirtualAssociationDO::getMeasurementInstrumentId, createReqVO.getMeasurementInstrumentId()));
        if (CollUtil.isEmpty(list)) {
            ids.forEach(id -> {
                MeasurementVirtualAssociationDO measurementAssociationDO = new MeasurementVirtualAssociationDO();
                measurementAssociationDO.setMeasurementId(id);
                measurementAssociationDO.setMeasurementInstrumentId(createReqVO.getMeasurementInstrumentId());
                list.add(measurementAssociationDO);
            });
            measurementVirtualAssociationMapper.insertBatch(list);
            return;
        }
        // 1. 找出需要删除的关联
        List<Long> toDelete = list.stream()
                .filter(association -> !ids.contains(association.getMeasurementId()))
                .map(MeasurementVirtualAssociationDO::getId)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(toDelete)) {
            measurementVirtualAssociationMapper.deleteByIds(toDelete);
        }

        // 2. 找出需要新增的关联 ID
        List<Long> toAddIds = ids.stream()
                .filter(id -> list.stream().noneMatch(association -> association.getMeasurementId().equals(id)))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(toAddIds)) {
            return;
        }
        List<MeasurementVirtualAssociationDO> toAddList = new ArrayList<>();
        toAddIds.forEach(id -> {
            MeasurementVirtualAssociationDO measurementAssociationDO = new MeasurementVirtualAssociationDO();
            measurementAssociationDO.setMeasurementId(id);
            measurementAssociationDO.setMeasurementInstrumentId(createReqVO.getMeasurementInstrumentId());
            toAddList.add(measurementAssociationDO);
        });
        measurementVirtualAssociationMapper.insertBatch(toAddList);
    }

    /**
     * 其他补充属性
     *
     * @param respVOS
     */
    @Override
    public void sbOtherField(List<StandingbookRespVO> respVOS) {
        //根据respVOS 获取typeId 列表;
        List<Long> sbTypeIds = respVOS.stream().map(StandingbookRespVO::getTypeId).collect(Collectors.toList());

        //根据typeIds获取能源ID关联
        List<StandingbookTmplDaqAttrDO> standingbookTmplDaqAttrDOS = standingbookTmplDaqAttrMapper.selectEnergyMapping(sbTypeIds);
        if (standingbookTmplDaqAttrDOS.isEmpty()) {
            return;
        }
        //获取所有能源ID
        List<Long> energyIds = standingbookTmplDaqAttrDOS.stream().map(StandingbookTmplDaqAttrDO::getEnergyId).collect(Collectors.toList());
        //根据能源ID获取所有能源信息
        List<EnergyConfigurationDO> energyConfigurations = energyConfigurationMapper
                .selectList(new LambdaQueryWrapper<EnergyConfigurationDO>()
                        .in(EnergyConfigurationDO::getId, energyIds));
        //能源ID打包成Map
        Map<Long, EnergyConfigurationDO> energyMap = energyConfigurations
                .stream()
                .collect(Collectors.toMap(EnergyConfigurationDO::getId, energyConfigurationDO -> energyConfigurationDO));
        //Map typeID 与 能源ID关联
        Map<Long, Long> energyTypeIdMap = standingbookTmplDaqAttrDOS.stream().collect(Collectors.toMap(StandingbookTmplDaqAttrDO::getTypeId, StandingbookTmplDaqAttrDO::getEnergyId));

        //添加能源信息
        respVOS.forEach(sb -> {
            sb.setEnergyId(energyTypeIdMap.get(sb.getTypeId()));
            if (energyMap.get(energyTypeIdMap.get(sb.getTypeId())) != null) {
                sb.setEnergyName(energyMap.get(energyTypeIdMap.get(sb.getTypeId())).getEnergyName());
            }
        });
    }

    @Override
    public List<StandingBookTypeTreeRespVO> treeWithEnergyParam(StandingbookEnergyParamReqVO standingbookEnergyParamReqVO) {
        // 根据能源参数筛选出能源然后筛选出台账分类，
        List<Long> energyIds = energyParametersService.getByEnergyIdByParamName(standingbookEnergyParamReqVO.getEnergyParamCnName());
        if (CollUtil.isEmpty(energyIds)) {
            return Collections.emptyList();
        }
        // 再筛选一遍能源挂在哪个台账分类下，
        List<Long> energyTypeIds = standingbookTmplDaqAttrMapper.selectSbTypeIdsByEnergyIds(energyIds);
        if (CollUtil.isEmpty(energyTypeIds)) {
            return Collections.emptyList();
        }
        // 根据台账分类查询台账
        List<Long> sbIds = standingbookMapper.selectStandingbookIdByCondition(null, energyTypeIds, null, null);
        if (CollUtil.isEmpty(sbIds)) {
            return Collections.emptyList();
        }
        // 根据台账名称和台账编码模糊搜出台账
        List<StandingBookTypeTreeRespVO> sbNodes = standingbookAttributeService.getStandingbookByCodeAndName(standingbookEnergyParamReqVO.getSbCode(), standingbookEnergyParamReqVO.getSbName(), sbIds);
        if (CollUtil.isEmpty(sbNodes)) {
            return Collections.emptyList();
        }

        // 台账分类属性结构
        List<StandingbookTypeDO> standingbookTypeDOTree = standingbookTypeService.getStandingbookTypeIdList(energyTypeIds);
        return buildTreeWithDevices(standingbookTypeDOTree, sbNodes);

    }

    @Override
    @Cacheable(value = RedisKeyConstants.STANDING_BOOK_DEVICE_CODE_LIST, key = "'all'", unless = "#result == null || #result.isEmpty()")
    public List<String> getStandingbookCodeDeviceList() {
        return standingbookAttributeMapper.getStandingbookCodeDeviceList();
    }

    @Override
    @Cacheable(value = RedisKeyConstants.STANDING_BOOK_MEASUREMENT_CODE_LIST, key = "'all'", unless = "#result == null || #result.isEmpty()")
    public List<String> getStandingbookCodeMeasurementList() {
        return standingbookAttributeMapper.getStandingbookCodeMeasurementList();
    }

    @Override
    public StandingbookExportVO getExcelData(Map<String, String> paramVO) {

        // 返回结果
        StandingbookExportVO resultVo = new StandingbookExportVO();

        // 0.校验type
        String typeId = paramVO.get(ATTR_TYPE_ID);
        AttributeTreeNodeTypeEnum attributeTreeNodeTypeEnum = validTypeId(typeId);
        if (Objects.isNull(attributeTreeNodeTypeEnum)) {
            throw exception(ErrorCodeConstants.STANDINGBOOK_TYPE_NOT_EXISTS);
        }

        // 1.文件名字处理
        String filename = null;
        switch (attributeTreeNodeTypeEnum) {
            case EQUIPMENT:
                filename = EQUIPMENT_STANDING_BOOK + XLSX;
                break;
            case MEASURING:
                filename = MEASURING_STANDING_BOOK + XLSX;
                break;
            default:
                filename = DEFAULT + XLSX;
        }
        resultVo.setFilename(filename);

        // 2.表头数据处理
        List<List<String>> headerList = ListUtils.newArrayList();
        headerList.add(Collections.singletonList(SB_TYPE.getDesc()));
        // 获取模版下的所有属性
        List<StandingbookAttributeDO> standingbookAttributeList = standingbookAttributeService.getStandingbookAttributeByTypeId(Long.valueOf(typeId));
        List<StandingbookAttributeDO> sottedAttributeList = standingbookAttributeList
                .stream()
                .filter(s -> {
                    String isRequired = s.getIsRequired();
                    return "0".equals(isRequired);
                })
                .sorted(Comparator.comparing(StandingbookAttributeDO::getSort))
                .collect(Collectors.toList());

        List<String> attributeNameList = sottedAttributeList
                .stream()
                .map(StandingbookAttributeDO::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        attributeNameList.forEach(a -> headerList.add(Collections.singletonList(a)));

        // 获取所有一级标签
        List<LabelConfigDO> labelList = labelConfigMapper.selectList(new LambdaQueryWrapperX<LabelConfigDO>()
                .eq(LabelConfigDO::getParentId, 0L)
                .orderByAsc(LabelConfigDO::getSort));

        labelList.forEach(l -> headerList.add(Collections.singletonList(l.getLabelName())));
        resultVo.setHeaderList(headerList);

        // 3.行数据处理
        List<List<Object>> dataList = ListUtils.newArrayList();


        List<String> attributeCodeList = sottedAttributeList
                .stream()
                .map(StandingbookAttributeDO::getCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<Long> labelIdList = labelList
                .stream()
                .map(LabelConfigDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 获取台账列表
        List<StandingbookDO> standingbookDOS = getStandingbookList(paramVO);
        if (CollUtil.isEmpty(standingbookDOS)) {
            return resultVo;
        }

        // 标签list转换成map
        Map<Long, LabelConfigDO> labelMap = labelConfigService.getAllLabelConfig()
                .stream()
                .collect(Collectors.toMap(LabelConfigDO::getId, Function.identity()));

        // 填充返回结果
        for (StandingbookDO standingbookDO : standingbookDOS) {
            List<Object> data = ListUtils.newArrayList();
            data.add(attributeTreeNodeTypeEnum.getDesc());

            // 3.1 固定数据
            List<StandingbookAttributeDO> attributes = standingbookDO.getChildren();

            if (CollUtil.isNotEmpty(attributes)) {
                Map<String, String> attributeCodeValueMap = attributes
                        .stream()
                        .collect(Collectors.toMap(StandingbookAttributeDO::getCode, StandingbookAttributeDO::getValue));

                attributeCodeList.forEach(a -> {
                    String s = attributeCodeValueMap.get(a);
                    data.add(s);
                });
            } else {
                attributeCodeList.forEach(a -> {
                    data.add(null);
                });
            }

            // 3.2 标签数据
            List<StandingbookLabelInfoDO> labelInfo = standingbookDO.getLabelInfo();

            if (CollUtil.isNotEmpty(labelInfo)) {
                Map<String, String> labelInfoNameValueMap = labelInfo
                        .stream()
                        .collect(Collectors.toMap(StandingbookLabelInfoDO::getName, StandingbookLabelInfoDO::getValue));

                labelIdList.forEach(l -> {
                    // 拼接name
                    String name = ATTR_LABEL_INFO_PREFIX + l;
                    String value = labelInfoNameValueMap.get(name);
                    String labelName = null;
                    if (CharSequenceUtil.isNotBlank(value)) {
                        String[] labelIds = value.split(StrPool.COMMA);
                        labelName = Arrays.stream(labelIds)
                                .map(labelId -> labelMap.get(Long.valueOf(labelId)).getLabelName())
                                .collect(Collectors.joining(StrPool.COMMA));
                    }

                    data.add(labelName);
                });
            } else {
                labelIdList.forEach(l -> {
                    data.add(null);
                });
            }

            dataList.add(data);
        }
        resultVo.setDataList(dataList);
        return resultVo;
    }

    private AttributeTreeNodeTypeEnum validTypeId(String typeIdStr) {

        if (CharSequenceUtil.isNotBlank(typeIdStr)) {
            Long typeId = Long.valueOf(typeIdStr);
            StandingbookTypeDO standingbookType = standingbookTypeService.getStandingbookType(typeId);

            if (Objects.nonNull(standingbookType)) {
                String topType = standingbookType.getTopType();
                if (CharSequenceUtil.isNotBlank(topType)) {

                    Integer id = Integer.valueOf(topType);
                    if (EQUIPMENT.getCode().equals(id)) {
                        return EQUIPMENT;
                    } else if (MEASURING.getCode().equals(id)) {
                        return MEASURING;
                    } else {
                        return null;
                    }
                }
            }
        }

        return null;
    }

    @Override
    @Cacheable(value = RedisKeyConstants.STANDING_BOOK_LIST, key = "'all'", unless = "#result == null || #result.isEmpty()")
    public List<StandingbookDTO> getStandingbookDTOList() {
        return standingbookAttributeMapper.getStandingbookDTO();
    }


    @Cacheable(value = RedisKeyConstants.STANDING_BOOK_CODE_KEYMAP, key = "'codeKeyAll'", unless = "#result == null || #result.isEmpty()")
    public Map<String, StandingBookHeaderDTO> getStandingBookCodeKeyMap() {
        List<StandingbookDTO> list = standingbookAttributeMapper.getStandingbookDTO();
        List<StandingBookHeaderDTO> standingBookHeaderDTOList = BeanUtils.toBean(list, StandingBookHeaderDTO.class);
        return CollectionUtils.convertMap(standingBookHeaderDTOList, StandingBookHeaderDTO::getCode, Function.identity());
    }

    /**
     * 根据excel表头来获取对应的台账信息
     *
     * @param headList 表头s
     * @return
     */
    @Override
    public List<StandingBookHeaderDTO> getStandingBookHeadersByHeaders(List<String> headList) {
        if (CollUtil.isEmpty(headList)) {
            return null;
        }

        // 去重 去空格
        List<String> collect = headList.stream().map(String::trim).distinct().collect(Collectors.toList());

        // 获取已有台账数据
        Map<String, StandingBookHeaderDTO> standingBookHeaderMap = getStandingBookCodeKeyMap();

        List<StandingBookHeaderDTO> standingBookHeaderDTOList = new ArrayList<>();

        for (String header : collect) {
            header = header.trim();
            String s = header.split(" ")[0];
            s = s.trim();
            for (String value : standingBookHeaderMap.keySet()) {
                // 编码完全匹配0 编码前部匹配1 编码后部匹配2
                if (s.equals(value) || value.startsWith(s) || value.endsWith(s)) {
                    StandingBookHeaderDTO standingBookHeader = standingBookHeaderMap.get(value);
                    standingBookHeader.setHeader(header);
                    standingBookHeaderDTOList.add(standingBookHeader);
                    break;
                }

                // 如果code包含了-D- 才会做去-操作
                if (s.contains("-D-")) {

                    String s1 = s.replaceAll("-(?![\\s\\S]*-)", "");

                    // 与清洗后的s 编码完全匹配5 编码前部匹配6 编码后部匹配7
                    if (s1.equals(value) || value.startsWith(s1) || value.endsWith(s1)) {
                        StandingBookHeaderDTO standingBookHeader = standingBookHeaderMap.get(value);
                        standingBookHeader.setHeader(header);
                        standingBookHeaderDTOList.add(standingBookHeader);
                        break;
                    }
                }
            }
        }
        return standingBookHeaderDTOList;
    }

    @Override
    public List<StandingBookTypeTreeRespVO> treeDeviceWithParam(StandingbookParamReqVO standingbookParamReqVO) {

        // 查询 指定 模糊名称的重点设备
        List<StandingBookTypeTreeRespVO> sbNodes = standingbookAttributeService.selectDeviceNodeByCodeAndName(null, standingbookParamReqVO.getActualSbName(), null);

        if (CollUtil.isEmpty(sbNodes)) {
            return Collections.emptyList();
        }
        List<Long> sbIds = sbNodes.stream().map(StandingBookTypeTreeRespVO::getRawId).collect(Collectors.toList());

        // 再进行 模糊搜索
        List<StandingBookTypeTreeRespVO> filterNodes = standingbookAttributeService.selectDeviceNodeByCodeAndName(standingbookParamReqVO.getSbCode(), standingbookParamReqVO.getSbName(), sbIds);
        if (CollUtil.isEmpty(filterNodes)) {
            return Collections.emptyList();
        }

        // 台账分类属性结构
        List<StandingbookTypeDO> standingbookTypeDOTree = standingbookTypeService.getStandingbookTypeByTopType(CommonConstants.KEY_EQUIPMENT_ID.intValue());
        return buildTreeWithDevices(standingbookTypeDOTree, filterNodes);
    }

    @Override
    public List<Long> getStandingBookIdsByStage(Integer stage) {
        return standingbookMapper.selectStandingbookIdByCondition(null, null, stage, null);
    }


    /**
     * 分类list和台账节点list
     *
     * @param categoryList
     * @param sbLeafNodes
     * @return
     */
    private List<StandingBookTypeTreeRespVO> buildTreeWithDevices(
            List<StandingbookTypeDO> categoryList,
            List<StandingBookTypeTreeRespVO> sbLeafNodes
    ) {
        // 1. 将计量器具（sbLeafNodes）分组：pNodeId -> List<leaf>
        Map<String, List<StandingBookTypeTreeRespVO>> leafGroupedByParent =
                sbLeafNodes.stream().collect(Collectors.groupingBy(StandingBookTypeTreeRespVO::getParentNodeId));

        // 2. 将分类转换为 VO 节点，标记为非叶子（show = false）
        Map<String, StandingBookTypeTreeRespVO> categoryMap = categoryList.stream()
                .map(cat -> {
                    StandingBookTypeTreeRespVO vo = new StandingBookTypeTreeRespVO();
                    vo.setNodeId(cat.getId() + StringPool.HASH + false);
                    vo.setParentNodeId(cat.getSuperId() + StringPool.HASH + false);
                    vo.setRawId(cat.getId());
                    vo.setNodeName(cat.getName());
                    vo.setShow(false);
                    vo.setChildren(new ArrayList<>());
                    return vo;
                })
                .collect(Collectors.toMap(StandingBookTypeTreeRespVO::getNodeId, Function.identity()));

        // 3. 将叶子节点挂到对应的分类上
        leafGroupedByParent.forEach((parentId, leaves) -> {
            StandingBookTypeTreeRespVO parent = categoryMap.get(parentId);
            if (parent != null) {
                parent.getChildren().addAll(leaves);
            }
        });

        // 4. 构造分类树结构
        List<StandingBookTypeTreeRespVO> roots = new ArrayList<>();
        for (StandingBookTypeTreeRespVO node : categoryMap.values()) {
            if (categoryMap.containsKey(node.getParentNodeId())) {
                StandingBookTypeTreeRespVO parent = categoryMap.get(node.getParentNodeId());
                parent.getChildren().add(node);
            } else {
                roots.add(node);
            }
        }

        // 5. 剪枝：递归保留含有计量器具的分支
        return roots.stream()
                .map(this::filterNodeWithLeaf)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 剪枝辅助方法：只保留包含叶子的节点
     *
     * @param node
     * @return
     */
    private StandingBookTypeTreeRespVO filterNodeWithLeaf(StandingBookTypeTreeRespVO node) {
        if (node.isShow()) {
            return node; // 是计量器具，保留
        }

        List<StandingBookTypeTreeRespVO> filteredChildren = Optional.ofNullable(node.getChildren())
                .orElse(Collections.emptyList())
                .stream()
                .map(this::filterNodeWithLeaf)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!filteredChildren.isEmpty()) {
            node.setChildren(filteredChildren);
            return node;
        }

        return null; // 没有叶子，不保留该分类节点
    }

    @Override
    public List<StandingbookDO> getByTypeIds(List<Long> typeIds) {
        if (CollUtil.isNotEmpty(typeIds)) {
            LambdaQueryWrapper<StandingbookDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(StandingbookDO::getTypeId, typeIds);
            return standingbookMapper.selectList(wrapper);
        } else {
            return new ArrayList<>();
        }

    }

    @Override
    public List<StandingbookDO> getByStandingbookIds(List<Long> standingbookIds) {
        return standingbookMapper.selectList(new LambdaQueryWrapper<StandingbookDO>().in(StandingbookDO::getId,
                standingbookIds));
    }

    /**
     * 递归查询出上级计量器具的id
     *
     * @param childId      当前计量器具id
     * @param associations 所有计量器具关联关系
     * @param parentIds    上级计量器具id集合
     */
    private static void findAllParentsRecursiveVirtual(Long childId, List<MeasurementVirtualAssociationDO> associations,
                                                       Set<Long> parentIds) {
        for (MeasurementVirtualAssociationDO association : associations) {
            if (association.getMeasurementId().equals(childId)) {
                Long parentId = association.getMeasurementInstrumentId();
                if (parentId != null && !parentIds.contains(parentId)) {
                    parentIds.add(parentId); // 添加父节点 ID
                    // 递归查找父节点的父节点
                    findAllParentsRecursiveVirtual(parentId, associations, parentIds);
                }
            }
        }
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
    @CacheEvict(value = {RedisKeyConstants.STANDING_BOOK_LIST,
            RedisKeyConstants.STANDING_BOOK_MEASUREMENT_CODE_LIST,
            RedisKeyConstants.STANDING_BOOK_DEVICE_CODE_LIST}, allEntries = true)
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
        String standingbookCode = StringPool.EMPTY;
        if (StandingbookTypeTopEnum.EQUIPMENT.getCode().equals(sb.getTopType())) {
            standingbookCode = createReqVO.get(ATTR_EQUIPMENT_ID);
            validateSbCodeUnique(standingbookCode);
        } else if (StandingbookTypeTopEnum.MEASURING_INSTRUMENT.getCode().equals(sb.getTopType())) {
            standingbookCode = createReqVO.get(ATTR_MEASURING_INSTRUMENT_ID);
            validateSbCodeUnique(standingbookCode);
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
        standingbookAttributeByTypeId.forEach(standingbookAttributeDO -> {
            StandingbookAttributeDO attribute = BeanUtils.toBean(standingbookAttributeDO, StandingbookAttributeDO.class);
            //根据code查询分类属性，
            attribute.setValue(createReqVO.get(attribute.getCode()));
            attribute.setStandingbookId(standingbook.getId());
            attribute.setId(null);
            attribute.setCreateTime(null);
            attribute.setUpdateTime(null);
            children.add(attribute);
        });

        // 新增台账属性
        standingbookAttributeMapper.insertBatch(children);

        // 新增台账-双碳映射
        if (StandingbookTypeTopEnum.EQUIPMENT.getCode().equals(sb.getTopType()) || StandingbookTypeTopEnum.MEASURING_INSTRUMENT.getCode().equals(sb.getTopType())) {
            doubleCarbonService.addMapping(standingbookCode);
        }
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
    @CacheEvict(value = {RedisKeyConstants.STANDING_BOOK_LIST,
            RedisKeyConstants.STANDING_BOOK_MEASUREMENT_CODE_LIST,
            RedisKeyConstants.STANDING_BOOK_DEVICE_CODE_LIST}, allEntries = true)
    public void updateStandingbook(Map<String, String> updateReqVO) {
        // 校验存在
        validateStandingbookExists(Long.valueOf(updateReqVO.get("id")));
        // 更新
        StandingbookDO standingbook = new StandingbookDO();
        standingbook.setTypeId(Long.valueOf(updateReqVO.get("typeId")));
        standingbook.setId(Long.valueOf(updateReqVO.get("id")));
        // 修改标签信息 先删后增
        if (StringUtils.isNotEmpty(updateReqVO.get(ATTR_LABEL_INFO))) {
            standingbookLabelInfoMapper.delete(new LambdaQueryWrapper<StandingbookLabelInfoDO>().eq(StandingbookLabelInfoDO::getStandingbookId, standingbook.getId()));
            createLabelInfoList(updateReqVO.get(ATTR_LABEL_INFO), standingbook.getId());
        }

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

    @Transactional
    @Override
    @CacheEvict(value = {RedisKeyConstants.STANDING_BOOK_LIST,
            RedisKeyConstants.STANDING_BOOK_MEASUREMENT_CODE_LIST,
            RedisKeyConstants.STANDING_BOOK_DEVICE_CODE_LIST}, allEntries = true)
    public void deleteStandingbookBatch(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        // 如果存在关联关系，则不删除台账
        Long count = measurementDeviceMapper.selectCount(new LambdaQueryWrapper<MeasurementDeviceDO>()
                .in(MeasurementDeviceDO::getDeviceId, ids)
                .or().in(MeasurementDeviceDO::getMeasurementInstrumentId, ids));
        if (count > 0) {
            throw exception(ErrorCodeConstants.STANDINGBOOK_ASSOCIATION_EXISTS);
        }
        count = measurementAssociationMapper.selectCount(new LambdaQueryWrapper<MeasurementAssociationDO>()
                .in(MeasurementAssociationDO::getMeasurementId, ids)
                .or().in(MeasurementAssociationDO::getMeasurementInstrumentId, ids));
        if (count > 0) {
            throw exception(ErrorCodeConstants.STANDINGBOOK_ASSOCIATION_EXISTS);
        }
        // 查询存在启用的数采关联
        List<StandingbookAcquisitionDO> standingbookAcquisitionList =
                standingbookAcquisitionService.queryListByStandingbookIds(ids);
        if (CollUtil.isNotEmpty(standingbookAcquisitionList)) {
            throw exception(ErrorCodeConstants.STANDINGBOOK_ACQUISITION_EXISTS);
        }
        // 查询存在告警配置
        if (warningStrategyService.existsByStandingbookIds(ids)) {
            throw exception(ErrorCodeConstants.STANDINGBOOK_REL_STRATEGY);
        }

        // 删除
        standingbookMapper.deleteByIds(ids);
        // 删除标签信息
        standingbookLabelInfoMapper.delete(new LambdaQueryWrapperX<StandingbookLabelInfoDO>()
                .inIfPresent(StandingbookLabelInfoDO::getStandingbookId, ids));
        // 删除属性
        standingbookAttributeMapper.delete(new LambdaQueryWrapperX<StandingbookAttributeDO>()
                .inIfPresent(StandingbookAttributeDO::getStandingbookId, ids));

        // 删除数采关联
        standingbookAcquisitionService.deleteByStandingbookIds(ids);
        // 删除数采配置redis缓存与台账对应的io地址缓存
        standingbookAcquisitionService.deleteRedisAcqConfigByStandingbookIds(ids);
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
    public List<StandingbookDO> getByIds(List<Long> ids) {

        //台账信息
        List<StandingbookDO> standingbookDOS =
                standingbookMapper.selectList(StandingbookDO::getId, ids);
        if (CollUtil.isEmpty(standingbookDOS)) {
            return Collections.emptyList();
        }

        //台账属性
        LambdaQueryWrapper<StandingbookAttributeDO> attributeQueryWrapper = new LambdaQueryWrapper<>();
        attributeQueryWrapper
                .select(StandingbookAttributeDO::getStandingbookId, StandingbookAttributeDO::getId,
                        StandingbookAttributeDO::getName, StandingbookAttributeDO::getValue,
                        StandingbookAttributeDO::getTypeId, StandingbookAttributeDO::getCode)
                .in(StandingbookAttributeDO::getStandingbookId, ids);
        List<StandingbookAttributeDO> standingbookAttributeDOS =
                standingbookAttributeMapper.selectList(attributeQueryWrapper);
        Map<Long, List<StandingbookAttributeDO>> attributeMap = new HashMap<>();
        if (CollUtil.isNotEmpty(standingbookAttributeDOS)) {
            attributeMap = standingbookAttributeDOS.stream().collect(Collectors.groupingBy(StandingbookAttributeDO::getStandingbookId));
        }

        //台账标签信息
        LambdaQueryWrapper<StandingbookLabelInfoDO> labelQueryWrapper = new LambdaQueryWrapper<>();
        labelQueryWrapper
                .select(StandingbookLabelInfoDO::getStandingbookId, StandingbookLabelInfoDO::getId,
                        StandingbookLabelInfoDO::getName, StandingbookLabelInfoDO::getValue)
                .in(StandingbookLabelInfoDO::getStandingbookId, ids);
        List<StandingbookLabelInfoDO> standingbookLabelInfoDOList =
                standingbookLabelInfoMapper.selectList(labelQueryWrapper);
        Map<Long, List<StandingbookLabelInfoDO>> labelInfoMap = new HashMap<>();
        if (CollUtil.isNotEmpty(standingbookAttributeDOS)) {
            labelInfoMap = standingbookLabelInfoDOList.stream().collect(Collectors.groupingBy(StandingbookLabelInfoDO::getStandingbookId));
        }
        Map<Long, List<StandingbookAttributeDO>> finalAttributeMap = attributeMap;
        Map<Long, List<StandingbookLabelInfoDO>> finalLabelInfoMap = labelInfoMap;


        standingbookDOS.forEach(standingbookDO -> {
            Long standingbookId = standingbookDO.getId();
            if (finalAttributeMap.containsKey(standingbookId)) {
                standingbookDO.addChildAll(finalAttributeMap.get(standingbookId));
            }

            if (finalLabelInfoMap.containsKey(standingbookId)) {
                standingbookDO.setLabelInfo(finalLabelInfoMap.get(standingbookId));
            }
        });


        return standingbookDOS;
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
            //逗号分割的数据 转为Long类型列表
            List<Long> energyIds = Arrays.stream(energy.split(StringPool.COMMA))
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
            energyTypeIds = standingbookTmplDaqAttrMapper.selectSbTypeIdsByEnergyIds(energyIds);
            if (CollUtil.isEmpty(energyTypeIds)) {
                return Collections.emptyList();
            }
        }

        // 分类多选条件(可能为空)
        List<String> sbTypeIdList = new ArrayList<>();
        String sbTypeIds = pageReqVO.get(ATTR_SB_TYPE_ID);
        if (StringUtils.isNotEmpty(sbTypeIds)) {
            sbTypeIdList = Arrays.stream(sbTypeIds.split(StringPool.HASH))
                    .map(s -> s.split(StringPool.COMMA))
                    .map(Arrays::stream)
                    .map(stream -> stream.reduce((first, second) -> second).orElse(""))
                    .collect(Collectors.toList());
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
        if (CollUtil.isNotEmpty(labelInfoConditions)) {
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
        List<StandingbookDO> result = getByIds(sbIds);

        return result;
    }

    /**
     * 只获取指定字段
     *
     * @param pageReqVO 条件map
     * @return
     */
    @Override
    public List<StandingbookRespVO> getSimpleStandingbookList(Map<String, String> pageReqVO) {
        List<StandingbookRespVO> result = BeanUtils.toBean(getStandingbookList(pageReqVO), StandingbookRespVO.class);

        if (CollUtil.isNotEmpty(result)) {
            return result.stream().map(r -> {
                List<StandingbookAttributeRespVO> childrens = r.getChildren();
                if (CollUtil.isNotEmpty(childrens)) {

                    // 计量器具名称
                    StandingbookAttributeRespVO measuringInstrumentName = childrens
                            .stream()
                            .filter(attribute -> ATTR_MEASURING_INSTRUMENT_MAME.equals(attribute.getCode())).findFirst()
                            .orElse(null);
                    if (Objects.nonNull(measuringInstrumentName)) {
                        r.setStandingbookName(measuringInstrumentName.getValue());
                    }

                    // 计量器具code
                    StandingbookAttributeRespVO measuringInstrumentId = childrens
                            .stream()
                            .filter(attribute -> ATTR_MEASURING_INSTRUMENT_ID.equals(attribute.getCode())).findFirst()
                            .orElse(null);
                    if (Objects.nonNull(measuringInstrumentId)) {
                        r.setStandingbookCode(measuringInstrumentId.getValue());
                    }
                }
                return r;
            }).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }

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
                    if (CollUtil.isNotEmpty(attributeDOS)) {
                        Optional<StandingbookAttributeDO> nameOptional = attributeDOS.stream().filter(attribute -> ATTR_MEASURING_INSTRUMENT_MAME.equals(attribute.getCode())).findFirst();
                        Optional<StandingbookAttributeDO> codeOptional = attributeDOS.stream().filter(attribute -> ATTR_MEASURING_INSTRUMENT_ID.equals(attribute.getCode())).findFirst();
                        associationData.setStandingbookName(nameOptional.map(StandingbookAttributeDO::getValue).orElse(StringPool.EMPTY));
                        associationData.setStandingbookCode(codeOptional.map(StandingbookAttributeDO::getValue).orElse(StringPool.EMPTY));
                    }
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

    /**
     * 查询所有台账关联的下级计量器具
     *
     * @param sbIds
     */
    @Override
    public Map<Long, List<MeasurementAssociationDO>> getSubStandingbookIdsBySbIds(List<Long> sbIds) {
        // 查询所有台账关联的下级计量器具
        List<MeasurementAssociationDO> assosicationSbList = measurementAssociationMapper.selectList(
                new LambdaQueryWrapper<MeasurementAssociationDO>().in(MeasurementAssociationDO::getMeasurementInstrumentId, sbIds));

        if (CollUtil.isNotEmpty(assosicationSbList)) {
            // 分组 台账id-下级计量器具们
            return assosicationSbList
                    .stream()
                    .collect(Collectors.groupingBy(MeasurementAssociationDO::getMeasurementInstrumentId));
        }

        return null;
    }

    /**
     * 查询所有台账关联的下级计量器具
     *
     * @param sbIds
     */
    @Override
    public Map<Long, List<MeasurementAssociationDO>> getUpStandingbookIdsBySbIds(List<Long> sbIds) {
        // 查询所有台账关联的下级计量器具
        List<MeasurementAssociationDO> assosicationSbList = measurementAssociationMapper.selectList(
                new LambdaQueryWrapper<MeasurementAssociationDO>().in(MeasurementAssociationDO::getMeasurementId, sbIds));

        if (CollUtil.isNotEmpty(assosicationSbList)) {
            // 分组 台账id-下级计量器具们
            return assosicationSbList
                    .stream()
                    .collect(Collectors.groupingBy(MeasurementAssociationDO::getMeasurementId));
        }
        return null;
    }

    @Override
    public List<StandingbookEnergyTypeVO> getEnergyAndTypeByStandingbookIds(List<Long> standingbookIds) {
        LambdaQueryWrapper<StandingbookDO> standingbookWrapper = new LambdaQueryWrapper<>();
        standingbookWrapper.in(StandingbookDO::getId, standingbookIds);
        List<StandingbookDO> standingbookDOS = standingbookMapper.selectList(standingbookWrapper);

        Set<Long> typeIds = standingbookDOS.stream().map(StandingbookDO::getTypeId).collect(Collectors.toSet());

        LambdaQueryWrapper<StandingbookTmplDaqAttrDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(StandingbookTmplDaqAttrDO::getTypeId, StandingbookTmplDaqAttrDO::getEnergyId);
        wrapper.in(StandingbookTmplDaqAttrDO::getTypeId, typeIds);
        wrapper.eq(StandingbookTmplDaqAttrDO::getEnergyFlag, true);
        wrapper.groupBy(StandingbookTmplDaqAttrDO::getTypeId, StandingbookTmplDaqAttrDO::getEnergyId);
        List<StandingbookTmplDaqAttrDO> standingbookTmplDaqAttrDOS = standingbookTmplDaqAttrMapper.selectList(wrapper);
        Map<Long, StandingbookTmplDaqAttrDO> typeTmplMap = standingbookTmplDaqAttrDOS.stream().collect(Collectors.toMap(StandingbookTmplDaqAttrDO::getTypeId, Function.identity()));

        List<StandingbookEnergyTypeVO> result = new ArrayList<>();
        standingbookDOS.forEach(standingbookDO -> {
            StandingbookEnergyTypeVO vo = new StandingbookEnergyTypeVO();
            vo.setStandingbookId(standingbookDO.getId());
            vo.setTypeId(standingbookDO.getTypeId());
            StandingbookTmplDaqAttrDO standingbookTmplDaqAttrDO = typeTmplMap.get(standingbookDO.getTypeId());
            vo.setEnergyId(standingbookTmplDaqAttrDO.getEnergyId());
            result.add(vo);
        });

        return result;
    }

    @Override
    public List<StandingbookEnergyTypeVO> getAllEnergyAndType() {
        LambdaQueryWrapper<StandingbookDO> standingbookWrapper = new LambdaQueryWrapper<>();
        standingbookWrapper.select(StandingbookDO::getId, StandingbookDO::getTypeId);
        List<StandingbookDO> standingbookDOS = standingbookMapper.selectList(standingbookWrapper);

        Set<Long> typeIds = standingbookDOS.stream().map(StandingbookDO::getTypeId).collect(Collectors.toSet());

        LambdaQueryWrapper<StandingbookTmplDaqAttrDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(StandingbookTmplDaqAttrDO::getTypeId, StandingbookTmplDaqAttrDO::getEnergyId);
        wrapper.in(StandingbookTmplDaqAttrDO::getTypeId, typeIds);
        wrapper.eq(StandingbookTmplDaqAttrDO::getEnergyFlag, true);
        wrapper.groupBy(StandingbookTmplDaqAttrDO::getTypeId, StandingbookTmplDaqAttrDO::getEnergyId);
        List<StandingbookTmplDaqAttrDO> standingbookTmplDaqAttrDOS = standingbookTmplDaqAttrMapper.selectList(wrapper);
        Map<Long, StandingbookTmplDaqAttrDO> typeTmplMap = standingbookTmplDaqAttrDOS.stream().collect(Collectors.toMap(StandingbookTmplDaqAttrDO::getTypeId, Function.identity()));

        List<StandingbookEnergyTypeVO> result = new ArrayList<>();
        standingbookDOS.forEach(standingbookDO -> {

            if (Objects.nonNull(typeTmplMap.get(standingbookDO.getTypeId()))) {
                StandingbookEnergyTypeVO vo = new StandingbookEnergyTypeVO();
                vo.setStandingbookId(standingbookDO.getId());
                vo.setTypeId(standingbookDO.getTypeId());
                StandingbookTmplDaqAttrDO standingbookTmplDaqAttrDO = typeTmplMap.get(standingbookDO.getTypeId());
                vo.setEnergyId(standingbookTmplDaqAttrDO.getEnergyId());
                result.add(vo);
            }


        });

        return result;
    }

    @Override
    public void exportMeterTemplate(HttpServletResponse response) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();

            // 列宽
            sheet.setColumnWidth(0, 28 * 256); // 计量器具编号
            sheet.setColumnWidth(1, 36 * 256); // 下级计量器具编号
            sheet.setColumnWidth(2, 36 * 256); // 关联设备
            sheet.setColumnWidth(3, 16 * 256); // 环节

            // —— 样式
            // 说明样式
            CellStyle noteStyle = wb.createCellStyle();
            noteStyle.setWrapText(true);
            noteStyle.setVerticalAlignment(VerticalAlignment.TOP);
            Font noteFont = wb.createFont();
            noteFont.setFontHeightInPoints((short) 11);
            noteStyle.setFont(noteFont);

            // 表头样式
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // 普通单元格样式
            CellStyle bodyStyle = wb.createCellStyle();
            bodyStyle.setBorderTop(BorderStyle.THIN);
            bodyStyle.setBorderBottom(BorderStyle.THIN);
            bodyStyle.setBorderLeft(BorderStyle.THIN);
            bodyStyle.setBorderRight(BorderStyle.THIN);
            bodyStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // —— 说明（A1:C1 合并）
            Row noteRow = sheet.createRow(0);
            noteRow.setHeightInPoints(46);
            Cell noteCell = noteRow.createCell(0);
            noteCell.setCellValue("说明：\n1、*为必填；\n2、下级计量器具有多个时请以英文“;”隔开。");
            noteCell.setCellStyle(noteStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            for (int c = 1; c <= 3; c++) {
                Cell tmp = noteRow.createCell(c);
                tmp.setCellStyle(noteStyle);
            }

            // —— 表头（第2行）
            Row header = sheet.createRow(1);
            header.setHeightInPoints(20);
            String[] headers = {"*计量器具编号", "下级计量器具编号", "关联设备", "环节"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // —— 示例数据（第3行）
            Row demo = sheet.createRow(2);
            demo.setHeightInPoints(18);
            String[] demoValues = {
                    "FMCS_051F_BLR01_FT11_PV",
                    "FMCS_051F_BLR01_FT12_PV;5103Fab",
                    "",
                    "购入存储"
            };
            for (int i = 0; i < demoValues.length; i++) {
                Cell cell = demo.createCell(i);
                cell.setCellValue(demoValues[i]);
                cell.setCellStyle(bodyStyle);
            }

            // —— 输出响应
            String fileName = URLEncoder.encode("计量器具导入模板.xlsx", StandardCharsets.UTF_8.name())
                    .replaceAll("\\+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
            // 可选：避免缓存
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.addHeader("File-Name", fileName);
            response.addHeader("Access-Control-Expose-Headers", "File-Name");
            wb.write(response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
            throw new RuntimeException("导出模板失败", e);
        }
    }

    @Override
    public void exportLedgerTemplate(HttpServletResponse response) throws UnsupportedEncodingException {
        String zipName = URLEncoder.encode("台账模板.zip", StandardCharsets.UTF_8.name())
                .replaceAll("\\+", "%20");
        response.setContentType("application/zip");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + zipName);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.addHeader("Access-Control-Expose-Headers", "File-Name");
        response.addHeader("File-Name", zipName);

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            // 1) 重点设备台账模版.xlsx
            writeWorkbookToZip(zos, LedgerType.DEVICE, "重点设备台账模版.xlsx");
            // 2) 计量器具台账模版.xlsx
            writeWorkbookToZip(zos, LedgerType.METER, "计量器具台账模版.xlsx");

            zos.finish();
            response.flushBuffer();
        } catch (Exception e) {
            throw new RuntimeException("导出台账模板失败", e);
        }
    }

    private void writeWorkbookToZip(ZipOutputStream zos, LedgerType type, String entryName) throws Exception {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        try (Workbook wb = new XSSFWorkbook()) {
            buildMainSheet(wb, type);       // Sheet1：台账（你的原逻辑 + 首行说明）
            buildDictionarySheet(wb, type); // Sheet2：数据字典（两列）
            wb.write(zos);
        }
        zos.closeEntry();
    }

    // ================= Sheet1：台账（固定列 + ems_label_config顶级标签名） =================
    private void buildMainSheet(Workbook wb, LedgerType type) {
        final String sheetName = (type == LedgerType.DEVICE) ? "重点设备台账" : "计量器具台账";
        Sheet sheet = wb.createSheet(sheetName);

        int col = 0;
        sheet.setColumnWidth(col++, 18 * 256); // 设备分类
        if (type == LedgerType.METER) {
            sheet.setColumnWidth(col++, 14 * 256); // 表类型（仅 meter 主表需要）
        }
        sheet.setColumnWidth(col++, 18 * 256); // 设备名称
        sheet.setColumnWidth(col++, 20 * 256); // 设备编号

        // 取 ems_label_config：未删除 & parent_id IS NULL/0 的顶级标签名，作为追加列头
        List<String> topLabelNames = loadTopLevelLabelNames();
        for (int i = 0; i < topLabelNames.size(); i++) {
            sheet.setColumnWidth(col + i, 18 * 256);
        }

        // ① 首行“说明”（索引 0），合并 A1 ~ 最后一列
        CellStyle noteStyle = wb.createCellStyle();
        noteStyle.setWrapText(true);
        noteStyle.setVerticalAlignment(VerticalAlignment.TOP);
        noteStyle.setAlignment(HorizontalAlignment.LEFT);
        Font noteFont = wb.createFont();
        noteFont.setFontHeightInPoints((short) 11);
        noteStyle.setFont(noteFont);

        Row noteRow = sheet.createRow(0);
        noteRow.setHeightInPoints(46);
        Cell noteCell = noteRow.createCell(0);
        noteCell.setCellValue("说明：\n1、*为必填；\n2、请按照模板格式填写或选择对应在信息。");
        noteCell.setCellStyle(noteStyle);

        // 计算总列数用于合并
        int totalCols = (type == LedgerType.METER ? 4 : 3) + topLabelNames.size();
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, totalCols - 1));
        // 合并区域内其余单元格也套上样式，避免选中出现差异
        for (int c = 1; c < totalCols; c++) {
            Cell tmp = noteRow.createCell(c);
            tmp.setCellStyle(noteStyle);
        }

        // ② 表头（索引 1）
        CellStyle headerStyle = headerStyle(wb);
        Row header = sheet.createRow(1);
        header.setHeightInPoints(20);

        int idx = 0;
        createCell(header, idx++, "*设备分类", headerStyle);
        if (type == LedgerType.METER) {
            createCell(header, idx++, "*表类型", headerStyle);
        }
        createCell(header, idx++, "设备名称", headerStyle);
        createCell(header, idx++, "*设备编号", headerStyle);
        for (String label : topLabelNames) {
            createCell(header, idx++, label, headerStyle);
        }

    }

    // ================= Sheet2：数据字典（两列固定） =================
    private void buildDictionarySheet(Workbook wb, LedgerType type) {
        Sheet sheet = wb.createSheet("数据字典");
        CellStyle headerStyle = headerStyle(wb);

        // 列头
        Row head = sheet.createRow(0);
        head.setHeightInPoints(20);
        sheet.setColumnWidth(0, 22 * 256);
        sheet.setColumnWidth(1, 22 * 256);
        createCell(head, 0, "设备分类", headerStyle);
        createCell(head, 1, "标签", headerStyle);

        // 数据
        List<String> categories = loadAllStandingbookTypesDisplay();
        List<String> allTypes = loadAllLabelConfigDisplay();

        int maxRows = Math.max(categories.size(), allTypes.size());
        for (int i = 0; i < maxRows; i++) {
            Row row = sheet.createRow(i + 1);
            String v1 = (i < categories.size()) ? categories.get(i) : "";
            String v2 = (i < allTypes.size()) ? allTypes.get(i) : "";
            row.createCell(0).setCellValue(v1);
            row.createCell(1).setCellValue(v2);
        }
    }


    private CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void createCell(Row row, int col, String text, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(text);
        cell.setCellStyle(style);
    }


    enum LedgerType {
        DEVICE, METER;

        static LedgerType from(String s) {
            if ("device".equalsIgnoreCase(s)) return DEVICE;
            if ("meter".equalsIgnoreCase(s)) return METER;
            throw new IllegalArgumentException("type 仅支持 device|meter, 实际: " + s);
        }

        String fileName() {
            return (this == DEVICE) ? "重点设备台账模版.xlsx" : "计量器具台账模版.xlsx";
        }
    }

    public List<String> loadTopLevelLabelNames() {
        List<LabelConfigDO> rows = labelConfigMapper.selectList(
                Wrappers.<LabelConfigDO>lambdaQuery()
                        .eq(LabelConfigDO::getDeleted, false)
                        .and(w -> w.isNull(LabelConfigDO::getParentId).or().eq(LabelConfigDO::getParentId, 0L))
                        .orderByAsc(LabelConfigDO::getSort, LabelConfigDO::getId)
        );
        return rows.stream()
                .map(LabelConfigDO::getLabelName)
                .filter(s -> s != null && !s.trim().isEmpty())
                .collect(Collectors.toList());
    }

    public List<LabelConfigDO> loadTopLevelLabelNamesList() {
        List<LabelConfigDO> rows = labelConfigMapper.selectList(
                Wrappers.<LabelConfigDO>lambdaQuery()
                        .eq(LabelConfigDO::getDeleted, false)
                        .and(w -> w.isNull(LabelConfigDO::getParentId).or().eq(LabelConfigDO::getParentId, 0L))
                        .orderByAsc(LabelConfigDO::getSort, LabelConfigDO::getId)
        );
        return rows.stream()
                .filter(s -> s != null && s.getLabelName() != null && !s.getLabelName().trim().isEmpty()) // Added null check for getLabelName()
                .collect(Collectors.toList()); // 将过滤后的 Stream 收集回 List
    }

    /**
     * 字典页第1列：ems_label_config 全部未删除 → label_name（code）
     */
    private List<String> loadAllLabelConfigDisplay() {
        List<LabelConfigDO> rows = labelConfigMapper.selectList(
                Wrappers.<LabelConfigDO>lambdaQuery()
                        .eq(LabelConfigDO::getDeleted, false)
                        .orderByAsc(LabelConfigDO::getSort, LabelConfigDO::getId)
        );
        return rows.stream()
                .map(r -> {
                    String name = r.getLabelName() == null ? "" : r.getLabelName().trim();
                    String code = r.getCode() == null ? "" : r.getCode().trim();
                    return String.format("%s(%s)", name, code);
                })
                .collect(Collectors.toList());
    }

    /**
     * 字典页第2列：power_standingbook_type 全部未删除 → name（code）
     */
    private List<String> loadAllStandingbookTypesDisplay() {
        List<StandingbookTypeDO> rows = standingbookTypeMapper.selectList(
                Wrappers.<StandingbookTypeDO>lambdaQuery()
                        .eq(StandingbookTypeDO::getDeleted, false)
                        .orderByAsc(StandingbookTypeDO::getSort, StandingbookTypeDO::getId)
        );
        return rows.stream()
                .map(r -> {
                    String name = r.getName() == null ? "" : r.getName().trim();
                    String code = r.getCode() == null ? "" : r.getCode().trim();
                    return String.format("%s(%s)", name, code);
                })
                .collect(Collectors.toList());
    }


}
