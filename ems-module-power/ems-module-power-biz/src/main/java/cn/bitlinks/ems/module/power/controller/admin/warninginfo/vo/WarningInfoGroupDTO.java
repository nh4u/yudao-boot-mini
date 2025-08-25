package cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WarningInfoGroupDTO {
    private Long strategyId;
    private LocalDateTime warningTime;
}