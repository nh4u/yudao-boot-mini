package cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class MultiMinuteAggDataDTO implements Serializable {

    private static final long serialVersionUID = 1L; // 推荐指定序列化版本

    private List<MinuteAggregateDataDTO> minuteAggregateDataDTOList;

    private Boolean copFlag;

}
