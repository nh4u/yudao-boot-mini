package cn.bitlinks.ems.module.power.service.warningtemplate;

import java.util.*;
import javax.validation.*;
import cn.bitlinks.ems.module.power.controller.admin.warningtemplate.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.warningtemplate.WarningTemplateDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;

/**
 * 告警模板 Service 接口
 *
 * @author bitlinks
 */
public interface WarningTemplateService {

    /**
     * 创建告警模板
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createWarningTemplate(@Valid WarningTemplateSaveReqVO createReqVO);

    /**
     * 更新告警模板
     *
     * @param updateReqVO 更新信息
     */
    void updateWarningTemplate(@Valid WarningTemplateSaveReqVO updateReqVO);

    /**
     * 删除告警模板
     *
     * @param id 编号
     */
    void deleteWarningTemplate(Long id);

    /**
     * 获得告警模板
     *
     * @param id 编号
     * @return 告警模板
     */
    WarningTemplateDO getWarningTemplate(Long id);

    /**
     * 获得告警模板分页
     *
     * @param pageReqVO 分页查询
     * @return 告警模板分页
     */
    PageResult<WarningTemplateDO> getWarningTemplatePage(WarningTemplatePageReqVO pageReqVO);

    /**
     * 删除告警模板（批量）
     * @param ids ids
     */
    void deleteWarningTemplateBatch(List<Long> ids);

    /**
     * 根据模板id查询是否关联，返回模板编码
     * @return 编码s
     */
    List<String> queryUsedByStrategy(List<Long> ids);

    /**
     * 查询告警模板列表(全部）
     * @param type 模板类型
     * @param name 模板名称
     * @return 列表集合
     */
    List<WarningTemplateDO> getWarningTemplateList(Integer type, String name) ;
}