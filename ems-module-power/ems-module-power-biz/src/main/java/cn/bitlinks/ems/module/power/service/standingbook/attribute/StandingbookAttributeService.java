package cn.bitlinks.ems.module.power.service.standingbook.attribute;

import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;

import java.util.List;
import java.util.Map;

/**
 * 台账属性 Service 接口
 *
 * @author bitlinks
 */
public interface StandingbookAttributeService {

    /**
     * 更改属性
     *
     * @param updateReqVO
     */
    void update(StandingbookAttributeSaveReqVO updateReqVO);


    /**
     * 获得台账属性by typeId
     *
     * @param typeId 编号
     * @return 台账属性
     */
    List<StandingbookAttributeDO> getStandingbookAttributeByTypeId(Long typeId);

    /**
     * 根据台账属性条件获取台账id
     *
     * @param children 台账属性条件参数
     * @param sbIds    台账id
     * @return 台账id
     */
    List<Long> getStandingbookIdByCondition(Map<String, List<String>> children, List<Long> sbIds);

    /**
     * 点击提交（包含删除、修改、新增操作）
     *
     * @param createReqVOs
     */
    void saveMultiple(List<StandingbookAttributeSaveReqVO> createReqVOs);

    /**
     * 根据台账类型id获取台账属性列表
     *
     * @param typeId
     * @return
     */
    List<StandingbookAttributeRespVO> getByTypeId(Long typeId);


    /**
     * 查询台账关联的台账属性列表
     *
     * @param sbIds 台账ids
     * @return 台账属性列表分组列表
     */
    Map<Long, List<StandingbookAttributeDO>> getAttributesBySbIds(List<Long> sbIds);
}
