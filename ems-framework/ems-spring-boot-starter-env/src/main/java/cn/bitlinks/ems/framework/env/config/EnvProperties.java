package cn.bitlinks.ems.framework.env.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 环境配置
 *
 * @author bitlinks
 */
@ConfigurationProperties(prefix = "ems.env")
@Data
public class EnvProperties {

    public static final String TAG_KEY = "ems.env.tag";

    /**
     * 环境标签
     */
    private String tag;

}
