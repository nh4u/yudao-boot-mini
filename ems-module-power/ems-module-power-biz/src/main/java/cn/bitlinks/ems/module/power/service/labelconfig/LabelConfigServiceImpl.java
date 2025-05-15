package cn.bitlinks.ems.module.power.service.labelconfig;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.labelconfig.vo.LabelConfigPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.labelconfig.vo.LabelConfigSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.labelconfig.LabelConfigDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.mysql.coalfactorhistory.CoalFactorHistoryMapper;
import cn.bitlinks.ems.module.power.dal.mysql.labelconfig.LabelConfigMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.StandingbookMapper;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNode;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

/**
 * 配置标签 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class LabelConfigServiceImpl implements LabelConfigService {

    @Resource
    private LabelConfigMapper labelConfigMapper;

    @Resource
    private CoalFactorHistoryMapper coalFactorHistoryMapper;

    @Resource
    private StandingbookMapper standingbookMapper;

    @Override
    public Long createLabelConfig(LabelConfigSaveReqVO createReqVO) {
        // 校验层数限制
        validateLabelLayer(createReqVO.getParentId());

        // 判断是否已有10条
        Long count = labelConfigMapper.selectCount(new LambdaQueryWrapperX<LabelConfigDO>()
                .eq(LabelConfigDO::getParentId, createReqVO.getParentId()));
        if (count >= CommonConstants.LABEL_NUM_LIMIT) {
            throw exception(LABEL_CONFIG_REACH_LIMIT);
        }
        Long existCount = labelConfigMapper.selectCount(new LambdaQueryWrapperX<LabelConfigDO>()
                .eq(LabelConfigDO::getCode, createReqVO.getCode()));
        if (existCount > 0) {
            throw exception(LABEL_CONFIG_CODE_DUPLICATE);
        }

        // 插入
        LabelConfigDO labelConfig = BeanUtils.toBean(createReqVO, LabelConfigDO.class);
        labelConfigMapper.insert(labelConfig);
        // 返回
        return labelConfig.getId();
    }

    /**
     * 校验标签的层数限制
     */
    private void validateLabelLayer(Long parentId) {
        int layerCount = 1;
        while (parentId != null && !parentId.equals(CommonConstants.LABEL_TREE_ROOT_ID)) {
            LabelConfigDO parentLabel = labelConfigMapper.selectById(parentId);
            if (parentLabel == null) {
                throw exception(LABEL_CONFIG_NOT_EXISTS);
            }
            parentId = parentLabel.getParentId();
            layerCount++;
        }

        if (layerCount > CommonConstants.LABEL_LAYER_LIMIT) {
            throw exception(LABEL_CONFIG_REACH_LAYER_LIMIT); // 新增异常类型
        }
    }


    @Override
    public void updateLabelConfig(LabelConfigSaveReqVO updateReqVO) {
        LabelConfigDO currentLabel = labelConfigMapper.selectById(updateReqVO.getId());
        // 校验存在
        validateLabelConfigExists(updateReqVO.getId());
        // 如果编码被修改，校验新编码是否重复
        if (!currentLabel.getCode().equals(updateReqVO.getCode())) {
            Long existCount = labelConfigMapper.selectCount(new LambdaQueryWrapperX<LabelConfigDO>()
                    .eq(LabelConfigDO::getCode, updateReqVO.getCode())
                    .ne(LabelConfigDO::getId, updateReqVO.getId())); // 排除自身
            if (existCount > 0) {
                throw exception(LABEL_CONFIG_CODE_DUPLICATE);
            }
        }
        // 更新
        LabelConfigDO updateObj = BeanUtils.toBean(updateReqVO, LabelConfigDO.class);
        labelConfigMapper.updateById(updateObj);
    }

    @Override
    public void deleteLabelConfig(Long id) {
        // 校验存在
        validateLabelConfigExists(id);
        // 2. 校验设备绑定
        validateLabelDeviceBinding(id);
        // 删除
        labelConfigMapper.deleteById(id);
    }

    private void validateLabelConfigExists(Long id) {
        if (labelConfigMapper.selectById(id) == null) {
            throw exception(LABEL_CONFIG_NOT_EXISTS);
        }
    }

    @Override
    public LabelConfigDO getLabelConfig(Long id) {
        return labelConfigMapper.selectById(id);
    }

    @Override
    public PageResult<LabelConfigDO> getLabelConfigPage(LabelConfigPageReqVO pageReqVO) {


        return labelConfigMapper.selectPage(pageReqVO);
    }

    @Override
    public List<Tree<Long>> getLabelTree(boolean lazy, Long parentId, String labelName) {
        if (!lazy) {

            if (parentId == null) {
                List<TreeNode<Long>> collect = labelConfigMapper
                        .selectList(Wrappers.<LabelConfigDO>lambdaQuery()
                                .like(StrUtil.isNotEmpty(labelName), LabelConfigDO::getLabelName, labelName)
                                .orderByAsc(LabelConfigDO::getSort)).stream()
                        .map(getNodeFunction()).collect(Collectors.toList());
                return TreeUtil.build(collect, CommonConstants.LABEL_TREE_ROOT_ID);
            } else {
                // 递归获取该节点下所有子集
                List<LabelConfigDO> collect = getLabelTreeNodeList(parentId);
                List<TreeNode<Long>> collect1 = collect.stream().map(getNodeFunction()).collect(Collectors.toList());
                return TreeUtil.build(collect1, parentId);
            }

        }
        Long parent = parentId == null ? CommonConstants.LABEL_TREE_ROOT_ID : parentId;

        List<TreeNode<Long>> collect = labelConfigMapper
                .selectList(Wrappers.<LabelConfigDO>lambdaQuery()
                        .like(StrUtil.isNotEmpty(labelName), LabelConfigDO::getLabelName, labelName)
                        .eq(LabelConfigDO::getParentId, parent)
                        .orderByAsc(LabelConfigDO::getSort))
                .stream().map(getNodeFunction()).collect(Collectors.toList());

        return TreeUtil.build(collect, parent);

    }

    private List<LabelConfigDO> getLabelTreeNodeList(Long parentId) {

        List<LabelConfigDO> list = new ArrayList<>();
        List<LabelConfigDO> labelList = labelConfigMapper
                .selectList(Wrappers.<LabelConfigDO>lambdaQuery()
                        .eq(LabelConfigDO::getParentId, parentId)
                        .orderByAsc(LabelConfigDO::getSort));

        if (CollectionUtil.isEmpty(labelList)) {
            LabelConfigDO labelConfigDO = labelConfigMapper.selectById(parentId);
            list.add(labelConfigDO);
            return list;
        }

        for (LabelConfigDO labelConfigDO : labelList) {

            List<LabelConfigDO> subLabelList = getLabelTreeNodeList(labelConfigDO.getId());
            list.add(labelConfigDO);
            list.addAll(subLabelList);
        }

        return list;
    }

    @Override
    public ImmutablePair<List<LabelConfigDO>, List<Tree<Long>>> getLabelPair(boolean lazy, Long parentId, String labelName) {
        if (!lazy) {

            List<LabelConfigDO> list = labelConfigMapper
                    .selectList(Wrappers.<LabelConfigDO>lambdaQuery()
                            .like(StrUtil.isNotEmpty(labelName), LabelConfigDO::getLabelName, labelName)
                            .orderByAsc(LabelConfigDO::getSort));
            List<TreeNode<Long>> collect = list.stream().map(getNodeFunction()).collect(Collectors.toList());

            return ImmutablePair.of(list, TreeUtil.build(collect, CommonConstants.LABEL_TREE_ROOT_ID));
        }
        Long parent = parentId == null ? CommonConstants.LABEL_TREE_ROOT_ID : parentId;

        List<LabelConfigDO> list = labelConfigMapper
                .selectList(Wrappers.<LabelConfigDO>lambdaQuery()
                        .like(StrUtil.isNotEmpty(labelName), LabelConfigDO::getLabelName, labelName)
                        .eq(LabelConfigDO::getParentId, parent)
                        .orderByAsc(LabelConfigDO::getSort));
        List<TreeNode<Long>> collect = list.stream().map(getNodeFunction()).collect(Collectors.toList());

        return ImmutablePair.of(list, TreeUtil.build(collect, parent));

    }

    @Override
    public List<Tree<Long>> getLabelTreeByParam(List<Long> labelIdList) {

        List<Long> distinctList = labelIdList.stream().distinct().collect(Collectors.toList());
        List<LabelConfigDO> labelList = labelConfigMapper.selectList("id", distinctList);

        List<LabelConfigDO> list = new ArrayList<>();

        for (LabelConfigDO labelConfigDO : labelList) {
            List<LabelConfigDO> parent = getParent(labelConfigDO);
            List<LabelConfigDO> children = getChildren(labelConfigDO);

            list.addAll(parent);
            list.addAll(children);
        }

        // 对所有list递归取所有相关联标签节点
        List<TreeNode<Long>> collect = list.stream().map(getNodeFunction()).collect(Collectors.toList());
        return TreeUtil.build(collect, CommonConstants.LABEL_TREE_ROOT_ID);
    }

    @Override
    public ImmutablePair<List<LabelConfigDO>, List<Tree<Long>>> getLabelPairByParam(List<Long> labelIdList) {

        List<Long> distinctList = labelIdList.stream().distinct().collect(Collectors.toList());
        List<LabelConfigDO> labelList = labelConfigMapper.selectList("id", distinctList);

        List<LabelConfigDO> list = new ArrayList<>();

        for (LabelConfigDO labelConfigDO : labelList) {
            List<LabelConfigDO> parent = getParent(labelConfigDO);
            List<LabelConfigDO> children = getChildren(labelConfigDO);

            list.addAll(parent);
            list.addAll(children);
        }
        list = list.stream().distinct().collect(Collectors.toList());
        // 对所有list递归取所有相关联标签节点
        List<TreeNode<Long>> collect = list.stream().map(getNodeFunction()).collect(Collectors.toList());
        return ImmutablePair.of(list, TreeUtil.build(collect, CommonConstants.LABEL_TREE_ROOT_ID));
    }

    @Override
    public List<LabelConfigDO> getAllLabelConfig() {
        LambdaQueryWrapperX<LabelConfigDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.select(LabelConfigDO::getId, LabelConfigDO::getParentId, LabelConfigDO::getLabelName);
        return labelConfigMapper.selectList(wrapper);
    }

    @Override
    public List<LabelConfigDO> getByIds(List<Long> ids) {
        LambdaQueryWrapperX<LabelConfigDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.in(LabelConfigDO::getId, ids);
        return labelConfigMapper.selectList(wrapper);
    }

    /**
     * 获取父节点到顶
     *
     * @param label 标签
     * @return
     */
    private List<LabelConfigDO> getParent(LabelConfigDO label) {
        List<LabelConfigDO> list = new ArrayList<>();

        Long parentId = label.getParentId();
        if (CommonConstants.LABEL_TREE_ROOT_ID.equals(parentId)) {
            list.add(label);
            return list;
        }

        list.add(label);
        LabelConfigDO labelConfigDO = labelConfigMapper.selectById(parentId);
        List<LabelConfigDO> parents = getParent(labelConfigDO);
        list.addAll(parents);
        return list;
    }

    /**
     * 获取子节点到底
     *
     * @param label 标签
     * @return
     */
    private List<LabelConfigDO> getChildren(LabelConfigDO label) {

        List<LabelConfigDO> list = new ArrayList<>();

        List<LabelConfigDO> labelList = labelConfigMapper.selectList("parent_id", label.getId());
        if (CollectionUtil.isEmpty(labelList)) {
            list.add(label);
            return list;
        }

        list.add(label);
        for (LabelConfigDO labelConfigDO : labelList) {

            List<LabelConfigDO> childrens = getChildren(labelConfigDO);
            list.addAll(childrens);
        }
        return list;

    }


    @NotNull
    private Function<LabelConfigDO, TreeNode<Long>> getNodeFunction() {
        return label -> {
            Map<String, Object> extraData = new HashMap<>();
            extraData.put("code", label.getCode());
            extraData.put("ifDefault", label.getIfDefault());

            TreeNode<Long> node = new TreeNode<>();
            node.setId(label.getId());
            node.setName(label.getLabelName());
            node.setParentId(label.getParentId());
            node.setWeight(label.getSort());
            node.setExtra(extraData);
            return node;
        };
    }

    private void validateDirectBinding(Long labelId) {
        LambdaQueryWrapperX<StandingbookDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.apply("JSON_SEARCH(label_info, 'one', {0}) IS NOT NULL", labelId);

        Long count = standingbookMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw exception(LABEL_CONFIG_HAS_DEVICE); // 错误码示例：父标签作为 Value 被绑定
        }
    }

    private void validateChildBinding(List<Long> childIds) {
        if (CollectionUtil.isEmpty(childIds)) return;

        LambdaQueryWrapperX<StandingbookDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.and(qw -> {
            childIds.forEach(childId ->
                    qw.or().apply("JSON_SEARCH(label_info, 'one', {0}) IS NOT NULL", childId)
            );
        });

        Long count = standingbookMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw exception(LABEL_CONFIG_CHILDREN_HAS_DEVICE);
        }
    }

    private void validateLabelDeviceBinding(Long labelId) {
        // 1. 获取所有子标签ID（排除自身）
        List<Long> allLabelIds = getAllRelatedLabelIds(labelId);
        List<Long> childIds = allLabelIds.stream()
                .filter(id -> !id.equals(labelId))
                .collect(Collectors.toList());

        // 2. 分层校验
        validateDirectBinding(labelId);  // 先校验自身是否被绑定
        validateChildBinding(childIds);  // 再校验子标签是否被绑定
    }

    // 获取标签及其子标签ID（包含自身）
    private List<Long> getAllRelatedLabelIds(Long labelId) {
        List<Tree<Long>> labelTree = getLabelTree(false, labelId, null);
        if (CollectionUtil.isEmpty(labelTree)) return Collections.emptyList();
        List<Long> ids = new ArrayList<>();
        ids.add(labelId);
        extractTreeIds(labelTree, ids);
        return ids.stream().distinct().collect(Collectors.toList());
    }

    // 递归提取树节点ID
    private void extractTreeIds(List<Tree<Long>> trees, List<Long> ids) {
        if (CollectionUtil.isEmpty(trees)) return;  // 关键：避免处理空集合
        for (Tree<Long> tree : trees) {
            if (tree == null) continue;  // 跳过空节点
            ids.add(tree.getId());
            List<Tree<Long>> children = tree.getChildren();
            if (CollectionUtil.isNotEmpty(children)) {  // 检查子节点是否非空
                extractTreeIds(children, ids);
            }
        }
    }
}