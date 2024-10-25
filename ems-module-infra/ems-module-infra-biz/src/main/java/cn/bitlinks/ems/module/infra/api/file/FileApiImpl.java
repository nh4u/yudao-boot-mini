package cn.bitlinks.ems.module.infra.api.file;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.infra.api.file.dto.FileCreateReqDTO;
import cn.bitlinks.ems.module.infra.service.file.FileService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

@RestController // 提供 RESTful API 接口，给 Feign 调用
@Validated
public class FileApiImpl implements FileApi {

    @Resource
    private FileService fileService;

    @Override
    public CommonResult<String> createFile(FileCreateReqDTO createReqDTO) {
        return success(fileService.createFile(createReqDTO.getName(), createReqDTO.getPath(),
                createReqDTO.getContent()));
    }

    @Override
    public CommonResult<Long> createFileReturnId(FileCreateReqDTO createReqDTO) {
        return success(fileService.createFileReturnDO(createReqDTO.getName(), createReqDTO.getPath(),
                createReqDTO.getContent()).getId());
    }

    @Override
    public CommonResult getFile(Long id) {
        return success(fileService.getFile(id));
    }

    @Override
    public CommonResult<byte[]> getFileContent( FileCreateReqDTO createReqDTO) throws Exception {
        return success(fileService.getFileContent(createReqDTO.getConfigId(), createReqDTO.getPath()));
    }

}
