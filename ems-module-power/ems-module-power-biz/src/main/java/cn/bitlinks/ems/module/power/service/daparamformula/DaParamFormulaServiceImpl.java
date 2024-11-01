package cn.bitlinks.ems.module.power.service.daparamformula;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.daparamformula.vo.DaParamFormulaPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.daparamformula.vo.DaParamFormulaSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.daparamformula.DaParamFormulaDO;
import cn.bitlinks.ems.module.power.dal.mysql.daparamformula.DaParamFormulaMapper;
import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

import java.util.List;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.DA_PARAM_FORMULA_NOT_EXISTS;

/**
 * 数据来源为关联计量器具时的参数公式 Service 实现类
 *
 * @author Mingdy
 */
@Service
@Validated
public class DaParamFormulaServiceImpl implements DaParamFormulaService {

    @Resource
    private DaParamFormulaMapper daParamFormulaMapper;

    @Override
    public Long createDaParamFormula(DaParamFormulaSaveReqVO createReqVO) {
        // 插入
        DaParamFormulaDO daParamFormula = BeanUtils.toBean(createReqVO, DaParamFormulaDO.class);
        daParamFormulaMapper.insert(daParamFormula);
        // 返回
        return daParamFormula.getId();
    }

    @Override
    public void updateDaParamFormula(DaParamFormulaSaveReqVO updateReqVO) {
        // 校验存在
        validateDaParamFormulaExists(updateReqVO.getId());
        // 更新
        DaParamFormulaDO updateObj = BeanUtils.toBean(updateReqVO, DaParamFormulaDO.class);
        daParamFormulaMapper.updateById(updateObj);
    }

    @Override
    public void deleteDaParamFormula(Long id) {
        // 校验存在
        validateDaParamFormulaExists(id);
        // 删除
        daParamFormulaMapper.deleteById(id);
    }

    private void validateDaParamFormulaExists(Long id) {
        if (daParamFormulaMapper.selectById(id) == null) {
            throw exception(DA_PARAM_FORMULA_NOT_EXISTS);
        }
    }

    @Override
    public DaParamFormulaDO getDaParamFormula(Long id) {
        return daParamFormulaMapper.selectById(id);
    }

    @Override
    public PageResult<DaParamFormulaDO> getDaParamFormulaPage(DaParamFormulaPageReqVO pageReqVO) {
        return daParamFormulaMapper.selectPage(pageReqVO);
    }

    @Override
    public Boolean batchDealDaParamFormula(DaParamFormulaSaveReqVO createReqVO) {
        List<DaParamFormulaSaveReqVO> daParamFormulaList = createReqVO.getDaParamFormulaList();
        if (CollUtil.isEmpty(daParamFormulaList)) {
            throw exception(DA_PARAM_FORMULA_NOT_EXISTS);
        }

        for (DaParamFormulaSaveReqVO vo : daParamFormulaList) {
            if (vo.getId() == null) {
                // 插入
                DaParamFormulaDO daParamFormula = BeanUtils.toBean(vo, DaParamFormulaDO.class);
                daParamFormulaMapper.insert(daParamFormula);
            } else {
                // 更新
                DaParamFormulaDO updateObj = BeanUtils.toBean(vo, DaParamFormulaDO.class);
                daParamFormulaMapper.updateById(updateObj);
            }
        }

        return true;
    }

}