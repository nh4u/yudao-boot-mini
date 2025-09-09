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

    List<StandingbookLabelInfoDO> getByStandingBookId(Long standingBookId);

    List<StandingbookLabelInfoDO> getByValuesSelected(List<String> values);

    /**
     * 获取该标签绑定的台账
     *
     * @param labelValue
     * @return
     */
    List<StandingbookLabelInfoDO> getSelfByLabelValues(String labelValue);

    /**
     * 取该标签下一级的标签绑定信息
     *
     * @param labelValue
     * @return
     */
    List<StandingbookLabelInfoDO> getSubByLabelValues(String labelValue);

    /**
     * 取该标签下所有子级的标签绑定信息
     *
     * @param labelValue
     * @return
     */
    List<StandingbookLabelInfoDO> getAllSubByLabelValues(String labelValue);

    /**
     * 获取该标签绑定的台账 如果没有则获取该标签下所有子级的标签绑定信息
     *
     * @param labelValue
     * @return
     */
    List<StandingbookLabelInfoDO> getByLabelValues(String labelValue);

}
