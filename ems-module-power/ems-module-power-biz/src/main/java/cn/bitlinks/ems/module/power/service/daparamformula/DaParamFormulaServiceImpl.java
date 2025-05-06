package cn.bitlinks.ems.module.power.service.daparamformula;

import cn.bitlinks.ems.framework.common.exception.ErrorCode;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.daparamformula.vo.DaParamFormulaPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.daparamformula.vo.DaParamFormulaSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.daparamformula.DaParamFormulaDO;
import cn.bitlinks.ems.module.power.dal.mysql.daparamformula.DaParamFormulaMapper;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

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

        isDuplicate(daParamFormula.getId(),
                daParamFormula.getEnergyId(),
                daParamFormula.getFormulaType(),
                daParamFormula.getEnergyFormula());

        // 设置未使用
        daParamFormula.setFormulaStatus(0);
        daParamFormulaMapper.insert(daParamFormula);
        // 返回
        return daParamFormula.getId();
    }

    /**
     * 目前公式是没有更新操作的
     *
     * @param updateReqVO 更新信息
     */
    @Override
    public void updateDaParamFormula(DaParamFormulaSaveReqVO updateReqVO) {
        // 校验存在
        DaParamFormulaDO daParamFormulaDO = validateDaParamFormulaExists(updateReqVO.getId());
        // 根据数据库中公式状态判断是否能更新 （已使用、使用中是不允许修改的）
        if (daParamFormulaDO.getFormulaStatus() != 0) {
            throw exception(FORMULA_HAVE_BIND_UPDATE);
        }
        // 校验是否重复
        isDuplicate(updateReqVO.getId(),
                updateReqVO.getEnergyId(),
                updateReqVO.getFormulaType(),
                updateReqVO.getEnergyFormula());

        // 更新
        DaParamFormulaDO updateObj = BeanUtils.toBean(updateReqVO, DaParamFormulaDO.class);
        daParamFormulaMapper.updateById(updateObj);
    }

    @Override
    public void deleteDaParamFormula(Long id) {
        // 校验存在
        validateDaParamFormulaExists(id);
        validateFormulaBind(Collections.singletonList(id));
        // 删除
        daParamFormulaMapper.deleteById(id);
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
    @Transactional(rollbackFor = Exception.class)
    public Boolean change(List<DaParamFormulaSaveReqVO> formulas) {
        if (CollectionUtil.isEmpty(formulas)) {
            throw exception(FORMULA_LIST_NOT_EXISTS);
        }

        // 获取能源id和公式类型
        Long energyId = formulas.get(0).getEnergyId();
        Integer formulaType = formulas.get(0).getFormulaType();

        // 获取现有公式id
        List<Long> formulaIds = daParamFormulaMapper.getFormulaIdList(energyId, formulaType);

        // 对传来的list进行分类处理
        for (DaParamFormulaSaveReqVO formula : formulas) {
            Long id = formula.getId();
            if (Objects.isNull(id)) {
                //新增  名称判重
                createDaParamFormula(formula);
            } else {
                // 修改  名称判重
                formulaIds.remove(id);
                // 目前没有更新操作
//                updateDaParamFormula(formula);
            }
        }

        // 删除操作
        if (CollectionUtil.isNotEmpty(formulaIds)) {
            deleteDaParamFormulas(formulaIds);
        }

        return true;
    }

    /**
     * 判断公式是否重复
     *
     * @param id            公式id
     * @param energyId      能源id
     * @param formulaType   公式类型 1折标煤公式，2用能成本公式
     * @param energyFormula 公式
     * @return
     */
    @Override
    public Boolean isDuplicate(Long id, Long energyId, Integer formulaType, String energyFormula) {

        // 校验  type 是否传入
        if (Objects.isNull(formulaType)) {
            throw exception(FORMULA_TYPE_NOT_EXISTS);
        }
        // 检验公式是否传入
        if (StrUtil.isEmpty(energyFormula)) {
            throw exception(FORMULA_NOT_EXISTS);
        }
        // 检验能源id是否传入
        if (Objects.isNull(energyId)) {
            throw exception(ENERGY_ID_NOT_EXISTS);
        }

        List<DaParamFormulaDO> list = daParamFormulaMapper.getDuplicateFormulas(id, energyId, formulaType, energyFormula);
        if (CollUtil.isEmpty(list)) {
            throw exception(FORMULA_HAVE_EXISTS);
        } else {
            return true;
        }
    }

    /**
     * 判断公式是否已经使用绑定
     *
     * @param id
     * @return
     */
    @Override
    public Boolean isDelete(Long id) {

        // 根据id获取对应的公式状态。
        if (Objects.isNull(id)) {
            throw exception(FORMULA_ID_NOT_EXISTS);
        }
        DaParamFormulaDO formula = daParamFormulaMapper.selectById(id);

        // 判断公式的状态
        Integer formulaStatus = formula.getFormulaStatus();
        if (formulaStatus.equals(0)) {
            // 未使用，则可以删除
            return true;
        } else {
            throw exception(FORMULA_HAVE_BIND_DELETE);
        }

    }

    @Override
    public List<DaParamFormulaDO> getDaParamFormulaList(DaParamFormulaSaveReqVO reqVO) {

        return daParamFormulaMapper.getDaParamFormulaList(reqVO);
    }

    private void deleteDaParamFormulas(List<Long> formulaIds) {

        // 校验是否已经绑定
        validateFormulaBind(formulaIds);
        // 删除
        daParamFormulaMapper.deleteByIds(formulaIds);
    }

    private DaParamFormulaDO validateDaParamFormulaExists(Long id) {
        DaParamFormulaDO daParamFormulaDO = daParamFormulaMapper.selectById(id);
        if (daParamFormulaDO == null) {
            throw exception(FORMULA_NOT_EXISTS);
        } else {
            return daParamFormulaDO;
        }
    }

    private void validateFormulaBind(List<Long> formulaIds) {
        StringBuilder strBuilder = new StringBuilder();
        // 校验是否已经绑定
        List<DaParamFormulaDO> list = daParamFormulaMapper.selectBatchIds(formulaIds);
        // 删除时需要校验是否已经绑定了能源  如果绑定了则不能删除，
        for (DaParamFormulaDO formula : list) {
            if (formula.getFormulaStatus() != 0) {
                strBuilder.append(formula.getFormulaScale()).append(StrPool.COMMA);
            }
        }
        dealMessage(strBuilder);
    }

    private void dealMessage(StringBuilder strBuilder) {
        // 如何有报错信息
        if (strBuilder.length() > 0) {
            // 删除多余，号
            strBuilder.deleteCharAt(strBuilder.length() - 1);

            //组装一下
            strBuilder.insert(0, "【");
            strBuilder.append("】已绑定");
            ErrorCode errorCode = new ErrorCode(1_001_801_100, strBuilder.toString());
            throw exception(errorCode);
        }
    }
}