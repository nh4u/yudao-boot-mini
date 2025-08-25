package cn.bitlinks.ems.module.acquisition.api.config;

import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class ImportFeignConfig {

    @Bean
    public Request.Options requestOptions() {
        // 连接超时5秒，读取超时5分钟
        return new Request.Options(
            5, TimeUnit.SECONDS,
            5, TimeUnit.MINUTES,
            true
        );
    }
}