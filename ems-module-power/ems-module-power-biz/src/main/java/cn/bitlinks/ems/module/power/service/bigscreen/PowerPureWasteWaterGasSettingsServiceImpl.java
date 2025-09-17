package cn.bitlinks.ems.module.power.service.bigscreen;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.PowerPureWasteWaterGasSettingsPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.PowerPureWasteWaterGasSettingsSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookDTO;
import cn.bitlinks.ems.module.power.dal.dataobject.bigscreen.PowerPureWasteWaterGasSettingsDO;
import cn.bitlinks.ems.module.power.dal.mysql.bigscreen.PowerPureWasteWaterGasSettingsMapper;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.*;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

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
    @Lazy
    private StandingbookService standingbookService;

    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;

    @Override
    public void updateBatch(List<PowerPureWasteWaterGasSettingsSaveReqVO> powerPureWasteWaterGasSettingsList) {
        // 校验
        if (CollUtil.isEmpty(powerPureWasteWaterGasSettingsList)) {
            throw exception(PURE_WASTE_GAS_SETTINGS_LIST_NOT_EXISTS);
        }

        // 不能重复   除了 NAOH HCL外都需要有计量器
        powerPureWasteWaterGasSettingsList.forEach(p -> {

            if (!Objects.equals(p.getCode(), HCL) && !Objects.equals(p.getCode(), NAOH)) {

                String standingbookIds = p.getStandingbookIds();

                if (CharSequenceUtil.isNotBlank(standingbookIds)) {
                    String[] split = standingbookIds.split(StrPool.COMMA);
                    List<String> list = Arrays.stream(split).collect(Collectors.toList());

                    List<String> collect = list.stream()
                            .distinct()
                            .collect(Collectors.toList());
                    if (collect.size() != list.size()) {
                        throw exception(BIG_SCREEN_STANDINGBOOK_REPEAT);
                    }
                } else {
                    throw exception(STANDINGBOOK_NOT_EXISTS);
                }
            }
        });

        // 统一保存
        List<PowerPureWasteWaterGasSettingsDO> list = BeanUtils.toBean(powerPureWasteWaterGasSettingsList, PowerPureWasteWaterGasSettingsDO.class);
        powerPureWasteWaterGasSettingsMapper.updateBatch(list);
    }

    @Override
    public List<PowerPureWasteWaterGasSettingsDO> getPowerPureWasteWaterGasSettingsList(PowerPureWasteWaterGasSettingsPageReqVO pageReqVO) {


        List<PowerPureWasteWaterGasSettingsDO> powerPureWasteWaterGasSettingsDOS = powerPureWasteWaterGasSettingsMapper
                .selectList((new LambdaQueryWrapperX<PowerPureWasteWaterGasSettingsDO>()
                        .eqIfPresent(PowerPureWasteWaterGasSettingsDO::getSystem, pageReqVO.getSystem())
                        .eqIfPresent(PowerPureWasteWaterGasSettingsDO::getCode, pageReqVO.getCode())
                        .orderByAsc(PowerPureWasteWaterGasSettingsDO::getId)));


        // 根据计量器具code获取名称和id
        // 查询设备信息
        List<StandingbookDTO> standingbookDTOS = standingbookService.getStandingbookDTOList();

        StandingbookDTO standingbookDTO = standingbookDTOS.stream()
                .filter(dto -> dto != null &&
                        Objects.equals(dto.getCode(), WASTE_WATER_STANDING_BOOK_CODE))
                .findFirst()
                .orElse(null);

        if (Objects.nonNull(standingbookDTO)) {
            for (PowerPureWasteWaterGasSettingsDO p : powerPureWasteWaterGasSettingsDOS) {
                if (Objects.equals(p.getEnergyCodes(), WASTE_WATER_STANDING_BOOK_CODE)) {
                    p.setWasteWaterName(standingbookDTO.getName() + "(" + p.getCode() + ")");
                    p.setStandingbookIds(standingbookDTO.getStandingbookId().toString());
                    break;
                }
            }
        }

        return powerPureWasteWaterGasSettingsDOS;
    }

}
