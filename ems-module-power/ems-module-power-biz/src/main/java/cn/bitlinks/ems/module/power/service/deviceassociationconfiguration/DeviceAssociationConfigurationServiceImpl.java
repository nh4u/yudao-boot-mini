package cn.bitlinks.ems.module.power.service.deviceassociationconfiguration;

import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.DeviceAssociationSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.MeasurementAssociationSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.measurementassociation.MeasurementAssociationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.measurementdevice.MeasurementDeviceDO;
import cn.bitlinks.ems.module.power.dal.mysql.measurementassociation.MeasurementAssociationMapper;
import cn.bitlinks.ems.module.power.dal.mysql.measurementdevice.MeasurementDeviceMapper;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备关联配置 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class DeviceAssociationConfigurationServiceImpl implements DeviceAssociationConfigurationService {

    @Resource
    private MeasurementDeviceMapper measurementDeviceMapper;
    @Resource
    private MeasurementAssociationMapper measurementAssociationMapper;

    @Override
    public void updAssociationMeasurementInstrument(MeasurementAssociationSaveReqVO createReqVO) {

        List<Long> ids = createReqVO.getMeasurementIds();

        List<MeasurementAssociationDO> list = measurementAssociationMapper.selectList(new LambdaQueryWrapper<MeasurementAssociationDO>()
                .eq(MeasurementAssociationDO::getMeasurementInstrumentId, createReqVO.getMeasurementInstrumentId()));
        if (CollUtil.isEmpty(list)) {
            ids.forEach(id -> {
                MeasurementAssociationDO measurementAssociationDO = new MeasurementAssociationDO();
                measurementAssociationDO.setMeasurementId(createReqVO.getMeasurementInstrumentId());
                measurementAssociationDO.setMeasurementInstrumentId(id);
                list.add(measurementAssociationDO);
            });
            measurementAssociationMapper.insertBatch(list);
            return;
        }
        // 1. 找出需要删除的关联
        List<Long> toDelete = list.stream()
                .filter(association -> !ids.contains(association.getMeasurementId()))
                .map(MeasurementAssociationDO::getId)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(toDelete)) {
            measurementAssociationMapper.deleteByIds(toDelete);
        }

        // 2. 找出需要新增的关联 ID
        List<Long> toAddIds = ids.stream()
                .filter(id -> list.stream().noneMatch(association -> association.getId().equals(id)))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(toAddIds)) {
            return;
        }
        List<MeasurementAssociationDO> toAddList = new ArrayList<>();
        toAddIds.forEach(id -> {
            MeasurementAssociationDO measurementAssociationDO = new MeasurementAssociationDO();
            measurementAssociationDO.setMeasurementId(createReqVO.getMeasurementInstrumentId());
            measurementAssociationDO.setMeasurementInstrumentId(id);
            toAddList.add(measurementAssociationDO);
        });
        measurementAssociationMapper.insertBatch(toAddList);

    }

    @Override
    public void updAssociationDevice(DeviceAssociationSaveReqVO createReqVO) {
        MeasurementDeviceDO existing = measurementDeviceMapper.selectOne(new LambdaQueryWrapper<MeasurementDeviceDO>()
                .eq(MeasurementDeviceDO::getMeasurementInstrumentId, createReqVO.getMeasurementInstrumentId()));
        if (existing == null) {
            MeasurementDeviceDO measurementDeviceDO = new MeasurementDeviceDO();
            measurementDeviceDO.setDeviceId(createReqVO.getDeviceId());
            measurementDeviceDO.setMeasurementInstrumentId(createReqVO.getMeasurementInstrumentId());
            measurementDeviceMapper.insert(measurementDeviceDO);
            return;
        }
        measurementDeviceMapper.update(new LambdaUpdateWrapper<MeasurementDeviceDO>()
                .set(MeasurementDeviceDO::getDeviceId, createReqVO.getDeviceId())
                .eq(MeasurementDeviceDO::getMeasurementInstrumentId, createReqVO.getMeasurementInstrumentId()));

    }

}