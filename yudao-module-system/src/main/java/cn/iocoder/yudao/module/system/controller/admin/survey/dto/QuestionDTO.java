package cn.iocoder.yudao.module.system.controller.admin.survey.dto;

import lombok.Data;

import java.util.List;

// 题目创建 DTO
@Data
public class QuestionDTO {
    private String content;
    private List<DimensionWeightDTO> dimensionWeights;
}



// 问卷详情展示 VO


