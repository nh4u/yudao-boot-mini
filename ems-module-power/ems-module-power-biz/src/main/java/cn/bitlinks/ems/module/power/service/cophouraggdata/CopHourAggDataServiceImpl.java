package cn.bitlinks.ems.module.power.service.cophouraggdata;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StandardCoalInfo;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;
import cn.bitlinks.ems.module.power.dal.mysql.copsettings.CopHourAggDataMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;


/**
 * @author liumingqiang
 */
@DS("starrocks")
@Slf4j
@Service
@Validated
public class CopHourAggDataServiceImpl implements CopHourAggDataService {

    @Resource
    private CopHourAggDataMapper copHourAggDataMapper;

    @Override
    public StatisticsResultV2VO<StandardCoalInfo> copTable(StatisticsParamV2VO paramVO) {

        // TODO: 2025/6/21 查血COP报表数据
        copHourAggDataMapper.selectList();
        return null;
    }


}
