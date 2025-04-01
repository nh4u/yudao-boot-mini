package cn.bitlinks.ems.module.power.service.starrocks;

import cn.bitlinks.ems.framework.common.exception.ErrorCode;
import cn.bitlinks.ems.module.power.dal.mysql.starrocks.StarrocksMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.LABEL_CONFIG_NOT_EXISTS;

/**
 * Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
@DS("starrocks")
public class StarrocksServiceImpl implements StarrocksService {

    @Resource
    private StarrocksMapper starrocksMapper;

    @Override
    public List<Map<String, Objects>> queryData() {

        List<Map<String, Objects>> map = starrocksMapper.queryData();
        map.forEach(System.out::println);

        return map;
    }

    @Override
    public void addData(String date) {

        if (Objects.isNull(date)) {
            ErrorCode message = new ErrorCode(1_000_000_000, "日期不存在");
            throw exception(message);
        }
        starrocksMapper.addData(date);
    }

    @Override
    public void deleteData(String date) {
        if (Objects.isNull(date)) {
            ErrorCode message = new ErrorCode(1_000_000_000, "日期不存在");
            throw exception(message);
        }
        starrocksMapper.deleteData(date);
    }

    @Override
    public BigDecimal getEnergyUsage(LocalDateTime startTime, LocalDateTime endTime) {
        // 转换日期格式（根据StarRocks实际存储格式调整）
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return starrocksMapper.selectUsage(
                startTime.format(formatter),
                endTime.format(formatter)
        );
    }
}