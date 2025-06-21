package cn.bitlinks.ems.module.power.service.standingbook.tmpl;

import cn.bitlinks.ems.module.power.controller.admin.standingbook.tmpl.vo.StandingbookTmplDaqAttrRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.tmpl.vo.StandingbookTmplDaqAttrSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyparameters.EnergyParametersDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;

import java.util.List;
import java.util.Map;

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
     * @param status 可为空，可以查询所有启用的
     * @return 数采参数列表
     */
    List<StandingbookTmplDaqAttrRespVO> getByTypeIdAndEnergyFlag(Long typeId, Boolean energyFlag,Boolean status);

    /**
     * 能源新增属性关联添加数采参数（默认启用）
     *
     * @param energyId     能源id
     * @param energyParams 能源参数集合
     */
    void cascadeAddDaqAttrByEnergyParams(Long energyId, List<EnergyParametersDO> energyParams);

    /**
     * 获取台账私有属性 by typeId
     *
     * @param typeId 编号
     * @return 台账属性
     */
    List<StandingbookTmplDaqAttrDO> getPrivateStandingbookAttributeByTypeId(Long typeId, Boolean energyFlag);

    /**
     * 判断能源id是否关联模板
     *
     * @param energyId 能源id
     * @return 是否关联
     */
    boolean isAssociationWithEnergyId(Long energyId);

    /**
     * 获取台账对应的用量数采参数
     *
     * @param id 台账id
     * @return 用量数采参数
     */
    StandingbookTmplDaqAttrDO getUsageAttrBySbId(Long id);

    /**
     * 根据台账分类ids获取数采属性列表
     *
     * @param typeIds 台账分类ids
     * @return typeId-attrList
     */
    Map<Long, List<StandingbookTmplDaqAttrDO>> getDaqAttrsByTypeIds(List<Long> typeIds);

    /**
     * 获取能源id
     *
     * @param sbId
     * @return
     */
    Long getEnergyIdBySbId(Long sbId);

    /**
     * 获取能源id
     *
     * @param typeId
     * @return
     */
    Long getEnergyIdByTypeId(Long typeId);

    /**
     * 根据台账ids获取数采属性列表（启用的）
     *
     * @param sbIds 台账id
     * @return sbId-attrList
     */
    Map<Long, List<StandingbookTmplDaqAttrDO>> getDaqAttrsBySbIds(List<Long> sbIds);
    /**
     * 根据台账ids获取能源参数列表（启用的）
     *
     * @param sbIds 台账id
     * @return sbId-attrList
     */
    Map<Long, List<StandingbookTmplDaqAttrDO>> getEnergyDaqAttrsBySbIds(List<Long> sbIds);

    /**
     * 获取台账数采属性（启用的）
     *
     * @param standingbookId 台账id
     * @return 数采属性列表
     */
    List<StandingbookTmplDaqAttrDO> getDaqAttrsByStandingbookId(Long standingbookId);

    /**
     * 根据能源ids获取台账模板数据
     *
     * @param energyIds
     * @return
     */
    List<StandingbookTmplDaqAttrDO> getByEnergyIds(List<Long> energyIds);

    /**
     * 获取所有的typeId-ennergyId映射
     *
     * @return
     */
    List<StandingbookTmplDaqAttrDO> getEnergyMapping();

    List<StandingbookTmplDaqAttrDO> getByTypeIds(List<Long> typeIds);

}
