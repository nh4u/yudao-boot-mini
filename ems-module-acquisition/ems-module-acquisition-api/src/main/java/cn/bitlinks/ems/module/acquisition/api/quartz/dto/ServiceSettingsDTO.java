package cn.bitlinks.ems.module.acquisition.api.quartz.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * OpcDa 连接
 *
 * @author bitlinks
 */
@Data
public class ServiceSettingsDTO implements Serializable {
    private static final long serialVersionUID = 1L; // 推荐指定序列化版本

    /**
     * 服务名称
     */
    private String ipAddress;
    /**
     * 端口号
     */
    private Integer port;
    /**
     * 协议类型(0：OPC-DA 1:MODBUS-TCP)
     */
    private Integer protocol;

    /**
     * 注册表ID
     */
    private String clsid;
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private String password;


}