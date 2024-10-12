package cn.bitlinks.ems.module.infra.framework.file.config;

import cn.bitlinks.ems.module.infra.framework.file.core.client.FileClientFactory;
import cn.bitlinks.ems.module.infra.framework.file.core.client.FileClientFactoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件配置类
 *
 * @author bitlinks
 */
@Configuration(proxyBeanMethods = false)
public class EmsFileAutoConfiguration {

    @Bean
    public FileClientFactory fileClientFactory() {
        return new FileClientFactoryImpl();
    }

}
