package cn.bitlinks.ems.module.acquisition.task.entity;

import lombok.Data;

import java.util.List;

@Data
public class AcConfig {
    private String ip;
    private String port;
    private String username;
    private String password;
    private String clsid;
    private List<String> ioAddresses;
}
