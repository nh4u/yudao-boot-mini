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

    List<DaParamFormulaDO> getDaParamFormulaList(DaParamFormulaSaveReqVO pageReqVO);
}