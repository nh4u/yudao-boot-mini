package cn.bitlinks.ems.module.power.service.sharefile;

import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.AcqDataExcelCoordinate;
import cn.bitlinks.ems.module.power.dal.dataobject.sharefile.ShareFileSettingsDO;
import cn.bitlinks.ems.module.power.dal.mysql.sharefile.ShareFileSettingsMapper;
import cn.bitlinks.ems.module.power.service.additionalrecording.ExcelMeterDataProcessor2;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.bitlinks.ems.module.power.utils.ShareFileUtil.scanAndCollectExcelFiles;

/**
 * 服务设置 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
@Slf4j
public class ShareFileSettingsServiceImpl implements ShareFileSettingsService {

    @Resource
    private ShareFileSettingsMapper shareFileSettingsMapper;

    @Resource
    private ExcelMeterDataProcessor2 excelMeterDataProcessor2;

    @Value("${ems.excel-sleep}")
    private long excelSleep;
    @Override
    public void dealFile(String filePath, Boolean year) {

        // 3. 访问该路径，获取所有 Excel 文件
        List<File> excelFiles = scanAndCollectExcelFiles(filePath);
        log.info("excel共享补录开始处理：{}", excelFiles.size());
        // 4. 遍历并处理每一个 File（这里只是打印文件名，后续你可解析或上传）
        for (File excelFile : excelFiles) {
            // 每个文件的两个流都声明在 try() 中，循环一次自动关闭一次，不影响下一个文件
            try (InputStream excelStream1 = Files.newInputStream(excelFile.toPath());
                 InputStream excelStream2 = Files.newInputStream(excelFile.toPath())) {
                // 注意：使用完后需要自己关闭流！！或者用 try-with-resources
                // 1.调用自动识别功能，
                AcqDataExcelCoordinate excelImportCoordinate = excelMeterDataProcessor2.getExcelImportCoordinate(excelStream1);

                if(year){
                    // 2.然后调用导入功能
                    excelMeterDataProcessor2.processYear(
                            excelStream2,
                            excelImportCoordinate.getAcqTimeStart(),
                            excelImportCoordinate.getAcqTimeEnd(),
                            excelImportCoordinate.getAcqNameStart(),
                            excelImportCoordinate.getAcqNameEnd());
                }else{
                    excelMeterDataProcessor2.process(
                            excelStream2,
                            excelImportCoordinate.getAcqTimeStart(),
                            excelImportCoordinate.getAcqTimeEnd(),
                            excelImportCoordinate.getAcqNameStart(),
                            excelImportCoordinate.getAcqNameEnd());
                }
                log.info("excel共享补录 手动执行指定目录excel文件[{}]完成", excelFile.getName());
            } catch (Exception e) {
                log.error("excel共享补录 手动执行指定目录excel文件[{}]写入失败:{}", excelFile.getName(), e.getMessage(), e);
            }
            // 限速，避免瞬时压力过大
            try {

                Thread.sleep(excelSleep);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }


    }

    @Override
    public void dealFile() throws IOException {

        List<ShareFileSettingsDO> shareFileSettingsDOS = shareFileSettingsMapper.selectList();
        LocalDate yesterday = LocalDate.now().minusDays(1);

        if (CollUtil.isEmpty(shareFileSettingsDOS)) {
            return;
        }
        for (ShareFileSettingsDO shareFileSettingsDO : shareFileSettingsDOS) {

            String dir = shareFileSettingsDO.getDir();
            Integer type = shareFileSettingsDO.getType();

            if (dir == null || dir.trim().isEmpty()) {
                log.error("【{}】未获取到有效的 UNC 路径", shareFileSettingsDO.getName());
                continue;
            }

            String path = dir.trim();

            // 确保 UNC 路径以 \ 结尾，如果用户输入没有，我们补上
            if (!path.endsWith(StrPool.SLASH)) {
                path = path + StrPool.SLASH;
            }

            if (type == 1) {
                // 1：年月日；
                path = path + yesterday.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")) + StrPool.SLASH;
            } else {
                // 2：年。
                path = path + yesterday.getYear() + StrPool.SLASH;
            }

            // 3. 访问该路径，获取所有 Excel 文件
            List<File> excelFiles = scanAndCollectExcelFiles(path);

            // 4. 遍历并处理每一个 File（这里只是打印文件名，后续你可解析或上传）
            for (File excelFile : excelFiles) {
                // 每个文件的两个流都声明在 try() 中，循环一次自动关闭一次，不影响下一个文件
                try (InputStream excelStream1 = Files.newInputStream(excelFile.toPath());
                     InputStream excelStream2 = Files.newInputStream(excelFile.toPath())) {
                    // 注意：使用完后需要自己关闭流！！或者用 try-with-resources
                    // 1.调用自动识别功能，
                    AcqDataExcelCoordinate excelImportCoordinate = excelMeterDataProcessor2.getExcelImportCoordinate(excelStream1);

                    // 2.然后调用导入功能
                    if (type == 1) {
                        excelMeterDataProcessor2.process(
                                excelStream2,
                                excelImportCoordinate.getAcqTimeStart(),
                                excelImportCoordinate.getAcqTimeEnd(),
                                excelImportCoordinate.getAcqNameStart(),
                                excelImportCoordinate.getAcqNameEnd());
                    }else{
                        excelMeterDataProcessor2.processYear(
                                excelStream2,
                                excelImportCoordinate.getAcqTimeStart(),
                                excelImportCoordinate.getAcqTimeEnd(),
                                excelImportCoordinate.getAcqNameStart(),
                                excelImportCoordinate.getAcqNameEnd());
                    }
                } catch (Exception e) {
                    log.error("excel文件[{}]写入失败:{}", excelFile.getName(), e.getMessage(), e);
                }
                // 限速，避免瞬时压力过大
                try {
                    Thread.sleep(excelSleep);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // 5. 注意：调用方应负责关闭这些流
            log.info("共获取到 " + excelFiles.size() + " 个 Excel 文件");

        }
    }

    @Override
    public Map<String, List<Map<String, Object>>> testShareFile() throws IOException {

        Map<String, List<Map<String, Object>>> map = new HashMap<>();

        List<ShareFileSettingsDO> shareFileSettingsDOS = shareFileSettingsMapper.selectList();
        LocalDate yesterday = LocalDate.now().minusDays(1);

        if (CollUtil.isEmpty(shareFileSettingsDOS)) {
            return map;
        }
        for (ShareFileSettingsDO shareFileSettingsDO : shareFileSettingsDOS) {

            List<Map<String, Object>> result = new ArrayList<>();

            String dir = shareFileSettingsDO.getDir();
            Integer type = shareFileSettingsDO.getType();

            if (dir == null || dir.trim().isEmpty()) {
                log.error("【{}】未获取到有效的 UNC 路径", shareFileSettingsDO.getName());
                continue;
            }

            String path = dir.trim();

            // 确保 UNC 路径以 \ 结尾，如果用户输入没有，我们补上
            if (!path.endsWith(StrPool.SLASH)) {
                path = path + StrPool.SLASH;
            }

            if (type == 1) {
                // 1：年月日；
                path = path + yesterday.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")) + StrPool.SLASH;
            } else {
                // 2：年。
                path = path + yesterday.getYear() + StrPool.SLASH;
            }

            // 3. 访问该路径，获取所有 Excel 文件
            List<File> excelFiles = scanAndCollectExcelFiles(path);

            // 4. 遍历并处理每一个 File（这里只是打印文件名，后续你可解析或上传）
            for (File excelFile : excelFiles) {
                try {
                    // 注意：使用完后需要自己关闭流！！或者用 try-with-resources
                    // 1.调用自动识别功能，
                    InputStream excelStream1 = Files.newInputStream(excelFile.toPath());
                    AcqDataExcelCoordinate excelImportCoordinate = excelMeterDataProcessor2.getExcelImportCoordinate(excelStream1);
                    Map<String, Object> map1 = new HashMap<>();
                    map1.put("fileName", excelFile.getName());
                    map1.put("coordinate", excelImportCoordinate);
                    result.add(map1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // 5. 注意：调用方应负责关闭这些流
            log.info("共获取到 " + excelFiles.size() + " 个 Excel 文件");

            map.put(shareFileSettingsDO.getName(), result);
        }

        return map;
    }


}