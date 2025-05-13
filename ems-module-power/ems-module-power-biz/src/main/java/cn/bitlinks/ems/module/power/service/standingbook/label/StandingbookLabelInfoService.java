package cn.bitlinks.ems.module.power.service.standingbook.label;

import java.util.List;

import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookLabelInfoDO;

/**
 * @author wangl
 * @date 2025年05月07日 15:42
 */
public interface StandingbookLabelInfoService {

    List<StandingbookLabelInfoDO> getByLabelNames(List<String> labelName);

    List<StandingbookLabelInfoDO> getByValues(List<String> values);

    List<StandingbookLabelInfoDO> getByStandingBookIds(List<Long> standingBookIdList);
}
