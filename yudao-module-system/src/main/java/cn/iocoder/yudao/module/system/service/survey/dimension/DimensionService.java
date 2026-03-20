package cn.iocoder.yudao.module.system.service.survey.dimension;

import cn.iocoder.yudao.module.system.dal.dataobject.survey.dimension.SurveyDimension;

import java.util.List;

public interface DimensionService {
    boolean addDimension(SurveyDimension dimension);

    boolean updateDimension(SurveyDimension dimension);

    boolean deleteDimension(Integer id);

    List<SurveyDimension> getAllDimensions();
}
