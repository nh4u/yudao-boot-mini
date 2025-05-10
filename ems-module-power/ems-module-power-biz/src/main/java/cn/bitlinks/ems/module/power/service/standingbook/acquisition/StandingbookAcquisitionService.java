package cn.bitlinks.ems.module.power.service.standingbook.acquisition;

import cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo.StandingbookAcquisitionFormulaVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo.StandingbookAcquisitionRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo.StandingbookAcquisitionTestReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo.StandingbookAcquisitionVO;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 台账-数采设置 Service 接口
 *
 * @author bitlinks
 */
public interface StandingbookAcquisitionService {

    /**
     * 创建/更新台账-数采设置
     *
     * @param updateReqVO 创建/修改信息
     * @return 编号
     */
    Long createOrUpdateStandingbookAcquisition(@Valid StandingbookAcquisitionVO updateReqVO);

    /**
     * 数采设置列表页
     *
     * @param queryReqVO 台账查询条件（标签和台账属性）
     * @return
     */
    List<StandingbookAcquisitionRespVO> getStandingbookAcquisitionList(Map<String, String> queryReqVO);


    /**
     * 根据台账id获取数采设置
     *
     * @param standingbookId 台账id
     * @return 数采设置
     */
    StandingbookAcquisitionVO getAcquisitionByStandingbookId(Long standingbookId);
//
//    /**
//     * 测试连接，获取采集结果
//     * @param testReqVO 参数数采相关
//     * @return 采集结果
//     */
//    String testData(StandingbookAcquisitionTestReqVO testReqVO);
//
//    /**
//     * 校验公式是否有循环引用
//     */
//    Boolean checkFormula(StandingbookAcquisitionFormulaVO formulaVO);
}