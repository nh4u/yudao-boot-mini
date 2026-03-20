package cn.iocoder.yudao.module.system.service.survey.question;

import cn.iocoder.yudao.module.system.controller.admin.survey.dto.DimensionWeightDTO;
import cn.iocoder.yudao.module.system.controller.admin.survey.dto.QuestionDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.survey.question.SurveyQuestion;
import cn.iocoder.yudao.module.system.dal.dataobject.survey.question.SurveyQuestionDimension;
import cn.iocoder.yudao.module.system.dal.mysql.survey.SurveyQuestionDimensionMapper;
import cn.iocoder.yudao.module.system.dal.mysql.survey.SurveyQuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuestionServiceImpl implements QuestionService {

    @Autowired
    private SurveyQuestionDimensionMapper weightMapper;
    @Autowired
    private SurveyQuestionMapper questionMapper;

//    @Transactional(rollbackFor = Exception.class)
//    public void createWithWeights(QuestionDTO dto) {
//        // 1. 保存题目基础信息
//        SurveyQuestion question = new SurveyQuestion();
//        question.setContent(dto.getContent());
//        questionMapper.insert(question);
//
//        // 2. 保存维度系数关联
//        if (dto.getDimensionWeights() != null) {
//            for (DimensionWeightDTO weightDto : dto.getDimensionWeights()) {
//                SurveyQuestionDimension qd = new SurveyQuestionDimension();
//                qd.setQuestionId(question.getId());
//                qd.setDimensionId(weightDto.getDimensionId());
//                qd.setWeight(weightDto.getWeight());
//                weightMapper.insert(qd);
//            }
//        }
//    }
//
//    public QuestionVO getQuestionDetail(Integer id) {
//        SurveyQuestion question = questionMapper.selectById(id);
//        // 查询该题目关联的所有维度权重
//        List<SurveyQuestionDimension> weights = weightMapper.selectList(
//                new LambdaQueryWrapper<SurveyQuestionDimension>().eq(SurveyQuestionDimension::getQuestionId, id)
//        );
//
//        QuestionVO vo = new QuestionVO();
//        BeanUtils.copyProperties(question, vo);
//        vo.setDimensionWeights(weights);
//        return vo;
//    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createQuestion(QuestionDTO dto) {
        // 1. 调用 surveyQuestionMapper 插入题目基础信息
        SurveyQuestion question = new SurveyQuestion();
        question.setContent(dto.getContent());
        int result = questionMapper.insert(question);

        // 2. 处理题目与维度的权重关系
        if (result > 0 && dto.getDimensionWeights() != null) {
            for (DimensionWeightDTO weightDto : dto.getDimensionWeights()) {
                SurveyQuestionDimension qd = new SurveyQuestionDimension();
                qd.setQuestionId(question.getId());
                qd.setDimensionId(weightDto.getDimensionId());
                qd.setWeight(weightDto.getWeight());
                // 调用 weightMapper 保存关联
                weightMapper.insert(qd);
            }
        }
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeQuestion(Integer id) {
        // 1. 先删除题目关联的所有维度权重系数记录
        LambdaQueryWrapper<SurveyQuestionDimension> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SurveyQuestionDimension::getQuestionId, id);
        weightMapper.delete(wrapper);

        // 2. 调用 baseMapper 删除题目本身
        return questionMapper.deleteById(id) > 0;
    }

    @Override
    public List<SurveyQuestion> listQuestions() {
        return questionMapper.selectList(null);
    }
}