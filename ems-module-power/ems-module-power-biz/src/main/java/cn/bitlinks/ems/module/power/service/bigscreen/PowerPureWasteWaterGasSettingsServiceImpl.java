package cn.bitlinks.ems.module.power.service.bigscreen;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.PowerPureWasteWaterGasSettingsPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.PowerPureWasteWaterGasSettingsSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.bigscreen.PowerPureWasteWaterGasSettingsDO;
import cn.bitlinks.ems.module.power.dal.mysql.bigscreen.PowerPureWasteWaterGasSettingsMapper;
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.PURE_WASTE_GAS_SETTINGS_LIST_NOT_EXISTS;

/**
 * @author liumingqiang
 */
@Slf4j
@Service
@Validated
public class PowerPureWasteWaterGasSettingsServiceImpl implements PowerPureWasteWaterGasSettingsService {

    @Resource
    private PowerPureWasteWaterGasSettingsMapper powerPureWasteWaterGasSettingsMapper;


    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;

    @Override
    public void updateBatch(List<PowerPureWasteWaterGasSettingsSaveReqVO> powerPureWasteWaterGasSettingsList) {
        // 校验
        if (CollUtil.isEmpty(powerPureWasteWaterGasSettingsList)) {
            throw exception(PURE_WASTE_GAS_SETTINGS_LIST_NOT_EXISTS);
        }
        // 统一保存
        List<PowerPureWasteWaterGasSettingsDO> list = BeanUtils.toBean(powerPureWasteWaterGasSettingsList, PowerPureWasteWaterGasSettingsDO.class);
        powerPureWasteWaterGasSettingsMapper.updateBatch(list);
    }

    @Override
    public List<PowerPureWasteWaterGasSettingsDO> getPowerPureWasteWaterGasSettingsList(PowerPureWasteWaterGasSettingsPageReqVO pageReqVO) {
        return powerPureWasteWaterGasSettingsMapper.selectList((new LambdaQueryWrapperX<PowerPureWasteWaterGasSettingsDO>()
                .inIfPresent(PowerPureWasteWaterGasSettingsDO::getSystem, pageReqVO.getSystems())
                .eqIfPresent(PowerPureWasteWaterGasSettingsDO::getSystem, pageReqVO.getSystem())
                .eqIfPresent(PowerPureWasteWaterGasSettingsDO::getCode, pageReqVO.getCode())
                .orderByAsc(PowerPureWasteWaterGasSettingsDO::getId)));
    }

}
