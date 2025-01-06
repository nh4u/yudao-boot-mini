package cn.bitlinks.ems.module.power.utils;

import cn.hutool.core.date.LocalDateTimeUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
}
