package cn.iocoder.yudao.module.system.service.survey;

import cn.iocoder.yudao.module.system.dal.dataobject.survey.SurveyItem;
import cn.iocoder.yudao.module.system.dal.dataobject.survey.SurveyMain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SurveyServiceImpl extends ServiceImpl<SurveyMainMapper, SurveyMain> implements SurveyService {

    @Autowired
    private SurveyItemMapper itemMapper;
    @Autowired
    private SurveyQuestionMapper questionMapper;
    @Autowired
    private SurveyQuestionDimensionMapper weightMapper;

    /**
     * 给问卷绑定题目
     */
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
    public SurveyVO getFullSurveyStructure(Integer surveyId) {
        SurveyMain survey = this.getById(surveyId);
        
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
}