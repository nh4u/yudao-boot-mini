package cn.iocoder.yudao.module.system.dal.mysql.survey;
import cn.iocoder.yudao.module.system.dal.dataobject.survey.record.SurveyRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SurveyRecordMapper extends BaseMapper<SurveyRecord> {

    /**
     * 查询某个学生在特定维度下的得分明细（用于统计趋势）
     * 计算逻辑：SUM(得分 * 维度系数) / SUM(10 * 维度系数) * 100
     */
    @Select("SELECT " +
            "  r.fill_date AS fillDate, " +
            "  (SUM(a.score * qd.weight) / SUM(10 * qd.weight)) * 100 AS standardizedScore " +
            "FROM survey_record r " +
            "JOIN survey_answer a ON r.id = a.record_id " +
            "JOIN survey_question_dimension qd ON a.question_id = qd.question_id " +
            "WHERE r.student_id = #{studentId} " +
            "  AND qd.dimension_id = #{dimensionId} " +
            "GROUP BY r.fill_date " +
            "ORDER BY r.fill_date ASC")
    List<Map<String, Object>> getStudentDimensionTrend(@Param("studentId") Integer studentId,
                                                       @Param("dimensionId") Integer dimensionId);
//
//    /**
//     * 查询全班/全校的常模（平均分）趋势
//     */
//    @Select("SELECT " +
//            "  r.fill_date AS fillDate, " +
//            "  (SUM(a.score * qd.weight) / SUM(10 * qd.weight)) * 100 AS averageScore " +
//            "FROM survey_record r " +
//            "JOIN survey_answer a ON r.id = a.record_id " +
//            "JOIN survey_question_dimension qd ON a.question_id = qd.question_id " +
//            "WHERE qd.dimension_id = #{dimensionId} " +
//            "GROUP BY r.fill_date")
//    List<Map<String, Object>> getGlobalDimensionTrend(@Param("dimensionId") Integer dimensionId);
}