package cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo;

import lombok.Data;

import java.util.List;

/**
 * @Title: identifier-carrier
 * @description:
 * @Author: Jiayun CUI
 * @Date 2025/02/25 17:45
 **/
@Data
public class CreateAdditionalRecordingDTO {
    private List<Long> voucherIds;
    private Long standingbookId;
}
