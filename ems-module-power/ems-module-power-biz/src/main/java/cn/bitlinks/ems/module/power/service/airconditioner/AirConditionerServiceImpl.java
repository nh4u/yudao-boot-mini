package cn.bitlinks.ems.module.power.service.airconditioner;

import cn.bitlinks.ems.module.power.dal.dataobject.airconditioner.AirConditionerSettingsDO;
import cn.bitlinks.ems.module.power.dal.mysql.airconditioner.AirConditionerSettingsMapper;
import cn.bitlinks.ems.module.power.dal.mysql.airconditioner.AirConditionerStatusDataMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@Validated
public class AirConditionerServiceImpl implements AirConditionerService {
    @Resource
    private AirConditionerStatusDataMapper airConditionerStatusDataMapper;

    @Resource
    private AirConditionerSettingsMapper airConditionerSettingsMapper;


    @Override
    public List<String> getOptions() {
        return airConditionerSettingsMapper.selectList(new LambdaQueryWrapper<AirConditionerSettingsDO>()
                        .orderByAsc(AirConditionerSettingsDO::getSortNo))
                .stream()
                .map(AirConditionerSettingsDO::getItemName)
                .collect(Collectors.toList());
    }
}
