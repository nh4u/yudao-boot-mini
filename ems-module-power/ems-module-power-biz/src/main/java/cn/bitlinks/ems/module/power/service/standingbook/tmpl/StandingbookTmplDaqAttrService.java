package cn.bitlinks.ems.module.power.service.standingbook.tmpl;

import cn.bitlinks.ems.module.power.controller.admin.standingbook.tmpl.vo.StandingbookTmplDaqAttrRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.tmpl.vo.StandingbookTmplDaqAttrSaveReqVO;

import java.util.List;

/**
 * 台账模板数采属性 Service 接口
 *
 * @author bitlinks
 */
public interface StandingbookTmplDaqAttrService {


    /**
     * 点击提交（包含删除、修改、新增操作）
     *
     * @param saveReqVOList
     */
    void saveMultiple(List<StandingbookTmplDaqAttrSaveReqVO> saveReqVOList);

    /**
     * 根据台账类型id获取台账分类数采属性列表
     *
     * @param typeId   台账类型id
     * @param energyFlag 是否能源数采
     * @return
     */
    List<StandingbookTmplDaqAttrRespVO> getByTypeIdAndEnergyFlag(Long typeId, Boolean energyFlag);


}
