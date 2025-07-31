package cn.bitlinks.ems.module.power.controller.admin.report.electricity;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理后台 - 个性化报表[电]-电费报表")
@RestController
@RequestMapping("/power/report/electricity/feeStatistics")
@Validated
public class FeeStatisticsController {
}
