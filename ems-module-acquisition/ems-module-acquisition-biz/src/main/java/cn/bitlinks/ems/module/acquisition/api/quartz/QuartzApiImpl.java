package cn.bitlinks.ems.module.acquisition.api.quartz;

import cn.bitlinks.ems.module.acquisition.api.job.QuartzApi;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;



@RestController // 提供 RESTful API 接口，给 Feign 调用
@Validated
public class QuartzApiImpl implements QuartzApi {

//    @Resource
//    private ConfigService configService;

//    @Override
//    public CommonResult<String> getConfigValueByKey(String key) {
//        ConfigDO config = configService.getConfigByKey(key);
//        return success(config != null ? config.getValue() : null);
//    }

}
