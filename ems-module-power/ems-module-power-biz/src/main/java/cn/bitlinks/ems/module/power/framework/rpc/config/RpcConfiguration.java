package cn.bitlinks.ems.module.power.framework.rpc.config;

import cn.bitlinks.ems.module.infra.api.config.ConfigApi;
import cn.bitlinks.ems.module.infra.api.file.FileApi;
import cn.bitlinks.ems.module.infra.api.websocket.WebSocketSenderApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableFeignClients(clients = {FileApi.class, WebSocketSenderApi.class, ConfigApi.class})
public class RpcConfiguration {
}
