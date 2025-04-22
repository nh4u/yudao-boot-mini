package cn.bitlinks.ems.module.power.service.daparamformula;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.daparamformula.vo.DaParamFormulaPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.daparamformula.vo.DaParamFormulaSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.daparamformula.DaParamFormulaDO;
import cn.bitlinks.ems.module.power.dal.mysql.daparamformula.DaParamFormulaMapper;
import cn.bitlinks.ems.module.system.api.user.AdminUserApi;
import cn.bitlinks.ems.module.system.api.user.dto.AdminUserRespDTO;
import com.alibaba.nacos.common.utils.CollectionUtils;
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
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public Long createDaParamFormula(DaParamFormulaSaveReqVO createReqVO) {
        // 插入
        DaParamFormulaDO daParamFormula = BeanUtils.toBean(createReqVO, DaParamFormulaDO.class);

        // TODO: 2025/4/18  点击“加入公式列表”时若判断为重复则系统弹出提示“已存在相同的公式，不可重复添加”
        
        // 设置未使用
        daParamFormula.setFormulaStatus(0);
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

        // 设置创建人昵称
        PageResult<DaParamFormulaDO> pageResult = daParamFormulaMapper.selectPage(pageReqVO);
        // 遍历结果集，设置 unitPrice 和 creator 昵称
        if (pageResult != null && CollectionUtils.isNotEmpty(pageResult.getList())) {
            for (DaParamFormulaDO daParamFormulaDO : pageResult.getList()) {
                // 设置创建人昵称
                if (daParamFormulaDO.getCreator() != null) {
                    CommonResult<AdminUserRespDTO> user = adminUserApi.getUser(Long.valueOf(daParamFormulaDO.getCreator()));
                    if (user.getData() != null) {
                        daParamFormulaDO.setCreator(user.getData().getNickname());
                    }
                }
            }
        }
        return pageResult;
    }

    @Override
    public List<DaParamFormulaDO> getDaParamFormulaList(DaParamFormulaSaveReqVO reqVO) {

        return daParamFormulaMapper.getDaParamFormulaList(reqVO);
    }

}