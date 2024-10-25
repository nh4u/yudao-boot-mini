package cn.bitlinks.ems.module.infra.api.file;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.infra.api.file.dto.FileCreateReqDTO;
import cn.bitlinks.ems.module.infra.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;

@FeignClient(name = ApiConstants.NAME) // TODO bitlinks：fallbackFactory =
@Tag(name = "RPC 服务 - 文件")
public interface FileApi {

    String PREFIX = ApiConstants.PREFIX + "/file";

    /**
     * 保存文件，并返回文件的访问路径
     *
     * @param content 文件内容
     * @return 文件路径
     */
    default String createFile(byte[] content) {
        return createFile(null, null, content);
    }

    /**
     * 保存文件，并返回文件的访问路径
     *
     * @param path 文件路径
     * @param content 文件内容
     * @return 文件路径
     */
    default String createFile(String path, byte[] content) {
        return createFile(null, path, content);
    }

    /**
     * 保存文件，并返回文件的访问路径
     *
     * @param name 原文件名称
     * @param path 文件路径
     * @param content 文件内容
     * @return 文件路径
     */
    default String createFile(@RequestParam("name") String name,
                              @RequestParam("path") String path,
                              @RequestParam("content") byte[] content) {
        return createFile(new FileCreateReqDTO().setName(name).setPath(path).setContent(content)).getCheckedData();
    }

    @PostMapping(PREFIX + "/create")
    @Operation(summary = "保存文件，并返回文件的访问路径")
    CommonResult<String> createFile(@Valid @RequestBody FileCreateReqDTO createReqDTO);
    @PostMapping(PREFIX + "/createFile")
    @Operation(summary = "保存文件，并返回文件的访问路径")
    CommonResult<Long> createFileReturnId(@Valid @RequestBody FileCreateReqDTO createReqDTO);
    @GetMapping(PREFIX + "/getFile")
    @Operation(summary = "保存文件，并返回文件的访问路径")
    CommonResult getFile(@RequestParam("id")  Long id);
    @PostMapping(PREFIX + "/getFileContent")
    @Operation(summary = "返回文件内容")
    CommonResult<byte[]> getFileContent(  @RequestBody FileCreateReqDTO createReqDTO) throws Exception;
    default  CommonResult<byte[]> getFileContent(Long id,String path) throws Exception {
         FileCreateReqDTO createReqDTO=new FileCreateReqDTO();
         createReqDTO.setConfigId(id);
         createReqDTO.setPath(path);
        return getFileContent(createReqDTO);
    }
}
