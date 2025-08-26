package cn.bitlinks.ems.module.power.service.standingbook;

import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.StandingbookWithAssociations;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.measurementassociation.MeasurementAssociationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 台账属性 Service 接口
 *
 * @author bitlinks
 */
public interface StandingbookService {

    /**
     * 创建台账属性
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createStandingbook(@Valid Map<String, String> createReqVO);

    /**
     * 更新台账属性
     *
     * @param updateReqVO 更新信息
     */
    void updateStandingbook(@Valid Map<String, String> updateReqVO);

    /**
     * 获得台账属性
     *
     * @param id 编号
     * @return 台账属性
     */
    StandingbookDO getStandingbook(Long id);

    /**
     * 获得台账属性
     *
     * @return 台账属性
     */
    List<StandingbookDO> getByIds(List<Long> ids);

    /**
     * 条件查询台账（标签、属性、分类ids、分类id、topType、环节、创建时间）
     *
     * @param pageReqVO 条件map
     * @return 台账列表
     */
    List<StandingbookDO> getStandingbookList(Map<String, String> pageReqVO);

    /**
     * 关联计量器具：根据条件获得台账列表和计量器具联系
     * @param pageReqVO 查询条件
     * @return 台账列表
     */
    List<StandingbookWithAssociations> getStandingbookListWithAssociations(Map<String, String> pageReqVO);

    Long count(Long typeId);


    /**
     * 关联下级计量器具/关联设备（需要防止循环嵌套关联）
     *
     * @param reqVO 查询条件
     * @return 可关联的台账列表
     */
    List<StandingbookRespVO> listSbAllWithAssociations(StandingbookAssociationReqVO reqVO);


    /**
     * 根据分类ID查询台账ID
     */
    List<StandingbookDO> getByTypeIds(List<Long> typeIds);

    /**
     * 根据分类ID查询台账ID
     */
    List<StandingbookDO> getByStandingbookIds(List<Long> standingbookIds);



    /**
     * 批量删除
     * @param ids 台账ids
     */
    void deleteStandingbookBatch(List<Long> ids);

    Map<Long, List<MeasurementAssociationDO>>  getSubStandingbookIdsBySbIds(List<Long> sbIds);


    Map<Long, List<MeasurementAssociationDO>>  getUpStandingbookIdsBySbIds(List<Long> sbIds);

    /**
     * 根据台账ID获取台账和能源关系
     * @param standingbookIds
     * @return
     */
    List<StandingbookEnergyTypeVO> getEnergyAndTypeByStandingbookIds(List<Long> standingbookIds);

    /**
     * 获取所有台账和能源关系
     * @return
     */
    List<StandingbookEnergyTypeVO> getAllEnergyAndType();


    /**
     * 虚拟表：关联下级计量器具
     * @param reqVO
     * @return
     */
    List<StandingbookRespVO> listSbAllWithAssociationsVirtual(StandingbookAssociationReqVO reqVO);

    /**
     * 虚拟表关联下级计量器具
     * @param createReqVO
     */
    void updAssociationMeasurementInstrument(MeasurementVirtualAssociationSaveReqVO createReqVO);

    void sbOtherField(List<StandingbookRespVO> respVOS);

    /**
     * 根据能源参数查询台账id
     * @param standingbookEnergyParamReqVO
     * @return
     */
    List<StandingBookTypeTreeRespVO> treeWithEnergyParam(StandingbookEnergyParamReqVO standingbookEnergyParamReqVO);

    /**
     * id-name-code
     * @return
     */
    List<StandingbookDTO> getStandingbookDTOList();


    List<StandingBookHeaderDTO> getStandingBookHeadersByHeaders(List<String> headList);
    /**
     * 根据能源参数查询重点设备
     * @param standingbookParamReqVO
     * @return
     */
    List<StandingBookTypeTreeRespVO> treeDeviceWithParam(StandingbookParamReqVO standingbookParamReqVO);

    List<Long> getStandingBookIdsByStage(Integer stage);

    /**
     * 下载“计量器具导入模板”
     */
    void exportMeterTemplate(HttpServletResponse response);

    /**
     * 导出台账模板
     * @param type device|meter
     */
    void exportLedgerTemplate(String type, HttpServletResponse response);

}
