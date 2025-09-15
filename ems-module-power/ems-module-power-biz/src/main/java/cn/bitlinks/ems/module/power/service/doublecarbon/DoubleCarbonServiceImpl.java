package cn.bitlinks.ems.module.power.service.doublecarbon;

import cn.bitlinks.ems.framework.common.exception.ErrorCode;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.doublecarbon.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.doublecarbon.DoubleCarbonMappingDO;
import cn.bitlinks.ems.module.power.dal.dataobject.doublecarbon.DoubleCarbonSettingsDO;
import cn.bitlinks.ems.module.power.dal.mysql.doublecarbon.DoubleCarbonMappingMapper;
import cn.bitlinks.ems.module.power.dal.mysql.doublecarbon.DoubleCarbonSettingsMapper;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.*;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;


@Service
@Slf4j
@Validated
public class DoubleCarbonServiceImpl implements DoubleCarbonService {
    @Resource
    private DoubleCarbonSettingsMapper doubleCarbonSettingsMapper;
    @Resource
    private DoubleCarbonMappingMapper doubleCarbonMappingMapper;

    @Override
    public DoubleCarbonSettingsRespVO getSettings() {
        DoubleCarbonSettingsDO settingsDO = doubleCarbonSettingsMapper.selectOne(null);
        return BeanUtils.toBean(settingsDO, DoubleCarbonSettingsRespVO.class);
    }


    @Override
    public void updSettings(DoubleCarbonSettingsUpdVO updVO) {
        doubleCarbonSettingsMapper.update(new LambdaUpdateWrapper<DoubleCarbonSettingsDO>()
                .set(DoubleCarbonSettingsDO::getUpdateFrequency, updVO.getUpdateFrequency())
                .set(DoubleCarbonSettingsDO::getUpdateFrequencyUnit, updVO.getUpdateFrequencyUnit())
                .eq(DoubleCarbonSettingsDO::getId, updVO.getId())
        );
    }

    @Override
    public void updMapping(DoubleCarbonMappingUpdVO updVO) {
        // 校验台账是否存在
        DoubleCarbonMappingDO doubleCarbonMappingDO = doubleCarbonMappingMapper.selectById(updVO.getId());

        if (StringUtils.isEmpty(updVO.getDoubleCarbonCode())) {
            return;
        }
        // 如果编码被修改，校验新编码是否重复
        if (!updVO.getDoubleCarbonCode().equals(doubleCarbonMappingDO.getDoubleCarbonCode())) {
            Long existCount = doubleCarbonMappingMapper.selectCount(new LambdaQueryWrapperX<DoubleCarbonMappingDO>()
                    .eq(DoubleCarbonMappingDO::getDoubleCarbonCode, updVO.getDoubleCarbonCode())
                    .ne(DoubleCarbonMappingDO::getId, updVO.getId())); // 排除自身
            if (existCount > 0) {
                throw exception(DOUBLE_CARBON_CODE_DUPLICATE);
            }
        }
        doubleCarbonMappingMapper.update(new LambdaUpdateWrapper<DoubleCarbonMappingDO>()
                .set(DoubleCarbonMappingDO::getDoubleCarbonCode, updVO.getDoubleCarbonCode())
                .eq(DoubleCarbonMappingDO::getId, updVO.getId())
        );
    }

    @Override
    public PageResult<DoubleCarbonMappingRespVO> getMappingPage(DoubleCarbonMappingPageReqVO pageReqVO) {
        PageResult<DoubleCarbonMappingRespVO> pageResult = new PageResult<>();
        PageResult<DoubleCarbonMappingDO> page = doubleCarbonMappingMapper.selectPage(pageReqVO);
        if (page == null) {
            pageResult.setTotal(0L);
            pageResult.setList(Collections.emptyList());
            return pageResult;
        }
        pageResult.setList(BeanUtils.toBean(page.getList(), DoubleCarbonMappingRespVO.class));
        pageResult.setTotal(page.getTotal());

        return pageResult;
    }

    @Override
    public void addMapping(String standingbookCode) {
        if (StringUtils.isEmpty(standingbookCode)) {
            return;
        }
        DoubleCarbonMappingDO mappingDO = new DoubleCarbonMappingDO();
        mappingDO.setStandingbookCode(standingbookCode);
        doubleCarbonMappingMapper.insert(mappingDO);
    }

    @Override
    public void delMapping(List<String> standingbookCodes) {
        if (CollUtil.isEmpty(standingbookCodes)) {
            return;
        }
        doubleCarbonMappingMapper.delete(new LambdaQueryWrapperX<DoubleCarbonMappingDO>()
                .in(DoubleCarbonMappingDO::getStandingbookCode, standingbookCodes));
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 添加事务，异常则回滚所有导入
    public DoubleCarbonMappingImportRespVO importExcel(MultipartFile file) {
        // 1.1 参数校验
        if (file.isEmpty()) {
            throw exception(STANDINGBOOK_IMPORT_FILE_ERROR);
        }
        String fileName = file.getOriginalFilename();
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            throw exception(STANDINGBOOK_IMPORT_EXCEL_ERROR);
        }

        // 2. 遍历，逐个创建 or 更新
        DoubleCarbonMappingImportRespVO respVO = DoubleCarbonMappingImportRespVO
                .builder()
                .updateCodes(new ArrayList<>())
                .failureCodes(new LinkedHashMap<>())
                .build();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);


            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String doubleCarbonCode = getCellValue(row.getCell(0));
                String standingbookCode = getCellValue(row.getCell(3));

                Long count = doubleCarbonMappingMapper.selectCount("standingbook_code", standingbookCode);
                if (count.equals(0L) && Objects.nonNull(standingbookCode)) {
                    respVO.getFailureCodes().put(standingbookCode, "数据库不存在");
                } else {
                    doubleCarbonMappingMapper.update(new LambdaUpdateWrapper<DoubleCarbonMappingDO>()
                            .set(DoubleCarbonMappingDO::getDoubleCarbonCode, doubleCarbonCode)
                            .eq(DoubleCarbonMappingDO::getStandingbookCode, standingbookCode));

                    respVO.getUpdateCodes().add(standingbookCode);
                }
            }

        } catch (Exception e) {
            log.error("文件解析失败", e);
            throw exception(new ErrorCode(STANDINGBOOK_IMPORT_ALL_ERROR.getCode(), e.getMessage()));
        }


        return respVO;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return null;
            default:
                return null;
        }
    }
}
