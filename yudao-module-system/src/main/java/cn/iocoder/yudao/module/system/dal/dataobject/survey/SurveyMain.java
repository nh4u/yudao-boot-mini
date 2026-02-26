package cn.iocoder.yudao.module.system.dal.dataobject.survey;
import cn.iocoder.yudao.module.system.dal.dataobject.survey.question.SurveyQuestion;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("survey_main")
public class SurveyMain {
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private String title;      // 问卷标题：如“期末综合素质评估”
    private Integer isActive;  // 是否启用：1启用，0禁用
    private LocalDateTime createdAt;
    
    // 非数据库字段：组装问卷时包含的题目列表
    @TableField(exist = false)
    private List<SurveyQuestion> questions;
}