package cn.bitlinks.ems.module.power.service.standingbook.attribute;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.AttributeTreeNode;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributePageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 台账属性 Service 接口
 *
 * @author bitlinks
 */
public interface StandingbookAttributeService {

    /**
     * 创建台账属性
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createStandingbookAttribute(@Valid StandingbookAttributeSaveReqVO createReqVO);
    Long create(StandingbookAttributeSaveReqVO createReqVO);

    /**
     * 更新台账属性
     *
     * @param updateReqVO 更新信息
     */
    void updateStandingbookAttribute(@Valid StandingbookAttributeSaveReqVO updateReqVO);
    void update(StandingbookAttributeSaveReqVO updateReqVO);

    /**
     * 删除台账属性
     *
     * @param id 编号
     */
    void deleteStandingbookAttribute(Long id);

    /**
     * 批量删除台账属性by typeId
     * @param
     */
    void deleteStandingbookAttributeByTypeId(Long typeId);

    /**
     * 批量删除台账属性by standingbookId
     * @param
     */
    void deleteStandingbookAttributeByStandingbookId(Long standingbookId);

    /**
     * 批量添加台账属性
     * @param dos
     */
    void createStandingbookAttributeBatch(List<StandingbookAttributeDO> dos);

    /**
     * 获得台账属性
     *
     * @param id 编号
     * @return 台账属性
     */
    StandingbookAttributeDO getStandingbookAttribute(Long id);

    /**
     * 获得台账属性by standingbookId
     *
     * @param id 编号
     * @return 台账属性
     */
    List<StandingbookAttributeDO>  getStandingbookAttributeByStandingbookId(Long standingbookId);
   /**
     * 获得台账属性by typeId
     *
     * @param id 编号
     * @return 台账属性
     */
    List<StandingbookAttributeDO>  getStandingbookAttributeByTypeId(Long typeId);

    /**
     * 获得台账属性分页
     *
     * @param pageReqVO 分页查询
     * @return 台账属性分页
     */
    PageResult<StandingbookAttributeDO> getStandingbookAttributePage(StandingbookAttributePageReqVO pageReqVO);

    /**
     * 根据台账属性条件获取台账id
     *
     * @param children            台账属性条件参数
     * @param sbIds              台账id
     * @return 台账id
     */
    List<Long> getStandingbookIdByCondition(Map<String, List<String>> children, List<Long> sbIds);

    List<StandingbookDO> getStandingbookIntersection(List<StandingbookAttributePageReqVO> children, Long typeId);

    /**
     * 点击提交（包含删除、修改、新增操作）
     * @param createReqVOs
     */
    void saveMultiple(List<StandingbookAttributeSaveReqVO> createReqVOs);

    /**
     * 获取台账属性Tree结构
     * @param standingbookIds 台账ids
     * @param typeIds 台账类型ids
     */
    List<AttributeTreeNode> queryAttributeTreeNodeByTypeAndSb(List<Long> standingbookIds, List<Long> typeIds);

    /**
     * 根据台账类型id获取台账属性列表
     * @param typeId
     * @return
     */
    List<StandingbookAttributeRespVO> getByTypeId(Long typeId);
}
