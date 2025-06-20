package cn.bitlinks.ems.module.power.service.additionalrecording;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExcelMeterDataProcessor {

    public static class MinuteData {
        public String meterName;
        public LocalDateTime time;
        public BigDecimal value;
        public BigDecimal all;

        public MinuteData(String meterName, LocalDateTime time, BigDecimal value, BigDecimal all) {
            this.meterName = meterName;
            this.time = time;
            this.value = value;
            this.all = all;
        }

        public String getMeterName() {
            return meterName;
        }

        public void setMeterName(String meterName) {
            this.meterName = meterName;
        }

        public LocalDateTime getTime() {
            return time;
        }

        public void setTime(LocalDateTime time) {
            this.time = time;
        }

        public BigDecimal getValue() {
            return value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }

        public BigDecimal getAll() {
            return all;
        }

        public void setAll(BigDecimal all) {
            this.all = all;
        }

        @Override
        public String toString() {
            return "MinuteData{" +
                    "meterName='" + meterName + '\'' +
                    ", time=" + time +
                    ", value=" + value +
                    ", all=" + all +
                    '}';
        }
    }


    public static void main(String[] args) {
        try {
            long l = System.currentTimeMillis();
            // 1. 指定本地 Excel 文件路径（可绝对或相对路径）
            String filePath = "D:\\全是文档\\燕东\\5105.xls"; // <-- 替换为你本地文件路径
            FileInputStream fis = new FileInputStream(new File(filePath));


            // 3. 调用处理器（构造时传入 workbook 而不是 MultipartFile）
            ExcelMeterDataProcessor processor = new ExcelMeterDataProcessor();

            // 提供单元格范围参数
            List<MinuteData> result = processor.process(
                    fis,
                    "A4",  // 时间起始单元格
                    "A27", // 时间结束单元格
                    "B3",  // 计量器具起始单元格
                    "S3"   // 计量器具结束单元格
            );

            // 4. 打印结果
            Collections.sort(result, Comparator.comparing(MinuteData::getTime));
            result.stream().filter(s -> s.getMeterName().equals("5105F3-d 备用 正向有功电能")).forEach(System.out::println);
            System.out.println("共生成分钟数据条数: " + result.size());


            fis.close();
            System.out.println("耗时: " + (System.currentTimeMillis() - l) + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 单元格位置处理
     *
     * @param cellRef
     * @return
     */
    public static int[] parseCell(String cellRef) {
        Pattern pattern = Pattern.compile("([A-Z]+)([0-9]+)");
        Matcher matcher = pattern.matcher(cellRef.toUpperCase());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid cell reference: " + cellRef);
        }
        String colStr = matcher.group(1);
        int row = Integer.parseInt(matcher.group(2)) - 1;

        int col = 0;
        for (int i = 0; i < colStr.length(); i++) {
            col *= 26;
            col += colStr.charAt(i) - 'A' + 1;
        }
        return new int[]{row, col - 1};
    }

    public List<MinuteData> process(InputStream file,
                                    String timeStartCell,
                                    String timeEndCell,
                                    String meterStartCell,
                                    String meterEndCell) throws IOException {

        int[] timeStart = parseCell(timeStartCell);
        int[] timeEnd = parseCell(timeEndCell);
        int[] meterStart = parseCell(meterStartCell);
        int[] meterEnd = parseCell(meterEndCell);
        //判断时间方向是否列
        boolean timeVertical = timeStart[1] == timeEnd[1];
        //判断计量器具方向是否行
        boolean meterHorizontal = meterStart[0] == meterEnd[0];

        Workbook workbook = WorkbookFactory.create(file);
        Sheet sheet = workbook.getSheetAt(0);
        //表中计量器具名称
        List<String> meterNames = new ArrayList<>();
        //横向计量器
        if (meterHorizontal) {
            Row meterRow = sheet.getRow(meterStart[0]);
            for (int c = meterStart[1]; c <= meterEnd[1]; c++) {
                Cell cell = meterRow.getCell(c);
                meterNames.add(cell == null ? "" : cell.toString());
            }
        } else {
            for (int r = meterStart[0]; r <= meterEnd[0]; r++) {
                Row row = sheet.getRow(r);
                if (row == null) meterNames.add("");
                else {
                    Cell cell = row.getCell(meterStart[1]);
                    meterNames.add(cell == null ? "" : cell.toString());
                }
            }
        }
        //时间列表
        List<LocalDateTime> times = new ArrayList<>();
        //纵向时间
        if (timeVertical) {
            int col = timeStart[1];
            for (int r = timeStart[0]; r <= timeEnd[0]; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Cell cell = row.getCell(col);
                LocalDateTime dt = parseTime(cell);
                if (dt != null) times.add(dt);
            }
        } else {
            int rowIdx = timeStart[0];
            Row row = sheet.getRow(rowIdx);
            if (row != null) {
                for (int c = timeStart[1]; c <= timeEnd[1]; c++) {
                    Cell cell = row.getCell(c);
                    LocalDateTime dt = parseTime(cell);
                    if (dt != null) times.add(dt);
                }
            }
        }

        Map<String, List<BigDecimal>> meterValuesMap = new LinkedHashMap<>();
        meterNames.forEach(name -> meterValuesMap.put(name, new ArrayList<>()));
        //纵向时间横向计量器具
        if (timeVertical && meterHorizontal) {
            int timeRowStart = timeStart[0];
            for (int r = timeRowStart; r < timeRowStart + times.size(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                for (int i = 0; i < meterNames.size(); i++) {
                    Cell cell = row.getCell(meterStart[1] + i);
                    meterValuesMap.get(meterNames.get(i)).add(getNumericValue(cell));
                }
            }
        } else {//横向时间纵向计量器具
            for (int c = timeStart[1]; c < timeStart[1] + times.size(); c++) {
                for (int i = 0; i < meterNames.size(); i++) {
                    Row row = sheet.getRow(meterStart[0] + i);
                    if (row == null) continue;
                    Cell cell = row.getCell(c);
                    meterValuesMap.get(meterNames.get(i)).add(getNumericValue(cell));
                }
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(meterNames.size(), Runtime.getRuntime().availableProcessors()));
        List<Future<List<MinuteData>>> futures = new ArrayList<>();

        for (Map.Entry<String, List<BigDecimal>> entry : meterValuesMap.entrySet()) {
            String meter = entry.getKey();
            List<BigDecimal> values = entry.getValue();

            futures.add(executor.submit(() -> {
                List<MinuteData> subResult = new ArrayList<>();
                for (int i = 0; i < times.size() - 1; i++) {
                    LocalDateTime start = times.get(i);
                    LocalDateTime end = times.get(i + 1);
                    BigDecimal diff = values.get(i + 1).subtract(values.get(i));
                    long minutes = Duration.between(start, end).toMinutes();
                    // 跳过非法或零时长区间
                    if (minutes <= 0) continue;
                    BigDecimal perMinute = BigDecimal.ZERO;
                    if (!(diff.compareTo(BigDecimal.ZERO) == 0)) {
                        // 计算每分钟的增量值，保留10位小数
                        perMinute = diff.divide(BigDecimal.valueOf(minutes), 10, RoundingMode.HALF_UP);
                    }
                    BigDecimal nowAll = values.get(i);
                    for (int m = 0; m < minutes; m++) {

                        if (m == 0 && start.getHour() == 0 && start.getMinute() == 0) {
                            // 判断是否是整点零时（00:00）开头，第一分钟增量为0
                            subResult.add(new MinuteData(meter, start.plusMinutes(m), BigDecimal.ZERO, values.get(i)));
                        }
                        if (m == 0 && start.getMinute() == 0 && start.getHour() > 0) {
                            // 判断是否是其他整点时间，第一分钟使用当前整点表底值，无需累加
                            subResult.add(new MinuteData(meter, start.plusMinutes(m), perMinute, values.get(i)));
                        } else {
                            // 普通分钟，累加当前增量
                            LocalDateTime localDateTime = start.plusMinutes(m);
                            nowAll = nowAll.add(perMinute);
                            subResult.add(new MinuteData(meter, localDateTime, perMinute, nowAll));
                        }

                    }
                }
                return subResult;
            }));
        }

        List<MinuteData> result = new ArrayList<>();
        for (Future<List<MinuteData>> future : futures) {
            try {
                result.addAll(future.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();
        workbook.close();
        return result;
    }

    private LocalDateTime parseTime(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().withMinute(0).withSecond(0).withNano(0);
        }
        if (cell.getCellType() == CellType.STRING) {
            String val = cell.getStringCellValue().trim();
            try {
                if (val.matches("\\d{1,2}:\\d{2}")) {
                    LocalDateTime now = LocalDateTime.now();
                    String today = now.toLocalDate().toString();
                    return LocalDateTime.parse(today + " " + val, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                }
                return LocalDateTime.parse(val, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            } catch (DateTimeParseException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取表中数字
     *
     * @param cell
     * @return
     */
    private BigDecimal getNumericValue(Cell cell) {
        if (cell == null) return BigDecimal.ZERO;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        if (cell.getCellType() == CellType.STRING) {
            try {
                return BigDecimal.valueOf(Double.parseDouble(cell.getStringCellValue()));
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }
}

