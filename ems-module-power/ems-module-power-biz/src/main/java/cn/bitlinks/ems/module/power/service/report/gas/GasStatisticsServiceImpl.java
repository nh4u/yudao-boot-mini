package cn.bitlinks.ems.module.power.service.report.gas;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.report.gas.vo.PowerTankSettingsRespVO;
import cn.bitlinks.ems.module.power.controller.admin.report.gas.vo.SettingsParamVO;
import cn.bitlinks.ems.module.power.dal.dataobject.report.gas.PowerTankSettingsDO;
import cn.bitlinks.ems.module.power.dal.mysql.report.gas.PowerTankSettingsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;

import static cn.bitlinks.ems.module.power.enums.CommonConstants.*;

/**
 * 气化科报表 Service 实现类
 *
 * @author bmqi
 */
@Service
@Validated
@Slf4j
public class GasStatisticsServiceImpl implements GasStatisticsService {


    @Resource
    private PowerTankSettingsMapper powerTankSettingsMapper;

    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;

    // 后续可能根据三目运算符来取动态的有效数字位scale
    private Integer scale = DEFAULT_SCALE;


    @Override
    public List<PowerTankSettingsRespVO> getPowerTankSettings() {
        return BeanUtils.toBean(powerTankSettingsMapper.selectList(), PowerTankSettingsRespVO.class);
    }

    @Override
    public Boolean savePowerTankSettings(SettingsParamVO paramVO) {
        return powerTankSettingsMapper
                .savePowerTankSettings(BeanUtils.toBean(paramVO.getPowerTankSettingsParamVOList(), PowerTankSettingsDO.class));
    }

    @Override
    public List<List<String>> getExcelHeader(ConsumptionStatisticsParamVO paramVO) {
        return null;
    }

    @Override
    public List<List<Object>> getExcelData(ConsumptionStatisticsParamVO paramVO) {
        return null;
    }
}