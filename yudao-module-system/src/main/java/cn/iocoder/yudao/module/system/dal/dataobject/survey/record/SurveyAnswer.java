package cn.iocoder.yudao.module.system.dal.dataobject.survey.record;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("survey_answer")
public class SurveyAnswer {
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private Integer recordId;   // 关联的评价记录ID
    private Integer questionId; // 题目ID
    private Integer score;      // 老师打的分数 (1-10)
}