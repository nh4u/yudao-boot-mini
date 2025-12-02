package cn.bitlinks.ems.module.power.service.invoice;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.dict.core.DictFrameworkUtils;
import cn.bitlinks.ems.module.power.controller.admin.invoice.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
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
    public InvoicePowerRecordRespVO getInvoicePowerRecord(LocalDate recordMonth) {
        InvoicePowerRecordDO record = recordMapper.selectByRecordMonth(recordMonth);
        List<InvoicePowerRecordItemDO> items;

        if (record == null) {
            // 没有主表记录：构造一个“空”的记录，只带月份，方便前端编辑
            record = new InvoicePowerRecordDO();
            record.setId(null);
            record.setRecordMonth(recordMonth);
            record.setAmount(null);
            record.setRemark(null);
            items = Collections.emptyList();
        } else {
            // 有主表记录：查明细
            items = itemMapper.selectListByRecordId(record.getId());
        }

        // 先用原来的逻辑组装 VO（里面会把 DO -> RespVO）
        InvoicePowerRecordRespVO vo = buildResp(record, items);

        // ========= 关键补齐逻辑：用字典补全所有电表 =========

        // 1. 从字典获取所有电表编码（顺序就是表格展示顺序）
        List<String> meterCodes = DictFrameworkUtils.getDictDataLabelList("INVOICE_METER_CODE");
        if (meterCodes == null || meterCodes.isEmpty()) {
            // 字典没配好就不强行补齐，直接返回原始数据
            return vo;
        }

        // 2. 现有的明细转 map，key = meterCode
        Map<String, InvoicePowerRecordItemRespVO> itemMap =
                Optional.ofNullable(vo.getItems())
                        .orElse(Collections.emptyList())
                        .stream()
                        .filter(i -> i.getMeterCode() != null)
                        .collect(Collectors.toMap(InvoicePowerRecordItemRespVO::getMeterCode,
                                Function.identity(),
                                (a, b) -> a));

        // 3. 按字典顺序重新构造 items：有数据用原有的，没有就补一条 totalKwh/demandKwh = null
        List<InvoicePowerRecordItemRespVO> normalizedItems = new ArrayList<>();
        for (String meterCode : meterCodes) {
            InvoicePowerRecordItemRespVO itemVO = itemMap.get(meterCode);
            if (itemVO == null) {
                itemVO = new InvoicePowerRecordItemRespVO();
                itemVO.setMeterCode(meterCode);
                itemVO.setTotalKwh(null);
                itemVO.setDemandKwh(null);
            }
            normalizedItems.add(itemVO);
        }

        vo.setItems(normalizedItems);
        return vo;
    }


    private static class MeterMonthAgg {
        BigDecimal totalKwh = BigDecimal.ZERO;
        BigDecimal demandKwh = BigDecimal.ZERO;
    }

    @Override
    public StatisticsResultV2VO<InvoicePowerRecordStatisticsInfo> getInvoicePowerRecordList(
            InvoicePowerRecordPageReqVO pageReqVO) {

        // 1. 解析月份范围 -> monthList + header （保持你现在的实现）
        LocalDate[] range = pageReqVO.getRecordMonth();
        LocalDate start;
        LocalDate end;
        if (range != null && range.length == 2) {
            start = range[0];
            end = range[1];
        } else {
            start = LocalDate.now().withDayOfMonth(1);
            end = start;
        }

        List<YearMonth> monthList = new ArrayList<>();
        List<String> header = new ArrayList<>();
        YearMonth cursor = YearMonth.from(start);
        YearMonth last = YearMonth.from(end);
        DateTimeFormatter headerFormatter = DateTimeFormatter.ofPattern("yyyy年MM月");
        while (!cursor.isAfter(last)) {
            monthList.add(cursor);
            header.add(cursor.format(headerFormatter));
            cursor = cursor.plusMonths(1);
        }

        StatisticsResultV2VO<InvoicePowerRecordStatisticsInfo> result = new StatisticsResultV2VO<>();
        result.setHeader(header);
        result.setDataTime(LocalDateTime.now());

        // 2. 查主表记录
        List<InvoicePowerRecordDO> records = recordMapper.selectList(pageReqVO);
        if (records.isEmpty()) {
            result.setStatisticsInfoList(Collections.emptyList());
            return result;
        }

        // 3. 查明细
        List<Long> recordIds = records.stream()
                .map(InvoicePowerRecordDO::getId)
                .collect(Collectors.toList());

        List<InvoicePowerRecordItemDO> allItems = itemMapper.selectListByRecordIds(recordIds);
        Map<Long, List<InvoicePowerRecordItemDO>> itemsByRecordId = allItems.stream()
                .collect(Collectors.groupingBy(InvoicePowerRecordItemDO::getRecordId));

        // 3.1 记录 -> YearMonth
        Map<Long, YearMonth> recordMonthMap = records.stream()
                .collect(Collectors.toMap(
                        InvoicePowerRecordDO::getId,
                        r -> YearMonth.from(r.getRecordMonth())
                ));

        // 3.2 电表-月份-电度 聚合
        Map<String, Map<YearMonth, MeterMonthAgg>> meterMonthMap = new LinkedHashMap<>();

        // 3.3 月份-金额 聚合
        Map<YearMonth, BigDecimal> monthAmountMap = new HashMap<>();

        for (InvoicePowerRecordDO record : records) {
            Long recordId = record.getId();
            YearMonth ym = recordMonthMap.get(recordId);

            // 金额：按月份汇总
            if (record.getAmount() != null) {
                monthAmountMap.merge(ym, record.getAmount(), BigDecimal::add);
            }

            List<InvoicePowerRecordItemDO> items = itemsByRecordId.getOrDefault(recordId, Collections.emptyList());
            for (InvoicePowerRecordItemDO item : items) {
                String meterCode = item.getMeterCode();
                if (meterCode == null) {
                    continue;
                }
                Map<YearMonth, MeterMonthAgg> monthAggMap =
                        meterMonthMap.computeIfAbsent(meterCode, k -> new HashMap<>());
                MeterMonthAgg agg = monthAggMap.computeIfAbsent(ym, k -> new MeterMonthAgg());

                if (item.getTotalKwh() != null) {
                    agg.totalKwh = agg.totalKwh.add(item.getTotalKwh());
                }
                if (item.getDemandKwh() != null) {
                    agg.demandKwh = agg.demandKwh.add(item.getDemandKwh());
                }
            }
        }

        // 4. 组装“电表行”：总电度 + 需量电度，没有就 null
        List<InvoicePowerRecordStatisticsInfo> statisticsInfoList = new ArrayList<>();
        long idx = 1L;

        for (Map.Entry<String, Map<YearMonth, MeterMonthAgg>> entry : meterMonthMap.entrySet()) {
            String meterCode = entry.getKey();
            Map<YearMonth, MeterMonthAgg> monthAggMap = entry.getValue();

            InvoicePowerRecordStatisticsInfo info = new InvoicePowerRecordStatisticsInfo();
            info.setId(idx++);
            info.setName(meterCode);

            List<InvoicePowerRecordStatisticsData> dataList = new ArrayList<>();

            for (int i = 0; i < monthList.size(); i++) {
                YearMonth ym = monthList.get(i);

                InvoicePowerRecordStatisticsData data = new InvoicePowerRecordStatisticsData();
                data.setDate(header.get(i)); // 2025年09月

                MeterMonthAgg agg = monthAggMap.get(ym);
                if (agg != null) {
                    data.setTotalKwh(
                            agg.totalKwh.compareTo(BigDecimal.ZERO) == 0 ? null : agg.totalKwh);
                    data.setDemandKwh(
                            agg.demandKwh.compareTo(BigDecimal.ZERO) == 0 ? null : agg.demandKwh);
                } else {
                    // 没数据 = null
                    data.setTotalKwh(null);
                    data.setDemandKwh(null);
                }
                // 电表行不写金额
                data.setAmount(null);

                dataList.add(data);
            }

            info.setStatisticsDateDataList(dataList);
            statisticsInfoList.add(info);
        }

        // 5. 追加“金额”一行：每个月只写 amount，其它为 null
        InvoicePowerRecordStatisticsInfo amountRow = new InvoicePowerRecordStatisticsInfo();
        amountRow.setId(idx);
        amountRow.setName("金额");

        List<InvoicePowerRecordStatisticsData> amountDataList = new ArrayList<>();
        for (int i = 0; i < monthList.size(); i++) {
            YearMonth ym = monthList.get(i);

            InvoicePowerRecordStatisticsData data = new InvoicePowerRecordStatisticsData();
            data.setDate(header.get(i));

            BigDecimal monthAmount = monthAmountMap.get(ym);
            data.setAmount(monthAmount);   // 金额
            data.setTotalKwh(null);        // 不用
            data.setDemandKwh(null);       // 不用

            amountDataList.add(data);
        }
        amountRow.setStatisticsDateDataList(amountDataList);
        statisticsInfoList.add(amountRow);

        result.setStatisticsInfoList(statisticsInfoList);
        return result;
    }



    /**
     * 组装 RespVO（不再计算平均电价）
     */
    private InvoicePowerRecordRespVO buildResp(InvoicePowerRecordDO record, List<InvoicePowerRecordItemDO> items) {
        InvoicePowerRecordRespVO vo = BeanUtils.toBean(record, InvoicePowerRecordRespVO.class);

        // 明细
        List<InvoicePowerRecordItemRespVO> itemVOList = BeanUtils.toBean(items, InvoicePowerRecordItemRespVO.class);
        vo.setItems(itemVOList);

        // 不再计算 avgPrice，保持为 null 或数据库原值
        vo.setAvgPrice(null);

        return vo;
    }

    @Override
    public void exportInvoicePowerRecordExcel(HttpServletResponse response,
                                              InvoicePowerRecordPageReqVO exportReqVO) throws IOException {

        // 1. 用「列表接口同一套逻辑」拿数据，保证导出 = 页面
        List<InvoicePowerRecordRespVO> data = getInvoicePowerRecordListRaw(exportReqVO);

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
     * 导出专用：获取“原始列表结构”的发票电量记录
     */
    private List<InvoicePowerRecordRespVO> getInvoicePowerRecordListRaw(InvoicePowerRecordPageReqVO pageReqVO) {
        // 1. 不分页，直接查全部记录
        List<InvoicePowerRecordDO> records = recordMapper.selectList(pageReqVO);
        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 批量查明细
        List<Long> recordIds = records.stream()
                .map(InvoicePowerRecordDO::getId)
                .collect(Collectors.toList());

        List<InvoicePowerRecordItemDO> allItems = itemMapper.selectListByRecordIds(recordIds);
        Map<Long, List<InvoicePowerRecordItemDO>> itemMap = allItems.stream()
                .collect(Collectors.groupingBy(InvoicePowerRecordItemDO::getRecordId));

        // 3. 复用原来的 buildResp 组装 RespVO
        return records.stream()
                .map(r -> buildResp(r, itemMap.getOrDefault(r.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
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
