package cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class MinutePrevExistNextDataDTO implements Serializable {

    private static final long serialVersionUID = 1L; // 推荐指定序列化版本

    private MinuteAggregateDataDTO prevFullValue;
    private MinuteAggregateDataDTO existFullValue;
    private MinuteAggregateDataDTO nextFullValue;
}
