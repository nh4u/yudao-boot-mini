package cn.iocoder.yudao.module.system.service.survey.question;

import cn.iocoder.yudao.module.system.controller.admin.survey.dto.QuestionDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.survey.question.SurveyQuestion;

import java.util.List;

public interface QuestionService {
    boolean createQuestion(QuestionDTO dto);

    boolean removeQuestion(Integer id);

    List<SurveyQuestion> listQuestions();
}
