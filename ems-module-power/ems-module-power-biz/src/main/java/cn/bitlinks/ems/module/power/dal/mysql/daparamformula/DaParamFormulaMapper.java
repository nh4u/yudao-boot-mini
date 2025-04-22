package cn.bitlinks.ems.module.power.dal.mysql.daparamformula;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.daparamformula.vo.DaParamFormulaPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.daparamformula.vo.DaParamFormulaSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.daparamformula.DaParamFormulaDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Objects;

/**
 * 数据来源为关联计量器具时的参数公式 Mapper
 *
 * @author Mingdy
 */
@Mapper
public interface DaParamFormulaMapper extends BaseMapperX<DaParamFormulaDO> {

    default PageResult<DaParamFormulaDO> selectPage(DaParamFormulaPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DaParamFormulaDO>()
                .eqIfPresent(DaParamFormulaDO::getEnergyId, reqVO.getEnergyId())
                .eqIfPresent(DaParamFormulaDO::getEnergyFormula, reqVO.getEnergyFormula())
                .eqIfPresent(DaParamFormulaDO::getFormulaType, reqVO.getFormulaType())
                .betweenIfPresent(DaParamFormulaDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(DaParamFormulaDO::getId));
    }

    default DaParamFormulaDO getLatestOne(DaParamFormulaDO daParamFormulaDO) {
        return selectOne(new LambdaQueryWrapperX<DaParamFormulaDO>()
                .eqIfPresent(DaParamFormulaDO::getEnergyId, daParamFormulaDO.getEnergyId())
                .eqIfPresent(DaParamFormulaDO::getFormulaType, daParamFormulaDO.getFormulaType())
                .orderByDesc(DaParamFormulaDO::getStartEffectiveTime)
                .last("limit 1"));
    }

    /**
     * 获取对应能源下指定类型的公式
     *
     * @param reqVO
     * @return
     */
    default List<DaParamFormulaDO> getDaParamFormulaList(DaParamFormulaSaveReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<DaParamFormulaDO>()
                .eqIfPresent(DaParamFormulaDO::getEnergyId, reqVO.getEnergyId())
                .eqIfPresent(DaParamFormulaDO::getFormulaType, reqVO.getFormulaType())
                .eqIfPresent(DaParamFormulaDO::getFormulaStatus, reqVO.getFormulaStatus())
                .orderByAsc(DaParamFormulaDO::getFormulaStatus).orderByDesc(DaParamFormulaDO::getCreateTime));
    }
}