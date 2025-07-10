package cn.bitlinks.ems.framework.common.util.calc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

public class AggSplitUtils {
    public static BigDecimal calculatePerMinuteIncrement(LocalDateTime startTime, LocalDateTime endTime,
                                                         BigDecimal startFull, BigDecimal endFull) {
        if (startTime == null || endTime == null || startFull == null || endFull == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes <= 0) {
            throw new IllegalArgumentException("起止时间必须相差至少1分钟");
        }

        BigDecimal totalIncrement = endFull.subtract(startFull);
        return totalIncrement.divide(BigDecimal.valueOf(minutes), 10, RoundingMode.HALF_UP);
    }
}
