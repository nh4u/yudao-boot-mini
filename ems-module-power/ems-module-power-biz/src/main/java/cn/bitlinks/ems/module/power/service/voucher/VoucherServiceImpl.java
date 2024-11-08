package cn.bitlinks.ems.module.power.service.voucher;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.voucher.vo.VoucherPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.voucher.vo.VoucherSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.additionalrecording.AdditionalRecordingDO;
import cn.bitlinks.ems.module.power.dal.dataobject.voucher.VoucherDO;
import cn.bitlinks.ems.module.power.dal.mysql.additionalrecording.AdditionalRecordingMapper;
import cn.bitlinks.ems.module.power.dal.mysql.voucher.VoucherMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;
//思路1
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

/**
 * 凭证管理 Service 实现类
 *
 * @author 张亦涵
 */
@Service
@Validated
public class VoucherServiceImpl implements VoucherService {

    @Resource
    private VoucherMapper voucherMapper;
    @Resource
    private AdditionalRecordingMapper additionalRecordingMapper;


    @Override
    public VoucherDO createVoucher(VoucherSaveReqVO createReqVO) {
        // 转换请求对象到数据对象
        VoucherDO voucher = BeanUtils.toBean(createReqVO, VoucherDO.class);

        // 生成凭证编号前缀部分并设置到 voucher 中
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String codePrefix = "PZ" + today;

        // 获取当天最大流水号并生成新流水号
        Integer maxSerial = voucherMapper.selectMaxSerialByCodePrefix(codePrefix);
        int newSerial = (maxSerial == null) ? 1 : maxSerial + 1;
        String voucherCode = codePrefix + String.format("%04d", newSerial);

        voucher.setCode(voucherCode);

        // 插入数据库
        voucherMapper.insert(voucher);

        // 返回记录 ID
        return voucher;
    }



    @Override
    public void updateVoucher(VoucherSaveReqVO updateReqVO) {
        Long voucherId = updateReqVO.getId();
        // Step 1: 校验凭证是否存在
        validateVoucherExists(voucherId);

        // Step 2: 获取现有凭证记录并检查“用量”是否更改
        VoucherDO existingVoucher = voucherMapper.selectById(voucherId);

        // Step 3: 判断“用量”是否发生更改
        if (!existingVoucher.getUsage().equals(updateReqVO.getUsage())) {
            // Step 4: 如果“用量”已修改，检查 `additional_recording` 表中是否使用该凭证记录
//            boolean isUsedInAdditionalRecording = additionalRecordingMapper.existsByVoucherId(voucherId);
            Long l = additionalRecordingMapper.selectCount(new LambdaQueryWrapperX<AdditionalRecordingDO>()
                    .eq(AdditionalRecordingDO::getVoucherId, voucherId)
                    .eq(AdditionalRecordingDO::getDeleted, 0));
            if ( l > 0) {
                throw exception(VOUCHER_USAGE_MODIFIED_ERROR);
            }
        }

        // Step 5: 更新凭证记录
        VoucherDO updateObj = BeanUtils.toBean(updateReqVO, VoucherDO.class);
        voucherMapper.updateById(updateObj);
    }


    @Override
    public void deleteVoucher(Long id) {
        // 校验存在
        validateVoucherExists(id);
        // 删除
        voucherMapper.deleteById(id);
    }

    private void validateVoucherExists(Long id) {
        if (voucherMapper.selectById(id) == null) {
            throw exception(VOUCHER_NOT_EXISTS);
        }
    }

    @Override
    public VoucherDO getVoucher(Long id) {
        return voucherMapper.selectById(id);
    }

    @Override
    public PageResult<VoucherDO> getVoucherPage(VoucherPageReqVO pageReqVO) {
        return voucherMapper.selectPage(pageReqVO);
    }


    @Override
    public void deleteVouchers(VoucherSaveReqVO deleteVO) {
        // 1.检查 ids 列表是否为空
        List<Long> ids = deleteVO.getIds();
        if (ids == null || ids.isEmpty()) {
            throw exception(VOUCHER_LIST_IS_EMPTY);
        }
        // 2.批量删除
        voucherMapper.deleteByIds(ids);
    }


}