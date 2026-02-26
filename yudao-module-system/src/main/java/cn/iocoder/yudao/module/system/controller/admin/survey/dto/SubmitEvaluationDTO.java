package cn.iocoder.yudao.module.system.controller.admin.survey.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SubmitEvaluationDTO {
    private Integer surveyId;
    private Integer studentId;
    private LocalDate fillDate;
    private String remark;
    // 题目打分列表
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        private Integer questionId;
        private Integer score;
    }
}