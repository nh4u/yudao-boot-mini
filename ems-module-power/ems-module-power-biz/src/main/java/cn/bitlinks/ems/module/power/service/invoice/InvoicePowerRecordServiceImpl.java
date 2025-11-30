package cn.bitlinks.ems.module.power.service.invoice;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.invoice.vo.InvoicePowerRecordItemRespVO;
import cn.bitlinks.ems.module.power.controller.admin.invoice.vo.InvoicePowerRecordPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.invoice.vo.InvoicePowerRecordRespVO;
import cn.bitlinks.ems.module.power.controller.admin.invoice.vo.InvoicePowerRecordSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.invoice.InvoicePowerRecordDO;
import cn.bitlinks.ems.module.power.dal.dataobject.invoice.InvoicePowerRecordItemDO;
import cn.bitlinks.ems.module.power.dal.mysql.invoice.InvoicePowerRecordItemMapper;
import cn.bitlinks.ems.module.power.dal.mysql.invoice.InvoicePowerRecordMapper;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.builder.ExcelWriterSheetBuilder;
import com.alibaba.excel.write.merge.OnceAbsoluteMergeStrategy;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Validated
public class InvoicePowerRecordServiceImpl implements InvoicePowerRecordService {

    @Resource
    private InvoicePowerRecordMapper recordMapper;
    @Resource
    private InvoicePowerRecordItemMapper itemMapper;

    private static final DateTimeFormatter MONTH_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM");

    private static final String EXCEL_FILE_NAME = "发票电量记录";
    private static final String EXCEL_SHEET_NAME = "数据";

    private static final String COL_STAT_LABEL = "统计项";
    private static final String COL_TOTAL_KWH_LABEL = "总电量";
    private static final String COL_DEMAND_KWH_LABEL = "需量电量";

    private static final String ROW_TOTAL_LABEL = "合计";
    private static final String ROW_AMOUNT_LABEL = "金额(含税13%)";
    private static final String ROW_AVG_PRICE_LABEL = "平均电价";

    // 两行多级表头，每个月 2 列（总电量 + 需量电量）
    private static final int HEADER_ROW_COUNT = 2;
    private static final int MONTH_COL_PER_GROUP = 2;
    private static final int STAT_COLUMN_INDEX = 0;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveInvoicePowerRecord(InvoicePowerRecordSaveReqVO reqVO) {
        // 1. 先保存主表
        InvoicePowerRecordDO record = BeanUtils.toBean(reqVO, InvoicePowerRecordDO.class);
        if (record.getId() == null) {
            recordMapper.insert(record);
        } else {
            recordMapper.updateById(record);
            // 先删旧的明细
            itemMapper.deleteByRecordId(record.getId());
        }

        Long recordId = record.getId();

        // 2. 保存明细列表
        if (reqVO.getItems() != null && !reqVO.getItems().isEmpty()) {
            List<InvoicePowerRecordItemDO> items = reqVO.getItems().stream()
                    .map(vo -> {
                        InvoicePowerRecordItemDO item = BeanUtils.toBean(vo, InvoicePowerRecordItemDO.class);
                        item.setId(null);
                        item.setRecordId(recordId);
                        return item;
                    }).collect(Collectors.toList());
            items.forEach(itemMapper::insert);
        }

        return recordId;
    }

    @Override
    public InvoicePowerRecordRespVO getInvoicePowerRecord(Long id) {
        InvoicePowerRecordDO record = recordMapper.selectById(id);
        if (record == null) {
            return null;
        }
        List<InvoicePowerRecordItemDO> items = itemMapper.selectListByRecordId(id);
        return buildResp(record, items);
    }

    @Override
    public PageResult<InvoicePowerRecordRespVO> getInvoicePowerRecordPage(InvoicePowerRecordPageReqVO pageReqVO) {
        PageResult<InvoicePowerRecordDO> page = recordMapper.selectPage(pageReqVO);
        if (page.getList().isEmpty()) {
            return new PageResult<>(java.util.Collections.emptyList(), page.getTotal());
        }


        // 批量查明细
        List<Long> recordIds = page.getList().stream().map(InvoicePowerRecordDO::getId).collect(Collectors.toList());
        List<InvoicePowerRecordItemDO> allItems = itemMapper.selectListByRecordIds(recordIds);
        Map<Long, List<InvoicePowerRecordItemDO>> itemMap = allItems.stream()
                .collect(Collectors.groupingBy(InvoicePowerRecordItemDO::getRecordId));

        List<InvoicePowerRecordRespVO> list = page.getList().stream()
                .map(r -> buildResp(r, itemMap.getOrDefault(r.getId(), Collections.emptyList())))
                .collect(Collectors.toList());

        return new PageResult<>(list, page.getTotal());
    }

    /**
     * 组装 RespVO，并计算平均电价
     */
    private InvoicePowerRecordRespVO buildResp(InvoicePowerRecordDO record, List<InvoicePowerRecordItemDO> items) {
        InvoicePowerRecordRespVO vo = BeanUtils.toBean(record, InvoicePowerRecordRespVO.class);

        // 明细
        List<InvoicePowerRecordItemRespVO> itemVOList = BeanUtils.toBean(items, InvoicePowerRecordItemRespVO.class);
        vo.setItems(itemVOList);

        // 平均电价：金额 ÷ 总电度之和
        BigDecimal totalKwhSum = items.stream()
                .map(InvoicePowerRecordItemDO::getTotalKwh)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (record.getAmount() != null && totalKwhSum.compareTo(BigDecimal.ZERO) > 0) {
            vo.setAvgPrice(
                    record.getAmount().divide(totalKwhSum, 4, RoundingMode.HALF_UP)
            );
        } else {
            // 金额为空或总电度为 0，不计算，前端展示为空
            vo.setAvgPrice(null);
        }

        return vo;
    }

    @Override
    public void exportInvoicePowerRecordExcel(HttpServletResponse response,
                                              InvoicePowerRecordPageReqVO exportReqVO) throws IOException {

        // 1. 用「列表接口同一套逻辑」拿数据，保证导出 = 页面
        PageResult<InvoicePowerRecordRespVO> page = getInvoicePowerRecordPage(exportReqVO);
        List<InvoicePowerRecordRespVO> data = page.getList();

        // 1.0 聚合结构
        LinkedHashSet<String> monthSet = new LinkedHashSet<>();
        LinkedHashSet<String> meterSet = new LinkedHashSet<>();
        Map<String, MonthAgg> monthAggMap = new LinkedHashMap<>();

        if (data != null) {
            for (InvoicePowerRecordRespVO record : data) {
                if (record.getRecordMonth() == null) {
                    continue;
                }
                String month = record.getRecordMonth().format(MONTH_FMT);
                monthSet.add(month);

                MonthAgg monthAgg = monthAggMap.computeIfAbsent(month, k -> new MonthAgg());

                if (record.getAmount() != null) {
                    monthAgg.amount = monthAgg.amount.add(record.getAmount());
                }

                List<InvoicePowerRecordItemRespVO> items = record.getItems();
                if (items == null || items.isEmpty()) {
                    continue;
                }
                for (InvoicePowerRecordItemRespVO item : items) {
                    if (item.getMeterCode() == null) {
                        continue;
                    }
                    String meter = item.getMeterCode();
                    meterSet.add(meter);

                    MeterAgg mAgg = monthAgg.meterMap
                            .computeIfAbsent(meter, k -> new MeterAgg());

                    if (item.getTotalKwh() != null) {
                        mAgg.totalKwh = mAgg.totalKwh.add(item.getTotalKwh());
                        monthAgg.totalKwh = monthAgg.totalKwh.add(item.getTotalKwh());
                    }
                    if (item.getDemandKwh() != null) {
                        mAgg.demandKwh = mAgg.demandKwh.add(item.getDemandKwh());
                        monthAgg.demandKwh = monthAgg.demandKwh.add(item.getDemandKwh());
                    }
                }
            }
        }

        // 1.1 计算平均电价 = 金额 / 总电量
        for (MonthAgg agg : monthAggMap.values()) {
            if (agg.amount != null
                    && agg.totalKwh != null
                    && agg.totalKwh.compareTo(BigDecimal.ZERO) > 0) {
                agg.avgPrice = agg.amount.divide(agg.totalKwh, 4, RoundingMode.HALF_UP);
            }
        }

        List<String> months = new ArrayList<>(monthSet);

        // ================== 2. 组装多级表头 ==================
        List<List<String>> head = new ArrayList<>();

        // 第一列：统计项目
        List<String> colStat = new ArrayList<>();
        colStat.add(COL_STAT_LABEL);
        head.add(colStat);

        // 后面每个月两列：总电量 / 需量电量
        for (String month : months) {
            List<String> colTotal = new ArrayList<>();
            colTotal.add(month);
            colTotal.add(COL_TOTAL_KWH_LABEL);
            head.add(colTotal);

            List<String> colDemand = new ArrayList<>();
            colDemand.add(month);
            colDemand.add(COL_DEMAND_KWH_LABEL);
            head.add(colDemand);
        }

        // ================== 3. 组装行 ==================
        List<List<Object>> rows = new ArrayList<>();

        // 3.1 各表计行
        for (String meter : meterSet) {
            List<Object> row = new ArrayList<>();
            row.add(meter); // 统计项目列 = 表计号

            for (String month : months) {
                MonthAgg monthAgg = monthAggMap.get(month);
                MeterAgg mAgg = (monthAgg == null ? null : monthAgg.meterMap.get(meter));
                if (mAgg != null) {
                    row.add(mAgg.totalKwh);
                    row.add(mAgg.demandKwh);
                } else {
                    row.add(null);
                    row.add(null);
                }
            }
            rows.add(row);
        }

        // 3.2 合计行
        List<Object> totalRow = new ArrayList<>();
        totalRow.add(ROW_TOTAL_LABEL);
        for (String month : months) {
            MonthAgg agg = monthAggMap.get(month);
            totalRow.add(agg == null ? null : agg.totalKwh);
            totalRow.add(agg == null ? null : agg.demandKwh);
        }
        rows.add(totalRow);

        // 3.3 金额(含税13%)
        List<Object> amountRow = new ArrayList<>();
        amountRow.add(ROW_AMOUNT_LABEL);
        for (String month : months) {
            MonthAgg agg = monthAggMap.get(month);
            amountRow.add(agg == null ? null : agg.amount); // 放在“总电量”列
            amountRow.add(null);                            // “需量电量”列留空
        }
        rows.add(amountRow);

        // 3.4 平均电价
        List<Object> avgRow = new ArrayList<>();
        avgRow.add(ROW_AVG_PRICE_LABEL);
        for (String month : months) {
            MonthAgg agg = monthAggMap.get(month);
            avgRow.add(agg == null ? null : agg.avgPrice);
            avgRow.add(null);
        }
        rows.add(avgRow);

        // ================== 4. 写 Excel ==================
        String fileName = URLEncoder.encode(EXCEL_FILE_NAME, StandardCharsets.UTF_8.name())
                .replaceAll("\\+", "%20");

        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition",
                "attachment;filename=" + fileName + ".xlsx");

        // 4.1 计算需要合并的行号（0 基）
        int dataRowStart = HEADER_ROW_COUNT;
        int meterCount = meterSet.size();

        int totalRowIndex = dataRowStart + meterCount;        // “合计”
        int amountRowIndex = totalRowIndex + 1;               // “金额(含税13%)”
        int avgRowIndex = totalRowIndex + 2;                  // “平均电价”

        // 4.2 构建样式：给所有数据单元格加边框
        WriteCellStyle headStyle = new WriteCellStyle();
        WriteFont headFont = new WriteFont();
        headFont.setBold(true);
        headStyle.setWriteFont(headFont);

        WriteCellStyle contentStyle = new WriteCellStyle();
        contentStyle.setBorderTop(BorderStyle.THIN);
        contentStyle.setBorderBottom(BorderStyle.THIN);
        contentStyle.setBorderLeft(BorderStyle.THIN);
        contentStyle.setBorderRight(BorderStyle.THIN);

        HorizontalCellStyleStrategy styleStrategy =
                new HorizontalCellStyleStrategy(headStyle, contentStyle);

        // 4.3 注册「一次性合并」策略：金额 / 平均电价 两行，每个月两列合并
        ExcelWriterSheetBuilder sheetBuilder = EasyExcel
                .write(response.getOutputStream())
                .head(head)
                .sheet(EXCEL_SHEET_NAME)
                .registerWriteHandler(styleStrategy);

        for (int i = 0; i < months.size(); i++) {
            int colStart = STAT_COLUMN_INDEX + 1 + i * MONTH_COL_PER_GROUP; // 从 B 列开始
            int colEnd = colStart + MONTH_COL_PER_GROUP - 1;

            sheetBuilder.registerWriteHandler(
                    new OnceAbsoluteMergeStrategy(amountRowIndex, amountRowIndex, colStart, colEnd));
            sheetBuilder.registerWriteHandler(
                    new OnceAbsoluteMergeStrategy(avgRowIndex, avgRowIndex, colStart, colEnd));
        }

        sheetBuilder.doWrite(rows);
    }

    /**
     * 某个月的聚合数据
     */
    private static class MonthAgg {
        // 表计号 -> 表计聚合
        Map<String, MeterAgg> meterMap = new LinkedHashMap<>();
        BigDecimal totalKwh = BigDecimal.ZERO;
        BigDecimal demandKwh = BigDecimal.ZERO;
        BigDecimal amount = BigDecimal.ZERO;
        BigDecimal avgPrice = null;
    }

    /**
     * 某个月某个表计的聚合
     */
    private static class MeterAgg {
        BigDecimal totalKwh = BigDecimal.ZERO;
        BigDecimal demandKwh = BigDecimal.ZERO;
    }

}
