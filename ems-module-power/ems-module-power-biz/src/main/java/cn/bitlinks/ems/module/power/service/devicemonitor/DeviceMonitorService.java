package cn.bitlinks.ems.module.power.service.devicemonitor;

import cn.bitlinks.ems.framework.common.enums.CommonStatusEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.infra.api.config.ConfigApi;
import cn.bitlinks.ems.module.power.controller.admin.minitor.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookDTO;
import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.WarningInfoStatisticsRespVO;
import cn.bitlinks.ems.module.power.dal.dataobject.labelconfig.LabelConfigDO;
import cn.bitlinks.ems.module.power.dal.dataobject.monitor.DeviceMonitorQrcodeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookLabelInfoDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.warninginfo.WarningInfoDO;
import cn.bitlinks.ems.module.power.dal.mysql.monitor.DeviceMonitorQrcodeMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.attribute.StandingbookAttributeMapper;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.bitlinks.ems.module.power.service.standingbook.label.StandingbookLabelInfoService;
import cn.bitlinks.ems.module.power.service.standingbook.type.StandingbookTypeService;
import cn.bitlinks.ems.module.power.service.warninginfo.WarningInfoService;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.DATE_RANGE_EXCEED_LIMIT;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.END_TIME_MUST_AFTER_START_TIME;
import static cn.bitlinks.ems.module.power.enums.warninginfo.WarningTemplateKeyWordEnum.WARNING_DETAIL_LINK;

@Service
public class DeviceMonitorService {
    @Resource
    @Lazy
    private StandingbookService standingbookService;
    @Resource
    @Lazy
    private StandingbookTypeService standingbookTypeService;
    @Resource
    @Lazy
    private StandingbookLabelInfoService standingbookLabelInfoService;
    @Resource
    @Lazy
    private LabelConfigService labelConfigService;
    @Resource
    @Lazy
    private WarningInfoService warningInfoService;

    @Resource
    private StandingbookAttributeMapper standingbookAttributeMapper;
    @Resource
    private DeviceMonitorQrcodeMapper deviceMonitorQrcodeMapper;
    @Resource
    private ConfigApi configApi;

    static final String INIT_DEVICE_LINK = "power.device.monitor.url";

    public DeviceMonitorWarningRespVO getWarningInfo(@Valid DeviceMonitorWarningReqVO reqVO) {
        DeviceMonitorWarningRespVO respVO = new DeviceMonitorWarningRespVO();
        // 校验时间范围合法性
        LocalDateTime[] rangeOrigin = reqVO.getRange();
        LocalDateTime startTime = rangeOrigin[0];
        LocalDateTime endTime = rangeOrigin[1];
        if (!startTime.isBefore(endTime)) {
            throw exception(END_TIME_MUST_AFTER_START_TIME);
        }
        if (!LocalDateTimeUtils.isWithinDays(startTime, endTime, CommonConstants.YEAR)) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }
        // 查询设备信息
        List<StandingbookDTO> standingbookDTOS = standingbookService.getStandingbookDTOList();
        StandingbookDTO standingbookDTO = standingbookDTOS.stream()
                .filter(dto -> dto != null &&
                        Objects.equals(dto.getStandingbookId(), reqVO.getSbId()))
                .findFirst()
                .orElse(null);
        if (standingbookDTO != null) {
            List<WarningInfoDO> warningInfoDOList = warningInfoService.getMonitorListBySbCode(reqVO.getRange(), standingbookDTO.getCode());
            respVO.setList(warningInfoDOList);
            WarningInfoStatisticsRespVO statisticsRespVO = warningInfoService.getMonitorStatisticsBySbCode(standingbookDTO.getCode());
            respVO.setStatistics(statisticsRespVO);
        }

        return respVO;
    }

    /**
     * 获取设备信息
     *
     * @param reqVO
     * @return
     */
    public DeviceMonitorDeviceRespVO getDeviceInfo(@Valid DeviceMonitorDeviceReqVO reqVO) {
        DeviceMonitorDeviceRespVO respVO = new DeviceMonitorDeviceRespVO();
        // 查询设备信息
        List<StandingbookDTO> standingbookDTOS = standingbookService.getStandingbookDTOList();
        StandingbookDTO standingbookDTO = standingbookDTOS.stream()
                .filter(dto -> dto != null &&
                        Objects.equals(dto.getStandingbookId(), reqVO.getSbId()))
                .findFirst()
                .orElse(null);
        respVO.setCode(standingbookDTO.getCode());
        respVO.setName(standingbookDTO.getName());
        respVO.setSbId(standingbookDTO.getStandingbookId());
        // 查询设备能耗状态
        long count = warningInfoService.countMonitorBySbCode(standingbookDTO.getCode());
        if (count > 0) {
            respVO.setStatus(CommonStatusEnum.DISABLE.getStatus());
        } else {
            respVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        }
        // 查询设备图片信息
        List<StandingbookAttributeDO> attributeDOS =
                standingbookAttributeMapper.selectList(new LambdaQueryWrapperX<StandingbookAttributeDO>()
                        .eq(StandingbookAttributeDO::getStandingbookId, reqVO.getSbId()));
        Optional<StandingbookAttributeDO> paramOptional = attributeDOS.stream()
                .filter(attribute -> attribute.getCode().equals("picture"))

                .findFirst();
        paramOptional.ifPresent(standingbookAttributeDO -> respVO.setImage(standingbookAttributeDO.getValue()));

        // 查询设备动态标签属性值
        List<StandingbookLabelInfoDO> labels = standingbookLabelInfoService.getByStandingBookId(reqVO.getSbId());
        if (CollUtil.isEmpty(labels)) {
            return respVO;
        }

        List<LabelConfigDO> labelConfigDOList = labelConfigService.getAllLabelConfig();
        Map<Long, LabelConfigDO> labelConfigDOMap = labelConfigDOList.stream()
                .collect(Collectors.toMap(LabelConfigDO::getId, Function.identity()));
        List<DeviceMonitorDeviceLabel> labelList = new ArrayList<>();
        labels.forEach(labelInfo -> {
            DeviceMonitorDeviceLabel result = new DeviceMonitorDeviceLabel();
            String topLabelKey = labelInfo.getName();
            Long topLabelId = Long.valueOf(topLabelKey.substring(topLabelKey.indexOf("_") + 1));
            result.setName(labelConfigDOMap.get(topLabelId).getLabelName());
            String value = labelInfo.getValue();
            if (StringUtils.isNotBlank(value)) {
                // 取最后一个勾选的值
                String[] parts = value.split(StringPool.COMMA);
                if (parts.length > 0) {
                    result.setValue(labelConfigDOMap.get(Long.parseLong(parts[parts.length - 1].trim())).getLabelName());
                }
            }
            labelList.add(result);
        });
        respVO.setLabels(labelList);
        return respVO;
    }

    public String getQrCode(@Valid DeviceMonitorDeviceReqVO reqVO) {
        StandingbookDO standingbookDO = standingbookService.getById(reqVO.getSbId());
        StandingbookTypeDO standingbookTypeDO = standingbookTypeService.getStandingbookType(standingbookDO.getTypeId());
        // 设备详情跳转链接
        String initLink = configApi.getConfigValueByKey(INIT_DEVICE_LINK).getCheckedData();
        String url = String.format(initLink,
                reqVO.getSbId(), standingbookTypeDO.getTopType());
        String qrCode =url+ "&token=" +  UUID.randomUUID();
        // 拼接token
        DeviceMonitorQrcodeDO qrcodeDO = new DeviceMonitorQrcodeDO();qrcodeDO.setDeviceId(reqVO.getSbId());
        qrcodeDO.setQrcode(qrCode);

        // 删除所有的链接信息
        deviceMonitorQrcodeMapper.delete(new LambdaQueryWrapperX<DeviceMonitorQrcodeDO>().eq(DeviceMonitorQrcodeDO::getDeviceId,reqVO.getSbId()));
        // 保存新的链接信息
        deviceMonitorQrcodeMapper.insert(qrcodeDO);
        return qrCode;
    }

    public Boolean validQrCode(String code) {
        DeviceMonitorQrcodeDO exist = deviceMonitorQrcodeMapper.selectOne(new LambdaQueryWrapperX<DeviceMonitorQrcodeDO>().eq(DeviceMonitorQrcodeDO::getQrcode, code));
        return Objects.nonNull(exist);
    }
}
