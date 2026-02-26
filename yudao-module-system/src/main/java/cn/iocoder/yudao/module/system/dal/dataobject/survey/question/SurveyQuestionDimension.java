package cn.iocoder.yudao.module.system.dal.dataobject.survey.question;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("survey_question_dimension")
public class SurveyQuestionDimension {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer questionId;
    private Integer dimensionId;

    // 维度系数，使用 BigDecimal 保证计算精度
    private BigDecimal weight;
}