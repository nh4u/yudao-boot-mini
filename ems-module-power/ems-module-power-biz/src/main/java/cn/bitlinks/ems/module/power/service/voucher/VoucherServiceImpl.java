package cn.bitlinks.ems.module.power.service.voucher;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.voucher.vo.VoucherPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.voucher.vo.VoucherSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.voucher.VoucherDO;
import cn.bitlinks.ems.module.power.dal.mysql.voucher.VoucherMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.VOUCHER_LIST_IS_EMPTY;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.VOUCHER_NOT_EXISTS;

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

    @Override
    public Long createVoucher(VoucherSaveReqVO createReqVO) {
        // 插入
        VoucherDO voucher = BeanUtils.toBean(createReqVO, VoucherDO.class);
        voucherMapper.insert(voucher);
        // 返回
        return voucher.getId();
    }

    @Override
    public void updateVoucher(VoucherSaveReqVO updateReqVO) {
        // 校验存在
        validateVoucherExists(updateReqVO.getId());
        // 更新
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


//    @Override
//    public void deleteVouchers(List<Long> ids) {
//        boolean empty = CollUtil.isEmpty(ids);
//        if (empty){
//            throw exception(VOUCHER_BATCH_NOT_EXISTS);
//        }
//        // 校验每个凭证是否存在
//        validateVouchersExist(ids);
//        // 批量删除
//        voucherMapper.deleteByIds(ids);
//    }

//    @Override
//    public void deleteVouchers(List<Long> ids) {
//        // 校验每个凭证是否存在
//        validateVouchersExist(ids);
//        // 批量删除
//        voucherMapper.deleteByIds(ids);
//    }

//    private void validateVouchersExist(List<Long> ids) {
//        // 查询数据库中存在的 ID 列表
//        List<Long> existingIds = voucherMapper.selectByIds(ids);
//
//        // 判断是否有不存在的 ID
//        if (existingIds.size() != ids.size()) {
//            // 找出不存在的 ID
//            ids.removeAll(existingIds);
//            throw exception(VOUCHER_BATCH_NOT_EXISTS, "以下凭证不存在: " + ids);
//        }
//    }

//    private void validateVouchersExist(List<Long> ids) {
//        for (Long id : ids) {
//            if (voucherMapper.selectById(id) == null) {
//                throw exception(VOUCHER_NOT_EXISTS, "以下凭证不存在: " + id);
//            }
//        }

//    private void validateVouchersExist(List<Long> ids) {
//        // 查询所有存在的凭证 ID
//        List<Long> existingIds = voucherMapper.selectBatchIds(ids);
//        // 检查是否有不存在的 ID
//        for (Long id : ids) {
//            if (!existingIds.contains(id)) {
//                throw exception(VOUCHER_BATCH_NOT_EXISTS);  // 抛出异常，说明某个 ID 不存在
//            }
//        }


}