package cn.bitlinks.ems.module.power.service.standingbook.label;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import javax.annotation.Resource;

import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookLabelInfoDO;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.StandingbookLabelInfoMapper;

/**
 * @author wangl
 * @date 2025年05月07日 15:48
 */
@Service("standingbookLabelInfoService")
@Validated
public class StandingbookLabelInfoServiceImpl implements StandingbookLabelInfoService {


    @Resource
    private StandingbookLabelInfoMapper standingbookLabelInfoMapper;

    @Override
    public List<StandingbookLabelInfoDO> getByLabelNames(List<String> labelNames) {

        return standingbookLabelInfoMapper.getByLabelNames(labelNames);
    }

    @Override
    public List<StandingbookLabelInfoDO> getByValues(List<String> values) {
        return standingbookLabelInfoMapper.getByValues(values);
    }
}
