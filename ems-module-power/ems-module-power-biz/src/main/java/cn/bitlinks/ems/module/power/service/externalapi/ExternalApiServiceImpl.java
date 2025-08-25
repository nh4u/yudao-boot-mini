package cn.bitlinks.ems.module.power.service.externalapi;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.externalapi.vo.ExternalApiPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.externalapi.vo.ExternalApiSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.externalapi.ExternalApiDO;
import cn.bitlinks.ems.module.power.dal.mysql.externalapi.ExternalApiMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.EXTERNAL_API_CODE_REPEAT;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.EXTERNAL_API_NOT_EXISTS;


/**
 * @author liumingqiang
 */
@Service
@Validated
public class ExternalApiServiceImpl implements ExternalApiService {

    @Resource
    private ExternalApiMapper externalApiMapper;

    @Override
    public ExternalApiDO createExternalApi(ExternalApiSaveReqVO createReqVO) {


        // 接口编码不能重复 校验
        Long count = externalApiMapper.selectCount(new LambdaQueryWrapper<ExternalApiDO>()
                .eq(ExternalApiDO::getCode, createReqVO.getCode()));

        if (count > 0) {
            throw exception(EXTERNAL_API_CODE_REPEAT);
        }

        // 转换请求对象到数据对象
        ExternalApiDO externalApi = BeanUtils.toBean(createReqVO, ExternalApiDO.class);
        // 插入数据库
        externalApiMapper.insert(externalApi);

        // 返回记录 ID
        return externalApi;
    }

    @Override
    public void updateExternalApi(ExternalApiSaveReqVO updateReqVO) {
        Long id = updateReqVO.getId();
        // Step 1: 校验凭证是否存在
        validateExternalApiExists(id);

        // 接口编码不能重复 校验
        Long count = externalApiMapper.selectCount(new LambdaQueryWrapper<ExternalApiDO>()
                .eq(ExternalApiDO::getCode, updateReqVO.getCode())
                .ne(ExternalApiDO::getId, id));

        if (count > 0) {
            throw exception(EXTERNAL_API_CODE_REPEAT);
        }

        // Step 5: 更新凭证记录
        ExternalApiDO updateObj = BeanUtils.toBean(updateReqVO, ExternalApiDO.class);
        externalApiMapper.updateById(updateObj);
    }

    @Override
    public void deleteExternalApi(Long id) {
        // 删除
        externalApiMapper.deleteById(id);
    }

    @Override
    public ExternalApiDO getExternalApi(Long id) {
        return externalApiMapper.selectById(id);
    }

    @Override
    public PageResult<ExternalApiDO> getExternalApiPage(ExternalApiPageReqVO pageReqVO) {
        return externalApiMapper.selectPage(pageReqVO);
    }

    @Override
    public Object testExternalApi(ExternalApiSaveReqVO createReqVO) {


        // TODO: 2025/8/22 发送http请求  获取返回数据

        // 1.当是 post时

        // 2.当是 get时


        return null;
    }

    private void validateExternalApiExists(Long id) {
        if (externalApiMapper.selectById(id) == null) {
            throw exception(EXTERNAL_API_NOT_EXISTS);
        }
    }
}