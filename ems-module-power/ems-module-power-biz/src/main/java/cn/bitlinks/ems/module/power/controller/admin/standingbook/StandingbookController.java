package cn.bitlinks.ems.module.power.controller.admin.standingbook;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.StandingbookWithAssociations;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookRespVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.mysql.energyconfiguration.EnergyConfigurationMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.StandingbookMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.type.StandingbookTypeMapper;
import cn.bitlinks.ems.module.power.enums.ErrorCodeConstants;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.bitlinks.ems.module.power.service.standingbook.attribute.StandingbookAttributeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.IMPORT;
import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 台账")
@RestController
@RequestMapping("/power/standingbook")
@Validated
public class StandingbookController {
    @Resource
    private StandingbookService standingbookService;
    @Resource
    private StandingbookAttributeService standingbookAttributeService;
    @Resource
    private EnergyConfigurationMapper energyConfigurationMapper;
    @Resource
    private StandingbookTypeMapper standingbookTypeMapper;

    @PostMapping("/create")
    @Operation(summary = "创建台账")
    @PreAuthorize("@ss.hasPermission('power:standingbook:create')")
    public CommonResult<Long> createStandingbook(@Valid  @RequestBody Map <String,String>  createReqVO) {
        return success(standingbookService.createStandingbook(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新台账")
    @PreAuthorize("@ss.hasPermission('power:standingbook:update')")
    public CommonResult<Boolean> updateStandingbook(@Valid @RequestBody Map <String,String>  updateReqVO) {
        standingbookService.updateStandingbook(updateReqVO);
        return success(true);
    }

    @GetMapping("/getUnitById")
    @Operation(summary = "根据台账id获取对应的单位")
    @PreAuthorize("@ss.hasPermission('power:standingbook:update')")
    public CommonResult<String> getUnitById(@RequestParam("id") Long id) {
        String energy = standingbookTypeMapper.selectAttributeValueByCode(id, "energy");
        return success(energyConfigurationMapper.selectUnitByEnergyNameAndChinese(energy));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除台账")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('power:standingbook:delete')")
    public CommonResult<Boolean> deleteStandingbook( @RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return CommonResult.error(ErrorCodeConstants.STANDINGBOOK_NOT_EXISTS.getCode(), "台账编号不能为空");
        }
        for (Long aLong : ids) {
            standingbookService.deleteStandingbook(aLong);
        }
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得台账")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<StandingbookRespVO> getStandingbook(@RequestParam("id") Long id) {
        StandingbookDO standingbook = standingbookService.getStandingbook(id);
        return success(BeanUtils.toBean(standingbook, StandingbookRespVO.class));
    }
    @PostMapping("/list")
    @Operation(summary = "获得台账列表")
    @PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<List<StandingbookRespVO>> getStandingbookPage(@Valid @RequestBody Map <String,String> pageReqVO) {
        List<StandingbookDO> list = standingbookService.getStandingbookList(pageReqVO);
        return success(BeanUtils.toBean(list, StandingbookRespVO.class));
    }
    @PostMapping("/listByBaseTopType")
    @Operation(summary = "获得计量器具（topType=2）或者重点设备（topType=1）台账列表")
    @PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<List<StandingbookRespVO>> listByBaseTypeId(@RequestBody Map<String, String> pageReqVO) {
        List<StandingbookDO> list = standingbookService.listByBaseTypeId(pageReqVO);
        return success(BeanUtils.toBean(list, StandingbookRespVO.class));
    }

    @PostMapping("/listBy")
    @Operation(summary = "根据条件获得台账列表")
    @PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<List<StandingbookRespVO>> getStandingbookPageBy(@Valid @RequestBody Map <String,String> pageReqVO) {
        List<StandingbookDO> list = standingbookService.getStandingbookListBy(pageReqVO);
        return success(BeanUtils.toBean(list, StandingbookRespVO.class));
    }

    @PostMapping("/listWithAssociations")
    @Operation(summary = "根据条件获得台账列表和联系")
    @PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<List<StandingbookWithAssociations>> getStandingbookListWithAssociations(@RequestBody Map<String, String> pageReqVO) {
        List<StandingbookWithAssociations> list = standingbookService.getStandingbookListWithAssociations(pageReqVO);
        return success(BeanUtils.toBean(list, StandingbookWithAssociations.class));
    }

    @PostMapping(value = "importStandingbook")
    @Operation(summary = "导入台账 Excel")
    @PreAuthorize("@ss.hasPermission('power:standingbook:export')")
    @ApiAccessLog(operateType = IMPORT)
    public  CommonResult importStandingbook(@RequestParam("file")MultipartFile file, @RequestBody StandingbookRespVO pageReqVO) {
        if(pageReqVO.getTypeId() == null){
            return CommonResult.error(ErrorCodeConstants.STANDINGBOOK_TYPE_NOT_EXISTS.getCode(),"台账类型不能为空");
        }
        return success(standingbookService.importStandingbook(file,pageReqVO));
    }
    @PostMapping("/export-excel")
    @Operation(summary = "导出台账 Excel")
    @PreAuthorize("@ss.hasPermission('power:standingbook:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStandingbookExcel(@Valid @RequestBody Map <String,String> pageReqVO,
              HttpServletResponse response) {
        if (pageReqVO == null && pageReqVO.get("typeId") == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        standingbookService.exportStandingbookExcel(pageReqVO, response);
    }
     @GetMapping("/export-excel-template")
    @Operation(summary = "下载台账导入模板 Excel")
    @PreAuthorize("@ss.hasPermission('power:standingbook:export')")
     @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @ApiAccessLog(operateType = EXPORT)
    public void template(@RequestParam("typeId") Long typeId, HttpServletResponse response)  {
        if (typeId == null) {
            throw new IllegalArgumentException("台账编号不能为空");
        }
         standingbookService.template(typeId, response);

    }

}
