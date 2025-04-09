package cn.bitlinks.ems.module.power.service.warningtemplate;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.warningtemplate.vo.WarningTemplatePageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.warningtemplate.vo.WarningTemplateSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.warningtemplate.WarningTemplateDO;
import cn.bitlinks.ems.module.power.dal.mysql.warningstrategy.WarningStrategyMapper;
import cn.bitlinks.ems.module.power.dal.mysql.warningtemplate.WarningTemplateMapper;
import cn.bitlinks.ems.module.power.enums.warninginfo.WarningTemplateKeyWordEnum;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.ap.internal.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
@Slf4j
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
        // 校验 关键字是否合法
        validTemplateIllegal(warningTemplate);
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
        // 校验 关键字是否合法
        validTemplateIllegal(updateObj);
        warningTemplateMapper.updateById(updateObj);
    }

    /**
     * 校验 参数中的关键字是否合法
     *
     * @param warningTemplateDO 模板实体
     */
    private void validTemplateIllegal(WarningTemplateDO warningTemplateDO) {
        boolean contentValid = WarningTemplateKeyWordEnum.areAnyKeywordsOutsideRange(warningTemplateDO.getParams());
        if (contentValid) {
            throw exception(WARNING_TEMPLATE_CONTENT_ILLEGAL);
        }
        boolean titleValid = WarningTemplateKeyWordEnum.areAnyKeywordsOutsideRange(warningTemplateDO.getTParams());
        if (titleValid) {
            throw exception(WARNING_TEMPLATE_TITLE_ILLEGAL);
        }
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


    @Override
    public List<String> parseTemplateContentParams(String content) {
        return ReUtil.findAllGroup1(PATTERN_PARAMS, content);
    }

    @Override
    public String buildTitleOrContentByParams(List<String> keyWord, String templateStr, List<Map<String, String>> conditionParamsMapList) {
        try {
            // 如果模板中参数都是唯一的，那么不需要循环，直接按照模板填充一次即可，否则，需要循环内容，
            StringBuilder sb = new StringBuilder();
            //没有系统关键字，则不需要填充参数，直接返回模板的字符串
            if(CollUtil.isEmpty(keyWord)){
                return templateStr;
            }
            boolean isNoKey = WarningTemplateKeyWordEnum.areAnyKeywordsOutsideRange(keyWord);
            if (isNoKey) {
                return templateStr;
            }
            // 是否只有唯一的字符串
            boolean isUnique = WarningTemplateKeyWordEnum.areAnyKeywordsOutsideUniqueRange(keyWord);
            if (isUnique) {
                sb.append(StrUtil.format(templateStr, conditionParamsMapList.get(0)));
                return sb.toString();
            }
            // 如果没有表格，按照整体内容处理
            if (!templateStr.contains("<table>")) {
                conditionParamsMapList.forEach(paramMap -> sb.append(StrUtil.format(templateStr, paramMap)));
                return sb.toString();
            }
            // --------------   非人能及（分割线）  ------------------------------
            // 如果有表格，按照表格拆分成三块内容进行处理，表格上方是文字主题，表格下方是文字主题，表格的tr行需要重复处理
            // 假设有多个<table>出现，一样按照三块内容处理，用户的问题，
            int firstTableIndex = templateStr.indexOf("<table>");
            int lastTableIndex = templateStr.lastIndexOf("</table>");
            String part1 = templateStr.substring(0, firstTableIndex);
            String part2 = templateStr.substring(firstTableIndex, lastTableIndex + "</table>".length());
            String part3 = templateStr.substring(lastTableIndex + "</table>".length());
            if (Strings.isNotEmpty(part1)) {
                // part1当作文本主题，需要查询此部分的关键字，是否唯一，是否有，如果有，则，
                List<String> part1Key = parseTemplateContentParams(part1);
                sb.append(buildTitleOrContentByParams(part1Key, part1, conditionParamsMapList));
            }
            //处理表格内容，需要重复tr部分的字符串，补充完成的表格。只处理tbody下第一个tr/tr之间的重复，其他忽视，其他不处理，倒反天罡而已。
            if (Strings.isNotEmpty(part2)) {
                int tbodyIndex = part2.indexOf("<tbody>");
                int firstTrIndex = part2.indexOf("<tr>", tbodyIndex);
                int firstTrCloseIndex = part2.indexOf("</tr>", firstTrIndex);
                String part21 = part2.substring(0, tbodyIndex + "<tbody>".length());
                sb.append(part21);
                String part22 = part2.substring(firstTrIndex, firstTrCloseIndex + "</tr>".length());
                conditionParamsMapList.forEach(paramMap -> sb.append(String.format(part22, paramMap)));
                sb.append(part2.substring(firstTrCloseIndex + "</tr>".length()));
                // sb.append(part23);
            }
            if (Strings.isNotEmpty(part3)) {
                // part3当作文本主题，需要查询此部分的关键字，是否唯一，是否有，如果有，则，
                List<String> part3Key = parseTemplateContentParams(part3);
                sb.append(buildTitleOrContentByParams(part3Key, part3, conditionParamsMapList));

            }
            return sb.toString();
        } catch (Exception e) {
            log.error("告警模板内容：【{}】，关键字：【{}】，填充参数:【{}】，发生异常", keyWord, templateStr, conditionParamsMapList, e);
            return null;
        }
    }

}