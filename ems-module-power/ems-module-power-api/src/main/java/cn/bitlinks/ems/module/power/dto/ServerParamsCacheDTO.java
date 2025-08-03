package cn.bitlinks.ems.module.power.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 服务配置 与 io地址的映射关系
 */
@Data
public class ServerParamsCacheDTO implements Serializable {
    private static final long serialVersionUID = 1L; // 推荐指定序列化版本
    /**
     * 服务主键（ss.protocol,ss.ip_address, ss.username, ss.`password`, ss.clsid）
     */
    private String serverKey;

    /**
     * io地址
     */
    private String dataSite;
    /**
     * 台账id
     */
    private Long standingbookId;

}

