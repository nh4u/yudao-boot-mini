package cn.bitlinks.ems.module.power.service.chemicals;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.chemicals.vo.PowerChemicalsSettingsPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.chemicals.vo.PowerChemicalsSettingsSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.chemicals.PowerChemicalsSettingsDO;
import cn.bitlinks.ems.module.power.dal.mysql.chemicals.PowerChemicalsSettingsMapper;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.HCL;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.NAOH;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.PURE_WASTE_GAS_SETTINGS_LIST_NOT_EXISTS;

/**
 * @author liumingqiang
 */
@Slf4j
@Service
@Validated
public class PowerChemicalsSettingsServiceImpl implements PowerChemicalsSettingsService {

    @Resource
    private PowerChemicalsSettingsMapper powerChemicalsSettingsMapper;


    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;

    @Override
    public void updateBatch(List<PowerChemicalsSettingsSaveReqVO> powerChemicalsSettingsList) {
        // 校验
        if (CollUtil.isEmpty(powerChemicalsSettingsList)) {
            throw exception(PURE_WASTE_GAS_SETTINGS_LIST_NOT_EXISTS);
        }
        // 统一保存
        List<PowerChemicalsSettingsDO> list = BeanUtils.toBean(powerChemicalsSettingsList, PowerChemicalsSettingsDO.class);
        powerChemicalsSettingsMapper.updateBatch(list);
    }

    @Override
    public List<PowerChemicalsSettingsDO> getPowerChemicalsSettingsList(PowerChemicalsSettingsPageReqVO pageReqVO) {

        // 先判断当天有值没 如果没有就新增一下
        PowerChemicalsSettingsDO one = powerChemicalsSettingsMapper.selectOne(new LambdaQueryWrapperX<PowerChemicalsSettingsDO>()
                .orderByDesc(PowerChemicalsSettingsDO::getTime)
                .last("limit 1"));

        LocalDateTime time = LocalDateTimeUtil.beginOfDay(LocalDateTime.now());
        if (Objects.isNull(one) || !time.isEqual(one.getTime())) {
            // 如果今天数据不存在则新增
            addTodayData(time);
        }

        return powerChemicalsSettingsMapper.selectList((new LambdaQueryWrapperX<PowerChemicalsSettingsDO>()
                .between(PowerChemicalsSettingsDO::getTime, time.minusDays(6), time)
                .eq(PowerChemicalsSettingsDO::getCode, pageReqVO.getCode())
                .orderByAsc(PowerChemicalsSettingsDO::getCode, PowerChemicalsSettingsDO::getTime)));
    }

    /**
     * 添加今日初始数据
     *
     * @param time
     */
    private void addTodayData(LocalDateTime time) {
        // 30%NAOH（氢氧化钠）
        PowerChemicalsSettingsDO naoh = new PowerChemicalsSettingsDO();
        naoh.setTime(time);
        naoh.setCode(NAOH);

        // 30%HCL（盐酸）
        PowerChemicalsSettingsDO hcl = new PowerChemicalsSettingsDO();
        hcl.setTime(time);
        hcl.setCode(HCL);

        powerChemicalsSettingsMapper.insert(naoh);
        powerChemicalsSettingsMapper.insert(hcl);
    }

}
