package cn.bitlinks.ems.module.power.controller.admin.report.hvac;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "管理后台 - 个性化报表[暖通科报表]-天然气报表")
@RestController
@RequestMapping("/power/report/hvac/naturalGas")
@Validated
public class NaturalGasController {

}
