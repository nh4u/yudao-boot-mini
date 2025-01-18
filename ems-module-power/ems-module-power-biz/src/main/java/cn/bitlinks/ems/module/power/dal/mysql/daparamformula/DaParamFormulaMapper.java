package cn.bitlinks.ems.module.power.dal.mysql.daparamformula;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.daparamformula.vo.DaParamFormulaPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.daparamformula.DaParamFormulaDO;
import org.apache.ibatis.annotations.Mapper;

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
                .eqIfPresent(DaParamFormulaDO::getEnergyParam, reqVO.getEnergyParam())
                .betweenIfPresent(DaParamFormulaDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(DaParamFormulaDO::getId));
    }

}