package cn.bitlinks.ems.module.power.service.labelconfig;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.json.JsonUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.labelconfig.vo.LabelConfigPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.labelconfig.vo.LabelConfigSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.coalfactorhistory.CoalFactorHistoryDO;
import cn.bitlinks.ems.module.power.dal.dataobject.labelconfig.LabelConfigDO;
import cn.bitlinks.ems.module.power.dal.mysql.coalfactorhistory.CoalFactorHistoryMapper;
import cn.bitlinks.ems.module.power.dal.mysql.labelconfig.LabelConfigMapper;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNode;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
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

    @Override
    public Long createLabelConfig(LabelConfigSaveReqVO createReqVO) {
        // 校验层数限制
        validateLabelLayer(createReqVO.getParentId());

        // 校验标签 code 唯一性
        boolean codeExists = labelConfigMapper.selectCount(new LambdaQueryWrapperX<LabelConfigDO>()
                .eq(LabelConfigDO::getCode, createReqVO.getCode())) > 0;
        if (codeExists) {
            throw exception(LABEL_CONFIG_CODE_NOT_UNIQUE);
        }

        // 判断是否已有10条
        Long count = labelConfigMapper.selectCount(new LambdaQueryWrapperX<LabelConfigDO>()
                .eq(LabelConfigDO::getParentId, createReqVO.getParentId()));
        if (count >= CommonConstants.LABEL_NUM_LIMIT) {
            throw exception(LABEL_CONFIG_REACH_LIMIT);
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
        // 校验存在
        validateLabelConfigExists(updateReqVO.getId());
        // 更新
        LabelConfigDO updateObj = BeanUtils.toBean(updateReqVO, LabelConfigDO.class);
        labelConfigMapper.updateById(updateObj);
    }

    @Override
    public void deleteLabelConfig(Long id) {
        // 校验存在
        validateLabelConfigExists(id);
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
    @DS("slave")
    public LabelConfigDO getLabelConfig07(Long id) {

        List<Map<String, Objects>> map = labelConfigMapper.queryData();
        map.forEach(System.out::println);

        List<CoalFactorHistoryDO> list = coalFactorHistoryMapper.selectList();
        // 美化打印
        System.out.println(JsonUtils.toJsonPrettyString(list));


        // TODO: 2024/11/1 多数据源测试后续可删
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

}