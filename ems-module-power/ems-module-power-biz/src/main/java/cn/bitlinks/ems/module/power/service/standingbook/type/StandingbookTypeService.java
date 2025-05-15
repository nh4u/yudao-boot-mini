package cn.bitlinks.ems.module.power.service.standingbook.type;

import cn.bitlinks.ems.module.power.controller.admin.standingbook.type.vo.StandingbookTypeListReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.type.vo.StandingbookTypeSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 台账类型 Service 接口
 *
 * @author bitlinks
 */
public interface StandingbookTypeService {

    /**
     * 创建台账类型
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createStandingbookType(@Valid StandingbookTypeSaveReqVO createReqVO);

    /**
     * 更新台账类型
     *
     * @param updateReqVO 更新信息
     */
    void updateStandingbookType(@Valid StandingbookTypeSaveReqVO updateReqVO);

    /**
     * 删除台账类型
     *
     * @param id 编号
     */
    void deleteStandingbookType(Long id);

    /**
     * 获得台账类型
     *
     * @param id 编号
     * @return 台账类型
     */
    StandingbookTypeDO getStandingbookType(Long id);
    List<StandingbookTypeDO> getStandingbookType(String name);

    /**
     * 获得台账类型列表
     *
     * @param listReqVO 查询条件
     * @return 台账类型列表
     */
    List<StandingbookTypeDO> getStandingbookTypeList(StandingbookTypeListReqVO listReqVO);
    List<StandingbookTypeDO> getStandingbookTypeNode();

    /**
     * 获得台账类型列表Map<id,DO>
     * @return map
     */
    Map<Long,StandingbookTypeDO> getStandingbookTypeIdMap(List<Long> typeIds);

    /**
     * 递归查询子节点 id
     * @param typeList
     * @param targetId
     * @return
     */
    List<Long> getSubtreeIds(List<StandingbookTypeDO> typeList, Long targetId);

    /**
     * 查询是否关联台账
     * @param id 分类id
     * @return
     */
    Boolean checkRelStandingbook(Long id);
}
