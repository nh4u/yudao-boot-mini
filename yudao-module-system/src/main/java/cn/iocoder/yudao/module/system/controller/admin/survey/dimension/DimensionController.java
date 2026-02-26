package cn.iocoder.yudao.module.system.controller.admin.survey.dimension;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.system.dal.dataobject.survey.dimension.SurveyDimension;
import cn.iocoder.yudao.module.system.service.survey.dimension.DimensionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/survey/dimension")
public class DimensionController {

    @Autowired
    private DimensionService dimensionService;

    @PostMapping("/add")
    public CommonResult<String> add(@RequestBody SurveyDimension dimension) {
        dimensionService.save(dimension);
        return CommonResult.success("维度创建成功");
    }

    @GetMapping("/list") // Changed from Result to CommonResult
    public CommonResult<String> list() {
        return CommonResult.success(dimensionService.list());
    }

    @PutMapping("/update")
    public CommonResult<String> update(@RequestBody SurveyDimension dimension) {
        dimensionService.updateById(dimension);
        return CommonResult.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public CommonResult<String> delete(@PathVariable Long id) {
        dimensionService.removeById(id);
        return CommonResult.success("删除成功");
    }
}