package cn.iocoder.yudao.module.system.controller.admin.survey.dto;

import cn.iocoder.yudao.module.system.dal.dataobject.survey.question.SurveyQuestionDimension;
import lombok.Data;

import java.util.List;

@Data
public class QuestionVO {
    private Integer id;
    private String content;
    private List<SurveyQuestionDimension> dimensionWeights;
}