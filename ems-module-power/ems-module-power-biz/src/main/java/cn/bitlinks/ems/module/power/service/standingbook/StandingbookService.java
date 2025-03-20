package cn.bitlinks.ems.module.power.service.standingbook;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.StandingbookWithAssociations;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import org.springframework.web.multipart.MultipartFile;

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
    Long createStandingbook(@Valid Map <String,String>  createReqVO);

    /**
     * 更新台账属性
     *
     * @param updateReqVO 更新信息
     */
    void updateStandingbook(@Valid Map <String,String>  updateReqVO);

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
     * 获得台账属性分页
     *
     * @param pageReqVO 分页查询
     * @return 台账属性分页
     */
    PageResult<StandingbookDO> getStandingbookPage(StandingbookPageReqVO pageReqVO);

    /**
     * 条件查询台账
     * @param pageReqVO 条件map
     * @return 台账列表
     */
    List<StandingbookDO> getStandingbookList( Map<String,String> pageReqVO);

//    List<StandingbookDO> getStandingbookListBy(Map<String,String> pageReqVO);

    List<StandingbookWithAssociations> getStandingbookListWithAssociations(Map<String, String> pageReqVO);

    Object importStandingbook(MultipartFile file, StandingbookRespVO pageReqVO);

    void exportStandingbookExcel(Map <String,String> pageReqVO, HttpServletResponse response);

    void template(Long typeId, HttpServletResponse response);

    Long create(StandingbookSaveReqVO saveReq);

    Long count(Long typeId);

    /**
     * 获取计量器具/重点设备的台账列表(加top_type)
     * @param pageReqVO 查询条件
     * @return 台账列表
     */
    List<StandingbookDO> listSbAll(Map<String, String> pageReqVO );
}
