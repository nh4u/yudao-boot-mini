package cn.bitlinks.ems.module.power.service.warningtemplate;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.warningtemplate.vo.WarningTemplatePageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.warningtemplate.vo.WarningTemplateSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.warningtemplate.WarningTemplateDO;
import cn.bitlinks.ems.module.power.dal.mysql.warningstrategy.WarningStrategyMapper;
import cn.bitlinks.ems.module.power.dal.mysql.warningtemplate.WarningTemplateMapper;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ReUtil;
import com.google.common.annotations.VisibleForTesting;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

/**
 * 告警模板 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class WarningTemplateServiceImpl implements WarningTemplateService {
    /**
     * 正则表达式，匹配 {} 中的变量
     */
    private static final Pattern PATTERN_PARAMS = Pattern.compile("\\{(.*?)}");

    @Resource
    private WarningTemplateMapper warningTemplateMapper;
    @Resource
    private WarningStrategyMapper warningStrategyMapper;

    @Override
    public Long createWarningTemplate(WarningTemplateSaveReqVO createReqVO) {
        // 校验 code 是否唯一
        validateCodeUnique(null, createReqVO.getType(), createReqVO.getCode());
        // 插入
        WarningTemplateDO warningTemplate = BeanUtils.toBean(createReqVO, WarningTemplateDO.class)
                .setParams(parseTemplateContentParams(createReqVO.getContent()))
                .setTParams(parseTemplateContentParams(createReqVO.getTitle()));
        warningTemplateMapper.insert(warningTemplate);
        // 返回
        return warningTemplate.getId();
    }

    @Override
    public void updateWarningTemplate(WarningTemplateSaveReqVO updateReqVO) {
        // 校验存在
        validateWarningTemplateExists(updateReqVO.getId());
        // 校验 code 是否唯一
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getType(), updateReqVO.getCode());
        // 更新
        WarningTemplateDO updateObj = BeanUtils.toBean(updateReqVO, WarningTemplateDO.class)
                .setParams(parseTemplateContentParams(updateReqVO.getContent()))
                .setTParams(parseTemplateContentParams(updateReqVO.getTitle()));
        warningTemplateMapper.updateById(updateObj);
    }

    @Override
    public void deleteWarningTemplate(Long id) {
        // 校验是否引用
        List<String> codes = queryUsedByStrategy(Collections.singletonList(id));
        if (CollUtil.isNotEmpty(codes)) {
            throw exception(WARNING_TEMPLATE_DELETE_ERROR);
        }
        // 删除
        warningTemplateMapper.deleteById(id);
    }


    private void validateWarningTemplateExists(Long id) {
        if (warningTemplateMapper.selectById(id) == null) {
            throw exception(WARNING_TEMPLATE_NOT_EXISTS);
        }
    }

    @Override
    public WarningTemplateDO getWarningTemplate(Long id) {
        return warningTemplateMapper.selectById(id);
    }

    @Override
    public PageResult<WarningTemplateDO> getWarningTemplatePage(WarningTemplatePageReqVO pageReqVO) {
        return warningTemplateMapper.selectPage(pageReqVO);
    }

    @Override
    public void deleteWarningTemplateBatch(List<Long> ids) {
        // 校验是否引用
        List<String> codes = queryUsedByStrategy(ids);
        if (CollUtil.isNotEmpty(codes)) {
            throw exception(WARNING_TEMPLATE_DELETE_BATCH_ERROR, CollUtil.join(codes, "、"));
        }
        warningTemplateMapper.deleteByIds(ids);
    }

    @Override
    public List<String> queryUsedByStrategy(List<Long> ids) {
        List<Long> usedTemplateIds = warningStrategyMapper.getAllTemplateIds();
        List<Long> result = ids.stream()
                .filter(usedTemplateIds::contains)
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(result)) {
            return null;
        }
        List<WarningTemplateDO> templateList = warningTemplateMapper.selectBatchIds(result);

        return templateList.stream().map(WarningTemplateDO::getCode).collect(Collectors.toList());
    }

    @Override
    public List<WarningTemplateDO> getWarningTemplateList(Integer type, String name) {
        return warningTemplateMapper.selectList(new LambdaQueryWrapperX<WarningTemplateDO>()
                    .likeIfPresent(WarningTemplateDO::getName, name)
                    .eq(WarningTemplateDO::getType, type)
                    .orderByDesc(WarningTemplateDO::getCreateTime));
    }

    @VisibleForTesting
    void validateCodeUnique(Long id, Integer type, String code) {
        WarningTemplateDO template = warningTemplateMapper.selectByCode(type, code);
        if (template == null) {
            return;
        }
        // 存在 template 记录的情况下
        if (id == null // 新增时，说明重复
                || ObjUtil.notEqual(id, template.getId())) { // 更新时，如果 id 不一致，说明重复
            throw exception(WARNING_TEMPLATE_CODE_EXISTS, code);
        }
    }

    @VisibleForTesting
    public List<String> parseTemplateContentParams(String content) {
        return ReUtil.findAllGroup1(PATTERN_PARAMS, content);
    }

}