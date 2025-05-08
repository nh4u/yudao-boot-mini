package cn.bitlinks.ems.module.power.service.standingbook.acquisition;

import javax.validation.*;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.acquisition.StandingbookAcquisitionDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;

import java.util.List;
import java.util.Map;

/**
 * 台账-数采设置 Service 接口
 *
 * @author bitlinks
 */
public interface StandingbookAcquisitionService {

//    /**
//     * 创建台账-数采设置
//     *
//     * @param createReqVO 创建信息
//     * @return 编号
//     */
//    Long createStandingbookAcquisition(@Valid StandingbookAcquisitionSaveReqVO createReqVO);
//
//    /**
//     * 更新台账-数采设置
//     *
//     * @param updateReqVO 更新信息
//     */
//    void updateStandingbookAcquisition(@Valid StandingbookAcquisitionSaveReqVO updateReqVO);
//
//    /**
//     * 删除台账-数采设置
//     *
//     * @param id 编号
//     */
//    void deleteStandingbookAcquisition(Long id);
//
//    /**
//     * 获得台账-数采设置
//     *
//     * @param id 编号
//     * @return 台账-数采设置
//     */
//    StandingbookAcquisitionDO getStandingbookAcquisition(Long id);
//
//    /**
//     * 获得台账-数采设置分页
//     *
//     * @param pageReqVO 分页查询
//     * @return 台账-数采设置分页
//     */
//    PageResult<StandingbookAcquisitionDO> getStandingbookAcquisitionPage(StandingbookAcquisitionPageReqVO pageReqVO);

    List<StandingbookAcquisitionRespVO> getStandingbookAcquisitionList(Map<String, String> queryReqVO);
}