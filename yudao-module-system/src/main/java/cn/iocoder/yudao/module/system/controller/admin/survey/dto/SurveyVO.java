package cn.iocoder.yudao.module.system.controller.admin.survey.dto;

import lombok.Data;

import java.util.List;

@Data
public class SurveyVO {
    private Integer id;
    private String title;
    private List<QuestionVO> questions;
}