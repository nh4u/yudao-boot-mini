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
    private static final String COL_TOTAL_KWH_LABEL = "总电度";
    private static final String COL_DEMAND_KWH_LABEL = "需量电度";

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

        // 先用原来的逻辑组装 VO（里面会把 DO -> RespVO，并且把 0 -> null）
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

    /**
     * 组装 RespVO（不再计算平均电价）
     */
    private InvoicePowerRecordRespVO buildResp(InvoicePowerRecordDO record, List<InvoicePowerRecordItemDO> items) {
        InvoicePowerRecordRespVO vo = BeanUtils.toBean(record, InvoicePowerRecordRespVO.class);

        // 明细
        List<InvoicePowerRecordItemRespVO> itemVOList = BeanUtils.toBean(items, InvoicePowerRecordItemRespVO.class);

        // === 这里做 0 -> null 的转换 ===
        if (itemVOList != null) {
            for (InvoicePowerRecordItemRespVO item : itemVOList) {
                item.setTotalKwh(zeroToNull(item.getTotalKwh()));
                item.setDemandKwh(zeroToNull(item.getDemandKwh()));
            }
        }

        vo.setItems(itemVOList);

        // 不再计算 avgPrice，保持为 null 或数据库原值
        vo.setAvgPrice(null);

        return vo;
    }

    /**
     * BigDecimal 为 0 或 null 时都返回 null
     */
    private BigDecimal zeroToNull(BigDecimal val) {
        if (val == null) {
            return null;
        }
        return BigDecimal.ZERO.compareTo(val) == 0 ? null : val;
    }



    private static class MeterMonthAgg {
        BigDecimal totalKwh = BigDecimal.ZERO;
        BigDecimal demandKwh = BigDecimal.ZERO;
    }

    @Override
    public StatisticsResultV2VO<InvoicePowerRecordStatisticsInfo> getInvoicePowerRecordList(
            InvoicePowerRecordPageReqVO pageReqVO) {

        // 1. 解析月份范围 -> monthList + header
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

        // 从字典获取电表顺序
        List<String> meterCodes = DictFrameworkUtils.getDictDataLabelList("INVOICE_METER_CODE");

        if (meterCodes == null || meterCodes.isEmpty()) {

            for (Map.Entry<String, Map<YearMonth, MeterMonthAgg>> entry : meterMonthMap.entrySet()) {
                String meterCode = entry.getKey();
                Map<YearMonth, MeterMonthAgg> monthAggMap = entry.getValue();

                InvoicePowerRecordStatisticsInfo info = buildMeterRow(
                        idx++, meterCode, monthAggMap, monthList, header);
                statisticsInfoList.add(info);
            }
        } else {
            // 用字典顺序来控制电表行的顺序
            for (String meterCode : meterCodes) {
                Map<YearMonth, MeterMonthAgg> monthAggMap = meterMonthMap.get(meterCode);
                if (monthAggMap == null) {
                    continue;
                }
                InvoicePowerRecordStatisticsInfo info = buildMeterRow(
                        idx++, meterCode, monthAggMap, monthList, header);
                statisticsInfoList.add(info);
            }
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
            data.setAmount(monthAmount);
            data.setTotalKwh(null);
            data.setDemandKwh(null);

            amountDataList.add(data);
        }
        amountRow.setStatisticsDateDataList(amountDataList);
        statisticsInfoList.add(amountRow);

        result.setStatisticsInfoList(statisticsInfoList);
        return result;
    }

    private InvoicePowerRecordStatisticsInfo buildMeterRow(
            long id,
            String meterCode,
            Map<YearMonth, MeterMonthAgg> monthAggMap,
            List<YearMonth> monthList,
            List<String> header) {

        InvoicePowerRecordStatisticsInfo info = new InvoicePowerRecordStatisticsInfo();
        info.setId(id);
        info.setName(meterCode);

        List<InvoicePowerRecordStatisticsData> dataList = new ArrayList<>();

        for (int i = 0; i < monthList.size(); i++) {
            YearMonth ym = monthList.get(i);

            InvoicePowerRecordStatisticsData data = new InvoicePowerRecordStatisticsData();
            data.setDate(header.get(i)); // 例如：2025年09月

            MeterMonthAgg agg = monthAggMap.get(ym);
            if (agg != null) {
                // 保持你现在的“0 -> null”的规则
                data.setTotalKwh(
                        agg.totalKwh.compareTo(BigDecimal.ZERO) == 0 ? null : agg.totalKwh);
                data.setDemandKwh(
                        agg.demandKwh.compareTo(BigDecimal.ZERO) == 0 ? null : agg.demandKwh);
            } else {
                data.setTotalKwh(null);
                data.setDemandKwh(null);
            }
            data.setAmount(null); // 电表行不写金额

            dataList.add(data);
        }

        info.setStatisticsDateDataList(dataList);
        return info;
    }


    @Override
    public void exportInvoicePowerRecordExcel(HttpServletResponse response,
                                              InvoicePowerRecordPageReqVO exportReqVO) throws IOException {

        // 1. 用「列表接口同一套逻辑」拿数据，保证导出 = 页面
        List<InvoicePowerRecordRespVO> data = getInvoicePowerRecordListRaw(exportReqVO);

        // ============ 1.0 聚合结构 ============
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
                    monthAgg.hasAmount = true;
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

                    MeterAgg mAgg = monthAgg.meterMap.computeIfAbsent(meter, k -> new MeterAgg());

                    if (item.getTotalKwh() != null) {
                        mAgg.totalKwh = mAgg.totalKwh.add(item.getTotalKwh());
                        mAgg.hasTotalKwh = true;

                        monthAgg.totalKwh = monthAgg.totalKwh.add(item.getTotalKwh());
                        monthAgg.hasTotalKwh = true;
                    }
                    if (item.getDemandKwh() != null) {
                        mAgg.demandKwh = mAgg.demandKwh.add(item.getDemandKwh());
                        mAgg.hasDemandKwh = true;

                        monthAgg.demandKwh = monthAgg.demandKwh.add(item.getDemandKwh());
                        monthAgg.hasDemandKwh = true;
                    }
                }
            }
        }

        // 1.1 计算平均电价 = 金额 / 总电量
        for (MonthAgg agg : monthAggMap.values()) {
            if (agg.hasAmount && agg.hasTotalKwh && agg.totalKwh.compareTo(BigDecimal.ZERO) != 0) {
                agg.avgPrice = agg.amount.divide(agg.totalKwh, 4, RoundingMode.HALF_UP);
            } else {
                agg.avgPrice = null;
            }
        }

        // ============ 2. 根据查询时间范围生成月份列表（即使没有数据也有骨架） ============

        List<YearMonth> ymList = new ArrayList<>();
        LocalDate[] range = exportReqVO.getRecordMonth();
        if (range != null && range.length == 2 && range[0] != null && range[1] != null) {
            YearMonth startYm = YearMonth.from(range[0]);
            YearMonth endYm = YearMonth.from(range[1]);
            YearMonth cursor = startYm;
            while (!cursor.isAfter(endYm)) {
                ymList.add(cursor);
                cursor = cursor.plusMonths(1);
            }
        } else if (!monthSet.isEmpty()) {
            // 没有传范围，就按照已有数据的月份来（兜底）
            for (String m : monthSet) {
                ymList.add(YearMonth.parse(m, MONTH_FMT));
            }
            ymList.sort(Comparator.naturalOrder());
        } else {
            // 再兜底：什么都没有时，至少给当前月份一个
            ymList.add(YearMonth.now());
        }

        List<String> months = ymList.stream()
                .map(ym -> ym.format(MONTH_FMT))
                .collect(Collectors.toList());

        // 2.1 统计周期文本，只在前端有选范围时展示，否则用 "/"
        String periodText = "/";
        if (range != null && range.length == 2 && range[0] != null && range[1] != null) {
            YearMonth startYm = YearMonth.from(range[0]);
            YearMonth endYm = YearMonth.from(range[1]);
            if (startYm.equals(endYm)) {
                // 同一个月份时只展示一个，例如：2024-09
                periodText = startYm.format(MONTH_FMT); // MONTH_FMT = yyyy-MM
            } else {
                // 不同月份时展示区间，例如：2024-09 ~ 2024-12
                periodText = startYm.format(MONTH_FMT) + " ~ " + endYm.format(MONTH_FMT);
            }
        }

        // 2.2 表计列表：优先用字典，保证没数据也有行
        List<String> dictMeters = DictFrameworkUtils.getDictDataLabelList("INVOICE_METER_CODE");
        List<String> meterList = new ArrayList<>();
        if (dictMeters != null && !dictMeters.isEmpty()) {
            meterList.addAll(dictMeters);
        } else {
            meterList.addAll(meterSet);
        }

        // ================== 3. 组装多级表头 ==================

        // 表头总行数（0-based 行号：0..3）
        // 0：表单名称
        // 1：统计周期
        // 2：A 列“统计项”（A3:A4 合并），B 开始是月份（每个月跨两列）
        // 3：每个月下面“总电度 / 需量电度”
        final int HEADER_ROW_COUNT = 4;

        List<List<String>> head = new ArrayList<>();

        // 第 1 列（统计列）的 4 行表头
        List<String> colStat = new ArrayList<>();
        colStat.add("表单名称");   // row 0 -> A1
        colStat.add("统计周期");   // row 1 -> A2
        colStat.add("统计项");     // row 2 -> A3（和 A4 合并）
        colStat.add("");          // row 3 -> A4
        head.add(colStat);

        // 其它列：每个月两列（总电度 / 需量电度）
        for (String month : months) {
            // 总电度列
            List<String> colTotal = new ArrayList<>();
            colTotal.add("发票电量记录");      // row 0：B1~... 合并显示
            colTotal.add(periodText);          // row 1：B2~... 合并显示
            colTotal.add(month);               // row 2：月份（跨两列）
            colTotal.add(COL_TOTAL_KWH_LABEL); // row 3：总电度
            head.add(colTotal);

            // 需量电度列
            List<String> colDemand = new ArrayList<>();
            colDemand.add("发票电量记录");
            colDemand.add(periodText);
            colDemand.add(month);
            colDemand.add(COL_DEMAND_KWH_LABEL); // row 3：需量电度
            head.add(colDemand);
        }

        // ================== 4. 组装数据行 ==================
        List<List<Object>> rows = new ArrayList<>();

        // 4.1 各表计行（即使没有数据，也按 meterList 输出一行，全是 "/"）
        for (String meter : meterList) {
            List<Object> row = new ArrayList<>();
            row.add(meter); // 统计项列 = 表计编号

            for (String month : months) {
                MonthAgg monthAgg = monthAggMap.get(month);
                MeterAgg mAgg = (monthAgg == null ? null : monthAgg.meterMap.get(meter));

                // 总电度：有数据 -> 值(可为 0)，无数据 -> "/"
                if (mAgg != null && mAgg.hasTotalKwh) {
                    row.add(mAgg.totalKwh);
                } else {
                    row.add("/");
                }

                // 需量电度：同理
                if (mAgg != null && mAgg.hasDemandKwh) {
                    row.add(mAgg.demandKwh);
                } else {
                    row.add("/");
                }
            }
            rows.add(row);
        }

        // 4.2 合计行
        List<Object> totalRow = new ArrayList<>();
        totalRow.add(ROW_TOTAL_LABEL);
        for (String month : months) {
            MonthAgg agg = monthAggMap.get(month);

            if (agg != null && agg.hasTotalKwh) {
                totalRow.add(agg.totalKwh);
            } else {
                totalRow.add("/");
            }

            if (agg != null && agg.hasDemandKwh) {
                totalRow.add(agg.demandKwh);
            } else {
                totalRow.add("/");
            }
        }
        rows.add(totalRow);

        // 4.3 金额(含税13%) —— 每个月两列合并成一格，只在左列写值
        List<Object> amountRow = new ArrayList<>();
        amountRow.add(ROW_AMOUNT_LABEL);
        for (String month : months) {
            MonthAgg agg = monthAggMap.get(month);
            if (agg != null && agg.hasAmount) {
                amountRow.add(agg.amount);
            } else {
                amountRow.add("/");   // 左列：无数据 -> "/"
            }
            amountRow.add(null);      // 右列：留空，后面合并
        }
        rows.add(amountRow);

        // 4.4 平均电价 —— 同样两列合并
        List<Object> avgRow = new ArrayList<>();
        avgRow.add(ROW_AVG_PRICE_LABEL);
        for (String month : months) {
            MonthAgg agg = monthAggMap.get(month);
            if (agg != null && agg.avgPrice != null) {
                avgRow.add(agg.avgPrice);
            } else {
                avgRow.add("/");      // 左列：无数据 -> "/"
            }
            avgRow.add(null);         // 右列留空
        }
        rows.add(avgRow);

        // ================== 5. 写 Excel ==================
        String fileName = URLEncoder.encode(EXCEL_FILE_NAME, StandardCharsets.UTF_8.name())
                .replaceAll("\\+", "%20");

        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition",
                "attachment;filename=" + fileName + ".xlsx");
        response.addHeader("Access-Control-Expose-Headers", "File-Name");
        response.addHeader("File-Name", fileName);

        // 5.1 计算行列索引（0 基）
        int dataRowStart = HEADER_ROW_COUNT;         // 数据起始行：第 5 行
        int meterCount = meterList.size();

        int totalRowIndex  = dataRowStart + meterCount; // “合计”
        int amountRowIndex = totalRowIndex + 1;         // “金额(含税13%)”
        int avgRowIndex    = totalRowIndex + 2;         // “平均电价”

        int firstDataCol = STAT_COLUMN_INDEX + 1;       // 从 B 列开始
        int lastCol = STAT_COLUMN_INDEX + months.size() * MONTH_COL_PER_GROUP;

        // 5.2 表头 & 内容样式（不换行 + 加边框）
        WriteCellStyle headStyle = new WriteCellStyle();
        WriteFont headFont = new WriteFont();
        headFont.setBold(true);
        headStyle.setWriteFont(headFont);
        headStyle.setWrapped(false);

        WriteCellStyle contentStyle = new WriteCellStyle();
        contentStyle.setBorderTop(BorderStyle.THIN);
        contentStyle.setBorderBottom(BorderStyle.THIN);
        contentStyle.setBorderLeft(BorderStyle.THIN);
        contentStyle.setBorderRight(BorderStyle.THIN);
        contentStyle.setWrapped(false);

        HorizontalCellStyleStrategy styleStrategy =
                new HorizontalCellStyleStrategy(headStyle, contentStyle);

        ExcelWriterSheetBuilder sheetBuilder = EasyExcel
                .write(response.getOutputStream())
                .head(head)
                .sheet(EXCEL_SHEET_NAME)
                .registerWriteHandler(styleStrategy)
                .registerWriteHandler(new com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy());

        // 5.3 表头合并

        // 5.3.1 第 1 行：发票电量记录（B1 ~ lastCol）
        sheetBuilder.registerWriteHandler(
                new OnceAbsoluteMergeStrategy(0, 0, firstDataCol, lastCol));
        // 5.3.2 第 2 行：统计周期（B2 ~ lastCol）
        sheetBuilder.registerWriteHandler(
                new OnceAbsoluteMergeStrategy(1, 1, firstDataCol, lastCol));
        // 5.3.3 A3:A4 合并为“统计项”（行 2~3，列 A）
        sheetBuilder.registerWriteHandler(
                new OnceAbsoluteMergeStrategy(2, 3, STAT_COLUMN_INDEX, STAT_COLUMN_INDEX));
        // 5.3.4 第 3 行（行号 2）：每个月跨两列合并（月份）
        for (int i = 0; i < months.size(); i++) {
            int colStart = firstDataCol + i * MONTH_COL_PER_GROUP;
            int colEnd = colStart + MONTH_COL_PER_GROUP - 1;
            sheetBuilder.registerWriteHandler(
                    new OnceAbsoluteMergeStrategy(2, 2, colStart, colEnd));
        }

        // 5.3.5 “金额 / 平均电价” 每个月两列合并
        for (int i = 0; i < months.size(); i++) {
            int colStart = firstDataCol + i * MONTH_COL_PER_GROUP;
            int colEnd = colStart + MONTH_COL_PER_GROUP - 1;

            sheetBuilder.registerWriteHandler(
                    new OnceAbsoluteMergeStrategy(amountRowIndex, amountRowIndex, colStart, colEnd));
            sheetBuilder.registerWriteHandler(
                    new OnceAbsoluteMergeStrategy(avgRowIndex, avgRowIndex, colStart, colEnd));
        }

        // 5.4 写入
        sheetBuilder.doWrite(rows);
    }

    /**
     * 导出专用：获取“原始列表结构”的发票电量记录（不做 0->null 等处理）
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

        // 3. 直接 DO -> RespVO，不经过 buildResp（否则 0 会被转成 null）
        return records.stream().map(r -> {
            InvoicePowerRecordRespVO vo = BeanUtils.toBean(r, InvoicePowerRecordRespVO.class);
            List<InvoicePowerRecordItemDO> items = itemMap.getOrDefault(r.getId(), Collections.emptyList());
            List<InvoicePowerRecordItemRespVO> itemVOs = BeanUtils.toBean(items, InvoicePowerRecordItemRespVO.class);
            vo.setItems(itemVOs);
            return vo;
        }).collect(Collectors.toList());
    }


    static class MeterAgg {
        BigDecimal totalKwh = BigDecimal.ZERO;
        BigDecimal demandKwh = BigDecimal.ZERO;

        boolean hasTotalKwh = false;
        boolean hasDemandKwh = false;
    }

    static class MonthAgg {
        BigDecimal totalKwh = BigDecimal.ZERO;
        BigDecimal demandKwh = BigDecimal.ZERO;
        BigDecimal amount    = BigDecimal.ZERO;
        BigDecimal avgPrice;

        boolean hasTotalKwh = false;
        boolean hasDemandKwh = false;
        boolean hasAmount = false;

        Map<String, MeterAgg> meterMap = new HashMap<>();
    }


}
