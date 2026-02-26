package cn.iocoder.yudao.module.system.controller.admin.survey;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.system.controller.admin.survey.dto.SurveyVO;
import cn.iocoder.yudao.module.system.dal.dataobject.survey.SurveyMain;
import cn.iocoder.yudao.module.system.service.survey.SurveyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/survey/main")
public class SurveyController {

    @Autowired
    private SurveyService surveyService;

    // 创建问卷基础信息
    @PostMapping("/save")
    public CommonResult saveSurvey(@RequestBody SurveyMain survey) {
        surveyService.save(survey);
        return CommonResult.success(survey.getId()); // 返回ID供后续绑定题目
    }

    // 给问卷分配题目
    @PostMapping("/{surveyId}/bind-questions")
    public CommonResult bindQuestions(@PathVariable Long surveyId, @RequestBody List<Long> questionIds) {
        // 在 survey_item 表中建立关联
        surveyService.bindQuestions(surveyId, questionIds);
        return CommonResult.success("问卷题目组装完成");
    }

    // 获取完整问卷（包含题目和各题对应的维度描述）
    @GetMapping("/{id}/full")
    public CommonResult getFullSurvey(@PathVariable Long id) {
        SurveyVO fullSurvey = surveyService.getFullSurveyStructure(id);
        return CommonResult.success(fullSurvey);
    }
}