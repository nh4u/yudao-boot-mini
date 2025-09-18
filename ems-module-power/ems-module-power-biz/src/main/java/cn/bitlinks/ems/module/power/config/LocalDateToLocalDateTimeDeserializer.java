package cn.bitlinks.ems.module.power.config;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

/**
 * 前端传来的 "2025-09-12"（只有日期，格式为 yyyy-MM-dd）自动转换为后端 Java 类型 LocalDateTime，并且自动补全时间为 "2025-09-12T00:00:00"
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/09/18 11:04
 **/

public class LocalDateToLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(FORMAT_YEAR_MONTH_DAY);

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // 1. 从前端 JSON 读取原始字符串，比如 "2025-09-12"
        String dateString = p.getText().trim();

        // 2. 解析为 LocalDate
        LocalDate localDate = LocalDate.parse(dateString, DATE_FORMATTER);

        // 3. 转为当天的 00:00:00，即 LocalDateTime
        return localDate.atStartOfDay();
    }
}