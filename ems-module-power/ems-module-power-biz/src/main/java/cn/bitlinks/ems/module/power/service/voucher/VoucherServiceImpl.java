package cn.bitlinks.ems.module.power.service.voucher;

import cn.bitlinks.ems.framework.common.exception.ErrorCode;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.voucher.vo.VoucherPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.voucher.vo.VoucherSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.additionalrecording.AdditionalRecordingDO;
import cn.bitlinks.ems.module.power.dal.dataobject.voucher.VoucherDO;
import cn.bitlinks.ems.module.power.dal.mysql.additionalrecording.AdditionalRecordingMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.type.StandingbookTypeMapper;
import cn.bitlinks.ems.module.power.dal.mysql.voucher.VoucherMapper;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrPool;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    @Resource
    private StandingbookTypeMapper standingbookTypeMapper;

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
            if (l > 0) {
                throw exception(VOUCHER_USAGE_MODIFIED_ERROR);
            }

            List<Long> standingbookIds = additionalRecordingMapper.selectStandingbookIdsByVoucherId(voucherId);


            StringBuilder strBuilder = new StringBuilder();
            strBuilder.append("请先删除关联的补录数据（");
            for (Long standingbookId : standingbookIds) {

                String name = standingbookTypeMapper.selectAttributeValueByCode(
                        standingbookId, "measuringInstrumentName");

                String code = standingbookTypeMapper.selectAttributeValueByCode(
                        standingbookId, "measuringInstrumentId");

                strBuilder.append(name).append("+").append(code).append(StrPool.COMMA);
            }

            // 删除多余，号
            if (strBuilder.length() > 0) {
                strBuilder.deleteCharAt(strBuilder.length() - 1);
            }
            // 添加最后一句话
            strBuilder.append("）");
            ErrorCode errorCode = new ErrorCode(1_001_501_006, strBuilder.toString());

        }

        // Step 5: 更新凭证记录
        VoucherDO updateObj = BeanUtils.toBean(updateReqVO, VoucherDO.class);
        voucherMapper.updateById(updateObj);
    }


    @Override
    public void deleteVoucher(Long id) {
        // 校验存在
        validateVoucherExists(id);
        // 是否绑定补录数据
        validateVoucherBind(id);
        // 删除
        voucherMapper.deleteById(id);
    }

    private void validateVoucherExists(Long id) {
        if (voucherMapper.selectById(id) == null) {
            throw exception(VOUCHER_NOT_EXISTS);
        }
    }

    private void validateVoucherBind(Long id) {
        Integer count = additionalRecordingMapper.countByVoucherId(id);
        if (count > 0) {
            throw exception(VOUCHER_HAS_ADDITIONAL_RECORDING);
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
    public void deleteVouchers(List<Long> ids) {
        // 校验存在
        for (Long id : ids) {
            validateVoucherExists(id);
        }
        // 2.校验关联补录数据[新增]
        validateVoucherNotLinked(ids);
        // 3.批量删除
        voucherMapper.deleteByIds(ids);
    }

    private void validateVoucherNotLinked(List<Long> voucherIds) {
        List<String> codes = additionalRecordingMapper.countByVoucherIds(voucherIds);
        if (CollUtil.isNotEmpty(codes)) {

            // 拼接message
            StringBuilder strBuilder = new StringBuilder();

            for (String code : codes) {
                strBuilder.append(code).append(StrPool.COMMA);
            }

            // 删除多余，号
            if (strBuilder.length() > 0) {
                strBuilder.deleteCharAt(strBuilder.length() - 1);
            }
            // 添加最后一句话
            strBuilder.append("已关联补录数据，不可进行删除！");
            ErrorCode errorCode = new ErrorCode(1_001_501_005, strBuilder.toString());
            throw exception(errorCode);
        }
    }

}