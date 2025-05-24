package cn.bitlinks.ems.module.acquisition.framework.rpc.config;

import cn.bitlinks.ems.module.acquisition.api.starrocks.StarRocksFeignClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableFeignClients(clients = {StarRocksFeignClient.class})
public class RpcConfiguration {
}
