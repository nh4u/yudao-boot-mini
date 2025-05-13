package cn.bitlinks.ems.module.power.service.daparamformula;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.daparamformula.vo.DaParamFormulaPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.daparamformula.vo.DaParamFormulaSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.daparamformula.DaParamFormulaDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 数据来源为关联计量器具时的参数公式 Service 接口
 *
 * @author Mingdy
 */
public interface DaParamFormulaService {

    /**
     * 创建数据来源为关联计量器具时的参数公式
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createDaParamFormula(@Valid DaParamFormulaSaveReqVO createReqVO);

    /**
     * 更新数据来源为关联计量器具时的参数公式
     *
     * @param updateReqVO 更新信息
     */
    void updateDaParamFormula(@Valid DaParamFormulaSaveReqVO updateReqVO);

    /**
     * 删除数据来源为关联计量器具时的参数公式
     *
     * @param id 编号
     */
    void deleteDaParamFormula(Long id);

    /**
     * 获得数据来源为关联计量器具时的参数公式
     *
     * @param id 编号
     * @return 数据来源为关联计量器具时的参数公式
     */
    DaParamFormulaDO getDaParamFormula(Long id);

    /**
     * 获得数据来源为关联计量器具时的参数公式分页
     *
     * @param pageReqVO 分页查询
     * @return 数据来源为关联计量器具时的参数公式分页
     */
    PageResult<DaParamFormulaDO> getDaParamFormulaPage(DaParamFormulaPageReqVO pageReqVO);

    List<DaParamFormulaDO> getDaParamFormulaList(DaParamFormulaSaveReqVO reqVO);

    /**
     * 公式 增删改
     *
     * @param formulas
     * @return
     */
    Boolean change(List<DaParamFormulaSaveReqVO> formulas);

    /**
     * 判断公式是否重复
     *
     * @param id          公式id
     * @param energyId    能源id
     * @param formulaType 公式类型 1折标煤公式，2用能成本公式
     * @param energyFormula 公式
     * @return
     */
    Boolean isDuplicate(Long id, Long energyId, Integer formulaType,String energyFormula);

    /**
     * 判断公式是否已经使用绑定
     *
     * @param id
     * @return
     */
    Boolean isDelete(Long id);
}