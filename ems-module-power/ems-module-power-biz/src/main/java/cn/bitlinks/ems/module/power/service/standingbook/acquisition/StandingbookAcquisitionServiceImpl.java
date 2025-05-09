package cn.bitlinks.ems.module.power.service.standingbook.acquisition;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.dict.core.DictFrameworkUtils;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo.StandingbookAcquisitionDetailAttrDTO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo.StandingbookAcquisitionDetailVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo.StandingbookAcquisitionRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo.StandingbookAcquisitionVO;
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
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.module.power.enums.CommonConstants.SERVICE_NAME_FORMAT;
import static cn.bitlinks.ems.module.power.enums.DictTypeConstants.ACQUISITION_FREQUENCY;
import static cn.bitlinks.ems.module.power.enums.DictTypeConstants.ACQUISITION_PROTOCOL;

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

    @Override
    public Long createOrUpdateStandingbookAcquisition(StandingbookAcquisitionVO updateReqVO) {
        // 1.没有id的，进行新增操作
        if (Objects.isNull(updateReqVO.getId())) {
            // 1.1 添加数采设置
            StandingbookAcquisitionDO standingbookAcquisition = BeanUtils.toBean(updateReqVO, StandingbookAcquisitionDO.class);
            standingbookAcquisitionMapper.insert(standingbookAcquisition);
            // 1.2 添加数采设置详情
            List<StandingbookAcquisitionDetailVO> detailVOS =
                    updateReqVO.getDetails();
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
        List<StandingbookAcquisitionDetailVO> detailVOS =
                updateReqVO.getDetails();
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
            StandingbookAcquisitionRespVO standingbookAcquisitionRespVO = BeanUtils.toBean(standingbookDO, StandingbookAcquisitionRespVO.class);
            StandingbookAcquisitionDO standingbookAcquisitionDO = standingbookAcquisitionMap.get(standingbookDO.getId());
            if (Objects.isNull(standingbookAcquisitionDO)) {
                result.add(standingbookAcquisitionRespVO);
                continue;
            }

            BeanUtils.copyProperties(standingbookAcquisitionDO, standingbookAcquisitionRespVO);
            standingbookAcquisitionRespVO.setAcquisitionId(standingbookAcquisitionDO.getId());

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

}