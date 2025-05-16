package cn.bitlinks.ems.module.acquisition.api.quartz;

import cn.bitlinks.ems.module.acquisition.api.job.QuartzApi;
import cn.bitlinks.ems.module.acquisition.api.job.dto.AcquisitionJobDTO;
import cn.bitlinks.ems.module.acquisition.api.job.dto.StandingbookAcquisitionDetailDTO;
import cn.bitlinks.ems.module.acquisition.quartz.entity.JobBean;
import cn.bitlinks.ems.module.acquisition.quartz.job.AcquisitionJob;
import cn.bitlinks.ems.module.acquisition.quartz.job.QuartzManager;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.ACQUISITION_JOB_DATA_MAP_KEY;
import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.ACQUISITION_JOB_NAME_PREFIX;

@Slf4j
@RestController // 提供 RESTful API 接口，给 Feign 调用
@Validated
public class QuartzApiImpl implements QuartzApi {


    @Resource
    private QuartzManager quartzManager;

    @Override
    public void createOrUpdateJob(AcquisitionJobDTO acquisitionJobDTO) {
        String jobName = String.format(ACQUISITION_JOB_NAME_PREFIX, acquisitionJobDTO.getStandingbookId());

        // 组装任务参数
        JobBean jobBean = new JobBean();
        jobBean.setJobName(jobName);
        jobBean.setCronExpression(acquisitionJobDTO.getCronExpression());
        jobBean.setJobClass(AcquisitionJob.class);
        Map<String, List<StandingbookAcquisitionDetailDTO>> detailDTOMap = new HashMap<>();
        detailDTOMap.put(ACQUISITION_JOB_DATA_MAP_KEY, acquisitionJobDTO.getDetails());
        jobBean.setJobDataMap(new JobDataMap(detailDTOMap));

        try {
            // 查询是否有此任务,有则修改, 无则新增
            if (quartzManager.checkExists(jobName)) {
                quartzManager.createJob(jobBean);
                return;
            }
            quartzManager.updateJob(jobBean);
        } catch (Exception e) {
            log.error("创建/修改任务失败 台账id:{},异常:{}", acquisitionJobDTO.getStandingbookId(), e.getMessage(), e);
        }
    }

    @Override
    public void deleteJob(Long standingbookId) {
        quartzManager.deleteJob(String.format(ACQUISITION_JOB_NAME_PREFIX, standingbookId));
    }
}
