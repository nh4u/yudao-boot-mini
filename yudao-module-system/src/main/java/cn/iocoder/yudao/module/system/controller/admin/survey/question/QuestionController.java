package cn.iocoder.yudao.module.system.controller.admin.survey.question;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.system.controller.admin.survey.dto.QuestionDTO;
import cn.iocoder.yudao.module.system.service.survey.question.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/survey/question")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    // 创建题目并绑定维度权重
    @PostMapping("/create")
    public CommonResult<String> create(@RequestBody QuestionDTO questionDto) {
        // QuestionDTO 包含题目内容及 List<DimensionWeight>
        questionService.createWithWeights(questionDto);
        return CommonResult.success("题目及权重配置成功");
    }

    @GetMapping("/{id}")
    public CommonResult<String> getDetail(@PathVariable Long id) {
        // 返回题目内容及关联的维度权重信息
        return CommonResult.success(questionService.getQuestionDetail(id));
    }

    @DeleteMapping("/{id}")
    public CommonResult<String> delete(@PathVariable Long id) {
        questionService.removeById(id); // 注意：需级联删除权重关联表
        return CommonResult.success("题目已移除");
    }
}