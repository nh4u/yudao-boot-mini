package cn.iocoder.yudao.module.system.service.survey;

import cn.iocoder.yudao.module.system.controller.admin.survey.dto.QuestionVO;
import cn.iocoder.yudao.module.system.controller.admin.survey.dto.SurveyVO;
import cn.iocoder.yudao.module.system.dal.dataobject.survey.SurveyItem;
import cn.iocoder.yudao.module.system.dal.dataobject.survey.SurveyMain;
import cn.iocoder.yudao.module.system.dal.dataobject.survey.question.SurveyQuestion;
import cn.iocoder.yudao.module.system.dal.dataobject.survey.question.SurveyQuestionDimension;
import cn.iocoder.yudao.module.system.dal.mysql.survey.SurveyItemMapper;
import cn.iocoder.yudao.module.system.dal.mysql.survey.SurveyMainMapper;
import cn.iocoder.yudao.module.system.dal.mysql.survey.SurveyQuestionDimensionMapper;
import cn.iocoder.yudao.module.system.dal.mysql.survey.SurveyQuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SurveyServiceImpl implements SurveyService {

    @Autowired
    private SurveyItemMapper itemMapper;
    @Autowired
    private SurveyMainMapper mainMapper;
    @Autowired
    private SurveyQuestionMapper questionMapper;
    @Autowired
    private SurveyQuestionDimensionMapper weightMapper;

    /**
     * 给问卷绑定题目
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindQuestions(Integer surveyId, List<Integer> questionIds) {
        // 先删除原有的题目绑定（简单处理，也可按需优化为增量更新）
        itemMapper.delete(new LambdaQueryWrapper<SurveyItem>().eq(SurveyItem::getSurveyId, surveyId));

        // 批量建立新关联
        for (int i = 0; i < questionIds.size(); i++) {
            SurveyItem item = new SurveyItem();
            item.setSurveyId(surveyId);
            item.setQuestionId(questionIds.get(i));
            item.setSortOrder(i + 1);
            itemMapper.insert(item);
        }
    }

    /**
     * 获取完整问卷结构（包含题目及其对应的维度系数）
     */
    @Override
    public SurveyVO getFullSurveyStructure(Integer surveyId) {
        SurveyMain survey = mainMapper.selectById(surveyId);

        // 1. 查询问卷下的所有题目ID
        List<SurveyItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<SurveyItem>()
                        .eq(SurveyItem::getSurveyId, surveyId)
                        .orderByAsc(SurveyItem::getSortOrder)
        );

        List<QuestionVO> questionVOs = new ArrayList<>();
        for (SurveyItem item : items) {
            // 2. 查询题目内容及维度权重信息
            SurveyQuestion q = questionMapper.selectById(item.getQuestionId());
            List<SurveyQuestionDimension> weights = weightMapper.selectList(
                    new LambdaQueryWrapper<SurveyQuestionDimension>()
                            .eq(SurveyQuestionDimension::getQuestionId, q.getId())
            );

            QuestionVO qVo = new QuestionVO();
            BeanUtils.copyProperties(q, qVo);
            qVo.setDimensionWeights(weights);
            questionVOs.add(qVo);
        }

        SurveyVO vo = new SurveyVO();
        BeanUtils.copyProperties(survey, vo);
        vo.setQuestions(questionVOs);
        return vo;
    }

    @Override
    public boolean createSurvey(SurveyMain survey) {
        // 保存问卷标题等基础信息
        return mainMapper.insert(survey) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSurvey(SurveyMain survey, List<Integer> questionIds) {
        // 1. 更新问卷基本信息
        mainMapper.updateById(survey);

        // 2. 更新题目关联：先清除旧关联，再插入新关联
        LambdaQueryWrapper<SurveyItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SurveyItem::getSurveyId, survey.getId());
        itemMapper.delete(wrapper);

        if (questionIds != null && !questionIds.isEmpty()) {
            for (int i = 0; i < questionIds.size(); i++) {
                SurveyItem item = new SurveyItem();
                item.setSurveyId(survey.getId());
                item.setQuestionId(questionIds.get(i));
                item.setSortOrder(i + 1);
                itemMapper.insert(item);
            }
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSurvey(Integer id) {
        // 1. 删除问卷下的题目关联
        LambdaQueryWrapper<SurveyItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SurveyItem::getSurveyId, id);
        itemMapper.delete(wrapper);

        // 2. 删除问卷主记录
        return mainMapper.deleteById(id) > 0;
    }

    @Override
    public SurveyMain getSurveyInfo(Integer id) {
        return mainMapper.selectById(id);
    }
}