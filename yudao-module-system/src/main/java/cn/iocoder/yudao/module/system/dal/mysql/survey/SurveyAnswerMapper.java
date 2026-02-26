package cn.iocoder.yudao.module.system.dal.mysql.survey;

import cn.iocoder.yudao.module.system.dal.dataobject.survey.record.SurveyAnswer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SurveyAnswerMapper extends BaseMapper<SurveyAnswer> {
}