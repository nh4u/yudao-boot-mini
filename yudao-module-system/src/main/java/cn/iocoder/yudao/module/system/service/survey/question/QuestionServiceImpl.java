package cn.iocoder.yudao.module.system.service.survey.question;

import cn.iocoder.yudao.module.system.dal.dataobject.survey.question.SurveyQuestion;
import cn.iocoder.yudao.module.system.dal.dataobject.survey.question.SurveyQuestionDimension;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuestionServiceImpl extends ServiceImpl<SurveyQuestionMapper, SurveyQuestion> implements QuestionService {

    @Autowired
    private SurveyQuestionDimensionMapper weightMapper;

    @Transactional(rollbackFor = Exception.class)
    public void createWithWeights(QuestionDTO dto) {
        // 1. 保存题目基础信息
        SurveyQuestion question = new SurveyQuestion();
        question.setContent(dto.getContent());
        this.save(question);

        // 2. 保存维度系数关联
        if (dto.getDimensionWeights() != null) {
            for (DimensionWeightDTO weightDto : dto.getDimensionWeights()) {
                SurveyQuestionDimension qd = new SurveyQuestionDimension();
                qd.setQuestionId(question.getId());
                qd.setDimensionId(weightDto.getDimensionId());
                qd.setWeight(weightDto.getWeight());
                weightMapper.insert(qd);
            }
        }
    }

    public QuestionVO getQuestionDetail(Integer id) {
        SurveyQuestion question = this.getById(id);
        // 查询该题目关联的所有维度权重
        List<SurveyQuestionDimension> weights = weightMapper.selectList(
            new LambdaQueryWrapper<SurveyQuestionDimension>().eq(SurveyQuestionDimension::getQuestionId, id)
        );
        
        QuestionVO vo = new QuestionVO();
        BeanUtils.copyProperties(question, vo);
        vo.setDimensionWeights(weights);
        return vo;
    }
}