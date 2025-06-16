package cn.bitlinks.ems.module.power.service.standingbook.acquisition;

import cn.bitlinks.ems.framework.common.core.ParameterKey;
import cn.bitlinks.ems.framework.common.core.StandingbookAcquisitionDetailDTO;
import cn.bitlinks.ems.framework.common.exception.ServiceException;
import cn.bitlinks.ems.framework.common.util.calc.AcquisitionFormulaUtils;
import cn.bitlinks.ems.framework.common.util.calc.FormulaUtil;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.common.util.opcda.ItemStatus;
import cn.bitlinks.ems.framework.common.util.opcda.OpcConnectionTester;
import cn.bitlinks.ems.framework.dict.core.DictFrameworkUtils;
import cn.bitlinks.ems.module.acquisition.api.quartz.QuartzApi;
import cn.bitlinks.ems.module.acquisition.api.quartz.dto.AcquisitionJobDTO;
import cn.bitlinks.ems.module.acquisition.api.quartz.dto.ServiceSettingsDTO;
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
import cn.bitlinks.ems.module.power.service.standingbook.tmpl.StandingbookTmplDaqAttrService;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cn.bitlinks.ems.framework.common.enums.CommonConstants.PATTERN_ACQUISITION_FORMULA_FILL;
import static cn.bitlinks.ems.framework.common.enums.CommonConstants.SPRING_PROFILES_ACTIVE_LOCAL;
import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.SERVICE_NAME_FORMAT;
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
@Slf4j
public class StandingbookAcquisitionServiceImpl implements StandingbookAcquisitionService {

    @Resource
    private StandingbookAcquisitionMapper standingbookAcquisitionMapper;
    @Resource
    private StandingbookAcquisitionDetailMapper standingbookAcquisitionDetailMapper;
    @Resource
    private ServiceSettingsMapper serviceSettingsMapper;
    @Lazy
    @Resource
    private StandingbookService standingbookService;
    @Resource
    private StandingbookTmplDaqAttrService standingbookTmplDaqAttrService;
    @Value("${spring.profiles.active}")
    private String env;
    @Resource
    private QuartzApi quartzApi;

    @Override
    @Transactional
    public Long createOrUpdateStandingbookAcquisition(StandingbookAcquisitionVO updateReqVO) {
        // 0.对公式进行解析检查，生成到io级别的实际公式, 处理填充具体的公式，
        List<StandingbookAcquisitionDetailVO> detailVOS = expandFormulas(updateReqVO.getDetails());

        // 查询服务设置
        ServiceSettingsDO serviceSettingsDO = serviceSettingsMapper.selectById(updateReqVO.getServiceSettingsId());
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
            // ***需要创建【定时任务】
            createOrUpdateJob(updateReqVO, detailVOS, serviceSettingsDO);
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
            // 【更新定时任务】
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
        // 【更新定时任务】
        createOrUpdateJob(updateReqVO, detailVOS, serviceSettingsDO);

        // 返回
        return updateReqVO.getId();
    }

    /**
     * 设备数采定时任务调用
     *
     * @param updateReqVO 数采设置详情
     * @param detailVOS   添加了真实公式的部分
     */
    private void createOrUpdateJob(StandingbookAcquisitionVO updateReqVO,
                                   List<StandingbookAcquisitionDetailVO> detailVOS,
                                   ServiceSettingsDO serviceSettingsDO) {
        // 【更新定时任务]
        AcquisitionJobDTO acquisitionJobDTO = new AcquisitionJobDTO();
        acquisitionJobDTO.setStatus(updateReqVO.getStatus());
        acquisitionJobDTO.setStandingbookId(updateReqVO.getStandingbookId());
        acquisitionJobDTO.setJobStartTime(updateReqVO.getStartTime());
        acquisitionJobDTO.setFrequency(updateReqVO.getFrequency());
        acquisitionJobDTO.setFrequencyUnit(updateReqVO.getFrequencyUnit());
        acquisitionJobDTO.setDetails(BeanUtils.toBean(detailVOS, StandingbookAcquisitionDetailDTO.class));
        acquisitionJobDTO.setServiceSettingsDTO(BeanUtils.toBean(serviceSettingsDO, ServiceSettingsDTO.class));
        quartzApi.createOrUpdateJob(acquisitionJobDTO);
    }


    @Override
    public List<StandingbookAcquisitionRespVO> getStandingbookAcquisitionList(Map<String, String> queryReqVO) {
        List<StandingbookDO> standingbookDOS = standingbookService.getStandingbookList(queryReqVO);
        if (CollUtil.isEmpty(standingbookDOS)) {
            return Collections.emptyList();
        }
        List<Long> sbIds = standingbookDOS.stream().map(StandingbookDO::getId).collect(Collectors.toList());
        // 查询台账对应的数采设置
        List<StandingbookAcquisitionDO> standingbookAcquisitionDOS =
                standingbookAcquisitionMapper.selectList(StandingbookAcquisitionDO::getStandingbookId, sbIds);
        if (CollUtil.isEmpty(standingbookAcquisitionDOS)) {
            return BeanUtils.toBean(standingbookDOS, StandingbookAcquisitionRespVO.class);
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
        try {
            // 获取当前的参数设置
            StandingbookAcquisitionDetailVO currentDetail = testReqVO.getCurrentDetail();
            String dataSite = currentDetail.getDataSite();
            String formula = currentDetail.getFormula();
            // 0.未配置io未配置公式
            if (StringUtils.isEmpty(dataSite) && StringUtils.isEmpty(formula)) {
                throw exception(STANDINGBOOK_ACQUISITION_TEST_FAIL);
            }
            currentDetail.setActualFormula(currentDetail.getFormula());
            // 创建一个 Map，用于存储参数的唯一标识 (ParameterKey) 到 StandingbookAcquisitionDetailVO 对象的映射
            Map<ParameterKey, StandingbookAcquisitionDetailVO> paramMap = new HashMap<>();
            Map<ParameterKey, StandingbookAcquisitionDetailDTO> paramDTOMap = new HashMap<>();
            for (StandingbookAcquisitionDetailVO detail : testReqVO.getDetails()) {
                ParameterKey key = new ParameterKey(detail.getCode(), detail.getEnergyFlag());
                detail.setActualFormula(detail.getFormula());
                paramMap.put(key, detail);
                StandingbookAcquisitionDetailDTO detailDTO = BeanUtils.toBean(detail,
                        StandingbookAcquisitionDetailDTO.class);
                paramDTOMap.put(key, detailDTO);
            }
            // 获取真实公式
            StandingbookAcquisitionDetailVO currentFormulaDetail = expandFormula(currentDetail, paramMap, new HashSet<>());
            StandingbookAcquisitionDetailDTO currentDetailDTO = BeanUtils.toBean(currentFormulaDetail,
                    StandingbookAcquisitionDetailDTO.class);

            // 0.获取服务设置
            ServiceSettingsDO serviceSettingsDO = serviceSettingsMapper.selectById(testReqVO.getServiceSettingsId());
            if (Objects.isNull(serviceSettingsDO)) {
                throw exception(SERVICE_SETTINGS_NOT_EXISTS);
            }

            // 2.要么有io要么有公式, 获取真实公式,可能为空
            String actualFormula = currentFormulaDetail.getActualFormula();
            List<String> dataSites;
            if (StringUtils.isNotEmpty(actualFormula)) {
                // 2.1 需要找到当前的参数设置的真实公式，然后找到依赖的参数，获取他们的dataSite，
                Set<ParameterKey> parameterKeys = FormulaUtil.getDependencies(currentFormulaDetail.getActualFormula());
                // 配置了公式但不需要依赖任何参数，公式必须包含参数，所以公式不对。
                if (CollUtil.isEmpty(parameterKeys)) {
                    throw exception(STANDINGBOOK_ACQUISITION_TEST_FAIL);
                }

                Map<ParameterKey, StandingbookAcquisitionDetailVO> relyParamMap = paramMap.entrySet().stream()
                        .filter(entry -> parameterKeys.contains(entry.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                if (CollUtil.isEmpty(relyParamMap)) {
                    throw exception(STANDINGBOOK_ACQUISITION_TEST_FAIL);
                }
                dataSites = relyParamMap.values().stream().map(StandingbookAcquisitionDetailVO::getDataSite).collect(Collectors.toList());
            } else {
                dataSites = Collections.singletonList(dataSite);
            }

            // 2.2 采集这些参数，
            Map<String, ItemStatus> itemStatusMap;
            if (env.equals(SPRING_PROFILES_ACTIVE_LOCAL)) {
                itemStatusMap = mockItemStatus(dataSites);
            } else {
                itemStatusMap = OpcConnectionTester.testLink(serviceSettingsDO.getIpAddress(),
                        serviceSettingsDO.getUsername(),
                        serviceSettingsDO.getPassword(),
                        serviceSettingsDO.getClsid(), dataSites);
            }
            if (CollUtil.isEmpty(itemStatusMap)) {
                throw exception(STANDINGBOOK_ACQUISITION_TEST_FAIL);
            }

            String resultValue = AcquisitionFormulaUtils.calcSingleParamValue(currentDetailDTO, paramDTOMap,
                    itemStatusMap);
            if (Objects.isNull(resultValue)) {
                throw exception(STANDINGBOOK_ACQUISITION_TEST_FAIL);
            }
            return resultValue;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("数采-测试，异常：{}", e.getMessage(), e);
            throw exception(STANDINGBOOK_ACQUISITION_TEST_FAIL);
        }

    }

    @Override
    @Transactional
    public void deleteByStandingbookIds(List<Long> ids) {
        List<StandingbookAcquisitionDO> list = queryListByStandingbookIds(ids);
        if (CollUtil.isEmpty(list)) {
            return;
        }
        List<Long> acquisitionIds = list.stream().map(StandingbookAcquisitionDO::getId).collect(Collectors.toList());
        standingbookAcquisitionMapper.deleteByIds(acquisitionIds);
        standingbookAcquisitionDetailMapper.delete(StandingbookAcquisitionDetailDO::getAcquisitionId, ids);
    }

    @Override
    public List<StandingbookAcquisitionDO> queryListByStandingbookIds(List<Long> ids) {
        return standingbookAcquisitionMapper.selectList(new LambdaQueryWrapper<StandingbookAcquisitionDO>()
                .eq(StandingbookAcquisitionDO::getStatus, true)
                .in(StandingbookAcquisitionDO::getStandingbookId, ids)
        );
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
            detail.setActualFormula(detail.getFormula());
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

        if (StringUtils.isEmpty(expandedDetail.getActualFormula())) {
            return expandedDetail; // 公式为空，无需展开
        }

        if (StringUtils.isNotEmpty(expandedDetail.getDataSite())) {
            return expandedDetail; // 是配置了io地址的，无需展开。
        }

        ParameterKey currentKey = new ParameterKey(expandedDetail.getCode(), expandedDetail.getEnergyFlag());

        // 检查是否已经访问过该参数，防止循环引用
        if (visited.contains(currentKey)) {
            throw exception(STANDINGBOOK_ACQUISITION_CYCLE_RELY);
        }

        visited.add(currentKey);

        String expandedFormula = expandedDetail.getActualFormula();
        Set<ParameterKey> dependencies = FormulaUtil.getDependencies(expandedFormula);
        // 如果当前参数没有依赖其他参数，则直接返回
        if (dependencies.contains(currentKey) && dependencies.size() == 1) {
            expandedDetail.setActualFormula(expandedFormula);
            visited.remove(currentKey);

            return expandedDetail;
        }

        // 替换公式中的参数引用
        for (ParameterKey dependency : dependencies) {
            if (paramMap.containsKey(dependency)) {
                StandingbookAcquisitionDetailVO dependencyDetail = paramMap.get(dependency);
                // 如果io地址不为空，无需展开
                if (StringUtils.isNotEmpty(dependencyDetail.getDataSite())) {
                    continue;
                }
                // 递归展开依赖参数的公式
                StandingbookAcquisitionDetailVO fullyExpandedDependency = expandFormula(dependencyDetail, paramMap, visited);

                // 如果依赖参数也有公式，则使用其展开后的公式进行替换。
                String replacement = fullyExpandedDependency.getActualFormula();
                if (StringUtils.isNotEmpty(replacement)) {
                    // 将公式中的参数引用替换为实际的公式
                    expandedFormula = expandedFormula.replace(String.format(PATTERN_ACQUISITION_FORMULA_FILL,
                            dependency.getCode(), dependency.getEnergyFlag()), "(" + replacement + ")");
//                    expandedFormula = expandedFormula.replace("{[\"" + dependency.getCode() + "\"," + dependency.getEnergyFlag() + "]}", replacement);
                }
            }
        }

        expandedDetail.setActualFormula(expandedFormula);
        visited.remove(currentKey);

        return expandedDetail;
    }


}