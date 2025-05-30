package cn.bitlinks.ems.module.power.dal.mysql.daparamformula;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.daparamformula.vo.DaParamFormulaPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.daparamformula.vo.DaParamFormulaSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.daparamformula.DaParamFormulaDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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
    List<DaParamFormulaDO> getDaParamFormulaList(@Param("vo") DaParamFormulaSaveReqVO reqVO);

    /**
     * 根据公式id获取对应公式
     * @param formulaIds ids
     * @return
     */
    List<DaParamFormulaDO> getFormulaListByIds(@Param("ids") List<Long> formulaIds);

    /**
     * 根据能源id和公式类别来获取所有公式
     *
     * @param energyId    能源id
     * @param formulaType 公式类别
     * @return
     */

    default List<Long> getFormulaIdList(Long energyId, Integer formulaType) {
        return selectObjs(new LambdaQueryWrapper<DaParamFormulaDO>()
                .select(DaParamFormulaDO::getId)
                .eq(DaParamFormulaDO::getEnergyId, energyId)
                .eq(DaParamFormulaDO::getFormulaType, formulaType));
    }

    /**
     * 获取重复的公式
     *
     * @param id            公式id
     * @param energyId      能源id
     * @param formulaType   公式类型 1折标煤公式，2用能成本公式
     * @param energyFormula 公式
     * @return
     */
    default List<DaParamFormulaDO> getDuplicateFormulas(Long id, Long energyId, Integer formulaType, String energyFormula) {
        return selectList(new LambdaQueryWrapperX<DaParamFormulaDO>()
                .neIfPresent(DaParamFormulaDO::getId, id)
                .eqIfPresent(DaParamFormulaDO::getEnergyId, energyId)
                .eqIfPresent(DaParamFormulaDO::getFormulaType, formulaType)
                .eqIfPresent(DaParamFormulaDO::getEnergyFormula, energyFormula));
    }


}