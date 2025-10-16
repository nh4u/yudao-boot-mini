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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;

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
    @Transactional
    public void updAssociationMeasurementInstrument(MeasurementAssociationSaveReqVO createReqVO) {
        // 删除原有关联关系
        measurementAssociationMapper.delete(new LambdaQueryWrapper<MeasurementAssociationDO>()
                .eq(MeasurementAssociationDO::getMeasurementInstrumentId, createReqVO.getMeasurementInstrumentId()));
        List<Long> ids = createReqVO.getMeasurementIds();
        Set<Long> idsSet = new HashSet<>(ids);
        if (CollUtil.isEmpty(idsSet)) {
            return;
        }
        List<MeasurementAssociationDO> list = new ArrayList<>();
        idsSet.forEach(id -> {
            MeasurementAssociationDO measurementAssociationDO = new MeasurementAssociationDO();
            measurementAssociationDO.setMeasurementId(id);
            measurementAssociationDO.setMeasurementInstrumentId(createReqVO.getMeasurementInstrumentId());
            list.add(measurementAssociationDO);
        });
        measurementAssociationMapper.insertBatch(list);
    }

    @Override
    @Transactional
    public void updAssociationDevice(DeviceAssociationSaveReqVO createReqVO) {

        // 如果设备为空说明是删除操作
        Long deviceId = createReqVO.getDeviceId();
        if(Objects.isNull(deviceId)){
            measurementDeviceMapper.delete(new LambdaQueryWrapper<MeasurementDeviceDO>()
                    .eq(MeasurementDeviceDO::getMeasurementInstrumentId, createReqVO.getMeasurementInstrumentId()));
            return;
        }

        MeasurementDeviceDO existing = measurementDeviceMapper.selectOne(new LambdaQueryWrapper<MeasurementDeviceDO>()
                .eq(MeasurementDeviceDO::getMeasurementInstrumentId, createReqVO.getMeasurementInstrumentId()));
        if (existing == null) {
            MeasurementDeviceDO measurementDeviceDO = new MeasurementDeviceDO();
            measurementDeviceDO.setDeviceId(deviceId);
            measurementDeviceDO.setMeasurementInstrumentId(createReqVO.getMeasurementInstrumentId());
            measurementDeviceMapper.insert(measurementDeviceDO);
            return;
        }
        measurementDeviceMapper.update(new LambdaUpdateWrapper<MeasurementDeviceDO>()
                .set(MeasurementDeviceDO::getDeviceId, deviceId)
                .eq(MeasurementDeviceDO::getMeasurementInstrumentId, createReqVO.getMeasurementInstrumentId()));

    }

}