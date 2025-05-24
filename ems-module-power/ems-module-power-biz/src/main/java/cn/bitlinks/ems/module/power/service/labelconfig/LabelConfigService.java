package cn.bitlinks.ems.module.power.service.labelconfig;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.labelconfig.vo.LabelConfigPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.labelconfig.vo.LabelConfigSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.labelconfig.LabelConfigDO;
import cn.hutool.core.lang.tree.Tree;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.validation.Valid;
import java.util.List;

/**
 * 配置标签 Service 接口
 *
 * @author bitlinks
 */
public interface LabelConfigService {

    /**
     * 创建配置标签
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createLabelConfig(@Valid LabelConfigSaveReqVO createReqVO);

    /**
     * 更新配置标签
     *
     * @param updateReqVO 更新信息
     */
    void updateLabelConfig(@Valid LabelConfigSaveReqVO updateReqVO);

    /**
     * 删除配置标签
     *
     * @param id 编号
     */
    void deleteLabelConfig(Long id);

    /**
     * 获得配置标签
     *
     * @param id 编号
     * @return 配置标签
     */
    LabelConfigDO getLabelConfig(Long id);

    /**
     * 获得配置标签分页
     *
     * @param pageReqVO 分页查询
     * @return 配置标签分页
     */
    PageResult<LabelConfigDO> getLabelConfigPage(LabelConfigPageReqVO pageReqVO);

    List<Tree<Long>> getLabelTree(boolean lazy, Long parentId, String labelName);

    ImmutablePair<List<LabelConfigDO>, List<Tree<Long>>> getLabelPair(boolean lazy, Long parentId, String labelName);

    List<Tree<Long>> getLabelTreeByParam(List<Long> labelIdList);

    ImmutablePair<List<LabelConfigDO>, List<Tree<Long>>> getLabelPairByParam(List<Long> labelIdList);


    /**
     * 获取所有的配置标签
     * @return 配置标签
     */
    List<LabelConfigDO> getAllLabelConfig();


    List<LabelConfigDO> getByIds(List<Long> ids);
}