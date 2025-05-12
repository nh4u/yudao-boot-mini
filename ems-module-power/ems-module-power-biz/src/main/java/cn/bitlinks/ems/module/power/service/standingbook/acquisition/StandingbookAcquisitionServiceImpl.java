package cn.bitlinks.ems.module.power.service.standingbook.acquisition;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.common.util.opcda.ItemStatus;
import cn.bitlinks.ems.framework.common.util.opcda.OpcDaUtils;
import cn.bitlinks.ems.framework.dict.core.DictFrameworkUtils;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.servicesettings.ServiceSettingsDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.acquisition.StandingbookAcquisitionDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.acquisition.StandingbookAcquisitionDetailDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import cn.bitlinks.ems.module.power.dal.mysql.servicesettings.ServiceSettingsMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.acquisition.StandingbookAcquisitionDetailMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.acquisition.StandingbookAcquisitionMapper;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.bitlinks.ems.module.power.service.standingbook.acquisition.dto.ParameterKey;
import cn.bitlinks.ems.module.power.service.standingbook.tmpl.StandingbookTmplDaqAttrService;
import cn.bitlinks.ems.module.power.utils.CalculateUtil;
import cn.hutool.core.collection.CollUtil;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.IExpressContext;
import org.mapstruct.ap.internal.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ApiConstants.*;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.SERVICE_NAME_FORMAT;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.SPRING_PROFILES_ACTIVE_PROD;
import static cn.bitlinks.ems.module.power.enums.DictTypeConstants.ACQUISITION_FREQUENCY;
import static cn.bitlinks.ems.module.power.enums.DictTypeConstants.ACQUISITION_PROTOCOL;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

/**
 * 台账-数采设置 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class StandingbookAcquisitionServiceImpl implements StandingbookAcquisitionService {

    @Resource
    private StandingbookAcquisitionMapper standingbookAcquisitionMapper;
    @Resource
    private StandingbookAcquisitionDetailMapper standingbookAcquisitionDetailMapper;
    @Resource
    private ServiceSettingsMapper serviceSettingsMapper;
    @Resource
    private StandingbookService standingbookService;
    @Resource
    private StandingbookTmplDaqAttrService standingbookTmplDaqAttrService;
    @Value("${spring.profiles.active}")
    private String env;

    @Override
    public Long createOrUpdateStandingbookAcquisition(StandingbookAcquisitionVO updateReqVO) {
        // 0.对公式进行解析检查，生成到io级别的实际公式, 处理填充具体的公式，
        List<StandingbookAcquisitionDetailVO> detailVOS = expandFormulas(updateReqVO.getDetails());

        // 1.没有id的，进行新增操作
        if (Objects.isNull(updateReqVO.getId())) {
            // 1.1 添加数采设置
            StandingbookAcquisitionDO standingbookAcquisition = BeanUtils.toBean(updateReqVO, StandingbookAcquisitionDO.class);
            standingbookAcquisitionMapper.insert(standingbookAcquisition);
            // 1.2 添加数采设置详情
//            List<StandingbookAcquisitionDetailVO> detailVOS =
//                    updateReqVO.getDetails();
            if (CollUtil.isEmpty(detailVOS)) {
                // 返回
                return standingbookAcquisition.getId();
            }
            List<StandingbookAcquisitionDetailDO> detailDOS = BeanUtils.toBean(detailVOS,
                    StandingbookAcquisitionDetailDO.class);
            detailDOS.forEach(detailDO -> detailDO.setAcquisitionId(standingbookAcquisition.getId()));
            standingbookAcquisitionDetailMapper.insertBatch(detailDOS);
            // 返回
            return standingbookAcquisition.getId();
        }

        // 2.有id的，进行更新操作
        // 2.1 更新数采设置
        StandingbookAcquisitionDO standingbookAcquisition = BeanUtils.toBean(updateReqVO, StandingbookAcquisitionDO.class);
        standingbookAcquisitionMapper.updateById(standingbookAcquisition);
        // 2.2 新增/更新数采设置详情，按照数采参数是否有详情id区分新增/更新操作
//        List<StandingbookAcquisitionDetailVO> detailVOS =
//                updateReqVO.getDetails();
        if (CollUtil.isEmpty(detailVOS)) {
            // 返回
            return updateReqVO.getId();
        }

        List<StandingbookAcquisitionDetailDO> detailDOS = BeanUtils.toBean(detailVOS,
                StandingbookAcquisitionDetailDO.class);

        Map<Boolean, List<StandingbookAcquisitionDetailDO>> partitionedDetails = detailDOS.stream()
                .collect(Collectors.partitioningBy(detailDO -> Objects.isNull(detailDO.getId())));
        List<StandingbookAcquisitionDetailDO> newDetails = partitionedDetails.get(true);
        List<StandingbookAcquisitionDetailDO> updatedDetails = partitionedDetails.get(false);
        // 2.2.1新增
        if (CollUtil.isNotEmpty(newDetails)) {
            newDetails.forEach(detailDO -> detailDO.setAcquisitionId(standingbookAcquisition.getId()));
            standingbookAcquisitionDetailMapper.insertBatch(newDetails);
        }

        // 2.2.2更新
        if (CollUtil.isNotEmpty(updatedDetails)) {
            standingbookAcquisitionDetailMapper.updateBatch(updatedDetails);
        }

        // 返回
        return updateReqVO.getId();
    }


    @Override
    public List<StandingbookAcquisitionRespVO> getStandingbookAcquisitionList(Map<String, String> queryReqVO) {
        List<StandingbookDO> standingbookDOS = standingbookService.getStandingbookList(queryReqVO);
        if (CollUtil.isEmpty(standingbookDOS)) {
            return Collections.emptyList();
        }
        List<Long> sbIds = standingbookDOS.stream().map(StandingbookDO::getId).collect(Collectors.toList());
        // 查询数采设置
        List<StandingbookAcquisitionDO> standingbookAcquisitionDOS =
                standingbookAcquisitionMapper.selectList(StandingbookAcquisitionDO::getStandingbookId, sbIds);
        if (CollUtil.isEmpty(standingbookAcquisitionDOS)) {
            return Collections.emptyList();
        }
        Map<Long, StandingbookAcquisitionDO> standingbookAcquisitionMap = standingbookAcquisitionDOS.stream()
                .collect(Collectors.toMap(StandingbookAcquisitionDO::getStandingbookId, Function.identity()));
        // 查询服务设置
        List<Long> serviceSettingsIdList =
                standingbookAcquisitionDOS.stream().map(StandingbookAcquisitionDO::getServiceSettingsId).collect(Collectors.toList());
        List<ServiceSettingsDO> serviceSettingsDOS = serviceSettingsMapper.selectList(ServiceSettingsDO::getId,
                serviceSettingsIdList);
        Map<Long, ServiceSettingsDO> serviceSettingsMap = new HashMap<>();
        if (CollUtil.isNotEmpty(serviceSettingsDOS)) {
            serviceSettingsMap = serviceSettingsDOS.stream()
                    .collect(Collectors.toMap(ServiceSettingsDO::getId, Function.identity()));
        }


        List<StandingbookAcquisitionRespVO> result = new ArrayList<>();
        for (StandingbookDO standingbookDO : standingbookDOS) {
            // 查询关联的数采设置主要信息
            // StandingbookAcquisitionRespVO standingbookAcquisitionRespVO = BeanUtils.toBean(standingbookDO,
            //         StandingbookAcquisitionRespVO.class);
            StandingbookAcquisitionRespVO standingbookAcquisitionRespVO = BeanUtils.toBean(standingbookDO, StandingbookAcquisitionRespVO.class);
            StandingbookAcquisitionDO standingbookAcquisitionDO = standingbookAcquisitionMap.get(standingbookDO.getId());
            if (Objects.isNull(standingbookAcquisitionDO)) {
                result.add(standingbookAcquisitionRespVO);
                continue;
            }

            BeanUtils.copyProperties(standingbookAcquisitionDO, standingbookAcquisitionRespVO);
            standingbookAcquisitionRespVO.setAcquisitionId(standingbookAcquisitionDO.getId());
            standingbookAcquisitionRespVO.setId(standingbookDO.getId());
            // 采集频率(展示)
            standingbookAcquisitionRespVO.setFrequencyLabel(
                    standingbookAcquisitionRespVO.getFrequency() + DictFrameworkUtils.getDictDataLabel(ACQUISITION_FREQUENCY,
                            standingbookAcquisitionRespVO.getFrequencyUnit()));
            // 数据连接服务(展示)
            ServiceSettingsDO serviceSettingsDO =
                    serviceSettingsMap.get(standingbookAcquisitionDO.getServiceSettingsId());
            standingbookAcquisitionRespVO.setServiceSettingsLabel(String.format(SERVICE_NAME_FORMAT, serviceSettingsDO.getServiceName(),
                    serviceSettingsDO.getIpAddress(), serviceSettingsDO.getPort(),
                    DictFrameworkUtils.getDictDataLabel(ACQUISITION_PROTOCOL,
                            serviceSettingsDO.getProtocol())));
            result.add(standingbookAcquisitionRespVO);
        }
        return result;
    }


    @Override
    public StandingbookAcquisitionVO getAcquisitionByStandingbookId(Long standingbookId) {
        // 1.1 查询台账关联的数采设置
        StandingbookAcquisitionDO standingbookAcquisitionDO =
                standingbookAcquisitionMapper.selectOne(StandingbookAcquisitionDO::getStandingbookId,
                        standingbookId);
        // 1.2 查询台账的数采属性（启用的）
        List<StandingbookTmplDaqAttrDO> standingbookTmplDaqAttrDOS =
                standingbookTmplDaqAttrService.getDaqAttrsByStandingbookId(standingbookId);

        // 2.1 无数采设置情况下，组装数采参数结构数据给前端
        if (Objects.isNull(standingbookAcquisitionDO)) {
            StandingbookAcquisitionVO standingbookAcquisitionVO = new StandingbookAcquisitionVO();
            standingbookAcquisitionVO.setStandingbookId(standingbookId);
            // 2.1.1 数采参数为空，
            if (CollUtil.isEmpty(standingbookTmplDaqAttrDOS)) {
                return standingbookAcquisitionVO;
            }
            // 2.1.2 数采参数不为空，填充台账对应的数采参数
            List<StandingbookAcquisitionDetailVO> detailVOS = new ArrayList<>();
            standingbookTmplDaqAttrDOS.forEach(standingbookTmplDaqAttrDO -> {
                StandingbookAcquisitionDetailAttrDTO standingbookAcquisitionDetailAttrDTO =
                        BeanUtils.toBean(standingbookTmplDaqAttrDO,
                                StandingbookAcquisitionDetailAttrDTO.class);
                StandingbookAcquisitionDetailVO standingbookAcquisitionDetailVO =
                        BeanUtils.toBean(standingbookAcquisitionDetailAttrDTO,
                                StandingbookAcquisitionDetailVO.class);
                detailVOS.add(standingbookAcquisitionDetailVO);
            });
            standingbookAcquisitionVO.setDetails(detailVOS);
            return standingbookAcquisitionVO;
        }
        // 2.2 有数采设置情况下，组装数采参数结构给前端
        StandingbookAcquisitionVO standingbookAcquisitionVO = BeanUtils.toBean(standingbookAcquisitionDO,
                StandingbookAcquisitionVO.class);

        if (CollUtil.isEmpty(standingbookTmplDaqAttrDOS)) {
            return standingbookAcquisitionVO;
        }
        // 根据数采设置查询数采参数详情,与台账参数进行比对，
        List<StandingbookAcquisitionDetailDO> detailDOS =
                standingbookAcquisitionDetailMapper.selectList(StandingbookAcquisitionDetailDO::getAcquisitionId,
                        standingbookAcquisitionDO.getId());
        List<StandingbookAcquisitionDetailVO> detailVOS = new ArrayList<>();
        standingbookTmplDaqAttrDOS.forEach(standingbookTmplDaqAttrDO -> {
            // 如果该参数能在数采参数中找到，则直接展示，否则需要根据台账模板手动填充，相当于新增的
            StandingbookAcquisitionDetailVO standingbookAcquisitionDetailVO = new StandingbookAcquisitionDetailVO();

            if (CollUtil.isNotEmpty(detailDOS)) {
                Optional<StandingbookAcquisitionDetailDO> detailDOOptional = detailDOS.stream()
                        .filter(detailDO -> detailDO.getCode().equals(standingbookTmplDaqAttrDO.getCode()) && detailDO.getEnergyFlag().equals(standingbookTmplDaqAttrDO.getEnergyFlag())).findFirst();
                if (detailDOOptional.isPresent()) {
                    standingbookAcquisitionDetailVO = BeanUtils.toBean(detailDOOptional.get(), StandingbookAcquisitionDetailVO.class);
                }
            }
            // 复制参数其他必须的属性过去给前端展示
            StandingbookAcquisitionDetailAttrDTO standingbookAcquisitionDetailAttrDTO =
                    BeanUtils.toBean(standingbookTmplDaqAttrDO,
                            StandingbookAcquisitionDetailAttrDTO.class);
            BeanUtils.copyProperties(standingbookAcquisitionDetailAttrDTO, standingbookAcquisitionDetailVO);

            detailVOS.add(standingbookAcquisitionDetailVO);
        });
        standingbookAcquisitionVO.setDetails(detailVOS);
        return standingbookAcquisitionVO;
    }


    @Override
    public String testData(StandingbookAcquisitionTestReqVO testReqVO) {

        // 获取当前的参数设置
        StandingbookAcquisitionDetailVO currentDetail = testReqVO.getCurrentDetail();
        String dataSite = currentDetail.getDataSite();
        String formula = currentDetail.getFormula();
        // 0.未配置io未配置公式
        if (Strings.isEmpty(dataSite) && Strings.isEmpty(formula)) {
            return STANDINGBOOK_ACQUISITION_FAIL;
        }
        // 0.获取服务设置
        ServiceSettingsDO serviceSettingsDO = serviceSettingsMapper.selectById(testReqVO.getServiceSettingsId());
        if (Objects.isNull(serviceSettingsDO)) {
            throw exception(SERVICE_SETTINGS_NOT_EXISTS);
        }
        try {
            // 1. 配置了io，配置了公式/未配置公式
            if (Strings.isNotEmpty(dataSite)) {
                // 采集参数
                Map<String, ItemStatus> itemStatusMap;
                if (env.equals(SPRING_PROFILES_ACTIVE_PROD)) {
                    itemStatusMap = OpcDaUtils.batchGetValue(serviceSettingsDO.getIpAddress(),
                            serviceSettingsDO.getUsername(),
                            serviceSettingsDO.getPassword(),
                            serviceSettingsDO.getClsid(), Collections.singletonList(dataSite));
                } else {
                    itemStatusMap = mockItemStatus(Collections.singletonList(dataSite));
                }

                if (CollUtil.isEmpty(itemStatusMap)) {
                    return STANDINGBOOK_ACQUISITION_FAIL;
                }
                // 1.1 未配置公式
                if (Strings.isEmpty(formula)) {
                    return String.format(STANDINGBOOK_ACQUISITION_SUCCESS, itemStatusMap.get(dataSite).getValue());
                }
                // 1.2 配置了公式，替换自身参数部分进行计算
                String currenParam = String.format(PATTERN_ACQUISITION_FORMULA_FILL, currentDetail.getCode(),
                        currentDetail.getEnergyFlag());
                IExpressContext<String, Object> context = new DefaultContext<>();
                context.put(currenParam, itemStatusMap.get(dataSite).getValue());
                return String.format(STANDINGBOOK_ACQUISITION_SUCCESS, CalculateUtil.calcAcquisitionFormula(formula, context));
            }
            // 2. 未配置io配置了公式, 需要计算出本身的公式

            // 创建一个 Map，用于存储参数的唯一标识 (ParameterKey) 到 StandingbookAcquisitionDetailVO 对象的映射
            Map<ParameterKey, StandingbookAcquisitionDetailVO> paramMap = new HashMap<>();
            for (StandingbookAcquisitionDetailVO detail : testReqVO.getDetails()) {
                ParameterKey key = new ParameterKey(detail.getCode(), detail.getEnergyFlag());
                paramMap.put(key, detail);
            }

            // 计算当前公式的真实公式
            StandingbookAcquisitionDetailVO currentFormulaDetail = expandFormula(currentDetail, paramMap, new HashSet<>());
            // 2.1 需要找到当前的参数设置的真实公式，然后找到依赖的参数，获取他们的dataSite，
            Set<ParameterKey> parameterKeys = getDependencies(currentFormulaDetail.getActualFormula());
            // 配置了公式但不需要依赖任何参数，公式必须包含参数，所以公式不对。
            if (CollUtil.isEmpty(parameterKeys)) {
                return STANDINGBOOK_ACQUISITION_FAIL;
            }

            Map<ParameterKey, StandingbookAcquisitionDetailVO> relyParamMap = paramMap.entrySet().stream()
                    .filter(entry -> parameterKeys.contains(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            if (CollUtil.isEmpty(relyParamMap)) {
                return STANDINGBOOK_ACQUISITION_FAIL;
            }
            List<String> dataSites = relyParamMap.values().stream().map(StandingbookAcquisitionDetailVO::getDataSite).collect(Collectors.toList());
            // 2.2 采集这些参数，
            // 采集参数
            Map<String, ItemStatus> itemStatusMap;
            if (env.equals(SPRING_PROFILES_ACTIVE_PROD)) {
                itemStatusMap = OpcDaUtils.batchGetValue(serviceSettingsDO.getIpAddress(),
                        serviceSettingsDO.getUsername(),
                        serviceSettingsDO.getPassword(),
                        serviceSettingsDO.getClsid(), dataSites);
            } else {
                itemStatusMap = mockItemStatus(dataSites);
            }

            if (CollUtil.isEmpty(itemStatusMap)) {
                return STANDINGBOOK_ACQUISITION_FAIL;
            }
            // 将计算后的数值替换到公式中，
            IExpressContext<String, Object> context = new DefaultContext<>();
            relyParamMap.forEach((parameterKey, detailVO) -> {
                String relyParam = String.format(PATTERN_ACQUISITION_FORMULA_FILL, detailVO.getCode(), currentDetail.getEnergyFlag());
                context.put(relyParam, itemStatusMap.get(detailVO.getDataSite()).getValue());
            });
            // 根据公式进行计算返回结果
            return String.format(STANDINGBOOK_ACQUISITION_SUCCESS, CalculateUtil.calcAcquisitionFormula(formula, context));

        } catch (Exception e) {
            return STANDINGBOOK_ACQUISITION_FAIL;
        }
    }

    /**
     * 用于非生产环境，测试数据
     *
     * @return 参数值为下标值，参与公式计算
     */
    private Map<String, ItemStatus> mockItemStatus(List<String> dataSites) {
        if (CollUtil.isEmpty(dataSites)) {
            return Collections.emptyMap();
        }
        Map<String, ItemStatus> itemStatusMap = new HashMap<>();
        // 设置索引值
        IntStream.range(0, dataSites.size())
                .forEach(index -> {
                    ItemStatus itemStatus = new ItemStatus();
                    itemStatus.setItemId(dataSites.get(index));
                    itemStatus.setValue(Integer.toString(index + 1));
                    itemStatus.setTime(LocalDateTime.now());
                    itemStatusMap.put(dataSites.get(index), itemStatus);
                });
        return itemStatusMap;
    }


    /**
     * 展开 List<StandingbookAcquisitionDetailVO> details 中所有参数的公式，直到不再包含其他参数引用。
     * 使用 visited 集合来防止循环引用导致的无限递归。
     *
     * @param details StandingbookAcquisitionDetailVO 列表
     * @return 展开后的公式列表
     */
    private static List<StandingbookAcquisitionDetailVO> expandFormulas(List<StandingbookAcquisitionDetailVO> details) {
        if (details == null || details.isEmpty()) {
            return new ArrayList<>();
        }

        // 创建一个 Map，用于存储参数的唯一标识 (ParameterKey) 到 StandingbookAcquisitionDetailVO 对象的映射
        Map<ParameterKey, StandingbookAcquisitionDetailVO> paramMap = new HashMap<>();
        for (StandingbookAcquisitionDetailVO detail : details) {
            ParameterKey key = new ParameterKey(detail.getCode(), detail.getEnergyFlag());
            paramMap.put(key, detail);
        }

        List<StandingbookAcquisitionDetailVO> expandedDetails = new ArrayList<>();
        for (StandingbookAcquisitionDetailVO detail : details) {
            StandingbookAcquisitionDetailVO expandedDetail = expandFormula(detail, paramMap, new HashSet<>());
            expandedDetails.add(expandedDetail);
        }

        return expandedDetails;
    }

    /**
     * 展开单个 StandingbookAcquisitionDetailVO 对象的公式。
     * 使用 visited 集合来防止循环引用导致的无限递归。
     *
     * @param detail   要展开公式的 StandingbookAcquisitionDetailVO 对象
     * @param paramMap 参数的唯一标识到 StandingbookAcquisitionDetailVO 对象的映射
     * @param visited  已访问的参数集合，用于防止循环引用，避免无限递归。
     * @return 展开后的 StandingbookAcquisitionDetailVO 对象
     */
    private static StandingbookAcquisitionDetailVO expandFormula(StandingbookAcquisitionDetailVO detail, Map<ParameterKey, StandingbookAcquisitionDetailVO> paramMap, Set<ParameterKey> visited) {
        // 复制原始对象，避免修改原始列表
        StandingbookAcquisitionDetailVO expandedDetail = BeanUtils.toBean(detail, StandingbookAcquisitionDetailVO.class);

        if (Strings.isEmpty(expandedDetail.getFormula())) {
            return expandedDetail; // 公式为空，无需展开
        }

        ParameterKey currentKey = new ParameterKey(expandedDetail.getCode(), expandedDetail.getEnergyFlag());

        // 检查是否已经访问过该参数，防止循环引用
        if (visited.contains(currentKey)) {
            throw exception(STANDINGBOOK_ACQUISITION_CYCLE_RELY);
        }

        visited.add(currentKey);

        String expandedFormula = expandedDetail.getFormula();
        Set<ParameterKey> dependencies = getDependencies(expandedFormula);

        // 替换公式中的参数引用
        for (ParameterKey dependency : dependencies) {
            if (paramMap.containsKey(dependency)) {
                StandingbookAcquisitionDetailVO dependencyDetail = paramMap.get(dependency);

                // 递归展开依赖参数的公式
                StandingbookAcquisitionDetailVO fullyExpandedDependency = expandFormula(dependencyDetail, paramMap, visited);
                String dataSite = fullyExpandedDependency.getDataSite();
                if (Strings.isEmpty(dataSite)) {
                    throw exception(STANDINGBOOK_ACQUISITION_FORMULA_SET);
                }

                // 如果依赖参数也有公式，则使用其展开后的公式进行替换。
                String replacement = fullyExpandedDependency.getFormula();
                if (Strings.isNotEmpty(replacement)) {
                    // 将公式中的参数引用替换为实际的公式
                    expandedFormula = expandedFormula.replace(String.format(PATTERN_ACQUISITION_FORMULA_FILL, dependency.getCode(), dependency.getEnergyFlag()), replacement);
//                    expandedFormula = expandedFormula.replace("{[\"" + dependency.getCode() + "\"," + dependency.getEnergyFlag() + "]}", replacement);
                }
            }
        }

        expandedDetail.setFormula(expandedFormula);
        visited.remove(currentKey);

        return expandedDetail;
    }

    /**
     * 从公式中提取依赖的参数。
     * 例如，从 "{[\"A\",true]}*4.2+{[\"B\",false]}" 提取出 ParameterKey(A, false) 和 ParameterKey(B, true)
     *
     * @param formula 公式字符串
     * @return 依赖的参数编码的集合
     */
    private static Set<ParameterKey> getDependencies(String formula) {
        Set<ParameterKey> dependencies = new HashSet<>();
        if (formula == null || formula.isEmpty()) {
            return dependencies;
        }

        // 使用正则表达式匹配公式中的参数引用，例如 String aa = "{[\"C\",\"true\"]}*3";
        //Pattern pattern = Pattern.compile("\\{\\[\"([^\"]+)\",(true|false|\"[^\"]+\")\\]\\}");
        Matcher matcher = PATTERN_ACQUISITION_FORMULA_PARAM.matcher(formula);

        while (matcher.find()) {
            String code = matcher.group(1);
            String energyFlagStr = matcher.group(2);
            boolean energyFlag = Boolean.TRUE.toString().equals(energyFlagStr);
            dependencies.add(new ParameterKey(code, energyFlag));
        }

        return dependencies;
    }

}