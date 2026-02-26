package cn.iocoder.yudao.module.system.dal.dataobject.survey;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("survey_item")
public class SurveyItem {
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private Integer surveyId;
    private Integer questionId;
    private Integer sortOrder; // 题目在问卷中的排序
}