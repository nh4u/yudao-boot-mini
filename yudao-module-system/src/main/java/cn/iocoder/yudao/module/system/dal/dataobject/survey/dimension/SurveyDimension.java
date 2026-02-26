package cn.iocoder.yudao.module.system.dal.dataobject.survey.dimension;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 维度
 */
@Data
@TableName("survey_dimension")
public class SurveyDimension {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;        // 维度名称：如“专注力”
    private String description; // 维度描述
    private LocalDateTime createdAt;
}