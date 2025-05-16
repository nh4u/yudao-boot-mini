package cn.bitlinks.ems.module.power.framework.rpc.config;

import cn.bitlinks.ems.module.acquisition.api.job.QuartzApi;
import cn.bitlinks.ems.module.infra.api.config.ConfigApi;
import cn.bitlinks.ems.module.infra.api.file.FileApi;
import cn.bitlinks.ems.module.infra.api.websocket.WebSocketSenderApi;
import cn.bitlinks.ems.module.system.api.dict.DictDataApi;
import cn.bitlinks.ems.module.system.api.user.AdminUserApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableFeignClients(clients = {FileApi.class, AdminUserApi.class, WebSocketSenderApi.class, ConfigApi.class,
        DictDataApi.class, QuartzApi.class})
public class RpcConfiguration {
}
