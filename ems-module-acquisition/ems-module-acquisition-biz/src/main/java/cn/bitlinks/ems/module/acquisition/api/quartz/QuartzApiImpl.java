package cn.bitlinks.ems.module.acquisition.api.quartz;

import cn.bitlinks.ems.module.acquisition.api.job.QuartzApi;
import cn.bitlinks.ems.module.acquisition.api.job.dto.AcquisitionJobDTO;
import cn.bitlinks.ems.module.acquisition.quartz.job.QuartzManager;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.ACQUISITION_JOB_NAME_PREFIX;


@RestController // 提供 RESTful API 接口，给 Feign 调用
@Validated
public class QuartzApiImpl implements QuartzApi {


    @Resource
    private QuartzManager quartzManager;

    @Override
    public void createOrUpdateJob(AcquisitionJobDTO acquisitionJobDTO) {
        String jobName = String.format(ACQUISITION_JOB_NAME_PREFIX, acquisitionJobDTO.getStandingbookId());
//        acquisitionJobDTO.getStandingbookId();
//
//        acquisitionJobDTO.getJobStartTime();
        // 根据任务名
        // 根据设备部分进行计算,
        // 创建任务,
    }

    @Override
    public void deleteJob(Long standingbookId) {
        String jobName = String.format(ACQUISITION_JOB_NAME_PREFIX, standingbookId);
        quartzManager.deleteJob(jobName);
    }
}
