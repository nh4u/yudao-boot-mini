package cn.bitlinks.ems.module.acquisition.quartz.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.quartz.Job;
import org.quartz.JobDataMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobBean {
    /**
     * 任务名-唯一标识
     */
    private String jobName;
    /**
     * 具体任务
     */
    private Class<? extends Job> jobClass;
    /**
     * 任务表达式
     */
    private String cronExpression;
    /**
     * 任务初始化数据
     */
    private JobDataMap jobDataMap;
}
