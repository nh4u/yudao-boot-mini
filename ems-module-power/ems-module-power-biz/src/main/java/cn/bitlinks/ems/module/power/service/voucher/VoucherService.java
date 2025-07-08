package cn.bitlinks.ems.module.power.service.voucher;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.voucher.vo.VoucherPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.voucher.vo.VoucherSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.voucher.VoucherDO;
import cn.hutool.json.JSONObject;

import javax.validation.Valid;
import java.util.List;

/**
 * 凭证管理 Service 接口
 *
 * @author 张亦涵
 */
public interface VoucherService {

    /**
     * 创建凭证管理
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    VoucherDO createVoucher(@Valid VoucherSaveReqVO createReqVO);

    /**
     * 更新凭证管理
     *
     * @param updateReqVO 更新信息
     */
    void updateVoucher(@Valid VoucherSaveReqVO updateReqVO);

    /**
     * 删除凭证管理
     *
     * @param id 编号
     */
    void deleteVoucher(Long id);

    /**
     * 获得凭证管理
     *
     * @param id 编号
     * @return 凭证管理
     */
    VoucherDO getVoucher(Long id);

    /**
     * 获得凭证管理分页
     *
     * @param pageReqVO 分页查询
     * @return 凭证管理分页
     */
    PageResult<VoucherDO> getVoucherPage(VoucherPageReqVO pageReqVO);

    /**
     * 批量删除凭证管理
     *
     * @param ids 删除实体
     */
    void deleteVouchers(List<Long> ids);


    String recognition(String url);
}