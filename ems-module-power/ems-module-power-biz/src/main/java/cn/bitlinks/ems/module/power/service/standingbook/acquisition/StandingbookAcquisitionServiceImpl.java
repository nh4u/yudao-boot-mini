package cn.bitlinks.ems.module.power.service.standingbook.acquisition;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.dict.core.DictFrameworkUtils;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo.StandingbookAcquisitionRespVO;
import cn.bitlinks.ems.module.power.dal.dataobject.servicesettings.ServiceSettingsDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.acquisition.StandingbookAcquisitionDO;
import cn.bitlinks.ems.module.power.dal.mysql.servicesettings.ServiceSettingsMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.acquisition.StandingbookAcquisitionMapper;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
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
    private StandingbookService standingbookService;
    @Resource
    private ServiceSettingsMapper serviceSettingsMapper;

//    @Override
//    public Long createStandingbookAcquisition(StandingbookAcquisitionSaveReqVO createReqVO) {
//        // 插入
//        StandingbookAcquisitionDO standingbookAcquisition = BeanUtils.toBean(createReqVO, StandingbookAcquisitionDO.class);
//        standingbookAcquisitionMapper.insert(standingbookAcquisition);
//        // 返回
//        return standingbookAcquisition.getId();
//    }
//
//    @Override
//    public void updateStandingbookAcquisition(StandingbookAcquisitionSaveReqVO updateReqVO) {
//        // 校验存在
//        validateStandingbookAcquisitionExists(updateReqVO.getId());
//        // 更新
//        StandingbookAcquisitionDO updateObj = BeanUtils.toBean(updateReqVO, StandingbookAcquisitionDO.class);
//        standingbookAcquisitionMapper.updateById(updateObj);
//    }
//
//    @Override
//    public void deleteStandingbookAcquisition(Long id) {
//        // 校验存在
//        validateStandingbookAcquisitionExists(id);
//        // 删除
//        standingbookAcquisitionMapper.deleteById(id);
//    }
//
//    private void validateStandingbookAcquisitionExists(Long id) {
//        if (standingbookAcquisitionMapper.selectById(id) == null) {
//            throw exception(STANDINGBOOK_ACQUISITION_NOT_EXISTS);
//        }
//    }
//
//    @Override
//    public StandingbookAcquisitionDO getStandingbookAcquisition(Long id) {
//        return standingbookAcquisitionMapper.selectById(id);
//    }
//
//    @Override
//    public PageResult<StandingbookAcquisitionDO> getStandingbookAcquisitionPage(StandingbookAcquisitionPageReqVO pageReqVO) {
//        return standingbookAcquisitionMapper.selectPage(pageReqVO);
//    }

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

}