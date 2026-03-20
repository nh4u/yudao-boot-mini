package cn.iocoder.yudao.module.system.service.survey.dimension;

import cn.iocoder.yudao.module.system.dal.dataobject.survey.dimension.SurveyDimension;
import cn.iocoder.yudao.module.system.dal.mysql.survey.SurveyDimensionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DimensionServiceImpl implements DimensionService {
    // 基础 CRUD 由 MyBatis-Plus 自动实现
    @Autowired
    private SurveyDimensionMapper dimensionMapper;

    @Override
    public boolean addDimension(SurveyDimension dimension) {
        // 调用 mapper 的 insert
        return dimensionMapper.insert(dimension) > 0;
    }

    @Override
    public boolean updateDimension(SurveyDimension dimension) {
        // 调用 mapper 的 updateById
        return dimensionMapper.updateById(dimension) > 0;
    }

    @Override
    public boolean deleteDimension(Integer id) {
        // 这里可以增加业务判断：如果维度已被题目关联，是否允许删除
        return dimensionMapper.deleteById(id) > 0;
    }

    @Override
    public List<SurveyDimension> getAllDimensions() {
        // 调用 mapper 的 selectList (null 表示无筛选条件)
        return dimensionMapper.selectList(null);
    }
}