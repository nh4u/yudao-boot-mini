package cn.bitlinks.ems.module.power.controller.admin.standingbook.tmpl;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.tmpl.vo.StandingbookTmplDaqAttrRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.tmpl.vo.StandingbookTmplDaqAttrSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import cn.bitlinks.ems.module.power.service.standingbook.tmpl.StandingbookTmplDaqAttrService;
import cn.hutool.core.collection.CollUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Objects;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 台账模板数采属性")
@RestController
@RequestMapping("/power/standingbook-tmpl-daq-attr")
@Validated
public class StandingBookTmplDaqAttrController {

    @Resource
    private StandingbookTmplDaqAttrService standingbookTemplAttrService;

    @PostMapping("/saveMultiple")
    @Operation(summary = "保存多个台账数采模板属性")
    @PreAuthorize("@ss.hasPermission('power:standingbook-tmpl-daq-attr:create')")
    public CommonResult<Boolean> saveMultiple(@Valid @RequestBody List<StandingbookTmplDaqAttrSaveReqVO> saveReqVOList) {
        standingbookTemplAttrService.saveMultiple(saveReqVOList);
        return success(true);
    }

    @GetMapping("/getByTypeIdAndEnergyFlag")
    @Operation(summary = "获得台账分类的模板数采参数（能源+自定义）")
    @PreAuthorize("@ss.hasPermission('power:standingbook-tmpl-daq-attr:query')")
    public CommonResult<List<StandingbookTmplDaqAttrRespVO>> getByTypeIdAndEnergyFlag(@RequestParam("typeId") Long typeId,
                                                                                      @RequestParam("energyFlag") Boolean energyFlag) {
        return success(standingbookTemplAttrService.getByTypeIdAndEnergyFlag(typeId, energyFlag));
    }

    @GetMapping("/getDaqAttrsByStandingbookId")
    @Operation(summary = "获取台账id对应的所有数采参数（能源+自定义）查询启用的")
    @PreAuthorize("@ss.hasPermission('power:standingbook-tmpl-daq-attr:query')")
    public CommonResult<List<StandingbookTmplDaqAttrRespVO>> getDaqAttrsByStandingbookId(@RequestParam("standingbookId") Long standingbookId) {
        List<StandingbookTmplDaqAttrDO> standingbookTmplDaqAttrDOS =
                standingbookTemplAttrService.getDaqAttrsByStandingbookId(standingbookId);
        if (CollUtil.isEmpty(standingbookTmplDaqAttrDOS)) {
            return success(null);
        }
        return success(BeanUtils.toBean(standingbookTmplDaqAttrDOS, StandingbookTmplDaqAttrRespVO.class));

    }

    @GetMapping("/getUsageAttrBySbId")
    @Operation(summary = "根据台账id获取对应的用量的单位")
    @PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<StandingbookTmplDaqAttrRespVO> getUsageAttrBySbId(@RequestParam("id") Long id) {
        StandingbookTmplDaqAttrDO standingbookTmplDaqAttrDO = standingbookTemplAttrService.getUsageAttrBySbId(id);
        if (Objects.isNull(standingbookTmplDaqAttrDO)) {
            return success(null);
        }
        return success(BeanUtils.toBean(standingbookTmplDaqAttrDO, StandingbookTmplDaqAttrRespVO.class));
    }

}
