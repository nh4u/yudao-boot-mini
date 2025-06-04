package cn.bitlinks.ems.module.acquisition.api.quartz;

import cn.bitlinks.ems.module.acquisition.api.quartz.dto.AcquisitionJobDTO;
import cn.bitlinks.ems.module.acquisition.quartz.entity.JobBean;
import cn.bitlinks.ems.module.acquisition.quartz.job.AcquisitionJob;
import cn.bitlinks.ems.module.acquisition.quartz.job.QuartzManager;
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.Trigger;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.*;

@Slf4j
@RestController // 提供 RESTful API 接口，给 Feign 调用
@Validated
public class QuartzApiImpl implements QuartzApi {


    @Resource
    private QuartzManager quartzManager;

    @Override
    public void createOrUpdateJob(AcquisitionJobDTO acquisitionJobDTO) {
        String jobName = String.format(ACQUISITION_JOB_NAME_PREFIX, acquisitionJobDTO.getStandingbookId());
        try {

            // 组装任务参数
            JobBean jobBean = new JobBean();
            jobBean.setJobName(jobName);
            jobBean.setFrequency(acquisitionJobDTO.getFrequency());
            jobBean.setFrequencyUnit(acquisitionJobDTO.getFrequencyUnit());
            jobBean.setStartTime(acquisitionJobDTO.getJobStartTime());
            jobBean.setJobClass(AcquisitionJob.class);
            Map<String, Object> detailDTOMap = new HashMap<>();
            detailDTOMap.put(ACQUISITION_JOB_DATA_MAP_KEY_DETAILS, acquisitionJobDTO.getDetails());
            detailDTOMap.put(ACQUISITION_JOB_DATA_MAP_KEY_STANDING_BOOK_ID, acquisitionJobDTO.getStandingbookId());
            detailDTOMap.put(ACQUISITION_JOB_DATA_MAP_KEY_SERVICE_SETTINGS, acquisitionJobDTO.getServiceSettingsDTO());
            detailDTOMap.put(ACQUISITION_JOB_DATA_MAP_KEY_STATUS, acquisitionJobDTO.getStatus());
            jobBean.setJobDataMap(new JobDataMap(detailDTOMap));


            Boolean status = acquisitionJobDTO.getStatus();
            Boolean existJob = quartzManager.checkExists(jobName);

            if (existJob) {
                // 查询任务状态
                Trigger.TriggerState triggerState = quartzManager.getTriggerState(jobName);
                log.info("任务[{}] 当前触发器状态: {}", jobName, triggerState);

                if (!status) {
                    if (triggerState != Trigger.TriggerState.PAUSED) {
                        quartzManager.pauseJob(jobName);
                        log.info("任务[{}]已暂停", jobName);
                    }
                    return;
                }
                // status = true：任务需要执行
                if (triggerState == Trigger.TriggerState.PAUSED) {
                    // 恢复暂停的任务
                    quartzManager.resumeJob(jobName);
                    log.info("任务[{}]已恢复", jobName);
                }
                // 更新任务（无论是否暂停，都可能需要更新 cron 或 startTime）
                quartzManager.updateJob(jobBean);
                log.info("任务[{}]已更新", jobName);
                return;
            }

            // 任务不存在，status = true 时创建
            if (status) {
                quartzManager.createJob(jobBean);
                log.info("任务[{}]已创建", jobName);
                return;
            }
            log.info("任务[{}]不存在 且 status=false，无需创建", jobName);

        } catch (Exception e) {
            log.error("任务[{}] 创建/修改失败, 异常:{}", acquisitionJobDTO.getStandingbookId(), e.getMessage(), e);
        }
    }

    @Override
    public void deleteJob(List<Long> standingbookIds) {
        if(CollUtil.isEmpty(standingbookIds)){
            return;
        }
        standingbookIds.forEach(standingbookId -> {
            String jobName = String.format(ACQUISITION_JOB_NAME_PREFIX, standingbookId);
            try {
                if (quartzManager.checkExists(jobName)) {
                    quartzManager.deleteJob(jobName);
                }
            } catch (Exception e) {
                log.error("删除任务[{}]失败, 异常:{}", jobName, e.getMessage(), e);
            }
        });
    }
}
