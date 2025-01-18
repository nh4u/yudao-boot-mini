package cn.bitlinks.ems.framework.jackson.config;

import cn.bitlinks.ems.framework.common.util.date.DateUtils;
import cn.hutool.core.collection.CollUtil;
import cn.bitlinks.ems.framework.common.util.json.JsonUtils;
import cn.bitlinks.ems.framework.common.util.json.databind.NumberSerializer;
import cn.bitlinks.ems.framework.common.util.json.databind.TimestampLocalDateTimeDeserializer;
import cn.bitlinks.ems.framework.common.util.json.databind.TimestampLocalDateTimeSerializer;
import cn.hutool.core.date.DatePattern;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@AutoConfiguration
@Slf4j
public class EmsJacksonAutoConfiguration {

    @Bean
    @SuppressWarnings("InstantiationOfUtilityClass")
    public JsonUtils jsonUtils(List<ObjectMapper> objectMappers) {
        // 1.1 创建 SimpleModule 对象
        SimpleModule simpleModule = new SimpleModule();
        simpleModule
                // 新增 Long 类型序列化规则，数值超过 2^53-1，在 JS 会出现精度丢失问题，因此 Long 自动序列化为字符串类型
                .addSerializer(Long.class, NumberSerializer.INSTANCE)
                .addSerializer(Long.TYPE, NumberSerializer.INSTANCE)
                .addSerializer(LocalDate.class, LocalDateSerializer.INSTANCE)
                .addDeserializer(LocalDate.class, LocalDateDeserializer.INSTANCE)
                .addSerializer(LocalTime.class, LocalTimeSerializer.INSTANCE)
                .addDeserializer(LocalTime.class, LocalTimeDeserializer.INSTANCE)
                // 新增 LocalDateTime 序列化、反序列化规则，使用 Long 时间戳
//                .addSerializer(LocalDateTime.class, TimestampLocalDateTimeSerializer.INSTANCE)
//                .addDeserializer(LocalDateTime.class, TimestampLocalDateTimeDeserializer.INSTANCE);
                .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)))
                .addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)));
        // 1.2 注册到 objectMapper
        objectMappers.forEach(objectMapper -> objectMapper.registerModule(simpleModule));

        // 2. 设置 objectMapper 到 JsonUtils
        JsonUtils.init(CollUtil.getFirst(objectMappers));
        log.info("[init][初始化 JsonUtils 成功]");
        return new JsonUtils();
    }

}
