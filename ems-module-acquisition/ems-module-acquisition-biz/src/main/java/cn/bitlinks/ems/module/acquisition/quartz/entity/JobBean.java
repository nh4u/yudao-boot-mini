package cn.bitlinks.ems.module.acquisition.quartz.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.quartz.Job;
import org.quartz.JobDataMap;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

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
     * 频率
     */
    private Long frequency;

    /**
     * 频率单位
     */
    private Integer frequencyUnit;
    /**
     * 任务初始化数据
     */
    private JobDataMap jobDataMap;
    /**
     * 任务开始时间
     */
    private LocalDateTime startTime;
}
