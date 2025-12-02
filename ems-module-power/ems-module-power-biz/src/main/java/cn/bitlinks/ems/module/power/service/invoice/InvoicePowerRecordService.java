package cn.bitlinks.ems.module.power.service.invoice;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.invoice.vo.InvoicePowerRecordPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.invoice.vo.InvoicePowerRecordRespVO;
import cn.bitlinks.ems.module.power.controller.admin.invoice.vo.InvoicePowerRecordSaveReqVO;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface InvoicePowerRecordService {

    /**
     * 新增或修改（数据补录弹窗点“提交”）
     */
    Long saveInvoicePowerRecord(@Valid InvoicePowerRecordSaveReqVO reqVO);

    /**
     * 获取详情（编辑回显用）
     */
    InvoicePowerRecordRespVO getInvoicePowerRecord(LocalDate recordMonth);

    /**
     * 列表
     */
    List<InvoicePowerRecordRespVO> getInvoicePowerRecordList(InvoicePowerRecordPageReqVO pageReqVO);


    /**
     * 列表导出（和列表数据一致）
     */
    void exportInvoicePowerRecordExcel(HttpServletResponse response,
                                       InvoicePowerRecordPageReqVO exportReqVO) throws IOException;
}
