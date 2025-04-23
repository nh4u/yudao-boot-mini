package cn.bitlinks.ems.module.power.service.standingbook.tmpl;

import cn.bitlinks.ems.module.power.controller.admin.standingbook.tmpl.vo.StandingbookTmplDaqAttrRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.tmpl.vo.StandingbookTmplDaqAttrSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyparameters.EnergyParametersDO;

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
     * @param typeId     台账类型id
     * @param energyFlag 是否能源数采
     * @return
     */
    List<StandingbookTmplDaqAttrRespVO> getByTypeIdAndEnergyFlag(Long typeId, Boolean energyFlag);

    /**
     * 能源新增属性关联添加数采参数（默认启用）
     *
     * @param energyId     能源id
     * @param energyParams 能源参数集合
     */
    void cascadeAddDaqAttrByEnergyParams(Long energyId, List<EnergyParametersDO> energyParams);

    /**
     * 判断能源id是否关联模板
     *
     * @param energyId 能源id
     * @return 是否关联
     */
    boolean isAssociationWithEnergyId(Long energyId);
}
