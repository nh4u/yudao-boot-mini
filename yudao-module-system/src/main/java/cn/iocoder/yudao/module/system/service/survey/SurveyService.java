package cn.iocoder.yudao.module.system.service.survey;

import cn.iocoder.yudao.module.system.controller.admin.survey.dto.SurveyVO;
import cn.iocoder.yudao.module.system.dal.dataobject.survey.SurveyMain;

import java.util.List;

public interface SurveyService {
    /**
     * 给问卷绑定题目
     */
    void bindQuestions(Integer surveyId, List<Integer> questionIds);

    /**
     * 获取完整问卷结构（包含题目及其对应的维度系数）
     */
    SurveyVO getFullSurveyStructure(Integer surveyId);

    boolean createSurvey(SurveyMain survey);

    boolean updateSurvey(SurveyMain survey, List<Integer> questionIds);

    boolean deleteSurvey(Integer id);

    SurveyMain getSurveyInfo(Integer id);
}
