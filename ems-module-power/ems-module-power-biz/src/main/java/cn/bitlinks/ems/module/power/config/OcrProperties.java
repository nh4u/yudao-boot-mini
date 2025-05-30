package cn.bitlinks.ems.module.power.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author liumingqiang
 */

@Component
@ConfigurationProperties(prefix = "ems.ocr")
@Data
public class OcrProperties {

    private String uploadUrl;

    private String token;

}
