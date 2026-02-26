package cn.iocoder.yudao.module.system.dal.dataobject.survey.question;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("survey_question")
public class SurveyQuestion {
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private String content;     // 题目内容：如“该生能按时完成作业”
    private LocalDateTime createdAt;
    
    // 非数据库字段：用于在管理后台展示该题目关联的维度信息
    @TableField(exist = false)
    private List<SurveyQuestionDimension> dimensionWeights;
}