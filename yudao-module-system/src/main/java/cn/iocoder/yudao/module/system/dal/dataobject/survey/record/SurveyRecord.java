package cn.iocoder.yudao.module.system.dal.dataobject.survey.record;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("survey_record")
public class SurveyRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private Integer surveyId;   // 使用了哪套评价标准
    private Integer teacherId;  // 评价人
    private Integer studentId;  // 被评人
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fillDate; // 老师填写的日期（成长趋势的关键轴）
    
    private String remark;      // 老师的文字评价
    private LocalDateTime createdAt;
}