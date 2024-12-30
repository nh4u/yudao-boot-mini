import cn.bitlinks.ems.framework.tenant.core.context.TenantContextHolder;
import cn.bitlinks.ems.module.power.PowerApplication;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamVO;
import cn.bitlinks.ems.module.power.dal.mysql.labelconfig.LabelConfigMapper;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.bitlinks.ems.module.power.service.statistics.StatisticsService;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@SpringBootTest(classes = PowerApplication.class)
@Slf4j
//@Import(LabelConfigServiceImpl.class)
class EmsPowerTests {


    @Autowired
    private StatisticsService statisticsService;

    @Resource
    private LabelConfigService labelConfigService;


    @Resource
    private LabelConfigMapper labelConfigMapper;

    @Test
    void dateTest() {

//        YdmeAcquisitionInfo ydmeAcquisitionInfo = ydmeAcquisitionInfoService.getById(300000100380L);
        DateTime startDateTime = DateUtil.parse("2023-10-14 00:00:00", DatePattern.CHINESE_DATE_PATTERN);
        DateTime endDateTime = DateUtil.parse("2023-10-17 12:00:00", DatePattern.NORM_DATETIME_PATTERN);
        long daysNum = DateUtil.between(DateUtil.beginOfDay(startDateTime), DateUtil.beginOfDay(endDateTime), DateUnit.DAY);
        long secondNum = DateUtil.between(startDateTime, endDateTime, DateUnit.SECOND);
        DateTime minEndDateTime = DateUtil.offsetDay(startDateTime, 1);
        DateTime aa = DateUtil.offsetDay(startDateTime, -1);
        System.out.println(startDateTime);
        System.out.println(endDateTime);
        System.out.println("-----------------------------------");
        System.out.println(daysNum);
        System.out.println(secondNum);
        System.out.println("====================================");
        System.out.println(minEndDateTime);
        System.out.println(aa);

        // 比较一下时间和初始时间的比较值
//        DateTime dateTime = DateUtil.beginOfDay(startDateTime);
//        System.out.println(dateTime);
//        if (dateTime.equals(startDateTime)){
//            System.out.println("相等");
//
//        }


        // startDateTime > dateTime   结果为：1
        // startDateTime < dateTime   结果为：-1
        // startDateTime = dateTime   结果为：0
//        int compare = DateUtil.compare(startDateTime, dateTime);
//        System.out.println(compare);
//        System.out.println(DateUtil.compare(dateTime,startDateTime));
//        System.out.println(DateUtil.compare(startDateTime,startDateTime));

//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime of = LocalDateTime.of(now.getYear(), 3, 31, 10, 0,0);
//        System.out.println(of);
//        LocalDateTime plusMonths = of.plusMonths(1);
//        System.out.println(plusMonths);
//        LocalDateTime minusMonths = of.minusMonths(1);
//        System.out.println(minusMonths);
//
//        LocalDateTime localDateTime = minusMonths.plusMonths(1);
//        System.out.println(localDateTime);

        LocalDateTime start = LocalDateTime.of(2024, 3, 31, 10, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 3, 31, 9, 55, 0);

        Duration duration = Duration.between(start, end);
        long hours = duration.toHours();//相差的小时数
        long l = duration.toMinutes();
        System.out.println(l);
        System.out.println(duration.getSeconds());

        String content = "【碳排报告】{year}年{month}月碳排报告";
        Integer year = start.getYear();
        Integer month = start.getMonthValue();
        String replace = content.replace("{year}", year.toString())
                .replace("{month}", month.toString());
        System.out.println(replace);
    }


    @Test
    void fileNameTest() {
        String name = "双碳优化及新增内直接容0402.rar";
        String prefix = FilenameUtils.getPrefix(name);
        String extension = FilenameUtils.getExtension(name);
        String name1 = FilenameUtils.getName(name);
        System.out.println(FilenameUtils.getBaseName(name));
        System.out.println(name1);
        System.out.println(prefix);
        System.out.println(extension);

        System.out.println(StrUtil.contains(name, "直接"));
    }

    @Test
    void fileName() throws IOException {
        String filePath = "C:\\Users\\liumingqiang\\Desktop\\xx\\1.zip";

        byte[] b = new byte[28];
        InputStream inputStream = null;

        try {
            inputStream = new FileInputStream(filePath);

//            FileMagic fileMagic = FileMagic.valueOf(inputStream);
//            System.out.println(fileMagic);
            inputStream.read(b, 0, 28);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        StringBuilder stringBuilder = new StringBuilder("");

        for (int i = 0; i < b.length; i++) {
            int v = b[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }

        System.out.println(stringBuilder.toString());
    }


    @Test
    void getTableHeaderTest() {

        TenantContextHolder.setTenantId(1L);

        StatisticsParamVO paramVO = new StatisticsParamVO();
        LocalDate now = LocalDate.now();
        LocalDate end = now.plusDays(10);
        LocalDate[] range = new LocalDate[]{now, end};
        paramVO.setRange(range);
        Map<String, Object> jsonObject = statisticsService.standardCoalAnalysis(paramVO);
        System.out.println(jsonObject);
    }


    /**
     * 返回参数 是 2个或3个的时候，可以使用ImmutablePair、ImmutableTriple而不是丢失变量类型的Map，即可以同时返回不同类型的返回值。
     */
    @Test
    void responseBodyTest() {
        ImmutableTriple<String, Integer, Boolean> mutableTriple = ImmutableTriple.of("李四", 19, false);
        ImmutablePair<Integer, String> pair = ImmutablePair.of(1, "xx");
    }
}
