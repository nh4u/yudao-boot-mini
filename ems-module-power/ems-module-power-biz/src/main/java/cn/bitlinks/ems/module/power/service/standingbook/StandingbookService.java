package cn.bitlinks.ems.module.power.service.standingbook;

import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.StandingbookWithAssociations;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookAssociationReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookRespVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;

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
     * 删除台账属性
     *
     * @param id 编号
     */
    void deleteStandingbook(Long id);

    /**
     * 获得台账属性
     *
     * @param id 编号
     * @return 台账属性
     */
    StandingbookDO getStandingbook(Long id);

    /**
     * 条件查询台账（标签、属性、分类ids、分类id、topType、环节、创建时间）
     *
     * @param pageReqVO 条件map
     * @return 台账列表
     */
    List<StandingbookDO> getStandingbookList(Map<String, String> pageReqVO);

    List<StandingbookWithAssociations> getStandingbookListWithAssociations(Map<String, String> pageReqVO);

    Long count(Long typeId);


    /**
     * 关联下级计量器具/关联设备（需要防止循环嵌套关联）
     *
     * @param reqVO 查询条件
     * @return 可关联的台账列表
     */
    List<StandingbookRespVO> listSbAllWithAssociations(StandingbookAssociationReqVO reqVO);
}
