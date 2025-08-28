package cn.bitlinks.ems.module.power.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.text.StrSplitter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static cn.bitlinks.ems.module.power.enums.CommonConstants.*;

/**
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/01/03 11:30
 **/
public class CommonUtil {

    /**
     * 格式 时间list ['2024/5/5','2024/5/5','2024/5/5'];
     *
     * @param rangeOrigin 时间范围
     * @return ['2024/5/5','2024/5/5','2024/5/5']
     */
    public static List<String> getTableHeader(LocalDateTime[] rangeOrigin, Integer dateType) {

        List<String> headerList = new ArrayList<>();

        LocalDate[] range = new LocalDate[]{rangeOrigin[0].toLocalDate(), rangeOrigin[1].toLocalDate()};
        LocalDate startDate = range[0];
        LocalDate endDate = range[1];

        if (1 == dateType) {
            // 月
            LocalDate tempStartDate = LocalDate.of(startDate.getYear(), startDate.getMonth(), 1);
            LocalDate tempEndDate = LocalDate.of(endDate.getYear(), endDate.getMonth(), 1);

            while (tempStartDate.isBefore(tempEndDate) || tempStartDate.isEqual(tempEndDate)) {

                int year = tempStartDate.getYear();
                int month = tempStartDate.getMonthValue();
                String monthSuffix = (month < 10 ? "-0" : "-") + month;
                headerList.add(year + monthSuffix);

                tempStartDate = tempStartDate.plusMonths(1);
            }

        } else if (2 == dateType) {
            // 年
            while (startDate.getYear() <= endDate.getYear()) {

                headerList.add(String.valueOf(startDate.getYear()));

                startDate = startDate.plusYears(1);
            }
        } else if (3 == dateType) {
            // 时
            LocalDateTime startDateTime = rangeOrigin[0];
            LocalDateTime endDateTime = rangeOrigin[1];

            while (startDateTime.isBefore(endDateTime) || startDateTime.isEqual(endDateTime)) {
                String formattedDate = LocalDateTimeUtil.format(startDateTime, "yyyy-MM-dd:HH");
                headerList.add(formattedDate);
                startDateTime = startDateTime.plusHours(1);
            }

        } else {
            // 日
            while (startDate.isBefore(endDate) || startDate.isEqual(endDate)) {

                String formattedDate = LocalDateTimeUtil.formatNormal(startDate);
                headerList.add(formattedDate);

                startDate = startDate.plusDays(1);
            }
        }

        return headerList;
    }

    /**
     * 同比率计算（避免除零）
     */
    public static BigDecimal calculateYearOnYearRatio(BigDecimal now, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0 || now == null) {
            return null;
        }
        return now.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * 定基比率计算（避免除零）
     */
    public static BigDecimal calculateBaseRatio(BigDecimal now, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0 || now == null) {
            return null;
        }
        return now.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public static BigDecimal dealBigDecimalScale(BigDecimal num, Integer scale) {
        if (num != null) {
            return num.setScale(scale, RoundingMode.HALF_UP);

        }
        return null;
    }

    public static Integer getLabelDeep(String childLabels) {

        Integer defaultDeep = 1;
        // 下级标签
        List<String> childLabelValues = StrSplitter.split(childLabels, "#", 0, true, true);
        if (CollUtil.isNotEmpty(childLabelValues)) {
            Optional<Integer> max = childLabelValues.stream()
                    .map(c -> {
                        List<String> split = StrSplitter.split(c, ",", 0, true, true);
                        return split.size();
                    }).max(Comparator.naturalOrder());
            if (max.isPresent()) {
                // 最多五层 top已经占了一层 deep最多是4
                Integer deep = max.get() + defaultDeep;
                if (deep > LABEL_MAX_DISPLAY_DEEP) {
                    return LABEL_MAX_DISPLAY_DEEP;
                }
                return deep;
            }
        }

        return defaultDeep;
    }

    /**
     * 两个数据相加
     *
     * @param first  1
     * @param second 2
     * @return add
     */
    public static BigDecimal addBigDecimal(BigDecimal first, BigDecimal second) {

        if (Objects.isNull(first)) {
            return second;
        } else {
            if (Objects.isNull(second)) {
                return first;
            }
            return first.add(second);
        }

    }

    /**
     * 根据数据返回对应数据 or /
     *
     * @param num 对应list
     * @return
     */
    public static Object getConvertData(BigDecimal num) {
        return !Objects.isNull(num) ? num : StrPool.SLASH;
    }


    /**
     * 根据数据返回对应数据 or /
     *
     * @param num 对应list
     * @return
     */
    public static Object getConvertData(Integer unit, Integer flag, BigDecimal num) {
        return !Objects.isNull(num) ? getNum(unit, flag, num) : StrPool.SLASH;
    }

    public static BigDecimal getNum(Integer unit, Integer flag, BigDecimal num) {
        if (flag == 1) {
            if (unit == 1) {
                return num;
            } else {
                // tce
                return num.divide(BigDecimal.valueOf(1000L), 2, RoundingMode.HALF_UP);
            }
        } else {
            if (unit == 1) {
                return num;
            } else {
                // 万元
                return num.divide(BigDecimal.valueOf(10000L), 2, RoundingMode.HALF_UP);
            }
        }

    }

    public static String getHeaderDesc(Integer unit, Integer flag, String prefix) {
        if (flag == 1) {
            if (unit == 1) {
                return prefix + COAT_UNIT1;
            } else {
                return prefix + COAT_UNIT2;
            }
        } else {
            if (unit == 1) {
                return prefix + COST_UNIT1;
            } else {
                return prefix + COST_UNIT2;
            }
        }

    }


    /**
     * 计算占比
     *
     * @param now   当前
     * @param total 总计
     * @return
     */
    public static BigDecimal getProportion(BigDecimal now, BigDecimal total) {

        if (now == null || total == null) {
            return null;
        }
        BigDecimal proportion = null;
        if (total.compareTo(BigDecimal.ZERO) != 0) {
            proportion = now.divide(total, 10, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return proportion;
    }

    /**
     * 获得标签名称
     *
     * @param label1
     * @param label2
     * @param label3
     * @param label4
     * @param label5
     * @return
     */
    public static String getName(String label1, String label2, String label3, String label4, String label5) {
        if (CharSequenceUtil.isNotEmpty(label5) && !StrPool.SLASH.equals(label5)) {
            return label5;
        }
        if (CharSequenceUtil.isNotEmpty(label4) && !StrPool.SLASH.equals(label4)) {
            return label4;
        }
        if (CharSequenceUtil.isNotEmpty(label3) && !StrPool.SLASH.equals(label3)) {
            return label3;
        }
        if (CharSequenceUtil.isNotEmpty(label2) && !StrPool.SLASH.equals(label2)) {
            return label2;
        }
        if (CharSequenceUtil.isNotEmpty(label1) && !StrPool.SLASH.equals(label1)) {
            return label1;
        }
        return null;
    }
    public static BigDecimal safeDivide100(BigDecimal numerator, BigDecimal denominator) {

        // 检查值是否为null
        if (numerator == null || denominator == null) {
            return null;
        }

        // 检查分母是否为零
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        // 执行除法运算
        return numerator.multiply(new BigDecimal(100)).divide(denominator, DEFAULT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * BigDecimal除法，包含有效位
     *
     * @param num
     * @param sum
     * @param scale
     * @return
     */
    public static BigDecimal divideWithScale(BigDecimal num, BigDecimal sum, Integer scale) {
        if (Objects.isNull(scale)) {
            return null;
        }

        if (num == null || sum == null || sum.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return sum.divide(num, scale, RoundingMode.HALF_UP);
    }
}
