package cn.bitlinks.ems.module.power.controller.admin.externalapi;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.externalapi.vo.ExternalApiPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.externalapi.vo.ExternalApiRespVO;
import cn.bitlinks.ems.module.power.controller.admin.externalapi.vo.ExternalApiSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.externalapi.ExternalApiDO;
import cn.bitlinks.ems.module.power.service.externalapi.ExternalApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

/**
 * @author liumingqiang
 */
@Tag(name = "管理后台 - 外部数据接口管理")
@RestController
@RequestMapping("/power/externalApi")
@Validated
public class ExternalApiController {

    @Resource
    private ExternalApiService externalApiService;

    @PostMapping("/create")
    @Operation(summary = "创建外部接口")
    //@PreAuthorize("@ss.hasPermission('power:externalApi:create')")
    public CommonResult<ExternalApiRespVO> createExternalApi(@Valid @RequestBody ExternalApiSaveReqVO createReqVO) {
        ExternalApiDO externalApi = externalApiService.createExternalApi(createReqVO);
        return success(BeanUtils.toBean(externalApi, ExternalApiRespVO.class));
    }


    @PutMapping("/update")
    @Operation(summary = "更新外部接口")
    //@PreAuthorize("@ss.hasPermission('power:externalApi:update')")
    public CommonResult<Boolean> updateExternalApi(@Valid @RequestBody ExternalApiSaveReqVO updateReqVO) {
        externalApiService.updateExternalApi(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除外部接口")
    @Parameter(name = "id", description = "编号", required = true)
    //@PreAuthorize("@ss.hasPermission('power:externalApi:delete')")
    public CommonResult<Boolean> deleteExternalApi(@RequestParam("id") Long id) {
        externalApiService.deleteExternalApi(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得外部接口")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    //@PreAuthorize("@ss.hasPermission('power:externalApi:query')")
    public CommonResult<ExternalApiRespVO> getExternalApi(@RequestParam("id") Long id) {
        ExternalApiDO externalApi = externalApiService.getExternalApi(id);
        return success(BeanUtils.toBean(externalApi, ExternalApiRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得外部接口分页")
    //@PreAuthorize("@ss.hasPermission('power:externalApi:query')")
    public CommonResult<PageResult<ExternalApiRespVO>> getExternalApiPage(@Valid ExternalApiPageReqVO pageReqVO) {
        PageResult<ExternalApiDO> pageResult = externalApiService.getExternalApiPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ExternalApiRespVO.class));
    }


    @PostMapping("/test")
    @Operation(summary = "测试外部接口")
    //@PreAuthorize("@ss.hasPermission('power:externalApi:create')")
    public CommonResult<Object> testExternalApi(@Valid @RequestBody ExternalApiSaveReqVO createReqVO) {
        return success(externalApiService.testExternalApi(createReqVO));
    }


}