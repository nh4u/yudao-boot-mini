package cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeSaveMultipleReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeSaveReqVO;
import cn.bitlinks.ems.module.power.service.standingbook.attribute.StandingbookAttributeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 台账属性")
@RestController
@RequestMapping("/power/standingbook-attribute")
@Validated
public class StandingbookAttributeController {

    @Resource
    private StandingbookAttributeService standingbookAttributeService;


    @PostMapping("/saveMultiple")
    @Operation(summary = "保存多个台账属性")
    @PreAuthorize("@ss.hasPermission('power:standingbook-attribute:create')")
    public CommonResult<Boolean> saveMultiple(@Valid @RequestBody StandingbookAttributeSaveMultipleReqVO multipleReqVO) {
        List<StandingbookAttributeSaveReqVO> createReqVOs = multipleReqVO.getCreateReqVOs();
        standingbookAttributeService.saveMultiple(createReqVOs);
        return success(true);
    }


    @GetMapping("/getByTypeId")
    @Operation(summary = "获得台账属性ByTypeId")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:standingbook-attribute:query')")
    public CommonResult<List<StandingbookAttributeRespVO>> getByTypeId(@RequestParam("typeId") Long typeId) {

        return success(standingbookAttributeService.getByTypeId(typeId));
    }


}
