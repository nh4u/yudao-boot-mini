package cn.bitlinks.ems.module.power.service.bigscreen;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.PowerMonthPlanSettingsPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.PowerMonthPlanSettingsSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.bigscreen.PowerMonthPlanSettingsDO;
import cn.bitlinks.ems.module.power.dal.mysql.bigscreen.PowerMonthPlanSettingsMapper;
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
public class PowerMonthPlanSettingsServiceImpl implements PowerMonthPlanSettingsService {

    @Resource
    private PowerMonthPlanSettingsMapper powerMonthPlanSettingsMapper;


    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;

    @Override
    public void updateBatch(List<PowerMonthPlanSettingsSaveReqVO> powerMonthPlanSettingsList) {
        // 校验
        if (CollUtil.isEmpty(powerMonthPlanSettingsList)) {
            throw exception(PURE_WASTE_GAS_SETTINGS_LIST_NOT_EXISTS);
        }
        // 统一保存
        List<PowerMonthPlanSettingsDO> list = BeanUtils.toBean(powerMonthPlanSettingsList, PowerMonthPlanSettingsDO.class);
        powerMonthPlanSettingsMapper.updateBatch(list);
    }

    @Override
    public List<PowerMonthPlanSettingsDO> getPowerMonthPlanSettingsList(PowerMonthPlanSettingsPageReqVO pageReqVO) {
        return powerMonthPlanSettingsMapper.selectList();
    }

    @Override
    public List<PowerMonthPlanSettingsDO> selectList() {
        return powerMonthPlanSettingsMapper.selectList();
    }

}
