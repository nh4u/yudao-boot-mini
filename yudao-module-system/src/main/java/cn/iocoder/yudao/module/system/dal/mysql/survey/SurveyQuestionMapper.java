package cn.iocoder.yudao.module.system.dal.mysql.survey;

import cn.iocoder.yudao.module.system.dal.dataobject.survey.question.SurveyQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface SurveyQuestionMapper extends BaseMapper<SurveyQuestion> {
}