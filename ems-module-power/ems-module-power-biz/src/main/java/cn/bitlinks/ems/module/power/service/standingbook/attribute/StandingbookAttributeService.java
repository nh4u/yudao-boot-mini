package cn.bitlinks.ems.module.power.service.standingbook.attribute;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributePageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;

import javax.validation.Valid;
import java.util.List;

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

    /**
     * 更新台账属性
     *
     * @param updateReqVO 更新信息
     */
    void updateStandingbookAttribute(@Valid StandingbookAttributeSaveReqVO updateReqVO);

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
     * 获得台账属性分页
     *
     * @param pageReqVO 分页查询
     * @return 台账属性分页
     */
    PageResult<StandingbookAttributeDO> getStandingbookAttributePage(StandingbookAttributePageReqVO pageReqVO);

}
